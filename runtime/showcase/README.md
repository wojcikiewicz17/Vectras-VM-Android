<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Runtime Showcase

Este diretório representa a camada de showcase do ecossistema RAFAELIA.

No estado atual, o runtime Android principal permanece em `app/` (com módulos de suporte)
para minimizar risco de quebra de build/CI do projeto legado.

Próximo passo de hard split:
- mover gradualmente app e módulos para `runtime/vectras-android/` com ajustes em `settings.gradle`.
