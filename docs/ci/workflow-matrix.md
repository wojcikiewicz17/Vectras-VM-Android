<!-- DOC_ORG_SCAN: 2026-04-30 | source-scan: workflow-classification-refresh -->

# CI workflow matrix (canonical + classification)

## Classificação oficial

### 1) Canônico obrigatório

| Workflow | Quando roda | Papel |
|---|---|---|
| `.github/workflows/pipeline-orchestrator.yml` | `push`, `pull_request`, `workflow_dispatch` | Orquestra perfil (`host_only`/`android_only`/`full`) e chama trilhas canônicas. |
| `.github/workflows/host-ci.yml` | direto por evento e/ou `workflow_call` | Pipeline host canônica (build, contratos e evidências host). |
| `.github/workflows/android-ci.yml` | `workflow_call` | Pipeline Android canônica parametrizada (Gradle/NDK/CMake/JNI/testes/artefatos). |
| `.github/workflows/quality-gates.yml` | `workflow_call` | Gate final consolidando resultado host + android por perfil. |

### 2) Wrapper permitido

| Workflow | Quando roda | Papel |
|---|---|---|
| `.github/workflows/android.yml` | `push`, `pull_request`, `workflow_dispatch` | Wrapper de entrada Android; delega para `android-ci.yml` (sem redefinir política oficial). |
| `.github/workflows/ci.yml` | múltiplos eventos | Wrapper legado/compatível para encaminhar trilha host canônica. |

### 3) Auxiliar técnico

| Workflow | Quando roda | Papel |
|---|---|---|
| `.github/workflows/android-native-ci.yml` | evento direto e/ou `workflow_call` | Matriz nativa Android (debug/release por perfil ABI) para cobertura técnica complementar. |
| `.github/workflows/compile-matrix.yml` | `workflow_call` | Matriz auxiliar de compatibilidade ABI/variant para regressão. |

### 4) Legado/arquivado

- Não há workflow removido automaticamente nesta atualização.
- Wrappers legados permanecem classificados e não devem substituir trilhas canônicas.

---

## Como os workflows são usados na prática

1. **Entrada principal recomendada:** `pipeline-orchestrator.yml`.
2. Orquestrador resolve perfil e dispara:
   - `host-ci`;
   - `android-native-ci` (oficial e arm32+arm64);
   - `android-ci` para release binaries arm32+arm64 (signed/unsigned conforme lane);
   - `quality-gates` no final.
3. Workflows wrapper (`android.yml`, `ci.yml`) são permitidos para compatibilidade, sem virar fonte de verdade de política.

## Política ABI resumida

- **Oficial:** `official_arm64`.
- **Validação interna controlada:** `official_arm32_arm64`, `internal_arm32_arm64` e matrizes expandidas conforme lane/profile.
- **NEON:** existe sinalização de build e inclusão condicional de fontes por ABI ARM; classificação de implementação deve sempre ser comprovada por execução/teste, não só por flag.
