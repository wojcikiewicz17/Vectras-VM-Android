# engine/

## Objetivo
Abriga o núcleo de execução determinística em linguagens de baixo nível para política, benchmark e inspeção de performance.

## Estrutura de arquivos
- `rmr/`: engine C com headers em `include/`, implementações em `src/` e documentação própria.
- `vectra_policy_kernel/`: kernel de política em Rust com `src/`, `tests/`, `Cargo.toml` e lockfile.

## Conceitos principais
1. **Determinismo operacional**: rotinas de benchmark e ciclo reproduzível de execução.
2. **Separação por linguagem**: C para paths de alta previsibilidade e Rust para kernel com testes dedicados.
3. **Instrumentação explícita**: APIs e binários voltados à medição, validação e políticas de execução.
