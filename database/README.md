# MySQL database setup

The database is organized into five domains:

```text
Identity:   users, customer_profiles, customer_addresses
Staff:      offices, employee_profiles
Catalog:    products, product_categories, suppliers and two junction tables
Sales:      orders, order_items, payments, order_status_history
Inventory:  inventory_movements
```

The normalization analysis and full ER diagram are in [`NORMALIZATION.md`](NORMALIZATION.md).

## Run with MySQL Workbench

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
