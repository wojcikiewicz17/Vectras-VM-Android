# BETA_APK_ABI_BOOTSTRAP_REPORT

## STATUS: READY (FECHADO)

## Evidências obrigatórias
- **Arquivo**: bibliotecas JNI por ABI em `app/src/main/jniLibs/{armeabi-v7a,arm64-v8a,x86,x86_64}/libXlorie.so`.
- **Task Gradle**: `:app:checkNativeExtendedMatrix`, `:app:checkNativeAllMatrix`, `:app:verifyDeliveredCompiledArtifacts`.
- **Script/CI**: `.github/workflows/android-native-ci.yml` roda assemble para `debug`/`release` com política ABI parametrizada.

## Critério fechado
- Matriz ABI está definida no build e no CI.
- Verificação de artefatos compilados foi formalizada para impedir release sem saída binária.
