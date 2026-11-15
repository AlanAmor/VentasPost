package com.ventaspost.service;

import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.common.exception.ResourceNotFoundException;
import com.ventaspost.domain.model.*;
import com.ventaspost.domain.repository.*;
import com.ventaspost.web.dto.arca.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ArcaFacturacionService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ConfiguracionArcaSucursalRepository configuracionArcaRepository;
    private final ComprobanteFiscalArcaRepository comprobanteArcaRepository;
    private final IntentoFacturacionArcaRepository intentoArcaRepository;
    private final SucursalRepository sucursalRepository;
    private final VentaRepository ventaRepository;
    private final ArcaWsClient arcaWsClient;

    @Transactional
    public List<ConfiguracionArcaResponseDto> listarConfiguracionesArca() {
        List<Sucursal> sucursales = sucursalRepository.findAll();
        for (Sucursal s : sucursales) {
            asegurarConfiguracionSucursal(s);
        }
        return configuracionArcaRepository.findAllByOrderBySucursalIdAsc().stream()
                .map(this::toConfigResponseDto)
                .toList();
    }

    @Transactional
    public ConfiguracionArcaResponseDto obtenerConfiguracionPorSucursal(Long sucursalId) {
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + sucursalId));
        ConfiguracionArcaSucursal config = asegurarConfiguracionSucursal(sucursal);
        return toConfigResponseDto(config);
    }

    @Transactional
    public ConfiguracionArcaResponseDto actualizarConfiguracionSucursal(
            Long sucursalId,
            ConfiguracionArcaRequestDto dto) {

        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + sucursalId));

        if (dto.condicionIvaEmisor() != CondicionIva.RESPONSABLE_INSCRIPTO
                && dto.condicionIvaEmisor() != CondicionIva.MONOTRIBUTO) {
            throw new BusinessException("La condición frente al IVA del comercio emisor debe ser RESPONSABLE_INSCRIPTO o MONOTRIBUTO.");
        }

        ConfiguracionArcaSucursal config = asegurarConfiguracionSucursal(sucursal);
        config.setRazonSocialEmisor(dto.razonSocialEmisor().trim());
        config.setCuitEmisor(dto.cuitEmisor().trim());
        config.setPuntoVentaArca(dto.puntoVentaArca());
        config.setCondicionIvaEmisor(dto.condicionIvaEmisor());
        config.setIngresosBrutos(limpiarTextoOpcional(dto.ingresosBrutos()));
        config.setFechaInicioActividades(dto.fechaInicioActividades());
        config.setDomicilioComercial(dto.domicilioComercial().trim());

        boolean cambioEntornoOCert = config.getEntornoArca() != dto.entornoArca();
        config.setEntornoArca(dto.entornoArca());

        // En Producción nunca se utiliza el simulador local
        if (dto.entornoArca() == EntornoArca.PRODUCCION) {
            config.setSimuladorHomologacionActivo(false);
        } else {
            config.setSimuladorHomologacionActivo(
                    dto.simuladorHomologacionActivo() == null || dto.simuladorHomologacionActivo()
            );
        }

        config.setMontoTopeConsumidorFinalSinDni(
                dto.montoTopeConsumidorFinalSinDni().setScale(2, RoundingMode.HALF_UP)
        );
        config.setPlazoMaximoDiasFacturacion(dto.plazoMaximoDiasFacturacion());

        // Si se enviaron nuevos contenidos de certificado .crt o clave .key, validarlos antes de guardar
        if (dto.certificadoCrtPem() != null && !dto.certificadoCrtPem().isBlank()) {
            try {
                arcaWsClient.cargarCertificadoX509(dto.certificadoCrtPem());
            } catch (Exception e) {
                throw new BusinessException("El archivo de Certificado Digital (.crt) no es un X.509 PEM válido: " + e.getMessage());
            }
            config.setCertificadoCrtPem(dto.certificadoCrtPem().trim());
            if (dto.nombreArchivoCrt() != null && !dto.nombreArchivoCrt().isBlank()) {
                config.setNombreArchivoCrt(dto.nombreArchivoCrt().trim());
            }
            cambioEntornoOCert = true;
        }

        if (dto.clavePrivadaKeyPem() != null && !dto.clavePrivadaKeyPem().isBlank()) {
            try {
                arcaWsClient.cargarClavePrivada(dto.clavePrivadaKeyPem());
            } catch (Exception e) {
                throw new BusinessException("El archivo de Clave Privada (.key) no es una clave RSA PEM válida: " + e.getMessage());
            }
            config.setClavePrivadaKeyPem(dto.clavePrivadaKeyPem().trim());
            if (dto.nombreArchivoKey() != null && !dto.nombreArchivoKey().isBlank()) {
                config.setNombreArchivoKey(dto.nombreArchivoKey().trim());
            }
            cambioEntornoOCert = true;
        }

        if (cambioEntornoOCert) {
            config.setWsaaTokenCache(null);
            config.setWsaaSignCache(null);
            config.setWsaaExpiracionCache(null);
        }

        // Mantener sincronizados el CUIT y Punto de Venta en la entidad Sucursal
        sucursal.setCuit(config.getCuitEmisor());
        sucursal.setPuntoVentaArca(config.getPuntoVentaArca());
        sucursalRepository.save(sucursal);

        ConfiguracionArcaSucursal guardada = configuracionArcaRepository.save(config);

        // Regla 1.2: Opción para copiar/compartir certificado y clave entre sucursales del mismo CUIT
        if (Boolean.TRUE.equals(dto.copiarCertificadosASucursalesMismoCuit())
                && guardada.poseeCertificadoYClaveCargados()) {
            String cuitBase = arcaWsClient.limpiarSoloDigitos(guardada.getCuitEmisor());
            List<ConfiguracionArcaSucursal> todas = configuracionArcaRepository.findAll();
            for (ConfiguracionArcaSucursal otra : todas) {
                if (!otra.getId().equals(guardada.getId())
                        && arcaWsClient.limpiarSoloDigitos(otra.getCuitEmisor()).equals(cuitBase)) {
                    otra.setCertificadoCrtPem(guardada.getCertificadoCrtPem());
                    otra.setClavePrivadaKeyPem(guardada.getClavePrivadaKeyPem());
                    otra.setNombreArchivoCrt(guardada.getNombreArchivoCrt());
                    otra.setNombreArchivoKey(guardada.getNombreArchivoKey());
                    otra.setWsaaTokenCache(null);
                    otra.setWsaaSignCache(null);
                    otra.setWsaaExpiracionCache(null);
                    configuracionArcaRepository.save(otra);
                }
            }
        }

        return toConfigResponseDto(guardada);
    }

    /**
     * Permite desde el Panel Admin probar la conexión WSAA o verificar el estado de Homologación.
     */
    @Transactional
    public Map<String, Object> probarConexionArca(Long sucursalId) {
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + sucursalId));
        ConfiguracionArcaSucursal config = asegurarConfiguracionSucursal(sucursal);

        if (config.getEntornoArca() == EntornoArca.HOMOLOGACION && config.isSimuladorHomologacionActivo()) {
            return Map.of(
                    "exitoso", true,
                    "modo", ModoEmisionArca.HOMOLOGACION_SIMULADA.name(),
                    "mensaje", "Simulador Local de Homologación ARCA activo y operativo para Punto de Venta "
                            + String.format("%05d", config.getPuntoVentaArca()) + " (" + config.getCondicionIvaEmisor().getDescripcion() + ")."
            );
        }

        ArcaWsClient.TicketAccesoWsaa ta = obtenerTicketAccesoWsaaConCache(config);
        return Map.of(
                "exitoso", true,
                "modo", ModoEmisionArca.WSFEV1_REAL.name(),
                "expiracionTa", ta.expiracion().toString(),
                "mensaje", "Autenticación exitosa contra WSAA (" + config.getEntornoArca().getDescripcion() + "). Ticket de Acceso vigente."
        );
    }

    /**
     * Reglas 1, 2 y 3: Factura una Venta realizada mediante ARCA (WSAA + WSFEv1 o Simulador de Homologación).
     * Persiste el intento fallido en caso de caída/rechazo de ARCA para permitir reintentos posteriores.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public ComprobanteFiscalArcaResponseDto facturarVenta(
            Long ventaId,
            FacturarVentaArcaRequestDto dto,
            String usuarioUsername) {

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + ventaId));

        if (venta.getEstadoVenta() == EstadoVenta.ANULADA) {
            throw new BusinessException("No se puede facturar una venta que se encuentra ANULADA.");
        }
        if (venta.getEstadoFacturacion() == EstadoFacturacionVenta.FACTURADA) {
            throw new BusinessException("Esta venta ya posee una Factura Electrónica ARCA autorizada con CAE.");
        }

        ConfiguracionArcaSucursal config = asegurarConfiguracionSucursal(venta.getSucursal());

        // Regla 3.1 A: Verificar plazo máximo interno configurado para facturar una venta ya realizada
        if (config.getPlazoMaximoDiasFacturacion() != null && config.getPlazoMaximoDiasFacturacion() > 0) {
            long diasTranscurridos = ChronoUnit.DAYS.between(venta.getFechaVenta().toLocalDate(), LocalDate.now());
            if (diasTranscurridos > config.getPlazoMaximoDiasFacturacion()) {
                throw new BusinessException(
                        "La venta supera el plazo máximo permitido para facturación posterior ("
                                + config.getPlazoMaximoDiasFacturacion() + " días). Días transcurridos: " + diasTranscurridos + "."
                );
            }
        }

        // Regla 2.1 A y 2.2: Validar datos del receptor y tope de Consumidor Final sin DNI
        CondicionIva receptorIva = dto.receptorCondicionIva() != null
                ? dto.receptorCondicionIva()
                : CondicionIva.CONSUMIDOR_FINAL;

        String docLimpio = arcaWsClient.limpiarSoloDigitos(dto.receptorDocumento());
        if ("0".equals(docLimpio)) {
            docLimpio = "";
        }

        TipoComprobanteArca tipoComprobante = TipoComprobanteArca.determinarFacturaSegunCondicion(
                config.getCondicionIvaEmisor(),
                receptorIva
        );

        int docTipoAfip;
        long docNroAfip;
        String docFormateado;
        String nombreReceptor = (dto.receptorNombreRazonSocial() != null && !dto.receptorNombreRazonSocial().isBlank())
                ? dto.receptorNombreRazonSocial().trim()
                : "Consumidor Final";

        if (receptorIva == CondicionIva.RESPONSABLE_INSCRIPTO
                || receptorIva == CondicionIva.MONOTRIBUTO
                || receptorIva == CondicionIva.EXENTO
                || tipoComprobante == TipoComprobanteArca.FACTURA_A) {
            if (docLimpio.length() != 11) {
                throw new BusinessException(
                        "Para facturar a un receptor '" + receptorIva.getDescripcion()
                                + "' es obligatorio ingresar un CUIT válido de 11 dígitos."
                );
            }
            docTipoAfip = 80; // CUIT
            docNroAfip = Long.parseLong(docLimpio);
            docFormateado = docLimpio;
            if ("Consumidor Final".equalsIgnoreCase(nombreReceptor)) {
                throw new BusinessException("Por favor ingrese la Razón Social o Nombre del receptor para emitir " + tipoComprobante.getDescripcion() + ".");
            }
        } else {
            // Consumidor Final
            BigDecimal topeCf = config.getMontoTopeConsumidorFinalSinDni();
            if (docLimpio.isEmpty()) {
                if (topeCf != null && venta.getTotalFinal().compareTo(topeCf) >= 0) {
                    throw new BusinessException(
                            "El total de la venta ($" + venta.getTotalFinal()
                                    + ") alcanza o supera el tope de ARCA para Consumidor Final sin identificar ($"
                                    + topeCf + "). Es obligatorio ingresar el DNI o CUIT y Nombre del comprador."
                    );
                }
                docTipoAfip = 99; // Sin identificar / Consumidor Final
                docNroAfip = 0L;
                docFormateado = "0";
            } else if (docLimpio.length() == 11) {
                docTipoAfip = 80; // CUIT
                docNroAfip = Long.parseLong(docLimpio);
                docFormateado = docLimpio;
            } else if (docLimpio.length() >= 7 && docLimpio.length() <= 8) {
                docTipoAfip = 96; // DNI
                docNroAfip = Long.parseLong(docLimpio);
                docFormateado = docLimpio;
            } else {
                throw new BusinessException("El documento ingresado debe ser un DNI (7 u 8 dígitos) o un CUIT (11 dígitos).");
            }
        }

        // Calcular desglose fiscal de IVA y Neto según tipo de comprobante
        DesgloseFiscalCalculado desglose = calcularDesgloseFiscalVenta(venta, tipoComprobante);
        LocalDate fechaEmision = LocalDate.now();

        ModoEmisionArca modoEmision = (config.getEntornoArca() == EntornoArca.HOMOLOGACION && config.isSimuladorHomologacionActivo())
                ? ModoEmisionArca.HOMOLOGACION_SIMULADA
                : ModoEmisionArca.WSFEV1_REAL;

        try {
            ArcaWsClient.ResultadoEmisionArca resultadoWs;
            if (modoEmision == ModoEmisionArca.HOMOLOGACION_SIMULADA) {
                long ultimoLocal = comprobanteArcaRepository.obtenerUltimoNumeroEmitidoLocal(
                        config.getCuitEmisor(),
                        config.getPuntoVentaArca(),
                        tipoComprobante.getCodigoAfip(),
                        config.getEntornoArca()
                );
                resultadoWs = arcaWsClient.simularEmisionHomologacion(ultimoLocal, fechaEmision);
            } else {
                ArcaWsClient.TicketAccesoWsaa ta = obtenerTicketAccesoWsaaConCache(config);
                ArcaWsClient.SolicitudCaeDatos solicitud = new ArcaWsClient.SolicitudCaeDatos(
                        tipoComprobante,
                        config.getPuntoVentaArca(),
                        fechaEmision,
                        docTipoAfip,
                        docNroAfip,
                        receptorIva,
                        desglose.importeTotal(),
                        desglose.importeNetoGravado(),
                        desglose.importeExento(),
                        desglose.importeIvaTotal(),
                        desglose.alicuotasAfip(),
                        null,
                        null,
                        null,
                        null
                );
                resultadoWs = arcaWsClient.emitirComprobanteWsfev1Real(config, ta.token(), ta.sign(), solicitud);
            }

            String numeroFormateado = String.format("%05d-%08d", config.getPuntoVentaArca(), resultadoWs.numeroComprobante());
            Map<String, String> qrDatos = arcaWsClient.generarDatosQrOficialArca(
                    fechaEmision,
                    config.getCuitEmisor(),
                    config.getPuntoVentaArca(),
                    tipoComprobante.getCodigoAfip(),
                    resultadoWs.numeroComprobante(),
                    desglose.importeTotal(),
                    docTipoAfip,
                    docNroAfip,
                    resultadoWs.cae()
            );

            ComprobanteFiscalArca comp = new ComprobanteFiscalArca();
            comp.setVenta(venta);
            comp.setSucursal(venta.getSucursal());
            comp.setComprobanteAsociado(null);
            comp.setNaturalezaComprobante("FACTURA");
            comp.setTipoComprobante(tipoComprobante);
            comp.setCodigoAfipComprobante(tipoComprobante.getCodigoAfip());
            comp.setLetraComprobante(tipoComprobante.getLetra());
            comp.setEntornoArca(config.getEntornoArca());
            comp.setModoEmision(modoEmision);
            comp.setCuitEmisor(config.getCuitEmisor());
            comp.setRazonSocialEmisor(config.getRazonSocialEmisor());
            comp.setCondicionIvaEmisor(config.getCondicionIvaEmisor());
            comp.setIngresosBrutosEmisor(config.getIngresosBrutos());
            comp.setFechaInicioActividadesEmisor(config.getFechaInicioActividades());
            comp.setDomicilioComercialEmisor(config.getDomicilioComercial());
            comp.setPuntoVenta(config.getPuntoVentaArca());
            comp.setNumeroComprobante(resultadoWs.numeroComprobante());
            comp.setNumeroFormateado(numeroFormateado);
            comp.setFechaEmision(fechaEmision);
            comp.setReceptorCondicionIva(receptorIva);
            comp.setReceptorCodigoAfipIva(receptorIva.getCodigoAfipReceptor());
            comp.setReceptorTipoDocAfip(docTipoAfip);
            comp.setReceptorNumeroDoc(docFormateado);
            comp.setReceptorNombreRazonSocial(nombreReceptor);
            comp.setReceptorDomicilio(limpiarTextoOpcional(dto.receptorDomicilio()));
            comp.setImporteNetoGravado(desglose.importeNetoGravado());
            comp.setImporteExento(desglose.importeExento());
            comp.setImporteIvaTotal(desglose.importeIvaTotal());
            comp.setImporteIva21(desglose.iva21());
            comp.setImporteIva105(desglose.iva105());
            comp.setImporteIva27(desglose.iva27());
            comp.setImporteIvaContenidoTransparencia(desglose.ivaContenidoTransparencia());
            comp.setImporteTotal(desglose.importeTotal());
            comp.setCae(resultadoWs.cae());
            comp.setCaeFechaVencimiento(resultadoWs.caeFechaVencimiento());
            comp.setQrUrlOficialArca(qrDatos.get("url"));
            comp.setQrBase64Payload(qrDatos.get("base64"));
            comp.setObservacionesArca(resultadoWs.observaciones());
            comp.setUsuarioEmisorUsername(usuarioUsername);
            comp.setActivo(true);

            ComprobanteFiscalArca guardado = comprobanteArcaRepository.save(comp);

            // Actualizar datos del cliente y estado de facturación en la Venta
            venta.setClienteNombre(nombreReceptor);
            venta.setClienteDocumento("0".equals(docFormateado) ? null : docFormateado);
            venta.setClienteCondicionIva(receptorIva);
            venta.setEstadoFacturacion(EstadoFacturacionVenta.FACTURADA);
            venta.setUltimoErrorArca(null);
            venta.setCantidadIntentosArca((venta.getCantidadIntentosArca() != null ? venta.getCantidadIntentosArca() : 0) + 1);
            ventaRepository.save(venta);

            registrarIntentoArca(
                    venta,
                    "EMISION_FACTURA",
                    config.getEntornoArca(),
                    modoEmision,
                    true,
                    "APROBADO",
                    "Comprobante " + tipoComprobante.getDescripcion() + " " + numeroFormateado + " emitido con CAE " + resultadoWs.cae(),
                    resultadoWs.cae(),
                    numeroFormateado,
                    usuarioUsername
            );

            return toComprobanteResponseDto(guardado);
        } catch (Exception ex) {
            String detalleError = ex.getMessage() != null ? ex.getMessage() : "Error de comunicación con los servidores de ARCA";
            if (detalleError.length() > 480) {
                detalleError = detalleError.substring(0, 480);
            }

            venta.setEstadoFacturacion(EstadoFacturacionVenta.ERROR_ARCA);
            venta.setUltimoErrorArca(detalleError);
            venta.setCantidadIntentosArca((venta.getCantidadIntentosArca() != null ? venta.getCantidadIntentosArca() : 0) + 1);
            ventaRepository.save(venta);

            registrarIntentoArca(
                    venta,
                    "EMISION_FACTURA",
                    config.getEntornoArca(),
                    modoEmision,
                    false,
                    "ERROR_ARCA",
                    detalleError,
                    null,
                    null,
                    usuarioUsername
            );

            throw new BusinessException("No se pudo obtener el CAE en ARCA (puede reintentar desde el historial): " + detalleError);
        }
    }

    /**
     * Regla 4.1 A: Al anular una venta que ya estaba FACTURADA en ARCA, emite automáticamente
     * la Nota de Crédito Electrónica correspondiente (NC A, NC B o NC C) vinculada a la factura original.
     */
    @Transactional
    public ComprobanteFiscalArcaResponseDto emitirNotaCreditoPorAnulacion(Venta venta, String usuarioUsername) {
        ComprobanteFiscalArca facturaOriginal = comprobanteArcaRepository
                .findFirstByVentaIdAndNaturalezaComprobanteOrderByIdDesc(venta.getId(), "FACTURA")
                .orElseThrow(() -> new BusinessException("No se encontró la Factura original ARCA para emitir la Nota de Crédito."));

        ConfiguracionArcaSucursal config = asegurarConfiguracionSucursal(venta.getSucursal());
        TipoComprobanteArca tipoNc = facturaOriginal.getTipoComprobante().obtenerNotaCreditoAsociada();
        LocalDate fechaEmision = LocalDate.now();

        ModoEmisionArca modoEmision = (config.getEntornoArca() == EntornoArca.HOMOLOGACION && config.isSimuladorHomologacionActivo())
                ? ModoEmisionArca.HOMOLOGACION_SIMULADA
                : ModoEmisionArca.WSFEV1_REAL;

        DesgloseFiscalCalculado desglose = calcularDesgloseFiscalVenta(venta, tipoNc);
        long docNroAfip = Long.parseLong(arcaWsClient.limpiarSoloDigitos(facturaOriginal.getReceptorNumeroDoc()));

        ArcaWsClient.ResultadoEmisionArca resultadoWs;
        if (modoEmision == ModoEmisionArca.HOMOLOGACION_SIMULADA) {
            long ultimoLocal = comprobanteArcaRepository.obtenerUltimoNumeroEmitidoLocal(
                    config.getCuitEmisor(),
                    config.getPuntoVentaArca(),
                    tipoNc.getCodigoAfip(),
                    config.getEntornoArca()
            );
            resultadoWs = arcaWsClient.simularEmisionHomologacion(ultimoLocal, fechaEmision);
        } else {
            ArcaWsClient.TicketAccesoWsaa ta = obtenerTicketAccesoWsaaConCache(config);
            ArcaWsClient.SolicitudCaeDatos solicitudNc = new ArcaWsClient.SolicitudCaeDatos(
                    tipoNc,
                    config.getPuntoVentaArca(),
                    fechaEmision,
                    facturaOriginal.getReceptorTipoDocAfip(),
                    docNroAfip,
                    facturaOriginal.getReceptorCondicionIva(),
                    desglose.importeTotal(),
                    desglose.importeNetoGravado(),
                    desglose.importeExento(),
                    desglose.importeIvaTotal(),
                    desglose.alicuotasAfip(),
                    facturaOriginal.getCodigoAfipComprobante(),
                    facturaOriginal.getPuntoVenta(),
                    facturaOriginal.getNumeroComprobante(),
                    facturaOriginal.getFechaEmision()
            );
            resultadoWs = arcaWsClient.emitirComprobanteWsfev1Real(config, ta.token(), ta.sign(), solicitudNc);
        }

        String numeroFormateado = String.format("%05d-%08d", config.getPuntoVentaArca(), resultadoWs.numeroComprobante());
        Map<String, String> qrDatos = arcaWsClient.generarDatosQrOficialArca(
                fechaEmision,
                config.getCuitEmisor(),
                config.getPuntoVentaArca(),
                tipoNc.getCodigoAfip(),
                resultadoWs.numeroComprobante(),
                desglose.importeTotal(),
                facturaOriginal.getReceptorTipoDocAfip(),
                docNroAfip,
                resultadoWs.cae()
        );

        ComprobanteFiscalArca nc = new ComprobanteFiscalArca();
        nc.setVenta(venta);
        nc.setSucursal(venta.getSucursal());
        nc.setComprobanteAsociado(facturaOriginal);
        nc.setNaturalezaComprobante("NOTA_CREDITO");
        nc.setTipoComprobante(tipoNc);
        nc.setCodigoAfipComprobante(tipoNc.getCodigoAfip());
        nc.setLetraComprobante(tipoNc.getLetra());
        nc.setEntornoArca(config.getEntornoArca());
        nc.setModoEmision(modoEmision);
        nc.setCuitEmisor(config.getCuitEmisor());
        nc.setRazonSocialEmisor(config.getRazonSocialEmisor());
        nc.setCondicionIvaEmisor(config.getCondicionIvaEmisor());
        nc.setIngresosBrutosEmisor(config.getIngresosBrutos());
        nc.setFechaInicioActividadesEmisor(config.getFechaInicioActividades());
        nc.setDomicilioComercialEmisor(config.getDomicilioComercial());
        nc.setPuntoVenta(config.getPuntoVentaArca());
        nc.setNumeroComprobante(resultadoWs.numeroComprobante());
        nc.setNumeroFormateado(numeroFormateado);
        nc.setFechaEmision(fechaEmision);
        nc.setReceptorCondicionIva(facturaOriginal.getReceptorCondicionIva());
        nc.setReceptorCodigoAfipIva(facturaOriginal.getReceptorCodigoAfipIva());
        nc.setReceptorTipoDocAfip(facturaOriginal.getReceptorTipoDocAfip());
        nc.setReceptorNumeroDoc(facturaOriginal.getReceptorNumeroDoc());
        nc.setReceptorNombreRazonSocial(facturaOriginal.getReceptorNombreRazonSocial());
        nc.setReceptorDomicilio(facturaOriginal.getReceptorDomicilio());
        nc.setImporteNetoGravado(desglose.importeNetoGravado());
        nc.setImporteExento(desglose.importeExento());
        nc.setImporteIvaTotal(desglose.importeIvaTotal());
        nc.setImporteIva21(desglose.iva21());
        nc.setImporteIva105(desglose.iva105());
        nc.setImporteIva27(desglose.iva27());
        nc.setImporteIvaContenidoTransparencia(desglose.ivaContenidoTransparencia());
        nc.setImporteTotal(desglose.importeTotal());
        nc.setCae(resultadoWs.cae());
        nc.setCaeFechaVencimiento(resultadoWs.caeFechaVencimiento());
        nc.setQrUrlOficialArca(qrDatos.get("url"));
        nc.setQrBase64Payload(qrDatos.get("base64"));
        nc.setObservacionesArca("Nota de Crédito asociada a " + facturaOriginal.getTipoComprobante().getDescripcion() + " " + facturaOriginal.getNumeroFormateado());
        nc.setUsuarioEmisorUsername(usuarioUsername);
        nc.setActivo(true);

        ComprobanteFiscalArca guardada = comprobanteArcaRepository.save(nc);

        venta.setEstadoFacturacion(EstadoFacturacionVenta.ANULADA_NOTA_CREDITO);
        venta.setUltimoErrorArca(null);
        ventaRepository.save(venta);

        registrarIntentoArca(
                venta,
                "EMISION_NOTA_CREDITO",
                config.getEntornoArca(),
                modoEmision,
                true,
                "APROBADO",
                "Nota de Crédito " + tipoNc.getDescripcion() + " " + numeroFormateado + " emitida con CAE " + resultadoWs.cae(),
                resultadoWs.cae(),
                numeroFormateado,
                usuarioUsername
        );

        return toComprobanteResponseDto(guardada);
    }

    @Transactional(readOnly = true)
    public List<ComprobanteFiscalArcaResponseDto> listarComprobantesPorVenta(Long ventaId) {
        return comprobanteArcaRepository.findByVentaIdOrderByIdAsc(ventaId).stream()
                .map(this::toComprobanteResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComprobanteFiscalArcaResponseDto obtenerComprobantePorId(Long comprobanteId) {
        ComprobanteFiscalArca c = comprobanteArcaRepository.findById(comprobanteId)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante fiscal ARCA no encontrado: " + comprobanteId));
        return toComprobanteResponseDto(c);
    }

    @Transactional(readOnly = true)
    public List<IntentoFacturacionArcaResponseDto> listarIntentosPorVenta(Long ventaId) {
        return intentoArcaRepository.findByVentaIdOrderByFechaIntentoDesc(ventaId).stream()
                .map(i -> new IntentoFacturacionArcaResponseDto(
                        i.getId(),
                        i.getVenta().getId(),
                        i.getSucursal().getId(),
                        i.getFechaIntento(),
                        i.getNaturalezaOperacion(),
                        i.getEntornoArca(),
                        i.getModoEmision(),
                        i.isExitoso(),
                        i.getCodigoRespuesta(),
                        i.getMensajeDetalle(),
                        i.getCaeObtenido(),
                        i.getNumeroComprobanteObtenido(),
                        i.getUsuarioUsername()
                ))
                .toList();
    }

    private ArcaWsClient.TicketAccesoWsaa obtenerTicketAccesoWsaaConCache(ConfiguracionArcaSucursal config) {
        if (config.poseeTicketAccesoWsaaVigente()) {
            return new ArcaWsClient.TicketAccesoWsaa(
                    config.getWsaaTokenCache(),
                    config.getWsaaSignCache(),
                    config.getWsaaExpiracionCache()
            );
        }
        ArcaWsClient.TicketAccesoWsaa nuevoTa = arcaWsClient.autenticarWsaa(config);
        config.setWsaaTokenCache(nuevoTa.token());
        config.setWsaaSignCache(nuevoTa.sign());
        config.setWsaaExpiracionCache(nuevoTa.expiracion());
        configuracionArcaRepository.save(config);
        return nuevoTa;
    }

    private ConfiguracionArcaSucursal asegurarConfiguracionSucursal(Sucursal sucursal) {
        return configuracionArcaRepository.findBySucursalId(sucursal.getId())
                .orElseGet(() -> {
                    ConfiguracionArcaSucursal nueva = new ConfiguracionArcaSucursal();
                    nueva.setSucursal(sucursal);
                    nueva.setRazonSocialEmisor("VENTASPOST POLIRUBRO - " + sucursal.getNombre());
                    nueva.setCuitEmisor(sucursal.getCuit());
                    nueva.setPuntoVentaArca(sucursal.getPuntoVentaArca());
                    nueva.setCondicionIvaEmisor(CondicionIva.RESPONSABLE_INSCRIPTO);
                    nueva.setIngresosBrutos("901-000000-1");
                    nueva.setFechaInicioActividades(LocalDate.of(2024, 1, 1));
                    nueva.setDomicilioComercial(sucursal.getDireccion());
                    nueva.setEntornoArca(EntornoArca.HOMOLOGACION);
                    nueva.setSimuladorHomologacionActivo(true);
                    nueva.setMontoTopeConsumidorFinalSinDni(new BigDecimal("344488.00"));
                    nueva.setPlazoMaximoDiasFacturacion(30);
                    nueva.setActivo(true);
                    return configuracionArcaRepository.save(nueva);
                });
    }

    private record DesgloseFiscalCalculado(
            BigDecimal importeNetoGravado,
            BigDecimal importeExento,
            BigDecimal importeIvaTotal,
            BigDecimal iva21,
            BigDecimal iva105,
            BigDecimal iva27,
            BigDecimal ivaContenidoTransparencia,
            BigDecimal importeTotal,
            List<ArcaWsClient.AlicuotaIvaDetalle> alicuotasAfip
    ) {}

    private DesgloseFiscalCalculado calcularDesgloseFiscalVenta(Venta venta, TipoComprobanteArca tipoComprobante) {
        BigDecimal totalFinal = venta.getTotalFinal().setScale(2, RoundingMode.HALF_UP);

        // En Factura C o Nota de Crédito C (Monotributista), no se discrimina IVA
        if (tipoComprobante == TipoComprobanteArca.FACTURA_C
                || tipoComprobante == TipoComprobanteArca.NOTA_CREDITO_C) {
            return new DesgloseFiscalCalculado(
                    totalFinal,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    totalFinal,
                    List.of()
            );
        }

        // Emisor Responsable Inscripto (Factura A/B o NC A/B):
        // Prorratear factor de ajuste (descuento global + recargo/descuento medio de pago) sobre cada ítem
        BigDecimal subtotalItems = venta.getSubtotalItems();
        BigDecimal factorGlobal = (subtotalItems != null && subtotalItems.compareTo(BigDecimal.ZERO) > 0)
                ? totalFinal.divide(subtotalItems, 6, RoundingMode.HALF_UP)
                : BigDecimal.ONE;

        BigDecimal base21 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva21 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal base105 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva105 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal base27 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva27 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal exento = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (VentaItem item : venta.getItems()) {
            BigDecimal finalItem = item.getSubtotalNetoItem().multiply(factorGlobal).setScale(2, RoundingMode.HALF_UP);
            BigDecimal alicuota = item.getAlicuotaIva() != null ? item.getAlicuotaIva() : new BigDecimal("21.00");

            if (alicuota.compareTo(BigDecimal.ZERO) <= 0) {
                exento = exento.add(finalItem);
            } else {
                BigDecimal divisor = BigDecimal.ONE.add(alicuota.divide(CIEN, 6, RoundingMode.HALF_UP));
                BigDecimal netoItem = finalItem.divide(divisor, 2, RoundingMode.HALF_UP);
                BigDecimal ivaItem = finalItem.subtract(netoItem).setScale(2, RoundingMode.HALF_UP);

                if (alicuota.compareTo(new BigDecimal("10.50")) == 0) {
                    base105 = base105.add(netoItem);
                    iva105 = iva105.add(ivaItem);
                } else if (alicuota.compareTo(new BigDecimal("27.00")) == 0) {
                    base27 = base27.add(netoItem);
                    iva27 = iva27.add(ivaItem);
                } else {
                    base21 = base21.add(netoItem);
                    iva21 = iva21.add(ivaItem);
                }
            }
        }

        BigDecimal netoGravadoTotal = base21.add(base105).add(base27).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ivaTotal = iva21.add(iva105).add(iva27).setScale(2, RoundingMode.HALF_UP);

        // Ajuste de centavos por redondeo para que Neto + Exento + IVA coincida exactamente con TotalFinal
        BigDecimal diferenciaCentavos = totalFinal.subtract(netoGravadoTotal.add(exento).add(ivaTotal));
        if (diferenciaCentavos.compareTo(BigDecimal.ZERO) != 0) {
            if (base21.compareTo(BigDecimal.ZERO) > 0) {
                base21 = base21.add(diferenciaCentavos);
                netoGravadoTotal = netoGravadoTotal.add(diferenciaCentavos);
            } else if (base105.compareTo(BigDecimal.ZERO) > 0) {
                base105 = base105.add(diferenciaCentavos);
                netoGravadoTotal = netoGravadoTotal.add(diferenciaCentavos);
            } else if (base27.compareTo(BigDecimal.ZERO) > 0) {
                base27 = base27.add(diferenciaCentavos);
                netoGravadoTotal = netoGravadoTotal.add(diferenciaCentavos);
            } else {
                exento = exento.add(diferenciaCentavos);
            }
        }

        List<ArcaWsClient.AlicuotaIvaDetalle> alicuotasAfip = new ArrayList<>();
        if (base21.compareTo(BigDecimal.ZERO) > 0) {
            alicuotasAfip.add(new ArcaWsClient.AlicuotaIvaDetalle(5, base21, iva21)); // 5 = 21%
        }
        if (base105.compareTo(BigDecimal.ZERO) > 0) {
            alicuotasAfip.add(new ArcaWsClient.AlicuotaIvaDetalle(4, base105, iva105)); // 4 = 10.5%
        }
        if (base27.compareTo(BigDecimal.ZERO) > 0) {
            alicuotasAfip.add(new ArcaWsClient.AlicuotaIvaDetalle(6, base27, iva27)); // 6 = 27%
        }

        return new DesgloseFiscalCalculado(
                netoGravadoTotal,
                exento,
                ivaTotal,
                iva21,
                iva105,
                iva27,
                ivaTotal,
                totalFinal,
                alicuotasAfip
        );
    }

    private void registrarIntentoArca(
            Venta venta,
            String naturalezaOperacion,
            EntornoArca entorno,
            ModoEmisionArca modo,
            boolean exitoso,
            String codigoRespuesta,
            String mensajeDetalle,
            String caeObtenido,
            String numeroComprobanteObtenido,
            String usuarioUsername) {

        IntentoFacturacionArca intento = new IntentoFacturacionArca();
        intento.setVenta(venta);
        intento.setSucursal(venta.getSucursal());
        intento.setFechaIntento(LocalDateTime.now());
        intento.setNaturalezaOperacion(naturalezaOperacion);
        intento.setEntornoArca(entorno);
        intento.setModoEmision(modo);
        intento.setExitoso(exitoso);
        intento.setCodigoRespuesta(codigoRespuesta);
        intento.setMensajeDetalle(mensajeDetalle);
        intento.setCaeObtenido(caeObtenido);
        intento.setNumeroComprobanteObtenido(numeroComprobanteObtenido);
        intento.setUsuarioUsername(usuarioUsername);
        intentoArcaRepository.save(intento);
    }

    private ConfiguracionArcaResponseDto toConfigResponseDto(ConfiguracionArcaSucursal c) {
        ArcaWsClient.ResumenCertificadoX509 resumenCert = arcaWsClient.parsearResumenCertificado(c.getCertificadoCrtPem());
        return new ConfiguracionArcaResponseDto(
                c.getId(),
                c.getSucursal().getId(),
                c.getSucursal().getCodigo(),
                c.getSucursal().getNombre(),
                c.getRazonSocialEmisor(),
                c.getCuitEmisor(),
                c.getPuntoVentaArca(),
                c.getCondicionIvaEmisor(),
                c.getCondicionIvaEmisor().getDescripcion(),
                c.getIngresosBrutos(),
                c.getFechaInicioActividades(),
                c.getDomicilioComercial(),
                c.getEntornoArca(),
                c.getEntornoArca().getDescripcion(),
                c.isSimuladorHomologacionActivo(),
                c.getCertificadoCrtPem() != null && !c.getCertificadoCrtPem().isBlank(),
                c.getClavePrivadaKeyPem() != null && !c.getClavePrivadaKeyPem().isBlank(),
                c.getNombreArchivoCrt(),
                c.getNombreArchivoKey(),
                resumenCert.subject(),
                resumenCert.fechaVencimiento(),
                c.getMontoTopeConsumidorFinalSinDni(),
                c.getPlazoMaximoDiasFacturacion(),
                c.poseeTicketAccesoWsaaVigente(),
                c.getWsaaExpiracionCache(),
                c.isActivo()
        );
    }

    public ComprobanteFiscalArcaResponseDto toComprobanteResponseDto(ComprobanteFiscalArca c) {
        String tipoDocDesc = switch (c.getReceptorTipoDocAfip()) {
            case 80 -> "CUIT";
            case 96 -> "DNI";
            default -> "Consumidor Final (S/I)";
        };
        return new ComprobanteFiscalArcaResponseDto(
                c.getId(),
                c.getVenta().getId(),
                c.getVenta().getNumeroComprobanteInterno(),
                c.getSucursal().getId(),
                c.getSucursal().getNombre(),
                c.getComprobanteAsociado() != null ? c.getComprobanteAsociado().getId() : null,
                c.getComprobanteAsociado() != null ? c.getComprobanteAsociado().getNumeroFormateado() : null,
                c.getNaturalezaComprobante(),
                c.getTipoComprobante(),
                c.getTipoComprobante().getDescripcion(),
                c.getCodigoAfipComprobante(),
                c.getLetraComprobante(),
                c.getEntornoArca(),
                c.getEntornoArca().getDescripcion(),
                c.getModoEmision(),
                c.getModoEmision().getDescripcion(),
                c.getCuitEmisor(),
                c.getRazonSocialEmisor(),
                c.getCondicionIvaEmisor(),
                c.getCondicionIvaEmisor().getDescripcion(),
                c.getIngresosBrutosEmisor(),
                c.getFechaInicioActividadesEmisor(),
                c.getDomicilioComercialEmisor(),
                c.getPuntoVenta(),
                c.getNumeroComprobante(),
                c.getNumeroFormateado(),
                c.getFechaEmision(),
                c.getReceptorCondicionIva(),
                c.getReceptorCondicionIva().getDescripcion(),
                c.getReceptorCodigoAfipIva(),
                c.getReceptorTipoDocAfip(),
                tipoDocDesc,
                c.getReceptorNumeroDoc(),
                c.getReceptorNombreRazonSocial(),
                c.getReceptorDomicilio(),
                c.getImporteNetoGravado(),
                c.getImporteExento(),
                c.getImporteIvaTotal(),
                c.getImporteIva21(),
                c.getImporteIva105(),
                c.getImporteIva27(),
                c.getImporteIvaContenidoTransparencia(),
                c.getImporteTotal(),
                c.getCae(),
                c.getCaeFechaVencimiento(),
                c.getQrUrlOficialArca(),
                c.getQrBase64Payload(),
                c.getObservacionesArca(),
                c.getUsuarioEmisorUsername(),
                c.getCreatedAt()
        );
    }

    private String limpiarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    public ArcaFacturacionService(ConfiguracionArcaSucursalRepository configuracionArcaRepository, ComprobanteFiscalArcaRepository comprobanteArcaRepository, IntentoFacturacionArcaRepository intentoArcaRepository, SucursalRepository sucursalRepository, VentaRepository ventaRepository, ArcaWsClient arcaWsClient) {
        this.configuracionArcaRepository = configuracionArcaRepository;
        this.comprobanteArcaRepository = comprobanteArcaRepository;
        this.intentoArcaRepository = intentoArcaRepository;
        this.sucursalRepository = sucursalRepository;
        this.ventaRepository = ventaRepository;
        this.arcaWsClient = arcaWsClient;
    }

}
