package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "configuracion_medios_pago")
public class ConfiguracionMedioPago extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private CodigoMedioPago codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "porcentaje_ajuste", nullable = false, precision = 8, scale = 2)
    private BigDecimal porcentajeAjuste = BigDecimal.ZERO;

    @Column(name = "requiere_referencia", nullable = false)
    private boolean requiereReferencia = false;

    @Column(name = "permite_vuelto", nullable = false)
    private boolean permiteVuelto = false;

    public ConfiguracionMedioPago() {
    }

    public Long getId() {
        return this.id;
    }

    public CodigoMedioPago getCodigo() {
        return this.codigo;
    }

    public String getNombre() {
        return this.nombre;
    }

    public BigDecimal getPorcentajeAjuste() {
        return this.porcentajeAjuste;
    }

    public boolean isRequiereReferencia() {
        return this.requiereReferencia;
    }

    public boolean isPermiteVuelto() {
        return this.permiteVuelto;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCodigo(CodigoMedioPago codigo) {
        this.codigo = codigo;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setPorcentajeAjuste(BigDecimal porcentajeAjuste) {
        this.porcentajeAjuste = porcentajeAjuste;
    }

    public void setRequiereReferencia(boolean requiereReferencia) {
        this.requiereReferencia = requiereReferencia;
    }

    public void setPermiteVuelto(boolean permiteVuelto) {
        this.permiteVuelto = permiteVuelto;
    }

}
