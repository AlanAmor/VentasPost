package com.ventaspost.web.dto.caja;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CierreCajaRequestDto(
        @NotNull(message = "Debe declarar el efectivo físico contado al cierre")
        @DecimalMin(value = "0.00", message = "El efectivo declarado no puede ser negativo")
        BigDecimal efectivoDeclaradoCierre,

        @Size(max = 500, message = "Las observaciones de cierre no pueden superar los 500 caracteres")
        String observacionesCierre
) {
}
