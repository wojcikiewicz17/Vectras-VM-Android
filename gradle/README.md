<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# gradle/

## Camada 1 — Propósito do diretório
Wrapper e bootstrap de build.

## Camada 2 — Estrutura (até 3 níveis)
- Nível 1: `gradle/`
- Nível 2: `wrapper/`
- Nível 3: detalhamento por arquivo em [`FILES_MAP.md`](FILES_MAP.md).

## Camada 3 — Arquivos e vínculos
- Catálogo completo: [`FILES_MAP.md`](FILES_MAP.md)
- Contexto global de camadas: [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md)

## Cadeia de comando (lógica de inspeção)
```bash
find gradle -maxdepth 3 -type d | sort
sed -n '1,120p' gradle/FILES_MAP.md
```

## Bootstrap Android SDK (local)
Quando o ambiente local ainda não tem `local.properties` coerente com o contrato do repositório:

```bash
./gradlew prepareAndroidLocalEnv
./gradlew verifyAndroidSdkPackages
```

O task `prepareAndroidLocalEnv` usa o mesmo contrato de fallback de SDK aplicado no build (`.android-sdk`, `/workspace/android-sdk`, `/usr/lib/android-sdk`, `/opt/android-sdk`, `/opt/android-sdk-linux`, `$HOME/Android/Sdk`) e grava apenas `sdk.dir` em `local.properties` (sem `ndk.dir`). 
