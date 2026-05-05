#include "rmr_external_engine.h"

#include <stdio.h>

#include "bitraf.h"

int RmR_External_DetectHardware(RmR_HW_Info *out_hw) {
  if (!out_hw) return -1;
  RmR_HW_Detect(out_hw);
  return 0;
}

int RmR_External_BuildTunePlan(const RmR_HW_Info *hw, RmR_LL_TunePlan *out_plan) {
  if (!hw || !out_plan) return -1;
  RmR_LL_ApplyTuneDefaults(hw, out_plan);
  return 0;
}

int RmR_External_RunPolicyPipeline(const RmR_ExternalPolicyRequest *request, RmR_AuditSummary *out_summary) {
  if (!request || !request->input_path || !request->output_path || !request->audit_log_path || !out_summary) return -1;
  return RmR_RunPolicyPipeline(request->input_path, request->output_path, request->audit_log_path, &request->config, out_summary);
}

int RmR_External_RunZipRaf(const RmR_ExternalZipRafRequest *request, RmR_ZiprafOutput *out_zipraf) {
  RmR_ZiprafInput in;
  if (!request || !request->input_data || request->input_size == 0u || !out_zipraf) return -1;
  in.seed = request->seed;
  in.trajectory_id = request->trajectory_id;
  in.invariant_mask = request->invariant_mask;
  in.payload_ptr = request->input_data;
  in.payload_len = request->input_size;
  return RmR_Zipraf_Execute(&in, out_zipraf);
}

int RmR_External_RunBitRafVerify(const RmR_ExternalBitRafVerifyRequest *request) {
  if (!request || !request->verify_data) return -1;
  return bitraf_verify(request->verify_data, request->verify_size, request->expected_hash64, request->seed) ? 0 : -1;
}

int RmR_External_RunBitOmegaStep(bitomega_node_t *node, const bitomega_ctx_t *ctx) {
  return (int)bitomega_transition(node, ctx);
}

int RmR_External_WriteStatePromotionReport(const char *report_path, const RmR_ExternalStatePromotionEntry *entry) {
  FILE *f;
  if (!report_path || !entry || !entry->event || !entry->mem_tier) return -1;
  f = fopen(report_path, "a");
  if (!f) return -2;
  fprintf(f,
          "{\"event\":\"%s\",\"offset\":%llu,\"size\":%llu,\"mem_tier\":\"%s\",\"route_id\":%u,\"crc32c\":%u,\"hash64\":%llu,\"entropy_milli\":%u,\"math_signature\":%u,\"stage_signature\":%llu,\"decision_mode\":%u,\"recompute_skipped\":%u,\"verify_ok\":%u}\n",
          entry->event,
          (unsigned long long)entry->offset,
          (unsigned long long)entry->size,
          entry->mem_tier,
          entry->route_id,
          entry->crc32c,
          (unsigned long long)entry->hash64,
          entry->entropy_milli,
          entry->math_signature,
          (unsigned long long)entry->stage_signature,
          (unsigned)entry->decision_mode,
          (unsigned)entry->recompute_skipped,
          (unsigned)entry->verify_ok);
  fclose(f);
  return 0;
}
