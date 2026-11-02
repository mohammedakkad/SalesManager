# Seller release readiness — full acceptance FAIL

Stage8 continues on existing `codex/seller-full-coverage-audit` from `a693da48b9c1a4114ac4508763710f4b24777542`. Original production base remains `22381d5b55fb59a9270932bcf720b283fa1c90ff`. Stage8 is **partial**, not accepted: whole-sale atomicity and a durable outbox remain unimplemented. No production data, Admin module, production branch, signing key or Git history was altered.

| Check | Status | Measured evidence / limits |
| --- | --- | --- |
| JVM regression suite | PASS | 104 passed,0 failed,0 skipped,20 classes;4 added in Stage8; all100 starting tests retained (retry routing assertions now check exact snapshot completion calls) |
| 100% coverage target | FAIL | Weighted13.2840% lines /4.4333% branches; method16.1678%,class14.8242%;3522/26513 lines,715/16128 branches |
| Module coverage | Verified | Shared32.3565% lines/19.1336% branches; Seller3.1272%/1.7455%; no production exclusions |
| Reviewed regression floors | PASS | Shared32.1% lines/18.9% branches; Seller3.0%/1.7%; all raised after measurements |
| Room22→23 upgrade | PASS canonical fixtures | Authentic exported22 schema; state backfill, records/balances/invoices/stock,FK checks,sequence and rollback |
| Historical upgrades1–21→23 | BLOCKED | Authentic schemas/databases unavailable; per-version acquisition matrix in stage5-blockers.md; no invented schemas |
| Return persistence / retries | PASS tested paths | Durable close/reopen, stable-ID idempotency, pending/conflict protection, timeout/cancellation and fractional return regressions retained |
| Inventory integrity | PASS tested local paths | Atomic session/snapshot/finalization, durable duplicate guard, local stock rollback, history/count/merchant validation; stock collaborator controlled |
| Customer sync | PASS tested paths | Remote snapshots preserve PENDING/FAILED/CONFLICT edits; synced edits retain debt history/visitor; logout cancels listener |
| Cash transfer / retry | PASS tested local paths | Real SQLite abort rolls back both balances/ledger; valid transfer conservation, initialization/correction history,FAILED retries,offline retention,batch isolation,cancellation and merchant listener replacement |
| Whole-sale / distributed atomicity | FAIL acceptance / NOT TESTED comprehensively | Cash sub-operation is transactional; sale/stock/debt/invoice/cash aggregate and independent remote writes remain unproven |
| Backup/restore | PASS tested cases | Encrypted round trip,orphan rollback,corruption,legacy return recovery and export snapshot boundary; large/interrupted/concurrent/minified workflows incomplete |
| Android instrumentation | BLOCKED execution / PASS compile | Four tests package successfully; adb finds no device,/dev/kvm absent; prior connectedDebugAndroidTest failed No connected devices |
| Native PDF content | BLOCKED | Two host verifier tests require real Android-generated PDFs; verifier compilation previously passed,execution not claimed |
| Android Lint | PASS errors / warnings remain | 0 errors,24 warnings,0 information;32→24 warnings this turn; four incompatible dependency registries skipped,explicit triage,no suppression |
| Debug APK | PASS | Assembly and debug apksigner verification; hash/size/application ID in evidence/apk-manifest.json |
| Minified unsigned release APK | PASS build | Actual R8/resource shrinking in stage7-verified.log; valid output reused in successful final aggregate; unsigned status confirmed |
| Signed/minified device runtime | BLOCKED / NOT TESTED | Approved uncompromised signing setup and device required; no installation/distribution or runtime pass claimed |
| Signing material policy | PASS current index / rotation BLOCKED | Export untracked/ignored and retained locally; history exposure requires owner assessment/rotation/remediation; contents never displayed |
| Firebase emulator / multiple devices / rules | NOT TESTED | Deterministic mocked protocol tests only; no production merchant requests |
| Device profiling / full security/advisory scan | NOT TESTED / BLOCKED external access | No device timing/heap/ANR figures or vulnerability-free claim |
| GitHub Actions | BLOCKED live verification | Workflow structure/local equivalent commands reviewed; API403 prevents observing remote runs; no CI success claimed |
| Existing PR51 | BLOCKED live state/update | User-provided Draft PR https://github.com/mohammedakkad/SalesManager/pull/51; no duplicate PR or merge; gh pr view51 returns Forbidden |
| Reusable setup draft | BLOCKED save / PASS local repeat | Service says draft uneditable; tested complete installer/start instructions preserved in environment-setup/,not confirmed saved/published |

