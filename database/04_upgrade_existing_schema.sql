-- Run this file ONCE only if 01_schema.sql was executed before Order/OrderItem support was added.
-- A newly created database using the latest 01_schema.sql does not need this file.

USE sales_service;

UPDATE products
SET description = ''
WHERE id > 0
  AND description IS NULL;

ALTER TABLE products
    MODIFY COLUMN description VARCHAR(1000) NOT NULL;

ALTER TABLE orders
    MODIFY COLUMN recipient_phone VARCHAR(30) NOT NULL;

ALTER TABLE order_items
    MODIFY COLUMN line_total DECIMAL(15, 2) NOT NULL;

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_line_total
        CHECK (line_total = unit_price * quantity);
