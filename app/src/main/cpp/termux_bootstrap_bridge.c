#include <errno.h>
#include <jni.h>
#include <limits.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

#include <android/log.h>

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

    if (payload == NULL || payload_size == 0U) {
        LOGE("%s: embedded payload unavailable payload=%p size=%zu", TERMUX_BOOTSTRAP_SYMBOL,
             (const void*)payload, payload_size);
        return return_controlled_null("bootstrap payload missing (asset not embedded)", "payload-source");
    }

#if !defined(TERMUX_BOOTSTRAP_PAYLOAD_DATA) || !defined(TERMUX_BOOTSTRAP_PAYLOAD_SIZE)
    return return_controlled_null("bootstrap payload missing (build symbols not configured)", "payload-source");
#endif

    if (payload_size < 4U) {
        LOGE("%s: embedded payload too small to be a ZIP archive size=%zu", TERMUX_BOOTSTRAP_SYMBOL,
             payload_size);
        return return_controlled_null("bootstrap payload invalid (too small)", "payload-validation");
    }

    if (payload[0] != 0x50U || payload[1] != 0x4BU) {
        LOGE("%s: embedded payload has invalid ZIP signature bytes=%02X%02X", TERMUX_BOOTSTRAP_SYMBOL,
             payload[0], payload[1]);
        return return_controlled_null("bootstrap payload invalid (bad ZIP signature)", "payload-validation");
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
