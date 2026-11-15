package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_arca_sucursal")
public class ConfiguracionArcaSucursal extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false, unique = true)
    private Sucursal sucursal;

    @Column(name = "razon_social_emisor", nullable = false, length = 160)
    private String razonSocialEmisor;

    @Column(name = "cuit_emisor", nullable = false, length = 13)
    private String cuitEmisor;

    @Column(name = "punto_venta_arca", nullable = false)
    private Integer puntoVentaArca;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_iva_emisor", nullable = false, length = 40)
    private CondicionIva condicionIvaEmisor = CondicionIva.RESPONSABLE_INSCRIPTO;

    @Column(name = "ingresos_brutos", length = 50)
    private String ingresosBrutos;

    @Column(name = "fecha_inicio_actividades")
    private LocalDate fechaInicioActividades;

    @Column(name = "domicilio_comercial", nullable = false, length = 220)
    private String domicilioComercial;

    @Enumerated(EnumType.STRING)
    @Column(name = "entorno_arca", nullable = false, length = 25)
    private EntornoArca entornoArca = EntornoArca.HOMOLOGACION;

    @Column(name = "simulador_homologacion_activo", nullable = false)
    private boolean simuladorHomologacionActivo = true;

    @Lob
    @Column(name = "certificado_crt_pem", columnDefinition = "LONGTEXT")
    private String certificadoCrtPem;

    @Lob
    @Column(name = "clave_privada_key_pem", columnDefinition = "LONGTEXT")
    private String clavePrivadaKeyPem;

    @Column(name = "nombre_archivo_crt", length = 150)
    private String nombreArchivoCrt;

    @Column(name = "nombre_archivo_key", length = 150)
    private String nombreArchivoKey;

    @Column(name = "monto_tope_consumidor_final_sin_dni", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoTopeConsumidorFinalSinDni = new BigDecimal("344488.00");

    @Column(name = "plazo_maximo_dias_facturacion", nullable = false)
    private Integer plazoMaximoDiasFacturacion = 30;

    @Lob
    @Column(name = "wsaa_token_cache", columnDefinition = "LONGTEXT")
    private String wsaaTokenCache;

    @Lob
    @Column(name = "wsaa_sign_cache", columnDefinition = "LONGTEXT")
    private String wsaaSignCache;

    @Column(name = "wsaa_expiracion_cache")
    private LocalDateTime wsaaExpiracionCache;

    public boolean poseeCertificadoYClaveCargados() {
        return certificadoCrtPem != null && !certificadoCrtPem.isBlank()
                && clavePrivadaKeyPem != null && !clavePrivadaKeyPem.isBlank();
    }

    public boolean poseeTicketAccesoWsaaVigente() {
        return wsaaTokenCache != null && !wsaaTokenCache.isBlank()
                && wsaaSignCache != null && !wsaaSignCache.isBlank()
                && wsaaExpiracionCache != null
                && wsaaExpiracionCache.isAfter(LocalDateTime.now().plusMinutes(10));
    }

    public ConfiguracionArcaSucursal() {
    }

    public Long getId() {
        return this.id;
    }

    public Sucursal getSucursal() {
        return this.sucursal;
    }

    public String getRazonSocialEmisor() {
        return this.razonSocialEmisor;
    }

    public String getCuitEmisor() {
        return this.cuitEmisor;
    }

    public Integer getPuntoVentaArca() {
        return this.puntoVentaArca;
    }

    public CondicionIva getCondicionIvaEmisor() {
        return this.condicionIvaEmisor;
    }

    public String getIngresosBrutos() {
        return this.ingresosBrutos;
    }

    public LocalDate getFechaInicioActividades() {
        return this.fechaInicioActividades;
    }

    public String getDomicilioComercial() {
        return this.domicilioComercial;
    }

    public EntornoArca getEntornoArca() {
        return this.entornoArca;
    }

    public boolean isSimuladorHomologacionActivo() {
        return this.simuladorHomologacionActivo;
    }

    public String getCertificadoCrtPem() {
        return this.certificadoCrtPem;
    }

    public String getClavePrivadaKeyPem() {
        return this.clavePrivadaKeyPem;
    }

    public String getNombreArchivoCrt() {
        return this.nombreArchivoCrt;
    }

    public String getNombreArchivoKey() {
        return this.nombreArchivoKey;
    }

    public BigDecimal getMontoTopeConsumidorFinalSinDni() {
        return this.montoTopeConsumidorFinalSinDni;
    }

    public Integer getPlazoMaximoDiasFacturacion() {
        return this.plazoMaximoDiasFacturacion;
    }

    public String getWsaaTokenCache() {
        return this.wsaaTokenCache;
    }

    public String getWsaaSignCache() {
        return this.wsaaSignCache;
    }

    public LocalDateTime getWsaaExpiracionCache() {
        return this.wsaaExpiracionCache;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public void setRazonSocialEmisor(String razonSocialEmisor) {
        this.razonSocialEmisor = razonSocialEmisor;
    }

    public void setCuitEmisor(String cuitEmisor) {
        this.cuitEmisor = cuitEmisor;
    }

    public void setPuntoVentaArca(Integer puntoVentaArca) {
        this.puntoVentaArca = puntoVentaArca;
    }

    public void setCondicionIvaEmisor(CondicionIva condicionIvaEmisor) {
        this.condicionIvaEmisor = condicionIvaEmisor;
    }

    public void setIngresosBrutos(String ingresosBrutos) {
        this.ingresosBrutos = ingresosBrutos;
    }

    public void setFechaInicioActividades(LocalDate fechaInicioActividades) {
        this.fechaInicioActividades = fechaInicioActividades;
    }

    public void setDomicilioComercial(String domicilioComercial) {
        this.domicilioComercial = domicilioComercial;
    }

    public void setEntornoArca(EntornoArca entornoArca) {
        this.entornoArca = entornoArca;
    }

    public void setSimuladorHomologacionActivo(boolean simuladorHomologacionActivo) {
        this.simuladorHomologacionActivo = simuladorHomologacionActivo;
    }

    public void setCertificadoCrtPem(String certificadoCrtPem) {
        this.certificadoCrtPem = certificadoCrtPem;
    }

    public void setClavePrivadaKeyPem(String clavePrivadaKeyPem) {
        this.clavePrivadaKeyPem = clavePrivadaKeyPem;
    }

    public void setNombreArchivoCrt(String nombreArchivoCrt) {
        this.nombreArchivoCrt = nombreArchivoCrt;
    }

    public void setNombreArchivoKey(String nombreArchivoKey) {
        this.nombreArchivoKey = nombreArchivoKey;
    }

    public void setMontoTopeConsumidorFinalSinDni(BigDecimal montoTopeConsumidorFinalSinDni) {
        this.montoTopeConsumidorFinalSinDni = montoTopeConsumidorFinalSinDni;
    }

    public void setPlazoMaximoDiasFacturacion(Integer plazoMaximoDiasFacturacion) {
        this.plazoMaximoDiasFacturacion = plazoMaximoDiasFacturacion;
    }

    public void setWsaaTokenCache(String wsaaTokenCache) {
        this.wsaaTokenCache = wsaaTokenCache;
    }

    public void setWsaaSignCache(String wsaaSignCache) {
        this.wsaaSignCache = wsaaSignCache;
    }

    public void setWsaaExpiracionCache(LocalDateTime wsaaExpiracionCache) {
        this.wsaaExpiracionCache = wsaaExpiracionCache;
    }

}
