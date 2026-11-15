package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_comprobante_interno", nullable = false, unique = true, length = 40)
    private String numeroComprobanteInterno;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "turno_caja_id", nullable = false)
    private TurnoCaja turnoCaja;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta;

    @Column(name = "cliente_nombre", nullable = false, length = 150)
    private String clienteNombre = "Consumidor Final";

    @Column(name = "cliente_documento", length = 20)
    private String clienteDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "cliente_condicion_iva", nullable = false, length = 40)
    private CondicionIva clienteCondicionIva = CondicionIva.CONSUMIDOR_FINAL;

    @Column(name = "subtotal_items", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotalItems = BigDecimal.ZERO;

    @Column(name = "descuento_manual_porcentaje", nullable = false, precision = 8, scale = 2)
    private BigDecimal descuentoManualPorcentaje = BigDecimal.ZERO;

    @Column(name = "descuento_manual_monto", nullable = false, precision = 19, scale = 2)
    private BigDecimal descuentoManualMonto = BigDecimal.ZERO;

    @Column(name = "ajuste_medios_pago_monto", nullable = false, precision = 19, scale = 2)
    private BigDecimal ajusteMediosPagoMonto = BigDecimal.ZERO;

    @Column(name = "total_final", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFinal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_venta", nullable = false, length = 30)
    private EstadoVenta estadoVenta = EstadoVenta.REALIZADA;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_facturacion", nullable = false, length = 30)
    private EstadoFacturacionVenta estadoFacturacion = EstadoFacturacionVenta.NO_FACTURADA;

    @Column(name = "ultimo_error_arca", length = 500)
    private String ultimoErrorArca;

    @Column(name = "cantidad_intentos_arca", nullable = false)
    private Integer cantidadIntentosArca = 0;

    @Column(name = "supervisor_autorizador_username", length = 60)
    private String supervisorAutorizadorUsername;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "usuario_anulacion_username", length = 60)
    private String usuarioAnulacionUsername;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<VentaItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<VentaPago> pagos = new ArrayList<>();

    public Venta() {
    }

    public Long getId() {
        return this.id;
    }

    public String getNumeroComprobanteInterno() {
        return this.numeroComprobanteInterno;
    }

    public Sucursal getSucursal() {
        return this.sucursal;
    }

    public TurnoCaja getTurnoCaja() {
        return this.turnoCaja;
    }

    public Usuario getUsuario() {
        return this.usuario;
    }

    public LocalDateTime getFechaVenta() {
        return this.fechaVenta;
    }

    public String getClienteNombre() {
        return this.clienteNombre;
    }

    public String getClienteDocumento() {
        return this.clienteDocumento;
    }

    public CondicionIva getClienteCondicionIva() {
        return this.clienteCondicionIva;
    }

    public BigDecimal getSubtotalItems() {
        return this.subtotalItems;
    }

    public BigDecimal getDescuentoManualPorcentaje() {
        return this.descuentoManualPorcentaje;
    }

    public BigDecimal getDescuentoManualMonto() {
        return this.descuentoManualMonto;
    }

    public BigDecimal getAjusteMediosPagoMonto() {
        return this.ajusteMediosPagoMonto;
    }

    public BigDecimal getTotalFinal() {
        return this.totalFinal;
    }

    public EstadoVenta getEstadoVenta() {
        return this.estadoVenta;
    }

    public EstadoFacturacionVenta getEstadoFacturacion() {
        return this.estadoFacturacion;
    }

    public String getUltimoErrorArca() {
        return this.ultimoErrorArca;
    }

    public Integer getCantidadIntentosArca() {
        return this.cantidadIntentosArca;
    }

    public String getSupervisorAutorizadorUsername() {
        return this.supervisorAutorizadorUsername;
    }

    public String getMotivoAnulacion() {
        return this.motivoAnulacion;
    }

    public LocalDateTime getFechaAnulacion() {
        return this.fechaAnulacion;
    }

    public String getUsuarioAnulacionUsername() {
        return this.usuarioAnulacionUsername;
    }

    public String getObservaciones() {
        return this.observaciones;
    }

    public List<VentaItem> getItems() {
        return this.items;
    }

    public List<VentaPago> getPagos() {
        return this.pagos;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNumeroComprobanteInterno(String numeroComprobanteInterno) {
        this.numeroComprobanteInterno = numeroComprobanteInterno;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public void setTurnoCaja(TurnoCaja turnoCaja) {
        this.turnoCaja = turnoCaja;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setFechaVenta(LocalDateTime fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public void setClienteDocumento(String clienteDocumento) {
        this.clienteDocumento = clienteDocumento;
    }

    public void setClienteCondicionIva(CondicionIva clienteCondicionIva) {
        this.clienteCondicionIva = clienteCondicionIva;
    }

    public void setSubtotalItems(BigDecimal subtotalItems) {
        this.subtotalItems = subtotalItems;
    }

    public void setDescuentoManualPorcentaje(BigDecimal descuentoManualPorcentaje) {
        this.descuentoManualPorcentaje = descuentoManualPorcentaje;
    }

    public void setDescuentoManualMonto(BigDecimal descuentoManualMonto) {
        this.descuentoManualMonto = descuentoManualMonto;
    }

    public void setAjusteMediosPagoMonto(BigDecimal ajusteMediosPagoMonto) {
        this.ajusteMediosPagoMonto = ajusteMediosPagoMonto;
    }

    public void setTotalFinal(BigDecimal totalFinal) {
        this.totalFinal = totalFinal;
    }

    public void setEstadoVenta(EstadoVenta estadoVenta) {
        this.estadoVenta = estadoVenta;
    }

    public void setEstadoFacturacion(EstadoFacturacionVenta estadoFacturacion) {
        this.estadoFacturacion = estadoFacturacion;
    }

    public void setUltimoErrorArca(String ultimoErrorArca) {
        this.ultimoErrorArca = ultimoErrorArca;
    }

    public void setCantidadIntentosArca(Integer cantidadIntentosArca) {
        this.cantidadIntentosArca = cantidadIntentosArca;
    }

    public void setSupervisorAutorizadorUsername(String supervisorAutorizadorUsername) {
        this.supervisorAutorizadorUsername = supervisorAutorizadorUsername;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public void setFechaAnulacion(LocalDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public void setUsuarioAnulacionUsername(String usuarioAnulacionUsername) {
        this.usuarioAnulacionUsername = usuarioAnulacionUsername;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setItems(List<VentaItem> items) {
        this.items = items;
    }

    public void setPagos(List<VentaPago> pagos) {
        this.pagos = pagos;
    }

}
