<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# CANONICAL_BUILD_STATUS

> Fonte de verdade para status oficial de build/release. Em caso de divergência, este arquivo prevalece.

## Última validação oficial (UTC)

- **Data/hora:** 2026-04-03T22:29:21Z
- **Commit SHA validado:** `0acd029fff6cb05d928249bace5d9d9a9d0c558f`

## Comandos oficiais

### Obrigatório (sempre)

1. `./tools/gradle_with_jdk21.sh clean :app:assembleDebug --stacktrace`

### Sob gate (somente quando política de release + segredos permitirem)

1. `./tools/gradle_with_jdk21.sh :app:assembleRelease --stacktrace`
2. `./tools/gradle_with_jdk21.sh :app:assemblePerfRelease --stacktrace`

## Resultado final por tarefa

| Tarefa | Resultado final oficial | Observação |
|---|---|---|
| `:app:assembleDebug` | ✅ SUCCESS | Validado oficialmente no commit acima. |
| `:app:assembleRelease` | ⛔ GATED | Executar somente com gate de release e assinatura oficial ativa. |
| `:app:assemblePerfRelease` | ⛔ GATED | Executar somente com gate de perf/release e política interna habilitada. |

## Observações operacionais

- O histórico de validações ad-hoc deve apontar para este arquivo como status canônico.
- O fluxo Android canônico (`.github/workflows/android-ci.yml`), acionado por wrapper/orquestrador, publica snapshot deste status como artifact de CI.


## Validação pendente no commit corrente
- Este commit ainda não possui validação canônica de CI concluída no GitHub Actions.
- Manter como fonte de verdade a última validação oficial listada acima até o término da execução CI.
- Após CI verde, atualizar: SHA validado, data/hora UTC e tarefas executadas.
