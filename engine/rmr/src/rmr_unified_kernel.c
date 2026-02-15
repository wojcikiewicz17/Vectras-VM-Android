#include "rmr_unified_kernel.h"
#include "rmr_hw_detect.h"

#include <stdlib.h>
#include <string.h>

#define RMR_UK_HANDLE_INDEX_BITS 10u
#define RMR_UK_HANDLE_INDEX_MASK ((1u << RMR_UK_HANDLE_INDEX_BITS) - 1u)
#define RMR_UK_HANDLE_GEN_SHIFT RMR_UK_HANDLE_INDEX_BITS
#define RMR_UK_HANDLE_GEN_MASK ((1u << (31u - RMR_UK_HANDLE_GEN_SHIFT)) - 1u)

static uint32_t rmr_crc32c_update(uint32_t seed, const uint8_t *data, size_t len) {
  uint32_t crc = ~seed;
  for (size_t i = 0; i < len; ++i) {
    crc ^= data[i];
    for (int b = 0; b < 8; ++b) {
      uint32_t mask = (uint32_t)-(int32_t)(crc & 1u);
      crc = (crc >> 1) ^ (0x82F63B78u & mask);
    }
  }
  return ~crc;
}

static uint32_t rmr_make_signature(const RmR_HW_Info *hw) {
  uint32_t arch = 0x0000u;
  switch (hw->arch) {
    case 4u: arch = 0x0100u; break;
    case 3u: arch = 0x0200u; break;
    case 2u: arch = 0x0300u; break;
    case 1u: arch = 0x0400u; break;
    case 5u: arch = (hw->ptr_bits == 64u) ? 0x0500u : 0x0600u; break;
    default: break;
  }
  return arch | 0x0010u;
}

static uint32_t rmr_feature_mask(const RmR_HW_Info *hw) {
  uint32_t mask = 0u;
  if (hw->arch == 3u || hw->arch == 4u) mask |= (1u << 0);
  if (hw->feature_bits_0 & (1u << 1)) mask |= (1u << 1);
  if (hw->feature_bits_0 & (1u << 2)) mask |= (1u << 2);
  if (hw->feature_bits_0 & (1u << 3)) mask |= (1u << 3);
  if (hw->feature_bits_0 & (1u << 4)) mask |= (1u << 4);
  if (hw->feature_bits_0 & (1u << 5)) mask |= (1u << 5);
  if (mask & ((1u << 0) | (1u << 4) | (1u << 5))) mask |= (1u << 6);
  return mask;
}

