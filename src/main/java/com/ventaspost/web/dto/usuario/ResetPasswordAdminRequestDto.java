package com.ventaspost.web.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordAdminRequestDto(
        @NotBlank(message = "La nueva contraseña temporal es obligatoria")
        @Size(min = 6, max = 128, message = "La contraseña debe tener al menos 6 caracteres")
        String nuevaPasswordTemporal
) {
}
