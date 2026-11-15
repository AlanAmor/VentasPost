-- =====================================================================
-- V6: FASE 6 - TARIFAS POR HORA (POR ROL Y PERSONALIZADA), HORARIOS
--     ASIGNADOS, FICHAJE/ASISTENCIA, ADELANTOS Y LIQUIDACIÓN DE SUELDOS
-- =====================================================================

-- 1. Valor por Hora General en Roles y Valor por Hora Personalizado en Usuarios (Regla 2.1)
ALTER TABLE roles
    ADD COLUMN valor_hora_general DECIMAL(19, 2) NOT NULL DEFAULT 3500.00 AFTER descripcion;

UPDATE roles SET valor_hora_general = 6000.00 WHERE codigo = 'ADMIN';
UPDATE roles SET valor_hora_general = 5000.00 WHERE codigo = 'ENCARGADO_SUCURSAL';
UPDATE roles SET valor_hora_general = 4000.00 WHERE codigo = 'CAJERO';
UPDATE roles SET valor_hora_general = 3800.00 WHERE codigo = 'DEPOSITO';
UPDATE roles SET valor_hora_general = 3400.00 WHERE codigo = 'LIMPIEZA';

ALTER TABLE usuarios
    ADD COLUMN valor_hora_personalizado DECIMAL(19, 2) NULL AFTER fecha_ingreso;

-- 2. Tabla de Configuración de Recargos Horarios (Horas Extras, Feriados/Domingos y Nocturnas - Regla 2.2 A)
CREATE TABLE configuracion_laboral_sueldos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    porcentaje_recargo_extra_simple DECIMAL(8, 2) NOT NULL DEFAULT 50.00,
    porcentaje_recargo_feriado_domingo DECIMAL(8, 2) NOT NULL DEFAULT 100.00,
    porcentaje_recargo_nocturno DECIMAL(8, 2) NOT NULL DEFAULT 15.00,
    hora_inicio_nocturno TIME NOT NULL DEFAULT '21:00:00',
    hora_fin_nocturno TIME NOT NULL DEFAULT '06:00:00',
    activo TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(60) NULL,
    updated_by VARCHAR(60) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO configuracion_laboral_sueldos (
    porcentaje_recargo_extra_simple,
    porcentaje_recargo_feriado_domingo,
    porcentaje_recargo_nocturno,
    hora_inicio_nocturno,
    hora_fin_nocturno,
    activo,
    created_by,
    updated_by
) VALUES (50.00, 100.00, 15.00, '21:00:00', '06:00:00', 1, 'SYSTEM', 'SYSTEM');

