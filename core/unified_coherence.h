#ifndef UNIFIED_COHERENCE_H
#define UNIFIED_COHERENCE_H

#include <stddef.h>
#include <stdint.h>

typedef struct {
    double s[7];
    double coherence;
    double entropy;
    uint64_t hash;
    uint32_t crc;
    uint32_t state;
} UCContext;

typedef struct {
    double coherence_in;
    double entropy_in;
    const uint8_t* data;
    size_t len;
    uint32_t state;
} UCInput;

void uc_init(UCContext* ctx, uint64_t seed);
void uc_step(UCContext* ctx, const UCInput* in);

#endif
