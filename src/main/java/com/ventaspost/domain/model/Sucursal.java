package com.ventaspost.domain.model;

import com.ventaspost.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
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
}
