#!/usr/bin/env bash
source "$(dirname "$0")/lib-test.sh"
install_frontend
init_stack security
# Includes all Java suites so rollback/concurrency/DB privilege checks cannot be omitted.
bash scripts/run-java-tests.sh
build_backend
build_frontend
start_web
python3 scripts/security-http.py
mapfile -t ids < <("${compose[@]}" ps -q)
python3 scripts/security-container.py "${ids[@]}"
(cd frontend && npx playwright install --with-deps chromium && npx playwright test --grep 'stored XSS')
(cd frontend && npm audit --audit-level=moderate)
python3 scripts/check-secrets.py
stage=$(python3 scripts/security-source.py)
scan_container -v "$stage:/src:ro" -v "$report_dir:/reports" \
  zricethezav/gitleaks@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f \
  dir /src --redact=100 --no-banner --report-format json --report-path /reports/gitleaks.json
(cd backend && mvn -B org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom)
scan_container -v "$root:/repo:ro" \
  ghcr.io/google/osv-scanner@sha256:5116601dedc01c1c580eb92371883ec052fc4c13c3fbc109d621a63ac416d475 \
  scan source -L /repo/backend/target/bom.json -L /repo/frontend/package-lock.json --format=json > "$report_dir/osv.json"
# Keep SQLite advisory cache on Linux storage (WSL mounted Windows paths are slow).
mkdir -p /tmp/local-circle-grype-cache
for service in app web db; do
  image=$(docker inspect "$("${compose[@]}" ps -q "$service")" --format '{{.Image}}')
  scan_container -v /var/run/docker.sock:/var/run/docker.sock -v "$report_dir:/reports" \
    -v /tmp/local-circle-grype-cache:/root/.cache/grype \
    anchore/grype@sha256:391bfda62888fb4e98ff5c4c81598f7431a3c1eac3f8519d69d1ff00df247c1d \
    "docker:$image" --fail-on critical -o json --file "/reports/image-$service.json"
done
echo 'PASS: application, browser, dependency, secret and container security gates'
