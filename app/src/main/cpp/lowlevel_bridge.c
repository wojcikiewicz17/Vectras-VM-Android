#include <jni.h>
#include <stdint.h>
#include <stdatomic.h>
#include "vectra_ll_base.h"

#include "rmr_lowlevel.h"
#include "hardware_profile_bridge_internal.h"
#include "vectra_lowlevel_backend.h"

typedef struct vectra_runtime_backend_state {
    atomic_int ready;
    vectra_lowlevel_backend_vtable_t table;
} vectra_runtime_backend_state_t;

static vectra_runtime_backend_state_t g_backend_state;

static uint32_t vectra_select_simd_mask(void) {
    return vectra_hw_runtime_simd_mask();
}

static int vectra_cstr_eq(const char* a, const char* b) {
    size_t i = 0;
    if (a == (void*)0 || b == (void*)0) return 0;
    while (a[i] != '\0' && b[i] != '\0') {
        if (a[i] != b[i]) return 0;
        i += 1u;
    }
    return a[i] == b[i];
}

static void vectra_bind_backend_once(void) {
    vectra_lowlevel_backend_vtable_t table;
    vectra_backend_bind_fallback(&table);

    const char* abi = vectra_hw_effective_abi();
    const uint32_t simd_mask = vectra_select_simd_mask();

    if (vectra_cstr_eq(abi, "arm64-v8a") && vectra_backend_arm64_available(simd_mask)) {
        vectra_backend_bind_arm64(&table);
    } else if (vectra_cstr_eq(abi, "armeabi-v7a") && vectra_backend_armv7_available(simd_mask)) {
        vectra_backend_bind_armv7(&table);
    } else if (vectra_cstr_eq(abi, "x86_64") && vectra_backend_x86_64_available(simd_mask)) {
        vectra_backend_bind_x86_64(&table);
    } else if (vectra_cstr_eq(abi, "x86") && vectra_backend_x86_available(simd_mask)) {
        vectra_backend_bind_x86(&table);
    } else if (vectra_cstr_eq(abi, "riscv64") && vectra_backend_riscv64_available(simd_mask)) {
        vectra_backend_bind_riscv64(&table);
    }

    g_backend_state.table = table;
    atomic_store_explicit(&g_backend_state.ready, 1, memory_order_release);
}

static const vectra_lowlevel_backend_vtable_t* vectra_backend(void) {
    if (atomic_load_explicit(&g_backend_state.ready, memory_order_acquire) == 0) {
        vectra_bind_backend_once();
    }
    return &g_backend_state.table;
}

JNIEXPORT jint JNICALL
Java_com_vectras_vm_core_LowLevelBridge_nativeFold32(JNIEnv* env, jclass clazz,
                                                      jint a, jint b, jint c, jint d) {
    (void)env;
    (void)clazz;
    return (jint)rmr_lowlevel_fold32((uint32_t)a, (uint32_t)b, (uint32_t)c, (uint32_t)d);
}

JNIEXPORT jint JNICALL
Java_com_vectras_vm_core_LowLevelBridge_nativeReduceXor(JNIEnv* env, jclass clazz,
                                                         jbyteArray data, jint offset, jint length) {
    (void)clazz;
    if (!data || offset < 0 || length < 0) return 0;
    const jsize n = (*env)->GetArrayLength(env, data);
    if (offset > n || length > (n - offset)) return 0;
    jbyte* p = (*env)->GetPrimitiveArrayCritical(env, data, NULL);
    if (!p) return 0;
    const uint32_t out = vectra_backend()->reduce_xor((const uint8_t*)p + (size_t)offset, (size_t)length);
    (*env)->ReleasePrimitiveArrayCritical(env, data, p, JNI_ABORT);
    return (jint)out;
}

JNIEXPORT jint JNICALL
Java_com_vectras_vm_core_LowLevelBridge_nativeChecksum32(JNIEnv* env, jclass clazz,
                                                          jbyteArray data, jint offset, jint length, jint seed) {
    (void)clazz;
    if (!data || offset < 0 || length < 0) return seed;
    const jsize n = (*env)->GetArrayLength(env, data);
    if (offset > n || length > (n - offset)) return seed;
    jbyte* p = (*env)->GetPrimitiveArrayCritical(env, data, NULL);
    if (!p) return seed;
    const uint32_t out = vectra_backend()->checksum32((const uint8_t*)p + (size_t)offset, (size_t)length, (uint32_t)seed);
    (*env)->ReleasePrimitiveArrayCritical(env, data, p, JNI_ABORT);
    return (jint)out;
}

