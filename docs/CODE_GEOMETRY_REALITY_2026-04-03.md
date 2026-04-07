<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Vectras-VM-Android — Code Geometry Reality (2026-04-03)

## Purpose

This file records a **code-first reading** of the repository.

The documentation remains important for navigation, provisioning, troubleshooting and project framing.
But the real operational geometry of the project is more clearly expressed in code than in prose.

## Reading rule

For this repository:
1. use docs as a map
2. use code as the source of truth for execution geometry

## Core geometry seen in code

### 1. The repository is not only an Android VM app surface
A meaningful part of the architecture is centered around a low-level native bridge, hardware-profile reading, arena-oriented buffer operations and deterministic fallbacks.

Representative files:
- `app/src/main/java/com/vectras/vm/core/LowLevelBridge.java`
- `app/src/main/java/com/vectras/vm/core/NativeFastPath.java`

### 2. Hardware / kernel contract is a real execution axis
The code explicitly models host and execution geometry through fields such as:
- pointer bits
- cache line bytes
- page bytes
- feature mask
- arena bytes
- io quantum bytes

This is not documentation-only language. It is part of the runtime contract surface.

### 3. Feature-mask geometry matters
The code models instruction-path capabilities through a feature mask including, among others:
- NEON
- AES
- CRC32
- POPCNT
- SSE4.2
- AVX2
- SIMD

This means the repository should be read not only as a VM front-end, but also as a host-sensitive execution system.

### 4. Arena/buffer operations are central to the hot path
The code exposes arena-oriented operations such as:
- arena allocation / free
- arena copy
- arena xor checksum
- arena fill
- arena write

This strongly suggests that a relevant part of the design is centered on:
- buffer locality
- controlled copy paths
- reduced allocation churn
- deterministic byte flow

### 5. The Java layer is not purely high-level business logic
Parts of the Java code are deliberately thin wrappers over native paths with explicit fallback logic.
That means the project should not be read only through an OO / Android-UI lens.
A better reading is:
- Android control shell
- JNI bridge
- native execution kernels
- contract / telemetry / fallback surfaces

### 6. Telemetry exists, but should not be confused with the hot path itself
The code contains boot/profile/telemetry formatting and counters.
Those are useful for observability, but the more important architectural fact is that they sit around a lower-level contract built on:
- native availability
- hardware profile
- kernel unit profile
- copy / xor / crc / route / audit primitives

## Short architecture interpretation

This repository can be read as four planes:

### Plane A — Android / orchestration
- app lifecycle
- settings
- UI and integration

### Plane B — bridge
- load native library
- route calls into native contracts
- maintain Java fallback paths

### Plane C — geometry
- pointer bits
- cache line
- page size
- feature mask
- arena bytes
- io quantum bytes

### Plane D — byte-flow execution
- copy
- xor / checksum / crc
- route
- audit
- arena-based operations

## Why this note exists

A reader may over-interpret documentation and under-read the implementation.
For Vectras, that is risky.
The codebase expresses a more physical/logical execution model than a standard Android application does.

## Maintainer summary

> The documentation shows the roads.
> The code shows the execution geometry.

For this repository, the execution geometry is defined by:
- native contract
- host profile
- feature mask
- arena/buffer path
- deterministic fallback
