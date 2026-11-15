package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "productos")
public class Producto extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_barras", nullable = false, unique = true, length = 60)
    private String codigoBarras;

    @Column(name = "codigo_proveedor_sku", length = 60)
    private String codigoProveedorSku;

    @Column(name = "es_codigo_interno", nullable = false)
    private boolean esCodigoInterno = false;

    @Column(name = "nombre", nullable = false, length = 180)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "marca_id", nullable = false)
    private Marca marca;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_medida", nullable = false, length = 30)
    private UnidadMedida unidadMedida = UnidadMedida.UNIDAD;

    @Column(name = "precio_compra", nullable = false, precision = 19, scale = 4)
    private BigDecimal precioCompra = BigDecimal.ZERO;

    @Column(name = "iva_incluido_compra", nullable = false)
    private boolean ivaIncluidoCompra = true;

    @Column(name = "alicuota_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal alicuotaIva = new BigDecimal("21.00");

    @Column(name = "porcentaje_ganancia", nullable = false, precision = 10, scale = 2)
    private BigDecimal porcentajeGanancia = BigDecimal.ZERO;

    @Column(name = "precio_venta_general", nullable = false, precision = 19, scale = 2)
    private BigDecimal precioVentaGeneral = BigDecimal.ZERO;

    @Column(name = "stock_minimo_defecto", nullable = false, precision = 19, scale = 3)
    private BigDecimal stockMinimoDefecto = BigDecimal.ZERO;

    /**
     * Calcula el costo final con IVA incluido a partir del precio de compra y la condición de IVA en compra.
     */
    public BigDecimal calcularCostoFinalConIva() {
        if (precioCompra == null) {
            return BigDecimal.ZERO;
        }
        if (ivaIncluidoCompra || alicuotaIva == null || alicuotaIva.compareTo(BigDecimal.ZERO) <= 0) {
            return precioCompra.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal factorIva = BigDecimal.ONE.add(alicuotaIva.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        return precioCompra.multiply(factorIva).setScale(4, RoundingMode.HALF_UP);
    }

    public Producto() {
    }

    public Long getId() {
        return this.id;
    }

    public String getCodigoBarras() {
        return this.codigoBarras;
    }

    public String getCodigoProveedorSku() {
        return this.codigoProveedorSku;
    }

    public boolean isEsCodigoInterno() {
        return this.esCodigoInterno;
    }

    public String getNombre() {
        return this.nombre;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public Categoria getCategoria() {
        return this.categoria;
    }

    public Marca getMarca() {
        return this.marca;
    }

    public Proveedor getProveedor() {
        return this.proveedor;
    }

    public UnidadMedida getUnidadMedida() {
        return this.unidadMedida;
    }

    public BigDecimal getPrecioCompra() {
        return this.precioCompra;
    }

    public boolean isIvaIncluidoCompra() {
        return this.ivaIncluidoCompra;
    }

    public BigDecimal getAlicuotaIva() {
        return this.alicuotaIva;
    }

    public BigDecimal getPorcentajeGanancia() {
        return this.porcentajeGanancia;
    }

    public BigDecimal getPrecioVentaGeneral() {
        return this.precioVentaGeneral;
    }

    public BigDecimal getStockMinimoDefecto() {
        return this.stockMinimoDefecto;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public void setCodigoProveedorSku(String codigoProveedorSku) {
        this.codigoProveedorSku = codigoProveedorSku;
    }

    public void setEsCodigoInterno(boolean esCodigoInterno) {
        this.esCodigoInterno = esCodigoInterno;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public void setMarca(Marca marca) {
        this.marca = marca;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public void setUnidadMedida(UnidadMedida unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra;
    }

    public void setIvaIncluidoCompra(boolean ivaIncluidoCompra) {
        this.ivaIncluidoCompra = ivaIncluidoCompra;
    }

    public void setAlicuotaIva(BigDecimal alicuotaIva) {
        this.alicuotaIva = alicuotaIva;
    }

    public void setPorcentajeGanancia(BigDecimal porcentajeGanancia) {
        this.porcentajeGanancia = porcentajeGanancia;
    }

    public void setPrecioVentaGeneral(BigDecimal precioVentaGeneral) {
        this.precioVentaGeneral = precioVentaGeneral;
    }

    public void setStockMinimoDefecto(BigDecimal stockMinimoDefecto) {
        this.stockMinimoDefecto = stockMinimoDefecto;
    }

}
