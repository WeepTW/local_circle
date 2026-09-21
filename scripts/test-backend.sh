#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
set -a
source "${ENV_FILE:-.env}"
set +a
export RUN_DB_TESTS=true
export DB_URL="${DB_URL:-jdbc:mysql://127.0.0.1:${DB_PORT:-3307}/local_circle?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true}"
cd backend
mvn -B test "$@"
