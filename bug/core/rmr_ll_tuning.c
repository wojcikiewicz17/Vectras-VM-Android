/* rmr_ll_tuning.c — RMR LOWLEVEL RUNTIME TUNING
 * ∆RAFAELIA_CORE·Ω
 * Calibrates: CRC32C path, phi-step rate, cache-line alignment
 * Autotunes after rmr_hw_detect_fill runs
 * ─────────────────────────────────────────────────────── */
#include "rmr_unified_kernel.h"
#include "rmr_lowlevel.h"

/* ── tuning parameters ── */
typedef struct rmr_ll_tune_params {
    rmr_u32 crc_block_size;      /* optimal block size for HW CRC */
    rmr_u32 phi_step_interval;   /* ticks between phi-state updates */
    rmr_u32 coherence_threshold; /* min coherence to use HW path */
    rmr_u32 cache_line;          /* from caps */
    rmr_u32 hw_crc_enabled;      /* 0=SW, 1=HW */
    rmr_u32 neon_enabled;        /* NEON/SIMD flag */
    rmr_u32 sve_enabled;         /* SVE flag */
} rmr_ll_tune_params_t;

static rmr_ll_tune_params_t s_tune = {
    .crc_block_size      = 64u,
    .phi_step_interval   = 16u,
    .coherence_threshold = 0x100u,
    .cache_line          = 64u,
    .hw_crc_enabled      = 0u,
    .neon_enabled        = 0u,
    .sve_enabled         = 0u,
};

/* ── calibrate from caps ── */
int rmr_ll_tuning_calibrate(const rmr_jni_capabilities_t *caps) {
    if (!caps) return RMR_KERNEL_ERR_STATE;

    s_tune.cache_line = caps->cache_line_bytes ? caps->cache_line_bytes : 64u;

    /* CRC block: 4× cache line */
    s_tune.crc_block_size = s_tune.cache_line * 4u;

    /* HW path flags */
    s_tune.hw_crc_enabled = (caps->feature_mask & RMR_FEAT_CRC32) ? 1u : 0u;
    s_tune.neon_enabled   = (caps->feature_mask & RMR_FEAT_SIMD)  ? 1u : 0u;
    s_tune.sve_enabled    = (caps->feature_mask & RMR_FEAT_SVE)   ? 1u : 0u;

    /* phi step: faster on NEON */
    s_tune.phi_step_interval = s_tune.neon_enabled ? 8u : 16u;

    /* coherence threshold: higher on weaker CPUs */
    s_tune.coherence_threshold = s_tune.hw_crc_enabled ? 0x80u : 0x200u;

    return RMR_KERNEL_OK;
}

/* ── getters ── */
rmr_u32 rmr_ll_tuning_crc_block(void)       { return s_tune.crc_block_size; }
rmr_u32 rmr_ll_tuning_phi_interval(void)    { return s_tune.phi_step_interval; }
rmr_u32 rmr_ll_tuning_coherence_thr(void)   { return s_tune.coherence_threshold; }
int     rmr_ll_tuning_hw_crc(void)          { return (int)s_tune.hw_crc_enabled; }
int     rmr_ll_tuning_neon(void)            { return (int)s_tune.neon_enabled; }

/* ── optimal CRC dispatch based on tuning ── */
rmr_u32 rmr_ll_tuning_crc32c(rmr_u32 crc, const rmr_u8 *data, rmr_u32 len) {
    if (s_tune.hw_crc_enabled)
        return rmr_lowlevel_crc32c_hw(crc, data, len);
    return rmr_lowlevel_crc32c_sw(crc, data, len);
}
