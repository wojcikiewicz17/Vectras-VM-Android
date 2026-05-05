# OPS Policy Pipeline

OPS in vectra_rmr is a deterministic operational sequence:

1. PLAN: evaluate chunks, route hints and triad status.
2. APPLY: write selected mutations or promoted state output.
3. DIFF: detect content deltas and chunk-level divergence.
4. VERIFY: validate crc32c and hash64 checks.
5. AUDIT: persist deterministic summary and signatures.

## Operational guarantees

- deterministic replay for same input and config
- low-copy chunk processing
- storage-aware and cache-aware routing
- buffer reuse across stages
- optional direct I/O behavior through tune plan
- explicit state promotion reporting in JSONL
