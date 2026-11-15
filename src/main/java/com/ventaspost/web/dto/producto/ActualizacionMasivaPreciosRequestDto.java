package com.ventaspost.web.dto.producto;

import com.ventaspost.domain.model.ModoRedondeo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ActualizacionMasivaPreciosRequestDto(
        Long proveedorId,
        Long categoriaId,
        Long marcaId,

        @NotNull(message = "El porcentaje de variación es obligatorio")
        BigDecimal porcentajeVariacion,

        @NotNull(message = "Debe indicar si el porcentaje se aplica sobre el costo y/o el precio de venta")
        Boolean actualizarTambienPrecioCompra,

        @NotNull(message = "El modo de redondeo es obligatorio")
        ModoRedondeo modoRedondeo,

        @DecimalMin(value = "0.01", message = "El múltiplo de redondeo debe ser mayor a 0")
        BigDecimal multiploRedondeo,

        Boolean aplicarPorcentajeAPreciosPersonalizadosSucursal,

        Boolean restablecerPreciosPersonalizadosSucursal
) {
}
