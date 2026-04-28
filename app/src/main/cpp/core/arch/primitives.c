#include "core/arch/primitives.h"

static uint64_t vectra_mix64(uint64_t x) {
    x ^= x >> 30;
    x *= UINT64_C(0xbf58476d1ce4e5b9);
    x ^= x >> 27;
    x *= UINT64_C(0x94d049bb133111eb);
    x ^= x >> 31;
    return x;
}

uint64_t vectra_hash64(const uint8_t* data, size_t len) {
    uint64_t acc = UINT64_C(0x9e3779b97f4a7c15) ^ (uint64_t)len;
    for (size_t i = 0; i < len; ++i) {
        acc ^= ((uint64_t)data[i] + UINT64_C(0x9e3779b97f4a7c15) + (acc << 6U) + (acc >> 2U));
        acc = vectra_mix64(acc);
    }
    return acc;
}

uint32_t vectra_crc32(const uint8_t* data, size_t len) {
    uint32_t crc = UINT32_C(0xffffffff);
    for (size_t i = 0; i < len; ++i) {
        crc ^= (uint32_t)data[i];
        for (uint32_t bit = 0; bit < 8; ++bit) {
            const uint32_t mask = (uint32_t)(-(int32_t)(crc & 1U));
            crc = (crc >> 1) ^ (UINT32_C(0xedb88320) & mask);
        }
    }
    return ~crc;
}

uint32_t vectra_entropy_q16(const uint8_t* data, size_t len) {
    if (len == 0U) {
        return 0U;
    }

    uint32_t counts[16] = {0};
    for (size_t i = 0; i < len; ++i) {
        counts[(uint32_t)(data[i] >> 4U)]++;
    }

    uint64_t accum = 0U;
    const uint64_t total = (uint64_t)len;
    for (uint32_t i = 0; i < 16U; ++i) {
        const uint64_t c = (uint64_t)counts[i];
        if (c == 0U) {
            continue;
        }
        const uint64_t p_q16 = (c << 16U) / total;
        accum += p_q16 * (UINT64_C(65536) - p_q16);
    }

    return (uint32_t)(accum >> 16U);
}

uint32_t vectra_coherence_q16(const uint8_t* data, size_t len) {
    if (len <= 1U) {
        return UINT32_C(65535);
    }

    uint32_t smooth = 0U;
    for (size_t i = 1; i < len; ++i) {
        const uint8_t a = data[i - 1U];
        const uint8_t b = data[i];
        const uint8_t d = (a > b) ? (uint8_t)(a - b) : (uint8_t)(b - a);
        if (d <= 12U) {
            smooth++;
        }
    }

    return (uint32_t)(((uint64_t)smooth << 16U) / (uint64_t)(len - 1U));
}
