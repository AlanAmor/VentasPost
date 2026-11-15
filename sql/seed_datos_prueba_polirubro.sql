-- ============================================================================
-- SCRIPT SQL INDEPENDIENTE CON DATOS INICIALES DE PRUEBA (SEED DATA)
-- Proyecto: VentasPost — Sistema Integral Polirubro Multi-Sucursal
-- Nota: Este script también se ejecuta automáticamente mediante Flyway (V7).
-- ============================================================================

-- 1. Segunda Sucursal Operativa ("Sucursal Norte" - Punto de Venta ARCA 00002)
INSERT IGNORE INTO sucursales (
    codigo, nombre, razon_social, cuit, condicion_iva, punto_venta_arca,
    ingresos_brutos, fecha_inicio_actividades, direccion, localidad, provincia,
    telefono, email, vende_al_publico, tiene_deposito, es_casa_central, activo, created_by
) VALUES (
    'SUC-002',
    'Sucursal Norte',
    'MI COMERCIO POLIRUBRO S.R.L.',
    '30-00000000-0',
    'RESPONSABLE_INSCRIPTO',
    2,
    '30-00000000-0',
    '2022-03-01',
    'Av. Cabildo 2450',
    'Ciudad Autónoma de Buenos Aires',
    'Buenos Aires',
    '011-4788-9900',
    'norte@ventaspost.com.ar',
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    'SYSTEM'
);

-- 2. Proveedores Comerciales de Prueba
INSERT IGNORE INTO proveedores (
    codigo, razon_social, nombre_fantasia, cuit, condicion_iva,
    telefono, email, direccion, localidad, provincia,
    observaciones, es_proveedor_sistema, activo, created_by
) VALUES
('PROV-001', 'DISTRIBUIDORA MAYORISTA BAZAR DEL SUR S.A.', 'Bazar del Sur', '30-71234567-9', 'RESPONSABLE_INSCRIPTO', '011-4300-1122', 'ventas@bazardelsur.com.ar', 'Av. Patricios 850', 'CABA', 'Buenos Aires', 'Entrega martes y jueves. Descuento 5% por volumen.', FALSE, TRUE, 'SYSTEM'),
('PROV-002', 'FERRETERÍA Y HERRAMIENTAS INDUSTRIALES S.R.L.', 'FerreIndus', '30-70987654-3', 'RESPONSABLE_INSCRIPTO', '011-4650-3344', 'pedidos@ferreindus.com.ar', 'Av. San Martín 4120', 'San Martín', 'Buenos Aires', 'Proveedor oficial de herramientas eléctricas y cables por metro.', FALSE, TRUE, 'SYSTEM'),
('PROV-003', 'PAPELERA Y LIBRERÍA MAYORISTA EL ESTUDIANTE S.A.', 'Papelera El Estudiante', '33-69887766-9', 'RESPONSABLE_INSCRIPTO', '011-4951-7788', 'contacto@elestudiante.com.ar', 'Pueyrredón 420', 'CABA', 'Buenos Aires', 'Artículos escolares, comerciales y regalería.', FALSE, TRUE, 'SYSTEM'),
('PROV-004', 'IMPORTADORA TECNOHOGAR ARGENTINA S.A.', 'TecnoHogar', '30-71554433-2', 'RESPONSABLE_INSCRIPTO', '011-5230-9000', 'mayorista@tecnohogar.com.ar', 'Av. Córdoba 3800', 'CABA', 'Buenos Aires', 'Pequeños electrodomésticos, iluminación LED y accesorios.', FALSE, TRUE, 'SYSTEM');

-- 3. Categorías del Polirubro
INSERT IGNORE INTO categorias (nombre, descripcion, activo, created_by) VALUES
('Bazar y Cocina', 'Termos, mates, vajilla, herméticos y utensilios gastronómicos', TRUE, 'SYSTEM'),
('Ferretería y Electricidad', 'Herramientas, cables por metro, cintas, tornillería e iluminación', TRUE, 'SYSTEM'),
('Librería y Papelería', 'Cuadernos, resmas, bolígrafos y artículos de oficina', TRUE, 'SYSTEM'),
('Pequeños Electrodomésticos', 'Pavas eléctricas, cafeteras, tostadoras y cuidado personal', TRUE, 'SYSTEM'),
('Alimentos Secos y Granel', 'Frutos secos, semillas, golosinas y artículos fraccionables por peso', TRUE, 'SYSTEM'),
('Limpieza y Perfumería', 'Detergentes, desinfectantes, jabones líquidos y anexos', TRUE, 'SYSTEM');

-- 4. Marcas Comerciales
INSERT IGNORE INTO marcas (nombre, descripcion, activo, created_by) VALUES
('Lumilagro', 'Termos e isotérmicos nacionales', TRUE, 'SYSTEM'),
('Tramontina', 'Cubiertos, cuchillería y herramientas', TRUE, 'SYSTEM'),
('Stanley', 'Termos y vasos térmicos de acero inoxidable', TRUE, 'SYSTEM'),
('Philips', 'Iluminación LED y pequeños electrodomésticos', TRUE, 'SYSTEM'),
('Rivarola', 'Cuadernos y papelería escolar/oficina', TRUE, 'SYSTEM'),
('Prysmian', 'Cables eléctricos normalizados', TRUE, 'SYSTEM'),
('Atma', 'Electrodomésticos para el hogar', TRUE, 'SYSTEM');
