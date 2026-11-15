package com.ventaspost.web.controller;

import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.security.SucursalSessionManager;
import com.ventaspost.service.EstadisticaNegocioService;
import com.ventaspost.web.dto.estadistica.DashboardEstadisticoResponseDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/estadisticas")
public class EstadisticaController {

    private final EstadisticaNegocioService estadisticaService;
    private final SucursalSessionManager sucursalSessionManager;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardEstadisticoResponseDto> obtenerDashboard(
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false, defaultValue = "TODAS_REALIZADAS") String filtroFacturacion,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {

        Long sucursalObjetivo = resolverSucursalFiltro(sucursalId, userDetails, session);
        return ResponseEntity.ok(
                estadisticaService.obtenerDashboard(
                        sucursalObjetivo,
                        fechaDesde,
                        fechaHasta,
                        filtroFacturacion,
                        userDetails
                )
        );
    }

    @GetMapping("/exportar-csv")
    public ResponseEntity<byte[]> exportarCsvExcel(
            @RequestParam(defaultValue = "RANKING_PRODUCTOS") String tipoReporte,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false, defaultValue = "TODAS_REALIZADAS") String filtroFacturacion,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {

        Long sucursalObjetivo = resolverSucursalFiltro(sucursalId, userDetails, session);
        String contenidoCsv = estadisticaService.generarReporteCsvExcel(
                tipoReporte,
                sucursalObjetivo,
                fechaDesde,
                fechaHasta,
                filtroFacturacion,
                userDetails
        );

        String nombreArchivo = "ventaspost_" + tipoReporte.toLowerCase() + "_" + LocalDate.now() + ".csv";
        byte[] bytes = contenidoCsv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    private Long resolverSucursalFiltro(Long sucursalIdParam, CustomUserDetails userDetails, HttpSession session) {
        boolean puedeVerTodas = userDetails != null && (
                userDetails.tieneRol(com.ventaspost.domain.model.CodigoRol.ADMIN)
                || userDetails.isAccesoTodasSucursales()
                || userDetails.tienePermiso(com.ventaspost.domain.model.PermisoGranular.VER_REPORTES_CONSOLIDADOS)
        );
        if (!puedeVerTodas) {
            return sucursalSessionManager.obtenerSucursalActivaId(session).orElse(sucursalIdParam);
        }
        if (sucursalIdParam != null) {
            return sucursalIdParam <= 0 ? null : sucursalIdParam;
        }
        if (sucursalSessionManager.esModoConsolidado(session)) {
            return null;
        }
        return sucursalSessionManager.obtenerSucursalActivaId(session).orElse(null);
    }

    public EstadisticaController(EstadisticaNegocioService estadisticaService, SucursalSessionManager sucursalSessionManager) {
        this.estadisticaService = estadisticaService;
        this.sucursalSessionManager = sucursalSessionManager;
    }

}
