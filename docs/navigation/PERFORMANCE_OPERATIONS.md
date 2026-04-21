<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Vectras VM — Performance Operations (Runbook)

## Resumo
Runbook de operação para medir e ajustar desempenho com rastreabilidade e rollback seguro.

## Escopo
- Coberto:
  - Preparação de ambiente.
  - Coleta de benchmark.
  - Ajuste incremental e validação.
- Não coberto:
  - Números fixos por dispositivo sem artefato anexado.

## Fluxo operacional
1. Preparar ambiente (energia, temperatura, carga em background).
2. Executar benchmark e registrar diagnósticos.
3. Ajustar um parâmetro por vez.
4. Comparar antes/depois com mesmo protocolo.
5. Registrar decisão técnica e rollback.

## Checklist de qualidade
- Mesma variante/build/dispositivo entre cenários.
- Resultado válido com relatório bruto salvo.
- Limitações de ambiente descritas.

## Metadados
- Versão do documento: 1.3
- Última atualização: 2026-03-06
- Commit de referência: `HEAD`
- Domínio de código coberto: Operação de performance com benchmark no app e execução runtime.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
