# Modular Build

This repository uses a modular split:
- orchestration: `CMakeLists.txt`
- source manifest: `engine/rmr/sources_rmr_core.cmake`
- platform modules: `engine/platform/*/CMakeLists.txt`
- CI contracts: `.github/workflows/*.yml`

Validation helpers:
- `tools/ci/verify_cmake_config.sh`
- `tools/ci/validate_build_matrix.py`
- `tools/ci/check_compilation_order.py`
