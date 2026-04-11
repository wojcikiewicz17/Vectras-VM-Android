#include <stdint.h>
#include <stdio.h>
#include <string.h>

// Assinatura simples baseada em estado RAFACODEphi

typedef struct {
    uint64_t s;
    uint64_t phi;
    uint64_t d;
} RAF_State;

uint64_t mix(uint64_t x) {
    x ^= x >> 33;
    x *= 0xff51afd7ed558ccdULL;
    x ^= x >> 33;
    x *= 0xc4ceb9fe1a85ec53ULL;
    x ^= x >> 33;
    return x;
}

uint64_t sign_data(const char* data, uint64_t key) {
    uint64_t hash = key;
    size_t len = strlen(data);

    for (size_t i = 0; i < len; i++) {
        hash ^= (uint64_t)data[i];
        hash = mix(hash);
    }

    return hash;
}

int verify_signature(const char* data, uint64_t key, uint64_t signature) {
    uint64_t expected = sign_data(data, key);
    return expected == signature;
}

int main() {
    const char* msg = "RAFACODEphi";
    uint64_t key = 0xDEADBEEFCAFEBABEULL;

    uint64_t sig = sign_data(msg, key);
    printf("Signature: %016llX\n", sig);

    if (verify_signature(msg, key, sig)) {
        printf("Valid signature\n");
    } else {
        printf("Invalid signature\n");
    }

    return 0;
}
