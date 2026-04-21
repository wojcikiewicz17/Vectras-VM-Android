#!/usr/bin/env python3
from __future__ import annotations

import argparse
import fnmatch
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LOWLEVEL_PATTERNS = [
    "engine/rmr/**",
    "app/src/main/cpp/**",
    "CMakeLists.txt",
    "app/src/main/cpp/CMakeLists.txt",
    "app/build.gradle",
    "tools/ci/lowlevel_abi_contract.json",
]


def git_changed(base: str, head: str) -> list[str]:
    proc = subprocess.run(
        ["git", "diff", "--name-only", f"{base}...{head}"],
        cwd=ROOT,
        text=True,
        capture_output=True,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or "git diff failed")
    return [line.strip() for line in proc.stdout.splitlines() if line.strip()]


def is_lowlevel(path: str) -> bool:
    return any(fnmatch.fnmatch(path, pattern) for pattern in LOWLEVEL_PATTERNS)


def main() -> int:
    parser = argparse.ArgumentParser(description="Bloqueia alterações low-level sem neutralidade/ganho de perf.")
    parser.add_argument("--base-sha", required=True)
    parser.add_argument("--head-sha", required=True)
    parser.add_argument("--comparison", default="bench/results/perf/comparison.json")
    args = parser.parse_args()

    changed = git_changed(args.base_sha, args.head_sha)
    lowlevel = [path for path in changed if is_lowlevel(path)]

    comparison_path = ROOT / args.comparison
    if not comparison_path.exists():
        raise SystemExit("comparison report not found")

    comparison = json.loads(comparison_path.read_text(encoding="utf-8"))
    gate = comparison.get("gate", "fail")

    output = {
        "base_sha": args.base_sha,
        "head_sha": args.head_sha,
        "changed_files": changed,
        "lowlevel_files": lowlevel,
        "comparison_gate": gate,
        "result": "pass",
        "reason": "no_lowlevel_change",
    }

    if lowlevel:
        output["reason"] = "lowlevel_change_requires_perf_neutral_or_gain"
        if gate != "pass":
            output["result"] = "fail"

    print(json.dumps(output, indent=2))
    return 1 if output["result"] == "fail" else 0


if __name__ == "__main__":
    raise SystemExit(main())
