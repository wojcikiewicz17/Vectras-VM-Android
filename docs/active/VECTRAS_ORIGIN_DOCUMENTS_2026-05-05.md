# Vectras — documentos de origem para correção estrutural

Data: 2026-05-05
Escopo: fonte de verdade auditada antes de qualquer implementação corretiva.

## 1. Hierarquia de origem

| Ordem | Fonte | Papel |
|---:|---|---|
| 1 | `settings.gradle` | Lista canônica dos módulos Gradle ativos. |
| 2 | `gradle.properties` | Valores versionados de API, NDK, CMake, JVM e ABI. |
| 3 | `build.gradle` | Validadores globais, políticas ABI e contratos de SDK/JVM. |
| 4 | `app/build.gradle` | Contrato principal Android: assinatura, variants, CMake, bootstrap e artefatos. |
| 5 | `app/src/main/cpp/CMakeLists.txt` | Entrada CMake/JNI do app. |
| 6 | `engine/platform/android/CMakeLists.txt` | Entrada CMake Android do engine/RMR. |
| 7 | `tools/ci/abi_profiles_contract.json` | Matriz declarativa de perfis ABI. |
| 8 | `tools/ci/resolve_abi_profile.py` | Resolução executável dos perfis ABI. |
| 9 | `.github/workflows/android-ci.yml` | Workflow Android canônico. |
| 10 | `.github/workflows/host-ci.yml` | Workflow host canônico. |
| 11 | `.github/workflows/pipeline-orchestrator.yml` | Orquestração canônica. |
| 12 | `.github/workflows/quality-gates.yml` | Gate final canônico. |
| 13 | `docs/ci/workflow-matrix.md` | Classificação documental dos workflows. |
| 14 | `docs/BUILD_ENV_ALIGNMENT.md` | Documento operacional de ambiente/build que precisa ser realinhado. |
| 15 | `reports/full_repo_audit.tsv` | Inventário completo de origem para todos os arquivos versionados. |
| 16 | `reports/non_md_inventory.tsv` | Inventário forense de origem para arquivos não-Markdown. |

## 2. Contrato ABI de origem

Políticas aceitas atualmente pelo código Gradle:

| Política | Uso auditado | ABI esperada |
|---|---|---|
| `arm64-only` | distribuição oficial | `arm64-v8a` |
| `arm32-arm64` | validação interna dual ARM | `arm64-v8a,armeabi-v7a` |
| `internal-4abi` | validação interna expandida app/bootstrap | `arm64-v8a,armeabi-v7a,x86,x86_64` |
| `internal-5abi` | validação interna expandida low-level | `arm64-v8a,armeabi-v7a,x86,x86_64,riscv64` |

Divergência de origem registrada: `gradle.properties` ativa `arm32-arm64`, enquanto a distribuição oficial descrita no contrato é `arm64-only`.

## 3. Contrato de assinatura de origem

| Trilha | Requisito |
|---|---|
| Debug/local | Pode ser unsigned/debug sem promover como release oficial. |
| Release unsigned interno | Permitido apenas como validação interna explícita. |
| Release signed oficial | Deve exigir signing config/segredos válidos; não pode degradar para unsigned por conveniência. |

Fontes executáveis relacionadas:

- `tools/ci/prepare_release_signing.sh`.
- `app/build.gradle` (`effectiveSignedRelease`, `signingConfigs.release`, `buildTypes.release`, `buildTypes.perfRelease`).
- `.github/workflows/android-ci.yml`.

## 4. Contrato de artefatos de origem

| Etapa | Fonte | Saída esperada |
|---|---|---|
| Build Gradle | `.github/workflows/android-ci.yml` + `app/build.gradle` | APK/AAB por variant solicitada. |
| Verificação de entrega | `:app:verifyDeliveredCompiledArtifacts` | `app/build/reports/artifacts/compiled-artifacts-report.json` e `artifact-manifest.json`. |
| Materialização CI | `tools/ci/materialize_android_ci_artifacts.sh` | `ci-artifacts/android-logs`, `ci-artifacts/android-artifacts`, `ci-artifacts/native-matrix`, `ci-artifacts/perf-results`. |
| Upload | `.github/workflows/android-ci.yml` / `host-ci.yml` / `quality-gates.yml` | `actions/upload-artifact@v4`. |

## 5. Estado dos gates de origem em 2026-05-05

| Gate | Estado | Observação |
|---|---|---|
| Inventário completo | PASS | `reports/full_repo_audit.tsv` gerado. |
| Inventário não-Markdown | PASS | `reports/non_md_inventory.tsv` gerado. |
| Dependências locais | PASS | Módulos/arquivos locais existem. |
| Diretórios host | PASS | Perfil host válido. |
| Low-level ABI | PASS | Sem binário crítico gerado nesta etapa. |
| CMake config | PASS | Layout CMake válido. |
| Gradle Android | BLOQUEADO | SDK/local.properties ausente. |
| Build matrix | FALHA | `android-ci missing token: internal_4abi`. |

## 6. Uso obrigatório deste documento

Antes de qualquer implementação corretiva, a próxima etapa deve usar este documento como matriz de origem para decidir:

1. Qual default ABI é canônico para o branch.
2. Como `internal_4abi` será representado no workflow Android canônico.
3. Como o Android SDK local será preparado para prova de build.
4. Como provar APK/AAB signed e unsigned sem enfraquecer o caminho oficial signed.
