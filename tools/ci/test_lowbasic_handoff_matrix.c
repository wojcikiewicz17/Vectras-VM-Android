#include <stdint.h>
#include <stdio.h>
#include "../baremetal/rafcode_phi/include/rafcode_phi_lowbasic.h"

static int run_case(const char* name, rafphi_boot_handoff_t h, raf_u32 must_set, raf_u32 must_not_set) {
    raf_u32 status = rafphi_boot_handoff_validate(&h);
    if ((status & must_set) != must_set) {
        fprintf(stderr, "FAIL %s status=0x%08x missing=0x%08x\n", name, status, must_set);
        return 1;
    }
    if ((status & must_not_set) != 0u) {
        fprintf(stderr, "FAIL %s status=0x%08x unexpected=0x%08x\n", name, status, must_not_set);
        return 1;
    }
    printf("PASS %s status=0x%08x\n", name, status);
    return 0;
}

int main(void) {
    int fail = 0;
    rafphi_boot_handoff_t base = {0};
    base.magic = RAFPHI_BOOT_MAGIC;
    base.version = RAFPHI_BOOT_VERSION;
    base.in_ptr = 0x1000;
    base.out_ptr = 0x2000;
    base.words = 64;

    rafphi_boot_handoff_t arm64 = base;
    arm64.arch = RAFPHI_ARCH_AARCH64;
    fail |= run_case("arm64-valid", arm64, RAFPHI_F_BOOT_OK, RAFPHI_F_BOOT_DENY | RAFPHI_F_BOOT_PTR_INVALID);

    rafphi_boot_handoff_t arm32 = base;
    arm32.arch = RAFPHI_ARCH_ARMV7;
    fail |= run_case("armv7-valid", arm32, RAFPHI_F_BOOT_OK, RAFPHI_F_BOOT_DENY | RAFPHI_F_BOOT_PTR_INVALID);

    rafphi_boot_handoff_t invalid_arch = base;
    invalid_arch.arch = RAFPHI_ARCH_UNKNOWN;
    fail |= run_case("invalid-arch", invalid_arch, RAFPHI_F_BOOT_DENY | RAFPHI_F_BOOT_ABI_MISM, RAFPHI_F_BOOT_OK);

    rafphi_boot_handoff_t invalid_ptr = arm64;
    invalid_ptr.in_ptr = 0;
    fail |= run_case("invalid-ptr", invalid_ptr, RAFPHI_F_BOOT_DENY | RAFPHI_F_BOOT_PTR_INVALID, RAFPHI_F_BOOT_OK);

    rafphi_boot_handoff_t invalid_version = arm64;
    invalid_version.version = 0;
    fail |= run_case("invalid-version", invalid_version, RAFPHI_F_BOOT_DENY, RAFPHI_F_BOOT_OK);

    return fail ? 1 : 0;
}
