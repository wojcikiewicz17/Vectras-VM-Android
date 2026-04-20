#!/usr/bin/env python3
"""Validate lowlevel ABI contract and no-implicit-libc policy."""

from __future__ import annotations

import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[2]
CONTRACT = ROOT / "tools" / "ci" / "lowlevel_abi_contract.json"
ABI_HEADER = ROOT / "app" / "src" / "main" / "cpp" / "lowlevel_abi.h"
ABI_SOURCE = ROOT / "app" / "src" / "main" / "cpp" / "lowlevel_abi.c"

FORBIDDEN_INCLUDES = ("#include <stdlib.h>", "#include <libc", "#include <stdio.h>")
FORBIDDEN_SYMBOLS = ("malloc(", "calloc(", "realloc(", "free(")


def fail(msg: str) -> int:
    print(f"LOWLEVEL_ABI_CONTRACT_VIOLATION: {msg}")
    return 1


def _check_forbidden(path: Path, content: str) -> list[str]:
    violations: list[str] = []
    for token in FORBIDDEN_INCLUDES:
        if token in content:
            violations.append(f"{path}: forbidden include detected ({token})")
    for token in FORBIDDEN_SYMBOLS:
        if token in content:
            violations.append(f"{path}: forbidden stdlib symbol detected ({token})")
    return violations


def main() -> int:
    if not CONTRACT.exists():
        return fail(f"missing contract file: {CONTRACT}")

    data = json.loads(CONTRACT.read_text(encoding="utf-8"))
    architectures = data.get("architectures", [])
    if len(architectures) != 7:
        return fail(f"expected exactly 7 architectures, found {len(architectures)}")

    required_keys = {
        "name",
        "abi_version",
        "stack_alignment",
        "frame_policy",
        "calling_convention",
        "prologue",
        "epilogue",
        "syscall_policy",
    }
    for arch in architectures:
        missing = required_keys.difference(arch.keys())
        if missing:
            return fail(f"architecture '{arch.get('name', '<unknown>')}' missing keys: {sorted(missing)}")

    interoperability = data.get("interoperability", [])
    if not interoperability:
        return fail("interoperability table is empty")

    for path in (ABI_HEADER, ABI_SOURCE):
        if not path.exists():
            return fail(f"missing lowlevel ABI source: {path}")
        content = path.read_text(encoding="utf-8")
        violations = _check_forbidden(path, content)
        if violations:
            return fail("; ".join(violations))

    print(
        "LOWLEVEL_ABI_CONTRACT_OK: architectures=7, interoperability-table=present, no implicit libc/stdlib in lowlevel ABI files"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
