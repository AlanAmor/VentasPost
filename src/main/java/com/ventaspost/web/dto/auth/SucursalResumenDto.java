package com.ventaspost.web.dto.auth;

public record SucursalResumenDto(
        Long id,
        String codigo,
        String nombre,
        Integer puntoVentaArca,
        boolean esCasaCentral
) {
}
