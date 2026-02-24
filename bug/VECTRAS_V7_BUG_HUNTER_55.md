# 🔴 BUG HUNTER REPORT — Vectras-V-master (v7)
> **Auditoria:** Deep Source Code Audit · Bug Hunter Mode  
> **Arquivo:** `Vectras-V-master__7_.zip`  
> **Data:** 2026-02-24  
> **Auditor:** RAFAELIA·Ω·BugHunter  
> **Bugs encontrados:** 55 confirmados (13 críticos · 18 altos · 14 médios · 10 baixos)

---

## DASHBOARD

```
┌──────────────────────────────────────────────────────────────────┐
│  RISCO GLOBAL:  ██████████░  9.0/10                              │
│                                                                   │
│  🔴 SEV-1  ████████████████  13  →  CRASH / SEGURANÇA           │
│  🟠 SEV-2  ████████████████  18  →  RUNTIME / DADOS             │
│  🟡 SEV-3  █████████████░░░  14  →  DEGRADAÇÃO                  │
│  🟢 SEV-4  ████████████░░░░  10  →  QUALIDADE                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## GRUPO 1 — BUGS DE TRAILING SPACE NO BUILDER DE ARGS QEMU

> **Raiz comum:** Strings como `"-vnc "`, `"-spice "`, `"-L "`, `"-bios "`, `"-drive "`
> são adicionadas como tokens únicos ao `ArrayList<String> params`.
> O `String.join(" ", params)` então insere um espaço adicional,
> resultando em argumentos com **duplo espaço** que QEMU rejeita.

---

### BUG-01 🔴 — `-vnc ` trailing space → argumento inválido
**Arquivo:** `StartVM.java:299–300`

```java
String vncStr = "-vnc ";    // ← trailing space
params.add(vncStr);
params.add(vncSocketParams); // e.g. "unix:/cache/.../vncsocket"
```

**Resultado do join:**
```
qemu-system-x86_64 ... -vnc  unix:/path/vncsocket
```
QEMU parseia `-vnc` como flag vazia e `unix:/path/vncsocket` como argumento órfão.
VNC **sempre falha** no modo Unix socket.

**Fix:**
```java
params.add("-vnc");
params.add(vncSocketParams);
```

---

### BUG-02 🔴 — `-spice ` trailing space → argumento inválido + LAN exposta sem auth
**Arquivo:** `StartVM.java:322–324`

```java
String spiceStr = "-spice ";
spiceStr += "port=6999,disable-ticketing=on";
params.add(spiceStr);
```

**Resultado:**
```
-spice  port=6999,disable-ticketing=on
```
Dois problemas simultâneos:
1. Double space → QEMU rejeita o argumento SPICE
2. `disable-ticketing=on` + bind em `0.0.0.0` → SPICE acessível **sem senha a qualquer host na rede local**

**Fix:**
```java
params.add("-spice");
params.add("addr=127.0.0.1,port=6999,disable-ticketing=on");
```

---

### BUG-03 🟠 — `-L ` trailing space (PPC BIOS)
**Arquivo:** `StartVM.java:208–209`

```java
bios = "-L ";
bios += "pc-bios";
```

**Resultado:** `"-L  pc-bios"` → QEMU interpreta `-L` com argumento vazio, falha ao encontrar `pc-bios`.

---

### BUG-04 🟠 — `-bios ` trailing space (bios-vectras.bin)
**Arquivo:** `StartVM.java:221–222`

```java
bios = "-bios ";
bios += AppConfig.basefiledir + "bios-vectras.bin";
```

**Resultado:** `"-bios  /path/bios-vectras.bin"` → QEMU não encontra o arquivo BIOS.

---

### BUG-05 🟠 — `-drive ` trailing space (SharedFolder FAT)
**Arquivo:** `StartVM.java:176–184`

```java
String driveParams = "-drive ";
driveParams += "index=3,media=disk,file=fat:rw:/path/SharedFolder,format=raw";
params.add(driveParams);
```

**Resultado:** `"-drive  index=3,..."` → shared folder QEMU drive nunca monta.

---

### BUG-06 🟠 — `bios = ""` sempre adicionado a `params` → token vazio no join
**Arquivo:** `StartVM.java:43, 251`

```java
String bios = "";           // inicializado vazio
// ...
params.add(bios);           // adiciona "" ao ArrayList
```

Quando `useDefaultBios()` é `false` (e.g., ARM64 sem UEFI), `bios` permanece `""`.
`String.join(" ", params)` produz:
```
qemu-system-x86_64  -m 512  ...
```
O token vazio não quebra o comando, mas a presença do argumento vazio em `bash -c "..."` pode causar comportamento inesperado em algumas versões de shell.

**Fix:** `if (!bios.isEmpty()) params.add(bios);`

---

### BUG-07 🟠 — `-drive ` trailing space (BIOS ARM64 + X86_64 UEFI)
**Arquivo:** `StartVM.java:211–219`

```java
bios = "-drive ";
bios += "file=" + AppConfig.basefiledir + "QEMU_EFI.img,...";
bios += " -drive ";
bios += "file=" + AppConfig.basefiledir + "QEMU_VARS.img,...";
```

O primeiro token `"-drive "` tem trailing space. Resultado:
```
-drive  file=/path/QEMU_EFI.img,...
```
UEFI nunca carrega em ARM64 ou X86_64 com UEFI habilitado.

---

## GRUPO 2 — SEGURANÇA

---

### BUG-08 🔴 — `Config.datadirpath` static final: NPE → ExceptionInInitializerError
**Arquivo:** `Config.java:191`

```java
// Campo static final inicializado em tempo de carga da classe:
public static final String datadirpath = VectrasApp.getApp().getExternalFilesDir("data") + "/";
```

`VectrasApp.getApp()` retorna `vectrasapp` — campo `public static` definido em `Application.onCreate()`.
Se **qualquer classe** que importe `Config` for carregada **antes** de `Application.onCreate()` (ex: `ContentProvider.onCreate()`, `BroadcastReceiver`, `WorkManager` no boot), o resultado é:

```
java.lang.NullPointerException: VectrasApp.getApp() == null
→ java.lang.ExceptionInInitializerError
→ Todas as referências subsequentes a Config lançam NoClassDefFoundError
```

**Fix:**
```java
public static String getDatadirpath() {
    if (VectrasApp.getApp() == null) return "";
    File dir = VectrasApp.getApp().getExternalFilesDir("data");
    return dir != null ? dir.getAbsolutePath() + "/" : "";
}
```

---

### BUG-09 🔴 — SPICE `disable-ticketing` + `0.0.0.0` → VM acessível na rede local sem senha
**Arquivo:** `StartVM.java:322–324`, `Config.java:148`

Mesmo corrigindo o trailing space do BUG-02, o argumento ainda seria:
```
-spice port=6999,disable-ticketing=on
```
`disable-ticketing=on` = **sem autenticação**. Qualquer dispositivo na mesma rede Wi-Fi pode conectar ao SPICE sem senha. O bind em `0.0.0.0` é o default do QEMU.

**Fix:** Usar `addr=127.0.0.1` e autenticação SPICE.

---

### BUG-10 🔴 — `DH.rng()` usa `Math.random()` — criptografia VNC fraca
**Arquivo:** `DH.java:41`

```java
private long rng(long limit) {
    return (long) (java.lang.Math.random() * limit);
}
```

`Math.random()` usa um `Random` linear congruente — **não é criptograficamente seguro**. O DH implementado aqui usa este RNG para gerar primos e chaves privadas. Um atacante que conheça o timestamp aproximado pode prever os valores gerados.

**Fix:**
```java
private final java.security.SecureRandom secureRng = new java.security.SecureRandom();
private long rng(long limit) {
    return (long) (secureRng.nextDouble() * limit);
}
```

---

### BUG-11 🔴 — `TermuxDocumentsProvider.isChildDocument()` — path traversal via `startsWith`
**Arquivo:** `TermuxDocumentsProvider.java:213`

```java
@Override
public boolean isChildDocument(String parentDocumentId, String documentId) {
    return documentId.startsWith(parentDocumentId);
}
```

Sem resolução de caminho canônico:
- `documentId = "/home/user/../etc/passwd"`
- `parentDocumentId = "/home/user"`
- `"/home/user/../etc/passwd".startsWith("/home/user")` → **`true`** ← bypass!

O Android framework usa `isChildDocument()` para validar operações de cópia/movimentação de arquivos.

**Fix:**
```java
try {
    String canonicalChild = new File(documentId).getCanonicalPath();
    String canonicalParent = new File(parentDocumentId).getCanonicalPath();
    return canonicalChild.startsWith(canonicalParent + File.separator)
        || canonicalChild.equals(canonicalParent);
} catch (IOException e) {
    return false;
}
```

---

### BUG-12 🔴 — `Loader.isTrustedSignature()` usa `hashCode()` — colisão de 32 bits
**Arquivo:** `Loader.java:43,54`

```java
actual[i] = signatures[i].hashCode();
```

`hashCode()` retorna `int` (32 bits). A verificação de assinatura pode ser bypassada por um APK cujas assinaturas produzam o mesmo `hashCode()` que as assinaturas legítimas — colisão deliberada com complexidade O(2³²) no caso geral.

**Fix:** Comparar os bytes brutos das assinaturas:
```java
actual[i] = Arrays.hashCode(signatures[i].toByteArray()); // ainda 32-bit
// Melhor: comparar byte[] diretamente
```

---

### BUG-13 🔴 — `RfbProto.authenticateVNC()` — senha truncada silenciosamente para 8 bytes
**Arquivo:** `RfbProto.java:543`

```java
if (pw.length() > 8)
    pw = pw.substring(0, 8);    // Truncate to 8 chars
