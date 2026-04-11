<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Toolchain core modules

Módulos internos para interoperabilidade do fluxo APK com JDK/SDK/NDK/CMake.

## Scripts

- `toolchain-core/detect-host.sh`
  - Detecta arquitetura/OS/page-size e sinais de NEON/ASIMD.
- `toolchain-core/resolve-toolchain.sh`
  - Resolve variáveis de caminho/versão da toolchain local.
- `toolchain-core/activate-env.sh`
  - Exporta ambiente (`ANDROID_SDK_ROOT`, `ANDROID_HOME`, `PATH`) para comandos seguintes.
- `toolchain-core/verify-toolchain.sh`
  - Verifica layout local de sdkmanager/NDK/CMake após bootstrap.

## Contrato mínimo

Entradas:
- `ANDROID_SDK_ROOT`, `ANDROID_NDK_VERSION`, `ANDROID_CMAKE_VERSION` (opcionais; têm defaults).

Saídas:
- variáveis exportadas com paths resolvidos
- status de verificação para gate/build

## Uso no orquestrador

O `orchestrate-build.sh` chama `detect-host.sh` para registro de telemetria local e `verify-toolchain.sh` ao final do bootstrap para validar completude do ambiente.
