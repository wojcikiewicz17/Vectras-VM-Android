# Cadeia de Arquivos da Raiz (Root File Chain)

Cada item abaixo segue reflexão técnica em três linhas: papel, ligação e comando de inspeção.

> Taxonomia de classificação (Ativo/Histórico/Arquivado) é mantida em `DOC_INDEX.md`; este arquivo foca no encadeamento técnico de inspeção para evitar duplicidade editorial.

## `.gitignore`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" .gitignore` (texto) ou `file .gitignore` (binário).

## `archive/root-history/1.md`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" archive/root-history/1.md` (texto) ou `file archive/root-history/1.md` (binário).

## `archive/root-history/ADVANCED_OPTIMIZATIONS.md`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" archive/root-history/ADVANCED_OPTIMIZATIONS.md` (texto) ou `file archive/root-history/ADVANCED_OPTIMIZATIONS.md` (binário).

## `archive/root-history/BENCHMARK_REFACTORING_SUMMARY.md`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" archive/root-history/BENCHMARK_REFACTORING_SUMMARY.md` (texto) ou `file archive/root-history/BENCHMARK_REFACTORING_SUMMARY.md` (binário).

## `CHANGELOG.md`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" CHANGELOG.md` (texto) ou `file CHANGELOG.md` (binário).

## `BUILDING.md`
- **Papel**: guia operacional de pré-requisitos e comandos de build no escopo raiz.
- **Liga com**: setup/CI em `README.md`, baseline de versões em `gradle.properties` e índice documental em `DOC_INDEX.md`.
- **Inspeção**: `sed -n "1,120p" BUILDING.md` (texto) ou `file BUILDING.md` (binário).

## `FIXES_SUMMARY.md`
- **Papel**: sumário técnico das correções aplicadas e impactos por subsistema.
- **Liga com**: estabilidade de versão (`VERSION_STABILITY.md`) e rastreabilidade de mudanças (`CHANGELOG.md`).
- **Inspeção**: `sed -n "1,120p" FIXES_SUMMARY.md` (texto) ou `file FIXES_SUMMARY.md` (binário).

## `CMakeLists.txt`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" CMakeLists.txt` (texto) ou `file CMakeLists.txt` (binário).

## `DOC_INDEX.md`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" DOC_INDEX.md` (texto) ou `file DOC_INDEX.md` (binário).

## `archive/root-history/IMPLEMENTATION_COMPLETE.md`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" archive/root-history/IMPLEMENTATION_COMPLETE.md` (texto) ou `file archive/root-history/IMPLEMENTATION_COMPLETE.md` (binário).

## `archive/root-history/IMPLEMENTATION_SUMMARY.md`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" archive/root-history/IMPLEMENTATION_SUMMARY.md` (texto) ou `file archive/root-history/IMPLEMENTATION_SUMMARY.md` (binário).

## `LICENSE`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" LICENSE` (texto) ou `file LICENSE` (binário).

## `Makefile`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" Makefile` (texto) ou `file Makefile` (binário).

## `PROJECT_STATE.md`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" PROJECT_STATE.md` (texto) ou `file PROJECT_STATE.md` (binário).

## `README.md`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" README.md` (texto) ou `file README.md` (binário).

## `RELEASE_NOTES.md`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" RELEASE_NOTES.md` (texto) ou `file RELEASE_NOTES.md` (binário).

## `THIRD_PARTY_NOTICES.md`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" THIRD_PARTY_NOTICES.md` (texto) ou `file THIRD_PARTY_NOTICES.md` (binário).

## `TROUBLESHOOTING.md`
- **Papel**: runbook de troubleshooting para erros frequentes de setup/build/bootstrap.
- **Liga com**: guia de build (`BUILDING.md`) e seção de governança no `README.md`.
- **Inspeção**: `sed -n "1,120p" TROUBLESHOOTING.md` (texto) ou `file TROUBLESHOOTING.md` (binário).

## `VERSION_STABILITY.md`
- **Papel**: manifesto/checklist de estabilidade metodológica por versão.
- **Liga com**: correções consolidadas (`FIXES_SUMMARY.md`) e estado atual (`PROJECT_STATE.md`).
- **Inspeção**: `sed -n "1,120p" VERSION_STABILITY.md` (texto) ou `file VERSION_STABILITY.md` (binário).

## `VECTRAS_MEGAPROMPT_DOCS.md`
- **Papel**: guia macro de governança documental e direcionadores editoriais.
- **Liga com**: índice canônico (`DOC_INDEX.md`) e documentação central (`docs/README.md`).
- **Inspeção**: `sed -n "1,120p" VECTRAS_MEGAPROMPT_DOCS.md` (texto) ou `file VECTRAS_MEGAPROMPT_DOCS.md` (binário).

## `VECTRA_CORE.md`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" VECTRA_CORE.md` (texto) ou `file VECTRA_CORE.md` (binário).

## `build.gradle`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" build.gradle` (texto) ou `file build.gradle` (binário).

## `gradle.properties`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" gradle.properties` (texto) ou `file gradle.properties` (binário).

## `gradlew`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" gradlew` (texto) ou `file gradlew` (binário).

## `gradlew.bat`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" gradlew.bat` (texto) ou `file gradlew.bat` (binário).

## `settings.gradle`
- **Papel**: arquivo de controle/documentação/build no escopo raiz do repositório.
- **Liga com**: documentação global (`README.md`, `DOC_INDEX.md`) e módulos declarados em `settings.gradle`/`build.gradle` quando aplicável.
- **Inspeção**: `sed -n "1,120p" settings.gradle` (texto) ou `file settings.gradle` (binário).

