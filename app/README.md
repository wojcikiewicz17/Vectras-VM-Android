# app/

## Objetivo
Módulo Android principal do Vectras VM: concentra UI, fluxo de execução de VM, integrações nativas, configurações de build e suíte principal de testes unitários.

## Estrutura de arquivos
- `build.gradle`: configuração Android application, flavors de build e dependências.
- `src/main/`: código-fonte da aplicação, recursos (`res/`) e manifesto.
- `src/test/`: testes unitários para núcleo lógico (benchmark, core e módulos Rafaelia).
- `proguard-rules.pro`: regras de minificação/obfuscação.
- `FIREBASE.md`: notas de integração Firebase no contexto do app.

## Conceitos principais
1. **Orquestração Android-first**: o módulo é o ponto de entrada operacional do produto.
2. **Integração híbrida**: combina camadas Kotlin/Java com componentes nativos (via CMake/NDK).
3. **Rastreabilidade por testes**: diretórios de teste cobrem utilitários determinísticos, benchmark e componentes de runtime.
