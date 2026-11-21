# Bounded SaleCommands seeded-fault sensitivity sample

Archive source: ef1c8999577518e14f1fc799328360eb38721874. Production checkout and its measured coverage stayed unchanged. Eight selected real Room baseline cases PASS. No Admin changes, branch, PR or production service was involved.

| Mutant | Injected financial defect | Result | Targeted runtime cases |
|---|---|---|---:|
| M01 | Omit persisted invoice lines | KILLED | 1 |
| M02 | Reverse sale inventory effect | KILLED | 1 |
| M03 | Omit sale cash effect | KILLED | 1 |
| M04 | Allow negative remaining stock | KILLED | 1 |
| M05 | Accept a changed request under an already used operation identifier | KILLED | 1 |
| M06 | Accept stale editing snapshot | KILLED | 1 |
| M07 | Restore already returned stock again on deletion | KILLED | 2 |
| M08 | Omit durable transactional outbox insertion | KILLED | 2 |

All eight preselected faults were detected. Ten selected-case executions intentionally failed under faulty source. The failed XML is expected fault-detection evidence, not unresolved failures in the passing409-case source suite. No compilation failure was counted as a kill.

This manual sample does not establish a comprehensive mutation score or100% coverage. Broader mutation generation/operators and the other four component campaigns remain unqualified. The archived source was restored byte-for-byte after the experiment. The sibling sale-mutation-runner.py records the environment-specific procedure; it is not run in release CI.
