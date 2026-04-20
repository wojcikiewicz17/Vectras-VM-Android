# Compilation Fixes (JNI-first alignment)

Date: 2026-04-20

## Applied structural fixes
- Android native path standardized as JNI/hosted (`RMR_JNI_BUILD=1`) and explicit removal of baremetal assumptions for Android platform module.
- Introduced platform modules:
  - `engine/platform/android/CMakeLists.txt`
  - `engine/platform/linux/CMakeLists.txt`
- Added Android compatibility allocator/runtime shim for JNI:
  - `engine/platform/android/rmr_android_compat.c`
- Root CMake now computes and exports `VECTRA_HAS_CASM_MARKER` consistently and logs enabled/disabled state.
- Host executables/selftests in root CMake are now guarded under `if(NOT ANDROID)`.
- Source manifest moved `rmr_baremetal_compat.c` from core group to host-only group.
- Added CI helper validation script:
  - `tools/ci/verify_cmake_config.sh`
- BUILDING documentation aligned with active branch ABI defaults (`arm32-arm64`) and JNI-first model.
