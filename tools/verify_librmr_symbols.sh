#!/usr/bin/env sh
set -eu

if [ "$#" -lt 2 ]; then
  echo "usage: $0 <librmr.a> <required_symbol> [required_symbol ...]" >&2
  exit 2
fi

LIBRARY_PATH=$1
shift

if [ ! -f "$LIBRARY_PATH" ]; then
  echo "[link-contract] archive not found: $LIBRARY_PATH" >&2
  exit 1
fi

if ! command -v nm >/dev/null 2>&1; then
  echo "[link-contract] nm is required but was not found in PATH" >&2
  exit 1
fi

EXPORTED_SYMBOLS=$(nm -g --defined-only "$LIBRARY_PATH" | awk 'NF>0 {print $NF}')
MISSING_COUNT=0

for REQUIRED_SYMBOL in "$@"; do
  if ! printf '%s\n' "$EXPORTED_SYMBOLS" | awk -v sym="$REQUIRED_SYMBOL" '$0 == sym {found=1} END {exit(found ? 0 : 1)}'; then
    echo "[link-contract] missing symbol in $LIBRARY_PATH: $REQUIRED_SYMBOL" >&2
    MISSING_COUNT=$((MISSING_COUNT + 1))
  fi
done

if [ "$MISSING_COUNT" -ne 0 ]; then
  echo "[link-contract] failed: $MISSING_COUNT required symbol(s) missing" >&2
  exit 1
fi

echo "[link-contract] verified required symbols in $LIBRARY_PATH"
