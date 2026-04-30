<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: complete -->

# PROJECT_STATE

Estado atual do projeto: `REFACTORING`.

## Fonte única de referência
Toda a documentação normativa, relatórios vigentes e histórico deve ser consultada exclusivamente em:

### Δ — 57 Pontos Corrigidos
Veja `FIXES_SUMMARY.md` para tabela completa.

## Definições
- **STABLE**: ciclo estável, foco em manutenção e releases.
- **EXPERIMENTAL**: ciclo exploratório, mudanças rápidas e validações.
- **REFACTORING**: ciclo de reestruturação e consolidação técnica.
- **FIXED_REFACTORING**: reestruturação concluída, bugs críticos corrigidos, build funcional.

## Escopo atual (REFACTORING)
- ✅ Consolidação de contratos CI host/android em andamento.
- ✅ Fontes externas críticas (`qemu_rafaelia`, `androidx_RmR`) definidas por manifesto e script de verificação.
- ⚠️ Status de build **não pode ser inferido como atual** sem execução CI no commit corrente.
- ⚠️ Afirmações de aceleração/otimização (ex.: NEON) devem ser tratadas como capacidade de build declarada até validação executada no commit atual.
- ✅ Política ABI oficialmente separada entre trilha oficial e validação interna controlada.

## Documentos canônicos
- `reports/CANONICAL_BUILD_STATUS.md` — **última validação conhecida** de build/release; não substitui execução CI do commit atual.
- `FIXES_SUMMARY.md` — tabela completa 57 fixes
- `docs/SETUP_SDK_NDK.md` — setup local
- `tools/qemu_launch.yml` — QEMU configuration
- `archive/root-history/IMPLEMENTATION_COMPLETE.md`
- Política de overlays: ZIPs na raiz não são fonte de verdade; somente a árvore Git é oficial, com bloqueio em CI para conteúdo duplicado.

> Atualize este arquivo sempre que o estado do projeto mudar.

## CI canonical reference (Android/Host)

- Canonical Android pipeline: `.github/workflows/android-ci.yml`.
- Android wrapper entrypoint: `.github/workflows/android.yml`.
- Auxiliary Android ABI compatibility matrix: `.github/workflows/compile-matrix.yml`.
- Canonical host pipeline: `.github/workflows/host-ci.yml`.
- Orchestration and final gates: `.github/workflows/pipeline-orchestrator.yml` and `.github/workflows/quality-gates.yml`.
- Canonical matrix documentation: `docs/ci/workflow-matrix.md`.
