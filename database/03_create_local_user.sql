-- Optional local development account.
-- Run as a MySQL administrator. These credentials are examples only.

CREATE USER IF NOT EXISTS 'sales_user'@'localhost'
    IDENTIFIED BY 'sales_password';

GRANT SELECT, INSERT, UPDATE, DELETE
    ON sales_service.*
    TO 'sales_user'@'localhost';

FLUSH PRIVILEGES;
