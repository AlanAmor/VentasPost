package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "liquidacion_conceptos")
public class LiquidacionConcepto extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liquidacion_id", nullable = false)
    private LiquidacionSueldo liquidacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_concepto", nullable = false, length = 35)
    private TipoConceptoLiquidacion tipoConcepto;

    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    @Column(name = "monto", nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adelanto_id")
    private AdelantoSueldo adelanto;

    public LiquidacionConcepto() {
    }

    public Long getId() {
        return this.id;
    }

    public LiquidacionSueldo getLiquidacion() {
        return this.liquidacion;
    }

    public TipoConceptoLiquidacion getTipoConcepto() {
        return this.tipoConcepto;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public BigDecimal getMonto() {
        return this.monto;
    }

    public AdelantoSueldo getAdelanto() {
        return this.adelanto;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLiquidacion(LiquidacionSueldo liquidacion) {
        this.liquidacion = liquidacion;
    }

    public void setTipoConcepto(TipoConceptoLiquidacion tipoConcepto) {
        this.tipoConcepto = tipoConcepto;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public void setAdelanto(AdelantoSueldo adelanto) {
        this.adelanto = adelanto;
    }

}
