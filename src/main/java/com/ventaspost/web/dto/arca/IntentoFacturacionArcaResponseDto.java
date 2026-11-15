package com.ventaspost.web.dto.arca;

import com.ventaspost.domain.model.EntornoArca;
import com.ventaspost.domain.model.ModoEmisionArca;

import java.time.LocalDateTime;

public record IntentoFacturacionArcaResponseDto(
        Long id,
        Long ventaId,
        Long sucursalId,
        LocalDateTime fechaIntento,
        String naturalezaOperacion,
        EntornoArca entornoArca,
        ModoEmisionArca modoEmision,
        boolean exitoso,
        String codigoRespuesta,
        String mensajeDetalle,
        String caeObtenido,
        String numeroComprobanteObtenido,
        String usuarioUsername
) {}
