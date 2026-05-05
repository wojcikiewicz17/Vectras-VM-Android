#!/usr/bin/env bash
set -euo pipefail
mkdir -p reports
apk="${1:-}"
if [[ -z "$apk" ]]; then
  apk="$(find dist/apk-matrix/signed dist/apk-matrix/unsigned app/build/outputs/apk/debug -name '*.apk' 2>/dev/null | head -n1 || true)"
fi
ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
status="DEVICE_PENDING"
install_status="pending"; launch_status="pending"; runas_status="pending"; notes=""
if ! command -v adb >/dev/null 2>&1; then notes="adb missing"; else
  dev="$(adb devices | awk 'NR>1 && $2=="device"{print $1;exit}')"
  if [[ -z "$dev" ]]; then notes="no connected device"; else
    model="$(adb shell getprop ro.product.model | tr -d '')"
    abi="$(adb shell getprop ro.product.cpu.abi | tr -d '')"
    abilist="$(adb shell getprop ro.product.cpu.abilist | tr -d '')"
    sdk="$(adb shell getprop ro.build.version.sdk | tr -d '')"
    android_rel="$(adb shell getprop ro.build.version.release | tr -d '')"
    page_size="$(adb shell getconf PAGE_SIZE 2>/dev/null | tr -d '' || echo unknown)"
    if [[ -n "$apk" && -f "$apk" ]]; then adb install -r "$apk" >/tmp/adb_install.log 2>&1 && install_status="ok" || install_status="fail"; fi
    adb shell monkey -p com.termux.rafacodephi -c android.intent.category.LAUNCHER 1 >/tmp/adb_launch.log 2>&1 && launch_status="ok" || launch_status="fail"
    sleep 3
    adb logcat -d | rg -i "termux|rafacodephi" > reports/device_runtime_logcat.txt || true
    runas_status="warn"; adb shell run-as com.termux.rafacodephi ls files/usr/bin >/tmp/runas1.log 2>&1 && runas_status="ok" || true
    adb shell run-as com.termux.rafacodephi sh -lc 'echo ok' >/tmp/runas2.log 2>&1 || true
    procs="$(adb shell ps -A | grep -Ei 'termux|rafcodephi|com.termux.rafacodephi' || true)"
    if [[ "$install_status" == "ok" && "$launch_status" == "ok" ]]; then status="DEVICE_PARTIAL"; [[ "$runas_status" == "ok" ]] && status="DEVICE_VALIDATED"; else status="DEVICE_FAILED"; fi
  fi
fi
: "${model:=unknown}" "${abi:=unknown}" "${abilist:=unknown}" "${sdk:=unknown}" "${android_rel:=unknown}" "${page_size:=unknown}" "${procs:=}"
cat > reports/device_runtime_smoke.json <<JSON
{"timestamp_utc":"$ts","apk":"${apk:-not_found}","device_model":"$model","abi":"$abi","abilist":"$abilist","sdk":"$sdk","android_release":"$android_rel","page_size":"$page_size","install_status":"$install_status","launch_status":"$launch_status","run_as_status":"$runas_status","notes":"$notes","process_list":"$(echo "$procs" | tr '
' ';' | sed 's/"/\\"/g')","final_status":"$status"}
JSON
cat > reports/device_runtime_smoke.md <<MD
# Device Runtime Smoke

- Timestamp (UTC): $ts
- APK: ${apk:-not_found}
- Model: $model
- ABI: $abi ($abilist)
- SDK: $sdk (Android $android_rel)
- Page size: $page_size
- Install: $install_status
- Launch: $launch_status
- Run-as: $runas_status
- Notes: $notes
- Final status: $status
MD
