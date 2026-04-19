#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════
# APPLY_FIXES.sh — Aplica todos os 7 fixes de compilação ao projeto
# Vectras-VM-Android
#
# USO:
#   1. Copie este script e os arquivos FIX_0*.* para a RAIZ do projeto
#      (mesmo diretório que contém CMakeLists.txt e settings.gradle)
#   2. Execute: bash APPLY_FIXES.sh
#   3. Edite local.properties com o caminho real do seu Android SDK
#
# REQUISITOS:
#   - bash, cp, sed
#   - Os arquivos FIX_0*.* presentes no mesmo diretório do script
# ═══════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_err()  { echo -e "${RED}[ERRO]${NC} $1"; }

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  Vectras-VM-Android — Aplicando Fixes de Compilação"
echo "═══════════════════════════════════════════════════════"
echo ""

# ── Verificar que estamos na raiz do projeto ──────────────────────
if [ ! -f "$PROJECT_ROOT/settings.gradle" ] || [ ! -f "$PROJECT_ROOT/CMakeLists.txt" ]; then
    log_err "Execute este script a partir da RAIZ do projeto Vectras-VM-Android."
    log_err "Esperado: settings.gradle e CMakeLists.txt no diretório atual."
    exit 1
fi
log_ok "Raiz do projeto detectada: $PROJECT_ROOT"

# ── FIX 01: local.properties ──────────────────────────────────────
log_info "FIX 01: local.properties..."
if [ ! -f "$PROJECT_ROOT/local.properties" ]; then
    cp "$SCRIPT_DIR/FIX_01_local.properties" "$PROJECT_ROOT/local.properties"
    log_ok "local.properties criado."
    log_warn "AÇÃO NECESSÁRIA: Edite $PROJECT_ROOT/local.properties"
    log_warn "  Substitua sdk.dir= pelo caminho real do seu Android SDK."
    log_warn "  Exemplo Linux: sdk.dir=/home/\$USER/Android/Sdk"
else
    log_warn "local.properties já existe — não sobrescrito. Verifique se sdk.dir está correto."
fi

# ── FIX 02: google-services.json (placeholder) ───────────────────
log_info "FIX 02: app/google-services.json (placeholder para validação interna)..."
if [ ! -f "$PROJECT_ROOT/app/google-services.json" ]; then
    cp "$SCRIPT_DIR/FIX_02_google-services.json" "$PROJECT_ROOT/app/google-services.json"
    log_ok "app/google-services.json placeholder criado."
    log_warn "ATENÇÃO: Este é um placeholder para builds de validação interna."
    log_warn "  Para release oficial, substitua pelo google-services.json real."
    log_warn "  Use -PCI_INTERNAL_VALIDATION=true no Gradle para builds de debug."
else
    log_warn "app/google-services.json já existe — não sobrescrito."
fi

# ── FIX 03: sources_rmr_core.cmake ───────────────────────────────
log_info "FIX 03: engine/rmr/sources_rmr_core.cmake — removendo rmr_neon_simd.c do grupo CORE..."
SOURCES_CMAKE="$PROJECT_ROOT/engine/rmr/sources_rmr_core.cmake"
if [ -f "$SOURCES_CMAKE" ]; then
    cp "$SOURCES_CMAKE" "${SOURCES_CMAKE}.bak"
    cp "$SCRIPT_DIR/FIX_03_sources_rmr_core.cmake" "$SOURCES_CMAKE"
    log_ok "sources_rmr_core.cmake atualizado. Backup: ${SOURCES_CMAKE}.bak"
else
    log_err "Arquivo não encontrado: $SOURCES_CMAKE"
    exit 1
fi

# ── FIX 04: CMakeLists.txt raiz ───────────────────────────────────
log_info "FIX 04: CMakeLists.txt (raiz) — consumindo novo grupo ASM_ARM64_NEON condicionalmente..."
ROOT_CMAKE="$PROJECT_ROOT/CMakeLists.txt"
if [ -f "$ROOT_CMAKE" ]; then
    cp "$ROOT_CMAKE" "${ROOT_CMAKE}.bak"
    cp "$SCRIPT_DIR/FIX_04_CMakeLists.txt" "$ROOT_CMAKE"
    log_ok "CMakeLists.txt (raiz) atualizado. Backup: ${ROOT_CMAKE}.bak"
