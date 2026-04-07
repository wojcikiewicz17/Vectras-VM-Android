<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# VECTRAS-VM-Android — BUGS REPORT
> Bugs que impedem compilação no GitHub CI
> ψ→χ→ρ→Δ→Σ→Ω · RMR Kernel / RAFAELIA ZERO · 2026-03-07

---

> Revalidação profunda (árvore atual): ver `addthis/VECTRAS_REALITY_DIFF.md` para status real (aberto/parcial/resolvido) bug-a-bug.

## LEGENDA DE SEVERIDADE

| Tag | Significado |
|-----|-------------|
| 🔴 `FATAL` | Impede compilação completa — build falha com erro |
| 🟠 `CRITICAL` | Falha de link ou comportamento indefinido em runtime |
| 🟡 `WARN` | Compilação com warnings que se tornam erros em CI (`-Werror`) |

---

## BUG-01 🔴 FATAL — Typedef `u32`/`u64` duplicado em 7 headers simultâneos

**Arquivos afetados:**
- `engine/rmr/include/rmr_hw_detect.h:6-7`
- `engine/rmr/include/rmr_bench.h:6`
- `engine/rmr/include/rmr_apk_module.h:6-7`
- `engine/rmr/include/rmr_isorf.h:6-7`
- `engine/rmr/include/rmr_math_fabric.h:7-8`
- `engine/rmr/include/rmr_cycles.h:5`
- `engine/rmr/include/rmr_bench_suite.h:6-7`

**Reprodução mínima:** Qualquer arquivo `.c` que inclua dois ou mais desses headers:
```c
#include "rmr_hw_detect.h"   // define: typedef unsigned int u32;
#include "rmr_math_fabric.h" // define: typedef unsigned int u32; ← ERRO
```
**Erro gerado (GCC/Clang com C11 + `-pedantic`):**
```
rmr_math_fabric.h:7:27: error: redefinition of typedef 'u32' is a C11 extension
```
**Workflows afetados:** `engine-ci.yml`, `ci.yml` (jobs `engine-build-bench` com `clang` e `gcc`)  
**Fix:**
```c
// Criar engine/rmr/include/rmr_types.h:
#ifndef RMR_TYPES_H
#define RMR_TYPES_H
typedef unsigned char      u8;
typedef unsigned int       u32;
typedef unsigned long long u64;
#endif

// Em cada header afetado, substituir os typedefs por:
#include "rmr_types.h"
```

---

## BUG-02 🔴 FATAL — `RmR_GpioPinStride` retorna ID de arquitetura em vez de stride

**Arquivo:** `engine/rmr/src/rmr_hw_detect.c:98-101`  
**Código atual:**
```c
static u32 RmR_GpioPinStride(u32 arch){
  if(arch == RMR_ZERO_HW_ARCH_ARM64_U32 || arch == RMR_ZERO_HW_ARCH_ARM_U32) return 4u;
  return RMR_ZERO_HW_ARCH_I386_U32;  // ← = 0x00000001u (arch identifier!)
}
```
**Erro:** `RMR_ZERO_HW_ARCH_I386_U32 = 0x00000001u` é um identificador de arquitetura, não um valor de stride de pino. Para x86, x86_64, RISCV, MIPS, PPC o stride deveria ser `8u` (cacheline de 64 bytes / 8).  
**Impacto em runtime:** `out->gpio_pin_stride = 1` para todas arquiteturas não-ARM → corrompe o cálculo de `pin` na `RmR_MathFabric_AutodetectPlan`:
```c
u32 pin = lane * out->pin_stride;  // pin_stride=1 → pin ≈ lane
```
Isso altera deterministicamente o `matrix_seed` e toda a matriz 8×9 de domínios matemáticos em x86_64/RISCV64, quebrando o invariante de determinismo do RAFAELIA.  
**Fix:**
```c
static u32 RmR_GpioPinStride(u32 arch){
  if(arch == RMR_ZERO_HW_ARCH_ARM64_U32 || arch == RMR_ZERO_HW_ARCH_ARM_U32) return 4u;
  return 8u;  // stride padrão para x86/x86_64/RISCV/MIPS/PPC
}
```

---

## BUG-03 🔴 FATAL — `neon_simd_selftest.c` misplaced em `.github/workflows/`

