# Auditoria Vectras — build, release, CI e integração nativa

Data da auditoria: 2026-05-05
Modo: auditoria/classificação/documentação de origem, sem implementação corretiva.
Repositório auditado: `Vectras-VM-Android`.

## 1. Escopo executado

Esta auditoria tratou o repositório como sistema único e classificou a cadeia:

1. Gradle raiz e módulos Android.
2. Políticas de ABI, SDK, NDK, CMake e JVM.
3. Contrato JNI/NDK/CMake e camada low-level/freestanding.
4. Workflows GitHub Actions e upload/materialização de artefatos.
5. Documentação operacional existente versus fonte de verdade atual.
6. Inventário de todos os arquivos versionados e inventário forense não-Markdown.

## 2. Documentos e inventários de origem gerados/atualizados

| Saída | Tipo | Origem executável | Finalidade |
|---|---|---|---|
| `reports/FULL_REPO_AUDIT_REPORT.md` | relatório sintético | `python3 tools/audit_full_repo.py` | Sumário de todos os arquivos versionados. |
| `reports/full_repo_audit.tsv` | inventário arquivo-a-arquivo | `python3 tools/audit_full_repo.py` | Base rastreável para auditoria completa. |
| `reports/NON_MD_AUDIT_REPORT.md` | relatório sintético | `python3 tools/audit_non_md_inventory.py` | Sumário forense de arquivos não-Markdown. |
| `reports/non_md_inventory.tsv` | inventário arquivo-a-arquivo + SHA-256 | `python3 tools/audit_non_md_inventory.py` | Base de origem para código, scripts, binários, configs e artefatos versionados. |
| `docs/active/VECTRAS_SOURCE_AUDIT_2026-05-05.md` | relatório técnico | revisão direta + comandos desta sessão | Classificação de causa-raiz e bloqueios atuais. |
| `docs/active/VECTRAS_ORIGIN_DOCUMENTS_2026-05-05.md` | matriz de origem | revisão direta + relatórios gerados | Fonte de verdade auditável para a próxima etapa de correção. |

## 3. Estado quantitativo do repositório

Resultado da auditoria completa:

- Arquivos versionados auditados: **1855**.
- Diretórios derivados dos paths versionados: **300**.
- Arquivos de texto: **1685**.
- Arquivos binários: **170**.
- Linhas de texto inspecionadas: **275352**.
- Inconsistências automáticas detectadas:
  - `trailing-whitespace`: **3476** ocorrências.
  - `CRLF`: **183** ocorrências.
  - `missing-final-newline`: **141** ocorrências.
  - `broken-md-links`: **19** ocorrências.

Resultado da auditoria não-Markdown:

- Arquivos não-Markdown auditados: **1518**.
- Arquivos não-Markdown textuais: **1328**.
- Arquivos não-Markdown binários: **190**.
- Arquivos >= 5 MiB versionados:
  - `app/src/main/assets/roms/QEMU_EFI.img` — 67108864 bytes.
  - `app/src/main/assets/roms/QEMU_VARS.img` — 67108864 bytes.
- Candidato sensível por padrão de nome:
  - `tools/termux-arm64-orchestrator/resolve-release-keystore.sh`.

## 4. Classificação da cadeia Android/Gradle/NDK/JNI

### 4.1 Fonte de verdade Gradle atual

| Domínio | Arquivo | Estado auditado |
|---|---|---|
| Inclusão de módulos | `settings.gradle` | Inclui `:app`, `:terminal-emulator`, `:terminal-view`, `:shell-loader:stub`, `:shell-loader`. |
| Baseline global | `build.gradle` | Define SDK/API/JDK/NDK/CMake, políticas ABI, validadores e gates automáticos. |
| Política ativa versionada | `gradle.properties` | Define `APP_ABI_POLICY=arm32-arm64` e `SUPPORTED_ABIS=arm64-v8a,armeabi-v7a`. |
| App Android/JNI | `app/build.gradle` | Controla assinatura, build types, CMake, bootstrap shell-loader, ABI e relatório de artefatos compilados. |
| CMake app | `app/src/main/cpp/CMakeLists.txt` | Fonte nativa principal do app e ponte low-level. |
| Engine nativo | `engine/platform/android/CMakeLists.txt` + `engine/rmr/*` | Fonte low-level/RMR usada por host e Android. |

