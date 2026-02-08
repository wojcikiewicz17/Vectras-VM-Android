#include "bitraf.h"
#include <stdint.h>
#include <stdio.h>

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
      0x5Fu,0x45u,0x4Eu,0x47u,0x49u,0x4Eu,0x45u,0x00u
  };
  uint8_t frame[128];
  uint8_t out[128];
  uint64_t seed = 0x123456789ABCDEF0ULL;

  bitraf_init(seed);
  size_t frame_len = bitraf_compress(payload, sizeof(payload), frame, sizeof(frame), seed);
  size_t plain_len = bitraf_reconstruct(frame, frame_len, out, sizeof(out), seed);
  uint64_t h = bitraf_hash(out, plain_len, seed);

  int ok = (frame_len > 0u) & (plain_len == sizeof(payload))
      & equal_buf(payload, out, sizeof(payload))
      & bitraf_verify(out, plain_len, h, seed);

  printf("bitraf_selftest frame=%zu plain=%zu ok=%d\n", frame_len, plain_len, ok);
  return ok ? 0 : 1;
}
