# BETA_SHELL_LOADER_STATUS

## Estado final
- **STATUS:** `SHELL_LOADER_READY`
- **CI gate:** `STRICT_BOOTSTRAP_VALIDATION_ENFORCED`
- **Fail policy:** `BLOCK_ON_MISSING_LOADER_APK`

## Critérios de conclusão

### 1) Cadeia obrigatória de tasks (fonte de verdade: Gradle app)
A cadeia de bootstrap do shell-loader está amarrada de forma determinística:

1. `:app:verifyShellLoaderArtifact`
2. `:app:syncShellLoaderBootstrap`
3. `:app:preBuild`

Relações obrigatórias no `app/build.gradle`:
- `syncShellLoaderBootstrap` **dependsOn** `verifyShellLoaderArtifact`.
- `preBuild` **dependsOn** `syncShellLoaderBootstrap`.

Consequência: qualquer `assemble*` da app passa pelo gate de verificação do `loader.apk` antes de compilar pacote final.

### 2) Regra de bloqueio por ausência de `loader.apk`
A ausência/invalidez de artifact do shell-loader falha o build com erro acionável:
- variante solicitada;
- caminhos tentados (AGP artifacts API + fallback convencional);
- comando de correção (`-PloaderVariant=<...>` + `:shell-loader:assemble<Variant>`).

### 3) CI com validação estrita de bootstrap
A pipeline Android CI executa validação de bootstrap em modo estrito, sem depender apenas de `assemble`:

- Execução de `tools/ci/verify_bootstrap_contract.sh`.
- Esse script força:
  - `:app:verifyShellLoaderArtifact`
  - `:app:syncShellLoaderBootstrap`
  - `python3 tools/verify_bootstrap_assets.py --strict-generated-assets`
  - `./tools/gradle_with_jdk21.sh verifyBootstrapAssets`

### 4) Condição de bloqueio oficial
Se `app/build/generated/bootstrapAssets/bootstrap/loader.apk` não existir ou estiver vazio,
a CI deve interromper com erro explícito (`::error::`) e instrução de correção.

## Estados possíveis
- `SHELL_LOADER_READY`: cadeia ativa + CI estrita + bloqueio de ausência funcionando.
- `SHELL_LOADER_BLOCKED`: qualquer quebra na cadeia `verify -> sync -> preBuild`.
- `SHELL_LOADER_CI_WEAK`: CI roda apenas `assemble*` sem validação estrita de bootstrap.
- `SHELL_LOADER_ARTIFACT_MISSING`: `loader.apk` ausente/vazio e gate de bloqueio acionado.
