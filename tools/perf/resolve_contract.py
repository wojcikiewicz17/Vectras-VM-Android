#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MANIFEST = ROOT / "tools" / "perf" / "profiles.json"


def main() -> int:
    parser = argparse.ArgumentParser(description="Resolve/validate perf CI contract from manifest.")
    parser.add_argument("--manifest", default=str(DEFAULT_MANIFEST))
    parser.add_argument("--require-consistency", action="store_true")
    parser.add_argument("--github-output", default="")
    args = parser.parse_args()

    doc = json.loads(Path(args.manifest).read_text(encoding="utf-8"))
    ci = doc.get("ci_contract", {})
    runs = ci.get("runs", doc.get("default_runs"))
    if not isinstance(runs, int) or runs < 1:
        raise SystemExit("Invalid perf CI contract: 'runs' must be integer >= 1")

    default_runs = doc.get("default_runs")
    if args.require_consistency and isinstance(default_runs, int) and default_runs != runs:
        raise SystemExit(
            f"Perf manifest contract mismatch: default_runs={default_runs} ci_contract.runs={runs}"
        )

    payload = {"perf_runs": runs, "manifest": str(Path(args.manifest).as_posix())}
    print(json.dumps(payload))

    if args.github_output:
        out = Path(args.github_output)
        with out.open("a", encoding="utf-8") as fh:
            fh.write(f"perf_runs={runs}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
