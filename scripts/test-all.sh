#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
failed=()
for suite in static frontend backend migration security e2e pages; do
  if ! bash "scripts/test-$suite.sh"; then failed+=("$suite"); fi
done
if ((${#failed[@]})); then printf 'FAIL/BLOCKED suites: %s\n' "${failed[*]}" >&2; exit 1; fi
echo 'PASS: all independent verification suites'
