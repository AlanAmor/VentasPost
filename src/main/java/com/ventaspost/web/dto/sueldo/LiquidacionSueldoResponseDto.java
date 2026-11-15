package com.ventaspost.web.dto.sueldo;

import com.ventaspost.domain.model.TipoConceptoLiquidacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LiquidacionSueldoResponseDto(
        Long id,
        String numeroReciboInterno,
        Long usuarioId,
        String usuarioUsername,
        String usuarioNombreCompleto,
        String usuarioDni,
        String usuarioLegajo,
        Long sucursalId,
        String sucursalCodigo,
        String sucursalNombre,
        String sucursalRazonSocial,
        String sucursalCuit,
        String sucursalDireccion,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        LocalDate fechaLiquidacion,
        BigDecimal valorHoraBaseAplicado,
        String origenValorHora,
        BigDecimal porcentajeExtraAplicado,
        BigDecimal porcentajeFeriadoAplicado,
        BigDecimal porcentajeNocturnoAplicado,
        BigDecimal totalHorasNormales,
        BigDecimal totalHorasExtras,
        BigDecimal totalHorasFeriado,
        BigDecimal totalHorasNocturnas,
        BigDecimal totalHorasTrabajadas,
        BigDecimal importeHorasNormales,
        BigDecimal importeHorasExtras,
        BigDecimal importeHorasFeriado,
        BigDecimal importeAdicionalNocturno,
        BigDecimal subtotalHaberesHoras,
        BigDecimal totalBonosAdicionales,
        BigDecimal totalAdelantosDescontados,
        BigDecimal totalOtrosDescuentos,
        BigDecimal netoACobrar,
        String estado,
        String medioPago,
        LocalDateTime fechaPago,
        String observaciones,
        String liquidadoPorUsername,
        List<ConceptoLiquidacionResponseDto> conceptos,
        List<AsistenciaJornadaResponseDto> jornadasLiquidadas
) {
    public record ConceptoLiquidacionResponseDto(
            Long id,
            TipoConceptoLiquidacion tipoConcepto,
            String tipoConceptoDescripcion,
            String descripcion,
            BigDecimal monto,
            Long adelantoId
    ) {}
}
