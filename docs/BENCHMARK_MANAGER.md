<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# BenchmarkManager — documentação técnica baseada no código-fonte

## Objetivo

Este documento descreve o comportamento real do `BenchmarkManager` com base no código em `app/src/main/java/com/vectras/vm/benchmark/BenchmarkManager.java`, evitando suposições não implementadas.

## Escopo funcional (implementado)

O `BenchmarkManager` é um orquestrador de execução e validação para o `VectraBenchmark`.

Ele entrega cinco blocos principais:

1. **Pré-checagens ambientais** com avisos (`warnings`) antes do benchmark.
2. **Perfil de execução** (`ExecutionProfile`) e perfil de tuning derivado (`TuningProfile`).
3. **Execução instrumentada** dos 79 métricas do `VectraBenchmark` com callback de progresso.
4. **Validação estatística** de consistência com score de confiança.
5. **Relatório de diagnósticos** (drift de clock, jitter, estabilidade, sinais de emulador, ABI mismatch).

## APIs públicas relevantes

### 1) Callback de progresso

```java
public interface ProgressCallback {
    void onProgress(int metricIndex, int totalMetrics, String currentMetric);
    void onWarning(String warning);
    void onComplete(BenchmarkResult result);
    void onError(String error);
}
```

### 2) Modos de execução

```java
public enum ExecutionProfile {
    AUTO_ADAPTIVE,
    DETERMINISTIC,
    THROUGHPUT,
    LOW_LATENCY
}
```

### 3) Perfil de tuning efetivo

`TuningProfile` materializa as decisões de runtime:

- `mode`: perfil lógico solicitado.
- `copyStripeBytes`: tamanho de faixa para cópia em hotpaths.
- `threadPriority`: prioridade de thread aplicada com `Process.setThreadPriority(...)`.
- `warmupDelayMs`: janela de estabilização antes de medir.
- `label`: rótulo legível de diagnóstico.

### 4) Execução principal

```java
public BenchmarkResult runBenchmark(ProgressCallback callback)
public BenchmarkResult runBenchmark(ProgressCallback callback, ExecutionProfile mode)
```

## Fluxo de execução (fonte de verdade)

1. **Captura de ambiente inicial** (`captureEnvironment`).
2. **Pré-flight checks** (`performPreflightChecks`) e emissão de `warnings`.
3. **Resolução de tuning profile** (`resolveTuningProfile`).
4. **Otimização local** (`optimizeEnvironment`): prioridade de thread + `System.gc()` + warmup.
5. **Execução de benchmark** em `VectraBenchmark` com callbacks.
6. **Captura de ambiente final**.
7. **Validação de resultados** (`ValidationReport`).
8. **Montagem de diagnóstico estruturado** (`DiagnosticMetrics`).

## Interferências e validação

O `ValidationReport` agrega:

- `warnings` e `errors`;
- `confidenceScore` (0.0 a 1.0);
- flags de interferência (`gcDetected`, `thermalDetected`, `memoryPressure`);
- `resultVariance`;
- `interferenceCount`.

A confiança é degradada por variância e eventos de interferência, com clamp para faixa válida.

## Diagnósticos expostos

O objeto `BenchmarkResult` fornece `getDiagnosticsView()` com métricas nomeadas:

- `Timer Drift` (%),
- `Timer Jitter` (%),
- `CPU Stability Variance` (%),
- `Emulator Signals` (DETECTED/NOT DETECTED),
- `ABI/CPU Mismatch` (DETECTED/NOT DETECTED).

## Integração com camadas de performance

O manager integra sinais de hardware e fast-paths sem quebrar fallback:

- usa `BareMetalProfile` para inferência de características do host;
- registra disponibilidade JNI por `NativeFastPath.isNativeAvailable()` no rótulo do perfil;
- ajusta `copyStripeBytes` conforme temperatura, memória livre e carga observada.

## Limitações conhecidas (estado atual)

1. O comportamento depende de permissões/disponibilidade de arquivos `sysfs` no dispositivo.
2. Em ambientes sem Android SDK configurado (ex.: CI/container sem `ANDROID_HOME`), testes unitários Android não executam.
3. A classe prioriza robustez e observabilidade; não substitui perfilagem nativa com ferramentas externas (Perfetto/simpleperf).

## Diretrizes de uso profissional

- Rodar benchmark com bateria adequada, sem economia de energia e sem carga intensa em background.
- Comparar runs com o **mesmo** `ExecutionProfile`.
- Usar `ValidationReport` + `DiagnosticMetricsView` como critério de aceitação de resultados.
- Considerar inválidos resultados com baixa confiança persistente ou alta variância recorrente.

## Metadados
- Versão do documento: 1.2
- Última atualização: 2026-03-06
- Commit de referência: `a70a4d9`
- Domínio de código coberto: Gerenciador de benchmark do app Android e diagnóstico de execução.
