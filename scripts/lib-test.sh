#!/usr/bin/env bash
# Standalone test entrypoints never target the development stack.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
root=$PWD
check_node() {
  [[ $(node -p 'process.versions.node.split(".")[0]') == 24 ]] || { echo 'Node.js 24 is required.' >&2; exit 1; }
}
install_frontend() { check_node; (cd frontend && npm ci); }
build_backend() { (cd backend && mvn -B -DskipTests package); }
build_frontend() { (cd frontend && VITE_SHOWCASE=false VITE_BASE_PATH=/ npm run build); }
init_stack() {
  local kind=$1
  mkdir -p tmp/ci tmp/test-results
  run_dir=$(mktemp -d "/tmp/lc-tests-${kind}.XXXXXX")
  project="lc_test_${kind}_$(basename "$run_dir" | tr '[:upper:].' '[:lower:]_')"
  report_dir="$root/tmp/test-results/$kind/$(basename "$run_dir")"
  mkdir -p "$report_dir"
  envfile="$run_dir/runtime.env"
  (umask 077; python3 - "$envfile" <<'PY'
import secrets, socket, sys
with socket.socket() as db, socket.socket() as web:
    db.bind(('127.0.0.1', 0)); web.bind(('127.0.0.1', 0))
    with open(sys.argv[1], 'w') as f:
        f.write(f'MYSQL_ROOT_PASSWORD={secrets.token_hex(24)}\nMYSQL_PASSWORD={secrets.token_hex(24)}\nDB_PORT={db.getsockname()[1]}\nWEB_PORT={web.getsockname()[1]}\n')
PY
  )
  chmod 600 "$envfile"
  [[ $(stat -c '%a' "$envfile") == 600 ]] || { echo 'Unsafe credential permissions' >&2; exit 1; }
  compose=(docker compose --env-file "$envfile" -p "$project")
  trap cleanup_stack EXIT
  "${compose[@]}" up -d --wait --wait-timeout 240 db
  export ENV_FILE="$envfile"
}
cleanup_stack() {
  local result=$?
  trap - EXIT
  if [[ $result != 0 ]]; then
    "${compose[@]}" logs --no-color app web 2>&1 | python3 -c '
import pathlib,sys
secrets=[s.partition("=")[2] for s in pathlib.Path(sys.argv[1]).read_text().splitlines() if "PASSWORD=" in s]
text=sys.stdin.read()
for secret in secrets: text=text.replace(secret,"[REDACTED]")
pathlib.Path(sys.argv[2]).write_text(text)
' "$envfile" "$report_dir/containers.log" || true
  fi
  if [[ $project == lc_test_* ]]; then
    docker rm -f "${project}_scan" >/dev/null 2>&1 || true
    "${compose[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || result=1
    rm -f -- "$envfile"
    rmdir -- "$run_dir" || result=1
  else result=1; fi
  exit "$result"
}
start_web() {
  "${compose[@]}" up -d --build --wait --wait-timeout 240
  export BASE_URL="http://$("${compose[@]}" port web 80)"
}
scan_container() { timeout 600 docker run --name "${project}_scan" --rm "$@"; }
