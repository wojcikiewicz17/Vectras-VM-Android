#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

usage() {
  cat <<'USAGE'
Usage: bootstrap_local_android_sdk.sh [--sdk-root <path>] [--cmdline-tools-url <url>] [--skip-licenses]

Bootstraps a local Android SDK installation aligned with gradle.properties and writes local.properties sdk.dir.
USAGE
}

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/workspace/android-sdk}}"
CMDLINE_TOOLS_URL="${ANDROID_CMDLINE_TOOLS_URL:-https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip}"
ACCEPT_LICENSES="true"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --sdk-root)
      SDK_ROOT="$2"
      shift 2
      ;;
    --cmdline-tools-url)
      CMDLINE_TOOLS_URL="$2"
      shift 2
      ;;
    --skip-licenses)
      ACCEPT_LICENSES="false"
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

read_gradle_prop() {
  local key="$1"
  awk -F= -v key="$key" '
    /^[[:space:]]*#/ { next }
    {
      k=$1
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", k)
      if (k == key) {
        sub(/^[^=]*=/, "", $0)
        v=$0
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", v)
        print v
        exit
      }
    }
  ' "${REPO_ROOT}/gradle.properties"
}

COMPILE_API="$(read_gradle_prop 'compile.api')"
TOOLS_VERSION="$(read_gradle_prop 'tools.version')"
CMAKE_VERSION="$(read_gradle_prop 'cmake.version')"
NDK_VERSION="$(read_gradle_prop 'ndk.version')"
JAVA_VERSION="$(read_gradle_prop 'java.language.version')"
MAX_RUNTIME_JAVA_VERSION="$(read_gradle_prop 'gradle.max.runtime.java.version')"

for required_var in COMPILE_API TOOLS_VERSION CMAKE_VERSION NDK_VERSION JAVA_VERSION MAX_RUNTIME_JAVA_VERSION; do
  if [[ -z "${!required_var}" ]]; then
    echo "Missing ${required_var} in gradle.properties" >&2
    exit 1
  fi
done

ensure_java_runtime() {
  if command -v java >/dev/null 2>&1; then
    return 0
  fi

  for candidate in \
    "$HOME/.local/share/mise/installs/java/17/bin/java" \
    "$HOME/.local/share/mise/installs/java/21/bin/java" \
    "/usr/lib/jvm/java-17-openjdk-amd64/bin/java" \
    "/usr/lib/jvm/java-21-openjdk-amd64/bin/java"; do
    if [[ -x "$candidate" ]]; then
      export JAVA_HOME="$(dirname "$(dirname "$candidate")")"
      export PATH="$JAVA_HOME/bin:$PATH"
      break
    fi
  done

  if ! command -v java >/dev/null 2>&1; then
    echo "java not found in PATH and no known JDK candidate was detected" >&2
    exit 1
  fi
}

detect_java_major() {
  local java_bin
  java_bin="$(command -v java)"
  "$java_bin" -XshowSettings:properties -version 2>&1 \
    | awk -F= '/java\.specification\.version/ {gsub(/ /,"",$2); print $2; exit}'
}

assert_java_runtime_policy() {
  local java_major="$1"
  if (( java_major < JAVA_VERSION )); then
    echo "Java runtime ${java_major} is below baseline ${JAVA_VERSION}" >&2
    exit 1
  fi
  if (( java_major > MAX_RUNTIME_JAVA_VERSION )); then
    echo "Java runtime ${java_major} exceeds allowed maximum ${MAX_RUNTIME_JAVA_VERSION}" >&2
    exit 1
  fi
}

mkdir -p "${SDK_ROOT}"
export ANDROID_SDK_ROOT="${SDK_ROOT}"
export ANDROID_HOME="${SDK_ROOT}"
ensure_java_runtime
JAVA_MAJOR="$(detect_java_major)"
assert_java_runtime_policy "${JAVA_MAJOR}"
if [[ -d "${SDK_ROOT}/cmdline-tools/latest/bin" ]]; then
  export PATH="${SDK_ROOT}/cmdline-tools/latest/bin:${SDK_ROOT}/platform-tools:${PATH}"
fi

if ! command -v sdkmanager >/dev/null 2>&1; then
  echo "sdkmanager not found; installing Android command-line tools into ${SDK_ROOT}"
  tmp_zip="$(mktemp)"
  trap 'rm -f "${tmp_zip}"' EXIT
  curl -fsSL "${CMDLINE_TOOLS_URL}" -o "${tmp_zip}"
  mkdir -p "${SDK_ROOT}/cmdline-tools"
  rm -rf "${SDK_ROOT}/cmdline-tools/latest"
  mkdir -p "${SDK_ROOT}/cmdline-tools/latest"
  unzip -q -o "${tmp_zip}" -d "${SDK_ROOT}/cmdline-tools"
  if [[ -d "${SDK_ROOT}/cmdline-tools/cmdline-tools" ]]; then
    rm -rf "${SDK_ROOT}/cmdline-tools/latest"
    mv "${SDK_ROOT}/cmdline-tools/cmdline-tools" "${SDK_ROOT}/cmdline-tools/latest"
  fi
  export PATH="${SDK_ROOT}/cmdline-tools/latest/bin:${SDK_ROOT}/platform-tools:${PATH}"
else
  echo "sdkmanager found in PATH"
fi

if ! command -v sdkmanager >/dev/null 2>&1; then
  echo "sdkmanager still unavailable after bootstrap" >&2
  exit 1
fi

if [[ "${ACCEPT_LICENSES}" == "true" ]]; then
  set +o pipefail
  yes | sdkmanager --licenses >/dev/null
  set -o pipefail
fi

sdkmanager \
  "platform-tools" \
  "platforms;android-${COMPILE_API}" \
  "build-tools;${TOOLS_VERSION}" \
  "cmake;${CMAKE_VERSION}" \
  "ndk;${NDK_VERSION}"

"${SCRIPT_DIR}/prepare_android_env.sh" \
  --java-version "${JAVA_MAJOR}" \
  --sdk-root "${SDK_ROOT}" \
  --ndk-version "${NDK_VERSION}"

"${SCRIPT_DIR}/verify_android_local_properties_contract.sh"

echo "Android SDK bootstrap completed: ${SDK_ROOT}"
