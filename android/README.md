# android/ (LEGACY, DEPRECATED)

Este diretório é **legado** e não faz parte do pipeline oficial de build Android.

## Status
- ❌ Não é caminho canônico de build.
- ❌ Não deve ser usado em CI/CD, scripts de release ou documentação operacional.
- ✅ Mantido apenas para compatibilidade histórica/auditoria.

## Caminho canônico
Use sempre o projeto Gradle na raiz do repositório:

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

## Proteções ativas
- `android/settings.gradle` falha propositalmente para bloquear uso acidental deste caminho legado.
- `tools/ci/validate_android_sdk_alignment.sh` valida em CI que não existem referências oficiais ao caminho `android/` e que valores de SDK permanecem alinhados ao baseline canônico.
