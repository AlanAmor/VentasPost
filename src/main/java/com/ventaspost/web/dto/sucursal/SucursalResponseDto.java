package com.ventaspost.web.dto.sucursal;

import com.ventaspost.domain.model.CondicionIva;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SucursalResponseDto(
        Long id,
        String codigo,
        String nombre,
        String razonSocial,
        String cuit,
        CondicionIva condicionIva,
        String condicionIvaDescripcion,
        Integer puntoVentaArca,
        String ingresosBrutos,
        LocalDate fechaInicioActividades,
        String direccion,
        String localidad,
        String provincia,
        String telefono,
        String email,
        boolean vendeAlPublico,
        boolean tieneDeposito,
        boolean esCasaCentral,
        boolean activo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
