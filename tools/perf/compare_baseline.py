#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

METRICS = {
    "latency_p50_ms": ("latency_ms", "p50", "lower_better"),
    "latency_p95_ms": ("latency_ms", "p95", "lower_better"),
    "throughput_median_score_per_s": ("throughput", "median_score_per_s", "higher_better"),
    "binary_size_bytes": (None, None, "lower_better"),
    "cold_start_ms": ("startup_ms", "cold", "lower_better"),
    "warm_start_ms": ("startup_ms", "warm_median", "lower_better"),
}


def pick(item: dict, key: str) -> float:
    section, subkey, _ = METRICS[key]
    if section is None:
        return float(item[key])
    return float(item[section][subkey])


def main() -> int:
    parser = argparse.ArgumentParser(description="Compara benchmark atual contra baseline versionado.")
    parser.add_argument("--suite", default="bench/results/perf/suite.json")
    parser.add_argument("--baseline-dir", default="bench/baseline")
    parser.add_argument("--margin", type=float, default=0.03, help="margem relativa para neutralidade")
    parser.add_argument("--out", default="bench/results/perf/comparison.json")
    args = parser.parse_args()

    suite = json.loads((ROOT / args.suite).read_text(encoding="utf-8"))
    baseline_dir = ROOT / args.baseline_dir

    comparisons: list[dict] = []
    gate_failed = False

    for profile in suite["profiles"]:
        profile_id = profile["profile_id"]
        baseline_path = baseline_dir / f"{profile_id}.json"
        if not baseline_path.exists():
            comparisons.append({"profile_id": profile_id, "status": "missing_baseline", "gate": "fail"})
            gate_failed = True
            continue
        baseline = json.loads(baseline_path.read_text(encoding="utf-8"))

        metric_results = []
        profile_pass = True
        for metric, (_, _, direction) in METRICS.items():
            current = pick(profile, metric)
            base = float(baseline[metric])
            delta_rel = 0.0 if base == 0 else (current - base) / base
            if direction == "higher_better":
                improved = delta_rel > args.margin
                regressed = delta_rel < -args.margin
            else:
                improved = delta_rel < -args.margin
                regressed = delta_rel > args.margin
            neutral = not improved and not regressed
            status = "improved" if improved else "regressed" if regressed else "neutral"
            if regressed:
                profile_pass = False
            metric_results.append(
                {
                    "metric": metric,
                    "baseline": base,
                    "current": current,
                    "delta_rel": delta_rel,
                    "status": status,
                }
            )

        comparisons.append({
            "profile_id": profile_id,
            "status": "pass" if profile_pass else "fail",
            "margin": args.margin,
            "metrics": metric_results,
            "gate": "pass" if profile_pass else "fail",
        })
        gate_failed = gate_failed or (not profile_pass)

    report = {
        "schema": "vectras-perf-comparison-v1",
        "margin": args.margin,
        "comparisons": comparisons,
        "gate": "fail" if gate_failed else "pass",
    }
    out_path = ROOT / args.out
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps({"comparison": str(out_path.relative_to(ROOT)), "gate": report["gate"]}))
    return 1 if gate_failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
