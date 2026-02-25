#!/usr/bin/env bash
set -euo pipefail

find_java_home() {
  local candidate

  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    printf '%s\n' "$JAVA_HOME"
    return 0
  fi

  local mise_root="${MISE_DATA_DIR:-$HOME/.local/share/mise}/installs/java"
  if [[ -d "$mise_root" ]]; then
    for candidate in "$mise_root"/21* "$mise_root"/17*; do
      if [[ -x "$candidate/bin/java" ]]; then
        printf '%s\n' "$candidate"
        return 0
      fi
    done
  fi

  local candidates=(
    "/usr/lib/jvm/java-21-openjdk-amd64"
    "/usr/lib/jvm/java-21-openjdk"
    "/usr/lib/jvm/temurin-21-jdk-amd64"
    "/usr/lib/jvm/java-17-openjdk-amd64"
    "/usr/lib/jvm/java-17-openjdk"
  )

  local c
  for c in "${candidates[@]}"; do
    if [[ -x "$c/bin/java" ]]; then
      printf '%s\n' "$c"
      return 0
    fi
  done

  return 1
}

print_exports() {
  local java_home="$1"
  printf 'export JAVA_HOME=%q\n' "$java_home"
  # Keep PATH dynamic for the caller shell: Java bin path is escaped, $PATH expands when sourced.
  # Expected shape: export PATH=<escaped_java_home>/bin:"$PATH"
  printf 'export PATH=%q:"$PATH"\n' "$java_home/bin"
}

run_self_check() {
  local sample_java_home='/tmp/jdk 21'
  local generated_path_line
  generated_path_line="$(print_exports "$sample_java_home" | sed -n '2p')"

  if [[ "$generated_path_line" != export\ PATH=*':"$PATH"' ]]; then
    echo "ERRO: formato inesperado para linha de PATH: $generated_path_line" >&2
    return 1
  fi

  local expanded_path
  expanded_path="$(PATH='/base/path' /bin/sh -c "$generated_path_line; printf '%s' \"\$PATH\"")"
  if [[ "$expanded_path" != "$sample_java_home/bin:/base/path" ]]; then
    echo "ERRO: PATH não expandiu no source-time: $expanded_path" >&2
    return 1
  fi

  echo "OK: --print gera PATH com expansão em source-time."
}

JAVA_HOME_DETECTED="$(find_java_home || true)"
if [[ -z "$JAVA_HOME_DETECTED" ]]; then
  echo "ERRO: JDK 21/17 não encontrado para configurar JAVA_HOME." >&2
  exit 2
fi

if [[ "${1:-}" == "--print" ]]; then
  print_exports "$JAVA_HOME_DETECTED"
  exit 0
fi

if [[ "${1:-}" == "--self-check" ]]; then
  run_self_check
  exit $?
fi

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  echo "JAVA_HOME detectado: $JAVA_HOME_DETECTED"
  echo "Use no shell atual:"
  echo "  source <(./tools/configure_java_home.sh --print)"
  echo
  ./tools/configure_java_home.sh --print
fi
