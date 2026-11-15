package com.ventaspost.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "intentos_facturacion_arca")
public class IntentoFacturacionArca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "fecha_intento", nullable = false)
    private LocalDateTime fechaIntento = LocalDateTime.now();

    @Column(name = "naturaleza_operacion", nullable = false, length = 30)
    private String naturalezaOperacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "entorno_arca", nullable = false, length = 25)
    private EntornoArca entornoArca;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_emision", nullable = false, length = 30)
    private ModoEmisionArca modoEmision;

    @Column(name = "exitoso", nullable = false)
    private boolean exitoso;

    @Column(name = "codigo_respuesta", length = 60)
    private String codigoRespuesta;

    @Column(name = "mensaje_detalle", nullable = false, length = 600)
    private String mensajeDetalle;

    @Column(name = "cae_obtenido", length = 30)
    private String caeObtenido;

    @Column(name = "numero_comprobante_obtenido", length = 25)
    private String numeroComprobanteObtenido;

    @Column(name = "usuario_username", nullable = false, length = 60)
    private String usuarioUsername;

    public IntentoFacturacionArca() {
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

    public LocalDateTime getFechaIntento() {
        return this.fechaIntento;
    }

    public String getNaturalezaOperacion() {
        return this.naturalezaOperacion;
    }

    public EntornoArca getEntornoArca() {
        return this.entornoArca;
    }

    public ModoEmisionArca getModoEmision() {
        return this.modoEmision;
    }

    public boolean isExitoso() {
        return this.exitoso;
    }

    public String getCodigoRespuesta() {
        return this.codigoRespuesta;
    }

    public String getMensajeDetalle() {
        return this.mensajeDetalle;
    }

    public String getCaeObtenido() {
        return this.caeObtenido;
    }

    public String getNumeroComprobanteObtenido() {
        return this.numeroComprobanteObtenido;
    }

    public String getUsuarioUsername() {
        return this.usuarioUsername;
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

    public void setFechaIntento(LocalDateTime fechaIntento) {
        this.fechaIntento = fechaIntento;
    }

    public void setNaturalezaOperacion(String naturalezaOperacion) {
        this.naturalezaOperacion = naturalezaOperacion;
    }

    public void setEntornoArca(EntornoArca entornoArca) {
        this.entornoArca = entornoArca;
    }

    public void setModoEmision(ModoEmisionArca modoEmision) {
        this.modoEmision = modoEmision;
    }

    public void setExitoso(boolean exitoso) {
        this.exitoso = exitoso;
    }

    public void setCodigoRespuesta(String codigoRespuesta) {
        this.codigoRespuesta = codigoRespuesta;
    }

    public void setMensajeDetalle(String mensajeDetalle) {
        this.mensajeDetalle = mensajeDetalle;
    }

    public void setCaeObtenido(String caeObtenido) {
        this.caeObtenido = caeObtenido;
    }

    public void setNumeroComprobanteObtenido(String numeroComprobanteObtenido) {
        this.numeroComprobanteObtenido = numeroComprobanteObtenido;
    }

    public void setUsuarioUsername(String usuarioUsername) {
        this.usuarioUsername = usuarioUsername;
    }

}
