package com.ventaspost.domain.model;

public enum TipoMovimientoCaja {
    INGRESO("Ingreso de Efectivo"),
    EGRESO("Egreso / Retiro de Efectivo");

    private final String descripcion;

    TipoMovimientoCaja(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
