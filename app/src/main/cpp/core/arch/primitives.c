#include <stdint.h>

/*
 * RAFCoder portable primitives fallback unit.
 * This translation unit is intentionally tiny and always safe for Android ABIs.
 * ABI-specialized ASM may replace it when available and validated.
 */

uint32_t rafcoder_primitives_portable_marker(void) {
    return 0x52414643u; /* 'RAFC' */
}
