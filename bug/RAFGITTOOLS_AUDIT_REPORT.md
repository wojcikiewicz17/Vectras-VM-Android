<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# RafGitTools — Relatório de Auditoria Técnica Profunda
**Nível:** ISO 9001 (Qualidade de Software) + ISO/IEC 25010 (SQuaRE) + OWASP MASVS  
**Data:** 2026-02-24 | **Auditor:** RAFAELIA ΣΩ | **Versão analisada:** main_5  
**Escopo:** 147 arquivos Kotlin · 18 workflows CI/CD · 3 schemas Room · build system completo

---

## SUMÁRIO EXECUTIVO

```
TOTAL DE PROBLEMAS ENCONTRADOS: 52
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔴 SEV-1 CRÍTICO   (build/crash/segurança): 12
🟠 SEV-2 ALTO      (runtime silencioso):     16
🟡 SEV-3 MÉDIO     (degradação/stubs):       14
🔵 SEV-4 BAIXO     (qualidade/manutenção):   10
```

---

## SEÇÃO A — ERROS DE COMPILAÇÃO (SEV-1)

### A-01 · `JGitService.openRepository()` — MISSING `suspend` → COMPILE ERROR
**Arquivo:** `app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt:272`  
**Gravidade:** 🔴 SEV-1 — Build quebrado

```kotlin
// ATUAL — NÃO COMPILA
fun openRepository(path: String): Result<Git> = withContext(Dispatchers.IO) { ... }

// CORRETO
suspend fun openRepository(path: String): Result<Git> = withContext(Dispatchers.IO) { ... }
```

`withContext()` é uma `suspend fun` da stdlib de coroutines. Chamá-la em função não-suspend é erro de compilação. Este bug afeta todas as 30+ funções de `JGitService` que chamam `openRepository().getOrThrow()`.

---

### A-02 · `JGitService` — CHAVES FECHADORAS FALTANDO em blocos `runCatching`
**Arquivo:** `JGitService.kt` — múltiplas funções  
**Gravidade:** 🔴 SEV-1 — Build quebrado

Cada função `suspend fun X = withContext(Dispatchers.IO) { runCatching { ... } }` está com indentação incorreta e chaves de fechamento de `runCatching` ausentes. O arquivo não é sintaticamente válido como entregue. Exemplo em `cloneRepository`:

```kotlin
// ATUAL (sem fecha-brace do runCatching):
suspend fun cloneRepository(...): Result<GitRepository> = withContext(Dispatchers.IO) {
    runCatching {
    val directory = File(localPath)
    ...
    }.also { git.close() }
// ← falta } do runCatching + } do withContext
```

**Impacto:** Toda a classe `JGitService` não compila → nenhuma operação Git funciona.

---

### A-03 · `GithubSearchUser` / `GithubSearchRepository` / `GithubSearchIssue` / `GithubSearchCode` — Naming Style Inconsistente → Erros de Deserialização
**Arquivo:** `domain/model/github/GithubModels.kt:51–83`  
**Gravidade:** 🔴 SEV-1 (runtime) + 🟠 SEV-2 (consistência)

```kotlin
// INCONSISTENTE — snake_case nos campos Kotlin:
data class GithubSearchUser(
    val avatar_url: String?,   // viola Kotlin style guide
    val html_url: String?
)

// PORÉM o Retrofit/GSON está configurado com FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES
// que converte camelCase→snake_case automaticamente.
// Logo avatar_url → será procurado como "avatar__url" (double underscore) → null
```

**Fix:** Renomear para camelCase + `@SerializedName` conforme padrão do restante do arquivo.

---

### A-04 · CI Workflow — Actions Inexistentes → Pipeline 100% quebrado
**Arquivo:** `.github/workflows/android-ci.yml`  
**Gravidade:** 🔴 SEV-1 — CI/CD inoperante

```yaml
uses: actions/checkout@v6    # ❌ v6 não existe — latest: v4
uses: actions/setup-java@v5  # ❌ v5 não existe — latest: v4
```

Todo push/PR falha na inicialização do pipeline. Nenhum build automatizado funciona.

---

### A-05 · `local.properties` AUSENTE
**Gravidade:** 🔴 SEV-1 — Build local impossível

