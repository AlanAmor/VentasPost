-- =====================================================================
-- V4: CONFIGURACIÓN ARCA (WSAA / WSFEv1), FACTURACIÓN ELECTRÓNICA,
--     NOTAS DE CRÉDITO AUTOMÁTICAS, HISTORIAL DE REINTENTOS Y QR OFICIAL
-- =====================================================================

-- 1. Columnas adicionales de seguimiento ARCA en tabla ventas
ALTER TABLE ventas
    ADD COLUMN ultimo_error_arca VARCHAR(500) NULL AFTER estado_facturacion,
    ADD COLUMN cantidad_intentos_arca INT NOT NULL DEFAULT 0 AFTER ultimo_error_arca;

-- 2. Tabla de Configuración ARCA por Sucursal
CREATE TABLE configuracion_arca_sucursal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sucursal_id BIGINT NOT NULL UNIQUE,
    razon_social_emisor VARCHAR(160) NOT NULL,
    cuit_emisor VARCHAR(13) NOT NULL,
    punto_venta_arca INT NOT NULL,
    condicion_iva_emisor VARCHAR(40) NOT NULL,
    ingresos_brutos VARCHAR(50) NULL,
    fecha_inicio_actividades DATE NULL,
    domicilio_comercial VARCHAR(220) NOT NULL,
    entorno_arca VARCHAR(25) NOT NULL DEFAULT 'HOMOLOGACION',
    simulador_homologacion_activo TINYINT(1) NOT NULL DEFAULT 1,
    certificado_crt_pem LONGTEXT NULL,
    clave_privada_key_pem LONGTEXT NULL,
    nombre_archivo_crt VARCHAR(150) NULL,
    nombre_archivo_key VARCHAR(150) NULL,
    monto_tope_consumidor_final_sin_dni DECIMAL(19, 2) NOT NULL DEFAULT 344488.00,
    plazo_maximo_dias_facturacion INT NOT NULL DEFAULT 30,
    wsaa_token_cache LONGTEXT NULL,
    wsaa_sign_cache LONGTEXT NULL,
    wsaa_expiracion_cache DATETIME NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(60) NULL,
    updated_by VARCHAR(60) NULL,
    CONSTRAINT fk_cfg_arca_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Tabla de Comprobantes Fiscales Emitidos en ARCA (Facturas A/B/C y Notas de Crédito A/B/C)
CREATE TABLE comprobantes_fiscales_arca (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    comprobante_asociado_id BIGINT NULL,
    naturaleza_comprobante VARCHAR(30) NOT NULL,
    tipo_comprobante VARCHAR(35) NOT NULL,
    codigo_afip_comprobante INT NOT NULL,
    letra_comprobante VARCHAR(5) NOT NULL,
    entorno_arca VARCHAR(25) NOT NULL,
    modo_emision VARCHAR(30) NOT NULL,
    cuit_emisor VARCHAR(13) NOT NULL,
    razon_social_emisor VARCHAR(160) NOT NULL,
    condicion_iva_emisor VARCHAR(40) NOT NULL,
    ingresos_brutos_emisor VARCHAR(50) NULL,
    fecha_inicio_actividades_emisor DATE NULL,
    domicilio_comercial_emisor VARCHAR(220) NOT NULL,
    punto_venta INT NOT NULL,
    numero_comprobante BIGINT NOT NULL,
    numero_formateado VARCHAR(25) NOT NULL,
    fecha_emision DATE NOT NULL,
    receptor_condicion_iva VARCHAR(40) NOT NULL,
    receptor_codigo_afip_iva INT NOT NULL,
    receptor_tipo_doc_afip INT NOT NULL,
    receptor_numero_doc VARCHAR(20) NOT NULL,
    receptor_nombre_razon_social VARCHAR(160) NOT NULL,
    receptor_domicilio VARCHAR(220) NULL,
    importe_neto_gravado DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    importe_exento DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    importe_iva_total DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    importe_iva_21 DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    importe_iva_105 DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    importe_iva_27 DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    importe_iva_contenido_transparencia DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    importe_total DECIMAL(19, 2) NOT NULL,
    cae VARCHAR(30) NOT NULL,
    cae_fecha_vencimiento DATE NOT NULL,
    qr_url_oficial_arca VARCHAR(1200) NOT NULL,
    qr_base64_payload VARCHAR(900) NOT NULL,
    observaciones_arca VARCHAR(500) NULL,
    usuario_emisor_username VARCHAR(60) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(60) NULL,
    updated_by VARCHAR(60) NULL,
    CONSTRAINT fk_comp_arca_venta FOREIGN KEY (venta_id) REFERENCES ventas (id),
    CONSTRAINT fk_comp_arca_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id),
    CONSTRAINT fk_comp_arca_asociado FOREIGN KEY (comprobante_asociado_id) REFERENCES comprobantes_fiscales_arca (id),
    CONSTRAINT uk_comp_arca_pv_tipo_nro UNIQUE (cuit_emisor, punto_venta, codigo_afip_comprobante, numero_comprobante, entorno_arca)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_comp_arca_venta ON comprobantes_fiscales_arca (venta_id);
CREATE INDEX idx_comp_arca_sucursal_fecha ON comprobantes_fiscales_arca (sucursal_id, fecha_emision);

-- 4. Tabla de Auditoría de Intentos y Reintentos de Comunicación con ARCA
CREATE TABLE intentos_facturacion_arca (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    fecha_intento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    naturaleza_operacion VARCHAR(30) NOT NULL,
    entorno_arca VARCHAR(25) NOT NULL,
    modo_emision VARCHAR(30) NOT NULL,
    exitoso TINYINT(1) NOT NULL DEFAULT 0,
    codigo_respuesta VARCHAR(60) NULL,
    mensaje_detalle VARCHAR(600) NOT NULL,
    cae_obtenido VARCHAR(30) NULL,
    numero_comprobante_obtenido VARCHAR(25) NULL,
    usuario_username VARCHAR(60) NOT NULL,
    CONSTRAINT fk_intento_arca_venta FOREIGN KEY (venta_id) REFERENCES ventas (id),
    CONSTRAINT fk_intento_arca_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_intento_arca_venta ON intentos_facturacion_arca (venta_id, fecha_intento);

-- 5. Semilla inicial de Configuración ARCA para las sucursales existentes (Modo Homologación Asistido habilitado)
INSERT INTO configuracion_arca_sucursal (
    sucursal_id,
    razon_social_emisor,
    cuit_emisor,
    punto_venta_arca,
    condicion_iva_emisor,
    ingresos_brutos,
    fecha_inicio_actividades,
    domicilio_comercial,
    entorno_arca,
    simulador_homologacion_activo,
    monto_tope_consumidor_final_sin_dni,
    plazo_maximo_dias_facturacion,
    activo,
    created_by,
    updated_by
)
SELECT
    s.id,
    CONCAT('VENTASPOST POLIRUBRO - ', s.nombre),
    s.cuit,
    s.punto_venta_arca,
    'RESPONSABLE_INSCRIPTO',
    '901-000000-1',
    '2024-01-01',
    s.direccion,
    'HOMOLOGACION',
    1,
    344488.00,
    30,
    1,
    'SYSTEM',
    'SYSTEM'
FROM sucursales s
WHERE NOT EXISTS (
    SELECT 1 FROM configuracion_arca_sucursal c WHERE c.sucursal_id = s.id
);
