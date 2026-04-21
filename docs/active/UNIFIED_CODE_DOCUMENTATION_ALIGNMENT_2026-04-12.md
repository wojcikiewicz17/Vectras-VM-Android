<!-- DOC_ORG_SCAN: 2026-04-12 | source-scan: manual-native-core-and-bridges -->

# Alinhamento Unificado Código↔Documentação (2026-04-12)

## 1) Propósito formal
Este documento trata a documentação como **uma estrutura única** para eliminar drift entre o texto e o código efetivo. O foco desta revisão é o caminho que realmente está implementado em fonte:

- `app/src/main/cpp/*` (núcleo JNI + backends low-level + ponte de bootstrap);
- `app/src/main/java/com/vectras/vm/core/*` (ponte Java para o núcleo nativo);
- `app/src/main/cpp/CMakeLists.txt` (composição binária e política de build).

> Escopo deliberado: aprofundar onde há execução determinística, detecção de arquitetura/SIMD e integração JNI crítica para VM.

---

## 2) Estrutura única (camadas reais observadas)

1. **Camada de Build Nativo**: `CMakeLists.txt` decide quais unidades C/ASM entram na `libvectra_core_accel.so` e quando `termux-bootstrap` é compilada por ABI suportada.
2. **Camada de Detecção HW/ABI**: `hardware_profile_bridge.c` coleta ABI efetiva, bits SIMD, ponteiro e feature bits via `RmR_HW_Detect` + `getauxval`/`cpuid`.
3. **Camada de Seleção de Backend**: `lowlevel_bridge.c` seleciona uma vtable (fallback, arm64, armv7, x86_64, x86, riscv64) por ABI + máscara SIMD.
4. **Camada de Execução Low-Level**: cada backend implementa `reduce_xor`, `checksum32` e `crc32c`.
5. **Camada de Exposição JNI**: funções `Java_com_vectras_vm_core_*` exportam operações para Java.
6. **Camada Java de Orquestração**: `NativeFastPath`, `LowLevelBridge`, `HardwareProfileBridge`, `VmFlowNativeBridge` fazem fallback, telemetria e contratos de consumo.

---

## 3) Catálogo técnico por arquivo e funções (somente o que está no código)

### 3.1 `app/src/main/cpp/CMakeLists.txt`
- Define projeto C/ASM (`project(vectra_core_accel C ASM)`) e padrão C11.
- Injeta fontes da engine `engine/rmr` via `sources_rmr_core.cmake`.
- Cria `vectra_core_accel` com:
  - `vectra_core_accel.c`, `lowlevel_bridge.c`, `hardware_profile_bridge.c`;
  - todos os backends (`fallback`, `arm64`, `armv7`, `x86_64`, `x86`, `riscv64`);
  - grupos opcionais ASM/CASM por arquitetura.
- Garante compilação de `termux-bootstrap` apenas para ABIs Android suportadas (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`).
- Aplica flags ABI-específicas (`-march=armv8-a+crc+simd`, `-msse4.2`, etc.) e link option para page size 16 KiB no Android.

### 3.2 `app/src/main/cpp/hardware_profile_bridge.c`
**Funções implementadas:**
- `vectra_hw_effective_abi`: retorna string ABI em compile-time (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`, `riscv64`, `unknown`).
- `vectra_simd_mask` (estática): detecta SIMD por plataforma (`cpuid`, `xgetbv`, `getauxval`, flags HWCAP).
- `vectra_hw_runtime_simd_mask`: expõe a máscara SIMD.
- `Java_com_vectras_vm_core_HardwareProfileBridge_nativeCollectSnapshot`: retorna `jintArray[9]` com snapshot de HW.
- `Java_com_vectras_vm_core_HardwareProfileBridge_nativeEffectiveAbi`: retorna ABI para Java.
- `vectra_hw_collect_snapshot`: preenche vetor com `RmR_HW_Detect` + SIMD runtime.

### 3.3 `app/src/main/cpp/lowlevel_bridge.c`
**Funções implementadas (núcleo de binding):**
- `vectra_select_simd_mask` e `vectra_bind_backend_once`: resolvem backend por ABI+SIMD com fallback seguro.
- `vectra_backend`: lazy-init thread-safe (atômico).

**Funções JNI exportadas:**
- `nativeFold32`, `nativeReduceXor`, `nativeChecksum32`, `nativeXorChecksumCompat`, `nativeCrc32cCompat`.
- `nativeValidateReduceXorBackendParity`: compara `reduce_xor` de cada backend contra `rmr_lowlevel_reduce_xor` e retorna bitmask de divergência.

