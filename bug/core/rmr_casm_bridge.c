/* rmr_casm_bridge.c — RMR CASM BRIDGE
 * ∆RAFAELIA_CORE·Ω
 * Connects C kernel to ASM hot-paths
 * Provides: bridge marker, asm dispatch table, feature routing
 * ─────────────────────────────────────────────────────── */
#include "rmr_unified_kernel.h"
#include "rmr_lowlevel.h"

/* ── bridge marker: identifies that ASM is linked ── */
#if defined(__GNUC__)
__attribute__((visibility("default")))
#endif
rmr_u32 rmr_casm_bridge_marker(void) {
    /* "CASM" = 0x4D534143 */
    return 0x4D534143u;
}

/* ── ASM hot-path declarations (implemented in .S files) ── */
#if defined(__aarch64__)
extern rmr_u32 zipraf_crc32c_arm64(rmr_u32 crc, const rmr_u8 *data, rmr_u32 len);
extern rmr_u32 vectra_phi_fold_arm64(rmr_u32 a, rmr_u32 b, rmr_u32 c, rmr_u32 d);
#elif defined(__x86_64__)
extern rmr_u32 vectra_phi_fold_x86_64(rmr_u32 a, rmr_u32 b, rmr_u32 c, rmr_u32 d);
#endif

/* ── dispatch: CRC32C with HW accel ── */
rmr_u32 rmr_casm_crc32c(rmr_u32 crc, const rmr_u8 *data, rmr_u32 len) {
    if (!data || !len) return crc;
#if defined(__aarch64__) && defined(__ARM_FEATURE_CRC32)
    return zipraf_crc32c_arm64(crc, data, len);
#else
    return rmr_lowlevel_crc32c_hw(crc, data, len);
#endif
}

/* ── dispatch: phi fold with SIMD ── */
rmr_u32 rmr_casm_phi_fold(rmr_u32 a, rmr_u32 b, rmr_u32 c, rmr_u32 d) {
#if defined(__aarch64__)
#if defined(VECTRA_HAS_ASM_PHI_FOLD)
    return vectra_phi_fold_arm64(a, b, c, d);
#endif
#elif defined(__x86_64__)
#if defined(VECTRA_HAS_ASM_PHI_FOLD)
    return vectra_phi_fold_x86_64(a, b, c, d);
#endif
#endif
    return rmr_lowlevel_fold32(a, b, c, d);
}

/* ── capability probe: which ASM paths are active ── */
rmr_u32 rmr_casm_active_mask(void) {
    rmr_u32 mask = 0u;
#if defined(__aarch64__)
    mask |= (1u << 0); /* ARM64 ASM present */
#if defined(__ARM_FEATURE_CRC32)
    mask |= (1u << 1); /* CRC32C HW */
#endif
#if defined(__ARM_NEON)
    mask |= (1u << 2); /* NEON SIMD */
#endif
#elif defined(__x86_64__)
    mask |= (1u << 8); /* x86_64 ASM */
#if defined(__SSE4_2__)
    mask |= (1u << 9);
#endif
#elif defined(__arm__)
    mask |= (1u << 16);
#elif defined(__riscv)
    mask |= (1u << 24);
#endif
    return mask;
}
