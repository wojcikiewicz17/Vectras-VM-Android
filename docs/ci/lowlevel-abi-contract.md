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

via `python3 tools/ci/validate_lowlevel_abi_contract.py`, incluindo bloqueio de dependências implícitas (`stdlib/libc`) em `app/src/main/cpp/lowlevel_abi.[ch]`.
