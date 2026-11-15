package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "comprobantes_fiscales_arca",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_comp_arca_pv_tipo_nro",
                        columnNames = {"cuit_emisor", "punto_venta", "codigo_afip_comprobante", "numero_comprobante", "entorno_arca"}
                )
        }
)
public class ComprobanteFiscalArca extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "comprobante_asociado_id")
    private ComprobanteFiscalArca comprobanteAsociado;

    @Column(name = "naturaleza_comprobante", nullable = false, length = 30)
    private String naturalezaComprobante;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 35)
    private TipoComprobanteArca tipoComprobante;

    @Column(name = "codigo_afip_comprobante", nullable = false)
    private Integer codigoAfipComprobante;

    @Column(name = "letra_comprobante", nullable = false, length = 5)
    private String letraComprobante;

    @Enumerated(EnumType.STRING)
    @Column(name = "entorno_arca", nullable = false, length = 25)
    private EntornoArca entornoArca;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_emision", nullable = false, length = 30)
    private ModoEmisionArca modoEmision;

    @Column(name = "cuit_emisor", nullable = false, length = 13)
    private String cuitEmisor;

    @Column(name = "razon_social_emisor", nullable = false, length = 160)
    private String razonSocialEmisor;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_iva_emisor", nullable = false, length = 40)
    private CondicionIva condicionIvaEmisor;

    @Column(name = "ingresos_brutos_emisor", length = 50)
    private String ingresosBrutosEmisor;

    @Column(name = "fecha_inicio_actividades_emisor")
    private LocalDate fechaInicioActividadesEmisor;

    @Column(name = "domicilio_comercial_emisor", nullable = false, length = 220)
    private String domicilioComercialEmisor;

    @Column(name = "punto_venta", nullable = false)
    private Integer puntoVenta;

    @Column(name = "numero_comprobante", nullable = false)
    private Long numeroComprobante;

    @Column(name = "numero_formateado", nullable = false, length = 25)
    private String numeroFormateado;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "receptor_condicion_iva", nullable = false, length = 40)
    private CondicionIva receptorCondicionIva;

    @Column(name = "receptor_codigo_afip_iva", nullable = false)
    private Integer receptorCodigoAfipIva;

    @Column(name = "receptor_tipo_doc_afip", nullable = false)
    private Integer receptorTipoDocAfip;

    @Column(name = "receptor_numero_doc", nullable = false, length = 20)
    private String receptorNumeroDoc;

    @Column(name = "receptor_nombre_razon_social", nullable = false, length = 160)
    private String receptorNombreRazonSocial;

    @Column(name = "receptor_domicilio", length = 220)
    private String receptorDomicilio;

    @Column(name = "importe_neto_gravado", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeNetoGravado = BigDecimal.ZERO;

    @Column(name = "importe_exento", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeExento = BigDecimal.ZERO;

    @Column(name = "importe_iva_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeIvaTotal = BigDecimal.ZERO;

    @Column(name = "importe_iva_21", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeIva21 = BigDecimal.ZERO;

    @Column(name = "importe_iva_105", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeIva105 = BigDecimal.ZERO;

    @Column(name = "importe_iva_27", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeIva27 = BigDecimal.ZERO;

    @Column(name = "importe_iva_contenido_transparencia", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeIvaContenidoTransparencia = BigDecimal.ZERO;

    @Column(name = "importe_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeTotal = BigDecimal.ZERO;

    @Column(name = "cae", nullable = false, length = 30)
    private String cae;

    @Column(name = "cae_fecha_vencimiento", nullable = false)
    private LocalDate caeFechaVencimiento;

    @Column(name = "qr_url_oficial_arca", nullable = false, length = 1200)
    private String qrUrlOficialArca;

    @Column(name = "qr_base64_payload", nullable = false, length = 900)
    private String qrBase64Payload;

    @Column(name = "observaciones_arca", length = 500)
    private String observacionesArca;

    @Column(name = "usuario_emisor_username", nullable = false, length = 60)
    private String usuarioEmisorUsername;

    public ComprobanteFiscalArca() {
    }

    public Long getId() {
        return this.id;
    }

    public Venta getVenta() {
        return this.venta;
    }

    public Sucursal getSucursal() {
        return this.sucursal;
    }

    public ComprobanteFiscalArca getComprobanteAsociado() {
        return this.comprobanteAsociado;
    }

    public String getNaturalezaComprobante() {
        return this.naturalezaComprobante;
    }

    public TipoComprobanteArca getTipoComprobante() {
        return this.tipoComprobante;
    }

    public Integer getCodigoAfipComprobante() {
        return this.codigoAfipComprobante;
    }

    public String getLetraComprobante() {
        return this.letraComprobante;
    }

    public EntornoArca getEntornoArca() {
        return this.entornoArca;
    }

    public ModoEmisionArca getModoEmision() {
        return this.modoEmision;
    }

    public String getCuitEmisor() {
        return this.cuitEmisor;
    }

    public String getRazonSocialEmisor() {
        return this.razonSocialEmisor;
    }

    public CondicionIva getCondicionIvaEmisor() {
        return this.condicionIvaEmisor;
    }

    public String getIngresosBrutosEmisor() {
        return this.ingresosBrutosEmisor;
    }

    public LocalDate getFechaInicioActividadesEmisor() {
        return this.fechaInicioActividadesEmisor;
    }

    public String getDomicilioComercialEmisor() {
        return this.domicilioComercialEmisor;
    }

    public Integer getPuntoVenta() {
        return this.puntoVenta;
    }

    public Long getNumeroComprobante() {
        return this.numeroComprobante;
    }

    public String getNumeroFormateado() {
        return this.numeroFormateado;
    }

    public LocalDate getFechaEmision() {
        return this.fechaEmision;
    }

    public CondicionIva getReceptorCondicionIva() {
        return this.receptorCondicionIva;
    }

    public Integer getReceptorCodigoAfipIva() {
        return this.receptorCodigoAfipIva;
    }

    public Integer getReceptorTipoDocAfip() {
        return this.receptorTipoDocAfip;
    }

    public String getReceptorNumeroDoc() {
        return this.receptorNumeroDoc;
    }

    public String getReceptorNombreRazonSocial() {
        return this.receptorNombreRazonSocial;
    }

    public String getReceptorDomicilio() {
        return this.receptorDomicilio;
    }

    public BigDecimal getImporteNetoGravado() {
        return this.importeNetoGravado;
    }

    public BigDecimal getImporteExento() {
        return this.importeExento;
    }

    public BigDecimal getImporteIvaTotal() {
        return this.importeIvaTotal;
    }

    public BigDecimal getImporteIva21() {
        return this.importeIva21;
    }

    public BigDecimal getImporteIva105() {
        return this.importeIva105;
    }

    public BigDecimal getImporteIva27() {
        return this.importeIva27;
    }

    public BigDecimal getImporteIvaContenidoTransparencia() {
        return this.importeIvaContenidoTransparencia;
    }

    public BigDecimal getImporteTotal() {
        return this.importeTotal;
    }

    public String getCae() {
        return this.cae;
    }

    public LocalDate getCaeFechaVencimiento() {
        return this.caeFechaVencimiento;
    }

    public String getQrUrlOficialArca() {
        return this.qrUrlOficialArca;
    }

    public String getQrBase64Payload() {
        return this.qrBase64Payload;
    }

    public String getObservacionesArca() {
        return this.observacionesArca;
    }

    public String getUsuarioEmisorUsername() {
        return this.usuarioEmisorUsername;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public void setComprobanteAsociado(ComprobanteFiscalArca comprobanteAsociado) {
        this.comprobanteAsociado = comprobanteAsociado;
    }

    public void setNaturalezaComprobante(String naturalezaComprobante) {
        this.naturalezaComprobante = naturalezaComprobante;
    }

    public void setTipoComprobante(TipoComprobanteArca tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public void setCodigoAfipComprobante(Integer codigoAfipComprobante) {
        this.codigoAfipComprobante = codigoAfipComprobante;
    }

    public void setLetraComprobante(String letraComprobante) {
        this.letraComprobante = letraComprobante;
    }

    public void setEntornoArca(EntornoArca entornoArca) {
        this.entornoArca = entornoArca;
    }

    public void setModoEmision(ModoEmisionArca modoEmision) {
        this.modoEmision = modoEmision;
    }

    public void setCuitEmisor(String cuitEmisor) {
        this.cuitEmisor = cuitEmisor;
    }

    public void setRazonSocialEmisor(String razonSocialEmisor) {
        this.razonSocialEmisor = razonSocialEmisor;
    }

    public void setCondicionIvaEmisor(CondicionIva condicionIvaEmisor) {
        this.condicionIvaEmisor = condicionIvaEmisor;
    }

    public void setIngresosBrutosEmisor(String ingresosBrutosEmisor) {
        this.ingresosBrutosEmisor = ingresosBrutosEmisor;
    }

    public void setFechaInicioActividadesEmisor(LocalDate fechaInicioActividadesEmisor) {
        this.fechaInicioActividadesEmisor = fechaInicioActividadesEmisor;
    }

    public void setDomicilioComercialEmisor(String domicilioComercialEmisor) {
        this.domicilioComercialEmisor = domicilioComercialEmisor;
    }

    public void setPuntoVenta(Integer puntoVenta) {
        this.puntoVenta = puntoVenta;
    }

    public void setNumeroComprobante(Long numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public void setNumeroFormateado(String numeroFormateado) {
        this.numeroFormateado = numeroFormateado;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public void setReceptorCondicionIva(CondicionIva receptorCondicionIva) {
        this.receptorCondicionIva = receptorCondicionIva;
    }

    public void setReceptorCodigoAfipIva(Integer receptorCodigoAfipIva) {
        this.receptorCodigoAfipIva = receptorCodigoAfipIva;
    }

    public void setReceptorTipoDocAfip(Integer receptorTipoDocAfip) {
        this.receptorTipoDocAfip = receptorTipoDocAfip;
    }

    public void setReceptorNumeroDoc(String receptorNumeroDoc) {
        this.receptorNumeroDoc = receptorNumeroDoc;
    }

    public void setReceptorNombreRazonSocial(String receptorNombreRazonSocial) {
        this.receptorNombreRazonSocial = receptorNombreRazonSocial;
    }

    public void setReceptorDomicilio(String receptorDomicilio) {
        this.receptorDomicilio = receptorDomicilio;
    }

    public void setImporteNetoGravado(BigDecimal importeNetoGravado) {
        this.importeNetoGravado = importeNetoGravado;
    }

    public void setImporteExento(BigDecimal importeExento) {
        this.importeExento = importeExento;
    }

    public void setImporteIvaTotal(BigDecimal importeIvaTotal) {
        this.importeIvaTotal = importeIvaTotal;
    }

    public void setImporteIva21(BigDecimal importeIva21) {
        this.importeIva21 = importeIva21;
    }

    public void setImporteIva105(BigDecimal importeIva105) {
        this.importeIva105 = importeIva105;
    }

    public void setImporteIva27(BigDecimal importeIva27) {
        this.importeIva27 = importeIva27;
    }

    public void setImporteIvaContenidoTransparencia(BigDecimal importeIvaContenidoTransparencia) {
        this.importeIvaContenidoTransparencia = importeIvaContenidoTransparencia;
    }

    public void setImporteTotal(BigDecimal importeTotal) {
        this.importeTotal = importeTotal;
    }

    public void setCae(String cae) {
        this.cae = cae;
    }

    public void setCaeFechaVencimiento(LocalDate caeFechaVencimiento) {
        this.caeFechaVencimiento = caeFechaVencimiento;
    }

    public void setQrUrlOficialArca(String qrUrlOficialArca) {
        this.qrUrlOficialArca = qrUrlOficialArca;
    }

    public void setQrBase64Payload(String qrBase64Payload) {
        this.qrBase64Payload = qrBase64Payload;
    }

    public void setObservacionesArca(String observacionesArca) {
        this.observacionesArca = observacionesArca;
    }

    public void setUsuarioEmisorUsername(String usuarioEmisorUsername) {
        this.usuarioEmisorUsername = usuarioEmisorUsername;
    }

}
