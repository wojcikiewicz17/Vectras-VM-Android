<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: complete -->

# PROJECT_STATE

Estado atual do projeto: `FIXED_REFACTORING`.

## Fonte única de referência
Toda a documentação normativa, relatórios vigentes e histórico deve ser consultada exclusivamente em:

### Δ — 57 Pontos Corrigidos
Veja `FIXES_SUMMARY.md` para tabela completa.

## Definições
- **STABLE**: ciclo estável, foco em manutenção e releases.
- **EXPERIMENTAL**: ciclo exploratório, mudanças rápidas e validações.
- **REFACTORING**: ciclo de reestruturação e consolidação técnica.
- **FIXED_REFACTORING**: reestruturação concluída, bugs críticos corrigidos, build funcional.

## Escopo (FIXED_REFACTORING)
- ✅ Bug crítico NATIVE_OK_MAGIC corrigido (Java/C alinhados)
- ✅ Link errors corrigidos (lowlevel sources adicionados)
- ✅ Flags incompatíveis (-ffreestanding) removidas
- ✅ NEON/SIMD baremetal aceleração adicionada
- ✅ QEMU bootstrap e YAML config criados
- ✅ Arena API declarada publicamente
- ✅ Todos os 57 pontos corrigidos e documentados
- ✅ Política ABI arm32-arm64 ativa: artefatos compilados para arm64-v8a (NEON/SIMD/CRC32HW) e armeabi-v7a (NEON via -march=armv7-a -mfpu=neon)

## Documentos canônicos
- `reports/CANONICAL_BUILD_STATUS.md` — **arquivo canônico obrigatório** para status de validação de build/release (fonte de verdade operacional).
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
