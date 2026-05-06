# Build and Release Matrix

## Local

```bash
export ANDROID_SDK_ROOT=/path/to/Android/Sdk
./tools/ci/bootstrap_android_sdk.sh
./tools/ci/build_apk_matrix.sh
```

## Validations

- APK list: `app/build/outputs/logs/apk-list.txt`
- Signature/certs + native-code metadata: `app/build/outputs/logs/apk-signature-report.txt`

## CI

Workflow: `.github/workflows/gaiaphi-android-build.yml`

- setup Java 21
- bootstrap SDK/NDK/CMake
- build debug + release APKs
- upload APKs + logs as artifacts
