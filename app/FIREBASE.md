# Backend de Telemetria e Falhas (substituto do Firebase)

## Decisão arquitetural

O módulo `app/` **não usa mais Firebase** (Analytics, Crashlytics, Messaging).
A estratégia oficial passa a ser um pipeline autoral local, chamado **Bitstack Local Pipeline (BLP)**,
com coleta determinística no dispositivo e exportação controlada via artefatos locais.

## Objetivo do BLP

- Evitar acoplamento com serviços externos para build/execução.
- Permitir operação offline e previsível.
- Manter trilha técnica para diagnóstico usando componentes já existentes no projeto.

## Como funciona

1. A aplicação registra eventos operacionais e falhas em trilhas locais.
2. Os registros são mantidos em buffer/ledger local com política de rotação.
3. Em fluxo de suporte, os artefatos podem ser exportados manualmente para análise.

> Referências de arquitetura operacional e auditoria: `docs/ARCHITECTURE.md`.

## Impacto no build

- **Não é necessário** `app/google-services.json` para compilar.
- Não é necessário plugin `com.google.gms.google-services` no `app/build.gradle`.
- O CI não depende de segredo Firebase para `assembleDebug`/`assembleRelease`.

## Migração (Firebase → BLP)

- Remover procedimentos de provisionamento Firebase dos guias de onboarding.
- Manter apenas fluxo de build Android/Gradle padrão.
- Centralizar rastreabilidade em documentação de arquitetura e operação local.

## Checklist rápido

- [x] Sem requisito de `google-services.json` no fluxo padrão.
- [x] Sem regras ProGuard específicas de Firebase no módulo `app/`.
- [x] Onboarding alinhado ao pipeline local autoral.
