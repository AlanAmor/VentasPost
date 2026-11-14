package com.ventaspost.web.dto.auth;

public record SeleccionSucursalRequestDto(
        Long sucursalId,
        Boolean modoConsolidado
) {
}
