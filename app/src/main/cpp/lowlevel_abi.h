#ifndef VECTRA_LOWLEVEL_ABI_H
#define VECTRA_LOWLEVEL_ABI_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define VECTRA_LOWLEVEL_ABI_SCHEMA_VERSION 0x00010000u

#if defined(__GNUC__)
#define VECTRA_LL_ABI_EXPORT __attribute__((visibility("default")))
#else
#define VECTRA_LL_ABI_EXPORT
#endif

#define VECTRA_LOWLEVEL_ABI_CONTRACT_VERSION "1.0.0"

typedef enum vectra_lowlevel_arch {
    VECTRA_LL_ARCH_ARM64_V8A = 0,
    VECTRA_LL_ARCH_ARMEABI_V7A = 1,
    VECTRA_LL_ARCH_X86_64 = 2,
    VECTRA_LL_ARCH_X86 = 3,
    VECTRA_LL_ARCH_RISCV64 = 4,
    VECTRA_LL_ARCH_RISCV32 = 5,
    VECTRA_LL_ARCH_MIPS64 = 6,
    VECTRA_LL_ARCH_COUNT = 7
} vectra_lowlevel_arch_t;

typedef enum vectra_lowlevel_error {
    VECTRA_LL_ERR_OK = 0,
    VECTRA_LL_ERR_UNSUPPORTED_ARCH = -1000,
    VECTRA_LL_ERR_UNSUPPORTED_ABI_VERSION = -1001,
    VECTRA_LL_ERR_STACK_MISALIGNED = -1002,
    VECTRA_LL_ERR_BAD_FRAME_POLICY = -1003,
    VECTRA_LL_ERR_CALL_CONVENTION = -1004,
    VECTRA_LL_ERR_FORBIDDEN_SYSCALL = -1005,
    VECTRA_LL_ERR_BOUNDARY_VIOLATION = -1006,
    VECTRA_LL_ERR_NULL_PTR = -1007
} vectra_lowlevel_error_t;

typedef enum vectra_lowlevel_frame_policy {
    VECTRA_LL_FRAME_REQUIRED = 0,
    VECTRA_LL_FRAME_LEAF_OPTIONAL = 1,
    VECTRA_LL_FRAME_FORBIDDEN = 2
} vectra_lowlevel_frame_policy_t;

typedef enum vectra_lowlevel_syscall_policy {
    VECTRA_LL_SYSCALL_NONE = 0,
    VECTRA_LL_SYSCALL_ALLOWLISTED_GATE = 1,
    VECTRA_LL_SYSCALL_HOST_ADAPTER_ONLY = 2
} vectra_lowlevel_syscall_policy_t;

typedef struct vectra_lowlevel_calling_convention {
    const char* input_registers;
    const char* return_registers;
    const char* caller_saved;
    const char* callee_saved;
} vectra_lowlevel_calling_convention_t;

typedef struct vectra_lowlevel_abi_descriptor {
    vectra_lowlevel_arch_t arch;
    const char* arch_name;
    uint16_t abi_version_major;
    uint16_t abi_version_minor;
    uint8_t stack_alignment_bytes;
    vectra_lowlevel_frame_policy_t frame_policy;
    const char* prologue_rules;
    const char* epilogue_rules;
    vectra_lowlevel_syscall_policy_t syscall_policy;
    vectra_lowlevel_calling_convention_t cc;
} vectra_lowlevel_abi_descriptor_t;

typedef struct vectra_lowlevel_interop_rule {
    uint16_t producer_major;
    uint16_t producer_minor_min;
    uint16_t producer_minor_max;
    uint16_t consumer_major;
    uint16_t consumer_minor_min;
    uint16_t consumer_minor_max;
    uint8_t adaptive_bridge;
    const char* note;
} vectra_lowlevel_interop_rule_t;

const vectra_lowlevel_abi_descriptor_t* vectra_lowlevel_abi_descriptors(uint32_t* count);
const vectra_lowlevel_interop_rule_t* vectra_lowlevel_abi_interop_rules(uint32_t* count);
int vectra_lowlevel_validate_interop(uint16_t producer_major,
                                     uint16_t producer_minor,
                                     uint16_t consumer_major,
                                     uint16_t consumer_minor,
                                     uint8_t* adaptive_bridge_enabled);

VECTRA_LL_ABI_EXPORT const char* abi_entry_get_contract_version(void);
VECTRA_LL_ABI_EXPORT const vectra_lowlevel_abi_descriptor_t* abi_entry_get_arch_descriptors(uint32_t* count);
VECTRA_LL_ABI_EXPORT const vectra_lowlevel_interop_rule_t* abi_entry_get_interop_rules(uint32_t* count);
VECTRA_LL_ABI_EXPORT int abi_entry_validate_interop(uint16_t producer_major,
                                                    uint16_t producer_minor,
                                                    uint16_t consumer_major,
                                                    uint16_t consumer_minor,
                                                    uint8_t* adaptive_bridge_enabled);

#ifdef __cplusplus
}
#endif

#endif
