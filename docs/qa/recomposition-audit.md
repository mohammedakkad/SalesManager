# Recomposition audit (Phase 6C)

Date: 2026-11-22. Scope: every screen of `:salesmanager` (27 screens + 6 animated states) and `:admin` (10 screens).

## Method

Layout Inspector needs a running emulator or device; this environment has no KVM, so the audit uses a
JVM substitute that counts the same thing.

* `testing/recomposition/RecompositionTracker.kt` installs a Compose `CompositionTracer`. The Compose compiler
  emits a trace marker at the start of every composable body that **runs**; skipped calls emit nothing. So
  the tracer counts real (re)compositions per composable, including library internals. It is in unit-test
  source sets only (`sourceSets.test.kotlin.directories += ../testing/recomposition`), so it can never be part
  of a debug or release APK.
* `RecompositionMeter` opens a screen with the real theme and a **manually driven clock**, so infinite animations
  run exactly as on a device (Compose test rules otherwise cancel them). It records three phases:
  *open* (first 2 s of frames), *idle* (10 s, 600+ frames, no input) and *one interaction* (type into the first
  text field, otherwise click the first clickable node).
* View models are relaxed MockK fakes whose `Flow` properties are stable `MutableStateFlow`s with representative
  data for the main screens (`FakeViewModels`). A relaxed mock that returned a new flow per call would itself
  cause recompositions.
* Re-run: `./gradlew :salesmanager:testDebugUnitTest --tests '*ScreenRecompositionAuditTest*' :admin:testReleaseUnitTest`
  (results in `<module>/build/recomposition/<stage>/*.tsv`; `RECOMPOSITION_STAGE` names the stage).
* Compose compiler reports: `./gradlew -PcomposeReports=true :salesmanager:compileDebugKotlin :admin:compileReleaseKotlin`
  (output in `<module>/build/compose_compiler`).

## Compiler reports (static stability)

| Module | Composables | Restartable | Skippable | Unstable args | Classes unstable / uncertain / stable |
|---|---|---|---|---|---|
| salesmanager | 1385 | 1376 | 796 | 28 | 52 / 33 / 109 |
| admin | 375 | 375 | 217 | 2 | 14 / 7 / 15 |
| app (core) | 1 | 1 | 1 | 0 | n/a |

Strong skipping is on (Kotlin 2.4 default), so every **named** composable is skippable; the non-skippable entries
are composable lambdas, which strong skipping memoises instead. Only two unstable parameters reach a function
signature (`DayTransactionsViewModel?` default and a `Context?`), and neither is on a hot path. Static stability
was therefore not the problem; the runtime counts below are.

## Results (executions: all composables / app composables)

Open = first 2 s; idle = 10 s without input (target 0); interaction = one input. "Before" is the
code before 6C; "after" is the code at the end of 6C.

| Screen / state | Open before | Open after | **Idle before** | **Idle after** | One interaction (after) |
|---|---|---|---|---|---|
| activation | 7483 / 574 | 415 / 16 | **23864 / 1884** | **0 / 0** | 735 / 0 |
| backup | 628 / 23 | 628 / 23 | **0 / 0** | **0 / 0** | 0 / 0 |
| blocked | 151 / 15 | 151 / 15 | **0 / 0** | **0 / 0** | 0 / 0 |
| cash-boxes | 276 / 15 | 269 / 15 | **0 / 0** | **0 / 0** | 0 / 0 |
| chat | 441 / 20 | 441 / 20 | **0 / 0** | **0 / 0** | 611 / 0 |
| chat-pending | 6113 / 766 | 563 / 26 | **18810 / 2508** | **0 / 0** | - / - |
| customer-add | 628 / 26 | 628 / 26 | **0 / 0** | **0 / 0** | 796 / 1 |
| customer-details | 347 / 19 | 347 / 19 | **0 / 0** | **0 / 0** | 0 / 0 |
| customers-list | 1496 / 42 | 1496 / 42 | **0 / 0** | **0 / 0** | 623 / 0 |
| day-transactions | 880 / 41 | 880 / 41 | **0 / 0** | **0 / 0** | 623 / 0 |
| debts | 174 / 12 | 174 / 12 | **0 / 0** | **0 / 0** | 0 / 0 |
| employees | 287 / 20 | 287 / 20 | **0 / 0** | **0 / 0** | 0 / 0 |
| empty-state | 73 / 9 | 66 / 9 | **0 / 0** | **0 / 0** | - / - |
| export-loading | 3702 / 383 | 131 / 13 | **12549 / 1254** | **0 / 0** | - / - |
| home | 1663 / 107 | 1655 / 107 | **0 / 0** | **0 / 0** | 0 / 0 |
| home-sync-failed | 2328 / 282 | 1270 / 97 | **3714 / 627** | **0 / 0** | - / - |
| home-sync-pending | 2576 / 283 | 1270 / 97 | **4410 / 630** | **0 / 0** | - / - |
| inventory-list | 1364 / 73 | 1364 / 73 | **0 / 0** | **0 / 0** | 623 / 0 |
| inventory-session | 141 / 12 | 141 / 12 | **0 / 0** | **0 / 0** | 0 / 0 |
| invoice-items | 248 / 20 | 248 / 20 | **0 / 0** | **0 / 0** | 0 / 0 |
| payment-methods | 233 / 16 | 233 / 16 | **0 / 0** | **0 / 0** | 0 / 0 |
| product-add | 1987 / 41 | 1987 / 41 | **0 / 0** | **0 / 0** | 808 / 1 |
| product-detail | 323 / 23 | 323 / 23 | **0 / 0** | **0 / 0** | 0 / 0 |
| reports | 4110 / 226 | 4110 / 226 | **0 / 0** | **0 / 0** | 0 / 0 |
| return-process | 199 / 16 | 199 / 16 | **0 / 0** | **0 / 0** | 0 / 0 |
| sessions | 171 / 14 | 171 / 14 | **0 / 0** | **0 / 0** | 0 / 0 |
| settings | 1002 / 41 | 1002 / 41 | **0 / 0** | **0 / 0** | 0 / 0 |
| splash | 3423 / 194 | 93 / 9 | **11286 / 627** | **0 / 0** | - / - |
| stock-reports | 379 / 23 | 379 / 23 | **0 / 0** | **0 / 0** | 0 / 0 |
| transaction-add | 1267 / 34 | 1267 / 34 | **0 / 0** | **0 / 0** | 724 / 0 |
| transaction-details | 555 / 39 | 555 / 39 | **0 / 0** | **0 / 0** | 0 / 0 |
| transaction-edit | 1267 / 34 | 1267 / 34 | **0 / 0** | **0 / 0** | 724 / 0 |
| transactions-list | 398 / 17 | 398 / 17 | **0 / 0** | **0 / 0** | 0 / 0 |
| admin-add-merchant | (no code change) | 730 / 21 | = after | **0 / 0** | 735 / 0 |
| admin-chat-detail | (no code change) | 410 / 13 | = after | **0 / 0** | 611 / 0 |
| admin-chat-list | (no code change) | 123 / 7 | = after | **0 / 0** | 0 / 0 |
| admin-dashboard | (no code change) | 475 / 24 | = after | **0 / 0** | 0 / 0 |
| admin-login | (no code change) | 519 / 13 | = after | **0 / 0** | 735 / 0 |
| admin-merchant-detail | (no code change) | 548 / 25 | = after | **0 / 0** | 0 / 0 |
| admin-merchants | (no code change) | 910 / 20 | = after | **0 / 0** | 617 / 0 |
| admin-notifications | (no code change) | 127 / 7 | = after | **0 / 0** | 0 / 0 |
| admin-requests | (no code change) | 156 / 11 | = after | **0 / 0** | 0 / 0 |
| admin-subscription | (no code change) | 211 / 13 | = after | **0 / 0** | 0 / 0 |

