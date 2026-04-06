# CANONICAL_BUILD_STATUS

> Fonte de verdade para status de build oficial. Substitui relatórios ad-hoc quando houver divergência.

## Última validação oficial (UTC)

- **Data/hora:** 2026-04-03T22:29:21Z
- **Commit SHA validado:** `0acd029fff6cb05d928249bace5d9d9a9d0c558f`

## Comandos oficiais

### Obrigatório (sempre)

1. `./tools/gradle_with_jdk21.sh clean :app:assembleDebug --stacktrace`

### Sob gate (somente quando política/segredos permitirem)

1. `./tools/gradle_with_jdk21.sh :app:assembleRelease --stacktrace`
2. `./tools/gradle_with_jdk21.sh :app:assemblePerfRelease --stacktrace`
3. `./tools/gradle_with_jdk21.sh :app:verifyDeliveredCompiledArtifacts --stacktrace`

## Resultado final por tarefa

| Tarefa | Resultado | Evidência |
|---|---|---|
| `:app:assembleDebug` | ✅ SUCCESS | `reports/build-verification-2026-04-03-fix.md` |
| `:app:assembleRelease` | ⛔ GATED (não executado na validação oficial) | Exige gate de release + segredos |
| `:app:assemblePerfRelease` | ⛔ GATED (não executado na validação oficial) | Exige gate de release/perf |
| `:app:verifyDeliveredCompiledArtifacts` | ⛔ GATED (não executado na validação oficial) | Executar após release/perfRelease quando gate ativo |

## Observações operacionais

- Este arquivo é o único status canônico para decisões de build/release.
- CI deve publicar snapshot deste status como artifact em toda execução do workflow Android.
