package com.ventaspost.web.dto.auth;

import com.ventaspost.domain.model.PermisoGranular;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AutorizacionSupervisorRequestDto(
        @NotBlank(message = "El usuario del supervisor es obligatorio")
        String usernameSupervisor,

        @NotBlank(message = "La contraseña del supervisor es obligatoria")
        String passwordSupervisor,

        @NotNull(message = "Debe especificar el permiso requerido para la operación")
        PermisoGranular permisoRequerido,

        String motivoOperacion
) {
}
