#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

require_binary="false"
critical_binary=""

fail() {
  local prefix="$1"
  local what="$2"
  local target="$3"
  local expected="$4"
  local fix="$5"

  {
    echo "${prefix}: ${what}"
    echo "  alvo: ${target}"
    echo "  comando esperado: ${expected}"
    echo "  ação de correção: ${fix}"
  } >&2
  exit 1
}

usage() {
  cat <<USAGE
Usage: tools/ci/validate_lowlevel_abi.sh [--critical-binary <path>] [--require-critical-binary]

Runs consolidated lowlevel ABI validations:
  1) ABI contract schema and mandatory exported symbols.
  2) Freestanding source contract for critical native layer.
  3) Optional critical binary symbol policy (blocks malloc/calloc/realloc/free/posix_memalign).
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --critical-binary)
      if [[ $# -lt 2 || -z "${2:-}" ]]; then
        fail "CONFIG" "argumento obrigatório ausente para --critical-binary" "$1" "tools/ci/validate_lowlevel_abi.sh --critical-binary <path>" "informar caminho válido para o binário crítico"
      fi
      critical_binary="$2"
      shift 2
      ;;
    --require-critical-binary)
      require_binary="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      usage >&2
      fail "CONFIG" "argumento desconhecido" "$1" "tools/ci/validate_lowlevel_abi.sh [--critical-binary <path>] [--require-critical-binary]" "remover argumento inválido ou usar uma flag suportada"
      ;;
  esac
done

echo "[validate-lowlevel-abi] validating ABI contract drift (gradle/qemu/workflows)"
if ! command -v python3 >/dev/null 2>&1; then
  fail "ENV" "dependência ausente para validação de contrato ABI" "python3" "command -v python3" "instalar Python 3 no ambiente de CI/build"
fi

python_drift_cmd=(python3 "${ROOT_DIR}/tools/ci/check_abi_contract_drift.py")
if ! "${python_drift_cmd[@]}"; then
  fail "CI_SCRIPT" "drift de contrato ABI detectado" "${ROOT_DIR}/tools/ci/check_abi_contract_drift.py" "${python_drift_cmd[*]}" "alinhar gradle.properties, tools/qemu_launch.yml e .github/workflows ao contrato tools/ci/abi_profiles_contract.json"
fi

echo "[validate-lowlevel-abi] validating schema + exported symbols"
if ! command -v python3 >/dev/null 2>&1; then
  fail "ENV" "dependência ausente para validação de contrato ABI" "python3" "command -v python3" "instalar Python 3 no ambiente de CI/build"
fi

python_contract_cmd=(python3 "${ROOT_DIR}/tools/ci/validate_lowlevel_abi_contract.py")
if ! "${python_contract_cmd[@]}"; then
  fail "CI_SCRIPT" "validação de contrato ABI falhou" "${ROOT_DIR}/tools/ci/validate_lowlevel_abi_contract.py" "${python_contract_cmd[*]}" "corrigir schema/exported symbols obrigatórios conforme erro reportado"
fi

echo "[validate-lowlevel-abi] validating freestanding source contract"
source_contract_cmd=("${ROOT_DIR}/tools/ci/verify_android_freestanding_contract.sh")
if ! "${source_contract_cmd[@]}"; then
  fail "SOURCE" "contrato freestanding do código nativo foi violado" "${ROOT_DIR}/tools/ci/verify_android_freestanding_contract.sh" "${source_contract_cmd[*]}" "remover dependências proibidas e alinhar a camada nativa ao contrato freestanding"
fi

if [[ -z "${critical_binary}" ]]; then
  detected="$(find "${ROOT_DIR}/app/build" -type f -name 'libvectra_core_accel.so' 2>/dev/null | head -n 1 || true)"
  if [[ -n "${detected}" ]]; then
    critical_binary="${detected}"
  fi
fi

if [[ -n "${critical_binary}" ]]; then
  [[ -f "${critical_binary}" ]] || fail "SOURCE" "binário crítico informado não existe" "${critical_binary}" "test -f ${critical_binary}" "gerar o .so antes da validação ou corrigir o caminho enviado em --critical-binary"
  echo "[validate-lowlevel-abi] validating freestanding binary policy: ${critical_binary}"

  nm_tool=""
  for candidate in llvm-nm nm; do
    if command -v "${candidate}" >/dev/null 2>&1; then
      nm_tool="${candidate}"
      break
    fi
  done

  [[ -n "${nm_tool}" ]] || fail "ENV" "ferramenta de inspeção de símbolos ausente" "${critical_binary}" "command -v llvm-nm || command -v nm" "instalar llvm-nm ou nm no ambiente de CI/build"

  forbidden_regex='^(malloc|calloc|realloc|free|posix_memalign)$'
  symbols="$(${nm_tool} -D --undefined-only "${critical_binary}" 2>/dev/null || true)"
  if [[ -z "${symbols}" ]]; then
    symbols="$(${nm_tool} -u "${critical_binary}" 2>/dev/null || true)"
  fi

  symbol_names="$(printf '%s\n' "${symbols}" | awk '{print $NF}')"

  if command -v rg >/dev/null 2>&1; then
    violations="$(printf '%s\n' "${symbol_names}" | rg -N "${forbidden_regex}" || true)"
  elif command -v grep >/dev/null 2>&1; then
    violations="$(printf '%s\n' "${symbol_names}" | grep -E "${forbidden_regex}" || true)"
  else
    fail "ENV" "ferramenta para filtro de símbolos proibidos ausente" "${critical_binary}" "command -v rg || command -v grep" "instalar ripgrep (preferencial) ou grep no ambiente de CI/build"
  fi

  if [[ -n "${violations}" ]]; then
    {
      echo "ABI_CONTRACT: binário crítico referencia símbolos de runtime proibidos"
      echo "  alvo: ${critical_binary}"
      echo "  comando esperado: ${nm_tool} -D --undefined-only ${critical_binary} | awk '{print \$NF}' | (rg -N|grep -E) '${forbidden_regex}'"
      echo "  ação de correção: remover dependências de malloc/calloc/realloc/free/posix_memalign da camada crítica"
      echo "  símbolos encontrados:"
      printf '%s\n' "${violations}"
    } >&2
    exit 1
  fi

  echo "[validate-lowlevel-abi] binary policy OK"
elif [[ "${require_binary}" == "true" ]]; then
  fail "ABI_CONTRACT" "--require-critical-binary ativo sem binário detectado" "libvectra_core_accel.so" "tools/ci/validate_lowlevel_abi.sh --require-critical-binary --critical-binary <path>" "gerar o binário crítico antes da validação ou informar caminho explícito"
else
  echo "[validate-lowlevel-abi] no critical binary found yet; binary policy check skipped"
fi

echo "[validate-lowlevel-abi] all checks passed"
