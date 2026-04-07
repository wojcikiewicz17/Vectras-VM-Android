# Provisioning Report (2026-04-03)

## Environment Provisioned

- JDK: OpenJDK 21 (`/usr/lib/jvm/java-21-openjdk-amd64`)
- Android SDK root: `/workspace/android-sdk`
- Installed Android packages:
  - `platforms;android-35`
  - `build-tools;35.0.0`
  - `ndk;27.2.12479018`
  - `cmake;3.22.1`

## Exports used

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:/workspace/android-sdk/cmdline-tools/latest/bin:$PATH"
export ANDROID_SDK_ROOT=/workspace/android-sdk
export ANDROID_HOME=/workspace/android-sdk
```

## local.properties

`local.properties` was generated for local builds with:

```properties
sdk.dir=/workspace/android-sdk
```

## Build/Validation Run

- Canonical validation command attempted:
  - `./tools/gradle_with_jdk21.sh checkNativeAllMatrix`
- Result: failed due required Firebase release configuration (`google-services.json` for release variant).
- Debug build attempted:
  - `./tools/gradle_with_jdk21.sh :app:assembleDebug`
- Result: failed with existing compilation errors in repository sources (duplicate methods/symbol resolution issues), unrelated to SDK/JDK provisioning.

## Revalidation (2026-04-03, requested steps)

- `./tools/gradle_with_jdk21.sh --version`: **OK** (Gradle 8.7 under JDK 21).
- `./tools/gradle_with_jdk21.sh :app:assembleDebug`: **failed** due existing Java compile errors in source tree:
  - `app/src/main/java/com/vectras/qemu/utils/RamInfo.java`: duplicate method `ensureMinimumVmMemoryMb(int)`.
  - `app/src/main/java/com/vectras/qemu/utils/RamInfo.java`: unresolved symbol `MIN_VM_MEMORY_MB`.
