#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path


def fail(msg: str) -> int:
    print(f"[java-contract] ERROR: {msg}", file=sys.stderr)
    return 1


def main() -> int:
    repo_root = Path(__file__).resolve().parents[2]
    native_fastpath = repo_root / "app/src/main/java/com/vectras/vm/core/NativeFastPath.java"

    if not native_fastpath.exists():
        return fail(f"Arquivo não encontrado: {native_fastpath}")

    content = native_fastpath.read_text(encoding="utf-8")
    pattern = re.compile(r"public\s+static\s+int\s+torusFlowChecksum\s*\(\s*int\s+seed\s*,\s*int\s+steps\s*\)")
    matches = list(pattern.finditer(content))
    if len(matches) != 1:
        return fail(
            "Assinatura de NativeFastPath.torusFlowChecksum(int,int) deve existir exatamente 1 vez; "
            f"encontrado={len(matches)} em {native_fastpath}"
        )

    print("[java-contract] OK: NativeFastPath.torusFlowChecksum(int,int) assinatura única.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
