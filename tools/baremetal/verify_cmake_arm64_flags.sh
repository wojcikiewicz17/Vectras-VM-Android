#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$repo_root"

sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android-sdk}}"
java_home="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
ndk_version="${NDK_VERSION:-26.3.11579264}"

echo "[info] repo_root=$repo_root"
echo "[info] sdk_dir=$sdk_dir"
echo "[info] java_home=$java_home"

if [ ! -d "$sdk_dir" ]; then
  echo "[error] Android SDK não encontrado em: $sdk_dir" >&2
  exit 1
fi

cat > local.properties <<PROP
sdk.dir=$sdk_dir
PROP

export JAVA_HOME="$java_home"
export ANDROID_HOME="$sdk_dir"
export ANDROID_SDK_ROOT="$sdk_dir"

./gradlew :app:configureCMakeDebug[arm64-v8a] \
  -PNDK_VERSION="$ndk_version" \
  -PGRADLE_JAVA_RUNTIME_VERSION=21 \
  -PRMR_ENABLE_POLICY_MODULE=OFF \
  -PRMR_ENABLE_BITRAF_MODULE=OFF

hash_dir="$(find app/.cxx/Debug -mindepth 1 -maxdepth 1 -type d | head -n1)"
if [ -z "$hash_dir" ]; then
  echo "[error] Nenhum hash encontrado em app/.cxx/Debug" >&2
  exit 1
fi

cache_file="$hash_dir/arm64-v8a/CMakeCache.txt"
if [ ! -f "$cache_file" ]; then
  echo "[error] CMakeCache.txt não encontrado: $cache_file" >&2
  exit 1
fi

echo "[check] $cache_file"
rg -n "RMR_ENABLE_POLICY_MODULE:BOOL=OFF" "$cache_file"
rg -n "RMR_ENABLE_BITRAF_MODULE:BOOL=OFF" "$cache_file"

flags_file="$hash_dir/arm64-v8a/CMakeFiles/vectra_core_accel.dir/flags.make"
if [ -f "$flags_file" ]; then
  echo "[check] $flags_file"
  rg -n -- "-DRMR_ENABLE_POLICY_MODULE=0" "$flags_file"
  rg -n -- "-DRMR_ENABLE_BITRAF_MODULE=0" "$flags_file"
else
  ninja_file="$hash_dir/arm64-v8a/build.ninja"
  compile_commands_file="$hash_dir/arm64-v8a/compile_commands.json"
  echo "[warn] flags.make ausente (gerador Ninja). Validando em build.ninja/compile_commands.json"
  rg -n -- "-DRMR_ENABLE_POLICY_MODULE=0" "$ninja_file" "$compile_commands_file"
  rg -n -- "-DRMR_ENABLE_BITRAF_MODULE=0" "$ninja_file" "$compile_commands_file"
fi

echo "[ok] validação concluída para hash $(basename "$hash_dir")"
