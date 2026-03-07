#ifndef RMR_MATRIX_H
#define RMR_MATRIX_H

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

/*
 * RAFAELIA Matrix
 * 10x10x10 body + 4 tetrafractal parities + 2 dual-core cells
 *
 * Convenção:
 * - body[][][] guarda valores em Q16.16
 * - frac_parity[4] guarda projeções/fractal parities em Q16.16
 * - dual_core[2] guarda dois núcleos centrais escolhidos
 * - singularity_q16 guarda a constante simbólico-geométrica (√2 em Q16)
 */

#define RAF_MATRIX_DIM            10u
#define RAF_MATRIX_TOTAL          1000u

#define RAF_PARITY_TETRA          4u
#define RAF_DUAL_CORE             2u
#define RAF_VN3D_DEGREE           6u

#define RAF_CORE_LO               4u
#define RAF_CORE_HI               5u

#define RAF_Q16_ONE               0x00010000u

/* Constantes geométricas em Q16.16 */
#define RAF_SQRT2_Q16             0x00016A0Au  /* 1.41421356 * 65536 = 92682 */
#define RAF_SQRT2_HALF_Q16        0x0000B505u  /* 0.70710678 * 65536 = 46341 */
#define RAF_SQRT3_HALF_Q16        0x0000DDB4u  /* 0.86602540 * 65536 = 56756 */

/* Offsets clássicos de Von Neumann em 3D */
static const int8_t g_rmr_vn3d_offsets[RAF_VN3D_DEGREE][3] = {
    {  1,  0,  0 },
    { -1,  0,  0 },
    {  0,  1,  0 },
    {  0, -1,  0 },
    {  0,  0,  1 },
    {  0,  0, -1 }
};

typedef struct {
    uint32_t body[RAF_MATRIX_DIM][RAF_MATRIX_DIM][RAF_MATRIX_DIM]; /* Q16.16 */
    uint32_t frac_parity[RAF_PARITY_TETRA];                        /* Q16.16 */
    uint32_t dual_core[RAF_DUAL_CORE];                             /* Q16.16 */
    uint32_t singularity_q16;                                      /* √2 */
} RafMatrix_t;

/* ---------- utilitários básicos ---------- */

static inline bool rmr_matrix_in_bounds(uint32_t i, uint32_t j, uint32_t k)
{
    return (i < RAF_MATRIX_DIM) && (j < RAF_MATRIX_DIM) && (k < RAF_MATRIX_DIM);
}

static inline uint32_t rmr_q16_mul(uint32_t a_q16, uint32_t b_q16)
{
    return (uint32_t)((((uint64_t)a_q16 * (uint64_t)b_q16) + 0x8000u) >> 16);
}

static inline uint32_t rmr_q16_div(uint32_t a_q16, uint32_t b_q16)
{
    if (b_q16 == 0u) return 0u;
    return (uint32_t)((((uint64_t)a_q16 << 16) + (b_q16 >> 1)) / b_q16);
}

/* raiz inteira de 64 bits */
static inline uint32_t rmr_isqrt_u64(uint64_t x)
{
    uint64_t op  = x;
    uint64_t res = 0;
    uint64_t one = (uint64_t)1 << 62;

    while (one > op) {
        one >>= 2;
    }

    while (one != 0) {
        if (op >= res + one) {
            op  -= res + one;
            res  = (res >> 1) + one;
        } else {
            res >>= 1;
        }
        one >>= 2;
    }

    return (uint32_t)res;
}

/* ---------- acesso à matriz ---------- */

static inline void rmr_matrix_init(RafMatrix_t *m)
{
    if (!m) return;

    for (uint32_t i = 0; i < RAF_MATRIX_DIM; ++i) {
        for (uint32_t j = 0; j < RAF_MATRIX_DIM; ++j) {
            for (uint32_t k = 0; k < RAF_MATRIX_DIM; ++k) {
                m->body[i][j][k] = 0u;
            }
        }
    }

    for (uint32_t n = 0; n < RAF_PARITY_TETRA; ++n) {
        m->frac_parity[n] = 0u;
    }

    for (uint32_t n = 0; n < RAF_DUAL_CORE; ++n) {
        m->dual_core[n] = 0u;
    }

    m->singularity_q16 = RAF_SQRT2_Q16;
}

