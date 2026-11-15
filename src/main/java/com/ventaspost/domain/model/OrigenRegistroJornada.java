package com.ventaspost.domain.model;

public enum OrigenRegistroJornada {
    FICHAJE_RELOJ("Fichaje de Ingreso / Egreso"),
    HORARIO_TEORICO("Generado desde Horario Asignado"),
    CARGA_MANUAL("Carga / Ajuste Manual");

    private final String descripcion;

    OrigenRegistroJornada(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
