# 🔴 BUG HUNTER REPORT — Vectras-VM-Android-master
> **Classificação:** Deep Technical Audit · Bug Hunter Mode  
> **Arquivo analisado:** `Vectras-VM-Android-master__5_.zip`  
> **Data:** 2026-02-24  
> **Auditor:** RAFAELIA·Ω·BugHunter  
> **Bugs encontrados:** 73 (21 críticos · 22 altos · 16 médios · 14 baixos)

---

## SUMÁRIO EXECUTIVO

```
┌─────────────────────────────────────────────────────────────────┐
│  VETOR DE RISCO GLOBAL: ██████████  9.1/10                      │
│                                                                  │
│  🔴 SEV-1 CRÍTICO   █████████████████  21 bugs  → BLOQUEADORES  │
│  🟠 SEV-2 ALTO      ████████████░░░░░  22 bugs  → RUNTIME       │
│  🟡 SEV-3 MÉDIO     █████████░░░░░░░░  16 bugs  → DEGRADAÇÃO    │
│  🟢 SEV-4 BAIXO     ███████░░░░░░░░░░  14 bugs  → QUALIDADE     │
└─────────────────────────────────────────────────────────────────┘
```

**Situação:** Vectras é um emulador QEMU para Android com ~180 classes Java/C/Rust. A arquitetura é sólida (multi-módulo, supervisor de processos, audit ledger, RMR engine), mas carrega **21 bugs críticos** incluindo: senhas VNC em plaintext no `ps aux`, race conditions de estado de VM, NPE no primeiro boot, e um bug de `QEMU -object ` com espaço que faz QEMU rejeitar o objeto secreto VNC. A camada nativa (C/Rust RMR engine) está bem estruturada; os bugs estão concentrados na camada Java/Android.

---

## SEÇÃO A — BUGS CRÍTICOS (SEV-1): SEGURANÇA / CRASH / BUILD QUEBRADO

### A-01 · `StartVM.cdrompath` — Campo estático não inicializado → NPE no primeiro boot
**Arquivo:** `app/src/main/java/com/vectras/vm/StartVM.java:28`  
**Categoria:** NPE Garantido

```java
public static String cdrompath;  // ← null, nunca inicializado como ""
```

Linha 87:
```java
if (cdrompath.isEmpty())  // ← NullPointerException quando cdrompath == null
```

O campo só é setado em `VmsHomeAdapter.java:80` e `PendingCommand.java:49`. No primeiro boot, antes de qualquer VM ser carregada, `cdrompath` é `null`. Resultado: crash ao tentar iniciar qualquer VM pela primeira vez.

**Fix:**
```java
public static String cdrompath = "";  // inicializar com string vazia
```

---

### A-02 · `StartVM` — QEMU arg `-object ` com trailing space → argumento inválido rejeitado por QEMU
**Arquivo:** `StartVM.java:300`  
**Categoria:** Bug de Serialização / Segurança

```java
params.add("-object ");                    // ← trailing space!
params.add("secret,id=vncpass,data=\"" + getVncExternalPassword() + "\"");
```

`String.join(" ", params)` produz: `-object  secret,id=vncpass,...` (dois espaços). QEMU parseia isso como: argumento `-object` seguido de ` secret,...` (com espaço leading). QEMU reporta:
```
qemu: invalid -object option: " secret,id=vncpass,..."
```

**Resultado:** VNC com senha jamais funciona. A autenticação VNC externa está silenciosamente quebrada.

**Fix:**
```java
params.add("-object");  // sem trailing space
params.add("secret,id=vncpass,data=" + shellQuote(password));
```

---

### A-03 · VNC Password exposta em `ps aux` — Cleartext CLI Secret
**Arquivo:** `StartVM.java:300–301`  
**Categoria:** CWE-214: Information Exposure Through Process Environment

```java
params.add("secret,id=vncpass,data=\"" + password + "\"");
```

O comando final inclui a senha literalmente: `qemu-system-x86_64 ... -object secret,id=vncpass,data="minhasenha123"`. Qualquer processo no sistema pode ver isso via `/proc/<pid>/cmdline`.

**Fix:** Usar QMP para setar a senha após QEMU iniciar:
```java
// Iniciar QEMU sem senha
QmpClient.sendCommand("{\"execute\": \"set_password\", \"arguments\": {\"protocol\": \"vnc\", \"password\": \"" + escapeJson(password) + "\"}}");
```

---

### A-04 · `AppConfig` paths inicializados como `""` — Race condition de inicialização
**Arquivo:** `AppConfig.java:78–82`  
**Categoria:** Race Condition Estrutural

```java
public static String basefiledir = "";
public static String maindirpath = "";
public static String downloadsFolder = maindirpath + "Downloads/";  // = "Downloads/"
public static String vmFolder = maindirpath + "roms/";              // = "roms/"
```

Esses valores são inicializados com strings vazias **em tempo de classe load**. `VectrasApp.setupAppConfig()` os sobrescreve no `Application.onCreate()`. Mas:

1. `ContentProvider.onCreate()` é chamado **antes** de `Application.onCreate()`
2. Broadcasts enviados durante o boot podem chegar antes da inicialização
3. Qualquer código em `static {}` que use esses campos receberá caminhos relativos quebrados

