# terminal-view/

## Objetivo
Camada visual do terminal: renderização, gestos e integração com a engine de emulação.

## Estrutura de arquivos
- `build.gradle`: configuração do módulo de view.
- `src/main/java/com/termux/view/`: renderizador, view e interfaces cliente.
- `src/main/res/`: recursos visuais e strings.
- `proguard-rules.pro`: regras de minificação.

## Conceitos principais
1. **Pipeline de renderização dedicado**: separa desenho, entrada e contrato de cliente.
2. **Interação de alta responsividade**: tratamento de gesto/escala voltado a uso terminal.
