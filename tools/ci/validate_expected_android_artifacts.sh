#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <variant1,variant2,...>" >&2
  exit 2
fi

variants_csv="$1"
report_path="app/build/reports/artifacts/compiled-artifacts-report.json"

if [[ ! -s "${report_path}" ]]; then
  echo "Expected compiled artifacts report was not found or is empty: ${report_path}" >&2
  exit 1
fi

IFS=',' read -r -a variants <<< "${variants_csv}"
for raw_variant in "${variants[@]}"; do
  variant="${raw_variant// /}"
  [[ -z "${variant}" ]] && continue

  apk_dir="app/build/outputs/apk/${variant}"
  bundle_dir="app/build/outputs/bundle/${variant}"
  has_apk=false
  has_bundle=false

  if [[ -d "${apk_dir}" ]] && compgen -G "${apk_dir}/*.apk" > /dev/null; then
    has_apk=true
  fi

  if [[ -d "${bundle_dir}" ]] && compgen -G "${bundle_dir}/*.aab" > /dev/null; then
    has_bundle=true
  fi

  if [[ "${variant}" == "debug" ]]; then
    if [[ "${has_apk}" != true ]]; then
      echo "Expected at least one APK for variant '${variant}' in ${apk_dir}" >&2
      exit 1
    fi
  elif [[ "${has_apk}" != true && "${has_bundle}" != true ]]; then
    echo "Expected APK or AAB for variant '${variant}' in ${apk_dir} or ${bundle_dir}" >&2
    exit 1
  fi

done

echo "Validated expected APK/AAB outputs and compiled artifacts report for variants: ${variants_csv}"