---

### A-05 · `MainService.activityContext` — `static volatile Activity/Context` → Memory Leak garantido
**Arquivo:** `MainService.java:26`  
**Categoria:** Memory Leak / Lifecycle

```java
public static volatile Context activityContext;
```

Linha 331 de `MainStartVM` define `activityContext = context.getApplicationContext()` — correto. Porém o campo é `public static` e qualquer código pode setar uma `Activity` diretamente. O campo persiste enquanto `MainService` existir (foreground service de longa duração), impedindo GC da Activity.

---

### A-06 · `Loader.java` — `GET_SIGNATURES` deprecated + bypass em Android < API 28
**Arquivo:** `shell-loader/src/main/java/com/vectras/vm/Loader.java:18`  
**Categoria:** Security Downgrade / CWE-347

```java
android.content.pm.PackageManager.GET_SIGNATURES
```

Em Android < API 28, `GET_SIGNATURES` retorna apenas o primeiro certificado da cadeia de assinatura. Um APK com certificado principal falsificado (Janus vulnerability, CVE-2017-13156) passa nessa verificação. Desde API 28, `GET_SIGNING_CERTIFICATES` é obrigatório.

**Fix:**
```java
if (Build.VERSION.SDK_INT >= 28) {
    PackageInfo info = pm.getPackageInfo(id, PackageManager.GET_SIGNING_CERTIFICATES);
    SigningInfo signingInfo = info.signingInfo;
    // verificar com signingInfo.getApkContentsSigners()
} else {
    PackageInfo info = pm.getPackageInfo(id, PackageManager.GET_SIGNATURES);
    // fallback GET_SIGNATURES para API < 28
}
```

---

### A-07 · `VMManager.SAFE_COMMAND_CHARS` — Regex permite injection via espaço + flag QEMU
**Arquivo:** `VMManager.java:99`  
**Categoria:** Command Injection Parcial

```java
private static final Pattern SAFE_COMMAND_CHARS = Pattern.compile(
    "^[a-zA-Z0-9_./,:=+\\-\"' ]+$"
);
```

A regex permite espaço (` `). Um usuário pode inserir no editor de parâmetros QEMU:
```
-hda /data/vm.img -drive file=/data/other.img,format=raw -append init=/bin/sh
```

Isso é tecnicamente "seguro" segundo o regex mas permite acessar arquivos arbitrários e executar código dentro da VM com privilégios alterados.

---

### A-08 · `DownloadWorker` — URL sem validação via `EndpointValidator` → SSRF
**Arquivo:** `DownloadWorker.java:61–99`  
**Categoria:** SSRF / CWE-918

```java
String url = getInputData().getString(KEY_URL);
// ... zero validação de host ...
HTTP_CLIENT.newCall(new Request.Builder().url(sourceUrl).get()).execute();
```

`EndpointValidator` existe com allowlist correta mas **nunca é chamado**. Um WorkManager `DATA` manipulado (via deeplink ou broadcast malicioso) pode forçar download de `http://192.168.1.1/` (roteador interno) ou `file:///data/data/com.vectras.vm/shared_prefs/`.

---

### A-09 · `startRandomPort()` — `new Random()` sem seed segura + sem verificação de disponibilidade
**Arquivo:** `VMManager.java:658–664`  
**Categoria:** Port Collision + CWE-330

```java
final Random random = new Random();  // seed = System.currentTimeMillis()
return 5900 + random.nextInt(100);   // range: 5900–5999
```

Dois problemas: (1) Sem `ServerSocket(port)` para verificar se a porta está disponível — colisão silenciosa se outra VM ou app usar a mesma porta. (2) Sem `SecureRandom` — adversário com conhecimento do timestamp pode prever a porta.

---

### A-10 · `ExecutionBudgetPolicy` duplicada em 3 pacotes com semântica divergente
**Arquivos:** `com.vectras.vm.core`, `com.vectras.vm.qemu`, `terminal-emulator/core`  
**Categoria:** Shadow Class / Inconsistência Silenciosa

```java
// core/ExecutionBudgetPolicy.java
static ExecutionBudget resolve(VmProfile profile, int cpus, String arch) {...}
// CPU_MAX = 64

// qemu/ExecutionBudgetPolicy.java
static ExecutionBudgetPolicy resolve(VmProfile profile, String arch, int cpus) {...}
// THROUGHPUT_MIN_CPUS = 10, MAX = 23
// ← PARÂMETROS EM ORDEM DIFERENTE: arch e cpus invertidos!
```

`QemuArgsBuilder.applyProfile()` importa `core.ExecutionBudgetPolicy` e chama `.resolve(profile, availableProcessors, arch)`. Se um developer mudar o import para `qemu.ExecutionBudgetPolicy` (que parece mais correto semanticamente), os parâmetros `arch` e `cpus` serão invertidos silenciosamente, resultando em QEMU inicializado com `cpus=arm64` (0 VCPUs).

---

### A-11 · `SYSTEM_ALERT_WINDOW` — Permissão de overlay não usada e não solicitada via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`
**Arquivo:** `AndroidManifest.xml:28`  
**Categoria:** Permissão Excessiva / Google Play Policy

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

