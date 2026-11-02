# Seller lint triage

Latest Stage7 measured lint: **0 errors, 24 warnings, 0 information**. Stage6 baseline:0 errors,32 warnings,1 information. No baseline or suppression was added. The warning count is not zero and four dependency registries are skipped by lint; successful lint execution does not mean those missing checks ran.

| Finding | Stage6 count | Decision / next validation |
| --- | ---: | --- |
| UnusedResources | 7 | Seller-only unused colors/subtitle; verify static and dynamic references before removal. |
| ObsoleteSdkInt | 1 | Identical drawable and drawable-v24 launcher XML (same SHA256); minSdk24 permits retaining only base resource. |
| UnusedAttribute | 1 | Keep dataExtractionRules: required on API31+, older devices use fullBackupContent. Removing it would weaken OS backup policy. No warning suppression. |
| StaticFieldLeak | 2 | ViewModels normalize to applicationContext, but store Context-typed fields. A retained Activity leak was not measured; migrate to Application/typed service only with lifecycle regression coverage. |
| ObsoleteLintCustomCheck | 4 | Compose UI and Navigation common/compose/runtime lint registries reference unavailable lint APIs with AGP8.3.2. Compatible AGP/lint/dependency alignment requires a separate validated toolchain increment; no blanket suppression or blind major upgrade. |
| IconLauncherShape | 10 | Launcher branding fills square/round bounds. Needs visual design and real API24/26/33+ device verification. |
| IconDuplicates | 5 | Bitmap round/square launcher variants identical. Correct alongside branding/shape decision, not by changing merchant-facing identity during integrity fixes. |
| MonochromeLauncherIcon | 2 | Add a reviewed monochrome icon and validate themed launcher behavior on API33+. |
| AutoboxingStateCreation (information) | 1 | Scanner timestamp can use mutableLongStateOf without changing scan cooldown. |

Compiler deprecations and compileSdk35/AGP8.3.2 compatibility warnings are separately retained in build logs. They are not counted as lint errors. Security policy checks are also separate and do not substitute for lint or dependency advisory scanning.

Stage7 verified changes: removed all six unused Seller colors and the unused backup subtitle after source/resource/dynamic lookup review; removed the byte-identical drawable-v24 copy while retaining the base icon; switched the scanner cooldown timestamp to mutableLongStateOf. No branding, backup-policy attribute, dependency warning or leak warning was suppressed.

Final stage7-final.log:24 warnings (17 launcher branding/themed-icon,4 incompatible dependency registries,2 Context-typed ViewModel fields,1 intentional API31+ backup attribute). All7 unused resource warnings, the obsolete folder warning and scanner information are gone. The intermediate stage7-verified.log still reports the empty v24 directory; removing that empty directory and re-running lint produced the final24. No lint XML was manually edited.


## Stage 9 continuation

See [Stage 9 partial evidence](stage9-progress.md). PDF export mechanics are updated but device/hosted success is unverified. Atomic sale commands, transactional outbox and next-schema migration remain unimplemented. Release readiness remains **FAIL**; earlier results are not new validation.


## Stage 10 implementation

[Atomic Seller commands and evidence](stage10-atomic-sales.md) supersede the earlier statement that Seller sale commands are unimplemented. Creation/edit/deletion screen paths now use one Room aggregate transaction, with144 passing JVM tests and unchanged passing coverage gates. Distributed sync/outbox guarantees remain Stage11 work; overall release readiness remains FAIL.


## Stage 11 current evidence

The Stage11 final lint/build evidence is recorded in stage11-outbox.md. An initial run reported25 warnings: the new UseTomlInstead warning came from the work-testing dependency declaration. It was moved to the existing version catalog using the same workmanager version. No suppression or lint gate reduction was added. Existing dependency lint-registry incompatibilities, context warnings and launcher warnings remain; skipped incompatible dependency checks do not establish a clean static analysis of those checks.

[Implementation, test matrix and evidence](stage11-outbox.md).

Final lint result after the version-catalog repair: **0 errors,24 warnings**. See evidence/continuation/stage11/final-lint-summary.json; incompatible dependency lint registries remain among the warnings.
