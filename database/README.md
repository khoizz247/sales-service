# MySQL database setup

The database currently consists of four tables:

```text
users 1 --- N orders 1 --- N order_items N --- 1 products
```

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

Expected tables:

```text
users
products
orders
order_items
```

## Verify the schema

Run in a Workbench query tab:

```sql
USE sales_service;

SHOW TABLES;

SELECT id, sku, name, price, stock_quantity, status
FROM products;
```

## Generate an EER diagram in Workbench

After creating the database:

1. Select **Database > Reverse Engineer**.
2. Choose the local MySQL connection.
3. Select the `sales_service` schema.
4. Complete the wizard and choose **Place imported objects on a diagram**.
5. Save the model as `sales_service.mwb` if the team wants to version the diagram.

## Important notes

- `line_total` is calculated by the application from the snapshot price and quantity.
- Product deletion is represented by `status = 'INACTIVE'`; do not physically delete products referenced by orders.
- Creating an order, inserting its items, and subtracting stock must happen in one transaction in the application.
- Run the application with `SPRING_PROFILES_ACTIVE=mysql` after executing all three scripts.
- Never commit real database passwords. Use environment variables for shared or production environments.
