<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# demo_cli/

## Camada 1 — Propósito do diretório
Demonstrações CLI e auto-testes de componentes nativos.

## Camada 2 — Estrutura (até 3 níveis)
- Nível 1: `demo_cli/`
- Nível 2: `src/`
- Nível 3: detalhamento por arquivo em [`FILES_MAP.md`](FILES_MAP.md).

## Camada 3 — Arquivos e vínculos
- Catálogo completo: [`FILES_MAP.md`](FILES_MAP.md)
- Contexto global de camadas: [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md)

## Cadeia de comando (lógica de inspeção)
```bash
find demo_cli -maxdepth 3 -type d | sort
sed -n '1,120p' demo_cli/FILES_MAP.md
```
