<!-- DOC_ORG_SCAN: 2026-04-20 | source-scan: android-workflow-contract -->

# Android CI canonical mode matrix

> Matriz completa de workflows CI (host + android + orquestração): `docs/ci/workflow-matrix.md`.

Este documento descreve o **contrato Android efetivo** entre `.github/workflows/android.yml` (entrada/wrapper) e `.github/workflows/android-ci.yml` (pipeline canônica).

## Modelo canônico (fonte de verdade)

Existe **um único workflow Android canônico de build/release**: `.github/workflows/android-ci.yml`.

`android.yml` existe como camada de entrada para eventos `push/pull_request/workflow_dispatch`, resolve parâmetros e encaminha a execução para `android-ci.yml`.

## Perfis `run_workfile`

1. `smoke_debug` → `:app:assembleDebug`
2. `unit_loader` → `:shell-loader:testDebugUnitTest`
3. `full_debug` → `:app:assembleDebug :app:testDebugUnitTest :terminal-emulator:testDebugUnitTest :shell-loader:testDebugUnitTest`
4. `compat_matrix_debug` → `:app:checkNativeExtendedMatrix` + build/testes debug
5. `release_gate` → `:app:assembleRelease :app:testReleaseUnitTest :shell-loader:testReleaseUnitTest`

## Matriz de logs (`log_level`)

| log_level | flags Gradle (`android-ci.yml`) | uso recomendado |
|---|---|---|
| `lifecycle` | `-Dorg.gradle.logging.level=lifecycle` | execução mais limpa/curta |
| `info` | `-Dorg.gradle.logging.level=info` | padrão para CI diária |
| `debug` | `-Dorg.gradle.logging.level=debug` | diagnóstico profundo |

## Flags principais

- `build_variant`: `debug`, `release` ou `both`.
- `signing_mode`: `auto`, `signed` ou `unsigned`.
- `abi_profile` aceito no canônico (`android-ci.yml`):
  - `official_arm64`
  - `official_arm32_arm64`
  - `internal_arm64`
  - `internal_arm32_arm64`
  - `internal_4abi`
  - `internal_5abi` / `internal_riscv64`
  - `generic`
- `native_matrix_profile`: `canonical`, `pilot_android9_16_5arch`, `android8_16plus_allarch`.
- `run_lint`: controla execução de `:app:lintDebug`.
- `run_native_checks`: controla execução de matriz CMake Android.

## Regras de assinatura e entrega

- Release (`build_variant=release|both`) é bloqueado para `abi_profile` diferente de `official_arm64`.
- `signing_mode=auto` em release resolve para `signed`.
- `signing_mode=unsigned` só deve ser usado para validação interna explícita; não substitui trilha oficial de distribuição.

## Política ABI e matriz nativa

- `official_arm64` → `APP_ABI_POLICY=arm64-only`, `SUPPORTED_ABIS=arm64-v8a`.
- `official_arm32_arm64` → `APP_ABI_POLICY=arm32-arm64`, `SUPPORTED_ABIS=arm64-v8a,armeabi-v7a`.
- `internal_4abi` → `APP_ABI_POLICY=internal-4abi`, `SUPPORTED_ABIS=arm64-v8a,armeabi-v7a,x86,x86_64`.
- `internal_5abi`/`internal_riscv64` → `APP_ABI_POLICY=internal-5abi`, `SUPPORTED_ABIS=arm64-v8a,armeabi-v7a,x86,x86_64,riscv64`.
- `native_matrix_profile=canonical` atualmente executa `min-api=26` com `arm64-v8a`.
- `native_matrix_profile=pilot_android9_16_5arch` executa:
  - `min-api=28` com `armeabi-v7a arm64-v8a x86 x86_64`
  - `min-api=35` com `riscv64`
- `native_matrix_profile=android8_16plus_allarch` executa:
  - `min-api=26` e `min-api=28` para `armeabi-v7a arm64-v8a x86 x86_64`
  - `min-api=35` para `riscv64`

## Escopo do wrapper `android.yml`

`android.yml` mantém subconjunto de opções em `workflow_dispatch` para reduzir superfície de entrada manual:

- `abi_profile`: `official_arm64`, `internal_arm32_arm64`, `internal_4abi`.
- `native_matrix_profile`: `canonical`, `pilot_android9_16_5arch`, `android8_16plus_allarch`.

Ao precisar de perfis avançados (`official_arm32_arm64`, `internal_arm64`, `internal_5abi`, `generic`), o consumo deve acontecer por `workflow_call` direto do `android-ci.yml` em callers internos.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
