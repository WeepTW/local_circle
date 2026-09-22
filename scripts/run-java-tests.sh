#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ENV_FILE:?An isolated test environment is required}"
set -a
source "$ENV_FILE"
set +a
export RUN_DB_TESTS=true
export DB_URL="jdbc:mysql://127.0.0.1:${DB_PORT}/local_circle?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
(cd backend && mvn -B clean test "$@")
python3 scripts/verify-test-results.py
