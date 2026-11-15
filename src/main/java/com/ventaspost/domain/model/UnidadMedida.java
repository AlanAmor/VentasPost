package com.ventaspost.domain.model;

public enum UnidadMedida {
    UNIDAD("Unidad", "u.", false),
    KG("Kilogramo", "Kg", true),
    GRAMO("Gramo", "g", true),
    LITRO("Litro", "L", true),
    METRO("Metro", "m", true),
    PAR("Par", "par", false),
    CAJA("Caja / Pack", "caja", false);

    private final String nombre;
    private final String abreviatura;
    private final boolean permiteDecimales;

    UnidadMedida(String nombre, String abreviatura, boolean permiteDecimales) {
        this.nombre = nombre;
        this.abreviatura = abreviatura;
        this.permiteDecimales = permiteDecimales;
    }

    public String getNombre() {
        return nombre;
    }

    public String getAbreviatura() {
        return abreviatura;
    }

    public boolean isPermiteDecimales() {
        return permiteDecimales;
    }
}
