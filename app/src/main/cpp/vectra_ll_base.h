#ifndef VECTRA_LL_BASE_H
#define VECTRA_LL_BASE_H

#if defined(VECTRA_LL_NO_STDLIB_TYPES)
/*
 * Freestanding low-level profile:
 * - no stdint.h
 * - no stddef.h
 * Uses compiler primitive widths expected by Android NDK toolchains.
 */
typedef __SIZE_TYPE__ size_t;
typedef unsigned char uint8_t;
typedef unsigned short uint16_t;
typedef unsigned int uint32_t;
typedef unsigned long long uint64_t;
typedef signed char int8_t;
typedef short int16_t;
typedef int int32_t;
typedef long long int64_t;
#else
#include <stddef.h>
#include <stdint.h>
#endif

#endif
