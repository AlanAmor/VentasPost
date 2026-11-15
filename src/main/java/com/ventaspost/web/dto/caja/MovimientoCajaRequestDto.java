package com.ventaspost.web.dto.caja;

import com.ventaspost.domain.model.TipoMovimientoCaja;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MovimientoCajaRequestDto(
        @NotNull(message = "El tipo de movimiento (INGRESO / EGRESO) es obligatorio")
        TipoMovimientoCaja tipo,

        @NotNull(message = "El monto del movimiento es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto del movimiento debe ser mayor a 0")
        BigDecimal monto,

        @NotBlank(message = "El motivo del movimiento de caja es obligatorio")
        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
        String motivo
) {
}
