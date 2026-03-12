# Toolchain licensing map (Termux ARM64 orchestrator)

Este arquivo mapeia proveniência/licença mínima dos componentes externos consumidos no fluxo de APK.

## Componentes

- android-cmdline-tools
  - Origem: https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
  - Licença: Android SDK License
- android-ndk
  - Origem: `sdkmanager "ndk;27.2.12479018"`
  - Licença: Android SDK License
- android-cmake
  - Origem: `sdkmanager "cmake;3.22.1"`
  - Licença: Android SDK License
- jdk
  - Origem: JDK local detectado por `tools/gradle_with_jdk21.sh` (major 21/17)
  - Licença: distribuição do JDK instalado no host (tipicamente GPL-2.0-with-classpath-exception para OpenJDK)

## Observações

- Este repositório não redistribui diretamente binários oficiais de SDK/NDK/CMake/JDK.
- Integridade de download de cmdline-tools é validada por SHA256 em `bootstrap-termux-android15.sh`.
- Inventário de componentes também existe em `toolchain-manifests/toolchain-bom.json`.
