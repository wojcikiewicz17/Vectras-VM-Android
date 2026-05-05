# BETA_SHELL_LOADER_STATUS

Data: 2026-05-05 (UTC)

## Auditoria de pipeline

- `shell-loader/build.gradle` contém:
  - `publishShellLoaderArtifactMetadata`
  - `buildStableLoader`
  - publicação do APK por variante via mapa (`shellLoaderVariantApkProviders`) consumido em `app/build.gradle`.
- `app/build.gradle` contém:
  - `verifyShellLoaderArtifact`
  - `syncShellLoaderBootstrap`
  - source set de assets gerados: `app/build/generated/bootstrapAssets`.

## Estado atual

- Build do shell-loader não foi executado neste ambiente por ausência de Android SDK.
- `loader.apk` não existe em `app/src/main/assets/bootstrap/`.
- `verify_bootstrap_assets.py --strict-generated-assets` falha corretamente sem `loader.apk`.

## Status

- `SHELL_LOADER_BLOCKED`

## Condição para ficar READY

1. Executar `:shell-loader:assembleRelease` (ou variante configurada).
2. Executar `:app:syncShellLoaderBootstrap`.
3. Confirmar `app/build/generated/bootstrapAssets/bootstrap/loader.apk` com tamanho > 0.
4. Reexecutar `python3 tools/verify_bootstrap_assets.py --strict-generated-assets` com sucesso.
