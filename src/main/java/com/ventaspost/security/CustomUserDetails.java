package com.ventaspost.security;

import com.ventaspost.domain.model.CodigoRol;
import com.ventaspost.domain.model.PermisoGranular;
import com.ventaspost.domain.model.Rol;
import com.ventaspost.domain.model.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long usuarioId;
    private final String username;
    private final String password;
    private final String nombreCompleto;
    private final boolean activo;
    private final boolean cuentaBloqueada;
    private final boolean requiereCambioPassword;
    private final boolean accesoTodasSucursales;
    private final Set<CodigoRol> codigosRol;
    private final Set<PermisoGranular> permisosEfectivos;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Usuario usuario) {
        this.usuarioId = usuario.getId();
        this.username = usuario.getUsername();
        this.password = usuario.getPasswordHash();
        this.nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
        this.activo = usuario.isActivo();
        this.cuentaBloqueada = usuario.isCuentaBloqueada();
        this.requiereCambioPassword = usuario.isRequiereCambioPassword();
        this.accesoTodasSucursales = usuario.isAccesoTodasSucursales();
        this.codigosRol = usuario.getRoles().stream()
                .filter(Rol::isActivo)
                .map(Rol::getCodigo)
                .collect(Collectors.toSet());

        if (this.codigosRol.contains(CodigoRol.ADMIN)) {
            this.permisosEfectivos = EnumSet.allOf(PermisoGranular.class);
        } else {
            this.permisosEfectivos = usuario.getPermisosGranulares() != null && !usuario.getPermisosGranulares().isEmpty()
                    ? EnumSet.copyOf(usuario.getPermisosGranulares())
                    : EnumSet.noneOf(PermisoGranular.class);
        }

        Set<GrantedAuthority> auths = new HashSet<>();
        this.codigosRol.forEach(rol -> auths.add(new SimpleGrantedAuthority("ROLE_" + rol.name())));
        this.permisosEfectivos.forEach(perm -> auths.add(new SimpleGrantedAuthority("PERM_" + perm.name())));
        this.authorities = auths;
    }

    public boolean tieneRol(CodigoRol rol) {
        return codigosRol.contains(rol);
    }

    public boolean tienePermiso(PermisoGranular permiso) {
        return permisosEfectivos.contains(permiso);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !cuentaBloqueada;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
