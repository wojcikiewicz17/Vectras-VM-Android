# docs/

Repositório central da documentação técnica, operacional e de governança do Vectras VM Android.

## Papel desta camada documental
A documentação foi organizada para manter coerência entre:
1. **Código-fonte ativo** (módulos e diretórios do repositório).
2. **Racional técnico** (arquitetura, segurança, operação, benchmark).
3. **Conformidade e rastreabilidade** (licenças, matriz de fonte e relatórios).

## Fluxo de leitura recomendado
1. `../README.md` — visão institucional, abstract e navegação global.
2. `../DOC_INDEX.md` — índice rápido de documentos raiz e técnicos.
3. `ARCHITECTURE.md` e `SOURCE_TRACEABILITY_MATRIX.md` — base de arquitetura e rastreabilidade.
4. `OPERATIONS.md`, `PERFORMANCE_INTEGRITY.md`, `BENCHMARKS.md` — operação e validação.
5. `LEGAL_AND_LICENSES.md` e `IP_MAP.md` — conformidade jurídica e propriedade intelectual.

## Estrutura temática
### Fundamentos e visão
- `ABSTRACT.md`
- `RESUMO.md`
- `PREFACE.md`
- `WHITEPAPER.md`

### Arquitetura e engenharia
- `ARCHITECTURE.md`
- `API.md`
- `INTEGRACAO_RM_QEMU_ANDROIDX.md`
- `DETERMINISTIC_VM_MUTATION_LAYER.md`

### Operação e desempenho
- `OPERATIONS.md`
- `PERFORMANCE_INTEGRITY.md`
- `BENCHMARKS.md`
- `BENCHMARK_MANAGER.md`
- `RAFAELIA_PERF_OPS.md`

### Governança documental
- `DOCUMENTATION_STANDARDS.md`
- `DOCUMENTATION_SUMMARY.md`
- `ROOT_FILES_CATALOG.md`
- `REPO_XRAY.md`

### Conformidade
- `SECURITY.md`
- `LEGAL_AND_LICENSES.md`
- `SOURCE_TRACEABILITY_MATRIX.md`
- `IP_MAP.md`

### Navegação por público
- `navigation/INDEX.md`
- `navigation/ENTERPRISE_COMPANIES.md`
- `navigation/SCIENTISTS_RESEARCH.md`
- `navigation/UNIVERSITIES_ACADEMIC.md`
- `navigation/BENCHMARK_COMPARISONS.md`
- `navigation/PERFORMANCE_OPERATIONS.md`

## Princípio de manutenção
Toda alteração estrutural em diretórios de primeiro nível deve refletir:
- no `README.md` raiz (mapa institucional);
- no README local do diretório afetado;
- e, quando aplicável, em documentação técnica específica deste diretório `docs/`.
