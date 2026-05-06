#ifndef RAFCODE_PHI_LOWBASIC_H
#define RAFCODE_PHI_LOWBASIC_H

/*
 * RAFCODE PHI LOWBASIC HEADER
 * - sem libc
 * - caminho branchless por máscaras (hotfix ABI/bootstrap)
 * - registradores fixos para handoff de bootloader -> bootstrap -> kernel
 */

#include "rafcode_phi_abi.h"

#define RAFPHI_BOOT_MAGIC   0x52414642u /* RAFB */
#define RAFPHI_BOOT_VERSION 0x00010001u

/* Layout mínimo para handoff ABI sem libc. */
typedef struct {
  raf_u32 magic;
  raf_u32 version;
  raf_u32 arch;
  raf_u32 flags;
  raf_u64 in_ptr;
  raf_u64 out_ptr;
  raf_u64 words;
  raf_u64 crc_seed;
} rafphi_boot_handoff_t;

/* Flags branchless para estado do pipeline bootstrap. */
enum {
  RAFPHI_F_BOOT_OK         = 1u << 0,
  RAFPHI_F_BOOT_RETRY      = 1u << 1,
  RAFPHI_F_BOOT_DENY       = 1u << 2,
  RAFPHI_F_BOOT_ABI_MISM   = 1u << 3,
  RAFPHI_F_BOOT_CRC_FAIL   = 1u << 4,
  RAFPHI_F_BOOT_PTR_INVALID= 1u << 5
};


/*
 * Contrato de registradores (AArch64):
 * x0 = in_ptr, x1 = out_ptr, x2 = words, x3 = flags
 * x4 = crc, x5 = tmp, v0-v3 = payload, v4-v5 = invariantes
 */
#if defined(__aarch64__)
#define RAFPHI_LOWBASIC_A64_STEP()                                              \
  __asm__ __volatile__(                                                         \
    "prfm pldl1keep, [x0, #128]\n\t"                                          \
    "ld1 {v0.2d, v1.2d, v2.2d, v3.2d}, [x0], #64\n\t"                        \
    "fmul v0.2d, v0.2d, v4.d[0]\n\t"                                          \
    "fmla v1.2d, v1.2d, v5.d[0]\n\t"                                          \
    "fmov x5, d0\n\t"                                                         \
    "crc32cx w4, w4, x5\n\t"                                                 \
    "fmov x5, d7\n\t"                                                         \
    "cmp x5, #0\n\t"                                                          \
    "csetm x5, ne\n\t"                                                        \
    "and x3, x3, x5\n\t"                                                      \
    "st1 {v0.2d, v1.2d, v2.2d, v3.2d}, [x1], #64\n\t"                        \
    : : : "x0", "x1", "x3", "x4", "x5", "v0", "v1", "v2", "v3", "memory", "cc")
#endif

/*
 * Contrato de registradores (x86_64 SysV):
 * rdi = in_ptr, rsi = out_ptr, rdx = words, rcx = flags, r8d = crc
 */
#if defined(__x86_64__)
#define RAFPHI_LOWBASIC_X64_STEP()                                              \
  __asm__ __volatile__(                                                         \
    "movdqu (%%rdi), %%xmm0\n\t"                                              \
    "movdqu 16(%%rdi), %%xmm1\n\t"                                            \
    "movdqu 32(%%rdi), %%xmm2\n\t"                                            \
    "movdqu 48(%%rdi), %%xmm3\n\t"                                            \
    "add $64, %%rdi\n\t"                                                      \
    "movq %%xmm0, %%rax\n\t"                                                  \
    "crc32q %%rax, %%r8\n\t"                                                  \
    "movq %%xmm3, %%rax\n\t"                                                  \
    "neg %%rax\n\t"                                                           \
    "sbb %%rax, %%rax\n\t"                                                    \
    "and %%rax, %%rcx\n\t"                                                    \
    "movdqu %%xmm0, (%%rsi)\n\t"                                              \
    "movdqu %%xmm1, 16(%%rsi)\n\t"                                            \
    "movdqu %%xmm2, 32(%%rsi)\n\t"                                            \
    "movdqu %%xmm3, 48(%%rsi)\n\t"                                            \
    "add $64, %%rsi\n\t"                                                      \
    : : : "rax", "rdi", "rsi", "rcx", "r8", "xmm0", "xmm1", "xmm2", "xmm3", "memory", "cc")
#endif

/* Hotfix ABI: validação mínima de cabeçalho de handoff sem libc. */
static inline raf_u32 rafphi_boot_handoff_validate(const rafphi_boot_handoff_t *h) {
  raf_u32 m = (h && h->magic == RAFPHI_BOOT_MAGIC);
  raf_u32 v = (h && h->version >= RAFPHI_BOOT_VERSION);
  raf_u32 p = (h && h->in_ptr != 0u && h->out_ptr != 0u);
  raf_u32 a = (h && (h->arch == RAFPHI_ARCH_ARMV7 || h->arch == RAFPHI_ARCH_AARCH64));
  if ((m & v & p & a) != 0u) {
    return RAFPHI_F_BOOT_OK;
  }
  raf_u32 status = RAFPHI_F_BOOT_DENY;
  if (!p) status |= RAFPHI_F_BOOT_PTR_INVALID;
  if (!a) status |= RAFPHI_F_BOOT_ABI_MISM;
  return status;
}

#endif