## Evidence and reproduction

Stage7 before evidence: evidence/continuation/stage7/stage7-cash-before.log and cash-before.xml (5failures/8tests). Initial after:8/8 pass. Expanded/final suite:100/100. Actual R8 aggregate:stage7-verified.log **BUILD SUCCESSFUL in6m57s**. Final exact-source aggregate:stage7-final.log **BUILD SUCCESSFUL in3m45s,227 tasks (54executed,173valid up-to-date)**. The final run rechecks the last transaction boundary and removes the empty obsolete drawable folder; it does not claim R8 ran twice. Current JUnit:evidence/tests/. Actual original XML compressed with hashes:evidence/coverage/. Exact uncovered classes,source lines and branch counts:seller-class-coverage-gaps.csv and seller-coverage-gaps.csv. APK metadata/signatures:evidence/apk-manifest.json. Stage-by-stage files/bugs/counts:seller-audit-continuation.md.

```bash
cd /workspace/SalesManager
/workspace/setup/continuation/gradle --continue \
  :app:qaCoverage :salesmanager:qaCoverage :salesmanager:lintDebug \
  :salesmanager:assembleDebug :salesmanager:assembleDebugAndroidTest :salesmanager:assembleRelease \
  -x :salesmanager:uploadCrashlyticsMappingFileRelease
python3 scripts/report-seller-coverage.py --check
python3 scripts/check-seller-secrets.py
```

Full JDK17,SDK35/build-tools34 and checksum-verified Gradle8.9 are required. The tested helper preserves proxy/CA trust,uses four workers and avoids unrelated admin signing configuration. Complete transferable setup:environment-setup/install.sh and start.md. No Crashlytics mapping upload or production key use. Existing client config is for packaging only. Use the offline fixture generator only in a disposable checkout; OfflineQaRunner refuses real Firebase projects before Application/provider initialization. See qa/pdf-verifier/README.md for actual device/PDF commands. Compiled tests are not executed tests.

## Remaining release blockers and next priorities

1. Reproduce aggregate failure paths in TransactionRepository insert/update/delete across sale,cash,stock,invoice and debt,then implement a reviewed shared local transaction/outbox boundary. StockRepositoryImpl launches remote writes independently; inventory rollback tests use a controlled transaction-aware collaborator and do not prove remote rollback. Stale upload acknowledgements,concurrent restore/sync,tombstones,merchant switching and same-unit/multiple-price returns need further regression coverage and explicit server policy. No complete conflict resolver or zero remaining P0 guarantee is claimed.
2. Obtain authentic schemas/sanitized databases1–21 with provenance/version/identity hashes and test every supported chain; historical records missing from durable ledgers cannot be reconstructed from balances alone. Follow stage5-blockers.md; never use destructive fallback.
3. Signing owner must assess/rotate/reset exposed material through approved procedures and verify update continuity. Current-index removal does not revoke historical copies. Firebase API restrictions/rules and dependency advisory review remain unverified.
4. Execute4 instrumentation tests and2 host PDF content tests in the isolated CI emulator/device workflow. Then verify signed/minified upgrades,backup round trips,print/share,RTL/accessibility/restoration and profiling. Large/malformed JSON and ZIP expansion limits remain incomplete.
5. Close exact coverage gaps meaningfully;100 tests are not100% coverage. Align dependency lint registries with a validated toolchain,review Context fields on device and address launcher design warnings. No coverage/quality gate was weakened.

## PR and setup access blockers

`gh pr view 51 --json url,state,isDraft,baseRefName,headRefName,statusCheckRollup` returned `Post "https://api.github.com/graphql": Forbidden`. Native Git fetch/push works; no GitHub connector is available. Allow authorized api.github.com access through environment networking/proxy settings,then inspect PR51 and use `gh pr edit 51 --body-file docs/qa/draft-pr-body.md`; preserve Draft/unmerged state. Do not ask for or paste token contents. Exact comparison URL: https://github.com/mohammedakkad/SalesManager/compare/production...codex/seller-full-coverage-audit .

