<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# ∆RAFAELIA LOWLEVEL NUCLEUS
`R(t+1)=R(t)×Φ_ethica×E_Verbo×(√3/2)^(πφ)`

## Arquivos Gerados

### JNI (jni/)
| Arquivo | Função |
|---|---|
| `zipraf_jni.c` | **★ ARQUIVO FALTANTE** — Bridge JNI para ZiprafEngine.java |
| `zipraf_core_bridge.c` | Adaptador zr_open/triple/crc/geo ↔ RMR |
| `vectra_cpu_safe.c` | **★ SIGILL GUARD** — CPU detect Android-safe (sem crash EL1) |

### Engine (engine/rmr/)
| Arquivo | Função |
|---|---|
| `include/rmr_unified_kernel.h` | Header mestre — tipos, constantes, API |
| `include/rmr_lowlevel.h` | Ops lowlevel — fold/xor/checksum/crc/encode10 |
| `include/rmr_policy_kernel.h` | Φ_ethica — policy e ética do kernel |
| `src/rmr_unified_kernel.c` | Init/tick do ciclo ψ→χ→ρ→Δ→Σ→Ω |
| `src/rmr_hw_detect.c` | Detect HW multi-arch (Android-safe) |
| `src/rmr_corelib.c` | φ-fold, Fibonacci-Rafael, encode10, √3/2 |
| `src/rmr_ll_ops.c` | geo4x4 trace, virt_size, path_hash, triple |
| `src/rmr_cycles.c` | TSC/CNTFRQ, fΩ=963↔999 validator |
| `src/rmr_casm_bridge.c` | Bridge C↔ASM + dispatch table |
| `src/rmr_policy_kernel.c` | Φ_ethica = Min(ε) × Max(coherence) |
| `src/rmr_math_fabric.c` | OWLψ analyzer, Stack42H, Bitraf64, fractais |
| `src/rmr_ll_tuning.c` | Auto-calibração runtime |
| `src/bitraf.c` | BITRAF ring + ZIPRAF slot routing |
| `interop/rmr_casm_arm64.S` | ARM64: CRC32C HW + phi-fold NEON |
| `interop/rmr_casm_x86_64.S` | x86_64: SSE4.2 + phi-fold |
| `interop/rmr_casm_riscv64.S` | RISC-V 64 stubs |

### Java (java/)
| Arquivo | Função |
|---|---|
| `RafaeliaKernel.java` | Kernel controller — coordena todos módulos |
| `NativeFastPath.java` | API JNI para vectra_core_accel |
| `LowLevelBridge.java` | API JNI para lowlevel_bridge |

### CMake (cmake/)
| Arquivo | Função |
|---|---|
| `CMakeLists_unified.txt` | Build unificado: zipraf + vectra_core_accel + cpu_detect |

---

## Integração no Android

### 1. Copiar arquivos

```
app/src/main/cpp/
  ├── zipraf_jni.c              ← novo
  ├── zipraf_core_bridge.c      ← novo  
  ├── vectra_cpu_safe.c         ← novo (substitui vectra_cpu_detect.c unsafe)
  └── CMakeLists.txt            ← usar CMakeLists_unified.txt como base

engine/rmr/
  ├── include/   ← copiar .h
  ├── src/       ← copiar .c
  └── interop/   ← copiar .S
```

### 2. Java — adicionar ao projeto

```
app/src/main/java/com/vectras/vm/core/
  ├── ZiprafEngine.java    (já existe)
  ├── VectraCpuDetect.java (já existe)
  ├── RafaeliaKernel.java  ← novo
  ├── NativeFastPath.java  ← novo
  └── LowLevelBridge.java  ← novo
```

### 3. Inicialização

```java
// Application.onCreate()
RafaeliaKernel kernel = RafaeliaKernel.getInstance();
kernel.init();  // detecta CPU, carrega libs, aquece φ-state

// Usar zipraf
byte[] zipData = /* read file */;
ZiprafEngine.Container c = kernel.open(zipData);
Log.d("RAFA", c.toString()); // Container{...}

// Status do ciclo ψ→Ω
Log.d("RAFA", kernel.toString()); // ∆RAFAELIA{cycle=Ω φ=... Φe=42 ...}
```

---

## Fixes Críticos Resolvidos

### ① zipraf_jni.c (FALTAVA COMPLETAMENTE)
Implementa exatamente os 5 native methods de `ZiprafEngine.java`:
- `nativeOpen` → `zr_open()`
- `nativeTripleComplete` → triple redundancy resolver
- `nativeCrc32cHw` → HW CRC32C dispatch
- `nativeGeo4x4Trace` → 4×4 geo tracer
- `nativeVirtSize` → amplification calc

### ② vectra_cpu_safe.c (SIGILL GUARD)
Substitui leituras diretas `mrs *_EL1` por:
1. `getauxval(AT_HWCAP/AT_HWCAP2)` — primário, sempre seguro EL0
2. `/proc/cpuinfo` fallback
3. `SIGILL probe` com `sigsetjmp/siglongjmp` para MIDR_EL1 (não crasha)
4. `CTR_EL0`, `CNTFRQ_EL0` — regs válidos em EL0

---

## Ciclo ψ→χ→ρ→Δ→Σ→Ω

```
ψ = intenção    → init(), loadLibrary()
χ = observação  → hw_detect, getauxval
ρ = ruído       → absorção de eventos externos
Δ = transmutação→ phi_step() × √3/2 × Φ_ethica
Σ = memória     → phi_state acumulado
Ω = completude  → Amor = coerência máxima
```

`D'Ele, Amor` → reset: `RafaeliaKernel.getInstance().init()`
