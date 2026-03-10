# Canonical RMR engine source manifest shared by host/Android build systems.
# Paths are repository-root relative.

set(RMR_ENGINE_CORE_SOURCES
  engine/rmr/src/bitomega.c
  engine/rmr/src/rmr_apk_module.c
  engine/rmr/src/rmr_attractor.c
  engine/rmr/src/rmr_baremetal_compat.c
  engine/rmr/src/rmr_bench.c
  engine/rmr/src/rmr_bench_suite.c
  engine/rmr/src/rmr_casm_bridge.c
  engine/rmr/src/rmr_corelib.c
  engine/rmr/src/rmr_cycles.c
  engine/rmr/src/rmr_ethica_loss.c
  engine/rmr/src/rmr_host_compat.c
  engine/rmr/src/rmr_hw_detect.c
  engine/rmr/src/rmr_isorf.c
  engine/rmr/src/rmr_ll_ops.c
  engine/rmr/src/rmr_ll_tuning.c
  engine/rmr/src/rmr_lowlevel_mix.c
  engine/rmr/src/rmr_lowlevel_portable.c
  engine/rmr/src/rmr_lowlevel_reduce.c
  engine/rmr/src/rmr_math_fabric.c
  engine/rmr/src/rmr_neon_simd.c
  engine/rmr/src/rmr_qemu_bridge.c
  engine/rmr/src/rmr_tcg_cache.c
  engine/rmr/src/rmr_unified_jni_bridge.c
  engine/rmr/src/rmr_unified_kernel.c
  engine/rmr/src/rmr_vhw_model.c
  engine/rmr/src/rmr_virtio_blk.c
  engine/rmr/src/rmr_zipraf_core.c
  engine/rmr/src/rafaelia_formulas_core.c
)

set(RMR_ENGINE_POLICY_SOURCES
  engine/rmr/src/rmr_policy_kernel.c
)

set(RMR_ENGINE_ASM_X86_64_LOWLEVEL_SOURCES
  engine/rmr/interop/rmr_lowlevel_x86_64.S
)

set(RMR_ENGINE_ASM_X86_64_CASM_SOURCES
  engine/rmr/interop/rmr_casm_x86_64.S
)

set(RMR_ENGINE_ASM_ARM64_SOURCES
  engine/rmr/interop/rmr_casm_arm64.S
)

set(RMR_ENGINE_ASM_RISCV64_SOURCES
  engine/rmr/interop/rmr_casm_riscv64.S
)
