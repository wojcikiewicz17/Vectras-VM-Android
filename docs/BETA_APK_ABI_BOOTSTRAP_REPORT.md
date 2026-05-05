# BETA_APK_ABI_BOOTSTRAP_REPORT

Data: 2026-05-05 (UTC)

## Comandos alvo da fase

1. `./tools/gradle_with_jdk21.sh -PAPP_ABI_POLICY=arm32-arm64 -PSUPPORTED_ABIS=arm64-v8a,armeabi-v7a -PCI_INTERNAL_VALIDATION=true -Psigning_mode=unsigned :app:assembleDebug`
2. `./tools/gradle_with_jdk21.sh -PAPP_ABI_POLICY=arm32-arm64 -PSUPPORTED_ABIS=arm64-v8a,armeabi-v7a -PCI_INTERNAL_VALIDATION=true :app:verifyTermuxBootstrapAbiCoverage`

## Resultado real no ambiente atual

- **BLOQUEADO**: Android SDK ausente (`ANDROID_SDK_ROOT/ANDROID_HOME` não definidos e sem fallback).
- Sem APK gerado nesta execução.
- Sem inventário final de libs/assets dentro do APK nesta execução.

## Pré-condições para fechar esta fase

- Configurar SDK (`local.properties` com `sdk.dir` ou `ANDROID_SDK_ROOT`).
- Reexecutar comandos acima.
- Validar no APK:
  - `lib/arm64-v8a/libtermux-bootstrap.so`
  - `lib/armeabi-v7a/libtermux-bootstrap.so`
  - `lib/arm64-v8a/libvectra_core_accel.so`
  - `lib/armeabi-v7a/libvectra_core_accel.so`
  - `assets/bootstrap/arm64-v8a.tar`
  - `assets/bootstrap/armeabi-v7a.tar`
  - `assets/bootstrap/loader.apk` (versionado ou via generated assets)
