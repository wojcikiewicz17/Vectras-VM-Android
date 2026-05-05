# Vectra External Engine Contract

Technical engine name: `vectra_rmr`.
Human program reference: RAFCODEΦ. Technical consumer id: `rafcodephi`.

## Module mapping
- BitOmega: state transition, directionality, coherence, entropy.
- BitRaf: frame, payload, hash64, CRC, chunk table, deterministic reconstruction.
- ZipRaf: triangular composition, route tag, coherence flags, stage signature.
- Policy Kernel: PLAN, APPLY, DIFF, VERIFY, AUDIT.
- HW Detect: arch, cacheline, page size, L1/L2/L3/L4 hints, bus, alignment.
- LL Tuning: batch size, lane width, commit quantum, prefetch, direct I/O flag.
- MathFabric: architecture and memory dependent matrix plan.
- Unified Kernel: Android/JNI/external integration entrypoint.

## External stable C API
Header: `engine/rmr/include/rmr_external_engine.h`.
Source: `engine/rmr/src/rmr_external_engine.c`.

Functions:
- `RmR_External_DetectHardware`
- `RmR_External_BuildTunePlan`
- `RmR_External_RunPolicyPipeline`
- `RmR_External_RunZipRaf`
- `RmR_External_RunBitRafVerify`
- `RmR_External_RunBitOmegaStep`
- `RmR_External_WriteStatePromotionReport`

## State pipeline contract
The engine exposes a low-copy, storage-aware, cache-aware, direct-I/O-capable, buffer-reuse pipeline with explicit state promotion.

The external hash dependency `blake3_rmr` is out of scope in this repository and must be executed by `rafcodephi` or a future explicit stage.
