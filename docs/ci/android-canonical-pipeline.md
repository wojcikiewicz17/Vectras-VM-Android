<!-- DOC_ORG_SCAN: 2026-04-20 | source-scan: android-ci-canonicalization -->

# Android pipeline canônica

## Decisão

A pipeline Android canônica do repositório é **`.github/workflows/android-ci.yml`**.

## Racional

1. Centraliza contratos de build Android (Gradle, SDK/NDK/CMake/JDK, ABI policy e validações).
2. Elimina duplicação de lógica entre workflows acionáveis e reutilizáveis.
3. Mantém `pipeline-orchestrator.yml` dependente de um único workflow Android para reduzir drift.

## Regras operacionais

- `android.yml` permanece como **wrapper de entrada** (push/pull_request/workflow_dispatch), delegando para `android-ci.yml` e acionando `compile-matrix.yml` como gate adaptativo em trilhas internas/`compat_matrix_debug`.
- Evoluções de contrato ABI lowlevel devem atualizar `tools/ci/lowlevel_abi_contract.json` e o gate `tools/ci/validate_lowlevel_abi_contract.py`.
- `quality-gates.yml` + `tools/ci/validate_build_matrix.py` bloqueiam regressões que reintroduzam workflows Android concorrentes com responsabilidades sobrepostas.


## Alinhamento de callers Android

- `pipeline-orchestrator.yml`, `android.yml` e `compile-matrix.yml` devem chamar apenas `android-ci.yml` para qualquer responsabilidade de build Android.
- `android.yml` e `compile-matrix.yml` ficam restritos a wrappers (entrada/compatibilidade), sem bootstrap Gradle/SDK/NDK próprio.
