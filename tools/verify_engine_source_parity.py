#!/usr/bin/env python3
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "engine/rmr/sources_rmr_core.cmake"
ROOT_CMAKE = ROOT / "CMakeLists.txt"
APP_CMAKE = ROOT / "app/src/main/cpp/CMakeLists.txt"
MAKEFILE = ROOT / "Makefile"
MK_MANIFEST = ROOT / "engine/rmr/sources_rmr_core.mk"


def fail(msg: str) -> None:
    print(f"[source-parity] ERROR: {msg}")
    sys.exit(1)


def extract_manifest(var: str) -> list[str]:
    text = MANIFEST.read_text()
    m = re.search(rf"set\({var}\s*(.*?)\)", text, re.S)
    if not m:
        fail(f"variable {var} not found in {MANIFEST}")
    block = m.group(1)
    items = []
    for line in block.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        line = line.split("#", 1)[0].strip()
        if not line:
            continue
        line = line.replace("${RMR_REPO_ROOT}/", "")
        items.append(line)
    return items


def extract_make_block(path: Path, var: str) -> list[str]:
    text = path.read_text()
    m = re.search(rf"^{var}\s*:?=\s*(.*?)\n\n", text, re.M | re.S)
    if not m:
        fail(f"variable {var} not found in {path}")
    block = m.group(1).replace("\\\n", "\n")
    items = []
    for line in block.splitlines():
        line = line.strip().rstrip("\\").strip()
        if not line or line.startswith("$"):
            continue
        items.append(line)
    return items


def extract_make_inline(var: str) -> list[str]:
    text = MAKEFILE.read_text()
    m = re.search(rf"^{var}\s*:?=\s*(.+)$", text, re.M)
    if not m:
        fail(f"variable {var} not found in {MAKEFILE}")
    return [line.strip() for line in m.group(1).split() if line.strip()]


def ensure_contains(path: Path, snippet: str) -> None:
    if snippet not in path.read_text():
        fail(f"expected snippet missing in {path}: {snippet}")


def compare(label: str, left: list[str], right: list[str]) -> None:
    if left == right:
        return
    fail(
        f"{label} mismatch\n  left-only: {sorted(set(left)-set(right))}\n  right-only: {sorted(set(right)-set(left))}"
    )


def main() -> None:
    manifest_core = extract_manifest("RMR_SOURCE_GROUP_CORE")
    manifest_host = extract_manifest("RMR_SOURCE_GROUP_HOST_ONLY")
    manifest_policy = extract_manifest("RMR_SOURCE_GROUP_OPTIONAL_POLICY")
    manifest_android = extract_manifest("RMR_SOURCE_GROUP_ANDROID_ONLY")
    manifest_arm64_neon = extract_manifest("RMR_SOURCE_GROUP_ASM_ARM64_NEON")

    mk_core = extract_make_block(MK_MANIFEST, "RMR_SOURCE_GROUP_CORE")
    mk_host = extract_make_block(MK_MANIFEST, "RMR_SOURCE_GROUP_HOST_ONLY")
    mk_policy = extract_make_block(MK_MANIFEST, "RMR_SOURCE_GROUP_OPTIONAL_POLICY")
    mk_android = extract_make_block(MK_MANIFEST, "RMR_SOURCE_GROUP_ANDROID_ONLY")
    mk_arm64_neon = extract_make_block(MK_MANIFEST, "RMR_SOURCE_GROUP_ASM_ARM64_NEON")

    compare("core group sources", manifest_core, mk_core)
    compare("host-only sources", manifest_host, mk_host)
    compare("policy sources", manifest_policy, mk_policy)
    compare("android-only sources", manifest_android, mk_android)
    compare("arm64 neon sources", manifest_arm64_neon, mk_arm64_neon)

    compare("make ENGINE_CORE_SRCS mapping", ["$(RMR_SOURCE_GROUP_CORE)", "$(RMR_SOURCE_GROUP_HOST_ONLY)"], extract_make_inline("ENGINE_CORE_SRCS"))
    compare("make ENGINE_POLICY_SRCS mapping", ["$(RMR_SOURCE_GROUP_OPTIONAL_POLICY)"], extract_make_inline("ENGINE_POLICY_SRCS"))
    compare("make ENGINE_ASM_ARM64_NEON_SRCS mapping", ["$(RMR_SOURCE_GROUP_ASM_ARM64_NEON)"], extract_make_inline("ENGINE_ASM_ARM64_NEON_SRCS"))

    ensure_contains(ROOT_CMAKE, "include(${CMAKE_SOURCE_DIR}/engine/rmr/sources_rmr_core.cmake)")
    ensure_contains(ROOT_CMAKE, "${RMR_SOURCE_GROUP_CORE}")
    ensure_contains(ROOT_CMAKE, "${RMR_SOURCE_GROUP_HOST_ONLY}")
    ensure_contains(APP_CMAKE, "include(${VECTRA_REPO_ROOT}/engine/rmr/sources_rmr_core.cmake)")
    ensure_contains(APP_CMAKE, "${RMR_SOURCE_GROUP_CORE}")
    ensure_contains(APP_CMAKE, "${RMR_SOURCE_GROUP_ANDROID_ONLY}")

    print("[source-parity] OK: manifest, CMake, and Makefile lists are aligned")


if __name__ == "__main__":
    main()