static inline uint32_t rmr_matrix_get(const RafMatrix_t *m,
                                      uint32_t i, uint32_t j, uint32_t k)
{
    if (!m || !rmr_matrix_in_bounds(i, j, k)) return 0u;
    return m->body[i][j][k];
}

static inline void rmr_matrix_set(RafMatrix_t *m,
                                  uint32_t i, uint32_t j, uint32_t k,
                                  uint32_t value_q16)
{
    if (!m || !rmr_matrix_in_bounds(i, j, k)) return;
    m->body[i][j][k] = value_q16;
}

/* ---------- dual core central ---------- */

/*
 * Em grade 10x10x10, o centro geométrico é (4.5,4.5,4.5).
 * Escolhemos duas células centrais sobre a diagonal principal:
 *   N1 = (4,4,4)
 *   N2 = (5,5,5)
 */
static inline uint32_t rmr_matrix_core_n1_get(const RafMatrix_t *m)
{
    if (!m) return 0u;
    return m->body[RAF_CORE_LO][RAF_CORE_LO][RAF_CORE_LO];
}

static inline uint32_t rmr_matrix_core_n2_get(const RafMatrix_t *m)
{
    if (!m) return 0u;
    return m->body[RAF_CORE_HI][RAF_CORE_HI][RAF_CORE_HI];
}

static inline void rmr_matrix_core_refresh(RafMatrix_t *m)
{
    if (!m) return;
    m->dual_core[0] = rmr_matrix_core_n1_get(m);
    m->dual_core[1] = rmr_matrix_core_n2_get(m);
}

/* ---------- operador E / cruzamento ---------- */

/*
 * Operador E como média geométrica:
 *
 *   E = sqrt(N1 * N2)
 *
 * Se N1 e N2 estão em Q16.16:
 *   N1*N2 -> Q32.32
 *   sqrt  -> Q16.16
 */
static inline uint32_t rmr_parity_cross_q16(uint32_t n1_q16, uint32_t n2_q16)
{
    return rmr_isqrt_u64((uint64_t)n1_q16 * (uint64_t)n2_q16);
}

static inline uint32_t rmr_matrix_cross_E_q16(RafMatrix_t *m)
{
    if (!m) return 0u;

    rmr_matrix_core_refresh(m);
    return rmr_parity_cross_q16(m->dual_core[0], m->dual_core[1]);
}

/*
 * Constante singular do sistema.
 * Mantém separado do operador E para não misturar:
 * - E = cruzamento dinâmico dos núcleos
 * - singularity_q16 = constante geométrica √2
 */
static inline uint32_t rmr_matrix_singularity_q16(const RafMatrix_t *m)
{
    if (!m) return RAF_SQRT2_Q16;
    return m->singularity_q16;
}

/* ---------- paridade tetrafractal ---------- */

/*
 * Mapeamento simples de 4 vértices tetraédricos alternados no cubo 10x10x10:
 *   T0 = (0,0,0)
 *   T1 = (9,9,0)
 *   T2 = (9,0,9)
 *   T3 = (0,9,9)
 *
 * Isso representa melhor uma paridade tetrafractal do que "4 de 8 vértices"
 * sem critério explícito.
 */
static inline void rmr_matrix_refresh_tetra_parity(RafMatrix_t *m)
{
    if (!m) return;

    m->frac_parity[0] = m->body[0][0][0];
    m->frac_parity[1] = m->body[9][9][0];
    m->frac_parity[2] = m->body[9][0][9];
    m->frac_parity[3] = m->body[0][9][9];
}

/* ---------- média global ---------- */

static inline uint32_t rmr_matrix_mean_q16(const RafMatrix_t *m)
{
    if (!m) return 0u;

    uint64_t acc = 0u;

    for (uint32_t i = 0; i < RAF_MATRIX_DIM; ++i) {
        for (uint32_t j = 0; j < RAF_MATRIX_DIM; ++j) {
            for (uint32_t k = 0; k < RAF_MATRIX_DIM; ++k) {
                acc += m->body[i][j][k];
            }
        }
    }

    return (uint32_t)(acc / RAF_MATRIX_TOTAL);
}

#endif /* RMR_MATRIX_H */
