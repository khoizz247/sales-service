#!/bin/sh
set -eu

# A TCP connection succeeds only after MySQL's first-run SQL scripts finish.
# Checking schema objects also prevents the API from starting on an old volume.
MYSQL_PWD="$MYSQL_PASSWORD" mysql --protocol=tcp --host=127.0.0.1 \
    --user="$MYSQL_USER" --database=sales_service --batch --skip-column-names \
    --execute="
SELECT
    (SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = 'sales_service' AND table_type = 'BASE TABLE'
       AND table_name IN (
           'users', 'customer_profiles', 'customer_addresses',
           'offices', 'employee_profiles', 'products',
           'product_categories', 'product_category_assignments',
           'suppliers', 'product_suppliers', 'orders', 'order_items',
           'payments', 'order_status_history', 'inventory_movements'
       )) = 15
    AND (SELECT COUNT(*) FROM information_schema.views
         WHERE table_schema = 'sales_service'
           AND table_name IN ('v_order_calculated_totals', 'v_product_catalog')) = 2
    AND (SELECT COUNT(*) FROM information_schema.triggers
         WHERE trigger_schema = 'sales_service'
           AND trigger_name IN (
               'trg_users_create_customer_profile',
               'trg_orders_initial_status',
               'trg_orders_status_change'
           )) = 3;
" | grep -qx 1

echo "sales_service schema ready: 15 tables, 2 views, 3 triggers"
