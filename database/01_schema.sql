-- Sales Service - MySQL schema
-- Compatible with MySQL 8.0+.
-- This script is non-destructive: it does not drop existing tables or data.

CREATE DATABASE IF NOT EXISTS sales_service
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE sales_service;

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
    line_total DECIMAL(15, 2) NOT NULL,

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
    CONSTRAINT chk_order_items_line_total CHECK (line_total = unit_price * quantity),

    INDEX idx_order_items_product (product_id)
) ENGINE = InnoDB;
