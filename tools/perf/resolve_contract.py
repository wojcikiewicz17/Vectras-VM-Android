#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MANIFEST = ROOT / "tools" / "perf" / "profiles.json"


def _validate_workflows(contract: dict, manifest_path: Path) -> None:
    workflows = contract.get("enforced_workflows", [])
    if not isinstance(workflows, list) or not workflows:
        raise SystemExit("Invalid perf CI contract: 'enforced_workflows' must be a non-empty list")

    required_tokens = [
        "tools/perf/resolve_contract.py",
        "--github-output \"$GITHUB_ENV\"",
        'tools/perf/run_suite.py --runs "$perf_runs"',
    ]

    for workflow in workflows:
        if not isinstance(workflow, str) or not workflow.strip():
            raise SystemExit("Invalid perf CI contract: all 'enforced_workflows' entries must be non-empty strings")
        wf_path = (ROOT / workflow).resolve()
        try:
            wf_path.relative_to(ROOT)
        except ValueError as exc:
            raise SystemExit(f"Workflow path escapes repository root: {workflow}") from exc

        if not wf_path.exists():
            raise SystemExit(f"Workflow listed in perf contract was not found: {workflow}")

        content = wf_path.read_text(encoding="utf-8")
        missing = [token for token in required_tokens if token not in content]
        if missing:
            raise SystemExit(
                "Workflow contract mismatch for "
                f"{workflow}: missing required tokens {missing}"
            )


def main() -> int:
    parser = argparse.ArgumentParser(description="Resolve/validate perf CI contract from manifest.")
    parser.add_argument("--manifest", default=str(DEFAULT_MANIFEST))
    parser.add_argument("--require-consistency", action="store_true")
    parser.add_argument("--validate-workflows", action="store_true")
    parser.add_argument("--github-output", default="")
    args = parser.parse_args()

    manifest = Path(args.manifest)
    doc = json.loads(manifest.read_text(encoding="utf-8"))
    ci = doc.get("ci_contract", {})
    runs = ci.get("runs")
    if not isinstance(runs, int) or runs < 1:
        raise SystemExit("Invalid perf CI contract: 'ci_contract.runs' must be integer >= 1")

    if args.require_consistency or args.validate_workflows:
        _validate_workflows(ci, manifest)

    payload = {
        "perf_runs": runs,
        "manifest": str(manifest.as_posix()),
        "enforced_workflows": ci.get("enforced_workflows", []),
    }
    print(json.dumps(payload))

    if args.github_output:
        out = Path(args.github_output)
        with out.open("a", encoding="utf-8") as fh:
            fh.write(f"perf_runs={runs}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
