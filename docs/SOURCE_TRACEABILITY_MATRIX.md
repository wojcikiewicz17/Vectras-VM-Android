# Source Traceability Matrix — Inventário Consolidado de Funcionalidades

## Objetivo

Este documento consolida o inventário funcional do repositório com **rastreabilidade bidirecional** entre código-fonte e documentação. O foco é refletir o que está implementado hoje, com linguagem formal, navegável e auditável.

---

## 1) Camadas do sistema e fontes de verdade

| Camada | Finalidade | Código-fonte principal | Documentação relacionada |
|---|---|---|---|
| Launch/Orquestração QEMU | Construção determinística de argumentos, escolha de binário e aceleração | `app/src/main/java/com/vectras/vm/StartVM.java`, `app/src/main/java/com/vectras/vm/qemu/QemuArgsBuilder.java`, `app/src/main/java/com/vectras/vm/qemu/KvmProbe.java`, `app/src/main/java/com/vectras/vm/qemu/VmProfile.java`, `app/src/main/java/com/vectras/vm/qemu/VmLaunchLedger.java` | `docs/ARCHITECTURE.md`, `docs/INTEGRACAO_RM_QEMU_ANDROIDX.md` |
| Benchmark e validação | Execução de 79 métricas, pré-checagens, validação e diagnósticos | `app/src/main/java/com/vectras/vm/benchmark/BenchmarkManager.java`, `app/src/main/java/com/vectras/vm/benchmark/VectraBenchmark.java`, `app/src/main/java/com/vectras/vm/benchmark/BenchmarkActivity.java` | `docs/BENCHMARK_MANAGER.md`, `docs/PERFORMANCE_INTEGRITY.md`, `docs/navigation/BENCHMARK_COMPARISONS.md` |
| Núcleo low-level matemático | Operações bitwise, vetoriais, integridade e utilitários determinísticos | `app/src/main/java/com/vectras/vm/core/BitwiseMath.java`, `app/src/main/java/com/vectras/vm/core/LowLevelAsm.java`, `app/src/main/java/com/vectras/vm/core/AdvancedAlgorithms.java` | `README.md`, `VECTRA_CORE.md`, `archive/root-history/ADVANCED_OPTIMIZATIONS.md` |
| Fast-path nativo opcional | JNI para hot paths com fallback Java | `app/src/main/java/com/vectras/vm/core/NativeFastPath.java`, `app/src/main/cpp/vectra_core_accel.c`, `app/src/main/cpp/CMakeLists.txt` | `docs/ARCHITECTURE.md`, `docs/BENCHMARK_MANAGER.md` |
| Perfil de hardware bare-metal | Inferência de capacidade para tuning de execução | `app/src/main/java/com/vectras/vm/core/BareMetalProfile.java` | `docs/BENCHMARK_MANAGER.md`, `archive/root-history/BENCHMARK_REFACTORING_SUMMARY.md` |
| Qualidade e regressão | Cobertura de testes unitários em funções críticas | `app/src/test/java/com/vectras/vm/core/NativeFastPathTest.java`, `app/src/test/java/com/vectras/vm/core/BareMetalProfileTest.java`, `app/src/test/java/com/vectras/vm/core/AdvancedAlgorithmsTest.java`, `app/src/test/java/com/vectras/vm/core/BitwiseMathTest.java`, `app/src/test/java/com/vectras/vm/benchmark/BenchmarkManagerTest.java` | `docs/CONTRIBUTING.md`, `docs/PERFORMANCE_INTEGRITY.md` |

---

## 2) Inventário funcional por subsistema

### 2.1 QEMU Launch Stack (determinístico)

**Funcionalidades implementadas:**
- resolução de binário por arquitetura;
- resolução de interface de drive com fallback seguro;
- aplicação de perfis de execução da VM;
- aplicação condicional de virtio storage hints sem duplicar flags existentes;
- aplicação de aceleração KVM condicionada a `KvmProbe`;
- metadados/ledger de lançamento.

**Arquivos envolvidos:**
- `app/src/main/java/com/vectras/vm/qemu/QemuArgsBuilder.java`
- `app/src/main/java/com/vectras/vm/qemu/KvmProbe.java`
- `app/src/main/java/com/vectras/vm/StartVM.java`
- `app/src/main/java/com/vectras/vm/qemu/VmLaunchLedger.java`

### 2.2 Benchmark Stack (medição + validação)

