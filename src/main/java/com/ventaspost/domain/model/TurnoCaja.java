package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "turnos_caja")
public class TurnoCaja extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTurnoCaja estado = EstadoTurnoCaja.ABIERTO;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_inicial_efectivo", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoInicialEfectivo = BigDecimal.ZERO;

    @Column(name = "total_ventas_efectivo", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalVentasEfectivo = BigDecimal.ZERO;

    @Column(name = "total_ventas_transferencia", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalVentasTransferencia = BigDecimal.ZERO;

    @Column(name = "total_ventas_qr", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalVentasQr = BigDecimal.ZERO;

    @Column(name = "total_ventas_debito", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalVentasDebito = BigDecimal.ZERO;

    @Column(name = "total_ventas_credito", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalVentasCredito = BigDecimal.ZERO;

    @Column(name = "total_ingresos_manuales", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalIngresosManuales = BigDecimal.ZERO;

    @Column(name = "total_egresos_manuales", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalEgresosManuales = BigDecimal.ZERO;

    @Column(name = "efectivo_esperado", nullable = false, precision = 19, scale = 2)
    private BigDecimal efectivoEsperado = BigDecimal.ZERO;

    @Column(name = "efectivo_declarado_cierre", precision = 19, scale = 2)
    private BigDecimal efectivoDeclaradoCierre;

    @Column(name = "diferencia_efectivo", precision = 19, scale = 2)
    private BigDecimal diferenciaEfectivo;

    @Column(name = "observaciones_apertura", length = 300)
    private String observacionesApertura;

    @Column(name = "observaciones_cierre", length = 500)
    private String observacionesCierre;

    public BigDecimal recalcularEfectivoEsperado() {
        BigDecimal esperado = montoInicialEfectivo
                .add(totalVentasEfectivo)
                .add(totalIngresosManuales)
                .subtract(totalEgresosManuales);
        this.efectivoEsperado = esperado;
        return esperado;
    }

    public BigDecimal calcularTotalGeneralVentas() {
        return totalVentasEfectivo
                .add(totalVentasTransferencia)
                .add(totalVentasQr)
                .add(totalVentasDebito)
                .add(totalVentasCredito);
    }

    public TurnoCaja() {
    }

    public Long getId() {
        return this.id;
    }

    public Sucursal getSucursal() {
        return this.sucursal;
    }

    public Usuario getUsuario() {
        return this.usuario;
    }

    public EstadoTurnoCaja getEstado() {
        return this.estado;
    }

    public LocalDateTime getFechaApertura() {
        return this.fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return this.fechaCierre;
    }

    public BigDecimal getMontoInicialEfectivo() {
        return this.montoInicialEfectivo;
    }

    public BigDecimal getTotalVentasEfectivo() {
        return this.totalVentasEfectivo;
    }

    public BigDecimal getTotalVentasTransferencia() {
        return this.totalVentasTransferencia;
    }

    public BigDecimal getTotalVentasQr() {
        return this.totalVentasQr;
    }

    public BigDecimal getTotalVentasDebito() {
        return this.totalVentasDebito;
    }

    public BigDecimal getTotalVentasCredito() {
        return this.totalVentasCredito;
    }

    public BigDecimal getTotalIngresosManuales() {
        return this.totalIngresosManuales;
    }

    public BigDecimal getTotalEgresosManuales() {
        return this.totalEgresosManuales;
    }

    public BigDecimal getEfectivoEsperado() {
        return this.efectivoEsperado;
    }

    public BigDecimal getEfectivoDeclaradoCierre() {
        return this.efectivoDeclaradoCierre;
    }

    public BigDecimal getDiferenciaEfectivo() {
        return this.diferenciaEfectivo;
    }

    public String getObservacionesApertura() {
        return this.observacionesApertura;
    }

    public String getObservacionesCierre() {
        return this.observacionesCierre;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setEstado(EstadoTurnoCaja estado) {
        this.estado = estado;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public void setMontoInicialEfectivo(BigDecimal montoInicialEfectivo) {
        this.montoInicialEfectivo = montoInicialEfectivo;
    }

    public void setTotalVentasEfectivo(BigDecimal totalVentasEfectivo) {
        this.totalVentasEfectivo = totalVentasEfectivo;
    }

    public void setTotalVentasTransferencia(BigDecimal totalVentasTransferencia) {
        this.totalVentasTransferencia = totalVentasTransferencia;
    }

    public void setTotalVentasQr(BigDecimal totalVentasQr) {
        this.totalVentasQr = totalVentasQr;
    }

    public void setTotalVentasDebito(BigDecimal totalVentasDebito) {
        this.totalVentasDebito = totalVentasDebito;
    }

    public void setTotalVentasCredito(BigDecimal totalVentasCredito) {
        this.totalVentasCredito = totalVentasCredito;
    }

    public void setTotalIngresosManuales(BigDecimal totalIngresosManuales) {
        this.totalIngresosManuales = totalIngresosManuales;
    }

    public void setTotalEgresosManuales(BigDecimal totalEgresosManuales) {
        this.totalEgresosManuales = totalEgresosManuales;
    }

    public void setEfectivoEsperado(BigDecimal efectivoEsperado) {
        this.efectivoEsperado = efectivoEsperado;
    }

    public void setEfectivoDeclaradoCierre(BigDecimal efectivoDeclaradoCierre) {
        this.efectivoDeclaradoCierre = efectivoDeclaradoCierre;
    }

    public void setDiferenciaEfectivo(BigDecimal diferenciaEfectivo) {
        this.diferenciaEfectivo = diferenciaEfectivo;
    }

    public void setObservacionesApertura(String observacionesApertura) {
        this.observacionesApertura = observacionesApertura;
    }

    public void setObservacionesCierre(String observacionesCierre) {
        this.observacionesCierre = observacionesCierre;
    }

}
