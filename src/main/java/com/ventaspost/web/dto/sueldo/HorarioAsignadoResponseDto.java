package com.ventaspost.web.dto.sueldo;

import java.math.BigDecimal;
import java.time.LocalTime;

public record HorarioAsignadoResponseDto(
        Long id,
        Long usuarioId,
        String usuarioUsername,
        String usuarioNombreCompleto,
        Long sucursalId,
        String sucursalCodigo,
        String sucursalNombre,
        Integer diaSemana,
        String diaSemanaNombre,
        LocalTime horaEntrada,
        LocalTime horaSalida,
        BigDecimal horasTeoricas,
        String observaciones,
        boolean activo
) {}