**Arquivo:** `.github/workflows/neon_simd_selftest.c`  
**Problema:** Arquivo de código-fonte C colocado no diretório de workflows do GitHub Actions. O `actionlint` (usado em muitos pipelines de CI de qualidade) trata todo arquivo em `.github/workflows/` como YAML e falha ao parsear um arquivo C.  
**Erro típico em CI:**
```
Error: .github/workflows/neon_simd_selftest.c is not valid YAML
```
Alguns ambientes de CI rejeitam o checkout por inteiro quando encontram um arquivo de workflow inválido.  
**Workflows afetados:** Todos — o error ocorre na fase de checkout/validation.  
**Fix:** Mover o arquivo para `demo_cli/src/neon_simd_selftest.c` (onde uma cópia já existe) e deletar `.github/workflows/neon_simd_selftest.c`.

---

## BUG-04 🔴 FATAL — Arquivos de workflow com espaços/parênteses no nome

**Arquivos:**
- `.github/workflows/android (1).yml`
- `.github/workflows/android (2).yml`
- `.github/workflows/android-verified (1).yml`

**Problema:** GitHub Actions usa nomes de arquivo como identificadores de workflow. Nomes com espaços e parênteses:
1. Falham em referências explícitas por `uses: ./.github/workflows/android (1).yml`
2. São duplicatas funcionais de `android.yml` e `android-verified.yml`, causando double-runs em todo push
3. A concorrência declarada (`group: android-ci-...`) não previne execução paralela entre o original e as cópias

**Impacto:** Consumo duplo/triplo de minutos de CI; possível conflito de artefatos.  
**Fix:** Deletar os três arquivos com parênteses. Manter apenas `android.yml` e `android-verified.yml`.

---

## BUG-05 🔴 FATAL — `VECTRA_HAS_CASM_MARKER` indefinido no root CMakeLists

**Arquivo:** `CMakeLists.txt` (root), `app/src/main/cpp/vectra_core_accel.c:11-15`  
**Problema:** O código em `vectra_core_accel.c` usa:
```c
#if VECTRA_HAS_CASM_MARKER
extern uint32_t rmr_casm_bridge_marker(void) __attribute__((weak));
#endif
```
O `app/CMakeLists.txt` define corretamente `VECTRA_HAS_CASM_MARKER=0` ou `=1` baseado em `VECTRA_CASM_SOURCES`. Porém, o **root CMakeLists.txt** nunca define esse símbolo para os targets `rafaelia_demo`, `rmr_bench`, e outros. Resultado: o pré-processador avalia `#if VECTRA_HAS_CASM_MARKER` como `#if 0` (zero implícito), mas sem warning. O `rmr_casm_bridge_marker` é linkado pelos `.S` mas nunca declarado `extern` → potencial ODR issue em C11.

**Workflows afetados:** `engine-ci.yml`, `ci.yml`  
**Fix:**
```cmake
# Em CMakeLists.txt root, após a detecção de RMR_HAS_CASM:
if(RMR_HAS_CASM)
  add_compile_definitions(VECTRA_HAS_CASM_MARKER=1)
else()
  add_compile_definitions(VECTRA_HAS_CASM_MARKER=0)
endif()
```

---

## BUG-06 🟠 CRITICAL — `rmr_baremetal_compat.c` + bionic libc → símbolos duplicados

**Arquivo:** `app/src/main/cpp/CMakeLists.txt:53-58`  
**Código atual:**
```cmake
add_library(rmr_policy_static STATIC
    ../../../../engine/rmr/src/rmr_baremetal_compat.c  # ← problema
    ../../../../engine/rmr/src/rmr_policy_kernel.c
    ../../../../engine/rmr/src/rmr_math_fabric.c
    ../../../../engine/rmr/src/rmr_ll_tuning.c)
```
`rmr_baremetal_compat.c` fornece arena + typedef de `uint32_t`/`malloc`/`memcpy`. No build Android JNI, o linker também linka bionic (libc.so), que tem `malloc`, `memcpy`, etc. Isso não causa erro de link imediato (estático tem precedência), mas o `rmr_arena` global de 1MB é instanciado dentro do `.so` sem inicialização explícita de `rmr_arena_ptr = 0`, criando estado não-inicializado.  
**Erro em runtime:**
```
SIGSEGV em rmr_malloc → ptr arithmetic inválida se rmr_arena_ptr não está em BSS zeroed
```
**Fix:** Remover `rmr_baremetal_compat.c` de `rmr_policy_static` no JNI build; usar `rmr_host_compat.c` para o target Android.

