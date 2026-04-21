#include "rmr_policy_kernel.h"

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int file_equal(const char *a, const char *b) {
  FILE *fa = fopen(a, "rb");
  FILE *fb = fopen(b, "rb");
  if (!fa || !fb) return 0;
  int eq = 1;
  while (1) {
    unsigned char ba[4096], bb[4096];
    size_t ra = fread(ba, 1, sizeof(ba), fa);
    size_t rb = fread(bb, 1, sizeof(bb), fb);
    if (ra != rb || memcmp(ba, bb, ra) != 0) { eq = 0; break; }
    if (ra == 0) break;
  }
  fclose(fa);
  fclose(fb);
  return eq;
}

static int write_fixture(const char *path) {
  FILE *f = fopen(path, "wb");
  if (!f) return -1;
  for (uint32_t i = 0; i < 32768u; ++i) {
    uint8_t v = (uint8_t)((i * 17u + 29u) & 0xFFu);
    if (fwrite(&v, 1, 1, f) != 1) { fclose(f); return -1; }
  }
  fclose(f);
  return 0;
}

int main(void) {
  const char *fixture_dir = "bench/results";
  const char *policy_in = "bench/results/policy_in.bin";
  const char *policy_out1 = "bench/results/policy_out1.bin";
  const char *policy_out2 = "bench/results/policy_out2.bin";
  const char *policy_out3 = "bench/results/policy_out3.bin";
  const char *policy_log_same = "bench/results/policy_log_same.log";
  const char *policy_log1 = "bench/results/policy_log1.log";
  const char *policy_log2 = "bench/results/policy_log2.log";
  const char *policy_log3 = "bench/results/policy_log3.log";

  if (system("mkdir -p bench/results") != 0) {
    printf("FAIL create fixture dir %s\n", fixture_dir);
    return 1;
  }

  const uint8_t vec[] = {'1','2','3','4','5','6','7','8','9'};
  uint32_t crc = RmR_CRC32C(vec, sizeof(vec));
  if (crc != 0xE3069283u) {
    printf("FAIL golden crc32c got=%08x exp=e3069283\n", crc);
    return 1;
  }

  if (write_fixture(policy_in) != 0) {
    printf("FAIL fixture write\n");
    return 1;
  }

  RmR_PipelineConfig cfg;
  cfg.chunk_size = 4096u;
  cfg.mutation_xor = 0xA5u;
  cfg.mutation_stride = 31u;
  cfg.triad.cpu_ok = 1;
  cfg.triad.ram_ok = 1;
  cfg.triad.disk_ok = 1;

  RmR_AuditSummary s1, s2;
  RmR_AuditSummary s_same;
  int rc_same = RmR_RunPolicyPipeline(policy_in, policy_in, policy_log_same, &cfg, &s_same);
  if (rc_same != -7) {
    printf("FAIL same input/output path should be rejected rc=%d\n", rc_same);
    return 1;
  }

  int rc1 = RmR_RunPolicyPipeline(policy_in, policy_out1, policy_log1, &cfg, &s1);
  int rc2 = RmR_RunPolicyPipeline(policy_in, policy_out2, policy_log2, &cfg, &s2);
  if (rc1 != 0 || rc2 != 0) {
    printf("FAIL pipeline run rc1=%d rc2=%d\n", rc1, rc2);
    return 1;
  }
  if (!file_equal(policy_log1, policy_log2)) {
    printf("FAIL determinism log mismatch\n");
    return 1;
  }

  FILE *flip = fopen(policy_out2, "r+b");
  if (!flip) {
    printf("FAIL open bitflip target\n");
    return 1;
  }
  if (fseek(flip, 17, SEEK_SET) != 0) { fclose(flip); return 1; }
  int c = fgetc(flip);
  if (c == EOF) { fclose(flip); return 1; }
  if (fseek(flip, 17, SEEK_SET) != 0) { fclose(flip); return 1; }
  fputc((c ^ 0x01), flip);
  fclose(flip);

  rc2 = RmR_RunPolicyPipeline(policy_out2, policy_out3, policy_log3, &cfg, &s2);
  if (rc2 != 0) {
    printf("FAIL rerun over corrupted stream rc=%d\n", rc2);
    return 1;
  }
  if (file_equal(policy_log1, policy_log3)) {
    printf("FAIL corruption resistance expected different logs after bitflip\n");
    return 1;
  }

  printf("OK policy kernel selftest\n");
  return 0;
}
