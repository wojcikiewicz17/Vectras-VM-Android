#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
import math
import os
import platform
import shutil
import statistics
import subprocess
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROFILES_PATH = ROOT / "tools" / "perf" / "profiles.json"
DEFAULT_OUT_DIR = ROOT / "bench" / "results" / "perf"


def run(cmd: list[str], cwd: Path | None = None, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, cwd=cwd, check=check, text=True, capture_output=True)


def percentile(vals: list[float], p: float) -> float:
    if not vals:
        return 0.0
    ordered = sorted(vals)
    idx = int(math.ceil((p / 100.0) * len(ordered))) - 1
    idx = max(0, min(idx, len(ordered) - 1))
    return ordered[idx]


def parse_total_score(stdout: str, json_path: Path) -> float:
    for line in stdout.splitlines():
        if line.startswith("bench_total_score="):
            return float(line.split("=", 1)[1].strip())
    payload = json.loads(json_path.read_text(encoding="utf-8"))
    return float(payload.get("total_score", 0.0))


def maybe_perf_cache_misses(binary_path: Path) -> dict[str, float | str | None]:
    perf = shutil.which("perf")
    if not perf:
        return {"available": False, "cache_misses": None, "cache_references": None, "note": "perf_not_installed"}
    cmd = [perf, "stat", "-e", "cache-misses,cache-references", str(binary_path), "/tmp/perf.csv", "/tmp/perf.json"]
    proc = subprocess.run(cmd, text=True, capture_output=True)
    if proc.returncode != 0:
        return {"available": False, "cache_misses": None, "cache_references": None, "note": "perf_stat_unavailable"}
    misses = None
    refs = None
    for line in proc.stderr.splitlines():
        if "cache-misses" in line:
            token = line.strip().split()[0].replace(",", "")
            misses = float(token) if token.replace(".", "", 1).isdigit() else None
        if "cache-references" in line:
            token = line.strip().split()[0].replace(",", "")
            refs = float(token) if token.replace(".", "", 1).isdigit() else None
    return {"available": misses is not None and refs is not None, "cache_misses": misses, "cache_references": refs, "note": "ok"}


