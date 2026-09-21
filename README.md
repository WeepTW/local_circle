# local_circle

A financial product preference registry built with Vue 3, Spring Boot and MySQL.

[Interactive website](https://weeptw.github.io/local_circle/) · [Architecture](docs/ARCHITECTURE.md) · [Security review](docs/SECURITY_REVIEW.md)

Save and organize product preferences, select owned account references and inspect estimated amounts. Shared product updates require ADMIN **and** matching owner label. Versions protect concurrent writes; multi-table changes are transactional.

The website uses synthetic reference data stored only in the current browser tab. Refresh resets the showcase. It does not call a backend, connect to a bank, execute orders or move money. The Java/MySQL implementation is included for local execution; GitHub Pages serves only the static showcase.

## Run the complete application

Requirements: Java 21, Maven 3.9+, Node 22.12+, Docker Compose, Bash and Python 3. Use a Docker-enabled WSL/Linux shell.

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

The full gate uses an isolated Compose project on ports 8188/3317, initializes a fresh schema, runs backend/frontend tests and real browser flows. It stops only the acceptance instance afterward; normal application data is untouched.

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

GitHub Actions verifies the complete stack and builds/tests/deploys Pages. Actions are pinned to commit SHAs with minimal deployment permissions. Public release includes source code and technical documentation; private input documents, local logs, environment files and development history are excluded.
