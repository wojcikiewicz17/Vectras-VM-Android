# VECTRAS STARTUP PATH AUDIT

Fluxo auditado no código (fonte de verdade):
UI/Adapter → `StartVM.env(...)` → `QemuExecConfig.resolveBinary(...)` → `QemuBinaryResolver.resolveForArch(...)` → `QemuArgsBuilder.resolveProfile/applyProfile/applyAcceleration(...)` → `StartVM.buildCommand(...)` → `MainStartVM.startNow(...)` → `SetupFeatureCore.runVmStartPreflight(...)` → `VMManager.isthiscommandsafe(...)` → `MainService.startCommand(...)` → QEMU runtime → NativeFastPath/JNI/RMR.

## 1) UI/Adapter
- S0_ORIGEM: chamadas para `MainStartVM.startNow(...)` a partir da UI.
- S1_STATUS: OK
- S2_ENTRADA: `context`, `vmName`, `env`, `vmID`, `thumbnailFile`.
- S3_SAÍDA: início de sequência de launch.
- S4_CONTRATO: VM id válido ou gerado transitório.
- S5_FALHA: abortos com `VmFlowTracker ERROR`.
- S6_FALLBACK: launch id transitório.
- S7_RISCO: estado global estático em launch concorrente.
- S8_TESTE: NEEDS_TEST concorrência multi-VM.

## 2) StartVM.env
- S0_ORIGEM: `app/src/main/java/com/vectras/vm/StartVM.java::env`.
- S1_STATUS: PARCIAL
- S2_ENTRADA: `activity`, `extras`, `img`, `isQuickRun`.
- S3_SAÍDA: string de comando QEMU.
- S4_CONTRATO: atualiza `lastRuntimeContract` em `prepared`.
- S5_FALHA: comando vazio marca `lastStartError=empty_command`.
- S6_FALLBACK: ajustes de UI (`-display none` fallback).
- S7_RISCO: tokenização por split/join, quoting e `finalextra` monobloco.
- S8_TESTE: unit tests de `buildCommand` e regressões VNC.

## 3) Binário/Perfil/Aceleração
- S0_ORIGEM: `QemuExecConfig`, `QemuBinaryResolver`, `QemuArgsBuilder`, `KvmProbe`.
- S1_STATUS: OK
- S2_ENTRADA: arch/config/extras.
- S3_SAÍDA: binário resolvido, profile e flags de aceleração.
- S4_CONTRATO: `lastResolvedProfile`, `lastKvmEnabled`, `lastKvmReason`.
- S5_FALHA: fallback para TCG quando KVM indisponível.
- S6_FALLBACK: perfil BALANCED/default.
- S7_RISCO: ABI mismatch e feature drift.
- S8_TESTE: testes de `QemuBinaryResolver`/`KvmProbe` existentes.

## 4) MainStartVM.startNow + preflight + safety
- S0_ORIGEM: `app/src/main/java/com/vectras/vm/main/core/MainStartVM.java::startNow`.
- S1_STATUS: PARCIAL
- S2_ENTRADA: env pré-montado.
- S3_SAÍDA: `finalCommand` para service ou abort.
- S4_CONTRATO: marca flow STARTING/RUNNING/ERROR.
- S5_FALHA: preflight, comando unsafe, pasta cache, porta VNC/SPICE.
- S6_FALLBACK: abort controlado + dialogs + ledger.
- S7_RISCO: estados estáticos (`pendingVMID`,`lastVMID`) e corrida.
- S8_TESTE: NEEDS_TEST de abort contract completo.

## 5) SPICE/VNC/X11/Headless
- S0_ORIGEM: `StartVM.env`, `VmLaunchMode.determine`, `MainStartVM.reserveSpicePortIfNeeded`.
- S1_STATUS: OK (após correção de placeholder SPICE)
- S2_ENTRADA: UI mode + prefs.
- S3_SAÍDA: args `-spice/-vnc/-display` coerentes.
- S4_CONTRATO: SPICE usa placeholder e substituição dinâmica.
- S5_FALHA: reserva de porta SPICE pode falhar.
- S6_FALLBACK: aborta antes de `MainService`.
- S7_RISCO: colisão de porta externa VNC.
- S8_TESTE: unit/regressão de placeholder e mensagens.

## 6) MainService/QEMU runtime
- S0_ORIGEM: `MainService.startCommand(finalCommand, context)`.
- S1_STATUS: OK
- S2_ENTRADA: comando final com wrapper/audio/display.
- S3_SAÍDA: processo QEMU.
- S4_CONTRATO: comando deve ser passado uma vez.
- S5_FALHA: erro de runtime/exec.
- S6_FALLBACK: restart/try-again via fluxo UI.
- S7_RISCO: duplicação de wrappers se concatenação errada.
- S8_TESTE: NEEDS_TEST integração end-to-end.

## 7) NativeFastPath/JNI/RMR + Build
- S0_ORIGEM: `NativeFastPath.java`, `vectra_core_accel.c`, `CMakeLists.txt`, `rmr_unified_jni_bridge.c`, `rmr_unified_kernel.c`, `app/build.gradle`.
- S1_STATUS: OK
- S2_ENTRADA: contratos JNI + flags Gradle/CMake.
- S3_SAÍDA: `libvectra_core_accel.so` com bridge RMR.
- S4_CONTRATO: fallback Java quando nativo indisponível.
- S5_FALHA: load/lib mismatch por ABI.
- S6_FALLBACK: caminhos Java em `NativeFastPath`.
- S7_RISCO: divergência ABI policy vs artefatos.
- S8_TESTE: matriz ABI + testes unitários existentes.