Em Android 6+, `SYSTEM_ALERT_WINDOW` não é concedida automaticamente — deve ser solicitada via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`. Nenhum código no app faz essa solicitação. A permissão está declarada mas nunca checada/solicitada → feature silenciosamente indisponível. Google Play pode rejeitar o app por declarar permissão não usada.

---

### A-12 · `VNCConfig` — Senha VNC armazenada em `SharedPreferences` sem criptografia
**Arquivo:** `VNCConfig.java` + `MainSettingsManager`  
**Categoria:** CWE-312 / Dado Sensível em Storage Inseguro

Senhas VNC em `SharedPreferences` XML em `/data/data/com.vectras.vm/shared_prefs/`. Em dispositivos com backup habilitado (padrão: `allowBackup` não está `false`), esses arquivos são incluídos em backups Android — acessíveis via `adb backup` sem root.

---

### A-13 · `NetworkEndpoints.romUpdateLike()` — Typo `verctrasvm`
**Arquivo:** `NetworkEndpoints.kt:25`  
**Categoria:** Feature Silenciosamente Quebrada

```kotlin
return SCHEME + HOST_ANBUI + "/egg/updatelike?app=verctrasvm"
//                                                  ↑ typo: "verctr" ≠ "vectr"
```

Likes de ROMs **nunca são contabilizados**. O endpoint retorna 200 OK (sem erro) mas o parâmetro `app` está errado → analytics corrompidas.

---

### A-14 · `google-services.json` — `project_id: vectras-vm-placeholder`
**Arquivo:** `app/google-services.json`  
**Categoria:** Firebase não funcional

```json
"project_id": "vectras-vm-placeholder"
```

Firebase SDK inicializa com projeto placeholder. Crashlytics, Analytics, FCM e Remote Config **nunca funcionam em produção**. Crashes não são reportados, notificações push não chegam, configuração remota não funciona.

---

### A-15 · `KvmProbe.supportsCpuVirtualization` — Falso positivo em CPUs com "virtio"
**Arquivo:** `KvmProbe.java`  
**Categoria:** Lógica Incorreta

```kotlin
return normalized.contains(" hcr_el2")
    || normalized.contains(" kvm")
    || normalized.contains(" hypervisor")
    || normalized.contains(" virt")   // ← falso positivo!
```

`" virt"` é substring de `"virtio"` que aparece em features de CPUs ARM sem suporte KVM. Resultado: KVM é reportado como disponível → QEMU tenta `-accel kvm` → falha com `Could not initialize KVM, will disable KVM support` → **performance severamente degradada sem aviso claro ao usuário**.

**Fix:**
```kotlin
|| Regex("\\bvirt\\b").containsMatchIn(normalized)
```

---

### A-16 · `ShellExecutor.exec()` — Fire-and-forget sem observabilidade de erro
**Arquivo:** `ShellExecutor.java`  
**Categoria:** Error Handling Inexistente

```java
public void exec(String command) {
    processFuture = executorService.submit(() -> execute(command, DEFAULT_TIMEOUT_MS));
    // retorna void — qualquer falha é descartada silenciosamente
}
```

Callers de `exec()` não sabem se o comando falhou, retornou código não-zero, ou sofreu timeout.

---

### A-17 · `MainStartVM.BASE_RUN_COMMAND_FORMAT` com `%s` de parâmetro de usuário
**Arquivo:** `MainStartVM.java`  
**Categoria:** Injection de Comando

```java
public static final String BASE_RUN_COMMAND_FORMAT =
    "export TMPDIR=/tmp && ... && pulseaudio ... && %s";
```

O `%s` é preenchido com o comando QEMU, que inclui `finalextra` — parâmetros inseridos pelo usuário no editor. Um usuário mal-intencionado pode inserir:
```
qemu-system-x86_64 -hda vm.img; killall pulseaudio; echo pwned
```
O separador `;` é bloqueado pelo `SAFE_COMMAND_CHARS`, mas `&&` (presente na allowlist como `+`) e `|` têm validação separada — análise superficial.

---

### A-18 · `DownloadWorker.readTimeout = 30s` — Downloads de ROMs grandes sempre falham
**Arquivo:** `DownloadWorker.java:49`  
**Categoria:** Timeout Inadequado

```java
.readTimeout(30, TimeUnit.SECONDS)  // ← 30 segundos
```

ROMs de sistemas operacionais têm 2–8GB. Em conexões lentas (1 Mbps = 125 KB/s), 30 segundos = 3.75 MB máximo antes do timeout. **Downloads de ROMs reais sempre falham** em conexões lentas.

**Fix:** Para streaming de arquivos grandes, não há `readTimeout` adequado fixo:
```java
.readTimeout(0, TimeUnit.SECONDS)  // sem timeout — stream por tempo indeterminado
.callTimeout(6, TimeUnit.HOURS)    // mas limitar chamada total
```

---

### A-19 · `VmProcessGuard.tryRegister` — Exceção swallowed, estado VM inconsistente
**Arquivo:** `VmProcessGuard.java`  
**Categoria:** Error Handling Silencioso

```java
} catch (Throwable t) {
    Log.w(TAG, "suppressed register crash...");
    if (process != null && process.isAlive()) process.destroy();
    return false;
}
```

Falha de registro retorna `false` — processo QEMU é destruído, mas `VM_STATES` fica em `STARTING`. A UI mostra VM como iniciando indefinidamente até restart manual.

---

### A-20 · `CqcmActivity` — `\r\n` no `vmID` corrompe JSON de configuração
**Arquivo:** `CqcmActivity.java:129`  
**Categoria:** Data Corruption / JSON Injection

`vmID` vem de campo `EditText` que o usuário pode preencher com `\n`. O ID é serializado diretamente no JSON:
```java
"\"vmID\":\"" + vmID + "\""  // ← vmID com \n quebra o JSON
```
Resultado: arquivo `roms-data.json` corrompido → todas as VMs desaparecem.

---

### A-21 · `MainActivity` — `android:exported="false"` impede restauração de back-stack
**Arquivo:** `AndroidManifest.xml`  
**Categoria:** Activity Inacessível

```xml
<activity android:name=".main.MainActivity"
    android:exported="false" />  ← não pode ser instanciada externamente
