#include <jni.h>
#include <stdio.h>

#if defined(__GNUC__) || defined(__clang__)
__attribute__((weak))
#endif
const char *run_sector(void);

JNIEXPORT jstring JNICALL
Java_com_rafacodephi_app_MainActivity_nativeMessage(JNIEnv *env, jobject thiz) {
    (void)thiz;
    return (*env)->NewStringUTF(env, "RAFACODEphi native bridge active");
}

JNIEXPORT jstring JNICALL
Java_com_rafacodephi_app_MainActivity_nativeSectorSummary(JNIEnv *env, jobject thiz) {
    (void)thiz;

    const char *sector = NULL;
    if (run_sector != NULL) {
        sector = run_sector();
    }
    if (sector == NULL || sector[0] == '\0') {
        sector = "unavailable";
    }

    char json[512];
    (void)snprintf(
        json,
        sizeof(json),
        "{\"source\":\"run_sector\",\"summary\":\"%s\",\"status\":\"ok\"}",
        sector);

    return (*env)->NewStringUTF(env, json);
}
