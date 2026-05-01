# VM Execution Flow (UI → StartVM → JNI → rmr_* → QEMU final args)

Este documento é a **fonte primária** do fluxo operacional de execução de VM no app Android.

## Fluxo fechado (alto nível)

1. **UI dispara a inicialização** via telas/ações como `VmsHomeAdapter` (home) e `PendingCommand` (pendente), que chamam `StartVM.env(...)` para montar a linha de comando QEMU.
2. **Orquestração de start** em `MainStartVM.startNow(...)`: valida preflight, estado do fluxo VM, modo de launch (headless/UI) e persistência de contexto de execução.
3. **Builders/Resolvers** definem binário, perfil e aceleração:
   - `QemuExecConfig.resolveBinary(...)`
   - `QemuBinaryResolver.resolveForArch(...)`
   - `QemuArgsBuilder.resolveProfile/applyProfile/applyAcceleration(...)`
4. **JNI hotpath/bridges** são disponibilizados por `NativeFastPath` (Java), com ponte para `vectra_core_accel.c`.
5. **Bridge C para kernel rmr_*** usa `rmr_jni_kernel_*` (em `rmr_unified_jni_bridge.c`) e delega para `RmR_UnifiedKernel_*` (em `rmr_unified_kernel.c`).
6. **StartVM fecha os argumentos finais** com UI target (`VNC`/`SPICE`/`X11`/`none`) e retorna string final via `buildCommand(...)`.

---

## Sequência detalhada

### A) Entrada de UI e start
- `MainStartVM.startNow(...)` recebe `env` (comando montado), resolve modo de execução, roda preflight e marca estado em tracker/ledger antes de executar.
- `VmsHomeAdapter` e `PendingCommand` alimentam esse fluxo ao invocar `StartVM.env(...)`.

### B) Construção de comando QEMU (`StartVM.env`)
- Resolve arquitetura/binário (`QemuExecConfig` + `QemuBinaryResolver`).
- Monta discos/ISO, BIOS/UEFI, memória, boot, rede e periféricos.
- Aplica tuning de perfil (`VmProfile`) e aceleração (`KVM`/`TCG`) por `QemuArgsBuilder`.
- Injeta parâmetros de UI (`-vnc`, `-spice`, `-display`).
- Fecha string com `buildCommand(...)`.

### C) Camada nativa (NativeFastPath/JNI)
- `NativeFastPath` tenta `System.loadLibrary("vectra_core_accel")` + `nativeInit()`.
- Se falhar, ativa fallback Java (`isFallbackActive()`), preservando contrato funcional.
- Em sucesso, chamadas como `nativeIngest/nativeProcessRoute/nativeVerify/nativeAudit` usam JNI C.

### D) Bridge JNI C e kernel rmr_*
- `vectra_core_accel.c` valida estado, sincroniza mutex e chama `rmr_jni_kernel_*`.
- `rmr_unified_jni_bridge.c` converte contratos JNI e despacha para `RmR_UnifiedKernel_*`.
- `rmr_unified_kernel.c` executa ingest/process/route/verify/audit e operações auxiliares.

### E) Resultado operacional
- O comando QEMU final é devolvido por `StartVM.env(...)`.
- O runtime usa telemetria/ledger para observabilidade de profile, KVM e fallback.

---

## Tabela de mapeamento (arquivo/classe/função)

