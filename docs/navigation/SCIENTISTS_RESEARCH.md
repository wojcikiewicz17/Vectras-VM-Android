# Vectras VM — Scientists & Research (Reproducible)

## Resumo
Guia de pesquisa alinhado ao código do projeto, com foco em reprodutibilidade experimental e rastreabilidade de resultados.

## Escopo
- Coberto:
  - Perguntas de pesquisa suportadas pela implementação atual.
  - Protocolo de coleta e publicação de dados.
- Não coberto:
  - Resultados estatísticos fixos sem dataset versionado no repositório.

## Fontes no repositório
- `app/src/main/java/com/vectras/vm/benchmark/VectraBenchmark.java`
- `app/src/main/java/com/vectras/vm/benchmark/BenchmarkManager.java`
- `app/src/main/java/com/vectras/vm/core/*`
- `docs/BENCHMARK_MANAGER.md`
- `docs/PERFORMANCE_INTEGRITY.md`

## Perguntas sugeridas
1. Como cada categoria de métrica varia entre cenários equivalentes?
2. Quais sinais de ambiente afetam consistência dos resultados?
3. Quais ajustes de execução alteram latência/throughput?

## Protocolo mínimo para paper
1. Fixar commit SHA, variante e dispositivo.
2. Executar múltiplas rodadas por cenário com mesmas condições.
3. Versionar dados brutos e scripts de análise.
4. Publicar método e limitações do ambiente de teste.

## Metadados
- Versão do documento: 1.3
- Última atualização: 2026-03-06
- Commit de referência: `HEAD`
- Domínio de código coberto: Pesquisa reproduzível sobre benchmark, core e integração JNI/app.
