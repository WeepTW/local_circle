# Security disposition — 2026-09-23

**Release status: BLOCKED.** No findings are suppressed. Keep this work on its correction branch; do not merge or deploy while unresolved findings remain.

The previous images had 411 package/advisory matches: 10 High, 280 Medium, 106 Low and 15 Negligible. The final images have 24: 5 High and 19 Medium. These are matches, not distinct exploitable vulnerabilities.

The [baseline comparison](security-baseline-comparison.csv) accounts for all 411 earlier records: 20 still match, 391 no longer match. There are 4 new image/package/advisory combinations. In particular, the Alpine application runtime now includes a zlib High match and three BusyBox Medium matches; this tradeoff is disclosed and blocks release. A lower match count is not proof that every risk decreased.

## Applied changes

- Replaced the application JDK/Jammy runtime with a pinned Java 21 JRE/Alpine runtime; removed unused GnuPG and coreutils. The actual application, encryption and health checks were exercised.
- Updated MySQL 8.4 packages and removed unused MySQL Shell, eliminating its bundled Python package findings. Rebuilt gosu with Go 1.26.8 and golang.org/x/sys 0.44.0; privilege switching and database initialization passed.
- Updated the web base to pinned Alpine 3.24 and applied stable-channel updates. Retained package metadata and scanned actual built images.
- npm audit, Java/npm OSV scan and Gitleaks returned no findings in this run. This does not override container findings.

Grype database: v6.1.9, built 2026-09-23T06:31:39Z, reported valid. The same scanner and all severities were retained. Raw reports are kept with the local verification evidence; [machine-readable current findings](security-findings.json) record exact image IDs and dispositions.

## Unresolved groups

