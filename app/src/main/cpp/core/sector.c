#include "core/sector.h"

#include "core/arch/primitives.h"

void run_sector(const uint8_t* data, size_t len, vectra_sector_out* out) {
    if (out == (void*)0) {
        return;
    }

    if (data == (void*)0 || len == 0U) {
        out->hash64 = 0U;
        out->crc32 = 0U;
        out->coherence_q16 = 0U;
        out->entropy_q16 = 0U;
        out->last_entropy_milli = 0U;
        out->last_invariant_milli = 1000U;
        return;
    }

    out->hash64 = vectra_hash64(data, len);
    out->crc32 = vectra_crc32(data, len);
    out->coherence_q16 = vectra_coherence_q16(data, len);
    out->entropy_q16 = vectra_entropy_q16(data, len);

    out->last_entropy_milli = (out->entropy_q16 * 1000U) >> 16U;
    out->last_invariant_milli = (out->coherence_q16 * 1000U) >> 16U;
}
