# Workflows map

## Entry point
- `.github/workflows/pipeline-orchestrator.yml`
  - resolves profile (`host_only`, `android_only`, `full`)
  - dispatches reusable `host-ci` and `android-ci`
  - runs reusable `quality-gates`

## Reusable workflows
- `.github/workflows/host-ci.yml`
- `.github/workflows/android-ci.yml`
- `.github/workflows/quality-gates.yml`
- `.github/workflows/compile-matrix.yml`

## Legacy workflows
- `android.yml` and `ci.yml` remain for compatibility while orchestrator migration stabilizes.
