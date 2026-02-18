# docs/

Camada técnica central de documentação do Vectras VM Android.

## Estrutura em três camadas
- Camada 1 (institucional): [`../README.md`](../README.md)
- Camada 2 (diretório): READMEs locais por módulo.
- Camada 3 (arquivo): `FILES_MAP.md` em cada diretório.

## Guias estruturais desta revisão
- [`THREE_LAYER_ANALYSIS.md`](THREE_LAYER_ANALYSIS.md)
- [`ROOT_FILE_CHAIN.md`](ROOT_FILE_CHAIN.md)
- [`../DOC_INDEX.md`](../DOC_INDEX.md)

## Eixos técnicos especializados
- Arquitetura: `ARCHITECTURE.md`, `API.md`, `RAFAELIA_COHESION_ENTERPRISE_STACK.md`
- Operação e benchmark: `OPERATIONS.md`, `BENCHMARKS.md`, `BENCHMARK_MANAGER.md`
- Build e ambiente: `BUILD_ENV_ALIGNMENT.md`
- Qualidade e conformidade: `SECURITY.md`, `LEGAL_AND_LICENSES.md`, `SOURCE_TRACEABILITY_MATRIX.md`, `IP_MAP.md`
- Fullstack (código-fonte ponta a ponta): `FULLSTACK_SOURCE_MAP.md`
- Navegação por público: `navigation/INDEX.md` e derivados


## Checklist de freshness por domínio
- [ ] **app**: validar se APIs/fluxos em `app/` e `docs/API.md` continuam alinhados.
- [ ] **engine**: revisar mudanças em `engine/` e refletir impactos em `docs/ARCHITECTURE.md`.
- [ ] **tools**: confirmar atualização de utilitários/scripts e referências operacionais em `docs/OPERATIONS.md`.
- [ ] **web**: checar documentação de interfaces web (quando aplicável) e índices correlatos.
- [ ] **runtime**: sincronizar alterações de supervisão/execução VM com `docs/ARCHITECTURE.md` e `docs/BENCHMARK_MANAGER.md`.

## Cadeia de comando de validação documental
```bash
find docs -maxdepth 2 -type f | sort
sed -n '1,120p' docs/THREE_LAYER_ANALYSIS.md
sed -n '1,120p' docs/ROOT_FILE_CHAIN.md
```

## Metadados
- Versão do documento: 1.1
- Última atualização: 2026-02-18
- Commit de referência: `8a378fa`
- Domínio de código coberto: Portal documental transversal para app, engine, tools, web e runtime.
