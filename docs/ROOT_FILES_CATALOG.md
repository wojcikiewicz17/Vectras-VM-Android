<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Catálogo de Arquivos Raiz (Root) — Vectras-VM-Android

Este catálogo documenta os **arquivos soltos atualmente presentes no diretório raiz** do repositório. O objetivo é apoiar rastreabilidade, onboarding e auditoria documental.

---

## Escopo deste documento

- **Inclui** apenas arquivos com caminho `./<nome-do-arquivo>` (sem subdiretórios).
- **Não inclui** arquivos de `archive/`, `docs/` ou qualquer outro subdiretório no inventário principal de “arquivos raiz”.

---

## Arquivos reais na raiz (inventário atual)

## 1) `README.md`
- Documento principal de apresentação do projeto e navegação inicial.

## 2) `DOC_INDEX.md`
- Índice canônico da documentação e governança documental da raiz.

## 3) `PROJECT_STATE.md`
- Estado consolidado do projeto e marcos recentes.

## 4) `CHANGELOG.md`
- Histórico de mudanças por versão/período.

## 5) `RELEASE_NOTES.md`
- Notas de release e highlights por entrega.

## 6) `BUILDING.md`
- Guia operacional de pré-requisitos e comandos de build.

## 7) `TROUBLESHOOTING.md`
- Guia de diagnóstico para problemas recorrentes de setup/build/bootstrap.

## 8) `FIXES_SUMMARY.md`
- Sumário de correções aplicadas e impactos técnicos.

## 9) `VERSION_STABILITY.md`
- Manifesto/checklist de estabilidade metodológica da versão.

## 10) `VECTRA_CORE.md`
- Documento técnico do Vectra Core MVP.

## 11) `VECTRAS_MEGAPROMPT_DOCS.md`
- Documento de diretrizes/prompt documental do projeto.

## 12) `THIRD_PARTY_NOTICES.md`
- Avisos e atribuições de componentes de terceiros.

## 13) `LICENSE`
- Licença GPL v2.0 e termos de atribuição.

## 14) `.gitignore`
- Regras de exclusão de artefatos e arquivos sensíveis.

## 15) `build.gradle`
- Configuração Gradle de nível raiz (plugins, versões e propriedades globais).

## 16) `settings.gradle`
- Declara módulos incluídos no build.

## 17) `gradle.properties`
- Propriedades globais do Gradle/Android.

## 18) `gradlew`
- Wrapper Gradle para Unix-like.

## 19) `gradlew.bat`
- Wrapper Gradle para Windows.

## 20) `CMakeLists.txt`
- Configuração de build C/C++ (CMake) na raiz.

## 21) `Makefile`
- Alvos de automação/build auxiliares na raiz.

## 22) `local.properties.example`
- Exemplo de propriedades locais para configuração de ambiente.

## Itens históricos (fora da lista de arquivos raiz)

Os itens abaixo **não são arquivos soltos da raiz**; permanecem aqui apenas como referência histórica:

- `archive/root-history/ADVANCED_OPTIMIZATIONS.md`
- `archive/root-history/BENCHMARK_REFACTORING_SUMMARY.md`
- `archive/root-history/IMPLEMENTATION_SUMMARY.md`
- `archive/root-history/IMPLEMENTATION_COMPLETE.md`

### Nota histórica sobre assinatura
`vectras.jks` **não está presente na raiz atual**. Quando citado, deve ser tratado apenas como contexto histórico de assinatura, fora do inventário de arquivos raiz.

---

## Relação Consolidada (estado atual, não automática)

A relação abaixo descreve o encadeamento funcional de alto nível **com base em revisão manual periódica** (não é descoberta automática):

1. **Base institucional**: `README.md` e `LICENSE`.
2. **Build e configuração**: `build.gradle`, `settings.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, `CMakeLists.txt`, `Makefile`, `local.properties.example`.
3. **Governança documental**: `DOC_INDEX.md`, `PROJECT_STATE.md`, `CHANGELOG.md`, `RELEASE_NOTES.md`, `BUILDING.md`, `TROUBLESHOOTING.md`, `FIXES_SUMMARY.md`, `VERSION_STABILITY.md`, `THIRD_PARTY_NOTICES.md`, `VECTRAS_MEGAPROMPT_DOCS.md`, `VECTRA_CORE.md`.
4. **Integrações**: regras operacionais em `.gitignore` e cadeia documental de build local.

> Importante: este catálogo é um artefato editorial; pode ficar defasado se a raiz mudar sem atualização correspondente.

---

## Regra de manutenção recomendada

Após mudanças estruturais no repositório, executar auditoria para comparar arquivos reais da raiz com o inventário documentado.

Exemplo de comando de auditoria rápida:

```bash
find . -maxdepth 1 -type f -printf '%P\n' | sort
```

Se houver divergência, atualizar este catálogo na mesma PR/commit da alteração estrutural.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