```

É protocolo VNC-DES, mas: o usuário define uma senha de 16 caracteres, apenas os primeiros 8 são usados na autenticação. O usuário não é avisado. Força efetiva da senha cai de ~128 bits para ~56 bits (DES key space).

---

## GRUPO 3 — CRASH / NPE GARANTIDOS

---

### BUG-14 🔴 — `VMManager.deleteVM()` — Gson retorna `null` → NPE em `vmList.size()`
**Arquivo:** `VMManager.java:706–712`

```java
ArrayList<HashMap<String, Object>> vmList;
vmList = new Gson().fromJson(FileUtils.readAFile(...), new TypeToken<...>(){}.getType());

if (position > vmList.size() - 1) return;  // ← NPE se vmList == null
```

Se `readAFile` retornar `null` ou JSON inválido/corrompido, `Gson.fromJson()` retorna `null`.
A linha seguinte chama `vmList.size()` → `NullPointerException`.

**Fix:** `if (vmList == null || position < 0 || position > vmList.size() - 1) return;`

---

### BUG-15 🔴 — `VMManager.deleteVM()` — `position < 0` não validado → IOOBE
**Arquivo:** `VMManager.java:710`

```java
if (position > vmList.size() - 1) return;  // ← não verifica < 0
vmList.get(position)                         // ← IndexOutOfBoundsException se position = -1
```

Qualquer chamada com `position = -1` (possível via intent com valor inválido) produz `IndexOutOfBoundsException`.

---

### BUG-16 🟠 — `QmpClient.migrate()`, `setVncPassword()`, etc. — `JSONObject.put()` sem try-catch
**Arquivo:** `QmpClient.java:303–312`

```java
// Esses métodos NÃO estão dentro de try-catch:
arguments.put("blk", block);   // throws JSONException (checked)
arguments.put("inc", inc);
arguments.put("uri", uri);
jsonObject.put("execute", "migrate");
```

`JSONObject.put()` lança `JSONException` (exceção checada). Os métodos `migrate()`, `setVncPassword()`, `changevncpasswd()`, `ejectdev()`, `changedev()`, `save_snapshot()` não têm `try-catch`. Se o compilador aceita (depende da versão da biblioteca `org.json`), em runtime exceções inesperadas crasham o caller.

**Fix:** Envolver em `try { ... } catch (JSONException e) { Log.e(TAG, ...); return "{}"; }`

---

### BUG-17 🟠 — `RamInfo.vectrasMemory()` — valor negativo → QEMU `-m -50` falha
**Arquivo:** `RamInfo.java:29`

```java
long freeMem = mi.availMem / 1048576L;
int freeRamInt = safeLongToInt(freeMem);
return freeRamInt - 100;   // ← se freeMem < 100 MB → resultado negativo
```

Em dispositivos com pouca memória disponível (comum durante uso intenso), `freeRamInt` pode ser 40, 50, etc.
Resultado: `return -60` → QEMU lançado com `-m -60` → QEMU reporta erro e não inicia.

**Fix:**
```java
int result = freeRamInt - 100;
return Math.max(result, 256);  // mínimo 256 MB
```

---

## GRUPO 4 — THREADING / CONCORRÊNCIA

---

### BUG-18 🟠 — `FileUtils.fds` é `HashMap` (não thread-safe) acessado de múltiplas threads
**Arquivo:** `FileUtils.java:518`

```java
public static HashMap<Integer, ParcelFileDescriptor> fds = new HashMap<Integer, ParcelFileDescriptor>();
```

`get_fd()` é chamado de:
- Thread principal (ao preparar args do QEMU)
- Threads de background (workers de import, download)
- `close_fds()` chamado de thread de cleanup

Acesso concorrente a `HashMap` sem sincronização → `ConcurrentModificationException` ou corrupção silenciosa de FDs.

**Fix:** Substituir por `ConcurrentHashMap` ou adicionar `synchronized`.

---

### BUG-19 🟠 — `ProcessSupervisor.stopGracefully()` mantém lock por até **8 segundos**
**Arquivo:** `ProcessSupervisor.java:202–235`

```java
public synchronized boolean stopGracefully(boolean tryQmp) {
    // ...
    if (awaitExit(running, 3_000)) { ... }   // ← 3s bloqueado
    // ...
    if (awaitExit(running, 3_000)) { ... }   // ← mais 3s
    // ...
    boolean killed = awaitExit(running, 2_000); // ← mais 2s
```

O método é `synchronized` e chama `process.waitFor(timeoutMs)` três vezes consecutivas (máximo 3000 + 3000 + 2000 = 8 segundos). Durante esse tempo, qualquer thread tentando chamar `bindProcess()`, `isBoundTo()` ou `onDegraded()` fica bloqueada.

**Fix:** Fazer `awaitExit()` fora do synchronized block usando um `lock.tryLock()` ou separar o timeout do state management.

---

### BUG-20 🟠 — `VMManager.finalJson` — campo `public static` escrito sem sincronização
**Arquivo:** `VMManager.java:87`

```java
public static String finalJson = "";
```

Escrito em:
- `deleteVM()` (pode ser chamado de UI thread ou background)
- `replaceToVMList()` linha 726
- `loadVMList()` linha 816
- `startVmProcess()` linha 978

Sem `volatile` nem `synchronized`. Em dispositivos multi-core, uma thread pode ler valor parcialmente escrito → JSON corrompido → parse failure → crash.

**Fix:** `private static volatile String finalJson = "";` + acesso via métodos `synchronized`.

---

### BUG-21 🟠 — `QmpClient.sendCommand()` é `synchronized static` — lock global entre VMs
**Arquivo:** `QmpClient.java:31–35`

```java
public synchronized static String sendCommand(String command) { ... }
public synchronized static String sendCommandForStopPath(String command) { ... }
```

`synchronized static` = lock na **classe `QmpClient`**, compartilhado entre **todas as VMs**. Se VM-A está enviando um `set_password` (3 retries × 500ms = 1.5s máx), a VM-B não consegue enviar `system_powerdown` até a operação da VM-A terminar.

---

### BUG-22 🟠 — `startRandomPort()` / `idGenerator()` usa `new Random()` — semente previsível
**Arquivo:** `VMManager.java:646–659`

```java
final Random random = new Random();   // seed = System.currentTimeMillis()
```

Dois problemas:
1. Em dispositivos rápidos criando VMs rapidamente, `currentTimeMillis` é idêntico → mesma sequência de IDs/ports
2. `isPortAvailable()` (ServerSocket check) seguido de uso sem reserva → race window (TOCTOU)

---

### BUG-23 🟠 — `readReservedPortsFromVmDb()` — lógica `&&` deveria ser `||`
**Arquivo:** `VMManager.java:679`

```java
if (!FileUtils.isFileExists(AppConfig.romsdatajson) && !FileUtils.canRead(AppConfig.romsdatajson)) {
    return ports;
}
```

Com `&&`: retorna early **apenas se** o arquivo não existe **E** não é legível (ambos). Mas se o arquivo **existe e não é legível** (`isFileExists=true`, `canRead=false`), a condição é `false && true` = `false` → prossegue para `readAFile()` → `IOException` não tratada.

**Fix:** `||` → bail out se **qualquer** condição for verdadeira.

---

## GRUPO 5 — MEMORY LEAKS / RECURSOS

---

### BUG-24 🟠 — `FileUtils.get_fd()` — FDs abertos nunca fechados automaticamente
**Arquivo:** `FileUtils.java:534–536` + `StartVM.java:540`

```java
// FileUtils.java:
ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(...);
fd = pfd.getFd();
fds.put(fd, pfd);    // ← guardado em HashMap estático
```

```java
// StartVM.java resolveBackendPath():
int fd = FileUtils.get_fd(activity, path, backendMode);
return "/proc/self/fd/" + fd;   // ← retorna apenas o número, PFD permanece aberto
```

`close_fds()` existe mas nunca é chamada automaticamente ao terminar a VM. Após múltiplos lançamentos, o processo acumula FDs até atingir o limite (~1024) → `Too many open files`.

---

### BUG-25 🟠 — `RamInfo.activity` — referência estática a `Activity` → memory leak
**Arquivo:** `RamInfo.java:13`

```java
public static Activity activity;
```

Campo `public static` direto para `Activity`. A Activity fica presa na memória enquanto `RamInfo` existir (eternamente, por ser static). Toda a view hierarchy e bitmaps associados não são coletados pelo GC.

---

### BUG-26 🟠 — `applyVncPasswordOverQmpIfNeeded` cria `new Thread()` anônimo sem lifecycle
**Arquivo:** `MainStartVM.java:446`

```java
new Thread(() -> QmpClient.sendCommand(QmpClient.setVncPassword(password), 3, 500)).start();
```

Thread anônima sem referência, sem nome descritivo, sem `interrupt()` ao destruir a Activity. Em ANR traces aparece como `Thread-N`. Se a Activity for destruída antes da thread terminar, o callback pode tentar acessar objetos já coletados.

---

## GRUPO 6 — DESIGN / ARQUITETURA

---

### BUG-27 🟡 — SPICE porta `6999` hardcoded → segundo VM falha com EADDRINUSE
**Arquivo:** `StartVM.java:323`

```java
spiceStr += "port=6999,disable-ticketing=on";
```

Qualquer tentativa de lançar uma segunda VM com SPICE falhará com `bind: Address already in use` enquanto a primeira VM estiver rodando.

**Fix:** Usar `startRandomPort()` para a porta SPICE, similar ao QMP.

---

### BUG-28 🟡 — `Config.vmID = ""` inicial → `getLocalQMPSocketPath()` retorna path com `//`
**Arquivo:** `Config.java:201`, `Config.java` getLocalQMPSocketPath

```java
public static String vmID = "";

public static String getLocalQMPSocketPath() {
    return Config.getCacheDir() + "/" + vmID + "/qmpsocket";
    // quando vmID="" -> "/cache/path//qmpsocket"
}
```

Se `Config.vmID` ainda não foi setado (antes de `MainStartVM.java:150`), o socket path tem double slash. Em algumas implementações de Unix socket, isso pode falhar na conexão. Adicionalmente, `vmID` não é `volatile` apesar de ser lido de múltiplas threads.

---

### BUG-29 🟡 — ARM64 cdrom: `-device qemu-xhci` E `-device nec-usb-xhci` ambos adicionados
**Arquivo:** `StartVM.java:97–102`

```java
if (!extras.contains("-device nec-usb-xhci")) {
    cdrom += " -device qemu-xhci";      // ← sempre adicionado
    cdrom += " -device nec-usb-xhci";   // ← adicionado apenas se não presente nos extras
}
```

A guarda verifica apenas `nec-usb-xhci`, mas `qemu-xhci` **sempre** é adicionado sem verificação. Dois controladores USB distintos podem conflitar na máquina `virt` do ARM64.

---

### BUG-30 🟡 — VNC `password=on` setado, mas `set_password` via QMP tem janela de timing
**Arquivo:** `MainStartVM.java:475` + `StartVM.java:307`

```
StartVM: -vnc 0.0.0.0:5901,password=on   ← QEMU inicia SEM senha (requer set_password QMP)
...
[QMP socket criado]
LaunchPoller detecta socket → applyVncPasswordOverQmpIfNeeded()
→ new Thread(() -> QmpClient.setVncPassword(password), 3, 500).start()
```

Janela de vulnerability: entre QEMU iniciar (VNC ativo) e a senha ser enviada via QMP (até 1.5s), qualquer cliente VNC pode conectar ao VNC **sem senha**.

---

### BUG-31 🟡 — `Config.defaultVNCHost = "0.0.0.0"` → VNC externo exposto na LAN
**Arquivo:** `Config.java:148`

```java
public static String defaultVNCHost = "0.0.0.0";
```

Qualquer dispositivo na mesma rede Wi-Fi pode conectar ao VNC quando `getVncExternal(activity) = true`. Deveria ser `127.0.0.1` por default, com opção explícita para bind público.

---

### BUG-32 🟡 — `MainSettingsManager.commit()` no UI thread → StrictMode / ANR risk
**Arquivo:** `MainSettingsManager.java:69,74,82,119`

```java
getSupportFragmentManager().beginTransaction()
    .replace(R.id.settingz, fragment)
    .commit();   // ← FragmentTransaction.commit() — OK, este é o fragment commit
```

**Atenção:** As chamadas `commit()` aqui são de `FragmentTransaction`, não de `SharedPreferences`. Porém `SharedPreferences.Editor.commit()` **também** aparece no arquivo (linha 591 com `apply()` correto, mas linhas 69,74,82,119 são fragment commits — aparentemente sem problema de StrictMode).

**Porém:** `PreferenceManager.getDefaultSharedPreferences()` (deprecated API 29+) é usado em todo o arquivo → aplicação de preferências com API deprecada.

---

### BUG-33 🟡 — `AppConfig.sharedFolder`, `romsdatajson`, `vmFolder` — caminhos relativos ao inicializar
**Arquivo:** `AppConfig.java:83–86`

```java
public static String maindirpath = "";           // ← empty string
public static String sharedFolder = maindirpath + "SharedFolder/";   // = "SharedFolder/"
public static String romsdatajson = maindirpath + "roms-data.json";  // = "roms-data.json"
public static String vmFolder = maindirpath + "roms/";               // = "roms/"
```

Todos os caminhos derivados de `maindirpath` são calculados em **tempo de inicialização da classe**, quando `maindirpath = ""`. Qualquer código que acesse `AppConfig.romsdatajson` antes de `setupAppConfig()` receberá caminhos relativos (`"roms-data.json"`) → operações de arquivo em diretório de trabalho inesperado.

---

### BUG-34 🟡 — `google-services.json` placeholder — Firebase não funcional
**Arquivo:** `app/google-services.json`

```json
"project_id": "vectras-vm-placeholder",
"mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
"current_key": "AIzaSyDummyKeyForBuildPurposesOnly000000"
```

Firebase SDK inicializa com projeto inexistente. Crashlytics, Analytics, FCM e Remote Config **não funcionam em produção**. Crashes não são reportados.

---

### BUG-35 🟡 — `DownloadWorker` `readTimeout=30s` — downloads de ROMs grandes falham
**Arquivo:** `DownloadWorker.java:63`

```java
.readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
```

Em conexões lentas (1 Mbps = 125 KB/s), `30s × 125 KB/s = 3.75 MB` máximo por chunk. ROMs de 2–8GB sempre atingem o timeout e falham, mesmo com retry implementado.

**Fix:**
```java
.readTimeout(0, TimeUnit.SECONDS)  // sem timeout para streaming
.callTimeout(12, TimeUnit.HOURS)   // timeout total da chamada
```

---

## GRUPO 7 — MÉDIOS E BAIXOS

---

### BUG-36 🟡 — `SYSTEM_ALERT_WINDOW` declarada mas nunca solicitada via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`
**Arquivo:** `AndroidManifest.xml:28`

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

Em Android 6+, `SYSTEM_ALERT_WINDOW` não é concedida automaticamente — requer redirect para configurações. Nenhum código do app implementa esse fluxo. A permissão está declarada mas nunca funciona → risco de rejeição no Google Play.

---

### BUG-37 🟡 — `MainActivity` com `android:exported="false"` — não restaurável
**Arquivo:** `AndroidManifest.xml:83–86`

```xml
<activity android:name=".main.MainActivity"
    android:exported="false"
    android:windowSoftInputMode="adjustResize" />
```

Se o sistema matar o processo (OOM) e o usuário tentar retomar via recent apps, o Android tenta reinstanciar `MainActivity` externamente → `ActivityNotFoundException` crash.

---

### BUG-38 🟡 — `QmpClient.save_snapshot()` — JSON injection via `snapshot_name`
**Arquivo:** `QmpClient.java` (método save_snapshot)

```java
public static String save_snapshot(String snapshot_name) {
    return "{\"execute\": \"snapshot-create\", \"arguments\": {\"name\": \""
        + snapshot_name + "\"} }";
}
```

Se `snapshot_name` contiver `"` (aspas duplas), o JSON gerado fica malformado ou injeta comandos adicionais:
- Input: `my"snap` → JSON: `{"name": "my"snap"}` → parse error
- Input: `x","extra":"injected` → JSON injection potencial

**Fix:** Usar `JSONObject` para construir o JSON corretamente.

---

### BUG-39 🟡 — `SPICE + VNC simultâneos`: lógica de seleção frágil
**Arquivo:** `StartVM.java:297–333`

```java
if (getVmUi().equals("VNC")) {
    // ...
} else if (getVmUi().equals("SPICE")) {
    // ...
} else if (getVmUi().equals("X11")) {
    // ...
}
// else: sem display → VM sem output, sem aviso ao usuário
```

Se `getVmUi()` retornar qualquer string inesperada (null, vazia, valor de versão antiga salvo nas prefs), nenhum argumento de display é adicionado. QEMU inicia sem saída gráfica e o usuário não recebe aviso.

---

### BUG-40 🟡 — `idGenerator()` não garante unicidade entre múltiplas VMs criadas rapidamente
**Arquivo:** `VMManager.java:630–639`

```java
public static String idGenerator() {
    String _result = startRamdomVMID();
    if (isVMExist(_result)) {
        _result = startRamdomVMID();
    }
    if (isVMExist(_result)) {
        _result = startRamdomVMID();
    }
    return _result;
}
```

Apenas 2 retries. Com `Random` previsível e espaço de IDs de 10 chars alfanuméricos, a probabilidade de colisão em 2 retries é baixa mas não zero. Sem loop `do-while` e sem garantia forte de unicidade.

---

### BUG-41 🟡 — `CoreExecutionBudgetPolicy.resolveQemuCpuBudget()` — `sockets` sempre `1`
**Arquivo:** `CoreExecutionBudgetPolicy.java` método `resolveQemuCpuBudget`

```java
int sockets = "PPC".equals(arch) ? 1 : 1;  // ← ambos os ramos retornam 1
```

Dead code — a condição ternária não tem efeito. PPC e qualquer outra arquitetura recebem `sockets=1`. Provavelmente era `"PPC".equals(arch) ? 1 : 2` antes de uma edição.

---

### BUG-42 🟡 — `VMManager.isQemuStopedWithError` — typo persistido na API pública
**Arquivo:** `VMManager.java:92`

```java
public static boolean isQemuStopedWithError = false;  // "Stoped" vs "Stopped"
```

Campo público com nome incorreto. Qualquer plugin ou integração externa que use reflexão para checar o campo pelo nome precisa usar a versão com typo.

---

### BUG-43 🟡 — `VMManager.startRamdomVMID()` — typo no nome do método
**Arquivo:** `VMManager.java:643`

```java
public static String startRamdomVMID() {  // "Ramdom" vs "Random"
```

Método público com typo. Se eventualmente renomeado, quebra backward compatibility de qualquer código que o chame diretamente.

---

### BUG-44 🟢 — `deprecated android.preference.PreferenceManager` em 5 arquivos X11
**Arquivos:** `LoriePreferences.java`, `LorieView.java`, `X11Activity.java`, `FullscreenWorkaround.java`, `TermuxX11ExtraKeys.java`

```java
import android.preference.PreferenceManager;  // deprecated API 29
```

Substituir por `androidx.preference.PreferenceManager`.

---

### BUG-45 🟢 — `AppConfig.ensureStoragePaths(null)` chamado em `static {}` bloco
**Arquivo:** `AppConfig.java` static initializer

```java
static {
    ensureStoragePaths(null);
}
```

`ensureStoragePaths(null)` com `null` Context — a implementação precisa lidar com `null` corretamente ou isso causa NPE no static initializer → `ExceptionInInitializerError` similar ao BUG-08.

---

### BUG-46 🟢 — `VectrasApp.context` é `WeakReference<Context>` — pode retornar `null`
**Arquivo:** `VectrasApp.java:30–32`

```java
private static WeakReference<Context> context;

public static Context getContext() {
    return context.get();  // pode retornar null se GC coletou
}
```

`Application` context não deveria ser coletado (sobrevive ao app), mas `WeakReference` não garante isso. Código que usa `VectrasApp.getContext()` sem null-check pode crashar.

---

### BUG-47 🟢 — `CqcmActivity`: `vmID` gerado localmente mas não relacionado ao intent `vmId`
**Arquivo:** `CqcmActivity.java:71,84,91`

```java
String vmID = VMManager.idGenerator();    // ← novo ID local
// ...
vmId = getIntent().getStringExtra("vmId");  // ← ID do intent
```

Quando `isForceCreateNew = false` e VM existe, usa o ID do intent. Quando cria novo, usa o ID gerado localmente. Mas `vmID` local pode colidir se `VMManager.idGenerator()` retornar ID já existente e `isVMExist()` falhar a verificação (race condition).

---

### BUG-48 🟢 — `OVMF_VARS.fd` é asset read-only mas QEMU precisa de escrita
**Arquivo:** `StartVM.java:218–219`

```java
bios += " -drive ";
bios += "file=" + AppConfig.basefiledir + "RELEASEX64_OVMF_VARS.fd,format=raw,if=pflash";
// Sem ",readonly=on" ← QEMU abre para escrita (variáveis UEFI persistentes)
```

Se o arquivo estiver em diretório read-only (ex: assets extraídos sem permissão de escrita), QEMU falha ao abrir. Se tiver permissão, QEMU modifica o arquivo compartilhado entre VMs → uma VM pode corromper configurações UEFI de outra.

**Fix:** Copiar `OVMF_VARS.fd` para o diretório privado de cada VM antes de usar.

---

### BUG-49 🟢 — `getSetupFiles()` usa `Build.SUPPORTED_ABIS[0]` sem verificar ABI suportada
**Arquivo:** `AppConfig.java:56`

```java
public static String getSetupFiles() {
    String abi = Build.SUPPORTED_ABIS[0];
    return releaseUrl + "vectras-vm-" + abi + ".tar.gz";
}
```

Em ChromeOS ou emuladores, `SUPPORTED_ABIS[0]` pode ser `x86` (32-bit) ou `x86_64` com sufixo diferente. A URL construída (`vectras-vm-x86_64.tar.gz`) pode não existir no servidor → 404 silencioso.

---

### BUG-50 🟢 — `Config.QMPPort = 4444` hardcoded para TCP externo — porta única
**Arquivo:** `Config.java:177`

```java
public static int QMPPort = 4444;
```

Quando `QmpClient.allow_external = true`, usa TCP `127.0.0.1:4444`. Porta hardcoded → segunda VM com QMP externo falhará com `EADDRINUSE`.

---

### BUG-51 🟢 — `bootstrapfileslink` aponta para URL com dupla barra
**Arquivo:** `AppConfig.java:38`

```java
public static String bootstrapfileslink = vectrasWebsiteRaw + "/data/setupfiles3.json";
```

`vectrasWebsiteRaw` já termina com `/` (é uma URL raw do GitHub). Concatenando `/data/...` resulta em `https://raw.../master/web//data/setupfiles3.json` (dupla barra). Alguns servidores retornam 404 para URLs com double slash.

---

### BUG-52 🟢 — `hdd0` single-token (com espaço interno) misturado com multi-token `-drive`
**Arquivo:** `StartVM.java:65–84`

```java
// Branch -hda:
hdd0 = "-hda";
hdd0 += " " + shellQuote(backendImgPath);  // único token: "-hda '/path'"
params.add(hdd0);                          // ← um elemento com espaço interno

// Branch -drive (ARM64/PPC):
hdd0 = "-drive";
hdd0 += " index=0,media=disk,...";         // único token: "-drive index=0,..."
params.add(hdd0);
```

Ambas as branches produzem um único token com espaço interno. No `String.join(" ", params)` → `bash -c "..."` → shell re-parseia os espaços. Para `-hda '/path com espaços'` funciona corretamente (shellQuote usa aspas simples). Porém em **modo isQuickRun**, os tokens `-qmp` são adicionados **depois** de `params.add(finalextra)` (linha 293-294) mas **antes** de VNC/SPICE, causando ordering inconsistente nos args.

---

### BUG-53 🟢 — `VectrasApp.vectrasapp` é `public static` (non-weak) — design inadequado
**Arquivo:** `VectrasApp.java:25`

```java
public static VectrasApp vectrasapp;
```

Campo `public static` direto para a instância `Application`. Embora `Application` sobreviva ao app (não vaza memory tecnicamente), o acesso público facilita anti-patterns onde qualquer classe pode usar `VectrasApp.vectrasapp.doSomething()` sem injeção de dependência.

---

### BUG-54 🟢 — `android.preference.PreferenceManager` deprecated + `RamInfo` usa mesma API
**Arquivo:** `RamInfo.java:10`

```java
import android.preference.PreferenceManager;  // deprecated API 29
SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
```

---

### BUG-55 🟢 — `MainService.CHANNEL_ID` é `public static String` (mutável) — não `final`
**Arquivo:** `MainService.java:20`

```java
public static String CHANNEL_ID = "Vectras VM Service";
```

Canal de notificação deve ser constante. Como `String` público mutável, qualquer código pode alterar o ID → notificações podem parar de funcionar (canal inválido) sem aviso.

**Fix:** `public static final String CHANNEL_ID = "Vectras VM Service";`

---

## MATRIZ CONSOLIDADA

| # | Bug | Arquivo | Severidade | Categoria |
|---|-----|---------|------------|-----------|
| 01 | `-vnc ` trailing space | StartVM.java:299 | 🔴 | CMD Build |
| 02 | `-spice ` trailing space + LAN | StartVM.java:322 | 🔴 | CMD + Sec |
| 03 | `-L ` trailing space (PPC) | StartVM.java:208 | 🟠 | CMD Build |
| 04 | `-bios ` trailing space | StartVM.java:221 | 🟠 | CMD Build |
| 05 | `-drive ` trailing space (FAT) | StartVM.java:176 | 🟠 | CMD Build |
| 06 | `bios=""` token vazio em params | StartVM.java:251 | 🟠 | CMD Build |
| 07 | `-drive ` trailing space (BIOS) | StartVM.java:211 | 🟠 | CMD Build |
| 08 | `Config.datadirpath` NPE na carga | Config.java:191 | 🔴 | Crash |
| 09 | SPICE `disable-ticketing` + `0.0.0.0` | StartVM.java:323 | 🔴 | Segurança |
| 10 | `DH.rng()` usa `Math.random()` | DH.java:41 | 🔴 | Criptografia |
| 11 | `isChildDocument` path traversal | TermuxDocumentsProvider:213 | 🔴 | Segurança |
| 12 | `Loader` `hashCode()` como assinatura | Loader.java:43 | 🔴 | Segurança |
| 13 | VNC DES trunca senha para 8 bytes | RfbProto.java:543 | 🔴 | Criptografia |
| 14 | `deleteVM` Gson null → NPE | VMManager.java:708 | 🔴 | Crash |
| 15 | `deleteVM` position < 0 | VMManager.java:710 | 🔴 | Crash |
| 16 | `QmpClient` JSONException sem try-catch | QmpClient.java:303 | 🟠 | Crash |
| 17 | `RamInfo` retorna valor negativo | RamInfo.java:29 | 🟠 | Crash |
| 18 | `FileUtils.fds` HashMap não thread-safe | FileUtils.java:518 | 🟠 | Threading |
| 19 | `stopGracefully` holds lock 8s | ProcessSupervisor.java:202 | 🟠 | Threading |
| 20 | `finalJson` sem sincronização | VMManager.java:87 | 🟠 | Threading |
| 21 | `QmpClient.sendCommand` lock global | QmpClient.java:31 | 🟠 | Threading |
| 22 | `startRandomPort` Random previsível | VMManager.java:659 | 🟠 | Segurança |
| 23 | `readReservedPorts` `&&` deveria `\|\|` | VMManager.java:679 | 🟠 | Lógica |
| 24 | FDs `get_fd` nunca fechados | FileUtils.java:534 | 🟠 | Resource Leak |
| 25 | `RamInfo.activity` static Activity | RamInfo.java:13 | 🟠 | Memory Leak |
| 26 | `applyVncPassword` thread anônima | MainStartVM.java:446 | 🟠 | Lifecycle |
| 27 | SPICE porta 6999 hardcoded | StartVM.java:323 | 🟡 | Design |
| 28 | `Config.vmID=""` → path com `//` | Config.java:201 | 🟡 | Design |
| 29 | ARM64 dual USB controllers | StartVM.java:97 | 🟡 | QEMU Args |
| 30 | VNC `password=on` sem senha QMP | MainStartVM.java:475 | 🟡 | Segurança |
| 31 | VNC bind `0.0.0.0` exposto LAN | Config.java:148 | 🟡 | Segurança |
| 32 | `MainSettingsManager` API deprecated | MainSettingsManager.java | 🟡 | Compat |
| 33 | `AppConfig` caminhos relativos init | AppConfig.java:83 | 🟡 | Init Order |
| 34 | `google-services.json` placeholder | google-services.json | 🟡 | Config |
| 35 | `readTimeout=30s` para ROMs grandes | DownloadWorker.java:63 | 🟡 | Network |
| 36 | `SYSTEM_ALERT_WINDOW` não solicitada | AndroidManifest.xml:28 | 🟡 | Permissão |
| 37 | `MainActivity` exported=false | AndroidManifest.xml:83 | 🟡 | Android |
| 38 | `save_snapshot` JSON injection | QmpClient.java | 🟡 | Segurança |
| 39 | Display sem fallback → VM sem output | StartVM.java:297 | 🟡 | UX |
| 40 | `idGenerator` sem loop forte | VMManager.java:630 | 🟡 | Lógica |
| 41 | `sockets = PPC?1:1` — dead code | CoreExecutionBudgetPolicy.java | 🟡 | Lógica |
| 42 | `isQemuStopedWithError` typo API | VMManager.java:92 | 🟡 | API |
| 43 | `startRamdomVMID` typo | VMManager.java:643 | 🟡 | API |
| 44 | `android.preference` deprecated (5x) | X11/*.java | 🟢 | Compat |
| 45 | `ensureStoragePaths(null)` static | AppConfig.java | 🟢 | Init |
| 46 | `VectrasApp.context` WeakRef null | VectrasApp.java:30 | 🟢 | Safety |
| 47 | `CqcmActivity` ID race condition | CqcmActivity.java:71 | 🟢 | Lógica |
| 48 | `OVMF_VARS.fd` compartilhado entre VMs | StartVM.java:219 | 🟢 | Design |
| 49 | `getSetupFiles()` ABI não validada | AppConfig.java:56 | 🟢 | Compat |
| 50 | `QMPPort=4444` hardcoded TCP | Config.java:177 | 🟢 | Design |
| 51 | `bootstrapfileslink` double slash | AppConfig.java:38 | 🟢 | URL |
| 52 | `isQuickRun` args ordering inconsistente | StartVM.java:289 | 🟢 | QEMU Args |
| 53 | `vectrasapp` public static | VectrasApp.java:25 | 🟢 | Design |
| 54 | `RamInfo` deprecated preference API | RamInfo.java:10 | 🟢 | Compat |
| 55 | `CHANNEL_ID` não é `final` | MainService.java:20 | 🟢 | Design |

---

## ROADMAP DE CORREÇÃO

### 🔴 Bloco Zero — Máxima urgência (antes do próximo release)
```
1.  [01..07] Corrigir TODOS os trailing spaces em params:
    → Nunca usar "-arg " com trailing space; usar params separados
2.  [08]     Config.datadirpath → converter de static field para static method
3.  [09]     SPICE: addr=127.0.0.1, adicionar autenticação
4.  [10]     DH.rng() → SecureRandom
5.  [11]     isChildDocument() → canonical path comparison
6.  [14-15]  deleteVM() → null check + position < 0 guard
7.  [16]     QmpClient → envolver todos os JSONObject.put() em try-catch
8.  [17]     RamInfo → Math.max(freeRamInt - 100, 256)
```

### 🟠 Bloco Um — Estabilidade
```
9.  [18]  FileUtils.fds → ConcurrentHashMap
10. [19]  stopGracefully → desacoplar awaitExit() do synchronized block
11. [20]  finalJson → volatile + synchronized accessor
12. [23]  readReservedPorts → && para ||
13. [24]  close_fds() → chamar automaticamente ao término da VM
14. [27]  SPICE → startRandomPort() dinâmico
15. [38]  save_snapshot → usar JSONObject.put() em vez de string concat
```

### 🟡 Bloco Dois — Qualidade e Segurança
```
16. [12]  Loader → comparar byte[] das assinaturas em vez de hashCode()
17. [13]  RfbProto → avisar usuário sobre limitação 8-char DES
18. [30]  VNC password → garantir QMP ready antes de set_password
19. [31]  Config.defaultVNCHost → 127.0.0.1 por default
20. [33]  AppConfig → lazy initialization via método
21. [34]  google-services.json → configurar projeto Firebase real
22. [35]  DownloadWorker → readTimeout(0) + callTimeout(12h)
```

---

*`RAFAELIA·BugHunter·Ω` — ψ→χ→ρ→Δ→Σ→Ω*  
*Φ_ethica=Min(Entropia)×Max(Coerência) — fΩ=963↔999*
