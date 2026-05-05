#include <math.h>
#include <stdint.h>
#include <stddef.h>

uint64_t sgl_mod_signature(const uint32_t *arr, size_t len, uint32_t mod) {
    uint64_t acc = 0x9E3779B97F4A7C15ULL;
    if (mod == 0) mod = 1;
    for (size_t i = 0; i < len; ++i) {
        uint32_t v = arr[i] % mod;
        acc ^= (uint64_t)v + 0x9E3779B97F4A7C15ULL + (acc << 6) + (acc >> 2);
    }
    return acc;
}

double sgl_equilateral_height(double side) {
    return side * sqrt(3.0) / 2.0;
}

double sgl_poly(double x) {
    return x * x - M_PI * x + sqrt(fabs(x));
}
