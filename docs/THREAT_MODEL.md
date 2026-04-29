# Threat Model

## System Overview

Vectras VM Android combines:

- Android application UI and lifecycle management;
- QEMU process construction and execution;
- VNC and X11 display/input paths;
- Termux-compatible runtime components;
- native JNI acceleration in C/C++/ASM;
- local VM configuration and disk image management;
- CI/build/release gates.

This threat model defines practical security boundaries for development and review. It does not claim that the app is a complete sandbox against malicious guest operating systems.

## Primary Assets

| Asset | Why it matters |
|---|---|
| User VM images and ISO files | May contain private operating systems, credentials, documents or software. |
| VM metadata and QEMU parameters | May reveal file paths, OS choices, runtime behavior and local environment. |
| Android app private storage | Stores runtime state, configs, bootstraps and generated files. |
| Native ABI contracts | Memory safety and deterministic execution depend on stable C/JNI boundaries. |
| Release signing keys | Compromise allows malicious app updates. |
| CI secrets and artifacts | Can leak signing material, config or publish unsafe builds. |
| Runtime process supervisor | Prevents uncontrolled process spawning and resource exhaustion. |

## Trust Boundaries

| Boundary | Trusted side | Untrusted or semi-trusted side |
|---|---|---|
| Android app UI to QEMU command builder | App code | User-provided VM settings and command fragments |
| Java/Kotlin to JNI | Java validation layer | Native memory and ABI-specific code |
| App to guest VM | Host Android app | Guest OS, disk image, ISO, bootloader |
| App to external storage | App private state | User-selected files and shared storage |
| App to network | Explicit app feature | Remote endpoints, guest services, VNC/X11 exposure |
| CI to release | Maintainer-approved workflow | Pull requests, untrusted code changes, generated artifacts |

## Main Threats

### 1. Command injection in QEMU launch paths

Risk: user-controlled parameters could escape intended QEMU argument structure and execute shell operators or unintended commands.

Controls:

- keep command validation in `VmCommandSafetyValidator` active;
- move toward structured `argv` command construction;
- reject shell metacharacters and control operators;
- log rejection reasons without exposing sensitive data.

### 2. Malicious VM images or ISO files

Risk: a guest OS or boot image may attempt network abuse, UI deception, file corruption, resource exhaustion or exploitation of bundled emulation/runtime components.

Controls:

- do not describe the app as a full security sandbox;
- avoid unnecessary host file sharing;
- document guest networking behavior;
- keep QEMU/runtime components updated;
- isolate per-VM state where practical.

### 3. Native memory safety failures

Risk: JNI or C/ASM code may read/write out of bounds, mis-handle signed lengths, violate ABI contracts or trigger undefined behavior.

Controls:

- validate all Java arrays, offsets and lengths before native access;
- keep ABI contract tests in CI;
- preserve deterministic fallback behavior;
- avoid silent native downgrade in release paths;
- review ARM32 and ARM64 separately.

### 4. Resource exhaustion

Risk: multiple VM processes, runaway guests, VNC/X11 loops or large disk operations may exhaust CPU, RAM, battery, file descriptors or Android process budget.

Controls:

- enforce `ProcessBudgetRegistry` limits;
- terminate detached or rejected processes;
- prune inactive supervisors;
- keep Android 15+ lower process defaults unless measured otherwise;
- keep foreground service behavior explicit.

### 5. Sensitive data exposure through logs

Risk: logs may expose file paths, VM names, command lines, crash traces or environment details.

Controls:

- avoid logging secrets, full private paths and raw user command strings when not necessary;
- keep crash and diagnostic screens local by default;
- scrub logs before attaching them to public issues.

### 6. Permission abuse or overreach

Risk: broad Android permissions can reduce user trust and expand attack surface.

Controls:

- keep `docs/PERMISSIONS_RATIONALE.md` current;
- degrade safely when dangerous permissions are denied;
- remove permissions that no longer serve an active feature.

### 7. Release and supply-chain compromise

Risk: compromised dependencies, CI scripts, release signing material or binary blobs can produce unsafe builds.

Controls:

- do not commit signing material;
- use CI secrets for release signing;
- document binary provenance and hashes;
- keep third-party notices current;
- require CI gates for release/perf-release builds.

## Security Review Checklist

Before merging security-sensitive changes:

- [ ] Does it construct or execute commands?
- [ ] Does it change QEMU arguments or guest/device exposure?
- [ ] Does it cross Java/JNI/native memory boundaries?
- [ ] Does it add or change Android permissions?
- [ ] Does it access external storage or exported providers?
- [ ] Does it add network behavior?
- [ ] Does it touch signing, CI, release or artifacts?
- [ ] Does it add or update third-party binaries?
- [ ] Does it change logging or crash reporting?

## Current Priority Hardening Items

1. Replace raw QEMU command strings with structured argument lists.
2. Split VNC/QMP/mouse lifecycle logic out of large Activity classes.
3. Add regression tests for command validation and process-budget behavior.
4. Add binary provenance table for bootstraps, firmware and bundled runtime assets.
5. Keep ARM32/ARM64 native gates active in CI.
