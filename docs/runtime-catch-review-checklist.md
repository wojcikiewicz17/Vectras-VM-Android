<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Runtime/Setup Catch Review Checklist

Use este checklist em PRs que alterem classes em `app/src/main/java/com/vectras/vm/**` e `app/src/main/java/com/vectras/qemu/**`.

- [ ] Nenhum `catch` vazio/silencioso (`ignored`) foi introduzido sem justificativa técnica documentada no código.
- [ ] Cada `catch` registra `Log.w` ou `Log.e` com **contexto operacional** (`action`, `key`, exceção).
- [ ] O padrão de correlação foi usado: `tag=VectrasRuntime` e código `[VRT-XXXX]`.
- [ ] Onde impacta fluxo de setup/runtime para usuário, erro foi propagado em mensagem técnica resumida para a UI.
- [ ] Fallback funcional anterior foi preservado (sem regressão de comportamento).

> Exceções permitidas: blocos estritamente best-effort precisam comentário de justificativa explícita e referência de risco aceito.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