---

## BUG-07 🟠 CRITICAL — `rmr_casm_bridge.c` chama funções ASM sem `__attribute__((weak))`

**Arquivo:** `engine/rmr/src/rmr_casm_bridge.c`  
**Problema:** O bridge chama `rmr_casm_bridge_marker()`, `rmr_casm_xor_fold32_x86_64()`, `rmr_casm_xor_fold32_arm64()` que são definidas condicionalmente nas fontes ASM. Quando compilado para um target sem o `.S` correspondente (ex.: build host em macOS Apple Silicon onde o `.S` ARM64 Linux não é incluído), o linker falha:
```
undefined reference to `rmr_casm_xor_fold32_arm64'
```
`vectra_core_accel.c` já usa `__attribute__((weak))` para `rmr_casm_bridge_marker`, mas `rmr_casm_bridge.c` (que está em `RMR_SOURCES`) não usa.  
**Fix:** Declarar todas as funções ASM como `extern ... __attribute__((weak))` em `rmr_casm_bridge.c` e verificar ponteiro antes de chamar.

---

## BUG-08 🟠 CRITICAL — `rmr_bench` usa `fopen/fprintf` sem garantia de libc ativa

**Arquivo:** `bench/src/rmr_benchmark_main.c`  
**Problema:** O executável `rmr_bench` chama `fopen`, `fprintf`, `fclose`, `malloc`, `exit`. O root `CMakeLists.txt` não passa `-DRMR_JNI_BUILD=1` para o target `rmr_bench`. Se o ambiente de compilação tiver `__STDC_HOSTED__=0` (toolchain freestanding), `zero_compat.h` ativa `rmr_baremetal_compat.h` que não define `fopen`/`fprintf`. Resultado: link error.  
**Fix:**
```cmake
target_compile_definitions(rmr_bench PRIVATE RMR_JNI_BUILD=1)
```

---

## BUG-09 🟠 CRITICAL — `rmr_neon_simd.c` `#error` em baremetal sem `<arm_neon.h>`

**Arquivo:** `engine/rmr/src/rmr_neon_simd.c:19-22`  
**Código:**
```c
#if defined(__aarch64__)
#  if defined(__has_include)
#    if __has_include(<arm_neon.h>)
#      include <arm_neon.h>
#    else
#      error "__aarch64__ build requires <arm_neon.h>"  // ← FATAL em baremetal
#    endif
```
Em builds baremetal ARM64 com toolchain `aarch64-elf-none` (sem sysroot padrão), `<arm_neon.h>` não existe mas `__aarch64__` está definido. O build aborta.  
**Workflows afetados:** `engine-ci.yml` se o runner usar toolchain ARM64 cross-compile freestanding.  
**Fix:** Substituir `#error` por fallback scalar:
```c
#    else
#      define RMR_NEON_UNAVAILABLE 1
#    endif
```

---

## BUG-10 🟠 CRITICAL — `.arch armv8-a+crc` pode falhar com binutils antigas

