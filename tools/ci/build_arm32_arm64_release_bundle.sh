#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

mode="${1:-unsigned}"
if [[ "${mode}" != "unsigned" && "${mode}" != "signed" ]]; then
  echo "Usage: $0 [unsigned|signed]" >&2
  exit 2
fi

common_flags=(
  -PAPP_ABI_POLICY=arm32-arm64
  -PSUPPORTED_ABIS=arm64-v8a,armeabi-v7a
  -PCI_INTERNAL_VALIDATION=true
  -PALLOW_PLACEHOLDER_FIREBASE_FOR_RELEASE=true
)

if [[ "${mode}" == "unsigned" ]]; then
  ./tools/gradle_with_jdk21.sh \
    "${common_flags[@]}" \
    -Psigning_mode=unsigned \
    -PciRelease=false \
    -PALLOW_UNSIGNED_RELEASE=true \
    :app:assembleRelease
else
  ./tools/ci/prepare_release_signing.sh --mode signed
  ./tools/gradle_with_jdk21.sh \
    "${common_flags[@]}" \
    -Psigning_mode=signed \
    -PciRelease=true \
    :app:assembleRelease
fi

./tools/gradle_with_jdk21.sh \
  -PartifactVariants=release \
  -Pworkflow=local \
  -Plane=arm32-arm64-${mode} \
  -Pabi_profile=official_arm32_arm64 \
  -PAPP_ABI_POLICY=arm32-arm64 \
  -PSUPPORTED_ABIS=arm64-v8a,armeabi-v7a \
  -Psigning_mode="${mode}" \
  :app:verifyDeliveredCompiledArtifacts

