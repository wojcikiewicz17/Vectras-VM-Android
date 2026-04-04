#include <jni.h>
#include <stdint.h>

#if defined(__linux__)
#include <sys/auxv.h>
#endif

#if defined(__x86_64__) || defined(__i386__)
#include <cpuid.h>
#endif

#if defined(__linux__) && (defined(__aarch64__) || defined(__arm__) || defined(__riscv))
#include <asm/hwcap.h>
#endif

#include "rmr_hw_detect.h"
#include "hardware_profile_bridge_internal.h"

const char* vectra_hw_effective_abi(void) {
#if defined(__aarch64__)
    return "arm64-v8a";
#elif defined(__arm__)
    return "armeabi-v7a";
#elif defined(__x86_64__)
    return "x86_64";
#elif defined(__i386__)
    return "x86";
#elif defined(__riscv)
    return "riscv64";
#else
    return "unknown";
#endif
}

static uint32_t vectra_simd_mask(void) {
    uint32_t mask = 0u;

#if defined(__x86_64__) || defined(__i386__)
    unsigned int eax = 0u;
    unsigned int ebx = 0u;
    unsigned int ecx = 0u;
    unsigned int edx = 0u;

    if (__get_cpuid(1u, &eax, &ebx, &ecx, &edx) != 0u) {
        if ((edx & bit_SSE2) != 0u) mask |= (1u << 1);
        if ((ecx & bit_SSE4_2) != 0u) mask |= (1u << 2);

        if ((ecx & bit_AVX) != 0u && (ecx & bit_OSXSAVE) != 0u) {
#if defined(__x86_64__) || defined(__i386__)
            uint32_t xcr0_eax;
            uint32_t xcr0_edx;
            __asm__ volatile("xgetbv" : "=a"(xcr0_eax), "=d"(xcr0_edx) : "c"(0));
            (void)xcr0_edx;
            if ((xcr0_eax & 0x6u) == 0x6u) mask |= (1u << 3);
#endif
        }
    }
#elif defined(__aarch64__) || defined(__arm__)
#if defined(__linux__)
    const unsigned long hwcap = getauxval(AT_HWCAP);
#if defined(__aarch64__)
    if ((hwcap & HWCAP_ASIMD) != 0ul) mask |= 1u;
#elif defined(__arm__)
    if ((hwcap & HWCAP_NEON) != 0ul) mask |= 1u;
#endif
#endif
#elif defined(__riscv)
#if defined(__linux__)
    const unsigned long hwcap = getauxval(AT_HWCAP);
#ifdef COMPAT_HWCAP_ISA_V
    if ((hwcap & COMPAT_HWCAP_ISA_V) != 0ul) mask |= (1u << 4);
#elif defined(HWCAP_ISA_V)
    if ((hwcap & HWCAP_ISA_V) != 0ul) mask |= (1u << 4);
#endif
#endif
#endif

    return mask;
}

uint32_t vectra_hw_runtime_simd_mask(void) {
    return vectra_simd_mask();
}

JNIEXPORT jintArray JNICALL
Java_com_vectras_vm_core_HardwareProfileBridge_nativeCollectSnapshot(JNIEnv* env, jclass clazz) {
    (void)clazz;
    uint32_t values_u32[9];
    vectra_hw_collect_snapshot(values_u32);
    jint values[9];
    for (int i = 0; i < 9; ++i) values[i] = (jint)values_u32[i];

    jintArray out = (*env)->NewIntArray(env, 9);
    if (!out) return NULL;
    (*env)->SetIntArrayRegion(env, out, 0, 9, values);
    return out;
}

JNIEXPORT jstring JNICALL
Java_com_vectras_vm_core_HardwareProfileBridge_nativeEffectiveAbi(JNIEnv* env, jclass clazz) {
    (void)clazz;
    return (*env)->NewStringUTF(env, vectra_hw_effective_abi());
}

void vectra_hw_collect_snapshot(uint32_t out_values[9]) {
    RmR_HW_Info info;
    RmR_HW_Detect(&info);

    out_values[0] = info.arch;
    out_values[1] = info.arch_hex;
    out_values[2] = info.ptr_bits;
    out_values[3] = info.is_little_endian;
    out_values[4] = info.has_cycle_counter;
    out_values[5] = info.has_asm_probe;
    out_values[6] = info.feature_bits_0;
    out_values[7] = info.feature_bits_1;
    out_values[8] = vectra_hw_runtime_simd_mask();
}
