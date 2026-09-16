-- Sales Service - sample product data
-- Run after 01_schema.sql.

USE sales_service;

INSERT IGNORE INTO products
    (sku, name, description, price, stock_quantity, status)
VALUES
    ('KB-MECH-001', 'Bàn phím cơ mẫu',
     'Bàn phím cơ dùng để kiểm tra API', 890000.00, 20, 'ACTIVE'),
    ('MOUSE-WL-001', 'Chuột không dây mẫu',
     'Chuột không dây dùng để kiểm tra API', 450000.00, 35, 'ACTIVE'),
    ('HEADSET-001', 'Tai nghe chụp tai mẫu',
     'Tai nghe dùng để kiểm tra API', 650000.00, 15, 'ACTIVE');

SELECT id, sku, name, price, stock_quantity, status
FROM products
ORDER BY id;
