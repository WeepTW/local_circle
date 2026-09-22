#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
for suite in static frontend backend security e2e pages; do
  bash "scripts/test-$suite.sh"
done
echo 'PASS: all independent verification suites'
