package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

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

    @Column(name = "valor_hora_personalizado", precision = 19, scale = 2)
    private BigDecimal valorHoraPersonalizado;

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

    /**
     * Regla 2.1: Retorna el valor hora personalizado si está definido, o el mayor valor hora entre sus roles activos.
     */
    public BigDecimal obtenerValorHoraEfectivo() {
        if (valorHoraPersonalizado != null && valorHoraPersonalizado.compareTo(BigDecimal.ZERO) > 0) {
            return valorHoraPersonalizado;
        }
        return roles.stream()
                .filter(Rol::isActivo)
                .map(Rol::getValorHoraGeneral)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.naturalOrder())
                .orElse(new BigDecimal("3500.00"));
    }

    public String obtenerOrigenValorHora() {
        if (valorHoraPersonalizado != null && valorHoraPersonalizado.compareTo(BigDecimal.ZERO) > 0) {
            return "Tarifa Personalizada del Empleado";
        }
        return roles.stream()
                .filter(Rol::isActivo)
                .max(Comparator.comparing(r -> r.getValorHoraGeneral() != null ? r.getValorHoraGeneral() : BigDecimal.ZERO))
                .map(r -> "Tarifa General por Rol (" + r.getNombre() + ")")
                .orElse("Tarifa General Base");
    }

    public Usuario() {
    }

    public Long getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public String getNombre() {
        return this.nombre;
    }

    public String getApellido() {
        return this.apellido;
    }

    public String getDni() {
        return this.dni;
    }

    public String getLegajo() {
        return this.legajo;
    }

    public String getEmail() {
        return this.email;
    }

    public String getTelefono() {
        return this.telefono;
    }

    public String getDireccion() {
        return this.direccion;
    }

    public LocalDate getFechaIngreso() {
        return this.fechaIngreso;
    }

    public BigDecimal getValorHoraPersonalizado() {
        return this.valorHoraPersonalizado;
    }

    public boolean isAccesoTodasSucursales() {
        return this.accesoTodasSucursales;
    }

    public boolean isRequiereCambioPassword() {
        return this.requiereCambioPassword;
    }

    public int getIntentosFallidos() {
        return this.intentosFallidos;
    }

    public boolean isCuentaBloqueada() {
        return this.cuentaBloqueada;
    }

    public LocalDateTime getFechaBloqueo() {
        return this.fechaBloqueo;
    }

    public LocalDateTime getUltimoAcceso() {
        return this.ultimoAcceso;
    }

    public Set<Rol> getRoles() {
        return this.roles;
    }

    public Set<Sucursal> getSucursalesHabilitadas() {
        return this.sucursalesHabilitadas;
    }

    public Set<PermisoGranular> getPermisosGranulares() {
        return this.permisosGranulares;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public void setLegajo(String legajo) {
        this.legajo = legajo;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public void setValorHoraPersonalizado(BigDecimal valorHoraPersonalizado) {
        this.valorHoraPersonalizado = valorHoraPersonalizado;
    }

    public void setAccesoTodasSucursales(boolean accesoTodasSucursales) {
        this.accesoTodasSucursales = accesoTodasSucursales;
    }

    public void setRequiereCambioPassword(boolean requiereCambioPassword) {
        this.requiereCambioPassword = requiereCambioPassword;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public void setCuentaBloqueada(boolean cuentaBloqueada) {
        this.cuentaBloqueada = cuentaBloqueada;
    }

    public void setFechaBloqueo(LocalDateTime fechaBloqueo) {
        this.fechaBloqueo = fechaBloqueo;
    }

    public void setUltimoAcceso(LocalDateTime ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    public void setRoles(Set<Rol> roles) {
        this.roles = roles;
    }

    public void setSucursalesHabilitadas(Set<Sucursal> sucursalesHabilitadas) {
        this.sucursalesHabilitadas = sucursalesHabilitadas;
    }

    public void setPermisosGranulares(Set<PermisoGranular> permisosGranulares) {
        this.permisosGranulares = permisosGranulares;
    }

}
