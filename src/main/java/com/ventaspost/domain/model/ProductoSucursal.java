package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "producto_sucursales",
        uniqueConstraints = @UniqueConstraint(name = "uk_producto_sucursal", columnNames = {"producto_id", "sucursal_id"})
)
public class ProductoSucursal extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "stock_actual", nullable = false, precision = 19, scale = 3)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false, precision = 19, scale = 3)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "precio_venta_sucursal", precision = 19, scale = 2)
    private BigDecimal precioVentaSucursal;

    @Column(name = "ubicacion_gondola", length = 80)
    private String ubicacionGondola;

    public BigDecimal obtenerPrecioVentaEfectivo() {
        if (precioVentaSucursal != null) {
            return precioVentaSucursal;
        }
        return producto != null ? producto.getPrecioVentaGeneral() : BigDecimal.ZERO;
    }

    public boolean esBajoStock() {
        if (stockActual == null || stockMinimo == null) {
            return false;
        }
        return stockActual.compareTo(stockMinimo) <= 0;
    }

    public ProductoSucursal() {
    }

    public Long getId() {
        return this.id;
    }

    public Producto getProducto() {
        return this.producto;
    }

    public Sucursal getSucursal() {
        return this.sucursal;
    }

    public BigDecimal getStockActual() {
        return this.stockActual;
    }

    public BigDecimal getStockMinimo() {
        return this.stockMinimo;
    }

    public BigDecimal getPrecioVentaSucursal() {
        return this.precioVentaSucursal;
    }

    public String getUbicacionGondola() {
        return this.ubicacionGondola;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public void setStockActual(BigDecimal stockActual) {
        this.stockActual = stockActual;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public void setPrecioVentaSucursal(BigDecimal precioVentaSucursal) {
        this.precioVentaSucursal = precioVentaSucursal;
    }

    public void setUbicacionGondola(String ubicacionGondola) {
        this.ubicacionGondola = ubicacionGondola;
    }

}
