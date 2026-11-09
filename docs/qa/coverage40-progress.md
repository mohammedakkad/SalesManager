# Seller and weighted line coverage target40%

User target:at least40% line coverage for both Seller and weighted overall; branch counters remain measured and existing floors unchanged. Starting local source dff4876,221 passing JVM tests, Seller1123/17794 =6.3111%, weighted4980/28061 =17.7471%.

Wave1 adds18 behavioral tests:offline Compose invoice edits/Arabic quantities, report readiness/financial sections, cash-box initialization, inventory stock filters, paid/debt transaction filters/navigation, return limits/refunds, and real ReportsViewModel calendar/rank/analytics calculations. Compose uses a standard ActivityScenario test rule with a plain test activity registered under Config.NONE; production manifest/Firebase providers never initialize. Koin context is isolated per activity. No production source, Admin files, coverage floors or exclusions changed. Test-only Compose dependency matches the existing BOM.

Complete239 JVM tests pass (shared95/Seller144),0 failures/skips. Seller lines4586/17794 =25.7727%, branches2140/14312 =14.9525%; weighted lines8443/28061 =30.0880%, branches2783/17329 =16.0598%. Gates pass. Evidence:evidence/continuation/coverage40/wave1/. Target not yet reached; continue.

A diagnostic native JVM PDF probe failed because Robolectric's PdfDocument backend did not produce an open document. It is not part of the committed tests or counted as passing. No fake host PDFs or replacement PDF assertions were introduced; real Android-generated PDF verification remains in the hosted Compose workflow. Continue meaningful UI/business/export coverage without removing PDF production code from coverage.
