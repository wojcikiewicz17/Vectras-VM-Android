# BETA_STARTVM_PREFLIGHT

Data: 2026-05-06 (UTC)

## Auditoria direcionada de `app/src/main/java/com/vectras/vm/StartVM.java`

1. Comando final não vazio: **READY**.
   - `buildCommand(params)` permanece filtro de tokens vazios.
   - Quando vazio, agora ocorre fallback para `QemuExecConfig.resolveBinary(activity, arch)` para garantir comando executável mínimo.
2. `lastStartError` preenchido: **READY**.
   - Em erro: `lastStartError = "empty_command"`.
   - Em caminho válido: `lastStartError = ""` (limpeza explícita de erro anterior).
3. `lastRuntimeContract` atualizado: **READY**.
   - Pré-montagem: fase `prepared`.
   - Falha de comando vazio: fase `error:empty_command`.
   - Comando válido: fase `ready`.

## Status final

- **READY** — `STARTVM_PREFLIGHT_READY`
