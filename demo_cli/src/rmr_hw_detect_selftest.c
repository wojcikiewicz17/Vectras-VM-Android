#include "rmr_hw_detect.h"

#include <stdio.h>
#include <string.h>

static int expect_true(int cond, const char *msg) {
  if (!cond) {
    fprintf(stderr, "FAIL: %s\n", msg);
    return 1;
  }
  return 0;
}

int main(void) {
  int failed = 0;
  RmR_HW_Info hw;

  memset(&hw, 0, sizeof(hw));
  RmR_HW_Detect(&hw);

  failed += expect_true(hw.cacheline_bytes >= 32u, "cacheline hint is sane");
  failed += expect_true(hw.cache_hint_l1 > 0u, "l1 hint present");
  failed += expect_true(hw.cache_hint_l2 > 0u, "l2 hint present");
  failed += expect_true(hw.cache_hint_l3 > 0u, "l3 hint present");

  if (hw.arch == 2u || hw.arch == 4u) {
    failed += expect_true(hw.cache_hint_l4 >= (32u * 1024u * 1024u), "x64/arm64 exposes l4 hint");
  }
  if (hw.arch == 1u || hw.arch == 3u || hw.arch == 8u) {
    failed += expect_true(hw.cache_hint_l4 >= (8u * 1024u * 1024u), "x86/arm/ppc32 exposes l4 hint");
  }

  if (failed != 0) {
    fprintf(stderr, "rmr_hw_detect_selftest FAILED (%d)\n", failed);
    return 1;
  }

  printf("rmr_hw_detect_selftest OK (arch=%u l4=%u bytes)\n", hw.arch, hw.cache_hint_l4);
  return 0;
}
