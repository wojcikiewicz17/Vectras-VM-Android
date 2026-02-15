cmake_minimum_required(VERSION 3.22.1)
project(vectra_core_accel C)

option(RMR_ENABLE_POLICY_MODULE "Build optional RMR policy module (kernel/math/tuning)" OFF)
option(RMR_ENABLE_BITRAF_MODULE "Build BITRAF static module required by legacy unified symbols" ON)

add_library(rmr_core_static STATIC
    ../../../../engine/rmr/src/rmr_unified_kernel.c
    ../../../../engine/rmr/src/rmr_hw_detect.c
    ../../../../engine/rmr/src/rmr_corelib.c
    ../../../../engine/rmr/src/rmr_cycles.c
    ../../../../engine/rmr/src/rmr_ll_ops.c)

if (RMR_ENABLE_POLICY_MODULE)
    add_library(rmr_policy_static STATIC
        ../../../../engine/rmr/src/rmr_policy_kernel.c
        ../../../../engine/rmr/src/rmr_math_fabric.c
        ../../../../engine/rmr/src/rmr_ll_tuning.c)
endif()

add_library(bitraf_static STATIC
    ../../../../engine/rmr/src/bitraf.c)

add_library(vectra_core_accel SHARED
    vectra_core_accel.c)

find_library(log-lib log)
if(NOT log-lib)
    message(WARNING "Android log library not found in host configure; proceeding without it")
    set(log-lib "")
endif()

target_compile_options(rmr_core_static PRIVATE -O3 -ffast-math -fvisibility=hidden)
if (RMR_ENABLE_POLICY_MODULE)
    target_compile_options(rmr_policy_static PRIVATE -O3 -ffast-math -fvisibility=hidden)
endif()
target_compile_options(bitraf_static PRIVATE -O3 -ffast-math -fvisibility=hidden)
target_compile_options(vectra_core_accel PRIVATE -O3 -ffast-math -fvisibility=hidden)

target_include_directories(rmr_core_static PRIVATE ../../../../ ../../../../engine/rmr/include)
if (RMR_ENABLE_POLICY_MODULE)
    target_include_directories(rmr_policy_static PRIVATE ../../../../ ../../../../engine/rmr/include)
endif()
target_include_directories(bitraf_static PRIVATE ../../../../ ../../../../engine/rmr/include)
target_include_directories(vectra_core_accel PRIVATE ../../../../ ../../../../engine/rmr/include)

target_link_libraries(vectra_core_accel PRIVATE rmr_core_static)

if (RMR_ENABLE_BITRAF_MODULE)
    target_link_libraries(vectra_core_accel PRIVATE bitraf_static)
else()
    message(FATAL_ERROR "RMR_ENABLE_BITRAF_MODULE=OFF breaks legacy unified symbols (bitraf_hash/bitraf_verify) used by JNI bridge")
endif()

if (RMR_ENABLE_POLICY_MODULE)
    target_link_libraries(vectra_core_accel PRIVATE rmr_policy_static)
endif()

if (log-lib)
    target_link_libraries(vectra_core_accel PRIVATE ${log-lib})
endif()
