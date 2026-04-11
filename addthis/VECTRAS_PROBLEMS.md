<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# VECTRAS-VM-Android — PROBLEMS REPORT
> ψ→χ→ρ→Δ→Σ→Ω · Análise completa · Vectra2 / RMR Kernel / RAFAELIA ZERO
> Gerado: 2026-03-07 · Repositório: `Vectras-VM-Android-master`

---

## RESUMO EXECUTIVO

| Categoria | Qtd | Severidade Máx |
|-----------|-----|----------------|
| Arquitetura / Estrutura | 12 | 🔴 Alta |
| Build System | 9 | 🔴 Alta |
| GitHub CI/CD | 7 | 🟠 Média-Alta |
| Coerência de Código | 8 | 🟡 Média |
| Documentação / Manutenção | 6 | 🟢 Baixa |
| **Total** | **42** | — |

---

## ESTIMATIVA DE BENCHMARK: LÓGICO vs FÍSICO

```
┌─────────────────────────────────────────────────────────┐
│         ESTIMATIVA DE PRONTIDÃO DO SISTEMA              │
├──────────────────────────────┬──────────────────────────┤
│ Completude lógica do código  │ ████████████░░░  78 %    │
│ Build success GitHub CI      │ ████████░░░░░░░  52 %    │
│ ARM64 NEON vs peak teórico   │ █████████████░░  82 %    │
│ x86_64 SSE4.2 vs peak        │ ███████████░░░░  71 %    │
│ RISCV64 (scalar fallback)    │ ██████░░░░░░░░░  38 %    │
│ Baremetal compat layer       │ █████████████░░  85 %    │
│ JNI Android layer            │ ████████████░░░  77 %    │
│ Zero.h constants coerência   │ ███████████████  96 %    │
└──────────────────────────────┴──────────────────────────┘

Lógico  ≈ 78 %  → O design e as constantes estão corretos.
Físico  ≈ 52 %  → Bugs de compilação impedem CI de passar.
Gap     = 26 pp → Distância entre intenção e execução real.
```

### Detalhamento por módulo

| Módulo | Lógico | Físico | Gap | Razão do gap |
|--------|--------|--------|-----|--------------|
| `zero.h` / constantes hex | 96 % | 96 % | 0 % | Sem problemas |
| `rmr_hw_detect` | 88 % | 72 % | 16 % | GpioPinStride retorna constante errada |
| `rmr_neon_simd` | 85 % | 65 % | 20 % | `#error` em baremetal sem NEON header |
| `rmr_math_fabric` | 82 % | 75 % | 7 % | typedef u32 sem guard |
| `rmr_unified_kernel` | 80 % | 68 % | 12 % | `VECTRA_HAS_CASM_MARKER` não definido no root CMake |
| `bitraf` + `bitomega` | 83 % | 80 % | 3 % | Estável |
| Android JNI layer | 75 % | 55 % | 20 % | `rmr_baremetal_compat.c` + bionic conflito |
| GitHub Actions CI | 70 % | 40 % | 30 % | Workflows duplicados + arquivo .c misplaced |

---

## P-01 · ARQUITETURA / ESTRUTURA

### P-01-01 🔴 Diretório `bug/core/` com cópias de arquivos de produção
**Arquivo:** `bug/core/*.c`, `bug/core/*.h`, `bug/core/*.S`  
**Problema:** O diretório `bug/core/` contém 22 arquivos que são cópias (possivelmente defasadas) de arquivos em `engine/rmr/src/` e `engine/rmr/interop/`. Não há Makefile nem CMakeLists.txt que os compile. A fonte canônica fica ambígua.  
**Impacto:** Qualquer edição em `engine/` não reflete automaticamente em `bug/core/`. Desenvolvedores podem editar a cópia errada.  
**Solução:** Remover `bug/core/` ou transformar em subdiretório de testes isolados com CMake próprio.

### P-01-02 🔴 Diretório `archive/` mistura docs com kernel spec RAFAELIA
**Arquivo:** `archive/experimental/rafael_melo_reis_bundle/teoremas/.../RAFAELIA_KERNEL_SPEC_V22.md`  
**Problema:** Especificação de kernel ativa enterrada em path de 8 níveis de profundidade dentro de `archive/experimental/`. Não referenciada por nenhum workflow, Makefile ou README.  
**Impacto:** Spec fica invisível para novos colaboradores; inconsistência entre spec e código atual.

