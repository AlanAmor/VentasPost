package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "horarios_asignados_empleado")
public class HorarioAsignadoEmpleado extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana; // 1 = Lunes .. 7 = Domingo

    @Column(name = "hora_entrada", nullable = false)
    private LocalTime horaEntrada;

    @Column(name = "hora_salida", nullable = false)
    private LocalTime horaSalida;

    @Column(name = "horas_teoricas", nullable = false, precision = 8, scale = 2)
    private BigDecimal horasTeoricas;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    public HorarioAsignadoEmpleado() {
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

    public Integer getDiaSemana() {
        return this.diaSemana;
    }

    public LocalTime getHoraEntrada() {
        return this.horaEntrada;
    }

    public LocalTime getHoraSalida() {
        return this.horaSalida;
    }

    public BigDecimal getHorasTeoricas() {
        return this.horasTeoricas;
    }

    public String getObservaciones() {
        return this.observaciones;
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

    public void setDiaSemana(Integer diaSemana) {
        this.diaSemana = diaSemana;
    }

    public void setHoraEntrada(LocalTime horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public void setHoraSalida(LocalTime horaSalida) {
        this.horaSalida = horaSalida;
    }

    public void setHorasTeoricas(BigDecimal horasTeoricas) {
        this.horasTeoricas = horasTeoricas;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

}
