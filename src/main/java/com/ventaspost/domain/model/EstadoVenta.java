package com.ventaspost.domain.model;

public enum EstadoVenta {
    REALIZADA("Realizada"),
    ANULADA("Anulada");

    private final String descripcion;

    EstadoVenta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
