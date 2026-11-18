Standalone Seller stock adjustments could change quantity before a failed movement insert; pending/background uploads swallowed cancellation; an old status response could clear a newly selected merchant; queued manual-adjust submissions could write twice or remain busy after failure. Share the provided Room transaction for quantity+ledger, reject nonfinite/overflow inputs, propagate cancellation, compare activation inside atomic DataStore edit, and reserve/release manual submission with visible retry feedback. Finite arithmetic/clamp, financial formulas, schema25 and Outbox protocol are preserved.

36 new JVM cases, all1020 previous cases retained:1056 JVM PASS;31 Python PASS including6 provenance guards.13 final regression identities fail before repair. Clean full validation12m54s, Lint0errors/27warnings, Debug/AndroidTest/minified unsigned Release PASS.

|Scope|Line|Method|Branch|More branches for75%|
|---|---:|---:|---:|---:|
|Seller JVM|15479/17982 = 86.0805%|2702/3225 = 83.7829%|9547/14816 = 64.4371%|1565|
|Historical weighted module JVM|23832/28390 = 83.9451%|4560/5740 = 79.4425%|11476/17985 = 63.8087%|2013|
|Additional weighted both JVM|24158/28390 = 85.0933%|4692/5740 = 81.7422%|11583/17985 = 64.4037%|1906|

Fresh14 Android+5 PDFBox cases per API24/28/33/35 remain pending. The prior a5c402f run44288339259 passed all jobs; API24 retained2/5 path notices despite5 PDFBox passes. Continuously capture notices and verify device paths/hashes against actual pulled bytes. No fake PDFs or reused old EC. Unresolved11-branch Boxes variation is reported from the latest run with identical bytecode. Original report reader0.8.12/gates retained.7 authentic migrations PASS;1–21 BLOCKED;56 defined plan items remain. Branch75/prior Line95 NOT ACHIEVED; Release FAIL. Evidence: docs/qa/branch75-stock-boundaries-progress.md.
