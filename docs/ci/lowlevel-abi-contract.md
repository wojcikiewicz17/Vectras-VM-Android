# Lowlevel ABI contract (auto-adaptativo)

Fonte canônica do contrato lowlevel ABI: `tools/ci/lowlevel_abi_contract.json`.

## Escopo

- 7 arquiteturas alvo (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`, `riscv64`, `riscv32`, `mips64`).
- Calling convention por arquitetura (entrada/retorno + caller/callee-saved).
- Regras de stack alignment, frame policy, prólogo e epílogo.
- Códigos de erro padronizados (`-1000..-1007`).
- Política de fronteira syscall/raw-OS permitida (`host_adapter_only` e `allowlisted_gate`).

## Interoperabilidade de versões ABI

A seção `interoperability` no contrato define a matriz produtor↔consumidor com:

- modo `strict` (sem bridge),
- modo `autoadaptive`,
- modo `autoadaptive_backport`.

## Gates de CI

Validação executada em:

- `.github/workflows/android-ci.yml`
- `.github/workflows/compile-matrix.yml`
- `.github/workflows/quality-gates.yml`

via `python3 tools/ci/validate_lowlevel_abi_contract.py`, incluindo bloqueio de dependências implícitas (`stdlib/libc`) em `app/src/main/cpp/lowlevel_abi.[ch]`.

Além disso, o gate `./tools/ci/verify_android_freestanding_contract.sh` garante modo fail-closed para release Android:

- target crítico dedicado `abi_core_freestanding`;
- flags críticas (`-ffreestanding`, `-fno-exceptions`, `-fno-rtti`, `-fno-unwind-tables`, `-fno-asynchronous-unwind-tables`, `-fvisibility=hidden`);
- linking do bridge JNI (`vectra_core_accel`) contra o core freestanding;
- bloqueio explícito de downgrade silencioso (`VECTRA_REQUIRE_FREESTANDING_CORE` sempre `ON`).
