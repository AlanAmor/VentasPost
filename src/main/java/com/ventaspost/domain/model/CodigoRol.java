package com.ventaspost.domain.model;

public enum CodigoRol {
    ADMIN("Administrador General"),
    ENCARGADO_SUCURSAL("Encargado de Sucursal"),
    CAJERO("Cajero"),
    DEPOSITO("Operador de Depósito"),
    LIMPIEZA("Personal de Limpieza / Mantenimiento");

    private final String nombreMostrar;

    CodigoRol(String nombreMostrar) {
        this.nombreMostrar = nombreMostrar;
    }

    public String getNombreMostrar() {
        return nombreMostrar;
    }
}
