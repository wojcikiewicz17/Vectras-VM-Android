cmake_minimum_required(VERSION 3.22.1)
project(vectra_core_accel C)

option(RMR_ENABLE_POLICY_MODULE "Build and link rmr_policy_static extension module" OFF)
option(RMR_ENABLE_BITRAF_MODULE "Build and link bitraf_static support module" ON)

if(NOT RMR_ENABLE_BITRAF_MODULE)
    message(WARNING "RMR_ENABLE_BITRAF_MODULE=OFF breaks required symbols from rmr_unified_kernel.c; forcing ON for JNI-safe minimal profile")
    set(RMR_ENABLE_BITRAF_MODULE ON CACHE BOOL "Build and link bitraf_static support module" FORCE)
endif()

add_library(rmr_core_static STATIC
    ../../../../engine/rmr/src/rmr_unified_kernel.c
    ../../../../engine/rmr/src/rmr_hw_detect.c
    ../../../../engine/rmr/src/rmr_corelib.c
    ../../../../engine/rmr/src/rmr_ll_ops.c
    ../../../../engine/rmr/src/rmr_cycles.c)

if(RMR_ENABLE_POLICY_MODULE)
    add_library(rmr_policy_static STATIC
        ../../../../engine/rmr/src/rmr_policy_kernel.c
        ../../../../engine/rmr/src/rmr_math_fabric.c
        ../../../../engine/rmr/src/rmr_ll_tuning.c)
endif()

add_library(bitraf_static STATIC
    ../../../../engine/rmr/src/bitraf.c)

add_library(vectra_core_accel SHARED vectra_core_accel.c)

if(ANDROID)
    find_library(log-lib log)
else()
    set(log-lib "")
endif()

target_compile_options(rmr_core_static PRIVATE -O3 -ffast-math -fvisibility=hidden)
if(TARGET rmr_policy_static)
    target_compile_options(rmr_policy_static PRIVATE -O3 -ffast-math -fvisibility=hidden)
endif()
target_compile_options(bitraf_static PRIVATE -O3 -ffast-math -fvisibility=hidden)
target_compile_options(vectra_core_accel PRIVATE -O3 -ffast-math -fvisibility=hidden)

target_link_libraries(vectra_core_accel PRIVATE rmr_core_static ${log-lib})

if(TARGET rmr_policy_static)
    target_link_libraries(vectra_core_accel PRIVATE rmr_policy_static)
endif()

if(RMR_ENABLE_BITRAF_MODULE)
    target_link_libraries(vectra_core_accel PRIVATE bitraf_static)
endif()

target_include_directories(rmr_core_static PRIVATE ../../../../ ../../../../engine/rmr/include)
if(TARGET rmr_policy_static)
    target_include_directories(rmr_policy_static PRIVATE ../../../../ ../../../../engine/rmr/include)
endif()
target_include_directories(bitraf_static PRIVATE ../../../../ ../../../../engine/rmr/include)
target_include_directories(vectra_core_accel PRIVATE ../../../../ ../../../../engine/rmr/include)
