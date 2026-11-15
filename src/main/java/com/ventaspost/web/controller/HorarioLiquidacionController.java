package com.ventaspost.web.controller;

import com.ventaspost.domain.model.CodigoRol;
import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.security.SucursalSessionManager;
import com.ventaspost.service.HorarioLiquidacionService;
import com.ventaspost.web.dto.sueldo.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sueldos")
public class HorarioLiquidacionController {

    private final HorarioLiquidacionService horarioLiquidacionService;
    private final SucursalSessionManager sucursalSessionManager;

    /* =========================================================================
       1. CONFIGURACIÓN DE TARIFAS POR HORA (POR ROL Y EMPLEADO) Y RECARGOS
       ========================================================================= */
    @GetMapping("/configuracion-tarifas")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_SUELDOS_LIQUIDAR') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR')")
    public ResponseEntity<ConfiguracionTarifasSueldosResponseDto> obtenerConfiguracionTarifas() {
        return ResponseEntity.ok(horarioLiquidacionService.obtenerConfiguracionTarifas());
    }

    @PutMapping("/configuracion-tarifas")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_SUELDOS_LIQUIDAR')")
    public ResponseEntity<ConfiguracionTarifasSueldosResponseDto> actualizarConfiguracionTarifas(
            @Valid @RequestBody ConfiguracionTarifasSueldosRequestDto dto) {
        return ResponseEntity.ok(horarioLiquidacionService.actualizarConfiguracionTarifas(dto));
    }

    /* =========================================================================
       2. HORARIOS SEMANALES ASIGNADOS POR EMPLEADO Y SUCURSAL
       ========================================================================= */
    @GetMapping("/horarios")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR') or hasAuthority('PERM_SUELDOS_LIQUIDAR')")
    public ResponseEntity<List<HorarioAsignadoResponseDto>> listarHorarios(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long sucursalId,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {
        Long sucFiltro = resolverSucursalSegura(sucursalId, operador, session);
        return ResponseEntity.ok(horarioLiquidacionService.listarHorariosAsignados(usuarioId, sucFiltro));
    }

    @PostMapping("/horarios")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR')")
    public ResponseEntity<HorarioAsignadoResponseDto> crearHorario(
            @Valid @RequestBody HorarioAsignadoRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(horarioLiquidacionService.crearHorarioAsignado(dto));
    }

    @DeleteMapping("/horarios/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR')")
    public ResponseEntity<Void> eliminarHorario(@PathVariable Long id) {
        horarioLiquidacionService.eliminarHorarioAsignado(id);
        return ResponseEntity.noContent().build();
    }

    /* =========================================================================
       3. FICHAJE EN VIVO (RELOJ DE ENTRADA Y SALIDA PARA CUALQUIER EMPLEADO)
       ========================================================================= */
    @GetMapping("/fichaje/estado-actual")
    public ResponseEntity<Map<String, Object>> obtenerEstadoFichajeActual(
            @AuthenticationPrincipal CustomUserDetails operador) {
        return ResponseEntity.ok(horarioLiquidacionService.obtenerEstadoFichajeActual(operador.getUsuarioId()));
    }

    @PostMapping("/fichaje/ingreso")
    public ResponseEntity<AsistenciaJornadaResponseDto> ficharIngreso(
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {
        Long sucursalActivaId = sucursalSessionManager.esModoConsolidado(session)
                ? null
                : sucursalSessionManager.obtenerSucursalActivaId(session).orElse(null);
        return ResponseEntity.ok(horarioLiquidacionService.ficharIngresoReloj(operador.getUsuarioId(), sucursalActivaId));
    }

    @PostMapping("/fichaje/egreso")
    public ResponseEntity<AsistenciaJornadaResponseDto> ficharEgreso(
            @AuthenticationPrincipal CustomUserDetails operador) {
        return ResponseEntity.ok(horarioLiquidacionService.ficharEgresoReloj(operador.getUsuarioId()));
    }

    /* =========================================================================
       4. ASISTENCIAS Y JORNADAS DE TRABAJO (VALIDACIÓN Y CARGA MANUAL)
       ========================================================================= */
    @GetMapping("/asistencias")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR') or hasAuthority('PERM_SUELDOS_LIQUIDAR')")
    public ResponseEntity<List<AsistenciaJornadaResponseDto>> listarAsistencias(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {
        Long sucFiltro = resolverSucursalSegura(sucursalId, operador, session);
        return ResponseEntity.ok(horarioLiquidacionService.listarAsistencias(usuarioId, sucFiltro, desde, hasta));
    }

    @PostMapping("/asistencias/generar-desde-horarios")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR') or hasAuthority('PERM_SUELDOS_LIQUIDAR')")
    public ResponseEntity<Map<String, Object>> generarJornadasDesdeHorarios(
            @Valid @RequestBody GenerarAsistenciasDesdeHorarioRequestDto dto) {
        return ResponseEntity.ok(horarioLiquidacionService.generarJornadasDesdeHorariosTeoricos(dto));
    }

    @PostMapping("/asistencias")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR') or hasAuthority('PERM_SUELDOS_LIQUIDAR')")
    public ResponseEntity<AsistenciaJornadaResponseDto> crearJornadaManual(
            @Valid @RequestBody AsistenciaJornadaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(horarioLiquidacionService.guardarOActualizarJornadaManual(null, dto));
    }

    @PutMapping("/asistencias/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR') or hasAuthority('PERM_SUELDOS_LIQUIDAR')")
    public ResponseEntity<AsistenciaJornadaResponseDto> actualizarJornadaManual(
            @PathVariable Long id,
            @Valid @RequestBody AsistenciaJornadaRequestDto dto) {
        return ResponseEntity.ok(horarioLiquidacionService.guardarOActualizarJornadaManual(id, dto));
    }

    @DeleteMapping("/asistencias/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR') or hasAuthority('PERM_SUELDOS_LIQUIDAR')")
    public ResponseEntity<Void> eliminarJornada(@PathVariable Long id) {
        horarioLiquidacionService.eliminarJornadaNoLiquidada(id);
        return ResponseEntity.noContent().build();
    }

    /* =========================================================================
       5. ADELANTOS / VALES DE SUELDO
       ========================================================================= */
    @GetMapping("/adelantos")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_SUELDOS_LIQUIDAR') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR')")
    public ResponseEntity<List<AdelantoSueldoResponseDto>> listarAdelantos(
            @RequestParam(required = false) Long usuarioId) {
        return ResponseEntity.ok(horarioLiquidacionService.listarAdelantos(usuarioId));
    }

    @PostMapping("/adelantos")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_SUELDOS_LIQUIDAR') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR')")
    public ResponseEntity<AdelantoSueldoResponseDto> registrarAdelanto(
            @Valid @RequestBody AdelantoSueldoRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(horarioLiquidacionService.registrarAdelanto(dto, operador.getUsername()));
    }

    @DeleteMapping("/adelantos/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_SUELDOS_LIQUIDAR')")
    public ResponseEntity<Void> eliminarAdelanto(@PathVariable Long id) {
        horarioLiquidacionService.eliminarAdelantoPendiente(id);
        return ResponseEntity.noContent().build();
    }

    /* =========================================================================
       6. PRELIQUIDACIÓN Y EMISIÓN DE LIQUIDACIONES DE SUELDOS
       ========================================================================= */
    @GetMapping("/preliquidacion")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_SUELDOS_LIQUIDAR')")
    public ResponseEntity<PreliquidacionSueldoResponseDto> calcularPreliquidacion(
            @RequestParam Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return ResponseEntity.ok(horarioLiquidacionService.calcularPreliquidacion(usuarioId, fechaDesde, fechaHasta));
    }

    @PostMapping("/liquidaciones")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_SUELDOS_LIQUIDAR')")
    public ResponseEntity<LiquidacionSueldoResponseDto> emitirLiquidacion(
            @Valid @RequestBody EmitirLiquidacionSueldoRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(horarioLiquidacionService.emitirLiquidacion(dto, operador.getUsername()));
    }

    @GetMapping("/liquidaciones")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_SUELDOS_LIQUIDAR') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR')")
    public ResponseEntity<List<LiquidacionSueldoResponseDto>> listarLiquidaciones(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long sucursalId,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {
        Long sucFiltro = resolverSucursalSegura(sucursalId, operador, session);
        return ResponseEntity.ok(horarioLiquidacionService.listarLiquidaciones(usuarioId, sucFiltro));
    }

    @GetMapping("/liquidaciones/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENCARGADO_SUCURSAL') or hasAuthority('PERM_SUELDOS_LIQUIDAR') or hasAuthority('PERM_HORARIOS_ASISTENCIAS_GESTIONAR')")
    public ResponseEntity<LiquidacionSueldoResponseDto> obtenerLiquidacionPorId(@PathVariable Long id) {
        return ResponseEntity.ok(horarioLiquidacionService.obtenerLiquidacionPorId(id));
    }

    private Long resolverSucursalSegura(Long sucursalIdParam, CustomUserDetails operador, HttpSession session) {
        if (operador != null && !operador.tieneRol(CodigoRol.ADMIN) && !operador.isAccesoTodasSucursales()) {
            return sucursalSessionManager.obtenerSucursalActivaId(session).orElse(sucursalIdParam);
        }
        return sucursalIdParam;
    }

    public HorarioLiquidacionController(HorarioLiquidacionService horarioLiquidacionService, SucursalSessionManager sucursalSessionManager) {
        this.horarioLiquidacionService = horarioLiquidacionService;
        this.sucursalSessionManager = sucursalSessionManager;
    }

}
