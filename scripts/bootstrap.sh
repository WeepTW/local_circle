#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -f .env ]; then
  umask 077
  printf 'MYSQL_ROOT_PASSWORD=%s\nMYSQL_PASSWORD=%s\nDB_PORT=3307\nWEB_PORT=8088\n' "$(openssl rand -hex 24)" "$(openssl rand -hex 24)" > .env
fi
docker compose up -d --wait db
docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -h127.0.0.1 -uroot local_circle -e "SELECT VERSION(); CALL sp_product_list_active(); SHOW GRANTS FOR local_circle;"'
