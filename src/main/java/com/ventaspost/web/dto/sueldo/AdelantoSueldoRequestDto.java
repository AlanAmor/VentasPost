package com.ventaspost.web.dto.sueldo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdelantoSueldoRequestDto(
        @NotNull(message = "Debe seleccionar el empleado")
        Long usuarioId,

        @NotNull(message = "Debe seleccionar la sucursal")
        Long sucursalId,

        @NotNull(message = "La fecha del adelanto es obligatoria")
        LocalDate fechaAdelanto,

        @NotNull(message = "El monto del adelanto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto del adelanto debe ser mayor a 0")
        BigDecimal monto,

        @NotBlank(message = "El medio de pago es obligatorio")
        @Size(max = 40)
        String medioPago,

        @NotBlank(message = "El motivo/observación del adelanto es obligatorio")
        @Size(max = 255)
        String motivo
) {}
