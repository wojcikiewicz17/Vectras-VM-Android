cmake_minimum_required(VERSION 3.22.1)
project(vectra_core_accel C ASM)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

option(RMR_ENABLE_POLICY_MODULE "Enable RMR policy module" ON)
option(RMR_ENABLE_BITRAF_MODULE "Enable BITRAF module" ON)
option(RMR_ASM_CORE_EXPERIMENTAL "Enable experimental ASM core hot paths" ON)
option(RMR_ASM_CORE_X86_64_VALIDATED "Enable ASM hot paths on x86_64 (validated ABI)" ON)
option(RMR_ASM_CORE_ARM64_VALIDATED "Enable ASM hot paths on arm64-v8a (validated ABI)" ON)
option(RMR_ASM_CORE_RISCV64_VALIDATED "Enable RV64I-compatible CASM path on riscv64 (validated ABI)" ON)
option(VECTRA_REQUIRE_FREESTANDING_CORE "Fail configuration when freestanding ABI core is not active" ON)

set(VECTRA_REPO_ROOT ${CMAKE_CURRENT_LIST_DIR}/../../../../)
set(VECTRA_RMR_INCLUDE_DIR ${VECTRA_REPO_ROOT}/engine/rmr/include)
set(RMR_SOURCE_BASE "${VECTRA_REPO_ROOT}/")
set(VECTRA_PLATFORM_ANDROID_COMPAT_SOURCE ${VECTRA_REPO_ROOT}/engine/platform/android/rmr_android_compat.c)

include(${VECTRA_REPO_ROOT}/engine/rmr/sources_rmr_core.cmake)

rmr_manifest_apply_base(VECTRA_ENGINE_CORE_SOURCES ${RMR_SOURCE_GROUP_CORE} ${RMR_SOURCE_GROUP_ANDROID_ONLY})
list(REMOVE_ITEM VECTRA_ENGINE_CORE_SOURCES ${VECTRA_REPO_ROOT}/engine/rmr/src/rmr_baremetal_compat.c)

rmr_manifest_apply_base(VECTRA_ENGINE_POLICY_SOURCES ${RMR_SOURCE_GROUP_OPTIONAL_POLICY})
rmr_manifest_apply_base(VECTRA_ENGINE_ASM_X86_64_SOURCES ${RMR_SOURCE_GROUP_ASM_X86_64})
rmr_manifest_apply_base(VECTRA_ENGINE_ASM_ARM64_SOURCES ${RMR_SOURCE_GROUP_ASM_ARM64})
rmr_manifest_apply_base(VECTRA_ENGINE_ASM_RISCV64_SOURCES ${RMR_SOURCE_GROUP_ASM_RISCV64})
rmr_manifest_apply_base(VECTRA_ENGINE_ARM64_NEON_SOURCES ${RMR_SOURCE_GROUP_ASM_ARM64_NEON})

set(VECTRA_LOWLEVEL_SOURCES)
set(VECTRA_CASM_SOURCES)
set(VECTRA_NEON_SOURCES)

if(RMR_ASM_CORE_EXPERIMENTAL AND ANDROID AND CMAKE_ANDROID_ARCH_ABI STREQUAL "x86_64" AND RMR_ASM_CORE_X86_64_VALIDATED)
    list(APPEND VECTRA_LOWLEVEL_SOURCES ${VECTRA_ENGINE_ASM_X86_64_SOURCES})
    list(APPEND VECTRA_CASM_SOURCES ${VECTRA_ENGINE_ASM_X86_64_SOURCES})
endif()

if(RMR_ASM_CORE_EXPERIMENTAL AND NOT ANDROID AND (CMAKE_SYSTEM_PROCESSOR MATCHES "x86_64|AMD64") AND RMR_ASM_CORE_X86_64_VALIDATED)
    list(APPEND VECTRA_LOWLEVEL_SOURCES ${VECTRA_ENGINE_ASM_X86_64_SOURCES})
    list(APPEND VECTRA_CASM_SOURCES ${VECTRA_ENGINE_ASM_X86_64_SOURCES})
endif()

