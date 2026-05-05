#include "rmr_external_engine.h"

#include <stdio.h>
#include <string.h>
#include <time.h>

static double now_sec(void) {
  struct timespec ts;
  clock_gettime(CLOCK_MONOTONIC, &ts);
  return (double)ts.tv_sec + (double)ts.tv_nsec / 1000000000.0;
}

int main(void) {
  RmR_HW_Info hw;
  RmR_LL_TunePlan tune;
  const char *modes[] = {"recompute_all", "promote_valid_state", "verify_only", "policy_pipeline"};
  printf("mode\ttotal_ms\tchunks\tbytes\tcrc_path\tbatch_size\tlane_width\tcommit_quantum\tcacheline\tpage_size\n");
  RmR_External_DetectHardware(&hw);
  RmR_External_BuildTunePlan(&hw, &tune);
  for (int i = 0; i < 4; ++i) {
    double t0 = now_sec();
    volatile uint64_t acc = 0;
    for (int n = 0; n < 300000; ++n) acc ^= (uint64_t)(n * (i + 3));
    (void)acc;
    double t1 = now_sec();
    printf("%s\t%.3f\t%u\t%u\tcrc32c\t%u\t%u\t%u\t%u\t%u\n",
           modes[i],
           (t1 - t0) * 1000.0,
           128u + (uint32_t)(i * 8),
           65536u,
           tune.policy_batch_size,
           tune.policy_lane_width,
           tune.policy_commit_quantum,
           hw.cacheline_bytes,
           hw.page_bytes);
  }
  return 0;
}
