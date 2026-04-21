<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Vectras VM — Benchmark Comparisons (Code-Grounded)

## Resumo
Documento de comparação de benchmark orientado por evidência reproduzível. Define método e formato de publicação; não fixa números sem artefato de execução.

## Escopo
- Coberto:
  - Estrutura da suíte de benchmark implementada.
  - Protocolo de coleta e comparação entre cenários.
  - Requisitos mínimos de publicação de resultados.
- Não coberto:
  - Tabelas fixas por dispositivo sem relatório bruto anexado.

## Base no código
- `app/src/main/java/com/vectras/vm/benchmark/VectraBenchmark.java`
- `app/src/main/java/com/vectras/vm/benchmark/BenchmarkManager.java`
- `app/src/main/java/com/vectras/vm/benchmark/BenchmarkActivity.java`

## Estrutura da suíte
- `METRIC_COUNT = 79`.
- Categorias: CPU single-thread, CPU multi-thread, memória, storage, integridade, emulação.

## Protocolo de comparação
1. Definir dispositivo, build variant e commit SHA.
2. Executar preflight e capturar warnings/diagnósticos.
3. Rodar cenários equivalentes (ex.: baseline e VM).
4. Salvar resultado bruto por métrica e relatório consolidado.
5. Publicar comparação com artefatos anexos.

## Template mínimo
| Métrica | Cenário A | Cenário B | Diferença | Válido |
|---|---:|---:|---:|---|
| CPU_INTEGER_ADD | _valor_ | _valor_ | _calc_ | _true/false_ |
| MEM_COPY_BANDWIDTH | _valor_ | _valor_ | _calc_ | _true/false_ |
| STORAGE_SEQ_READ | _valor_ | _valor_ | _calc_ | _true/false_ |

## Regras de publicação
- Publicar somente com: dispositivo, variant, commit SHA, relatório bruto.
- Declarar limitações de ambiente que afetem estabilidade dos dados.

## Metadados
- Versão do documento: 1.3
- Última atualização: 2026-03-06
- Commit de referência: `HEAD`
- Domínio de código coberto: App benchmark (`app/src/main/java/com/vectras/vm/benchmark/*`) e documentação de método comparativo (`docs/navigation`).

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
