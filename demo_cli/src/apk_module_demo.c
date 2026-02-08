#include <stdio.h>
#include "rmr_apk_module.h"

static u32 parse_u32(const char *s){
  u32 v = 0u;
  u32 i = 0u;
  if(!s) return 0u;
  while(s[i] != '\0'){
    if(s[i] < '0' || s[i] > '9') return 0u;
    v = (v * 10u) + (u32)(s[i] - '0');
    i++;
  }
  return v;
}

int main(int argc, char **argv){
  RmR_ApkProfile profile;
  RmR_ApkStableIdentity stable;
  char plan[4096];
  u64 fp_stable;

  if(argc != 13){
    printf("uso: %s <keystore> <store_pass> <alias> <key_pass> <termux_prefix> <home> <shell> <compile_sdk> <ndk_major> <build_tools_major> <build_tools_minor> <build_tools_patch>\n", argv[0]);
    return 1;
  }

  RmR_ApkModule_InitProfile(&profile);
  profile.abi_mask = RMR_APK_ABI_UNIVERSAL;
  profile.termux_mode = RmR_ApkModule_DetectTermuxLike(argv[5], argv[6], argv[7]);
  RmR_ApkModule_AutotuneProfile(&profile);

  RmR_ApkModule_FillStableIdentity(&profile,
                                   parse_u32(argv[8]),
                                   parse_u32(argv[9]),
                                   parse_u32(argv[10]),
                                   parse_u32(argv[11]),
                                   parse_u32(argv[12]),
                                   &stable);

  if(RmR_ApkModule_BuildPlan(&profile, argv[1], argv[2], argv[3], argv[4], plan, (u32)sizeof(plan)) == 0u){
    printf("falha ao gerar plano determinístico de compilação/assinatura.\n");
    return 2;
  }

  fp_stable = RmR_ApkModule_StableFingerprint(&stable, 0xCAFEBABEULL);

  printf("termux_mode=%u\n", profile.termux_mode);
  printf("host_abi_mask=0x%08X\n", profile.host_abi_mask);
  printf("hw_cacheline=0x%08X\n", profile.hw_cacheline_bytes);
  printf("hw_page=0x%08X\n", profile.hw_page_bytes);
  printf("build_plan=%s\n", plan);
  printf("stable_fp=0x%016llX\n", (unsigned long long)fp_stable);

  return 0;
}
