# Matriz de alinhamento diretório ↔ documentação ativa

Matriz de governança documental ativa para acompanhar, por diretório crítico, a existência de README e o alinhamento entre documentação e código.

| Diretório | README existente | Status de alinhamento com código | Último ajuste |
|---|---|---|---|
| `app/` | sim (`app/README.md`) | parcial | 2026-04-07 (criação inicial da matriz) |
| `engine/` | sim (`engine/README.md`) | parcial | 2026-04-07 (criação inicial da matriz) |
| `engine/rmr/` | sim (`engine/rmr/README.md`) | parcial | 2026-04-07 (criação inicial da matriz) |
| `demo_cli/` | sim (`demo_cli/README.md`) | parcial | 2026-04-07 (criação inicial da matriz) |
| `terminal-emulator/` | sim (`terminal-emulator/README.md`) | parcial | 2026-04-07 (criação inicial da matriz) |
| `shell-loader/` | sim (`shell-loader/README.md`) | parcial | 2026-04-07 (criação inicial da matriz) |
| `web/` | sim (`web/README.md`) | parcial | 2026-04-07 (criação inicial da matriz) |
| `tools/` | sim (`tools/README.md`) | parcial | 2026-04-07 (criação inicial da matriz) |

## Critério de status
- `ok`: documentação do diretório cobre estrutura e fluxo atual do código sem lacunas relevantes.
- `parcial`: há README, mas são esperadas revisões pontuais de sincronização com a base atual.
- `pendente`: ausência de README e/ou ausência de cobertura mínima de documentação de domínio.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
