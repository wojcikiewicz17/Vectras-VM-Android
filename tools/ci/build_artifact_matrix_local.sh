#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

GRADLE_CMD="${GRADLE_CMD:-./tools/gradle_with_jdk21.sh}"
OUT_DIR="${OUT_DIR:-${ROOT_DIR}/artifacts/local-matrix}"
KEYSTORE_PATH="${KEYSTORE_PATH:-${OUT_DIR}/internal-release.jks}"
KEY_ALIAS="${KEY_ALIAS:-vectras-internal}"
KEYSTORE_PASS="${KEYSTORE_PASS:-changeit}"
KEY_PASS="${KEY_PASS:-changeit}"
KEY_DNAME="${KEY_DNAME:-CN=Vectras Internal,O=Vectras,OU=Engineering,L=San Francisco,ST=CA,C=US}"

mkdir -p "${OUT_DIR}"

log() {
  printf '[local-matrix] %s\n' "$*"
}

ensure_keystore() {
  if [[ -f "${KEYSTORE_PATH}" ]]; then
    log "keystore interno já existe em ${KEYSTORE_PATH}"
    return 0
  fi

  log "gerando keystore interno para validação local assinada"
  keytool -genkeypair \
    -keystore "${KEYSTORE_PATH}" \
    -storepass "${KEYSTORE_PASS}" \
    -keypass "${KEY_PASS}" \
    -alias "${KEY_ALIAS}" \
    -keyalg RSA \
    -keysize 4096 \
    -validity 3650 \
    -dname "${KEY_DNAME}" >/dev/null
}

copy_artifacts() {
  local flavor="$1"
  local src_apk="app/build/outputs/apk/release/app-release.apk"
  local src_aab="app/build/outputs/bundle/release/app-release.aab"
  local dst_apk="${OUT_DIR}/app-release-${flavor}.apk"
  local dst_aab="${OUT_DIR}/app-release-${flavor}.aab"

  cp -f "${src_apk}" "${dst_apk}"
  cp -f "${src_aab}" "${dst_aab}"

  sha256sum "${dst_apk}" "${dst_aab}" > "${OUT_DIR}/sha256-${flavor}.txt"
}

write_manifest() {
  cat > "${OUT_DIR}/manifest.json" <<JSON
{
  "generated_at_utc": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "profile": "internal_arm32_arm64",
  "supported_abis": ["arm64-v8a", "armeabi-v7a"],
  "artifacts": {
    "unsigned": {
      "apk": "app-release-unsigned.apk",
      "aab": "app-release-unsigned.aab",
      "sha256": "sha256-unsigned.txt"
    },
    "signed_internal": {
      "apk": "app-release-signed-internal.apk",
      "aab": "app-release-signed-internal.aab",
      "sha256": "sha256-signed-internal.txt",
      "keystore": "internal-release.jks"
    }
  }
}
JSON
}

log "limpando build"
"${GRADLE_CMD}" clean

log "build release unsigned (internal arm32+arm64)"
"${GRADLE_CMD}" :app:assembleRelease :app:bundleRelease \
  -PciRelease=false \
  -Psigning_mode=unsigned \
  -PCI_INTERNAL_VALIDATION=true \
  -PAPP_ABI_POLICY=arm32-arm64 \
  -PSUPPORTED_ABIS=arm64-v8a,armeabi-v7a
copy_artifacts "unsigned"

ensure_keystore

log "build release signed interno (internal arm32+arm64)"
"${GRADLE_CMD}" :app:assembleRelease :app:bundleRelease \
  -PciRelease=true \
  -Psigning_mode=signed \
  -PCI_INTERNAL_VALIDATION=true \
  -PAPP_ABI_POLICY=arm32-arm64 \
  -PSUPPORTED_ABIS=arm64-v8a,armeabi-v7a \
  -Pandroid.injected.signing.store.file="${KEYSTORE_PATH}" \
  -Pandroid.injected.signing.store.password="${KEYSTORE_PASS}" \
  -Pandroid.injected.signing.key.alias="${KEY_ALIAS}" \
  -Pandroid.injected.signing.key.password="${KEY_PASS}"
copy_artifacts "signed-internal"

write_manifest

log "artefatos gerados em ${OUT_DIR}"
