#include "bitomega.h"
#include "bitraf.h"
#include "rmr_external_engine.h"
#include "rmr_math_fabric.h"

#include <stdio.h>
#include <string.h>

int main(void) {
  const uint8_t payload[] = "vectra_rmr_external_contract";
  uint8_t frame[512];
  uint8_t rebuilt[512];
  bitomega_node_t node = {BITOMEGA_ZERO, BITOMEGA_DIR_NONE, BITOMEGA_Q16_HALF, BITOMEGA_Q16_HALF};
  bitomega_ctx_t ctx = bitomega_ctx_default(7u);
  RmR_HW_Info hw;
  RmR_LL_TunePlan tune;
  RmR_MathFabricPlan math0;
  RmR_MathFabricPlan math1;
  RmR_ZiprafInput zin;
  RmR_ZiprafOutput zout;
  size_t frame_len;
  size_t rebuilt_len;
  uint64_t h;
  int verify_ok = 0;

  if (RmR_External_DetectHardware(&hw) != 0) return 1;
  if (RmR_External_BuildTunePlan(&hw, &tune) != 0) return 2;

  ctx.coherence_in = bitomega_float_to_q16(0.85f);
  ctx.entropy_in = bitomega_float_to_q16(0.20f);
  ctx.noise_in = bitomega_float_to_q16(0.10f);
  ctx.load = bitomega_float_to_q16(0.30f);
  if (RmR_External_RunBitOmegaStep(&node, &ctx) != 0 || !bitomega_invariant_ok(&node)) return 3;

  frame_len = bitraf_compress(payload, sizeof(payload), frame, sizeof(frame), 11u);
  rebuilt_len = bitraf_reconstruct(frame, frame_len, rebuilt, sizeof(rebuilt), 11u);
  if (rebuilt_len != sizeof(payload) || memcmp(payload, rebuilt, sizeof(payload)) != 0) return 4;

  h = bitraf_hash(payload, sizeof(payload), 11u);
  if (RmR_External_RunBitRafVerify(payload, sizeof(payload), h, 11u, &verify_ok) != 0 || !verify_ok) return 5;

  zin.seed = 17u;
  zin.trajectory_id = 3u;
  zin.invariant_mask = 0x0Fu;
  zin.payload_ptr = payload;
  zin.payload_len = sizeof(payload);
  if (RmR_External_RunZipRaf(&zin, &zout) != 0) return 6;

  RmR_MathFabric_AutodetectPlan(&hw, &math0);
  RmR_MathFabric_AutodetectPlan(&hw, &math1);
  if (math0.matrix_seed != math1.matrix_seed) return 7;

  printf("rmr_external_engine_selftest OK arch=%u batch=%u lane=%u zip_flags=%u\n",
         hw.arch,
         tune.policy_batch_size,
         tune.policy_lane_width,
         zout.status_flags);
  return 0;
}
