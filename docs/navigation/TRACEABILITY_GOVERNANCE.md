<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Vectras VM — Traceability & Governance

## Resumo
Guia de governança para auditoria técnica e rastreabilidade entre requisitos, documentação e implementação, priorizando evidência verificável por comando.

## Escopo
- Coberto:
  - Matriz de rastreabilidade, padrões documentais e operação com evidência.
  - Conexão entre integridade de performance, segurança e ciclo de manutenção.
- Não coberto:
  - Declarações de compliance sem referência para arquivo e linha no repositório.

## Trilhas primárias
- `docs/SOURCE_TRACEABILITY_MATRIX.md`
- `docs/DOCUMENTATION_STANDARDS.md`
- `docs/archive/2026-04-06_status-superseded_VM_SUPERVISION_AUDIT_EVIDENCE.md`
- `docs/SECURITY.md`
- `docs/PERFORMANCE_INTEGRITY.md`

## Metadados
- Versão: 1.0
- Última atualização: 2026-02
- Responsável: manutenção documental
- Licença: GPL-2.0

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