The setup service rejected install_script/start_skill persistence: "This draft cannot be edited in its current state. It may already have been published, or a publication operation may still be in progress." A reread remained revision3. Proposed tested files are preserved in environment-setup/ for transfer to a new setup chat from environment settings; no save/publication was confirmed and no existing allowlist/secret binding was replaced.

## Stage8 partial increment
Four real-Room tests added: stale retry success/failure cannot overwrite the status of a newer edit; unchanged success/failure still acknowledge/remain retryable. Before:2/2failed; after:104 JVM tests pass. Reconnect completion compares the full uploaded snapshot and updates status within one Room transaction. Existing routing assertions now verify the uploaded snapshot ID/status and success/failure flag; cancellation/conflict selection assertions are retained. Initial async insert/update uploads, account switching, identical-content ABA and deletions are outside this fix.

The full aggregate (same commands above) passed in7m33s,227 tasks (64executed,163up-to-date), including actual R8. Lint remains0errors/24warnings. Coverage gates raised after measurement to shared32.1%/18.9%; Seller3.0%/1.7% unchanged. No exclusions or assertion weakening. Stage8 evidence is evidence/continuation/stage8/; current original JaCoCo XML/JUnit/APK hashes remain in evidence/.

CI recovery: both jobs replace setup-android@v3 with a SHA256-pinned isolated SDK installer that retains stderr, checks license-command status and asserts required tools/packages. Fresh JDK17 install and repeat pass locally. Forced resource linking printed /workspace/setup/stage8-sdk and passed; Debug assembly uses that SDK (incremental). This is not a claim of clean hosted compilation. The exact hosted SDK failure preceding stderr remains inaccessible; user-reported run41275351544 is not reclassified as passing. A legacy tools probe with the new CLI printed missing-package text but returned0, so it is not the hosted16.0 failure reproduction. See stage8-atomicity-and-outbox.md.

Whole-sale atomicity and transactional outbox: **NOT IMPLEMENTED; acceptance FAIL**, not externally blocked. The actual ViewModel/repository write graph, ten-boundary test matrix, durable deletion/revision/tenant requirements and remaining implementation are explicitly recorded in stage8-atomicity-and-outbox.md. Versions1–21 remain BLOCKED in stage8-migration-matrix.md. Device/PDF execution remains BLOCKED (no device/KVM; hosted API denied). Full Stage8 request and release acceptance are incomplete. No exactly-once remote guarantee is claimed.


## Stage 9 continuation

See [Stage 9 partial evidence](stage9-progress.md). PDF export mechanics are updated but device/hosted success is unverified. Atomic sale commands, transactional outbox and next-schema migration remain unimplemented. Release readiness remains **FAIL**; earlier results are not new validation.


## Stage 10 implementation

[Atomic Seller commands and evidence](stage10-atomic-sales.md) supersede the earlier statement that Seller sale commands are unimplemented. Creation/edit/deletion screen paths now use one Room aggregate transaction, with144 passing JVM tests and unchanged passing coverage gates. Distributed sync/outbox guarantees remain Stage11 work; overall release readiness remains FAIL.


## Stage 11 current evidence

Seller sale create/edit/delete atomicity and durable transactional outbox are now IMPLEMENTED and locally regression-tested, superseding earlier unimplemented statements. Room24→25 preservation/rollback and backup22 recovery pass;202 JVM tests and unchanged coverage gates pass. Actual conditional Firebase transport and WorkManager worker are wired, but server-runtime/rules enforcement and cross-backend reconciliation remain unverified. Hosted jobs for this HEAD are BLOCKED by GitHub API Forbidden; native instrumentation/PDF runtime was not repeated here. Current readiness remains **FAIL**. The Stage11 report contains the authoritative current counters, packaging results, protocol limitations and blocker list.

[Implementation, test matrix and evidence](stage11-outbox.md).

Final lint result after the version-catalog repair: **0 errors,24 warnings**. See evidence/continuation/stage11/final-lint-summary.json; incompatible dependency lint registries remain among the warnings.
