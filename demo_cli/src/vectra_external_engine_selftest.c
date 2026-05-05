#include "rmr_external_engine.h"
#include "bitraf.h"

#include <stdio.h>
#include <string.h>

static int bitomega_transition_test(void) {
  bitomega_node_t node = {BITOMEGA_ZERO, BITOMEGA_DIR_NONE, BITOMEGA_Q16_HALF, BITOMEGA_Q16_HALF};
  bitomega_ctx_t ctx = bitomega_ctx_default(7u);
  ctx.coherence_in = BITOMEGA_Q16_ONE;
  return RmR_External_RunBitOmegaStep(&node, &ctx) == 0 ? 0 : 1;
}

static int bitraf_roundtrip_test(void) {
  static const uint8_t payload[] = "vectra_rmr_roundtrip";
  uint64_t seed = 42u;
  uint64_t hash = bitraf_hash(payload, sizeof(payload), seed);
  RmR_ExternalBitRafVerifyRequest req = {payload, sizeof(payload), hash, seed};
  return RmR_External_RunBitRafVerify(&req);
}

static int zipraf_coherence_test(void) {
  static const uint8_t payload[] = {1u,2u,3u,4u,5u,6u};
  RmR_ExternalZipRafRequest req = {payload, sizeof(payload), 11u, 3u, 0u};
  RmR_ZiprafOutput out;
  return RmR_External_RunZipRaf(&req, &out);
}

static int policy_pipeline_replay_test(void) {
  RmR_ExternalPolicyRequest req;
  RmR_AuditSummary sum;
  memset(&req, 0, sizeof(req));
  req.input_path = "bench/results/policy_in.bin";
  req.output_path = "bench/results/policy_out_ext.bin";
  req.audit_log_path = "bench/results/policy_log_ext.log";
  req.config.chunk_size = 64u;
  req.config.mutation_stride = 0u;
  req.config.mutation_xor = 0u;
  req.config.triad.cpu_ok = 1u;
  req.config.triad.ram_ok = 1u;
  req.config.triad.disk_ok = 1u;
  return RmR_External_RunPolicyPipeline(&req, &sum);
}

int main(void) {
  RmR_HW_Info hw;
  RmR_LL_TunePlan tune;
  FILE *fixture = fopen("bench/results/policy_in.bin", "wb");
  uint8_t block[512];
  size_t i;
  if (!fixture) return 10;
  for (i = 0; i < sizeof(block); ++i) block[i] = (uint8_t)(i & 0xFFu);
  fwrite(block, 1u, sizeof(block), fixture);
  fclose(fixture);

  if (bitomega_transition_test() != 0) return 1;
  if (bitraf_roundtrip_test() != 0) return 2;
  if (zipraf_coherence_test() != 0) return 3;
  if (policy_pipeline_replay_test() != 0) return 4;
  if (RmR_External_DetectHardware(&hw) != 0) return 5;
  if (RmR_External_BuildTunePlan(&hw, &tune) != 0) return 6;

  printf("OK vectra_external_engine_selftest\n");
  return 0;
}
