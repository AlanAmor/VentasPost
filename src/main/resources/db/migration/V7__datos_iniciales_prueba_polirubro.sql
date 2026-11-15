-- ============================================================================
-- V7: FASE 7 - SCRIPT SQL LIMPIO E IDEMPOTENTE CON DATOS INICIALES DE PRUEBA
--     PARA DEMOSTRACIÓN INTEGRAL DEL POLIRUBRO MULTI-SUCURSAL (VENTASPOST)
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

-- Asegurar Configuración Fiscal ARCA Homologación en todas las sucursales
INSERT IGNORE INTO configuracion_arca_sucursal (
    sucursal_id, razon_social_emisor, cuit_emisor, punto_venta_arca,
    condicion_iva_emisor, ingresos_brutos, domicilio_comercial,
    entorno_arca, simulador_homologacion_activo,
    monto_tope_consumidor_final_sin_dni, plazo_maximo_dias_facturacion,
    activo, created_by, updated_by
)
SELECT
    s.id,
    s.razon_social,
    s.cuit,
    s.punto_venta_arca,
    s.condicion_iva,
    s.ingresos_brutos,
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

-- 5. Catálogo Inicial de Productos Polirubro (Unidades y Fraccionables)
INSERT IGNORE INTO productos (
    codigo_barras, codigo_proveedor_sku, es_codigo_interno, nombre, descripcion,
    categoria_id, marca_id, proveedor_id, unidad_medida,
    precio_compra, iva_incluido_compra, alicuota_iva, porcentaje_ganancia,
    precio_venta_general, stock_minimo_defecto, activo, created_by
) VALUES
(
    '7790011000015', 'LUM-1000-AC', FALSE,
    'Termo Acero Inoxidable 1 Litro Pico Cebador',
    'Termo de acero inoxidable bicapa con tapón cebador incluido',
    (SELECT id FROM categorias WHERE nombre = 'Bazar y Cocina' LIMIT 1),
    (SELECT id FROM marcas WHERE nombre = 'Lumilagro' LIMIT 1),
    (SELECT id FROM proveedores WHERE codigo = 'PROV-001' LIMIT 1),
    'UNIDAD', 18500.0000, TRUE, 21.00, 45.00, 26825.00, 5.000, TRUE, 'SYSTEM'
),
(
    '7790011000022', 'TRAM-SET24', FALSE,
    'Juego de Cubiertos Acero Inox x 24 Piezas',
    'Set de tenedores, cuchillos serrucho y cucharas mango polywood',
    (SELECT id FROM categorias WHERE nombre = 'Bazar y Cocina' LIMIT 1),
    (SELECT id FROM marcas WHERE nombre = 'Tramontina' LIMIT 1),
    (SELECT id FROM proveedores WHERE codigo = 'PROV-001' LIMIT 1),
    'CAJA', 14200.0000, TRUE, 21.00, 50.00, 21300.00, 4.000, TRUE, 'SYSTEM'
),
(
    '7790011000039', 'ATMA-PE8821', FALSE,
    'Pava Eléctrica Corte Automático Mate 1.7 Lts',
    'Pava eléctrica con selector de temperatura para mate y café',
    (SELECT id FROM categorias WHERE nombre = 'Pequeños Electrodomésticos' LIMIT 1),
    (SELECT id FROM marcas WHERE nombre = 'Atma' LIMIT 1),
    (SELECT id FROM proveedores WHERE codigo = 'PROV-004' LIMIT 1),
    'UNIDAD', 32000.0000, TRUE, 21.00, 40.00, 44800.00, 3.000, TRUE, 'SYSTEM'
),
(
    '7790011000046', 'PHI-LED12W', FALSE,
    'Lámpara LED Bulbo 12W Luz Fría E27',
    'Lámpara bajo consumo LED 12W equivalente a 100W',
    (SELECT id FROM categorias WHERE nombre = 'Ferretería y Electricidad' LIMIT 1),
    (SELECT id FROM marcas WHERE nombre = 'Philips' LIMIT 1),
    (SELECT id FROM proveedores WHERE codigo = 'PROV-002' LIMIT 1),
    'UNIDAD', 1650.0000, TRUE, 21.00, 55.00, 2557.50, 15.000, TRUE, 'SYSTEM'
),
(
    '2000001000018', 'PRYS-25MM', TRUE,
    'Cable Unipolar Normalizado 2.5mm (Venta por Metro)',
    'Cable eléctrico flexible 2.5mm fraccionable por metro',
    (SELECT id FROM categorias WHERE nombre = 'Ferretería y Electricidad' LIMIT 1),
    (SELECT id FROM marcas WHERE nombre = 'Prysmian' LIMIT 1),
    (SELECT id FROM proveedores WHERE codigo = 'PROV-002' LIMIT 1),
    'METRO', 780.0000, TRUE, 21.00, 60.00, 1248.00, 25.000, TRUE, 'SYSTEM'
),
(
    '2000001000025', 'GRAN-MIXFRU', TRUE,
    'Mix de Frutos Secos Premium (Venta por Kg)',
    'Almendras, nueces, castañas de cajú y pasas fraccionable por peso',
    (SELECT id FROM categorias WHERE nombre = 'Alimentos Secos y Granel' LIMIT 1),
    (SELECT id FROM marcas WHERE nombre = 'Genérica' LIMIT 1),
    (SELECT id FROM proveedores WHERE codigo = 'PROV-GEN' LIMIT 1),
    'KG', 11000.0000, TRUE, 21.00, 50.00, 16500.00, 3.000, TRUE, 'SYSTEM'
),
(
    '7790011000077', 'RIV-A4-84H', FALSE,
    'Cuaderno Universitario Espiral A4 84 Hojas Rayado',
    'Cuaderno tapa dura espiralado 84 hojas',
    (SELECT id FROM categorias WHERE nombre = 'Librería y Papelería' LIMIT 1),
    (SELECT id FROM marcas WHERE nombre = 'Rivarola' LIMIT 1),
    (SELECT id FROM proveedores WHERE codigo = 'PROV-003' LIMIT 1),
    'UNIDAD', 2900.0000, TRUE, 21.00, 50.00, 4350.00, 10.000, TRUE, 'SYSTEM'
),
(
    '7790011000084', 'ATMA-CA8190', FALSE,
    'Cafetera Express Automática 15 Bares Acero',
    'Cafetera express con espumador de leche (Stock inmovilizado de demostración)',
    (SELECT id FROM categorias WHERE nombre = 'Pequeños Electrodomésticos' LIMIT 1),
    (SELECT id FROM marcas WHERE nombre = 'Atma' LIMIT 1),
    (SELECT id FROM proveedores WHERE codigo = 'PROV-004' LIMIT 1),
    'UNIDAD', 145000.0000, TRUE, 21.00, 38.00, 200100.00, 2.000, TRUE, 'SYSTEM'
);

