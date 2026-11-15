package com.ventaspost.web.dto.venta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularVentaRequestDto(
        @NotBlank(message = "El motivo de anulación es obligatorio")
        @Size(max = 300, message = "El motivo de anulación no puede superar los 300 caracteres")
        String motivoAnulacion,

        @Size(max = 60)
        String supervisorUsername,

        @Size(max = 128)
        String supervisorPassword
) {
}
