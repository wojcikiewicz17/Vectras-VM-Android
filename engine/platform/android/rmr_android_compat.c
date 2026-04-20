#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

static uint8_t *g_rmr_scratch_area = NULL;
static size_t g_rmr_scratch_size = 0;
static pthread_mutex_t g_rmr_scratch_lock = PTHREAD_MUTEX_INITIALIZER;

uint8_t *rmr_arena_alloc(size_t size) {
    uint8_t *ptr = (uint8_t *)malloc(size);
    if (ptr != NULL) {
        memset(ptr, 0, size);
    }
    return ptr;
}

void rmr_arena_free(void *ptr) {
    free(ptr);
}

int rmr_android_init(void) {
    pthread_mutex_lock(&g_rmr_scratch_lock);
    if (g_rmr_scratch_area == NULL) {
        g_rmr_scratch_area = rmr_arena_alloc(1024u * 1024u);
        if (g_rmr_scratch_area == NULL) {
            pthread_mutex_unlock(&g_rmr_scratch_lock);
            return -1;
        }
        g_rmr_scratch_size = 1024u * 1024u;
    }
    pthread_mutex_unlock(&g_rmr_scratch_lock);
    return 0;
}

void rmr_android_cleanup(void) {
    pthread_mutex_lock(&g_rmr_scratch_lock);
    if (g_rmr_scratch_area != NULL) {
        rmr_arena_free(g_rmr_scratch_area);
        g_rmr_scratch_area = NULL;
        g_rmr_scratch_size = 0;
    }
    pthread_mutex_unlock(&g_rmr_scratch_lock);
}

uint8_t *rmr_android_get_scratch_area(void) {
    return g_rmr_scratch_area;
}

size_t rmr_android_get_scratch_size(void) {
    return g_rmr_scratch_size;
}