### 3.4 `app/src/main/cpp/vectra_lowlevel_backend.h`
Define contrato único da vtable:
- ponteiros de função: `reduce_xor`, `checksum32`, `crc32c`;
- enum de bits SIMD (`NEON`, `SSE2`, `SSE42`, `AVX`, `RVV`);
- API de `*_available` e `*_bind` para todos os backends.

### 3.5 `app/src/main/cpp/vectra_lowlevel_backend_fallback.c`
**Funções implementadas:**
- `vectra_reduce_xor_fallback`, `vectra_checksum32_fallback`, `vectra_crc32c_fallback`, `vectra_backend_bind_fallback`.
- Usa implementação C portátil como baseline sem instruções específicas.

### 3.6 `app/src/main/cpp/vectra_lowlevel_backend_arm64.c`
**Funções implementadas:**
- `vectra_checksum32_arm64`, `vectra_reduce_xor_arm64`, `vectra_crc32c_arm64`, `vectra_backend_arm64_available`, `vectra_backend_bind_arm64`.
- Em `__aarch64__`, CRC32C usa intrínsecos (`__crc32cd`, `__crc32cb`); fora disso cai para software.

### 3.7 `app/src/main/cpp/vectra_lowlevel_backend_armv7.c`
**Funções implementadas:**
- `vectra_checksum32_armv7`, `vectra_reduce_xor_armv7`, `vectra_crc32c_armv7`, `vectra_backend_armv7_available`, `vectra_backend_bind_armv7`.
- Quando `__arm__`, `reduce_xor` usa NEON com redução XOR por lanes; sem `__arm__`, usa fallback `rmr_lowlevel_reduce_xor`.

### 3.8 `app/src/main/cpp/vectra_lowlevel_backend_x86_64.c`
**Funções implementadas:**
- `vectra_crc32c_software`, `vectra_checksum32_x86_64`, `vectra_reduce_xor_x86_64`, `vectra_crc32c_x86_64`, `vectra_backend_x86_64_available`, `vectra_backend_bind_x86_64`.
- Seleciona CRC por SSE4.2 quando disponível; caso contrário mantém software.

### 3.9 `app/src/main/cpp/vectra_lowlevel_backend_x86.c`
**Funções implementadas:**
- `vectra_checksum32_x86`, `vectra_reduce_xor_x86`, `vectra_crc32c_x86`, `vectra_backend_x86_available`, `vectra_backend_bind_x86`.
- Em `__i386__`, usa SSE para `reduce_xor` e CRC intrínseco; fora disso fallback software.

### 3.10 `app/src/main/cpp/vectra_lowlevel_backend_riscv64.c`
**Funções implementadas:**
- `vectra_reduce_xor_riscv64`, `vectra_checksum32_riscv64`, `vectra_crc32c_riscv64`, `vectra_backend_riscv64_available`, `vectra_backend_bind_riscv64`.
- Implementação atual é C-portável com gate por bit RVV na disponibilidade.

### 3.11 `app/src/main/cpp/termux_bootstrap_bridge.c`
**Funções implementadas:**
- `get_embedded_bootstrap_data`, `get_embedded_bootstrap_size` (com/sem payload gerado), `return_controlled_null`, `Java_com_termux_app_TermuxInstaller_nativeGetZip`.
- `nativeGetZip` valida assinatura ZIP antes de retornar `jbyteArray`.

### 3.12 `app/src/main/java/com/vectras/vm/core/NativeFastPath.java`
**Papel observado:**
- Carregamento da `vectra_core_accel`, status de inicialização e fallback Java.
- Contratos de arquitetura/feature mask (`ARCH_*`, `FEATURE_*`).
- Exposição de telemetria e perfil de hardware de boot (`HardwareProfile`).
- Wrapper de dezenas de métodos nativos para operações de cópia, checksum, arena, roteamento, verificação e auditoria.

### 3.13 `app/src/main/java/com/vectras/vm/core/LowLevelBridge.java`
**Papel observado:**
- Wrapper Java de `nativeFold32`, `nativeReduceXor`, `nativeChecksum32`, `nativeXorChecksumCompat`, `nativeCrc32cCompat`, `nativeValidateReduceXorBackendParity`.
- Fallback para `LowLevelDeterminism.*Fallback` quando JNI indisponível ou com exceção.
- Registro de trilha de erro via `RuntimeErrorReporter.warn`.

### 3.14 `app/src/main/java/com/vectras/vm/core/HardwareProfileBridge.java`
**Papel observado:**
- Constrói `Snapshot` com ABI, bits de ponteiro, SIMD e flags.
- Persiste/restaura snapshot (via `MainSettingsManager`).
- Deriva tuning de benchmark (`benchmarkStripeScale`, `benchmarkWarmupMs`) a partir do perfil.