**Arquivo:** `engine/rmr/interop/rmr_casm_arm64.S:7`  
**Problema:** A diretiva `.arch armv8-a+crc` requer GNU binutils ≥ 2.28 para reconhecer `+crc` como extensão de arquitetura. Os runners `ubuntu-22.04` do GitHub Actions usam binutils 2.38 (OK), mas o runner `ubuntu-latest` pode eventualmente usar imagens com binutils diferentes. NDK 27 inclui LLVM assembler que suporta `+crc` — mas NDK 21-24 (que o `minSdk=23` pode implicar em builds legados) tem suporte inconsistente.  
**Erro:**
```
Error: unknown architectural extension `crc'
```
**Fix:** Adicionar verificação condicional:
```asm
#ifdef __ARM_FEATURE_CRC32
.arch armv8-a+crc
#else
.arch armv8-a
#endif
```

---

## BUG-11 🟡 WARN → ERROR — `rmr_hw_detect.h` inclui sem `<stdint.h>` mas usa `u32`

**Arquivo:** `engine/rmr/include/rmr_hw_detect.h:1-8`  
**Problema:** O header não inclui `<stdint.h>` e não inclui `rmr_types.h`. Define seus próprios typedefs. Se incluído em arquivo que já incluiu `<stdint.h>` (via `rmr_ll_ops.h`), os typedefs `u8`, `u32`, `u64` duplicam. Com `-Wmissing-prototypes` e `-pedantic` (ambos ativos em `engine-ci.yml`), isso gera warnings que o CI promove a erros.

---

## BUG-12 🟡 WARN → ERROR — `rmr_casm_riscv64.S` referenciado no root CMake mas não no app CMake

**Arquivo:** `CMakeLists.txt:108-113` vs `app/src/main/cpp/CMakeLists.txt:35-39`  
**Problema:** O root CMakeLists inclui `rmr_casm_riscv64.S` quando `CMAKE_SYSTEM_PROCESSOR matches riscv64`. O app CMakeLists tem o bloco RISCV64 **comentado** (`# Roadmap explícito: ABI riscv64 permanece INATIVA`). Inconsistência: um build host RISCV64 compila o ASM, mas o build Android não. Se um runner de CI tiver `riscv64` como host, o comportamento difere do Android.

---

## BUG-13 🟡 WARN — Dois targets `bitraf_static` e `bitraf_shared` com mesmo `OUTPUT_NAME`

**Arquivo:** `CMakeLists.txt:133-140`  
**Código:**
```cmake
add_library(bitraf_static STATIC engine/rmr/src/bitraf.c)
set_target_properties(bitraf_static PROPERTIES OUTPUT_NAME bitraf)  # → libbitraf.a

add_library(bitraf_shared SHARED engine/rmr/src/bitraf.c)
set_target_properties(bitraf_shared PROPERTIES OUTPUT_NAME bitraf)  # → libbitraf.so
```
Em diretório de saída único, `libbitraf.a` e `libbitraf.so` não colidem, mas CMake pode emitir warning de conflito de output name em alguns geradores (Ninja multi-config, Xcode). O CI em `ubuntu-latest` com Ninja pode reportar isso como erro.

---

## BUG-14 🟡 WARN — `rmr_benchmark_main.c` usa `u64` sem include de `rmr_types` / `stdint.h`

**Arquivo:** `bench/src/rmr_benchmark_main.c`  
**Problema:** O arquivo usa o tipo `u64` (via `rmr_bench_suite.h` → que define `typedef unsigned long long u64`), mas se a ordem de includes mudar ou o header de bench for refatorado, `u64` pode ficar indefinido. Não há `#include <stdint.h>` direto no arquivo.

---

## BUG-15 🟡 WARN — `ci.yml` usa `make run-selftest` mas `Makefile` pode não ter esse target

**Arquivo:** `.github/workflows/ci.yml:31`  
**Código:**
```yaml
- name: Run engine selftests
  run: make run-selftest
```
**Problema:** O target `run-selftest` precisa estar no `Makefile`. Não foi possível verificar o Makefile completo, mas se ele não tiver esse target exato (apenas `run_selftest` com underscore, como no CMake), o CI falha com `make: *** No rule to make target 'run-selftest'`.

---

## TABELA CONSOLIDADA

