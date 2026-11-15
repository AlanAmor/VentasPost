package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "venta_pagos")
public class VentaPago extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 40)
    private CodigoMedioPago medioPago;

    @Column(name = "monto_base", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoBase;

    @Column(name = "porcentaje_ajuste_aplicado", nullable = false, precision = 8, scale = 2)
    private BigDecimal porcentajeAjusteAplicado = BigDecimal.ZERO;

    @Column(name = "monto_ajuste", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoAjuste = BigDecimal.ZERO;

    @Column(name = "monto_final_cobrado", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoFinalCobrado;

    @Column(name = "monto_recibido_efectivo", precision = 19, scale = 2)
    private BigDecimal montoRecibidoEfectivo;

    @Column(name = "vuelto_efectivo", precision = 19, scale = 2)
    private BigDecimal vueltoEfectivo;

    @Column(name = "cuotas", nullable = false)
    private Integer cuotas = 1;

    @Column(name = "referencia_operacion", length = 100)
    private String referenciaOperacion;

    public VentaPago() {
    }

    public Long getId() {
        return this.id;
    }

    public Venta getVenta() {
        return this.venta;
    }

    public CodigoMedioPago getMedioPago() {
        return this.medioPago;
    }

    public BigDecimal getMontoBase() {
        return this.montoBase;
    }

    public BigDecimal getPorcentajeAjusteAplicado() {
        return this.porcentajeAjusteAplicado;
    }

    public BigDecimal getMontoAjuste() {
        return this.montoAjuste;
    }

    public BigDecimal getMontoFinalCobrado() {
        return this.montoFinalCobrado;
    }

    public BigDecimal getMontoRecibidoEfectivo() {
        return this.montoRecibidoEfectivo;
    }

    public BigDecimal getVueltoEfectivo() {
        return this.vueltoEfectivo;
    }

    public Integer getCuotas() {
        return this.cuotas;
    }

    public String getReferenciaOperacion() {
        return this.referenciaOperacion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public void setMedioPago(CodigoMedioPago medioPago) {
        this.medioPago = medioPago;
    }

    public void setMontoBase(BigDecimal montoBase) {
        this.montoBase = montoBase;
    }

    public void setPorcentajeAjusteAplicado(BigDecimal porcentajeAjusteAplicado) {
        this.porcentajeAjusteAplicado = porcentajeAjusteAplicado;
    }

    public void setMontoAjuste(BigDecimal montoAjuste) {
        this.montoAjuste = montoAjuste;
    }

    public void setMontoFinalCobrado(BigDecimal montoFinalCobrado) {
        this.montoFinalCobrado = montoFinalCobrado;
    }

    public void setMontoRecibidoEfectivo(BigDecimal montoRecibidoEfectivo) {
        this.montoRecibidoEfectivo = montoRecibidoEfectivo;
    }

    public void setVueltoEfectivo(BigDecimal vueltoEfectivo) {
        this.vueltoEfectivo = vueltoEfectivo;
    }

    public void setCuotas(Integer cuotas) {
        this.cuotas = cuotas;
    }

    public void setReferenciaOperacion(String referenciaOperacion) {
        this.referenciaOperacion = referenciaOperacion;
    }

}
