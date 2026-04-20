<!-- DOC_ORG_SCAN: 2026-04-20 | source-scan: incoming-pending-separated -->

# _incoming

Diretório de ingestão técnica controlada (não canônico para runtime/release).

## Estado factual (2026-04-20)

### Absorvido no código canônico
- `_incoming/rafaelia_bare.c` → `engine/rmr/include/rmr_torus_flow.h`, `engine/rmr/src/rmr_torus_flow.c`, `engine/rmr/src/rmr_bench_suite.c`.
- `_incoming/rafaelia_flow.c` → `engine/rmr/include/rmr_torus_flow.h`, `engine/rmr/src/rmr_torus_flow.c`, `engine/rmr/src/rmr_bench_suite.c`.
- `_incoming/rafaelia_ultra.c` e `_incoming/rafaelia_ultra.S` → `engine/rmr/src/rmr_torus_flow.c` (normalização para kernel toroidal determinístico).
- `_incoming/rmr_spiral.S` → `engine/rmr/include/rafaelia_formulas_core.h`, `engine/rmr/src/rafaelia_formulas_core.c`, `engine/rmr/include/rmr_math_fabric.h`, `engine/rmr/src/rmr_math_fabric.c`.
- `_incoming/rmr_matrix.S` → `engine/rmr/src/rafaelia_bitraf_core.c` (núcleo matricial/máscara de rotas textual).

### Backlog técnico (uso planejado)
- `_incoming/rmr_test.c` → candidato para virar regressão formal de `demo_cli` (gate de determinismo em CI).
- `_incoming/termux.c` → candidato para ponte nativa de diagnóstico no fluxo Android/Termux, somente se houver contrato JNI explícito.
- `_incoming/RAFAELIA_MATH_FORMULAS.md` → referência matemática para auditoria de fórmulas já migradas.

### Archived (sem uso planejado de release)
Motivo técnico comum: protótipos ASM/C isolados, sem contrato de ABI pública, sem registro em `CMakeLists.txt`, sem vínculo com JNI/Gradle e sem evidência de consumo pelo pipeline de CI/release.

Arquivos classificados como `archived`:
- `_incoming/r.S`
- `_incoming/rmr_final.S`
- `_incoming/rmr_hidden.S`
- `_incoming/rmr_nihil.S`
- `_incoming/rmr_persist.S`
- `_incoming/rafaelia_10x10.S`
- `_incoming/rafaelia_7d.S`
- `_incoming/rafaelia_7d_gyro.S`
- `_incoming/rafaelia_7d_shapes.S`
- `_incoming/rafaelia_8way.S`
- `_incoming/rafaelia_936_fast.S`
- `_incoming/rafaelia_999_logsin.S`
- `_incoming/rafaelia_abs.S`
- `_incoming/rafaelia_arm.c`
- `_incoming/rafaelia_arm32.c`
- `_incoming/rafaelia_avalanche.S`
- `_incoming/rafaelia_avalanche_v2.S`
- `_incoming/rafaelia_bench_phi.S`
- `_incoming/rafaelia_central_link.S`
- `_incoming/rafaelia_chrono.S`
- `_incoming/rafaelia_clock.c`
- `_incoming/rafaelia_clock_fix.c`
- `_incoming/rafaelia_decision.S`
- `_incoming/rafaelia_delta.S`
- `_incoming/rafaelia_equitas.S`
- `_incoming/rafaelia_final.S`
- `_incoming/rafaelia_final_bench.S`
- `_incoming/rafaelia_final_seal.S`
- `_incoming/rafaelia_fix.S`
- `_incoming/rafaelia_flash.S`
- `_incoming/rafaelia_flops.S`
- `_incoming/rafaelia_fractal.S`
- `_incoming/rafaelia_hold.S`
- `_incoming/rafaelia_hw_sync.S`
- `_incoming/rafaelia_integrity.S`
- `_incoming/rafaelia_invasion.S`
- `_incoming/rafaelia_l2.S`
- `_incoming/rafaelia_life.S`
- `_incoming/rafaelia_lowlevel.c`
- `_incoming/rafaelia_next.S`
- `_incoming/rafaelia_pilar.S`
- `_incoming/rafaelia_prime.S`
- `_incoming/rafaelia_prob.S`
- `_incoming/rafaelia_pure_asm.c`
- `_incoming/rafaelia_sin_log.S`
- `_incoming/rafaelia_stabile.S`
- `_incoming/rafaelia_toro.S`
- `_incoming/rafaelia_torus.S`
- `_incoming/rafaelia_ttl`
- `_incoming/rafaelia_ttl.S`
- `_incoming/rafaelia_vacuo.S`
- `_incoming/rafaelia_vacuo_v2.S`
- `_incoming/rafaelia_vortex.S`
- `_incoming/rafaelia_vortex_live.S`
- `_incoming/rafaelia_vortex_v2.S`
- `_incoming/bitraf64_prototype_Version4.py`

## Fonte de verdade de integração
- Evidência arquivo-a-arquivo: [`_incoming/INTEGRATION_EVIDENCE.md`](INTEGRATION_EVIDENCE.md)
- Patch cirúrgico por módulo/símbolo: [`docs/SURGICAL_PATCHSET.md`](../docs/SURGICAL_PATCHSET.md)
