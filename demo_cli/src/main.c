#include "rmr_apk_module.h"
#include "rmr_bench.h"
#include "rmr_hw_detect.h"
#include "rmr_isorf.h"
#include "rmr_math_fabric.h"
#include "rmr_policy_kernel.h"
#include "rmr_qemu_bridge.h"

#include <stdio.h>
#include <string.h>

static int run_policy_stack_demo(void) {
  static const unsigned char sample[] =
      "RAFAELIA full stack deterministic pipeline sample payload";
  const char *input_path = "build/demo/fullstack_input.bin";
  const char *output_path = "build/demo/fullstack_output.bin";
  const char *audit_path = "build/demo/fullstack_audit.log";

  FILE *in = fopen(input_path, "wb");
  if (in == NULL) {
    return -1;
  }
  if (fwrite(sample, 1u, sizeof(sample) - 1u, in) != sizeof(sample) - 1u) {
    fclose(in);
    return -2;
  }
  fclose(in);

  RmR_PipelineConfig cfg;
  RmR_AuditSummary summary;
  memset(&cfg, 0, sizeof(cfg));
  memset(&summary, 0, sizeof(summary));

  cfg.chunk_size = 16u;
  cfg.mutation_xor = 0x5Au;
  cfg.mutation_stride = 7u;
  cfg.triad.cpu_ok = 1u;
  cfg.triad.ram_ok = 1u;
  cfg.triad.disk_ok = 1u;

  if (RmR_RunPolicyPipeline(input_path, output_path, audit_path, &cfg, &summary) != 0) {
    return -3;
  }

  printf("policy chunks planned=%u applied=%u verified=%u fail=%u\n",
         summary.chunks_planned,
         summary.chunks_applied,
         summary.chunks_verified,
         summary.verify_failures);
  return 0;
}

int main(void) {
  RmR_HW_Info hw;
  RmR_Bench_Result bench;
  RmR_MathFabricPlan math_plan;
  u32 math_points[RMR_MATH_POINTS];
  u32 math_domains[RMR_MATH_DOMAINS];

  enum { PAGE_COUNT = 16, DATA_WORDS = 16 * 64 };
  RmR_ISOraf_Page pages[PAGE_COUNT];
  u64 data_words[DATA_WORDS];
  RmR_ISOraf_Store store;
  RmR_ISOraf_Manifest mf;
  u64 matrix_map[PAGE_COUNT];

  RmR_ApkProfile apk_profile;
  RmR_ApkStableIdentity apk_stable;
  RmR_QemuPlan qemu_plan;
  char apk_plan[512];
  char qemu_args[512];

  RmR_HW_Detect(&hw);
  RmR_Bench_Run(8u, 8u, &bench);
  RmR_MathFabric_AutodetectPlan(&hw, &math_plan);

  RmR_ISOraf_Init(&store, pages, PAGE_COUNT, data_words, DATA_WORDS, 4096u);
  RmR_ISOraf_SetBit(&store, 17u, 1u);
  RmR_ISOraf_SetBit(&store, 4098u, 1u);
  RmR_ISOraf_SetBit(&store, 12299u, 1u);
  RmR_ISOraf_ExportManifest(&store, &mf);
  u32 map_n = RmR_ISOraf_ExportMatrixMap(&store, matrix_map, PAGE_COUNT);

  for (u32 i = 0; i < RMR_MATH_POINTS; ++i) {
    math_points[i] = (u32)(mf.identity >> ((i & 7u) * 8u)) ^ (i * 0x9E3779B9u);
  }
  RmR_MathFabric_VectorMix(&math_plan, math_points, math_domains);

  RmR_ApkModule_InitProfile(&apk_profile);
  apk_profile.abi_mask = RMR_APK_ABI_UNIVERSAL;
  apk_profile.termux_mode = 0u;
  RmR_ApkModule_AutotuneProfile(&apk_profile);
  RmR_ApkModule_FillStableIdentity(&apk_profile, 34u, 26u, 34u, 0u, 0u, &apk_stable);
  if (RmR_ApkModule_BuildPlan(&apk_profile,
                              "keys/release.jks",
                              "store-pass",
                              "release",
                              "key-pass",
                              apk_plan,
                              (u32)sizeof(apk_plan)) == 0u) {
    strcpy(apk_plan, "<build plan unavailable>");
  }

  RmR_QemuPlan_Autotune(&hw, RMR_GUEST_ARCH_ARM64, 4096u, 0u, &qemu_plan);
  if (RmR_QemuPlan_BuildArgs(&qemu_plan, qemu_args, sizeof(qemu_args)) != 0) {
    strcpy(qemu_args, "<qemu args unavailable>");
  }

  printf("RAFAELIA demo_cli full stack\n");
  printf("arch=%u ptr=%u endian=%u cycle=%u\n", hw.arch, hw.ptr_bits, hw.is_little_endian,
         hw.has_cycle_counter);
  printf("bench alu=%u mem=%u branch=%u matrix=%u\n", bench.alu, bench.mem, bench.branch,
         bench.matrix);
  printf("math_fabric seed=%u lanes=%u stride=%u d0=%u d7=%u\n", math_plan.matrix_seed,
         math_plan.lane_count, math_plan.pin_stride, math_domains[0], math_domains[7]);
  printf("isorf identity=%llu pages_used=%u logical_bits=%llu physical_bits=%llu rebuild=%u map=%u\n",
         (unsigned long long)mf.identity, mf.pages_used, (unsigned long long)mf.logical_bits,
         (unsigned long long)mf.physical_bits, (unsigned)RmR_ISOraf_RebuildCheck(&store, &mf),
         map_n);
  printf("apk stable_fp=0x%016llX abi=0x%X host_abi=0x%X\n",
         (unsigned long long)RmR_ApkModule_StableFingerprint(&apk_stable, 0xCAFEBABEULL),
         apk_stable.abi_mask,
         apk_stable.host_abi_mask);
  printf("apk plan=%s\n", apk_plan);
  printf("qemu preset=%u vm_cpus=%u vm_mem=%u args=%s\n", (unsigned int)qemu_plan.preset,
         (unsigned int)qemu_plan.vm_cpus, (unsigned int)qemu_plan.vm_mem_mib, qemu_args);

  if (run_policy_stack_demo() != 0) {
    fprintf(stderr, "policy stack demo failed\n");
    return 1;
  }

  return 0;
}
