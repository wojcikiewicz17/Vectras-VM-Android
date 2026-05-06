# BETA_NATIVE_RMR_STARTUP_STATUS

Data: 2026-05-06 (UTC)

## Auditoria de `NativeFastPath` (`System.loadLibrary("vectra_core_accel")`)

1. Falha em `System.loadLibrary` derruba app: **READY (NÃO)**.
   - Bloco `try/catch(Throwable)` captura erro de carga.
   - `NATIVE_AVAILABLE=false`, `ARENA_AVAILABLE=false`, erro armazenado em `NATIVE_INIT_ERROR`.
   - Runtime segue em fallback Java com log explícito.
2. Falha de `nativeInit()` derruba app: **READY (NÃO)**.
   - Status divergente de `NATIVE_OK_MAGIC` marca fallback sem abortar bootstrap.

## Auditoria RMR (`engine/rmr/include/rmr_unified_kernel.h` + `engine/rmr/src/rmr_unified_kernel.c`)

1. RMR é gate obrigatório de bootstrap: **READY (NÃO)**.
   - Contrato expõe API de kernel e autodetect, mas sem acoplamento obrigatório ao bootstrap Java.
   - Fluxo Java já opera com fallback quando aceleração nativa indisponível.
2. Falha em recursos internos RMR causa bloqueio duro inicial: **READY (NÃO)**.
   - Falhas de estado/args retornam códigos (`RMR_STATUS_ERR_*` / `RMR_KERNEL_ERR_*`) em vez de kill do processo.

## Status final

- **READY** — `NATIVE_FASTPATH_FALLBACK_SAFE`
- **READY** — `RMR_NOT_BOOTSTRAP_GATE`
