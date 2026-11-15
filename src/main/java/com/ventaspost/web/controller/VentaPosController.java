package com.ventaspost.web.controller;

import com.ventaspost.domain.model.EstadoFacturacionVenta;
import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.security.SucursalSessionManager;
import com.ventaspost.service.VentaPosService;
import com.ventaspost.web.dto.venta.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ventas")
public class VentaPosController {

    private final VentaPosService ventaPosService;
    private final SucursalSessionManager sucursalSessionManager;

    @GetMapping("/medios-pago")
    public ResponseEntity<List<ConfiguracionMedioPagoResponseDto>> listarMediosPago(
            @RequestParam(defaultValue = "false") boolean soloActivos) {
        return ResponseEntity.ok(ventaPosService.listarMediosPago(soloActivos));
    }

    @PutMapping("/medios-pago/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionMedioPagoResponseDto> actualizarMedioPago(
            @PathVariable Long id,
            @Valid @RequestBody ConfiguracionMedioPagoRequestDto dto) {
        return ResponseEntity.ok(ventaPosService.actualizarMedioPago(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<VentaResponseDto>> listarVentas(
            @RequestParam(required = false) Long turnoCajaId,
            @RequestParam(required = false) EstadoFacturacionVenta estadoFacturacion,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {

        Long sucursalContextoId = sucursalSessionManager.esModoConsolidado(session)
                ? null
                : sucursalSessionManager.obtenerSucursalActivaId(session).orElse(null);

        return ResponseEntity.ok(ventaPosService.listarVentas(
                sucursalContextoId, turnoCajaId, estadoFacturacion, operador
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponseDto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ventaPosService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<VentaResponseDto> registrarVenta(
            @Valid @RequestBody RegistrarVentaRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {

        Long sucursalActivaId = sucursalSessionManager.esModoConsolidado(session)
                ? null
                : sucursalSessionManager.obtenerSucursalActivaId(session).orElse(null);

        VentaResponseDto creada = ventaPosService.registrarVenta(dto, sucursalActivaId, operador);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<VentaResponseDto> anularVenta(
            @PathVariable Long id,
            @Valid @RequestBody AnularVentaRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador) {
        return ResponseEntity.ok(ventaPosService.anularVenta(id, dto, operador));
    }

    public VentaPosController(VentaPosService ventaPosService, SucursalSessionManager sucursalSessionManager) {
        this.ventaPosService = ventaPosService;
        this.sucursalSessionManager = sucursalSessionManager;
    }

}
