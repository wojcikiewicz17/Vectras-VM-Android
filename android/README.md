# android/ (legacy-only / unsupported for release)

Este diretório é **somente legado** e **não é suportado para release**.

## Status
- 🚫 legacy-only
- 🚫 unsupported for release
- 🚫 não usar em CI/CD, release, assinatura ou documentação operacional
- ✅ mantido apenas para histórico/auditoria

## Comandos oficiais (na raiz do repositório)
Use apenas o caminho canônico na raiz:

```bash
./tools/gradle_with_jdk21.sh :app:assembleDebug
./tools/gradle_with_jdk21.sh :app:assembleRelease
./tools/gradle_with_jdk21.sh :app:bundleRelease
```

## Guardrail
- `android/settings.gradle` falha de forma explícita (hard-fail) para bloquear uso acidental de `android/`.
