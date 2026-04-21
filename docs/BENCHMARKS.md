<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# BENCHMARKS (RMR Engine)

## Build rápido
```bash
make clean && make all
```

## Selftest da API Bitraf
```bash
make run-selftest
```

## Rodar 1 execução
```bash
./build/bench/rmr_bench bench/results/latest.csv bench/results/latest.json
```

## Rodar N execuções + mediana/p95
```bash
bench/scripts/run_bench.sh 9 bench/results  # script shell + awk/sort (sem python)
```

Saídas:
- CSV por execução: `bench/results/run_*.csv`
- JSON por execução: `bench/results/run_*.json`
- Sumário estatístico: `bench/results/summary.json` (median/p95/min/max)

## Campos
- `score`: throughput relativo por teste
- `variance`: dispersão interna por teste
- `error_margin`: margem estimada
- `total_score`, `total_error`: agregados da suite (50 métricas)


## Gate baremetal (autodetect + integridade de árvore)
```bash
make run-baremetal-gate
```

Artefatos:
- `reports/baremetal/hw_caps.env`
- `reports/baremetal/dir_integrity_matrix.json`

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
