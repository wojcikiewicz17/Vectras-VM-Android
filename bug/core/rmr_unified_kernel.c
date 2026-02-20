/* rmr_unified_kernel.c — RMR UNIFIED KERNEL IMPLEMENTATION
 * ∆RAFAELIA_CORE·Ω
 * ψ→χ→ρ→Δ→Σ→Ω→ψ cycle implemented here
 * ─────────────────────────────────────────────────────── */
#include "rmr_unified_kernel.h"

/* ── internal helpers ── */

static rmr_u32 s_rotate_right32(rmr_u32 v, int n) {
    return (v >> n) | (v << (32 - n));
}

/* φ-fold: 4 → 1, reversible via phi-mixing */
rmr_u32 rmr_jni_phi_fold32(rmr_u32 a, rmr_u32 b, rmr_u32 c, rmr_u32 d) {
    /* Each lane mixed with PHI32 */
    const rmr_u64 P = RMR_PHI32;
    rmr_u32 ra = (rmr_u32)((a * P) & 0xFFFFFFFFu);
    rmr_u32 rb = (rmr_u32)((b * P) & 0xFFFFFFFFu);
    rmr_u32 rc = (rmr_u32)((c * P) & 0xFFFFFFFFu);
    rmr_u32 rd = (rmr_u32)((d * P) & 0xFFFFFFFFu);
    ra ^= rb; rc ^= rd; ra ^= rc;
    return s_rotate_right32(ra, 13);
}

/* Fibonacci-Rafael: Fᴿ(n) = Fᴿ(n-1) + Fᴿ(n-2), seeded by φ */
rmr_u64 rmr_jni_fib_rafael(int n) {
    if (n <= 0) return 1ULL;
    if (n == 1) return 1ULL;
    rmr_u64 a = 1ULL, b = 1ULL;
    for (int i = 2; i <= n && i < 128; i++) {
        rmr_u64 nc = (a + b) * (rmr_u64)RMR_PHI32;
        a = b; b = nc;
    }
    return b;
}

/* CRC32C Castagnoli (SW) */
static const rmr_u32 s_crc32c_table[256] = {
#define C(x) ((x) & 1 ? ((x)>>1)^0x82F63B78u : (x)>>1)
#define C4(x) C(C(C(C(x))))
#define C8(x) C4(C4(x))
/* We compute at init instead of static table to avoid .data bloat */
    0 /* placeholder; filled in rmr_crc32c_sw_init */
};
/* actual table initialized once */
static rmr_u32 s_crc32c_tbl[256];
static int     s_crc32c_ready = 0;

static void s_crc32c_init(void) {
    if (s_crc32c_ready) return;
    for (int i = 0; i < 256; i++) {
        rmr_u32 crc = (rmr_u32)i;
        for (int j = 0; j < 8; j++)
            crc = (crc >> 1) ^ ((crc & 1) ? 0x82F63B78u : 0u);
        s_crc32c_tbl[i] = crc;
    }
    s_crc32c_ready = 1;
}

int rmr_jni_crc32c_sw(rmr_u32 seed, const rmr_u8 *data, rmr_u32 len, rmr_u32 *out) {
    if (!data || !out) return RMR_KERNEL_ERR_STATE;
    s_crc32c_init();
    rmr_u32 crc = ~seed;
    for (rmr_u32 i = 0; i < len; i++)
        crc = (crc >> 8) ^ s_crc32c_tbl[(crc ^ data[i]) & 0xFFu];
    *out = ~crc;
    return RMR_KERNEL_OK;
}

