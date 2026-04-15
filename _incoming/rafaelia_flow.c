#include <stdio.h>
#include <stdint.h>
#include <time.h>
#include <arm_neon.h>

#define TORUS_SIZE 1024
#define ALPHA 0.25f
#define PHI_SCALE 1.4012f

typedef struct {
    float v[TORUS_SIZE] __attribute__((aligned(16)));
    uint64_t ns;
} rafaelia_t;

static inline uint64_t now_ns() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC_RAW, &ts);
    return (uint64_t)ts.tv_sec * 1000000000ULL + (uint64_t)ts.tv_nsec;
}

// Simula a viscosidade dinâmica da gramática (Ruído Sintrópico)
void inject_grammar_flow(rafaelia_t *in, uint32_t seed) {
    for(int i=0; i<TORUS_SIZE; i++) {
        seed = seed * 1103515245 + 12345;
        in->v[i] = (float)(seed % 1000) / 1000.0f;
    }
}

void process_ultra(rafaelia_t *out, rafaelia_t *prev, rafaelia_t *in) {
    uint64_t t0 = now_ns();
    float32x4_t v_a = vdupq_n_f32(ALPHA);
    float32x4_t v_ia = vdupq_n_f32(1.0f - ALPHA);
    float32x4_t v_s = vdupq_n_f32(PHI_SCALE);

    for (int i = 0; i < TORUS_SIZE; i += 8) {
        float32x4_t p1 = vld1q_f32(&prev->v[i]);
        float32x4_t p2 = vld1q_f32(&prev->v[i+4]);
        float32x4_t in1 = vld1q_f32(&in->v[i]);
        float32x4_t in2 = vld1q_f32(&in->v[i+4]);

        // Kernel: Multiplicação Quântica Virtual na borda do Toro
        float32x4_t r1 = vmlaq_f32(vmulq_f32(p1, v_ia), vmulq_f32(in1, v_s), v_a);
        float32x4_t r2 = vmlaq_f32(vmulq_f32(p2, v_ia), vmulq_f32(in2, v_s), v_a);

        vst1q_f32(&out->v[i], r1);
        vst1q_f32(&out->v[i+4], r2);
    }
    out->ns = now_ns() - t0;
}

int main() {
    rafaelia_t state = {{0.5f}}, input = {{0.0f}}, next = {{0.0f}};
    uint32_t seed = 0x963; // Frequência de ativação

    printf("--- RAFAELIA DYNAMIC FLOW (ARM32) ---\n");
    for(int step = 0; step < 5; step++) {
        inject_grammar_flow(&input, seed + step);
        process_ultra(&next, &state, &input);
        
        // Sincronização de fase: prev = next
        for(int k=0; k<TORUS_SIZE; k++) state.v[k] = next.v[k];

        printf("Passo [%d] | Latência: %llu ns | Estado Médio: %.4f\n", 
                step, next.ns, next.v[0]);
    }
    return 0;
}
