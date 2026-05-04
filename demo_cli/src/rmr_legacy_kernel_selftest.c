#include "rmr_unified_kernel.h"

#include <stdint.h>
#include <stdio.h>

#define LEGACY_KERNEL_TEST_MAX_POOL 64u

static int expect_status(rmr_status_t got, rmr_status_t expected, const char *label) {
  if (got != expected) {
    printf("FAIL %s got=%d expected=%d\n", label, (int)got, (int)expected);
    return 0;
  }
  return 1;
}

int main(void) {
  rmr_legacy_kernel_init_desc_t init_desc;
  rmr_legacy_kernel_t *kernel = NULL;
  rmr_legacy_kernel_t *pool[LEGACY_KERNEL_TEST_MAX_POOL];
  uint32_t i;
  uint32_t allocated = 0u;

  init_desc.seed = 0xA5A5u;

  if (!expect_status(rmr_legacy_kernel_init(&kernel, &init_desc), RMR_STATUS_OK, "single init")) return 1;
  if (!expect_status(rmr_legacy_kernel_init(&kernel, &init_desc), RMR_STATUS_ERR_ALREADY_INITIALIZED, "duplicate init")) return 1;
  if (!expect_status(rmr_legacy_kernel_shutdown(&kernel), RMR_STATUS_OK, "shutdown")) return 1;
  if (!expect_status(rmr_legacy_kernel_shutdown(&kernel), RMR_STATUS_ERR_ALREADY_SHUTDOWN, "double shutdown")) return 1;

  for (i = 0u; i < LEGACY_KERNEL_TEST_MAX_POOL; ++i) {
    rmr_status_t rc;
    pool[i] = NULL;
    rc = rmr_legacy_kernel_init(&pool[i], &init_desc);
    if (rc == RMR_STATUS_ERR_NOMEM) {
      allocated = i;
      break;
    }
    if (!expect_status(rc, RMR_STATUS_OK, "pool fill init")) return 1;
  }

  if (allocated == 0u) {
    printf("FAIL pool exhaustion did not trigger\n");
    return 1;
  }

  for (i = 0u; i < allocated; ++i) {
    if (!expect_status(rmr_legacy_kernel_shutdown(&pool[i]), RMR_STATUS_OK, "pool drain shutdown")) return 1;
  }


  {
    rmr_legacy_capabilities_t caps;
    rmr_legacy_kernel_process_result_t proc;
    rmr_memory_tier_t tier = RMR_MEM_TIER_STORAGE;
    caps.cache_hint_l1 = 32u * 1024u;
    caps.cache_hint_l2 = 256u * 1024u;
    proc.cpu_pressure = 200u;
    proc.storage_pressure = 10u;
    proc.io_pressure = 200u;
    proc.matrix_determinant = 1;
    if (!expect_status(rmr_legacy_kernel_select_memory_tier(&caps, &proc, 4096u, &tier), RMR_STATUS_OK, "tier select l1")) return 1;
    if (tier != RMR_MEM_TIER_L1) { printf("FAIL expected L1 tier\n"); return 1; }
  }

  printf("OK legacy kernel selftest\n");
  return 0;
}
