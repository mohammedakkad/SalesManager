# Seller lifecycle, metadata and native chart continuation — 2026-11-11

Starting HEAD: `c9bfb3a1516d210cb05822e631a821314020f2a2`. Same audit branch and Draft PR51. Goal95% remains unachieved; release readiness FAIL. No Admin business, Room schema, sale command, outbox protocol or financial mutation formula changes.

## Behavioral repairs and executed cases

95 new JVM cases bring the complete suite to758 PASS (app 208, Seller 550), zero failures/errors/skips. Every one of the663 starting test identities remains.22 Python coverage integrity guards PASS.26 final regression identities actually fail before their corresponding repair; the count denotes cases, not26 independent root causes. XML, logs and explicit fixture diagnostics are in `evidence/continuation/coverage95-lifecycle/`.

- 24 release metadata cases: actual parser, deterministic HttpURLConnection failures and one loopback HTTP request verifying method/headers/body. Connection cleanup now occurs on success, denied HTTP status and transport failure; cancellation propagates. The existing major*10000+minor*100+patch encoding now rejects negative components, minor/patch beyond two digits and Int overflow. Public no-argument API/production endpoint remain unchanged; an internal connection factory seam enables offline testing. No live release/Firebase service, APK download or signing verification is exercised by these metadata cases.
- 11 text invoice cases and1actual Compose quick-share journey: persisted invoice ID is included; returned invoices label original items, original amount, returned difference and already-net total separately. Defaults for store name, all five payment kinds, paid/unpaid status, notes, quantities and WhatsApp/chooser fallback are checked. Existing persisted original/net values are displayed; no cash/debt/stock mutations or return formulas change.
- 21 invoice draft lifecycle cases: real ViewModel with controlled catalog repository; unit-specific merging, custom/default/zero prices, removal, barcode hit/miss, stock boundaries, restoration, malformed input and cancellation. Catalog cancellation now cancels the loader job and preserves the prior basket instead of being swallowed. Existing missing-catalog skipping/legacy weight fallback remain unchanged; this is not a new immutable catalog snapshot guarantee. Existing local commercial POUND factor4.0 is explicitly preserved.
- 8 real FCM payload decoding/Android service notification cases on Robolectric API24/28, without token refresh/network startup. Content fallback/precedence, timestamps, channel, immutable chat intent and replacement slot are verified. Not end-to-end FCM delivery or authorization qualification.
- 10 low-stock worker recovery cases and8scheduling cases: cancellation propagates during settings, candidate lookup and cooldown marking. Notification/permission/channel suppression does not consume cooldown; existing best-effort behavior after a delivered notification and failed marking remains. Actual WorkManager enqueue/KEEP/replace/cancel and 24-hour interval are checked, plus 5deterministic clock/DST cases. Scheduling tests do not benchmark OS background delivery latency.
- 4 report summary cases: existing paid/unpaid/count values from the selected period now reach the summary card. No report arithmetic changes.
- 2 native rendered-color cases: sales/profit bar colors now match their legend; negative profit remains red. Actual decorated view is rendered through native Canvas/Bitmap; pixel column lengths detect the original reversed colors (sales57pixels/profit190before).
- 6 accessible/native chart cases: descriptions expose every actual date/amount, paid/unpaid split, signed profit and payment share. Native pixels verify multi-point curve, paid/debt bars, proportional donut, loss and zero/single-point behavior. Actual screenshots are preserved. Semantics tests do not substitute for real TalkBack/device GPU/performance qualification.

The95cases partition into60unit/model cases with deterministic fakes,13Compose UI cases (8also native bitmap),13WorkManager lifecycle cases,8Android notification component cases and1loopback HTTP case. Existing Room/migration cases remain; these in-memory drafts and platform components do not claim95 new application-Room tests.

## Before/after evidence integrity

`before/ReleaseMetadataVersionBoundaries.xml` retains separately executed negative/overflow/component cases. The earlier aggregate invalid-version test was replaced and is not counted again. `final/failing-before-final-identities.json` intersects actual failing-before identities with the final95 new cases. `after/` holds their complete passing class XML from the full suite. Diagnostics distinguish test-only constructor/method mistakes, lazy child selection and the retired WorkManager ID assumption. PixelCopy capture initially timed out because Robolectric lacks its hardware-window redraw callback; final tests render the real view into native Skia pixels, without synthetic/reference charts. No fake PDF is generated.

## Measured local coverage

Original qaCoverage/qaCombinedCoverage reports and gates stay unchanged. Additional reports merge actual app+Seller JVM execution while counting production classes once. Native canvas execution is real JVM execution and is not labeled Android instrumentation coverage. Class lists include generated code and no production exclusions are added. Counters follow clean Seller production compilation; current Seller source adds 33executable lines and 10 methods compared with the starting source.

|Scope|Line covered/total|Method covered/total|Branch covered/total|
|---|---:|---:|---:|
|Seller JVM|14974/17946 = 83.4392%|2605/3222 = 80.8504%|7981/14784 = 53.9840%|
|Weighted module-local JVM|23108/28253 = 81.7895%|4423/5737 = 77.0960%|9596/17895 = 53.6239%|
|Weighted both JVM suites|23432/28253 = 82.9363%|4555/5737 = 79.3969%|9708/17895 = 54.2498%|

Android+JVM counters for this source are PENDING hosted execution; previous-source device EC/counters are not reused. At the current local denominators95% needs 2075 additional covered Seller lines and 3409 weighted both-JVM lines. These are line gaps, not a number of tests. Method/Branch remain separately measured.

## Qualification checkpoint

The full aggregate PASS in6m56s after clean Seller compilation:758JVM cases, JaCoCo/original gates,22Python guards, Seller Lint0errors/24warnings, Debug, AndroidTest and minified unsigned Release (mapping upload excluded). Actual new presentation strings are present in packaged Debug DEX; APK bytes/hashes and unsigned verification are preserved. No runtime pass is inferred from APK compilation. The new source-specific hosted run is PENDING until push and actual execution; previous run42995958373 qualifies the previous source only.

Room 2.6.1/schema 25 unchanged,7 migration cases preserved; historical1–21BLOCKED without authentic fixtures. Existing atomic sale/outbox guarantees and server/tenant/writer/reconciliation limitations remain documented in stage10/stage11.56 defined work items remain 22 NOT TESTED / 32 BLOCKED / 2 PARTIAL; this is not an exhaustive count of all possible tests. Metadata checks do not resolve APK authenticity/signing/security blockers. Release FAIL.
