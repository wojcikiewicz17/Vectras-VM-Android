# BETA_SHELL_LOADER_STATUS

## STATUS: RISK (FECHADO)

## Evidências obrigatórias
- **Arquivo**: integração de shell-loader declarada em `app/build.gradle` (`verifyShellLoaderArtifact`, `syncShellLoaderBootstrap`).
- **Task Gradle**: `:app:verifyShellLoaderArtifact` com dependência de assemble do módulo `:shell-loader`.
- **Script/CI**: `.github/workflows/android-ci.yml` inclui `:shell-loader:testDebugUnitTest` e `:shell-loader:testReleaseUnitTest` na matriz de tasks.

## Critério fechado
- Cadeia de build e teste do shell-loader existe, porém o status é **RISK** até validação de artefato final no pipeline de release assinado.
