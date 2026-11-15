package com.ventaspost.web.dto.arca;

import com.ventaspost.domain.model.CondicionIva;
import com.ventaspost.domain.model.EntornoArca;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConfiguracionArcaResponseDto(
        Long id,
        Long sucursalId,
        String sucursalCodigo,
        String sucursalNombre,
        String razonSocialEmisor,
        String cuitEmisor,
        Integer puntoVentaArca,
        CondicionIva condicionIvaEmisor,
        String condicionIvaEmisorDescripcion,
        String ingresosBrutos,
        LocalDate fechaInicioActividades,
        String domicilioComercial,
        EntornoArca entornoArca,
        String entornoArcaDescripcion,
        boolean simuladorHomologacionActivo,
        boolean poseeCertificadoCargado,
        boolean poseeClavePrivadaCargada,
        String nombreArchivoCrt,
        String nombreArchivoKey,
        String certificadoSubjectResumen,
        String certificadoVencimientoResumen,
        BigDecimal montoTopeConsumidorFinalSinDni,
        Integer plazoMaximoDiasFacturacion,
        boolean poseeTicketAccesoWsaaVigente,
        LocalDateTime wsaaExpiracionCache,
        boolean activo
) {}
