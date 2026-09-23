#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -f .env ]; then
 project=${COMPOSE_PROJECT_NAME:-local_circle}
 db_port=${DB_PORT:-3307}
 web_port=${WEB_PORT:-8088}
 [[ $project =~ ^[a-z0-9][a-z0-9_-]*$ ]] || { echo 'Invalid COMPOSE_PROJECT_NAME' >&2; exit 1; }
 for port in "$db_port" "$web_port"; do
  [[ $port =~ ^[1-9][0-9]{0,4}$ ]] && ((port <= 65535)) || { echo 'Invalid port' >&2; exit 1; }
 done
 (
  umask 077
  printf 'COMPOSE_PROJECT_NAME=%s\nMYSQL_ROOT_PASSWORD=%s\nMYSQL_PASSWORD=%s\nDB_PORT=%s\nWEB_PORT=%s\n' \
    "$project" "$(openssl rand -hex 24)" "$(openssl rand -hex 24)" "$db_port" "$web_port" > .env
 )
fi
python3 scripts/check-compose-project.py
docker compose up -d --wait db
docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -h127.0.0.1 -uroot local_circle -e "SELECT VERSION(); CALL sp_product_list_active(); SHOW GRANTS FOR local_circle;"'
