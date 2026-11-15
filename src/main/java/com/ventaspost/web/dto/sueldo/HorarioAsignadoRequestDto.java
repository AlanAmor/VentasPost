package com.ventaspost.web.dto.sueldo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record HorarioAsignadoRequestDto(
        @NotNull(message = "Debe seleccionar el empleado")
        Long usuarioId,

        @NotNull(message = "Debe seleccionar la sucursal")
        Long sucursalId,

        @NotNull(message = "Debe indicar el día de la semana (1=Lunes .. 7=Domingo)")
        @Min(1)
        @Max(7)
        Integer diaSemana,

        @NotNull(message = "La hora de entrada es obligatoria")
        LocalTime horaEntrada,

        @NotNull(message = "La hora de salida es obligatoria")
        LocalTime horaSalida,

        @Size(max = 255)
        String observaciones
) {}
