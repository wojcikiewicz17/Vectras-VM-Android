#include "rmr_unified_kernel.h"

#include "bitraf.h"
#include "rmr_corelib.h"
#include "rmr_hw_detect.h"
#include "rmr_policy_kernel.h"

#include <stdlib.h>

typedef enum {
  RMR_KERNEL_STATE_NEW = 0,
  RMR_KERNEL_STATE_READY = 1,
  RMR_KERNEL_STATE_SHUTDOWN = 2
} rmr_kernel_state_t;

struct rmr_kernel {
  rmr_kernel_state_t lifecycle;
  uint32_t seed;
  uint32_t rolling_crc32c;
  uint64_t rolling_bitraf_hash;
  uint32_t stage_counter;
  uint64_t last_route_signature;
  rmr_kernel_capabilities_t capabilities;
};

static int rmr_kernel_is_ready(const rmr_kernel_t *kernel) {
  return kernel && kernel->lifecycle == RMR_KERNEL_STATE_READY;
}

static void rmr_kernel_capabilities_from_hw(const RmR_HW_Info *hw,
                                            rmr_kernel_capabilities_t *caps) {
  caps->arch = hw->arch;
  caps->arch_hex = hw->arch_hex;
  caps->word_bits = hw->word_bits;
  caps->ptr_bits = hw->ptr_bits;
  caps->is_little_endian = hw->is_little_endian;
  caps->has_cycle_counter = hw->has_cycle_counter;
  caps->has_asm_probe = hw->has_asm_probe;
  caps->reg_signature_0 = hw->reg_signature_0;
  caps->reg_signature_1 = hw->reg_signature_1;
  caps->reg_signature_2 = hw->reg_signature_2;
  caps->feature_bits_0 = hw->feature_bits_0;
  caps->feature_bits_1 = hw->feature_bits_1;
  caps->cacheline_bytes = hw->cacheline_bytes;
  caps->cache_hint_l1 = hw->cache_hint_l1;
  caps->cache_hint_l2 = hw->cache_hint_l2;
  caps->cache_hint_l3 = hw->cache_hint_l3;
  caps->page_bytes = hw->page_bytes;
  caps->mem_bus_bits = hw->mem_bus_bits;
  caps->gpio_word_bits = hw->gpio_word_bits;
  caps->gpio_pin_stride = hw->gpio_pin_stride;
  caps->align_bytes = hw->align_bytes;
}

rmr_status_t rmr_kernel_autodetect(rmr_kernel_capabilities_t *out_capabilities) {
  if (!out_capabilities) return RMR_STATUS_ERR_ARG;
  RmR_HW_Info hw;
  rmr_mem_set(&hw, 0u, sizeof(hw));
  RmR_HW_Detect(&hw);
  rmr_kernel_capabilities_from_hw(&hw, out_capabilities);
  return RMR_STATUS_OK;
}

rmr_status_t rmr_kernel_init(rmr_kernel_t **out_kernel, const rmr_kernel_init_desc_t *desc) {
  rmr_kernel_t *kernel;
  if (!out_kernel || !desc) return RMR_STATUS_ERR_ARG;
  if (*out_kernel) {
    if ((*out_kernel)->lifecycle == RMR_KERNEL_STATE_READY) return RMR_STATUS_ERR_ALREADY_INITIALIZED;
    if ((*out_kernel)->lifecycle == RMR_KERNEL_STATE_SHUTDOWN) return RMR_STATUS_ERR_ALREADY_SHUTDOWN;
    return RMR_STATUS_ERR_STATE;
  }

  kernel = (rmr_kernel_t *)malloc(sizeof(*kernel));
  if (!kernel) return RMR_STATUS_ERR_NOMEM;

  rmr_mem_set(kernel, 0u, sizeof(*kernel));
  kernel->lifecycle = RMR_KERNEL_STATE_NEW;
  kernel->seed = desc->seed;
  kernel->rolling_crc32c = desc->seed;
  kernel->rolling_bitraf_hash = (uint64_t)desc->seed;

  if (rmr_kernel_autodetect(&kernel->capabilities) != RMR_STATUS_OK) {
    free(kernel);
    return RMR_STATUS_ERR_STATE;
  }

  kernel->lifecycle = RMR_KERNEL_STATE_READY;
  *out_kernel = kernel;
  return RMR_STATUS_OK;
}

