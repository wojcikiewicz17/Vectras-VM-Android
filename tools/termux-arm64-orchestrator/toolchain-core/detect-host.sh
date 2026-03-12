#!/usr/bin/env bash
set -euo pipefail

ARCH="$(uname -m || true)"
OS="$(uname -s || true)"
KERNEL="$(uname -r || true)"
PAGE_SIZE="$(getconf PAGESIZE 2>/dev/null || true)"

CPUINFO_PATH="/proc/cpuinfo"
HAS_NEON=0
HAS_ASIMD=0
if [[ -f "$CPUINFO_PATH" ]]; then
  if rg -i "neon" "$CPUINFO_PATH" >/dev/null 2>&1; then HAS_NEON=1; fi
  if rg -i "asimd" "$CPUINFO_PATH" >/dev/null 2>&1; then HAS_ASIMD=1; fi
fi

cat <<EOT
ARCH=${ARCH:-unknown}
OS=${OS:-unknown}
KERNEL=${KERNEL:-unknown}
PAGE_SIZE=${PAGE_SIZE:-unknown}
HAS_NEON=$HAS_NEON
HAS_ASIMD=$HAS_ASIMD
EOT
