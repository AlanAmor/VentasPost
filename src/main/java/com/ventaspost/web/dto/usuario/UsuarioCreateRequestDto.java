package com.ventaspost.web.dto.usuario;

import com.ventaspost.domain.model.CodigoRol;
import com.ventaspost.domain.model.PermisoGranular;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Set;

public record UsuarioCreateRequestDto(
        @NotBlank(message = "El nombre de usuario (username) es obligatorio")
        @Size(min = 3, max = 60, message = "El usuario debe tener entre 3 y 60 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El usuario solo puede contener letras, números, puntos, guiones y guiones bajos")
        String username,

        @NotBlank(message = "La contraseña inicial es obligatoria")
        @Size(min = 6, max = 128, message = "La contraseña inicial debe tener al menos 6 caracteres")
        String passwordInicial,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String apellido,

        @NotBlank(message = "El DNI/CUIL es obligatorio")
        @Size(min = 7, max = 20, message = "El DNI/CUIL debe tener entre 7 y 20 caracteres")
        String dni,

        @Size(max = 40, message = "El legajo no puede superar los 40 caracteres")
        String legajo,

        @Email(message = "El formato del email no es válido")
        @Size(max = 120, message = "El email no puede superar los 120 caracteres")
        String email,

        @Size(max = 50, message = "El teléfono no puede superar los 50 caracteres")
        String telefono,

        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
        String direccion,

        LocalDate fechaIngreso,

        Boolean accesoTodasSucursales,

        @NotEmpty(message = "Debe asignar al menos un rol al empleado")
        Set<CodigoRol> roles,

        Set<Long> sucursalIds,

        Set<PermisoGranular> permisosGranulares
) {
}
