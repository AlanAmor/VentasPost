package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "liquidaciones_sueldos")
public class LiquidacionSueldo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_recibo_interno", nullable = false, unique = true, length = 40)
    private String numeroReciboInterno;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;

    @Column(name = "fecha_liquidacion", nullable = false)
    private LocalDate fechaLiquidacion;

    @Column(name = "valor_hora_base_aplicado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorHoraBaseAplicado;

    @Column(name = "origen_valor_hora", nullable = false, length = 100)
    private String origenValorHora;

    @Column(name = "porcentaje_extra_aplicado", nullable = false, precision = 8, scale = 2)
    private BigDecimal porcentajeExtraAplicado = BigDecimal.ZERO;

    @Column(name = "porcentaje_feriado_aplicado", nullable = false, precision = 8, scale = 2)
    private BigDecimal porcentajeFeriadoAplicado = BigDecimal.ZERO;

    @Column(name = "porcentaje_nocturno_aplicado", nullable = false, precision = 8, scale = 2)
    private BigDecimal porcentajeNocturnoAplicado = BigDecimal.ZERO;

    @Column(name = "total_horas_normales", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalHorasNormales = BigDecimal.ZERO;

    @Column(name = "total_horas_extras", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalHorasExtras = BigDecimal.ZERO;

    @Column(name = "total_horas_feriado", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalHorasFeriado = BigDecimal.ZERO;

    @Column(name = "total_horas_nocturnas", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalHorasNocturnas = BigDecimal.ZERO;

    @Column(name = "total_horas_trabajadas", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalHorasTrabajadas = BigDecimal.ZERO;

    @Column(name = "importe_horas_normales", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeHorasNormales = BigDecimal.ZERO;

    @Column(name = "importe_horas_extras", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeHorasExtras = BigDecimal.ZERO;

    @Column(name = "importe_horas_feriado", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeHorasFeriado = BigDecimal.ZERO;

    @Column(name = "importe_adicional_nocturno", nullable = false, precision = 19, scale = 2)
    private BigDecimal importeAdicionalNocturno = BigDecimal.ZERO;

    @Column(name = "subtotal_haberes_horas", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotalHaberesHoras = BigDecimal.ZERO;

    @Column(name = "total_bonos_adicionales", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalBonosAdicionales = BigDecimal.ZERO;

    @Column(name = "total_adelantos_descontados", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAdelantosDescontados = BigDecimal.ZERO;

    @Column(name = "total_otros_descuentos", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalOtrosDescuentos = BigDecimal.ZERO;

    @Column(name = "neto_a_cobrar", nullable = false, precision = 19, scale = 2)
    private BigDecimal netoACobrar = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 25)
    private String estado = "PAGADA";

    @Column(name = "medio_pago", nullable = false, length = 40)
    private String medioPago = "EFECTIVO";

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago = LocalDateTime.now();

    @Column(name = "observaciones", length = 400)
    private String observaciones;

    @Column(name = "liquidado_por_username", nullable = false, length = 60)
    private String liquidadoPorUsername;

    @OneToMany(mappedBy = "liquidacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LiquidacionConcepto> conceptos = new ArrayList<>();

    public LiquidacionSueldo() {
    }

    public Long getId() {
        return this.id;
    }

    public String getNumeroReciboInterno() {
        return this.numeroReciboInterno;
    }

    public Usuario getUsuario() {
        return this.usuario;
    }

    public Sucursal getSucursal() {
        return this.sucursal;
    }

    public LocalDate getFechaDesde() {
        return this.fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return this.fechaHasta;
    }

    public LocalDate getFechaLiquidacion() {
        return this.fechaLiquidacion;
    }

    public BigDecimal getValorHoraBaseAplicado() {
        return this.valorHoraBaseAplicado;
    }

    public String getOrigenValorHora() {
        return this.origenValorHora;
    }

    public BigDecimal getPorcentajeExtraAplicado() {
        return this.porcentajeExtraAplicado;
    }

    public BigDecimal getPorcentajeFeriadoAplicado() {
        return this.porcentajeFeriadoAplicado;
    }

    public BigDecimal getPorcentajeNocturnoAplicado() {
        return this.porcentajeNocturnoAplicado;
    }

    public BigDecimal getTotalHorasNormales() {
        return this.totalHorasNormales;
    }

    public BigDecimal getTotalHorasExtras() {
        return this.totalHorasExtras;
    }

    public BigDecimal getTotalHorasFeriado() {
        return this.totalHorasFeriado;
    }

    public BigDecimal getTotalHorasNocturnas() {
        return this.totalHorasNocturnas;
    }

    public BigDecimal getTotalHorasTrabajadas() {
        return this.totalHorasTrabajadas;
    }

    public BigDecimal getImporteHorasNormales() {
        return this.importeHorasNormales;
    }

    public BigDecimal getImporteHorasExtras() {
        return this.importeHorasExtras;
    }

    public BigDecimal getImporteHorasFeriado() {
        return this.importeHorasFeriado;
    }

    public BigDecimal getImporteAdicionalNocturno() {
        return this.importeAdicionalNocturno;
    }

    public BigDecimal getSubtotalHaberesHoras() {
        return this.subtotalHaberesHoras;
    }

    public BigDecimal getTotalBonosAdicionales() {
        return this.totalBonosAdicionales;
    }

    public BigDecimal getTotalAdelantosDescontados() {
        return this.totalAdelantosDescontados;
    }

    public BigDecimal getTotalOtrosDescuentos() {
        return this.totalOtrosDescuentos;
    }

    public BigDecimal getNetoACobrar() {
        return this.netoACobrar;
    }

    public String getEstado() {
        return this.estado;
    }

    public String getMedioPago() {
        return this.medioPago;
    }

    public LocalDateTime getFechaPago() {
        return this.fechaPago;
    }

    public String getObservaciones() {
        return this.observaciones;
    }

    public String getLiquidadoPorUsername() {
        return this.liquidadoPorUsername;
    }

    public List<LiquidacionConcepto> getConceptos() {
        return this.conceptos;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNumeroReciboInterno(String numeroReciboInterno) {
        this.numeroReciboInterno = numeroReciboInterno;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public void setFechaLiquidacion(LocalDate fechaLiquidacion) {
        this.fechaLiquidacion = fechaLiquidacion;
    }

    public void setValorHoraBaseAplicado(BigDecimal valorHoraBaseAplicado) {
        this.valorHoraBaseAplicado = valorHoraBaseAplicado;
    }

    public void setOrigenValorHora(String origenValorHora) {
        this.origenValorHora = origenValorHora;
    }

    public void setPorcentajeExtraAplicado(BigDecimal porcentajeExtraAplicado) {
        this.porcentajeExtraAplicado = porcentajeExtraAplicado;
    }

    public void setPorcentajeFeriadoAplicado(BigDecimal porcentajeFeriadoAplicado) {
        this.porcentajeFeriadoAplicado = porcentajeFeriadoAplicado;
    }

    public void setPorcentajeNocturnoAplicado(BigDecimal porcentajeNocturnoAplicado) {
        this.porcentajeNocturnoAplicado = porcentajeNocturnoAplicado;
    }

    public void setTotalHorasNormales(BigDecimal totalHorasNormales) {
        this.totalHorasNormales = totalHorasNormales;
    }

    public void setTotalHorasExtras(BigDecimal totalHorasExtras) {
        this.totalHorasExtras = totalHorasExtras;
    }

    public void setTotalHorasFeriado(BigDecimal totalHorasFeriado) {
        this.totalHorasFeriado = totalHorasFeriado;
    }

    public void setTotalHorasNocturnas(BigDecimal totalHorasNocturnas) {
        this.totalHorasNocturnas = totalHorasNocturnas;
    }

    public void setTotalHorasTrabajadas(BigDecimal totalHorasTrabajadas) {
        this.totalHorasTrabajadas = totalHorasTrabajadas;
    }

    public void setImporteHorasNormales(BigDecimal importeHorasNormales) {
        this.importeHorasNormales = importeHorasNormales;
    }

    public void setImporteHorasExtras(BigDecimal importeHorasExtras) {
        this.importeHorasExtras = importeHorasExtras;
    }

    public void setImporteHorasFeriado(BigDecimal importeHorasFeriado) {
        this.importeHorasFeriado = importeHorasFeriado;
    }

    public void setImporteAdicionalNocturno(BigDecimal importeAdicionalNocturno) {
        this.importeAdicionalNocturno = importeAdicionalNocturno;
    }

    public void setSubtotalHaberesHoras(BigDecimal subtotalHaberesHoras) {
        this.subtotalHaberesHoras = subtotalHaberesHoras;
    }

    public void setTotalBonosAdicionales(BigDecimal totalBonosAdicionales) {
        this.totalBonosAdicionales = totalBonosAdicionales;
    }

    public void setTotalAdelantosDescontados(BigDecimal totalAdelantosDescontados) {
        this.totalAdelantosDescontados = totalAdelantosDescontados;
    }

    public void setTotalOtrosDescuentos(BigDecimal totalOtrosDescuentos) {
        this.totalOtrosDescuentos = totalOtrosDescuentos;
    }

    public void setNetoACobrar(BigDecimal netoACobrar) {
        this.netoACobrar = netoACobrar;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setLiquidadoPorUsername(String liquidadoPorUsername) {
        this.liquidadoPorUsername = liquidadoPorUsername;
    }

    public void setConceptos(List<LiquidacionConcepto> conceptos) {
        this.conceptos = conceptos;
    }

}
