package com.ventaspost.web.dto.venta;

import com.ventaspost.domain.model.CodigoMedioPago;

import java.math.BigDecimal;

public record ConfiguracionMedioPagoResponseDto(
        Long id,
        CodigoMedioPago codigo,
        String nombre,
        BigDecimal porcentajeAjuste,
        boolean requiereReferencia,
        boolean permiteVuelto,
        boolean activo
) {
}
