# Stage8 authentic migration matrix

Available fetched history exports only22 and23. GitHub artifacts cannot be inspected (API Forbidden). No schema was invented.

| Source version | Authentic fixture available | Upgrade path | Records preserved | Test result |
| --- | --- | --- | --- | --- |
| 1 | No | Declared 1→…→22→23 | Unverified | BLOCKED |
| 2 | No | Declared 2→…→22→23 | Unverified | BLOCKED |
| 3 | No | Declared 3→…→22→23 | Unverified | BLOCKED |
| 4 | No | Declared 4→…→22→23 | Unverified | BLOCKED |
| 5 | No | Declared 5→…→22→23 | Unverified | BLOCKED |
| 6 | No | Declared 6→…→22→23 | Unverified | BLOCKED |
| 7 | No | Declared 7→…→22→23 | Unverified | BLOCKED |
| 8 | No | Declared 8→…→22→23 | Unverified | BLOCKED |
| 9 | No | Declared 9→…→22→23 | Unverified | BLOCKED |
| 10 | No | Declared 10→…→22→23 | Unverified | BLOCKED |
| 11 | No | Declared 11→…→22→23 | Unverified | BLOCKED |
| 12 | No | Declared 12→…→22→23 | Unverified | BLOCKED |
| 13 | No | Declared 13→…→22→23 | Unverified | BLOCKED |
| 14 | No | Declared 14→…→22→23 | Unverified | BLOCKED |
| 15 | No | Declared 15→…→22→23 | Unverified | BLOCKED |
| 16 | No | Declared 16→…→22→23 | Unverified | BLOCKED |
| 17 | No | Declared 17→…→22→23 | Unverified | BLOCKED |
| 18 | No | Declared 18→…→22→23 | Unverified | BLOCKED |
| 19 | No | Declared 19→…→22→23 | Unverified | BLOCKED |
| 20 | No | Declared 20→…→22→23 | Unverified | BLOCKED |
| 21 | No | Declared 21→…→22→23 | Unverified | BLOCKED |
| 22 | Actual exported schema; synthetic representative database | 22→23 | Verified canonical records/FKs/sequence/rollback | PASS retained tests |

Acquire missing fixtures from authorized offline copies of historical backups or exact release builds. Record source commit, Room/compiler versions, user_version, identity hash and SHA256. Sanitize copies offline while preserving PK/FK links, amounts and representative orphan/return cases; never alter the original or upload live merchant records. Export schemas using the exact historical entities/compiler, then validate each complete chain through Room and compare record/financial invariants plus failure rollback. Owner provenance is required; reversing migration SQL into a golden schema is not authentic evidence.
