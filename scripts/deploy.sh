#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test -f .env || { echo 'Run bash scripts/bootstrap.sh first'; exit 1; }
test -f "${ACCOUNT_KEY_FILE:-.secrets/account.key}" || { echo "Account key missing; restore it or run the documented legacy migration." >&2; exit 1; }
python3 scripts/check-compose-project.py
(cd backend && mvn -B -DskipTests package)
(cd frontend && npm ci && VITE_DEMO_LOGIN=true VITE_SHOWCASE=false VITE_BASE_PATH=/ npm run build)
docker compose up -d --build --wait
address=$(docker compose port web 80)
printf 'local_circle ready: http://%s/preferences\n' "$address"
