package com.ventaspost.domain.model;

public enum CondicionIva {
    RESPONSABLE_INSCRIPTO("IVA Responsable Inscripto"),
    MONOTRIBUTO("Responsable Monotributo"),
    EXENTO("IVA Sujeto Exento"),
    CONSUMIDOR_FINAL("Consumidor Final"),
    NO_RESPONSABLE("IVA No Responsable");

    private final String descripcion;

    CondicionIva(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