**Funcionalidades implementadas:**
- execução instrumentada com callback de progresso;
- preflight checks ambientais (temperatura, memória, carga, relógio);
- geração de `ValidationReport` com confiança/variância/interferência;
- snapshots ambientais antes/depois;
- diagnósticos estruturados (timer drift/jitter, sinais de emulador, ABI mismatch);
- perfis `AUTO_ADAPTIVE`, `DETERMINISTIC`, `THROUGHPUT`, `LOW_LATENCY`.

**Arquivos envolvidos:**
- `app/src/main/java/com/vectras/vm/benchmark/BenchmarkManager.java`
- `app/src/main/java/com/vectras/vm/benchmark/VectraBenchmark.java`
- `app/src/main/java/com/vectras/vm/benchmark/BenchmarkActivity.java`

### 2.3 Core Low-Level (algoritmos e bitwise)

**Funcionalidades implementadas:**
- operações bitwise e rotinas matemáticas fixas determinísticas;
- otimizações low-level em rotações, popcount, leading/trailing zeros;
- rotinas avançadas (incluindo caminhos de busca/heap conforme módulo `AdvancedAlgorithms`);
- estratégia com minimização de alocação em caminhos quentes.

**Arquivos envolvidos:**
- `app/src/main/java/com/vectras/vm/core/BitwiseMath.java`
- `app/src/main/java/com/vectras/vm/core/LowLevelAsm.java`
- `app/src/main/java/com/vectras/vm/core/AdvancedAlgorithms.java`

### 2.4 Native Fast Path (JNI opcional, fallback garantido)

**Funcionalidades implementadas:**
- `copyBytes(...)`;
- `xorChecksum(...)`;
- `popcount32(...)` com fallback Java SWAR;
- inicialização JNI com verificação de disponibilidade;
- implementação C com ramo otimizado (incluindo condições de compilador/arquitetura).

**Arquivos envolvidos:**
- `app/src/main/java/com/vectras/vm/core/NativeFastPath.java`
- `app/src/main/cpp/vectra_core_accel.c`
- `app/src/main/cpp/CMakeLists.txt`

---

## 3) Matriz de rastreabilidade documentação ⇄ código

| Documento | O que deve refletir | Evidência de código para revisão |
|---|---|---|
| `docs/ARCHITECTURE.md` | Arquitetura executável de launch, KVM, fast-path e benchmark | `StartVM`, `QemuArgsBuilder`, `KvmProbe`, `NativeFastPath`, `BenchmarkManager` |
| `docs/BENCHMARK_MANAGER.md` | API real, fluxo de execução, validação e diagnósticos | `BenchmarkManager` |
| `docs/PERFORMANCE_INTEGRITY.md` | Metodologia de medição e interpretação de consistência | `VectraBenchmark`, `BenchmarkManager` |
| `docs/navigation/BENCHMARK_COMPARISONS.md` | Leitura comparativa por categoria de métricas | `VectraBenchmark` |
| `README.md` | visão executiva de capacidades técnicas reais | `BitwiseMath`, `VectraBenchmark`, stack QEMU |
| `VECTRA_CORE.md` | contexto do framework e integração no app | classes de core + configuração de build |

---

## 4) Checklist de coerência editorial (para manutenção contínua)

1. Toda afirmação de funcionalidade deve apontar para ao menos um arquivo-fonte.
2. Mudança em API pública exige atualização no documento correspondente.
3. Mudança em fluxo de benchmark exige atualização conjunta de:
   - `docs/BENCHMARK_MANAGER.md`
   - `docs/PERFORMANCE_INTEGRITY.md`
   - `docs/navigation/BENCHMARK_COMPARISONS.md`
4. Mudança em launch/KVM/QEMU exige atualização de:
   - `docs/ARCHITECTURE.md`
   - `docs/INTEGRACAO_RM_QEMU_ANDROIDX.md`
5. Índices (`docs/README.md` e `docs/navigation/INDEX.md`) devem sempre incluir novos documentos estruturais.

---

## 5) Navegação recomendada (ordem formal)

1. `docs/README.md` (entrada global)
2. `docs/SOURCE_TRACEABILITY_MATRIX.md` (inventário e mapa de evidências)
3. `docs/ARCHITECTURE.md` (camadas e fluxo sistêmico)
4. `docs/BENCHMARK_MANAGER.md` (detalhe operacional de benchmark)
5. `docs/navigation/INDEX.md` (rotas por audiência)

---

## 6) Nota de método

Este inventário foi montado por inspeção direta dos arquivos-fonte citados, com ênfase em coerência entre texto, implementação e navegação documental.
