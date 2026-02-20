/* rmr_hw_detect.c — ANDROID-SAFE HARDWARE DETECT
 * ∆RAFAELIA_CORE·Ω
 * Estratégia:
 *   1. getauxval(AT_HWCAP/AT_HWCAP2) — Bionic safe EL0
 *   2. /proc/cpuinfo fallback
 *   3. SIGILL probe para mrs *_EL1 com setjmp/longjmp guard
 *   4. Nunca crasha em userland Android
 * ─────────────────────────────────────────────────────── */
#include "rmr_unified_kernel.h"

/* system headers — android bionic only */
#if defined(__ANDROID__) || defined(ANDROID)
#  include <sys/auxv.h>      /* getauxval */
#  include <signal.h>        /* sigaction, SIGILL */
#  include <setjmp.h>        /* sigsetjmp / siglongjmp */
#  include <sys/types.h>     /* off_t */
#  include <fcntl.h>         /* open */
#  include <unistd.h>        /* read, close */
#  define RMR_ON_ANDROID 1
#else
#  define RMR_ON_ANDROID 0
#endif

/* ── HWCAP bits (from linux/auxvec.h) ── */
#define RMR_HWCAP_FP          (1u <<  0)
#define RMR_HWCAP_ASIMD       (1u <<  1)
#define RMR_HWCAP_CRC32       (1u <<  7)
#define RMR_HWCAP_AES         (1u <<  3)
#define RMR_HWCAP_SHA1        (1u <<  5)
#define RMR_HWCAP_SHA2        (1u <<  6)
#define RMR_HWCAP_ATOMICS     (1u <<  8)
#define RMR_HWCAP_SVE         (1u << 22)
#define RMR_HWCAP_PMULL       (1u <<  4)
#define RMR_HWCAP2_MTE        (1u <<  8)
#define RMR_HWCAP2_BTI        (1u << 17)

/* x86 HWCAP */
#define RMR_HWCAP_X86_SSE     (1u << 25)
#define RMR_HWCAP_X86_SSE2    (1u << 26)

/* ── SIGILL guard for EL1 sysreg probing ── */
#if RMR_ON_ANDROID && defined(__aarch64__)
static sigjmp_buf s_sigill_jmp;
static volatile int s_sigill_hit = 0;

static void s_sigill_handler(int sig) {
    (void)sig;
    s_sigill_hit = 1;
    siglongjmp(s_sigill_jmp, 1);
}

/* Try to read a system register; returns 0 on SIGILL */
static rmr_u64 s_safe_mrs_midr(void) {
    struct sigaction sa, old_sa;
    sa.sa_handler = s_sigill_handler;
    sa.sa_flags   = SA_RESETHAND;
    /* sigemptyset */
    for (int i = 0; i < 4; i++)
        ((unsigned long*)&sa.sa_mask)[i] = 0UL;

    s_sigill_hit = 0;
    sigaction(SIGILL, &sa, &old_sa);

    rmr_u64 midr = 0ULL;
    if (sigsetjmp(s_sigill_jmp, 1) == 0) {
        /* EL1 read — may SIGILL in EL0 */
        __asm__ volatile("mrs %0, MIDR_EL1" : "=r"(midr));
    } else {
        midr = 0ULL;   /* SIGILL caught */
    }
    sigaction(SIGILL, &old_sa, (void*)0);
    return midr;
}

/* Safe CTR_EL0 — always valid in EL0 */
static rmr_u64 s_safe_mrs_ctr(void) {
    rmr_u64 ctr = 0;
    __asm__ volatile("mrs %0, CTR_EL0" : "=r"(ctr));
    return ctr;
}
static rmr_u64 s_safe_mrs_cntfrq(void) {
    rmr_u64 v = 0;
    __asm__ volatile("mrs %0, CNTFRQ_EL0" : "=r"(v));
    return v;
}
#endif /* RMR_ON_ANDROID && __aarch64__ */

