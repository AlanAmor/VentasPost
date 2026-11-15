package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "adelantos_sueldo")
public class AdelantoSueldo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "fecha_adelanto", nullable = false)
    private LocalDate fechaAdelanto;

    @Column(name = "monto", nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "medio_pago", nullable = false, length = 40)
    private String medioPago = "EFECTIVO";

    @Column(name = "motivo", nullable = false, length = 255)
    private String motivo;

    @Column(name = "descontado", nullable = false)
    private boolean descontado = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liquidacion_id")
    private LiquidacionSueldo liquidacion;

    @Column(name = "registrado_por_username", nullable = false, length = 60)
    private String registradoPorUsername;

    public AdelantoSueldo() {
    }

    public Long getId() {
        return this.id;
    }

    public Usuario getUsuario() {
        return this.usuario;
    }

    public Sucursal getSucursal() {
        return this.sucursal;
    }

    public LocalDate getFechaAdelanto() {
        return this.fechaAdelanto;
    }

    public BigDecimal getMonto() {
        return this.monto;
    }

    public String getMedioPago() {
        return this.medioPago;
    }

    public String getMotivo() {
        return this.motivo;
    }

    public boolean isDescontado() {
        return this.descontado;
    }

    public LiquidacionSueldo getLiquidacion() {
        return this.liquidacion;
    }

    public String getRegistradoPorUsername() {
        return this.registradoPorUsername;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public void setFechaAdelanto(LocalDate fechaAdelanto) {
        this.fechaAdelanto = fechaAdelanto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public void setDescontado(boolean descontado) {
        this.descontado = descontado;
    }

    public void setLiquidacion(LiquidacionSueldo liquidacion) {
        this.liquidacion = liquidacion;
    }

    public void setRegistradoPorUsername(String registradoPorUsername) {
        this.registradoPorUsername = registradoPorUsername;
    }

}
