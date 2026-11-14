package com.ventaspost.service;

import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.common.exception.ResourceNotFoundException;
import com.ventaspost.domain.model.CodigoRol;
import com.ventaspost.domain.model.PermisoGranular;
import com.ventaspost.domain.model.Rol;
import com.ventaspost.domain.model.Sucursal;
import com.ventaspost.domain.model.Usuario;
import com.ventaspost.domain.repository.RolRepository;
import com.ventaspost.domain.repository.SucursalRepository;
import com.ventaspost.domain.repository.UsuarioRepository;
import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.web.dto.auth.SucursalResumenDto;
import com.ventaspost.web.dto.usuario.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Set<CodigoRol> ROLES_INFERIORES_PERMITIDOS_ENCARGADO = EnumSet.of(
            CodigoRol.CAJERO,
            CodigoRol.DEPOSITO,
            CodigoRol.LIMPIEZA
    );

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public CatalogoRolesPermisosDto obtenerCatalogoRolesPermisos(CustomUserDetails operador) {
        validarPermisoGestionUsuarios(operador);
        boolean esAdmin = operador.tieneRol(CodigoRol.ADMIN);

        List<CatalogoRolesPermisosDto.RolItemDto> roles = rolRepository.findAll().stream()
                .filter(Rol::isActivo)
                .filter(r -> esAdmin || ROLES_INFERIORES_PERMITIDOS_ENCARGADO.contains(r.getCodigo()))
                .map(r -> new CatalogoRolesPermisosDto.RolItemDto(
                        r.getCodigo().name(),
                        r.getNombre(),
                        r.getDescripcion()
                ))
                .toList();

        List<CatalogoRolesPermisosDto.PermisoItemDto> permisos = Arrays.stream(PermisoGranular.values())
                .filter(p -> esAdmin || (!p.isExclusivoAdmin() && p != PermisoGranular.GESTIONAR_EMPLEADOS_SUCURSAL))
                .map(p -> new CatalogoRolesPermisosDto.PermisoItemDto(
                        p.name(),
                        p.getDescripcion(),
                        p.isExclusivoAdmin()
                ))
                .toList();

        Map<String, Set<String>> porDefecto = new LinkedHashMap<>();
        for (CodigoRol codigoRol : CodigoRol.values()) {
            Set<String> perms = PermisoGranular.permisosPorDefectoParaRol(codigoRol).stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            porDefecto.put(codigoRol.name(), perms);
        }

        return new CatalogoRolesPermisosDto(roles, permisos, porDefecto);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listarUsuarios(CustomUserDetails operador, Long sucursalFiltroId) {
        validarPermisoGestionUsuarios(operador);
        boolean esAdmin = operador.tieneRol(CodigoRol.ADMIN);
        Usuario usuarioOperador = buscarEntidadPorId(operador.getUsuarioId());
        Set<Long> sucursalesOperadorIds = usuarioOperador.getSucursalesHabilitadas().stream()
                .map(Sucursal::getId)
                .collect(Collectors.toSet());

        return usuarioRepository.findAllByOrderByApellidoAscNombreAsc().stream()
                .filter(u -> {
                    if (!esAdmin) {
                        // El Encargado solo ve usuarios de sus sucursales y nunca ve Administradores
                        if (u.tieneRol(CodigoRol.ADMIN)) {
                            return false;
                        }
                        boolean comparteSucursal = u.getSucursalesHabilitadas().stream()
                                .anyMatch(s -> sucursalesOperadorIds.contains(s.getId()));
                        if (!comparteSucursal) {
                            return false;
                        }
                    }
                    if (sucursalFiltroId != null) {
                        return u.isAccesoTodasSucursales() || u.getSucursalesHabilitadas().stream()
                                .anyMatch(s -> s.getId().equals(sucursalFiltroId));
                    }
                    return true;
                })
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDto obtenerPorId(Long id, CustomUserDetails operador) {
        validarPermisoGestionUsuarios(operador);
        Usuario objetivo = buscarEntidadPorId(id);
        validarAlcanceOperadorSobreObjetivo(operador, objetivo);
        return toResponseDto(objetivo);
    }

    @Transactional
    public UsuarioResponseDto crear(UsuarioCreateRequestDto dto, CustomUserDetails operador) {
        validarPermisoGestionUsuarios(operador);

        String usernameNorm = dto.username().trim().toLowerCase();
        String dniNorm = dto.dni().trim();
        String legajoNorm = limpiarTextoOpcional(dto.legajo());
        String emailNorm = limpiarTextoOpcional(dto.email());

        if (usuarioRepository.existsByUsernameIgnoreCase(usernameNorm)) {
            throw new BusinessException("Ya existe un usuario con el username: " + usernameNorm, HttpStatus.CONFLICT);
        }
        if (usuarioRepository.existsByDni(dniNorm)) {
            throw new BusinessException("Ya existe un empleado registrado con el DNI/CUIL: " + dniNorm, HttpStatus.CONFLICT);
        }
        if (legajoNorm != null && usuarioRepository.existsByLegajoIgnoreCase(legajoNorm)) {
            throw new BusinessException("Ya existe un empleado con el legajo: " + legajoNorm, HttpStatus.CONFLICT);
        }
        if (emailNorm != null && usuarioRepository.existsByEmailIgnoreCase(emailNorm)) {
            throw new BusinessException("Ya existe un empleado con el email: " + emailNorm, HttpStatus.CONFLICT);
        }

        Usuario nuevo = new Usuario();
        nuevo.setUsername(usernameNorm);
        nuevo.setPasswordHash(passwordEncoder.encode(dto.passwordInicial()));
        // Regla 4.2: Al crear un empleado, debe poder elegir su propia contraseña al ingresar
        nuevo.setRequiereCambioPassword(true);
        nuevo.setIntentosFallidos(0);
        nuevo.setCuentaBloqueada(false);
        nuevo.setActivo(true);

        aplicarDatosYAsignaciones(
                nuevo,
                dto.nombre(),
                dto.apellido(),
                dniNorm,
                legajoNorm,
                emailNorm,
                dto.telefono(),
                dto.direccion(),
                dto.fechaIngreso(),
                dto.accesoTodasSucursales(),
                dto.roles(),
                dto.sucursalIds(),
                dto.permisosGranulares(),
                operador
        );

        return toResponseDto(usuarioRepository.save(nuevo));
    }

    @Transactional
    public UsuarioResponseDto actualizar(Long id, UsuarioUpdateRequestDto dto, CustomUserDetails operador) {
        validarPermisoGestionUsuarios(operador);
        Usuario usuario = buscarEntidadPorId(id);
        validarAlcanceOperadorSobreObjetivo(operador, usuario);

        String usernameNorm = dto.username().trim().toLowerCase();
        String dniNorm = dto.dni().trim();
        String legajoNorm = limpiarTextoOpcional(dto.legajo());
        String emailNorm = limpiarTextoOpcional(dto.email());

        if (usuarioRepository.existsByUsernameIgnoreCaseAndIdNot(usernameNorm, id)) {
            throw new BusinessException("Ya existe otro usuario con el username: " + usernameNorm, HttpStatus.CONFLICT);
        }
        if (usuarioRepository.existsByDniAndIdNot(dniNorm, id)) {
            throw new BusinessException("Ya existe otro empleado registrado con el DNI/CUIL: " + dniNorm, HttpStatus.CONFLICT);
        }
        if (legajoNorm != null && usuarioRepository.existsByLegajoIgnoreCaseAndIdNot(legajoNorm, id)) {
            throw new BusinessException("Ya existe otro empleado con el legajo: " + legajoNorm, HttpStatus.CONFLICT);
        }
        if (emailNorm != null && usuarioRepository.existsByEmailIgnoreCaseAndIdNot(emailNorm, id)) {
            throw new BusinessException("Ya existe otro empleado con el email: " + emailNorm, HttpStatus.CONFLICT);
        }

        // Proteger que no se quite el rol ADMIN al único administrador activo
        if (usuario.tieneRol(CodigoRol.ADMIN) && !dto.roles().contains(CodigoRol.ADMIN)) {
            validarNoEsUltimoAdminActivo(id);
        }

        usuario.setUsername(usernameNorm);
        aplicarDatosYAsignaciones(
                usuario,
                dto.nombre(),
                dto.apellido(),
                dniNorm,
                legajoNorm,
                emailNorm,
                dto.telefono(),
                dto.direccion(),
                dto.fechaIngreso(),
                dto.accesoTodasSucursales(),
                dto.roles(),
                dto.sucursalIds(),
                dto.permisosGranulares(),
                operador
        );

        return toResponseDto(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDto cambiarEstadoActivo(Long id, boolean nuevoEstado, CustomUserDetails operador) {
        validarPermisoGestionUsuarios(operador);
        if (operador.getUsuarioId().equals(id) && !nuevoEstado) {
            throw new BusinessException("No puede desactivar su propio usuario en sesión.");
        }

        Usuario objetivo = buscarEntidadPorId(id);
        validarAlcanceOperadorSobreObjetivo(operador, objetivo);

        if (!nuevoEstado && objetivo.tieneRol(CodigoRol.ADMIN)) {
            validarNoEsUltimoAdminActivo(id);
        }

        objetivo.setActivo(nuevoEstado);
        return toResponseDto(usuarioRepository.save(objetivo));
    }

    @Transactional
    public UsuarioResponseDto desbloquearCuenta(Long id, CustomUserDetails operador) {
        validarPermisoGestionUsuarios(operador);
        Usuario objetivo = buscarEntidadPorId(id);
        validarAlcanceOperadorSobreObjetivo(operador, objetivo);

        objetivo.setIntentosFallidos(0);
        objetivo.setCuentaBloqueada(false);
        objetivo.setFechaBloqueo(null);

        return toResponseDto(usuarioRepository.save(objetivo));
    }

    @Transactional
    public UsuarioResponseDto blanquearPassword(Long id, ResetPasswordAdminRequestDto dto, CustomUserDetails operador) {
        validarPermisoGestionUsuarios(operador);
        Usuario objetivo = buscarEntidadPorId(id);
        validarAlcanceOperadorSobreObjetivo(operador, objetivo);

        objetivo.setPasswordHash(passwordEncoder.encode(dto.nuevaPasswordTemporal()));
        // Regla 4.2: Al blanquear la clave, el empleado deberá elegir su propia contraseña en su próximo ingreso
        objetivo.setRequiereCambioPassword(true);
        objetivo.setIntentosFallidos(0);
        objetivo.setCuentaBloqueada(false);
        objetivo.setFechaBloqueo(null);

        return toResponseDto(usuarioRepository.save(objetivo));
    }

    private void aplicarDatosYAsignaciones(
            Usuario usuario,
            String nombre,
            String apellido,
            String dni,
            String legajo,
            String email,
            String telefono,
            String direccion,
            java.time.LocalDate fechaIngreso,
            Boolean accesoTodasSucursalesReq,
            Set<CodigoRol> codigosRol,
            Set<Long> sucursalIds,
            Set<PermisoGranular> permisosReq,
            CustomUserDetails operador) {

        boolean operadorEsAdmin = operador.tieneRol(CodigoRol.ADMIN);
        Usuario entidadOperador = buscarEntidadPorId(operador.getUsuarioId());

        // Regla 3.1 B: Si el operador es Encargado (no Admin), validar que solo asigne roles inferiores y de sus sucursales
        if (!operadorEsAdmin) {
            for (CodigoRol rolSolicitado : codigosRol) {
                if (!ROLES_INFERIORES_PERMITIDOS_ENCARGADO.contains(rolSolicitado)) {
                    throw new BusinessException(
                            "Un Encargado de Sucursal solo puede asignar los roles CAJERO, DEPOSITO o LIMPIEZA.",
                            HttpStatus.FORBIDDEN
                    );
                }
            }
            if (Boolean.TRUE.equals(accesoTodasSucursalesReq)) {
                throw new BusinessException(
                        "Solo un Administrador General puede otorgar acceso a todas las sucursales.",
                        HttpStatus.FORBIDDEN
                );
            }
        }

        Set<Rol> rolesEntidad = new HashSet<>();
        for (CodigoRol cod : codigosRol) {
            Rol rol = rolRepository.findByCodigo(cod)
                    .orElseThrow(() -> new BusinessException("Rol inexistente: " + cod));
            rolesEntidad.add(rol);
        }

        boolean tendraRolAdmin = codigosRol.contains(CodigoRol.ADMIN);
        boolean accesoGlobal = tendraRolAdmin || (operadorEsAdmin && Boolean.TRUE.equals(accesoTodasSucursalesReq));

        Set<Sucursal> sucursalesAsignadas = new HashSet<>();
        if (accesoGlobal) {
            sucursalesAsignadas.addAll(sucursalRepository.findByActivoTrueOrderByCodigoAsc());
        } else {
            if (sucursalIds == null || sucursalIds.isEmpty()) {
                throw new BusinessException("Debe asignar al menos una sucursal habilitada al empleado.");
            }
            Set<Long> sucursalesOperadorIds = entidadOperador.getSucursalesHabilitadas().stream()
                    .map(Sucursal::getId)
                    .collect(Collectors.toSet());

            for (Long sucId : sucursalIds) {
                if (!operadorEsAdmin && !sucursalesOperadorIds.contains(sucId)) {
                    throw new BusinessException(
                            "No tiene permiso para asignar al empleado una sucursal fuera de su propio alcance.",
                            HttpStatus.FORBIDDEN
                    );
                }
                Sucursal s = sucursalRepository.findById(sucId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + sucId));
                sucursalesAsignadas.add(s);
            }
        }

        // Regla 1.1 B: Permisos granulares por usuario (si no se envían, usar los por defecto de los roles)
        Set<PermisoGranular> permisosFinales;
        if (tendraRolAdmin) {
            permisosFinales = EnumSet.allOf(PermisoGranular.class);
        } else if (permisosReq != null) {
            permisosFinales = EnumSet.noneOf(PermisoGranular.class);
            for (PermisoGranular p : permisosReq) {
                if (!operadorEsAdmin && (p.isExclusivoAdmin() || p == PermisoGranular.GESTIONAR_EMPLEADOS_SUCURSAL)) {
                    throw new BusinessException(
                            "No puede otorgar el permiso restringido: " + p.getDescripcion(),
                            HttpStatus.FORBIDDEN
                    );
                }
                permisosFinales.add(p);
            }
        } else {
            permisosFinales = PermisoGranular.permisosPorDefectoParaRoles(codigosRol);
        }

        usuario.setNombre(nombre.trim());
        usuario.setApellido(apellido.trim());
        usuario.setDni(dni);
        usuario.setLegajo(legajo);
        usuario.setEmail(email);
        usuario.setTelefono(limpiarTextoOpcional(telefono));
        usuario.setDireccion(limpiarTextoOpcional(direccion));
        usuario.setFechaIngreso(fechaIngreso);
        usuario.setAccesoTodasSucursales(accesoGlobal);
        usuario.setRoles(rolesEntidad);
        usuario.setSucursalesHabilitadas(sucursalesAsignadas);
        usuario.setPermisosGranulares(permisosFinales);
    }

    private void validarPermisoGestionUsuarios(CustomUserDetails operador) {
        if (!operador.tieneRol(CodigoRol.ADMIN)
                && !operador.tieneRol(CodigoRol.ENCARGADO_SUCURSAL)
                && !operador.tienePermiso(PermisoGranular.GESTIONAR_EMPLEADOS_SUCURSAL)) {
            throw new BusinessException("No tiene permisos para gestionar usuarios y empleados.", HttpStatus.FORBIDDEN);
        }
    }

    private void validarAlcanceOperadorSobreObjetivo(CustomUserDetails operador, Usuario objetivo) {
        if (operador.tieneRol(CodigoRol.ADMIN)) {
            return;
        }
        if (objetivo.tieneRol(CodigoRol.ADMIN) || objetivo.tieneRol(CodigoRol.ENCARGADO_SUCURSAL)) {
            throw new BusinessException(
                    "Un Encargado de Sucursal no puede modificar cuentas de Administradores ni de otros Encargados.",
                    HttpStatus.FORBIDDEN
            );
        }
        Usuario entidadOperador = buscarEntidadPorId(operador.getUsuarioId());
        Set<Long> sucursalesOperadorIds = entidadOperador.getSucursalesHabilitadas().stream()
                .map(Sucursal::getId)
                .collect(Collectors.toSet());

        boolean comparteSucursal = objetivo.getSucursalesHabilitadas().stream()
                .anyMatch(s -> sucursalesOperadorIds.contains(s.getId()));
        if (!comparteSucursal) {
            throw new BusinessException("El empleado seleccionado no pertenece a sus sucursales habilitadas.", HttpStatus.FORBIDDEN);
        }
    }

    private void validarNoEsUltimoAdminActivo(Long idUsuarioExcluido) {
        long otrosAdminsActivos = usuarioRepository.findAll().stream()
                .filter(u -> u.isActivo() && !u.getId().equals(idUsuarioExcluido) && u.tieneRol(CodigoRol.ADMIN))
                .count();
        if (otrosAdminsActivos == 0) {
            throw new BusinessException("No se puede desactivar ni quitar el rol al único Administrador General activo del sistema.");
        }
    }

    private Usuario buscarEntidadPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    private String limpiarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private UsuarioResponseDto toResponseDto(Usuario u) {
        Set<String> roles = u.getRoles().stream()
                .filter(Rol::isActivo)
                .map(r -> r.getCodigo().name())
                .collect(Collectors.toSet());

        Set<String> permisos = (u.tieneRol(CodigoRol.ADMIN)
                ? EnumSet.allOf(PermisoGranular.class)
                : u.getPermisosGranulares())
                .stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        List<SucursalResumenDto> sucursales = u.getSucursalesHabilitadas().stream()
                .sorted(Comparator.comparing(Sucursal::getCodigo))
                .map(s -> new SucursalResumenDto(
                        s.getId(),
                        s.getCodigo(),
                        s.getNombre(),
                        s.getPuntoVentaArca(),
                        s.isEsCasaCentral()
                ))
                .toList();

        return new UsuarioResponseDto(
                u.getId(),
                u.getUsername(),
                u.getNombre(),
                u.getApellido(),
                u.getDni(),
                u.getLegajo(),
                u.getEmail(),
                u.getTelefono(),
                u.getDireccion(),
                u.getFechaIngreso(),
                u.isAccesoTodasSucursales(),
                u.isRequiereCambioPassword(),
                u.getIntentosFallidos(),
                u.isCuentaBloqueada(),
                u.getFechaBloqueo(),
                u.getUltimoAcceso(),
                u.isActivo(),
                roles,
                permisos,
                sucursales,
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }
}
