#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
for script in scripts/*.sh; do bash -n "$script"; done
python3 scripts/static-check.py
python3 scripts/test-quotes.py
python3 scripts/check-secrets.py
