package com.ventaspost.domain.model;

public enum EntornoArca {
    HOMOLOGACION(
            "Homologación (Testing ARCA)",
            "https://wsaahomo.afip.gov.ar/ws/services/LoginCms",
            "https://wswhomo.afip.gov.ar/wsfev1/service.asmx"
    ),
    PRODUCCION(
            "Producción (Fiscal Real ARCA)",
            "https://wsaa.afip.gov.ar/ws/services/LoginCms",
            "https://servicios1.afip.gov.ar/wsfev1/service.asmx"
    );

    private final String descripcion;
    private final String wsaaUrl;
    private final String wsfev1Url;

    EntornoArca(String descripcion, String wsaaUrl, String wsfev1Url) {
        this.descripcion = descripcion;
        this.wsaaUrl = wsaaUrl;
        this.wsfev1Url = wsfev1Url;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getWsaaUrl() {
        return wsaaUrl;
    }

    public String getWsfev1Url() {
        return wsfev1Url;
    }
}