1. **zlib: 2 High matches.** Both application and web contain 1.3.2-r0. The [upstream site](https://www.zlib.net/) still lists 1.3.2; [Ubuntu analysis](https://ubuntu.com/security/CVE-2026-85091) cautions that one proposed patch does not fix its reproducer. No unverified patch or version downgrade was applied. A tested vendor fix, or rigorous workload-specific evidence, is required.
2. **BusyBox: 6 Medium matches.** busybox, busybox-binsh and ssl_client in each Alpine image match the same wget request-target issue. The [upstream patch discussion](https://lists.busybox.net/pipermail/busybox/2025-November/091817.html) was reviewed. Current health checks use constant loopback URLs; user input is not passed to wget. This limits the exposed path but is not declared a completed fix.
3. **Oracle packages: 16 matches, including 3 High.** Installed OpenSSL 3.5.8-1.0.1.el9_8, GnuTLS 3.8.10-8.el9_8 and libgcrypt 1.10.0-13.el9_8 are non-FIPS packages. Scanner fix targets use the FIPS channel and epoch 10. [OpenSSL](https://linux.oracle.com/errata/ELSA-2026-50075.html) and [GnuTLS](https://linux.oracle.com/errata/ELSA-2026-50346.html) advisories explicitly describe this epoch change. Image RPM changelogs were captured. This is evidence of a matching ambiguity, not sufficient blanket proof that every CVE is inapplicable. Each row remains blocking pending standard-channel vendor mapping; the libgcrypt advisory could not be independently retrieved during review.

No exceptions were added to security-decisions.json. A future not-affected decision must identify the exact image, package and advisory, include evidence and primary sources, and have a review deadline. Report omission, expired decisions and unresolved findings fail the gate.

## Every remaining match

| Image | Package / version | Advisory | Severity | Disposition |
|---|---|---|---|---|
| image-app | zlib 1.3.2-r0 | [CVE-2026-85091](https://nvd.nist.gov/vuln/detail/CVE-2026-85091) | High | unresolved |
| image-app | busybox 1.37.0-r31 | [CVE-2025-60876](https://nvd.nist.gov/vuln/detail/CVE-2025-60876) | Medium | unresolved |
| image-app | busybox-binsh 1.37.0-r31 | [CVE-2025-60876](https://nvd.nist.gov/vuln/detail/CVE-2025-60876) | Medium | unresolved |
| image-app | ssl_client 1.37.0-r31 | [CVE-2025-60876](https://nvd.nist.gov/vuln/detail/CVE-2025-60876) | Medium | unresolved |
| image-db | openssl 1:3.5.8-1.0.1.el9_8 | [ELSA-2024-12675](https://linux.oracle.com/errata/ELSA-2024-12675.html) | Medium | unresolved |
| image-db | openssl-libs 1:3.5.8-1.0.1.el9_8 | [ELSA-2024-12675](https://linux.oracle.com/errata/ELSA-2024-12675.html) | Medium | unresolved |
| image-db | openssl 1:3.5.8-1.0.1.el9_8 | [ELSA-2026-50075](https://linux.oracle.com/errata/ELSA-2026-50075.html) | High | unresolved |
| image-db | openssl-libs 1:3.5.8-1.0.1.el9_8 | [ELSA-2026-50075](https://linux.oracle.com/errata/ELSA-2026-50075.html) | High | unresolved |
| image-db | gnutls 3.8.10-8.el9_8 | [ELSA-2024-12336](https://linux.oracle.com/errata/ELSA-2024-12336.html) | Medium | unresolved |
| image-db | openssl 1:3.5.8-1.0.1.el9_8 | [ELSA-2025-28011](https://linux.oracle.com/errata/ELSA-2025-28011.html) | Medium | unresolved |
| image-db | openssl-libs 1:3.5.8-1.0.1.el9_8 | [ELSA-2025-28011](https://linux.oracle.com/errata/ELSA-2025-28011.html) | Medium | unresolved |
| image-db | gnutls 3.8.10-8.el9_8 | [ELSA-2025-20606](https://linux.oracle.com/errata/ELSA-2025-20606.html) | Medium | unresolved |
| image-db | gnutls 3.8.10-8.el9_8 | [ELSA-2026-50346](https://linux.oracle.com/errata/ELSA-2026-50346.html) | High | unresolved |
| image-db | openssl 1:3.5.8-1.0.1.el9_8 | [ELSA-2026-50379](https://linux.oracle.com/errata/ELSA-2026-50379.html) | Medium | unresolved |
| image-db | openssl-libs 1:3.5.8-1.0.1.el9_8 | [ELSA-2026-50379](https://linux.oracle.com/errata/ELSA-2026-50379.html) | Medium | unresolved |
| image-db | openssl 1:3.5.8-1.0.1.el9_8 | [ELSA-2026-50345](https://linux.oracle.com/errata/ELSA-2026-50345.html) | Medium | unresolved |
| image-db | openssl-libs 1:3.5.8-1.0.1.el9_8 | [ELSA-2026-50345](https://linux.oracle.com/errata/ELSA-2026-50345.html) | Medium | unresolved |
| image-db | gnutls 3.8.10-8.el9_8 | [ELSA-2024-12364](https://linux.oracle.com/errata/ELSA-2024-12364.html) | Medium | unresolved |
| image-db | gnutls 3.8.10-8.el9_8 | [ELSA-2026-50149](https://linux.oracle.com/errata/ELSA-2026-50149.html) | Medium | unresolved |
| image-db | libgcrypt 1.10.0-13.el9_8 | [ELSA-2026-500145](https://linux.oracle.com/errata/ELSA-2026-500145.html) | Medium | unresolved |
| image-web | zlib 1.3.2-r0 | [CVE-2026-85091](https://nvd.nist.gov/vuln/detail/CVE-2026-85091) | High | unresolved |
| image-web | busybox 1.37.0-r31 | [CVE-2025-60876](https://nvd.nist.gov/vuln/detail/CVE-2025-60876) | Medium | unresolved |
| image-web | busybox-binsh 1.37.0-r31 | [CVE-2025-60876](https://nvd.nist.gov/vuln/detail/CVE-2025-60876) | Medium | unresolved |
| image-web | ssl_client 1.37.0-r31 | [CVE-2025-60876](https://nvd.nist.gov/vuln/detail/CVE-2025-60876) | Medium | unresolved |

## Scanned image IDs

- `image-app.json`: `sha256:f52c81d8ddaf1500c73ddd97d8cba83264cde98ba9dce2e47be9a0f4e1e88dbe`
- `image-db.json`: `sha256:e7adccb96d42e94827d7a74e7f5785adf6a4bda686e9606935af7f5af1fa18b2`
- `image-web.json`: `sha256:6809047284838a1dd013ba4abfab57ac4b9b510710e0f3c5ab3deb1173d478e0`
