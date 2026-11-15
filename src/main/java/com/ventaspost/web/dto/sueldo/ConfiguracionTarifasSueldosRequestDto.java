package com.ventaspost.web.dto.sueldo;

import com.ventaspost.domain.model.CodigoRol;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record ConfiguracionTarifasSueldosRequestDto(
        @NotNull(message = "El porcentaje de recargo por hora extra simple es obligatorio")
        @DecimalMin(value = "0.00")
        BigDecimal porcentajeRecargoExtraSimple,

        @NotNull(message = "El porcentaje de recargo por feriado/domingo es obligatorio")
        @DecimalMin(value = "0.00")
        BigDecimal porcentajeRecargoFeriadoDomingo,

        @NotNull(message = "El porcentaje de recargo por hora nocturna es obligatorio")
        @DecimalMin(value = "0.00")
        BigDecimal porcentajeRecargoNocturno,

        LocalTime horaInicioNocturno,

        LocalTime horaFinNocturno,

        List<TarifaRolDto> tarifasPorRol,

        List<TarifaEmpleadoDto> tarifasPersonalizadasEmpleados
) {
    public record TarifaRolDto(
            @NotNull CodigoRol codigoRol,
            @NotNull @DecimalMin("0.01") BigDecimal valorHoraGeneral
    ) {}

    public record TarifaEmpleadoDto(
            @NotNull Long usuarioId,
            BigDecimal valorHoraPersonalizado
    ) {}
}
