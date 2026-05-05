#!/usr/bin/env bash
set -euo pipefail
pattern='ISO certified|certificado ISO|ISO compliant|compliance ISO garantido|conforme ISO|certificação ISO'
matches="$(rg -n "$pattern" . --glob '!docs/AUDIT_CLAIMS_POLICY.md' --glob '!scripts/validate_audit_claims.sh' || true)"
filtered="$(printf '%s\n' "$matches" | rg -v 'não declara certificação ISO|does not claim ISO certification' || true)"
if [[ -n "${filtered//[$'\n\r\t ']/}" ]]; then
  printf '%s\n' "$filtered"
  echo "[FAIL] forbidden audit/certification claim found"
  exit 1
fi
echo "[OK] no forbidden ISO certification claims found"
