/* rmr_unified_kernel.h — RMR UNIFIED KERNEL
 * ∆RAFAELIA_CORE·Ω  R(t+1)=R(t)×Φ_ethica×E_Verbo×(√3/2)^(πφ)
 * Σ: núcleo determinístico — zero stdlib — multi-arch
 * Kernel contract: JNI ↔ C ↔ ASM ↔ HW
 * ─────────────────────────────────────────────────────── */
#ifndef RMR_UNIFIED_KERNEL_H
#define RMR_UNIFIED_KERNEL_H

/* ── primitive types (no stdint.h) ── */
#if defined(__LP64__) || defined(__aarch64__) || defined(__x86_64__) || defined(__riscv)
  typedef unsigned long long rmr_u64;
  typedef signed   long long rmr_i64;
  typedef unsigned int       rmr_u32;
  typedef signed   int       rmr_i32;
  typedef unsigned short     rmr_u16;
  typedef unsigned char      rmr_u8;
  #define RMR_PTR_BITS 64
#else
  typedef unsigned long  rmr_u64;
  typedef signed   long  rmr_i64;
  typedef unsigned int   rmr_u32;
  typedef signed   int   rmr_i32;
  typedef unsigned short rmr_u16;
  typedef unsigned char  rmr_u8;
  #define RMR_PTR_BITS 32
#endif

/* ── return codes ── */
#define RMR_KERNEL_OK             0
#define RMR_KERNEL_ERR_INIT      -1
#define RMR_KERNEL_ERR_STATE     -2
#define RMR_KERNEL_ERR_CAPS      -3
#define RMR_KERNEL_ERR_ARCH      -4
#define RMR_KERNEL_ERR_ALLOC     -5

/* magic for native OK check — MUST match NativeFastPath.NATIVE_OK_MAGIC = 0x56414343 */
#define RMR_UK_NATIVE_OK_MAGIC   0x56414343u  /* "VACC" — Java contract anchor */

/* ── architecture IDs ── */
#define RMR_ARCH_UNKNOWN   0x00u
#define RMR_ARCH_ARM64     0x01u
#define RMR_ARCH_ARM32     0x02u
#define RMR_ARCH_X86_64    0x03u
#define RMR_ARCH_X86_32    0x04u
#define RMR_ARCH_RISCV64   0x05u
#define RMR_ARCH_RISCV32   0x06u

/* ── feature bitmask ── */
#define RMR_FEAT_SIMD      (1u <<  0)
#define RMR_FEAT_CRC32     (1u <<  1)
#define RMR_FEAT_AES       (1u <<  2)
#define RMR_FEAT_SHA1      (1u <<  3)
#define RMR_FEAT_SHA2      (1u <<  4)
#define RMR_FEAT_ATOMICS   (1u <<  5)
#define RMR_FEAT_FP64      (1u <<  6)
#define RMR_FEAT_SVE       (1u <<  7)
#define RMR_FEAT_PMULL     (1u <<  8)
#define RMR_FEAT_BTI       (1u <<  9)
#define RMR_FEAT_MTE       (1u << 10)
#define RMR_FEAT_RDRAND    (1u << 11)
#define RMR_FEAT_PAUTH     (1u << 12)

/* ── phi constant (kernel seed) ── */
#define RMR_PHI32          0x9E3779B9u
#define RMR_TRINITY633     0x633u
#define RMR_STACK42        42u
#define RMR_BITRAF64       64u

/* ── capabilities struct ── */
typedef struct rmr_jni_capabilities {
    rmr_u32 signature;          /* "VCAP" = 0x50414356 */
    rmr_u32 pointer_bits;       /* 32 or 64            */
    rmr_u32 cache_line_bytes;   /* L1 cache line       */
    rmr_u32 page_bytes;         /* page size           */
    rmr_u32 feature_mask;       /* RMR_FEAT_*          */
    rmr_u32 reg_signature_0;    /* arch-specific sig   */
    rmr_u32 reg_signature_1;
    rmr_u32 reg_signature_2;
    rmr_u32 gpio_word_bits;
    rmr_u32 gpio_pin_stride;
    rmr_u32 arch_id;            /* RMR_ARCH_*          */
    rmr_u32 feature_bits_hi;    /* extended features   */
    rmr_u64 tsc_hz;             /* timer frequency     */
    rmr_u64 midr_raw;           /* MIDR / CPUID        */
    rmr_u64 phi_state;          /* φ-state accumulator */
} rmr_jni_capabilities_t;

/* ── kernel state ── */
typedef struct rmr_jni_kernel_state {
    rmr_u32              magic;       /* RMR_UK_NATIVE_OK_MAGIC */
    rmr_u32              init_flags;
    rmr_u32              cycle;       /* ψ→χ→ρ→Δ→Σ→Ω cycle counter */
    rmr_u32              coherence;   /* Φ_ethica: coerência */
    rmr_jni_capabilities_t caps;
} rmr_jni_kernel_state_t;

/* ── API ── */
#ifdef __cplusplus
extern "C" {
#endif

int rmr_jni_kernel_init(rmr_jni_kernel_state_t *state, rmr_u32 seed);
int rmr_jni_kernel_get_capabilities(rmr_jni_kernel_state_t *state,
                                    rmr_jni_capabilities_t  *out);
int rmr_jni_kernel_tick(rmr_jni_kernel_state_t *state);
rmr_u32 rmr_jni_phi_fold32(rmr_u32 a, rmr_u32 b, rmr_u32 c, rmr_u32 d);
rmr_u64 rmr_jni_fib_rafael(int n);
int rmr_jni_crc32c_sw(rmr_u32 seed, const rmr_u8 *data, rmr_u32 len, rmr_u32 *out);
int rmr_jni_crc32c_hw(rmr_u32 seed, const rmr_u8 *data, rmr_u32 len, rmr_u32 *out);

/* hw detect (internal) */
int rmr_hw_detect_fill(rmr_jni_capabilities_t *caps);

#ifdef __cplusplus
}
#endif

#endif /* RMR_UNIFIED_KERNEL_H */
