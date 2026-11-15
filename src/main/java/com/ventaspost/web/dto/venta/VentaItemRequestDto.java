package com.ventaspost.web.dto.venta;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record VentaItemRequestDto(
        @NotNull(message = "El ID de producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
        BigDecimal cantidad,

        @DecimalMin(value = "0.00", message = "El descuento de ítem no puede ser negativo")
        @DecimalMax(value = "100.00", message = "El descuento de ítem no puede superar el 100%")
        BigDecimal descuentoItemPorcentaje
) {
}