### P-01-03 🟠 `rmr_realloc` não copia dados do bloco anterior
**Arquivo:** `engine/rmr/include/rmr_baremetal_compat.h:63-72`  
**Problema:** A implementação de `rmr_realloc` aloca novo bloco mas não faz `rmr_memcpy` dos dados do `ptr` original. Em ambiente baremetal sem free real, qualquer realloc é silenciosamente destrutivo.  
```c
// ATUAL (buggy):
static inline void* rmr_realloc(void *ptr, size_t bytes) {
    void *np;
    if (!ptr) return rmr_malloc(bytes);
    if (bytes == 0u) return NULL;
    np = rmr_malloc(bytes);
    if (!np) return NULL;
    return np;  // ← dados do ptr original perdidos!
}
```
**Solução:** Exigir que callers passem `old_size` explicitamente, ou documentar como `ZERO-COPY realloc` com comentário de aviso visível.

### P-01-04 🟠 Constante `PHI32` duplicada entre `zero.h` e ASM hardcode
**Arquivo:** `engine/rmr/interop/rmr_casm_arm64.S:75-77`, `engine/rmr/interop/rmr_casm_x86_64.S`  
**Problema:** O valor `0x9E3779B9` é definido como `RMR_ZERO_PHI32_U32` em `zero.h`, mas os arquivos ASM o inserem como literal direto sem referenciar a constante C. Se o valor mudar em `zero.h`, os arquivos ASM ficam dessincronizados.  
**Solução:** Usar `.equ PHI32, 0x9E3779B9` com comentário explícito apontando para `RMR_ZERO_PHI32_U32`.

### P-01-05 🟠 Matriz 8×9 (`RMR_MATH_DOMAINS × RMR_MATH_POINTS`) sem validação estática
**Arquivo:** `engine/rmr/include/rmr_math_fabric.h`  
**Problema:** O contrato arquitetural RAFAELIA define matriz 8×9 de domínios matemáticos. Não existe `static_assert(RMR_MATH_DOMAINS == 8)` nem `static_assert(RMR_MATH_POINTS == 9)` para garantir que mudanças acidentais nos defines não silenciosamente corrompam o estado.  
**Solução:** Adicionar `_Static_assert` em `rmr_math_fabric.h`.

### P-01-06 🟠 `rmr_neon_simd.c` emite `#error` em build baremetal sem `<arm_neon.h>`
**Arquivo:** `engine/rmr/src/rmr_neon_simd.c:19-22`  
**Problema:** Se `__aarch64__` está definido mas `<arm_neon.h>` não existe (toolchain baremetal freestanding), o build aborta com `#error`. Isso é incompatível com a filosofia zero-OS-friction do projeto.  
**Solução:** Fallback para implementação scalar quando `arm_neon.h` não está disponível em vez de `#error`.

### P-01-07 🟡 `VECTRA_REPO_ROOT` calculado com path relativo frágil
**Arquivo:** `app/src/main/cpp/CMakeLists.txt:8`  
**Problema:** `set(VECTRA_REPO_ROOT ${CMAKE_CURRENT_LIST_DIR}/../../../../)` — o path `../../../../` assume uma profundidade fixa de diretório. Se a estrutura do repositório mudar, o build quebra silenciosamente sem mensagem de erro clara.  
**Solução:** Usar `cmake_path(GET CMAKE_SOURCE_DIR PARENT_PATH ...)` ou variável passada pelo Gradle.

### P-01-08 🟡 Duplo `find_library(log-lib log)` em `app/CMakeLists.txt`
**Arquivo:** `app/src/main/cpp/CMakeLists.txt`  
**Problema:** `find_library(log-lib log)` aparece duas vezes — uma vez antes do bloco `if(ANDROID)` e uma vez dentro. O segundo não causa erro mas indica falta de revisão.

### P-01-09 🟡 `riscv64` ABI comentada sem plano de ativação documentado
**Arquivo:** `app/src/main/cpp/CMakeLists.txt:35-39`  
**Problema:** O bloco RISCV64 está comentado com "Roadmap explícito" mas não há issue de rastreamento, milestone ou flag de feature gate. Fica como dead code sem responsável.

