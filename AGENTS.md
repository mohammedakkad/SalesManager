# AGENTS.md

## Cursor Cloud specific instructions

### Overview

This is a native Android app ("Sales Manager" / مدير المبيعات) built with Kotlin + Jetpack Compose. Single module (`:app`), no backend services — the app talks directly to Firebase (Realtime Database, Crashlytics, Analytics).

### Prerequisites

- **JDK 21** (pre-installed on the VM)
- **Android SDK** with `platforms;android-35`, `build-tools;35.0.0`, and `platform-tools` installed at `/opt/android-sdk`
- `ANDROID_HOME=/opt/android-sdk` must be set (added to `~/.bashrc`)
- `local.properties` with `sdk.dir=/opt/android-sdk` must exist at repo root (gitignored, recreated by update script)

### google-services.json

The `google-services.json` file is **gitignored** and **required** for the build to succeed (the `com.google.gms.google-services` plugin fails without it). A placeholder is created by the update script at `app/google-services.json` if it doesn't already exist. This placeholder allows compilation but Firebase features (activation, sync) will not work at runtime. To use real Firebase, provide a valid `google-services.json` via the `GOOGLE_SERVICES_JSON` secret.

### Build / Lint / Test commands

| Task | Command |
|------|---------|
| Debug build | `./gradlew assembleDebug` |
| Release build | `./gradlew assembleRelease` (requires keystore) |
| Lint | `./gradlew lint` |
| Check (all) | `./gradlew check` |
| Install on device | `./gradlew installDebug` |

There are **no unit or instrumentation tests** in the codebase.

### Running the emulator

KVM is **not available** in Cloud Agent VMs. The Android emulator must be started with `-no-accel` for software emulation:

```bash
export ANDROID_HOME=/opt/android-sdk
export PATH=$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH
emulator -avd test_device -no-window -no-audio -gpu swiftshader_indirect -no-snapshot -no-accel -memory 2048
```

**Warning:** Software emulation is extremely slow. Boot takes ~10-15 minutes. APK installation takes ~1 minute. Consider avoiding emulator-based testing unless strictly necessary.

### Gradle wrapper

The `gradle-wrapper.jar` is gitignored. The update script regenerates it using a locally downloaded Gradle 8.9 distribution. If the wrapper jar is missing, build will fail with a misleading `ClassNotFoundException: "-Xmx64m"` error.

### Known build warnings

- `compileSdk = 35` triggers a warning about AGP 8.5.2 only being tested up to compileSdk 34. This is safe to ignore.
- Several deprecated API usages (`Icons.Filled.ArrowBack`, `Modifier.menuAnchor()`) produce Kotlin warnings. These are cosmetic.
