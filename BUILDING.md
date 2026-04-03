# BUILDING

## CLI prerequisites
- JDK 17 (baseline runtime)
- Android SDK Platform 35 + Build Tools 35.0.0
- NDK 27.2.12479018
- CMake 3.22.1

> Baseline único de CMake: host (raiz) e Android JNI usam 3.22.1 para manter paridade de toolchain.

## Setup environment (example)
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_SDK_ROOT=/workspace/android-sdk
printf 'sdk.dir=%s\n' "$ANDROID_SDK_ROOT" > local.properties
```

If `local.properties` is missing, `./tools/gradle_with_jdk21.sh` now auto-writes `sdk.dir`
from `ANDROID_SDK_ROOT` (or `ANDROID_HOME`) when the directory exists.

## Build commands
```bash
./tools/gradle_with_jdk21.sh --version
./tools/gradle_with_jdk21.sh clean
./tools/gradle_with_jdk21.sh :app:assembleDebug --stacktrace
./tools/gradle_with_jdk21.sh :app:assembleRelease --stacktrace
./tools/gradle_with_jdk21.sh :app:assemblePerfRelease --stacktrace
./tools/gradle_with_jdk21.sh :app:verifyDeliveredCompiledArtifacts -PartifactVariants=debug,release,perfRelease
./tools/gradle_with_jdk21.sh :app:lintDebug --stacktrace
```

## Fluxo oficial para gerar bootstrap/loader.apk (Termux)
O artefato `loader.apk` é produzido pelo módulo `shell-loader` e copiado para assets intermediários do app
em `app/build/generated/bootstrapAssets/bootstrap/loader.apk` (sem versionar binário em `app/src/main/assets`).

```bash
# Variant estável padrão (release). Pode sobrescrever com -PloaderVariant=debug.
./tools/gradle_with_jdk21.sh :shell-loader:buildStableLoader

# Copia o loader gerado para os assets de build do app.
./tools/gradle_with_jdk21.sh :app:syncShellLoaderBootstrap

