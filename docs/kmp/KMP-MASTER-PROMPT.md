# SalesManager → Kotlin Multiplatform (Android + Windows Desktop) — Master Prompt

> For Claude Code (Claude Sonnet 5.5). Paste **one phase at a time** (see "How to run this prompt").
> Reports to the owner: detailed **Arabic**. Code, commits, PR titles, docs under `docs/kmp/`: **English**,
> except the user-facing desktop guide, which is Arabic.

---

## 0. Context (measured on 2026-11-22, verify before relying on it)

- Repo `mohammedakkad/SalesManager`. Modules: `:app` (shared core, package `com.trader.core`: domain,
  Room, DataStore, Firebase, Koin), `:salesmanager` (seller Android app), `:admin` (admin Android app).
  Arabic, RTL, unpublished; updates ship through GitHub Releases.
- Toolchain: Gradle 9.9.2, AGP 9.5.2 (built-in Kotlin), Kotlin 2.4.21, KSP 2.3.14, compileSdk/targetSdk 36,
  minSdk 24, Compose BOM 2026.06.01, Room 2.8.5, Koin 4.2.2, Navigation 2.9.8, Lifecycle 2.10.0,
  DataStore 1.3.0, Coil 3.5.0, Vico 1.15.0, OkHttp 5.4.0, Gson, WorkManager 2.12.0, Firebase BoM 35.2.0,
  CameraX + ML Kit barcode, BouncyCastle.
- 277 Kotlin source files. Android coupling:
  - 66 files import `android.*`;
  - 26 files use `com.google.firebase`, including **domain models** (`Merchant`, `ChatMessage` use Firebase `Timestamp`);
  - 18 files use `android.graphics.pdf` / `PdfDocument` (the PDF engine; it already has a `PdfPageSink` abstraction);
  - 7 files use WorkManager;
  - 1 file uses CameraX/ML Kit;
  - 4 files use Gson;
  - 1 file uses crypto (javax.crypto/BouncyCastle).
- Existing quality bar (keep it): ~1,190 JVM tests green, coverage gates, lint 0 errors, device CI on API 24–36,
  R8 smoke lane, recomposition guards, accessibility tests, merchant-isolation tests, PDFBox verifier (`qa/pdf-verifier`).
- `AppNumberFormat` (Arabic-Indic/Western digits, bidi isolates) and `AppLocale` (forced Arabic + RTL) already exist.
- **Key simplification:** Android and Desktop are both JVM targets. Code that uses `java.*`, `javax.crypto`,
  OkHttp or Gson can live in a shared **`jvmShared`** intermediate source set (androidMain + desktopMain)
  instead of `expect/actual`.

## 1. Goal and scope

Turn the project into Kotlin Multiplatform so the **seller app runs on Android (unchanged behaviour)
and on Windows desktop**, sharing domain, data, view models and Compose UI.

- **In scope:**
  - targets `android` + `desktop` (JVM, Windows 10/11 x64);
  - seller app first, admin app last;
  - a Windows installer (MSI);
  - documentation.
- **Out of scope:**
  - iOS, Web and Server;
  - publishing to Google Play or the Microsoft Store;
  - key rotation;
  - Room 3;
  - translating to English;
  - redesigning the UI;
  - changing business rules.
- **The Android app must keep shipping:** every commit builds and passes the existing Android test suite.

## 2. Hard rules

1. **Strangler migration, not a rewrite.** Move code into shared modules in small steps. The Android app keeps
   working after every task. No big-bang branch.
2. **Micro-tasks.** Each task changes one concern and as few files as possible (a soft cap of about 10), ends with
   green build + tests, and is **one commit**. If a task grows, split it and note the split in `docs/kmp/progress.md`.
3. **Never delete or weaken tests.** Moving a test to `commonTest` or a `jvmShared` test set is allowed and must be
   counted: the total test count may only go up. If a fixture changes, say why in the commit message.
4. Keep Clean Architecture, MVVM, one class per file, SOLID and DRY. `commonMain` must not import `android.*`,
   `java.*` (except through `jvmShared`), Firebase Android SDK types, or Gson.
5. **Never print, log, rotate or commit secrets. Do not touch git history.** No real `google-services.json` or keystore
   in desktop resources.
