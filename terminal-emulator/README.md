# terminal-emulator/

## Objetivo
Biblioteca de emulação de terminal usada pelo ecossistema da aplicação.

## Estrutura de arquivos
- `build.gradle`: configuração do módulo Android library.
- `src/main/`: implementação da engine de emulação.
- `src/test/`: suíte extensa de testes de sequência de controle e comportamento de tela.
- `proguard-rules.pro`: regras de otimização.

## Conceitos principais
1. **Conformidade de protocolo terminal**: foco em parsing/rendering correto de sequências.
2. **Confiabilidade via testes**: forte cobertura para regressões de comportamento.
