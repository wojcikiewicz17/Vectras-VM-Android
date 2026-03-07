#!/usr/bin/env bash
set -euo pipefail

runs="${1:-7}"
out_dir="${2:-bench/results}"
mkdir -p "$out_dir"

make run-bench >/dev/null

scores_file="$out_dir/scores.txt"
formulas_file="$out_dir/score_formula_ids.txt"
: > "$scores_file"
: > "$formulas_file"

for i in $(seq 1 "$runs"); do
  csv="$out_dir/run_${i}.csv"
  json="$out_dir/run_${i}.json"
  ./build/bench/rmr_bench "$csv" "$json" | tee "$out_dir/run_${i}.log"
  score=$(awk -F= '/bench_total_score=/{print $2}' "$out_dir/run_${i}.log")
  if [ -z "$score" ]; then
    score=$(awk -F: '/"total_score"/{gsub(/[ ,]/, "", $2); print $2; exit}' "$json")
  fi
  formula_id=$(awk -F: '/"score_formula_id"/{gsub(/[ ",]/, "", $2); print $2; exit}' "$json")
  if [ -z "$score" ]; then
    echo "missing score in run $i" >&2
    exit 1
  fi
  if [ -z "$formula_id" ]; then
    echo "missing score_formula_id in run $i" >&2
    exit 1
  fi
  echo "$score" >> "$scores_file"
  echo "$formula_id" >> "$formulas_file"
done

sorted_file="$out_dir/scores_sorted.txt"
sort -n "$scores_file" > "$sorted_file"
count=$(wc -l < "$sorted_file" | tr -d ' ')
if [ "$count" -eq 0 ]; then
  echo "no scores" >&2
  exit 1
fi
median=$(awk -v n="$count" '{a[NR]=$1} END{if(n%2==1){print a[(n+1)/2]}else{print int((a[n/2]+a[n/2+1])/2)}}' "$sorted_file")
p95_index=$(( (95*count + 99)/100 ))
if [ "$p95_index" -lt 1 ]; then p95_index=1; fi
p95=$(awk -v idx="$p95_index" 'NR==idx{print $1}' "$sorted_file")
minv=$(awk 'NR==1{print $1}' "$sorted_file")
maxv=$(awk -v n="$count" 'NR==n{print $1}' "$sorted_file")
scores_json=$(awk 'BEGIN{printf "["} {if(NR>1)printf ","; printf "%s", $1} END{printf "]"}' "$sorted_file")
formula_id=$(sort -u "$formulas_file")
formula_count=$(printf '%s\n' "$formula_id" | sed '/^$/d' | wc -l | tr -d ' ')
if [ "$formula_count" -ne 1 ]; then
  echo "inconsistent score_formula_id values:" >&2
  sort -u "$formulas_file" >&2
  exit 1
fi
cat > "$out_dir/summary.json" <<JSON
{
  "runs": $count,
  "score_formula_id": "${formula_id}",
  "median": $median,
  "p95": $p95,
  "min": $minv,
  "max": $maxv,
  "scores": $scores_json
}
JSON
cat "$out_dir/summary.json"
