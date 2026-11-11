Seller export retries, inventory completion and unit editing coverage

Failed downloads previously discarded the generated file state. All five Seller export consumers now retain the file/sheet for explicit retry and show failure feedback. Pending-item searches no longer claim inventory completion when the query hides uncounted items. Inventory and persisted-unit deletion actions gain accessible names. Financial formulas/schema remain unchanged.

Validation:632JVM PASS (31new;all601prior identities retained),22Python guardsPASS (10new);10actual failing-before UI regressions nowPASS. Clean Lint/Debug/AndroidTest/R8 unsigned Release aggregatePASS in6m40s;Lint0errors/24warnings. Source ee1fcc2ecf41ebba24884e366d6f8abf9748c467: hosted42983779778 SUCCESS, seller and compose API24/28/33/35;7Android/5PDFBox per lane with actual files/path hashes/artifacts.

|Measured scope|Line covered/total|Method covered/total|Branch covered/total|
|---|---:|---:|---:|
|Seller — project-local JVM|14223/17903 = 79.4448%|2463/3201 = 76.9447%|7402/14708 = 50.3264%|
|Weighted — project-local JVM|22357/28210 = 79.2520%|4281/5716 = 74.8950%|9017/17819 = 50.6033%|
|Weighted — both JVM suites|22671/28210 = 80.3651%|4399/5716 = 76.9594%|9127/17819 = 51.2206%|
|Seller — Android + JVM|15228/17903 = 85.0584%|2533/3201 = 79.1315%|7567/14708 = 51.4482%|
|Weighted — Android + module JVM|23379/28210 = 82.8749%|4369/5716 = 76.4346%|9182/17819 = 51.5293%|
|Weighted — Android + both JVM suites|23680/28210 = 83.9419%|4473/5716 = 78.2540%|9292/17819 = 52.1466%|

Additional whole-suite reports merge actual app+Seller JVM streams, optionally Android. Existing reports/gates/class lists remain unchanged. Scope correction is explicit and is not attributed to new tests alone.

Goal95% remains unmet; releaseFAIL. No Admin business changes, destructive migrations, production access, signing input, exclusions or exactly-once distributed claims. See docs/qa/coverage95-next-progress.md. Keep PR51 Draft and unmerged.
