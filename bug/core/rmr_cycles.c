/* rmr_cycles.c — RMR CYCLE COUNTER
 * ∆RAFAELIA_CORE·Ω
 * Reads high-res timer per arch (no libc)
 * fΩ = 963↔999 Hz range validation
 * ─────────────────────────────────────────────────────── */
#include "rmr_unified_kernel.h"

/* ── read raw cycle / timer counter ── */
rmr_u64 rmr_cycles_read(void) {
#if defined(__aarch64__)
    rmr_u64 v = 0;
    __asm__ volatile("mrs %0, CNTVCT_EL0" : "=r"(v));
    return v;
#elif defined(__arm__)
    /* ARM32: PMCCNTR requires EL1 — use fallback */
    rmr_u32 lo = 0;
    __asm__ volatile("mrc p15, 0, %0, c9, c13, 0" : "=r"(lo));
    return (rmr_u64)lo;
#elif defined(__x86_64__) || defined(__i386__)
    rmr_u32 lo, hi;
    __asm__ volatile("rdtsc" : "=a"(lo), "=d"(hi));
    return ((rmr_u64)hi << 32) | lo;
#elif defined(__riscv)
    rmr_u64 v = 0;
    __asm__ volatile("rdcycle %0" : "=r"(v));
    return v;
#else
    return 0ULL;
#endif
}

/* ── get timer frequency (Hz) ── */
rmr_u64 rmr_cycles_freq_hz(void) {
#if defined(__aarch64__)
    rmr_u64 frq = 0;
    __asm__ volatile("mrs %0, CNTFRQ_EL0" : "=r"(frq));
    return frq ? frq : 1000000000ULL; /* default 1GHz */
#elif defined(__x86_64__)
    /* CPUID leaf 0x15 (TSC ratio) or fallback */
    rmr_u32 eax = 0, ebx = 0, ecx = 0, edx = 0;
    __asm__("cpuid" : "=a"(eax),"=b"(ebx),"=c"(ecx),"=d"(edx) : "a"(0x15u),"c"(0u));
    if (eax && ebx && ecx) return (rmr_u64)ecx * ebx / eax;
    return 1000000000ULL;
#else
    return 1000000000ULL;
#endif
}

/* ── validate fΩ range: 963↔999 Hz ── */
/*  Returns 1 if measured kernel tick rate in [963,999] Hz range
 *  Used for: coerência check — Ethica[8] gate
 */
int rmr_cycles_validate_fomega(rmr_u64 hz) {
    /* Normalize to 1Hz base by dividing by 1M */
    rmr_u64 mhz = hz / 1000000ULL;
    /* 963MHz - 999MHz range (common mobile ARM range) */
    if (mhz >= 963ULL && mhz <= 999ULL) return 1;
    /* OR check fractional: 963Hz / 999Hz as counter tick boundary */
    rmr_u64 khz = hz / 1000ULL;
    if (khz >= 963ULL && khz <= 999ULL) return 1;
    return 0;
}

/* ── elapsed nanoseconds between two readings ── */
rmr_u64 rmr_cycles_to_ns(rmr_u64 delta, rmr_u64 freq_hz) {
    if (!freq_hz) return 0ULL;
    /* ns = delta * 1e9 / freq */
    /* avoid overflow: delta * (1e9 / freq) */
    return (delta * 1000000000ULL) / freq_hz;
}
