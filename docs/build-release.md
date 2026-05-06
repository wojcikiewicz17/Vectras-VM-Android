# Build and Release Matrix

## Local

```bash
export ANDROID_SDK_ROOT=/path/to/Android/Sdk
./tools/ci/build_apk_matrix.sh
```

## Outputs

- Debug APK (assinado com debug keystore)
- Release APK (conforme regras do Gradle do projeto)

## CI

Workflow: `.github/workflows/gaiaphi-android-build.yml`

- instala Java 21 + Android SDK + NDK + CMake
- executa build matrix
- publica artefatos APK
