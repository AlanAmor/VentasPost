package com.ventaspost.web.dto.sueldo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdelantoSueldoResponseDto(
        Long id,
        Long usuarioId,
        String usuarioUsername,
        String usuarioNombreCompleto,
        Long sucursalId,
        String sucursalNombre,
        LocalDate fechaAdelanto,
        BigDecimal monto,
        String medioPago,
        String motivo,
        boolean descontado,
        Long liquidacionId,
        String registradoPorUsername
) {}
