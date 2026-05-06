# BETA_STARTVM_PREFLIGHT

Data: 2026-05-06 (UTC)

## Escopo
- `app/src/main/java/com/vectras/vm/StartVM.java`

## Checklist de preflight

| Item | Evidência | Status |
|---|---|---|
| Comando final não vazio | `buildCommand(params)` remove nulos/vazios; se vazio, força fallback em `QemuExecConfig.resolveBinary(activity, arch)` | **READY** |
| `lastStartError` preenchido corretamente | Em comando vazio: `lastStartError = "empty_command"`; caminho normal limpa para `""` | **READY** |
| `lastRuntimeContract` atualizado em todas as fases | Atualiza para `prepared`, `error:empty_command` e `ready` | **READY** |

## Resultado
- **READY** — `STARTVM_PREFLIGHT_READY`