### 4.2 ABI e segurança de release

Achado: a política ativa versionada é dual ARM (`arm32-arm64`), mas o próprio contrato de raiz descreve `arm32-arm64` como validação interna dual-ABI e `arm64-only` como distribuição oficial. Isso não é apenas textual: `app/build.gradle` exige `CI_INTERNAL_VALIDATION=true` para `arm32-arm64` em validações de bootstrap, enquanto release assinado usa trilha estrita quando `signing_mode=signed` ou `ciRelease=true`.

Classificação: **divergência de política de origem**.

Impacto:

- Build local sem parâmetros herda dual-ABI interno a partir de `gradle.properties`.
- Release oficial seguro deve permanecer assinado e não deve virar unsigned por conveniência.
- Antes de implementar correção, é necessário decidir se o branch deve ter default oficial (`arm64-only`) ou default interno (`arm32-arm64`) com documentação/CI explicitamente interna.

### 4.3 Assinatura e artefatos

Achado: a cadeia possui contrato explícito para assinatura e artefatos:

- `tools/ci/prepare_release_signing.sh` resolve modo `signed|unsigned|auto` e falha em `signed` quando segredos estão ausentes.
- `app/build.gradle` configura `release` e `perfRelease` para exigir `signingConfigs.release` quando `effectiveSignedRelease` é verdadeiro.
- `:app:verifyDeliveredCompiledArtifacts` gera manifestos de artefato em `app/build/reports/artifacts/`.
- `tools/ci/materialize_android_ci_artifacts.sh` materializa logs, artefatos, matriz nativa e resultados de performance para upload.
- `.github/workflows/android-ci.yml` contém uploads via `actions/upload-artifact@v4`.

Classificação: **contrato de release existe, mas não pôde ser provado localmente nesta etapa por ausência de Android SDK local**.

## 5. Classificação CI/GitHub Actions

### 5.1 Workflows canônicos e auxiliares

| Workflow | Classificação auditada | Observação |
|---|---|---|
| `.github/workflows/pipeline-orchestrator.yml` | canônico/orquestrador | Encaminha host/android/quality conforme perfil. |
| `.github/workflows/host-ci.yml` | canônico host | Executa contratos host, Make/CMake e upload de evidências. |
| `.github/workflows/android-ci.yml` | canônico Android | Resolve perfis, SDK/NDK/CMake, Gradle, assinatura, artefatos e upload. |
| `.github/workflows/android.yml` | wrapper | Deve delegar para Android canônico. |
| `.github/workflows/ci.yml` | alias legado host | Deve delegar para host canônico. |
| `.github/workflows/compile-matrix.yml` | auxiliar | Matriz ABI/compatibilidade. |
| `.github/workflows/android-native-ci.yml` | auxiliar Android nativo | Matriz técnica complementar, classificada na documentação. |
| Workflows de fórmula | domínio paralelo | Fora da cadeia Android principal. |

### 5.2 Drift detectado por gate de matriz

Comando `python3 tools/ci/validate_build_matrix.py` falhou com:

```text
android-ci missing token: internal_4abi
```

Causa-raiz: `tools/ci/abi_profiles_contract.json` declara alias/perfil `internal_4abi`, e `tools/ci/validate_build_matrix.py` exige que `.github/workflows/android-ci.yml` contenha esse token, mas o workflow canônico Android auditado não expõe esse token literal.

Classificação: **drift entre contrato de perfil ABI e workflow canônico Android**.

Impacto:

- O gate de matriz falha antes da compilação Android completa.
- A documentação de workflow fica à frente/atrás do arquivo executável dependendo da interpretação.
- A próxima etapa deve alinhar contrato, resolver de perfil e workflow, sem mascarar o gate.

## 6. Bloqueio de ambiente local

