/* rmr_math_fabric.c — RMR MATH FABRIC
 * ∆RAFAELIA_CORE·Ω
 * OWLψ: analyzer + dispatch
 * Matriz 10×10×10 + fractais ocultos layers 0-3
 * Fibonacci-Rafael modificada (Fᴿ)
 * Stack42H / Bitraf64 dispatch
 * ─────────────────────────────────────────────────────── */
#include "rmr_unified_kernel.h"
#include "rmr_lowlevel.h"

/* ── 10×10×10 Cube: flat index → (x,y,z) ── */
static void s_cube_coords(rmr_u32 idx, rmr_u32 *x, rmr_u32 *y, rmr_u32 *z) {
    *x = idx / 100u;
    *y = (idx / 10u) % 10u;
    *z = idx % 10u;
}

/* ── Fibonacci-Rafael modified: Fᴿ(n) seeded by coordinate φ ── */
rmr_u64 rmr_fabric_fib_rafael(rmr_u32 n, rmr_u32 x, rmr_u32 y, rmr_u32 z) {
    rmr_u32 seed = rmr_lowlevel_fold32(n, x * 100u + y * 10u + z, RMR_PHI32, RMR_TRINITY633);
    rmr_u64 a = 1ULL + (seed & 0xFu);
    rmr_u64 b = 1ULL + ((seed >> 4u) & 0xFu);
    for (rmr_u32 i = 2; i <= n && i < 64u; i++) {
        rmr_u64 nc = (a + b) * (rmr_u64)RMR_PHI32;
        a = b; b = nc;
    }
    return b;
}

/* ── Fractal layer expansion: base cube → layer 0..3 ── */
/*  layer_crc = base_crc * Fᴿ(layer)
 *  Used by: ZiprafEngine.expandFractals() C counterpart
 */
rmr_u32 rmr_fabric_fractal_crc(rmr_u32 base_crc, rmr_u32 layer, rmr_u32 cell_idx) {
    rmr_u32 x, y, z;
    s_cube_coords(cell_idx % 1000u, &x, &y, &z);
    rmr_u64 fib = rmr_fabric_fib_rafael(layer + 1u, x, y, z);
    return (rmr_u32)((rmr_u64)base_crc * fib);
}

rmr_u64 rmr_fabric_fractal_offset(rmr_u64 base_off, rmr_u32 layer, rmr_u32 cell_idx) {
    rmr_u32 x, y, z;
    s_cube_coords(cell_idx % 1000u, &x, &y, &z);
    rmr_u64 fib = rmr_fabric_fib_rafael(layer + 1u, x, y, z);
    return base_off ^ (fib * (rmr_u64)RMR_PHI32);
}

/* ── OWLψ Analyzer: score a data block for pattern coherence ──
 *  Returns: coherence score 0..255
 *  Principle: high score = low entropy + phi-alignment
 */
rmr_u32 rmr_fabric_owl_analyze(const rmr_u8 *data, rmr_u32 len) {
    if (!data || len < 4u) return 0u;

    /* byte frequency histogram (truncated to first 256 bytes) */
    rmr_u32 freq[16] = {0};
    rmr_u32 scan = len < 256u ? len : 256u;
    for (rmr_u32 i = 0; i < scan; i++)
        freq[data[i] & 0xFu]++;

    /* entropy proxy: deviation from uniform */
    rmr_u32 expected = scan / 16u + 1u;
    rmr_u32 deviation = 0u;
    for (int i = 0; i < 16; i++) {
        rmr_u32 d = freq[i] > expected ? freq[i] - expected : expected - freq[i];
        deviation += d;
    }

    /* phi-alignment: check if header/tail align to PHI32 pattern */
    rmr_u32 head = (rmr_u32)((data[0] | (data[1]<<8) | (data[2]<<16) | (data[3]<<24)));
    rmr_u32 phi_align = (head * RMR_PHI32) >> 28u;  /* top 4 bits of phi-product */

    /* score: low deviation = high coherence */
    rmr_u32 entropy_score = deviation > 256u ? 0u : (256u - deviation);
    rmr_u32 phi_score     = phi_align * 16u;
    return (entropy_score + phi_score) >> 1u;
}

/* ── Stack42H dispatch: map 42-slot hotpath table ──
 *  Used by: JNI fast-path to select optimal C or ASM path
 */
typedef rmr_u32 (*rmr_hotpath_fn)(rmr_u32, rmr_u32);

static rmr_u32 s_noop(rmr_u32 a, rmr_u32 b) {
    return a ^ b;
}

static rmr_hotpath_fn s_stack42[42];
static int s_stack42_init = 0;

static void s_init_stack42(void) {
    if (s_stack42_init) return;
    for (int i = 0; i < 42; i++) s_stack42[i] = s_noop;
    /* slot 0: phi-fold entry (2-arg wrapper) */
    /* slot 1: crc32 entry */
    /* slots filled dynamically via rmr_fabric_stack42_register */
    s_stack42_init = 1;
}

void rmr_fabric_stack42_register(rmr_u32 slot, void *fn) {
    s_init_stack42();
    if (slot < 42u && fn)
        s_stack42[slot] = (rmr_hotpath_fn)fn;
}

rmr_u32 rmr_fabric_stack42_call(rmr_u32 slot, rmr_u32 a, rmr_u32 b) {
    s_init_stack42();
    if (slot >= 42u) return 0u;
    return s_stack42[slot](a, b);
}

/* ── Bitraf64: 64-slot bitfield for feature routing ── */
static rmr_u64 s_bitraf64_state = 0ULL;

void rmr_fabric_bitraf64_set(rmr_u32 bit) {
    if (bit < 64u) s_bitraf64_state |= (1ULL << bit);
}
void rmr_fabric_bitraf64_clear(rmr_u32 bit) {
    if (bit < 64u) s_bitraf64_state &= ~(1ULL << bit);
}
int rmr_fabric_bitraf64_test(rmr_u32 bit) {
    if (bit >= 64u) return 0;
    return (s_bitraf64_state >> bit) & 1u;
}
rmr_u64 rmr_fabric_bitraf64_state(void) {
    return s_bitraf64_state;
}
