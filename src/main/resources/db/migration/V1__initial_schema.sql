-- Initial MySQL schema. Existing installations must be baselined at version 1.
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role ENUM('CUSTOMER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    status ENUM('ACTIVE', 'LOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_full_name CHECK (CHAR_LENGTH(TRIM(full_name)) > 0),
    CONSTRAINT chk_users_password_hash CHECK (CHAR_LENGTH(password_hash) >= 50)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    price DECIMAL(15, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT chk_products_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_products_price CHECK (price > 0),
    CONSTRAINT chk_products_stock CHECK (stock_quantity >= 0),
    CONSTRAINT chk_products_version CHECK (version >= 0),

    INDEX idx_products_status_created_at (status, created_at),
    INDEX idx_products_name (name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_code VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(120) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    shipping_address VARCHAR(500) NOT NULL,
    status ENUM(
        'PENDING',
        'CONFIRMED',
        'SHIPPING',
        'COMPLETED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uk_orders_order_code UNIQUE (order_code),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT chk_orders_recipient_name
        CHECK (CHAR_LENGTH(TRIM(recipient_name)) > 0),
    CONSTRAINT chk_orders_recipient_phone
        CHECK (CHAR_LENGTH(TRIM(recipient_phone)) BETWEEN 8 AND 30),
    CONSTRAINT chk_orders_shipping_address
        CHECK (CHAR_LENGTH(TRIM(shipping_address)) > 0),
    CONSTRAINT chk_orders_total_amount CHECK (total_amount >= 0),

    INDEX idx_orders_user_created_at (user_id, created_at),
    INDEX idx_orders_status_created_at (status, created_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(160) NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    quantity INT NOT NULL,

    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT uk_order_items_order_product UNIQUE (order_id, product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT chk_order_items_product_name
        CHECK (CHAR_LENGTH(TRIM(product_name)) > 0),
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price > 0),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),

    INDEX idx_order_items_product (product_id)
) ENGINE = InnoDB;

-- Organization structure, equivalent to offices/employees in classicmodels.
CREATE TABLE IF NOT EXISTS offices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    office_code VARCHAR(20) NOT NULL,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(190) NULL,
    address_line1 VARCHAR(150) NOT NULL,
    address_line2 VARCHAR(150) NULL,
    city VARCHAR(80) NOT NULL,
    state_province VARCHAR(80) NULL,
    postal_code VARCHAR(20) NULL,
    country_code CHAR(2) NOT NULL DEFAULT 'VN',
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_offices PRIMARY KEY (id),
    CONSTRAINT uk_offices_code UNIQUE (office_code),
    CONSTRAINT chk_offices_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_offices_country CHECK (country_code REGEXP '^[A-Z]{2}$'),

    INDEX idx_offices_status_city (status, city)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS employee_profiles (
    user_id BIGINT NOT NULL,
    employee_code VARCHAR(30) NOT NULL,
    office_id BIGINT NOT NULL,
    manager_user_id BIGINT NULL,
    job_title VARCHAR(100) NOT NULL,
    extension VARCHAR(20) NULL,
    hire_date DATE NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT pk_employee_profiles PRIMARY KEY (user_id),
    CONSTRAINT uk_employee_profiles_code UNIQUE (employee_code),
    CONSTRAINT fk_employee_profiles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_employee_profiles_office FOREIGN KEY (office_id)
        REFERENCES offices (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_employee_profiles_manager FOREIGN KEY (manager_user_id)
        REFERENCES employee_profiles (user_id) ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT chk_employee_profiles_title CHECK (CHAR_LENGTH(TRIM(job_title)) > 0),

    INDEX idx_employee_profiles_office (office_id),
    INDEX idx_employee_profiles_manager (manager_user_id)
) ENGINE = InnoDB;

-- Customer-specific data is separated from authentication data in users.
CREATE TABLE IF NOT EXISTS customer_profiles (
    user_id BIGINT NOT NULL,
    phone VARCHAR(30) NULL,
    date_of_birth DATE NULL,
    credit_limit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_customer_profiles PRIMARY KEY (user_id),
    CONSTRAINT fk_customer_profiles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_customer_profiles_phone CHECK (phone IS NULL OR CHAR_LENGTH(TRIM(phone)) BETWEEN 8 AND 30),
    CONSTRAINT chk_customer_profiles_credit CHECK (credit_limit >= 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS customer_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_user_id BIGINT NOT NULL,
    label VARCHAR(50) NOT NULL DEFAULT 'Địa chỉ',
    recipient_name VARCHAR(120) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    address_line1 VARCHAR(150) NOT NULL,
    address_line2 VARCHAR(150) NULL,
    ward VARCHAR(80) NULL,
    district VARCHAR(80) NULL,
    city VARCHAR(80) NOT NULL,
    state_province VARCHAR(80) NULL,
    postal_code VARCHAR(20) NULL,
    country_code CHAR(2) NOT NULL DEFAULT 'VN',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_customer_addresses PRIMARY KEY (id),
    CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (customer_user_id)
        REFERENCES customer_profiles (user_id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT chk_customer_addresses_recipient CHECK (CHAR_LENGTH(TRIM(recipient_name)) > 0),
    CONSTRAINT chk_customer_addresses_phone CHECK (CHAR_LENGTH(TRIM(recipient_phone)) BETWEEN 8 AND 30),
    CONSTRAINT chk_customer_addresses_line1 CHECK (CHAR_LENGTH(TRIM(address_line1)) > 0),
    CONSTRAINT chk_customer_addresses_country CHECK (country_code REGEXP '^[A-Z]{2}$'),

    INDEX idx_customer_addresses_customer (customer_user_id, is_default)
) ENGINE = InnoDB;

-- Product lines are normalized as hierarchical categories.
CREATE TABLE IF NOT EXISTS product_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL DEFAULT '',
    parent_id BIGINT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_product_categories PRIMARY KEY (id),
    CONSTRAINT uk_product_categories_code UNIQUE (category_code),
    CONSTRAINT uk_product_categories_name UNIQUE (name),
    CONSTRAINT fk_product_categories_parent FOREIGN KEY (parent_id)
        REFERENCES product_categories (id) ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT chk_product_categories_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),

    INDEX idx_product_categories_parent_status (parent_id, status)
) ENGINE = InnoDB;

-- Vendor data is no longer repeated as text on every product.
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    supplier_code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    contact_name VARCHAR(120) NULL,
    email VARCHAR(190) NULL,
    phone VARCHAR(30) NULL,
    address_line1 VARCHAR(150) NULL,
    address_line2 VARCHAR(150) NULL,
    city VARCHAR(80) NULL,
    state_province VARCHAR(80) NULL,
    postal_code VARCHAR(20) NULL,
    country_code CHAR(2) NOT NULL DEFAULT 'VN',
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_suppliers PRIMARY KEY (id),
    CONSTRAINT uk_suppliers_code UNIQUE (supplier_code),
    CONSTRAINT chk_suppliers_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_suppliers_country CHECK (country_code REGEXP '^[A-Z]{2}$'),

    INDEX idx_suppliers_status_name (status, name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS product_category_assignments (
    product_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    assigned_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_product_category_assignments PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_product_category_assignments_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_product_category_assignments_category FOREIGN KEY (category_id)
        REFERENCES product_categories (id) ON UPDATE RESTRICT ON DELETE CASCADE,

    INDEX idx_product_category_assignments_category (category_id, product_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS product_suppliers (
    product_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    supplier_sku VARCHAR(64) NULL,
    purchase_price DECIMAL(15, 2) NOT NULL,
    lead_time_days SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    is_preferred BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_product_suppliers PRIMARY KEY (product_id, supplier_id),
    CONSTRAINT uk_product_suppliers_supplier_sku UNIQUE (supplier_id, supplier_sku),
    CONSTRAINT fk_product_suppliers_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_product_suppliers_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_product_suppliers_price CHECK (purchase_price > 0),

    INDEX idx_product_suppliers_supplier (supplier_id, product_id)
) ENGINE = InnoDB;

-- One order can have multiple payment attempts, but each code is unique.
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    payment_code VARCHAR(40) NOT NULL,
    provider_transaction_id VARCHAR(100) NULL,
    method ENUM('COD', 'BANK_TRANSFER', 'CARD', 'E_WALLET') NOT NULL,
    status ENUM('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(15, 2) NOT NULL,
    paid_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uk_payments_code UNIQUE (payment_code),
    CONSTRAINT uk_payments_provider_transaction UNIQUE (provider_transaction_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_paid_at CHECK (status <> 'PAID' OR paid_at IS NOT NULL),

    INDEX idx_payments_order_created_at (order_id, created_at),
    INDEX idx_payments_status_created_at (status, created_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    from_status ENUM('PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED') NULL,
    to_status ENUM('PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED') NOT NULL,
    changed_by_user_id BIGINT NULL,
    note VARCHAR(500) NULL,
    changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_order_status_history PRIMARY KEY (id),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_order_status_history_user FOREIGN KEY (changed_by_user_id)
        REFERENCES users (id) ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT chk_order_status_history_change CHECK (from_status IS NULL OR from_status <> to_status),

    INDEX idx_order_status_history_order_time (order_id, changed_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS inventory_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    movement_type ENUM(
        'OPENING_BALANCE', 'PURCHASE', 'SALE', 'SALE_REVERSAL',
        'ADJUSTMENT_IN', 'ADJUSTMENT_OUT'
    ) NOT NULL,
    quantity_change INT NOT NULL,
    stock_before INT NOT NULL,
    stock_after INT NOT NULL,
    reference_code VARCHAR(80) NULL,
    note VARCHAR(500) NULL,
    created_by_user_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_inventory_movements PRIMARY KEY (id),
    CONSTRAINT uk_inventory_movements_reference UNIQUE (reference_code),
    CONSTRAINT fk_inventory_movements_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_movements_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_movements_user FOREIGN KEY (created_by_user_id)
        REFERENCES users (id) ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT chk_inventory_movements_quantity CHECK (quantity_change <> 0),
    CONSTRAINT chk_inventory_movements_stock CHECK (
        stock_before >= 0 AND stock_after >= 0
        AND stock_after = stock_before + quantity_change
    ),

    INDEX idx_inventory_movements_product_time (product_id, created_at),
    INDEX idx_inventory_movements_order (order_id)
) ENGINE = InnoDB;

-- Derived values are exposed through views instead of being repeated in detail rows.
CREATE OR REPLACE VIEW v_order_calculated_totals AS
SELECT
    o.id AS order_id,
    o.order_code,
    COALESCE(SUM(oi.unit_price * oi.quantity), 0.00) AS calculated_total,
    o.total_amount AS recorded_total
FROM orders o
LEFT JOIN order_items oi ON oi.order_id = o.id
GROUP BY o.id, o.order_code, o.total_amount;

CREATE OR REPLACE VIEW v_product_catalog AS
SELECT
    p.id,
    p.sku,
    p.name,
    p.price,
    p.stock_quantity,
    p.status,
    GROUP_CONCAT(DISTINCT pc.name ORDER BY pc.name SEPARATOR ', ') AS categories
FROM products p
LEFT JOIN product_category_assignments pca ON pca.product_id = p.id
LEFT JOIN product_categories pc ON pc.id = pca.category_id
GROUP BY p.id, p.sku, p.name, p.price, p.stock_quantity, p.status;

-- Keep profile and status history complete even when the current Java API writes core tables only.
DROP TRIGGER IF EXISTS trg_users_create_customer_profile;
CREATE TRIGGER trg_users_create_customer_profile
AFTER INSERT ON users
FOR EACH ROW
INSERT IGNORE INTO customer_profiles (user_id)
SELECT NEW.id
WHERE NEW.role = 'CUSTOMER';

DROP TRIGGER IF EXISTS trg_orders_initial_status;
CREATE TRIGGER trg_orders_initial_status
AFTER INSERT ON orders
FOR EACH ROW
INSERT INTO order_status_history
    (order_id, from_status, to_status, changed_by_user_id, note, changed_at)
VALUES
    (NEW.id, NULL, NEW.status, NEW.user_id, 'Đơn hàng được tạo', NEW.created_at);

DROP TRIGGER IF EXISTS trg_orders_status_change;
CREATE TRIGGER trg_orders_status_change
AFTER UPDATE ON orders
FOR EACH ROW
INSERT INTO order_status_history
    (order_id, from_status, to_status, changed_by_user_id, note, changed_at)
SELECT NEW.id, OLD.status, NEW.status, NULL, 'Trạng thái đơn hàng thay đổi', NEW.updated_at
WHERE OLD.status <> NEW.status;
