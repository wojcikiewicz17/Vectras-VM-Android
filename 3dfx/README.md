<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# 3dfx/

## Camada 1 — Propósito do diretório
Artefatos ISO de wrappers 3dfx para cenários de compatibilidade.

## Camada 2 — Estrutura (até 3 níveis)
- Nível 1: `3dfx/`
- Nível 2: Sem subdiretórios de primeiro nível relevantes.
- Nível 3: detalhamento por arquivo em [`FILES_MAP.md`](FILES_MAP.md).

## Camada 3 — Arquivos e vínculos
- Catálogo completo: [`FILES_MAP.md`](FILES_MAP.md)
- Contexto global de camadas: [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md)

## Cadeia de comando (lógica de inspeção)
```bash
find 3dfx -maxdepth 3 -type d | sort
sed -n '1,120p' 3dfx/FILES_MAP.md
```
