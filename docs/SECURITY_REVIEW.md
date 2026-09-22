# Stability and security review

Review date: 2026-09-22. Scope: application code, dependency manifests, database access, local deployment and static public showcase.

## Remediation

- Preserved the repository port byte-for-byte. Extracted JDBC execution from row mapping, view state from Vue markup and transport from UI behavior.
- Added bounded HTTP, pool, query, transaction and proxy waits. Centralized mutation lifecycle rejects duplicate in-flight calls.
- Removed prominent sample-data branding while keeping an accurate reference-data notice. This does not turn the local identity selector into authentication.
- Public showcase uses isolated in-memory data and performs no API requests. Refresh clears its state. It contains no bank data or secrets.
- The catalog loads synthetic quotes from a same-origin static JSON file. Explicit application changes only current catalog prices; saved snapshots remain immutable. Invalid, unsupported or duplicate symbols and non-finite prices are rejected. Live-mode prices older than two minutes are not applied.
- The read-only provider adapter keeps credentials local and exports normalized prices to a gitignored file. Authentication and live provider access remain unverified until authorized credentials and SDK are supplied. Sample quotes are not advertised as current market prices.
- Applied CSP to the static showcase; full deployment retains Nginx CSP, nosniff and frame restrictions.
- Public release uses an explicit allowlist, excluding private inputs, internal instructions, local logs, environment files and prior development history.

## Dependency findings

Expanded container scanning found two High OpenJDK matches in the old application image and Critical matches in the old Nginx image's package set. The application base now explicitly uses Temurin 21.0.12+8. The web image now installs the Alpine-maintained Nginx package after applying stable-branch package updates, without the unused curl package. The image gate remains enabled; all three image reports are collected before the suite fails.

Initial OSV scan found advisory matches in Jackson Databind 2.21.4, Log4j API 2.24.3 and Tomcat 10.1.55. These are dependency-level findings, not proof that every advisory was exploitable through this application's configuration. Upgraded Jackson BOM to 2.21.5, Log4j to 2.25.5 and Tomcat to 10.1.59 without changing application contracts.

References: [Jackson](https://github.com/advisories/GHSA-5gvw-p9qm-jgwh), [Log4j API](https://github.com/advisories/GHSA-qv9r-c865-cp47), [Tomcat](https://github.com/advisories/GHSA-9xv2-5v5q-p794).

## Checks

- Application DB role has EXECUTE only; direct SELECT/UPDATE is rejected.
- SQL injection strings remain data; fixed procedure calls bind all values.
- Stored XSS payloads render as text without executable elements.
- Ownership, inactive accounts, strict fields, fractional inputs, ADMIN AND label, CORS and disabled non-local identity are tested.
- Real MySQL rollback covers second-write failure and database constraint failure.
- Concurrent updates and update/delete conflicts have one winner.
- Full-stack UI CRUD, account masking, responsive layout and network-error recovery pass.
- Three repeated browser suites: 9/9 passed before dependency patch; patched full gate reruns before publication.
- Bounded read test: 100 requests, concurrency 10, zero failures; observed p95 134 ms and max 150 ms locally. Not an SLA or capacity benchmark.
- Pages browser test covers subpath, deep-link reload, CRUD, isolation, mobile layout and zero API calls.

Final patched gate: 16 backend tests, 7 frontend tests and 3 full-stack browser tests passed. The Pages browser test also passed. Final OSV scan of 44 Java components and 228 npm package records returned zero known vulnerabilities; npm audit also returned zero. These results describe the versions and advisory database at review time.

Tomcat 10.1.58 was not published after its release vote failed; the actual published fix is 10.1.59, verified against [Apache's security notes](https://tomcat.apache.org/security-10.html). Public source and its new history are scanned for secrets before upload.

## Limits

The backend is for local use until a real identity provider, TLS boundary and operational controls are added. Pages demonstrates behavior with synthetic data; it is not a deployed banking API. These checks do not certify absence of all vulnerabilities. The expanded CI now includes container image vulnerability scans; see [coverage and blocking policy](SECURITY_TESTING.md). Formal compliance certification remains outside this review.
