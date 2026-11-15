package com.ventaspost.web.dto.caja;

import com.ventaspost.domain.model.EstadoTurnoCaja;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TurnoCajaResponseDto(
        Long id,
        Long sucursalId,
        String sucursalCodigo,
        String sucursalNombre,
        Long usuarioId,
        String usuarioUsername,
        String usuarioNombreCompleto,
        EstadoTurnoCaja estado,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,
        BigDecimal montoInicialEfectivo,
        BigDecimal totalVentasEfectivo,
        BigDecimal totalVentasTransferencia,
        BigDecimal totalVentasQr,
        BigDecimal totalVentasDebito,
        BigDecimal totalVentasCredito,
        BigDecimal totalGeneralVentas,
        BigDecimal totalIngresosManuales,
        BigDecimal totalEgresosManuales,
        BigDecimal efectivoEsperado,
        BigDecimal efectivoDeclaradoCierre,
        BigDecimal diferenciaEfectivo,
        String observacionesApertura,
        String observacionesCierre,
        List<MovimientoCajaResponseDto> movimientos
) {
}
