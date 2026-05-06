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

## CI/Workflow modularization (follow-up)
- Added reusable workflows for host/android/quality and deterministic compile matrix.
- Updated orchestrator to dispatch reusable jobs by profile.
- Added docs and static validation scripts for workflow/build-matrix consistency.

## CI canonical reference (Android/Host)

- Canonical Android pipeline: `.github/workflows/android-ci.yml`.
- Android wrapper entrypoint: `.github/workflows/android.yml`.
- Auxiliary Android ABI compatibility matrix: `.github/workflows/compile-matrix.yml`.
- Canonical host pipeline: `.github/workflows/host-ci.yml`.
- Orchestration and final gates: `.github/workflows/pipeline-orchestrator.yml` and `.github/workflows/quality-gates.yml`.
- Canonical matrix documentation: `docs/ci/workflow-matrix.md`.

## Build local arm32+arm64 (release)

Use `tools/ci/local_build_arm32_arm64.sh` to execute canonical release build path locally.

```bash
# unsigned internal validation build
./tools/ci/local_build_arm32_arm64.sh unsigned

# signed official-style build (requires signing secrets exported)
./tools/ci/local_build_arm32_arm64.sh signed
```
