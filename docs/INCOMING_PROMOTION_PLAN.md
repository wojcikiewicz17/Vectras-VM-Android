# Incoming Promotion Plan

Este documento define o plano mínimo para promover código de `_incoming/` para trilhas canônicas (`engine/`, `app/src/main/cpp/` ou módulos de runtime).

## Contrato de promoção

1. Registrar/atualizar a linha do arquivo em `docs/INCOMING_INGESTION_MAP.md` com status e plano.
2. Para promover (`integrado`), apontar `target_path` para arquivo real no repositório e incluir patch funcional.
3. Executar validações de CI locais e no GitHub Actions, incluindo `tools/ci/check_incoming_ingestion.py`.
4. Atualizar documentação técnica relevante (`engine/README.md` e/ou `docs/CONTRIBUTING.md`) quando o contrato mudar.

## Estados

- `pendente`: sem promoção iniciada; requer plano de implementação.
- `em_avaliacao`: em revisão técnica/arquitetural; promoção ainda bloqueada.
- `integrado`: promoção concluída com `target_path` existente e validações aprovadas.
- `descartado`: não será promovido; manter justificativa no PR/commit de decisão.
