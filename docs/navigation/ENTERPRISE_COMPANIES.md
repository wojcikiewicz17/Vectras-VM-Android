<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Vectras VM — Enterprise Guide (Operational)

## Resumo
Guia para avaliação e operação corporativa com foco em implantação técnica, validação reproduzível e coesão full-stack (produto + runtime + governança).

## Escopo
- Coberto:
  - Cenários de uso e requisitos técnicos.
  - Procedimento de validação antes de adoção.
  - Matriz de coesão coerente entre arquitetura, operação e compliance.
- Não coberto:
  - Estimativas monetárias sem evidência auditável.

## Cirurgia de Coesão Coerente (Enterprise)

### Pilar 1 — Produto (Android App)
- UX orientada a operação segura (erros acionáveis, fluxo de import/export, trilha de logs).
- Compatibilidade com versões Android e modelo de permissões por API.
- Critério de aceite: experiência consistente entre criação, execução e observabilidade da VM.

### Pilar 2 — Runtime (QEMU + Vectra Core)
- QEMU parametrizado por perfil com baseline reprodutível.
- Vectra Core como camada determinística de evidência (`CRC32C`, paridade, log append-only).
- Critério de aceite: execução estável + rastreabilidade de estado (`Input → Process → Output → Next`).

### Pilar 3 — Plataforma (Storage + Build + Release)
- Pipeline de build assinado e segregado (debug/release).
- Gestão de artefatos, versionamento e cadeia de inspeção documental.
- Critério de aceite: build reproduzível, trilha de auditoria e rollback possível.

### Pilar 4 — Governança (Segurança + Compliance)
- Baseline com ISO/NIST/IEEE/RFC já referenciados na documentação do projeto.
- Matriz de rastreabilidade entre requisito, implementação e evidência.
- Critério de aceite: conformidade demonstrável por artefato e comando de inspeção.

## Matriz de Coerência (Σ Ω Δ φ)
| Camada | Objetivo | Evidência mínima | Comando sugerido |
|---|---|---|---|
| Produto | Fluxo operacional claro | prints, logs, checklist de UX | `sed -n '1,200p' docs/OPERATIONS.md` |
| Runtime | Determinismo e integridade | log binário, benchmark, estado de ciclo | `sed -n '1,220p' VECTRA_CORE.md` |
| Plataforma | Reprodutibilidade de build | versionamento, assinaturas, changelog | `sed -n '1,220p' README.md` |
| Governança | Auditabilidade | matriz fonte↔requisito↔evidência | `sed -n '1,200p' docs/SOURCE_TRACEABILITY_MATRIX.md` |

## Cenários de uso
- Laboratório de validação de ambientes virtualizados.
- Compatibilidade de software legado.
- Treinamento técnico com imagem controlada.
- Testes de integração em dispositivos Android.
- Pré-produção com validação determinística antes de rollout.

## Runbook de validação operacional
1. Definir baseline por workload e dispositivo.
2. Rodar benchmark com coleta de diagnósticos.
3. Validar logs de integridade e eventos de runtime.
4. Aplicar critérios de aceite por cenário.
5. Registrar artefatos, versão de build e hash de evidência.
6. Publicar relatório de decisão (go/no-go) com riscos residuais.

## Metadados
- Versão do documento: 1.4
- Última atualização: 2026-03-06
- Commit de referência: `HEAD`
- Domínio de código coberto: Arquitetura e operação full-stack (app, engine/runtime, governança docs).

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
