# BETA_STARTVM_PREFLIGHT

## STATUS: BLOCKED (FECHADO)

## Evidências obrigatórias
- **Arquivo**: validações prévias de ambiente em `build.gradle` (`verifyAndroidSdkPackages`, `verifyGradleRuntimeJvm`, `verifyArm64ToolchainCompatibility`).
- **Task Gradle**: `:app:deviceRuntimeSmoke` existe como execução dedicada de smoke runtime.
- **Script/CI**: `.github/workflows/device-runtime-smoke.yml` e `.github/workflows/android-ci.yml` dão cobertura parcial.

## Motivo do bloqueio
- Ausência de evidência determinística no repositório de execução obrigatória de preflight StartVM em todos os fluxos de release.
- Gate existe, mas não está comprovado como hard requirement universal para promoção beta.