### 3.15 `app/src/main/java/com/vectras/vm/core/VmFlowNativeBridge.java`
**Papel observado:**
- Disponibiliza rastreamento de estado VM em memória nativa (`init`, `mark`, `current`, `stats`, `vmLastMonoNanos`) com fallback silencioso quando indisponível.

---

## 4) Diferenças concretas entre narrativa documental e código atual

### Divergência A — contrato `reduce_xor`
Nos arquivos de backend e no bridge há comentários afirmando contrato canônico ligado a `rmr_lowlevel_reduce_xor` (fold por lanes + rotação). Porém:
- `arm64`, `x86_64`, `fallback`, `riscv64` chamam explicitamente `rmr_lowlevel_reduce_xor`;
- `armv7` (`__arm__`) e `x86` (`__i386__`) implementam redução XOR vetorial direta por lanes/bytes, sem a mesma transformação do contrato canônico.

Resultado: existe risco real de mismatch por ABI, e o próprio `nativeValidateReduceXorBackendParity` foi criado para detectar exatamente essa divergência.

### Divergência B — “detecção total de hardware” vs snapshot efetivo
O snapshot JNI atual publica 9 inteiros fixos (arch, ptr bits, endian, cycle flag, asm flag, feature bits e simd mask). Não há no payload atual, por exemplo, inventário completo de cache hierarchy, NUMA, microcode ou throughput real medido em runtime.

### Divergência C — “bare metal sem libc” vs build Android
A toolchain observada compila JNI para Android/bionic e mantém comentários explícitos de compatibilidade (`-ffreestanding/-fno-builtin removed — Android JNI requires bionic libc`). A prática atual é híbrida: low-level/asm onde possível, sem abandonar o runtime Android.

---

## 5) 18 conceitos essenciais (formalizados para software VM competitivo)

1. **Determinismo de contrato algorítmico por ABI** (`reduce_xor`, `checksum32`, `crc32c`).
2. **Paridade cross-backend mensurável** (bitmask de divergência nativa).
3. **Fallback funcional obrigatório** (sem crash quando JNI/feature falha).
4. **Detecção runtime de capacidade SIMD** (não só compile-time).
5. **Vtable de backend única e audível** (seleção explícita por ABI).
6. **Controle de versão de contrato JNI** (assinaturas estáveis entre Java/C).
7. **Telemetria de caminho nativo vs fallback** (hits/calls/bytes).
8. **Controle de erro orientado a código** (`RuntimeErrorReporter` + IDs).
9. **Segurança de faixa e argumentos em JNI** (offset/length/NULL-check).
10. **Observabilidade de estado de fluxo VM** (`VmFlowNativeBridge`).
11. **Política de build por ABI com flags explícitas** (CMake).
12. **Compatibilidade com page size Android moderno** (16 KiB link option).
13. **Validação de payload binário antes de consumo** (ZIP signature no bootstrap).
14. **Modelo de persistência de perfil de hardware** (snapshot serializado).
15. **Seletor de otimização baseado em perfil real** (warmup/stripe por SIMD+ptr bits).
16. **Convivência de C/ASM com camada app Java/Kotlin** (arquitetura híbrida pragmática).
17. **Governança de drift documental** (auditoria ativa + atualização no mesmo ciclo).
18. **Rastreabilidade arquivo→função→responsabilidade** (catálogo técnico canônico).

---

## 6) Refatoração documental aplicada

- A documentação passa a declarar explicitamente o escopo auditado (núcleo JNI/low-level + bridges Java), evitando afirmações genéricas não verificáveis.
- O documento agora separa:
  - o que é **implementado hoje**;
  - o que é **objetivo de evolução**;
  - o que é **divergência ativa** a ser tratado.

---

## 7) Próximas atualizações recomendadas (sem invenção)

1. Tornar `reduce_xor` semanticamente idêntico em todos os backends ou ajustar contrato e documentação para duas semânticas explícitas.
2. Publicar versão de contrato JNI (ex.: `JNI_CONTRACT_REV`) em Java+C para invalidar leituras antigas.
3. Expandir snapshot de hardware com campos adicionais somente quando coletados de fato no código.
4. Integrar o resultado de `nativeValidateReduceXorBackendParity` em pipeline de validação contínua por ABI.

---

## Metadados
- Documento: `UNIFIED_CODE_DOCUMENTATION_ALIGNMENT_2026-04-12.md`
- Data: 2026-04-12
- Base de verificação: leitura direta dos arquivos citados neste relatório.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
