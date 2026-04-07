<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# API — Bitraf Engine (C)

API pública estável do Produto 1 (Engine), sem exposição de structs internas.

## Headers
- `engine/rmr/include/bitraf.h`
- `engine/rmr/include/bitraf_version.h`

## SemVer
- `BITRAF_VERSION_MAJOR`
- `BITRAF_VERSION_MINOR`
- `BITRAF_VERSION_PATCH`
- `BITRAF_VERSION_STRING`

## Contrato mínimo
```c
int bitraf_init(uint64_t seed);
uint64_t bitraf_hash(const uint8_t *data, size_t len, uint64_t seed);
size_t bitraf_compress(const uint8_t *in, size_t in_len,
                       uint8_t *out, size_t out_cap,
                       uint64_t seed);
size_t bitraf_reconstruct(const uint8_t *in, size_t in_len,
                          uint8_t *out, size_t out_cap,
                          uint64_t seed);
int bitraf_verify(const uint8_t *data, size_t len,
                  uint64_t expected_hash, uint64_t seed);
```

## Exemplo de uso (C)
```c
#include "bitraf.h"
#include <stdint.h>
#include <stdio.h>

int main(void) {
  static const uint8_t payload[] = "RAFAELIA_ENGINE";
  uint8_t packed[256];
  uint8_t restored[256];

  uint64_t seed = 0x123456789ABCDEF0ULL;
  bitraf_init(seed);

  size_t packed_len = bitraf_compress(payload, sizeof(payload)-1, packed, sizeof(packed), seed);
  size_t plain_len = bitraf_reconstruct(packed, packed_len, restored, sizeof(restored), seed);

  uint64_t h = bitraf_hash(restored, plain_len, seed);
  int ok = bitraf_verify(restored, plain_len, h, seed);

  printf("packed=%zu plain=%zu ok=%d\n", packed_len, plain_len, ok);
  return ok ? 0 : 1;
}
```

## Linkagem
### Make
```bash
make all
cc -O3 -Iengine/rmr/include app.c build/engine/libbitraf.a -o app
```

### CMake
```bash
cmake -S . -B build-cmake
cmake --build build-cmake -j
# alvo: bitraf_static (libbitraf.a) e bitraf_shared (libbitraf.so)
```


## API de Supervisão de Processo (Java/Android)

Classes de alto impacto para ciclo de vida de VM e parada determinística:

### `com.vectras.vm.VMManager`
- `registerVmProcess(Context, String, Process)`
  - vincula processo QEMU ao supervisor da VM.
- `stopVmProcess(Context, String, boolean)`
  - aplica parada escalonada (QMP → TERM → KILL) via supervisor.

### `com.vectras.vm.core.ProcessSupervisor`
- `bindProcess(Process)`
  - inicia transições `START -> VERIFY -> RUN`.
- `onDegraded(int, long)`
  - marca estado `DEGRADED` sob flood/backpressure.
- `stopGracefully(boolean)`
  - executa failover determinístico e registra auditoria de transições.

### Estados
`START`, `VERIFY`, `RUN`, `DEGRADED`, `FAILOVER`, `STOP`.

### Garantias operacionais
- transições auditáveis com `AuditLedger`;
- timeouts explícitos em cada etapa de parada;
- compatibilidade com cenário sem QMP (fallback direto para TERM/KILL).


## API de Rede e Governança de Endpoints

Camada para composição e proteção de endpoints HTTP(S) usados no app, com validações em duas fases: sintática e por política de allowlist.

### `com.vectras.vm.network.EndpointFeature`
- **Responsabilidade:** definir capacidades de rede por feature com host(s) e padrão de path permitidos.
- **Contrato:**
  - `isAllowedHost(String normalizedHost)` retorna `true` apenas para host já normalizado e presente no conjunto da feature.
  - `isAllowedPath(String path)` aplica regex da feature (ou aceita qualquer path se regex for `null`).
  - `getAllowedHosts()` expõe conjunto imutável dos hosts aceitos.
  - `getAllowedPathPatternDescription()` retorna regex textual (ou `<any>`).

