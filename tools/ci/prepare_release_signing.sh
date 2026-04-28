#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: prepare_release_signing.sh --mode <signed|unsigned|auto>

Required secrets for signed mode:
  ANDROID_KEYSTORE_BASE64
  ANDROID_KEYSTORE_PASSWORD
  ANDROID_KEY_ALIAS
  ANDROID_KEY_PASSWORD

Outputs (when GITHUB_ENV exists):
  VECTRAS_CI_RELEASE_FLAGS
  VECTRAS_CI_SIGNING_ARGS
  VECTRAS_RELEASE_STORE_FILE (if signed)
USAGE
}

MODE=""
KEYSTORE_OUT=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      MODE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown arg: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ -z "$MODE" ]]; then
  echo "::error::--mode is required" >&2
  exit 1
fi

cleanup_keystore() {
  if [[ -n "${KEYSTORE_OUT:-}" && -f "$KEYSTORE_OUT" ]]; then
    rm -f "$KEYSTORE_OUT"
  fi
}

trap cleanup_keystore EXIT

missing_required_secrets() {
  local missing=()
  [[ -n "${ANDROID_KEYSTORE_BASE64:-}" ]] || missing+=("ANDROID_KEYSTORE_BASE64")
  [[ -n "${ANDROID_KEYSTORE_PASSWORD:-}" ]] || missing+=("ANDROID_KEYSTORE_PASSWORD")
  [[ -n "${ANDROID_KEY_ALIAS:-}" ]] || missing+=("ANDROID_KEY_ALIAS")
  [[ -n "${ANDROID_KEY_PASSWORD:-}" ]] || missing+=("ANDROID_KEY_PASSWORD")
  printf '%s\n' "${missing[@]:-}"
}

has_required_signing_secrets() {
  [[ -n "${ANDROID_KEYSTORE_BASE64:-}" && -n "${ANDROID_KEYSTORE_PASSWORD:-}" && -n "${ANDROID_KEY_ALIAS:-}" && -n "${ANDROID_KEY_PASSWORD:-}" ]]
}

resolve_keystore() {
  local runner_tmp="${RUNNER_TEMP:-/tmp}"
  umask 077
  KEYSTORE_OUT="$(mktemp "${runner_tmp%/}/vectras-release-XXXXXX.keystore")"

  if ! printf '%s' "${ANDROID_KEYSTORE_BASE64}" | base64 --decode > "$KEYSTORE_OUT"; then
    echo "::error::Failed to decode ANDROID_KEYSTORE_BASE64" >&2
    exit 1
  fi

  if [[ ! -s "$KEYSTORE_OUT" ]]; then
    echo "::error::ANDROID_KEYSTORE_BASE64 provided but decoded keystore is empty" >&2
    exit 1
  fi

  export VECTRAS_RELEASE_STORE_FILE="$KEYSTORE_OUT"
}

signed_ready="false"
if has_required_signing_secrets; then
  resolve_keystore
  signed_ready="true"
fi

RELEASE_FLAGS=""
SIGNING_ARGS=""

case "$MODE" in
  signed)
    if [[ "$signed_ready" != "true" ]]; then
      mapfile -t missing < <(missing_required_secrets)
      if [[ ${#missing[@]} -gt 0 && -n "${missing[0]}" ]]; then
        echo "::error::signed mode missing required secrets: ${missing[*]}" >&2
      else
        echo "::error::signed mode requires valid keystore and signing secrets" >&2
      fi
      exit 1
    fi
    ;;
  unsigned)
    RELEASE_FLAGS="-PALLOW_UNSIGNED_RELEASE=true -PALLOW_PLACEHOLDER_FIREBASE_FOR_RELEASE=true -PCI_INTERNAL_VALIDATION=true"
    ;;
  auto)
    if [[ "$signed_ready" != "true" ]]; then
      RELEASE_FLAGS="-PALLOW_UNSIGNED_RELEASE=true -PALLOW_PLACEHOLDER_FIREBASE_FOR_RELEASE=true -PCI_INTERNAL_VALIDATION=true"
    fi
    ;;
  *)
    echo "::error::Invalid mode: $MODE" >&2
    exit 1
    ;;
esac

if [[ -z "$RELEASE_FLAGS" ]]; then
  SIGNING_ARGS="-Pandroid.injected.signing.store.file=${VECTRAS_RELEASE_STORE_FILE} -Pandroid.injected.signing.store.password=${ANDROID_KEYSTORE_PASSWORD} -Pandroid.injected.signing.key.alias=${ANDROID_KEY_ALIAS} -Pandroid.injected.signing.key.password=${ANDROID_KEY_PASSWORD}"
fi

if [[ -n "${GITHUB_ENV:-}" ]]; then
  {
    echo "VECTRAS_CI_RELEASE_FLAGS=${RELEASE_FLAGS}"
    echo "VECTRAS_CI_SIGNING_ARGS=${SIGNING_ARGS}"
    if [[ -n "${VECTRAS_RELEASE_STORE_FILE:-}" ]]; then
      echo "VECTRAS_RELEASE_STORE_FILE=${VECTRAS_RELEASE_STORE_FILE}"
    fi
  } >> "$GITHUB_ENV"
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "release_flags=${RELEASE_FLAGS}"
    echo "signing_args=${SIGNING_ARGS}"
    echo "signed_ready=${signed_ready}"
  } >> "$GITHUB_OUTPUT"
fi

echo "Resolved release signing mode=${MODE} signed_ready=${signed_ready}"
