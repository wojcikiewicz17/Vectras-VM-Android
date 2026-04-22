<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Native hardening backlog

## Componentes com dívida declarada

- `terminal-emulator/.../TerminalEmulator.java`
  - Revisar reverse wrap-around e ramos ambíguos de escape-state.
  - Testes alvo: cenários de wrap ANSI, transições de estado ESC.
- `androidVNC/{FullBufferBitmapData,RfbProto,VncCanvas}.java`
  - Eliminar APIs deprecated, corrigir fluxo de input/update e remover stubs.
  - Testes alvo: atualização incremental de framebuffer, eventos de toque/rato.
- `UIUtils.java`, `AppUpdater.java`, `Config.java`
  - Endurecer lifecycle legado e pontos de risco operacional documentados.
  - Testes alvo: atualização em background, persistência de configuração, ciclos de Activity.

## Cobertura (reativação incremental)

1. Reativar gradualmente testes fora de quarentena (prioridade JNI/storage).
2. Garantir regressão ABI/JNI em build `release` minificado.
3. Adicionar smoke tests para URI SAF/content:// em fluxos de importação.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.

## Atualização 2026-04-21 (núcleo low-level C/ASM adaptativo)

- Core de ABI freestanding (`abi_core_freestanding`) agora compila com `VECTRA_LL_NO_STDLIB_TYPES=1`, removendo dependência direta de `stdint.h/stddef.h` no contrato low-level.
- Tipos-base nativos centralizados em `app/src/main/cpp/vectra_ll_base.h` com modo freestanding e modo hosted.
- Seleção de backend adaptativo deixou de depender de `strcmp` (libc), usando comparação local `vectra_cstr_eq` em `lowlevel_bridge.c`.
- Limite estrutural mantido: camada JNI Android continua dependente de `jni.h` e runtime do SO; acesso direto a registradores/pinos físicos da GPU não é permitido sem trilha kernel/driver (fora do escopo app sem root).
