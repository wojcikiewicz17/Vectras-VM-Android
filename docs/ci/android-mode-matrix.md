# Android CI canonical mode matrix

Este é o ponto único de documentação dos modos Android usados por `.github/workflows/android-verified.yml` e acionados por `.github/workflows/pipeline-orchestrator.yml`.

## Trilha canônica no orquestrador

1. **baseline** (`mode=fast`, `run_lint=false`, `run_native_matrix=false`)  
   Objetivo: validação rápida de compilação e smoke checks.
2. **verified** (`mode=<input>`, `run_lint=true`, `run_native_matrix=true`)  
   Objetivo: validação padrão de qualidade/promovida após baseline bem-sucedido.
3. **minimal (opcional)** (`mode=fast`, `run_lint=false`, `run_native_matrix=false`)  
   Objetivo: rerun leve pós-verified quando solicitado manualmente (`promote_android_minimal=true`).

## Matriz de modos (`mode`)

| mode | unit tests | gradle logs | deep diagnostics | uso recomendado |
|---|---:|---|---:|---|
| `fast` | não | `--console=plain` | não | baseline/smoke |
| `moderado` | sim | `--stacktrace` | não | PR padrão |
| `profundo` | sim | `--stacktrace --info` | sim | validação aprofundada |
| `ultra_minucioso` | sim | `--stacktrace --info` | sim | investigação extensa |
| `diagnostico` | sim | `--stacktrace --info` | sim | troubleshooting CI |
| `interoperavel` | sim | `--stacktrace --info` | sim | compatibilidade transversal |
| `confiavel` | sim | `--stacktrace --info` | sim | hardening de confiabilidade |
| `portavel` | sim | `--stacktrace --info` | sim | portabilidade |
| `hardening` | sim | `--stacktrace --info` | sim | endurecimento release |
| `matrix_total` | sim | `--stacktrace --info` | sim | cobertura máxima |
| `forense` | sim | `--stacktrace --info` | sim | análise forense |

## Flags complementares

- `build_variant`: `debug`, `release` ou `both`.
- `run_lint`: controla execução de `lintDebug`.
- `run_native_matrix`: controla execução de `checkNativeAllMatrix`.

> Nota: `run_lint` e `run_native_matrix` são flags de perfil e podem ser sobrescritas pelo orquestrador sem criar workflows Android paralelos.
