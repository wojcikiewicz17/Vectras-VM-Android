<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# app/

## Camada 1 — Propósito do diretório
Módulo Android principal com UI, runtime e testes unitários.

## Camada 2 — Estrutura (até 3 níveis)
- Nível 1: `app/`
- Nível 2: `src/`
- Nível 3: detalhamento por arquivo em [`FILES_MAP.md`](FILES_MAP.md).

## Camada 3 — Arquivos e vínculos
- Catálogo completo: [`FILES_MAP.md`](FILES_MAP.md)
- Contexto global de camadas: [`docs/THREE_LAYER_ANALYSIS.md`](../docs/THREE_LAYER_ANALYSIS.md)

## Cadeia de comando (lógica de inspeção)
```bash
find app -maxdepth 3 -type d | sort
sed -n '1,120p' app/FILES_MAP.md
```

## Modos operacionais de build/release (Gradle)

> Fonte de verdade das flags: `app/build.gradle`.

### 1) `debug-local`
- Objetivo: ciclo local mais rápido, com bypass apenas de pré-validações pesadas.
- Propriedades:
  - `-PdevFastPath=true`
  - `-PciRelease=false`
  - `-Psigning_mode=unsigned` (ou `auto` sem credenciais)
  - `-POFFICIAL_CI=false` (opcional local)
- Efeito:
  - Pode pular `validateCriticalNativeAbiLayer`, `verifyShellLoaderArtifact` e `syncShellLoaderBootstrap`.
  - Logs de classificação explicam cada skip (`ENV`, `CONFIG`, `ABI_CONTRACT`, `SIGNING`, `BOOTSTRAP`).

### 2) `debug-internal-arm32-arm64`
- Objetivo: validação interna com matriz dual ARM.
- Propriedades:
  - `-PAPP_ABI_POLICY=arm32-arm64`
  - `-PCI_INTERNAL_VALIDATION=true`
  - `-PcheckVariant=debug`
  - Opcional: `-PdevFastPath=true` para acelerar apenas trilha local.
- Observação:
  - Mantém regras de contrato ABI/política interna; `APP_ABI_POLICY=arm32-arm64` exige `CI_INTERNAL_VALIDATION=true`.

### 3) `release-unsigned-internal`
- Objetivo: validação interna de release sem assinatura oficial.
- Propriedades:
  - `-PciRelease=false`
  - `-Psigning_mode=unsigned`
  - `-PALLOW_UNSIGNED_RELEASE=true`
  - `-PALLOW_PLACEHOLDER_FIREBASE_FOR_RELEASE=true` (apenas validação interna)
  - `-POFFICIAL_CI=false`
- Observação:
  - Mesmo com unsigned, tarefas release/perfRelease mantêm trilha estrita de pré-validações pesadas.

### 4) `release-signed-official`
- Objetivo: trilha oficial de distribuição.
- Propriedades:
  - `-PciRelease=true`
  - `-Psigning_mode=signed`
  - `-POFFICIAL_CI=true` (ou `GITHUB_ACTIONS=true`)
  - Credenciais de assinatura release válidas (`android.injected.signing.*` ou `VECTRAS_RELEASE_*`)
- Efeito:
  - Sempre estrito: sem bypass de pré-validação pesada.
  - Validação de assinatura e contrato release/perfRelease bloqueante.

## Fontes nativas obrigatórias do APK

### Obrigatório
- `app/src/main/cpp/vectra_core_accel.c`
  - Motivo: exporta os símbolos JNI consumidos pelo app (`Java_com_vectras_vm_core_NativeFastPath_*` e `Java_com_vectras_vm_core_NativeLogcatBridge_*`) e faz a ponte direta para o núcleo `RmR_UnifiedKernel_*`.
- `engine/rmr/src/rmr_unified_kernel.c`
  - Motivo: provê os símbolos-chave `RmR_UnifiedKernel_*` usados pelo bridge JNI, incluindo init/shutdown, ingest/process/route/verify/audit, operações de arena e utilitários bitwise/checksum.
- `engine/rmr/src/rmr_hw_detect.c`
  - Motivo: provê `RmR_HW_Detect`, dependência direta de `RmR_UnifiedKernel_Detect`/`RmR_UnifiedKernel_Init` para montar o contrato de capacidades de hardware retornado ao app.

### Opcional
- Não há `.c` opcionais no target atual do APK (`vectra_core_accel`): os três arquivos acima são todos os sources compilados no `add_library`.

### Bibliotecas/targets externos para `RmR_UnifiedKernel_*`
- Target local obrigatório no APK: `vectra_core_accel` (biblioteca `SHARED` gerada pelo CMake do app).
- Biblioteca do NDK necessária no link: `log` (`find_library(log-lib log)` + `target_link_libraries(vectra_core_accel ${log-lib})`).
- Runtime C/pthreads vem do toolchain Android (não há target externo adicional fora do C local para resolver `RmR_UnifiedKernel_*`; os símbolos são resolvidos pelos `.c` listados acima).

### Manutenção
- Sempre que entrar um novo símbolo JNI em `app/src/main/cpp/vectra_core_accel.c`, atualizar esta seção para manter o mapeamento “símbolo JNI -> fonte `.c` obrigatória” e o inventário de dependências de link do APK.


### Build local ARM32+ARM64 (release)
- Unsigned interno:
```bash
./tools/ci/build_arm32_arm64_release_bundle.sh unsigned
```
- Signed oficial (exige secrets/credenciais de assinatura):
```bash
./tools/ci/build_arm32_arm64_release_bundle.sh signed
```

A rotina executa `assembleRelease` e `verifyDeliveredCompiledArtifacts` com política ABI `arm32-arm64`.
