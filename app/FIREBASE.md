<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Backend de Telemetria e Falhas (BLP + compatibilidade release com Firebase)

## Política oficial

A política oficial do módulo `app/` é:

- **BLP (Bitstack Local Pipeline)** é o caminho padrão para desenvolvimento local.
- **Debug não depende de Firebase** e pode compilar/rodar sem `app/google-services.json`.
- **perfRelease e release exigem configuração Firebase real**; no CI, esses jobs só executam quando o segredo de Firebase está disponível.

Essa política evita ambiguidade: BLP é o padrão de desenvolvimento, mas o pipeline de release ainda protege compatibilidade de telemetria de produção.

## Matriz por variante

| Variante | Requisito de Firebase | Regra prática |
|---|---|---|
| `debug` | Opcional | Sem `google-services.json`, build local continua usando fallback sem Firebase. |
| `perfRelease` | Obrigatório | Falha sem JSON real. |
| `release` | Obrigatório | Falha sem JSON real. |

## Regras validadas no Gradle

A task `validateFirebaseReleaseConfig` em `app/build.gradle` aplica as seguintes regras para `perfRelease/release`:

1. Falha se `app/google-services.json` estiver ausente.
2. Falha se o JSON for inválido.
3. Falha se `project_info.project_id` estiver vazio.
4. Falha se `project_id` contiver `placeholder`.
5. A flag `-PALLOW_PLACEHOLDER_FIREBASE_FOR_RELEASE=true` existe apenas para validação interna controlada, não para release de produção.

## CI/CD (segredo para produção)

`app/google-services.json` de produção **não deve ser versionado**. Injete via segredo (ex.: base64) no pipeline:

```bash
echo "$GOOGLE_SERVICES_JSON_B64" | base64 --decode > app/google-services.json
```

Para pipelines de release/perfRelease, a recomendação oficial é sempre usar JSON real do projeto de produção.

## Comportamento no CI sem secret de Firebase

Quando `VECTRAS_GOOGLE_SERVICES_JSON_B64` não existe, o workflow Android **não gera placeholder para release**.
Nesse cenário, ele registra skip explícito e pula `validateFirebaseReleaseConfig` + build `release/perfRelease`.
O fluxo padrão (`debug`/local) continua independente de Firebase.
