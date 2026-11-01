# Seller release readiness — full acceptance FAIL

Continued on existing `codex/seller-full-coverage-audit` from `cea574c4049b5f989fc9e88bc07a39cd4a130eaa`; Stage6 committed/pushed as5efe40a, Stage7 follows it. Fresh production base remains `22381d5b55fb59a9270932bcf720b283fa1c90ff`. No branch restart, admin-module change, production-data access, force-push, merge or deployment. Do not approve a production release from these results.

| Check | Status | Measured evidence / limits |
| --- | --- | --- |
| JVM regression suite | PASS | 100 passed,0 failed,0 skipped,19 classes;25 added this continuation; original30 audit tests and all75 starting tests retained |
| 100% coverage target | FAIL | Weighted13.1653% lines /4.3662% branches; method15.9734%,class14.6789%;3490/26509 lines,704/16124 branches |
| Module coverage | Verified | Shared32.0230% lines/18.7224% branches; Seller3.1272%/1.7455%; no production exclusions |
| Reviewed regression floors | PASS | Shared31.9% lines/18.5% branches; Seller3.0%/1.7%; all raised after measurements |
| Room22→23 upgrade | PASS canonical fixtures | Authentic exported22 schema; state backfill, records/balances/invoices/stock,FK checks,sequence and rollback |
| Historical upgrades1–21→23 | BLOCKED | Authentic schemas/databases unavailable; per-version acquisition matrix in stage5-blockers.md; no invented schemas |
| Return persistence / retries | PASS tested paths | Durable close/reopen, stable-ID idempotency, pending/conflict protection, timeout/cancellation and fractional return regressions retained |
| Inventory integrity | PASS tested local paths | Atomic session/snapshot/finalization, durable duplicate guard, local stock rollback, history/count/merchant validation; stock collaborator controlled |
| Customer sync | PASS tested paths | Remote snapshots preserve PENDING/FAILED/CONFLICT edits; synced edits retain debt history/visitor; logout cancels listener |
| Cash transfer / retry | PASS tested local paths | Real SQLite abort rolls back both balances/ledger; valid transfer conservation, initialization/correction history,FAILED retries,offline retention,batch isolation,cancellation and merchant listener replacement |
| Whole-sale / distributed atomicity | NOT TESTED comprehensively | Cash sub-operation is transactional; sale/stock/debt/invoice/cash aggregate and independent remote writes remain unproven |
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
