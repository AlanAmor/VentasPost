package com.ventaspost.web.dto.caja;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AperturaCajaRequestDto(
        @NotNull(message = "El monto inicial de efectivo (fondo fijo) es obligatorio")
        @DecimalMin(value = "0.00", message = "El monto inicial de caja no puede ser negativo")
        BigDecimal montoInicialEfectivo,

        @Size(max = 300, message = "Las observaciones no pueden superar los 300 caracteres")
        String observacionesApertura
) {
}
