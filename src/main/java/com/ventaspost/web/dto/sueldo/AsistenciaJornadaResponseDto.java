package com.ventaspost.web.dto.sueldo;

import com.ventaspost.domain.model.EstadoJornadaTrabajo;
import com.ventaspost.domain.model.OrigenRegistroJornada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AsistenciaJornadaResponseDto(
        Long id,
        Long usuarioId,
        String usuarioUsername,
        String usuarioNombreCompleto,
        Long sucursalId,
        String sucursalCodigo,
        String sucursalNombre,
        LocalDate fechaJornada,
        OrigenRegistroJornada origenRegistro,
        String origenRegistroDescripcion,
        EstadoJornadaTrabajo estadoJornada,
        String estadoJornadaDescripcion,
        LocalTime horaEntradaProgramada,
        LocalTime horaSalidaProgramada,
        LocalDateTime fechaHoraIngresoReal,
        LocalDateTime fechaHoraEgresoReal,
        boolean esFeriadoODomingo,
        BigDecimal horasNormales,
        BigDecimal horasExtrasSimples,
        BigDecimal horasFeriadoDomingo,
        BigDecimal horasNocturnas,
        BigDecimal totalHorasComputadas,
        boolean liquidada,
        Long liquidacionId,
        String observaciones
) {}
