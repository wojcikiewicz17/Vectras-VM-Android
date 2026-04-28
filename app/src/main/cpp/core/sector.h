#ifndef VECTRA_CORE_SECTOR_H
#define VECTRA_CORE_SECTOR_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct vectra_sector_out {
    uint64_t hash64;
    uint32_t crc32;
    uint32_t coherence_q16;
    uint32_t entropy_q16;
    uint32_t last_entropy_milli;
    uint32_t last_invariant_milli;
} vectra_sector_out;

/*
 * Stable API contract:
 * - run_sector() is ABI-stable and always exported by C object code.
 * - Internals may use ABI-specific ASM primitives behind core/arch/primitives.h.
 * - If no ASM backend is enabled for the current ABI, C fallback is used.
 */
void run_sector(const uint8_t* data, size_t len, vectra_sector_out* out);

#ifdef __cplusplus
}
#endif

#endif
