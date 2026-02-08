#!/usr/bin/env bash
set -euo pipefail

out="${1:-reports/baremetal/hw_caps.env}"
mkdir -p "$(dirname "$out")"

arch="$(uname -m)"
os="$(uname -s)"
kernel="$(uname -r)"
cpus="$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 1)"
pagesize="$(getconf PAGESIZE 2>/dev/null || echo 4096)"
endian="unknown"
case "$arch" in
  x86_64|i386|i686|aarch64|armv7*|armv8*|riscv64|riscv32) endian="little" ;;
  s390*|ppc64) endian="big" ;;
esac

cacheline="$(cat /sys/devices/system/cpu/cpu0/cache/index0/coherency_line_size 2>/dev/null || echo 64)"
mem_kb="$(awk '/MemTotal:/ {print $2}' /proc/meminfo 2>/dev/null || echo 0)"

{
  echo "ARCH=$arch"
  echo "OS=$os"
  echo "KERNEL=$kernel"
  echo "CPUS=$cpus"
  echo "PAGESIZE=$pagesize"
  echo "ENDIAN=$endian"
  echo "CACHELINE_BYTES=$cacheline"
  echo "MEMTOTAL_KB=$mem_kb"
} > "$out"

cat "$out"