rmr_status_t rmr_kernel_shutdown(rmr_kernel_t **kernel) {
  rmr_kernel_t *ctx;
  if (!kernel) return RMR_STATUS_ERR_ARG;
  if (!*kernel) return RMR_STATUS_ERR_ALREADY_SHUTDOWN;

  ctx = *kernel;
  if (ctx->lifecycle == RMR_KERNEL_STATE_SHUTDOWN) {
    free(ctx);
    *kernel = NULL;
    return RMR_STATUS_ERR_ALREADY_SHUTDOWN;
  }

  ctx->lifecycle = RMR_KERNEL_STATE_SHUTDOWN;
  rmr_mem_set(ctx, 0u, sizeof(*ctx));
  free(ctx);
  *kernel = NULL;
  return RMR_STATUS_OK;
}

rmr_status_t rmr_kernel_ingest(rmr_kernel_t *kernel,
                               const rmr_kernel_ingest_desc_t *desc,
                               rmr_kernel_ingest_result_t *out_result) {
  if (!rmr_kernel_is_ready(kernel) || !desc || !out_result || (!desc->data && desc->data_len != 0u)) {
    return RMR_STATUS_ERR_ARG;
  }

  kernel->rolling_crc32c = RmR_CRC32C(desc->data, desc->data_len) ^ kernel->rolling_crc32c;
  kernel->rolling_bitraf_hash = bitraf_hash(desc->data, desc->data_len, kernel->seed) ^ kernel->rolling_bitraf_hash;
  kernel->stage_counter += 1u;

  out_result->crc32c = kernel->rolling_crc32c;
  out_result->bitraf_hash = kernel->rolling_bitraf_hash;
  out_result->entropy_milli = RmR_EntropyEstimateMilli(desc->data, desc->data_len);
  out_result->stage_counter = kernel->stage_counter;
  return RMR_STATUS_OK;
}

rmr_status_t rmr_kernel_process(rmr_kernel_t *kernel,
                                const rmr_kernel_process_desc_t *desc,
                                rmr_kernel_process_result_t *out_result) {
  if (!rmr_kernel_is_ready(kernel) || !desc || !out_result) return RMR_STATUS_ERR_ARG;

  out_result->cpu_pressure = (uint32_t)((desc->cpu_cycles >> 10u) & 0xFFFFu);
  out_result->storage_pressure =
      (uint32_t)(((desc->storage_read_bytes + desc->storage_write_bytes) >> 10u) & 0xFFFFu);
  out_result->io_pressure = (uint32_t)(((desc->input_bytes + desc->output_bytes) >> 10u) & 0xFFFFu);
  out_result->matrix_determinant =
      (desc->matrix_m00 * desc->matrix_m11) - (desc->matrix_m01 * desc->matrix_m10);

  kernel->stage_counter += 1u;
  return RMR_STATUS_OK;
}

rmr_status_t rmr_kernel_route(rmr_kernel_t *kernel,
                              const rmr_kernel_process_result_t *process,
                              rmr_kernel_route_result_t *out_result) {
  uint32_t route;
  if (!rmr_kernel_is_ready(kernel) || !process || !out_result) return RMR_STATUS_ERR_ARG;

  route = RMR_ROUTE_DISK;
  if (process->cpu_pressure >= process->storage_pressure && process->cpu_pressure >= process->io_pressure) {
    route = RMR_ROUTE_CPU;
  } else if (process->storage_pressure >= process->io_pressure) {
    route = RMR_ROUTE_RAM;
  }

  out_result->route_id = route;
  out_result->route_signature = ((uint64_t)kernel->rolling_crc32c << 32)
                              ^ kernel->rolling_bitraf_hash
                              ^ (uint64_t)process->matrix_determinant
                              ^ (uint64_t)route;
  kernel->last_route_signature = out_result->route_signature;
  kernel->stage_counter += 1u;
  return RMR_STATUS_OK;
}

