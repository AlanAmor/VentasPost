package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "roles")
public class Rol extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private CodigoRol codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "valor_hora_general", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorHoraGeneral = new BigDecimal("3500.00");

    public Rol() {
    }

    public Long getId() {
        return this.id;
    }

    public CodigoRol getCodigo() {
        return this.codigo;
    }

    public String getNombre() {
        return this.nombre;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public BigDecimal getValorHoraGeneral() {
        return this.valorHoraGeneral;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCodigo(CodigoRol codigo) {
        this.codigo = codigo;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setValorHoraGeneral(BigDecimal valorHoraGeneral) {
        this.valorHoraGeneral = valorHoraGeneral;
    }

}
