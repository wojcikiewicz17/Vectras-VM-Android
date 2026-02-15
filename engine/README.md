# engine/

## Camada 1 — Propósito do diretório
Núcleo nativo C/Rust para políticas e performance.

## Camada 2 — Estrutura (até 3 níveis)
- Nível 1: `engine/`
- Nível 2: `rmr/`, `vectra_policy_kernel/`
- Nível 3: detalhamento por arquivo em [`FILES_MAP.md`](FILES_MAP.md).

## Camada 3 — Arquivos e vínculos
- Catálogo completo: [`FILES_MAP.md`](FILES_MAP.md)
- Contexto global de camadas: [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md)

## Cadeia de comando (lógica de inspeção)
```bash
find engine -maxdepth 3 -type d | sort
sed -n '1,120p' engine/FILES_MAP.md
```

## Core RMR unificado
- O ponto único de verdade do core nativo agora é `engine/rmr/include/rmr_unified_kernel.h`.
- A implementação correspondente está em `engine/rmr/src/rmr_unified_kernel.c`, encapsulando policy kernel, autodetect de hardware, bitraf e corelib por uma API pública estável.