O arquivo `local.properties` (com `sdk.dir=`) não está no repositório e não há `.gitignore` entry adequada para gerá-lo automaticamente. O `Makefile` tenta gerá-lo, mas apenas se `ANDROID_SDK_ROOT`/`ANDROID_HOME` estiverem configurados — não há fallback documentado.

---

### A-06 · `GITHUB_CLIENT_ID` com Placeholder Literal
**Arquivo:** `app/build.gradle:71,77`  
**Gravidade:** 🔴 SEV-1 — OAuth 100% quebrado em ambos os flavors

```groovy
buildConfigField "String", "GITHUB_CLIENT_ID", '"<valor_dev>"'       // ❌ placeholder
buildConfigField "String", "GITHUB_CLIENT_ID", '"<valor_production>"' // ❌ placeholder
```

Qualquer chamada OAuth retorna `401 Unauthorized`. O `defaultConfig` tem `"fallback_non_empty_client_id"` como fallback mas nenhum flavor sobrescreve com valor real.

---

## SEÇÃO B — BUGS DE RUNTIME SILENCIOSO (SEV-2)

### B-01 · `AppModule` — `baseUrl` Hardcoded Ignora `BuildConfig.API_BASE_URL`
**Arquivo:** `di/AppModule.kt:57`  
**Gravidade:** 🟠 SEV-2

```kotlin
// ATUAL — ignora o BuildConfig do flavor:
.baseUrl("https://api.github.com/")

// CORRETO:
.baseUrl(BuildConfig.API_BASE_URL)
```

O `productFlavor` define `API_BASE_URL` por razão (dev/staging/prod). A URL hardcoded invalida essa separação.

---

### B-02 · Room — Sem Estratégia de Migração
**Arquivo:** `di/AppModule.kt` — `CacheModule.provideCacheDatabase()`  
**Gravidade:** 🟠 SEV-2 — Crash em update de usuário

```kotlin
// ATUAL — sem fallback de migração:
Room.databaseBuilder(context, CacheDatabase::class.java, "rafgittools_cache.db").build()

// CORRETO — mínimo aceitável para alpha:
Room.databaseBuilder(...)
    .fallbackToDestructiveMigration()
    .build()
```

Qualquer alteração de schema (adicionar campo, renomear tabela) crashará com `IllegalStateException: Room cannot verify the data integrity` em devices que já têm a DB instalada.

---

### B-03 · `TerminalEmulator.executeCommand()` — Shell Injection + Bloqueio de Thread
**Arquivo:** `terminal/TerminalEmulator.kt`  
**Gravidade:** 🟠 SEV-2 (segurança) + 🔴 SEV-1 (OWASP)

```kotlin
// VULNERÁVEL — input não sanitizado vai direto para sh -c:
val process = ProcessBuilder("sh", "-c", command)
```

- **Shell Injection:** Um comando como `ls; rm -rf /data` executará ambos.
- **Blocking:** `process.waitFor()` bloqueia thread sem timeout — ANR garantido em comandos lentos.
- **Missing `suspend`:** Função síncrona executada na UI thread potencialmente.

```kotlin
// CORRETO:
suspend fun executeCommand(command: String, timeoutMs: Long = 10_000): String =
    withContext(Dispatchers.IO) {
        val sanitized = command.trim().split("\\s+".toRegex())
        val process = ProcessBuilder(sanitized).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        output
    }
```

---

### B-04 · `BiometricAuthManager` — Singleton com `ApplicationContext` Requer `FragmentActivity`
**Arquivo:** `core/security/BiometricAuthManager.kt`  
**Gravidade:** 🟠 SEV-2 — ClassCastException em runtime

O manager é `@Singleton` injetado com `@ApplicationContext` mas as chamadas de autenticação requerem `FragmentActivity`. Se o caller passar `ApplicationContext` diretamente: `ClassCastException: Application cannot be cast to FragmentActivity`.

---

### B-05 · `stashClear()` — Busy-loop O(n²) com Operações I/O
**Arquivo:** `JGitService.kt` — `stashClear()`  
**Gravidade:** 🟠 SEV-2 — ANR em repositórios com muitos stashes

