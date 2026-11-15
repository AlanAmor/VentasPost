package com.ventaspost.web.controller;

import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.security.SucursalSessionManager;
import com.ventaspost.service.CajaService;
import com.ventaspost.web.dto.caja.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/cajas")
public class CajaController {

    private final CajaService cajaService;
    private final SucursalSessionManager sucursalSessionManager;

    @GetMapping("/turno-actual")
    public ResponseEntity<Map<String, Object>> obtenerTurnoActual(
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {

        Long sucursalActivaId = sucursalSessionManager.esModoConsolidado(session)
                ? null
                : sucursalSessionManager.obtenerSucursalActivaId(session).orElse(null);

        Optional<TurnoCajaResponseDto> turnoOpt = cajaService.obtenerTurnoAbiertoActual(
                sucursalActivaId, operador.getUsuarioId()
        );

        return ResponseEntity.ok(Map.of(
                "tieneTurnoAbierto", turnoOpt.isPresent(),
                "turno", turnoOpt.orElse(null) != null ? turnoOpt.get() : Map.of()
        ));
    }

    @GetMapping("/turnos")
    public ResponseEntity<List<TurnoCajaResponseDto>> listarTurnos(
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {

        Long sucursalContextoId = sucursalSessionManager.esModoConsolidado(session)
                ? null
                : sucursalSessionManager.obtenerSucursalActivaId(session).orElse(null);

        return ResponseEntity.ok(cajaService.listarTurnos(sucursalContextoId, operador));
    }

    @PostMapping("/abrir")
    public ResponseEntity<TurnoCajaResponseDto> abrirTurno(
            @Valid @RequestBody AperturaCajaRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {

        Long sucursalActivaId = sucursalSessionManager.esModoConsolidado(session)
                ? null
                : sucursalSessionManager.obtenerSucursalActivaId(session).orElse(null);

        TurnoCajaResponseDto creado = cajaService.abrirTurno(dto, sucursalActivaId, operador);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PostMapping("/movimientos")
    public ResponseEntity<TurnoCajaResponseDto> registrarMovimiento(
            @Valid @RequestBody MovimientoCajaRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {

        Long sucursalActivaId = sucursalSessionManager.esModoConsolidado(session)
                ? null
                : sucursalSessionManager.obtenerSucursalActivaId(session).orElse(null);

        return ResponseEntity.ok(cajaService.registrarMovimientoManual(dto, sucursalActivaId, operador));
    }

    @PostMapping("/cerrar")
    public ResponseEntity<TurnoCajaResponseDto> cerrarTurno(
            @Valid @RequestBody CierreCajaRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {

        Long sucursalActivaId = sucursalSessionManager.esModoConsolidado(session)
                ? null
                : sucursalSessionManager.obtenerSucursalActivaId(session).orElse(null);

        return ResponseEntity.ok(cajaService.cerrarTurno(dto, sucursalActivaId, operador));
    }

    public CajaController(CajaService cajaService, SucursalSessionManager sucursalSessionManager) {
        this.cajaService = cajaService;
        this.sucursalSessionManager = sucursalSessionManager;
    }

}
