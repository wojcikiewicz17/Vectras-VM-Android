#include <stdint.h>
#include <stdio.h>
#include <time.h>

// Gerador simples de chave baseado em fase + delta
// Integra com modelo RAFACODEphi

typedef struct {
    uint64_t s;
    uint64_t phi;
    uint64_t d;
} RAF_State;

uint64_t next_attractor(uint64_t s) {
    // simples rotação + mistura
    return (s << 7) ^ (s >> 3) ^ 0x9E3779B97F4A7C15ULL;
}

uint64_t raf_step(RAF_State* st, uint64_t input) {
    st->phi += input;
    st->s ^= st->phi;
    st->d ^= st->s;

    if (st->d > 0xFFFFFFFFFFFFULL) {
        st->s = next_attractor(st->s);
    }

    return st->s ^ st->phi ^ st->d;
}

uint64_t generate_key(uint64_t seed) {
    RAF_State st = {seed, seed ^ 0xABCDEF, seed << 1};
    uint64_t key = 0;

    for (int i = 0; i < 64; i++) {
        key ^= raf_step(&st, (uint64_t)clock() + i);
        key = (key << 1) | (key >> 63);
    }

    return key;
}

int main() {
    uint64_t seed = (uint64_t)time(NULL);
    uint64_t key = generate_key(seed);

    printf("Generated Key: %016llX\n", key);
    return 0;
}
