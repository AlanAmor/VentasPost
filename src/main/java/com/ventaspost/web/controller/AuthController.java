package com.ventaspost.web.controller;

import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.service.AuthSessionService;
import com.ventaspost.web.dto.auth.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthSessionService authSessionService;

    @PostMapping("/login")
    public ResponseEntity<SesionActivaResponseDto> login(
            @Valid @RequestBody LoginRequestDto requestDto,
            HttpServletRequest request,
            HttpServletResponse response) {
        SesionActivaResponseDto sesion = authSessionService.iniciarSesion(requestDto, request, response);
        return ResponseEntity.ok(sesion);
    }

    @GetMapping("/me")
    public ResponseEntity<SesionActivaResponseDto> obtenerSesionActual(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {
        return ResponseEntity.ok(authSessionService.obtenerEstadoSesion(userDetails, session));
    }

    @PostMapping("/cambiar-password")
    public ResponseEntity<SesionActivaResponseDto> cambiarPasswordPropia(
            @Valid @RequestBody CambioPasswordRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {
        SesionActivaResponseDto sesion = authSessionService.cambiarPasswordPropia(
                requestDto, userDetails, request, response
        );
        return ResponseEntity.ok(sesion);
    }

    @PostMapping("/seleccionar-sucursal")
    public ResponseEntity<SesionActivaResponseDto> seleccionarSucursal(
            @Valid @RequestBody SeleccionSucursalRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {
        SesionActivaResponseDto sesion = authSessionService.seleccionarSucursalActiva(
                requestDto,
                userDetails,
                session
        );
        return ResponseEntity.ok(sesion);
    }

    @PostMapping("/autorizar-supervisor")
    public ResponseEntity<AutorizacionSupervisorResponseDto> autorizarConSupervisor(
            @Valid @RequestBody AutorizacionSupervisorRequestDto requestDto,
            HttpSession session) {
        return ResponseEntity.ok(authSessionService.autorizarConSupervisor(requestDto, session));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        authSessionService.cerrarSesion(request);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente."));
    }
}