/* ── /proc/cpuinfo scanner ── */
#if RMR_ON_ANDROID
static int s_cpuinfo_has(const char *feature) {
    /* open /proc/cpuinfo and scan for "Features" line */
    int fd = open("/proc/cpuinfo", 0 /* O_RDONLY */);
    if (fd < 0) return 0;
    char buf[4096];
    int n = (int)read(fd, buf, sizeof(buf) - 1);
    close(fd);
    if (n <= 0) return 0;
    buf[n] = '\0';

    /* Find "Features" line */
    const char *p = buf;
    while (*p) {
        if (p[0]=='F' && p[1]=='e' && p[2]=='a' && p[3]=='t') {
            while (*p && *p != ':') p++;
            if (*p == ':') p++;
            /* scan tokens */
            const char *fn = feature;
            while (*p && *p != '\n') {
                /* skip spaces */
                while (*p == ' ' || *p == '\t') p++;
                /* match token */
                const char *q = fn;
                const char *start = p;
                int match = 1;
                while (*q && *p && *p != ' ' && *p != '\t' && *p != '\n') {
                    if (*p != *q) { match = 0; break; }
                    p++; q++;
                }
                if (match && !*q && (*p==' '||*p=='\t'||*p=='\n'||!*p))
                    return 1;
                /* skip to end of token */
                while (*p && *p != ' ' && *p != '\t' && *p != '\n') p++;
            }
            break;
        }
        while (*p && *p != '\n') p++;
        if (*p) p++;
    }
    return 0;
}
#endif

