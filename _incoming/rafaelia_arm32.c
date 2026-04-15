#include <stdio.h>
#include <stdint.h>
#include <time.h>
#include <arm_neon.h>

#define TORUS_SIZE 1024
#define ALPHA 0.25f
#define PHI_SCALE 1.4012f // PHI * sqrt(3)/2

typedef struct {
    float v[TORUS_SIZE] __attribute__((aligned(16)));
    uint64_t ns;
} rafaelia_t;

static inline uint64_t now_ns() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC_RAW, &ts);
    return (uint64_t)ts.tv_sec * 1000000000ULL + (uint64_t)ts.tv_nsec;
}

void process_neon(rafaelia_t *out, rafaelia_t *prev, rafaelia_t *in) {
    uint64_t t0 = now_ns();
    float32x4_t v_a = vdupq_n_f32(ALPHA);
    float32x4_t v_ia = vdupq_n_f32(1.0f - ALPHA);
    float32x4_t v_s = vdupq_n_f32(PHI_SCALE);

    for (int i = 0; i < TORUS_SIZE; i += 4) {
        float32x4_t p = vld1q_f32(&prev->v[i]);
        float32x4_t in_v = vld1q_f32(&in->v[i]);
        // Out = (prev * 0.75) + (in * 1.4012 * 0.25)
        float32x4_t r = vmlaq_f32(vmulq_f32(p, v_ia), vmulq_f32(in_v, v_s), v_a);
        vst1q_f32(&out->v[i], r);
    }
    out->ns = now_ns() - t0;
}

int main() {
    rafaelia_t p = {{1.0f}}, i = {{0.5f}}, o = {{0.0f}};
    process_neon(&o, &p, &i);
    printf("ARM32 NEON Core: %llu ns | Output[0]: %.4f\n", o.ns, o.v[0]);
    return 0;
}
