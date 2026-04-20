<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# RAFAELIA — Cirurgia de Coesão Coerente (Enterprise Stack)

> Documento de coesão arquitetural para consolidar símbolos metodológicos (Σ Ω Δ Φ, Trinity_633, parser/index/proof) em trilhas técnicas auditáveis no Vectras VM Android.

## Objetivo

Transformar linguagem simbólica em contratos de engenharia executáveis, com foco em:
- determinismo operacional;
- rastreabilidade ponta-a-ponta;
- segurança por evidência;
- evolução contínua sem regressão de coerência.

## Canon de Coesão (símbolo → função técnica)

| Símbolo/Eixo | Interpretação operacional | Evidência no repositório |
|---|---|---|
| `Σ` (Index) | agregação de estado, catálogo e inventário de artefatos | `DOC_INDEX.md`, `docs/FILES_MAP.md`, `README.md` |
| `Ω` (Parser) | normalização de entrada, contratos e regras de integração | `docs/API.md`, `docs/ARCHITECTURE.md`, `engine/rmr/include/` |
| `Δ` (Mutação controlada) | evolução incremental com validação por camada | `CHANGELOG.md`, `docs/THREE_LAYER_ANALYSIS.md` |
| `Φ` (Proof) | prova de integridade, segurança e conformidade | `docs/SECURITY.md`, `docs/SOURCE_TRACEABILITY_MATRIX.md`, `security/` |
| `Trinity_633` | ciclo triplo: Build → Runtime → Evidência | `.github/workflows/`, `runtime/`, `reports/` |

## Pipeline Enterprise (full stack, coerente)

1. **Indexação (`Σ`)**
   - consolidar estado, documentos e mapas de arquivos;
   - manter catálogo rastreável por módulo.

2. **Parsing/Contratos (`Ω`)**
   - definir contratos de interface entre app/engine/runtime;
   - rejeitar entradas ambíguas (falha explícita > comportamento implícito).

3. **Mutação Determinística (`Δ`)**
   - aplicar mudanças pequenas, verificáveis e reversíveis;
   - registrar impacto técnico antes/depois.

4. **Prova (`Φ`)**
   - validar por testes/checagens de consistência;
   - publicar evidência em relatórios e histórico de release.

## Mapeamento para domínios do repositório

- **Governança documental**: `README.md`, `DOC_INDEX.md`, `docs/README.md`, `docs/FILES_MAP.md`.
- **Núcleo low-level (RMR)**: `engine/rmr/include/`, `engine/rmr/src/`.
- **Aplicação Android**: `app/` (UI, ciclo de vida, integração com runtime).
- **Execução e operação**: `runtime/`, `tools/`, `reports/`.
- **Segurança e conformidade**: `security/`, `docs/SECURITY.md`, `docs/LEGAL_AND_LICENSES.md`.

## Critérios de aceite da coesão

Uma mudança é considerada **coesa** quando atende simultaneamente:
1. **Coerência estrutural**: aparece no índice certo e no mapa de arquivos.
2. **Coerência de contrato**: explicita ligação com arquitetura/API/fluxo operacional.
3. **Coerência de prova**: possui validação técnica reproduzível (comando + resultado).
4. **Coerência de evolução**: registra mutação em changelog/release/relatório.

## Checklist curto (uso prático por PR)

- [ ] A mudança está indexada (`Σ`) em docs de navegação?
- [ ] O contrato técnico foi atualizado (`Ω`) sem ambiguidade?
- [ ] A mutação foi mínima e rastreável (`Δ`)?
- [ ] A prova de validação foi anexada (`Φ`)?

## Resultado esperado

Com este padrão, o repositório mantém uma linha contínua entre visão simbólica e execução técnica: **Index → Parser → Mutação → Prova**, preservando determinismo, auditabilidade e governança enterprise.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
