This continues the Seller audit on the same draft PR/base production. Firebase activation/status/bulk snapshot reads now propagate cancellation, including cached Task completion. Backup crypto fixes the actual API24 missing PBKDF2 factory with compatible SHA256 derivation and unchanged version1 envelope.43 new JVM cases cover wire/cancellation contracts, crypto interoperability and live UI state/identity updates; all956 prior cases retained:999 JVM PASS,25 Python PASS. Seven regression identities fail before repair.

|Scope|Line|Method|Branch|More branches for75%|
|---|---:|---:|---:|---:|
|Seller JVM|15365/17971 = 85.4989%|2689/3224 = 83.4057%|9356/14812 = 63.1650%|1753|
|Historical weighted module JVM|23596/28359 = 83.2046%|4524/5747 = 78.7193%|11191/17941 = 62.3767%|2265|
|Additional weighted both JVM|23929/28359 = 84.3789%|4658/5747 = 81.0510%|11298/17941 = 62.9731%|2158|

Local Lint0errors/27warnings, Debug/AndroidTest/minified unsigned Release PASS; mapping upload excluded. New source runtime requirements:12 Android+5 PDFBox cases across API24/28/33/35; pending until the source-specific run completes. No old Android EC is merged. Branch75 and previous Line95 are not achieved; Release FAIL. Room/schema25/financial/Outbox protocols unchanged;7 authentic migration cases pass,1–21 remain blocked. Evidence: docs/qa/branch75-compatibility-progress.md.
