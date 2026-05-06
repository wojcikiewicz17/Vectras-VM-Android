# BETA_APK_ABI_BOOTSTRAP_REPORT

## STATUS: BETA_BLOCKED (inventário depende de APK gerado no commit corrente)

## Evidências obrigatórias
- **Arquivo**: bibliotecas JNI por ABI em `app/src/main/jniLibs/{armeabi-v7a,arm64-v8a,x86,x86_64}/libXlorie.so`.
- **Task Gradle**: `:app:checkNativeExtendedMatrix`, `:app:checkNativeAllMatrix`, `:app:verifyDeliveredCompiledArtifacts`.
- **Script/CI**: `.github/workflows/android-native-ci.yml` roda assemble para `debug`/`release` com política ABI parametrizada.

## Critério para fechamento
- Matriz ABI está definida no build e no CI.
- Verificação de artefatos compilados foi formalizada para impedir release sem saída binária.


## Estado no commit corrente (2026-05-06)
- A geração de inventário final depende de APK produzido no commit corrente (`reports/APK_ABI_BOOTSTRAP_INVENTORY.md`).
- Sem Android SDK no ambiente local desta execução, o APK não foi recompilado aqui; manter status bloqueado até CI canônico publicar o inventário.