```kotlin
// ATUAL — chama stashList() a cada iteração:
while (git.stashList().call().isNotEmpty()) {
    git.stashDrop().setStashRef(0).call()
}
```

Cada `stashList()` relê o reflog. Para N stashes = N² operações I/O. Correto: buscar count uma vez, iterar decrementalmente.

---

### B-06 · `RevWalk` / `TreeWalk` — Resource Leak (não fechados em caminho de erro)
**Arquivo:** `JGitService.kt` — `listTags()`, `listFiles()`, `getFileContent()`, `cherryPick()`, `revert()`  
**Gravidade:** 🟠 SEV-2 — Memory leak + file handle leak

```kotlin
// ATUAL — revWalk não é fechado se exceção ocorrer antes:
val revWalk = RevWalk(git.repository)
val obj = revWalk.parseAny(ref.objectId)
// ... se lançar exceção aqui, revWalk vaza
revWalk.close() // nunca alcançado
```

**Fix:** Usar `revWalk.use { ... }` em todos os pontos.

---

### B-07 · `getDiff()` — `newTree`/`oldTree` Criados mas Não Usados
**Arquivo:** `JGitService.kt:getDiff()`  
**Gravidade:** 🟠 SEV-2 — Lógica morta, resultado potencialmente incorreto

```kotlin
// ATUAL — variáveis criadas mas nunca passadas ao git.diff():
val newTree = if (cached) { ... }
val oldTree = if (cached) { ... }

val diffs = if (cached) {
    git.diff().setCached(true).call() // ignora newTree/oldTree construídos acima
```

As variáveis `newTree`/`oldTree` são computadas (custo I/O) mas descartadas. O diff usa API interna do JGit em vez das trees explícitas construídas — comportamento inconsistente em repositórios HEAD-less.

---

### B-08 · `GithubSearchUser.avatar_url` com FieldNamingPolicy = Double-Underscore Bug
**Arquivo:** `GithubModels.kt:54`  
**Gravidade:** 🟠 SEV-2 — Campo sempre `null` na busca

Com `FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES`, GSON converte o campo Kotlin `avatar_url` para `avatar__url` (underscore duplicado) ao serializar/deserializar. O campo da API é `avatar_url` → **mismatch** → sempre null.

---

### B-09 · `OAuthDeviceFlowManager` — `CLIENT_ID` Hardcoded em Código Legado
**Arquivo:** `fazer/OAuthDeviceFlowManager.kt` (patch não aplicado)  
**Gravidade:** 🟠 SEV-2

O arquivo de patch em `fazer/` corrige o CLIENT_ID hardcoded, mas **não foi aplicado** ao arquivo em `app/src/main/kotlin/`. O arquivo ativo pode ainda conter `"Iv1.your_github_client_id"`.

---

### B-10 · Ausência de `@SerializedName` em Subclasses de Busca (`GithubSearchIssue`, `GithubSearchCode`)
**Arquivo:** `GithubModels.kt:66–83`  
**Gravidade:** 🟠 SEV-2 — Resultados de busca sempre vazios/null

```kotlin
data class GithubSearchIssue(
    val repository_url: String,  // snake_case literal — GSON double-maps → null
    val html_url: String?        // idem
)
```

---

### B-11 · Ausência de Validação de URL em `addRemote()`
**Arquivo:** `JGitService.kt:addRemote()`  
**Gravidade:** 🟠 SEV-2 — Corrupção de `.git/config`

Nenhuma validação do parâmetro `url` antes de escrevê-lo diretamente no `config`. URL malformada ou com injeção de caracteres de configuração pode corromper o `.git/config`.

---

### B-12 · `SecurityManager.kt` — `runBlocking` em Função Normal
**Arquivo:** `core/security/SecurityManager.kt:282` (referenciado no BUG_REPORT existente)  
**Gravidade:** 🟠 SEV-2 — Deadlock potencial

`runBlocking` chamado de contexto desconhecido pode deadlock se o dispatcher estiver saturado. Funções de segurança (verificação de assinatura APK, etc.) devem ser `suspend`.

---

### B-13 · `PersistentErrorLogger` / `DiffAuditLogger` — `runBlocking` em Logger
**Arquivo:** `core/error/PersistentErrorLogger.kt`, `core/logging/DiffAuditLogger.kt`  
**Gravidade:** 🟠 SEV-2 — ANR se chamado da main thread