6. Verify every claim with evidence: command output, test names, file paths. Write "not verified" when you could not verify something.
7. **Stop and ask the owner before:**
   - changing the Room schema or migrations, the Firebase data shape, financial calculations, or merchant isolation;
   - choosing the desktop Firebase strategy (task K1.3);
   - removing or degrading any Android feature;
   - adding a paid service or a new backend;
   - anything needing key rotation.
8. Pin versions from the version catalog only, and check compatibility with compileSdk 36 and AGP 9.5.
   Never use a dynamic version.
9. After each phase: update docs, then send the owner a detailed **Arabic** report (what changed, evidence, numbers,
   risks, next phase).

## 3. Target architecture (verify with the K0 spike; adjust only via an ADR)

```
:shared:core-model     KMP  commonMain: domain models, value types (no Firebase Timestamp: use epoch millis / kotlinx-datetime)
:shared:core-domain    KMP  commonMain: repository interfaces, use cases, MerchantContext, AppNumberFormat (pure parts)
:shared:core-data      KMP  commonMain: Room KMP DAOs/entities + repository impls; jvmShared: OkHttp/crypto; androidMain/desktopMain: drivers, paths
:shared:core-remote    KMP  commonMain: remote interfaces (sync, chat, activation...); androidMain: Firebase Android SDK; desktopMain: chosen strategy (K1.3)
:shared:core-pdf       KMP  commonMain: layout engine + PdfPageSink API; androidMain: PdfDocument sink; desktopMain: desktop sink (K6)
:shared:seller-ui      KMP  commonMain: Compose Multiplatform screens, view models, navigation, theme, resources
:salesmanager          Android application (thin): Application, MainActivity, FCM, WorkManager, CameraX, Android DI
:desktopApp            JVM application (thin): main(), Window, tray, desktop DI, scheduler, packaging
:admin                 later (K10): same pattern
```

- Library modules use the AGP 9 KMP library plugin. App modules stay thin.
- Verify the AGP 9 rule on combining the KMP plugin with `com.android.application`, and record the decision in ADR-0001.
- Source-set hierarchy: `commonMain` → `jvmShared` → { `androidMain`, `desktopMain` }.

**Platform capability map** (this becomes `docs/kmp/platform-matrix.md`):

| Capability | Android | Desktop (Windows) |
|---|---|---|
| UI | Compose (Jetpack) | Compose Multiplatform Desktop (Skia; Arabic shaping + RTL) |
| DB | Room KMP + Android driver | Room KMP + `BundledSQLiteDriver`, file in `%APPDATA%\SalesManager` |
| Preferences | DataStore | DataStore KMP (okio path) |
| DI | Koin | Koin |
| Firebase Auth/RTDB/Firestore | Firebase Android SDK | **decision K1.3** |
| Push (FCM) | FCM | none; realtime listeners while open + tray notification |
| Background work | WorkManager | coroutine scheduler while the app runs |
| Barcode | CameraX + ML Kit | USB/HID scanner (keyboard input); camera deferred |
| PDF | `PdfDocument` | **decision K6.1** (Arabic shaping is the risk) |
| Charts | Vico 1.15 | **decision K5.x** (Vico KMP vs Canvas chart) |
| Share/print | Intent | open file / folder, system print dialog |
| Crash reporting | Crashlytics | local log file, no remote (ADR) |
| In-app update | GitHub Releases | GitHub Releases: notify + open download (auto-update later) |

## 4. How to run this prompt

- **One phase per session.** Start each session with:
  "Read `docs/kmp/KMP-MASTER-PROMPT.md` and `docs/kmp/progress.md`, then continue with the next unchecked task of phase Kx."
- Keep `docs/kmp/progress.md` as the single source of truth. Each task has a checkbox, a commit hash and an evidence line.
- Branch `feat/kmp-android-desktop`, created from `production` after PRs #52/#53 are merged, otherwise from `chore/phase6-hardening`.
  Open one draft PR and push after every task.
- **Per-task loop:**
  1. Read the task.
  2. Run the baseline commands.
  3. Change.
  4. Build both targets.
  5. Run the tests.
  6. Commit.
  7. Update `progress.md`.
  8. Push.

## 5. Phases and micro-tasks

