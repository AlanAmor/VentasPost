-- ============================================================================
-- V1: Esquema Base Fase 1 - Sucursales, Roles, Permisos Granulares y Usuarios
-- ============================================================================

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sucursales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    razon_social VARCHAR(150) NOT NULL,
    cuit VARCHAR(13) NOT NULL,
    condicion_iva VARCHAR(40) NOT NULL,
    punto_venta_arca INT NOT NULL,
    ingresos_brutos VARCHAR(50),
    fecha_inicio_actividades DATE,
    direccion VARCHAR(255) NOT NULL,
    localidad VARCHAR(100),
    provincia VARCHAR(100),
    telefono VARCHAR(50),
    email VARCHAR(120),
    vende_al_publico BOOLEAN NOT NULL DEFAULT TRUE,
    tiene_deposito BOOLEAN NOT NULL DEFAULT TRUE,
    es_casa_central BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT chk_punto_venta_arca CHECK (punto_venta_arca >= 1 AND punto_venta_arca <= 99999),
    -- Regla 5.1: Unicidad fiscal sobre (cuit, punto_venta_arca)
    CONSTRAINT uk_sucursal_cuit_pv UNIQUE (cuit, punto_venta_arca)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(60) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    dni VARCHAR(20) NOT NULL UNIQUE,
    legajo VARCHAR(40) UNIQUE,
    email VARCHAR(120) UNIQUE,
    telefono VARCHAR(50),
    direccion VARCHAR(255),
    fecha_ingreso DATE,
    acceso_todas_sucursales BOOLEAN NOT NULL DEFAULT FALSE,
    -- Regla 4.2: Forzar elección de contraseña en primer ingreso o tras blanqueo
    requiere_cambio_password BOOLEAN NOT NULL DEFAULT TRUE,
    -- Regla 4.3: Bloqueo automático a los 8 intentos fallidos consecutivos
    intentos_fallidos INT NOT NULL DEFAULT 0,
    cuenta_bloqueada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_bloqueo DATETIME NULL,
    ultimo_acceso DATETIME NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Relación Muchos a Muchos: Un empleado puede tener más de un rol
CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_usuario_roles_rol FOREIGN KEY (rol_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Relación Muchos a Muchos: Sucursales habilitadas por usuario (Opción 2B)
CREATE TABLE usuario_sucursales (
    usuario_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, sucursal_id),
    CONSTRAINT fk_usuario_sucursales_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_usuario_sucursales_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Permisos Granulares por Usuario (Opción 1.1 B: además del rol, permisos específicos activables)
CREATE TABLE usuario_permisos (
    usuario_id BIGINT NOT NULL,
    permiso VARCHAR(60) NOT NULL,
    PRIMARY KEY (usuario_id, permiso),
    CONSTRAINT fk_usuario_permisos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- DATOS INICIALES: Roles del Sistema (2.2)
-- ============================================================================
INSERT INTO roles (codigo, nombre, descripcion, activo, created_by) VALUES
('ADMIN', 'Administrador General', 'Acceso total al sistema y configuración fiscal/multi-sucursal', TRUE, 'SYSTEM'),
('ENCARGADO_SUCURSAL', 'Encargado de Sucursal', 'Supervisión operativa, empleados locales, precios de sucursal, stock y cajas', TRUE, 'SYSTEM'),
('CAJERO', 'Cajero', 'Operación de punto de venta, facturación y arqueo de su caja', TRUE, 'SYSTEM'),
('DEPOSITO', 'Operador de Depósito', 'Recepción de mercadería, control de stock y transferencias', TRUE, 'SYSTEM'),
('LIMPIEZA', 'Personal de Limpieza / Mantenimiento', 'Acceso básico de perfil y tareas operativas internas', TRUE, 'SYSTEM');

-- ============================================================================
-- DATOS INICIALES: Sucursal Base (Configurable por Admin - 3.1 y 3.2)
-- ============================================================================
INSERT INTO sucursales (
    codigo, nombre, razon_social, cuit, condicion_iva, punto_venta_arca,
    ingresos_brutos, direccion, localidad, provincia,
    vende_al_publico, tiene_deposito, es_casa_central, activo, created_by
) VALUES (
    'SUC-001',
    'Casa Central',
    'MI COMERCIO POLIRUBRO S.R.L.',
    '30-00000000-0',
    'RESPONSABLE_INSCRIPTO',
    1,
    '30-00000000-0',
    'Av. Principal 100',
    'Ciudad Autónoma de Buenos Aires',
    'Buenos Aires',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    'SYSTEM'
);
