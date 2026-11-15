-- ============================================================================
-- V8: Normalización de Unidades de Medida en Productos de Prueba (CAJA y KG)
-- ============================================================================

UPDATE productos SET unidad_medida = 'CAJA' WHERE unidad_medida = 'PACK';
UPDATE productos SET unidad_medida = 'KG' WHERE unidad_medida = 'KILOGRAMO';