### `com.vectras.vm.network.NetworkEndpoints`
- **Responsabilidade:** compor URLs canônicas dos fluxos suportados (ROM API, GitHub API/Web, módulos de idioma).
- **Contrato:**
  - gera endpoints com `https://` e hosts constantes (`go.anbui.ovh`, `api.github.com`, `github.com`, `raw.githubusercontent.com`);
  - `languageModuleRaw(languageCode)` normaliza `languageCode` para minúsculas.

### `com.vectras.vm.network.EndpointValidator`
- **Responsabilidade:** validar sintaxe e superfície mínima de segurança de URL.
- **Contrato:**
  - aceita somente `https`;
  - exige host não vazio e presente na allowlist (`DEFAULT_ALLOWLIST` ou lista customizada);
  - rejeita `userinfo` e porta fora de `443`;
  - retorna `false` para URL vazia/malformada.

### `com.vectras.vm.network.EndpointPolicy`
- **Responsabilidade:** aplicar governança por contexto funcional (`Feature`) usando prefixos permitidos.
- **Contrato:**
  - `isAllowedApi` e `isAllowedActionView` retornam `true` quando endpoint começa com prefixo autorizado para a feature;
  - `requireAllowedApi` e `requireAllowedActionView` lançam `IllegalArgumentException` quando endpoint for bloqueado;
  - features sem prefixos registrados são bloqueadas por padrão.

### `com.vectras.vm.localization.NetworkEndpoints`
- **Responsabilidade:** compor endpoint de módulo de idioma na camada de localização (Kotlin).
- **Contrato:** `languageModule(languageCode)` retorna `https://raw.githubusercontent.com/.../resources/lang/<languageCode>.json`.

### `com.vectras.vm.localization.EndpointValidator`
- **Responsabilidade:** validação rápida para endpoints de módulos de idioma na camada de localização.
- **Contrato:**
  - valida protocolo `https`, host preenchido e path com sufixo `.json`;
  - retorna URL normalizada (`String`) quando válida;
  - retorna `null` para entrada inválida ou erro de parse.

### Fluxo recomendado de uso
1. **Composição do endpoint:** usar `NetworkEndpoints` (Java ou Kotlin) para evitar string literal solta.
2. **Validação sintática:** aplicar `EndpointValidator` da camada correspondente.
3. **Validação por política (allowlist):** aplicar `EndpointPolicy`/`EndpointFeature` para o contexto de consumo.
4. **Consumo:** só executar request ou `ACTION_VIEW` após passar nas etapas anteriores.

### Exemplos curtos (permitido / bloqueado)

#### Java — cenário permitido
```java
String endpoint = com.vectras.vm.network.NetworkEndpoints.githubUserApi("octocat");
if (com.vectras.vm.network.EndpointValidator.isAllowed(endpoint)
        && com.vectras.vm.network.EndpointPolicy.isAllowedApi(
                com.vectras.vm.network.EndpointPolicy.Feature.GITHUB_API,
                endpoint)) {
    // consumir endpoint
}
```

#### Java — cenário bloqueado (erro esperado)
```java
String blocked = "https://evil.example/users/octocat";
com.vectras.vm.network.EndpointPolicy.requireAllowedApi(
        com.vectras.vm.network.EndpointPolicy.Feature.GITHUB_API,
        blocked);
// Esperado: IllegalArgumentException("Endpoint blocked by API policy: ...")
```

#### Kotlin — cenário permitido
```kotlin
val endpoint = com.vectras.vm.localization.NetworkEndpoints.languageModule("pt-BR")
val validated = com.vectras.vm.localization.EndpointValidator
    .validateLanguageModuleEndpoint(endpoint)
if (validated != null) {
    // consumir endpoint
}
```

#### Kotlin — cenário bloqueado/erro esperado de validação
```kotlin
val blocked = "http://raw.githubusercontent.com/repo/lang/pt-BR.txt"
val validated = com.vectras.vm.localization.EndpointValidator
    .validateLanguageModuleEndpoint(blocked)
// Esperado: validated == null (protocolo/path inválido)
```

### Rastreabilidade
- Mapa dos arquivos e localização das classes: [`app/FILES_MAP.md`](../app/FILES_MAP.md).
- Diretrizes e controles de segurança operacional: [`docs/SECURITY.md`](SECURITY.md).


## Roadmap imediato
- Ver sequência de execução técnica: `docs/VM_SUPERVISION_NEXT_5_STEPS.md`.
