cmake_minimum_required(VERSION 3.22.1)
project(vectra_core_accel C)

add_library(vectra_core_accel SHARED
    vectra_core_accel.c
    ../../../../engine/rmr/src/rmr_unified_kernel.c
    ../../../../engine/rmr/src/rmr_hw_detect.c)

find_library(log-lib log)
find_package(JNI REQUIRED)

target_compile_options(vectra_core_accel PRIVATE -O3 -ffast-math -fvisibility=hidden)

target_link_libraries(vectra_core_accel ${log-lib})

target_include_directories(vectra_core_accel PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../engine/rmr/include
    ${JNI_INCLUDE_DIRS})

if(ANDROID AND DEFINED CMAKE_ANDROID_NDK)
    target_include_directories(vectra_core_accel PRIVATE
        ${CMAKE_ANDROID_NDK}/toolchains/llvm/prebuilt/${CMAKE_HOST_SYSTEM_NAME}-x86_64/sysroot/usr/include)
endif()
