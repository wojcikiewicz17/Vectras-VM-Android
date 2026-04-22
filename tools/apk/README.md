<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# tools/apk

Scripts focados em gerar artefato APK release assinado com rastreabilidade.

## build_apk_wizard.sh

Wizard de build CI/local para trilhas debug multi-ABI com foco em compatibilidade.

Fluxo:
1. bootstrap de SDK/NDK/CMake com `tools/ci/bootstrap_local_android_sdk.sh`;
2. build `:app:assembleDebug` arm64 oficial (`APP_ABI_POLICY=arm64-only`);
3. build `:app:assembleDebug` dual ARM (`APP_ABI_POLICY=arm32-arm64`);
4. materialização de artefatos em `artifacts/apk-wizard/` com relatório de tamanho.

### Uso

```bash
bash tools/ci/build_apk_wizard.sh
```

### Saídas

- `artifacts/apk-wizard/app-debug-arm64-v8a.apk`
- `artifacts/apk-wizard/app-debug-arm32-arm64.apk`
- `artifacts/apk-wizard/sizes.tsv`
- `artifacts/apk-wizard/REPORT.md`

## build_release_signed_local.sh

Pipeline local para:
1. validar Java/SDK e `local.properties`;
2. executar `:app:assembleRelease`;
3. verificar assinatura com `apksigner`;
4. registrar hash SHA-256 e metadados do APK.

### Uso

```bash
bash tools/apk/build_release_signed_local.sh
```

### Saídas

- APK: `app/build/outputs/apk/release/app-release.apk`
- Logs: `build/reports/apk-local/`
  - `gradle_assemble_release.log`
  - `apksigner_verify.log`
  - `apk_metadata.txt`


### Variáveis canônicas de assinatura (release)

- `VECTRAS_RELEASE_STORE_FILE`
- `VECTRAS_RELEASE_STORE_PASSWORD`
- `VECTRAS_RELEASE_KEY_ALIAS`
- `VECTRAS_RELEASE_KEY_PASSWORD`

Esses nomes são os canônicos da cadeia de build e mapeiam diretamente para as propriedades Gradle `android.injected.signing.*`.