JNIEXPORT jint JNICALL
Java_com_vectras_vm_core_LowLevelBridge_nativeXorChecksumCompat(JNIEnv* env, jclass clazz,
                                                                 jbyteArray data, jint offset, jint length) {
    (void)clazz;
    if (!data || offset < 0 || length < 0) return 0;
    const jsize n = (*env)->GetArrayLength(env, data);
    if (offset > n || length > (n - offset)) return 0;
    if (length == 0) return 0;

    jbyte* p = (*env)->GetPrimitiveArrayCritical(env, data, NULL);
    if (!p) return 0;

    const uint8_t* src = (const uint8_t*)p + (size_t)offset;
    uint32_t x = 0u;
    jint i = 0;
    const jint end = length & ~7;
    while (i < end) {
        x ^= (uint32_t)(src[i] & 0xFFu);
        x ^= (uint32_t)(src[i + 1] & 0xFFu);
        x ^= (uint32_t)(src[i + 2] & 0xFFu);
        x ^= (uint32_t)(src[i + 3] & 0xFFu);
        x ^= (uint32_t)(src[i + 4] & 0xFFu);
        x ^= (uint32_t)(src[i + 5] & 0xFFu);
        x ^= (uint32_t)(src[i + 6] & 0xFFu);
        x ^= (uint32_t)(src[i + 7] & 0xFFu);
        i += 8;
    }
    while (i < length) {
        x ^= (uint32_t)(src[i] & 0xFFu);
        i += 1;
    }

    (*env)->ReleasePrimitiveArrayCritical(env, data, p, JNI_ABORT);
    return (jint)x;
}

JNIEXPORT jint JNICALL
Java_com_vectras_vm_core_LowLevelBridge_nativeCrc32cCompat(JNIEnv* env, jclass clazz,
                                                            jint initial, jbyteArray data, jint offset, jint length) {
    (void)clazz;
    if (!data || offset < 0 || length < 0) return initial;
    const jsize n = (*env)->GetArrayLength(env, data);
    if (offset > n || length > (n - offset)) return initial;
    if (length == 0) return initial;

    jbyte* p = (*env)->GetPrimitiveArrayCritical(env, data, NULL);
    if (!p) return initial;

    const uint8_t* src = (const uint8_t*)p + (size_t)offset;
    const uint32_t out = vectra_backend()->crc32c((uint32_t)initial, src, (size_t)length);

    (*env)->ReleasePrimitiveArrayCritical(env, data, p, JNI_ABORT);
    return (jint)out;
}

JNIEXPORT jint JNICALL
Java_com_vectras_vm_core_LowLevelBridge_nativeValidateReduceXorBackendParity(JNIEnv* env, jclass clazz,
                                                                              jbyteArray data, jint offset, jint length) {
    (void)clazz;
    if (!data || offset < 0 || length < 0) return -1;
    const jsize n = (*env)->GetArrayLength(env, data);
    if (offset > n || length > (n - offset)) return -1;

    jbyte* p = (*env)->GetPrimitiveArrayCritical(env, data, NULL);
    if (!p) return -1;

    const uint8_t* src = (const uint8_t*)p + (size_t)offset;
    const size_t src_len = (size_t)length;
    const uint32_t expected = rmr_lowlevel_reduce_xor(src, src_len);

    uint32_t mismatch_mask = 0u;
    vectra_lowlevel_backend_vtable_t table;

    vectra_backend_bind_fallback(&table);
    if (table.reduce_xor(src, src_len) != expected) mismatch_mask |= (1u << 0);

    vectra_backend_bind_arm64(&table);
    if (table.reduce_xor(src, src_len) != expected) mismatch_mask |= (1u << 1);

    vectra_backend_bind_armv7(&table);
    if (table.reduce_xor(src, src_len) != expected) mismatch_mask |= (1u << 2);

    vectra_backend_bind_x86_64(&table);
    if (table.reduce_xor(src, src_len) != expected) mismatch_mask |= (1u << 3);

    vectra_backend_bind_x86(&table);
    if (table.reduce_xor(src, src_len) != expected) mismatch_mask |= (1u << 4);

    vectra_backend_bind_riscv64(&table);
    if (table.reduce_xor(src, src_len) != expected) mismatch_mask |= (1u << 5);

    (*env)->ReleasePrimitiveArrayCritical(env, data, p, JNI_ABORT);
    return (jint)mismatch_mask;
}
