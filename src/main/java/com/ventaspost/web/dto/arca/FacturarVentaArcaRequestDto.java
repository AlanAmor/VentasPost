package com.ventaspost.web.dto.arca;

import com.ventaspost.domain.model.CondicionIva;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FacturarVentaArcaRequestDto(
        @NotNull(message = "La condición frente al IVA del receptor es obligatoria")
        CondicionIva receptorCondicionIva,

        @Size(max = 20, message = "El documento (CUIT/DNI) no puede superar 20 caracteres")
        String receptorDocumento,

        @Size(max = 160, message = "El nombre o razón social del receptor no puede superar 160 caracteres")
        String receptorNombreRazonSocial,

        @Size(max = 220, message = "El domicilio del receptor no puede superar 220 caracteres")
        String receptorDomicilio
) {}
