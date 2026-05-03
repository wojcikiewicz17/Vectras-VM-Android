#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:-ci-artifacts/apk-report}"
mkdir -p "$OUT_DIR"
REPORT="$OUT_DIR/apk_report.txt"
JSON="$OUT_DIR/apk_report.json"

mapfile -t apks < <(find app/build/outputs/apk -type f -name '*.apk' 2>/dev/null | sort)
if [[ ${#apks[@]} -eq 0 ]]; then
  echo "Nenhum APK encontrado em app/build/outputs/apk" >&2
  exit 1
fi

python3 - <<'PY' "${apks[@]}" "$REPORT" "$JSON"
import json, os, subprocess, sys, zipfile
apks = sys.argv[1:-2]
report = sys.argv[-2]
json_path = sys.argv[-1]
rows=[]
for apk in apks:
    size=os.path.getsize(apk)
    sha=subprocess.check_output(["sha256sum", apk], text=True).split()[0]
    with zipfile.ZipFile(apk) as z:
      names=z.namelist()
    has_a32=any(n.startswith("lib/armeabi-v7a/") for n in names)
    has_a64=any(n.startswith("lib/arm64-v8a/") for n in names)
    signed=not apk.endswith("-unsigned.apk")
    rows.append({"apk":apk,"size_bytes":size,"sha256":sha,"has_armeabi_v7a":has_a32,"has_arm64_v8a":has_a64,"is_signed_filename":signed})

rows.sort(key=lambda r:r["apk"])
with open(report,"w",encoding="utf-8") as f:
    f.write("APK REPORT\n")
    for r in rows:
        f.write(f"{r['apk']}\n")
        f.write(f"  size_bytes={r['size_bytes']}\n")
        f.write(f"  sha256={r['sha256']}\n")
        f.write(f"  has_armeabi_v7a={r['has_armeabi_v7a']}\n")
        f.write(f"  has_arm64_v8a={r['has_arm64_v8a']}\n")
        f.write(f"  is_signed_filename={r['is_signed_filename']}\n")

unsigned=[r for r in rows if r['apk'].endswith('-unsigned.apk')]
signed=[r for r in rows if not r['apk'].endswith('-unsigned.apk')]
if unsigned and signed:
    rows.append({"delta_signed_minus_unsigned_bytes": signed[0]['size_bytes']-unsigned[0]['size_bytes']})
with open(json_path,"w",encoding="utf-8") as jf:
    json.dump(rows,jf,indent=2)

if not any(r.get('has_armeabi_v7a') for r in rows if 'apk' in r):
    raise SystemExit("Nenhum APK com lib/armeabi-v7a detectado")
if not any(r.get('has_arm64_v8a') for r in rows if 'apk' in r):
    raise SystemExit("Nenhum APK com lib/arm64-v8a detectado")
PY

cat "$REPORT"