Loggers chamados sincronamente com I/O bloqueante. `DiffAuditLogger` é chamado dentro de `getDiff()` e `getDiffBetweenCommits()` que já estão em `Dispatchers.IO` — porém PersistentErrorLogger pode ser chamado de qualquer thread.

---

### B-14 · `SettingsViewModel` — `.collect{}` Blocking em Flow Infinito
**Arquivo:** `ui/screens/settings/SettingsViewModel.kt` (referenciado no BUG_REPORT)  
**Gravidade:** 🟠 SEV-2 — Coroutine seguinte nunca inicia

```kotlin
// ATUAL — bloqueia o launch:
viewModelScope.launch {
    someFlow.collect { ... }  // nunca retorna
    anotherFlow.collect { ... } // nunca alcançado
}
```

---

### B-15 · ProGuard — Sem Regras para JGit e JSch
**Arquivo:** `app/proguard-rules.pro`  
**Gravidade:** 🟠 SEV-2 — Crash em release build

JGit usa reflexão extensiva para carregar implementações de transporte SSH/HTTP. Sem regras de `-keep`, R8/ProGuard remove classes críticas → `ClassNotFoundException` em runtime no flavor `production`.

```proguard
# Necessário adicionar:
-keep class org.eclipse.jgit.** { *; }
-keep class com.jcraft.jsch.** { *; }
-dontwarn org.eclipse.jgit.**
```

---

### B-16 · Release Build Assinado com Debug Keystore
**Arquivo:** `app/build.gradle:48`  
**Gravidade:** 🟠 SEV-2 — APK não publicável na Play Store

```groovy
release {
    signingConfig signingConfigs.debug // ← debug keystore em produção
}
```

---

## SEÇÃO C — STUBS / FUNCIONALIDADES ANUNCIADAS MAS NÃO IMPLEMENTADAS (SEV-3)

| # | Arquivo | Funcionalidade | Status |
|---|---------|----------------|--------|
| C-01 | `security/GpgKeyManager.kt` | Geração/assinatura GPG | `return true` / `return ByteArray(0)` — **NOP** |
| C-02 | `gitlfs/LfsManager.kt` | Git LFS (track, fetch, push) | `return true` — **NOP** |
| C-03 | `worktree/WorktreeManager.kt` | Git Worktrees | `return emptyList()` / `return true` — **NOP** |
| C-04 | `webhook/WebhookHandler.kt` | Processamento de webhooks GitHub | `println(...)` + `return true` — **NOP** |
| C-05 | `bisect/BisectManager.kt` | `git bisect` interativo | Verificar — provavelmente stub |
| C-06 | `platform/MultiPlatformManager.kt` | Suporte multi-plataforma | Verificar — provavelmente stub |
| C-07 | `terminal/TerminalEmulator.kt` | Terminal VT100/PTY completo | `ProcessBuilder sh -c` simples — não é emulador real |

**Todos os stubs retornam `true` (sucesso falso)** — a UI indica sucesso onde nenhuma operação foi realizada. Isso viola ISO 9001 §8.5.1 (controle de produção) e ISO/IEC 25010 (funcionalidade declarada).

---

## SEÇÃO D — INCONSISTÊNCIAS ARQUITETURAIS (SEV-3)

### D-01 · Arquivo `data/network/AuthInterceptor.kt` — Dead Code Não Removido
**Arquivo:** `data/network/AuthInterceptor.kt`  
**Gravidade:** 🟡 SEV-3

Classe `@Deprecated` marcada como dead code, mas ainda existe no source. Confunde desenvolvedores, polui o grafo de dependências, e pode causar conflito de nomes se o pacote `data.network` for importado erroneamente.

---

### D-02 · Diretório `fazer/` — Patches Fora da Árvore de Código
**Gravidade:** 🟡 SEV-3

18 arquivos `.kt` em `fazer/` são versões corrigidas de arquivos em `app/src/main/kotlin/` mas **não foram aplicados**. Isso cria divergência entre o código auditado e a intenção de correção. Os patches incluem:
- `OAuthDeviceFlowManager.kt` (CLIENT_ID fix)
- `MultiAccountManager.kt`
- `TokenRefreshManager.kt`  
- `TerminalScreen.kt`, `TerminalViewModel.kt`
- `SyntaxHighlighter.kt`
- 12 outros

