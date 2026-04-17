#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

QUICK_MODE=0
if [[ "${1:-}" == "--quick" ]]; then
  QUICK_MODE=1
fi

log_ok() { printf 'OK  - %s\n' "$1"; }
log_warn() { printf 'WARN- %s\n' "$1"; }
log_err() { printf 'ERR - %s\n' "$1"; }

first_existing_dir() {
  local d
  for d in "$@"; do
    if [[ -n "$d" && -d "$d" ]]; then
      printf '%s\n' "$d"
      return 0
    fi
  done
  return 1
}

extract_prop() {
  local file="$1" key="$2"
  awk -F= -v k="$key" '
    /^[[:space:]]*#/ { next }
    {
      kk=$1
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", kk)
      if (kk == k) {
        sub(/^[^=]*=/, "", $0)
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", $0)
        print $0
        exit
      }
    }
  ' "$file" 2>/dev/null | tr -d '\r'
}

resolve_prop_with_alias() {
  local file="$1"
  local canonical="$2"
  local legacy="$3"
  local value=""

  value="$(extract_prop "$file" "$canonical")"
  if [[ -z "$value" ]]; then
    value="$(extract_prop "$file" "$legacy")"
  fi
  printf '%s\n' "$value"
}

SDK_DIR=""
if [[ -f "$REPO_ROOT/local.properties" ]]; then
  SDK_DIR="$(extract_prop "$REPO_ROOT/local.properties" "sdk.dir")"
fi
if [[ -z "$SDK_DIR" ]]; then
  SDK_DIR="$(first_existing_dir "${ANDROID_SDK_ROOT:-}" "${ANDROID_HOME:-}" "/workspace/android-sdk" "$HOME/Android/Sdk" || true)"
fi

if [[ -n "$SDK_DIR" && ! -d "$SDK_DIR" ]]; then
  log_warn "sdk.dir definido porém não existe: $SDK_DIR"
  SDK_DIR=""
fi

JAVA_BIN="${JAVA_HOME:-}/bin/java"
if [[ ! -x "$JAVA_BIN" ]]; then
  JAVA_BIN="$(command -v java || true)"
fi
if [[ -z "$JAVA_BIN" || ! -x "$JAVA_BIN" ]]; then
  for candidate in \
    "$HOME/.local/share/mise/installs/java/21/bin/java" \
    "$HOME/.local/share/mise/installs/java/17/bin/java" \
    "/usr/lib/jvm/java-21-openjdk-amd64/bin/java" \
    "/usr/lib/jvm/java-21-openjdk/bin/java" \
    "/usr/lib/jvm/temurin-21-jdk-amd64/bin/java" \
    "/usr/lib/jvm/java-17-openjdk-amd64/bin/java" \
    "/usr/lib/jvm/java-17-openjdk/bin/java"; do
    if [[ -x "$candidate" ]]; then
      JAVA_BIN="$candidate"
      break
    fi
  done
fi

if [[ -z "$JAVA_BIN" || ! -x "$JAVA_BIN" ]]; then
  log_err "Java/JDK não encontrado no PATH nem em JAVA_HOME"
  exit 1
fi

JAVA_MAJOR="$("$JAVA_BIN" -XshowSettings:properties -version 2>&1 | awk -F= '/java\.specification\.version/ {gsub(/ /,"",$2); print $2; exit}')"
if [[ -z "$JAVA_MAJOR" ]]; then
  JAVA_MAJOR="$("$JAVA_BIN" -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n1)"
fi

if [[ -n "$JAVA_MAJOR" && "$JAVA_MAJOR" -ge 17 ]]; then
  log_ok "JDK/Java detectado (major=$JAVA_MAJOR): $JAVA_BIN"
  if [[ "$JAVA_MAJOR" != "21" && "$JAVA_MAJOR" != "17" ]]; then
    log_warn "Java major $JAVA_MAJOR detectado; para Gradle/AGP deste projeto prefira JDK 21 ou 17"
  fi
else
  log_err "JDK incompatível. Necessário Java >= 17, atual: ${JAVA_MAJOR:-desconhecido}"
  exit 1
fi

if [[ -n "$SDK_DIR" ]]; then
  log_ok "Android SDK detectado: $SDK_DIR"
else
  log_warn "Android SDK não detectado (local.properties/ANDROID_SDK_ROOT/ANDROID_HOME)"
fi

GRADLE_NDK_VERSION="$(resolve_prop_with_alias "$REPO_ROOT/gradle.properties" "ndk.version" "NDK_VERSION")"
GRADLE_CMAKE_VERSION="$(resolve_prop_with_alias "$REPO_ROOT/gradle.properties" "cmake.version" "CMAKE_VERSION")"

if [[ -n "$SDK_DIR" ]]; then
  NDK_DIR="$SDK_DIR/ndk/$GRADLE_NDK_VERSION"
  if [[ -n "$GRADLE_NDK_VERSION" && -d "$NDK_DIR" ]]; then
    log_ok "NDK alinhado (gradle.properties): $NDK_DIR"
  else
    log_warn "NDK ${GRADLE_NDK_VERSION:-<não definido>} ausente em $SDK_DIR/ndk"
  fi

  CMAKE_DIR="$SDK_DIR/cmake/$GRADLE_CMAKE_VERSION"
  if [[ -n "$GRADLE_CMAKE_VERSION" && -x "$CMAKE_DIR/bin/cmake" ]]; then
    log_ok "CMake alinhado (gradle.properties): $CMAKE_DIR"
  else
    log_warn "CMake ${GRADLE_CMAKE_VERSION:-<não definido>} ausente em $SDK_DIR/cmake"
  fi

  JNI_HEADER="$(find "$SDK_DIR/ndk" -path '*/sysroot/usr/include/jni.h' -print -quit 2>/dev/null || true)"
  if [[ -n "$JNI_HEADER" ]]; then
    log_ok "JNI headers detectados: $JNI_HEADER"
  else
    log_warn "JNI headers (jni.h) não encontrados no NDK instalado"
  fi
else
  log_warn "Checks de NDK/CMake/JNI pulados por ausência de SDK"
fi

if [[ -x "$REPO_ROOT/gradlew" ]]; then
  if "$REPO_ROOT/gradlew" -q help >/dev/null 2>&1; then
    log_ok "Gradle wrapper executa corretamente"
  else
    log_warn "Gradle wrapper não validado (ambiente sem SDK ou dependências)"
  fi
else
  log_err "gradlew não encontrado no repositório"
  exit 1
fi

if [[ "$QUICK_MODE" -eq 0 ]]; then
  printf '\nResumo alvo (JNI/NDK/JDK/Java + 5 checks extras) concluído.\n'
fi
