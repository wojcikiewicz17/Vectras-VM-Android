<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# reports/

## Camada 1 — Propósito do diretório
Relatórios técnicos e métricas vigentes.

## Camada 2 — Estrutura (até 3 níveis)
- Nível 1: `reports/`
- Nível 2: relatórios ativos de build/validação
- Nível 3: detalhamento por arquivo em [`FILES_MAP.md`](FILES_MAP.md).

## Camada 3 — Arquivos e vínculos
- Catálogo completo: [`FILES_MAP.md`](FILES_MAP.md)
- Índice canônico transversal: [`docs/INDEX_CANONICAL.md`](../docs/INDEX_CANONICAL.md)
- Evidências brutas e auditoria superseded: diretórios `evidence/` e `docs/archive/`

## Cadeia de comando (lógica de inspeção)
```bash
find reports -maxdepth 3 -type d | sort
sed -n '1,120p' reports/FILES_MAP.md
```