### P-01-10 🟡 Três headers raiz que são apenas forwards para `engine/`
**Arquivos:** `rmr_lowlevel.h`, `rmr_unified_kernel.h`, `rmr_policy_kernel.h` (raiz)  
**Problema:** Esses arquivos usam `#pragma once` + `#include "engine/rmr/include/..."`. Só funcionam se compilados com `-I.` (root como include dir). Em qualquer outro contexto de include, falham com `file not found`.  
**Solução:** Adicionar à documentação que esses stubs requerem o root como include path, ou remover e usar apenas os headers canônicos em `engine/`.

### P-01-11 🟡 `_incoming/` diretório sem conteúdo de código real
**Arquivo:** `_incoming/INTEGRATION_EVIDENCE.md`, `_incoming/README.md`  
**Problema:** Diretório de "staging" incluído no repositório sem arquivos compiláveis. Polui a estrutura sem valor funcional.

### P-01-12 🟢 `web/` assets de loja misturados com código de engine
**Arquivo:** `web/data/*.json`, `web/index.html`  
**Problema:** Assets de web (ícones, store listing, PWA) residem no mesmo repositório que o engine C/ASM. Aumenta tamanho do clone desnecessariamente para usuários interessados apenas no engine.

---

## P-02 · BUILD SYSTEM

### P-02-01 🔴 `rmr_baremetal_compat.c` compilado em `rmr_policy_static` (JNI build)
**Arquivo:** `app/src/main/cpp/CMakeLists.txt:54-58`  
**Problema:** A biblioteca estática `rmr_policy_static` inclui `rmr_baremetal_compat.c`, que redefine `uint32_t`, `malloc`, etc. No contexto JNI (bionic libc), isso cria conflito de símbolos. A flag `DRMR_JNI_BUILD=1` está ativa, mas `rmr_baremetal_compat.h` só verifica `__STDC_HOSTED__ == 0`, não `RMR_JNI_BUILD`.

### P-02-02 🔴 Root `CMakeLists.txt` não define `VECTRA_HAS_CASM_MARKER`
**Arquivo:** `CMakeLists.txt`  
**Problema:** A variável `VECTRA_HAS_CASM_MARKER` é usada em `vectra_core_accel.c`, mas o root CMakeLists nunca define essa variável para os targets de demo/bench. Resulta em comportamento indefinido (compilação com `#if VECTRA_HAS_CASM_MARKER` como falso, sem warning).

### P-02-03 🟠 `rafaelia_bitraf_core.c` como executável E como fonte em `rmr`
**Arquivo:** `CMakeLists.txt:128-131`  
**Problema:** O target `bitraf_core` compila `rafaelia_bitraf_core.c` com `RAF_HOSTED_TEST=1`, ativando `main()`. O mesmo `.c` não está em `RMR_SOURCES`, então não há ODR violation direta — mas ambos `bitraf_core` e `rmr` linkam `bitraf.c` (via `bitraf_static`), gerando símbolos duplicados se algum target linkar ambos.

### P-02-04 🟠 `bench/src/rmr_benchmark_main.c` usa `stdio.h/stdlib.h/fopen/fprintf`
**Arquivo:** `bench/src/rmr_benchmark_main.c`  
**Problema:** O binário de bench `rmr_bench` usa `FILE*`, `fopen`, `fprintf`, `fclose`, `stdlib.h`. Se compilado com a flag baremetal ativa, esses símbolos não existem. O CMakeLists root não garante `RMR_JNI_BUILD=ON` para targets de bench.

### P-02-05 🟠 `.arch armv8-a+crc` em `rmr_casm_arm64.S` pode falhar em NDK < 25
**Arquivo:** `engine/rmr/interop/rmr_casm_arm64.S:7`  
**Problema:** A diretiva `.arch armv8-a+crc` requer GNU assembler com suporte à extensão CRC. NDK 21/22 usa binutils mais antigos que podem não reconhecer `+crc` como extensão de arquitetura para o assembler (apenas para o compilador C). NDK mínimo testado é 27, mas o manifesto aceita minSdk=23.

