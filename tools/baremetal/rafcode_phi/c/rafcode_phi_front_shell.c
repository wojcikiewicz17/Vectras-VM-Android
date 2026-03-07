#include "rafcode_phi_abi.h"

static raf_u32 rafphi_ascii_eq(const char *a, raf_u32 a_len, const char *b) {
  raf_u32 i = 0u;
  while (i < a_len && b[i] != '\0') {
    if (a[i] != b[i]) {
      return 0u;
    }
    i++;
  }
  return (i == a_len && b[i] == '\0') ? 1u : 0u;
}

rafphi_arch_t rafphi_detect_native_arch(void) {
#if defined(__aarch64__)
  return RAFPHI_ARCH_AARCH64;
#elif defined(__x86_64__)
  return RAFPHI_ARCH_X86_64;
#elif defined(__riscv) && (__riscv_xlen == 64)
  return RAFPHI_ARCH_RISCV64;
#else
  return RAFPHI_ARCH_UNKNOWN;
#endif
}

static raf_u32 rafphi_encode_aarch64(const char *token, raf_u32 token_len, raf_u32 *out_hex) {
  if (rafphi_ascii_eq(token, token_len, "NOP")) {
    *out_hex = 0xD503201Fu;
    return 1u;
  }
  if (rafphi_ascii_eq(token, token_len, "RET")) {
    *out_hex = 0xD65F03C0u;
    return 1u;
  }
  if (rafphi_ascii_eq(token, token_len, "BRK")) {
    *out_hex = 0xD4200000u;
    return 1u;
  }
  if (rafphi_ascii_eq(token, token_len, "HLT")) {
    *out_hex = 0xD4400000u;
    return 1u;
  }
  return 0u;
}

static raf_u32 rafphi_encode_x86_64(const char *token, raf_u32 token_len, raf_u32 *out_hex) {
  if (rafphi_ascii_eq(token, token_len, "NOP")) {
    *out_hex = 0x00000090u;
    return 1u;
  }
  if (rafphi_ascii_eq(token, token_len, "RET")) {
    *out_hex = 0x000000C3u;
    return 1u;
  }
  if (rafphi_ascii_eq(token, token_len, "BRK")) {
    *out_hex = 0x000000CCu;
    return 1u;
  }
  if (rafphi_ascii_eq(token, token_len, "HLT")) {
    *out_hex = 0x000000F4u;
    return 1u;
  }
  return 0u;
}

static raf_u32 rafphi_encode_riscv64(const char *token, raf_u32 token_len, raf_u32 *out_hex) {
  if (rafphi_ascii_eq(token, token_len, "NOP")) {
    *out_hex = 0x00000013u;
    return 1u;
  }
  if (rafphi_ascii_eq(token, token_len, "RET")) {
    *out_hex = 0x00008067u;
    return 1u;
  }
  if (rafphi_ascii_eq(token, token_len, "BRK")) {
    *out_hex = 0x00100073u;
    return 1u;
  }
  if (rafphi_ascii_eq(token, token_len, "HLT")) {
    *out_hex = 0x10500073u;
    return 1u;
  }
  return 0u;
}

raf_u32 rafphi_encode_token_to_hex_arch(const char *token, raf_u32 token_len, rafphi_arch_t arch, raf_u32 *out_hex) {
  if (!token || token_len == 0u || !out_hex) {
    return 0u;
  }

  switch (arch) {
    case RAFPHI_ARCH_AARCH64:
      return rafphi_encode_aarch64(token, token_len, out_hex);
    case RAFPHI_ARCH_X86_64:
      return rafphi_encode_x86_64(token, token_len, out_hex);
    case RAFPHI_ARCH_RISCV64:
      return rafphi_encode_riscv64(token, token_len, out_hex);
    default:
      return 0u;
  }
}

raf_u32 rafphi_encode_token_to_hex(const char *token, raf_u32 token_len, raf_u32 *out_hex) {
  return rafphi_encode_token_to_hex_arch(token, token_len, rafphi_detect_native_arch(), out_hex);
}

static raf_u32 rafphi_crc32c_u32(raf_u32 crc, raf_u32 v) {
  raf_u32 x = crc ^ v;
  raf_u32 i;
  for (i = 0u; i < 32u; i++) {
    raf_u32 lsb = x & 1u;
    x >>= 1u;
    if (lsb) {
      x ^= 0x82F63B78u;
    }
  }
  return x;
}

raf_u32 rafphi_crc32c_words(const raf_u32 *words, raf_u32 word_count) {
  if (!words || word_count == 0u) {
    return 0u;
  }

  raf_u32 crc = 0u;
  raf_u32 i;
  for (i = 0u; i < word_count; i++) {
    crc = rafphi_crc32c_u32(crc, words[i]);
  }
  return crc;
}

rafphi_emit_stats_t rafphi_emit_block_hex_arch(const char **tokens, const raf_u32 *token_lens, raf_u32 token_count,
                                               rafphi_arch_t arch, raf_u32 *out_words, raf_u32 cap_words) {
  rafphi_emit_stats_t stats;
  stats.accepted = 0u;
  stats.rejected = 0u;
  stats.crc32c = 0u;

  if (!tokens || !token_lens || !out_words || cap_words == 0u) {
    return stats;
  }

  raf_u32 write_index = 0u;
  raf_u32 i;
  for (i = 0u; i < token_count; i++) {
    raf_u32 hex = 0u;
    if (!rafphi_encode_token_to_hex_arch(tokens[i], token_lens[i], arch, &hex)) {
      stats.rejected++;
      continue;
    }

    raf_u32 written = rafphi_emit_word_asm(hex, out_words, cap_words, write_index);
    if (written == 0u) {
      stats.rejected++;
      continue;
    }

    write_index += written;
    stats.accepted++;
  }

  stats.crc32c = rafphi_crc32c_words(out_words, stats.accepted);
  return stats;
}

rafphi_emit_stats_t rafphi_emit_block_hex(const char **tokens, const raf_u32 *token_lens, raf_u32 token_count,
                                          raf_u32 *out_words, raf_u32 cap_words) {
  return rafphi_emit_block_hex_arch(tokens, token_lens, token_count, rafphi_detect_native_arch(), out_words, cap_words);
}
