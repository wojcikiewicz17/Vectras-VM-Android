#include "rafcode_phi_abi.h"

static raf_u32 rafphi_popcount32(raf_u32 x) {
  x = x - ((x >> 1u) & 0x55555555u);
  x = (x & 0x33333333u) + ((x >> 2u) & 0x33333333u);
  x = (x + (x >> 4u)) & 0x0F0F0F0Fu;
  return (x * 0x01010101u) >> 24u;
}

static raf_u64 rafphi_fnv1a64_words(const raf_u32 *words, raf_u32 word_count) {
  raf_u64 h = 0xcbf29ce484222325ull;
  raf_u32 i;
  for (i = 0u; i < word_count; ++i) {
    raf_u32 w = words[i];
    raf_u32 b;
    for (b = 0u; b < 4u; ++b) {
      raf_u8 by = (raf_u8)((w >> (b * 8u)) & 0xFFu);
      h ^= (raf_u64)by;
      h *= 0x100000001b3ull;
    }
  }
  return h;
}

rafphi_vecbit_t rafphi_vecbit_verify(const raf_u32 *words, raf_u32 word_count, raf_u32 max_neighbor_distance) {
  rafphi_vecbit_t out;
  out.violations = 0u;
  out.max_neighbor_distance = 0u;
  out.chain_hash_fnv1a64 = 0xcbf29ce484222325ull;
  out.compile_ok = 0u;

  if (!words || word_count == 0u) {
    return out;
  }

  out.chain_hash_fnv1a64 = rafphi_fnv1a64_words(words, word_count);

  if (word_count == 1u) {
    out.compile_ok = 1u;
    return out;
  }

  raf_u32 i;
  for (i = 0u; i + 1u < word_count; ++i) {
    raf_u32 d = rafphi_popcount32(words[i] ^ words[i + 1u]);
    if (d > out.max_neighbor_distance) {
      out.max_neighbor_distance = d;
    }
    if (d > max_neighbor_distance) {
      out.violations += 1u;
    }
  }

  out.compile_ok = (out.violations == 0u) ? 1u : 0u;
  return out;
}
