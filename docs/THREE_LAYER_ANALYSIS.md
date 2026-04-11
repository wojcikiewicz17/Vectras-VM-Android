<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Análise Estrutural em Três Camadas (3-Layer)

Este documento define a cadeia formal de documentação em **três camadas** para cada diretório de primeiro nível, com ligação para mapa detalhado de arquivos e cadeia de comandos de validação.

## Camadas padrão
1. **Camada 1 — Diretório**: propósito técnico e responsabilidade primária.
2. **Camada 2 — Subdiretórios**: organização interna por domínio (código, assets, scripts, testes).
3. **Camada 3 — Arquivos**: mapa arquivo-a-arquivo com papel, ligação e comando de inspeção no `FILES_MAP.md` local.

## Navegação por diretório
### `3dfx/`
- Camada 1: ver [`3dfx/README.md`](../3dfx/README.md).
- Camada 2: subestruturas detectadas: (sem subdiretórios relevantes).
- Camada 3: mapa detalhado em [`3dfx/FILES_MAP.md`](../3dfx/FILES_MAP.md).

### `app/`
- Camada 1: ver [`app/README.md`](../app/README.md).
- Camada 2: subestruturas detectadas: app/src, app/src/main, app/src/main/aidl, app/src/main/aidl/com, app/src/main/aidl/com/vectras ....
- Camada 3: mapa detalhado em [`app/FILES_MAP.md`](../app/FILES_MAP.md).

### `archive/`
- Camada 1: ver [`archive/README.md`](../archive/README.md).
- Camada 2: subestruturas detectadas: archive/experimental, archive/experimental/rafael_melo_reis_bundle, archive/experimental/rafael_melo_reis_bundle/teoremas, archive/experimental/rafael_melo_reis_bundle/teoremas/modulos, archive/experimental/rafael_melo_reis_bundle/teoremas/modulos/camadas ....
- Camada 3: mapa detalhado em [`archive/FILES_MAP.md`](../archive/FILES_MAP.md).

### `bench/`
- Camada 1: ver [`bench/README.md`](../bench/README.md).
- Camada 2: subestruturas detectadas: bench/results, bench/scripts, bench/src.
- Camada 3: mapa detalhado em [`bench/FILES_MAP.md`](../bench/FILES_MAP.md).

### `demo_cli/`
- Camada 1: ver [`demo_cli/README.md`](../demo_cli/README.md).
- Camada 2: subestruturas detectadas: demo_cli/src.
- Camada 3: mapa detalhado em [`demo_cli/FILES_MAP.md`](../demo_cli/FILES_MAP.md).

### `docs/`
- Camada 1: ver [`docs/README.md`](../docs/README.md).
- Camada 2: subestruturas detectadas: docs/navigation.
- Camada 3: mapa detalhado em [`docs/FILES_MAP.md`](../docs/FILES_MAP.md).

### `engine/`
- Camada 1: ver [`engine/README.md`](../engine/README.md).
- Camada 2: subestruturas detectadas: engine/rmr, engine/rmr/include, engine/rmr/src, engine/vectra_policy_kernel, engine/vectra_policy_kernel/src ....
- Camada 3: mapa detalhado em [`engine/FILES_MAP.md`](../engine/FILES_MAP.md).

### `fastlane/`
- Camada 1: ver [`fastlane/README.md`](../fastlane/README.md).
- Camada 2: subestruturas detectadas: fastlane/metadata, fastlane/metadata/android, fastlane/metadata/android/en-US, fastlane/metadata/android/en-US/images, fastlane/metadata/android/en-US/images/phoneScreenshots.
- Camada 3: mapa detalhado em [`fastlane/FILES_MAP.md`](../fastlane/FILES_MAP.md).

### `gradle/`
- Camada 1: ver [`gradle/README.md`](../gradle/README.md).
- Camada 2: subestruturas detectadas: gradle/wrapper.
- Camada 3: mapa detalhado em [`gradle/FILES_MAP.md`](../gradle/FILES_MAP.md).

### `reports/`
- Camada 1: ver [`reports/README.md`](../reports/README.md).
- Camada 2: subestruturas detectadas: reports/baremetal, reports/metrics.
- Camada 3: mapa detalhado em [`reports/FILES_MAP.md`](../reports/FILES_MAP.md).

### `resources/`
- Camada 1: ver [`resources/README.md`](../resources/README.md).
- Camada 2: subestruturas detectadas: resources/android, resources/android/res, resources/android/res/mipmap-anydpi-v26, resources/android/res/mipmap-hdpi, resources/android/res/mipmap-mdpi ....
- Camada 3: mapa detalhado em [`resources/FILES_MAP.md`](../resources/FILES_MAP.md).

### `runtime/`
- Camada 1: ver [`runtime/README.md`](../runtime/README.md).
- Camada 2: subestruturas detectadas: runtime/showcase.
- Camada 3: mapa detalhado em [`runtime/FILES_MAP.md`](../runtime/FILES_MAP.md).

### `shell-loader/`
- Camada 1: ver [`shell-loader/README.md`](../shell-loader/README.md).
- Camada 2: subestruturas detectadas: shell-loader/.idea, shell-loader/release, shell-loader/src, shell-loader/src/main, shell-loader/src/main/java ....
- Camada 3: mapa detalhado em [`shell-loader/FILES_MAP.md`](../shell-loader/FILES_MAP.md).

### `terminal-emulator/`
- Camada 1: ver [`terminal-emulator/README.md`](../terminal-emulator/README.md).
- Camada 2: subestruturas detectadas: terminal-emulator/src, terminal-emulator/src/main, terminal-emulator/src/main/java, terminal-emulator/src/main/java/com, terminal-emulator/src/main/java/com/termux ....
- Camada 3: mapa detalhado em [`terminal-emulator/FILES_MAP.md`](../terminal-emulator/FILES_MAP.md).

### `terminal-view/`
- Camada 1: ver [`terminal-view/README.md`](../terminal-view/README.md).
- Camada 2: subestruturas detectadas: terminal-view/src, terminal-view/src/main, terminal-view/src/main/java, terminal-view/src/main/java/com, terminal-view/src/main/java/com/termux ....
- Camada 3: mapa detalhado em [`terminal-view/FILES_MAP.md`](../terminal-view/FILES_MAP.md).

### `tools/`
- Camada 1: ver [`tools/README.md`](../tools/README.md).
- Camada 2: subestruturas detectadas: tools/apk, tools/baremetal.
- Camada 3: mapa detalhado em [`tools/FILES_MAP.md`](../tools/FILES_MAP.md).

### `web/`
- Camada 1: ver [`web/README.md`](../web/README.md).
- Camada 2: subestruturas detectadas: web/data.
- Camada 3: mapa detalhado em [`web/FILES_MAP.md`](../web/FILES_MAP.md).

## Cadeia de comandos recomendada (auditoria)
```bash
git ls-files
find . -maxdepth 2 -type d | sort
for d in 3dfx app archive bench demo_cli docs engine fastlane gradle reports resources runtime shell-loader terminal-emulator terminal-view tools web; do echo "## $d"; sed -n "1,120p" "$d/README.md"; done
```
