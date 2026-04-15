// Definições para Syscalls ARM32 (r7=syscall_nr)
#define SYS_WRITE 4
#define SYS_EXIT  1
#define SYS_CLOCK 263 // clock_gettime

struct timespec {
    long tv_sec;
    long tv_nsec;
};

// Escrita direta no terminal
void print(const char* s, int len) {
    __asm__ volatile ("mov r0, #1; mov r1, %[b]; mov r2, %[l]; mov r7, #%[n]; svc #0" 
    : : [b]"r"(s), [l]"r"(len), [n]"i"(SYS_WRITE) : "r0","r1","r2","r7");
}

void _start() {
    // Alinhamento geométrico
    static float torus[1024] __attribute__((aligned(16)));
    struct timespec t1, t2;
    char out_buf[32];
    
    print("══ MONITORAMENTO GEOMÉTRICO (14 PONTOS) ══\n", 44);

    for(int s = 0; s < 14; s++) {
        // T1: Marcação de entrada (Syscall direta)
        __asm__ volatile ("mov r0, #1; mov r1, %[t]; mov r7, #%[n]; svc #0" 
        : : [t]"r"(&t1), [n]"i"(SYS_CLOCK) : "r0","r1","r7");

        // --- NÚCLEO NEON (AÇÃO) ---
        __asm__ volatile (
            "vdup.32 q8, d0[0] \n\t" // Dummy work para ocupar o pipeline
            "vld1.32 {q0,q1}, [%0] \n\t"
            "vadd.f32 q0, q0, q1 \n\t"
            "vst1.32 {q0,q1}, [%0] \n\t"
            : : "r"(torus) : "q0","q1","q8","memory"
        );

        // T2: Marcação de saída
        __asm__ volatile ("mov r0, #1; mov r1, %[t]; mov r7, #%[n]; svc #0" 
        : : [t]"r"(&t2), [n]"i"(SYS_CLOCK) : "r0","r1","r7");

        // Cálculo bruto de nanosegundos (Delta)
        long delta = (t2.tv_nsec - t1.tv_nsec);
        if (delta < 0) delta += 1000000000;

        // Feedback visual da latência (Representação simbólica)
        if(delta < 15000) print("•", 1); else print("!", 1);
    }

    print("\nANÁLISE: Se '.', latência estável. Se '!', jitter do Android detectado.\n", 68);

    __asm__ volatile ("mov r0, #0; mov r1, #0; mov r7, #1; svc #0");
}
