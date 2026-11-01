# Seller lint triage

Stage6 measured lint: 0 errors, 32 warnings, 1 information. No baseline or suppression was added. The warning count is not zero and four dependency registries are skipped by lint; successful lint execution does not mean those missing checks ran.

| Finding | Count | Decision / next validation |
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
