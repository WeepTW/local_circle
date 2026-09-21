#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose "$@" exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot local_circle' < DB/006_demo_utf8_repair.sql
