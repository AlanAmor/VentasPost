package com.ventaspost.web.controller;

import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.domain.model.CodigoRol;
import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.service.ArcaFacturacionService;
import com.ventaspost.web.dto.arca.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/arca")
public class ArcaController {

    private final ArcaFacturacionService arcaFacturacionService;

    @GetMapping("/configuraciones")
    public ResponseEntity<List<ConfiguracionArcaResponseDto>> listarConfiguraciones(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        validarSoloAdmin(userDetails);
        return ResponseEntity.ok(arcaFacturacionService.listarConfiguracionesArca());
    }

    @GetMapping("/configuraciones/sucursal/{sucursalId}")
    public ResponseEntity<ConfiguracionArcaResponseDto> obtenerConfiguracionSucursal(
            @PathVariable Long sucursalId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        validarSoloAdmin(userDetails);
        return ResponseEntity.ok(arcaFacturacionService.obtenerConfiguracionPorSucursal(sucursalId));
    }

    @PutMapping("/configuraciones/sucursal/{sucursalId}")
    public ResponseEntity<ConfiguracionArcaResponseDto> actualizarConfiguracionSucursal(
            @PathVariable Long sucursalId,
            @Valid @RequestBody ConfiguracionArcaRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        validarSoloAdmin(userDetails);
        return ResponseEntity.ok(arcaFacturacionService.actualizarConfiguracionSucursal(sucursalId, dto));
    }

    @PostMapping("/configuraciones/sucursal/{sucursalId}/probar-conexion")
    public ResponseEntity<Map<String, Object>> probarConexionArca(
            @PathVariable Long sucursalId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        validarSoloAdmin(userDetails);
        return ResponseEntity.ok(arcaFacturacionService.probarConexionArca(sucursalId));
    }

    @PostMapping("/ventas/{ventaId}/facturar")
    public ResponseEntity<ComprobanteFiscalArcaResponseDto> facturarVenta(
            @PathVariable Long ventaId,
            @Valid @RequestBody FacturarVentaArcaRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                arcaFacturacionService.facturarVenta(ventaId, dto, userDetails.getUsername())
        );
    }

    @GetMapping("/ventas/{ventaId}/comprobantes")
    public ResponseEntity<List<ComprobanteFiscalArcaResponseDto>> listarComprobantesVenta(
            @PathVariable Long ventaId) {
        return ResponseEntity.ok(arcaFacturacionService.listarComprobantesPorVenta(ventaId));
    }

    @GetMapping("/ventas/{ventaId}/intentos")
    public ResponseEntity<List<IntentoFacturacionArcaResponseDto>> listarIntentosVenta(
            @PathVariable Long ventaId) {
        return ResponseEntity.ok(arcaFacturacionService.listarIntentosPorVenta(ventaId));
    }

    @GetMapping("/comprobantes/{comprobanteId}")
    public ResponseEntity<ComprobanteFiscalArcaResponseDto> obtenerComprobante(
            @PathVariable Long comprobanteId) {
        return ResponseEntity.ok(arcaFacturacionService.obtenerComprobantePorId(comprobanteId));
    }

    private void validarSoloAdmin(CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.tieneRol(CodigoRol.ADMIN)) {
            throw new BusinessException(
                    "Solo el perfil ADMINISTRADOR puede acceder a la Configuración Fiscal ARCA.",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    public ArcaController(ArcaFacturacionService arcaFacturacionService) {
        this.arcaFacturacionService = arcaFacturacionService;
    }

}
