<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Evidência de integração dos patchsets RAFAELIA

Origem recuperada (externa ao estado atual da árvore): commit histórico `5924890`.

## Matriz arquivo-a-arquivo (origem → destino)

### Integrações absorvidas
| Origem | Destino canônico | Status | Evidência técnica |
|---|---|---|---|
| `_incoming/rafaelia_bare.c` | `engine/rmr/include/rmr_torus_flow.h` + `engine/rmr/src/rmr_torus_flow.c` + `engine/rmr/src/rmr_bench_suite.c` | absorbed | Promoção para API/impl determinística do Toroidal Flow, com consumo na suíte oficial (`kind=5`). |
| `_incoming/rafaelia_flow.c` | `engine/rmr/include/rmr_torus_flow.h` + `engine/rmr/src/rmr_torus_flow.c` + `engine/rmr/src/rmr_bench_suite.c` | absorbed | Mesma trilha canônica do kernel toroidal para benchmark contínuo/CI. |
| `_incoming/rafaelia_ultra.c` / `_incoming/rafaelia_ultra.S` | `engine/rmr/src/rmr_torus_flow.c` | absorbed | Consolidação textual de dinâmica toroidal em Q16.16 e helper determinístico. |
| `_incoming/rmr_spiral.S` | `engine/rmr/include/rafaelia_formulas_core.h` + `engine/rmr/src/rafaelia_formulas_core.c` + `engine/rmr/include/rmr_math_fabric.h` + `engine/rmr/src/rmr_math_fabric.c` | absorbed | Operadores de spiral/fibração migrados para API C de produção (sem assembly solto). |
| `_incoming/rmr_matrix.S` | `engine/rmr/src/rafaelia_bitraf_core.c` | absorbed | Núcleo matricial/máscara de rotas incorporado em implementação textual auditável. |
| `RAFAELIA_VECTRA2_PATCHES.zip` (histórico) | `engine/rmr/*`, `app/src/main/java/com/vectras/vm/core/MathUtils.java`, `app/src/main/java/com/vectras/vm/rafaelia/*` | absorbed | Fórmulas, Math Fabric e classes Android migradas como fonte versionada. |
| `RAFAELIA-CHANGED-FILES.zip` (histórico) | `bug/core/bitraf.c`, `bug/core/zipraf_core_bridge.c` | absorbed | Utilitários de popcount/CRC32 incorporados em sandbox técnico com rastreio de promoção. |

### Backlog técnico (planejado)
| Origem | Destino planejado | Status | Justificativa |
|---|---|---|---|
| `_incoming/rmr_test.c` | `demo_cli/src/*` (novo selftest/gate) | backlog | Falta contrato final de entrada/saída para virar gate formal de CI. |
| `_incoming/termux.c` | `app`/JNI bridge (se aprovado) | backlog | Depende de contrato JNI + boundary de segurança para não vazar caminho experimental em release oficial. |
| `_incoming/RAFAELIA_MATH_FORMULAS.md` | docs de auditoria matemática | backlog | Material de referência; não é executável nem parte de toolchain. |

### Arquivos classificados como `archived`
Todos os demais arquivos do diretório `_incoming/` estão `archived` por motivo técnico unificado: ausência de integração em build canônico (Gradle/CMake/JNI), ausência de contrato ABI estável e ausência de consumo explícito no pipeline oficial de CI/release.

Referência da listagem consolidada: `_incoming/README.md`.

## Navegação de evidência por módulo
- `engine/rmr`: [`docs/SURGICAL_PATCHSET.md#module-engine-rmr`](../docs/SURGICAL_PATCHSET.md#module-engine-rmr)
- `bug/core`: [`docs/SURGICAL_PATCHSET.md#module-bug-core`](../docs/SURGICAL_PATCHSET.md#module-bug-core)
- `app`: [`docs/SURGICAL_PATCHSET.md#module-app`](../docs/SURGICAL_PATCHSET.md#module-app)
