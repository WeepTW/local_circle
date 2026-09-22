#!/usr/bin/env bash
source "$(dirname "$0")/lib-test.sh"
install_frontend
(cd frontend && npm test && npm audit --audit-level=moderate)
(cd frontend && VITE_DEMO_LOGIN=false VITE_SHOWCASE=false VITE_BASE_PATH=/ npm run build)
python3 scripts/check-production-login.py
build_frontend
