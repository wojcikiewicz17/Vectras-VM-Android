# BETA_STARTVM_PREFLIGHT

Data: 2026-05-05 (UTC)

## Checagem estática de `StartVM`

1. QEMU binary resolve: **SIM** (montagem por `qemuDir + qemuArch + "-" + qemuIfType`).
2. arch resolve: **SIM** (`MainSettingsManager.getArch(activity)`).
3. disk image path resolve: **SIM** (`drive` com fallback `"null"`).
4. cdrom path resolve: **SIM** (`-cdrom` só quando não vazio).
5. shared folder não quebra: **SIM** (somente injeta `-virtfs` com pasta existente).
6. KVM probe não quebra: **SIM** (fallback documentado e controle por `MainSettingsManager.getAvx`).
7. fallback TCG documentado: **SIM** (`MachineMode.TCG` e contratos de runtime).
8. final command não fica vazio: **SIM** (retorna erro explícito `empty_command`).
9. `lastStartError` preenchido em erro real: **SIM** (`empty_command` e erros de preflight nas camadas de chamada).
10. `lastRuntimeContract` atualizado: **SIM** (`prepared` + atualizações posteriores).
11. `RafaeliaQemuTuning` injeta argumento inválido: **NÃO EVIDENCIADO** nesta auditoria estática.
12. Python guest como runtime/manual: **SIM** (não há dependência build-time em `StartVM`).

## Status

- `STARTVM_READY` (análise estática, sem regressão observada)
