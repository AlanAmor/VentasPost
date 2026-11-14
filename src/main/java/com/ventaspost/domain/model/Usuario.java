package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario extends AuditableEntity {

    public static final int MAX_INTENTOS_FALLIDOS = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "dni", nullable = false, unique = true, length = 20)
    private String dni;

    @Column(name = "legajo", unique = true, length = 40)
    private String legajo;

    @Column(name = "email", unique = true, length = 120)
    private String email;

    @Column(name = "telefono", length = 50)
    private String telefono;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "acceso_todas_sucursales", nullable = false)
    private boolean accesoTodasSucursales = false;

    @Column(name = "requiere_cambio_password", nullable = false)
    private boolean requiereCambioPassword = true;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "cuenta_bloqueada", nullable = false)
    private boolean cuentaBloqueada = false;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_sucursales",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "sucursal_id")
    )
    private Set<Sucursal> sucursalesHabilitadas = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "usuario_permisos",
            joinColumns = @JoinColumn(name = "usuario_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permiso", nullable = false, length = 60)
    private Set<PermisoGranular> permisosGranulares = new HashSet<>();

    public boolean tieneRol(CodigoRol codigoRol) {
        return roles.stream().anyMatch(r -> r.isActivo() && r.getCodigo() == codigoRol);
    }

    public boolean tienePermiso(PermisoGranular permiso) {
        if (tieneRol(CodigoRol.ADMIN)) {
            return true;
        }
        return permisosGranulares.contains(permiso);
    }
}
