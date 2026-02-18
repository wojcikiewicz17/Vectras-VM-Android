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
- Versão do documento: 1.2
- Última atualização: 2026-02-18
- Commit de referência: `8a378fa`
- Domínio de código coberto: App benchmark (`app/src/main/java/com/vectras/vm/benchmark/*`) e documentação de método comparativo (`docs/navigation`).
