# Updated audit environment instructions — draft save BLOCKED

Use the existing checkout /workspace/SalesManager and branch codex/seller-full-coverage-audit. Tasks already run in isolation; do not create a worktree unless explicitly requested. Preserve completed work and existing PR51. Setup alone must not modify tracked application/test/dependency files; the user's audit request separately authorizes coding.

The tested install.sh preserves repository files and existing Firebase client configuration, verifies tool archives and prepares full JDK17, Gradle8.9, SDK35/build-tools34 plus /workspace/setup/continuation/gradle. The helper preserves inherited proxy and CA trust, uses four workers and avoids unrelated admin signing configuration through configuration-on-demand. Network/local sockets may require execution outside the shell sandbox.

Run from the checkout:
```bash
/workspace/setup/continuation/gradle --continue \
  :app:qaCoverage :salesmanager:qaCoverage :salesmanager:lintDebug \
  :salesmanager:assembleDebug :salesmanager:assembleDebugAndroidTest :salesmanager:assembleRelease \
  -x :salesmanager:uploadCrashlyticsMappingFileRelease
python3 scripts/report-seller-coverage.py --check
python3 scripts/check-seller-secrets.py
```

app is the shared Android library; salesmanager is the Seller application. Do not follow the old instructions targeting app APK output. Inspect actual command status and fresh JUnit XML; R8 can take several minutes. Never upload Crashlytics mappings, use exposed signing material or access production Firebase for QA. Existing client configuration is for packaging only and must not be displayed or overwritten. Device tests require disposable offline configuration and OfflineQaRunner; generate it only in a disposable checkout. See qa/pdf-verifier/README.md for real device-generated PDF fixtures. No web server or localhost preview is applicable.

This instance supports JVM/Room tests, JaCoCo, lint and debug/minified unsigned builds. adb reports no devices and /dev/kvm is absent: device/native PDF behavior remains blocked. Git fetch/push works; PR51 GitHub API inspection returns Forbidden. Never ask for token contents in chat. Historical schemas1–21 and owner signing rotation remain external blockers. Read all seven docs/qa reports beginning with seller-release-readiness.md before continuing.

The configuration service rejected the attempted install_script/start_skill update: "This draft cannot be edited in its current state. It may already have been published, or a publication operation may still be in progress." A reread returned the same revision3 draft. Persistence/publication of this update is NOT confirmed. These complete proposed instructions and tested installer are preserved here for transfer to a new setup chat from environment settings. Existing saved fields remain stale; no network allowlist or secret binding was replaced.
