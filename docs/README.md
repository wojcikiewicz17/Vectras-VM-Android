# docs/

Camada técnica central de documentação do Vectras VM Android.

## Objetivo desta revisão (2026-04-03)
- Refatorar a navegação para reduzir ambiguidade entre índices globais e guias técnicos locais.
- Padronizar um fluxo de revisão contínua por domínio (app, engine, tools, web, runtime).
- Atualizar metadados de governança documental para facilitar auditoria e rastreabilidade.

## Estrutura de navegação em 5 níveis
1. **Nível 1 (institucional):** [`../README.md`](../README.md)
2. **Nível 2 (índice global):** [`../DOC_INDEX.md`](../DOC_INDEX.md)
3. **Nível 3 (domínio técnico docs):** [`README.md`](README.md)
4. **Nível 4 (audiência/domínio):** [`navigation/INDEX.md`](navigation/INDEX.md)
5. **Nível 5 (documento final rastreável):** arquivos específicos em `docs/` e `docs/navigation/`.

## Guias estruturais desta revisão
- [`THREE_LAYER_ANALYSIS.md`](THREE_LAYER_ANALYSIS.md)
- [`ROOT_FILE_CHAIN.md`](ROOT_FILE_CHAIN.md)
- [`../DOC_INDEX.md`](../DOC_INDEX.md)

## Eixos técnicos especializados
- Arquitetura: `ARCHITECTURE.md`, `API.md`, `RAFAELIA_COHESION_ENTERPRISE_STACK.md`
- Operação e benchmark: `OPERATIONS.md`, `BENCHMARKS.md`, `BENCHMARK_MANAGER.md`
- Build e ambiente: `BUILD_ENV_ALIGNMENT.md`
- Compilador autoral (cabeçalho C → ASM): `RAFCODE_PHI_COMPILER_HEADER.md`
- Qualidade e conformidade: `SECURITY.md`, `LEGAL_AND_LICENSES.md`, `SOURCE_TRACEABILITY_MATRIX.md`, `IP_MAP.md`
- Fullstack (código-fonte ponta a ponta): `FULLSTACK_SOURCE_MAP.md`
- Navegação por público: `navigation/INDEX.md` e derivados
- Inovação e autoria técnica (navegação): `navigation/TECHNOLOGY_INNOVATION_AUTHORSHIP.md`
- Estado atual do ciclo do projeto: `../PROJECT_STATE.md`


## Fluxo de revisão contínua por domínio
Use este checklist ao final de cada ciclo de mudanças técnicas:
- [ ] **app**: validar se APIs/fluxos em `app/` e `docs/API.md` continuam alinhados.
- [ ] **engine**: revisar mudanças em `engine/` e refletir impactos em `docs/ARCHITECTURE.md`.
- [ ] **tools**: confirmar atualização de utilitários/scripts e referências operacionais em `docs/OPERATIONS.md`.
- [ ] **web**: checar documentação de interfaces web (quando aplicável) e índices correlatos.
- [ ] **runtime**: sincronizar alterações de supervisão/execução VM com `docs/ARCHITECTURE.md` e `docs/BENCHMARK_MANAGER.md`.

### Procedimento sugerido (rápido)
1. Levantar mudanças com `git log --name-only --since='14 days ago'`.
2. Mapear impacto documental por domínio técnico.
3. Atualizar documentos afetados e registrar versão/data nesta página.
4. Executar a cadeia de validação documental abaixo.
5. Revisar links cruzados (`README.md`, `DOC_INDEX.md`, `navigation/INDEX.md`).

## Cadeia de comando de validação documental
```bash
find docs -maxdepth 2 -type f | sort
sed -n '1,120p' docs/THREE_LAYER_ANALYSIS.md
sed -n '1,120p' docs/ROOT_FILE_CHAIN.md
```

## Metadados
- Versão do documento: 1.3
- Última atualização: 2026-04-03
- Commit de referência: `HEAD`
- Domínio de código coberto: Portal documental transversal para app, engine, tools, web e runtime.
