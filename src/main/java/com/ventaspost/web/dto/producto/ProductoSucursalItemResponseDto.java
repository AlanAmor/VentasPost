package com.ventaspost.web.dto.producto;

import java.math.BigDecimal;

public record ProductoSucursalItemResponseDto(
        Long sucursalId,
        String sucursalCodigo,
        String sucursalNombre,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        BigDecimal precioVentaSucursal,
        BigDecimal precioVentaEfectivo,
        boolean tienePrecioPersonalizado,
        boolean bajoStock,
        String ubicacionGondola
) {
}
