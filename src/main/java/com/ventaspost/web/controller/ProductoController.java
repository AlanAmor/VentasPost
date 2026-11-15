package com.ventaspost.web.controller;

import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.security.SucursalSessionManager;
import com.ventaspost.service.ProductoService;
import com.ventaspost.web.dto.producto.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final SucursalSessionManager sucursalSessionManager;

    @GetMapping
    public ResponseEntity<List<ProductoResponseDto>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) Boolean soloBajoStock,
            @RequestParam(required = false) Boolean soloActivos,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {

        Long sucursalContextoId = obtenerSucursalContexto(session);
        return ResponseEntity.ok(productoService.listarProductos(
                q, categoriaId, marcaId, proveedorId, soloBajoStock, soloActivos, sucursalContextoId, operador
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDto> obtenerPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {
        Long sucursalContextoId = obtenerSucursalContexto(session);
        return ResponseEntity.ok(productoService.obtenerPorId(id, sucursalContextoId, operador));
    }

    @GetMapping("/generar-codigo-interno")
    public ResponseEntity<Map<String, String>> generarCodigoInterno() {
        String codigo = productoService.generarCodigoBarrasInternoUnico();
        return ResponseEntity.ok(Map.of("codigoBarras", codigo));
    }

    @PostMapping
    public ResponseEntity<ProductoResponseDto> crear(
            @Valid @RequestBody ProductoRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {
        Long sucursalContextoId = obtenerSucursalContexto(session);
        ProductoResponseDto creado = productoService.crear(dto, sucursalContextoId, operador);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {
        Long sucursalContextoId = obtenerSucursalContexto(session);
        return ResponseEntity.ok(productoService.actualizar(id, dto, sucursalContextoId, operador));
    }

    @PutMapping("/{id}/configuracion-sucursal")
    public ResponseEntity<ProductoResponseDto> actualizarConfiguracionSucursal(
            @PathVariable Long id,
            @Valid @RequestBody ConfiguracionSucursalProductoRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {
        Long sucursalContextoId = obtenerSucursalContexto(session);
        return ResponseEntity.ok(productoService.actualizarConfiguracionSucursal(id, dto, sucursalContextoId, operador));
    }

    @PostMapping("/actualizacion-masiva")
    public ResponseEntity<ActualizacionMasivaPreciosResponseDto> actualizarPreciosMasivo(
            @Valid @RequestBody ActualizacionMasivaPreciosRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador) {
        return ResponseEntity.ok(productoService.actualizarPreciosMasivo(dto, operador));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ProductoResponseDto> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal CustomUserDetails operador,
            HttpSession session) {
        boolean activo = Boolean.TRUE.equals(body.get("activo"));
        Long sucursalContextoId = obtenerSucursalContexto(session);
        return ResponseEntity.ok(productoService.cambiarEstadoActivo(id, activo, sucursalContextoId, operador));
    }

    private Long obtenerSucursalContexto(HttpSession session) {
        if (sucursalSessionManager.esModoConsolidado(session)) {
            return null;
        }
        return sucursalSessionManager.obtenerSucursalActivaId(session).orElse(null);
    }

    public ProductoController(ProductoService productoService, SucursalSessionManager sucursalSessionManager) {
        this.productoService = productoService;
        this.sucursalSessionManager = sucursalSessionManager;
    }

}
