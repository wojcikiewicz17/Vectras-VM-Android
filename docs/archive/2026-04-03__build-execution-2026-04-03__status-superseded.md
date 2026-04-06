# Build Execution Report — 2026-04-03

## Requested canonical flow

1. `./tools/gradle_with_jdk21.sh :app:assembleDebug --stacktrace`
2. `./tools/gradle_with_jdk21.sh :app:assembleRelease --stacktrace`
3. `./tools/gradle_with_jdk21.sh :app:verifyDeliveredCompiledArtifacts -PartifactVariants=debug,release,perfRelease`

## Results

All three commands were executed, and all failed at Gradle project configuration time with the same root cause:

- `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/workspace/Vectras-VM-Android/local.properties'.`

Additional informational message observed:

- `Release signing não configurado: build de release seguirá sem assinatura de produção.`

## Artifact collection

A filesystem scan was executed for:

- `*.apk`
- `*.aab`
- `app/build/reports/artifacts/compiled-artifacts-report.json`

No files were found because the build did not reach artifact generation.

## Status

Not deliverable in this environment until Android SDK path is configured (`ANDROID_HOME` or `local.properties` with `sdk.dir`).