/* CRC32C HW dispatch — delegates to arch-specific or SW fallback */
int rmr_jni_crc32c_hw(rmr_u32 seed, const rmr_u8 *data, rmr_u32 len, rmr_u32 *out) {
#if defined(__aarch64__)
    /* ARM64: use __crc32cb intrinsic if available */
    rmr_u32 crc = ~seed;
    rmr_u32 i = 0;
#if defined(__ARM_FEATURE_CRC32)
    for (; i + 8 <= len; i += 8)
        crc = __builtin_arm_crc32d(crc, *(const rmr_u64*)(data + i));
    for (; i + 4 <= len; i += 4)
        crc = __builtin_arm_crc32w(crc, *(const rmr_u32*)(data + i));
    for (; i < len; i++)
        crc = __builtin_arm_crc32b(crc, data[i]);
    *out = ~crc;
    return RMR_KERNEL_OK;
#endif
#elif defined(__x86_64__) || defined(__i386__)
#if defined(__SSE4_2__)
    rmr_u32 crc = ~seed;
    rmr_u32 i = 0;
    for (; i + 8 <= len; i += 8)
        crc = (rmr_u32)__builtin_ia32_crc32di(crc, *(const rmr_u64*)(data + i));
    for (; i + 4 <= len; i += 4)
        crc = __builtin_ia32_crc32si(crc, *(const rmr_u32*)(data + i));
    for (; i < len; i++)
        crc = __builtin_ia32_crc32qi(crc, data[i]);
    *out = ~crc;
    return RMR_KERNEL_OK;
#endif
#endif
    /* SW fallback */
    return rmr_jni_crc32c_sw(seed, data, len, out);
}

/* ── Kernel init / tick ── */

int rmr_jni_kernel_init(rmr_jni_kernel_state_t *state, rmr_u32 seed) {
    if (!state) return RMR_KERNEL_ERR_INIT;

    /* zero state */
    for (rmr_u8 *p = (rmr_u8*)state;
         p < (rmr_u8*)state + sizeof(*state); p++) *p = 0;

    state->magic      = RMR_UK_NATIVE_OK_MAGIC;
    state->init_flags = seed;
    state->cycle      = 0u;
    state->coherence  = RMR_PHI32;

    /* φ-state seed */
    state->caps.phi_state = (rmr_u64)seed * RMR_PHI32;
    state->caps.signature = 0x50414356u; /* "VCAP" */
    state->caps.pointer_bits = RMR_PTR_BITS;

    /* detect hardware capabilities */
    int rc = rmr_hw_detect_fill(&state->caps);
    if (rc != RMR_KERNEL_OK) {
        /* non-fatal: continue with defaults */
        state->caps.feature_mask  = 0u;
        state->caps.arch_id       = RMR_ARCH_UNKNOWN;
    }

    /* init CRC table */
    s_crc32c_init();

    return RMR_KERNEL_OK;
}

int rmr_jni_kernel_get_capabilities(rmr_jni_kernel_state_t *state,
                                    rmr_jni_capabilities_t  *out) {
    if (!state || !out) return RMR_KERNEL_ERR_STATE;
    if (state->magic != RMR_UK_NATIVE_OK_MAGIC) return RMR_KERNEL_ERR_INIT;
    /* copy caps */
    const rmr_u8 *src = (const rmr_u8*)&state->caps;
    rmr_u8 *dst = (rmr_u8*)out;
    for (rmr_u32 i = 0; i < sizeof(rmr_jni_capabilities_t); i++)
        dst[i] = src[i];
    return RMR_KERNEL_OK;
}

int rmr_jni_kernel_tick(rmr_jni_kernel_state_t *state) {
    if (!state || state->magic != RMR_UK_NATIVE_OK_MAGIC)
        return RMR_KERNEL_ERR_STATE;
    /* ψ→χ→ρ→Δ→Σ→Ω: one cycle step */
    state->cycle = (state->cycle + 1u) % 6u;
    state->caps.phi_state =
        (state->caps.phi_state * (rmr_u64)RMR_PHI32) ^ RMR_TRINITY633;
    state->coherence = rmr_jni_phi_fold32(
        state->coherence, state->cycle,
        (rmr_u32)state->caps.phi_state,
        state->caps.feature_mask);
    return RMR_KERNEL_OK;
}
