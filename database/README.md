# MySQL database setup

The database is organized into five domains; the shopping cart uses `cart_items`:

```text
Identity:   users, customer_profiles, customer_addresses
Staff:      offices, employee_profiles
Catalog:    products, product_categories, suppliers and two junction tables
Sales:      orders, order_items, payments, order_status_history
Inventory:  inventory_movements
Cart:       cart_items
Audit:      admin_account_audit
```

The normalization analysis and full ER diagram are in [`NORMALIZATION.md`](NORMALIZATION.md).

## Current deployment: Flyway

The application now applies `src/main/resources/db/migration/V1__initial_schema.sql`,
`V2__reference_seed.sql`, and `V3__cart_items.sql` automatically when the `mysql`
profile starts. Docker Compose no longer mounts SQL into `/docker-entrypoint-initdb.d`.
`database/01_schema.sql` and `02_seed.sql` remain reference/manual recovery scripts;
do not run them before Flyway on a new database.

For an existing normalized database, **back up the data first**. Flyway's
`baseline-on-migrate` marks the existing 15-table schema as version 1 and runs
subsequent migrations. If the old schema is incomplete, repair it before starting
this version of the app. Never edit an applied migration; add a new version.

## Legacy manual setup with MySQL Workbench

1. Start **MySQL Server** on Windows.
2. Open **MySQL Workbench** and connect as a MySQL administrator.
3. Select **File > Open SQL Script**.
4. Open and execute `01_schema.sql` by selecting the lightning icon.
5. Open and execute `02_seed.sql` to add sample products.
6. Optionally execute `03_create_local_user.sql` to create a local application account.
7. In the **Schemas** panel, select refresh and expand `sales_service > Tables`.

If you executed an older version of `01_schema.sql` before Order support was added, run
`04_upgrade_existing_schema.sql` once. Do not run this upgrade file on a database freshly
created from the latest `01_schema.sql`.

If your database already has the original four tables, run these files in order:

```text
05_prepare_normalized_upgrade.sql
01_schema.sql
02_seed.sql
```

`05_prepare_normalized_upgrade.sql` removes the derived `line_total` column. Rerunning
`01_schema.sql` is safe because all tables use `CREATE TABLE IF NOT EXISTS`; it only adds
the missing normalized tables and views.

Stop the Spring Boot application while running the upgrade, then rebuild/restart it because
the updated JPA mapping also no longer persists `line_total`.

Expected tables:

```text
users
customer_profiles
customer_addresses
offices
employee_profiles
products
product_categories
product_category_assignments
suppliers
product_suppliers
orders
order_items
payments
order_status_history
inventory_movements
cart_items
admin_account_audit
```

## Verify the schema

Run in a Workbench query tab:

```sql
USE sales_service;

SHOW TABLES;

SELECT id, sku, name, price, stock_quantity, status
FROM products;

SELECT * FROM v_product_catalog;
SELECT * FROM v_order_calculated_totals;
```

## Generate an EER diagram in Workbench

After creating the database:

1. Select **Database > Reverse Engineer**.
2. Choose the local MySQL connection.
3. Select the `sales_service` schema.
4. Complete the wizard and choose **Place imported objects on a diagram**.
5. Save the model as `sales_service.mwb` if the team wants to version the diagram.

## Important notes

- `line_total` is not stored; it is calculated from the snapshot price and quantity.
- Product deletion is represented by `status = 'INACTIVE'`; do not physically delete products referenced by orders.
- Creating an order, inserting its items, and subtracting stock must happen in one transaction in the application.
- New catalog, payment, staff and inventory-history tables are ready for later APIs; the current API continues to use the compatible core tables.
- Run the application with `SPRING_PROFILES_ACTIVE=mysql` after executing the scripts.
- Never commit real database passwords. Use environment variables for shared or production environments.
