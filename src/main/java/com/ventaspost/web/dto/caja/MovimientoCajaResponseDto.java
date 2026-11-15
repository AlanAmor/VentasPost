package com.ventaspost.web.dto.caja;

import com.ventaspost.domain.model.TipoMovimientoCaja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoCajaResponseDto(
        Long id,
        Long turnoCajaId,
        TipoMovimientoCaja tipo,
        String tipoDescripcion,
        BigDecimal monto,
        String motivo,
        String usuarioUsername,
        LocalDateTime fechaMovimiento
) {
}
