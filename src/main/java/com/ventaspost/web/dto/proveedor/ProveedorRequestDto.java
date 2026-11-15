package com.ventaspost.web.dto.proveedor;

import com.ventaspost.domain.model.CondicionIva;
import jakarta.validation.constraints.*;

public record ProveedorRequestDto(
        @NotBlank(message = "El código de proveedor es obligatorio")
        @Size(max = 30, message = "El código no puede superar los 30 caracteres")
        String codigo,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social no puede superar los 150 caracteres")
        String razonSocial,

        @Size(max = 150, message = "El nombre de fantasía no puede superar los 150 caracteres")
        String nombreFantasia,

        @Pattern(regexp = "^$|^\\d{2}-?\\d{8}-?\\d{1}$", message = "El CUIT debe tener formato válido (11 dígitos) o dejarse vacío")
        String cuit,

        @NotNull(message = "La condición frente al IVA es obligatoria")
        CondicionIva condicionIva,

        @Size(max = 60, message = "El teléfono no puede superar los 60 caracteres")
        String telefono,

        @Email(message = "El formato del email no es válido")
        @Size(max = 120, message = "El email no puede superar los 120 caracteres")
        String email,

        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
        String direccion,

        @Size(max = 100, message = "La localidad no puede superar los 100 caracteres")
        String localidad,

        @Size(max = 100, message = "La provincia no puede superar los 100 caracteres")
        String provincia,

        @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
        String observaciones
) {
}
