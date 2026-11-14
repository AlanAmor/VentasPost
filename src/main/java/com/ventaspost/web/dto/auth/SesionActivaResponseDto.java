package com.ventaspost.web.dto.auth;

import java.util.List;
import java.util.Set;

public record SesionActivaResponseDto(
        Long usuarioId,
        String username,
        String nombreCompleto,
        Set<String> roles,
        Set<String> permisos,
        boolean accesoTodasSucursales,
        boolean puedeVerConsolidado,
        boolean modoConsolidado,
        boolean requiereCambioPassword,
        boolean requiereSeleccionSucursal,
        SucursalResumenDto sucursalActiva,
        List<SucursalResumenDto> sucursalesDisponibles
) {
}