else
    log_err "Arquivo não encontrado: $ROOT_CMAKE"
    exit 1
fi

# ── FIX 05: app/src/main/cpp/CMakeLists.txt ──────────────────────
log_info "FIX 05: app/src/main/cpp/CMakeLists.txt — NEON sources por ABI..."
JNI_CMAKE="$PROJECT_ROOT/app/src/main/cpp/CMakeLists.txt"
if [ -f "$JNI_CMAKE" ]; then
    cp "$JNI_CMAKE" "${JNI_CMAKE}.bak"
    cp "$SCRIPT_DIR/FIX_05_app_src_main_cpp_CMakeLists.txt" "$JNI_CMAKE"
    log_ok "app/src/main/cpp/CMakeLists.txt atualizado. Backup: ${JNI_CMAKE}.bak"
else
    log_err "Arquivo não encontrado: $JNI_CMAKE"
    exit 1
fi

# ── FIX 06: CMakePresets.json ─────────────────────────────────────
log_info "FIX 06: CMakePresets.json — presets com fallback para ANDROID_SDK_ROOT..."
PRESETS="$PROJECT_ROOT/CMakePresets.json"
if [ -f "$PRESETS" ]; then
    cp "$PRESETS" "${PRESETS}.bak"
    cp "$SCRIPT_DIR/FIX_06_CMakePresets.json" "$PRESETS"
    log_ok "CMakePresets.json atualizado. Backup: ${PRESETS}.bak"
else
    log_err "Arquivo não encontrado: $PRESETS"
    exit 1
fi

# ── FIX 07: rmr_unified_jni_base.h ───────────────────────────────
log_info "FIX 07: engine/rmr/include/rmr_unified_jni_base.h — assert.h + RMR_STATIC_ASSERT portável..."
JNI_BASE_H="$PROJECT_ROOT/engine/rmr/include/rmr_unified_jni_base.h"
if [ -f "$JNI_BASE_H" ]; then
    cp "$JNI_BASE_H" "${JNI_BASE_H}.bak"
    cp "$SCRIPT_DIR/FIX_07_rmr_unified_jni_base.h" "$JNI_BASE_H"
    log_ok "rmr_unified_jni_base.h atualizado. Backup: ${JNI_BASE_H}.bak"
else
    log_err "Arquivo não encontrado: $JNI_BASE_H"
    exit 1
fi

# ── Verificação final ─────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════════════════"
echo "  Todos os fixes aplicados com sucesso."
echo "═══════════════════════════════════════════════════════"
echo ""
echo "PRÓXIMOS PASSOS:"
echo ""
echo "  1. Edite local.properties com o caminho real do SDK:"
echo "     nano $PROJECT_ROOT/local.properties"
echo ""
echo "  2. Build de validação interna (debug, sem Firebase real):"
echo "     ./tools/gradle_with_jdk21.sh :app:assembleDebug \\"
echo "         -PCI_INTERNAL_VALIDATION=true \\"
echo "         --stacktrace"
echo ""
echo "  3. Build CMake host (x86_64 Linux):"
echo "     cmake --preset host-ninja"
echo "     cmake --build --preset build-host -j\$(nproc)"
echo ""
echo "  4. Build CMake Android ARM64 (requer ANDROID_NDK_ROOT ou ANDROID_SDK_ROOT):"
echo "     export ANDROID_NDK_ROOT=\$HOME/Android/Sdk/ndk/27.2.12479018"
echo "     cmake --preset android-arm64-v8"
echo "     cmake --build --preset build-android-arm64-v8 -j\$(nproc)"
echo ""
echo "  Backups dos arquivos originais salvos com sufixo .bak"
echo ""
