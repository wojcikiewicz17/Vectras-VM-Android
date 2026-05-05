# Vectra External Engine Contract

This document defines the stable external contract for vectra_rmr so Termux RAFCODEΦ (technical id: rafcodephi) can consume state and pipeline behavior with ASCII-safe integration points.

## Module contract map

- BitOmega: state transition, direction channel, coherence metric, entropy metric.
- BitRaf: frame envelope, payload integrity, hash64, crc32c, chunk table, reconstruction and verify.
- ZipRaf: triangular composition, route tag, coherence flags and deterministic signature.
- Policy Kernel: PLAN, APPLY, DIFF, VERIFY, AUDIT execution chain with deterministic audit summary.
- HW Detect: architecture, cacheline, page size, l1/l2/l3/l4 hints, bus width and alignment.
- LL Tuning: batch size, lane width, commit quantum, prefetch and direct I/O flag.
- MathFabric: architecture-dependent matrix plan bound to cache and page topology.
- Unified Kernel: single integration entrypoint for Android/JNI and external C callers.

## External C API

Stable API: `engine/rmr/include/rmr_external_engine.h`.

Functions:
- `RmR_External_DetectHardware`
- `RmR_External_BuildTunePlan`
- `RmR_External_RunPolicyPipeline`
- `RmR_External_RunZipRaf`
- `RmR_External_RunBitRafVerify`
- `RmR_External_RunBitOmegaStep`
- `RmR_External_WriteStatePromotionReport`

The API hides internal structures and accepts simplified request/response data contracts.

## State and promotion contract

State promotion is low-copy, storage-aware, cache-aware, direct-I/O-capable, and supports buffer reuse. Promotion emits JSONL lines with:

- event
- offset
- size
- mem_tier
- route_id
- crc32c
- hash64
- entropy_milli
- math_signature
- stage_signature
- decision_mode
- recompute_skipped
- verify_ok

Default report sinks:
- `state_promotion_report.jsonl`
- `reports/vectra_state_promotion.jsonl`

## External hash boundary

blake3_rmr is not linked in vectra_rmr core at this stage. External hash orchestration must be performed by Termux rafcodephi or a future explicit pipeline stage.
