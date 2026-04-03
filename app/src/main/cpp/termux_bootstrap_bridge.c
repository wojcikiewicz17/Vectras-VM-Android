#include <errno.h>
#include <jni.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

#include <android/log.h>

#define TERMUX_BOOTSTRAP_TAG "termux-bootstrap"
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
/*
 * Controlled fallback: valid empty ZIP archive (EOCD only).
 * This keeps JNI contract valid while preserving diagnostics.
 */
static const unsigned char kEmptyZipPayload[] = {
    0x50, 0x4B, 0x05, 0x06,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00
};

static const unsigned char* get_embedded_bootstrap_data(void) {
    return kEmptyZipPayload;
}

static size_t get_embedded_bootstrap_size(void) {
    return sizeof(kEmptyZipPayload);
}
#endif

/* JNI symbol must exactly match: com.termux.app.TermuxInstaller#nativeGetZip() */
JNIEXPORT jbyteArray JNICALL
Java_com_termux_app_TermuxInstaller_nativeGetZip(JNIEnv* env, jclass clazz) {
    (void)clazz;

    if (env == NULL) {
        LOGE("nativeGetZip failed: env=NULL (context=jni-entry)");
        return NULL;
    }

    const unsigned char* payload = get_embedded_bootstrap_data();
    const size_t payload_size = get_embedded_bootstrap_size();

    if (payload == NULL || payload_size == 0U) {
        LOGE("nativeGetZip failed: embedded payload unavailable (payload=%p size=%zu)",
             (const void*)payload,
             payload_size);
        return NULL;
    }

#if !defined(TERMUX_BOOTSTRAP_PAYLOAD_DATA) || !defined(TERMUX_BOOTSTRAP_PAYLOAD_SIZE)
    LOGE("nativeGetZip using controlled fallback empty ZIP payload; real embedded bootstrap not configured");
#endif

    if (payload_size > (size_t)INT32_MAX) {
        LOGE("nativeGetZip failed: payload too large for jbyteArray (size=%zu max=%d)",
             payload_size,
             INT32_MAX);
        return NULL;
    }

    jbyteArray result = (*env)->NewByteArray(env, (jsize)payload_size);
    if (result == NULL) {
        const int saved_errno = errno;
        LOGE("nativeGetZip failed: NewByteArray returned NULL (size=%zu errno=%d msg=%s)",
             payload_size,
             saved_errno,
             strerror(saved_errno));
        return NULL;
    }

    (*env)->SetByteArrayRegion(env, result, 0, (jsize)payload_size, (const jbyte*)payload);
    if ((*env)->ExceptionCheck(env)) {
        LOGE("nativeGetZip failed: SetByteArrayRegion raised JNI exception (size=%zu)", payload_size);
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return NULL;
    }

    LOGI("nativeGetZip success: returned payload bytes=%zu", payload_size);
    return result;
}
