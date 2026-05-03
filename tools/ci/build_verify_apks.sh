#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "$REPO_ROOT"

SIGNED_KEYSTORE="${SIGNED_KEYSTORE:-$REPO_ROOT/.ci/release-keystore.jks}"
SIGNED_STORE_PASSWORD="${SIGNED_STORE_PASSWORD:-android}"
SIGNED_KEY_ALIAS="${SIGNED_KEY_ALIAS:-androiddebugkey}"
SIGNED_KEY_PASSWORD="${SIGNED_KEY_PASSWORD:-android}"

mkdir -p "$(dirname "$SIGNED_KEYSTORE")"

./tools/ci/prepare_android_env.sh

if [[ ! -f "$SIGNED_KEYSTORE" ]]; then
  keytool -genkeypair -v -storetype JKS -keystore "$SIGNED_KEYSTORE" -storepass "$SIGNED_STORE_PASSWORD" -alias "$SIGNED_KEY_ALIAS" -keypass "$SIGNED_KEY_PASSWORD" -keyalg RSA -keysize 2048 -validity 3650 -dname "CN=Vectras CI,O=Vectras,C=US"
fi

./gradlew --no-daemon clean :app:assembleRelease -Psigning_mode=unsigned -PCI_INTERNAL_VALIDATION=true
./gradlew --no-daemon :app:assembleRelease -Psigning_mode=signed -PciRelease=true \
  -Pandroid.injected.signing.store.file="$SIGNED_KEYSTORE" \
  -Pandroid.injected.signing.store.password="$SIGNED_STORE_PASSWORD" \
  -Pandroid.injected.signing.key.alias="$SIGNED_KEY_ALIAS" \
  -Pandroid.injected.signing.key.password="$SIGNED_KEY_PASSWORD"

UNSIGNED_APK="$REPO_ROOT/app/build/outputs/apk/release/app-release-unsigned.apk"
SIGNED_APK="$REPO_ROOT/app/build/outputs/apk/release/app-release.apk"

for apk in "$UNSIGNED_APK" "$SIGNED_APK"; do
  [[ -f "$apk" ]] || { echo "APK não encontrado: $apk" >&2; exit 1; }
  apksigner verify --verbose "$apk"
  echo "$(basename "$apk") size=$(stat -c%s "$apk")"
  unzip -l "$apk" | awk '/lib\/arm64-v8a\// {a64=1} /lib\/armeabi-v7a\// {a32=1} END{if(!a64||!a32){exit 1}}'
done

sha256sum "$UNSIGNED_APK" "$SIGNED_APK"

if cmp -s "$UNSIGNED_APK" "$SIGNED_APK"; then
  echo "ERRO: APK assinado e unsigned idênticos" >&2
  exit 1
fi

python3 - <<'PY'
import os
u='app/build/outputs/apk/release/app-release-unsigned.apk'
s='app/build/outputs/apk/release/app-release.apk'
us=os.path.getsize(u)
ss=os.path.getsize(s)
print(f'diff_bytes={ss-us}')
PY
