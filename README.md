# Vectras VM Android

> Plataforma Android de virtualização com base híbrida (Android + C/C++ + Rust), foco em determinismo operacional, rastreabilidade e governança documental.

## Abstract
Esta revisão consolida a documentação em três camadas por diretório (propósito, estrutura e arquivo-a-arquivo), conectando documentação raiz, mapas locais e cadeia de comandos de inspeção. O objetivo é eliminar lacunas entre arquivos soltos, módulos ativos e documentação técnica, com navegação formal e auditável.

## Atualização da revisão documental (2026-04-03)
- Fluxo de revisão contínua padronizado em `docs/README.md`.
- Navegação de `docs/` refatorada para estrutura numerada de 5 níveis.
- Metadados de governança documental atualizados para acompanhamento de ciclo.


## Governança e estado — navegação rápida
- Estado do projeto: [`PROJECT_STATE.md`](PROJECT_STATE.md)
- Histórico de mudanças: [`CHANGELOG.md`](CHANGELOG.md)
- Notas de release: [`RELEASE_NOTES.md`](RELEASE_NOTES.md)
- Índice documental: [`DOC_INDEX.md`](DOC_INDEX.md)
- Avisos de terceiros/licenciamento: [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)
- Referência do runtime: [`VECTRA_CORE.md`](VECTRA_CORE.md)
- Guia macro de documentação: [`VECTRAS_MEGAPROMPT_DOCS.md`](VECTRAS_MEGAPROMPT_DOCS.md)
- Guia operacional de build: [`BUILDING.md`](BUILDING.md)
- Troubleshooting operacional: [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md)
- Sumário de correções: [`FIXES_SUMMARY.md`](FIXES_SUMMARY.md)
- Manifesto de estabilidade: [`VERSION_STABILITY.md`](VERSION_STABILITY.md)

## Política de overlays ZIP
- Arquivos `*.zip` de overlay na raiz são apenas artefatos transitórios de transporte e **não** são fonte de verdade.
- A fonte oficial de código e documentação é **exclusivamente** a árvore versionada no Git.
- O CI valida e falha quando detectar overlay ZIP na raiz contendo código-fonte duplicado da árvore ativa.

## Histórico arquivado (raiz)
- [`archive/root-history/1.md`](archive/root-history/1.md)
- [`archive/root-history/ADVANCED_OPTIMIZATIONS.md`](archive/root-history/ADVANCED_OPTIMIZATIONS.md)
- [`archive/root-history/BENCHMARK_REFACTORING_SUMMARY.md`](archive/root-history/BENCHMARK_REFACTORING_SUMMARY.md)
- [`archive/root-history/IMPLEMENTATION_COMPLETE.md`](archive/root-history/IMPLEMENTATION_COMPLETE.md)
- [`archive/root-history/IMPLEMENTATION_SUMMARY.md`](archive/root-history/IMPLEMENTATION_SUMMARY.md)
- [`archive/root-history/VECTRAS_ANALYSIS_COMPLETE.md`](archive/root-history/VECTRAS_ANALYSIS_COMPLETE.md)
- [`archive/root-history/VECTRAS_DEEP_EVIDENCE.md`](archive/root-history/VECTRAS_DEEP_EVIDENCE.md)

## Dissertação analítica (modelo de 3 camadas)
1. **Camada 1 — Diretório**: define responsabilidade técnica de cada domínio.
2. **Camada 2 — Estrutura**: explicita subdiretórios e fronteiras de módulo.
3. **Camada 3 — Arquivos**: descreve cada arquivo com papel, ligação e comando de inspeção.

Referências estruturais:
- [`docs/THREE_LAYER_ANALYSIS.md`](docs/THREE_LAYER_ANALYSIS.md)
- [`docs/ROOT_FILE_CHAIN.md`](docs/ROOT_FILE_CHAIN.md)

