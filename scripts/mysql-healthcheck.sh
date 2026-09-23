#!/bin/sh
set -eu

# Flyway creates the schema when the API starts, so only test database readiness here.
MYSQL_PWD="$MYSQL_PASSWORD" mysql --protocol=tcp --host=127.0.0.1 \
    --user="$MYSQL_USER" --database=sales_service --batch --skip-column-names \
    --execute="SELECT 1" | grep -qx 1

echo "sales_service MySQL ready"
