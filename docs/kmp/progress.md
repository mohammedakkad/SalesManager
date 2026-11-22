# KMP migration progress

Single source of truth. Each task: checkbox, commit hash, evidence line. Split tasks are noted here.
Branch: `feat/kmp-android-desktop` (merged with `production` @ 44229d1, PRs #52/#53 included).

## Notes / deviations

- `baseline.md` supersedes the figures in section 0 of the master prompt (unit-test floor: 1,192).
- K0.1 commit: `328e3b7` (to be backfilled in the K0.2 commit).

## Tasks


### K0 — Baseline and spike (no production code changes)
- [x] K0.1 Record the baseline: test counts per module, coverage, lint, build times, APK size. Save to `docs/kmp/baseline.md`.  
  - commit: the commit titled "docs(kmp): K0.1 baseline (re-measured on merged production)" (a commit cannot contain its own hash; measured on code `44229d1`) · evidence: 1,192 unit tests from XML (app 292, salesmanager 884, admin 16), 0 failures/skipped; lint 0 errors (29/22/23 warnings); line coverage 90.31%; debug APK 59,851,221 B, release R8 27,616,762 B; cold assemble 93 s; toolchain matches prompt. Seller QA #90 on PR #53 head failed on API 24 (cause not verified); #88 green.
- [ ] K0.2 Generate the dependency inventory: every library mapped to KMP-ready / JVM-only / Android-only, with version and evidence link. Save to `docs/kmp/dependency-matrix.md`.  
  - commit: _pending_ · evidence: _pending_
- [ ] K0.3 Generate the Android-coupling inventory per file (`android.*`, Firebase, Gson, WorkManager, PDF, CameraX), grouped by module and layer. Save to `docs/kmp/coupling-inventory.md`.  
  - commit: _pending_ · evidence: _pending_
- [ ] K0.4 Spike in a throwaway branch: an empty KMP library + `desktopApp` showing one Arabic RTL Compose screen with the bundled IBM Plex Sans Arabic font on Windows. Record the Compose Multiplatform, Kotlin and AGP versions that work. Do not merge the spike; write ADR-0001 (module layout and plugins).  
  - commit: _pending_ · evidence: _pending_
- [ ] K0.5 Write `docs/kmp/architecture.md` (the target diagram above), `docs/kmp/platform-matrix.md` and `docs/kmp/adr/README.md`.  
  - commit: _pending_ · evidence: _pending_
- [ ] K0.6 Arabic report to the owner, plus the list of decisions needed: K1.3, K5 charts, K6.1 PDF.  
  - commit: _pending_ · evidence: _pending_

### K1 — Build foundation
- [ ] K1.1 Add Compose Multiplatform, KMP and Room KMP versions to the catalog (no usage yet). Build green.  
  - commit: _pending_ · evidence: _pending_
- [ ] K1.2 Create an empty `:shared:core-model` KMP module (android + desktop targets, `jvmShared` hierarchy). CI compiles it.  
  - commit: _pending_ · evidence: _pending_
- [ ] K1.3 **Decision (stop and ask):** the desktop Firebase strategy. Evaluate with a spike:  
  - commit: _pending_ · evidence: _pending_
- [ ] K1.4 Add a `desktopApp` module with `main()`, an empty window titled "مدير المبيعات", RTL, the app icon, and Arabic font resources. `./gradlew :desktopApp:run` works.  
  - commit: _pending_ · evidence: _pending_
- [ ] K1.5 CI: add a `windows-latest` job that compiles desktop and runs desktop tests (no packaging yet). Linux jobs unchanged.  
  - commit: _pending_ · evidence: _pending_
- [ ] K1.6 Docs: `docs/kmp/developer-setup.md` (JDK, running desktop and Android, IDE plugin).  
  - commit: _pending_ · evidence: _pending_

- [ ] K1.7 **Proposed, needs owner approval:** make `:admin` debug buildable (today `:admin:assembleDebug` fails in `processDebugGoogleServices`: no client for `com.trader.admin.debug`). Options: (a) add a `com.trader.admin.debug` Android app to the Firebase project and update `admin/google-services.json` (owner action, touches Firebase config); (b) drop `applicationIdSuffix = ".debug"` in `admin/build.gradle.kts:32` (changes the debug applicationId, so it no longer installs beside release). Not started.  
  - commit: _pending_ · evidence: _pending_

### K2 — Domain models to commonMain (smallest pieces first)
- [ ] K2.1 Move plain enums and value types (`PaymentType`, `UnitType`, `WeightUnit`, `SyncStatus`, `StockLevel`, ...) one file per commit. Keep the old package names (no import churn) unless the ADR says otherwise.  
  - commit: _pending_ · evidence: _pending_
- [ ] K2.2 Move data classes without platform types (`Customer`, `Transaction`, `Product`, `ProductUnit`, `InvoiceItem`, ...) in batches of at most 5 files. Move their pure unit tests to `commonTest`.  
  - commit: _pending_ · evidence: _pending_
- [ ] K2.3 `Merchant` and `ChatMessage`: replace Firebase `Timestamp` in the **domain** with epoch millis (or kotlinx-datetime `Instant`). Convert at the Firebase mapping layer only.  
  - commit: _pending_ · evidence: _pending_
- [ ] K2.4 Move the pure parts of `AppNumberFormat` to commonMain (digit mapping, parse/normalise, isolates).  
  - commit: _pending_ · evidence: _pending_
- [ ] K2.5 Move `MerchantContext` and the repository interfaces to `:shared:core-domain`. Android keeps its implementations for now.  
  - commit: _pending_ · evidence: _pending_

### K3 — Data layer
- [ ] K3.1 Room KMP on Android first: add the KMP Room setup and keep the **same schema JSON** (diff must be empty). Run all migration tests. **Stop and ask** if the schema hash would change.  
  - commit: _pending_ · evidence: _pending_
- [ ] K3.2 Move entities, then DAOs (in batches), to `:shared:core-data` commonMain. Room tests run in `jvmShared` tests (in-memory, bundled SQLite driver).  
  - commit: _pending_ · evidence: _pending_
- [ ] K3.3 Create the desktop database: file path in `%APPDATA%\SalesManager\salesmanager.db`, created on first run. Test that it opens, migrates and closes.  
  - commit: _pending_ · evidence: _pending_
- [ ] K3.4 DataStore: KMP DataStore with platform file paths. Migrate the `appDataStore` provider, and keep the Android file name so existing users keep their settings.  
  - commit: _pending_ · evidence: _pending_
- [ ] K3.5 Move repository implementations that only touch Room/DataStore to commonMain, one repository per commit. Keep the merchant-isolation tests green and move them too.  
  - commit: _pending_ · evidence: _pending_
- [ ] K3.6 Crypto/backup: move it to `jvmShared` (javax.crypto + BouncyCastle). The archive format must stay byte-compatible: an Android backup must restore on desktop and vice versa (test).  
  - commit: _pending_ · evidence: _pending_

### K4 — Remote layer (after K1.3 approval)
- [ ] K4.1 Define the remote interfaces in commonMain (activation, merchant status, sync, chat, products, outbox transport) from the current classes, signatures only.  
  - commit: _pending_ · evidence: _pending_
- [ ] K4.2 Move the Android Firebase implementations to `:shared:core-remote` androidMain, unchanged.  
  - commit: _pending_ · evidence: _pending_
- [ ] K4.3 Desktop implementation according to ADR-0002, one service per task:  
  - commit: _pending_ · evidence: _pending_
- [ ] K4.4 Merchant isolation on desktop: port `MerchantIsolationTest` so it runs for both targets.  
  - commit: _pending_ · evidence: _pending_
- [ ] K4.5 Desktop background work: a scheduler service that replaces WorkManager jobs while the app runs (outbox dispatch, status checks, unpaid-debt reminder). One job per commit, with tests.  
  - commit: _pending_ · evidence: _pending_

### K5 — Shared presentation
- [ ] K5.1 Move to commonMain:  
  - commit: _pending_ · evidence: _pending_
- [ ] K5.2 Theme, colors, typography: move them to `:shared:seller-ui` and load the font with Compose resources. Keep `AppLocale`/RTL behaviour, and keep `LocalLayoutDirection` RTL on both targets.  
  - commit: _pending_ · evidence: _pending_
- [ ] K5.3 Strings: move `strings.xml` entries used by moved screens to Compose Multiplatform resources. The 1,106 hard-coded literals stay as they are (out of scope), unless a moved file needs them.  
  - commit: _pending_ · evidence: _pending_
- [ ] K5.4 Navigation: move the navigation graph to multiplatform navigation. Android deep links and pending navigation keep working.  
  - commit: _pending_ · evidence: _pending_
- [ ] K5.5 Charts: decide (ADR-0003) between a multiplatform Vico version and a shared Canvas chart. The chart pixel test must keep passing on Android.  
  - commit: _pending_ · evidence: _pending_

### K6 — PDF on desktop
- [ ] K6.1 **Decision spike (ADR-0004):** how the desktop renders Arabic PDF with correct shaping and bidi. Evaluate:  
  - commit: _pending_ · evidence: _pending_
- [ ] K6.2 Move the PDF layout engine (design tokens, components, pagination, layout log) to `:shared:core-pdf` commonMain behind `PdfPageSink`.  
  - commit: _pending_ · evidence: _pending_
- [ ] K6.3 Desktop sink and save dialog ("حفظ PDF" to Documents, then open the folder).  
  - commit: _pending_ · evidence: _pending_
- [ ] K6.4 Run all PDF tests (statement > 25 rows, invoice, monthly report) on both targets. Compare Android and desktop page counts and layout logs; they must be identical.  
  - commit: _pending_ · evidence: _pending_

### K7 — Screens to commonMain (one screen per task, smallest first)
- [ ] move it with its components, without changing behaviour;  
  - commit: _pending_ · evidence: _pending_
- [ ] Android: its UI tests stay green (they still run on Android);  
  - commit: _pending_ · evidence: _pending_
- [ ] desktop: one smoke test (the screen opens, RTL, no crash);  
  - commit: _pending_ · evidence: _pending_
- [ ] keep its accessibility labels and its recomposition guard if it has one.  
  - commit: _pending_ · evidence: _pending_

### K8 — Desktop app experience
- [ ] K8.1 Window: minimum size, remembered size and position, title, icon. Use the existing `WideScreenContainer` for wide windows.  
  - commit: _pending_ · evidence: _pending_
- [ ] K8.2 Keyboard: shortcuts for the main actions; Tab order; Enter submits forms; a USB barcode scanner fills the active product search.  
  - commit: _pending_ · evidence: _pending_
- [ ] K8.3 System tray and notifications (chat messages, sync failures, low stock) while the app runs.  
  - commit: _pending_ · evidence: _pending_
- [ ] K8.4 Single instance (a second launch focuses the window). Graceful shutdown flushes the outbox.  
  - commit: _pending_ · evidence: _pending_
- [ ] K8.5 Logging to `%APPDATA%\SalesManager\logs` with rotation. No secrets or personal data in logs (test).  
  - commit: _pending_ · evidence: _pending_

### K9 — Windows packaging and release
- [ ] K9.1 Compose Desktop native distribution: MSI (x64), bundled runtime, Arabic app name, icon, version from the catalog.  
  - commit: _pending_ · evidence: _pending_
- [ ] K9.2 CI on `windows-latest`: build the MSI as an artifact on every PR, and install it silently plus launch a smoke test.  
  - commit: _pending_ · evidence: _pending_
- [ ] K9.3 Release workflow: attach the MSI to a GitHub Release on tag `desktop-v*` (unsigned).  
  - commit: _pending_ · evidence: _pending_
- [ ] K9.4 Desktop update check: compare the latest GitHub Release, notify, and open the download page.  
  - commit: _pending_ · evidence: _pending_

### K10 — Admin app (same pattern, after the seller is complete)
- [ ] K10.1 Plan the admin moves by reusing K2–K9; this task only produces the task list in `progress.md`.  
  - commit: _pending_ · evidence: _pending_
- [ ] K10.x Execute the plan one screen per task.  
  - commit: _pending_ · evidence: _pending_

### K11 — Final verification
- [ ] K11.1 Full Android regression: all JVM tests, device CI API 24–36, R8 smoke, coverage gates, lint 0 errors.  
  - commit: _pending_ · evidence: _pending_
- [ ] K11.2 Desktop: all desktop tests on Windows CI; MSI install, launch and uninstall smoke test.  
  - commit: _pending_ · evidence: _pending_
- [ ] K11.3 Cross-platform data test: the same merchant on Android and desktop. Sales from both appear on the other side, with no duplicates.  
  - commit: _pending_ · evidence: _pending_
- [ ] K11.4 Final docs and Arabic report (template in section 7).  
  - commit: _pending_ · evidence: _pending_
