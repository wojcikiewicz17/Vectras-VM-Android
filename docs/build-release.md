# Build and Release Matrix

## Local

```bash
export ANDROID_SDK_ROOT=/path/to/Android/Sdk
./tools/ci/build_apk_matrix.sh
```

## Release signing (optional)

```bash
export ANDROID_KEYSTORE_B64="$(base64 -w0 my-release.keystore)"
export ANDROID_KEYSTORE_PASSWORD="***"
export ANDROID_KEY_ALIAS="***"
export ANDROID_KEY_PASSWORD="***"
./tools/ci/build_apk_matrix.sh
```

## Outputs

- Debug APK (assinado com debug keystore)
- Release APK (assinado quando secrets/vars de keystore estão presentes)
- Relatório de ABI por `aapt dump badging`
- Verificação de assinatura por `apksigner`

## CI

Workflow: `.github/workflows/gaiaphi-android-build.yml`

- instala Java 21 + Android SDK + NDK + CMake
- executa build matrix
- publica artefatos APK/logs
