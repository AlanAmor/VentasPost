package com.ventaspost.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventaspost.common.exception.ApiErrorResponse;
import com.ventaspost.domain.model.CodigoRol;
import com.ventaspost.domain.model.Sucursal;
import com.ventaspost.domain.model.Usuario;
import com.ventaspost.domain.repository.SucursalRepository;
import com.ventaspost.domain.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Regla 5.2 y 4.2: Verifica en cada petición que el usuario autenticado siga activo,
 * no esté bloqueado, haya cumplido el cambio obligatorio de contraseña y que la
 * sucursal activa en su sesión continúe habilitada.
 */
@Component
public class ActiveSessionValidationFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final SucursalSessionManager sucursalSessionManager;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/v1/") || uri.equals("/api/v1/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(userDetails.getUsuarioId());
        if (usuarioOpt.isEmpty() || !usuarioOpt.get().isActivo() || usuarioOpt.get().isCuentaBloqueada()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            escribirErrorJson(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Sesión Invalidada",
                    "Su cuenta ha sido desactivada o bloqueada. Contacte al administrador.",
                    uri
            );
            return;
        }

        Usuario usuario = usuarioOpt.get();

        // Regla 4.2: Si requiere cambio de contraseña, bloquear cualquier operación salvo cambiar clave, me o logout
        if (usuario.isRequiereCambioPassword()
                && !uri.equals("/api/v1/auth/cambiar-password")
                && !uri.equals("/api/v1/auth/me")
                && !uri.equals("/api/v1/auth/logout")) {
            escribirErrorJson(
                    response,
                    HttpStatus.FORBIDDEN,
                    "Cambio de Contraseña Requerido",
                    "Por seguridad debe elegir una nueva contraseña antes de operar en el sistema.",
                    uri
            );
            return;
        }

        // Regla 5.2: Validar que la sucursal activa de la sesión siga activa y habilitada para el usuario
        HttpSession session = request.getSession(false);
        if (session != null) {
            Optional<Long> sucursalActivaIdOpt = sucursalSessionManager.obtenerSucursalActivaId(session);
            if (sucursalActivaIdOpt.isPresent()) {
                Long sucursalId = sucursalActivaIdOpt.get();
                Optional<Sucursal> sucOpt = sucursalRepository.findById(sucursalId);
                boolean esAdmin = usuario.tieneRol(CodigoRol.ADMIN) || usuario.isAccesoTodasSucursales();
                boolean habilitadaParaUsuario = esAdmin || usuario.getSucursalesHabilitadas().stream()
                        .anyMatch(s -> s.getId().equals(sucursalId) && s.isActivo());

                if (sucOpt.isEmpty() || !sucOpt.get().isActivo() || !habilitadaParaUsuario) {
                    sucursalSessionManager.limpiarContextoSucursal(session);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void escribirErrorJson(
            HttpServletResponse response,
            HttpStatus status,
            String errorTitle,
            String message,
            String path) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse err = ApiErrorResponse.of(status.value(), errorTitle, message, path);
        response.getWriter().write(objectMapper.writeValueAsString(err));
    }

    public ActiveSessionValidationFilter(UsuarioRepository usuarioRepository, SucursalRepository sucursalRepository, SucursalSessionManager sucursalSessionManager, ObjectMapper objectMapper) {
        this.usuarioRepository = usuarioRepository;
        this.sucursalRepository = sucursalRepository;
        this.sucursalSessionManager = sucursalSessionManager;
        this.objectMapper = objectMapper;
    }

}
