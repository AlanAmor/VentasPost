package com.ventaspost.web.dto.sueldo;

import com.ventaspost.domain.model.TipoConceptoLiquidacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmitirLiquidacionSueldoRequestDto(
        @NotNull(message = "El empleado es obligatorio")
        Long usuarioId,

        @NotNull(message = "La sucursal de imputación es obligatoria")
        Long sucursalId,

        @NotNull(message = "La fecha desde del período es obligatoria")
        LocalDate fechaDesde,

        @NotNull(message = "La fecha hasta del período es obligatoria")
        LocalDate fechaHasta,

        LocalDate fechaLiquidacion,

        @NotBlank(message = "El medio de pago es obligatorio")
        @Size(max = 40)
        String medioPago,

        Boolean descontarAdelantosPendientes,

        @Valid
        List<ConceptoManualRequestDto> conceptosManuales,

        @Size(max = 400)
        String observaciones
) {
    public record ConceptoManualRequestDto(
            @NotNull TipoConceptoLiquidacion tipoConcepto,
            @NotBlank @Size(max = 200) String descripcion,
            @NotNull @DecimalMin("0.01") BigDecimal monto
    ) {}
}
