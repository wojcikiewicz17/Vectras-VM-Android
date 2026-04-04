# Android CI canonical mode matrix

Este é o ponto único de documentação dos perfis Android usados por `.github/workflows/android.yml` e acionados por `.github/workflows/pipeline-orchestrator.yml`.

## Modelo de pipeline Android (fonte de verdade)

Existe **um único workflow Android canônico** (`android.yml`) com seleção por `run_workfile`:

1. `smoke_debug` → `:app:assembleDebug`
2. `unit_loader` → `:shell-loader:testDebugUnitTest`
3. `full_debug` → build + testes unitários de app/terminal-emulator/shell-loader
4. `release_gate` → build/testes unitários de release

## Matriz de logs (`log_level`)

| log_level | flags Gradle | uso recomendado |
|---|---|---|
| `lifecycle` | `--console=plain` | execução mais limpa/curta |
| `info` | `--console=plain --info` | padrão para CI diária |
| `debug` | `--console=plain --debug --stacktrace` | diagnóstico profundo |

## Flags complementares

- `build_variant`: `debug`, `release` ou `both`.
- `signing_mode`: `auto`, `signed` ou `unsigned`.
- `run_lint`: controla execução de `:app:lintDebug`.
- `run_native_checks`: controla validação de contrato Make/CMake.

> Nota: o orquestrador seleciona pipeline (`host_only`, `android_only`, `full`) e repassa `run_workfile`/`log_level` para reduzir drift entre execução manual e execução automática.
