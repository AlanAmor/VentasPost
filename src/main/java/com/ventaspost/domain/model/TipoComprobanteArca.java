package com.ventaspost.domain.model;

public enum TipoComprobanteArca {
    FACTURA_A(1, "A", "FACTURA", "Factura A"),
    FACTURA_B(6, "B", "FACTURA", "Factura B"),
    FACTURA_C(11, "C", "FACTURA", "Factura C"),
    NOTA_CREDITO_A(3, "A", "NOTA_CREDITO", "Nota de Crédito A"),
    NOTA_CREDITO_B(8, "B", "NOTA_CREDITO", "Nota de Crédito B"),
    NOTA_CREDITO_C(13, "C", "NOTA_CREDITO", "Nota de Crédito C");

    private final int codigoAfip;
    private final String letra;
    private final String naturaleza;
    private final String descripcion;

    TipoComprobanteArca(int codigoAfip, String letra, String naturaleza, String descripcion) {
        this.codigoAfip = codigoAfip;
        this.letra = letra;
        this.naturaleza = naturaleza;
        this.descripcion = descripcion;
    }

    public int getCodigoAfip() {
        return codigoAfip;
    }

    public String getLetra() {
        return letra;
    }

    public String getNaturaleza() {
        return naturaleza;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public TipoComprobanteArca obtenerNotaCreditoAsociada() {
        return switch (this) {
            case FACTURA_A, NOTA_CREDITO_A -> NOTA_CREDITO_A;
            case FACTURA_B, NOTA_CREDITO_B -> NOTA_CREDITO_B;
            case FACTURA_C, NOTA_CREDITO_C -> NOTA_CREDITO_C;
        };
    }

    public static TipoComprobanteArca determinarFacturaSegunCondicion(
            CondicionIva condicionEmisor,
            CondicionIva condicionReceptor) {
        if (condicionEmisor == CondicionIva.MONOTRIBUTO) {
            return FACTURA_C;
        }
        // Emisor Responsable Inscripto:
        // Según Ley 27.618 / RG ARCA: Emite Factura A a Responsable Inscripto y a Monotributista (con CUIT)
        if (condicionReceptor == CondicionIva.RESPONSABLE_INSCRIPTO
                || condicionReceptor == CondicionIva.MONOTRIBUTO) {
            return FACTURA_A;
        }
        return FACTURA_B;
    }
}
