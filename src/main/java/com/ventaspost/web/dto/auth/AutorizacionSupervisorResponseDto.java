package com.ventaspost.web.dto.auth;

import java.time.LocalDateTime;

public record AutorizacionSupervisorResponseDto(
        boolean autorizado,
        Long supervisorId,
        String supervisorUsername,
        String supervisorNombreCompleto,
        String permisoAutorizado,
        Long sucursalId,
        LocalDateTime fechaHoraAutorizacion
) {
}