rmr_status_t rmr_kernel_verify(rmr_kernel_t *kernel,
                               const rmr_kernel_verify_desc_t *desc,
                               rmr_kernel_verify_result_t *out_result) {
  if (!rmr_kernel_is_ready(kernel) || !desc || !out_result || (!desc->data && desc->data_len != 0u)) {
    return RMR_STATUS_ERR_ARG;
  }

  out_result->computed_crc32c = RmR_CRC32C(desc->data, desc->data_len);
  out_result->computed_bitraf_hash = bitraf_hash(desc->data, desc->data_len, kernel->seed);
  out_result->crc_ok = (out_result->computed_crc32c == desc->expected_crc32c) ? 1u : 0u;
  out_result->hash_ok = bitraf_verify(desc->data,
                                      desc->data_len,
                                      desc->expected_bitraf_hash,
                                      kernel->seed)
                      ? 1u
                      : 0u;
  kernel->stage_counter += 1u;

  if (!out_result->crc_ok || !out_result->hash_ok) return RMR_STATUS_ERR_VERIFY;
  return RMR_STATUS_OK;
}

rmr_status_t rmr_kernel_audit(rmr_kernel_t *kernel,
                              const rmr_kernel_ingest_result_t *ingest,
                              const rmr_kernel_process_result_t *process,
                              const rmr_kernel_route_result_t *route,
                              const rmr_kernel_verify_result_t *verify,
                              rmr_kernel_audit_result_t *out_result) {
  uint64_t signature;
  if (!rmr_kernel_is_ready(kernel) || !ingest || !process || !route || !verify || !out_result) {
    return RMR_STATUS_ERR_ARG;
  }

  signature = ((uint64_t)ingest->crc32c << 32) ^ ingest->bitraf_hash;
  signature ^= ((uint64_t)process->cpu_pressure << 48)
            ^ ((uint64_t)process->storage_pressure << 24)
            ^ (uint64_t)process->io_pressure;
  signature ^= (uint64_t)process->matrix_determinant;
  signature ^= route->route_signature;
  signature ^= ((uint64_t)verify->computed_crc32c << 1) ^ verify->computed_bitraf_hash;
  signature ^= ((uint64_t)verify->crc_ok << 2) ^ ((uint64_t)verify->hash_ok << 3);

  out_result->audit_signature = signature;
  out_result->audit_code = (verify->crc_ok && verify->hash_ok) ? 0u : 1u;
  kernel->stage_counter += 1u;
  return RMR_STATUS_OK;
}

rmr_status_t rmr_kernel_get_capabilities(const rmr_kernel_t *kernel,
                                         rmr_kernel_capabilities_t *out_capabilities) {
  if (!rmr_kernel_is_ready(kernel) || !out_capabilities) return RMR_STATUS_ERR_ARG;
  rmr_mem_copy(out_capabilities, &kernel->capabilities, sizeof(*out_capabilities));
  return RMR_STATUS_OK;
}

static void rmr_caps_from_unified(const RmR_UnifiedCapabilities *in, rmr_kernel_capabilities_t *out) {
  out->signature = in->signature;
  out->pointer_bits = in->pointer_bits;
  out->cache_line_bytes = in->cache_line_bytes;
  out->page_bytes = in->page_bytes;
  out->feature_mask = in->feature_mask;
  out->register_width_bits = in->pointer_bits;
  out->pin_count_hint = in->gpio_word_bits;
  out->feature_bits_hi = in->reg_signature_2;
}

int rmr_kernel_init(rmr_kernel_state_t *state, uint32_t seed) {
  RmR_UnifiedConfig config;
  if (!state) return RMR_KERNEL_ERR_ARG;
  config.seed = seed;
  config.arena_bytes = 64u * 1024u * 1024u;
  return RmR_UnifiedKernel_Init(state, &config);
}

int rmr_kernel_shutdown(rmr_kernel_state_t *state) {
  return RmR_UnifiedKernel_Shutdown(state);
}

int rmr_kernel_get_capabilities(const rmr_kernel_state_t *state, rmr_kernel_capabilities_t *out_caps) {
  RmR_UnifiedCapabilities caps;
  int rc;
  if (!state || !out_caps) return RMR_KERNEL_ERR_ARG;
  rc = RmR_UnifiedKernel_QueryCapabilities(state, &caps);
  if (rc != RMR_UK_OK) return rc;
  rmr_caps_from_unified(&caps, out_caps);
  return RMR_KERNEL_OK;
}

