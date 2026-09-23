CREATE TABLE cart_items (
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_cart_items PRIMARY KEY (user_id, product_id),
    CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0),
    INDEX idx_cart_items_product (product_id)
) ENGINE = InnoDB;

CREATE TABLE admin_account_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT NOT NULL,
    created_user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_admin_account_audit PRIMARY KEY (id),
    CONSTRAINT uk_admin_account_audit_created UNIQUE (created_user_id),
    CONSTRAINT fk_admin_account_audit_actor FOREIGN KEY (actor_user_id)
        REFERENCES users (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_admin_account_audit_created FOREIGN KEY (created_user_id)
        REFERENCES users (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_admin_account_audit_actor_time (actor_user_id, created_at)
) ENGINE = InnoDB;
