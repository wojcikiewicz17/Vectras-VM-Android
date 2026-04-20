# Android platform module for JNI builds.
if(NOT ANDROID)
  return()
endif()

message(STATUS "[VECTRAS-Android] Applying JNI platform flags for ${CMAKE_ANDROID_ARCH_ABI}")

add_compile_definitions(RMR_JNI_BUILD=1)
add_compile_options(-O2 -fno-fast-math -fno-exceptions -fno-rtti)

if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
  add_compile_options(-march=armv8-a+crc+simd)
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
  add_compile_options(-march=armv7-a -mfpu=neon)
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86_64")
  add_compile_options(-msse4.2 -mpopcnt)
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86")
  add_compile_options(-mssse3 -msse4.2 -mpopcnt)
endif()
