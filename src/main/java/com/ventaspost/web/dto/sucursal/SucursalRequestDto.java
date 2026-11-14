package com.ventaspost.web.dto.sucursal;

import com.ventaspost.domain.model.CondicionIva;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record SucursalRequestDto(
        @NotBlank(message = "El código de sucursal es obligatorio")
        @Size(max = 30, message = "El código no puede superar los 30 caracteres")
        String codigo,

        @NotBlank(message = "El nombre de la sucursal es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String nombre,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social no puede superar los 150 caracteres")
        String razonSocial,

        @NotBlank(message = "El CUIT es obligatorio")
        @Pattern(regexp = "^\\d{2}-?\\d{8}-?\\d{1}$", message = "El CUIT debe tener formato válido (ej. 30-12345678-9 o 11 dígitos)")
        String cuit,

        @NotNull(message = "La condición frente al IVA es obligatoria")
        CondicionIva condicionIva,

        @NotNull(message = "El Punto de Venta ARCA es obligatorio")
        @Min(value = 1, message = "El Punto de Venta ARCA debe ser mayor o igual a 1")
        @Max(value = 99999, message = "El Punto de Venta ARCA no puede superar 99999")
        Integer puntoVentaArca,

        @Size(max = 50, message = "Ingresos Brutos no puede superar los 50 caracteres")
        String ingresosBrutos,

        LocalDate fechaInicioActividades,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
        String direccion,

        @Size(max = 100, message = "La localidad no puede superar los 100 caracteres")
        String localidad,

        @Size(max = 100, message = "La provincia no puede superar los 100 caracteres")
        String provincia,

        @Size(max = 50, message = "El teléfono no puede superar los 50 caracteres")
        String telefono,

        @Email(message = "El formato del email no es válido")
        @Size(max = 120, message = "El email no puede superar los 120 caracteres")
        String email,

        @NotNull(message = "Debe indicar si vende al público")
        Boolean vendeAlPublico,

        @NotNull(message = "Debe indicar si tiene depósito")
        Boolean tieneDeposito,

        @NotNull(message = "Debe indicar si es casa central")
        Boolean esCasaCentral
) {
}
