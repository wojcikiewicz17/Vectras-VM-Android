# BETA_BOOTSTRAP_REALITY_CHECK

## STATUS: BETA_BLOCKED (aguardando CI canônico no commit corrente)

## Evidências obrigatórias
- **Arquivo**: bootstrap por ABI presente em `app/src/main/assets/bootstrap/` (`arm64-v8a.tar`, `armeabi-v7a.tar`, `x86.tar`, `x86_64.tar`).
- **Task Gradle**: `:app:verifyTermuxBootstrapAbiCoverage` e `:app:verifyTermuxBootstrapPackagingRules`.
- **Script/CI**: `.github/workflows/android-ci.yml` executa `./tools/ci/prepare_android_env.sh` e build com tasks de assemble/test.

## Critério para fechamento
- Cobertura de bootstrap para ABIs principais de runtime Android confirmada.
- Gate de empacotamento de bootstrap definido em task Gradle dedicada.


## Estado no commit corrente (2026-05-06)
- Sem Android SDK no ambiente local, `:app:syncShellLoaderBootstrap` não pôde ser executado neste commit.
- Consequência: `tools/verify_bootstrap_assets.py --strict-generated-assets` falha por ausência de `loader.apk` gerado em `app/build/generated/bootstrapAssets/bootstrap/loader.apk`.
- Este documento só deve voltar para `READY` após CI canônico verde com evidência de geração/cópia do `loader.apk`.