-- 3. Tabla de Horarios Semanales Asignados por Empleado y Sucursal (Regla 1.1 A)
CREATE TABLE horarios_asignados_empleado (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    dia_semana INT NOT NULL, -- 1 = Lunes .. 7 = Domingo
    hora_entrada TIME NOT NULL,
    hora_salida TIME NOT NULL,
    horas_teoricas DECIMAL(8, 2) NOT NULL,
    observaciones VARCHAR(255) NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(60) NULL,
    updated_by VARCHAR(60) NULL,
    CONSTRAINT fk_horario_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_horario_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_horario_usuario_dia ON horarios_asignados_empleado (usuario_id, dia_semana, activo);

-- 4. Cabecera de Liquidaciones de Sueldos por Hora (Reglas 3.1 y 3.2)
CREATE TABLE liquidaciones_sueldos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_recibo_interno VARCHAR(40) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    fecha_desde DATE NOT NULL,
    fecha_hasta DATE NOT NULL,
    fecha_liquidacion DATE NOT NULL,
    valor_hora_base_aplicado DECIMAL(19, 2) NOT NULL,
    origen_valor_hora VARCHAR(100) NOT NULL,
    porcentaje_extra_aplicado DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    porcentaje_feriado_aplicado DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    porcentaje_nocturno_aplicado DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    total_horas_normales DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_horas_extras DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_horas_feriado DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_horas_nocturnas DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_horas_trabajadas DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    importe_horas_normales DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    importe_horas_extras DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    importe_horas_feriado DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    importe_adicional_nocturno DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    subtotal_haberes_horas DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_bonos_adicionales DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_adelantos_descontados DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_otros_descuentos DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    neto_a_cobrar DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    estado VARCHAR(25) NOT NULL DEFAULT 'PAGADA', -- PAGADA | ANULADA
    medio_pago VARCHAR(40) NOT NULL DEFAULT 'EFECTIVO',
    fecha_pago DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observaciones VARCHAR(400) NULL,
    liquidado_por_username VARCHAR(60) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(60) NULL,
    updated_by VARCHAR(60) NULL,
    CONSTRAINT fk_liq_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_liq_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_liq_usuario_fechas ON liquidaciones_sueldos (usuario_id, fecha_hasta, estado);

-- 5. Tabla de Registro de Asistencias y Jornadas Efectivamente Trabajadas (Fichaje o Generación desde Horario Teórico)
CREATE TABLE asistencias_jornadas_trabajo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    fecha_jornada DATE NOT NULL,
    origen_registro VARCHAR(30) NOT NULL, -- FICHAJE_RELOJ | HORARIO_TEORICO | CARGA_MANUAL
    estado_jornada VARCHAR(35) NOT NULL,  -- EN_CURSO | TRABAJADA | INASISTENCIA_JUSTIFICADA | INASISTENCIA_INJUSTIFICADA
    hora_entrada_programada TIME NULL,
    hora_salida_programada TIME NULL,
    fecha_hora_ingreso_real DATETIME NULL,
    fecha_hora_egreso_real DATETIME NULL,
    es_feriado_o_domingo TINYINT(1) NOT NULL DEFAULT 0,
    horas_normales DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    horas_extras_simples DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    horas_feriado_domingo DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    horas_nocturnas DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    total_horas_computadas DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    liquidada TINYINT(1) NOT NULL DEFAULT 0,
    liquidacion_id BIGINT NULL,
    observaciones VARCHAR(300) NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(60) NULL,
    updated_by VARCHAR(60) NULL,
    CONSTRAINT fk_asist_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_asist_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id),
    CONSTRAINT fk_asist_liquidacion FOREIGN KEY (liquidacion_id) REFERENCES liquidaciones_sueldos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_asist_usuario_fecha ON asistencias_jornadas_trabajo (usuario_id, fecha_jornada, liquidada);

-- 6. Tabla de Adelantos / Vales de Sueldo (Regla 3.1 A)
CREATE TABLE adelantos_sueldo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    fecha_adelanto DATE NOT NULL,
    monto DECIMAL(19, 2) NOT NULL,
    medio_pago VARCHAR(40) NOT NULL DEFAULT 'EFECTIVO',
    motivo VARCHAR(255) NOT NULL,
    descontado TINYINT(1) NOT NULL DEFAULT 0,
    liquidacion_id BIGINT NULL,
    registrado_por_username VARCHAR(60) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(60) NULL,
    updated_by VARCHAR(60) NULL,
    CONSTRAINT fk_adelanto_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_adelanto_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id),
    CONSTRAINT fk_adelanto_liquidacion FOREIGN KEY (liquidacion_id) REFERENCES liquidaciones_sueldos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Conceptos Adicionales y Deducciones de cada Liquidación (Bonos, Premios, Adelantos, Descuentos)
CREATE TABLE liquidacion_conceptos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    liquidacion_id BIGINT NOT NULL,
    tipo_concepto VARCHAR(35) NOT NULL, -- HABER_ADICIONAL | DESCUENTO_ADELANTO | DESCUENTO_MANUAL
    descripcion VARCHAR(200) NOT NULL,
    monto DECIMAL(19, 2) NOT NULL,
    adelanto_id BIGINT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(60) NULL,
    updated_by VARCHAR(60) NULL,
    CONSTRAINT fk_liq_conc_liq FOREIGN KEY (liquidacion_id) REFERENCES liquidaciones_sueldos (id),
    CONSTRAINT fk_liq_conc_adelanto FOREIGN KEY (adelanto_id) REFERENCES adelantos_sueldo (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