## Mapa de diretórios (com READMEs + FILES_MAP)
| Diretório | README | Mapa de Arquivos |
|---|---|---|
| `app/` | [app/README.md](app/README.md) | [app/FILES_MAP.md](app/FILES_MAP.md) |
| `engine/` | [engine/README.md](engine/README.md) | [engine/FILES_MAP.md](engine/FILES_MAP.md) |
| `terminal-emulator/` | [terminal-emulator/README.md](terminal-emulator/README.md) | [terminal-emulator/FILES_MAP.md](terminal-emulator/FILES_MAP.md) |
| `terminal-view/` | [terminal-view/README.md](terminal-view/README.md) | [terminal-view/FILES_MAP.md](terminal-view/FILES_MAP.md) |
| `shell-loader/` | [shell-loader/README.md](shell-loader/README.md) | [shell-loader/FILES_MAP.md](shell-loader/FILES_MAP.md) |
| `bench/` | [bench/README.md](bench/README.md) | [bench/FILES_MAP.md](bench/FILES_MAP.md) |
| `bug/` | [bug/README.md](bug/README.md) | [bug/FILES_MAP.md](bug/FILES_MAP.md) |
| `demo_cli/` | [demo_cli/README.md](demo_cli/README.md) | [demo_cli/FILES_MAP.md](demo_cli/FILES_MAP.md) |
| `tools/` | [tools/README.md](tools/README.md) | [tools/FILES_MAP.md](tools/FILES_MAP.md) |
| `docs/` | [docs/README.md](docs/README.md) | [docs/FILES_MAP.md](docs/FILES_MAP.md) |
| `reports/` | [reports/README.md](reports/README.md) | [reports/FILES_MAP.md](reports/FILES_MAP.md) |
| `resources/` | [resources/README.md](resources/README.md) | [resources/FILES_MAP.md](resources/FILES_MAP.md) |
| `runtime/` | [runtime/README.md](runtime/README.md) | [runtime/FILES_MAP.md](runtime/FILES_MAP.md) |
| `web/` | [web/README.md](web/README.md) | [web/FILES_MAP.md](web/FILES_MAP.md) |
| `archive/` | [archive/README.md](archive/README.md) | [archive/FILES_MAP.md](archive/FILES_MAP.md) |
| `fastlane/` | [fastlane/README.md](fastlane/README.md) | [fastlane/FILES_MAP.md](fastlane/FILES_MAP.md) |
| `gradle/` | [gradle/README.md](gradle/README.md) | [gradle/FILES_MAP.md](gradle/FILES_MAP.md) |
| `3dfx/` | [3dfx/README.md](3dfx/README.md) | [3dfx/FILES_MAP.md](3dfx/FILES_MAP.md) |


## Política de assinatura (`vectras.jks`)
- A chave `vectras.jks` **não deve permanecer versionada** no Git.
- A assinatura de **release** deve usar segredo de CI/cofre seguro (`VECTRAS_RELEASE_*` / `android.injected.signing.*`).
- Builds de `debug` usam apenas assinatura debug padrão do Android Gradle Plugin (não usar chave de release).
- Rotação recomendada: a cada 90 dias (ou imediatamente após incidente), com revogação e atualização de segredos no CI.
- Acesso mínimo: apenas mantenedores responsáveis por release e conta de automação do CI.

## Cadeia de comando recomendada
```bash
git ls-files
find . -maxdepth 2 -type d | sort
./tools/gradle_with_jdk21.sh verifyRepoFileDependencies verifyBootstrapAssets
```

## Índices
- [DOC_INDEX.md](DOC_INDEX.md)
- [docs/README.md](docs/README.md)
- [docs/navigation/BIGTECH_REVOLUTION_ANNOUNCE.md](docs/navigation/BIGTECH_REVOLUTION_ANNOUNCE.md)

## Execução padrão de CI/CD
- A execução normal ocorre em **Actions > Pipeline Orchestrator** (`.github/workflows/pipeline-orchestrator.yml`), único ponto de entrada para branch/PR/manual.
- O repositório foi consolidado para **3 workflows canônicos**: `pipeline-orchestrator.yml`, `ci.yml` (host) e `android.yml` (Android).
- `pipeline-orchestrator.yml` escolhe o fluxo com `pipeline_profile` (`host_only`, `android_only`, `full`) e injeta `run_workfile`/`log_level` para o Android.

## Como rodar manualmente
- Fluxo recomendado: acesse **Actions > Pipeline Orchestrator > Run workflow** e selecione:
  - `pipeline_profile`: host-only, android-only ou full.
  - `run_workfile`: `smoke_debug`, `unit_loader`, `full_debug`, `release_gate`.
  - `log_level`: `lifecycle`, `info`, `debug`.
  - `abi_profile`: `official_arm64` (trilha oficial no workflow canônico).
- Depuração pontual: execute o workflow **android-ci** diretamente quando quiser isolar build/test Android.
- Inputs principais do workflow **android-ci**:
  - `run_workfile`: define o conjunto de tarefas Gradle.
  - `build_variant` (`debug`|`release`|`both`): define variante alvo.
  - `signing_mode` (`auto`|`signed`|`unsigned`): política de assinatura de release.
  - `abi_profile` (`official_arm64`): mantém a trilha ABI oficial no workflow canônico.
  - `run_lint` e `run_native_checks`: habilitam gates opcionais.
  - `log_level`: controla verbosidade e rastreabilidade dos logs.
- Para manter valores padrão por repositório em CI, configure variáveis em **Settings > Secrets and variables > Actions > Variables** (prefira canônicas: `compile.api`, `tools.version`, `ndk.version`, `cmake.version`, `java.language.version`; aliases legados como `COMPILE_API`, `TOOLS_VERSION`, `NDK_VERSION`, `CMAKE_VERSION`, `JAVA_VERSION` ficam como fallback de compatibilidade).

