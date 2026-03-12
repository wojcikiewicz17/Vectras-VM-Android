#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

LOG_PREFIX="[termux-local-build]"
PRIVATE_LOCAL_KEYSTORE_FALLBACK="$ROOT_DIR/.secrets/vectras-release.jks"
RELEASE_STORE_FILE="${VECTRAS_RELEASE_STORE_FILE:-}"

log(){ echo "$LOG_PREFIX $*"; }

if [[ -n "${GITHUB_ACTIONS:-}" ]]; then
  echo "$LOG_PREFIX este entrypoint é exclusivo para build local em terminal (não GitHub Actions)." >&2
  exit 1
fi

if [[ -z "$RELEASE_STORE_FILE" && -f "$PRIVATE_LOCAL_KEYSTORE_FALLBACK" ]]; then
  RELEASE_STORE_FILE="$PRIVATE_LOCAL_KEYSTORE_FALLBACK"
  log "usando fallback local privado de keystore em $PRIVATE_LOCAL_KEYSTORE_FALLBACK"
fi

export VECTRAS_RELEASE_STORE_FILE="$RELEASE_STORE_FILE"
export VECTRAS_RELEASE_STORE_PASSWORD="${VECTRAS_RELEASE_STORE_PASSWORD:-${VECTRAS_STORE_PASSWORD:-}}"
export VECTRAS_RELEASE_KEY_ALIAS="${VECTRAS_RELEASE_KEY_ALIAS:-${VECTRAS_KEY_ALIAS:-}}"
export VECTRAS_RELEASE_KEY_PASSWORD="${VECTRAS_RELEASE_KEY_PASSWORD:-${VECTRAS_KEY_PASSWORD:-}}"

export BOOTSTRAP_ANDROID="${BOOTSTRAP_ANDROID:-1}"
export ENABLE_SPILL="${ENABLE_SPILL:-1}"
export CI_DRY_RUN="${CI_DRY_RUN:-0}"

log "iniciando orchestrate-build (gate + bootstrap + build)"
bash tools/termux-arm64-orchestrator/orchestrate-build.sh
log "build local concluído"
