/* rmr_ll_ops.c — RMR LOWLEVEL OPERATIONS
 * ∆RAFAELIA_CORE·Ω
 * geo4x4 trace, virtual size calc, path ops
 * Hyperformas: 42/69/64 encoded into trace
 * ─────────────────────────────────────────────────────── */
#include "rmr_unified_kernel.h"
#include "rmr_lowlevel.h"

/* ── geo4x4 trace: 16 × 10-bit → 1 × 32-bit signature ── */
rmr_u32 rmr_ll_geo4x4_trace(const rmr_u16 *cells, rmr_u32 count) {
    if (!cells || !count) return 0u;
    rmr_u32 acc = RMR_TRINITY633;
    for (rmr_u32 i = 0; i < count && i < 16u; i++) {
        rmr_u32 decoded = rmr_lowlevel_decode10((rmr_u32)cells[i]);
        acc = rmr_lowlevel_fold32(acc, decoded, (rmr_u32)i, RMR_PHI32);
    }
    /* Stack42 final mix */
    acc ^= (rmr_u32)(acc << RMR_STACK42) | (acc >> (32u - RMR_STACK42));
    return acc;
}

/* ── virtual size amplification ──
 *   V = physSize × Fᴿ(cellCount) / BITRAF64
 *   Fᴿ: Fibonacci-Rafael
 */
rmr_u64 rmr_ll_virt_size(rmr_u64 phys_size, rmr_u32 cell_count, rmr_u32 path_count) {
    if (!phys_size) return 0ULL;
    rmr_u64 fib = rmr_jni_fib_rafael((int)(cell_count % 32u));
    /* scale fib down to avoid overflow; cap at 1000x */
    rmr_u64 scale = (fib >> RMR_BITRAF64) + 1ULL;
    if (scale > 1000ULL) scale = 1000ULL;
    /* path bonus: log2-ish of path_count */
    rmr_u64 path_bonus = 1ULL;
    rmr_u32 p = path_count;
    while (p >>= 1) path_bonus++;

    return phys_size * scale * path_bonus;
}

/* ── path hash: indices array → hash ── */
rmr_u32 rmr_ll_path_hash(const rmr_u32 *indices, rmr_u32 count, rmr_u32 seed) {
    if (!indices || !count) return seed;
    rmr_u32 h = seed;
    for (rmr_u32 i = 0; i < count; i++) {
        h ^= indices[i];
        h = (rmr_u32)((rmr_u64)h * RMR_PHI32);
        h = (h >> 7u) | (h << 25u);
    }
    return h;
}

/* ── triple complete: derive missing field ──
 *  (off, len) → crc via CRC32C HW/SW
 *  returns: 0=ok, -1=cannot
 */
int rmr_ll_triple_complete(rmr_u64 *off, rmr_u32 *len, rmr_u32 *crc,
                           rmr_u32  valid,
                           const rmr_u8 *data, rmr_u32 data_len) {
    const rmr_u32 V_OFF = 1u, V_LEN = 2u, V_CRC = 4u, V_ALL = 7u;
    if (valid == V_ALL) return 0;
    if (!data) return -1;

    if ((valid & V_OFF) && (valid & V_LEN) && !(valid & V_CRC)) {
        /* derive CRC from off+len */
        rmr_u32 o = (rmr_u32)(*off);
        if ((rmr_u64)o + *len > data_len) return -1;
        *crc = rmr_lowlevel_crc32c_hw(0u, data + o, *len);
        return 0;
    }
    if ((valid & V_OFF) && (valid & V_CRC) && !(valid & V_LEN)) {
        /* scan for length: find block where CRC matches — limited scan */
        rmr_u32 o = (rmr_u32)(*off);
        for (rmr_u32 l = 1u; l <= 65536u && (rmr_u64)o + l <= data_len; l++) {
            rmr_u32 c = rmr_lowlevel_crc32c_hw(0u, data + o, l);
            if (c == *crc) { *len = l; return 0; }
        }
        return -1;
    }
    if ((valid & V_LEN) && (valid & V_CRC) && !(valid & V_OFF)) {
        /* scan for offset */
        for (rmr_u32 o = 0u; o + *len <= data_len; o++) {
            rmr_u32 c = rmr_lowlevel_crc32c_hw(0u, data + o, *len);
            if (c == *crc) { *off = (rmr_u64)o; return 0; }
        }
        return -1;
    }
    return -1;
}
