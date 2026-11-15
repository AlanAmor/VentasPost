package com.ventaspost.web.dto.venta;

import com.ventaspost.domain.model.CodigoMedioPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VentaPagoRequestDto(
        @NotNull(message = "El medio de pago es obligatorio")
        CodigoMedioPago medioPago,

        @NotNull(message = "El monto base imputado al medio de pago es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto base del pago debe ser mayor a 0")
        BigDecimal montoBase,

        @DecimalMin(value = "0.01", message = "El monto recibido en efectivo debe ser mayor a 0")
        BigDecimal montoRecibidoEfectivo,

        @Min(value = 1, message = "La cantidad de cuotas debe ser al menos 1")
        Integer cuotas,

        @Size(max = 100, message = "La referencia de operación no puede superar los 100 caracteres")
        String referenciaOperacion
) {
}
