<!-- DOC_ORG_SCAN: 2026-04-27 | source-scan: synchronized-with-code -->

# AI Build/Release Index (Source-of-Truth)

## 1) Objetivo
Índice operacional para IA/automação navegar no pipeline Android sem ambiguidade entre código, CI e documentação.

## 2) Fontes canônicas
- Workflow Android canônico: `.github/workflows/android-ci.yml`
- Contrato ABI: `tools/ci/abi_profiles_contract.json`
- Regras de build/signing: `app/build.gradle`
- Baseline de versões: `gradle.properties`
- Wrapper oficial Gradle: `tools/gradle_with_jdk21.sh`
- Script local de matriz de artefatos (signed + unsigned): `tools/ci/build_artifact_matrix_local.sh`

## 3) Perfis ABI oficiais
| Perfil | APP_ABI_POLICY | ABIs | Canal |
|---|---|---|---|
| `official_arm64` | `arm64-only` | `arm64-v8a` | release oficial/store |
| `official_arm32_arm64` | `arm32-arm64` | `arm64-v8a, armeabi-v7a` | compatibilidade não-store |
| `internal_arm32_arm64` | `arm32-arm64` | `arm64-v8a, armeabi-v7a` | validação interna |

> Fonte de verdade completa: `tools/ci/abi_profiles_contract.json`.

## 4) Lanes de execução
| Lane | signing_mode | Perfil ABI resolvido | Uso |
|---|---|---|---|
| `debug-local` | unsigned | `official_arm32_arm64` | debug local |
| `debug-internal-arm32-arm64` | unsigned | `internal_arm32_arm64` | validação interna debug |
| `release-unsigned-internal` | unsigned | `internal_arm32_arm64` | release interno sem assinatura oficial |
| `release-signed-official` | signed | `official_arm64` | release oficial |

## 5) Artefatos e caminhos
- APK release: `app/build/outputs/apk/release/app-release.apk`
- AAB release: `app/build/outputs/bundle/release/app-release.aab`
- Relatório de artefatos: `app/build/reports/artifacts/compiled-artifacts-report.json`
- Matriz local consolidada (signed+unsigned): `artifacts/local-matrix/manifest.json`

## 6) Comandos canônicos
### 6.1 Release unsigned (interno, arm32+arm64)
```bash
./tools/gradle_with_jdk21.sh :app:assembleRelease :app:bundleRelease \
  -PciRelease=false -Psigning_mode=unsigned -PCI_INTERNAL_VALIDATION=true \
  -PAPP_ABI_POLICY=arm32-arm64 -PSUPPORTED_ABIS=arm64-v8a,armeabi-v7a
```

### 6.2 Release signed interno (keystore local, arm32+arm64)
```bash
./tools/ci/build_artifact_matrix_local.sh
```

### 6.3 Release oficial assinado (somente arm64)
```bash
./tools/gradle_with_jdk21.sh :app:assembleRelease :app:bundleRelease \
  -PciRelease=true -Psigning_mode=signed \
  -PAPP_ABI_POLICY=arm64-only -PSUPPORTED_ABIS=arm64-v8a \
  -Pandroid.injected.signing.store.file=<keystore> \
  -Pandroid.injected.signing.store.password=<store-pass> \
  -Pandroid.injected.signing.key.alias=<alias> \
  -Pandroid.injected.signing.key.password=<key-pass>
```

## 7) Regras mandatórias
- Não usar `android/` legado como trilha de build.
- Não converter release oficial para unsigned por conveniência.
- `signing_mode=signed` sem credencial válida deve falhar.
- `release-signed-official` deve manter `official_arm64`.
- Para dual-ABI ARM32/ARM64, usar trilha interna/compatibilidade explícita.
