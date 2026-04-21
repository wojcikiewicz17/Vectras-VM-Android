#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path


def _collect_local_runs(root: Path) -> list[tuple[str, int]]:
    collected: list[tuple[str, int]] = []
    for json_file in sorted(root.rglob("*.json")):
        try:
            payload = json.loads(json_file.read_text(encoding="utf-8"))
        except Exception:
            continue

        if json_file.name == "suite.json":
            run_candidates: list[int] = []
            ci_runs = payload.get("ci_contract", {}).get("runs")
            if isinstance(ci_runs, int):
                run_candidates.append(ci_runs)
            for profile in payload.get("profiles", []):
                runs = profile.get("runs")
                if isinstance(runs, int):
                    run_candidates.append(runs)
            for runs in run_candidates:
                collected.append((str(json_file), runs))
            continue

        if json_file.name in {"manifest.json", "artifact-manifest.json"}:
            runs = payload.get("runs", payload.get("perf_runs"))
            if isinstance(runs, int):
                collected.append((str(json_file), runs))

    return collected


def _collect_zip_runs(root: Path) -> list[tuple[str, int]]:
    collected: list[tuple[str, int]] = []
    for zip_path in sorted(root.rglob("*.zip")):
        try:
            with zipfile.ZipFile(zip_path, "r") as zf:
                for member in zf.namelist():
                    if not member.endswith(("suite.json", "manifest.json", "artifact-manifest.json")):
                        continue
                    with zf.open(member) as fh:
                        payload = json.loads(fh.read().decode("utf-8"))
                    if member.endswith("suite.json"):
                        ci_runs = payload.get("ci_contract", {}).get("runs")
                        if isinstance(ci_runs, int):
                            collected.append((f"{zip_path}!{member}", ci_runs))
                        for profile in payload.get("profiles", []):
                            runs = profile.get("runs")
                            if isinstance(runs, int):
                                collected.append((f"{zip_path}!{member}", runs))
                    else:
                        runs = payload.get("runs", payload.get("perf_runs"))
                        if isinstance(runs, int):
                            collected.append((f"{zip_path}!{member}", runs))
        except zipfile.BadZipFile as exc:
            raise SystemExit(f"Invalid zip artifact: {zip_path}: {exc}") from exc
    return collected


def main() -> int:
    parser = argparse.ArgumentParser(description="Valida consistência de perf runs em artefatos locais e zips.")
    parser.add_argument("--artifacts-root", required=True)
    parser.add_argument("--expected-runs", type=int, required=True)
    args = parser.parse_args()

    root = Path(args.artifacts_root)
    if not root.exists():
        raise SystemExit(f"Artifacts root not found: {root}")

    found = _collect_local_runs(root) + _collect_zip_runs(root)
    if not found:
        raise SystemExit("No suite/manifest with runs found in artifacts root.")

    mismatches = [(origin, runs) for origin, runs in found if runs != args.expected_runs]
    if mismatches:
        details = "\n".join(f" - {origin}: runs={runs}" for origin, runs in mismatches)
        raise SystemExit(
            f"Perf runs mismatch: expected {args.expected_runs} but found different values:\n{details}"
        )

    print(json.dumps({"expected_runs": args.expected_runs, "checked_entries": len(found)}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
