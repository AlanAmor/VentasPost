package com.ventaspost.domain.model;

public enum EstadoJornadaTrabajo {
    EN_CURSO("En Curso (Ingreso Fichado)"),
    TRABAJADA("Jornada Trabajada"),
    INASISTENCIA_JUSTIFICADA("Inasistencia Justificada"),
    INASISTENCIA_INJUSTIFICADA("Inasistencia Injustificada / Ausente");

    private final String descripcion;

    EstadoJornadaTrabajo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
