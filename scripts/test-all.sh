#!/usr/bin/env bash
# Isolated acceptance instance. Never resets the regular local_circle volume.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p tmp
envfile=tmp/acceptance.env
if [ ! -f "$envfile" ]; then
 umask 077
 printf 'MYSQL_ROOT_PASSWORD=%s\nMYSQL_PASSWORD=%s\nDB_PORT=3317\nWEB_PORT=8188\n' "$(openssl rand -hex 24)" "$(openssl rand -hex 24)" > "$envfile"
fi
compose=(docker compose --env-file "$envfile" -p local_circle_acceptance)
trap '"${compose[@]}" stop >/dev/null' EXIT
"${compose[@]}" up -d --wait db
bash scripts/verify-fresh-db.sh --env-file "$envfile" -p local_circle_acceptance
ENV_FILE="$envfile" bash scripts/test-backend.sh
(cd backend && mvn -B -DskipTests package)
(cd frontend && npm ci && npm test && npm run build && npm audit --audit-level=moderate)
"${compose[@]}" up -d --build --wait
(cd frontend && BASE_URL=http://127.0.0.1:8188 npm run test:e2e)
python3 scripts/static-check.py
echo 'PASS: isolated full acceptance. Test volumes retained; regular demo data untouched.'
