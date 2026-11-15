package com.ventaspost.web.dto.sueldo;

import com.ventaspost.domain.model.EstadoJornadaTrabajo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record AsistenciaJornadaRequestDto(
        @NotNull(message = "El empleado es obligatorio")
        Long usuarioId,

        @NotNull(message = "La sucursal es obligatoria")
        Long sucursalId,

        @NotNull(message = "La fecha de la jornada es obligatoria")
        LocalDate fechaJornada,

        @NotNull(message = "El estado de la jornada es obligatorio")
        EstadoJornadaTrabajo estadoJornada,

        LocalTime horaEntradaProgramada,

        LocalTime horaSalidaProgramada,

        Boolean esFeriadoODomingo,

        @DecimalMin("0.00")
        BigDecimal horasNormales,

        @DecimalMin("0.00")
        BigDecimal horasExtrasSimples,

        @DecimalMin("0.00")
        BigDecimal horasFeriadoDomingo,

        @DecimalMin("0.00")
        BigDecimal horasNocturnas,

        @Size(max = 300)
        String observaciones
) {}
