# App Identity

## Decision

The project keeps the current Android application id as the stable install identity:

```text
com.rafacodephi.app
```

The Android namespace remains the internal source namespace:

```text
com.vectras.vm
```

## Rationale

This keeps the existing build path stable while preserving the Vectras VM source lineage inside the codebase.

The repository name and public technical name may continue to use Vectras VM Android. The install identity remains Rafacodephi until a future migration is explicitly planned.

## Current Mapping

| Area | Value |
|---|---|
| Repository | `wojcikiewicz17/Vectras-VM-Android` |
| Technical project name | `Vectras VM Android` |
| Android application id | `com.rafacodephi.app` |
| Android namespace | `com.vectras.vm` |
| Source lineage | Vectras VM plus Rafacodephi/Rafaelia runtime work |

## Rule

Do not change the Android application id casually.

Any future change must include:

- migration note;
- release note;
- build review;
- user impact review;
- attribution review.

## Recommended Track

For now, use a single stable track:

```text
com.rafacodephi.app
```

A future dual track can be considered only if the project needs separate stable and experimental builds.
