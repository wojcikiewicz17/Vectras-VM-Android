# BETA_BOOTSTRAP_REALITY_CHECK

Data: 2026-05-05 (UTC)

## Resultado objetivo (13 perguntas)

1. Existem `.tar` em `app/src/main/assets/bootstrap/`? **SIM** (`arm64-v8a.tar`, `armeabi-v7a.tar`, `x86.tar`, `x86_64.tar`).
2. Cada tar é válido? **SIM** (abertura e leitura via `tarfile` OK no `tools/verify_bootstrap_assets.py`).
3. Cada tar tem arquivos essenciais? **PARCIALMENTE VALIDADO**: os tars não estão vazios (363/364 entradas), mas não existe assert semântico completo de conteúdo runtime no script atual.
4. Existe `loader.apk` versionado em assets? **NÃO** (`app/src/main/assets/bootstrap/loader.apk` ausente).
5. `syncShellLoaderBootstrap` gera/copia `loader.apk`? **SIM por contrato Gradle** (task existe e copia para `app/build/generated/bootstrapAssets/bootstrap/loader.apk`), mas **NÃO EXECUTADO** neste ambiente por falta de Android SDK.
6. `verify_bootstrap_assets.py` passa? **NÃO** no modo estrito: falha por ausência de `loader.apk` (versionado ou gerado).
7. `verify_bootstrap_contract.sh` passa? **NÃO EXECUTADO COM SUCESSO**: bloqueado por ausência de Android SDK.
8. `libtermux-bootstrap.so` é gerada? **CONTRATO SIM** (target `termux-bootstrap` em CMake para ABIs suportadas), build não concluído neste ambiente.
9. `nativeGetZip()` retorna ZIP real ou NULL? **NO ESTADO ATUAL: NULL CONTROLADO** se payload não foi embutido.
10. `TERMUX_BOOTSTRAP_PAYLOAD_DATA` está definido? **NÃO ENCONTRADO** no repositório/build atual.
11. `TERMUX_BOOTSTRAP_PAYLOAD_SIZE` está definido? **NÃO ENCONTRADO** no repositório/build atual.
12. Runtime usa TAR assets ou ZIP JNI? **OFICIAL ATUAL: TAR assets + shell-loader** (cadeia de verificação e cópia em Gradle/Python aponta isso).
13. Caminho oficial para beta? **MODELO A (TAR + loader.apk)**.

## Classificação

- `BOOTSTRAP_TAR_READY`
- `BOOTSTRAP_LOADER_WIRED`
- `BOOTSTRAP_JNI_PLACEHOLDER`
- `BOOTSTRAP_PENDING_CANONICAL_CI` (por ausência de `loader.apk` e impossibilidade de executar pipeline completo sem SDK)

## Evidências de execução

- `python3 tools/verify_bootstrap_assets.py --strict-generated-assets` → requer execução do pipeline Gradle com SDK para materializar `loader.apk` gerado.
- `./tools/ci/verify_bootstrap_contract.sh ...` → erro: Android SDK não encontrado.
