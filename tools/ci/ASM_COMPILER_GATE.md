# ASM Compiler Refactor Gate

Gate mínimo para integrar o compilador ASM refatorado (RAFCODE PHI) ao CI host.

## Objetivo
- validar build determinístico do backend ASM/C sem dependências externas extras;
- bloquear promoção quando houver divergência de integridade (CRC/layout);
- publicar manifesto técnico para auditoria e trilha legal.

## Execução
```bash
./tools/ci/run_asm_compiler_refactor_gate.sh
```

## Evidências geradas
- `reports/asm-compiler/build.log`
- `reports/asm-compiler/regression.log`
- `reports/asm-compiler/manifest.json`

## Fail-safe e rollback
- **fail-safe**: o gate falha fechado em qualquer regressão de CRC32C/layout.
- **rollback operacional**: manter lane anterior promovida e bloquear release da lane atual.