Every task's done condition includes:
- `./gradlew :salesmanager:assembleDebug` and the Android unit tests are green;
- from K2 on, `./gradlew :desktopApp:run` starts (or the module compiles for desktop).

### K0 — Baseline and spike (no production code changes)
- [ ] K0.1 Record the baseline: test counts per module, coverage, lint, build times, APK size. Save to `docs/kmp/baseline.md`.
- [ ] K0.2 Generate the dependency inventory: every library mapped to KMP-ready / JVM-only / Android-only, with version and evidence link. Save to `docs/kmp/dependency-matrix.md`.
- [ ] K0.3 Generate the Android-coupling inventory per file (`android.*`, Firebase, Gson, WorkManager, PDF, CameraX), grouped by module and layer. Save to `docs/kmp/coupling-inventory.md`.
- [ ] K0.4 Spike in a throwaway branch: an empty KMP library + `desktopApp` showing one Arabic RTL Compose screen with the bundled IBM Plex Sans Arabic font on Windows. Record the Compose Multiplatform, Kotlin and AGP versions that work. Do not merge the spike; write ADR-0001 (module layout and plugins).
- [ ] K0.5 Write `docs/kmp/architecture.md` (the target diagram above), `docs/kmp/platform-matrix.md` and `docs/kmp/adr/README.md`.
- [ ] K0.6 Arabic report to the owner, plus the list of decisions needed: K1.3, K5 charts, K6.1 PDF.

### K1 — Build foundation
- [ ] K1.1 Add Compose Multiplatform, KMP and Room KMP versions to the catalog (no usage yet). Build green.
- [ ] K1.2 Create an empty `:shared:core-model` KMP module (android + desktop targets, `jvmShared` hierarchy). CI compiles it.
- [ ] K1.3 **Decision (stop and ask):** the desktop Firebase strategy. Evaluate with a spike:
  - (a) a KMP Firebase SDK with JVM support;
  - (b) Firebase REST APIs (Identity Toolkit auth, RTDB REST + streaming, Firestore REST) behind the existing remote interfaces;
  - (c) another option.

  Criteria: auth flow, realtime listeners, offline behaviour, Firebase security rules compatibility, merchant isolation, maintenance cost.
  Write ADR-0002 and wait for approval.
- [ ] K1.4 Add a `desktopApp` module with `main()`, an empty window titled "مدير المبيعات", RTL, the app icon, and Arabic font resources. `./gradlew :desktopApp:run` works.
- [ ] K1.5 CI: add a `windows-latest` job that compiles desktop and runs desktop tests (no packaging yet). Linux jobs unchanged.
- [ ] K1.6 Docs: `docs/kmp/developer-setup.md` (JDK, running desktop and Android, IDE plugin).

### K2 — Domain models to commonMain (smallest pieces first)
- [ ] K2.1 Move plain enums and value types (`PaymentType`, `UnitType`, `WeightUnit`, `SyncStatus`, `StockLevel`, ...) one file per commit. Keep the old package names (no import churn) unless the ADR says otherwise.
- [ ] K2.2 Move data classes without platform types (`Customer`, `Transaction`, `Product`, `ProductUnit`, `InvoiceItem`, ...) in batches of at most 5 files. Move their pure unit tests to `commonTest`.
- [ ] K2.3 `Merchant` and `ChatMessage`: replace Firebase `Timestamp` in the **domain** with epoch millis (or kotlinx-datetime `Instant`). Convert at the Firebase mapping layer only.
  **Stop and ask** if any stored/remote field format would change. The Firebase data shape must stay identical; prove it with a mapping round-trip test.
- [ ] K2.4 Move the pure parts of `AppNumberFormat` to commonMain (digit mapping, parse/normalise, isolates).
  - Implement `String.format`-dependent parts in `jvmShared`.
  - Move `AppNumberFormatTest` with them; it must pass on both targets.
- [ ] K2.5 Move `MerchantContext` and the repository interfaces to `:shared:core-domain`. Android keeps its implementations for now.