int rmr_kernel_autodetect(rmr_kernel_capabilities_t *out_caps) {
  RmR_UnifiedCapabilities caps;
  int rc;
  if (!out_caps) return RMR_KERNEL_ERR_ARG;
  rc = RmR_UnifiedKernel_Detect(&caps);
  if (rc != RMR_UK_OK) return rc;
  rmr_caps_from_unified(&caps, out_caps);
  return RMR_KERNEL_OK;
}

int rmr_kernel_ingest(rmr_kernel_state_t *state, const uint8_t *data, uint32_t len, uint32_t *out_crc32c) {
  RmR_UnifiedIngestState ingest;
  int rc;
  if (!state || !out_crc32c || (!data && len != 0u)) return RMR_KERNEL_ERR_ARG;
  rc = RmR_UnifiedKernel_Ingest(state, data, (size_t)len, &ingest);
  if (rc != RMR_UK_OK) return rc;
  *out_crc32c = ingest.crc32c;
  return RMR_KERNEL_OK;
}

int rmr_kernel_process(rmr_kernel_state_t *state, int32_t a, int32_t b, uint32_t mode, int32_t *out_value) {
  uint32_t ua = (uint32_t)a;
  uint32_t ub = (uint32_t)b;
  if (!state || !out_value) return RMR_KERNEL_ERR_ARG;
  switch (mode & 3u) {
    case 0u: *out_value = (int32_t)(ua ^ ub); break;
    case 1u: *out_value = (int32_t)(ua + ub); break;
    case 2u: *out_value = (int32_t)(ua - ub); break;
    default: *out_value = (int32_t)((ua << (ub & 31u)) | (ua >> ((32u - (ub & 31u)) & 31u))); break;
  }
  state->stage_counter += 1u;
  return RMR_KERNEL_OK;
}

int rmr_kernel_route(rmr_kernel_state_t *state, const rmr_kernel_route_input_t *in, rmr_kernel_route_output_t *out) {
  RmR_UnifiedProcessState process;
  RmR_UnifiedRouteState route;
  int rc;
  if (!state || !in || !out) return RMR_KERNEL_ERR_ARG;
  rc = RmR_UnifiedKernel_Process(state,
                                 in->cpu_cycles,
                                 in->storage_read_bytes,
                                 in->storage_write_bytes,
                                 in->input_bytes,
                                 in->output_bytes,
                                 in->m00,
                                 in->m01,
                                 in->m10,
                                 in->m11,
                                 &process);
  if (rc != RMR_UK_OK) return rc;
  rc = RmR_UnifiedKernel_Route(state, &process, &route);
  if (rc != RMR_UK_OK) return rc;
  out->route = route.route_id;
  out->matrix_determinant = process.matrix_determinant;
  out->cpu_pressure = process.cpu_pressure;
  out->storage_pressure = process.storage_pressure;
  out->io_pressure = process.io_pressure;
  out->route_tag = route.route_tag;
  return RMR_KERNEL_OK;
}

int rmr_kernel_verify(rmr_kernel_state_t *state, const uint8_t *data, uint32_t len, uint32_t expected_crc32c, uint32_t *out_verify_ok) {
  RmR_UnifiedVerifyState verify;
  int rc;
  if (!state || !out_verify_ok || (!data && len != 0u)) return RMR_KERNEL_ERR_ARG;
  rc = RmR_UnifiedKernel_Verify(state, data, (size_t)len, expected_crc32c, &verify);
  if (rc != RMR_UK_OK) return rc;
  *out_verify_ok = verify.verify_ok;
  return verify.verify_ok ? RMR_KERNEL_OK : 1;
}

int rmr_kernel_audit(rmr_kernel_state_t *state, uint64_t *counters, uint32_t counter_count) {
  if (!state || !counters || counter_count < 7u) return RMR_KERNEL_ERR_ARG;
  counters[0] = state->caps.signature;
  counters[1] = state->caps.pointer_bits;
  counters[2] = state->crc32c;
  counters[3] = state->entropy;
  counters[4] = state->stage_counter;
  counters[5] = (uint64_t)(uint32_t)(state->last_route_tag & 0xFFFFFFFFu);
  counters[6] = (uint64_t)(uint32_t)((state->last_route_tag >> 32) & 0xFFFFFFFFu);
  return RMR_KERNEL_OK;
}
