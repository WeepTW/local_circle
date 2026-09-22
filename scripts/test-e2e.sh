#!/usr/bin/env bash
source "$(dirname "$0")/lib-test.sh"
install_frontend
build_frontend
build_backend
init_stack e2e
start_web
(cd frontend && npx playwright install --with-deps chromium && npm run test:e2e)