-- 6. Existencias Iniciales por Sucursal (incluyendo alertas de bajo stock en algunos artículos)
INSERT IGNORE INTO producto_sucursales (
    producto_id, sucursal_id, stock_actual, stock_minimo, precio_venta_sucursal, ubicacion_gondola, activo, created_by
)
SELECT
    p.id,
    s.id,
    CASE
        WHEN p.codigo_barras = '7790011000039' AND s.codigo = 'SUC-001' THEN 2.000 -- Bajo stock (mínimo 3)
        WHEN p.codigo_barras = '7790011000046' AND s.codigo = 'SUC-002' THEN 8.000 -- Bajo stock (mínimo 15)
        WHEN p.codigo_barras = '2000001000018' THEN 120.000
        WHEN p.codigo_barras = '2000001000025' THEN 14.500
        ELSE 18.000
    END,
    p.stock_minimo_defecto,
    NULL,
    CONCAT('Góndola ', p.id),
    TRUE,
    'SYSTEM'
FROM productos p
CROSS JOIN sucursales s
WHERE NOT EXISTS (
    SELECT 1 FROM producto_sucursales ps
    WHERE ps.producto_id = p.id AND ps.sucursal_id = s.id
);

-- 7. Horarios Semanales Asignados de Ejemplo (Lunes a Viernes 09:00 a 17:00 en Casa Central)
INSERT INTO horarios_asignados_empleado (
    usuario_id, sucursal_id, dia_semana, hora_entrada, hora_salida, horas_teoricas, observaciones, activo, created_by
)
SELECT
    u.id,
    (SELECT id FROM sucursales WHERE codigo = 'SUC-001' LIMIT 1),
    dias.dia,
    '09:00:00',
    '17:00:00',
    8.00,
    'Turno habitual Lunes a Viernes',
    1,
    'SYSTEM'
FROM usuarios u
CROSS JOIN (
    SELECT 1 AS dia UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) dias
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM horarios_asignados_empleado h
      WHERE h.usuario_id = u.id AND h.dia_semana = dias.dia
  );
