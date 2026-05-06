#include <errno.h>
#include <jni.h>
#include <limits.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

#include <android/log.h>

#include "../../../../tools/baremetal/rafcode_phi/include/rafcode_phi_lowbasic.h"

#define TERMUX_BOOTSTRAP_TAG "termux-bootstrap"
#define TERMUX_BOOTSTRAP_SYMBOL "Java_com_termux_app_TermuxInstaller_nativeGetZip"
#define LOGE(fmt, ...) __android_log_print(ANDROID_LOG_ERROR, TERMUX_BOOTSTRAP_TAG, fmt, ##__VA_ARGS__)
#define LOGI(fmt, ...) __android_log_print(ANDROID_LOG_INFO, TERMUX_BOOTSTRAP_TAG, fmt, ##__VA_ARGS__)

/*
 * Optional generated payload support.
 * If your build pipeline provides a real bootstrap payload, define:
 *   -DTERMUX_BOOTSTRAP_PAYLOAD_DATA=<symbol>
 *   -DTERMUX_BOOTSTRAP_PAYLOAD_SIZE=<symbol_or_constant>
 */
#if defined(TERMUX_BOOTSTRAP_PAYLOAD_DATA) && defined(TERMUX_BOOTSTRAP_PAYLOAD_SIZE)
extern const unsigned char TERMUX_BOOTSTRAP_PAYLOAD_DATA[];
static const unsigned char* get_embedded_bootstrap_data(void) {
    return TERMUX_BOOTSTRAP_PAYLOAD_DATA;
}

static size_t get_embedded_bootstrap_size(void) {
    return (size_t)TERMUX_BOOTSTRAP_PAYLOAD_SIZE;
}
#else
static const unsigned char* get_embedded_bootstrap_data(void) {
    return NULL;
}

static size_t get_embedded_bootstrap_size(void) {
    return 0U;
}
#endif

static jbyteArray return_controlled_null(const char* cause, const char* context) {
    LOGE("%s: returning NULL cause=%s context=%s", TERMUX_BOOTSTRAP_SYMBOL, cause, context);
    return NULL;
}

/* JNI symbol must exactly match: com.termux.app.TermuxInstaller#nativeGetZip() */
JNIEXPORT jbyteArray JNICALL
Java_com_termux_app_TermuxInstaller_nativeGetZip(JNIEnv* env, jclass clazz) {
    (void)clazz;

    if (env == NULL) {
        return return_controlled_null("env is NULL", "jni-entry");
    }

    const unsigned char* payload = get_embedded_bootstrap_data();
    const size_t payload_size = get_embedded_bootstrap_size();

    rafphi_boot_handoff_t handoff = {0};
    handoff.magic = RAFPHI_BOOT_MAGIC;
    handoff.version = RAFPHI_BOOT_VERSION;
#if defined(__aarch64__) || defined(RMR_ARCH_ARM64)
    handoff.arch = RAFPHI_ARCH_AARCH64;
#elif defined(__arm__) || defined(RMR_ARCH_ARM32)
    handoff.arch = RAFPHI_ARCH_ARMV7;
#elif defined(__x86_64__)
    handoff.arch = RAFPHI_ARCH_X86_64;
#elif defined(__riscv) && __riscv_xlen == 64
    handoff.arch = RAFPHI_ARCH_RISCV64;
#else
    handoff.arch = RAFPHI_ARCH_UNKNOWN;
#endif
    handoff.in_ptr = (raf_u64)(uintptr_t)payload;
    handoff.out_ptr = (raf_u64)(uintptr_t)payload;
    handoff.words = (raf_u64)payload_size;
    const raf_u32 handoff_status = rafphi_boot_handoff_validate(&handoff);
    if ((handoff_status & RAFPHI_F_BOOT_OK) == 0u) {
        LOGE("%s: lowbasic handoff validation failed status=0x%08x", TERMUX_BOOTSTRAP_SYMBOL, handoff_status);
        return return_controlled_null("lowbasic handoff validate failed", "handoff");
    }

    if (payload == NULL || payload_size == 0U) {
        if (payload == NULL) {
            LOGE("%s: TERMUX_BOOTSTRAP_PAYLOAD_DATA is NULL", TERMUX_BOOTSTRAP_SYMBOL);
        }
        LOGE("%s: embedded payload missing payload=%p size=%zu", TERMUX_BOOTSTRAP_SYMBOL,
             (const void*)payload, payload_size);
        return return_controlled_null("bootstrap payload missing (asset not embedded)", "payload-source");
    }

#if !defined(TERMUX_BOOTSTRAP_PAYLOAD_DATA) || !defined(TERMUX_BOOTSTRAP_PAYLOAD_SIZE)
    LOGE("%s: embedded payload symbols are not configured; refusing to return synthetic payload", TERMUX_BOOTSTRAP_SYMBOL);
#endif

    if (payload_size < 4U || payload[0] != 0x50U || payload[1] != 0x4BU ||
        (payload[2] != 0x03U && payload[2] != 0x05U && payload[2] != 0x07U) ||
        (payload[3] != 0x04U && payload[3] != 0x06U && payload[3] != 0x08U)) {
        if (payload_size >= 4U) {
            LOGE("%s: embedded payload invalid zip signature size=%zu first_bytes=%02X %02X %02X %02X",
                 TERMUX_BOOTSTRAP_SYMBOL, payload_size, payload[0], payload[1], payload[2], payload[3]);
        } else {
            LOGE("%s: embedded payload invalid zip signature size=%zu", TERMUX_BOOTSTRAP_SYMBOL,
                 payload_size);
        }
        return return_controlled_null("embedded payload invalid", "payload-validation");
    }

    if (payload_size > (size_t)INT_MAX) {
        LOGE("%s: payload too large for jbyteArray size=%zu max=%d", TERMUX_BOOTSTRAP_SYMBOL,
             payload_size, INT_MAX);
        return return_controlled_null("payload too large", "array-allocation");
    }

    jbyteArray result = (*env)->NewByteArray(env, (jsize)payload_size);
    if (result == NULL) {
        const int saved_errno = errno;
        LOGE("%s: NewByteArray returned NULL size=%zu errno=%d msg=%s", TERMUX_BOOTSTRAP_SYMBOL,
             payload_size, saved_errno, strerror(saved_errno));
        return return_controlled_null("NewByteArray returned NULL", "array-allocation");
    }

    (*env)->SetByteArrayRegion(env, result, 0, (jsize)payload_size, (const jbyte*)payload);
    if ((*env)->ExceptionCheck(env)) {
        LOGE("%s: SetByteArrayRegion raised JNI exception size=%zu", TERMUX_BOOTSTRAP_SYMBOL,
             payload_size);
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return return_controlled_null("SetByteArrayRegion JNI exception", "array-copy");
    }

    LOGI("%s: success bytes=%zu", TERMUX_BOOTSTRAP_SYMBOL, payload_size);
    return result;
}
