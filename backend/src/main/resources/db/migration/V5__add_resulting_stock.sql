ALTER TABLE stock_movements
ADD COLUMN resulting_stock INTEGER;

-- Reconstruir el stock después de cada movimiento anterior.
-- Se parte del stock actual y se deshacen los movimientos posteriores.

WITH calculated AS (
    SELECT
        sm.id,
        p.stock -
        COALESCE(
            SUM(
                CASE
                    WHEN sm.movement_type = 'IN' THEN sm.quantity
                    ELSE -sm.quantity
                END
            ) OVER (
                PARTITION BY sm.product_id
                ORDER BY sm.id DESC
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ),
            0
        ) AS historical_stock
    FROM stock_movements sm
    JOIN products p ON p.id = sm.product_id
)
UPDATE stock_movements sm
SET resulting_stock = calculated.historical_stock
FROM calculated
WHERE sm.id = calculated.id;

ALTER TABLE stock_movements
ADD CONSTRAINT chk_resulting_stock
CHECK (resulting_stock >= 0);

ALTER TABLE stock_movements
ALTER COLUMN resulting_stock SET NOT NULL;