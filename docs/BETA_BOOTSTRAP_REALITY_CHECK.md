# Beta Bootstrap Reality Check

## Decisão explícita de contrato

**Contrato oficial de bootstrap: `TAR` em `app/src/main/assets/bootstrap/*.tar` + `loader.apk` obrigatório**.

- Bootstraps oficiais por ABI: `arm64-v8a.tar`, `armeabi-v7a.tar`, `x86.tar`, `x86_64.tar`.
- `loader.apk` deve existir no caminho versionado (`app/src/main/assets/bootstrap/loader.apk`) ou no caminho gerado por build (`app/build/generated/bootstrapAssets/bootstrap/loader.apk`) após `:app:syncShellLoaderBootstrap`.
- A ausência de `loader.apk` no caminho TAR é quebra de contrato.

## JNI ZIP (compatibilidade)

O caminho JNI ZIP (`TermuxInstaller#nativeGetZip`) é **compatibilidade/fallback**, não trilha oficial de release.

- Pode ser mantido para cenários legados e validação controlada.
- Não substitui o contrato oficial TAR + `loader.apk` para release.
- CI deve validar explicitamente o contrato oficial para evitar ambiguidade.
