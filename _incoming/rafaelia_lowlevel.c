#include <stdio.h>
#include <stdint.h>
#include <time.h>

#define TORUS_LEN 1024
#define ALPHA 0.25f
#define PHI_SIGMA 1.401222f

typedef struct {
    float v[TORUS_LEN] __attribute__((aligned(16)));
    uint64_t ns;
} rafaelia_t;

int main() {
    static rafaelia_t p __attribute__((aligned(16)));
    static rafaelia_t i __attribute__((aligned(16)));
    static rafaelia_t o __attribute__((aligned(16)));
    
    for(int k=0; k<TORUS_LEN; k++) { p.v[k] = 1.0f; i.v[k] = 0.5f; }

    struct timespec t1, t2;
    clock_gettime(CLOCK_MONOTONIC_RAW, &t1);

    /* --- ASM INLINE LOW-LEVEL (ARM32 NEON) --- */
    float a = ALPHA;
    float ia = 1.0f - ALPHA;
    float s = PHI_SIGMA;

    float *ptr_p = p.v;
    float *ptr_i = i.v;
    float *ptr_o = o.v;
    int count = TORUS_LEN;

    __asm__ volatile (
        "vdup.32 q8, %[a]            \n\t" // q8 = alpha
        "vdup.32 q9, %[ia]           \n\t" // q9 = 1-alpha
        "vdup.32 q10, %[s]           \n\t" // q10 = phi_sigma
        
        "1:                          \n\t"
        "vld1.32 {q0, q1}, [%[pp]]!  \n\t" // Carrega 8 floats (p)
        "vld1.32 {q2, q3}, [%[pi]]!  \n\t" // Carrega 8 floats (in)
        
        // r1 = (p1 * (1-alpha)) + (in1 * phi * alpha)
        "vmul.f32 q4, q0, q9         \n\t" // p1 * ia
        "vmul.f32 q5, q2, q10        \n\t" // in1 * s
        "vmla.f32 q4, q5, q8         \n\t" // r1 = q4 + (q5 * alpha)
        
        // r2 = (p2 * (1-alpha)) + (in2 * phi * alpha)
        "vmul.f32 q6, q1, q9         \n\t" // p2 * ia
        "vmul.f32 q7, q3, q10        \n\t" // in2 * s
        "vmla.f32 q6, q7, q8         \n\t" // r2 = q6 + (q7 * alpha)

        "vst1.32 {q4, q5}, [%[po]]!  \n\t" // Descarrega 8 floats (out)
        
        "subs %[count], %[count], #8 \n\t" // Decrementa contador
        "bne 1b                      \n\t" // Loop até zero
        : [pp] "+r" (ptr_p), [pi] "+r" (ptr_i), [po] "+r" (ptr_o), [count] "+r" (count)
        : [a] "r" (a), [ia] "r" (ia), [s] "r" (s)
        : "q0", "q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10", "cc", "memory"
    );

    clock_gettime(CLOCK_MONOTONIC_RAW, &t2);
    uint64_t diff = (uint64_t)(t2.tv_sec - t1.tv_sec) * 1000000000ULL + (t2.tv_nsec - t1.tv_nsec);

    printf("╔══ RAFAELIA NO-ABSTRACTION (ASM) ══╗\n");
    printf("║ Latência Real: %llu ns\n", diff);
    printf("║ V[0]: %.6f\n", o.v[0]);
    printf("╚═══════════════════════════════════╝\n");

    return 0;
}