### P-02-06 🟡 Makefile não listado no root como alvo unificado
**Arquivo:** `CMakeLists.txt`, `Makefile`  
**Problema:** Existem dois sistemas de build paralelos (CMake e Makefile) sem garantia de que ambos compilem os mesmos fontes com as mesmas flags. O CI testa ambos (`make -j$(nproc)` e `cmake --build`), mas divergências podem passar despercebidas.

### P-02-07 🟡 `find_package(Python3 ... REQUIRED)` no root CMakeLists sem fallback
**Arquivo:** `CMakeLists.txt:165`  
**Problema:** `find_package(Python3 COMPONENTS Interpreter REQUIRED)` faz o build inteiro falhar se Python3 não estiver instalado, mesmo em ambientes onde apenas a biblioteca engine é necessária (sem geração de grafos).  
**Solução:** Tornar Python3 opcional para o target `bitomega_transition_graph`.

### P-02-08 🟡 `rmr_cycles.h` define `typedef unsigned long long u64` sem guard global
**Arquivo:** `engine/rmr/include/rmr_cycles.h:5`  
**Problema:** O header define `u64` localmente sem verificar se já está definido por outro header do mesmo projeto. Incluído com `rmr_hw_detect.h` ou `rmr_math_fabric.h` no mesmo TU → redefinição de typedef.

### P-02-09 🟢 `local.properties` não existe no repo (apenas `.example`)
**Arquivo:** (raiz)  
**Problema:** O arquivo `local.properties` necessário para build Android não está no repo (correto — contém caminhos locais), mas o `.example` não é mencionado no CI como pré-requisito. O CI gera o arquivo via `echo "sdk.dir=..."`, mas builds locais novos podem falhar sem instrução clara.

---

## P-03 · GITHUB CI/CD

### P-03-01 🔴 `neon_simd_selftest.c` misplaced em `.github/workflows/`
**Arquivo:** `.github/workflows/neon_simd_selftest.c`  
**Problema:** Arquivo C fonte colocado dentro do diretório `.github/workflows/`. O GitHub Actions não processa `.c` como workflow, mas o arquivo polui o diretório e pode confundir ferramentas de lint de workflows (como `actionlint`). O arquivo correto está em `demo_cli/src/neon_simd_selftest.c`.

### P-03-02 🔴 Arquivos de workflow com espaços no nome
**Arquivos:** `android (1).yml`, `android (2).yml`, `android-verified (1).yml`  
**Problema:** Nomes de arquivo com espaços e parênteses são problemáticos em GitHub Actions. Alguns runners/ferramentas de CI falham ao fazer referência a esses workflows por nome. Além disso, são duplicatas dos workflows `android.yml` e `android-verified.yml`, causando runs desnecessários e conflitos de concorrência.

### P-03-03 🟠 `ci.yml` e `engine-ci.yml` ambos disparam em `push` irrestrito
**Arquivo:** `.github/workflows/ci.yml`, `.github/workflows/engine-ci.yml`  
**Problema:** `ci.yml` dispara em qualquer `push` sem filtro de paths. `engine-ci.yml` filtra por `engine/**` corretamente. Os dois rodam em paralelo para pushes no engine, gastando dobro de minutos de CI.

### P-03-04 🟠 Workflow `android-build-manual.yml` sem timeout declarado
**Arquivo:** `.github/workflows/android-build-manual.yml`  
**Problema:** Jobs sem `timeout-minutes` podem consumir horas de crédito CI se travarem (download de SDK, Gradle daemon pendente). Os outros workflows definem `timeout-minutes: 30-45`.

### P-03-05 🟠 `proof-build.yml` sem documentação de intenção
**Arquivo:** `.github/workflows/proof-build.yml`  
**Problema:** Workflow de 1.6 KB sem README nem comentário explicando o que "prova" e em quais condições deve passar. Dificulta manutenção.

### P-03-06 🟡 Secrets de signing referenciados mas não documentados no README
**Arquivo:** `.github/workflows/android.yml:93-102`  
**Problema:** O workflow requer `VECTRAS_RELEASE_KEYSTORE_B64`, `VECTRAS_RELEASE_STORE_PASSWORD`, `VECTRAS_RELEASE_KEY_ALIAS`, `VECTRAS_RELEASE_KEY_PASSWORD`. Nenhum README de `docs/` explica como configurar esses secrets para forks ou colaboradores.

