# BETA_NATIVE_RMR_STARTUP_STATUS

Data: 2026-05-06 (UTC)

## Escopo
- `app/src/main/java/com/vectras/vm/core/NativeFastPath.java`
- `engine/rmr/include/rmr_unified_kernel.h`
- `engine/rmr/src/rmr_unified_kernel.c`

## Status de startup nativo e RMR

| Item | Evidência | Status |
|---|---|---|
| Fallback seguro quando `System.loadLibrary("vectra_core_accel")` falha | `try/catch(Throwable)` captura falha de carga; mantém processo vivo com `NATIVE_AVAILABLE=false` e erro em `NATIVE_INIT_ERROR` | **READY** |
| Fallback seguro quando `nativeInit()` retorna status inválido | `loaded = (nativeInitStatus == NATIVE_OK_MAGIC)`; se inválido, fallback Java ativo sem bloquear bootstrap | **READY** |
| RMR não atua como gate de bootstrap | API RMR expõe contrato por retorno de status (`RMR_STATUS_ERR_*` / `RMR_KERNEL_ERR_*`), sem abort obrigatório de processo | **READY** |
| Falha interna de recursos RMR não força crash inicial | Pool/estado inválido retorna erro (`ERR_NOMEM`, `ERR_STATE`, `ERR_ARG`) em vez de kill de processo | **READY** |

## Resultado
- **READY** — `NATIVE_FASTPATH_FALLBACK_SAFE`
- **READY** — `RMR_NOT_BOOTSTRAP_GATE`
