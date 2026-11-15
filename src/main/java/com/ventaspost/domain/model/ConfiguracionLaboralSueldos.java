package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "configuracion_laboral_sueldos")
public class ConfiguracionLaboralSueldos extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "porcentaje_recargo_extra_simple", nullable = false, precision = 8, scale = 2)
    private BigDecimal porcentajeRecargoExtraSimple = new BigDecimal("50.00");

    @Column(name = "porcentaje_recargo_feriado_domingo", nullable = false, precision = 8, scale = 2)
    private BigDecimal porcentajeRecargoFeriadoDomingo = new BigDecimal("100.00");

    @Column(name = "porcentaje_recargo_nocturno", nullable = false, precision = 8, scale = 2)
    private BigDecimal porcentajeRecargoNocturno = new BigDecimal("15.00");

    @Column(name = "hora_inicio_nocturno", nullable = false)
    private LocalTime horaInicioNocturno = LocalTime.of(21, 0);

    @Column(name = "hora_fin_nocturno", nullable = false)
    private LocalTime horaFinNocturno = LocalTime.of(6, 0);

    public ConfiguracionLaboralSueldos() {
    }

    public Long getId() {
        return this.id;
    }

    public BigDecimal getPorcentajeRecargoExtraSimple() {
        return this.porcentajeRecargoExtraSimple;
    }

    public BigDecimal getPorcentajeRecargoFeriadoDomingo() {
        return this.porcentajeRecargoFeriadoDomingo;
    }

    public BigDecimal getPorcentajeRecargoNocturno() {
        return this.porcentajeRecargoNocturno;
    }

    public LocalTime getHoraInicioNocturno() {
        return this.horaInicioNocturno;
    }

    public LocalTime getHoraFinNocturno() {
        return this.horaFinNocturno;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPorcentajeRecargoExtraSimple(BigDecimal porcentajeRecargoExtraSimple) {
        this.porcentajeRecargoExtraSimple = porcentajeRecargoExtraSimple;
    }

    public void setPorcentajeRecargoFeriadoDomingo(BigDecimal porcentajeRecargoFeriadoDomingo) {
        this.porcentajeRecargoFeriadoDomingo = porcentajeRecargoFeriadoDomingo;
    }

    public void setPorcentajeRecargoNocturno(BigDecimal porcentajeRecargoNocturno) {
        this.porcentajeRecargoNocturno = porcentajeRecargoNocturno;
    }

    public void setHoraInicioNocturno(LocalTime horaInicioNocturno) {
        this.horaInicioNocturno = horaInicioNocturno;
    }

    public void setHoraFinNocturno(LocalTime horaFinNocturno) {
        this.horaFinNocturno = horaFinNocturno;
    }

}
