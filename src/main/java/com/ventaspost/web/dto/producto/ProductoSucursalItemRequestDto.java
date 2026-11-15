package com.ventaspost.web.dto.producto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoSucursalItemRequestDto(
        @NotNull(message = "El ID de sucursal es obligatorio")
        Long sucursalId,

        @NotNull(message = "El stock actual de la sucursal es obligatorio")
        BigDecimal stockActual,

        @NotNull(message = "El stock mínimo de la sucursal es obligatorio")
        @DecimalMin(value = "0.000", message = "El stock mínimo no puede ser negativo")
        BigDecimal stockMinimo,

        @DecimalMin(value = "0.01", message = "El precio personalizado de sucursal debe ser mayor a 0")
        BigDecimal precioVentaSucursal,

        @Size(max = 80, message = "La ubicación en góndola no puede superar los 80 caracteres")
        String ubicacionGondola
) {
}
