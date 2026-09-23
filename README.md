# local_circle

A financial product preference registry built with Vue 3, Spring Boot and MySQL.

[Interactive website](https://weeptw.github.io/local_circle/) · [Architecture](docs/ARCHITECTURE.md) · [Security review](docs/SECURITY_REVIEW.md)

Save and organize product preferences, select owned account references and inspect estimated amounts. Shared product updates require ADMIN **and** matching owner label. Versions protect concurrent writes; multi-table changes are transactional.

The website uses synthetic reference data stored only in the current browser tab. Refresh resets the showcase. Product prices are reference values maintained through product management. It does not connect to a bank, execute orders or move money. The Java/MySQL implementation is included for local execution; GitHub Pages serves only the static showcase.

## Scope

Market-data integration is out of scope. The login screen supports a removable demonstration account provider; a real server-side login provider is not implemented. No E.SUN SDK, quote exporter, market-data file or polling UI is included. Product management maintains reference prices; previously saved amount snapshots remain unchanged.

## CI incident

Run `35670087560` built artifacts with restrictive permissions because secret-file creation changed the parent shell's umask. A subshell now confines `umask 077` to the environment file, and Docker explicitly copies the JAR read-only for its non-root runtime user. Corrected run [35670538202](https://github.com/WeepTW/local_circle/actions/runs/35670538202) passed. The historical failed run remains unchanged; rerunning its old commit would reproduce the defect.

## Next improvements

1. Add preference export/import and a readable change history.
2. Add catalog search and sorting.
3. Add database backup/restore exercises and error-rate/latency monitoring.

## Run the complete application

Requirements: Java 21, Maven 3.9+, Node 24 (version pinned in `.node-version`), Docker Compose v2, Git, OpenSSL, Bash and Python 3. Use a Docker-enabled WSL/Linux shell.

Keep a WSL terminal open. Clone into the Linux filesystem (for example `~/projects`) for faster dependency installation and builds:

```bash
mkdir -p ~/projects
cd ~/projects
git clone https://github.com/WeepTW/local_circle.git
cd local_circle
bash scripts/bootstrap.sh
bash scripts/deploy.sh
```

For a second checkout or review copy, choose an unused project name and ports **before the first bootstrap**:

```bash
COMPOSE_PROJECT_NAME=local_circle_review DB_PORT=13317 WEB_PORT=18088 bash scripts/bootstrap.sh
bash scripts/deploy.sh
```

Bootstrap saves those settings in the new `.env`, so later `docker compose stop` / `up` commands target the same project without extra shell exports. The review URL is `http://127.0.0.1:18088/preferences`. If `.env` already exists, edit its project/port settings explicitly; bootstrap never replaces existing passwords. Environment exports override `.env` for that shell only. The scripts refuse to modify containers belonging to another checkout with the same project name. Do not reuse another installation's persistent volume or delete it to bypass a startup error.


For the default settings, open http://localhost:8088/preferences; deploy prints the actual bound URL. Bootstrap creates random database passwords in gitignored `.env`. Nginx, Java and MySQL run as separate services. Only loopback web/database development ports are published.

```bash
docker compose stop          # preserve data
docker compose up -d --wait  # restart
```

Stop the host Maven process before starting another host backend on port 8089. Keep a WSL session open while using the application. The demonstration login selects a development identity; its public credentials are not production authentication. The default backend profile rejects it. Do not expose the local API to an untrusted network.

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
VITE_DEMO_LOGIN=true npm run dev
```

Run bootstrap before development to start MySQL. Source `.env` in the backend terminal as shown; the configured `DB_PORT` is used automatically unless `DB_URL` explicitly overrides it. Vite on 5173 proxies API requests to 8089. The browser never connects to MySQL. The application database role can execute procedures but cannot directly read or modify tables.

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
| `bash scripts/test-frontend.sh` | Node 24 unit tests, demo/production builds, demo credential exclusion and dependency audit |
| `bash scripts/test-backend.sh` | Fresh schema, all Java tests, zero skipped integration tests |
| `bash scripts/test-security.sh` | API abuse, stored XSS, secrets, dependencies, runtime hardening and image vulnerabilities |
| `bash scripts/test-e2e.sh` | Complete application and real browser flows |
| `bash scripts/test-pages.sh` | Pages build, subpath routing, isolated CRUD and reference catalog behavior |

Security checks exceed the functional baseline; see [security coverage and gate policy](docs/SECURITY_TESTING.md). Reports are written to `tmp/test-results`, Maven reports and Playwright report directories. Scanner/network errors fail the suite; they do not count as clean scans.

```bash
# Build and verify the static showcase
cd frontend
VITE_DEMO_LOGIN=true VITE_SHOWCASE=true VITE_BASE_PATH=/local_circle/ npm run build
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

## Modular login

The login screen uses a `LoginProvider` contract in `frontend/src/auth/contracts.ts`. Public sample accounts and their verification live only in `frontend/src/auth/demo/`. The dropdown fills credentials; login still requires explicit submission. Logout clears the active identity and unmounts all registry screens. Sessions and passwords are not persisted; refreshing requires login again. Role changes use logout/login, preserving existing ownership and product permissions.

Local deploy/test scripts and Pages builds explicitly set `VITE_DEMO_LOGIN=true`. A plain `npm run build` excludes the demo module. Vite chooses the provider at build time; `scripts/test-frontend.sh` checks that production assets contain none of the sample passwords.

For a production integration, leave `VITE_DEMO_LOGIN` unset or false and replace `frontend/src/auth/productionProvider.ts` with server-verified login. The default provider refuses every login. The complete `auth/demo/` folder can then be deleted without changing the login screen; remove the demo-only browser fixtures and explicit demo flags from your deployment/tests too. Replace the local HTTP identity header with real server session handling when implementing that provider. Disabling dropdown hints alone does not secure the backend. `RegistryRepository` remains unchanged.

## Functional boundaries

Preference creation selects an existing active product, an owned account reference and quantity. Product names, reference prices and fee rates are maintained by authorized product administrators. There is no arbitrary product-creation form or user-specific override of those fields. Accounts are pre-registered references and are displayed masked; the application does not collect a full account number. These boundaries should be considered when assessing a requirement for direct entry of product fields or complete account numbers.
