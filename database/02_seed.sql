-- Sales Service - normalized reference and sample data
-- Run after 01_schema.sql.

USE sales_service;

INSERT IGNORE INTO product_categories
    (category_code, name, description, parent_id, status)
VALUES
    ('PERIPHERALS', 'Thiết bị ngoại vi', 'Thiết bị ngoại vi máy tính', NULL, 'ACTIVE');

INSERT IGNORE INTO product_categories
    (category_code, name, description, parent_id, status)
SELECT 'KEYBOARDS', 'Bàn phím', 'Các loại bàn phím', id, 'ACTIVE'
FROM product_categories WHERE category_code = 'PERIPHERALS';

INSERT IGNORE INTO product_categories
    (category_code, name, description, parent_id, status)
SELECT 'MICE', 'Chuột máy tính', 'Các loại chuột máy tính', id, 'ACTIVE'
FROM product_categories WHERE category_code = 'PERIPHERALS';

INSERT IGNORE INTO product_categories
    (category_code, name, description, parent_id, status)
SELECT 'AUDIO', 'Thiết bị âm thanh', 'Tai nghe và thiết bị âm thanh', id, 'ACTIVE'
FROM product_categories WHERE category_code = 'PERIPHERALS';

INSERT IGNORE INTO suppliers
    (supplier_code, name, contact_name, email, phone, country_code, status)
VALUES
    ('SUP-DEMO-001', 'Nhà cung cấp thiết bị mẫu', 'Nguyễn Nhà Cung Cấp',
     'supplier@example.com', '0900000000', 'VN', 'ACTIVE');

INSERT IGNORE INTO customer_profiles (user_id)
SELECT id
FROM users
WHERE role = 'CUSTOMER';

INSERT IGNORE INTO products
    (sku, name, description, price, stock_quantity, status)
VALUES
    ('KB-MECH-001', 'Bàn phím cơ mẫu',
     'Bàn phím cơ dùng để kiểm tra API', 890000.00, 20, 'ACTIVE'),
    ('MOUSE-WL-001', 'Chuột không dây mẫu',
     'Chuột không dây dùng để kiểm tra API', 450000.00, 35, 'ACTIVE'),
    ('HEADSET-001', 'Tai nghe chụp tai mẫu',
     'Tai nghe dùng để kiểm tra API', 650000.00, 15, 'ACTIVE');

INSERT IGNORE INTO product_category_assignments (product_id, category_id)
SELECT p.id, pc.id
FROM products p
JOIN product_categories pc
  ON pc.category_code = CASE p.sku
      WHEN 'KB-MECH-001' THEN 'KEYBOARDS'
      WHEN 'MOUSE-WL-001' THEN 'MICE'
      WHEN 'HEADSET-001' THEN 'AUDIO'
  END
WHERE p.sku IN ('KB-MECH-001', 'MOUSE-WL-001', 'HEADSET-001');

INSERT IGNORE INTO product_suppliers
    (product_id, supplier_id, supplier_sku, purchase_price, lead_time_days, is_preferred)
SELECT
    p.id,
    s.id,
    CONCAT('VENDOR-', p.sku),
    ROUND(p.price * 0.70, 2),
    3,
    TRUE
FROM products p
CROSS JOIN suppliers s
WHERE p.sku IN ('KB-MECH-001', 'MOUSE-WL-001', 'HEADSET-001')
  AND s.supplier_code = 'SUP-DEMO-001';

INSERT IGNORE INTO inventory_movements
    (product_id, movement_type, quantity_change, stock_before, stock_after, reference_code, note)
SELECT
    p.id,
    'OPENING_BALANCE',
    p.stock_quantity,
    0,
    p.stock_quantity,
    CONCAT('OPENING-', p.sku),
    'Số dư tồn kho ban đầu'
FROM products p
WHERE p.sku IN ('KB-MECH-001', 'MOUSE-WL-001', 'HEADSET-001')
  AND p.stock_quantity > 0;

INSERT INTO order_status_history
    (order_id, from_status, to_status, changed_by_user_id, note, changed_at)
SELECT o.id, NULL, o.status, NULL,
       'Trạng thái hiện tại được ghi nhận khi nâng cấp', CURRENT_TIMESTAMP(6)
FROM orders o
WHERE NOT EXISTS (
    SELECT 1
    FROM order_status_history osh
    WHERE osh.order_id = o.id
);

SELECT id, sku, name, price, stock_quantity, status
FROM products
ORDER BY id;
