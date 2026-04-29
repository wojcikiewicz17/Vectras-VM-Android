#define _POSIX_C_SOURCE 200809L
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

#include "core/sector.h"

static uint64_t now_ns(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return ((uint64_t)ts.tv_sec * 1000000000ULL) + (uint64_t)ts.tv_nsec;
}

int main(void) {
    uint8_t data[256];
    for (size_t i = 0; i < sizeof(data); ++i) data[i] = (uint8_t)(i * 3u + 7u);

    vectra_sector_out out;
    memset(&out, 0, sizeof(out));

    const uint32_t iters = 100000;
    const uint64_t t0 = now_ns();
    for (uint32_t i = 0; i < iters; ++i) {
        run_sector(data, sizeof(data), &out);
    }
    const uint64_t t1 = now_ns();
    const uint64_t elapsed_ns = t1 - t0;
    const double avg_ns = (double)elapsed_ns / (double)iters;

    printf("benchmark_smoke: iters=%u total_ns=%llu avg_ns=%.2f hash64=%llu\n",
           iters,
           (unsigned long long)elapsed_ns,
           avg_ns,
           (unsigned long long)out.hash64);
    return 0;
}
