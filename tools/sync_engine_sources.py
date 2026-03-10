#!/usr/bin/env python3
"""Generate and verify Make-compatible engine source manifests from CMake manifest."""

from __future__ import annotations

import argparse
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
CMK_MANIFEST = ROOT / "engine" / "rmr" / "sources.cmake"
MK_MANIFEST = ROOT / "engine" / "rmr" / "sources.mk"

SET_PATTERN = re.compile(r"^set\((\w+)\s*$")


def parse_cmake_sets(path: pathlib.Path) -> dict[str, list[str]]:
    sets: dict[str, list[str]] = {}
    current_name: str | None = None
    current_values: list[str] = []

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.split("#", 1)[0].strip()
        if not line:
            continue

        if current_name is None:
            m = SET_PATTERN.match(line)
            if m:
                current_name = m.group(1)
                current_values = []
            continue

        if line == ")":
            sets[current_name] = current_values
            current_name = None
            current_values = []
            continue

        current_values.append(line)

    if current_name is not None:
        raise ValueError(f"Unterminated set() block for {current_name} in {path}")

    return sets


def render_make_manifest(data: dict[str, list[str]]) -> str:
    keys = [
        "RMR_ENGINE_CORE_SOURCES",
        "RMR_ENGINE_POLICY_SOURCES",
        "RMR_ENGINE_ASM_X86_64_LOWLEVEL_SOURCES",
        "RMR_ENGINE_ASM_X86_64_CASM_SOURCES",
        "RMR_ENGINE_ASM_ARM64_SOURCES",
        "RMR_ENGINE_ASM_RISCV64_SOURCES",
    ]

    out: list[str] = [
        "# Auto-generated from engine/rmr/sources.cmake by tools/sync_engine_sources.py",
        "# Do not edit directly.",
        "",
    ]

    for key in keys:
        values = data.get(key, [])
        out.append(f"{key} :=")
        for value in values:
            out.append(f"{key} += {value}")
        out.append("")

    out.append("ENGINE_CORE_SRCS := $(RMR_ENGINE_CORE_SOURCES)")
    out.append("ENGINE_POLICY_SRCS := $(RMR_ENGINE_POLICY_SOURCES)")
    out.append("ENGINE_ASM_X86_64_LOWLEVEL_SRCS := $(RMR_ENGINE_ASM_X86_64_LOWLEVEL_SOURCES)")
    out.append("ENGINE_ASM_X86_64_CASM_SRCS := $(RMR_ENGINE_ASM_X86_64_CASM_SOURCES)")
    out.append("ENGINE_ASM_ARM64_SRCS := $(RMR_ENGINE_ASM_ARM64_SOURCES)")
    out.append("ENGINE_ASM_RISCV64_SRCS := $(RMR_ENGINE_ASM_RISCV64_SOURCES)")
    out.append("")

    return "\n".join(out)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="fail if generated file differs")
    args = parser.parse_args()

    manifest = parse_cmake_sets(CMK_MANIFEST)
    generated = render_make_manifest(manifest)

    if args.check:
        current = MK_MANIFEST.read_text(encoding="utf-8") if MK_MANIFEST.exists() else ""
        if current != generated:
            print("engine source manifest is out of sync: run tools/sync_engine_sources.py", file=sys.stderr)
            return 1
        return 0

    MK_MANIFEST.write_text(generated, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
