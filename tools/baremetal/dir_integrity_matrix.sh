#!/usr/bin/env bash
set -euo pipefail

out="${1:-reports/baremetal/dir_integrity_matrix.json}"
mkdir -p "$(dirname "$out")"

dirs=(fastlane gradle resources shell-loader terminal-emulator terminal-view web)

hash_dir() {
  local d="$1"
  [ -d "$d" ] || { echo ""; return; }
  find "$d" -type f | LC_ALL=C sort | while read -r f; do
    sha256sum "$f"
  done | sha256sum | awk '{print $1}'
}

count_files() {
  local d="$1"
  [ -d "$d" ] || { echo 0; return; }
  find "$d" -type f | wc -l | tr -d ' '
}

{
  echo "{"
  echo '  "schema": "baremetal-integrity-v1",'
  echo '  "directories": ['
  n=${#dirs[@]}
  i=0
  for d in "${dirs[@]}"; do
    i=$((i+1))
    h="$(hash_dir "$d")"
    c="$(count_files "$d")"
    comma=","
    [ "$i" -eq "$n" ] && comma=""
    echo "    {\"path\": \"$d\", \"files\": $c, \"sha256_tree\": \"$h\"}$comma"
  done
  echo "  ]"
  echo "}"
} > "$out"

cat "$out"
