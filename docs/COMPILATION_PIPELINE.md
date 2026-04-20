# Compilation Pipeline

1. Resolve profile in `pipeline-orchestrator.yml`.
2. Run host build lane (`host-ci.yml`) when enabled.
3. Run Android lane (`android-ci.yml`) when enabled.
4. Enforce cross-lane checks (`quality-gates.yml`).

Android builds must use `tools/gradle_with_jdk21.sh`; host builds must pass
CMake configure + `run_selftest` target.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
