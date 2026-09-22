#!/usr/bin/env bash
source "$(dirname "$0")/lib-test.sh"
install_frontend
(cd frontend && npm test && npm audit --audit-level=moderate)
build_frontend
