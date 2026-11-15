package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "venta_items")
public class VentaItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "codigo_barras_historico", nullable = false, length = 60)
    private String codigoBarrasHistorico;

    @Column(name = "nombre_producto_historico", nullable = false, length = 180)
    private String nombreProductoHistorico;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_medida", nullable = false, length = 30)
    private UnidadMedida unidadMedida;

    @Column(name = "cantidad", nullable = false, precision = 19, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "costo_unitario_con_iva_historico", nullable = false, precision = 19, scale = 2)
    private BigDecimal costoUnitarioConIvaHistorico = BigDecimal.ZERO;

    @Column(name = "alicuota_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal alicuotaIva;

    @Column(name = "descuento_item_porcentaje", nullable = false, precision = 8, scale = 2)
    private BigDecimal descuentoItemPorcentaje = BigDecimal.ZERO;

    @Column(name = "descuento_item_monto", nullable = false, precision = 19, scale = 2)
    private BigDecimal descuentoItemMonto = BigDecimal.ZERO;

    @Column(name = "subtotal_neto_item", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotalNetoItem;

    @Column(name = "costo_total_con_iva_historico", nullable = false, precision = 19, scale = 2)
    private BigDecimal costoTotalConIvaHistorico = BigDecimal.ZERO;

    @Column(name = "stock_previo_sucursal", nullable = false, precision = 19, scale = 3)
    private BigDecimal stockPrevioSucursal;

    @Column(name = "stock_posterior_sucursal", nullable = false, precision = 19, scale = 3)
    private BigDecimal stockPosteriorSucursal;

    @Column(name = "vendido_sin_stock", nullable = false)
    private boolean vendidoSinStock = false;

    public VentaItem() {
    }

    public Long getId() {
        return this.id;
    }

    public Venta getVenta() {
        return this.venta;
    }

    public Producto getProducto() {
        return this.producto;
    }

    public String getCodigoBarrasHistorico() {
        return this.codigoBarrasHistorico;
    }

    public String getNombreProductoHistorico() {
        return this.nombreProductoHistorico;
    }

    public UnidadMedida getUnidadMedida() {
        return this.unidadMedida;
    }

    public BigDecimal getCantidad() {
        return this.cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return this.precioUnitario;
    }

    public BigDecimal getCostoUnitarioConIvaHistorico() {
        return this.costoUnitarioConIvaHistorico;
    }

    public BigDecimal getAlicuotaIva() {
        return this.alicuotaIva;
    }

    public BigDecimal getDescuentoItemPorcentaje() {
        return this.descuentoItemPorcentaje;
    }

    public BigDecimal getDescuentoItemMonto() {
        return this.descuentoItemMonto;
    }

    public BigDecimal getSubtotalNetoItem() {
        return this.subtotalNetoItem;
    }

    public BigDecimal getCostoTotalConIvaHistorico() {
        return this.costoTotalConIvaHistorico;
    }

    public BigDecimal getStockPrevioSucursal() {
        return this.stockPrevioSucursal;
    }

    public BigDecimal getStockPosteriorSucursal() {
        return this.stockPosteriorSucursal;
    }

    public boolean isVendidoSinStock() {
        return this.vendidoSinStock;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public void setCodigoBarrasHistorico(String codigoBarrasHistorico) {
        this.codigoBarrasHistorico = codigoBarrasHistorico;
    }

    public void setNombreProductoHistorico(String nombreProductoHistorico) {
        this.nombreProductoHistorico = nombreProductoHistorico;
    }

    public void setUnidadMedida(UnidadMedida unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public void setCostoUnitarioConIvaHistorico(BigDecimal costoUnitarioConIvaHistorico) {
        this.costoUnitarioConIvaHistorico = costoUnitarioConIvaHistorico;
    }

    public void setAlicuotaIva(BigDecimal alicuotaIva) {
        this.alicuotaIva = alicuotaIva;
    }

    public void setDescuentoItemPorcentaje(BigDecimal descuentoItemPorcentaje) {
        this.descuentoItemPorcentaje = descuentoItemPorcentaje;
    }

    public void setDescuentoItemMonto(BigDecimal descuentoItemMonto) {
        this.descuentoItemMonto = descuentoItemMonto;
    }

    public void setSubtotalNetoItem(BigDecimal subtotalNetoItem) {
        this.subtotalNetoItem = subtotalNetoItem;
    }

    public void setCostoTotalConIvaHistorico(BigDecimal costoTotalConIvaHistorico) {
        this.costoTotalConIvaHistorico = costoTotalConIvaHistorico;
    }

    public void setStockPrevioSucursal(BigDecimal stockPrevioSucursal) {
        this.stockPrevioSucursal = stockPrevioSucursal;
    }

    public void setStockPosteriorSucursal(BigDecimal stockPosteriorSucursal) {
        this.stockPosteriorSucursal = stockPosteriorSucursal;
    }

    public void setVendidoSinStock(boolean vendidoSinStock) {
        this.vendidoSinStock = vendidoSinStock;
    }

}
