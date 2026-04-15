/* --- NÍVEL BARE-METAL: SEM LIBC / SEM HEADERS --- */

struct timespec {
    long tv_sec;
    long tv_nsec;
};

// Escrita direta via Syscall (r7=4)
static void print(const char* s, int len) {
    __asm__ volatile (
        "mov r0, 1 \n\t"
        "mov r1, %0 \n\t"
        "mov r2, %1 \n\t"
        "mov r7, 4 \n\t"
        "svc 0"
        : : "r"(s), "r"(len) : "r0","r1","r2","r7"
    );
}

void _start() {
    static float torus[1024] __attribute__((aligned(16)));
    struct timespec t1, t2;
    
    print("══ MONITORAMENTO ATÔMICO (14 AMOSTRAS) ══\n", 44);

    for(int s = 0; s < 14; s++) {
        // T1: clock_gettime (r7=263)
        __asm__ volatile (
            "mov r0, 1 \n\t"
            "mov r1, %0 \n\t"
            "mov r7, 263 \n\t"
            "svc 0"
            : : "r"(&t1) : "r0","r1","r7","memory"
        );

        // --- NÚCLEO GEOMÉTRICO NEON ---
        __asm__ volatile (
            "vld1.32 {q0,q1}, [%0] \n\t"
            "vadd.f32 q2, q0, q1    \n\t"
            "vst1.32 {q2}, [%0]     \n\t"
            : : "r"(torus) : "q0","q1","q2","memory"
        );

        // T2
        __asm__ volatile (
            "mov r0, 1 \n\t"
            "mov r1, %0 \n\t"
            "mov r7, 263 \n\t"
            "svc 0"
            : : "r"(&t2) : "r0","r1","r7","memory"
        );

        long delta = (t2.tv_nsec - t1.tv_nsec);
        if (delta < 0) delta += 1000000000;

        // Feedback: '.' para rápido, '!' para lento (jitter)
        if(delta < 20000) print(".", 1); else print("!", 1);
    }

    print("\nANÁLISE: Pontos indicam estabilidade de fase.\n", 47);

    // Finaliza (r7=1)
    __asm__ volatile ("mov r0, 0 \n\t mov r7, 1 \n\t svc 0");
}
