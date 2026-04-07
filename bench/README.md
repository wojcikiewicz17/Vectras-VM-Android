<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# bench/

## Camada 1 — Propósito do diretório
Benchmarks dedicados para medição reproduzível.

## Camada 2 — Estrutura (até 3 níveis)
- Nível 1: `bench/`
- Nível 2: `results/`, `scripts/`, `src/`
- Nível 3: detalhamento por arquivo em [`FILES_MAP.md`](FILES_MAP.md).

## Camada 3 — Arquivos e vínculos
- Catálogo completo: [`FILES_MAP.md`](FILES_MAP.md)
- Contexto global de camadas: [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md)

## Cadeia de comando (lógica de inspeção)
```bash
find bench -maxdepth 3 -type d | sort
sed -n '1,120p' bench/FILES_MAP.md
```
