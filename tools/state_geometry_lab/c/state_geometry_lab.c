#include <math.h>
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

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

double sgl_sqrt3_over_2(void) {
    return sqrt(3.0) / 2.0;
}

double sgl_geometric_spiral(unsigned n) {
    return pow(sgl_sqrt3_over_2(), (double)n);
}

double sgl_fibonacci_rafael_fixed_point(void) {
    const double contraction = 0.86602540378443864676;
    const double forcing = -M_PI * sin(279.0 * M_PI / 180.0);
    return forcing / (1.0 - contraction);
}

double sgl_kaplan_yorke_dimension(double lambda_positive) {
    const double lambda_negative = log(sgl_sqrt3_over_2());
    return 1.0 + lambda_positive / fabs(lambda_negative);
}

uint32_t sgl_entropy_milli(const uint8_t *data, size_t len) {
    if (len == 0) return 0;
    uint8_t seen[256] = {0};
    uint32_t unique = 0;
    uint32_t transitions = 0;
    for (size_t i = 0; i < len; ++i) {
        if (!seen[data[i]]) {
            seen[data[i]] = 1;
            unique++;
        }
        if (i > 0 && data[i] != data[i - 1]) transitions++;
    }
    return (uint32_t)((unique * 6000U) / 256U + (transitions * 2000U) / (uint32_t)(len > 1 ? len - 1 : 1));
}

void sgl_toroidal_map7(const uint8_t *data, size_t len, double out[7]) {
    uint64_t h = 0xCBF29CE484222325ULL;
    for (size_t i = 0; i < len; ++i) {
        h ^= data[i];
        h *= 0x100000001B3ULL;
    }
    const double q16 = 65536.0;
    out[0] = (double)(h & 0xFFFFU) / q16;
    out[1] = (double)((h >> 16) & 0xFFFFU) / q16;
    out[2] = (double)((h >> 32) & 0xFFFFU) / q16;
    out[3] = (double)((h >> 48) & 0xFFFFU) / q16;
    out[4] = fmin(1.0, (double)sgl_entropy_milli(data, len) / 8000.0);
    out[5] = len == 0 ? 0.0 : fmin(1.0, (double)len / q16);
    out[6] = fmod((out[0] + out[1]) * sgl_sqrt3_over_2(), 1.0);
}
