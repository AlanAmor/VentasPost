package com.ventaspost.web.controller;

import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.service.UsuarioService;
import com.ventaspost.web.dto.usuario.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/catalogo-permisos")
    public ResponseEntity<CatalogoRolesPermisosDto> obtenerCatalogoRolesPermisos(
            @AuthenticationPrincipal CustomUserDetails operador) {
        return ResponseEntity.ok(usuarioService.obtenerCatalogoRolesPermisos(operador));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDto>> listarUsuarios(
            @AuthenticationPrincipal CustomUserDetails operador,
            @RequestParam(required = false) Long sucursalId) {
        return ResponseEntity.ok(usuarioService.listarUsuarios(operador, sucursalId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> obtenerPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails operador) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id, operador));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDto> crear(
            @Valid @RequestBody UsuarioCreateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador) {
        UsuarioResponseDto creado = usuarioService.crear(dto, operador);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador) {
        return ResponseEntity.ok(usuarioService.actualizar(id, dto, operador));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponseDto> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal CustomUserDetails operador) {
        boolean activo = Boolean.TRUE.equals(body.get("activo"));
        return ResponseEntity.ok(usuarioService.cambiarEstadoActivo(id, activo, operador));
    }

    @PostMapping("/{id}/desbloquear")
    public ResponseEntity<UsuarioResponseDto> desbloquearCuenta(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails operador) {
        return ResponseEntity.ok(usuarioService.desbloquearCuenta(id, operador));
    }

    @PostMapping("/{id}/blanquear-password")
    public ResponseEntity<UsuarioResponseDto> blanquearPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordAdminRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails operador) {
        return ResponseEntity.ok(usuarioService.blanquearPassword(id, dto, operador));
    }
}
