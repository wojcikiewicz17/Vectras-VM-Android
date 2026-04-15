#include <stdio.h>
#include <stdint.h>
#include <time.h>
#include <math.h>
#include <arm_neon.h>

#define TORUS_SIZE 1024
#define ALPHA 0.25f
#define PHI 1.6180339f

typedef struct {
    float v[TORUS_SIZE] __attribute__((aligned(16)));
    uint64_t ns_elapsed;
} rafaelia_node_t;

// Timer de alta precisão para Android/Linux
static inline uint64_t get_ns(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC_RAW, &ts);
    return (uint64_t)ts.tv_sec * 1000000000ULL + (uint64_t)ts.tv_nsec;
}

// Núcleo de Evolução Vetorial ARMv7 NEON
void evolve_neon(rafaelia_node_t *out, const rafaelia_node_t *prev, const rafaelia_node_t *in) {
    uint64_t t0 = get_ns();
    
    float32x4_t v_alpha = vdupq_n_f32(ALPHA);
    float32x4_t v_inv_alpha = vdupq_n_f32(1.0f - ALPHA);
    float32x4_t v_scale = vdupq_n_f32(PHI * 0.866025f); // PHI * sqrt(3)/2

    for (int i = 0; i < TORUS_SIZE; i += 4) {
        float32x4_t v_p = vld1q_f32(&prev->v[i]);
        float32x4_t v_i = vld1q_f32(&in->v[i]);

        // s' = (prev * 0.75) + (in * scale * 0.25)
        float32x4_t res = vmlaq_f32(vmulq_f32(v_p, v_inv_alpha), vmulq_f32(v_i, v_scale), v_alpha);
        
        vst1q_f32(&out->v[i], res);
    }
    out->ns_elapsed = get_ns() - t0;
}

int main() {
    rafaelia_node_t n_prev = {{1.0f}, 0}, n_in = {{0.5f}, 0}, n_out = {{0}, 0};
    
    // Inicialização simples
    for(int i=0; i<TORUS_SIZE; i++) { n_prev.v[i] = (float)i/TORUS_SIZE; n_in.v[i] = 1.0f - n_prev.v[i]; }

    printf("--- RAFAELIA ARM32 NEON CORE ---\n");
    evolve_neon(&n_out, &n_prev, &n_in);

    printf("Status: Concluído em %llu ns\n", n_out.ns_elapsed);
    printf("Exemplo de Saída [Index 42]: %.6f\n", n_out.v[42]);
    return 0;
}
