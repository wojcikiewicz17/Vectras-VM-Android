#ifndef VECTRA_CORE_ARCH_PRIMITIVES_H
#define VECTRA_CORE_ARCH_PRIMITIVES_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Primitive kernels for low-level sector processing.
 *
 * Fallback policy:
 * - Default implementation is always C and available for every ABI.
 * - ABI-specific ASM may override this contract if/when validated.
 *
 * ABI note:
 * - Arm64 fast path can be enabled with external assembly object files.
 * - Arm32/x86/x86_64 keep C fallback unless dedicated ASM is wired.
 */
#if defined(__aarch64__)
#define VECTRA_PRIMITIVES_ASM_CANDIDATE 1
#else
#define VECTRA_PRIMITIVES_ASM_CANDIDATE 0
#endif

uint64_t vectra_hash64(const uint8_t* data, size_t len);
uint32_t vectra_crc32(const uint8_t* data, size_t len);
uint32_t vectra_entropy_q16(const uint8_t* data, size_t len);
uint32_t vectra_coherence_q16(const uint8_t* data, size_t len);

#ifdef __cplusplus
}
#endif

#endif
