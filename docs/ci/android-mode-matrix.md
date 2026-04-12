<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

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
- `abi_profile`: `official_arm64` (trilha oficial no workflow canônico).
- `run_lint`: controla execução de `:app:lintDebug`.
- `run_native_checks`: controla validação de contrato Make/CMake + matriz Android CMake (`armeabi-v7a` e `arm64-v8a`).

### Regras de assinatura e segurança de entrega

- `build_variant=release|both` mantém o fluxo de release e respeita `signing_mode`.
- `signing_mode=signed`: força `-PciRelease=true`, exige segredos de assinatura e executa `prepare_release_signing.sh`.
- `signing_mode=auto`: para release, resolve para `signed` (comportamento padrão oficial).
- `signing_mode=unsigned`: permite compilar release sem assinatura (`-PciRelease=false`) para validação técnica interna e upload de artefatos não distribuíveis.
- Para trilha oficial de distribuição, mantenha `signing_mode=signed` (ou `auto`) com segredos `VECTRAS_RELEASE_*` configurados.
- `abi_profile=official_arm64`: injeta `APP_ABI_POLICY=arm64-only` e `SUPPORTED_ABIS=arm64-v8a`.
- validação `internal-5abi` é trilha técnica separada (execução manual/diagnóstico), não caminho canônico de release no workflow principal.
- Em `build_variant=release|both`, o passo `prepare_release_signing.sh` só executa quando `signing_mode_effective=signed`.
- O workflow publica artefato adicional `android-cmake-matrix-*` com saída nativa por ABI (`armeabi-v7a`/`arm64-v8a`) para acompanhamento de compilação low-level via CI.
- `run_native_checks=true` agora compila o build CMake e executa `verify_contracts` antes da etapa Android para estabilizar a cadeia nativa.
- O workflow executa `tools/ci/validate_pipeline_directories.sh --profile android` antes da configuração de toolchain para falhar cedo em divergências estruturais de diretórios/arquivos.

> Nota: o orquestrador seleciona pipeline (`host_only`, `android_only`, `full`) e repassa `run_workfile`/`log_level` para reduzir drift entre execução manual e execução automática.
