#ifndef UNIFIED_COHERENCE_H
#define UNIFIED_COHERENCE_H

#include <stddef.h>
#include <stdint.h>

typedef struct {
    double s[7];
    double coherence;
    double entropy;
    uint64_t hash;
    uint32_t crc;
    uint32_t state;
} UCContext;

typedef struct {
    double coherence_in;
    double entropy_in;
    const uint8_t* data;
    size_t len;
    uint32_t state;
} UCInput;

enum { UC_GRAPH_N = 42 };

typedef struct {
    double a[UC_GRAPH_N][UC_GRAPH_N];
    double d[UC_GRAPH_N];
    double l[UC_GRAPH_N][UC_GRAPH_N];
} UCSpectralGraph;

void uc_init(UCContext* ctx, uint64_t seed);
void uc_step(UCContext* ctx, const UCInput* in);
void uc_build_spectral_graph(const double m[UC_GRAPH_N], double lambda, double gamma, double epsilon, UCSpectralGraph* out);
void uc_dynamics_step(const UCSpectralGraph* g, const double m[UC_GRAPH_N], double alpha, double dt, const double x_in[UC_GRAPH_N], double x_out[UC_GRAPH_N]);

#endif
