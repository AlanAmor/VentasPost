package com.ventaspost.service;

import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.common.exception.ResourceNotFoundException;
import com.ventaspost.domain.model.CodigoRol;
import com.ventaspost.domain.model.PermisoGranular;
import com.ventaspost.domain.model.Rol;
import com.ventaspost.domain.model.Sucursal;
import com.ventaspost.domain.model.Usuario;
import com.ventaspost.domain.repository.SucursalRepository;
import com.ventaspost.domain.repository.UsuarioRepository;
import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.security.SucursalSessionManager;
import com.ventaspost.web.dto.auth.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthSessionService {

    private final SecurityContextRepository securityContextRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final SucursalSessionManager sucursalSessionManager;
    private final PasswordEncoder passwordEncoder;

    @Transactional(noRollbackFor = BusinessException.class)
    public SesionActivaResponseDto iniciarSesion(
            LoginRequestDto dto,
            HttpServletRequest request,
            HttpServletResponse response) {

        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(dto.username().trim())
                .orElseThrow(() -> new BusinessException("Usuario o contraseña incorrectos.", HttpStatus.UNAUTHORIZED));

        if (!usuario.isActivo()) {
            throw new BusinessException("La cuenta de usuario se encuentra desactivada.", HttpStatus.FORBIDDEN);
        }

        // Regla 4.3: Verificar si la cuenta está bloqueada por intentos fallidos
        if (usuario.isCuentaBloqueada()) {
            throw new BusinessException(
                    "Su cuenta se encuentra bloqueada tras alcanzar 8 intentos fallidos. Solicite el desbloqueo a un Administrador o Encargado.",
                    HttpStatus.LOCKED
            );
        }

        if (!passwordEncoder.matches(dto.password(), usuario.getPasswordHash())) {
            int nuevosIntentos = usuario.getIntentosFallidos() + 1;
            usuario.setIntentosFallidos(nuevosIntentos);

            if (nuevosIntentos >= Usuario.MAX_INTENTOS_FALLIDOS) {
                usuario.setCuentaBloqueada(true);
                usuario.setFechaBloqueo(LocalDateTime.now());
                usuarioRepository.save(usuario);
                throw new BusinessException(
                        "Su cuenta ha sido bloqueada por seguridad tras 8 intentos fallidos consecutivos.",
                        HttpStatus.LOCKED
                );
            }

            usuarioRepository.save(usuario);
            int restantes = Usuario.MAX_INTENTOS_FALLIDOS - nuevosIntentos;
            throw new BusinessException(
                    "Usuario o contraseña incorrectos. Intentos restantes antes del bloqueo: " + restantes,
                    HttpStatus.UNAUTHORIZED
            );
        }

        List<Sucursal> disponibles = obtenerSucursalesDisponibles(usuario);
        if (disponibles.isEmpty()) {
            throw new BusinessException(
                    "El usuario no tiene ninguna sucursal activa habilitada para operar.",
                    HttpStatus.FORBIDDEN
            );
        }

        // Login exitoso: reiniciar contador de intentos fallidos
        usuario.setIntentosFallidos(0);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        HttpSession session = request.getSession(true);

        // Regla 2B: Si tiene 1 sola sucursal habilitada, entra directo; si tiene varias, debe seleccionar
        if (disponibles.size() == 1 && !puedeUsarModoConsolidado(usuario)) {
            sucursalSessionManager.fijarSucursalActiva(session, disponibles.get(0).getId());
        } else {
            sucursalSessionManager.limpiarContextoSucursal(session);
        }

        return construirRespuestaSesion(usuario, disponibles, session);
    }

    @Transactional
    public SesionActivaResponseDto cambiarPasswordPropia(
            CambioPasswordRequestDto dto,
            CustomUserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {

        Usuario usuario = obtenerUsuarioActivo(userDetails.getUsuarioId());

        if (!passwordEncoder.matches(dto.passwordActual(), usuario.getPasswordHash())) {
            throw new BusinessException("La contraseña actual ingresada es incorrecta.", HttpStatus.BAD_REQUEST);
        }

        if (!dto.passwordNueva().equals(dto.confirmacionPassword())) {
            throw new BusinessException("La nueva contraseña y su confirmación no coinciden.", HttpStatus.BAD_REQUEST);
        }

        if (passwordEncoder.matches(dto.passwordNueva(), usuario.getPasswordHash())) {
            throw new BusinessException("La nueva contraseña debe ser diferente a la contraseña actual.", HttpStatus.BAD_REQUEST);
        }

        usuario.setPasswordHash(passwordEncoder.encode(dto.passwordNueva()));
        usuario.setRequiereCambioPassword(false);
        usuarioRepository.save(usuario);

        // Actualizar el principal en la sesión actual
        CustomUserDetails actualizadoDetails = new CustomUserDetails(usuario);
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                actualizadoDetails, null, actualizadoDetails.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(newAuth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        HttpSession session = request.getSession(false);
        List<Sucursal> disponibles = obtenerSucursalesDisponibles(usuario);
        if (disponibles.size() == 1 && !puedeUsarModoConsolidado(usuario)
                && sucursalSessionManager.obtenerSucursalActivaId(session).isEmpty()) {
            sucursalSessionManager.fijarSucursalActiva(session, disponibles.get(0).getId());
        }

        return construirRespuestaSesion(usuario, disponibles, session);
    }

    @Transactional(readOnly = true)
    public SesionActivaResponseDto seleccionarSucursalActiva(
            SeleccionSucursalRequestDto dto,
            CustomUserDetails userDetails,
            HttpSession session) {

        Usuario usuario = obtenerUsuarioActivo(userDetails.getUsuarioId());
        List<Sucursal> disponibles = obtenerSucursalesDisponibles(usuario);

        // Regla 2.1 A: Modo Consolidado ("Todas las Sucursales") para Admin / acceso global
        if (Boolean.TRUE.equals(dto.modoConsolidado())) {
            if (!puedeUsarModoConsolidado(usuario)) {
                throw new BusinessException(
                        "Su perfil no cuenta con permisos para activar la Vista Consolidada Multi-Sucursal.",
                        HttpStatus.FORBIDDEN
                );
            }
            sucursalSessionManager.activarModoConsolidado(session);
            return construirRespuestaSesion(usuario, disponibles, session);
        }

        if (dto.sucursalId() == null) {
            throw new BusinessException("Debe indicar una sucursal válida.", HttpStatus.BAD_REQUEST);
        }

        Sucursal seleccionada = disponibles.stream()
                .filter(s -> s.getId().equals(dto.sucursalId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "La sucursal seleccionada no está habilitada para su usuario o no se encuentra activa.",
                        HttpStatus.FORBIDDEN
                ));

        sucursalSessionManager.fijarSucursalActiva(session, seleccionada.getId());
        return construirRespuestaSesion(usuario, disponibles, session);
    }

    /**
     * Regla 1.2: Autorización en vivo de un Encargado o Admin en ventana emergente
     * sin cerrar la sesión del cajero.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public AutorizacionSupervisorResponseDto autorizarConSupervisor(
            AutorizacionSupervisorRequestDto dto,
            HttpSession session) {

        Long sucursalActivaId = sucursalSessionManager.obtenerSucursalActivaId(session)
                .orElseThrow(() -> new BusinessException(
                        "Debe existir una sucursal activa seleccionada en la terminal para solicitar autorización.",
                        HttpStatus.BAD_REQUEST
                ));

        Usuario supervisor = usuarioRepository.findByUsernameIgnoreCase(dto.usernameSupervisor().trim())
                .orElseThrow(() -> new BusinessException("Credenciales de supervisor inválidas.", HttpStatus.UNAUTHORIZED));

        if (!supervisor.isActivo() || supervisor.isCuentaBloqueada()) {
            throw new BusinessException("La cuenta del supervisor no está habilitada o se encuentra bloqueada.", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(dto.passwordSupervisor(), supervisor.getPasswordHash())) {
            int intentos = supervisor.getIntentosFallidos() + 1;
            supervisor.setIntentosFallidos(intentos);
            if (intentos >= Usuario.MAX_INTENTOS_FALLIDOS) {
                supervisor.setCuentaBloqueada(true);
                supervisor.setFechaBloqueo(LocalDateTime.now());
            }
            usuarioRepository.save(supervisor);
            throw new BusinessException("Credenciales de supervisor inválidas.", HttpStatus.UNAUTHORIZED);
        }

        supervisor.setIntentosFallidos(0);
        usuarioRepository.save(supervisor);

        boolean esAdmin = supervisor.tieneRol(CodigoRol.ADMIN);
        boolean tieneSucursal = esAdmin || supervisor.isAccesoTodasSucursales()
                || supervisor.getSucursalesHabilitadas().stream().anyMatch(s -> s.getId().equals(sucursalActivaId) && s.isActivo());

        if (!tieneSucursal) {
            throw new BusinessException("El supervisor ingresado no tiene alcance sobre esta sucursal.", HttpStatus.FORBIDDEN);
        }

        if (!supervisor.tienePermiso(dto.permisoRequerido())) {
            throw new BusinessException(
                    "El usuario ingresado no posee el permiso requerido: " + dto.permisoRequerido().getDescripcion(),
                    HttpStatus.FORBIDDEN
            );
        }

        return new AutorizacionSupervisorResponseDto(
                true,
                supervisor.getId(),
                supervisor.getUsername(),
                supervisor.getNombre() + " " + supervisor.getApellido(),
                dto.permisoRequerido().name(),
                sucursalActivaId,
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public SesionActivaResponseDto obtenerEstadoSesion(
            CustomUserDetails userDetails,
            HttpSession session) {

        Usuario usuario = obtenerUsuarioActivo(userDetails.getUsuarioId());
        List<Sucursal> disponibles = obtenerSucursalesDisponibles(usuario);

        if (disponibles.size() == 1
                && !puedeUsarModoConsolidado(usuario)
                && sucursalSessionManager.obtenerSucursalActivaId(session).isEmpty()) {
            sucursalSessionManager.fijarSucursalActiva(session, disponibles.get(0).getId());
        }

        return construirRespuestaSesion(usuario, disponibles, session);
    }

    public void cerrarSesion(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private Usuario obtenerUsuarioActivo(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        if (!usuario.isActivo()) {
            throw new BusinessException("El usuario se encuentra deshabilitado.", HttpStatus.FORBIDDEN);
        }
        return usuario;
    }

    private boolean puedeUsarModoConsolidado(Usuario usuario) {
        return usuario.tieneRol(CodigoRol.ADMIN)
                || usuario.isAccesoTodasSucursales()
                || usuario.tienePermiso(PermisoGranular.VER_REPORTES_CONSOLIDADOS);
    }

    private List<Sucursal> obtenerSucursalesDisponibles(Usuario usuario) {
        if (usuario.isAccesoTodasSucursales() || usuario.tieneRol(CodigoRol.ADMIN)) {
            return sucursalRepository.findByActivoTrueOrderByCodigoAsc();
        }

        return usuario.getSucursalesHabilitadas().stream()
                .filter(Sucursal::isActivo)
                .sorted(Comparator.comparing(Sucursal::getCodigo))
                .toList();
    }

    private SesionActivaResponseDto construirRespuestaSesion(
            Usuario usuario,
            List<Sucursal> disponibles,
            HttpSession session) {

        boolean puedeConsolidado = puedeUsarModoConsolidado(usuario);
        boolean modoConsolidado = puedeConsolidado && sucursalSessionManager.esModoConsolidado(session);

        Optional<Long> activaIdOpt = sucursalSessionManager.obtenerSucursalActivaId(session);
        SucursalResumenDto activaDto = null;

        if (!modoConsolidado && activaIdOpt.isPresent()) {
            Long activaId = activaIdOpt.get();
            activaDto = disponibles.stream()
                    .filter(s -> s.getId().equals(activaId))
                    .findFirst()
                    .map(this::toResumenDto)
                    .orElse(null);
        }

        boolean requiereSeleccion = !usuario.isRequiereCambioPassword() && !modoConsolidado && (activaDto == null);

        Set<String> roles = usuario.getRoles().stream()
                .filter(Rol::isActivo)
                .map(r -> r.getCodigo().name())
                .collect(Collectors.toSet());

        Set<String> permisos = (usuario.tieneRol(CodigoRol.ADMIN)
                ? Set.of(PermisoGranular.values())
                : usuario.getPermisosGranulares())
                .stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        List<SucursalResumenDto> disponiblesDto = disponibles.stream()
                .map(this::toResumenDto)
                .toList();

        return new SesionActivaResponseDto(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre() + " " + usuario.getApellido(),
                roles,
                permisos,
                usuario.isAccesoTodasSucursales(),
                puedeConsolidado,
                modoConsolidado,
                usuario.isRequiereCambioPassword(),
                requiereSeleccion,
                activaDto,
                disponiblesDto
        );
    }

    private SucursalResumenDto toResumenDto(Sucursal s) {
        return new SucursalResumenDto(
                s.getId(),
                s.getCodigo(),
                s.getNombre(),
                s.getPuntoVentaArca(),
                s.isEsCasaCentral()
        );
    }

    public AuthSessionService(SecurityContextRepository securityContextRepository, UsuarioRepository usuarioRepository, SucursalRepository sucursalRepository, SucursalSessionManager sucursalSessionManager, PasswordEncoder passwordEncoder) {
        this.securityContextRepository = securityContextRepository;
        this.usuarioRepository = usuarioRepository;
        this.sucursalRepository = sucursalRepository;
        this.sucursalSessionManager = sucursalSessionManager;
        this.passwordEncoder = passwordEncoder;
    }

}