Small open-phase differences on screens that were not changed (cash-boxes 276 → 269, home 1663 → 1655, empty-state
73 → 66) come from the test fake, not the app: the first run did not yet stub `isX` flow getters.

Interaction counts are small and confined to the touched control (text-field internals, ripple), with 0–1 app
composables re-run. That is expected: a fake view model does not change state on input. State-driven changes are
covered by the guard tests below.

## Causes found and fixed (one commit each, no visual or behaviour change)

| Screen | Cause | Fix | Idle 10 s before → after |
|---|---|---|---|
| Activation | Infinite logo pulse read by `Modifier.scale` in composition; the Scaffold content re-ran every frame | `graphicsLayer { scaleX/scaleY }` lambda (draw phase) | 23,864 → 0 |
| Splash | Same pattern | Same | 11,286 → 0 |
| Chat, pending message | Animated alpha used to build the `Surface` colour | Same shape filled in `drawBehind` | 18,810 → 0 |
| Home sync icon, pending | `Modifier.rotate(rotation)` in composition | `graphicsLayer { rotationZ }` | 4,410 → 0 |
| Home sync icon, failed | Animated tint alpha | `graphicsLayer { alpha }` with `CompositingStrategy.ModulateAlpha` (same as tint alpha) | 3,714 → 0 |
| Export button, exporting | Animated colour alpha on the progress indicator | Same as above | 12,549 → 0 |
| Empty state (all empty lists) | An infinite animation whose value was never read: no recomposition, but a frame callback every frame | Removed | 0 → 0 (frames no longer requested) |
| Customers list | `AnimatedContent` lambda captured the whole `uiState`; any unrelated change (sync badge) re-ran every visible row | Capture only the list | badge change: 319 → 139 (app 10 → 4, rows 6 → 0) |

Visual parity: all changed states were rendered before and after at identical clock times (28 frames,
Robolectric native graphics) and were pixel-identical. That rendering path does not show draw-layer
animation mid-motion, so the parity during animation rests on API equivalence: `Modifier.scale` and
`Modifier.rotate` are thin wrappers over `graphicsLayer`, `ModulateAlpha` applies alpha per draw call like
a tinted colour, and the chat bubble outline is the same shape and colour that `Surface` drew.

Checked and already correct: inventory sync rotation (`graphicsLayer`), barcode scan line (`drawWithContent`),
`collectAsStateWithLifecycle` use, lazy list keys, and number/date formatting (cached in `AppNumberFormat`).

Not changed (documented): the transaction add/edit form is one large composable scope, so a keystroke in the
amount field re-runs about 15 app composable bodies (no lists or rows). Splitting the form into sub-composables is
a structural refactor outside the surgical scope of 6C. The customers list also cross-fades its content on each
search keystroke (`AnimatedContent` target includes the query); that is existing behaviour and was left as is.

## Guard tests

`salesmanager/src/test/.../ui/recomposition/RecompositionGuardTest.kt` (12 tests):

* Home, reports, transaction add, transaction edit, customers list, invoice items: 10 s idle runs **no composable at
  all**, and one state change re-runs only allowed named composables, within an audited cap
  (home 4, reports 8 with `MetricCard` allowed, transaction form 15, customers 4, invoice 8).
* Sync pending, sync failed, chat pending bubble, export in progress, splash, activation: idle must be 0.
* All seven fixed cases fail on the pre-6C code (checked by running the guards against it).

`admin/src/test/.../ui/AdminRecompositionAuditTest.kt` asserts idle = 0 for all ten admin screens.

Raising a cap needs a reason recorded here.
