Seller lifecycle cancellation, invoice text and native report qualification

Cancellation previously became normal failure/success in invoice draft restoration and low-stock worker repository boundaries; update metadata also leaked connections on HTTP/transport failures. Cancellation now propagates, draft state remains intact and metadata connections close in finally. Existing version-code encoding rejects negative, aliased two-digit components and Int overflow. Public update API/production endpoint are preserved with an internal offline transport seam.

Quick invoice text now identifies the persisted invoice and separates original items/amount, refunded difference and persisted net total after returns. Existing period paid/unpaid/count values reach the report summary; sales/profit bar colors match their legend and charts expose actual dates, amounts and signed loss through semantics. Financial persistence/return calculations, Room schema and Admin business logic are unchanged. Existing atomic Seller sale commands/outbox retain their documented local guarantees; no new distributed exactly-once claim.

Validation:758JVM PASS,95new and all663starting identities retained;22Python integrity guards PASS; zero failures/errors/skips.26final before-failure regression identities are preserved separately from fixture diagnostics.95new cases include60unit/model cases,13actual Compose cases (8also native Canvas/Bitmap pixel checks),13WorkManager lifecycle cases,8notification component cases and1loopback HTTP case. Real WorkManager enqueue/KEEP/replace/cancel and DST clock boundaries are exercised. Native images are actual rendered views, not synthetic expected charts or device GPU qualification.

Full aggregate after clean Seller compilation PASS in6m56s: JaCoCo/original gates, Lint0errors/24warnings, Debug/AndroidTest and minified unsigned Release with mapping upload excluded. APK hashes/unsigned status and new Debug DEX presentation markers are preserved. No signing material or production Firebase use. Seller JVM Line14974/17946=83.4392%, Method2605/3222=80.8504%, Branch7981/14784=53.9840%; weighted both-JVM Line23432/28253=82.9363%, Method4555/5737=79.3969%, Branch9708/17895=54.2498%. Additional both-JVM scopes do not replace historical reports/gates or change exclusions.

Hosted source d37df33a041f29d661b46d23439cdd69b3b8ba74: https://github.com/mohammedakkad/SalesManager/actions/runs/43003544986 SUCCESS, seller and compose API24/28/33/35. Each lane7actual Android/5PDFBox PASS and five generated PDF paths/hashes/artifact preserved.792distinct cases PASS across qualified suites; device repeats are not new independent cases. Native chart/component JVM tests remain separate from Android instrumentation.

|Measured scope|Line covered/total|Method covered/total|Branch covered/total|
|---|---:|---:|---:|
|Seller — Android + JVM|15979/17946 = 89.0393%|2675/3222 = 83.0230%|8154/14784 = 55.1542%|
|Weighted — Android + module JVM|24130/28253 = 85.4069%|4511/5737 = 78.6299%|9769/17895 = 54.5907%|
|Weighted — Android + both JVM suites|24441/28253 = 86.5076%|4629/5737 = 80.6868%|9881/17895 = 55.2165%|


Room2.6.1/schema25 unchanged,7migration cases preserved; historical1–21BLOCKED without authentic fixtures. Metadata/component tests do not qualify signed APK authenticity, deployed authorization, real FCM delivery or hardware performance.56remaining defined work items stay22NOT TESTED/32BLOCKED/2PARTIAL. Server fencing/reconciliation, universal tenant/writer ownership, signed upgrades/security/device/performance remain open. Goal95% NOT ACHIEVED; releaseFAIL. Keep existing PR51 Draft and unmerged.

Evidence: docs/qa/coverage95-lifecycle-progress.md and docs/qa/evidence/continuation/coverage95-lifecycle/.
