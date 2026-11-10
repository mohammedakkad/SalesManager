Seller audit continuation: catalog integrity, safe navigation and measured coverage

Catalog synchronization previously lost unit cost, treated failed reads as empty data and let merchant child reads survive logout. Seller catalog processing now preserves numeric/cost fields, fails malformed numeric snapshots as a whole and owns reads within the active merchant scope. Notification navigation waits for the graph; URI input round trips once. Manual stock adjustment rejects nonfinite quantities.

|Metric (JVM)|Seller covered/total|Coverage|Weighted covered/total|Coverage|
|---|---:|---:|---:|---:|
|LINE|13896/17894|77.6573%|22030/28201|78.1178%|
|METHOD|2390/3200|74.6875%|4208/5715|73.6308%|
|BRANCH|7151/14696|48.6595%|8766/17807|49.2278%|

Validation: 593 JVM PASS (79 new), 12 Python guards PASS, unchanged gates, Lint 0 errors/24 warnings, Debug/AndroidTest/minified unsigned Release builds PASS. Hosted verification for this source is pending. No Admin business changes, destructive migration, signing inputs or exactly-once distributed claims.

The 95% target remains unmet. Release readiness FAIL: server authorization/revision fencing, reconciliation, historical migrations1–21 and broader device/security/performance qualification remain open. See docs/qa/coverage95-progress.md. Keep PR51 Draft and unmerged.