def main() -> int:
    parser = argparse.ArgumentParser(description="Executa suíte de microbenchmarks por ABI/política.")
    parser.add_argument("--profiles", default=str(PROFILES_PATH))
    parser.add_argument("--out-dir", default=str(DEFAULT_OUT_DIR))
    parser.add_argument("--runs", type=int, default=0)
    parser.add_argument("--profile-id", action="append", default=[])
    args = parser.parse_args()

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    profiles_doc = json.loads(Path(args.profiles).read_text(encoding="utf-8"))
    profiles = profiles_doc["profiles"]
    host_arch = platform.machine().lower()
    runs = args.runs if args.runs > 0 else int(profiles_doc.get("default_runs", 9))

    selected: list[dict] = []
    for profile in profiles:
        if args.profile_id and profile["id"] not in set(args.profile_id):
            continue
        allowed = [a.lower() for a in profile.get("enabled_if_host_arch", [])]
        if allowed and host_arch not in allowed:
            continue
        if shutil.which(profile["cc"]) is None:
            continue
        selected.append(profile)

    if not selected:
        raise SystemExit("Nenhum perfil de benchmark executável no host atual.")

    rows: list[dict] = []
    runs_dir = out_dir / "runs"
    runs_dir.mkdir(parents=True, exist_ok=True)

    for profile in selected:
        profile_dir = runs_dir / profile["id"]
        profile_dir.mkdir(parents=True, exist_ok=True)

        env = os.environ.copy()
        cflags = profile["cflags"]
        if profile.get("lto") == "thin":
            cflags = f"{cflags} -flto"
        env["CC"] = profile["cc"]
        env["CFLAGS"] = cflags

        build_proc = subprocess.run(["make", "clean"], cwd=ROOT, env=env, text=True, capture_output=True)
        if build_proc.returncode != 0:
            raise RuntimeError(f"make clean failed for {profile['id']}: {build_proc.stderr}")
        bench_proc = subprocess.run(["make", "run-bench"], cwd=ROOT, env=env, text=True, capture_output=True)
        if bench_proc.returncode != 0:
            raise RuntimeError(f"make run-bench failed for {profile['id']}: {bench_proc.stderr}")

        binary_path = ROOT / "build" / "bench" / "rmr_bench"
        if not binary_path.exists():
            raise RuntimeError(f"binary not found for profile {profile['id']}")
        binary_size = binary_path.stat().st_size

        latencies_ms: list[float] = []
        throughputs: list[float] = []
        run_artifacts: list[dict] = []

        for idx in range(1, runs + 1):
            csv_path = profile_dir / f"run_{idx}.csv"
            json_path = profile_dir / f"run_{idx}.json"
            start = time.perf_counter()
            proc = subprocess.run([str(binary_path), str(csv_path), str(json_path)], cwd=ROOT, text=True, capture_output=True)
            elapsed_ms = (time.perf_counter() - start) * 1000.0
            if proc.returncode != 0:
                raise RuntimeError(f"rmr_bench failed in {profile['id']} run={idx}: {proc.stderr}")
            score = parse_total_score(proc.stdout, json_path)
            throughput = score / (elapsed_ms / 1000.0) if elapsed_ms > 0 else 0.0
            latencies_ms.append(elapsed_ms)
            throughputs.append(throughput)
            run_artifacts.append({
                "run": idx,
                "latency_ms": elapsed_ms,
                "throughput_score_per_s": throughput,
                "total_score": score,
                "csv": str(csv_path.relative_to(ROOT)),
                "json": str(json_path.relative_to(ROOT)),
            })

        cold_start_ms = latencies_ms[0]
        warm_start_ms = statistics.median(latencies_ms[1:]) if len(latencies_ms) > 1 else latencies_ms[0]
        cache = maybe_perf_cache_misses(binary_path)

        profile_result = {
            "profile_id": profile["id"],
            "abi": profile["abi"],
            "policy": profile["policy"],
            "compiler": profile["cc"],
            "cflags": cflags,
            "inline_policy": profile.get("inline"),
            "lto": profile.get("lto", "off"),
            "runs": runs,
            "latency_ms": {
                "p50": percentile(latencies_ms, 50),
                "p95": percentile(latencies_ms, 95),
                "samples": latencies_ms,
            },
            "throughput": {
                "median_score_per_s": statistics.median(throughputs),
                "p95_score_per_s": percentile(throughputs, 95),
                "samples": throughputs,
            },
            "binary_size_bytes": binary_size,
            "startup_ms": {
                "cold": cold_start_ms,
                "warm_median": warm_start_ms,
            },
            "cache_counters": cache,
            "runs_detail": run_artifacts,
        }
        rows.append(profile_result)

    suite = {
        "schema": "vectras-perf-suite-v1",
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "host_arch": host_arch,
        "profiles": rows,
    }

    suite_json = out_dir / "suite.json"
    suite_json.write_text(json.dumps(suite, indent=2), encoding="utf-8")

    csv_path = out_dir / "suite.csv"
    with csv_path.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(
            fh,
            fieldnames=[
                "profile_id",
                "abi",
                "policy",
                "compiler",
                "cflags",
                "lto",
                "inline_policy",
                "latency_p50_ms",
                "latency_p95_ms",
                "throughput_median_score_per_s",
                "throughput_p95_score_per_s",
                "binary_size_bytes",
                "cold_start_ms",
                "warm_start_ms",
                "cache_misses",
                "cache_references",
                "cache_note",
            ],
        )
        writer.writeheader()
        for item in rows:
            writer.writerow(
                {
                    "profile_id": item["profile_id"],
                    "abi": item["abi"],
                    "policy": item["policy"],
                    "compiler": item["compiler"],
                    "cflags": item["cflags"],
                    "lto": item["lto"],
                    "inline_policy": item["inline_policy"],
                    "latency_p50_ms": f"{item['latency_ms']['p50']:.6f}",
                    "latency_p95_ms": f"{item['latency_ms']['p95']:.6f}",
                    "throughput_median_score_per_s": f"{item['throughput']['median_score_per_s']:.6f}",
                    "throughput_p95_score_per_s": f"{item['throughput']['p95_score_per_s']:.6f}",
                    "binary_size_bytes": item["binary_size_bytes"],
                    "cold_start_ms": f"{item['startup_ms']['cold']:.6f}",
                    "warm_start_ms": f"{item['startup_ms']['warm_median']:.6f}",
                    "cache_misses": item["cache_counters"]["cache_misses"],
                    "cache_references": item["cache_counters"]["cache_references"],
                    "cache_note": item["cache_counters"]["note"],
                }
            )

    print(json.dumps({"suite_json": str(suite_json.relative_to(ROOT)), "suite_csv": str(csv_path.relative_to(ROOT)), "profiles": len(rows)}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
