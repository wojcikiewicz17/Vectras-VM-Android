#!/usr/bin/env python3
"""Resolve canonical ABI profile contract for CI workflows."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONTRACT_PATH = ROOT / "tools" / "ci" / "abi_profiles_contract.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", required=True, help="ABI profile name from workflow input")
    parser.add_argument(
        "--allow-generic-fallback",
        action="store_true",
        help="Use adaptive generic fallback when profile is unknown",
    )
    parser.add_argument(
        "--format",
        choices=["shell", "json"],
        default="shell",
        help="Output format",
    )
    return parser.parse_args()


def load_contract() -> dict:
    return json.loads(CONTRACT_PATH.read_text(encoding="utf-8"))


def normalize_profile(raw_profile: str, aliases: dict[str, str]) -> str:
    profile = raw_profile.strip()
    return aliases.get(profile, profile)


def infer_generic_profile(raw_profile: str) -> str:
    token = raw_profile.lower().replace("-", "_")
    if any(k in token for k in ["5abi", "riscv"]):
        return "internal_5abi"
    if any(k in token for k in ["4abi", "x86"]):
        return "internal_4abi"
    if any(k in token for k in ["arm32", "dual", "32_64"]):
        return "internal_arm32_arm64"
    if "arm64" in token or "official" in token:
        return "official_arm64"
    return "generic"


def render_shell(values: dict[str, str]) -> str:
    lines = []
    for key, value in values.items():
        lines.append(f"{key}={value}")
    return "\n".join(lines)


def main() -> int:
    args = parse_args()
    contract = load_contract()
    profiles = contract["profiles"]
    aliases = contract.get("aliases", {})

    normalized = normalize_profile(args.profile, aliases)
    resolved_source = "official"

    if normalized not in profiles:
        if not args.allow_generic_fallback:
            known = ", ".join(sorted(profiles.keys()))
            print(f"unsupported abi_profile={args.profile}; known profiles: {known}", file=sys.stderr)
            return 1
        normalized = infer_generic_profile(args.profile)
        resolved_source = "generic_adaptive"

    config = profiles[normalized]
    output = {
      "abi_profile": normalized,
      "app_abi_policy": config["app_abi_policy"],
      "supported_abis": ",".join(config["supported_abis"]),
      "ci_internal_validation": str(config["ci_internal_validation"]).lower(),
      "profile_channel": config["channel"],
      "resolved_source": resolved_source,
    }

    if args.format == "json":
        print(json.dumps(output))
    else:
        print(render_shell(output))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
