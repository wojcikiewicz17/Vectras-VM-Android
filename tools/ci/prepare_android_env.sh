#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: prepare_android_env.sh [--java-version <major>] [--sdk-root <path>] [--local-properties <path>] [--require-sdkmanager]
USAGE
}

JAVA_VERSION_EXPECTED=""
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
LOCAL_PROPERTIES_PATH="local.properties"
REQUIRE_SDKMANAGER="false"
DEFAULT_SDK_FALLBACKS=(
  "/usr/lib/android-sdk"
  "/opt/android-sdk"
  "/opt/android-sdk-linux"
  "$HOME/Android/Sdk"
)

while [[ $# -gt 0 ]]; do
  case "$1" in
    --java-version)
      JAVA_VERSION_EXPECTED="$2"
      shift 2
      ;;
    --sdk-root)
      SDK_ROOT="$2"
      shift 2
      ;;
    --local-properties)
      LOCAL_PROPERTIES_PATH="$2"
      shift 2
      ;;
    --require-sdkmanager)
      REQUIRE_SDKMANAGER="true"
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

if [[ -n "$JAVA_VERSION_EXPECTED" ]]; then
  if ! command -v java >/dev/null 2>&1; then
    echo "::error::java not found in PATH" >&2
    exit 1
  fi
  JAVA_DETECTED="$(java -version 2>&1 | awk -F '\"' '/version/ {print $2}' | cut -d. -f1)"
  echo "Detected Java major: ${JAVA_DETECTED} (expected: ${JAVA_VERSION_EXPECTED})"
  if [[ "$JAVA_DETECTED" != "$JAVA_VERSION_EXPECTED" ]]; then
    echo "::error::Java version mismatch: expected ${JAVA_VERSION_EXPECTED}, got ${JAVA_DETECTED}" >&2
    exit 1
  fi
fi

if [[ -z "$SDK_ROOT" ]]; then
  for candidate in "${DEFAULT_SDK_FALLBACKS[@]}"; do
    if [[ -d "$candidate" ]]; then
      SDK_ROOT="$candidate"
      echo "ANDROID_SDK_ROOT/ANDROID_HOME not set; using fallback SDK root: ${SDK_ROOT}"
      break
    fi
  done
fi

if [[ -z "$SDK_ROOT" ]]; then
  echo "::error::ANDROID_SDK_ROOT/ANDROID_HOME not defined, --sdk-root not provided, and no fallback SDK root found (${DEFAULT_SDK_FALLBACKS[*]})" >&2
  exit 1
fi

if [[ ! -d "$SDK_ROOT" ]]; then
  echo "::error::Android SDK root does not exist: $SDK_ROOT" >&2
  exit 1
fi

if [[ "$REQUIRE_SDKMANAGER" == "true" ]] && ! command -v sdkmanager >/dev/null 2>&1; then
  echo "::error::sdkmanager not found in PATH" >&2
  exit 1
fi

if [[ -f "$LOCAL_PROPERTIES_PATH" ]]; then
  if grep -qE '^sdk\.dir=' "$LOCAL_PROPERTIES_PATH"; then
    tmp_file="$(mktemp)"
    awk -v sdk_dir="$SDK_ROOT" '
      BEGIN {
        replaced = 0
      }
      /^sdk\.dir=/ {
        if (!replaced) {
          print "sdk.dir=" sdk_dir
          replaced = 1
        }
        next
      }
      {
        print
      }
      END {
        if (!replaced) {
          print "sdk.dir=" sdk_dir
        }
      }
    ' "$LOCAL_PROPERTIES_PATH" > "$tmp_file"
    mv "$tmp_file" "$LOCAL_PROPERTIES_PATH"
    echo "Updated sdk.dir in ${LOCAL_PROPERTIES_PATH} to ${SDK_ROOT}"
  else
    printf '\nsdk.dir=%s\n' "$SDK_ROOT" >> "$LOCAL_PROPERTIES_PATH"
    echo "Added sdk.dir to ${LOCAL_PROPERTIES_PATH} with value ${SDK_ROOT}"
  fi
else
  printf 'sdk.dir=%s\n' "$SDK_ROOT" > "$LOCAL_PROPERTIES_PATH"
  echo "Created ${LOCAL_PROPERTIES_PATH} with sdk.dir=${SDK_ROOT}"
fi
