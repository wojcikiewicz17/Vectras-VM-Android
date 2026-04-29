# android/ (legado ativo para compatibilidade local)

Este diretório é **legado** e existe apenas para compatibilidade com fluxos antigos.

## Status do RAFCoder
- ✅ RAFCoder **ativo no módulo canônico `:app` na raiz**.
- ⚠️ `android/` **não é fonte de verdade** para CI/release oficial.
- ✅ `com.rafacodephi.app` foi portado para o entrypoint canônico `:app`.

## Fonte de verdade única
Use sempre a raiz do repositório:

```bash
./tools/gradle_with_jdk21.sh :app:assembleDebug
./tools/gradle_with_jdk21.sh :app:assembleRelease
./tools/gradle_with_jdk21.sh :app:bundleRelease
```

## Sobre `android/`
- O hard-fail anterior em `android/settings.gradle` foi removido para permitir bootstrap/local debug legado.
- `android/gradlew` e `android/gradlew.bat` são apenas shims que delegam para o wrapper canônico da raiz.
- Releases, assinatura, artefatos oficiais e CI continuam centralizados na raiz.
