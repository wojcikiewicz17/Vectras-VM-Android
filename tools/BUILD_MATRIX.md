# Build Matrix

## Canonical profiles
- `official_arm64`
  - `APP_ABI_POLICY=arm64-only`
  - `SUPPORTED_ABIS=arm64-v8a`
- `internal_arm32_arm64`
  - `APP_ABI_POLICY=arm32-arm64`
  - `SUPPORTED_ABIS=arm64-v8a,armeabi-v7a`

## Matrix lanes
- Host lane: CMake + Ninja + `run_selftest` target.
- Android lane: Gradle wrapper script `tools/gradle_with_jdk21.sh`.
- Quality lane: static CI validations (`verify_cmake_config`, matrix/ordering scripts).

## Determinism notes
- all matrix definitions live under `.github/workflows/` and are validated by
  `tools/ci/validate_build_matrix.py`.
