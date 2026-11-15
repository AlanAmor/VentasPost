package com.ventaspost.web.dto.venta;

import com.ventaspost.domain.model.CondicionIva;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record RegistrarVentaRequestDto(
        @Size(max = 150, message = "El nombre del cliente no puede superar los 150 caracteres")
        String clienteNombre,

        @Size(max = 20, message = "El documento/CUIT del cliente no puede superar los 20 caracteres")
        String clienteDocumento,

        CondicionIva clienteCondicionIva,

        @DecimalMin(value = "0.00", message = "El descuento manual global no puede ser negativo")
        @DecimalMax(value = "100.00", message = "El descuento manual global no puede superar el 100%")
        BigDecimal descuentoManualPorcentaje,

        @Size(max = 60)
        String supervisorUsername,

        @Size(max = 128)
        String supervisorPassword,

        @Size(max = 300, message = "Las observaciones no pueden superar los 300 caracteres")
        String observaciones,

        @NotEmpty(message = "El carrito de venta debe contener al menos un producto")
        @Valid
        List<VentaItemRequestDto> items,

        @NotEmpty(message = "Debe registrar al menos una forma de pago")
        @Valid
        List<VentaPagoRequestDto> pagos
) {
}
