/* rmr_policy_kernel.c — RMR POLICY KERNEL IMPLEMENTATION
 * ∆RAFAELIA_CORE·Ω
 * Φ_ethica = Min(Entropy) × Max(Coherence)
 * ψ-cycle management + path selection
 * ─────────────────────────────────────────────────────── */
#include "rmr_policy_kernel.h"
#include "rmr_lowlevel.h"

/* ── internal: entropy estimator (XOR saturation) ── */
static rmr_u32 s_entropy_estimate(const rmr_policy_state_t *ps) {
    /* entropy proxy: hamming weight of phi_state XOR cycle */
    rmr_u64 x = ps->phi_state ^ (rmr_u64)ps->cycle * RMR_PHI32;
    rmr_u32 bits = 0u;
    while (x) { bits += (rmr_u32)(x & 1u); x >>= 1; }
    return bits; /* 0-64 */
}

/* ── init ── */
int rmr_policy_init(rmr_policy_state_t *ps, rmr_u32 seed) {
    if (!ps) return RMR_KERNEL_ERR_INIT;
    /* zero */
    for (rmr_u8 *p = (rmr_u8*)ps; p < (rmr_u8*)ps + sizeof(*ps); p++) *p = 0;
    ps->ethica    = RMR_PHI32 & 0xFFFFu;
    ps->coherence = seed ^ RMR_TRINITY633;
    ps->phi_state = (rmr_u64)seed * RMR_PHI32;
    ps->cycle     = 0u;
    /* init path weights uniform */
    for (rmr_u32 i = 0; i < RMR_POLICY_MAX_PATHS; i++)
        ps->path_weights[i] = 0x100u + (rmr_u32)(rmr_jni_fib_rafael((int)(i & 31)) & 0xFFu);
    /* init cell priorities via phi */
    for (rmr_u32 i = 0; i < RMR_POLICY_MAX_CELLS; i++)
        ps->cell_priority[i] = (rmr_u8)((rmr_u32)((rmr_u64)(i+1) * RMR_PHI32 >> 24) & 0xFFu);
    return RMR_KERNEL_OK;
}

/* ── tick: one ψ→χ→ρ→Δ→Σ→Ω step ── */
int rmr_policy_tick(rmr_policy_state_t *ps, rmr_u32 event) {
    if (!ps) return RMR_KERNEL_ERR_STATE;

    /* ψ → χ: observe event */
    ps->entropy = s_entropy_estimate(ps);

    /* ρ: absorb event noise */
    ps->phi_state ^= (rmr_u64)event * RMR_PHI32;

    /* Δ: transmute via φ-step */
    ps->phi_state = rmr_lowlevel_phi_step(ps->phi_state, ps->coherence);

    /* Σ: update coherence */
    rmr_u32 anti_e = 64u - (ps->entropy & 63u);  /* 64 - entropy ≈ coherence */
    ps->coherence = rmr_lowlevel_fold32(ps->coherence, anti_e,
                                        (rmr_u32)ps->phi_state, event);

    /* Φ_ethica = anti_entropy × coherence / 64 */
    ps->ethica = (anti_e * (ps->coherence & 0xFFu)) >> 6u;
    if (ps->ethica > RMR_POLICY_ETHICA_MAX) ps->ethica = RMR_POLICY_ETHICA_MAX;

    /* Ω: cycle completion */
    ps->cycle = (ps->cycle + 1u) % 6u;

    return RMR_KERNEL_OK;
}

rmr_u32 rmr_policy_get_ethica(const rmr_policy_state_t *ps) {
    if (!ps) return 0u;
    return ps->ethica;
}

/* ── path selection: highest weight under phi-gating ── */
rmr_u32 rmr_policy_select_path(const rmr_policy_state_t *ps, rmr_u32 path_count) {
    if (!ps || !path_count) return 0u;
    if (path_count > RMR_POLICY_MAX_PATHS) path_count = RMR_POLICY_MAX_PATHS;

    rmr_u32 best_idx = 0u;
    rmr_u32 best_w   = 0u;
    /* phi-gated scan: start at phi_state % path_count, scan forward */
    rmr_u32 start = (rmr_u32)(ps->phi_state % path_count);
    for (rmr_u32 i = 0; i < path_count; i++) {
        rmr_u32 idx = (start + i) % path_count;
        rmr_u32 w   = ps->path_weights[idx];
        /* gate by ethica: high ethica → prefer high-weight paths */
        rmr_u32 gated = (w * ps->ethica) >> 8u;
        if (gated > best_w) { best_w = gated; best_idx = idx; }
    }
    return best_idx;
}

rmr_u32 rmr_policy_cell_priority(const rmr_policy_state_t *ps, rmr_u32 cell_idx) {
    if (!ps || cell_idx >= RMR_POLICY_MAX_CELLS) return 0u;
    return (rmr_u32)ps->cell_priority[cell_idx];
}

/* ── phi-spiral (ToroidΔπφ): state evolution on toroid ── */
/*  Toroidal: φ(n+1) = φ(n) × PHI32 mod (2^64), folded into [0, DIM_CUBE) */
rmr_u64 rmr_policy_phi_spiral(rmr_u64 state, int step) {
    for (int i = 0; i < step; i++) {
        state = (state * (rmr_u64)RMR_PHI32) ^ (rmr_u64)RMR_TRINITY633;
        state ^= state >> 33u;
        state *= 0xFF51AFD7ED558CCDull;
        state ^= state >> 33u;
    }
    return state;
}
