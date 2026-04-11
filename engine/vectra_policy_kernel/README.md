<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# vectra_policy_kernel

`vectra_policy_kernel` atua como **adapter/composição** para o kernel determinístico em C (`engine/rmr/src/rmr_unified_kernel.c`).

## Papel do adapter Rust
- expor ergonomia de integração (CLI, serialização de eventos, composição de pipeline);
- manter superfície de uso idiomática em Rust para o restante do projeto;
- delegar decisões determinísticas centrais ao kernel C via FFI explícita (`extern "C"` + bindings manuais).

## Limites de responsabilidade
- **não** reimplementar no Rust as mesmas regras determinísticas já canônicas no C;
- roteamento determinístico (pressão CPU/RAM/DISK e seleção de rota) vem de `RmR_UnifiedKernel_Process` + `RmR_UnifiedKernel_Route`;
- verificação CRC32C vem de `RmR_UnifiedKernel_Verify`.

## O que fica no Rust
- composição de estágios `PLAN/DIFF/APPLY/VERIFY/AUDIT`;
- adaptação de IO (`Read/Write`), eventos e auditoria;
- integração de tooling/CLI para consumo no workspace.
