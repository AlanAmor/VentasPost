package com.ventaspost.domain.model;

public enum CondicionIva {
    RESPONSABLE_INSCRIPTO("IVA Responsable Inscripto", 1),
    MONOTRIBUTO("Responsable Monotributo", 6),
    EXENTO("IVA Sujeto Exento", 4),
    CONSUMIDOR_FINAL("Consumidor Final", 5),
    NO_RESPONSABLE("IVA No Responsable", 7);

    private final String descripcion;
    private final int codigoAfipReceptor;

    CondicionIva(String descripcion, int codigoAfipReceptor) {
        this.descripcion = descripcion;
        this.codigoAfipReceptor = codigoAfipReceptor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getCodigoAfipReceptor() {
        return codigoAfipReceptor;
    }
}
