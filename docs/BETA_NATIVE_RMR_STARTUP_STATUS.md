# BETA_NATIVE_RMR_STARTUP_STATUS

## STATUS: RISK (FECHADO)

## Evidências obrigatórias
- **Arquivo**: fontes nativas e ABI bridge em `app/src/main/cpp/` (`lowlevel_bridge.c`, `lowlevel_abi.c`, `vectra_lowlevel_backend_*.c`).
- **Task Gradle**: `:app:validateRmrEquivalence`, `:app:collectVectraGradeBenchmarks`.
- **Script/CI**: `.github/workflows/host-ci.yml` executa CMake + `rmr_torus_flow_selftest`; `.github/workflows/android-native-ci.yml` valida build Android nativo.

## Critério fechado
- Stack nativa tem contratos e smoke de host, mas permanece **RISK** até comprovação de startup RMR fim-a-fim em device para arm32/arm64 sob release candidate assinado.
