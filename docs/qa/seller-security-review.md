# Seller security review

Status: partial source review; no penetration test or vulnerability advisory scan completed.

## Verified
- `keystore_base64.txt` is tracked, base64 decodes to a 2752-byte ASN.1 container consistent with PKCS#12 signing material. Contents/passwords were not printed, used or copied into reports. Owner must assess exposure, rotate the signing material where feasible, and plan repository-history remediation. Deleting the file now would not revoke prior exposure; no history rewriting was performed.
- SalesManagerApp logs FCM registration tokens at debug level. Remove sensitive token logging with a regression/static rule in a subsequent change; do not assume release logging is stripped.
- MainActivity is exported as the launcher. Messaging service, update receiver and FileProvider are nonexported. FileProvider grants URI permissions but exposes whole cache/external-files roots; narrow paths after testing export/update journeys.
- OS backup excludes device.xml only; data-extraction policy allows other app data. Confirm merchant privacy requirements and activation/session behavior after restore.
- Backups use AES-GCM, random salt/IV and PBKDF2-HMAC-SHA256 (100,000 iterations); actual encryption/tamper/password tests added. The version-1 cleartext schema/timestamp header is not authenticated as AAD. Payload schema is separately parsed, but tamper guarantees must be documented before changing the format.
- Downloaded APK installation delegates signing checks to Android. Downloader lacks explicit trusted-host/checksum policy and finally-based cleanup. Transport/failure/cancellation tests remain needed.
- Existing release workflow interpolates secrets into shell commands and signs on all branch pushes. New QA workflow uses no production secrets and cannot deploy or publish releases.

## Blocked / not tested
Firebase rules and authorization assumptions were not available for review; no live Firebase data/activation requests or merchant-account tests were made. Gradle attempted a Crashlytics mapping upload, which was denied with HTTP403; QA commands now exclude that task. No successful production write was observed. Runtime RBAC/activation bypass, FileProvider grants, backup exposure and signed upgrade security were not device-tested. GitHub API request returned Forbidden; no advisory scan/CI result was obtained. No claim of zero known vulnerabilities.

New QA XML/HTML reports include all production compiled class directories. `jdk.internal.*` is excluded only from JaCoCo's JVM instrumentation to avoid instrumenting JDK internals, not from the production report. No business-code coverage exclusions were added.
