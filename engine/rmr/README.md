# RAFAELIA Engine / RMR

## Estrutura
- `include/`: headers públicos
- `src/`: implementação low-level

## Build
```bash
make all
```

## Artefatos
- `build/engine/librmr.a`
- `build/engine/libbitraf.a`
- `build/engine/libbitraf.so`
- `build/demo/rafaelia_demo`
- `build/demo/bitraf_core`
- `build/bench/rmr_bench`
- `build/demo/policy_kernel_demo`
- `build/demo/policy_kernel_selftest`
- `build/demo/rmr_qemu_bridge_demo`
- `build/demo/rmr_qemu_bridge_selftest`

## Módulo APK determinístico
- Header: `include/rmr_apk_module.h`
- Fonte: `src/rmr_apk_module.c`
- Demo: `build/demo/apk_module_demo`

Recursos do módulo:
- plano determinístico de build `:app:assembleRelease`.
- modo Termux/Android (arm64) com `TERMUX_BUILD=1` e `GRADLE_USER_HOME=.gradle`.
- autotuning low-level via `RmR_HW_Detect` (cacheline/page + ABI host) para orquestração.
- fingerprint estável separado de diagnóstico variável.
- invariantes do fingerprint: ABI, page size, cacheline, compile/min/target sdk, versão de build-tools e major do NDK.
- validação de assinatura legítima (sem alias de debug).
- flags de pipeline ético/compliance (`IEEE/NIST/W3C/RFC/GDPR/LGPD`).

## Orquestrador ponta-a-ponta
- Script: `tools/apk/rmr_termux_release_orchestrator.sh`
- Executa checklist de ambiente (java/gradle/sdkmanager), build release, métricas de artefato, verificação de assinatura (`apksigner`/`jarsigner`) e salva rastros em `build/reports/rmr/`.

## Deterministic VM Mutation Layer (C)
- Header: `include/rmr_policy_kernel.h`
- Fonte: `src/rmr_policy_kernel.c`
- CLI: `build/demo/policy_kernel_demo`
- Selftest: `build/demo/policy_kernel_selftest`


## Ponte QEMU + AndroidX (RMR)
- Header: `include/rmr_qemu_bridge.h`
- Fonte: `src/rmr_qemu_bridge.c`
- Demo: `build/demo/rmr_qemu_bridge_demo`
- Selftest: `build/demo/rmr_qemu_bridge_selftest`

Recursos:
- autotuning determinístico de preset (balanced/performance/compatibility) com base em `RmR_HW_Detect`.
- builder de argumentos QEMU para CPU/Memória/IO (`-smp`, `-drive cache/aio`, `iothread`, `virtio`).
- lógica condicional de dispositivos: quando `use_virtio=1` usa `-drive if=virtio` e `-device virtio-net-pci`.
- fallback explícito quando `use_virtio=0`: usa `-drive if=ide` e NIC compatível por guest (`e1000` padrão, `rtl8139` para PPC, `virtio-net-device` para ARM64/virt).
- coerência de plano/preset: autotune para `RMR_GUEST_ARCH_PPC` força preset de compatibilidade e desativa caminhos `virtio/iothread` no comando final.
- parser low-level de telemetria QMP (`status`, `query-cpus-fast`) sem dependências externas.


Exemplos de linha de comando gerada:
- `use_virtio=1` (x86_64): `-drive if=virtio,cache=...,aio=... -netdev user,id=n0 -device virtio-net-pci,netdev=n0`
- `use_virtio=0` (fallback compat): `-drive if=ide,cache=...,aio=... -netdev user,id=n0 -device e1000,netdev=n0`
- `RMR_GUEST_ARCH_PPC` (autotune): força compatibilidade com `-drive if=ide ... -device rtl8139,netdev=n0`
