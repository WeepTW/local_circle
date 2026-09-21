#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test -f .env || { echo 'Run bash scripts/bootstrap.sh first'; exit 1; }
(cd backend && mvn -B -DskipTests package)
(cd frontend && npm ci && npm run build)
docker compose up -d --build --wait
printf 'local_circle ready: http://localhost:%s/preferences\n' "$(sed -n 's/^WEB_PORT=//p' .env)"
