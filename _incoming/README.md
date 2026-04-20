<!-- DOC_ORG_SCAN: 2026-04-20 | source-scan: incoming-pending-separated -->

# _incoming

Diretório de triagem manual de patchsets legados.

Status atual:
- itens **integrados** permanecem rastreados em `docs/INCOMING_INGESTION_MAP.md`;
- itens **pendentes** foram movidos para `_incoming/pending/`;
- promoção para build só pode ocorrer após migração para diretórios canônicos (`engine/rmr/src`, `engine/rmr/interop`, `bug/core`, `app/src/main`);
- `_incoming/` não deve ser usado como origem direta de compilação.
