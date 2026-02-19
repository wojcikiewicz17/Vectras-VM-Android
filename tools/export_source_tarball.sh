#!/usr/bin/env bash
set -euo pipefail

# Exporta arquivos de código-fonte versionados para um diretório e gera .tar.gz.
# Uso:
#   ./tools/export_source_tarball.sh [destino_base]
# Exemplo:
#   ./tools/export_source_tarball.sh archive/source-export

BASE_DIR="${1:-archive/source-export}"
STAMP="$(date +%Y%m%d-%H%M%S)"
EXPORT_DIR="${BASE_DIR}/vectras-source-${STAMP}"
SRC_DIR="${EXPORT_DIR}/sources"
LIST_FILE="${EXPORT_DIR}/sources.list"
TARBALL="${EXPORT_DIR}/vectras-source-${STAMP}.tar.gz"

mkdir -p "${SRC_DIR}"

# Lista de extensões de código/configuração textual normalmente úteis para edição.
git ls-files | awk '
  /\.(java|kt|kts|c|cc|cpp|cxx|h|hh|hpp|hxx|s|S|asm|aidl|rs|proto|gradle|properties|xml|json|yml|yaml|sh|mk|cmake|txt|md)$/ {
    print
  }
' > "${LIST_FILE}"

if [[ ! -s "${LIST_FILE}" ]]; then
  echo "Nenhum arquivo-fonte compatível encontrado em git ls-files." >&2
  exit 1
fi

while IFS= read -r file; do
  mkdir -p "${SRC_DIR}/$(dirname "${file}")"
  cp -a "${file}" "${SRC_DIR}/${file}"
done < "${LIST_FILE}"

tar -czf "${TARBALL}" -C "${EXPORT_DIR}" sources sources.list

printf 'Export concluído:\n'
printf '  Diretório: %s\n' "${SRC_DIR}"
printf '  Lista:     %s\n' "${LIST_FILE}"
printf '  Tarball:   %s\n' "${TARBALL}"
