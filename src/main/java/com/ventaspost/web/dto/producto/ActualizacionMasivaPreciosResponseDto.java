package com.ventaspost.web.dto.producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActualizacionMasivaPreciosResponseDto(
        int productosActualizados,
        BigDecimal porcentajeAplicado,
        String modoRedondeoAplicado,
        BigDecimal multiploRedondeo,
        LocalDateTime fechaEjecucion
) {
}
