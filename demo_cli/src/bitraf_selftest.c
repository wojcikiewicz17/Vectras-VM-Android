#include "bitraf.h"
#include "rmr_isorf.h"
#include <stdint.h>
#include <stdio.h>
#include <string.h>

static int equal_buf(const uint8_t *a, const uint8_t *b, size_t n) {
  uint8_t diff = 0u;
  for (size_t i = 0; i < n; ++i) {
    diff |= (uint8_t)(a[i] ^ b[i]);
  }
  return diff == 0u ? 1 : 0;
}

int main(void) {
  static const uint8_t payload[] = {
      0x52u,0x41u,0x46u,0x41u,0x45u,0x4Cu,0x49u,0x41u,
      0x5Fu,0x45u,0x4Eu,0x47u,0x49u,0x4Eu,0x45u,0x00u,
      0x42u,0x49u,0x54u,0x52u,0x41u,0x46u,0x5Fu,0x56u,
      0x32u,0x5Fu,0x43u,0x48u,0x55u,0x4Eu,0x4Bu,0x53u
  };
  uint8_t frame[256];
  uint8_t frame_flip[256];
  uint8_t out[256];
  uint64_t seed = 0x123456789ABCDEF0ULL;

  enum { PAGE_COUNT = 8, DATA_WORDS = 8 * 64 };
  RmR_ISOraf_Page pages[PAGE_COUNT];
  u64 words[DATA_WORDS];
  RmR_ISOraf_Store st;
  RmR_ISOraf_Manifest mf;

  bitraf_init(seed);
  size_t frame_len = bitraf_compress(payload, sizeof(payload), frame, sizeof(frame), seed);
  size_t plain_len = bitraf_reconstruct(frame, frame_len, out, sizeof(out), seed);
  uint64_t h = bitraf_hash(out, plain_len, seed);

  bitraf_diag d0;
  size_t plain_len_ex = bitraf_reconstruct_ex(frame, frame_len, out, sizeof(out), seed,
                                              BITRAF_RECON_MODE_STRICT, &d0);

  memcpy(frame_flip, frame, frame_len);
  frame_flip[24u + 8u + 5u] ^= 0x01u;

  bitraf_diag d1;
  size_t strict_len = bitraf_reconstruct_ex(frame_flip, frame_len, out, sizeof(out), seed,
                                            BITRAF_RECON_MODE_STRICT, &d1);

  bitraf_diag d2;
  size_t report_len = bitraf_reconstruct_ex(frame_flip, frame_len, out, sizeof(out), seed,
                                            BITRAF_RECON_MODE_REPORT, &d2);

  RmR_ISOraf_Init(&st, pages, PAGE_COUNT, words, DATA_WORDS, 2048u);
  RmR_ISOraf_SetBit(&st, 63u, 1u);
  RmR_ISOraf_SetBit(&st, 4097u, 1u);
  RmR_ISOraf_ExportManifest(&st, &mf);

  int ok = (frame_len > 0u) & (plain_len == sizeof(payload))
      & (plain_len_ex == sizeof(payload))
      & equal_buf(payload, out, sizeof(payload))
      & bitraf_verify(out, plain_len, h, seed)
      & (strict_len == 0u)
      & (d1.status == BITRAF_RECON_STATUS_CHUNK)
      & (d1.error_offset == 0u)
      & (d1.chunk_index == 0u)
      & (report_len == sizeof(payload))
      & (d2.status == BITRAF_RECON_STATUS_CHUNK)
      & (d2.error_offset == 0u)
      & (d2.chunk_index == 0u)
      & (d2.bad_chunk_count >= 1u)
      & RmR_ISOraf_RebuildCheck(&st, &mf)
      & (RmR_ISOraf_GetBit(&st, 63u) == 1u)
      & (RmR_ISOraf_GetBit(&st, 4097u) == 1u);

  printf("bitraf_selftest frame=%zu plain=%zu strict=%zu report=%zu off=%zu ok=%d\n",
         frame_len, plain_len, strict_len, report_len, d2.error_offset, ok);
  return ok ? 0 : 1;
}
