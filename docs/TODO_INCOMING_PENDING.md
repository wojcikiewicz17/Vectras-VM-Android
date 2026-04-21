# TODO - _incoming/pending triagem técnica

Todos os itens abaixo estão em `_incoming/pending/` e **não podem** entrar em build até promoção para diretórios canônicos (`engine/rmr/src`, `engine/rmr/interop`, `bug/core`, `app/src/main`).

## Critério de aceite técnico (obrigatório para qualquer promoção)

- Definir módulo canônico de destino e justificar ABI/arquitetura alvo (arm64-v8a, x86_64, etc.).
- Implementar/ajustar cabeçalhos públicos em `engine/rmr/include` quando necessário.
- Adicionar testes/auto-checks mínimos (build + símbolo exportado + comportamento determinístico quando aplicável).
- Atualizar `engine/rmr/sources.cmake` **somente** após promoção para diretório canônico.
- Rodar sincronizadores de manifest e validar diff nulo em arquivos derivados quando aplicável.
- Registrar commit de integração no `docs/INCOMING_INGESTION_MAP.md`.

## Backlog rastreável

- [ ] `RAFAELIA_MATH_FORMULAS.md` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `bitraf64_prototype_Version4.py` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `r.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_10x10.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_7d.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_7d_gyro.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_7d_shapes.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_8way.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_936_fast.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_999_logsin.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_abs.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_avalanche.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_avalanche_v2.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_bench_phi.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_central_link.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_chrono.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_decision.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_delta.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_equitas.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_final.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_final_bench.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_final_seal.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_fix.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_flash.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_flops.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_fractal.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_hold.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_hw_sync.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_integrity.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_invasion.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_l2.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_life.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_next.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_pilar.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_prime.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_prob.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_pure_asm.c` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_sin_log.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_stabile.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_toro.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_torus.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_ttl` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_ttl.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_ultra.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_vacuo.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_vacuo_v2.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_vortex.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_vortex_live.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rafaelia_vortex_v2.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rmr_final.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rmr_hidden.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rmr_matrix.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rmr_nihil.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rmr_persist.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rmr_spiral.S` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.
- [ ] `rmr_test.c` → destino: `TBD` | aceite: módulo canônico + teste + atualização de manifest + commit registrado.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
