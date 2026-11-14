package com.ventaspost.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(max = 60, message = "El usuario no puede superar los 60 caracteres")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 128, message = "La contraseña excede la longitud máxima permitida")
        String password
) {
}
