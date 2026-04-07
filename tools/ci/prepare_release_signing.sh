#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: prepare_release_signing.sh --mode <signed|unsigned|auto> [--keystore-out <path>] [--allow-legacy-fallback]

Outputs (when GITHUB_ENV exists):
  VECTRAS_CI_RELEASE_FLAGS
  VECTRAS_CI_SIGNING_ARGS
  VECTRAS_RELEASE_STORE_FILE (if signed)
USAGE
}

MODE=""
KEYSTORE_OUT="${RUNNER_TEMP:-/tmp}/vectras-release.keystore"
ALLOW_LEGACY_FALLBACK="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      MODE="$2"
      shift 2
      ;;
    --keystore-out)
      KEYSTORE_OUT="$2"
      shift 2
      ;;
    --allow-legacy-fallback)
      ALLOW_LEGACY_FALLBACK="true"
      shift
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

has_required_signing_secrets() {
  [[ -n "${VECTRAS_RELEASE_STORE_PASSWORD:-}" && -n "${VECTRAS_RELEASE_KEY_ALIAS:-}" && -n "${VECTRAS_RELEASE_KEY_PASSWORD:-}" ]]
}

resolve_keystore() {
  if [[ -n "${VECTRAS_RELEASE_STORE_FILE:-}" ]]; then
    [[ -s "${VECTRAS_RELEASE_STORE_FILE}" ]]
    return
  fi

  if [[ -n "${VECTRAS_RELEASE_KEYSTORE_B64:-}" ]]; then
    echo "${VECTRAS_RELEASE_KEYSTORE_B64}" | base64 --decode > "$KEYSTORE_OUT"
    if [[ ! -s "$KEYSTORE_OUT" ]]; then
      echo "::error::VECTRAS_RELEASE_KEYSTORE_B64 provided but decoded keystore is empty" >&2
      exit 1
    fi
    export VECTRAS_RELEASE_STORE_FILE="$KEYSTORE_OUT"
    return
  fi

  if [[ "$ALLOW_LEGACY_FALLBACK" == "true" && -n "${ANDROID_SIGNING_KEYSTORE:-}" ]]; then
    echo "::warning::Using legacy ANDROID_SIGNING_KEYSTORE fallback"
    echo "${ANDROID_SIGNING_KEYSTORE}" | base64 --decode > "$KEYSTORE_OUT"
    if [[ ! -s "$KEYSTORE_OUT" ]]; then
      echo "::error::ANDROID_SIGNING_KEYSTORE decoded keystore is empty" >&2
      exit 1
    fi
    export VECTRAS_RELEASE_STORE_FILE="$KEYSTORE_OUT"
    return
  fi

  return 1
}

signed_ready="false"
if has_required_signing_secrets && resolve_keystore; then
  signed_ready="true"
fi

RELEASE_FLAGS=""
SIGNING_ARGS=""

case "$MODE" in
  signed)
    if [[ "$signed_ready" != "true" ]]; then
      echo "::error::signed mode requires valid keystore and signing secrets" >&2
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
  SIGNING_ARGS="-Pandroid.injected.signing.store.file=${VECTRAS_RELEASE_STORE_FILE} -Pandroid.injected.signing.store.password=${VECTRAS_RELEASE_STORE_PASSWORD} -Pandroid.injected.signing.key.alias=${VECTRAS_RELEASE_KEY_ALIAS} -Pandroid.injected.signing.key.password=${VECTRAS_RELEASE_KEY_PASSWORD}"
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
