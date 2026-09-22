# local_circle

A financial product preference registry built with Vue 3, Spring Boot and MySQL.

[Interactive website](https://weeptw.github.io/local_circle/) · [Architecture](docs/ARCHITECTURE.md) · [Security review](docs/SECURITY_REVIEW.md)

Save and organize product preferences, select owned account references and inspect estimated amounts. Shared product updates require ADMIN **and** matching owner label. Versions protect concurrent writes; multi-table changes are transactional.

The website uses synthetic reference data stored only in the current browser tab. Refresh resets the showcase. The product catalog loads a same-origin sample quote file every 60 seconds while visible; applying it updates catalog prices without changing saved amount snapshots. It does not connect to a bank, execute orders or move money. The Java/MySQL implementation is included for local execution; GitHub Pages serves only the static showcase.

## Market data

The published `frontend/public/data/quotes.sample.json` is fabricated test data, not current prices. It covers 0050 and 0052; GLOBAL_TOP10 is a synthetic product with no market symbol. Open the product catalog to inspect timestamps, reload the file and apply sample prices.

`scripts/export-quotes.py` implements a read-only adapter using the official E.SUN Securities Python SDK. Install the SDK supplied by the provider in a compatible Python environment, obtain market-data permission and configure the provider's local INI file. Never commit credentials. Run:

```bash
timeout 30s python3 scripts/export-quotes.py --config /private/path/config.ini
```

The adapter writes only normalized actual-trade prices and trade timestamps to gitignored `tmp/quotes.live.json`. It excludes trial-auction prices and account data. Adapter parsing is tested with synthetic responses; authenticated SDK execution is not yet verified. No live data is uploaded or automatically substituted into Pages.

Live browser integration requires an authenticated server-side collector, a same-origin quote endpoint, provider permission for the intended display audience, and freshness/rate-limit controls. Pages cannot securely host bank credentials or run the collector. Keep live source timestamps, distinguish market closure from provider errors, and never silently fall back to fabricated prices.

Official references: [SDK setup](https://www.esunsec.com.tw/trading-platforms/api-trading/docs/market-data/http-api/getting-started/), [quote schema](https://www.esunsec.com.tw/trading-platforms/api-trading/docs/market-data/http-api/intraday/quote/).

## CI incident

Run `35670087560` built artifacts with restrictive permissions because secret-file creation changed the parent shell's umask. A subshell now confines `umask 077` to the environment file, and Docker explicitly copies the JAR read-only for its non-root runtime user. Corrected run [35670538202](https://github.com/WeepTW/local_circle/actions/runs/35670538202) passed. The historical failed run remains unchanged; rerunning its old commit would reproduce the defect.

## Next improvements

1. Replace local identity selection with OIDC login and server-enforced sessions before exposing the full backend.
2. Deploy an authorized quote collector with bounded retries, shared caching, source timestamps and stale-data monitoring.
3. Persist user preferences, provide export/import and maintain an audit trail for changes.
4. Add watchlist search, sorting and opt-in price alerts with clear delayed/closed-market states.
5. Add database backup/restore exercises and error-rate/latency monitoring.

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
