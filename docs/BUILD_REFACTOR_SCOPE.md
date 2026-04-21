<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Escopo de Refatoração Técnica — Build/Release/CI/Native

## Prioridade P0 — inconsistências estruturais (toolchain/ABI/CI)

### 1) Unidade: `.github/workflows/android-ci.yml` (canônico) + `.github/workflows/android.yml` (wrapper de entrada)
- **Motivo**: é o contrato oficial que governa variantes, modo de execução e política de assinatura para todo pipeline Android.
- **Problema observável**: documentação raiz descrevia inputs legados (`build_debug`, `build_release`, `sign_release`) que não existem mais no workflow canônico (hoje usa `build_variant`, `signing_mode`, `allow_legacy_fallback`).
- **Contrato esperado**: documentação e workflow devem ter o mesmo contrato de entrada para evitar disparo manual inválido e diagnósticos falsos de CI.
- **Risco de regressão**: médio (quebra operacional em runs manuais por parâmetros errados; não quebra compilação local diretamente).
- **Critério de aceite**: `README.md` refletindo inputs ativos do contrato canônico em `.github/workflows/android-ci.yml` e o subconjunto exposto por `.github/workflows/android.yml`.

### 2) Unidade: `README.md` + seção `Como rodar manualmente` (bloco de inputs do Android CI)
- **Motivo**: é o ponto de entrada operacional para release/build manual.
- **Problema observável**: lista de parâmetros estava defasada do workflow em produção.
- **Contrato esperado**: operadores devem conseguir executar `workflow_dispatch` sem tradução reversa do YAML.
- **Risco de regressão**: baixo no código, alto no processo (erro humano de operação).
- **Critério de aceite**: seção atualizada com `build_variant`, `mode`, `run_lint`, `run_native_matrix`, `signing_mode`, `allow_legacy_fallback`, `upload_telegram`.

### 3) Unidade: `docs/BUILD_REFACTOR_SCOPE.md` + seção `Execução especial solicitada (llama.cpp)`
- **Motivo**: registrar bloqueio estrutural real solicitado para evitar implementação fantasma.
- **Problema observável**: solicitação cita integração em `llama.cpp`, porém não existe `llama.cpp` no repositório.
- **Contrato esperado**: mudanças só em trilha reprodutível com fonte real versionada.
- **Risco de regressão**: alto se tentar “simular” integração inexistente (gera dívida técnica e falsa conformidade).
- **Critério de aceite**: bloqueio documentado com evidência de inventário (`rg --files | rg 'llama'` sem resultado).

## Prioridade P1 — ajustes de coerência documental adicional

### 4) Unidade: `docs/README.md` + eixo `Build e ambiente`
- **Motivo**: incluir índice explícito para este plano de refatoração e facilitar auditoria.
- **Problema observável**: ausência de referência ao novo plano operacional de build/release/CI.
- **Contrato esperado**: documentação de domínio apontar para o plano ativo de refatoração estrutural.
- **Risco de regressão**: baixo.
- **Critério de aceite**: link para `BUILD_REFACTOR_SCOPE.md` presente em `docs/README.md`.

---

## Execução especial solicitada (llama.cpp)
- **Status**: bloqueada por ausência de base fonte no repositório.
- **Evidência técnica**: não há arquivo `llama.cpp` no inventário atual do projeto.
- **Ação aplicada**: bloqueio formalizado neste plano para evitar alteração não reprodutível fora da árvore versionada.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
