#ifndef RAFCODE_PHI_ABI_H
#define RAFCODE_PHI_ABI_H

/*
 * RAFCODE❤️PHI ABI
 * C (casca) -> ASM (núcleo) com saída em palavras hex determinísticas.
 */

typedef unsigned char      raf_u8;
typedef unsigned short     raf_u16;
typedef unsigned int       raf_u32;
typedef unsigned long long raf_u64;

typedef enum {
  RAFPHI_ARCH_UNKNOWN = 0,
  RAFPHI_ARCH_AARCH64 = 1,
  RAFPHI_ARCH_X86_64  = 2,
  RAFPHI_ARCH_RISCV64 = 3
} rafphi_arch_t;

typedef struct {
  raf_u32 accepted;
  raf_u32 rejected;
  raf_u32 crc32c;
} rafphi_emit_stats_t;

typedef struct {
  raf_u32 magic;
  raf_u32 version;
  raf_u32 arch;
  raf_u32 word_count;
  raf_u32 crc32c;
  raf_u32 flags;
} rafphi_bin_header_t;

#define RAFPHI_BIN_MAGIC   0x52414650u /* 'RAFP' */
#define RAFPHI_BIN_VERSION 0x00010000u

/* Hook ASM/C backend: serializa uma palavra hex no buffer de saída. */
extern raf_u32 rafphi_emit_word_asm(raf_u32 opcode_hex, raf_u32 *out_words, raf_u32 cap_words, raf_u32 write_index);

/* Detector de arquitetura alvo (compile-time/runtime simples). */
rafphi_arch_t rafphi_detect_native_arch(void);

/* Parser mínimo de mnemônicos autorais para hex por arquitetura. */
raf_u32 rafphi_encode_token_to_hex_arch(const char *token, raf_u32 token_len, rafphi_arch_t arch, raf_u32 *out_hex);

/* Wrapper para arquitetura nativa detectada. */
raf_u32 rafphi_encode_token_to_hex(const char *token, raf_u32 token_len, raf_u32 *out_hex);

/* Emite bloco em formato hex determinístico por arquitetura. */
rafphi_emit_stats_t rafphi_emit_block_hex_arch(const char **tokens, const raf_u32 *token_lens, raf_u32 token_count,
                                               rafphi_arch_t arch, raf_u32 *out_words, raf_u32 cap_words);

/* Wrapper para arquitetura nativa detectada. */
rafphi_emit_stats_t rafphi_emit_block_hex(const char **tokens, const raf_u32 *token_lens, raf_u32 token_count,
                                          raf_u32 *out_words, raf_u32 cap_words);

/* CRC32C público para regressão e layout fixo. */
raf_u32 rafphi_crc32c_words(const raf_u32 *words, raf_u32 word_count);

#endif
