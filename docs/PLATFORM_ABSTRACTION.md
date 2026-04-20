# Platform Abstraction

- Core sources: `engine/rmr/`
- Platform hooks: `engine/platform/android`, `engine/platform/linux`

Rules:
- Android JNI uses hosted libc path, never baremetal flags.
- Linux host tooling is isolated through host CI and root CMake host targets.
- platform modules are included by root CMake platform detection.
