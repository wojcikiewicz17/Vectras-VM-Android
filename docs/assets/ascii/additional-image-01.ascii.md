<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# additional-image-01 (ASCII)

```text
VECTRAS VM PIPELINE (JAVA/KT → JNI C → RMR CORE → EXECUÇÃO)

┌────────────────────────────────────────────────────────────────┐
│ CAMADA 1 — ENTRADA/UI (Java/Kotlin)                           │
│ com.vectras.vm.main.MainActivity                              │
│ com.vectras.vm.main.core.MainStartVM                          │
│ operador define ROM/ISO + CPU/RAM/DISCO                       │
└───────────────────────────────┬────────────────────────────────┘
                                │
                                v
┌────────────────────────────────────────────────────────────────┐
│ CAMADA 2 — ORQUESTRAÇÃO VM (Java)                             │
│ com.vectras.vm.StartVM + com.vectras.qemu.MainVNCActivity     │
│ build cmd QEMU + bootstrap de VNC/áudio/rede                  │
└───────────────────────────────┬────────────────────────────────┘
                                │
                                v
┌────────────────────────────────────────────────────────────────┐
│ CAMADA 3 — FAST PATH NATIVE (JNI C)                           │
│ com.vectras.vm.core.NativeFastPath                            │
│ app/src/main/cpp/vectra_core_accel.c                          │
│ ponte Java/KT → C para contrato HW/KERNEL, CRC, audit, copy   │
└───────────────────────────────┬────────────────────────────────┘
                                │
                                v
┌────────────────────────────────────────────────────────────────┐
│ CAMADA 4 — NÚCLEO LOW LEVEL (C / ASM-oriented)                │
│ engine/rmr/src/rmr_unified_kernel.c                           │
│ engine/rmr/src/rmr_hw_detect.c + rmr_ll_ops.c + rmr_cycles.c  │
│ detecção de hardware + operações determinísticas de baixo nível│
└───────────────────────────────┬────────────────────────────────┘
                                │
                                v
┌────────────────────────────────────────────────────────────────┐
│ CAMADA 5 — ESTADO/OBSERVABILIDADE                             │
│ com.vectras.vm.logger.VMStatus                                │
│ com.vectras.vm.Fragment.LoggerFragment                        │
│ status V_STARTVM/V_STOPVM + telemetria/log operacional        │
└────────────────────────────────────────────────────────────────┘

Legenda: VM = Virtual Machine; JNI = Java Native Interface; RMR = Runtime Modular Router; VNC = Virtual Network Computing.
```

Este diagrama está aplicado ao código-fonte real e explicita o encadeamento Java/KT → JNI C → núcleo low level.
O layout mantém o padrão visual dos artefatos ASCII do diretório, com fluxo vertical, blocos nomeados e legenda curta.
