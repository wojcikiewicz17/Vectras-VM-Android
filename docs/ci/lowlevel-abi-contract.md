# Lowlevel ABI Contract — CI Gate

Documento normativo da camada crítica: `docs/abi/lowlevel_abi_contract.md`.

Fonte canônica para validação automatizada:
- `tools/ci/lowlevel_abi_contract.json`
- `tools/ci/validate_lowlevel_abi_contract.py`

## Gates obrigatórios

O contrato é bloqueante em:
- `.github/workflows/android-ci.yml`
- `.github/workflows/compile-matrix.yml`
- `.github/workflows/quality-gates.yml`

e também na trilha local de build:
- Gradle `:app:preBuild` via task `validateCriticalNativeAbiLayer`
- CMake target `lowlevel_abi_contract_check` dependência de `vectra_core_accel`

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
