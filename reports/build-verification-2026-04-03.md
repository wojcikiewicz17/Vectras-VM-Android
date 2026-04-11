<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Build Verification Report — 2026-04-03

## Canonical command discovery

Primary source: `BUILDING.md`.

Canonical Gradle wrapper command for this repository:

```bash
./tools/gradle_with_jdk21.sh :app:assembleDebug --stacktrace
```

## Environment preparation executed

```bash
apt-get update -y
apt-get install -y openjdk-21-jdk wget unzip
```

```bash
export ANDROID_SDK_ROOT=/workspace/android-sdk
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -o commandlinetools-linux-11076708_latest.zip
yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --licenses
sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
  'platform-tools' \
  'platforms;android-35' \
  'build-tools;35.0.0' \
  'ndk;27.2.12479018' \
  'cmake;3.22.1'
printf 'sdk.dir=%s\n' "$ANDROID_SDK_ROOT" > local.properties
```

## Compilation attempts and evidence

Attempt 1:

```bash
./tools/gradle_with_jdk21.sh clean :app:assembleDebug --stacktrace
```

Result: **FAILED** in `:app:compileDebugJavaWithJavac`.

Attempt 2 (focused Java compile to isolate blockers):

```bash
./tools/gradle_with_jdk21.sh :app:compileDebugJavaWithJavac --stacktrace
```

Result: **FAILED** with source-level errors.

### Critical blocker categories

1. **Missing types/APIs referenced by current source set**
   - `ProcessBudgetRegistry.SlotToken`
   - `ProcessBudgetRegistry.Snapshot`
   - `ProcessLaunch.LaunchTicket`
   - `ProcessLaunch.LaunchLease`
   - multiple missing helper methods in setup flow classes.

2. **Duplicate method declarations (compile-time conflicts)**
   - `PermissionUtils` duplicate methods (`canDrawOverlays`, `isIgnoringBatteryOptimizations`)
   - `TermuxInstaller` duplicate bootstrap helper methods.

3. **Signature mismatch across callsites**
   - `ProcessLaunch.withBudget(...)` invocation does not match current method signature.

4. **Large unresolved symbol set in setup wizard and feature core files**
   - `SetupWizard2Activity` and `SetupFeatureCore` contain extensive unresolved references and one duplicate local variable declaration.

## Artifacts/log evidence paths

- `/tmp/build-debug.log`
- `/tmp/compile-java.log`

These logs contain the executed Gradle tasks and compilation failures.

## Delivery status

Current repository state **is not deliverable as compiled Android app** because canonical build fails at Java compilation stage.

