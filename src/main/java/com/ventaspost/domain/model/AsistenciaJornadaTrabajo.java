package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "asistencias_jornadas_trabajo")
public class AsistenciaJornadaTrabajo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "fecha_jornada", nullable = false)
    private LocalDate fechaJornada;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_registro", nullable = false, length = 30)
    private OrigenRegistroJornada origenRegistro = OrigenRegistroJornada.HORARIO_TEORICO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_jornada", nullable = false, length = 35)
    private EstadoJornadaTrabajo estadoJornada = EstadoJornadaTrabajo.TRABAJADA;

    @Column(name = "hora_entrada_programada")
    private LocalTime horaEntradaProgramada;

    @Column(name = "hora_salida_programada")
    private LocalTime horaSalidaProgramada;

    @Column(name = "fecha_hora_ingreso_real")
    private LocalDateTime fechaHoraIngresoReal;

    @Column(name = "fecha_hora_egreso_real")
    private LocalDateTime fechaHoraEgresoReal;

    @Column(name = "es_feriado_o_domingo", nullable = false)
    private boolean esFeriadoODomingo = false;

    @Column(name = "horas_normales", nullable = false, precision = 8, scale = 2)
    private BigDecimal horasNormales = BigDecimal.ZERO;

    @Column(name = "horas_extras_simples", nullable = false, precision = 8, scale = 2)
    private BigDecimal horasExtrasSimples = BigDecimal.ZERO;

    @Column(name = "horas_feriado_domingo", nullable = false, precision = 8, scale = 2)
    private BigDecimal horasFeriadoDomingo = BigDecimal.ZERO;

    @Column(name = "horas_nocturnas", nullable = false, precision = 8, scale = 2)
    private BigDecimal horasNocturnas = BigDecimal.ZERO;

    @Column(name = "total_horas_computadas", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalHorasComputadas = BigDecimal.ZERO;

    @Column(name = "liquidada", nullable = false)
    private boolean liquidada = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liquidacion_id")
    private LiquidacionSueldo liquidacion;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    public AsistenciaJornadaTrabajo() {
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

    public LocalDate getFechaJornada() {
        return this.fechaJornada;
    }

    public OrigenRegistroJornada getOrigenRegistro() {
        return this.origenRegistro;
    }

    public EstadoJornadaTrabajo getEstadoJornada() {
        return this.estadoJornada;
    }

    public LocalTime getHoraEntradaProgramada() {
        return this.horaEntradaProgramada;
    }

    public LocalTime getHoraSalidaProgramada() {
        return this.horaSalidaProgramada;
    }

    public LocalDateTime getFechaHoraIngresoReal() {
        return this.fechaHoraIngresoReal;
    }

    public LocalDateTime getFechaHoraEgresoReal() {
        return this.fechaHoraEgresoReal;
    }

    public boolean isEsFeriadoODomingo() {
        return this.esFeriadoODomingo;
    }

    public BigDecimal getHorasNormales() {
        return this.horasNormales;
    }

    public BigDecimal getHorasExtrasSimples() {
        return this.horasExtrasSimples;
    }

    public BigDecimal getHorasFeriadoDomingo() {
        return this.horasFeriadoDomingo;
    }

    public BigDecimal getHorasNocturnas() {
        return this.horasNocturnas;
    }

    public BigDecimal getTotalHorasComputadas() {
        return this.totalHorasComputadas;
    }

    public boolean isLiquidada() {
        return this.liquidada;
    }

    public LiquidacionSueldo getLiquidacion() {
        return this.liquidacion;
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

    public void setFechaJornada(LocalDate fechaJornada) {
        this.fechaJornada = fechaJornada;
    }

    public void setOrigenRegistro(OrigenRegistroJornada origenRegistro) {
        this.origenRegistro = origenRegistro;
    }

    public void setEstadoJornada(EstadoJornadaTrabajo estadoJornada) {
        this.estadoJornada = estadoJornada;
    }

    public void setHoraEntradaProgramada(LocalTime horaEntradaProgramada) {
        this.horaEntradaProgramada = horaEntradaProgramada;
    }

    public void setHoraSalidaProgramada(LocalTime horaSalidaProgramada) {
        this.horaSalidaProgramada = horaSalidaProgramada;
    }

    public void setFechaHoraIngresoReal(LocalDateTime fechaHoraIngresoReal) {
        this.fechaHoraIngresoReal = fechaHoraIngresoReal;
    }

    public void setFechaHoraEgresoReal(LocalDateTime fechaHoraEgresoReal) {
        this.fechaHoraEgresoReal = fechaHoraEgresoReal;
    }

    public void setEsFeriadoODomingo(boolean esFeriadoODomingo) {
        this.esFeriadoODomingo = esFeriadoODomingo;
    }

    public void setHorasNormales(BigDecimal horasNormales) {
        this.horasNormales = horasNormales;
    }

    public void setHorasExtrasSimples(BigDecimal horasExtrasSimples) {
        this.horasExtrasSimples = horasExtrasSimples;
    }

    public void setHorasFeriadoDomingo(BigDecimal horasFeriadoDomingo) {
        this.horasFeriadoDomingo = horasFeriadoDomingo;
    }

    public void setHorasNocturnas(BigDecimal horasNocturnas) {
        this.horasNocturnas = horasNocturnas;
    }

    public void setTotalHorasComputadas(BigDecimal totalHorasComputadas) {
        this.totalHorasComputadas = totalHorasComputadas;
    }

    public void setLiquidada(boolean liquidada) {
        this.liquidada = liquidada;
    }

    public void setLiquidacion(LiquidacionSueldo liquidacion) {
        this.liquidacion = liquidacion;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

}
