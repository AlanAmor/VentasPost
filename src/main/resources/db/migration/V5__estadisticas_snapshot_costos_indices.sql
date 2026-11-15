-- =====================================================================
-- V5: FASE 5 - SNAPSHOT DE COSTO HISTÓRICO EN VENTA_ITEMS PARA CÁLCULO
--     DE MARGEN DE GANANCIA REAL E ÍNDICES DE INTELIGENCIA DE NEGOCIO
-- =====================================================================

ALTER TABLE venta_items
    ADD COLUMN costo_unitario_con_iva_historico DECIMAL(19, 2) NOT NULL DEFAULT 0.00 AFTER precio_unitario,
    ADD COLUMN costo_total_con_iva_historico DECIMAL(19, 2) NOT NULL DEFAULT 0.00 AFTER subtotal_neto_item;

-- Retroalimentar costo histórico en ítems de ventas previos a V5
UPDATE venta_items vi
INNER JOIN productos p ON p.id = vi.producto_id
SET
    vi.costo_unitario_con_iva_historico = ROUND(
        CASE
            WHEN p.iva_incluido_compra = 1 OR p.alicuota_iva <= 0 THEN p.precio_compra
            ELSE p.precio_compra * (1 + (p.alicuota_iva / 100))
        END, 2
    ),
    vi.costo_total_con_iva_historico = ROUND(
        vi.cantidad * (
            CASE
                WHEN p.iva_incluido_compra = 1 OR p.alicuota_iva <= 0 THEN p.precio_compra
                ELSE p.precio_compra * (1 + (p.alicuota_iva / 100))
            END
        ), 2
    )
WHERE vi.costo_unitario_con_iva_historico = 0.00;

CREATE INDEX idx_venta_items_producto ON venta_items (producto_id);
CREATE INDEX idx_venta_pagos_medio ON venta_pagos (medio_pago);