int RmR_UnifiedKernel_Detect(RmR_UnifiedCapabilities *out_caps) {
  if (!out_caps) return RMR_UK_ERR_ARG;
  RmR_HW_Info hw;
  memset(&hw, 0, sizeof(hw));
  RmR_HW_Detect(&hw);
  out_caps->signature = rmr_make_signature(&hw);
  out_caps->pointer_bits = hw.ptr_bits;
  out_caps->cache_line_bytes = hw.cacheline_bytes;
  out_caps->page_bytes = hw.page_bytes;
  out_caps->feature_mask = rmr_feature_mask(&hw);
  out_caps->reg_signature_0 = hw.reg_signature_0;
  out_caps->reg_signature_1 = hw.reg_signature_1;
  out_caps->reg_signature_2 = hw.reg_signature_2;
  out_caps->gpio_word_bits = hw.gpio_word_bits;
  out_caps->gpio_pin_stride = hw.gpio_pin_stride;
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_Init(RmR_UnifiedKernel *kernel, const RmR_UnifiedConfig *config) {
  if (!kernel || !config) return RMR_UK_ERR_ARG;
  memset(kernel, 0, sizeof(*kernel));
  if (RmR_UnifiedKernel_Detect(&kernel->caps) != RMR_UK_OK) return RMR_UK_ERR_STATE;
  kernel->seed = config->seed;
  kernel->crc32c = config->seed;
  kernel->entropy = config->seed ^ 0x9E3779B9u;
  kernel->arena_capacity = config->arena_bytes;
  if (kernel->arena_capacity != 0u) {
    kernel->arena_base = (uint8_t*)malloc(kernel->arena_capacity);
    if (!kernel->arena_base) return RMR_UK_ERR_NOMEM;
  }
  kernel->initialized = 1u;
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_Shutdown(RmR_UnifiedKernel *kernel) {
  if (!kernel) return RMR_UK_ERR_ARG;
  free(kernel->arena_base);
  memset(kernel, 0, sizeof(*kernel));
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_QueryCapabilities(const RmR_UnifiedKernel *kernel, RmR_UnifiedCapabilities *out_caps) {
  if (!kernel || !out_caps || !kernel->initialized) return RMR_UK_ERR_ARG;
  *out_caps = kernel->caps;
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_Ingest(RmR_UnifiedKernel *kernel, const uint8_t *data, size_t len, RmR_UnifiedIngestState *out) {
  if (!kernel || !out || (!data && len != 0) || !kernel->initialized) return RMR_UK_ERR_ARG;
  kernel->crc32c = rmr_crc32c_update(kernel->crc32c, data, len);
  kernel->entropy = rmr_crc32c_update(kernel->entropy ^ (uint32_t)len, data, len);
  kernel->stage_counter += 1u;
  out->crc32c = kernel->crc32c;
  out->entropy = kernel->entropy;
  out->stage_counter = kernel->stage_counter;
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_Process(RmR_UnifiedKernel *kernel,
                              uint64_t cpu_cycles,
                              uint64_t storage_read_bytes,
                              uint64_t storage_write_bytes,
                              uint64_t input_bytes,
                              uint64_t output_bytes,
                              int64_t m00,
                              int64_t m01,
                              int64_t m10,
                              int64_t m11,
                              RmR_UnifiedProcessState *out) {
  if (!kernel || !out || !kernel->initialized) return RMR_UK_ERR_ARG;
  out->cpu_pressure = (uint32_t)((cpu_cycles >> 10) & 0xFFFFu);
  out->storage_pressure = (uint32_t)(((storage_read_bytes + storage_write_bytes) >> 10) & 0xFFFFu);
  out->io_pressure = (uint32_t)(((input_bytes + output_bytes) >> 10) & 0xFFFFu);
  out->matrix_determinant = (m00 * m11) - (m01 * m10);
  kernel->stage_counter += 1u;
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_Route(RmR_UnifiedKernel *kernel, const RmR_UnifiedProcessState *process, RmR_UnifiedRouteState *out) {
  if (!kernel || !process || !out || !kernel->initialized) return RMR_UK_ERR_ARG;
  uint32_t route = 3u;
  if (process->cpu_pressure >= process->storage_pressure && process->cpu_pressure >= process->io_pressure) route = 1u;
  else if (process->storage_pressure >= process->io_pressure) route = 2u;
  out->route_id = route;
  out->route_tag = ((uint64_t)kernel->crc32c << 32) ^ ((uint64_t)kernel->entropy) ^ (uint64_t)route ^ (uint64_t)process->matrix_determinant;
  kernel->last_route_tag = out->route_tag;
  kernel->stage_counter += 1u;
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_Verify(RmR_UnifiedKernel *kernel, const uint8_t *data, size_t len, uint32_t expected_crc32c, RmR_UnifiedVerifyState *out) {
  if (!kernel || !out || (!data && len != 0) || !kernel->initialized) return RMR_UK_ERR_ARG;
  out->computed_crc32c = rmr_crc32c_update(0u, data, len);
  out->verify_ok = (expected_crc32c == out->computed_crc32c) ? 1u : 0u;
  kernel->stage_counter += 1u;
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_Audit(RmR_UnifiedKernel *kernel,
                            const RmR_UnifiedIngestState *ingest,
                            const RmR_UnifiedProcessState *process,
                            const RmR_UnifiedRouteState *route,
                            const RmR_UnifiedVerifyState *verify,
                            RmR_UnifiedAuditState *out) {
  if (!kernel || !ingest || !process || !route || !verify || !out || !kernel->initialized) return RMR_UK_ERR_ARG;
  uint64_t sig = ((uint64_t)ingest->crc32c << 32) ^ (uint64_t)ingest->entropy;
  sig ^= ((uint64_t)process->cpu_pressure << 48) ^ ((uint64_t)process->storage_pressure << 24) ^ (uint64_t)process->io_pressure;
  sig ^= (uint64_t)process->matrix_determinant;
  sig ^= route->route_tag;
  sig ^= ((uint64_t)verify->computed_crc32c << 1) ^ (uint64_t)verify->verify_ok;
  out->audit_signature = sig;
  out->audit_code = verify->verify_ok ? 0u : 1u;
  kernel->stage_counter += 1u;
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_Copy(RmR_UnifiedKernel *kernel, uint8_t *dst, const uint8_t *src, size_t len) {
  if (!kernel || !dst || !src || !kernel->initialized) return RMR_UK_ERR_ARG;
  memmove(dst, src, len);
  return RMR_UK_OK;
}

uint32_t RmR_UnifiedKernel_XorChecksum(RmR_UnifiedKernel *kernel, const uint8_t *data, size_t len) {
  if (!kernel || !data || !kernel->initialized) return 0u;
  uint32_t x = 0u;
  for (size_t i = 0; i < len; ++i) x ^= data[i];
  return x;
}

static int rmr_decode_handle(uint32_t handle, uint32_t *idx, uint32_t *gen) {
  if (!idx || !gen || handle == 0u) return RMR_UK_ERR_HANDLE;
  uint32_t index = handle & RMR_UK_HANDLE_INDEX_MASK;
  if (index == 0u || index > RMR_UK_MAX_SLOTS) return RMR_UK_ERR_HANDLE;
  uint32_t generation = (handle >> RMR_UK_HANDLE_GEN_SHIFT) & RMR_UK_HANDLE_GEN_MASK;
  if (generation == 0u) return RMR_UK_ERR_HANDLE;
  *idx = index - 1u;
  *gen = generation;
  return RMR_UK_OK;
}

static uint32_t rmr_make_handle(uint32_t idx, uint32_t gen) {
  uint32_t g = gen & RMR_UK_HANDLE_GEN_MASK;
  if (g == 0u) g = 1u;
  return (g << RMR_UK_HANDLE_GEN_SHIFT) | (idx + 1u);
}

int RmR_UnifiedKernel_ArenaAlloc(RmR_UnifiedKernel *kernel, uint32_t bytes, uint32_t *out_handle) {
  if (!kernel || !out_handle || !kernel->initialized || bytes == 0u || !kernel->arena_base) return RMR_UK_ERR_ARG;
  for (uint32_t i = 0; i < RMR_UK_MAX_SLOTS; ++i) {
    if (kernel->slots[i].in_use) continue;
    uint32_t cursor = 0u;
    for (;;) {
      uint32_t next_off = kernel->arena_capacity;
      int found = -1;
      for (uint32_t j = 0; j < RMR_UK_MAX_SLOTS; ++j) {
        if (!kernel->slots[j].in_use) continue;
        if (kernel->slots[j].offset >= cursor && kernel->slots[j].offset < next_off) {
          next_off = kernel->slots[j].offset;
          found = (int)j;
        }
      }
      if (next_off - cursor >= bytes) {
        kernel->slots[i].in_use = 1u;
        kernel->slots[i].offset = cursor;
        kernel->slots[i].size = bytes;
        kernel->slots[i].generation += 1u;
        *out_handle = rmr_make_handle(i, kernel->slots[i].generation);
        return RMR_UK_OK;
      }
      if (found < 0) break;
      cursor = kernel->slots[found].offset + kernel->slots[found].size;
      if (cursor > kernel->arena_capacity - bytes) break;
    }
  }
  return RMR_UK_ERR_NOMEM;
}

static int rmr_arena_ptr(RmR_UnifiedKernel *kernel, uint32_t handle, uint32_t offset, uint32_t len, uint8_t **out) {
  if (!kernel || !out || !kernel->arena_base) return RMR_UK_ERR_ARG;
  uint32_t idx = 0u, gen = 0u;
  int rc = rmr_decode_handle(handle, &idx, &gen);
  if (rc != RMR_UK_OK) return rc;
  RmR_UnifiedArenaSlot *s = &kernel->slots[idx];
  if (!s->in_use || s->generation != gen) return RMR_UK_ERR_HANDLE;
  if (offset > s->size || len > s->size - offset) return RMR_UK_ERR_RANGE;
  *out = kernel->arena_base + s->offset + offset;
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_ArenaFree(RmR_UnifiedKernel *kernel, uint32_t handle) {
  if (!kernel || !kernel->initialized) return RMR_UK_ERR_ARG;
  uint32_t idx = 0u, gen = 0u;
  int rc = rmr_decode_handle(handle, &idx, &gen);
  if (rc != RMR_UK_OK) return rc;
  RmR_UnifiedArenaSlot *s = &kernel->slots[idx];
  if (!s->in_use || s->generation != gen) return RMR_UK_ERR_HANDLE;
  memset(s, 0, sizeof(*s));
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_ArenaCopy(RmR_UnifiedKernel *kernel, uint32_t src_handle, uint32_t src_offset, uint32_t dst_handle, uint32_t dst_offset, uint32_t len) {
  uint8_t *src = NULL, *dst = NULL;
  int rc = rmr_arena_ptr(kernel, src_handle, src_offset, len, &src);
  if (rc != RMR_UK_OK) return rc;
  rc = rmr_arena_ptr(kernel, dst_handle, dst_offset, len, &dst);
  if (rc != RMR_UK_OK) return rc;
  memmove(dst, src, len);
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_ArenaFill(RmR_UnifiedKernel *kernel, uint32_t handle, uint32_t offset, uint32_t len, uint8_t value) {
  uint8_t *dst = NULL;
  int rc = rmr_arena_ptr(kernel, handle, offset, len, &dst);
  if (rc != RMR_UK_OK) return rc;
  memset(dst, value, len);
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_ArenaWrite(RmR_UnifiedKernel *kernel, uint32_t handle, uint32_t offset, const uint8_t *src, uint32_t len) {
  uint8_t *dst = NULL;
  if (!src) return RMR_UK_ERR_ARG;
  int rc = rmr_arena_ptr(kernel, handle, offset, len, &dst);
  if (rc != RMR_UK_OK) return rc;
  memcpy(dst, src, len);
  return RMR_UK_OK;
}

int RmR_UnifiedKernel_ArenaXorChecksum(RmR_UnifiedKernel *kernel, uint32_t handle, uint32_t offset, uint32_t len, uint32_t *out) {
  uint8_t *src = NULL;
  if (!out) return RMR_UK_ERR_ARG;
  int rc = rmr_arena_ptr(kernel, handle, offset, len, &src);
  if (rc != RMR_UK_OK) return rc;
  uint32_t x = 0u;
  for (uint32_t i = 0; i < len; ++i) x ^= src[i];
  *out = x;
  return RMR_UK_OK;
}

uint32_t RmR_UnifiedKernel_Popcount32(uint32_t value) {
  value = value - ((value >> 1u) & 0x55555555u);
  value = (value & 0x33333333u) + ((value >> 2u) & 0x33333333u);
  value = (value + (value >> 4u)) & 0x0F0F0F0Fu;
  value = value + (value >> 8u);
  value = value + (value >> 16u);
  return value & 0x3Fu;
}

uint32_t RmR_UnifiedKernel_ByteSwap32(uint32_t value) {
  return (value >> 24u) | ((value >> 8u) & 0x0000FF00u) | ((value << 8u) & 0x00FF0000u) | (value << 24u);
}

uint32_t RmR_UnifiedKernel_Rotl32(uint32_t value, uint32_t distance) {
  uint32_t d = distance & 31u;
  return (value << d) | (value >> ((32u - d) & 31u));
}

uint32_t RmR_UnifiedKernel_Rotr32(uint32_t value, uint32_t distance) {
  uint32_t d = distance & 31u;
  return (value >> d) | (value << ((32u - d) & 31u));
}
