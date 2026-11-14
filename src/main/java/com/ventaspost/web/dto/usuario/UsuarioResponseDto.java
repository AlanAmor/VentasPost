package com.ventaspost.web.dto.usuario;

import com.ventaspost.web.dto.auth.SucursalResumenDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record UsuarioResponseDto(
        Long id,
        String username,
        String nombre,
        String apellido,
        String dni,
        String legajo,
        String email,
        String telefono,
        String direccion,
        LocalDate fechaIngreso,
        boolean accesoTodasSucursales,
        boolean requiereCambioPassword,
        int intentosFallidos,
        boolean cuentaBloqueada,
        LocalDateTime fechaBloqueo,
        LocalDateTime ultimoAcceso,
        boolean activo,
        Set<String> roles,
        Set<String> permisosGranulares,
        List<SucursalResumenDto> sucursalesHabilitadas,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