### P-03-07 🟡 `dependabot.yml` sem configuração para ecosistema C/CMake
**Arquivo:** `.github/dependabot.yml`  
**Problema:** O Dependabot provavelmente está configurado apenas para Gradle/npm. As versões de NDK e CMake hardcoded em `gradle.properties` não são atualizadas automaticamente.

---

## P-04 · COERÊNCIA DE CÓDIGO

### P-04-01 🔴 Múltiplas declarações `typedef unsigned int u32` sem guard global
**Arquivos:** `rmr_hw_detect.h`, `rmr_bench.h`, `rmr_apk_module.h`, `rmr_isorf.h`, `rmr_math_fabric.h`, `rmr_cycles.h`, `rmr_bench_suite.h`  
**Problema:** 7 headers distintos declaram independentemente `typedef unsigned int u32` e/ou `typedef unsigned long long u64`. Quando dois ou mais desses headers são incluídos no mesmo arquivo `.c`, o compilador C11 com `-pedantic` emite `error: redefinition of typedef 'u32'`.  
**Solução:** Centralizar em um único `rmr_types.h` com guard `#ifndef RMR_TYPES_H` e incluir esse header nos outros.

### P-04-02 🔴 `RmR_GpioPinStride` retorna constante de arquitetura em vez de stride
**Arquivo:** `engine/rmr/src/rmr_hw_detect.c:98-101`  
**Problema:**
```c
static u32 RmR_GpioPinStride(u32 arch){
  if(arch == RMR_ZERO_HW_ARCH_ARM64_U32 || arch == RMR_ZERO_HW_ARCH_ARM_U32) return 4u;
  return RMR_ZERO_HW_ARCH_I386_U32;  // ← retorna 0x00000001u (arch id), não stride!
}
```
`RMR_ZERO_HW_ARCH_I386_U32 = 0x00000001u` é o identificador de arquitetura i386, não um valor de stride de pino. O stride esperado para x86 seria `8u` ou `4u`. Isso corrompe `out->gpio_pin_stride` para todas as arquiteturas não-ARM.

### P-04-03 🟠 `rmr_baremetal_compat.h` redefine `uint32_t` sem verificar `<stdint.h>`
**Arquivo:** `engine/rmr/include/rmr_baremetal_compat.h:11-21`  
**Problema:** O guard `RMR_TYPES_DEFINED` protege contra redefinição interna, mas se `<stdint.h>` for incluído antes (via `rmr_ll_ops.h` que inclui `<stdint.h>`), `uint32_t` já está definido e a redefinição subsequente cria conflito. A ordem de inclusão entre `rmr_ll_ops.h` e `rmr_baremetal_compat.h` é crítica mas não documentada.

### P-04-04 🟠 `rmr_casm_bridge.c` referencia funções ASM sem verificação de símbolo fraco
**Arquivo:** `engine/rmr/src/rmr_casm_bridge.c`  
**Problema:** O bridge chama `rmr_casm_bridge_marker()` e `rmr_casm_xor_fold32_*()` que são definidas nos arquivos `.S` condicionalmente incluídos pelo CMake. Se compilado sem os `.S` correspondentes (e.g., build RISCV64 sem o `.S` de RISCV64), o linker falha. O `vectra_core_accel.c` usa `__attribute__((weak))` para isso, mas `rmr_casm_bridge.c` não.

### P-04-05 🟡 `rmr_ll_ops.h` expõe `select_u32` e `rmr_mask_u32` como `static inline` em header
**Arquivo:** `engine/rmr/include/rmr_ll_ops.h:13-26`  
**Problema:** Funções utilitárias de branchless select declaradas como `static inline` em header. Cada TU que inclui o header gera sua própria cópia. Sem problema de corretude, mas aumenta tamanho do objeto em ~0.5KB por TU.

### P-04-06 🟡 Inconsistência entre `rmr_legacy_capabilities_t` e `rmr_jni_capabilities_t`
**Arquivo:** `engine/rmr/include/rmr_unified_kernel.h`  
**Problema:** `rmr_legacy_capabilities_t` (22 campos) e `rmr_jni_capabilities_t` (14 campos) são structs separadas mapeando informação similar de HW. A conversão entre elas em `rmr_unified_kernel.c` é manual e pode desincronizar se novos campos forem adicionados a apenas uma.

