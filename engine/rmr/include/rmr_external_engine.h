#ifndef RMR_EXTERNAL_ENGINE_H
#define RMR_EXTERNAL_ENGINE_H

#include <stddef.h>
#include <stdint.h>

#include "bitomega.h"
#include "rmr_hw_detect.h"
#include "rmr_ll_tuning.h"
#include "rmr_policy_kernel.h"
#include "rmr_unified_kernel.h"
#include "rmr_zipraf_core.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
  const uint8_t *input_data;
  size_t input_size;
  uint32_t seed;
  uint32_t trajectory_id;
  uint32_t invariant_mask;
} RmR_ExternalZipRafRequest;

typedef struct {
  const uint8_t *verify_data;
  size_t verify_size;
  uint64_t expected_hash64;
  uint64_t seed;
} RmR_ExternalBitRafVerifyRequest;

typedef struct {
  const char *input_path;
  const char *output_path;
  const char *audit_log_path;
  RmR_PipelineConfig config;
} RmR_ExternalPolicyRequest;

typedef struct {
  const char *event;
  uint64_t offset;
  uint64_t size;
  const char *mem_tier;
  uint32_t route_id;
  uint32_t crc32c;
  uint64_t hash64;
  uint32_t entropy_milli;
  uint32_t math_signature;
  uint64_t stage_signature;
  uint8_t decision_mode;
  uint8_t recompute_skipped;
  uint8_t verify_ok;
} RmR_ExternalStatePromotionEntry;

int RmR_External_DetectHardware(RmR_HW_Info *out_hw);
int RmR_External_BuildTunePlan(const RmR_HW_Info *hw, RmR_LL_TunePlan *out_plan);
int RmR_External_RunPolicyPipeline(const RmR_ExternalPolicyRequest *request, RmR_AuditSummary *out_summary);
int RmR_External_RunZipRaf(const RmR_ExternalZipRafRequest *request, RmR_ZiprafOutput *out_zipraf);
int RmR_External_RunBitRafVerify(const RmR_ExternalBitRafVerifyRequest *request);
int RmR_External_RunBitOmegaStep(bitomega_node_t *node, const bitomega_ctx_t *ctx);
int RmR_External_WriteStatePromotionReport(const char *report_path, const RmR_ExternalStatePromotionEntry *entry);

#ifdef __cplusplus
}
#endif

#endif