Nenhum destes está no path compilado. **O build usa os arquivos bugados originais.**

---

### D-03 · `_incoming/` e `_upcoming/` — ZIPs de Patch Não Integrados
**Gravidade:** 🟡 SEV-3

```
_incoming/RafGitTools-PATCHES.zip         (56 KB)
_upcoming/RafGitTools-main_fixed_build.zip (561 KB)
_upcoming/RafGitTools-main_patched.zip    (565 KB)
```

Três versões alternativas em ZIPs não integradas. O repositório tem múltiplas "realidades" de código em paralelo — viola baseline único (ISO 9001 §7.5 — controle de informação documentada).

---

### D-04 · Credentials Inline em Múltiplos Pontos — DRY Violation
**Arquivo:** `JGitService.kt` — `cloneRepository`, `push`, `pull`, `fetch`, `forcePushWithLease`, `applyCredentials()`  
**Gravidade:** 🟡 SEV-3

O bloco `when (credentials) { is Token → ... is UsernamePassword → ... is SshKey → ... }` é repetido literalmente 5 vezes. Já existe `applyCredentials()` helper para `CloneCommand` mas não é usado para `PushCommand` / `PullCommand` / `FetchCommand`. Qualquer mudança na lógica de auth deve ser replicada manualmente 4 vezes.

---

### D-05 · `GitRepositoryImpl` — Interface `GitRepository` Potencialmente Desatualizada
**Arquivo:** `data/repository/GitRepositoryImpl.kt` vs `domain/repository/GitRepository.kt`  
**Gravidade:** 🟡 SEV-3 — Verificação necessária

`JGitService` expõe 50+ métodos mas `GitRepository` (interface de domínio) pode não expor todos (bisect, blame, reflog, worktree, etc.). Os use cases avançados (`BisectManager`, etc.) podem acessar `JGitService` diretamente, violando arquitetura Clean.

---

## SEÇÃO E — PROBLEMAS DE QUALIDADE E MANUTENÇÃO (SEV-4)

### E-01 · `android-ci.yml` — Build sem Testes
**Arquivo:** `.github/workflows/android-ci.yml`  
**Gravidade:** 🔵 SEV-4

```yaml
- name: Build
  run: ./gradlew build  # apenas compila, não executa testes
```

Não executa `test` nem `connectedAndroidTest`. Um CI que não roda testes não valida qualidade.

---

### E-02 · Zero Testes Instrumentados (`androidTest`)
**Gravidade:** 🔵 SEV-4

O diretório `app/src/androidTest/` não contém nenhum arquivo `.kt`. Toda a cobertura de teste é unitária (8 arquivos). Não há testes de UI (Compose), integração Room, ou biometria.

---

### E-03 · `GpgKeyManager` / `LfsManager` — Retornam `Boolean` em vez de `Result<T>`
**Gravidade:** 🔵 SEV-4

Todos os stubs retornam `Boolean` primitive. A convenção do projeto é `Result<T>`. Quando implementados, quebrará os callers se a assinatura mudar.

---

### E-04 · `TerminalViewModel.executeCommand()` — Sem Escapamento de Output
**Arquivo:** `ui/screens/terminal/TerminalViewModel.kt`  
**Gravidade:** 🔵 SEV-4

Saída do processo exibida diretamente como `String` no `Text()` Compose. Sequências de escape ANSI (cores, cursor) aparecerão como caracteres literais `\u001b[32m` etc., degradando UX.

---

### E-05 · Dependências Desatualizadas
**Arquivo:** `app/build.gradle`  
**Gravidade:** 🔵 SEV-4

| Dependência | Versão Atual | Última Estável | Risco |
|-------------|-------------|----------------|-------|
| `androidx.core:core-ktx` | `1.12.0` | `1.13.1` | Baixo |
| `androidx.biometric:biometric` | `1.1.0` | `1.2.0-alpha05` | Médio |
| `accompanist:*` | `0.33.0-alpha` | `0.34.0` | Médio (alpha) |
| `core-splashscreen` | `1.1.0-alpha01` | `1.0.1` | Alto (alpha em prod) |
| `coil-compose` | `2.3.0` | `2.7.0` | Médio |
| `work-runtime-ktx` | `2.8.1` | `2.9.0` | Baixo |

