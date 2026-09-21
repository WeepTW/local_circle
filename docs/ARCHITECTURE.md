# Architecture

## Runtime boundaries

Vue presents data and delegates actions to `useRegistry`. A stable `api` facade selects an HTTP adapter for the full application or an in-memory adapter for the static showcase. Both adapters implement the same typed contract. Presentation logic does not know how data is stored.

The full stack uses Nginx -> Spring Boot -> MySQL. Controllers validate requests and delegate to transactional business services. `RegistryRepository` is the unchanged domain port. `StoredProcedureGateway` maps domain objects to fixed procedure calls; `ProcedureExecutor` owns JDBC resources, parameter binding and query deadlines.

## Core rules

- Shared product master: writes require ADMIN AND matching owner label.
- Preferences and active account references belong to the selected actor; IDs never imply authorization.
- Save/update/delete and audit writes share a transaction. Versions reject stale writes.
- Saved amounts are snapshots; explicit preference updates refresh them.
- Java double arithmetic is retained. JDBC DECIMAL conversion is a persistence concern, not a new rounding policy.
- Typed repository methods and API request/response shapes remain stable.

## GitHub Pages

The showcase is a separate static deployment. It uses synthetic, per-tab in-memory records, no API requests, no database connection and no persistent bank information. Refresh resets its state. Hash routing and a repository base path support deep-link refresh under `/local_circle/`.

It demonstrates behavior, not a publicly deployed Java service. The Java application retains local-profile test identity handling and refuses that header outside the local profile. Real public backend hosting requires a separately implemented authentication provider.

## Reliability

Writes reject duplicate in-flight submission in the UI. Stale responses cannot replace a newer identity's state. HTTP requests have a 15-second timeout; JDBC calls have a 10-second query timeout, pool acquisition 10 seconds and transaction timeout 15 seconds. Nginx has bounded upstream connection/read/send timeouts. These limits prevent indefinite waiting; they are not performance guarantees.
