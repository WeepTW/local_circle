#!/usr/bin/env bash
source "$(dirname "$0")/lib-test.sh"
init_stack backend
bash scripts/verify-fresh-db.sh --env-file "$envfile" -p "$project"
bash scripts/run-java-tests.sh "$@"
