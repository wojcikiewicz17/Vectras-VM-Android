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
- `abi_profile`: `official_arm64` (trilha oficial) ou `internal_5abi` (validação interna multi-ABI).
- `run_lint`: controla execução de `:app:lintDebug`.
- `run_native_checks`: controla validação de contrato Make/CMake.

### Regras de assinatura e segurança de entrega

- `signing_mode=signed`: exige secrets válidos e produz release assinada.
- `signing_mode=auto`: assina release quando secrets existem; sem secrets, cai para trilha **interna** unsigned com `ALLOW_UNSIGNED_RELEASE=true` e `CI_INTERNAL_VALIDATION=true`.
- `signing_mode=unsigned`: força trilha **interna** unsigned, mantendo bloqueio de release oficial (loja) no caminho padrão.
- `abi_profile=official_arm64`: injeta `APP_ABI_POLICY=arm64-only` e `SUPPORTED_ABIS=arm64-v8a`.
- `abi_profile=internal_5abi`: injeta `APP_ABI_POLICY=internal-5abi`, `SUPPORTED_ABIS=arm64-v8a,armeabi-v7a,x86,x86_64,riscv64`, força `CI_INTERNAL_VALIDATION=true` e eleva `min.api` para o baseline de compile SDK.
- `abi_profile=internal_5abi` bloqueia `signing_mode=signed` para impedir uso acidental da trilha oficial de distribuição.
- Em `build_variant=release|both`, o passo `prepare_release_signing.sh` sempre executa para evitar drift entre modo escolhido e flags Gradle efetivas.
- `run_native_checks=true` agora compila o build CMake e executa `verify_contracts` antes da etapa Android para estabilizar a cadeia nativa.

> Nota: o orquestrador seleciona pipeline (`host_only`, `android_only`, `full`) e repassa `run_workfile`/`log_level` para reduzir drift entre execução manual e execução automática.