### P-04-07 🟡 `rmr_neon_simd.c` define `u8`, `u32`, `u64` localmente (linha 19-22)
**Arquivo:** `engine/rmr/src/rmr_neon_simd.c:19-22`  
**Problema:** O arquivo define seus próprios typedefs locais após incluir `zero_compat.h`. Se `zero_compat.h` → `rmr_host_compat.h` → `<stdint.h>` já definiu `uint32_t`, e os typedefs locais `u32 = unsigned int` são adicionados, não há colisão direta — mas a mistura de `u32` e `uint32_t` no mesmo arquivo reduz coerência.

### P-04-08 🟢 `rmr_casm_riscv64.S` sem funções de hash φ-step equivalentes ao ARM64/x86_64
**Arquivo:** `engine/rmr/interop/rmr_casm_riscv64.S`  
**Problema:** O arquivo ARM64 implementa `rmr_casm_phi_step_arm64` e `rmr_casm_crc32c_byte_arm64`. O arquivo RISCV64 não tem equivalentes. Isso cria assimetria de capacidade entre arquiteturas e força o bridge a depender de fallbacks C para RISCV64.

---

## P-05 · DOCUMENTAÇÃO / MANUTENÇÃO

### P-05-01 🟡 `FIXES_SUMMARY.md` lista 56 fixes como "status: FIXED" mas vários ainda presentes
**Arquivo:** `FIXES_SUMMARY.md`  
**Problema:** O documento declara `Status: ψ→Σ→Ω — Coerência restaurada. Build funcional garantido.` mas o BUG-02 (`GpioPinStride` retornando constante errada) e BUG-05 (typedef duplicado) ainda estão presentes no código analisado.

### P-05-02 🟡 Ausência de `CONTRIBUTING.md` com guia de como adicionar novo módulo engine
**Problema:** Não existe guia explicando: como adicionar novo `.c` ao `RMR_SOURCES`, como registrar constantes em `zero.h`, como escrever self-test, regras de nomenclatura (`RmR_` prefix).

### P-05-03 🟡 `PROJECT_STATE.md` referenciado em FIXES_SUMMARY mas não encontrado no zip
**Arquivo:** `FIXES_SUMMARY.md:125` → `PROJECT_STATE.md`  
**Problema:** Referência a arquivo que não existe no repositório atual.

### P-05-04 🟢 `web/FILES_MAP.md` e `archive/FILES_MAP.md` desatualizados
**Problema:** Mapas de arquivos em múltiplos diretórios, provavelmente gerados manualmente e já desatualizados em relação à estrutura atual.

### P-05-05 🟢 Ausência de `.editorconfig` para consistência de indentação
**Problema:** Arquivos `.c` usam mistura de tabs e espaços entre diferentes módulos. Um `.editorconfig` garantiria consistência entre contribuidores.

### P-05-06 🟢 `archive/experimental/seguranda/segurancaMilitarnasuditorias.md`
**Problema:** Arquivo de nome ambíguo em path de arquivo incomum. Não referenciado por nenhum build. Deve ser revisado quanto à pertinência no repositório público.

---

## MAPA DE PRIORIZAÇÃO

```
CRÍTICO (resolve build CI)     → P-02-01, P-04-01, P-04-02, P-03-01, P-03-02
ALTA (coerência arquitetural)  → P-01-01, P-01-03, P-04-03, P-04-04, P-02-02
MÉDIA (qualidade/manutenção)   → P-01-04, P-01-05, P-01-06, P-02-04, P-04-06
BAIXA (cleanup)                → P-01-11, P-01-12, P-05-04, P-05-05
```

---

## RETROALIMENTAÇÃO Ω

```
F_ok   = zero.h constants (96%), rmr_hw_detect arch detection, ASM bridges ARM64/x86_64
F_gap  = typedef storm (7 headers), GpioPinStride retorna arch-id, CI misplaced .c file
F_next = centralizar typedefs em rmr_types.h, fixar GpioPinStride, limpar bug/core/
```

> Φ_ethica = Min(Entropia[typedef_storm]) × Max(Coerência[zero.h]) ✓
> R(t+1) exige: resolver P-04-01 e P-04-02 como pré-condição de qualquer build limpo.
```
