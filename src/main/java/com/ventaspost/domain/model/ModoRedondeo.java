package com.ventaspost.domain.model;

public enum ModoRedondeo {
    SIN_REDONDEO("Exacto a 2 decimales (sin redondeo a múltiplo)"),
    MULTIPLO_SUPERIOR("Redondear hacia arriba al múltiplo configurado"),
    MULTIPLO_MAS_CERCANO("Redondear al múltiplo más cercano configurado"),
    MULTIPLO_INFERIOR("Redondear hacia abajo al múltiplo configurado");

    private final String descripcion;

    ModoRedondeo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
