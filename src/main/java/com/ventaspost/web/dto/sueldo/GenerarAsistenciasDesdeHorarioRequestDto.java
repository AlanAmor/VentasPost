package com.ventaspost.web.dto.sueldo;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GenerarAsistenciasDesdeHorarioRequestDto(
        Long usuarioId, // null = todos los empleados activos

        @NotNull(message = "La fecha desde es obligatoria")
        LocalDate fechaDesde,

        @NotNull(message = "La fecha hasta es obligatoria")
        LocalDate fechaHasta
) {}
