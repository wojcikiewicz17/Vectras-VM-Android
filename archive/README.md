<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# archive/

## Camada 1 — Propósito do diretório
Material experimental/histórico fora do fluxo primário de build.

## Camada 2 — Estrutura (até 3 níveis)
- Nível 1: `archive/`
- Nível 2: `experimental/`, `root-history/`
- Nível 3: detalhamento por arquivo em [`FILES_MAP.md`](FILES_MAP.md).

## Camada 3 — Arquivos e vínculos
- Catálogo completo: [`FILES_MAP.md`](FILES_MAP.md)
- Contexto global de camadas: [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md)

## Cadeia de comando (lógica de inspeção)
```bash
find archive -maxdepth 3 -type d | sort
sed -n '1,120p' archive/FILES_MAP.md
```
