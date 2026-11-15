package com.ventaspost.web.dto.producto;

import com.ventaspost.domain.model.UnidadMedida;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ProductoRequestDto(
        @Size(max = 60, message = "El código de barras no puede superar los 60 caracteres")
        String codigoBarras,

        Boolean generarCodigoInterno,

        @Size(max = 60, message = "El código SKU del proveedor no puede superar los 60 caracteres")
        String codigoProveedorSku,

        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 180, message = "El nombre no puede superar los 180 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @NotNull(message = "La marca es obligatoria")
        Long marcaId,

        Long proveedorId,

        @NotNull(message = "La unidad de medida es obligatoria")
        UnidadMedida unidadMedida,

        @NotNull(message = "El precio de compra es obligatorio")
        @DecimalMin(value = "0.0000", message = "El precio de compra no puede ser negativo")
        BigDecimal precioCompra,

        @NotNull(message = "Debe indicar si el precio de compra incluye IVA")
        Boolean ivaIncluidoCompra,

        @NotNull(message = "La alícuota de IVA es obligatoria")
        @DecimalMin(value = "0.00", message = "La alícuota de IVA no puede ser negativa")
        @DecimalMax(value = "100.00", message = "La alícuota de IVA no puede superar el 100%")
        BigDecimal alicuotaIva,

        @NotNull(message = "El porcentaje de ganancia es obligatorio")
        BigDecimal porcentajeGanancia,

        @NotNull(message = "El precio de venta general es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor a 0")
        BigDecimal precioVentaGeneral,

        @NotNull(message = "El stock mínimo por defecto del producto es obligatorio")
        @DecimalMin(value = "0.000", message = "El stock mínimo no puede ser negativo")
        BigDecimal stockMinimoDefecto,

        Boolean actualizarPreciosSucursalProporcional,

        Boolean restablecerPreciosSucursalAlGeneral,

        @Valid
        List<ProductoSucursalItemRequestDto> sucursalesConfig
) {
}
