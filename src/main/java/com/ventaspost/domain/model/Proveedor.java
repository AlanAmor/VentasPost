package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "proveedores")
public class Proveedor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "nombre_fantasia", length = 150)
    private String nombreFantasia;

    @Column(name = "cuit", unique = true, length = 13)
    private String cuit;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_iva", nullable = false, length = 40)
    private CondicionIva condicionIva = CondicionIva.RESPONSABLE_INSCRIPTO;

    @Column(name = "telefono", length = 60)
    private String telefono;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "localidad", length = 100)
    private String localidad;

    @Column(name = "provincia", length = 100)
    private String provincia;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "es_proveedor_sistema", nullable = false)
    private boolean esProveedorSistema = false;

    public Proveedor() {
    }

    public Long getId() {
        return this.id;
    }

    public String getCodigo() {
        return this.codigo;
    }

    public String getRazonSocial() {
        return this.razonSocial;
    }

    public String getNombreFantasia() {
        return this.nombreFantasia;
    }

    public String getCuit() {
        return this.cuit;
    }

    public CondicionIva getCondicionIva() {
        return this.condicionIva;
    }

    public String getTelefono() {
        return this.telefono;
    }

    public String getEmail() {
        return this.email;
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

    public String getObservaciones() {
        return this.observaciones;
    }

    public boolean isEsProveedorSistema() {
        return this.esProveedorSistema;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public void setNombreFantasia(String nombreFantasia) {
        this.nombreFantasia = nombreFantasia;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public void setCondicionIva(CondicionIva condicionIva) {
        this.condicionIva = condicionIva;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setEsProveedorSistema(boolean esProveedorSistema) {
        this.esProveedorSistema = esProveedorSistema;
    }

}
