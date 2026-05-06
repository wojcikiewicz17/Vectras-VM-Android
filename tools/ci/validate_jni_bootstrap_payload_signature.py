#!/usr/bin/env python3
import argparse
from pathlib import Path
import re
import sys

SIG_PATTERNS = [
    re.compile(r"0x50\s*,\s*0x4[bB]\s*,\s*0x03\s*,\s*0x04"),
    re.compile(r"0x50\s*,\s*0x4[bB]\s*,\s*0x05\s*,\s*0x06"),
    re.compile(r"0x50\s*,\s*0x4[bB]\s*,\s*0x07\s*,\s*0x08"),
]

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--mode", default="assets-loader")
    ap.add_argument("--payload", action="append", default=[])
    args = ap.parse_args()

    mode = (args.mode or "assets-loader").strip().lower()
    if mode != "embedded-zip":
        print(f"[jni-bootstrap-pk] skip: mode={mode}")
        return 0

    candidates = [Path(p) for p in args.payload]
    existing = [p for p in candidates if p.is_file()]
    if not existing:
        print("[jni-bootstrap-pk] fail: embedded-zip exige payload JNI gerado (.c/.S).", file=sys.stderr)
        return 1

    for path in existing:
        text = path.read_text(encoding="utf-8", errors="ignore")
        if any(p.search(text) for p in SIG_PATTERNS):
            print(f"[jni-bootstrap-pk] ok: assinatura PK detectada em {path}")
            return 0

    print("[jni-bootstrap-pk] fail: payload JNI sem assinatura PK válida (PK03/PK05/PK07).", file=sys.stderr)
    return 1

if __name__ == "__main__":
    raise SystemExit(main())
