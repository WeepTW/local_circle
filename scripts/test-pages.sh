#!/usr/bin/env bash
source "$(dirname "$0")/lib-test.sh"
install_frontend
(cd frontend && VITE_SHOWCASE=true VITE_BASE_PATH=/local_circle/ npm run build &&
  npx playwright install --with-deps chromium && npx playwright test --config=pages.config.ts)