## Setup rápido de build
- Copie `local.properties.example` para `local.properties` e ajuste `sdk.dir`.
- Ajuste versões via `gradle.properties` com precedência explícita: canônicas (`compile.api`, `tools.version`, `java.language.version`, `cmake.version`, `ndk.version`) primeiro; aliases legados (`COMPILE_API`, `TOOLS_VERSION`, `JAVA_LANGUAGE_VERSION`, `CMAKE_VERSION`, `NDK_VERSION`) apenas como fallback retroativo com warning de depreciação.
- **Manutenção de upgrade de SDK:** altere primeiro `sdk.baseline.api` e `compile.api`/`target.api`/`release.min.target.api` em `gradle.properties`; aliases legados (`SDK_BASELINE_API`, `COMPILE_API`, `TARGET_API`, `RELEASE_MIN_TARGET_API`) ficam somente como fallback. O `build.gradle` raiz consome esse baseline como fallback único para todos os módulos.
- Baseline único de CMake para host + Android: `3.22.1`. O `CMakeLists.txt` da raiz e o CMake do app JNI compartilham esse baseline para evitar drift entre build local/CI e NDK.
- Política de JVM do Gradle: execute preferencialmente com **JDK 17** (alinhado com `JAVA_LANGUAGE_VERSION=17`).
- Defina explicitamente `JAVA_HOME` para o JDK 17/21 ou configure `org.gradle.java.home=<path-do-jdk17-ou-jdk21>` em `~/.gradle/gradle.properties`.
- Use sempre o wrapper `./tools/gradle_with_jdk21.sh` (local e CI) para evitar regressão com JDK 22+.
- O build agora valida em bootstrap `GRADLE_JAVA_RUNTIME_VERSION` (padrão 17) e falha se a JVM runtime exceder `GRADLE_MAX_RUNTIME_JAVA_VERSION` (padrão 21).
- Para override pontual, use `-P` no comando Gradle.


### Precedência oficial de propriedades Gradle
- Regra fixa: propriedade canônica (`lowercase.with.dots`) sempre vence.
- Alias legado (`UPPER_SNAKE_CASE`) é somente fallback para compatibilidade retroativa.
- Uso de alias legado gera warning de depreciação no bootstrap Gradle para facilitar migração sem quebra imediata.

### ABIs oficialmente suportadas
- Escopos ABI em `tools/qemu_launch.yml`:
  - `official_distribution`: padrão oficial de distribuição, com `arm64-v8a`.
  - `internal_validation`: matriz técnica expandida (`arm64-v8a,armeabi-v7a,x86,x86_64`) para validação interna.
- **Política Gradle de distribuição oficial**:
  - `APP_ABI_POLICY=arm64-only` + `SUPPORTED_ABIS=arm64-v8a` (default oficial).
- **Política Gradle de validação técnica interna**:
  - `APP_ABI_POLICY=internal-5abi` + `SUPPORTED_ABIS=arm64-v8a,armeabi-v7a,x86,x86_64,riscv64` para validação interna unsigned em GitHub Actions (exige `CI_INTERNAL_VALIDATION=true` e `MIN_API>=35`).
  - a validação `internal-5abi` permanece para execução técnica interna explícita via Gradle local/diagnóstico, fora da trilha canônica de release no workflow principal.
- O CI executa `tools/check_abi_policy_alignment.py` para garantir alinhamento entre `gradle.properties` (`APP_ABI_POLICY`/`SUPPORTED_ABIS`) e os escopos ABI em `tools/qemu_launch.yml`.
- Entradas condicionais para ABIs fora dessa matriz no CMake (ex.: `riscv64`) são apenas roadmap e não representam ABI ativa no empacotamento Gradle.

### Exemplo de configuração de Java para build
```bash
source <(./tools/configure_java_home.sh --print)
./tools/gradle_with_jdk21.sh --version
./tools/gradle_with_jdk21.sh verifyGradleRuntimeJvm
```

### Validação canônica de setup/CI/build
```bash
./tools/gradle_with_jdk21.sh checkNativeAllMatrix
```

Essa task já encadeia `verifyMinApiAbiCompatibility`, `verifyArm64ToolchainCompatibility`, `verifyGradleRuntimeJvm` e executa `assembleDebug`, `assembleRelease`, `assemblePerfRelease` nas políticas de ABI suportadas.

Para fixar por usuário (sem depender de shell):
```properties
# ~/.gradle/gradle.properties
org.gradle.java.home=/usr/lib/jvm/java-21-openjdk
```

## Referência rápida de bugs
- Escopo e relação com os demais domínios: [`bug/README.md`](bug/README.md)
- Mapa arquivo-a-arquivo do domínio de bugs: [`bug/FILES_MAP.md`](bug/FILES_MAP.md)


## Canal oficial de comunidade e suporte
- Canal oficial neutro: https://vectras.vercel.app/community.html
- Para notícias, suporte e feedback, use sempre o canal oficial acima.
