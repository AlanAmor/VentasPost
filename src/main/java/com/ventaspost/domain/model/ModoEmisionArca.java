package com.ventaspost.domain.model;

public enum ModoEmisionArca {
    WSFEV1_REAL("Web Service Real ARCA (WSAA + WSFEv1)"),
    HOMOLOGACION_SIMULADA("Homologación Asistida Local (Testing)");

    private final String descripcion;

    ModoEmisionArca(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
