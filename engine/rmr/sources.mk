# Auto-generated from engine/rmr/sources.cmake by tools/sync_engine_sources.py
# Do not edit directly.

RMR_ENGINE_CORE_SOURCES :=
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/bitomega.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_apk_module.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_attractor.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_baremetal_compat.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_bench.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_bench_suite.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_casm_bridge.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_corelib.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_cycles.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_ethica_loss.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_host_compat.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_hw_detect.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_isorf.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_ll_ops.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_ll_tuning.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_lowlevel_mix.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_lowlevel_portable.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_lowlevel_reduce.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_math_fabric.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_neon_simd.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_qemu_bridge.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_tcg_cache.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_unified_jni_bridge.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_unified_kernel.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_vhw_model.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_virtio_blk.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rmr_zipraf_core.c
RMR_ENGINE_CORE_SOURCES += engine/rmr/src/rafaelia_formulas_core.c

RMR_ENGINE_POLICY_SOURCES :=
RMR_ENGINE_POLICY_SOURCES += engine/rmr/src/rmr_policy_kernel.c

RMR_ENGINE_ASM_X86_64_LOWLEVEL_SOURCES :=
RMR_ENGINE_ASM_X86_64_LOWLEVEL_SOURCES += engine/rmr/interop/rmr_lowlevel_x86_64.S

RMR_ENGINE_ASM_X86_64_CASM_SOURCES :=
RMR_ENGINE_ASM_X86_64_CASM_SOURCES += engine/rmr/interop/rmr_casm_x86_64.S

RMR_ENGINE_ASM_ARM64_SOURCES :=
RMR_ENGINE_ASM_ARM64_SOURCES += engine/rmr/interop/rmr_casm_arm64.S

RMR_ENGINE_ASM_RISCV64_SOURCES :=
RMR_ENGINE_ASM_RISCV64_SOURCES += engine/rmr/interop/rmr_casm_riscv64.S

ENGINE_CORE_SRCS := $(RMR_ENGINE_CORE_SOURCES)
ENGINE_POLICY_SRCS := $(RMR_ENGINE_POLICY_SOURCES)
ENGINE_ASM_X86_64_LOWLEVEL_SRCS := $(RMR_ENGINE_ASM_X86_64_LOWLEVEL_SOURCES)
ENGINE_ASM_X86_64_CASM_SRCS := $(RMR_ENGINE_ASM_X86_64_CASM_SOURCES)
ENGINE_ASM_ARM64_SRCS := $(RMR_ENGINE_ASM_ARM64_SOURCES)
ENGINE_ASM_RISCV64_SRCS := $(RMR_ENGINE_ASM_RISCV64_SOURCES)
