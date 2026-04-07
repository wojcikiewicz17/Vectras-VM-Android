#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: validate_pipeline_directories.sh [--profile <host|android|orchestrator>]
USAGE
}

PROFILE="android"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --profile)
      PROFILE="$2"
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

required_paths=()

case "$PROFILE" in
  host)
    required_paths=(
      "Makefile"
      "CMakeLists.txt"
      "engine"
      "tools/check_overlay_zip_duplicates.sh"
      "tools/check_docs_reference_commit.sh"
      "tools/check_no_firebasestorage_links.sh"
    )
    ;;
  android)
    required_paths=(
      "app"
      "shell-loader"
      "terminal-emulator"
      "terminal-view"
      "gradlew"
      "gradle/wrapper/gradle-wrapper.jar"
      "tools/gradle_with_jdk21.sh"
      "tools/ci/prepare_android_env.sh"
      "tools/ci/materialize_firebase_config.sh"
      "tools/ci/prepare_release_signing.sh"
      "tools/ci/validate_expected_android_artifacts.sh"
      "tools/ci/validate_android_sdk_alignment.sh"
    )
    ;;
  orchestrator)
    required_paths=(
      ".github/workflows/pipeline-orchestrator.yml"
      ".github/workflows/ci.yml"
      ".github/workflows/android.yml"
    )
    ;;
  *)
    echo "::error::Invalid profile: ${PROFILE}" >&2
    exit 1
    ;;
esac

missing=()
for path in "${required_paths[@]}"; do
  if [[ ! -e "${path}" ]]; then
    missing+=("${path}")
  fi
done

if [[ ${#missing[@]} -gt 0 ]]; then
  echo "::error::Missing required paths for profile '${PROFILE}': ${missing[*]}" >&2
  exit 1
fi

echo "Pipeline directory contract is valid for profile=${PROFILE}"
