# shell-loader/

## Objetivo
Módulo Android auxiliar para carregamento/encadeamento de componentes do runtime.

## Estrutura de arquivos
- `build.gradle`: configuração do módulo principal.
- `src/main/`: manifesto e implementação Java do loader.
- `stub/`: submódulo de stubs Android (`build.gradle`, manifesto e classes de compatibilidade).
- `release/output-metadata.json`: metadados de build.

## Conceitos principais
1. **Camada de bootstrap**: organiza entrada e compatibilidade de carregamento.
2. **Isolamento de stubs**: APIs auxiliares ficam encapsuladas no submódulo `stub`.
