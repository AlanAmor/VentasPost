-- ============================================================================
-- V2: Fase 2 - Proveedores, Categorías, Marcas, Productos y Stock por Sucursal
-- ============================================================================

CREATE TABLE proveedores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    razon_social VARCHAR(150) NOT NULL,
    nombre_fantasia VARCHAR(150),
    cuit VARCHAR(13) UNIQUE,
    condicion_iva VARCHAR(40) NOT NULL DEFAULT 'RESPONSABLE_INSCRIPTO',
    telefono VARCHAR(60),
    email VARCHAR(120),
    direccion VARCHAR(255),
    localidad VARCHAR(100),
    provincia VARCHAR(100),
    observaciones VARCHAR(500),
    es_proveedor_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE marcas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_barras VARCHAR(60) NOT NULL UNIQUE,
    codigo_proveedor_sku VARCHAR(60),
    es_codigo_interno BOOLEAN NOT NULL DEFAULT FALSE,
    nombre VARCHAR(180) NOT NULL,
    descripcion VARCHAR(500),
    categoria_id BIGINT NOT NULL,
    marca_id BIGINT NOT NULL,
    proveedor_id BIGINT NOT NULL,
    unidad_medida VARCHAR(30) NOT NULL DEFAULT 'UNIDAD',
    precio_compra DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    iva_incluido_compra BOOLEAN NOT NULL DEFAULT TRUE,
    alicuota_iva DECIMAL(5, 2) NOT NULL DEFAULT 21.00,
    porcentaje_ganancia DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    precio_venta_general DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    stock_minimo_defecto DECIMAL(19, 3) NOT NULL DEFAULT 0.000,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT fk_productos_marca FOREIGN KEY (marca_id) REFERENCES marcas (id),
    CONSTRAINT fk_productos_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_productos_nombre ON productos (nombre);
CREATE INDEX idx_productos_filtro ON productos (categoria_id, marca_id, proveedor_id, activo);

-- Existencias, Stock Mínimo y Precio Personalizado por Sucursal (Reglas 1.1 y 1.2)
CREATE TABLE producto_sucursales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    stock_actual DECIMAL(19, 3) NOT NULL DEFAULT 0.000,
    stock_minimo DECIMAL(19, 3) NOT NULL DEFAULT 0.000,
    precio_venta_sucursal DECIMAL(19, 2) NULL,
    ubicacion_gondola VARCHAR(80),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uk_producto_sucursal UNIQUE (producto_id, sucursal_id),
    CONSTRAINT fk_prod_suc_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_prod_suc_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- DATOS SEMILLA OBLIGATORIOS (Reglas 4.1 y 4.2)
-- ============================================================================
INSERT INTO proveedores (
    codigo, razon_social, nombre_fantasia, condicion_iva,
    observaciones, es_proveedor_sistema, activo, created_by
) VALUES (
    'PROV-GEN',
    'PROVEEDOR GENERAL (POR DEFECTO)',
    'Proveedor General',
    'RESPONSABLE_INSCRIPTO',
    'Proveedor genérico del sistema para asociar artículos sin proveedor específico asignado.',
    TRUE,
    TRUE,
    'SYSTEM'
);

INSERT INTO categorias (nombre, descripcion, activo, created_by) VALUES
('General', 'Categoría general por defecto', TRUE, 'SYSTEM');

INSERT INTO marcas (nombre, descripcion, activo, created_by) VALUES
('Genérica', 'Marca genérica / sin marca especificada', TRUE, 'SYSTEM');