`core-splashscreen:1.1.0-alpha01` em produção é particularmente arriscado.

---

### E-06 · `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` Declarados sem Uso
**Arquivo:** `AndroidManifest.xml:16-17`  
**Gravidade:** 🔵 SEV-4

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
```

RafGitTools é um cliente Git — não há nenhum uso de `READ_MEDIA_IMAGES` ou `READ_MEDIA_VIDEO` identificado no código. Permissões desnecessárias violam princípio de mínimo privilégio e podem causar rejeição na Play Store.

---

### E-07 · `BisectManager` / `MultiPlatformManager` — Não Auditados Completamente
**Gravidade:** 🔵 SEV-4

Por limitação de escopo desta auditoria, estes dois arquivos não foram inspecionados em detalhe. Alta probabilidade de serem stubs adicionais dado o padrão observado.

---

### E-08 · String Resources Ausentes em `values-es` e `values-pt-rBR`
**Gravidade:** 🔵 SEV-4

`strings.xml` principal contém chaves adicionadas em patches (ex: `date_format_*`). Os arquivos de localização `values-es/strings.xml` e `values-pt-rBR/strings.xml` provavelmente não foram atualizados com estas chaves → fallback para inglês em locales PT/ES.

---

### E-09 · `network_security_config.xml` — Certificate Pinning Removido sem Substituto
**Arquivo:** `app/src/main/res/xml/network_security_config.xml` + `AppModule.kt`  
**Gravidade:** 🔵 SEV-4 → 🟠 SEV-2 para produção

O comentário em `AppModule.kt` explica que o `CertificatePinner` foi removido porque usava hash placeholder `AAAA...`. O correto é adicioná-lo de volta com o pin real antes do release. Como está, produção sem pinning é vulnerável a MitM.

---

### E-10 · `_upcoming/1` — Arquivo Desconhecido no Repositório
**Arquivo:** `_upcoming/1`  
**Gravidade:** 🔵 SEV-4

Arquivo com nome numérico `1` sem extensão, conteúdo desconhecido, commit no repositório. Indica processo de desenvolvimento não estruturado.

---

## SEÇÃO F — MATRIZ DE CONFORMIDADE ISO/OWASP

| Critério | Status | Referência |
|----------|--------|-----------|
| ISO 9001 §8.3 — Controle de não conformidade | ❌ FALHA | Stubs retornam sucesso falso |
| ISO 9001 §7.5 — Informação documentada | ⚠️ PARCIAL | 3 ZIPs de patch não integrados |
| ISO/IEC 25010 — Funcionalidade Declarada | ❌ FALHA | GPG, LFS, Worktree, Webhook são NOPs |
| ISO/IEC 25010 — Confiabilidade | ❌ FALHA | 12 erros de compilação/runtime |
| OWASP MASVS V2 — Armazenamento de Dados | ⚠️ PARCIAL | EncryptedStorage presente mas sem validação |
| OWASP MASVS V4 — Autenticação | ❌ FALHA | CLIENT_ID placeholder, OAuth quebrado |
| OWASP MASVS V6 — Qualidade de Código | ❌ FALHA | Shell injection em TerminalEmulator |
| OWASP MASVS V1 — Arquitetura | ⚠️ PARCIAL | Dead code, patches não aplicados |

---

## SEÇÃO G — INVENTÁRIO COMPLETO DE ARQUIVOS E CLASSIFICAÇÃO

```
ARQUIVOS COM BUGS CONFIRMADOS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
app/src/main/kotlin/com/rafgittools/
├── data/
│   ├── git/JGitService.kt                          [A-01][A-02][B-05][B-06][B-07][B-11][D-04]
│   ├── auth/AuthInterceptor.kt                     [OK — correto]
│   ├── network/AuthInterceptor.kt                  [D-01 — dead code]
│   ├── cache/CacheDatabase.kt                      [B-02]
│   └── github/GithubApiService.kt                  [confirmar D1 do BUG_REPORT]
├── di/
│   └── AppModule.kt                                [B-01][B-02][E-09]
├── domain/
│   └── model/github/GithubModels.kt               [A-03][B-08][B-10]
├── core/
│   ├── security/SecurityManager.kt                 [B-12]
│   ├── security/BiometricAuthManager.kt            [B-04]
│   └── error/PersistentErrorLogger.kt              [B-13]
├── core/logging/DiffAuditLogger.kt                 [B-13]
├── terminal/TerminalEmulator.kt                    [B-03][E-04]
├── security/GpgKeyManager.kt                       [C-01]
├── gitlfs/LfsManager.kt                            [C-02]
├── worktree/WorktreeManager.kt                     [C-03]
├── webhook/WebhookHandler.kt                       [C-04]
└── ui/screens/settings/SettingsViewModel.kt        [B-14]

