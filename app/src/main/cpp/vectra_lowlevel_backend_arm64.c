#include "vectra_lowlevel_backend.h"

#if defined(__aarch64__)
#include <arm_acle.h>
#include <arm_neon.h>
#endif

#include "rmr_lowlevel.h"

static uint32_t vectra_checksum32_arm64(const uint8_t* data, size_t len, uint32_t seed) {
    return rmr_lowlevel_checksum32(data, len, seed);
}

static uint32_t vectra_reduce_xor_arm64(const uint8_t* data, size_t len) {
    /*
     * Global contract: reduce_xor follows rmr_lowlevel_reduce_xor semantics
     * (byte-to-lane fold + rotate-left by 3 per byte), not plain XOR parity.
     */
    return rmr_lowlevel_reduce_xor(data, len);
}

#if defined(__aarch64__)
static uint32_t vectra_crc32c_arm64(uint32_t initial, const uint8_t* data, size_t len) {
    uint32_t crc = initial;
    size_t i = 0;
    for (; i + 8u <= len; i += 8u) {
        uint64_t v;
        __builtin_memcpy(&v, data + i, sizeof(v));
        crc = __crc32cd(crc, v);
    }
    for (; i < len; ++i) crc = __crc32cb(crc, data[i]);
    return crc;
}
#else
static uint32_t vectra_crc32c_arm64(uint32_t initial, const uint8_t* data, size_t len) {
    uint32_t crc = initial;
    for (size_t i = 0; i < len; ++i) {
        crc ^= data[i];
        for (uint32_t b = 0; b < 8u; ++b) {
            const uint32_t mask = (uint32_t)-(int32_t)(crc & 1u);
            crc = (crc >> 1u) ^ (0x82F63B78u & mask);
        }
    }
    return crc;
}
#endif

int vectra_backend_arm64_available(uint32_t simd_mask) {
    return (simd_mask & VECTRA_SIMD_NEON) != 0u;
}

void vectra_backend_bind_arm64(vectra_lowlevel_backend_vtable_t* out) {
    out->name = "arm64-neon-crc";
    out->reduce_xor = vectra_reduce_xor_arm64;
    out->checksum32 = vectra_checksum32_arm64;
    out->crc32c = vectra_crc32c_arm64;
}
