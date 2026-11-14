package com.ventaspost.web.dto.usuario;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record CatalogoRolesPermisosDto(
        List<RolItemDto> rolesDisponibles,
        List<PermisoItemDto> permisosDisponibles,
        Map<String, Set<String>> permisosPorDefectoPorRol
) {
    public record RolItemDto(
            String codigo,
            String nombre,
            String descripcion
    ) {}

    public record PermisoItemDto(
            String codigo,
            String descripcion,
            boolean exclusivoAdmin
    ) {}
}
