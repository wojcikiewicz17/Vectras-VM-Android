# OPS Policy Pipeline

## Stages
- PLAN: chunk planning and route preselection.
- APPLY: deterministic transformation and output write.
- DIFF: delta accounting by chunk signature.
- VERIFY: CRC/hash validation and replay integrity.
- AUDIT: append-only summary and execution signature.

## Operational rules
- Every run must persist deterministic audit output.
- Verification failures increment `verify_failures` and block promotion.
- Promotion requires coherent route + verify_ok = 1.
