#!/usr/bin/env python3
"""Checks ABI policy alignment between gradle.properties and tools/qemu_launch.yml."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GRADLE_PROPERTIES = ROOT / "gradle.properties"
QEMU_LAUNCH = ROOT / "tools" / "qemu_launch.yml"

ALLOWED_SCOPES = {"official_distribution", "internal_validation"}
POLICY_TO_SCOPE = {
    "arm64-only": "official_distribution",
}


def parse_gradle_properties(path: Path) -> tuple[str, list[str]]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()

    policy = values.get("APP_ABI_POLICY", "")
    supported_abis_raw = values.get("SUPPORTED_ABIS", "")
    supported_abis = [abi.strip() for abi in supported_abis_raw.split(",") if abi.strip()]
    return policy, supported_abis


def parse_qemu_abi_scopes(path: Path) -> tuple[str, dict[str, list[str]]]:
    lines = path.read_text(encoding="utf-8").splitlines()

    in_abi_filters = False
    active_scope_list: str | None = None
    selected_scope = ""
    scope_abis: dict[str, list[str]] = {"official_distribution": [], "internal_validation": []}

    for line in lines:
        if not in_abi_filters and re.match(r"^  abi_filters:\s*$", line):
            in_abi_filters = True
            continue

        if in_abi_filters and re.match(r"^  [a-zA-Z0-9_]+:", line):
            break

        if not in_abi_filters:
            continue

        scope_match = re.match(r"^    scope:\s*([a-z_]+)\s*$", line)
        if scope_match:
            selected_scope = scope_match.group(1)
            active_scope_list = None
            continue

        list_header_match = re.match(r"^    (official_distribution|internal_validation):\s*$", line)
        if list_header_match:
            active_scope_list = list_header_match.group(1)
            continue

        item_match = re.match(r"^      -\s*([^\s#]+)\s*$", line)
        if item_match and active_scope_list is not None:
            scope_abis[active_scope_list].append(item_match.group(1))

    return selected_scope, scope_abis


def fail(message: str) -> int:
    print(f"[check_abi_policy_alignment] FAIL: {message}")
    return 1


def main() -> int:
    if not GRADLE_PROPERTIES.exists():
        return fail(f"missing file: {GRADLE_PROPERTIES}")
    if not QEMU_LAUNCH.exists():
        return fail(f"missing file: {QEMU_LAUNCH}")

    policy, gradle_supported_abis = parse_gradle_properties(GRADLE_PROPERTIES)
    qemu_scope, qemu_scope_abis = parse_qemu_abi_scopes(QEMU_LAUNCH)

    if policy not in {"arm64-only"}:
        return fail(f"unsupported APP_ABI_POLICY={policy!r}")

    if qemu_scope not in ALLOWED_SCOPES:
        return fail(f"tools/qemu_launch.yml abi_filters.scope must be one of {sorted(ALLOWED_SCOPES)}, got {qemu_scope!r}")

    for scope_name in sorted(ALLOWED_SCOPES):
        if not qemu_scope_abis.get(scope_name):
            return fail(f"tools/qemu_launch.yml abi_filters.{scope_name} is empty")

    if qemu_scope != "official_distribution":
        return fail("tools/qemu_launch.yml abi_filters.scope must default to official_distribution")

    expected_abis: list[str]
    mapped_scope = POLICY_TO_SCOPE[policy]
    expected_abis = qemu_scope_abis[mapped_scope]

    if gradle_supported_abis != expected_abis:
        return fail(
            f"gradle SUPPORTED_ABIS ({','.join(gradle_supported_abis)}) does not match expected "
            f"ABI list for policy {policy}: {','.join(expected_abis)}"
        )

    print("[check_abi_policy_alignment] OK")
    print(f"  policy={policy}")
    print(f"  supported_abis={','.join(gradle_supported_abis)}")
    print(f"  qemu_scope={qemu_scope}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
