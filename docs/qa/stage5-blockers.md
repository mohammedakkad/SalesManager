# Stage5 migration acquisition and signing-owner actions

| Starting version →23 | Status | Missing prerequisite |
| --- | --- | --- |
| 1→23 | BLOCKED | Authentic exported schema1 or sanitized database with verified version/identity |
| 2→23 | BLOCKED | Authentic exported schema2 or sanitized database with verified version/identity |
| 3→23 | BLOCKED | Authentic exported schema3 or sanitized database with verified version/identity |
| 4→23 | BLOCKED | Authentic exported schema4 or sanitized database with verified version/identity |
| 5→23 | BLOCKED | Authentic exported schema5 or sanitized database with verified version/identity |
| 6→23 | BLOCKED | Authentic exported schema6 or sanitized database with verified version/identity |
| 7→23 | BLOCKED | Authentic exported schema7 or sanitized database with verified version/identity |
| 8→23 | BLOCKED | Authentic exported schema8 or sanitized database with verified version/identity |
| 9→23 | BLOCKED | Authentic exported schema9 or sanitized database with verified version/identity |
| 10→23 | BLOCKED | Authentic exported schema10 or sanitized database with verified version/identity |
| 11→23 | BLOCKED | Authentic exported schema11 or sanitized database with verified version/identity |
| 12→23 | BLOCKED | Authentic exported schema12 or sanitized database with verified version/identity |
| 13→23 | BLOCKED | Authentic exported schema13 or sanitized database with verified version/identity |
| 14→23 | BLOCKED | Authentic exported schema14 or sanitized database with verified version/identity |
| 15→23 | BLOCKED | Authentic exported schema15 or sanitized database with verified version/identity |
| 16→23 | BLOCKED | Authentic exported schema16 or sanitized database with verified version/identity |
| 17→23 | BLOCKED | Authentic exported schema17 or sanitized database with verified version/identity |
| 18→23 | BLOCKED | Authentic exported schema18 or sanitized database with verified version/identity |
| 19→23 | BLOCKED | Authentic exported schema19 or sanitized database with verified version/identity |
| 20→23 | BLOCKED | Authentic exported schema20 or sanitized database with verified version/identity |
| 21→23 | BLOCKED | Authentic exported schema21 or sanitized database with verified version/identity |
| 22→23 | PASS canonical fixtures | Existing real compiler export; historical upgrade-produced variants remain unverified |

Recover older release commits/artifacts from the owner; export with the exact historical Room/compiler versions in an isolated checkout. Hash schemas and record commit/toolchain provenance. Obtain anonymized representative databases offline, verify user_version/identity hash and foreign keys, then test every supported chain and rollback without destructive fallback. Do not reverse-engineer migrations into invented golden schemas or upload live merchant data.

Signing owner: assess who could read the historical export; identify signing versus upload-key roles and Play App Signing enrollment; rotate/reset only through the approved owner/provider procedure; verify update continuity in an isolated release track; revoke obsolete access; remove history copies through an owner-approved coordinated process; inspect releases/caches/forks; verify secret scanning and branch protection. No key rotation or history rewrite was performed by this audit.

No KVM/device is configured locally. An emulator would require an SDK emulator/system image and software rendering or hardware acceleration; do not overwrite the tracked Firebase config. The existing disposable CI emulator job uses the offline fixture and safety runner. Its execution and real PDF output remain unverified. API access to existing PR51 still returns Forbidden; never create a duplicate PR.

## Sync protocol matrix

| Requested scenario | Evidence/status |
| --- | --- |
| Offline creation → reconnect | NOT TESTED end-to-end; durable return reconnect checks retained |
| Failed upload → retry | PASS transaction PENDING/FAILED selection; CONFLICT is not auto-retried |
| Interrupted upload | PASS cancellation propagation without failure/success acknowledgement in retry path |
| Duplicate/out-of-order remote events | NOT TESTED comprehensively |
| Simultaneous devices | NOT TESTED; no conflict policy invented |
| Conflicting customer/transaction updates | Partial only; broader overwrite/ack races remain unverified |
| Remote deletion/tombstones | NOT TESTED protocol; stale SYNCED deletion risk remains |
| Logout during in-flight upload | NOT TESTED external write; transaction listener cancellation PASS |
| Merchant switching | Prior return guard tests retained; whole-app switching NOT TESTED |
| Partial batch failure | PASS failed upload does not prevent next pending transaction |
| Idempotent return | Prior return-ID regressions retained |

Transaction insert/update/delete still span DAO/cash/stock operations without one demonstrated shared transaction. Individual DAO transactions do not prove aggregate atomicity. Stage5 does not fix or certify that architecture; failure-injection integration tests and a reviewed local outbox/transaction boundary remain required. Existing rollback tests cover return processing through controlled collaborators only.