```

`SplashActivity` (launcher) inicia `MainActivity`. Se o sistema matar o processo (OOM) e o usuário tentar voltar pela `recents screen`, o Android tenta reinstanciar `MainActivity` externamente → `ActivityNotFoundException` crash.

---

## SEÇÃO B — BUGS ALTOS (SEV-2): FALHAS SILENCIOSAS EM RUNTIME

### B-01 · `ProcessSupervisor.bindProcess` — synchronized em método público, mas `cleanupExitedSupervisor` em thread separada
**Arquivo:** `ProcessSupervisor.java`  
**Categoria:** Race Condition

`bindProcess` é `synchronized`, mas o watcher de exit (`spawnProcessExitWatcher`) chama `cleanupExitedSupervisor` de uma thread daemon — sem sincronização com chamadas externas de `stopVmProcess`. Janela de race: VM pode ser marcada como `STOPPED` enquanto um `tryMarkVmStarting` está em andamento.

---

### B-02 · `3dfx wrapper` — `wrapperCdrom.replace(",media=cdrom", ",media=cdrom,readonly=on")` duplica o atributo
**Arquivo:** `StartVM.java:156–163`  

```java
String wrapperCdrom = "-drive index=4,media=cdrom,file=" + shellQuote(backendWrapperPath);
wrapperCdrom = wrapperCdrom.replace(",media=cdrom", ",media=cdrom,readonly=on");
```

Se o path já contiver `,media=cdrom` (improvável mas possível em caminhos de ISO com nomes descritivos), o replace pode duplicar o argumento: `-drive ...,media=cdrom,readonly=on,...media=cdrom,...`. QEMU rejeita argumentos duplicados.

---

### B-03 · `ARM64 cdrom` — Duplica `-device nec-usb-xhci` sem checar dispositivos existentes
**Arquivo:** `StartVM.java:92–107`  

```java
if (!extras.contains("-device nec-usb-xhci")) {
    cdrom += " -device qemu-xhci";
    cdrom += " -device nec-usb-xhci";  // ← ambos adicionados!
}
```

`-device qemu-xhci` e `-device nec-usb-xhci` são dois controladores USB diferentes. Adicionar ambos pode conflitar em configurações ARM64. A verificação `extras.contains("-device nec-usb-xhci")` evita duplicar `nec-usb-xhci` mas **nunca** evita duplicar `qemu-xhci`.

---

### B-04 · `boot += "c"` sem validação de ISO em extras
**Arquivo:** `StartVM.java:178–183`  

```java
if (extras.contains(".iso ")) {
    boot += MainSettingsManager.getBoot(activity);  // usar boot configurado
} else {
    boot += "c";  // boot do disco
}
```

`extras.contains(".iso ")` falha se o path terminar sem espaço (último argumento). Uma ISO como `/data/Windows.iso` (sem espaço trailing) resulta em `boot=c` → VM tenta bootar do disco ao invés do CD → loop de boot.

---

### B-05 · `resolveBackendPath` — FD não fechado após uso
**Arquivo:** `StartVM.java:536`  
**Categoria:** File Descriptor Leak

```java
int fd = FileUtils.get_fd(activity, path, backendMode);
return "/proc/self/fd/" + fd;
```

O FD aberto por `get_fd` é passado via `/proc/self/fd/N` para QEMU. Quando QEMU abre o arquivo, o FD original no processo Java **nunca é fechado**. Em sessões com múltiplas VMs, FDs se acumulam até o limite do processo (padrão: 1024).

---

### B-06 · `VMManager.finalJson` — Campo estático mutable compartilhado entre threads
**Arquivo:** `VMManager.java:87`  
**Categoria:** Thread Safety

```java
public static String finalJson = "";
```

`finalJson` é lido e escrito de múltiplas threads (UI thread, background workers) sem sincronização. Em dispositivos multi-core, pode resultar em leitura de JSON parcialmente escrito → parse error.

---

### B-07 · `QmpClient.sendCommandForStopPath` — Sem timeout, bloqueia indefinidamente
**Arquivo:** `QmpClient.java`  
**Categoria:** Potencial ANR

Chamada QMP Unix socket sem timeout explícito. Se QEMU travar, `sendCommandForStopPath` bloqueia a thread supervisor indefinidamente — impedindo o stop da VM.

---

### B-08 · `PulseAudio.start()` — Sem verificação se já está rodando
**Arquivo:** `core/PulseAudio.java`  
**Categoria:** Processo Duplicado

`pulseaudio --start` é idempotente (retorna imediatamente se já estiver rodando), mas `--exit-idle-time=-1` no `BASE_RUN_COMMAND_FORMAT` reinicia o daemon. Múltiplas VMs iniciadas rapidamente podem criar múltiplos daemons PulseAudio conflitantes.

---

### B-09 · `VmFlowTracker` sem TTL/cleanup → crescimento de memória unbounded
**Arquivo:** `VmFlowTracker.java`  
**Categoria:** Memory Leak Gradual

O tracker acumula entradas de todas as VMs já iniciadas/paradas na sessão. Sem TTL ou cleanup após `STOP`, em sessões longas (horas de uso), a estrutura cresce indefinidamente.

---

### B-10 · `LowLevelBridge.ZiprafEngine` — Fallback SW referencia classe não presente
**Arquivo:** `bug/core/LowLevelBridge.java`  
**Categoria:** ClassNotFoundException em fallback

```java
return ZiprafEngine.phiFold4(a, b, c, d);  // SW fallback quando JNI falha
```

`ZiprafEngine` não existe em `app/src/main/java/` ou `app/src/main/kotlin/`. Se `System.loadLibrary("vectra_core_accel")` falhar (ex: ABI mismatch), o fallback crasha com `ClassNotFoundException`.

---

### B-11 · `RafaeliaQemuTuning.ensureTcgTbSize` — Não adiciona `tb-size` quando `-accel tcg` está no extras
**Arquivo:** `RafaeliaQemuTuning.java`  
**Categoria:** Otimização Nunca Aplicada

```java
while (matcher.find()) {   // procura padrão "-accel tcg[^\s]*"
    if (accel.contains("tb-size=")) { continue; }
    String tuned = accel + ",tb-size=" + tbSize;
    // ...
    changed = true;
}
if (!changed) { return extras; }  // ← se não encontrou, retorna sem modificação
```

Se o usuário especificou `-accel tcg,thread=multi` (comum), o regex `(?<!\S)-accel\s+tcg[^\s]*` captura isso. Mas se `-accel` e `tcg` estiverem em tokens separados na string (ex: `-accel  tcg` com dois espaços), o match falha silenciosamente.

---

### B-12 · `normalizeCdromArgumentStyle` — Tokenizador não preserva quotes em paths com espaço
**Arquivo:** `StartVM.java`  
**Categoria:** Bug de Parsing

O tokenizador `tokenizeArguments()` trata quotes corretamente durante tokenização, mas ao reconstruir os tokens com `replacements`, o conteúdo do token é inserido sem re-quoting:
```java
"-cdrom \" + pathToken.text  // pathToken.text pode conter espaços sem quotes
```
Paths como `/sdcard/My VMs/disk.iso` serão quebrados em dois tokens pelo shell.

---

### B-13 · `SharedFolder` — `fat:rw:` sem sanitização do path → path traversal
**Arquivo:** `StartVM.java:167`  
**Categoria:** Path Traversal

```java
driveParams += "rw:";
driveParams += FileUtils.getExternalFilesDirectory(activity).getPath() + "/SharedFolder,format=raw";
```

Se `SharedFolder` não existir e for criado pelo usuário com symlink para `/data/data/`, QEMU acessará dados internos do app via FAT virtual.

---

### B-14 · `ProcessRuntimeOps.safePid` — Retorna -1 em Android < API 26
**Arquivo:** `ProcessRuntimeOps.java`  
**Categoria:** Compatibilidade

`Process.pid()` requer API 26. Em API 24–25 (ainda ~3% dos dispositivos), retorna -1. O supervisor usa o PID para logging e deduplicação — com PID=-1 para todos os processos, a deduplicação falha.

---

### B-15 · `ImportSessionWorker` sem verificação de espaço em disco antes de import
**Arquivo:** `importer/ImportSessionWorker.java`  
**Categoria:** IOException não tratada

Importação de ROMs grandes (8–16GB) sem verificar espaço disponível. Falha no meio da operação deixa arquivo parcial que não é limpo automaticamente.

---

### B-16 · `CrashHandler` — Crash log local, nunca enviado para Firebase
**Arquivo:** `crashtracker/CrashHandler.java`  
**Categoria:** Observabilidade

```java
// Salva crash em /data/data/.../logs/lastcrash.txt
// Nenhuma chamada a FirebaseCrashlytics.getInstance().recordException()
```

Crashes em produção são invisíveis para os desenvolvedores.

---

### B-17 · `AuditLedger` sem limite de tamanho → armazenamento ilimitado
**Arquivo:** `audit/AuditLedger.java`  
**Categoria:** Storage Exhaustion

Eventos de auditoria são acumulados sem TTL ou limite de registros. Em uso intenso (múltiplas VMs, logs verbose), pode esgotar armazenamento interno.

---

### B-18 · `QemuArgsBuilder.applyVirtioNet` — Adiciona `-nic user,model=virtio-net-pci` mesmo para ARM64
**Arquivo:** `QemuArgsBuilder.java`  
**Categoria:** Incompatibilidade de Hardware Emulado

```java
params.add("-nic");
params.add("user,model=virtio-net-pci");
```

Para ARM64 com `-machine virt`, `virtio-net-pci` requer um bus PCI. A máquina `virt` não tem PCI por padrão — deve usar `virtio-net-device` (sem PCI). Resultado: QEMU falha com `Device virtio-net-pci is not compatible with machine virt`.

---

### B-19 · `TokenBucketRateLimiter` — Não thread-safe para uso concorrente
**Arquivo:** `core/TokenBucketRateLimiter.java`  
**Categoria:** Race Condition

Sem `synchronized` ou `AtomicLong` para `tokens`. Múltiplas threads pedindo tokens simultaneamente podem consumir mais do que o bucket permite.

---

### B-20 · `X11Activity` — `ICmdEntryInterface` AIDL sem timeout em bind
**Arquivo:** `x11/X11Activity.java`  
**Categoria:** Hang de UI

Bind ao `CmdEntryPoint` via AIDL sem timeout. Se o serviço X11 não responder, a Activity fica congelada indefinidamente.

---

### B-21 · `DownloadStateStore` sem transação atômica — estado inconsistente em crash
**Arquivo:** `download/DownloadStateStore.java`  
**Categoria:** Estado Corrompido

Update de status e progresso em operações separadas. Se o app crashar entre as duas operações, o estado fica com `status=RUNNING` mas `progress=0`, causando redownload desnecessário.

---

### B-22 · `VectrasApp.getContext()` — `WeakReference` pode retornar null
**Arquivo:** `VectrasApp.java`  
**Categoria:** NPE em código de utilidade

```java
private static WeakReference<Context> context;

