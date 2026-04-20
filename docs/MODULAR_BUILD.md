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

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
