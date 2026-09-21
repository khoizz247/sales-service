-- Upgrade preparation for an existing sales_service database.
-- Run this file once, then rerun the latest 01_schema.sql and 02_seed.sql.
-- It removes the derived order_items.line_total column; the API now calculates it.

USE sales_service;

SET @drop_line_total_check = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'order_items'
          AND constraint_name = 'chk_order_items_line_total'
    ),
    'ALTER TABLE order_items DROP CHECK chk_order_items_line_total',
    'SELECT ''line_total check does not exist'' AS info'
);
PREPARE stmt FROM @drop_line_total_check;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_line_total_column = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'order_items'
          AND column_name = 'line_total'
    ),
    'ALTER TABLE order_items DROP COLUMN line_total',
    'SELECT ''line_total column already removed'' AS info'
);
PREPARE stmt FROM @drop_line_total_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Now run 01_schema.sql and then 02_seed.sql' AS next_step;
