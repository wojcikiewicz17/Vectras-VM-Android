<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# resources/

## Camada 1 — Propósito do diretório
Ativos compartilhados android/web/lang.

## Camada 2 — Estrutura (até 3 níveis)
- Nível 1: `resources/`
- Nível 2: `android/`, `lang/`, `web/`
- Nível 3: detalhamento por arquivo em [`FILES_MAP.md`](FILES_MAP.md).

## Camada 3 — Arquivos e vínculos
- Catálogo completo: [`FILES_MAP.md`](FILES_MAP.md)
- Contexto global de camadas: [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md)

## Cadeia de comando (lógica de inspeção)
```bash
find resources -maxdepth 3 -type d | sort
sed -n '1,120p' resources/FILES_MAP.md
```
