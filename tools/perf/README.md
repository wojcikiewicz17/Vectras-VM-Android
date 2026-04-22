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
- `bench/results/perf/manifest.json`
- `bench/results/perf/comparison.json`
- `bench/results/perf/flag_decisions.json`

## Contrato de runs (fonte única)
- Fonte única: `tools/perf/profiles.json` em `ci_contract.runs`.
- Validação automática: `python3 tools/perf/resolve_contract.py --require-consistency --validate-workflows` falha se o contrato estiver inválido ou se os workflows canônicos não consumirem o contrato.
- Workflows canônicos (`host-ci.yml` e `quality-gates.yml`) resolvem `perf_runs` desse contrato antes de executar `run_suite.py`.
- `suite.json` publica `ci_contract.runs` e `manifest.json` publica `runs`; o quality gate valida consistência desse valor em todos os manifests/suites coletados (incluindo conteúdo de `.zip`).

## Gate low-level
Use `tools/perf/enforce_lowlevel_gate.py` com base/head sha para bloquear alteração low-level sem neutralidade/ganho dentro da margem.
