<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# shell-loader/

## Camada 1 — Propósito do diretório
Loader Android e submódulo de stubs.

## Camada 2 — Estrutura (até 3 níveis)
- Nível 1: `shell-loader/`
- Nível 2: `.idea/`, `release/`, `src/`, `stub/`
- Nível 3: detalhamento por arquivo em [`FILES_MAP.md`](FILES_MAP.md).

## Camada 3 — Arquivos e vínculos
- Catálogo completo: [`FILES_MAP.md`](FILES_MAP.md)
- Contexto global de camadas: [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md)

## Cadeia de comando (lógica de inspeção)
```bash
find shell-loader -maxdepth 3 -type d | sort
sed -n '1,120p' shell-loader/FILES_MAP.md
```

## Contrato formal do artefato `loader.apk`

Para a cadeia de bootstrap do módulo `app`, o artefato do shell-loader segue o contrato abaixo:

- **Task de validação:** `:app:verifyShellLoaderArtifact`
- **Origem esperada:** `shell-loader/build/outputs/apk/<loaderVariant>/loader.apk`
- **Pré-condição de assemble:** `:shell-loader:assemble<LoaderVariant>`
- **Regras obrigatórias:**
  1. Arquivo deve existir no caminho exato da variant selecionada (`-PloaderVariant`, padrão `release`).
  2. Arquivo deve ser regular (`isFile`).
  3. Tamanho deve ser maior que zero.
- **Regra opcional de conteúdo interno:**
  - Se `-PverifyShellLoaderManifest=true`, o APK deve conter `AndroidManifest.xml`.
- **Erro padronizado:**
  - Falhas usam mensagem única no formato:
    - `Shell loader artifact inválido em '<caminho absoluto esperado>': <motivo>.`
- **Encadeamento obrigatório:**
  - `:app:syncShellLoaderBootstrap` depende de `:app:verifyShellLoaderArtifact`.

