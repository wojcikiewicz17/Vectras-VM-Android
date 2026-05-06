# BETA_BOOTSTRAP_REALITY_CHECK

## STATUS: READY (FECHADO)

## Evidências obrigatórias
- **Arquivo**: bootstrap por ABI presente em `app/src/main/assets/bootstrap/` (`arm64-v8a.tar`, `armeabi-v7a.tar`, `x86.tar`, `x86_64.tar`).
- **Task Gradle**: `:app:verifyTermuxBootstrapAbiCoverage` e `:app:verifyTermuxBootstrapPackagingRules`.
- **Script/CI**: `.github/workflows/android-ci.yml` executa `./tools/ci/prepare_android_env.sh` e build com tasks de assemble/test.

## Critério fechado
- Cobertura de bootstrap para ABIs principais de runtime Android confirmada.
- Gate de empacotamento de bootstrap definido em task Gradle dedicada.
