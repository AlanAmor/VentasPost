package com.ventaspost.web.dto.sueldo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PreliquidacionSueldoResponseDto(
        Long usuarioId,
        String usuarioUsername,
        String usuarioNombreCompleto,
        String usuarioDni,
        String usuarioLegajo,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
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
        BigDecimal totalAdelantosPendientes,
        BigDecimal netoSugeridoInicial,
        List<AsistenciaJornadaResponseDto> jornadasIncluidas,
        List<AdelantoSueldoResponseDto> adelantosPendientes
) {}
