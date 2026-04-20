# Lowlevel ABI Contract (camada crítica)

Versão do contrato: **1.0.0**  
Fonte canônica de validação CI/build: `tools/ci/lowlevel_abi_contract.json` + `tools/ci/validate_lowlevel_abi_contract.py`.

## 1) Arquiteturas alvo

- **Obrigatória (release oficial):** `arm64-v8a` (AArch64).
- **Opcional interna (nunca release oficial):** `armeabi-v7a` (ARMv7).

## 2) Calling convention

### AArch64 (`arm64-v8a`)
- Entrada: `x0-x7`
- Retorno: `x0-x1`
- Caller-saved: `x0-x18, v0-v31`
- Callee-saved: `x19-x28, x29(fp), sp`

### ARMv7 (`armeabi-v7a`, trilha interna)
- Entrada: `r0-r3`
- Retorno: `r0-r1`
- Caller-saved: `r0-r3, r12`
- Callee-saved: `r4-r11, sp`

## 3) Stack alignment, frame policy e prólogo/epílogo

### AArch64
- Stack alignment: **16 bytes**
- Frame policy: `leaf_optional`
- Prólogo canônico: `stp x29, x30, [sp, #-16]!; mov x29, sp`
- Epílogo canônico: `ldp x29, x30, [sp], #16; ret`

### ARMv7 (interno)
- Stack alignment: **8 bytes**
- Frame policy: `leaf_optional`
- Prólogo canônico: `push {r7, lr}; add r7, sp, #0`
- Epílogo canônico: `pop {r7, pc}`

## 4) Códigos de erro padronizados

- `0`  → `ok`
- `-1000` → `unsupported_arch`
- `-1001` → `unsupported_abi_version`
- `-1002` → `stack_misaligned`
- `-1003` → `bad_frame_policy`
- `-1004` → `calling_convention`
- `-1005` → `forbidden_syscall`
- `-1006` → `boundary_violation`
- `-1007` → `null_ptr`

## 5) Política de syscall / raw-OS boundary

- `raw_os_syscalls_forbidden`: **proibidas** na camada crítica.
- Toda fronteira de SO deve passar por **host adapter gate allowlistado e auditável** (`host_adapter_only_allowlisted`).

## 6) Símbolos exportáveis estáveis (ABI `abi_entry_*`)

- `abi_entry_get_contract_version` (since ABI `1.0.0`)
- `abi_entry_get_arch_descriptors` (since ABI `1.0.0`)
- `abi_entry_get_interop_rules` (since ABI `1.0.0`)
- `abi_entry_validate_interop` (since ABI `1.0.0`)

## 7) Regra de build e gate obrigatório de CI

- **Build local/Gradle:** `:app:preBuild` depende de `validateCriticalNativeAbiLayer`.
- **CMake/JNI:** `vectra_core_accel` depende de `lowlevel_abi_contract_check`.
- **CI obrigatório:**
  - `.github/workflows/android-ci.yml` (job gate dedicado de contrato ABI).
  - `.github/workflows/compile-matrix.yml` (job gate dedicado de contrato ABI).
