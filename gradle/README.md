# gradle/

## Objetivo
Componente de bootstrap do sistema de build, garantindo execução reproduzível do Gradle wrapper.

## Estrutura de arquivos
- `wrapper/gradle-wrapper.properties`: versão/configuração de distribuição do Gradle.
- `wrapper/gradle-wrapper.jar`: binário do wrapper.

## Conceitos principais
1. **Reprodutibilidade de build**: mesma cadeia de build entre ambientes.
2. **Padronização de toolchain**: elimina dependência de instalação manual do Gradle.