| Arquivo / classe / função | Responsabilidade | Entradas → Saídas | Falhas e fallback |
|---|---|---|---|
| `app/src/main/java/com/vectras/vm/main/vms/VmsHomeAdapter.java` → chamada `StartVM.env(...)` | Gatilho de launch vindo da UI de VMs | Estado da VM/extra/path → comando base | Se dados inválidos, cadeia superior bloqueia no preflight em `MainStartVM` |
| `app/src/main/java/com/vectras/vm/main/core/PendingCommand.java` → chamada `StartVM.env(...)` | Reidratar launch pendente | comando pendente → comando QEMU final | Fluxo pendente aborta se contexto inválido/ausente |
| `app/src/main/java/com/vectras/vm/main/core/MainStartVM.java` → `startNow(...)` | Orquestra start end-to-end e valida preflight | contexto + vm metadata + env → processo de launch/abort | Em preflight erro: ledger + diálogo + abort (`VmFlowState.ERROR`) |
| `app/src/main/java/com/vectras/vm/StartVM.java` → `env(...)` | Montagem completa dos args QEMU | `activity`, `extras`, `img`, `isQuickRun` → string comando final | UI desconhecida cai para `-display none`; parâmetros faltantes usam defaults |
| `app/src/main/java/com/vectras/vm/StartVM.java` → `buildCommand(...)` | Normalização final da linha de comando | lista de tokens → string final | Ignora tokens `null`/vazios |
| `app/src/main/java/com/vectras/vm/qemu/QemuExecConfig.java` → `resolveBinary(...)` | Resolver binário via config + fallback runtime | `activity`, `arch` → path/binário | JSON inválido, binário ausente ou sem permissão executável: fallback para resolver padrão |
| `app/src/main/java/com/vectras/vm/qemu/QemuBinaryResolver.java` → `resolveForArch(...)` | Descobrir executável por arquitetura e diretórios candidatos | `filesDir`, `arch` → `Resolution(found/fullPath/reason)` | Não encontrado: retorna `notFound` e fallback para binário primário por arch |
| `app/src/main/java/com/vectras/vm/qemu/QemuArgsBuilder.java` → `resolveProfile(...)` | Seleção de perfil (`LOW_LATENCY`, `THROUGHPUT`, etc.) | `activity`, `extras` → `VmProfile` | Se sem correspondência: `BALANCED` |
| `app/src/main/java/com/vectras/vm/qemu/QemuArgsBuilder.java` → `applyProfile(...)` | Aplicar CPU/boot flags por budget | `params`, `profile`, `arch`, `cores` → args `-cpu/-smp/...` | Sem CPUs válidas: normaliza para mínimo 1 |
| `app/src/main/java/com/vectras/vm/qemu/QemuArgsBuilder.java` → `applyAcceleration(...)` | Decidir `kvm` vs `tcg` | `params`, `extras` → args `-accel` + marcador `-name` | Sem KVM: usa `tcg,thread=multi`; se extras já definem aceleração, respeita externo |
| `app/src/main/java/com/vectras/vm/core/NativeFastPath.java` → init estático + `nativeInit()` | Expor fastpath JNI e fallback Java | load da lib + init → `NATIVE_AVAILABLE`/telemetria | Falha de `System.loadLibrary`/init: fallback Java ativo |
| `app/src/main/cpp/vectra_core_accel.c` → `Java_com_vectras_vm_core_NativeFastPath_*` | Bridge JNI C de alto desempenho | arrays/escalares Java → chamadas `rmr_jni_*` / `RmR_UnifiedKernel_*` | Estado inválido/args inválidos retornam códigos `RMR_KERNEL_ERR_*` ou `NULL` |
| `engine/rmr/src/rmr_unified_jni_bridge.c` → `rmr_jni_kernel_*` | Adaptar contratos JNI para kernel unificado | structs JNI route/ingest/etc. → chamadas `RmR_UnifiedKernel_*` | Propaga erros de kernel (`RMR_UK_*`) |
| `engine/rmr/src/rmr_unified_kernel.c` → `RmR_UnifiedKernel_Ingest/Process/RouteEx/Verify/Audit` | Núcleo determinístico rmr_* | dados + estado kernel → métricas, decisão de rota, verificação e auditoria | Erros de estado/argumento/capabilidade retornam códigos de erro de kernel |

---

## Observações de operação

- O fluxo JNI é **opcional**: se indisponível, o sistema mantém compatibilidade via fallback Java.
- A aceleração (`KVM`) é oportunista e auditável (`lastKvmEnabled`, `lastKvmReason`).
- A string final do QEMU sempre passa por fechamento em `StartVM.buildCommand(...)`, evitando tokens nulos/vazios.
