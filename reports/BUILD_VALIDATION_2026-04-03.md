<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Build Validation Report — 2026-04-03

> superseded by CANONICAL_BUILD_STATUS.md

## Scope executed

1. Installed Android SDK components:
   - `ndk;27.2.12479018`
   - `cmake;3.22.1`
2. Verified alignment with `gradle.properties` (`ndk.version`, `cmake.version`).
3. Executed:
   - `./tools/gradle_with_jdk21.sh :app:assembleDebug --stacktrace`
   - `./tools/gradle_with_jdk21.sh :app:assembleRelease --stacktrace`
   - `./tools/gradle_with_jdk21.sh :app:verifyDeliveredCompiledArtifacts --stacktrace`

## Environment notes

- Java 21 installed and active.
- Android SDK root used: `.android-sdk/` at repository root.

## Version alignment result

- `gradle.properties` `ndk.version=27.2.12479018` ✅
- Installed NDK version: `27.2.12479018` ✅
- `gradle.properties` `cmake.version=3.22.1` ✅
- Installed CMake version: `3.22.1` ✅

## Build outcomes

### `:app:assembleDebug`

**Status:** ❌ failed (codebase compile error unrelated to SDK/NDK/CMake provisioning).

Key error:

- `RamInfo.java: method ensureMinimumVmMemoryMb(int) is already defined in class RamInfo`
- `RamInfo.java: cannot find symbol MIN_VM_MEMORY_MB`

### `:app:assembleRelease`

**Status:** ❌ failed (release secret/config expected by project).

Key error:

- `:app:validateFirebaseReleaseConfig`
- `google-services.json ausente para variant release`

### `:app:verifyDeliveredCompiledArtifacts`

**Status:** ❌ failed (expected, because debug/release artifacts were not successfully assembled).

Key error:

- Missing compiled artifacts for variants `debug`, `release`, `perfRelease`.

## Conclusion

- Requested NDK and CMake versions are installed and aligned with `gradle.properties`.
- Build verification is blocked by existing repository issues:
  - Java compile errors in app sources (debug).
  - Missing release Firebase configuration (release).