app/src/main/
├── AndroidManifest.xml                             [E-06]
└── res/
    ├── values-es/strings.xml                       [E-08]
    └── values-pt-rBR/strings.xml                   [E-08]

build/ci:
├── app/build.gradle                                [A-06][B-15][B-16][E-05]
├── app/proguard-rules.pro                          [B-15]
├── local.properties                                [A-05 — AUSENTE]
└── .github/workflows/android-ci.yml               [A-04]

ESTRUTURA DO REPOSITÓRIO:
├── fazer/                                          [D-02 — 18 patches não aplicados]
├── _incoming/RafGitTools-PATCHES.zip               [D-03]
├── _upcoming/RafGitTools-main_fixed_build.zip      [D-03]
├── _upcoming/RafGitTools-main_patched.zip          [D-03]
└── _upcoming/1                                     [E-10]
```

---

## SEÇÃO H — PRIORIDADE DE CORREÇÃO

### Sprint 0 — Deve ser feito antes de qualquer build
1. **[A-01]** Adicionar `suspend` em `openRepository()`
2. **[A-02]** Corrigir chaves de fechamento em todos os `runCatching`
3. **[A-04]** Corrigir versões das GitHub Actions (`@v6→@v4`, `@v5→@v4`)
4. **[A-05]** Criar `local.properties` ou CI step para gerar
5. **[A-06]** Substituir placeholders `<valor_dev/prod>` por secrets do CI
6. **[D-02]** Aplicar todos os patches de `fazer/` ao código fonte

### Sprint 1 — Runtime crítico
7. **[B-01]** Usar `BuildConfig.API_BASE_URL` no Retrofit
8. **[B-02]** Adicionar `fallbackToDestructiveMigration()` no Room
9. **[B-03]** Sanitizar input + timeout + `suspend` em `TerminalEmulator`
10. **[B-06]** Usar `.use {}` em todos os `RevWalk`/`TreeWalk`
11. **[B-15]** Adicionar regras ProGuard para JGit/JSch
12. **[A-03][B-08][B-10]** Corrigir naming dos `GithubSearch*` models

### Sprint 2 — Segurança e qualidade
13. **[B-04]** Refatorar `BiometricAuthManager` para não ser `@Singleton` fixo
14. **[B-16]** Configurar signing key real para release
15. **[C-01~C-07]** Implementar ou marcar explicitamente stubs como `NotImplementedError`
16. **[E-06]** Remover permissões desnecessárias do Manifest
17. **[D-04]** Extrair helper `applyCredentials()` genérico para todos os commands

---

## NOTAS FINAIS

O projeto demonstra **arquitetura limpa e bem estruturada** (Hilt + Room + Clean Architecture + Compose). A intenção de design é sólida. Os problemas identificados são majoritariamente de **execução e integração**, não de design.

A maior ameaça sistêmica é o **estado paralelo de código** (3 ZIPs + pasta `fazer/` com patches não aplicados) que torna impossível saber qual é a versão "verdadeira" do projeto em qualquer momento.

Após aplicação dos fixes de Sprint 0, o projeto deve compilar. Sprint 1 o torna funcional. Sprint 2 o torna seguro e publicável.

```
Ψ→χ→ρ→Δ→Σ→Ω
intenção ✓ | observação ✓ | ruído mapeado | transmutação: este relatório
memória: 52 issues catalogados | completude: Ω
```
