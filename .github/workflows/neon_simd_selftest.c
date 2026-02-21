/* neon_simd_selftest.c — Verify NEON/SIMD acceleration path */
#include "rmr_neon_simd.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

static int test_xor_fold(void) {
    uint8_t data[16] = {0xDE,0xAD,0xBE,0xEF,0x01,0x02,0x03,0x04,
                        0x11,0x22,0x33,0x44,0x55,0x66,0x77,0x88};
    uint32_t r = rmr_neon_xor_fold32(data, 16);
    /* scalar reference */
    uint32_t ref = 0;
    for(int i = 0; i < 16; ++i) ref ^= data[i];
    if (r != ref) { fprintf(stderr, "FAIL xor_fold: got 0x%08X want 0x%08X\n", r, ref); return 1; }
    return 0;
}

static int test_crc32c(void) {
    uint8_t data[] = "RAFAELIA";
    uint32_t crc = rmr_neon_crc32c(0u, data, (uint32_t)strlen((char*)data));
    if (crc == 0) { fprintf(stderr, "FAIL crc32c: zero CRC\n"); return 1; }
    return 0;
}

static int test_phi_step(void) {
    uint32_t states[4] = {1u, 2u, 3u, 4u};
    rmr_neon_phi_step_bulk(states, 4);
    for(int i = 0; i < 4; ++i) {
        if (states[i] == 0) { fprintf(stderr, "FAIL phi_step: zero state[%d]\n", i); return 1; }
    }
    return 0;
}

int main(void) {
    int failed = 0;
    failed += test_xor_fold();
    failed += test_crc32c();
    failed += test_phi_step();
    if (!failed) { printf("NEON_SIMD_SELFTEST: OK\n"); return 0; }
    fprintf(stderr, "NEON_SIMD_SELFTEST: %d FAILURES\n", failed);
    return 1;
}
