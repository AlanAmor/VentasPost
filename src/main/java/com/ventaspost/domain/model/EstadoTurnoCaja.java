package com.ventaspost.domain.model;

public enum EstadoTurnoCaja {
    ABIERTO("Turno Abierto"),
    CERRADO("Turno Cerrado");

    private final String descripcion;

    EstadoTurnoCaja(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
