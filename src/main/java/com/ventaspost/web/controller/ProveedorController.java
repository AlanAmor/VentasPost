package com.ventaspost.web.controller;

import com.ventaspost.service.ProveedorService;
import com.ventaspost.web.dto.proveedor.ProveedorRequestDto;
import com.ventaspost.web.dto.proveedor.ProveedorResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public ResponseEntity<List<ProveedorResponseDto>> listar(
            @RequestParam(defaultValue = "false") boolean soloActivos) {
        return ResponseEntity.ok(proveedorService.listarTodos(soloActivos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponseDto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_CAMBIAR_PRECIOS_GLOBAL') or hasAuthority('PERM_GESTIONAR_STOCK_SUCURSAL')")
    public ResponseEntity<ProveedorResponseDto> crear(@Valid @RequestBody ProveedorRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_CAMBIAR_PRECIOS_GLOBAL') or hasAuthority('PERM_GESTIONAR_STOCK_SUCURSAL')")
    public ResponseEntity<ProveedorResponseDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorRequestDto dto) {
        return ResponseEntity.ok(proveedorService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_CAMBIAR_PRECIOS_GLOBAL')")
    public ResponseEntity<ProveedorResponseDto> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean activo = Boolean.TRUE.equals(body.get("activo"));
        return ResponseEntity.ok(proveedorService.cambiarEstadoActivo(id, activo));
    }

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

}
