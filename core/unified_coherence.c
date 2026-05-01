#include <math.h>
#include <stdint.h>
#include <stddef.h>
#include <string.h>

#define UC_DIM 7U
#define UC_ALPHA 0.25
#define UC_ATTRACTOR_CARDINALITY 42U
#define UC_FNV_PRIME 0x100000001B3ULL
#define UC_TAU 6.28318530717958647692

typedef struct {
    double s[UC_DIM];
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

static inline double frac01(double x) {
    double f = x - floor(x);
    return (f < 0.0) ? (f + 1.0) : f;
}

static inline uint64_t fnv1a_step(uint64_t h, uint8_t byte) {
    h ^= (uint64_t)byte;
    return h * UC_FNV_PRIME;
}

static uint32_t crc32c_sw_step(uint32_t crc, uint8_t byte) {
    uint32_t x = (crc ^ (uint32_t)byte);
    for (int i = 0; i < 8; ++i) {
        uint32_t mask = (uint32_t)-(int32_t)(x & 1U);
        x = (x >> 1) ^ (0x82F63B78U & mask);
    }
    return x;
}

static void toroidal_map(const UCInput* in, UCContext* ctx) {
    const double inv255 = 1.0 / 255.0;
    uint64_t acc = ctx->hash ^ (uint64_t)in->state;

    for (size_t i = 0; i < in->len; ++i) {
        acc = fnv1a_step(acc, in->data[i]);
    }

    const double b0 = (double)((acc >> 0) & 0xFFU) * inv255;
    const double b1 = (double)((acc >> 8) & 0xFFU) * inv255;
    const double b2 = (double)((acc >> 16) & 0xFFU) * inv255;
    const double b3 = (double)((acc >> 24) & 0xFFU) * inv255;
    const double b4 = (double)((acc >> 32) & 0xFFU) * inv255;
    const double b5 = (double)((acc >> 40) & 0xFFU) * inv255;
    const double b6 = (double)((acc >> 48) & 0xFFU) * inv255;

    ctx->s[0] = frac01(0.61803398875 * b0 + 0.38196601125 * ctx->coherence);
    ctx->s[1] = frac01(0.86602540378 * b1 + 0.13397459622 * ctx->entropy);
    ctx->s[2] = frac01(0.5 * (b2 + ctx->s[0]));
    ctx->s[3] = frac01(0.5 * (b3 + ctx->s[1]));
    ctx->s[4] = frac01(0.5 * (b4 + b5));
    ctx->s[5] = frac01(0.5 * (b6 + ctx->s[2]));
    ctx->s[6] = frac01((ctx->s[0] + ctx->s[3] + ctx->s[5]) * (1.0 / 3.0));
}

static double spectral_link(const UCContext* ctx) {
    const double dtheta = (ctx->s[0] - ctx->s[3]) * UC_TAU;
    const double dphi = (ctx->s[1] - ctx->s[4]) * UC_TAU;
    return sin(dtheta) * cos(dphi);
}

void uc_init(UCContext* ctx, uint64_t seed) {
    memset(ctx, 0, sizeof(*ctx));
    ctx->coherence = 0.5;
    ctx->entropy = 0.5;
    ctx->hash = seed ^ 0x9E3779B97F4A7C15ULL;
    ctx->crc = 0xFFFFFFFFU;
    ctx->state = 1U;
}

void uc_step(UCContext* ctx, const UCInput* in) {
    ctx->coherence = (1.0 - UC_ALPHA) * ctx->coherence + UC_ALPHA * in->coherence_in;
    ctx->entropy = (1.0 - UC_ALPHA) * ctx->entropy + UC_ALPHA * in->entropy_in;

    toroidal_map(in, ctx);

    const double phi = (1.0 - ctx->entropy) * ctx->coherence;
    for (size_t i = 0; i < in->len; ++i) {
        ctx->hash = fnv1a_step(ctx->hash, in->data[i]);
        ctx->crc = crc32c_sw_step(ctx->crc, in->data[i]);
    }

    const double link = spectral_link(ctx);
    const uint32_t attractor = (uint32_t)((ctx->s[6] * 1000003.0) + (phi * 4096.0)) % UC_ATTRACTOR_CARDINALITY;

    ctx->state = (in->state != 0U) ? (attractor + 1U) : 0U;
    if (ctx->state != 0U) {
        ctx->s[6] = frac01(ctx->s[6] + (0.25 * phi * link));
    }
}

#ifdef UC_SELFTEST
#include <stdio.h>
int main(void) {
    UCContext ctx;
    uc_init(&ctx, 0xDEADBEEFCAFEBABEULL);
    const uint8_t msg[] = "toroidal-linguistic-payload";
    UCInput in = {.coherence_in = 0.91, .entropy_in = 0.27, .data = msg, .len = sizeof(msg)-1, .state = 1};
    for (int i = 0; i < 4; ++i) uc_step(&ctx, &in);
    printf("state=%u coherence=%.6f entropy=%.6f s6=%.6f hash=%016llx crc=%08x\n",
           ctx.state, ctx.coherence, ctx.entropy, ctx.s[6],
           (unsigned long long)ctx.hash, ~ctx.crc);
    return 0;
}
#endif