if(RMR_ASM_CORE_EXPERIMENTAL AND ANDROID AND CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a" AND RMR_ASM_CORE_ARM64_VALIDATED)
    list(APPEND VECTRA_CASM_SOURCES ${VECTRA_ENGINE_ASM_ARM64_SOURCES})
endif()

if(ANDROID AND (CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a" OR CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a"))
    list(APPEND VECTRA_NEON_SOURCES ${VECTRA_ENGINE_ARM64_NEON_SOURCES})
endif()

# Roadmap explícito: ABI riscv64 permanece INATIVA por política Android
# (APP_ABI_POLICY/SUPPORTED_ABIS). A rotina CASM riscv64 já é RV64I-compatible
# (sem extensão M) e depende apenas de habilitação dessa política ABI.
# if(RMR_ASM_CORE_EXPERIMENTAL AND ANDROID AND CMAKE_ANDROID_ARCH_ABI STREQUAL "riscv64" AND RMR_ASM_CORE_RISCV64_VALIDATED)
#     list(APPEND VECTRA_CASM_SOURCES ${VECTRA_ENGINE_ASM_RISCV64_SOURCES})
# endif()


if(DEFINED Python3_EXECUTABLE AND NOT Python3_EXECUTABLE STREQUAL "")
    execute_process(
        COMMAND ${Python3_EXECUTABLE} --version
        RESULT_VARIABLE VECTRA_PYTHON3_OK
        OUTPUT_QUIET
        ERROR_QUIET
    )
    if(NOT VECTRA_PYTHON3_OK EQUAL 0)
        message(WARNING "Ignoring unusable Python3_EXECUTABLE from environment/cache: ${Python3_EXECUTABLE}")
        unset(Python3_EXECUTABLE CACHE)
        unset(Python3_EXECUTABLE)
    endif()
endif()

if(NOT DEFINED Python3_EXECUTABLE OR Python3_EXECUTABLE STREQUAL "")
    find_program(VECTRA_PYTHON3_FALLBACK NAMES python3 python REQUIRED)
    set(Python3_EXECUTABLE "${VECTRA_PYTHON3_FALLBACK}" CACHE FILEPATH "Python interpreter used by lowlevel ABI checks" FORCE)
endif()

find_package(Python3 COMPONENTS Interpreter REQUIRED)
add_custom_target(lowlevel_abi_contract_check
    COMMAND ${Python3_EXECUTABLE} ${VECTRA_REPO_ROOT}/tools/ci/validate_lowlevel_abi_contract.py
    WORKING_DIRECTORY ${VECTRA_REPO_ROOT}
    COMMENT "Validating lowlevel ABI contract + no implicit libc/stdlib policy")

add_library(abi_core_freestanding STATIC
    lowlevel_abi.c
    vectra_lowlevel_backend_fallback.c
    vectra_lowlevel_backend_arm64.c
    vectra_lowlevel_backend_armv7.c
    vectra_lowlevel_backend_x86_64.c
    vectra_lowlevel_backend_x86.c
    vectra_lowlevel_backend_riscv64.c
    ${VECTRA_LOWLEVEL_SOURCES}
    ${VECTRA_CASM_SOURCES}
    ${VECTRA_NEON_SOURCES})

add_library(vectra_core_accel SHARED
    vectra_core_accel.c
    lowlevel_bridge.c
    hardware_profile_bridge.c
    ${VECTRA_PLATFORM_ANDROID_COMPAT_SOURCE}
    ${VECTRA_ENGINE_CORE_SOURCES})

add_dependencies(vectra_core_accel lowlevel_abi_contract_check)

set(TERMUX_BOOTSTRAP_SUPPORTED_ANDROID_ABIS "arm64-v8a;armeabi-v7a;x86;x86_64" CACHE STRING "ABIs that should build termux-bootstrap")

set(TERMUX_BOOTSTRAP_BUILD_ENABLED OFF)

if(ANDROID)
    list(FIND TERMUX_BOOTSTRAP_SUPPORTED_ANDROID_ABIS "${CMAKE_ANDROID_ARCH_ABI}" TERMUX_BOOTSTRAP_ABI_INDEX)
    if(NOT TERMUX_BOOTSTRAP_ABI_INDEX EQUAL -1)
        set(TERMUX_BOOTSTRAP_BUILD_ENABLED ON)
    else()
        message(STATUS "termux-bootstrap disabled for unsupported ABI: ${CMAKE_ANDROID_ARCH_ABI}")
    endif()
endif()

if(NOT ANDROID OR CMAKE_ANDROID_ARCH_ABI IN_LIST TERMUX_BOOTSTRAP_SUPPORTED_ANDROID_ABIS)
    add_library(termux-bootstrap SHARED
        termux_bootstrap_bridge.c)
else()
    message(STATUS "Skipping termux-bootstrap for unsupported ABI: ${CMAKE_ANDROID_ARCH_ABI}")
endif()

if(VECTRA_CASM_SOURCES)
    target_compile_definitions(vectra_core_accel PRIVATE VECTRA_HAS_CASM_MARKER=1)
else()
    target_compile_definitions(vectra_core_accel PRIVATE VECTRA_HAS_CASM_MARKER=0)
endif()

if(RMR_ENABLE_POLICY_MODULE)
    add_library(rmr_policy_static STATIC
        ${VECTRA_ENGINE_POLICY_SOURCES})
endif()

if(RMR_ENABLE_BITRAF_MODULE)
    add_library(bitraf_static STATIC
        ../../../../engine/rmr/src/bitraf.c)
endif()

if(ANDROID)
    find_library(log-lib log)
else()
    find_package(JNI REQUIRED COMPONENTS JVM)
    set(log-lib "")
endif()

set(VECTRA_COMMON_COMPILE_OPTIONS -O2 -fno-fast-math -fvisibility=hidden -DRMR_JNI_BUILD=1 -DRMR_BUILD_HOST_TOOLING=1)
set(VECTRA_FREESTANDING_COMPILE_OPTIONS
    -O2
    -fno-fast-math
    -fvisibility=hidden
    -ffreestanding
    -fno-exceptions
    -fno-rtti
    -fno-unwind-tables
    -fno-asynchronous-unwind-tables
    -fdata-sections
    -ffunction-sections
    -DRMR_BUILD_HOST_TOOLING=1)
set(VECTRA_ABI_COMPILE_OPTIONS)
set(VECTRA_LTO_MODE "${VECTRA_LTO_MODE}" CACHE STRING "LTO mode from CI perf decisions")
set(VECTRA_INLINE_POLICY "${VECTRA_INLINE_POLICY}" CACHE STRING "Inline policy from CI perf decisions")

# Flag arm64 vinda do Gradle: baseline (-march=armv8-a) e tuning opcional (-mcpu=<valor>).
set(VECTRA_ARM64_CPP_FLAGS "${VECTRA_ARM64_CPP_FLAGS}" CACHE STRING "Extra arm64 C/C++ flags from Gradle")
set(VECTRA_ARM64_CPP_FLAGS_LIST)
if(VECTRA_ARM64_CPP_FLAGS)
    separate_arguments(VECTRA_ARM64_CPP_FLAGS_LIST NATIVE_COMMAND "${VECTRA_ARM64_CPP_FLAGS}")
endif()

# CMake já define -march=armv8-a+crc+simd para arm64-v8a; evita sobrescrever esse targeting.
set(VECTRA_ARM64_TUNING_FLAGS)
foreach(_flag IN LISTS VECTRA_ARM64_CPP_FLAGS_LIST)
    if(NOT _flag MATCHES "^-march=")
        list(APPEND VECTRA_ARM64_TUNING_FLAGS ${_flag})
    endif()
endforeach()

if(ANDROID AND CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    # +crc enables CRC32 intrinsics | +simd enables NEON | +sha2 enables SHA hw
    set(VECTRA_ABI_COMPILE_OPTIONS
        -march=armv8-a+crc+simd
        -DRMR_ARCH_ARM64=1
        -DRMR_FEAT_CRC32_HW=1)
    list(APPEND VECTRA_ABI_COMPILE_OPTIONS ${VECTRA_ARM64_TUNING_FLAGS})
endif()
if(ANDROID AND CMAKE_ANDROID_ARCH_ABI STREQUAL "x86_64")
    set(VECTRA_ABI_COMPILE_OPTIONS -msse4.2 -mpopcnt -DRMR_ARCH_X86_64=1)
endif()

if(ANDROID AND CMAKE_ANDROID_ARCH_ABI STREQUAL "x86")
    set(VECTRA_ABI_COMPILE_OPTIONS -mssse3 -msse4.2 -mpopcnt -DRMR_ARCH_X86=1)
endif()
if(ANDROID AND CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
    # arm32-v7 baseline com NEON explícito para evitar regressões no NDK r27+
    # em toolchains que oscilam entre soft/softfp ao integrar JNI.
    set(VECTRA_ABI_COMPILE_OPTIONS
        -march=armv7-a
        -mthumb
        -mfpu=neon
        -mfloat-abi=softfp
        -DRMR_ARCH_ARM32=1
        -DRMR_FEAT_NEON=1)
endif()

if(VECTRA_INLINE_POLICY STREQUAL "aggressive")
    list(APPEND VECTRA_ABI_COMPILE_OPTIONS -finline-functions)
elseif(VECTRA_INLINE_POLICY STREQUAL "controlled")
    list(APPEND VECTRA_ABI_COMPILE_OPTIONS -fno-inline-functions)
endif()

if(VECTRA_LTO_MODE STREQUAL "thin" OR VECTRA_LTO_MODE STREQUAL "full")
    list(APPEND VECTRA_ABI_COMPILE_OPTIONS -flto)
endif()

if(TARGET rmr_policy_static)
    target_compile_options(rmr_policy_static PRIVATE
        ${VECTRA_COMMON_COMPILE_OPTIONS}
        ${VECTRA_ABI_COMPILE_OPTIONS})
    target_include_directories(rmr_policy_static PRIVATE
        ${VECTRA_RMR_INCLUDE_DIR})
    target_compile_definitions(vectra_core_accel PRIVATE RMR_ENABLE_POLICY_MODULE=1)
else()
    target_compile_definitions(vectra_core_accel PRIVATE RMR_ENABLE_POLICY_MODULE=0)
endif()

if(TARGET rmr_policy_static)
    target_link_libraries(vectra_core_accel PRIVATE rmr_policy_static)
endif()

if(TARGET bitraf_static)
    target_compile_options(bitraf_static PRIVATE
        ${VECTRA_COMMON_COMPILE_OPTIONS}
        ${VECTRA_ABI_COMPILE_OPTIONS})
    target_include_directories(bitraf_static PRIVATE
        ${VECTRA_RMR_INCLUDE_DIR})
    target_compile_definitions(vectra_core_accel PRIVATE RMR_ENABLE_BITRAF_MODULE=1)
else()
    target_compile_definitions(vectra_core_accel PRIVATE RMR_ENABLE_BITRAF_MODULE=0)
endif()

if(TARGET bitraf_static)
    target_link_libraries(vectra_core_accel PRIVATE bitraf_static)
endif()

if(VECTRA_REQUIRE_FREESTANDING_CORE AND NOT TARGET abi_core_freestanding)
    message(FATAL_ERROR "Freestanding ABI core target missing. Silent downgrade is forbidden.")
endif()

if(NOT VECTRA_REQUIRE_FREESTANDING_CORE)
    message(FATAL_ERROR "VECTRA_REQUIRE_FREESTANDING_CORE must remain ON for release-safe builds.")
endif()

target_compile_options(abi_core_freestanding PRIVATE
    ${VECTRA_FREESTANDING_COMPILE_OPTIONS}
    ${VECTRA_ABI_COMPILE_OPTIONS})
target_include_directories(abi_core_freestanding PRIVATE
    ${VECTRA_RMR_INCLUDE_DIR})
target_link_options(abi_core_freestanding PRIVATE
    -nostdlib
    -Wl,--gc-sections
    -Wl,--build-id=none)

if(ANDROID)
    # Compatibilidade Android 15/16 (dispositivos com page size de 16 KiB).
    target_link_options(vectra_core_accel PRIVATE -Wl,-z,max-page-size=16384)
    if(TARGET termux-bootstrap)
        target_link_options(termux-bootstrap PRIVATE -Wl,-z,max-page-size=16384)
    endif()
endif()

target_compile_options(vectra_core_accel PRIVATE
    ${VECTRA_COMMON_COMPILE_OPTIONS}
    ${VECTRA_ABI_COMPILE_OPTIONS})
if(TARGET termux-bootstrap)
    target_compile_options(termux-bootstrap PRIVATE
        ${VECTRA_COMMON_COMPILE_OPTIONS}
        ${VECTRA_ABI_COMPILE_OPTIONS})
endif()

target_compile_definitions(vectra_core_accel PRIVATE
    RMR_ASM_CORE_EXPERIMENTAL=$<BOOL:${RMR_ASM_CORE_EXPERIMENTAL}>
    RMR_ASM_CORE_X86_64_VALIDATED=$<BOOL:${RMR_ASM_CORE_X86_64_VALIDATED}>
    RMR_ASM_CORE_ARM64_VALIDATED=$<BOOL:${RMR_ASM_CORE_ARM64_VALIDATED}>
    RMR_ASM_CORE_RISCV64_VALIDATED=$<BOOL:${RMR_ASM_CORE_RISCV64_VALIDATED}>)

target_link_libraries(vectra_core_accel PRIVATE ${log-lib})
target_link_libraries(vectra_core_accel PRIVATE abi_core_freestanding)
if(TARGET termux-bootstrap)
    target_link_libraries(termux-bootstrap PRIVATE ${log-lib})
endif()

target_include_directories(vectra_core_accel PRIVATE
    ${VECTRA_RMR_INCLUDE_DIR})

if(TARGET termux-bootstrap)
    target_include_directories(termux-bootstrap PRIVATE
        ${VECTRA_RMR_INCLUDE_DIR})
endif()

if(NOT ANDROID)
    target_include_directories(vectra_core_accel PRIVATE ${JNI_INCLUDE_DIRS})
    if(TARGET termux-bootstrap)
        target_include_directories(termux-bootstrap PRIVATE ${JNI_INCLUDE_DIRS})
    endif()
endif()

if(CMAKE_ANDROID_NDK)
    target_include_directories(vectra_core_accel PRIVATE
        ${CMAKE_ANDROID_NDK}/sysroot/usr/include
        ${CMAKE_ANDROID_NDK}/sysroot/usr/include/${CMAKE_ANDROID_ARCH_TRIPLE})
    if(TARGET termux-bootstrap)
        target_include_directories(termux-bootstrap PRIVATE
            ${CMAKE_ANDROID_NDK}/sysroot/usr/include
            ${CMAKE_ANDROID_NDK}/sysroot/usr/include/${CMAKE_ANDROID_ARCH_TRIPLE})
    endif()
endif()
