#!/usr/bin/env python3
import argparse
import hashlib
import zipfile
from pathlib import Path

REQ = [
    'lib/arm64-v8a/libtermux-bootstrap.so',
    'lib/armeabi-v7a/libtermux-bootstrap.so',
    'lib/arm64-v8a/libvectra_core_accel.so',
    'lib/armeabi-v7a/libvectra_core_accel.so',
    'assets/bootstrap/arm64-v8a.tar',
    'assets/bootstrap/armeabi-v7a.tar',
    'assets/bootstrap/loader.apk',
]

def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

ap = argparse.ArgumentParser()
ap.add_argument('--apk', required=True)
ap.add_argument('--out', default='reports/APK_ABI_BOOTSTRAP_INVENTORY.md')
a = ap.parse_args()

apk = Path(a.apk)
out = Path(a.out)
out.parent.mkdir(parents=True, exist_ok=True)
if not apk.exists():
    raise SystemExit(f'APK not found: {apk}')

ok = True
with zipfile.ZipFile(apk) as z:
    names = set(z.namelist())
    lines = ['# APK_ABI_BOOTSTRAP_INVENTORY', '', f'- APK: `{apk}`', '']
    for r in REQ:
        has = r in names
        ok = ok and has
        if has:
            raw = z.read(r)
            lines.append(f"- OK `{r}` size={len(raw)} sha256={sha256(raw)[:16]}")
        else:
            lines.append(f"- MISSING `{r}`")
    lines.append('')
    lines.append(f'- STATUS: {"PASS" if ok else "FAIL"}')

out.write_text('\n'.join(lines) + '\n', encoding='utf-8')
print(out)
if not ok:
    raise SystemExit(2)
