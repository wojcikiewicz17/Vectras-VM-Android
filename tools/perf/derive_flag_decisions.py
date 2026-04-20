#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def main() -> int:
    parser = argparse.ArgumentParser(description="Deriva recomendações de flags nativas por ABI com base na comparação de perf.")
    parser.add_argument("--suite", default="bench/results/perf/suite.json")
    parser.add_argument("--comparison", default="bench/results/perf/comparison.json")
    parser.add_argument("--out", default="bench/results/perf/flag_decisions.json")
    args = parser.parse_args()

    suite = json.loads((ROOT / args.suite).read_text(encoding="utf-8"))
    comparison = json.loads((ROOT / args.comparison).read_text(encoding="utf-8"))
    by_profile = {entry["profile_id"]: entry for entry in suite["profiles"]}
    cmp_by_profile = {entry["profile_id"]: entry for entry in comparison["comparisons"]}

    per_abi: dict[str, list[dict]] = {}
    for profile_id, profile in by_profile.items():
        abi = profile["abi"]
        per_abi.setdefault(abi, []).append({
            "profile_id": profile_id,
            "cflags": profile["cflags"],
            "lto": profile["lto"],
            "inline": profile["inline_policy"],
            "latency_p50": profile["latency_ms"]["p50"],
            "throughput": profile["throughput"]["median_score_per_s"],
            "size": profile["binary_size_bytes"],
            "comparison": cmp_by_profile.get(profile_id, {}),
        })

    decisions = {}
    for abi, options in per_abi.items():
        safe = [o for o in options if o.get("comparison", {}).get("status") != "fail"]
        pool = safe if safe else options
        pool.sort(key=lambda item: (-item["throughput"], item["latency_p50"], item["size"]))
        best = pool[0]
        decisions[abi] = {
            "selected_profile": best["profile_id"],
            "cflags": best["cflags"],
            "lto": best["lto"],
            "inline": best["inline"],
            "selection_basis": "best_throughput_with_non_regression_preference",
            "fallback_used": len(safe) == 0,
        }

    out = {
        "schema": "vectras-native-flags-decision-v1",
        "source_suite": args.suite,
        "source_comparison": args.comparison,
        "decisions": decisions,
    }
    out_path = ROOT / args.out
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(out, indent=2), encoding="utf-8")
    print(json.dumps({"decisions": str(out_path.relative_to(ROOT)), "abis": sorted(decisions.keys())}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
