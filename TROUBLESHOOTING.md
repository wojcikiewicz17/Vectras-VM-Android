<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# TROUBLESHOOTING

## Build fails with "SDK location not found"
Set `local.properties` with `sdk.dir=<path-to-android-sdk>`.

## Build fails before compilation with unresolved provider
If app depends on `:shell-loader:stub` as compile-only and AGP resolves no artifact, ensure dependency is regular implementation in `app/build.gradle`.

## PROOT bootstrap diagnostics
Search logcat for:
- `PROOT_BOOTSTRAP ABI_SELECTED`
- `PROOT_BOOTSTRAP URL_NORMALIZED`
- `PROOT_BOOTSTRAP PRECHECK_FAIL`
- `PROOT_PREFLIGHT_FAIL:`

These identify ABI selection, URL normalization, and filesystem prerequisite failures.

## First-run setup reports missing binaries
Run setup again after clearing app data. The app now checks:
- `$files/usr/bin/proot` (exists + executable)
- `$files/distro/bin/busybox` (exists + executable)
- `$files/distro/bin/sh` (exists + executable)
- `$files/usr/tmp` (writable)

If any check fails, setup stops with explicit reason codes.

## Bootstrap URL errors
The app validates host/scheme and normalizes duplicated slashes in URL path.
If metadata contains malformed URLs or unsupported host, remote setup is rejected.

## Release/perfRelease falha com erro de Firebase em CI
- Caminho **oficial assinado** requer `app/google-services.json` real (secret `VECTRAS_GOOGLE_SERVICES_JSON_B64` no workflow).
- Caminho de **validação interna unsigned** pode usar placeholder, mas deve sinalizar explicitamente:
  - `-PCI_INTERNAL_VALIDATION=true`
  - (compatibilidade) `-PALLOW_PLACEHOLDER_FIREBASE_FOR_RELEASE=true`
- Sem o sinal explícito de CI interno, builds `release/perfRelease` continuam bloqueando placeholder.

## CI canonical reference (Android/Host)

- Canonical Android pipeline: `.github/workflows/android-ci.yml`.
- Android wrapper entrypoint: `.github/workflows/android.yml`.
- Auxiliary Android ABI compatibility matrix: `.github/workflows/compile-matrix.yml`.
- Canonical host pipeline: `.github/workflows/host-ci.yml`.
- Orchestration and final gates: `.github/workflows/pipeline-orchestrator.yml` and `.github/workflows/quality-gates.yml`.
- Canonical matrix documentation: `docs/ci/workflow-matrix.md`.

- Release interno unsigned exige: `-PCI_INTERNAL_VALIDATION=true -Psigning_mode=unsigned` e, quando firebase placeholder, `-PALLOW_PLACEHOLDER_FIREBASE_FOR_RELEASE=true`.
