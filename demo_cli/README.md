# demo_cli/

## Objetivo
Exemplos de linha de comando para validar módulos de baixo nível sem depender da interface Android.

## Estrutura de arquivos
- `src/main.c`: executável principal de demonstração.
- `src/*_demo.c`: demos específicas de módulo.
- `src/*_selftest.c`: auto-testes locais para validação de comportamento.

## Conceitos principais
1. **Teste incremental via CLI**: facilita depuração de algoritmos e kernels.
2. **Desacoplamento de UI**: valida a lógica nativa independentemente do app.
