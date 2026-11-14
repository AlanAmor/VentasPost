package com.ventaspost.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioPasswordRequestDto(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String passwordActual,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 6, max = 128, message = "La nueva contraseña debe tener al menos 6 caracteres")
        String passwordNueva,

        @NotBlank(message = "Debe confirmar la nueva contraseña")
        String confirmacionPassword
) {
}
