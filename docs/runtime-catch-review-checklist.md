# Runtime/Setup Catch Review Checklist

Use este checklist em PRs que alterem classes em `app/src/main/java/com/vectras/vm/**` e `app/src/main/java/com/vectras/qemu/**`.

- [ ] Nenhum `catch` vazio/silencioso (`ignored`) foi introduzido sem justificativa técnica documentada no código.
- [ ] Cada `catch` registra `Log.w` ou `Log.e` com **contexto operacional** (`action`, `key`, exceção).
- [ ] O padrão de correlação foi usado: `tag=VectrasRuntime` e código `[VRT-XXXX]`.
- [ ] Onde impacta fluxo de setup/runtime para usuário, erro foi propagado em mensagem técnica resumida para a UI.
- [ ] Fallback funcional anterior foi preservado (sem regressão de comportamento).

> Exceções permitidas: blocos estritamente best-effort precisam comentário de justificativa explícita e referência de risco aceito.
