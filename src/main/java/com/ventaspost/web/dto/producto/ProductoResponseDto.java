package com.ventaspost.web.dto.producto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ventaspost.domain.model.UnidadMedida;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductoResponseDto(
        Long id,
        String codigoBarras,
        String codigoProveedorSku,
        boolean esCodigoInterno,
        String nombre,
        String descripcion,
        Long categoriaId,
        String categoriaNombre,
        Long marcaId,
        String marcaNombre,
        Long proveedorId,
        String proveedorRazonSocial,
        UnidadMedida unidadMedida,
        String unidadMedidaAbreviatura,
        boolean permiteDecimales,
        // Regla 4.3: Estos tres campos de costo/margen se envían null si el usuario no tiene permiso de precios/costos
        BigDecimal precioCompra,
        Boolean ivaIncluidoCompra,
        BigDecimal costoFinalConIva,
        BigDecimal porcentajeGanancia,
        BigDecimal alicuotaIva,
        BigDecimal precioVentaGeneral,
        BigDecimal stockMinimoDefecto,
        // Datos contextuales según la sucursal activa (o consolidado)
        BigDecimal stockContextoActual,
        BigDecimal stockContextoMinimo,
        BigDecimal precioVentaContextoEfectivo,
        boolean tienePrecioPersonalizadoEnContexto,
        boolean alertaBajoStock,
        boolean activo,
        List<ProductoSucursalItemResponseDto> sucursales,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
