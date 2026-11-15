package com.ventaspost.web.dto.producto;

public record MaestroSimpleResponseDto(
        Long id,
        String nombre,
        String descripcion,
        boolean activo
) {
}
