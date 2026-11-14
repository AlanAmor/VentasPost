package com.ventaspost.web.controller;

import com.ventaspost.service.SucursalService;
import com.ventaspost.web.dto.sucursal.SucursalRequestDto;
import com.ventaspost.web.dto.sucursal.SucursalResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sucursales")
@RequiredArgsConstructor
public class SucursalController {

    private final SucursalService sucursalService;

    @GetMapping
    public ResponseEntity<List<SucursalResponseDto>> listarTodas() {
        return ResponseEntity.ok(sucursalService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SucursalResponseDto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(sucursalService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SucursalResponseDto> crear(@Valid @RequestBody SucursalRequestDto dto) {
        SucursalResponseDto creada = sucursalService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SucursalResponseDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SucursalRequestDto dto) {
        return ResponseEntity.ok(sucursalService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SucursalResponseDto> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean activo = Boolean.TRUE.equals(body.get("activo"));
        return ResponseEntity.ok(sucursalService.cambiarEstadoActivo(id, activo));
    }
}
