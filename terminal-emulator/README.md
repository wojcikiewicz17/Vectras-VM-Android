# terminal-emulator/

## Camada 1 — Propósito do diretório
Biblioteca de emulação de terminal.

## Camada 2 — Estrutura (até 3 níveis)
- Nível 1: `terminal-emulator/`
- Nível 2: `src/`
- Nível 3: detalhamento por arquivo em [`FILES_MAP.md`](FILES_MAP.md).

## Camada 3 — Arquivos e vínculos
- Catálogo completo: [`FILES_MAP.md`](FILES_MAP.md)
- Contexto global de camadas: [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md)

## Cadeia de comando (lógica de inspeção)
```bash
find terminal-emulator -maxdepth 3 -type d | sort
sed -n '1,120p' terminal-emulator/FILES_MAP.md
```

## Regra de ABIs (anti-regressão)
- O módulo `terminal-emulator` é **arm64-only** e valida isso explicitamente no `build.gradle`.
- A lista de exclusões em `packagingOptions.jniLibs` é gerada dinamicamente a partir de `rootProject.ext.supportedAbis`, em linha com a mesma política usada no módulo `app`.
- Se `APP_ABI_POLICY` / `SUPPORTED_ABIS` solicitar ABIs fora de `arm64-v8a`, o build falha cedo com mensagem clara para ajustar a política (`arm64-only`) ou restringir `SUPPORTED_ABIS=arm64-v8a`.
