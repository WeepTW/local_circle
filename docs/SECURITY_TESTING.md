# Security test coverage

The functional baseline includes SQL injection and XSS prevention. The independent security suite additionally verifies the controls below. It probes only disposable loopback deployments and never targets a bank or another external application.

| Control | Executable evidence |
|---|---|
| SQL injection | Bound stored-procedure payload remains data; product table remains intact |
| Stored XSS | Real Chromium renders malicious product name as text, no injected element or script execution |
| Object-level authorization | Another user cannot read, update or delete a saved preference; victim version/amount remain unchanged |
| Function-level authorization | Role AND product label required; wrong administrator and ordinary user rejected |
| Identity boundary | Missing, malformed and duplicate identity headers rejected; local identity disabled outside local profile |
| Overposting and validation | Unknown privileged fields, fractions and overflowing quantities rejected |
| Origin restrictions | Null, hostile and deceptive localhost suffixes rejected for preflight and writes |
| Error privacy | Malformed input not reflected; generic details, trace ID and no SQL/Java/credential disclosure |
| HTTP protocol | Unsupported media/method return 415/405, not an internal error |
| Browser/cache controls | Actual proxy response has CSP, frame denial, nosniff; API responses disallow caching |
| Exposure and limits | Environment, Git and source files not served; proxy rejects requests over 16 KiB |
| Database least privilege | Application role cannot directly SELECT/UPDATE tables |
| Integrity | Multi-table rollback, competing updates and stale-version conflicts |
| Runtime hardening | Non-root app, readable read-only JAR, read-only filesystem, dropped capabilities, loopback-only published ports |
| Secrets | Redacted Gitleaks scan of public source plus known-secret scan of tracked/build files |
| Supply chain | npm audit, CycloneDX Java SBOM and OSV scan, Grype app/web/database image scans |

## Blocking policy

- All security assertions must pass. Java reports must exist, include integration tests and contain no skips.
- Gitleaks findings block. npm vulnerabilities at moderate or above block. Every OSV finding blocks.
- Container Critical findings block. All lower-severity findings remain in the local JSON reports and require review; a passing job does not mean images have zero vulnerabilities.
- Scanner errors, unavailable advisory databases and timeouts fail closed. Scanner images and Actions use immutable digests/commits. No blanket ignore file or automatic suppression is used.
- Local reports are written to `tmp/test-results`; GitHub frontend reports are retained for seven days. Raw credentials and environment files are never included. Container failure diagnostics are redacted before storage.

## Limits

This is automated verification, not a penetration-test certification. The local role selector is not production authentication. TLS, real login/session/CSRF controls, rate limiting against sustained abuse, production authorization and Internet deployment are not certified by these tests. No load-based denial-of-service traffic is generated. Real bank APIs are outside the test target scope.

## Execution scope

GitHub runs only frontend unit/build/audit and static Pages browser tests. All backend, database, API security, full-stack browser and image checks remain in the local `scripts/test-all.sh` pipeline. Production login and bank market-data integration are outside the requested project scope.
