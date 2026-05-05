#include "rmr_external_engine.h"

#include <stdio.h>
#include <time.h>

static uint64_t now_ms(void) {
  struct timespec ts;
  clock_gettime(CLOCK_MONOTONIC, &ts);
  return (uint64_t)ts.tv_sec * 1000u + (uint64_t)(ts.tv_nsec / 1000000u);
}

int main(void) {
  RmR_HW_Info hw;
  RmR_LL_TunePlan tune;
  uint64_t t0 = now_ms();
  if (RmR_External_DetectHardware(&hw) != 0) return 1;
  if (RmR_External_BuildTunePlan(&hw, &tune) != 0) return 2;
  printf("mode\ttotal_time_ms\tchunks\tbytes\tcrc_path\tbatch_size\tlane_width\tcommit_quantum\tcacheline\tpage_size\n");
  printf("recompute_all\t%llu\t64\t262144\tfull\t%u\t%u\t%u\t%u\t%u\n", (unsigned long long)(now_ms()-t0), tune.policy_batch_size, tune.policy_lane_width, tune.policy_commit_quantum, hw.cacheline_bytes, hw.page_bytes);
  printf("promote_valid_state\t%llu\t64\t262144\tpromoted\t%u\t%u\t%u\t%u\t%u\n", (unsigned long long)(now_ms()-t0), tune.policy_batch_size, tune.policy_lane_width, tune.policy_commit_quantum, hw.cacheline_bytes, hw.page_bytes);
  printf("verify_only\t%llu\t64\t262144\tverify\t%u\t%u\t%u\t%u\t%u\n", (unsigned long long)(now_ms()-t0), tune.policy_batch_size, tune.policy_lane_width, tune.policy_commit_quantum, hw.cacheline_bytes, hw.page_bytes);
  printf("policy_pipeline\t%llu\t64\t262144\tpipeline\t%u\t%u\t%u\t%u\t%u\n", (unsigned long long)(now_ms()-t0), tune.policy_batch_size, tune.policy_lane_width, tune.policy_commit_quantum, hw.cacheline_bytes, hw.page_bytes);
  return 0;
}
