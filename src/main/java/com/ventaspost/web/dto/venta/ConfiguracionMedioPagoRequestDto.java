package com.ventaspost.web.dto.venta;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConfiguracionMedioPagoRequestDto(
        @NotNull(message = "El porcentaje de ajuste es obligatorio (positivo para recargo, negativo para descuento, 0 para neutro)")
        @DecimalMin(value = "-90.00", message = "El descuento no puede superar el -90%")
        @DecimalMax(value = "200.00", message = "El recargo no puede superar el 200%")
        BigDecimal porcentajeAjuste,

        @NotNull(message = "Debe indicar si requiere referencia de operación")
        Boolean requiereReferencia,

        @NotNull(message = "Debe indicar si el medio de pago está activo")
        Boolean activo
) {
}
