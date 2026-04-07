<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# gradle/FILES_MAP.md

Mapa arquivo-a-arquivo em três linhas por item: papel, ligação e comando de inspeção.

## `gradle/README.md`
- **Papel**: documentação local do diretório.
- **Liga com**: ver [`gradle/README.md`](README.md) e [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md) para contexto de camadas.
- **Inspeção**: `file "gradle/README.md"` e, quando texto, `sed -n "1,80p" "gradle/README.md"`.

## `gradle/wrapper/gradle-wrapper.jar`
- **Papel**: artefato binário versionado para execução/compatibilidade.
- **Liga com**: ver [`gradle/README.md`](README.md) e [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md) para contexto de camadas.
- **Inspeção**: `file "gradle/wrapper/gradle-wrapper.jar"` e, quando texto, `sed -n "1,80p" "gradle/wrapper/gradle-wrapper.jar"`.

## `gradle/wrapper/gradle-wrapper.properties`
- **Papel**: configuração declarativa de build, metadata ou catálogo.
- **Liga com**: ver [`gradle/README.md`](README.md) e [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md) para contexto de camadas.
- **Inspeção**: `file "gradle/wrapper/gradle-wrapper.properties"` e, quando texto, `sed -n "1,80p" "gradle/wrapper/gradle-wrapper.properties"`.

