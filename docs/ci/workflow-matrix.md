<!-- DOC_ORG_SCAN: 2026-04-20 | source-scan: manual-ci-canonicalization -->

# CI workflow matrix (canonical)

Matriz final de workflows CI após consolidação da trilha host/android.

| Workflow | Trigger | Responsabilidades | Artefatos |
|---|---|---|---|
| `.github/workflows/pipeline-orchestrator.yml` | `push`, `pull_request`, `workflow_dispatch` | Resolve perfil (`host_only`, `android_only`, `full`) e orquestra `host-ci`, `android-ci` e `quality-gates`. | Não publica artefatos diretamente (delegado aos workflows chamados). |
| `.github/workflows/host-ci.yml` **(canônico host)** | `push`, `pull_request`, `workflow_dispatch`, `workflow_call` | Valida diretórios de pipeline host, contratos de ingestão/docs/assets, build e selftests Make/CMake, verificação de contratos de link e coleta de evidências host. | `host-engine-binaries-*`, `host-ci-results`, `host-logs-*`. |
| `.github/workflows/android-ci.yml` **(canônico Android)** | `workflow_call` | Executa trilha Android parametrizada (`run_workfile`, `build_variant`, `abi_profile`, `signing_mode`, `native_matrix_profile`), valida contratos Android e gera artefatos de build/test. | APK/AAB, relatórios de teste/lint, artefatos da matriz CMake Android. |
| `.github/workflows/android.yml` | `push`, `pull_request`, `workflow_dispatch` | **Wrapper/entrada**: encaminha execução para `android-ci.yml` com parâmetros explícitos; opcionalmente aciona `compile-matrix.yml` como gate adaptativo em trilhas internas. | Herda artefatos da pipeline canônica chamada + artefatos do gate adaptativo quando acionado. |
| `.github/workflows/quality-gates.yml` | `workflow_call` | Consolida status de host/android e aplica gate final por perfil. | Relatório de gate (logs do próprio job). |
| `.github/workflows/compile-matrix.yml` | `workflow_call` | **Pipeline auxiliar de compatibilidade ABI**: executa matriz própria (`abi x variant`) para validação de regressão Android; não é wrapper puro de `android-ci.yml`. | `compile-matrix-<abi>-<variant>`. |
| `.github/workflows/ci.yml` | `push`, `pull_request`, `workflow_dispatch`, `workflow_call` | **Alias legado**: encaminha para `host-ci.yml` e não mantém checks host próprios. | Herda artefatos do `host-ci.yml` chamado. |

## Regras de canonicalização

1. Toda responsabilidade host (validação de diretório, contratos host, build/test host e coleta de artefato host) fica em `host-ci.yml`.
2. `ci.yml` é apenas compatibilidade temporária e não deve acumular lógica de host.
3. Callers (`pipeline-orchestrator.yml` e futuros workflows reutilizáveis) devem chamar diretamente `host-ci.yml` e `android-ci.yml` para trilhas oficiais.
4. `tools/ci/validate_build_matrix.py` bloqueia regressões que reintroduzam trilhas host duplicadas/conflitantes.
5. Toda responsabilidade Android oficial (SDK/NDK/JDK, Gradle tasks, validações nativas e artefatos de release) fica em `android-ci.yml`; wrappers Android devem apenas delegar ou acionar gates auxiliares sem duplicar política oficial.
