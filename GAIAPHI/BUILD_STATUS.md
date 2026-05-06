# Build status and execution contract

## Required env

- `ANDROID_HOME` or `ANDROID_SDK_ROOT`

## Optional signing env

- `RELEASE_KEYSTORE_PATH`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

## Build outputs

- Unsigned/signed APKs from `:app:assembleDebug :app:assembleRelease`
- Signed AAB path when signing env is present via `:app:bundleRelease`
- Artifact upload handled in `.github/workflows/gaiaphi-android-build.yml`
