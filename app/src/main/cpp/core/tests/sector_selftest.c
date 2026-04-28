#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "core/sector.h"

static const uint8_t kInput[] = {
    0x10, 0x22, 0x37, 0x41, 0x58, 0x63, 0x77, 0x89,
    0x9A, 0xAB, 0xBC, 0xCD, 0xDE, 0xEF, 0xF1, 0x04,
};

static const vectra_sector_out kExpected = {
    .hash64 = UINT64_C(4976857866249629387),
    .crc32 = UINT32_C(1080243643),
    .coherence_q16 = UINT32_C(13107),
    .entropy_q16 = UINT32_C(61440),
    .last_entropy_milli = UINT32_C(937),
    .last_invariant_milli = UINT32_C(199),
};

static int assert_output(const char* label, const vectra_sector_out* got) {
    if (got->hash64 != kExpected.hash64) {
        fprintf(stderr, "[%s] hash64 mismatch: got=%llu expected=%llu\n", label,
                (unsigned long long)got->hash64,
                (unsigned long long)kExpected.hash64);
        return 1;
    }
    if (got->crc32 != kExpected.crc32) {
        fprintf(stderr, "[%s] crc32 mismatch: got=%u expected=%u\n", label,
                got->crc32, kExpected.crc32);
        return 1;
    }
    if (got->coherence_q16 != kExpected.coherence_q16) {
        fprintf(stderr,
                "[%s] coherence_q16 mismatch: got=%u expected=%u\n",
                label, got->coherence_q16, kExpected.coherence_q16);
        return 1;
    }
    if (got->entropy_q16 != kExpected.entropy_q16) {
        fprintf(stderr, "[%s] entropy_q16 mismatch: got=%u expected=%u\n",
                label, got->entropy_q16, kExpected.entropy_q16);
        return 1;
    }
    if (got->last_entropy_milli != kExpected.last_entropy_milli) {
        fprintf(stderr,
                "[%s] last_entropy_milli mismatch: got=%u expected=%u\n",
                label, got->last_entropy_milli, kExpected.last_entropy_milli);
        return 1;
    }
    if (got->last_invariant_milli != kExpected.last_invariant_milli) {
        fprintf(stderr,
                "[%s] last_invariant_milli mismatch: got=%u expected=%u\n",
                label, got->last_invariant_milli,
                kExpected.last_invariant_milli);
        return 1;
    }
    return 0;
}

typedef struct thread_ctx {
    vectra_sector_out out;
    int rc;
} thread_ctx;

static void* run_in_thread(void* arg) {
    thread_ctx* ctx = (thread_ctx*)arg;
    memset(&ctx->out, 0, sizeof(ctx->out));
    run_sector(kInput, sizeof(kInput), &ctx->out);
    ctx->rc = assert_output("parallel", &ctx->out);
    return NULL;
}

int main(void) {
    vectra_sector_out out0;
    vectra_sector_out out1;

    memset(&out0, 0, sizeof(out0));
    memset(&out1, 0, sizeof(out1));

    run_sector(kInput, sizeof(kInput), &out0);
    if (assert_output("serial#1", &out0) != 0) {
        return 1;
    }

    run_sector(kInput, sizeof(kInput), &out1);
    if (assert_output("serial#2", &out1) != 0) {
        return 1;
    }

    if (memcmp(&out0, &out1, sizeof(out0)) != 0) {
        fprintf(stderr, "serial outputs differ unexpectedly\n");
        return 1;
    }

    thread_ctx a;
    thread_ctx b;
    pthread_t ta;
    pthread_t tb;

    memset(&a, 0, sizeof(a));
    memset(&b, 0, sizeof(b));

    if (pthread_create(&ta, NULL, run_in_thread, &a) != 0) {
        perror("pthread_create(ta)");
        return 1;
    }
    if (pthread_create(&tb, NULL, run_in_thread, &b) != 0) {
        perror("pthread_create(tb)");
        (void)pthread_join(ta, NULL);
        return 1;
    }

    (void)pthread_join(ta, NULL);
    (void)pthread_join(tb, NULL);

    if (a.rc != 0 || b.rc != 0) {
        return 1;
    }
    if (memcmp(&a.out, &b.out, sizeof(a.out)) != 0) {
        fprintf(stderr, "parallel outputs differ unexpectedly\n");
        return 1;
    }

    puts("sector_selftest: ok");
    return 0;
}