Comando `./gradlew --no-daemon tasks --all` falhou durante configuração de `:app` com:

```text
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/workspace/Vectras-VM-Android/local.properties'.
```

Classificação: **bloqueio ambiental real, não falha de código comprovada**.

Impacto:

- Não foi possível provar `assembleDebug`, `assembleRelease`, assinatura, APK/AAB ou `:app:verifyDeliveredCompiledArtifacts` nesta etapa.
- A próxima etapa executável precisa instalar/descobrir Android SDK ou materializar `local.properties` com `tools/ci/prepare_android_env.sh`/`tools/ci/bootstrap_local_android_sdk.sh` antes de compilar.

## 7. Gates que passaram nesta auditoria

| Comando | Resultado |
|---|---|
| `python3 tools/audit_full_repo.py` | PASS — relatórios completos gerados. |
| `python3 tools/audit_non_md_inventory.py` | PASS — inventário não-Markdown gerado. |
| `python3 tools/verify_repo_file_dependencies.py` | PASS — dependências locais de módulos/arquivos existem. |
| `./tools/ci/validate_pipeline_directories.sh --profile host` | PASS — contrato de diretórios host válido. |
| `./tools/ci/validate_lowlevel_abi.sh` | PASS — contrato low-level ABI/freestanding válido sem binário crítico gerado. |
| `./tools/ci/verify_cmake_config.sh` | PASS — layout CMake verificado. |

## 8. Gates bloqueados/falhos nesta auditoria

| Comando | Resultado | Classificação |
|---|---|---|
| `./gradlew --no-daemon tasks --all` | FAIL — Android SDK/local.properties ausente. | Bloqueio ambiental. |
| `python3 tools/ci/validate_build_matrix.py` | FAIL — `android-ci missing token: internal_4abi`. | Drift de contrato CI/ABI. |

## 9. Causas-raiz priorizadas

1. **Ambiente Android local incompleto**: não há `ANDROID_HOME`/`sdk.dir`, portanto Gradle Android não configura `:app`.
2. **Drift CI/ABI**: contrato exige `internal_4abi`, mas o workflow Android canônico não contém o token esperado.
3. **Divergência de default ABI**: `gradle.properties` ativa dual-ABI interno, enquanto a política oficial documentada e o contrato raiz tratam `arm64-only` como distribuição oficial.
4. **Documentação de ABI com nomenclatura antiga**: `docs/BUILD_ENV_ALIGNMENT.md` ainda referencia políticas antigas (`with-32bit`, `all`) que não correspondem ao conjunto aceito atual (`arm64-only`, `arm32-arm64`, `internal-4abi`, `internal-5abi`).
5. **Dívida documental/formatting ampla**: auditoria completa encontrou CRLF, trailing whitespace, newline final ausente e links Markdown quebrados; isso não bloqueia build, mas reduz confiabilidade documental.

## 10. Próxima etapa recomendada, ainda sem implementar nesta auditoria

1. Preparar Android SDK/NDK/CMake local via contrato existente.
2. Corrigir drift `internal_4abi` entre `abi_profiles_contract.json`, `validate_build_matrix.py` e `android-ci.yml`.
3. Decidir e alinhar default ABI do branch:
   - oficial: `arm64-only` como default, dual-ABI apenas via CI interna; ou
   - interno: manter `arm32-arm64` com todo documento/workflow explicitamente interno.
4. Atualizar documentação de ABI obsoleta após a decisão.
5. Só então executar builds verificáveis:
   - debug unsigned;
   - release unsigned interno;
   - release signed oficial com segredo/keystore válido;
   - verificação de APK/AAB com arm32 e arm64 conforme lane.

## 11. Conclusão da auditoria

Nenhuma implementação corretiva foi aplicada nesta etapa. A auditoria gerou os documentos de origem e isolou bloqueios que impedem afirmar que a cadeia completa está compilável neste ambiente atual. O estado correto para a próxima etapa é correção estrutural orientada pelos achados acima, não mascaramento de release unsigned nem relaxamento de segurança.
