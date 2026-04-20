# Compilation Pipeline

1. Resolve profile in `pipeline-orchestrator.yml`.
2. Run host build lane (`host-ci.yml`) when enabled.
3. Run Android lane (`android-ci.yml`) when enabled.
4. Enforce cross-lane checks (`quality-gates.yml`).

Android builds must use `tools/gradle_with_jdk21.sh`; host builds must pass
CMake configure + `run_selftest` target.