/* ── main detect function ── */
int rmr_hw_detect_fill(rmr_jni_capabilities_t *caps) {
    if (!caps) return RMR_KERNEL_ERR_CAPS;

    caps->feature_mask    = 0u;
    caps->feature_bits_hi = 0u;
    caps->cache_line_bytes = 64u;   /* safe default */
    caps->page_bytes       = 4096u; /* safe default */
    caps->gpio_word_bits   = 32u;
    caps->gpio_pin_stride  = 1u;

#if defined(__aarch64__)
    caps->arch_id = RMR_ARCH_ARM64;

#if RMR_ON_ANDROID
    /* ── Primary: getauxval (always safe EL0) ── */
    unsigned long hwcap  = getauxval(16UL /*AT_HWCAP*/);
    unsigned long hwcap2 = getauxval(26UL /*AT_HWCAP2*/);

    if (hwcap & RMR_HWCAP_ASIMD)   caps->feature_mask |= RMR_FEAT_SIMD;
    if (hwcap & RMR_HWCAP_CRC32)   caps->feature_mask |= RMR_FEAT_CRC32;
    if (hwcap & RMR_HWCAP_AES)     caps->feature_mask |= RMR_FEAT_AES;
    if (hwcap & RMR_HWCAP_SHA1)    caps->feature_mask |= RMR_FEAT_SHA1;
    if (hwcap & RMR_HWCAP_SHA2)    caps->feature_mask |= RMR_FEAT_SHA2;
    if (hwcap & RMR_HWCAP_ATOMICS) caps->feature_mask |= RMR_FEAT_ATOMICS;
    if (hwcap & RMR_HWCAP_FP)      caps->feature_mask |= RMR_FEAT_FP64;
    if (hwcap & RMR_HWCAP_SVE)     caps->feature_mask |= RMR_FEAT_SVE;
    if (hwcap & RMR_HWCAP_PMULL)   caps->feature_mask |= RMR_FEAT_PMULL;
    if (hwcap2 & RMR_HWCAP2_MTE)   caps->feature_mask |= RMR_FEAT_MTE;
    if (hwcap2 & RMR_HWCAP2_BTI)   caps->feature_mask |= RMR_FEAT_BTI;

    /* ── Secondary: /proc/cpuinfo (if hwcap incomplete) ── */
    if (!caps->feature_mask) {
        if (s_cpuinfo_has("crc32"))    caps->feature_mask |= RMR_FEAT_CRC32;
        if (s_cpuinfo_has("aes"))      caps->feature_mask |= RMR_FEAT_AES;
        if (s_cpuinfo_has("sha1"))     caps->feature_mask |= RMR_FEAT_SHA1;
        if (s_cpuinfo_has("sha2"))     caps->feature_mask |= RMR_FEAT_SHA2;
        if (s_cpuinfo_has("asimd"))    caps->feature_mask |= RMR_FEAT_SIMD;
    }

    /* ── CTR_EL0 → cache line (safe EL0) ── */
    rmr_u64 ctr = s_safe_mrs_ctr();
    rmr_u32 dminline = (rmr_u32)((ctr >> 16) & 0xFu);
    if (dminline) caps->cache_line_bytes = 4u << dminline;

    /* ── TSC freq from CNTFRQ_EL0 (safe EL0) ── */
    caps->tsc_hz = s_safe_mrs_cntfrq();

    /* ── MIDR_EL1: guarded by SIGILL probe ── */
    caps->midr_raw = s_safe_mrs_midr();  /* 0 if trapped */

    /* ── reg signatures from safe regs ── */
    caps->reg_signature_0 = (rmr_u32)(ctr & 0xFFFFFFFFu);
    caps->reg_signature_1 = (rmr_u32)(caps->tsc_hz & 0xFFFFFFFFu);
    caps->reg_signature_2 = (rmr_u32)(caps->midr_raw & 0xFFFFFFFFu);

#else
    /* non-Android ARM64: direct sysreg access (bare-metal / Linux EL0 ok) */
    rmr_u64 midr = 0; __asm__("mrs %0, MIDR_EL1" : "=r"(midr));
    caps->midr_raw = midr;
    rmr_u64 ctr  = 0; __asm__("mrs %0, CTR_EL0"  : "=r"(ctr));
    rmr_u32 dm = (rmr_u32)((ctr >> 16) & 0xFu);
    if (dm) caps->cache_line_bytes = 4u << dm;
    rmr_u64 frq = 0; __asm__("mrs %0, CNTFRQ_EL0" : "=r"(frq));
    caps->tsc_hz = frq;
    caps->feature_mask = RMR_FEAT_SIMD | RMR_FEAT_FP64;
    caps->reg_signature_0 = (rmr_u32)(ctr & 0xFFFFFFFFu);
    caps->reg_signature_1 = (rmr_u32)(frq & 0xFFFFFFFFu);
    caps->reg_signature_2 = (rmr_u32)(midr & 0xFFFFFFFFu);
#endif

#elif defined(__arm__)
    caps->arch_id = RMR_ARCH_ARM32;
#if RMR_ON_ANDROID
    unsigned long hwcap = getauxval(16UL);
    if (hwcap & (1u << 12)) caps->feature_mask |= RMR_FEAT_SIMD; /* NEON */
    if (hwcap & (1u << 28)) caps->feature_mask |= RMR_FEAT_AES;
    if (hwcap & (1u << 29)) caps->feature_mask |= RMR_FEAT_PMULL;
    if (hwcap & (1u << 30)) caps->feature_mask |= RMR_FEAT_SHA1;
    if (hwcap & (1u << 31)) caps->feature_mask |= RMR_FEAT_SHA2;
#endif
    caps->cache_line_bytes = 32u;

#elif defined(__x86_64__)
    caps->arch_id = RMR_ARCH_X86_64;
    /* CPUID */
    rmr_u32 eax, ebx, ecx, edx;
    __asm__("cpuid" : "=a"(eax),"=b"(ebx),"=c"(ecx),"=d"(edx)
                    : "a"(1u), "c"(0u));
    if (edx & (1u<<25)) caps->feature_mask |= RMR_FEAT_SIMD;  /* SSE */
    if (ecx & (1u<<0))  caps->feature_mask |= RMR_FEAT_SHA2;  /* SSE3 */
    if (ecx & (1u<<20)) caps->feature_mask |= RMR_FEAT_CRC32; /* SSE4.2 CRC32 */
    if (ecx & (1u<<25)) caps->feature_mask |= RMR_FEAT_AES;
    if (ecx & (1u<<30)) caps->feature_mask |= RMR_FEAT_RDRAND;
    caps->reg_signature_0 = eax;
    caps->reg_signature_1 = ecx;
    caps->reg_signature_2 = edx;
    caps->cache_line_bytes = 64u;

    /* CPUID leaf 7 for AVX2/AVX512/BMI2 */
    __asm__("cpuid" : "=a"(eax),"=b"(ebx),"=c"(ecx),"=d"(edx)
                    : "a"(7u), "c"(0u));
    if (ebx & (1u << 5))  caps->feature_bits_hi |= (1u << 0); /* AVX2 */
    if (ebx & (1u << 16)) caps->feature_bits_hi |= (1u << 1); /* AVX512F */
    if (ebx & (1u << 8))  caps->feature_bits_hi |= (1u << 2); /* BMI2 */

#elif defined(__i386__)
    caps->arch_id = RMR_ARCH_X86_32;
    caps->cache_line_bytes = 32u;

#elif defined(__riscv) && (__riscv_xlen == 64)
    caps->arch_id = RMR_ARCH_RISCV64;
    /* RISC-V: parse /proc/cpuinfo ISA string */
#if RMR_ON_ANDROID
    if (s_cpuinfo_has("c")) caps->feature_bits_hi |= (1u << 3);
    if (s_cpuinfo_has("m")) caps->feature_bits_hi |= (1u << 4);
    if (s_cpuinfo_has("a")) caps->feature_bits_hi |= (1u << 5);
    if (s_cpuinfo_has("f")) caps->feature_bits_hi |= (1u << 6);
    if (s_cpuinfo_has("d")) caps->feature_bits_hi |= (1u << 7);
    if (s_cpuinfo_has("v")) caps->feature_bits_hi |= (1u << 8);
#endif

#else
    caps->arch_id = RMR_ARCH_UNKNOWN;
#endif

    return RMR_KERNEL_OK;
}