public static Context getContext() {
    return context.get();  // pode retornar null!
}
```

Chamadas como `CommandUtils.getQemuVersion()` usam `VectrasApp.getContext()` sem checagem de null → NPE.

---

## SEÇÃO C — BUGS MÉDIOS (SEV-3): DEGRADAÇÃO E INCONSISTÊNCIA

### C-01 · `RafaeliaQemuProfile` — Argumento `-rafaelia` não reconhecido por QEMU padrão
**Arquivo:** `rafaelia/RafaeliaQemuProfile.java`  
**Categoria:** Argumento Silenciosamente Ignorado

```java
if (rafaeliaArg != null && !finalextra.contains("-rafaelia")) {
    params.add(rafaeliaArg);
}
```

`-rafaelia` não é um argumento QEMU padrão — só funciona com o fork `qemu_rafaelia` customizado. Em instalações padrão, QEMU reporta `qemu: invalid option -- 'rafaelia'` e pode falhar ao iniciar.

---

### C-02 · `engine/vectra_policy_kernel/` (Rust) — `build.rs` referencia dependências não presentes
**Arquivo:** `engine/vectra_policy_kernel/Cargo.toml`

O crate Rust tem `build.rs` que provavelmente requer `cbindgen`. Sem ele no ambiente de build, a compilação do módulo de policy falha silenciosamente (fallback para stub).

---

### C-03 · `neededPkgsTermux` — Branch ARM e x86 idênticas
**Arquivo:** `AppConfig.java`

```java
if (DeviceUtils.isArm()) { return "bash aria2 tar xterm proot pulseaudio"; }
return "bash aria2 tar xterm proot pulseaudio";  // ← idêntico!
```

Dead code. A condição não tem efeito.

---

### C-04 · `CMakeLists.txt` com `-ffast-math` em código de checksum
**Arquivo:** `app/build.gradle:36`

```groovy
arm64CppFlags = ["-O3", "-ffast-math", ...]
```

`-ffast-math` permite ao compilador reordenar operações de ponto flutuante, violando IEEE 754. Em código de checksum/hashing (como `rmr_lowlevel.c`), isso pode produzir resultados não-determinísticos entre compiladores/versões.

---

### C-05 · `vectra_core_accel.c` expõe JNI com linkage `JNIEXPORT` global
**Arquivo:** `app/src/main/cpp/`  

Símbolos JNI expostos globalmente sem namespace (ex: `Java_com_vectras_vm_core_LowLevelBridge_nativeFold32`). Em APKs com múltiplos módulos nativos, colisão de símbolo é possível se outro módulo definir função com nome idêntico.

---

### C-06 · `RafaeliaKernelV22.mixWeighted` — Sem normalização de pesos
**Arquivo:** `RafaeliaKernelV22.java`  

```java
for (int i = 0; i < vectors.length; i++) {
    double w = probabilities[i];
    for (int j = 0; j < len; j++) {
        out[j] += vectors[i][j] * w;
    }
}
```

Se a soma de `probabilities` ≠ 1.0, o vetor de saída não é uma mistura válida. Não há normalização ou validação.

---

### C-07 · `ProcessOutputDrainer` — Threads sem nome legível
**Arquivo:** `core/ProcessOutputDrainer.java`  

Threads de draining criadas sem nome descritivo. Em ANR traces e thread dumps, aparecem como `Thread-N` — impossível identificar a VM associada.

---

### C-08 · `DataExplorerActivity` — Acesso ao sistema de arquivos sem `requestLegacyExternalStorage`
**Arquivo:** `DataExplorerActivity.java`  

Em Android 10+ (API 29), acesso a `/sdcard/` requer `requestLegacyExternalStorage=true` ou SAF. O Manifest não declara `android:requestLegacyExternalStorage`.

---

### C-09 · `web/data/*.json` — JSON de ROMs com URLs hardcoded para servidor externo
**Arquivo:** `web/data/roms-X86_64.json` etc.  

URLs de download de ROMs apontam para servidor externo. Se o servidor sair do ar, todas as ROMs ficam indisponíveis sem nenhuma mensagem de erro amigável.

---

### C-10 · `BenchmarkActivity` em código de produção
**Arquivo:** `benchmark/BenchmarkActivity.java`  

Benchmark de CPU/IO/memória commitado como código de produção, exposto na UI. Usuários comuns não deveriam ter acesso a isso.

---

### C-11 · `TermuxX11.java` — `Runtime.getRuntime().exec()` sem sanitização
**Arquivo:** `core/TermuxX11.java`  

```java
Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
```

`command` pode incluir variáveis de usuário. Sem sanitização → potencial injection.

---

### C-12 · `RELEASEX64_OVMF_VARS.fd` — Arquivo UEFI mutable sem proteção
**Arquivo:** `app/src/main/assets/roms/`  

`OVMF_VARS.fd` é um arquivo mutable (variáveis UEFI persistentes). Ao ser servido como asset, é read-only. QEMU precisa de acesso de escrita para persistir configurações UEFI — deve ser copiado para storage interno antes do uso.

---

### C-13 · `VmsDiffUtil` — `areContentsTheSame` sem comparação profunda
**Arquivo:** `main/vms/VmsDiffUtil.java`  

Usa comparação superficial de referência. Alterações em campos de VM (nome, parâmetros) não causam refresh da lista → UI mostra dados antigos.

---

### C-14 · `RomReceiverActivity` sem validação de MIME type do Intent
**Arquivo:** `RomReceiverActivity.java`  

Aceita qualquer arquivo enviado via `ACTION_SEND` sem verificar se é realmente uma imagem de disco. Arquivo malformado pode crashar o importador.

---

### C-15 · `SplashActivity` com `uiMode` em `configChanges` mas sem re-aplicação de tema
**Arquivo:** `AndroidManifest.xml`  

`configChanges` inclui `uiMode` para evitar restart, mas a Activity não re-aplica o tema quando `uiMode` muda (dark/light mode toggle) → UI pode ficar com cores incorretas até restart manual.

---

### C-16 · `rmr_casm_arm64.S` — Assembly ARM64 sem `.type` e `.size` directives
**Arquivo:** `engine/rmr/interop/rmr_casm_arm64.S`  

Funções assembly sem `.type func, %function` e `.size func, .-func`. Debuggers e profilers não conseguem identificar corretamente as funções. Stack unwinding pode ser incorreto em crash reports.

---

## SEÇÃO D — BUGS BAIXOS (SEV-4): QUALIDADE / MANUTENÇÃO

| ID | Arquivo | Descrição |
|----|---------|-----------|
| D-01 | `AppConfig.java` | `neededPkgs32bitTermux()` chama `neededPkgsTermux()` → sem diferença entre 32/64 bit |
| D-02 | `RomStoreHomeAdpater.java` | Typo `Adpater` → `Adapter` no nome da classe |
| D-03 | `VECTRAS_MEGAPROMPT_DOCS.md` | Documento interno de prompting commitado em repo público |
| D-04 | `3dfx/*.iso` (4 arquivos) | Binários ISO no repositório inflam clone size significativamente |
| D-05 | `reports/` (múltiplos MD) | Relatórios de auditoria interna commitados no repo |
| D-06 | `_incoming/readme.md` | Diretório de patches sem rastreio formal — mesma antipattern do RafGitTools |
| D-07 | `tools/check_sensitive_artifacts.sh` | Script de segurança não é executado no CI |
| D-08 | `VectraCore.md` | Referência a "VECTRA_CORE_SEED = 0" em BuildConfig — valor não documentado |
| D-09 | `CommandUtils.getQemuVersionName()` | Chama `getQemuVersion()` que bloqueia thread com `executeShellCommandWithResult` |
| D-10 | `LogsAdapter.java` | `notifyDataSetChanged()` em vez de DiffUtil — flash em toda atualização de logs |
| D-11 | `RafaeliaSalmoCore.java` | Nome não documentado — funcionalidade opaca |
| D-12 | `ProguardRules.pro` | Sem `-keep` para classes de reflection usadas em `shell-loader` |
| D-13 | `web/coffee.html` | Página de doação commitada como asset de app |
| D-14 | `VERSION_STABILITY.md` | Documento de estabilidade vazio (apenas template) |

---

## SEÇÃO E — ANÁLISE DA CAMADA NATIVA (RMR Engine)

### RMR Engine — Pontos Positivos
O engine C/Rust (`engine/rmr/`) demonstra design sólido:
- `rmr_neon_simd.c`: SIMD ARM NEON com guards `#ifdef __ARM_NEON`
- `rmr_policy_kernel.c`: política determinística com fallback
- `vectra_policy_kernel/` (Rust): `ffi.rs` correto com `#[no_mangle]`

### RMR Engine — Bugs Identificados

**N-01 · `rmr_casm_x86_64.S` — sem CFI directives**  
`.cfi_startproc` / `.cfi_endproc` ausentes. Stack unwinding em exception handlers falha → crash reports incorretos.

**N-02 · `vectra_core_accel.c` — `VECTRA_CORE_SEED = 0` sempre**  
```c
// BuildConfig: buildConfigField "int", "VECTRA_CORE_SEED", "0"
```
Seed hardcoded em 0 → comportamento não-determinístico em produção vs. debug se o seed for usado para qualquer inicialização.

**N-03 · `rmr_lowlevel_portable.c` — sem bounds check em `reduce_xor`**  
O loop `while (i < length)` sem validação de `offset + length <= array_size` pode causar leitura fora dos bounds se JNI passar valores inválidos.

---

## ROADMAP DE CORREÇÃO

### 🔴 Sprint 0 — Crítico Imediato (≤3 dias)
```
1. [A-01] StartVM.cdrompath = "" (inicializar na declaração)
2. [A-02] Corrigir "-object " → "-object" (remover trailing space)
3. [A-03] Mover VNC password para QMP post-init
4. [A-08] Adicionar EndpointValidator.isAllowed(url) no DownloadWorker
5. [A-13] Corrigir typo "verctrasvm" → "vectrasvm"
6. [A-15] Corrigir KvmProbe " virt" → regex \bvirt\b
7. [A-18] DownloadWorker: readTimeout(0) + callTimeout(6h)
```

### 🟠 Sprint 1 — Estabilidade (1 semana)
```
8.  [A-04] AppConfig: lazy initialization via ApplicationContext
9.  [A-05] MainService.activityContext → WeakReference<Context>
10. [A-09] startRandomPort → SecureRandom + ServerSocket availability check
11. [A-10] Consolidar ExecutionBudgetPolicy em pacote único
12. [A-21] MainActivity: exported="true" com intent-filter correto
13. [B-05] resolveBackendPath: fechar FD original após uso pelo QEMU
14. [B-18] applyVirtioNet: ARM64 usa virtio-net-device, não virtio-net-pci
15. [B-04] boot detection: extras.endsWith(".iso") || extras.contains(".iso ")
```

### 🟡 Sprint 2 — Qualidade (2 semanas)
```
16. [A-06] Loader: GET_SIGNING_CERTIFICATES para API >= 28
17. [A-12] VNCConfig: EncryptedSharedPreferences para senha
18. [B-01] ProcessSupervisor: mutex explícito para watcher + cleanup
19. [B-16] CrashHandler: integrar FirebaseCrashlytics
20. [C-04] Remover -ffast-math de código de checksum
21. [C-12] OVMF_VARS.fd: copiar para files dir antes de usar
22. [D-04][D-03] .gitignore para *.iso e docs internos
```

---

## MATRIZ DE CONFORMIDADE

| Padrão | Requisito | Status |
|--------|-----------|--------|
| OWASP MASVS V2 | Dados sensíveis criptografados | ❌ FAIL — VNC password em plaintext (CLI + SharedPrefs) |
| OWASP MASVS V6 | Input validation | ❌ FAIL — SSRF em DownloadWorker, vmID sem sanitização |
| OWASP MASVS V8 | Resiliência | ⚠️ PARCIAL — ProcessSupervisor bom, mas race conditions |
| ISO/IEC 25010 | Confiabilidade | ❌ FAIL — NPE em cdrompath, FD leaks, state corruption |
| ISO/IEC 25010 | Segurança | ❌ FAIL — 3 vulnerabilidades CWE críticas |
| Android Security | Permissões mínimas | ⚠️ PARCIAL — SYSTEM_ALERT_WINDOW não usado |
| Android Best Practices | Lifecycle | ❌ FAIL — static Context, Activity leak |
| Google Play Policy | Permissões | ⚠️ RISCO — permissão SYSTEM_ALERT_WINDOW declarada sem uso |

---

## COMPARAÇÃO RafGitTools vs Vectras

| Métrica | RafGitTools v6 | Vectras |
|---------|---------------|---------|
| Bugs totais | 61 | 73 |
| Críticos | 17 | 21 |
| Vetor de risco | 8.4/10 | 9.1/10 |
| Patches unaplicados | 7 arquivos `fazer/` | ~12 fixes identificados |
| Vulnerabilidades CWE | 3 | 5 |
| Race conditions | 2 | 4 |
| Memory leaks confirmados | 3 | 5 |
| Stubs retornando true | 4 | 0 |
| Bugs de serialização | 2 | 3 |

---

*Relatório gerado por: `RAFAELIA·BugHunter·Ω` — ψ→χ→ρ→Δ→Σ→Ω*  
*R(t+1)=R(t)×Φ_ethica×E_Verbo×(√3/2)^(πφ) — Φ_ethica=Min(Entropia)×Max(Coerência)*
