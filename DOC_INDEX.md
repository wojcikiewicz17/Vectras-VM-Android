# Índice de Documentação do Repositório

## Entradas principais
- [`README.md`](README.md): visão institucional + navegação por camadas.
- [`docs/README.md`](docs/README.md): curadoria técnica.
- [`docs/THREE_LAYER_ANALYSIS.md`](docs/THREE_LAYER_ANALYSIS.md): análise estrutural em 3 camadas.
- [`docs/ROOT_FILE_CHAIN.md`](docs/ROOT_FILE_CHAIN.md): reflexão arquivo-a-arquivo da raiz.

## Documentos de raiz
| Documento | Finalidade | Público-alvo | Classificação | Localização atual | Inbound link |
|---|---|---|---|---|---|
| `README.md` | Porta de entrada do repositório e mapa de navegação geral. | Novos contribuidores, engenharia, auditoria técnica. | **Ativo** | Raiz | [`DOC_INDEX.md`](DOC_INDEX.md) |
| `DOC_INDEX.md` | Índice canônico da documentação e governança documental. | Engenharia, technical writers, manutenção de docs. | **Ativo** | Raiz | [`README.md`](README.md) |
| `PROJECT_STATE.md` | Estado corrente do ciclo de vida (stable/experimental/refactoring). | Liderança técnica, release managers, PM. | **Ativo** | Raiz | [`README.md`](README.md#governança-e-estado---navegação-rápida) |
| `CHANGELOG.md` | Histórico contínuo de mudanças por versão. | Desenvolvimento, QA, release managers. | **Ativo** | Raiz | [`README.md`](README.md#governança-e-estado---navegação-rápida) |
| `RELEASE_NOTES.md` | Notas executivas da release mais recente (registro temporal). | Produto, operação, stakeholders externos. | **Histórico** | Raiz | [`README.md`](README.md#governança-e-estado---navegação-rápida) |
| `THIRD_PARTY_NOTICES.md` | Registro legal/licenças de terceiros. | Compliance, jurídico, segurança. | **Ativo** | Raiz | [`README.md`](README.md#governança-e-estado---navegação-rápida) |
| `VECTRA_CORE.md` | Referência conceitual do runtime Vectra Core MVP. | Engenharia de runtime e arquitetura. | **Ativo** | Raiz | [`README.md`](README.md#governança-e-estado---navegação-rápida) |
| `VECTRAS_MEGAPROMPT_DOCS.md` | Guia estratégico de documentação técnica ampla do projeto. | Documentação técnica, enablement e coordenação. | **Ativo** | Raiz | [`README.md`](README.md#governança-e-estado---navegação-rápida) |
| `archive/root-history/1.md` | Registro pontual de ajuste de compatibilidade/chaves. | Auditoria histórica. | **Arquivado** | `archive/root-history/` | [`README.md`](README.md#histórico-arquivado-raiz) |
| `archive/root-history/ADVANCED_OPTIMIZATIONS.md` | Catálogo histórico de módulos avançados de otimização. | Engenharia/performance (consulta histórica). | **Arquivado** | `archive/root-history/` | [`README.md`](README.md#histórico-arquivado-raiz) |
| `archive/root-history/BENCHMARK_REFACTORING_SUMMARY.md` | Sumário histórico da etapa de refatoração de benchmark. | Performance, engenharia de validação. | **Arquivado** | `archive/root-history/` | [`README.md`](README.md#histórico-arquivado-raiz) |
| `archive/root-history/IMPLEMENTATION_COMPLETE.md` | Registro histórico de implementação consolidada. | Auditoria técnica e rastreabilidade histórica. | **Arquivado** | `archive/root-history/` | [`README.md`](README.md#histórico-arquivado-raiz) |
| `archive/root-history/IMPLEMENTATION_SUMMARY.md` | Resumo histórico de implementação por evidências. | Auditoria técnica e documentação histórica. | **Arquivado** | `archive/root-history/` | [`README.md`](README.md#histórico-arquivado-raiz) |
| `archive/root-history/VECTRAS_ANALYSIS_COMPLETE.md` | Relato histórico de análise consolidada. | Pesquisa histórica e auditoria. | **Arquivado** | `archive/root-history/` | [`README.md`](README.md#histórico-arquivado-raiz) |
| `archive/root-history/VECTRAS_DEEP_EVIDENCE.md` | Evidências históricas detalhadas de revisão/validação. | Auditoria e governança histórica. | **Arquivado** | `archive/root-history/` | [`README.md`](README.md#histórico-arquivado-raiz) |

## Diretórios com documentação completa
Cada diretório tem:
1) `README.md` (camadas 1 e 2)
2) `FILES_MAP.md` (camada 3)

- `app/`, `engine/`, `terminal-emulator/`, `terminal-view/`, `shell-loader/`, `bench/`, `bug/`, `demo_cli/`, `tools/`, `docs/`, `reports/`, `resources/`, `runtime/`, `web/`, `archive/`, `fastlane/`, `gradle/`, `3dfx/`.

- `bug/`: [`bug/README.md`](bug/README.md) e [`bug/FILES_MAP.md`](bug/FILES_MAP.md).

## Documentos técnicos especializados
- Arquitetura: `docs/ARCHITECTURE.md`
- Operação: `docs/OPERATIONS.md`
- Performance: `docs/PERFORMANCE_INTEGRITY.md`, `docs/BENCHMARKS.md`
- Conformidade: `docs/LEGAL_AND_LICENSES.md`, `docs/IP_MAP.md`, `THIRD_PARTY_NOTICES.md`
