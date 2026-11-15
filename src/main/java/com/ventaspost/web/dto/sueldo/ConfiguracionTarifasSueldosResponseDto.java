package com.ventaspost.web.dto.sueldo;

import com.ventaspost.domain.model.CodigoRol;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ConfiguracionTarifasSueldosResponseDto(
        BigDecimal porcentajeRecargoExtraSimple,
        BigDecimal porcentajeRecargoFeriadoDomingo,
        BigDecimal porcentajeRecargoNocturno,
        LocalTime horaInicioNocturno,
        LocalTime horaFinNocturno,
        List<RolTarifaResponseDto> tarifasPorRol,
        List<EmpleadoTarifaResponseDto> empleados
) {
    public record RolTarifaResponseDto(
            Long id,
            CodigoRol codigo,
            String nombre,
            BigDecimal valorHoraGeneral
    ) {}

    public record EmpleadoTarifaResponseDto(
            Long usuarioId,
            String username,
            String nombreCompleto,
            String legajo,
            List<String> roles,
            BigDecimal valorHoraPersonalizado,
            BigDecimal valorHoraEfectivo,
            String origenValorHora,
            LocalDate fechaUltimaLiquidacion,
            LocalDate fechaSugeridaProximaDesde
    ) {}
}
