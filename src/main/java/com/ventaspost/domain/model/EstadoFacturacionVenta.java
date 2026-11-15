package com.ventaspost.domain.model;

public enum EstadoFacturacionVenta {
    NO_FACTURADA("Pendiente de Facturación / No Facturada"),
    FACTURADA("Facturada en ARCA (CAE Otorgado)"),
    ERROR_ARCA("Error ARCA - Pendiente de Reintento"),
    ANULADA_NOTA_CREDITO("Anulada con Nota de Crédito ARCA");

    private final String descripcion;

    EstadoFacturacionVenta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
