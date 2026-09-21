#!/usr/bin/env bash
# Creates and removes only its own temporary schema. Existing name => fail safely.
set -euo pipefail
cd "$(dirname "$0")/.."
compose=(docker compose "$@")
mysql() { "${compose[@]}" exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -N -uroot "$@"' sh "$@"; }
mysql -e 'CREATE DATABASE local_circle_fresh_check CHARACTER SET utf8mb4'
trap "mysql -e 'DROP DATABASE local_circle_fresh_check'" EXIT
for file in DB/001_ddl.sql DB/002_stored_procedures.sql DB/003_seed_demo.sql; do mysql local_circle_fresh_check < "$file"; done
actual=$(mysql local_circle_fresh_check -e 'SELECT product_name FROM product ORDER BY product_id')
expected=$(printf '元大台灣50 ETF（DEMO）\n富邦科技 ETF（DEMO）\n全球前十大股票示範組合（DEMO）')
test "$actual" = "$expected"
echo 'PASS: fresh DDL, all stored procedures and exact UTF-8 demo names'
