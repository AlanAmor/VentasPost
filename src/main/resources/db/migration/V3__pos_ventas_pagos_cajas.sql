-- ============================================================================
-- V3: Fase 3 - Medios de Pago, Turnos/Movimientos de Caja y Punto de Venta (POS)
-- ============================================================================

-- Configuración de Medios de Pago (Reglas 1.3 y 2.1 A: Recargo/Descuento configurable por Admin)
CREATE TABLE configuracion_medios_pago (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL,
    -- Positivo = % Recargo automático | Negativo = % Descuento automático | 0.00 = Neutro
    porcentaje_ajuste DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    requiere_referencia BOOLEAN NOT NULL DEFAULT FALSE,
    permite_vuelto BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Turnos de Caja por Usuario y Sucursal (Reglas 4.1, 4.2 A y 4.4 A)
CREATE TABLE turnos_caja (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sucursal_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    fecha_apertura DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre DATETIME NULL,
    monto_inicial_efectivo DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_ventas_efectivo DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_ventas_transferencia DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_ventas_qr DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_ventas_debito DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_ventas_credito DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_ingresos_manuales DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_egresos_manuales DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    efectivo_esperado DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    efectivo_declarado_cierre DECIMAL(19, 2) NULL,
    diferencia_efectivo DECIMAL(19, 2) NULL,
    observaciones_apertura VARCHAR(300),
    observaciones_cierre VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_turnos_caja_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id),
    CONSTRAINT fk_turnos_caja_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_turnos_caja_busqueda ON turnos_caja (sucursal_id, usuario_id, estado);

-- Movimientos Manuales de Caja (Ingresos y Egresos/Retiros durante el turno - Regla 4.3)
CREATE TABLE movimientos_caja (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    turno_caja_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL, -- INGRESO | EGRESO
    monto DECIMAL(19, 2) NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    fecha_movimiento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_mov_caja_turno FOREIGN KEY (turno_caja_id) REFERENCES turnos_caja (id),
    CONSTRAINT fk_mov_caja_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id),
    CONSTRAINT fk_mov_caja_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cabecera de Ventas (Reglas 2.2, 5.1 y 5.2)
CREATE TABLE ventas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_comprobante_interno VARCHAR(40) NOT NULL UNIQUE,
    sucursal_id BIGINT NOT NULL,
    turno_caja_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    fecha_venta DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Datos de cliente (Consumidor Final por defecto, o datos para facturación posterior)
    cliente_nombre VARCHAR(150) NOT NULL DEFAULT 'Consumidor Final',
    cliente_documento VARCHAR(20),
    cliente_condicion_iva VARCHAR(40) NOT NULL DEFAULT 'CONSUMIDOR_FINAL',
    -- Importes
    subtotal_items DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    descuento_manual_porcentaje DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    descuento_manual_monto DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    ajuste_medios_pago_monto DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_final DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    -- Estados independientes (Regla 5.1)
    estado_venta VARCHAR(30) NOT NULL DEFAULT 'REALIZADA', -- REALIZADA | ANULADA
    estado_facturacion VARCHAR(30) NOT NULL DEFAULT 'NO_FACTURADA', -- NO_FACTURADA | FACTURADA
    supervisor_autorizador_username VARCHAR(60),
    motivo_anulacion VARCHAR(300),
    fecha_anulacion DATETIME NULL,
    usuario_anulacion_username VARCHAR(60),
    observaciones VARCHAR(300),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_ventas_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id),
    CONSTRAINT fk_ventas_turno_caja FOREIGN KEY (turno_caja_id) REFERENCES turnos_caja (id),
    CONSTRAINT fk_ventas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ventas_sucursal_fecha ON ventas (sucursal_id, fecha_venta, estado_venta, estado_facturacion);

-- Detalle de Ítems Vendidos
CREATE TABLE venta_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    codigo_barras_historico VARCHAR(60) NOT NULL,
    nombre_producto_historico VARCHAR(180) NOT NULL,
    unidad_medida VARCHAR(30) NOT NULL,
    cantidad DECIMAL(19, 3) NOT NULL,
    precio_unitario DECIMAL(19, 2) NOT NULL,
    alicuota_iva DECIMAL(5, 2) NOT NULL DEFAULT 21.00,
    descuento_item_porcentaje DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    descuento_item_monto DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    subtotal_neto_item DECIMAL(19, 2) NOT NULL,
    stock_previo_sucursal DECIMAL(19, 3) NOT NULL,
    stock_posterior_sucursal DECIMAL(19, 3) NOT NULL,
    vendido_sin_stock BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_venta_items_venta FOREIGN KEY (venta_id) REFERENCES ventas (id),
    CONSTRAINT fk_venta_items_producto FOREIGN KEY (producto_id) REFERENCES productos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Desglose de Pagos Combinados por Venta (Reglas 1.1 A, 1.2, 1.3 y 2.1 A)
CREATE TABLE venta_pagos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    medio_pago VARCHAR(40) NOT NULL,
    monto_base DECIMAL(19, 2) NOT NULL,
    porcentaje_ajuste_aplicado DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    monto_ajuste DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    monto_final_cobrado DECIMAL(19, 2) NOT NULL,
    monto_recibido_efectivo DECIMAL(19, 2) NULL,
    vuelto_efectivo DECIMAL(19, 2) NULL,
    cuotas INT NOT NULL DEFAULT 1,
    referencia_operacion VARCHAR(100),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_venta_pagos_venta FOREIGN KEY (venta_id) REFERENCES ventas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- DATOS SEMILLA: Medios de Pago Habilitados (Reglas 1.3 y 2.1 A)
-- ============================================================================
INSERT INTO configuracion_medios_pago (
    codigo, nombre, porcentaje_ajuste, requiere_referencia, permite_vuelto, activo, created_by
) VALUES
('EFECTIVO', 'Efectivo', 0.00, FALSE, TRUE, TRUE, 'SYSTEM'),
('TRANSFERENCIA', 'Transferencia Bancaria', 0.00, FALSE, FALSE, TRUE, 'SYSTEM'),
('QR_BILLETERA', 'QR / Billetera Virtual (MP / MODO)', 0.00, FALSE, FALSE, TRUE, 'SYSTEM'),
('TARJETA_DEBITO', 'Tarjeta de Débito', 0.00, FALSE, FALSE, TRUE, 'SYSTEM'),
('TARJETA_CREDITO', 'Tarjeta de Crédito', 0.00, FALSE, FALSE, TRUE, 'SYSTEM');
