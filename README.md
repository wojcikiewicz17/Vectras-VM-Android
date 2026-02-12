# Vectras VM Android

> Plataforma Android para execução, orquestração e observabilidade de máquinas virtuais com trilhas determinísticas de execução, integração com componentes nativos e foco em rastreabilidade operacional.

## Abstract
Este repositório organiza uma base híbrida (Android + C/C++ + Rust + utilitários shell/web) para operar workloads de virtualização em dispositivos móveis. A arquitetura combina camadas de interface, supervisão de processos, módulos de terminal, kernel de políticas e instrumentação de benchmark para garantir previsibilidade de comportamento sob carga, inspeção auditável de eventos e evolução incremental do desempenho. A documentação desta versão foi refatorada para unificar conteúdos anteriormente dispersos, criando uma navegação por diretórios, objetivos técnicos e conceitos de cada módulo do código-fonte.

## Resumo executivo
O projeto está estruturado para separar responsabilidades entre:
- **Aplicação Android (`app/`)**: ponto principal de execução e experiência do usuário.
- **Engine determinística (`engine/`)**: componentes de baixo nível em C e Rust para política, benchmark e testes de núcleo.
- **Módulos de suporte (`terminal-*`, `shell-loader/`)**: terminal e carregamento auxiliar no ecossistema Android.
- **Operação e governança (`docs/`, `reports/`, `tools/`)**: documentação, auditoria técnica e automações de verificação.

## Dissertação analítica da arquitetura documental
A refatoração documental desta entrega parte de três problemas recorrentes: (1) documentos em posição isolada sem ligação explícita com o código, (2) baixa previsibilidade de navegação entre diretórios técnicos e (3) ausência de uma taxonomia única para manutenção. Para resolver isso, a estrutura foi consolidada em um eixo de leitura que sai deste `README.md`, passa pelos READMEs de cada diretório de primeiro nível e converge para os documentos especializados em `docs/`.

Em termos de governança, o repositório passa a operar por **encadeamento de contexto**:
1. **Contexto institucional** (objetivo, escopo e arquitetura global) neste documento.
2. **Contexto estrutural** (papel de cada diretório) nos READMEs locais.
3. **Contexto técnico profundo** em documentos analíticos e artefatos de relatório.

Esse modelo reduz divergências entre “estado documentado” e “estado implementado”, melhora onboarding técnico e cria base para auditoria de conformidade por trilhas reproduzíveis (build, teste, benchmark e histórico de mudanças).

## Mapa profissional de navegação por diretório
| Diretório | Função principal | README local |
|---|---|---|
| `app/` | Aplicação Android principal, integração de UI/runtime e testes unitários | [app/README.md](app/README.md) |
| `engine/` | Núcleo nativo determinístico (C/Rust), benchmark e política | [engine/README.md](engine/README.md) |
| `terminal-emulator/` | Biblioteca de emulação de terminal | [terminal-emulator/README.md](terminal-emulator/README.md) |
| `terminal-view/` | Camada de renderização/gestos do terminal | [terminal-view/README.md](terminal-view/README.md) |
| `shell-loader/` | Loader Android e módulo `stub` de compatibilidade | [shell-loader/README.md](shell-loader/README.md) |
| `bench/` | Benchmarks de baixo nível e scripts de execução | [bench/README.md](bench/README.md) |
| `demo_cli/` | Demos CLI de kernels e módulos nativos | [demo_cli/README.md](demo_cli/README.md) |
| `tools/` | Scripts utilitários para integridade e orquestração | [tools/README.md](tools/README.md) |
| `docs/` | Base documental técnica e de governança | [docs/README.md](docs/README.md) |
| `reports/` | Relatórios técnicos, métricas e validações | [reports/README.md](reports/README.md) |
| `resources/` | Ativos compartilhados (android/web/lang) | [resources/README.md](resources/README.md) |
| `runtime/` | Materiais de showcase/runtime | [runtime/README.md](runtime/README.md) |
| `web/` | Artefatos estáticos e catálogos de dados web | [web/README.md](web/README.md) |
| `archive/` | Materiais experimentais e históricos | [archive/README.md](archive/README.md) |
| `fastlane/` | Metadados de distribuição para publicação | [fastlane/README.md](fastlane/README.md) |
| `gradle/` | Wrapper e bootstrap de build | [gradle/README.md](gradle/README.md) |
| `3dfx/` | Imagens ISO associadas a wrappers 3dfx | [3dfx/README.md](3dfx/README.md) |

## Estrutura do projeto e funções críticas
### Build e módulos
- `settings.gradle` centraliza os módulos ativos de build: `app`, `terminal-emulator`, `terminal-view`, `shell-loader` e `shell-loader:stub`.
- `build.gradle` (raiz) define toolchain e task de verificação de dependências locais do repositório.

### Documentos raiz estratégicos
- `CHANGELOG.md` e `RELEASE_NOTES.md`: trilha de evolução e release.
- `PROJECT_STATE.md`: fotografia de estado técnico.
- `VECTRA_CORE.md` e documentos `IMPLEMENTATION_*`: materiais de implementação orientada a núcleo.
- `DOC_INDEX.md`: índice rápido complementar para consumo técnico.

## Fluxo de trabalho recomendado
1. Ler este `README.md` para visão institucional e mapa de navegação.
2. Abrir o README do diretório de interesse para contexto estrutural e conceitos.
3. Aprofundar em `docs/` e `reports/` para referências técnicas, operação e validação.
4. Executar build/testes com Gradle wrapper para consistência de ambiente.

## Comandos essenciais
```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew verifyRepoFileDependencies
```

## Referências internas
- Índice documental geral: [DOC_INDEX.md](DOC_INDEX.md)
- Catálogo de arquivos raiz: [docs/ROOT_FILES_CATALOG.md](docs/ROOT_FILES_CATALOG.md)
- Navegação por perfis: [docs/navigation/INDEX.md](docs/navigation/INDEX.md)
