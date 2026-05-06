# VECTRAS_BETA_READINESS_FINAL

## Consolidação de aceite (itens 1–7)

1. Bootstrap multi-ABI validado → **PASS** (`BETA_BOOTSTRAP_REALITY_CHECK`: READY)
2. Shell-loader integrado à cadeia de build/teste → **RISK** (`BETA_SHELL_LOADER_STATUS`: RISK)
3. APK/ABI/bootstrap com matriz nativa ativa → **PASS** (`BETA_APK_ABI_BOOTSTRAP_REPORT`: READY)
4. Preflight StartVM como gate universal de release → **FAIL** (`BETA_STARTVM_PREFLIGHT`: BLOCKED)
5. Startup nativo RMR fim-a-fim em device release candidate → **RISK** (`BETA_NATIVE_RMR_STARTUP_STATUS`: RISK)
6. Mapa de referência externa consolidado → **FAIL** (`BETA_EXTERNAL_REFERENCE_MAP`: PLACEHOLDER)
7. Verificação de artefatos + CI upload consistente → **PASS** (workflows android-ci/native com coleta de artefatos)

## FLAG FINAL

`VECTRAS_BETA_BLOCKED = true`

## Regra aplicada
- Se qualquer item 1–7 falhar (FAIL) ou permanecer bloqueado, o beta fica bloqueado.
- Itens em RISK não aprovam prontidão final sem mitigação explícita.

## Evidências de implementação
- **Arquivo**: tasks de validação e matriz ABI em `app/build.gradle`.
- **Task Gradle**: `:app:verifyDeliveredCompiledArtifacts`, `:app:checkNativeExtendedMatrix`, `:app:validateReleaseSigningConfig`.
- **Script/CI**: `.github/workflows/android-ci.yml`, `.github/workflows/android-native-ci.yml`, `.github/workflows/host-ci.yml`.