### K3 — Data layer
- [ ] K3.1 Room KMP on Android first: add the KMP Room setup and keep the **same schema JSON** (diff must be empty). Run all migration tests. **Stop and ask** if the schema hash would change.
- [ ] K3.2 Move entities, then DAOs (in batches), to `:shared:core-data` commonMain. Room tests run in `jvmShared` tests (in-memory, bundled SQLite driver).
- [ ] K3.3 Create the desktop database: file path in `%APPDATA%\SalesManager\salesmanager.db`, created on first run. Test that it opens, migrates and closes.
- [ ] K3.4 DataStore: KMP DataStore with platform file paths. Migrate the `appDataStore` provider, and keep the Android file name so existing users keep their settings.
- [ ] K3.5 Move repository implementations that only touch Room/DataStore to commonMain, one repository per commit. Keep the merchant-isolation tests green and move them too.
- [ ] K3.6 Crypto/backup: move it to `jvmShared` (javax.crypto + BouncyCastle). The archive format must stay byte-compatible: an Android backup must restore on desktop and vice versa (test).

### K4 — Remote layer (after K1.3 approval)
- [ ] K4.1 Define the remote interfaces in commonMain (activation, merchant status, sync, chat, products, outbox transport) from the current classes, signatures only.
- [ ] K4.2 Move the Android Firebase implementations to `:shared:core-remote` androidMain, unchanged.
- [ ] K4.3 Desktop implementation according to ADR-0002, one service per task:
  1. activation/auth;
  2. merchant status;
  3. products;
  4. transactions sync;
  5. outbox transport;
  6. chat.

  Each task adds contract tests that run against the same fake/emulator for both implementations.
- [ ] K4.4 Merchant isolation on desktop: port `MerchantIsolationTest` so it runs for both targets.
- [ ] K4.5 Desktop background work: a scheduler service that replaces WorkManager jobs while the app runs (outbox dispatch, status checks, unpaid-debt reminder). One job per commit, with tests.

### K5 — Shared presentation
- [ ] K5.1 Move to commonMain:
  - view models (lifecycle-viewmodel KMP) one per commit, starting with those that have no Android imports;
  - their tests to commonTest or `jvmShared` tests.
- [ ] K5.2 Theme, colors, typography: move them to `:shared:seller-ui` and load the font with Compose resources. Keep `AppLocale`/RTL behaviour, and keep `LocalLayoutDirection` RTL on both targets.
- [ ] K5.3 Strings: move `strings.xml` entries used by moved screens to Compose Multiplatform resources. The 1,106 hard-coded literals stay as they are (out of scope), unless a moved file needs them.
- [ ] K5.4 Navigation: move the navigation graph to multiplatform navigation. Android deep links and pending navigation keep working.
- [ ] K5.5 Charts: decide (ADR-0003) between a multiplatform Vico version and a shared Canvas chart. The chart pixel test must keep passing on Android.

### K6 — PDF on desktop
- [ ] K6.1 **Decision spike (ADR-0004):** how the desktop renders Arabic PDF with correct shaping and bidi. Evaluate:
  - a Skia/Skiko PDF backend;
  - PDFBox plus an Arabic shaping step;
  - another option.

  Acceptance: the existing `qa/pdf-verifier` (PDFBox text extraction, Arabic digits, totals, pagination) passes on desktop-generated files.
- [ ] K6.2 Move the PDF layout engine (design tokens, components, pagination, layout log) to `:shared:core-pdf` commonMain behind `PdfPageSink`.
- [ ] K6.3 Desktop sink and save dialog ("حفظ PDF" to Documents, then open the folder).
- [ ] K6.4 Run all PDF tests (statement > 25 rows, invoice, monthly report) on both targets. Compare Android and desktop page counts and layout logs; they must be identical.

### K7 — Screens to commonMain (one screen per task, smallest first)

Order:
1. Settings;
2. payment methods;
3. customers list → details → add/edit;
4. products list → detail → add/edit;
5. invoice items;
6. transaction add/edit → details;
7. returns;
8. reports, then day transactions;
9. debts;
10. cash boxes;
11. inventory session;
12. stock reports;
13. backup;
14. employees;
15. sessions;
16. chat;
17. home;
18. activation, splash and blocked.

For each screen:
- [ ] move it with its components, without changing behaviour;
- [ ] Android: its UI tests stay green (they still run on Android);
- [ ] desktop: one smoke test (the screen opens, RTL, no crash);
- [ ] keep its accessibility labels and its recomposition guard if it has one.

Platform-only pieces go behind small interfaces provided by DI. Do this for:
- the barcode source: camera on Android, keyboard wedge on desktop;
- share/print;
- notification permission;
- in-app update.

