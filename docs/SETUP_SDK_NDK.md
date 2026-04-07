# SDK/NDK Setup — Vectras VM / RAFAELIA

## Quick Start

1. **Copy local.properties:**
   ```bash
   cp local.properties.example local.properties
   # Edit: set sdk.dir to your Android SDK path
   ```

2. **Install required SDK packages:**
   ```bash
   sdkmanager --install \
     'platforms;android-36' \
     'build-tools;36.0.0' \
     'ndk;27.2.12479018' \
     'cmake;3.22.1'
   ```

3. **Build:**
   ```bash
   ./gradlew assembleDebug
   ```

## Android Compatibility Baseline (Android 10 → 16)

- **minSdk (runtime mínimo):** API 29 (**Android 10**).
- **targetSdk (release baseline):** API 36 (**Android 16**).
- **compileSdk (toolchain baseline):** API 36.
- **Native/JNI linker policy:** `-Wl,-z,max-page-size=16384` habilitado para bibliotecas JNI (`vectra_core_accel` e `termux-bootstrap`) para compatibilidade com dispositivos Android modernos usando page size de 16 KiB.

## JNI + Bootstrap Validation Checklist

```bash
# 1) valida toolchain Android (SDK/NDK/CMake + APIs baseline)
./gradlew validateAndroidSdkPackages

# 2) valida bootstrap nativo e cobertura de ABIs no APK
./gradlew :app:verifyTermuxBootstrapAbiCoverage -PcheckVariant=debug

# 3) gera APK debug com sincronização do loader bootstrap
./gradlew :app:assembleDebug
```

## NDK ABI Targets

| ABI | CPU | NEON/SIMD | CRC32 HW |
|-----|-----|-----------|----------|
| arm64-v8a | Cortex-A53+ | ✅ NEON | ✅ crc32cb |
| armeabi-v7a | Cortex-A7+ | ✅ VFPv4 | ❌ SW only |
| x86_64 | Goldfish/QEMU | ✅ SSE4.2 | ✅ _mm_crc32 |
| x86 | Goldfish | ✅ SSE2 | ❌ SW only |

## Engine Build (native, no Android)

```bash
make clean && make all     # builds all + selftests
make run-release-gate      # runs benchmark gate
cmake -S . -B build && cmake --build build -j$(nproc)
```

## QEMU Bootstrap

```bash
# x86_64 VM, 2GB RAM, KVM auto-detect
./tools/bootstrap_qemu.sh x86_64 2048

# ARM64 VM
./tools/bootstrap_qemu.sh aarch64 1024
```
