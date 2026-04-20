<!-- DOC_ORG_SCAN: 2026-04-20 | source-scan: android-ci-canonicalization -->

# Android pipeline canônica

## Decisão

A pipeline Android canônica do repositório é **`.github/workflows/android-ci.yml`**.

## Racional

1. Centraliza contratos de build Android (Gradle, SDK/NDK/CMake/JDK, ABI policy e validações).
2. Elimina duplicação de lógica entre workflows acionáveis e reutilizáveis.
3. Mantém `pipeline-orchestrator.yml` dependente de um único workflow Android para reduzir drift.

## Regras operacionais

- `android.yml` permanece como **wrapper de entrada** (push/pull_request/workflow_dispatch), delegando exclusivamente para `android-ci.yml` via `uses`.
- Qualquer evolução de comportamento Android (ex.: perfis de matriz nativa) deve ocorrer apenas em `android-ci.yml`.
- `quality-gates.yml` + `tools/ci/validate_build_matrix.py` bloqueiam regressões que reintroduzam workflows Android concorrentes com responsabilidades sobrepostas.
