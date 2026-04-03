# Android CI canonical mode matrix

Este é o ponto único de documentação dos modos Android usados por `.github/workflows/android-verified.yml` (fluxo canônico) e acionados por `.github/workflows/pipeline-orchestrator.yml`.

## Modelo de pipeline Android (fonte de verdade)

Existe **um único fluxo canônico**:

1. **internal validation (unsigned explícito)**
   `android-verified.yml` com `signing_mode=unsigned`, para validação interna com trilha explícita.
2. **official verified (signed/auto)**
   `android-verified.yml` com `signing_mode=auto` (ou `signed`) após sucesso da trilha interna.

Wrappers manuais/reutilizáveis (`android.yml` e `android-build-manual.yml`) apenas delegam para o fluxo canônico via `workflow_dispatch`/`workflow_call`, sem semântica paralela de build.

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
