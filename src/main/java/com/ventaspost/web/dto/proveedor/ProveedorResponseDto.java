package com.ventaspost.web.dto.proveedor;

import com.ventaspost.domain.model.CondicionIva;

import java.time.LocalDateTime;

public record ProveedorResponseDto(
        Long id,
        String codigo,
        String razonSocial,
        String nombreFantasia,
        String cuit,
        CondicionIva condicionIva,
        String condicionIvaDescripcion,
        String telefono,
        String email,
        String direccion,
        String localidad,
        String provincia,
        String observaciones,
        boolean esProveedorSistema,
        boolean activo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
