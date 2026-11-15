package com.ventaspost.web.dto.arca;

import com.ventaspost.domain.model.CondicionIva;
import com.ventaspost.domain.model.EntornoArca;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfiguracionArcaRequestDto(
        @NotBlank(message = "La Razón Social del emisor es obligatoria")
        @Size(max = 160)
        String razonSocialEmisor,

        @NotBlank(message = "El CUIT del emisor es obligatorio")
        @Pattern(regexp = "^[0-9]{2}-?[0-9]{8}-?[0-9]{1}$", message = "El CUIT debe tener 11 dígitos válidos (con o sin guiones)")
        String cuitEmisor,

        @NotNull(message = "El Punto de Venta ARCA es obligatorio")
        @Min(value = 1, message = "El Punto de Venta debe estar entre 1 y 99999")
        @Max(value = 99999, message = "El Punto de Venta debe estar entre 1 y 99999")
        Integer puntoVentaArca,

        @NotNull(message = "La condición frente al IVA del emisor es obligatoria")
        CondicionIva condicionIvaEmisor,

        @Size(max = 50)
        String ingresosBrutos,

        LocalDate fechaInicioActividades,

        @NotBlank(message = "El domicilio comercial es obligatorio")
        @Size(max = 220)
        String domicilioComercial,

        @NotNull(message = "El entorno ARCA (Homologación o Producción) es obligatorio")
        EntornoArca entornoArca,

        Boolean simuladorHomologacionActivo,

        String certificadoCrtPem,

        String clavePrivadaKeyPem,

        @Size(max = 150)
        String nombreArchivoCrt,

        @Size(max = 150)
        String nombreArchivoKey,

        @NotNull(message = "El monto tope para Consumidor Final sin DNI es obligatorio")
        @DecimalMin(value = "0.00", message = "El monto tope no puede ser negativo")
        BigDecimal montoTopeConsumidorFinalSinDni,

        @NotNull(message = "El plazo máximo de días para facturar es obligatorio")
        @Min(value = 0, message = "El plazo máximo no puede ser negativo (0 = sin límite)")
        @Max(value = 3650)
        Integer plazoMaximoDiasFacturacion,

        Boolean copiarCertificadosASucursalesMismoCuit
) {}
