# bench/

## Objetivo
Diretório de benchmark isolado para execução e armazenamento de resultados de medição do núcleo nativo.

## Estrutura de arquivos
- `src/rmr_benchmark_main.c`: entrada C para benchmark.
- `scripts/run_bench.sh`: script de execução automatizada.
- `results/`: artefatos de saída e placeholders versionados.

## Conceitos principais
1. **Benchmark reprodutível**: separação entre código de medição e resultados.
2. **Pipeline simples de execução**: script shell para facilitar repetição em ambiente local/CI.
