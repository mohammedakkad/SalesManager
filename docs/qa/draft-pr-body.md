Seller catalog integrity, safe navigation and truthful download completion

Catalog sync preserves unit cost/numeric values, fails malformed numeric snapshots as a whole and owns reads in the current merchant scope. Notification navigation waits for NavHost and URI arguments decode once. Manual stock edits reject nonfinite values. Downloads only succeed after copy and publication, and clean pending entries on failure.

|Metric (local JVM)|Seller covered/total|Coverage|Weighted covered/total|Coverage|
|---|---:|---:|---:|---:|
|LINE|13921/17898|77.7796%|22055/28205|78.1954%|
|METHOD|2391/3200|74.7188%|4209/5715|73.6483%|
|BRANCH|7148/14684|48.6788%|8763/17795|49.2442%|

Validation:601JVM PASS (87new;514retained);12Python guardsPASS; unchanged coverage gates; Lint0errors/24warnings; Debug/AndroidTest/minified unsigned ReleasePASS. Source fda922fb865dabf5633115268d3b4e72ccbb8e4f; hosted run42863854908 SUCCESS: seller and all compose API24/28/33/35 lanes;7Android/5PDFBox per lane. No Admin business changes, destructive migration, production network, signing input or exactly-once distributed claim.

Target95% remains unmet; release readinessFAIL. Server authorization/fencing/reconciliation, historical migrations1–21 and broader device/security/performance qualification remain open. See docs/qa/coverage95-progress.md. Keep PR51 Draft and unmerged.

|Metric (hosted API28 Android+JVM)|Seller covered/total|Coverage|Weighted covered/total|Coverage|
|---|---:|---:|---:|---:|
|LINE|14927/17898|83.4004%|23078/28205|81.8224%|
|METHOD|2461/3200|76.9062%|4297/5715|75.1881%|
|BRANCH|7313/14684|49.8025%|8928/17795|50.1714%|

Clean JVM compilation reconciles branch denominators with every hosted lane; the same production class set remains included. Source-specific evidence is in docs/qa/evidence/continuation/coverage95/.
