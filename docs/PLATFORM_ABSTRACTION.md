# Platform Abstraction

- Core sources: `engine/rmr/`
- Platform hooks: `engine/platform/android`, `engine/platform/linux`

Rules:
- Android JNI uses hosted libc path, never baremetal flags.
- Linux host tooling is isolated through host CI and root CMake host targets.
- platform modules are included by root CMake platform detection.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
