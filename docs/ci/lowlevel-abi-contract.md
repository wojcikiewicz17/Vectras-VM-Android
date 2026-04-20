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
