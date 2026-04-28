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
  local keytool_cmd="${KEYTOOL_CMD:-keytool}"
  local use_openssl_fallback="false"
  if ! command -v "${keytool_cmd}" >/dev/null 2>&1; then
    if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/keytool" ]]; then
      keytool_cmd="${JAVA_HOME}/bin/keytool"
    else
      use_openssl_fallback="true"
      if ! command -v openssl >/dev/null 2>&1; then
        echo "[local-matrix][error] keytool e openssl indisponíveis para gerar keystore local." >&2
        exit 1
      fi
    fi
  fi

  if [[ -f "${KEYSTORE_PATH}" ]]; then
    log "keystore interno já existe em ${KEYSTORE_PATH}"
    return 0
  fi

  if [[ "${use_openssl_fallback}" == "false" ]]; then
    log "gerando keystore interno para validação local assinada (keytool)"
    "${keytool_cmd}" -genkeypair \
      -keystore "${KEYSTORE_PATH}" \
      -storepass "${KEYSTORE_PASS}" \
      -keypass "${KEY_PASS}" \
      -alias "${KEY_ALIAS}" \
      -keyalg RSA \
      -keysize 4096 \
      -validity 3650 \
      -dname "${KEY_DNAME}" >/dev/null
  else
    log "gerando keystore interno para validação local assinada (fallback openssl/pkcs12)"
    local tmp_dir
    tmp_dir="$(mktemp -d)"
    trap 'rm -rf "${tmp_dir}"' RETURN
    local key_file="${tmp_dir}/internal-key.pem"
    local cert_file="${tmp_dir}/internal-cert.pem"
    local subject="/CN=Vectras Internal/O=Vectras/OU=Engineering/L=San Francisco/ST=CA/C=US"
    openssl req -x509 -newkey rsa:4096 \
      -keyout "${key_file}" \
      -out "${cert_file}" \
      -days 3650 \
      -sha256 \
      -passout "pass:${KEY_PASS}" \
      -subj "${subject}" >/dev/null 2>&1
    openssl pkcs12 -export \
      -name "${KEY_ALIAS}" \
      -inkey "${key_file}" \
      -passin "pass:${KEY_PASS}" \
      -in "${cert_file}" \
      -out "${KEYSTORE_PATH}" \
      -passout "pass:${KEYSTORE_PASS}" >/dev/null 2>&1
  fi
}

copy_artifacts() {
  local flavor="$1"
  local src_apk_signed="app/build/outputs/apk/release/app-release.apk"
  local src_apk_unsigned="app/build/outputs/apk/release/app-release-unsigned.apk"
  local src_apk=""
  local src_aab="app/build/outputs/bundle/release/app-release.aab"
  local dst_apk="${OUT_DIR}/app-release-${flavor}.apk"
  local dst_aab="${OUT_DIR}/app-release-${flavor}.aab"

  if [[ -f "${src_apk_signed}" ]]; then
    src_apk="${src_apk_signed}"
  elif [[ -f "${src_apk_unsigned}" ]]; then
    src_apk="${src_apk_unsigned}"
  else
    echo "[local-matrix][error] APK release não encontrado (${src_apk_signed} | ${src_apk_unsigned})" >&2
    exit 1
  fi

  if [[ ! -f "${src_aab}" ]]; then
    echo "[local-matrix][error] AAB release não encontrado (${src_aab})" >&2
    exit 1
  fi

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
