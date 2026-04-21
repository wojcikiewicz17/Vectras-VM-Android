# tools/perf

Suíte de microbenchmarks reproduzível por ABI/política para CI.

## O que mede por execução
- latência p50/p95 (ms)
- throughput (score/s)
- tamanho de binário (`build/bench/rmr_bench`)
- cold start / warm start (ms)
- counters de cache miss (`perf stat`) quando disponível

## Fluxo canônico
```bash
python3 tools/perf/run_suite.py
python3 tools/perf/compare_baseline.py --margin 0.03
python3 tools/perf/derive_flag_decisions.py
```

Saídas:
- `bench/results/perf/suite.json`
- `bench/results/perf/suite.csv`
- `bench/results/perf/comparison.json`
- `bench/results/perf/flag_decisions.json`

## Gate low-level
Use `tools/perf/enforce_lowlevel_gate.py` com base/head sha para bloquear alteração low-level sem neutralidade/ganho dentro da margem.
