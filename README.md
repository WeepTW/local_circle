# local_circle

A financial product preference registry built with Vue 3, Spring Boot and MySQL.

[Interactive website](https://weeptw.github.io/local_circle/) · [Architecture](docs/ARCHITECTURE.md) · [Security review](docs/SECURITY_REVIEW.md)

Save and organize product preferences, select owned account references and inspect estimated amounts. Shared product updates require ADMIN **and** matching owner label. Versions protect concurrent writes; multi-table changes are transactional.

The website uses synthetic reference data stored only in the current browser tab. Refresh resets the showcase. Product prices are reference values maintained through product management. It does not connect to a bank, execute orders or move money. The Java/MySQL implementation is included for local execution; GitHub Pages serves only the static showcase.

## Scope

Market-data integration and a production login system are out of scope. No E.SUN SDK, quote exporter, market-data file or polling UI is included. Product management maintains reference prices; previously saved amount snapshots remain unchanged.

## CI incident

Run `35670087560` built artifacts with restrictive permissions because secret-file creation changed the parent shell's umask. A subshell now confines `umask 077` to the environment file, and Docker explicitly copies the JAR read-only for its non-root runtime user. Corrected run [35670538202](https://github.com/WeepTW/local_circle/actions/runs/35670538202) passed. The historical failed run remains unchanged; rerunning its old commit would reproduce the defect.

## Next improvements

1. Add preference export/import and a readable change history.
2. Add catalog search and sorting.
3. Add database backup/restore exercises and error-rate/latency monitoring.

## Run the complete application

Requirements: Java 21, Maven 3.9+, Node 24 (version pinned in `.node-version`), Docker Compose, Bash and Python 3. Use a Docker-enabled WSL/Linux shell.

```bash
bash scripts/bootstrap.sh
bash scripts/deploy.sh
```

Open http://localhost:8088/preferences. Bootstrap creates random database passwords in gitignored `.env`. Nginx, Java and MySQL run as separate services. Only loopback web/database development ports are published.

```bash
docker compose stop          # preserve data
docker compose up -d --wait  # restart
```

Keep a WSL session open while using the application. The local role selector is a development identity mechanism, not production authentication. The default backend profile rejects it. Do not expose the local API to an untrusted network.

## Development

```bash
bash scripts/bootstrap.sh
# Backend terminal
set -a; source .env; set +a
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
# Frontend terminal, from repository root
cd frontend
npm ci
npm run dev
```

Vite on 5173 proxies API requests to 8089. The browser never connects to MySQL. The application database role can execute procedures but cannot directly read or modify tables.

## Verify

```bash
cd frontend
npm ci
npx playwright install --with-deps chromium
cd ..
bash scripts/test-all.sh
```

The full gate runs independent suites. Database-backed suites create a unique Compose project with random loopback ports and restricted temporary credentials, then remove only their own containers and volumes. Normal application data is untouched. Do not run multiple suites simultaneously in the same checkout because Maven and frontend build outputs are shared.

| Command | Coverage |
|---|---|
| `bash scripts/test-static.sh` | Shell syntax, architecture, known-secret checks |
| `bash scripts/test-frontend.sh` | Node 24 unit tests, build and dependency audit |
| `bash scripts/test-backend.sh` | Fresh schema, all Java tests, zero skipped integration tests |
| `bash scripts/test-security.sh` | API abuse, stored XSS, secrets, dependencies, runtime hardening and image vulnerabilities |
| `bash scripts/test-e2e.sh` | Complete application and real browser flows |
| `bash scripts/test-pages.sh` | Pages build, subpath routing, isolated CRUD and reference catalog behavior |

Security checks exceed the functional baseline; see [security coverage and gate policy](docs/SECURITY_TESTING.md). Reports are written to `tmp/test-results`, Maven reports and Playwright report directories. Scanner/network errors fail the suite; they do not count as clean scans.

```bash
# Build and verify the static showcase
cd frontend
VITE_SHOWCASE=true VITE_BASE_PATH=/local_circle/ npm run build
npx playwright test --config=pages.config.ts
```

`python3 scripts/stability.py` sends 100 read-only requests with concurrency 10 to a running local instance.

## Design

- Presentation -> Business service -> Repository port -> Stored Procedure gateway.
- `RegistryRepository` method signatures remain unchanged.
- `ProcedureExecutor` centralizes JDBC resources, binding and query deadlines.
- `useRegistry` separates Vue state/write lifecycle from markup.
- HTTP and browser showcase adapters share a typed API facade.
- Saved amount snapshots retain Java double arithmetic and DECIMAL persistence.
- Typed errors do not expose full accounts, SQL internals or credentials.

## API

Base `/api/v1`; local-only `X-Demo-User-Id` header with predefined IDs 1–4.

| Method | Resource | Purpose |
|---|---|---|
| GET | /me | actor and capabilities |
| GET | /products | active catalog |
| GET | /admin/products | administrator catalog |
| GET | /accounts | owned active masked accounts |
| GET / POST | /preferences | list / save |
| GET / PUT / DELETE | /preferences/{id} | owned detail / update / delete |
| PATCH | /admin/products/{id} | authorized product update |

POST accepts productId, accountId, plannedQuantity; PUT adds version. DELETE requires `?version=N`. Unknown fields and fractional IDs/quantities are rejected. Stale writes return 409.

## Automation

GitHub Actions runs frontend unit tests, dependency audit/build and the reusable Pages browser verification on Ubuntu 24.04 with Node 24 and pinned Action commits. Only successful frontend and Pages jobs on main can deploy Pages. Pull requests only verify; manual runs are available through Frontend verification. Reports are retained for seven days.

Backend, database, full-stack E2E, static and expanded security checks remain available locally through `bash scripts/test-all.sh`. They do not run in GitHub Actions and are not prerequisites for static Pages deployment. A green GitHub run therefore verifies only the frontend. Public release excludes private input documents, environment files and development history.