### K8 — Desktop app experience
- [ ] K8.1 Window: minimum size, remembered size and position, title, icon. Use the existing `WideScreenContainer` for wide windows.
- [ ] K8.2 Keyboard: shortcuts for the main actions; Tab order; Enter submits forms; a USB barcode scanner fills the active product search.
- [ ] K8.3 System tray and notifications (chat messages, sync failures, low stock) while the app runs.
- [ ] K8.4 Single instance (a second launch focuses the window). Graceful shutdown flushes the outbox.
- [ ] K8.5 Logging to `%APPDATA%\SalesManager\logs` with rotation. No secrets or personal data in logs (test).

### K9 — Windows packaging and release
- [ ] K9.1 Compose Desktop native distribution: MSI (x64), bundled runtime, Arabic app name, icon, version from the catalog.
- [ ] K9.2 CI on `windows-latest`: build the MSI as an artifact on every PR, and install it silently plus launch a smoke test.
- [ ] K9.3 Release workflow: attach the MSI to a GitHub Release on tag `desktop-v*` (unsigned).
  - Document how to sign later.
  - **Stop and ask** before buying or using a certificate.
- [ ] K9.4 Desktop update check: compare the latest GitHub Release, notify, and open the download page.

### K10 — Admin app (same pattern, after the seller is complete)
- [ ] K10.1 Plan the admin moves by reusing K2–K9; this task only produces the task list in `progress.md`.
- [ ] K10.x Execute the plan one screen per task.

### K11 — Final verification
- [ ] K11.1 Full Android regression: all JVM tests, device CI API 24–36, R8 smoke, coverage gates, lint 0 errors.
- [ ] K11.2 Desktop: all desktop tests on Windows CI; MSI install, launch and uninstall smoke test.
- [ ] K11.3 Cross-platform data test: the same merchant on Android and desktop. Sales from both appear on the other side, with no duplicates.
- [ ] K11.4 Final docs and Arabic report (template in section 7).

## 6. Documentation deliverables (`docs/kmp/`)

| File | Content |
|---|---|
| `KMP-MASTER-PROMPT.md` | this file |
| `progress.md` | task checklist with commit hash + evidence per task |
| `baseline.md` | numbers before the migration |
| `architecture.md` | modules, source sets, dependency rules, diagram |
| `platform-matrix.md` | capability per platform (section 3), kept current |
| `dependency-matrix.md` | every library and its KMP status |
| `coupling-inventory.md` | Android-coupled files and their target module/strategy |
| `adr/NNNN-*.md` | one ADR per decision (layout, desktop Firebase, charts, PDF, update) |
| `expect-actual-catalog.md` | every platform abstraction: interface, Android impl, desktop impl, tests |
| `testing-strategy.md` | which tests run where (common/jvmShared/android/desktop/device) |
| `developer-setup.md` | how to build/run/test both targets |
| `windows-release.md` | packaging, signing (future), release steps |
| `desktop-user-guide-ar.md` | Arabic guide for the owner/testers: install, first activation, scanner, backup, update |
| `manual-test-checklist-ar.md` | Android + Windows manual checks for the owner |

## 7. Report template (Arabic, after every phase)

1. ما تم في هذه المرحلة: المهام المنجزة ورابط كل commit.
2. الأدلة: أعداد الاختبارات قبل/بعد لكل وحدة، ونتائج البناء للهدفين، وCI.
3. القرارات المطلوبة منك (إن وُجدت) مع الخيارات والتوصية.
4. المخاطر والمشاكل المكتشفة.
5. ما التالي (المرحلة القادمة وأول ثلاث مهام).

## 8. Definition of done (whole project)

- Android: identical behaviour; all existing tests green; device CI API 24–36 green; R8 smoke green; lint 0 errors;
  test count ≥ baseline.
- Desktop (Windows 10/11 x64): MSI installs and launches; seller app fully usable offline and online.
  Arabic RTL with correct shaping. PDF passes the verifier. Merchant isolation tests pass.
  Desktop tests run in Windows CI.
- Shared code: domain, data, view models and screens live in commonMain/jvmShared. Platform code is limited
  to the thin app modules and documented abstractions.
- Docs in section 6 complete; final Arabic report delivered.
