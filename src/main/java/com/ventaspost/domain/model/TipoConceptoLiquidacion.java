package com.ventaspost.domain.model;

public enum TipoConceptoLiquidacion {
    HABER_ADICIONAL("Bono / Premio / Adicional (+)"),
    DESCUENTO_ADELANTO("Descuento de Adelanto de Sueldo (-)"),
    DESCUENTO_MANUAL("Descuento / Retención Manual (-)");

    private final String descripcion;

    TipoConceptoLiquidacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
