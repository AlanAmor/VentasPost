package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "sucursales")
public class Sucursal extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "cuit", nullable = false, length = 13)
    private String cuit;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_iva", nullable = false, length = 40)
    private CondicionIva condicionIva;

    @Column(name = "punto_venta_arca", nullable = false)
    private Integer puntoVentaArca;

    @Column(name = "ingresos_brutos", length = 50)
    private String ingresosBrutos;

    @Column(name = "fecha_inicio_actividades")
    private LocalDate fechaInicioActividades;

    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    @Column(name = "localidad", length = 100)
    private String localidad;

    @Column(name = "provincia", length = 100)
    private String provincia;

    @Column(name = "telefono", length = 50)
    private String telefono;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "vende_al_publico", nullable = false)
    private boolean vendeAlPublico = true;

    @Column(name = "tiene_deposito", nullable = false)
    private boolean tieneDeposito = true;

    @Column(name = "es_casa_central", nullable = false)
    private boolean esCasaCentral = false;

    public Sucursal() {
    }

    public Long getId() {
        return this.id;
    }

    public String getCodigo() {
        return this.codigo;
    }

    public String getNombre() {
        return this.nombre;
    }

    public String getRazonSocial() {
        return this.razonSocial;
    }

    public String getCuit() {
        return this.cuit;
    }

    public CondicionIva getCondicionIva() {
        return this.condicionIva;
    }

    public Integer getPuntoVentaArca() {
        return this.puntoVentaArca;
    }

    public String getIngresosBrutos() {
        return this.ingresosBrutos;
    }

    public LocalDate getFechaInicioActividades() {
        return this.fechaInicioActividades;
    }

    public String getDireccion() {
        return this.direccion;
    }

    public String getLocalidad() {
        return this.localidad;
    }

    public String getProvincia() {
        return this.provincia;
    }

    public String getTelefono() {
        return this.telefono;
    }

    public String getEmail() {
        return this.email;
    }

    public boolean isVendeAlPublico() {
        return this.vendeAlPublico;
    }

    public boolean isTieneDeposito() {
        return this.tieneDeposito;
    }

    public boolean isEsCasaCentral() {
        return this.esCasaCentral;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public void setCondicionIva(CondicionIva condicionIva) {
        this.condicionIva = condicionIva;
    }

    public void setPuntoVentaArca(Integer puntoVentaArca) {
        this.puntoVentaArca = puntoVentaArca;
    }

    public void setIngresosBrutos(String ingresosBrutos) {
        this.ingresosBrutos = ingresosBrutos;
    }

    public void setFechaInicioActividades(LocalDate fechaInicioActividades) {
        this.fechaInicioActividades = fechaInicioActividades;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setVendeAlPublico(boolean vendeAlPublico) {
        this.vendeAlPublico = vendeAlPublico;
    }

    public void setTieneDeposito(boolean tieneDeposito) {
        this.tieneDeposito = tieneDeposito;
    }

    public void setEsCasaCentral(boolean esCasaCentral) {
        this.esCasaCentral = esCasaCentral;
    }

}
