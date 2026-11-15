package com.ventaspost.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.domain.model.CondicionIva;
import com.ventaspost.domain.model.ConfiguracionArcaSucursal;
import com.ventaspost.domain.model.EntornoArca;
import com.ventaspost.domain.model.TipoComprobanteArca;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ArcaWsClient {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter FORMATO_FECHA_AFIP = DateTimeFormatter.ofPattern("yyyyMMdd");

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    public record ResumenCertificadoX509(
            String subject,
            String fechaVencimiento
    ) {}

    public record TicketAccesoWsaa(
            String token,
            String sign,
            LocalDateTime expiracion
    ) {}

    public record AlicuotaIvaDetalle(
            int codigoAfipAlicuota,
            BigDecimal baseImponible,
            BigDecimal importeIva
    ) {}

    public record SolicitudCaeDatos(
            TipoComprobanteArca tipoComprobante,
            int puntoVenta,
            LocalDate fechaEmision,
            int receptorTipoDocAfip,
            long receptorNroDocAfip,
            CondicionIva receptorCondicionIva,
            BigDecimal importeTotal,
            BigDecimal importeNetoGravado,
            BigDecimal importeExento,
            BigDecimal importeIvaTotal,
            List<AlicuotaIvaDetalle> alicuotasIva,
            Integer cbteAsocTipoAfip,
            Integer cbteAsocPtoVta,
            Long cbteAsocNro,
            LocalDate cbteAsocFecha
    ) {}

    public record ResultadoEmisionArca(
            long numeroComprobante,
            String cae,
            LocalDate caeFechaVencimiento,
            String observaciones
    ) {}

    /**
     * Analiza el certificado .crt PEM para mostrar su titular y fecha de expiración en el Panel Admin.
     */
    public ResumenCertificadoX509 parsearResumenCertificado(String certificadoPem) {
        if (certificadoPem == null || certificadoPem.isBlank()) {
            return new ResumenCertificadoX509("Sin certificado cargado", "-");
        }
        try {
            X509Certificate cert = cargarCertificadoX509(certificadoPem);
            String subject = cert.getSubjectX500Principal().getName();
            LocalDate vto = cert.getNotAfter().toInstant().atZone(ZONA_ARGENTINA).toLocalDate();
            return new ResumenCertificadoX509(subject, vto.toString());
        } catch (Exception e) {
            return new ResumenCertificadoX509("Certificado con formato inválido: " + e.getMessage(), "-");
        }
    }

    public X509Certificate cargarCertificadoX509(String certificadoPem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(certificadoPem.trim().getBytes(StandardCharsets.UTF_8))) {
            return (X509Certificate) cf.generateCertificate(bais);
        }
    }

    public PrivateKey cargarClavePrivada(String clavePrivadaPem) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(clavePrivadaPem.trim()))) {
            Object obj = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            if (obj instanceof PEMKeyPair keyPair) {
                return converter.getKeyPair(keyPair).getPrivate();
            } else if (obj instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo pkInfo) {
                return converter.getPrivateKey(pkInfo);
            }
            throw new IllegalArgumentException("No se pudo interpretar la clave privada PEM (.key).");
        }
    }

    /**
     * Obtiene un Ticket de Acceso (Token y Sign) desde WSAA firmando un LoginTicketRequest en formato CMS (PKCS#7).
     */
    public TicketAccesoWsaa autenticarWsaa(ConfiguracionArcaSucursal config) {
        if (!config.poseeCertificadoYClaveCargados()) {
            throw new BusinessException("No se han cargado el Certificado (.crt) y la Clave Privada (.key) para autenticar en WSAA de ARCA.");
        }
        try {
            X509Certificate cert = cargarCertificadoX509(config.getCertificadoCrtPem());
            PrivateKey privateKey = cargarClavePrivada(config.getClavePrivadaKeyPem());

            ZonedDateTime ahora = ZonedDateTime.now(ZONA_ARGENTINA);
            long uniqueId = ahora.toEpochSecond();
            String genTime = ahora.minusMinutes(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String expTime = ahora.plusHours(12).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            String loginTicketRequestXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <loginTicketRequest version="1.0">
                      <header>
                        <uniqueId>%d</uniqueId>
                        <generationTime>%s</generationTime>
                        <expirationTime>%s</expirationTime>
                      </header>
                      <service>wsfe</service>
                    </loginTicketRequest>
                    """.formatted(uniqueId, genTime, expTime).trim();

            CMSTypedData msg = new CMSProcessableByteArray(loginTicketRequestXml.getBytes(StandardCharsets.UTF_8));
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            ContentSigner sha256Signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(privateKey);

            gen.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build()
                    ).build(sha256Signer, cert)
            );
            gen.addCertificates(new JcaCertStore(List.of(cert)));

            CMSSignedData signedData = gen.generate(msg, true);
            String cmsBase64 = Base64.getEncoder().encodeToString(signedData.getEncoded());

            String soapEnvelope = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsaa="http://wsaa.view.sua.dvadac.desein.afip.gov">
                      <soapenv:Header/>
                      <soapenv:Body>
                        <wsaa:loginCms>
                          <wsaa:in0>%s</wsaa:in0>
                        </wsaa:loginCms>
                      </soapenv:Body>
                    </soapenv:Envelope>
                    """.formatted(cmsBase64).trim();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEntornoArca().getWsaaUrl()))
                    .timeout(Duration.ofSeconds(18))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "")
                    .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                String fault = extraerValorTagXml(response.body(), "faultstring");
                throw new RuntimeException("Error WSAA HTTP " + response.statusCode() + ": " + (fault != null ? fault : response.body()));
            }

            String loginCmsReturn = extraerValorTagXml(response.body(), "loginCmsReturn");
            if (loginCmsReturn == null || loginCmsReturn.isBlank()) {
                throw new RuntimeException("Respuesta WSAA sin nodo loginCmsReturn.");
            }

            String token = extraerValorTagXml(loginCmsReturn, "token");
            String sign = extraerValorTagXml(loginCmsReturn, "sign");
            if (token == null || sign == null) {
                throw new RuntimeException("No se recibieron Token y Sign válidos desde WSAA.");
            }

            return new TicketAccesoWsaa(token.trim(), sign.trim(), LocalDateTime.now().plusHours(11).plusMinutes(45));
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new RuntimeException("Fallo en autenticación WSAA de ARCA: " + e.getMessage(), e);
        }
    }

    /**
     * Consulta FECompUltimoAutorizado y solicita el CAE en WSFEv1 (FECAESolicitar).
     */
    public ResultadoEmisionArca emitirComprobanteWsfev1Real(
            ConfiguracionArcaSucursal config,
            String token,
            String sign,
            SolicitudCaeDatos datos) {
        try {
            String cuitLimpio = limpiarSoloDigitos(config.getCuitEmisor());
            long ultimoAutorizado = consultarUltimoComprobanteAutorizadoWsfev1(
                    config.getEntornoArca(),
                    token,
                    sign,
                    cuitLimpio,
                    datos.puntoVenta(),
                    datos.tipoComprobante().getCodigoAfip()
            );
            long proximoNumero = ultimoAutorizado + 1;

            StringBuilder ivaXml = new StringBuilder();
            // En Factura C (11) y Nota de Crédito C (13) ARCA prohíbe enviar el array <Iva>
            boolean llevaDetalleIva = datos.tipoComprobante() != TipoComprobanteArca.FACTURA_C
                    && datos.tipoComprobante() != TipoComprobanteArca.NOTA_CREDITO_C
                    && datos.alicuotasIva() != null
                    && !datos.alicuotasIva().isEmpty();

            if (llevaDetalleIva) {
                ivaXml.append("<ar:Iva>");
                for (AlicuotaIvaDetalle alic : datos.alicuotasIva()) {
                    if (alic.baseImponible().compareTo(BigDecimal.ZERO) > 0) {
                        ivaXml.append("""
                                <ar:AlicIva>
                                  <ar:Id>%d</ar:Id>
                                  <ar:BaseImp>%s</ar:BaseImp>
                                  <ar:Importe>%s</ar:Importe>
                                </ar:AlicIva>
                                """.formatted(alic.codigoAfipAlicuota(), alic.baseImponible().toPlainString(), alic.importeIva().toPlainString()));
                    }
                }
                ivaXml.append("</ar:Iva>");
            }

            StringBuilder cbteAsocXml = new StringBuilder();
            if (datos.cbteAsocTipoAfip() != null && datos.cbteAsocNro() != null) {
                String fechaAsoc = datos.cbteAsocFecha() != null
                        ? datos.cbteAsocFecha().format(FORMATO_FECHA_AFIP)
                        : datos.fechaEmision().format(FORMATO_FECHA_AFIP);
                cbteAsocXml.append("""
                        <ar:CbtesAsoc>
                          <ar:CbteAsoc>
                            <ar:Tipo>%d</ar:Tipo>
                            <ar:PtoVta>%d</ar:PtoVta>
                            <ar:Nro>%d</ar:Nro>
                            <ar:Cuit>%s</ar:Cuit>
                            <ar:CbteFch>%s</ar:CbteFch>
                          </ar:CbteAsoc>
                        </ar:CbtesAsoc>
                        """.formatted(
                        datos.cbteAsocTipoAfip(),
                        datos.cbteAsocPtoVta(),
                        datos.cbteAsocNro(),
                        cuitLimpio,
                        fechaAsoc
                ));
            }

            String fechaCbteStr = datos.fechaEmision().format(FORMATO_FECHA_AFIP);
            String soapBody = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ar="http://ar.gov.afip.dif.FEV1/">
                      <soapenv:Header/>
                      <soapenv:Body>
                        <ar:FECAESolicitar>
                          <ar:Auth>
                            <ar:Token>%s</ar:Token>
                            <ar:Sign>%s</ar:Sign>
                            <ar:Cuit>%s</ar:Cuit>
                          </ar:Auth>
                          <ar:FeCAEReq>
                            <ar:FeCabReq>
                              <ar:CantReg>1</ar:CantReg>
                              <ar:PtoVta>%d</ar:PtoVta>
                              <ar:CbteTipo>%d</ar:CbteTipo>
                            </ar:FeCabReq>
                            <ar:FeDetReq>
                              <ar:FECAEDetRequest>
                                <ar:Concepto>1</ar:Concepto>
                                <ar:DocTipo>%d</ar:DocTipo>
                                <ar:DocNro>%d</ar:DocNro>
                                <ar:CbteDesde>%d</ar:CbteDesde>
                                <ar:CbteHasta>%d</ar:CbteHasta>
                                <ar:CbteFch>%s</ar:CbteFch>
                                <ar:ImpTotal>%s</ar:ImpTotal>
                                <ar:ImpTotConc>0.00</ar:ImpTotConc>
                                <ar:ImpNeto>%s</ar:ImpNeto>
                                <ar:ImpOpEx>%s</ar:ImpOpEx>
                                <ar:ImpTrib>0.00</ar:ImpTrib>
                                <ar:ImpIVA>%s</ar:ImpIVA>
                                <ar:MonId>PES</ar:MonId>
                                <ar:MonCotiz>1</ar:MonCotiz>
                                <ar:CondicionIVAReceptorId>%d</ar:CondicionIVAReceptorId>
                                %s
                                %s
                              </ar:FECAEDetRequest>
                            </ar:FeDetReq>
                          </ar:FeCAEReq>
                        </ar:FECAESolicitar>
                      </soapenv:Body>
                    </soapenv:Envelope>
                    """.formatted(
                    token,
                    sign,
                    cuitLimpio,
                    datos.puntoVenta(),
                    datos.tipoComprobante().getCodigoAfip(),
                    datos.receptorTipoDocAfip(),
                    datos.receptorNroDocAfip(),
                    proximoNumero,
                    proximoNumero,
                    fechaCbteStr,
                    datos.importeTotal().toPlainString(),
                    datos.importeNetoGravado().toPlainString(),
                    datos.importeExento().toPlainString(),
                    datos.importeIvaTotal().toPlainString(),
                    datos.receptorCondicionIva().getCodigoAfipReceptor(),
                    cbteAsocXml.toString(),
                    ivaXml.toString()
            ).trim();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEntornoArca().getWsfev1Url()))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "http://ar.gov.afip.dif.FEV1/FECAESolicitar")
                    .POST(HttpRequest.BodyPublishers.ofString(soapBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Servidor WSFEv1 ARCA respondió HTTP " + response.statusCode());
            }

            String xmlResp = response.body();
            String resultado = extraerValorTagXml(xmlResp, "Resultado");
            String cae = extraerValorTagXml(xmlResp, "CAE");
            String caeFchVto = extraerValorTagXml(xmlResp, "CAEFchVto");
            String msgError = extraerValorTagXml(xmlResp, "Msg");

            if (!"A".equalsIgnoreCase(resultado) || cae == null || cae.isBlank()) {
                throw new RuntimeException("ARCA rechazó u observó la solicitud de CAE: "
                        + (msgError != null ? msgError : "Resultado=" + resultado));
            }

            LocalDate fechaVtoCae = (caeFchVto != null && caeFchVto.length() == 8)
                    ? LocalDate.parse(caeFchVto, FORMATO_FECHA_AFIP)
                    : datos.fechaEmision().plusDays(10);

            return new ResultadoEmisionArca(proximoNumero, cae.trim(), fechaVtoCae, msgError);
        } catch (Exception e) {
            throw new RuntimeException("Error al solicitar CAE en WSFEv1 ARCA: " + e.getMessage(), e);
        }
    }

    private long consultarUltimoComprobanteAutorizadoWsfev1(
            EntornoArca entorno,
            String token,
            String sign,
            String cuitLimpio,
            int puntoVenta,
            int codigoAfipComprobante) throws Exception {

        String soapEnvelope = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ar="http://ar.gov.afip.dif.FEV1/">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <ar:FECompUltimoAutorizado>
                      <ar:Auth>
                        <ar:Token>%s</ar:Token>
                        <ar:Sign>%s</ar:Sign>
                        <ar:Cuit>%s</ar:Cuit>
                      </ar:Auth>
                      <ar:PtoVta>%d</ar:PtoVta>
                      <ar:CbteTipo>%d</ar:CbteTipo>
                    </ar:FECompUltimoAutorizado>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(token, sign, cuitLimpio, puntoVenta, codigoAfipComprobante).trim();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(entorno.getWsfev1Url()))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "http://ar.gov.afip.dif.FEV1/FECompUltimoAutorizado")
                .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Error al consultar FECompUltimoAutorizado en ARCA (HTTP " + response.statusCode() + ")");
        }

        String cbteNroStr = extraerValorTagXml(response.body(), "CbteNro");
        if (cbteNroStr == null || cbteNroStr.isBlank()) {
            String errMsg = extraerValorTagXml(response.body(), "Msg");
            throw new RuntimeException("No se pudo obtener el último comprobante autorizado en ARCA: "
                    + (errMsg != null ? errMsg : "Respuesta inválida"));
        }
        return Long.parseLong(cbteNroStr.trim());
    }

    /**
     * Modo Homologación Simulada Local (Regla 3.2.4): Permite probar el circuito completo de facturación,
     * Notas de Crédito, CAE, numeración correlativa y QR oficial RG 4892 sin requerir certificado tramitado.
     */
    public ResultadoEmisionArca simularEmisionHomologacion(long ultimoNumeroLocal, LocalDate fechaEmision) {
        long proximoNumero = ultimoNumeroLocal + 1;
        // CAE oficial de 14 dígitos numéricos (prefijo 74 + 12 dígitos)
        long sufijo = ThreadLocalRandom.current().nextLong(100_000_000_000L, 999_999_999_999L);
        String caeSimulado = "74" + sufijo;
        LocalDate vtoCae = fechaEmision.plusDays(10);
        return new ResultadoEmisionArca(
                proximoNumero,
                caeSimulado,
                vtoCae,
                "CAE de Homologación emitido mediante Simulador Local Asistido."
        );
    }

    /**
     * Construye el JSON y la URL oficial del Código QR de ARCA según RG 4892.
     */
    public Map<String, String> generarDatosQrOficialArca(
            LocalDate fechaEmision,
            String cuitEmisor,
            int puntoVenta,
            int codigoAfipComprobante,
            long numeroComprobante,
            BigDecimal importeTotal,
            int tipoDocReceptor,
            long nroDocReceptor,
            String cae) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("ver", 1);
            payload.put("fecha", fechaEmision.toString());
            payload.put("cuit", Long.parseLong(limpiarSoloDigitos(cuitEmisor)));
            payload.put("ptoVta", puntoVenta);
            payload.put("tipoCmp", codigoAfipComprobante);
            payload.put("nroCmp", numeroComprobante);
            payload.put("importe", importeTotal);
            payload.put("moneda", "PES");
            payload.put("ctz", 1);
            payload.put("tipoDocRec", tipoDocReceptor);
            payload.put("nroDocRec", nroDocReceptor);
            payload.put("tipoCodAut", "E");
            payload.put("codAut", Long.parseLong(limpiarSoloDigitos(cae)));

            String json = objectMapper.writeValueAsString(payload);
            String base64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
            String urlOficial = "https://www.afip.gob.ar/fe/qr/?p=" + base64;

            return Map.of(
                    "base64", base64,
                    "url", urlOficial
            );
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar el código QR oficial de ARCA: " + e.getMessage());
        }
    }

    public String limpiarSoloDigitos(String valor) {
        if (valor == null) {
            return "0";
        }
        String limpio = valor.replaceAll("[^0-9]", "");
        return limpio.isEmpty() ? "0" : limpio;
    }

    private String extraerValorTagXml(String xml, String tagLocalName) {
        if (xml == null || xml.isBlank()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList nodes = doc.getElementsByTagNameNS("*", tagLocalName);
            if (nodes.getLength() == 0) {
                nodes = doc.getElementsByTagName(tagLocalName);
            }
            if (nodes.getLength() > 0 && nodes.item(0) != null) {
                return nodes.item(0).getTextContent();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public ArcaWsClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

}