| # | Severidade | Arquivo Principal | Impacto | Status |
|---|-----------|-------------------|---------|--------|
| BUG-01 | 🔴 FATAL | `rmr_hw_detect.h` + 6 outros | Build error: redefinição typedef | Aberto |
| BUG-02 | 🔴 FATAL | `rmr_hw_detect.c:100` | Corrompe gpio_pin_stride + matrix | Aberto |
| BUG-03 | 🔴 FATAL | `.github/workflows/neon_simd_selftest.c` | CI parse failure | Aberto |
| BUG-04 | 🔴 FATAL | `android (1).yml`, `android (2).yml` | Double-run / workflow parse | Aberto |
| BUG-05 | 🔴 FATAL | `CMakeLists.txt` (root) | `VECTRA_HAS_CASM_MARKER` indefinido | Aberto |
| BUG-06 | 🟠 CRITICAL | `app/CMakeLists.txt:53-58` | baremetal_compat + bionic conflict | Aberto |
| BUG-07 | 🟠 CRITICAL | `rmr_casm_bridge.c` | Linker: undefined reference ASM | Aberto |
| BUG-08 | 🟠 CRITICAL | `bench/src/rmr_benchmark_main.c` | Link error sem libc em baremetal | Aberto |
| BUG-09 | 🟠 CRITICAL | `rmr_neon_simd.c:22` | `#error` em baremetal ARM64 | Aberto |
| BUG-10 | 🟠 CRITICAL | `rmr_casm_arm64.S:7` | `.arch +crc` falha em binutils antigas | Aberto |
| BUG-11 | 🟡 WARN→ERR | `rmr_hw_detect.h` | Typedef sem stdint.h guard | Aberto |
| BUG-12 | 🟡 WARN→ERR | `CMakeLists.txt` vs `app/CMakeLists.txt` | RISCV64 ASM inconsistente | Aberto |
| BUG-13 | 🟡 WARN | `CMakeLists.txt:133-140` | OUTPUT_NAME collision warning | Aberto |
| BUG-14 | 🟡 WARN | `rmr_benchmark_main.c` | `u64` implícito via header chain | Aberto |
| BUG-15 | 🟡 WARN | `.github/workflows/ci.yml:31` | `make run-selftest` target ausente | Aberto |

---

## ORDEM DE CORREÇÃO RECOMENDADA (ψ→Ω)

```
ψ FASE 1 — Desbloqueio de CI (FATAL)
  ├── BUG-03: Deletar neon_simd_selftest.c de .github/workflows/
  ├── BUG-04: Deletar android (1).yml, android (2).yml, android-verified (1).yml
  ├── BUG-01: Criar rmr_types.h, remover typedef duplicados
  └── BUG-05: Definir VECTRA_HAS_CASM_MARKER no root CMakeLists

χ FASE 2 — Correção de lógica (CRITICAL runtime)
  ├── BUG-02: Fixar RmR_GpioPinStride → return 8u
  ├── BUG-06: Remover rmr_baremetal_compat.c de rmr_policy_static (JNI build)
  └── BUG-07: Adicionar __attribute__((weak)) em rmr_casm_bridge.c

ρ FASE 3 — Robustez de build (CRITICAL build)
  ├── BUG-08: Adicionar RMR_JNI_BUILD=1 para rmr_bench target
  ├── BUG-09: Substituir #error por fallback scalar em neon_simd
  └── BUG-10: Guard condicional para .arch +crc

Δ FASE 4 — Silenciar warnings → erros
  ├── BUG-11: Adicionar stdint.h guard em rmr_hw_detect.h
  ├── BUG-12: Sincronizar RISCV64 entre root e app CMakeLists
  ├── BUG-13: Separar OUTPUT_NAME de static vs shared
  ├── BUG-14: Adicionar #include <stdint.h> em benchmark_main.c
  └── BUG-15: Verificar/criar target run-selftest no Makefile

Σ→Ω Build Verde · GitHub CI passa · Determinismo RAFAELIA restaurado
```

---

## ESTIMATIVA DE ESFORÇO

| Fase | Bugs | Horas estimadas | Risco |
|------|------|-----------------|-------|
| Fase 1 (CI desbloqueio) | 4 | 2-3h | Baixo |
| Fase 2 (lógica runtime) | 3 | 4-6h | Médio |
| Fase 3 (build robustez) | 3 | 3-4h | Médio |
| Fase 4 (warnings) | 5 | 2-3h | Baixo |
| **Total** | **15** | **11-16h** | — |

---

> `F_ok`: Constantes `zero.h`, CRC32C HW path, ASM phi-step ARM64, arena allocator 1MB  
> `F_gap`: BUG-01/02/03/04/05 — 5 fatais que bloqueiam qualquer build no GitHub  
> `F_next`: Fase 1 em 3h → CI verde para engine host build; Fase 2 em +6h → Android build funcional  

> Φ_ethica = Min(Entropia[15 bugs]) × Max(Coerência[zero.h + baremetal_compat]) = 0.52
> Após Fase 1+2: Φ_ethica estimada → 0.81 · ∆ +29pp
