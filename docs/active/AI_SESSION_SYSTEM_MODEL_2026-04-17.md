<!-- DOC_ORG_SCAN: 2026-04-17 | source-scan: ai-assisted-structural-review -->

# AI Session — Modelo Sistêmico de Compreensão e Refatoração

## Resumo
Documento operacional para sessões de IA orientadas a engenharia de build/release, CI e integração nativa. O objetivo é forçar abordagem de causa-raiz, preservar cadeia oficial de release e manter documentação alinhada ao estado real do código.

## Escopo
**Cobre:** análise sistêmica de Gradle, workflows, CMake/NDK/JNI, geração de artefatos e upload em CI.

**Não cobre:** substituição de políticas de assinatura oficial, bypass de validações de segurança ou promoção de builds não conformes para release.

## Contrato de execução sistêmica
A sessão deve respeitar a ordem abaixo:
1. **Bootstrap de toolchain** (JDK/SDK/NDK/CMake).
2. **Configuração do build** (Gradle + variáveis + módulos).
3. **Compilação** (Java/Kotlin + nativo quando aplicável).
4. **Validação** (scripts de consistência/regras de diretório/ABI/artefato).
5. **Empacotamento e publicação de artefatos** (sem quebrar trilha de release oficial).
6. **Atualização documental mínima necessária** para refletir o estado executável.

## Matriz de causa-raiz por domínio
| Domínio | Sintoma comum | Causa-raiz típica | Verificação objetiva |
|---|---|---|---|
| Gradle/módulos | módulo não resolve | include/path divergente entre `settings.gradle` e disco | `python3 tools/verify_repo_file_dependencies.py` |
| CI/workflows | job quebra antes do build | contrato de diretórios/scripts desatualizado | `./tools/ci/validate_pipeline_directories.sh` |
| Docs de rastreio | documentação "verde" mas CI falha | metadata/commit sem vínculo com estado real | `./tools/check_docs_reference_commit.sh` |
| Artefatos | build roda sem saída válida | pipeline não valida outputs esperados | `./tools/ci/validate_expected_android_artifacts.sh` |

## Checklist de fechamento de sessão
- [ ] Build local verificável (ou falha classificada com causa concreta).
- [ ] CI coerente com scripts e caminhos existentes.
- [ ] Validações principais executadas com resultado registrado.
- [ ] Contrato de artefatos revisado.
- [ ] Documentação mínima sincronizada com estado atual.

## Evidências executadas nesta revisão (2026-04-17)
```bash
./gradlew -v
./tools/check_docs_reference_commit.sh
python3 tools/verify_repo_file_dependencies.py
./tools/ci/validate_pipeline_directories.sh
```

## Metadados
- Versão do documento: 1.0
- Última atualização: 2026-04-17
- Commit de referência: `HEAD`

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
