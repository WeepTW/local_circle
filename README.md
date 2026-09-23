# local_circle

A financial product preference registry built with Vue 3, Spring Boot and MySQL.

[Interactive website](https://weeptw.github.io/local_circle/) · [Architecture](docs/ARCHITECTURE.md) · [Security review](docs/SECURITY_REVIEW.md)

Save personal product names, prices and fee rates, select an owned account or enter a complete synthetic account number, and inspect estimated amounts. Shared product updates require ADMIN **and** matching owner label. Versions protect concurrent writes; multi-table changes are transactional.

The website uses synthetic reference data stored only in the current browser tab. Refresh resets the showcase. Product prices are reference values maintained through product management. It does not connect to a bank, execute orders or move money. The Java/MySQL implementation is included for local execution; GitHub Pages serves only the static showcase.

## Scope

Market-data integration is out of scope. The login screen supports a removable demonstration account provider; a real server-side login provider is not implemented. No E.SUN SDK, quote exporter, market-data file or polling UI is included. Product management maintains reference prices; previously saved amount snapshots remain unchanged.

## CI incident

Run `35670087560` built artifacts with restrictive permissions because secret-file creation changed the parent shell's umask. A subshell now confines `umask 077` to the environment file, and Docker explicitly copies the JAR read-only for its non-root runtime user. Corrected run [35670538202](https://github.com/WeepTW/local_circle/actions/runs/35670538202) passed. The historical failed run remains unchanged; rerunning its old commit would reproduce the defect.

## Next improvements

1. Add preference export/import and a readable change history.
2. Add catalog search and sorting.
3. Add error-rate/latency monitoring.

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
| `bash scripts/test-migration.sh` | Legacy upgrade, repeat execution, financial preservation and dump/restore |
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
- Personal preferences use BigDecimal arithmetic and DECIMAL persistence; fees round HALF_UP to 8 decimal places. The legacy calculator is retained for compatibility.
- Typed errors do not expose full accounts, SQL internals or credentials.

## API

Base `/api/v1`; local-only `X-Demo-User-Id` header with predefined IDs 1–4.

| Method | Resource | Purpose |
|---|---|---|
| GET | /me | actor and capabilities |
| GET | /products | active catalog |
| GET | /admin/products | administrator catalog |
| GET | /accounts | owned active masked accounts |
| GET | /accounts/{id}/number | reveal owned account; no-store response |
| GET / POST | /preferences | list / save |
| GET / PUT / DELETE | /preferences/{id} | owned detail / update / delete |
| PATCH | /admin/products/{id} | authorized product update |

POST accepts optional productId, productName/price/feeRate (all three together), plannedQuantity, and exactly one of accountId or accountNumber. Without custom fields, productId supplies the catalog defaults for backward compatibility. PUT adds version. Personal fields never update the shared catalog. DELETE requires `?version=N`. Unknown fields and fractional IDs/quantities are rejected. Stale writes return 409.

## Automation

GitHub Actions runs frontend unit tests, dependency audit/build and the reusable Pages browser verification on Ubuntu 24.04 with Node 24 and pinned Action commits. Only successful frontend and Pages jobs on main can deploy Pages. Pull requests only verify; manual runs are available through Frontend verification. Reports are retained for seven days.

Backend, database, full-stack E2E, static and expanded security checks remain available locally through `bash scripts/test-all.sh`. They do not run in GitHub Actions and are not prerequisites for static Pages deployment. A green GitHub run therefore verifies only the frontend. Public release excludes private input documents, environment files and development history.

## Modular login

The login screen uses a `LoginProvider` contract in `frontend/src/auth/contracts.ts`. Public sample accounts and their verification live only in `frontend/src/auth/demo/`. The dropdown fills credentials; login still requires explicit submission. Logout clears the active identity and unmounts all registry screens. Sessions and passwords are not persisted; refreshing requires login again. Role changes use logout/login, preserving existing ownership and product permissions.

Local deploy/test scripts and Pages builds explicitly set `VITE_DEMO_LOGIN=true`. A plain `npm run build` excludes the demo module. Vite chooses the provider at build time; `scripts/test-frontend.sh` checks that production assets contain none of the sample passwords.

For a production integration, leave `VITE_DEMO_LOGIN` unset or false and replace `frontend/src/auth/productionProvider.ts` with server-verified login. The default provider refuses every login. The complete `auth/demo/` folder can then be deleted without changing the login screen; remove the demo-only browser fixtures and explicit demo flags from your deployment/tests too. Replace the local HTTP identity header with real server session handling when implementing that provider. Disabling dropdown hints alone does not secure the backend. `RegistryRepository` remains unchanged.


## Personal products and encrypted accounts

Choose a catalog item to prefill the preference dialog, or select the custom-product option. Edit the name (1–160 characters), price (0–999999999999; up to 8 decimal places), fee rate (0–1; up to 12 decimal places) and quantity (1–1000000). Only your saved preference changes. Catalog updates do not rewrite its name or financial snapshots. Total is principal plus the fee; total values must remain below 10^16.

Account numbers accept 6–32 digits and preserve leading zeros. Use only synthetic values: the removable demonstration login is not a production identity system. The full local stack encrypts account numbers using AES-256-GCM with a random nonce and owner/account-bound authenticated data. The UI masks numbers until an explicit reveal and hides them again after 30 seconds. Pages keeps synthetic numbers only in memory; it does not provide server-side encryption or persist numbers in browser storage.

Bootstrap creates `.secrets/account.key` only for a new installation. The containing directory is mode 0700; the file is read-only and is the only file mounted into the non-root app. Keep the directory on Linux storage. Back up the key separately from SQL backups, preserve its directory permissions, and never commit or print it. Restarting never generates a replacement. Missing, malformed or mismatched keys cause explicit failures. The stored key identifier permits verification; key rotation is not automated. To recover, restore the matching original key, not a newly generated one.

Fresh local installs initialize predefined accounts with synthetic full numbers. Existing records with only last four digits stay masked and are marked incomplete; enter a complete synthetic number when editing a preference. Historical names unavailable in old data are populated from the current catalog during migration; no earlier name is fabricated.

## Upgrade and recovery

For an existing checkout and database, stop only its application services, then migrate before deploying the new application:

```bash
docker compose stop app web
bash scripts/db-maintenance.sh migrate
bash scripts/deploy.sh
```

Migration saves a restricted SQL backup under ignored `backups/`, applies the versioned, retryable schema upgrade, and records version 6. MySQL DDL commits independently; on failure, keep app/web stopped and retry only after resolving the error, or restore the backup into an isolated empty database. No volume is deleted. A key is created for legacy data only if no encrypted account/key-check data exists. Otherwise restore the original key.

For backups: `bash scripts/db-maintenance.sh backup backups/manual.sql` (create the destination directory first). For recovery, create a separate Compose project with an **empty** `local_circle` database and app/web stopped, then run `bash scripts/db-maintenance.sh restore /path/to/backup.sql`. This refuses a populated target. Restore the matching key separately to that checkout's private `.secrets/` directory before starting the app. Do not enable shell tracing while managing secrets.

All image severities are release-blocking until fixed or individually proven not affected with time-limited evidence. `scripts/assess-vulnerabilities.py` retains raw matches and produces a per-package disposition. Unresolved findings mean branch-only delivery: no main merge or Pages deployment, even if frontend CI passes.
