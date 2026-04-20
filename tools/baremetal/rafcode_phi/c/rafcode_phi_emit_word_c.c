#include "rafcode_phi_abi.h"

/* Fallback portátil quando backend ASM não está disponível no host. */
raf_u32 rafphi_emit_word_abi(raf_u32 opcode_hex, raf_u32 *out_words, raf_u32 cap_words, raf_u32 write_index) {
  if (!out_words) {
    return 0u;
  }
  if (write_index >= cap_words) {
    return 0u;
  }
  out_words[write_index] = opcode_hex;
  return 1u;
}

raf_u32 rafphi_emit_word_asm(raf_u32 opcode_hex, raf_u32 *out_words, raf_u32 cap_words, raf_u32 write_index) {
  return rafphi_emit_word_abi(opcode_hex, out_words, cap_words, write_index);
}