# Valida bootstraps versionados + loader quando Termux está habilitado.
python3 tools/verify_bootstrap_assets.py
```

`verifyDeliveredCompiledArtifacts` validates APK/AAB delivery per variant and writes
`app/build/reports/artifacts/compiled-artifacts-report.json`.

> Use `./tools/gradle_with_jdk21.sh` como comando canônico: o wrapper aplica a política de JVM suportada (17/21) e faz autoajuste de `sdk.dir` quando possível.

## ABI policy
Configured by `APP_ABI_POLICY` and `SUPPORTED_ABIS` in `gradle.properties`.
The ABI baseline is also declared in `tools/qemu_launch.yml` with explicit scope:
- `build_env.abi_filters.scope=official_distribution` (official default)
- `build_env.abi_filters.internal_validation` (expanded internal matrix)

Accepted policies in code and docs are exactly:
- `APP_ABI_POLICY=arm64-only` → `SUPPORTED_ABIS=arm64-v8a` (official minimum distribution)
- `APP_ABI_POLICY=with-32bit` → `SUPPORTED_ABIS=arm64-v8a,armeabi-v7a` (official distribution with 32-bit ARM)
- `APP_ABI_POLICY=all` → `SUPPORTED_ABIS=arm64-v8a,armeabi-v7a,x86,x86_64` (**internal validation only; not for official distribution**)

Default is arm64-only.

To include 32-bit ARM:
```bash
./tools/gradle_with_jdk21.sh -PAPP_ABI_POLICY=with-32bit -PSUPPORTED_ABIS=arm64-v8a,armeabi-v7a :app:assembleDebug
```

To run full internal ABI validation coverage:
```bash
./tools/gradle_with_jdk21.sh -PAPP_ABI_POLICY=all -PSUPPORTED_ABIS=arm64-v8a,armeabi-v7a,x86,x86_64 :app:assembleDebug
```

Alignment check command (used by CI before build):
```bash
python3 tools/check_abi_policy_alignment.py
```


## Supported version matrix
All values below are defaults from `gradle.properties` and can be overridden with `-P`.

| Area | Property | Min | Default | Max/Policy |
|---|---|---:|---:|---:|
| Compile SDK | `compile.api` → fallback `COMPILE_API` | 35 | 35 | follows Android baseline updates |
| Target SDK | `target.api` → fallback `TARGET_API` | 35 (`release.min.target.api`) | 35 | follows Android baseline updates |
| Build Tools | `tools.version` → fallback `TOOLS_VERSION` | 35.0.0 | 35.0.0 | keep aligned with compile SDK |
| NDK | `ndk.version` → fallback `NDK_VERSION` | 23.x | 27.2.12479018 | latest validated in CI |
| CMake | `cmake.version` → fallback `CMAKE_VERSION` | 3.22.1 | 3.22.1 | keep host+JNI parity |
| Java language level | `java.language.version` → fallback `JAVA_LANGUAGE_VERSION` | 17 | 17 | 21 (when toolchain validated) |
| Gradle runtime JVM | `gradle.java.runtime.version` → fallback `GRADLE_JAVA_RUNTIME_VERSION` | 17 | 17 | `gradle.max.runtime.java.version` (default 21) |


Property precedence rule (to avoid config drift):
- Canonical property names use dotted lowercase keys (for example: `compile.api`, `tools.version`).
- Legacy aliases in uppercase snake case (for example: `COMPILE_API`, `TOOLS_VERSION`) are fallback-only for backward compatibility.
- When a legacy alias is used, the Gradle bootstrap emits a deprecation warning and continues.

Strictness control by pipeline context:
- A validação de bootstrap (`verifyBootstrapAssets`) e a validação final (`verifyGradleRuntimeJvm` + gates de API/ABI) compartilham a mesma política de `buildStrict` (warning em modo local, bloqueante em CI/release).
- `-PbuildStrict=false` (default): local/debug mode; validations emit warnings where allowed.
- `-PbuildStrict=true`: official CI/release mode; validations are blocking.

## CI blocking checks by pipeline type
- Official release CI (`buildStrict=true`):
  - `verifyGradleRuntimeJvm` (blocking)
  - API/ABI validations (`verifyMinApiAbiCompatibility`, release target checks) (blocking)
  - `verifyBootstrapAssets` + `verifyRepoFileDependencies` (blocking; requires Python)
- Local/dev or debug CI (`buildStrict=false`):
  - Same checks run, but max-JVM/API-ABI non-release gates can warn.
  - Python-dependent checks are skipped with warning if Python is unavailable.

## Release oficial vs validação interna (unsigned/placeholder)
- **Release oficial (assinado)**:
  - exige keystore + credenciais de signing de produção;
  - exige `app/google-services.json` real (sem placeholder);
  - **não** usar `-PCI_INTERNAL_VALIDATION=true`.
- **Validação interna (unsigned)**:
  - permite `-PALLOW_UNSIGNED_RELEASE=true`;
  - permite placeholder Firebase **somente** com sinal explícito `-PCI_INTERNAL_VALIDATION=true` (opcionalmente junto de `-PALLOW_PLACEHOLDER_FIREBASE_FOR_RELEASE=true`);
  - usada para CI interno quando segredos de produção não estão disponíveis.


## Selftest matrix expectations
Canonical gate names:
- Make: `make run-selftest`
- CMake: `cmake --build <build-dir> --target run_selftest`

Expected per architecture:

| Environment | Canonical gate | Required architecture-specific selftests | Artifact logs |
|---|---|---|---|
| Host x86_64 Linux | `make run-selftest` and/or `run_selftest` | `rmr_casm_bridge_selftest` (supported ABI), `rmr_neon_simd_selftest` not required | `bench/results/selftest-host-x86_64.log`, `bench/results/rmr_casm_bridge_selftest-x86_64.log` |
| Host arm64 Linux (aarch64/arm64) | `make run-selftest` and/or `run_selftest` | `rmr_neon_simd_selftest` (required), `rmr_casm_bridge_selftest` optional/unsupported | `bench/results/selftest-host-arm64.log`, `bench/results/rmr_neon_simd_selftest-arm64.log`, `bench/results/rmr_casm_bridge_selftest-arm64.log` |
| Android (NDK / app ABI lanes) | Native selftests must be wired through the same gate contract before release | ABI-specific execution required for `arm64-v8a`; other ABIs per policy (`APP_ABI_POLICY`) | Keep per-ABI logs in CI artifacts and fail lane when required selftests are missing or failing |
