#!/usr/bin/env python3
"""Verifica dependências de arquivos locais referenciadas por Gradle/settings.

Objetivo: garantir que referências locais importantes existam dentro do repositório.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

GRADLE_FILES = [
    ROOT / "build.gradle",
    ROOT / "settings.gradle",
    ROOT / "app" / "build.gradle",
    ROOT / "terminal-emulator" / "build.gradle",
    ROOT / "terminal-view" / "build.gradle",
    ROOT / "shell-loader" / "build.gradle",
    ROOT / "shell-loader" / "stub" / "build.gradle",
]

FILE_CALL_RE = re.compile(r"file\((['\"])(.+?)\1\)")
ROOT_FILE_CALL_RE = re.compile(r"rootProject\.file\((['\"])(.+?)\1\)")
PROJECT_INCLUDE_RE = re.compile(r"include\s+(.+)")
PROJECT_TOKEN_RE = re.compile(r"['\"](:[^'\"]+)['\"]")
GRADLE_INTERPOLATION_RE = re.compile(r"(?<!\\)\$(?:\{[^}]+\}|[A-Za-z_]\w*)")

OPTIONAL_LOCAL_REFS = {
    "local.properties",
}

GENERATED_PATH_SEGMENTS = {
    "build/",
    "outputs/",
}

BUILD_FILES_FOR_LEGACY_SCAN = [
    ROOT / "tools" / "baremetal" / "rafcode_phi" / "build_rafcode_phi.sh",
]

LEGACY_REFERENCE_MAP = {
    "asm/rafaelia_core.S": "tools/baremetal/rafcode_phi/asm/rafcode_phi_emit_word.S",
}


def normalize_project_path(project_token: str) -> Path:
    # ':shell-loader:stub' -> 'shell-loader/stub'
    return ROOT / project_token.lstrip(":").replace(":", "/")


def has_gradle_interpolation(path: str) -> bool:
    """Retorna True se o caminho usa interpolação Gradle/Groovy ($var ou ${var})."""
    return bool(GRADLE_INTERPOLATION_RE.search(path))


def collect_references(text: str) -> list[tuple[str, bool]]:
    refs: list[tuple[str, bool]] = []
    root_file_ranges: list[tuple[int, int]] = []

    for match in ROOT_FILE_CALL_RE.finditer(text):
        path = match.group(2).strip()
        if not path or has_gradle_interpolation(path):
            continue
        refs.append((path, True))
        root_file_ranges.append((match.start(), match.end()))

    for match in FILE_CALL_RE.finditer(text):
        if any(start <= match.start() and match.end() <= end for start, end in root_file_ranges):
            continue
        path = match.group(2).strip()
        if not path or has_gradle_interpolation(path):
            continue
        refs.append((path, False))

    return refs


def should_skip_reference(ref: str) -> bool:
    if not ref:
        return True
    if ref.startswith("$") or "${" in ref:
        return True
    normalized = ref.replace("\\", "/").lstrip("./")
    return any(segment in normalized for segment in GENERATED_PATH_SEGMENTS)


def verify_gradle_files() -> tuple[list[str], list[str]]:
    checked: list[str] = []
    missing: list[str] = []

    for gradle_file in GRADLE_FILES:
        if not gradle_file.exists():
            missing.append(f"{gradle_file.relative_to(ROOT)} (arquivo gradle não encontrado)")
            continue

        text = gradle_file.read_text(encoding="utf-8")

        for ref, from_root in collect_references(text):
            if "*" in ref:
                continue
            if ref in OPTIONAL_LOCAL_REFS or should_skip_reference(ref):
                continue
            base_dir = ROOT if from_root else gradle_file.parent
            target = (base_dir / ref).resolve()
            checked.append(str(target.relative_to(ROOT)))
            if not target.exists():
                source_hint = "rootProject.file" if from_root else "file"
                missing.append(f"{target.relative_to(ROOT)} (referenciado via {source_hint} em {gradle_file.relative_to(ROOT)})")

        if gradle_file.name == "settings.gradle":
            for line in text.splitlines():
                if "include" not in line:
                    continue
                include_match = PROJECT_INCLUDE_RE.search(line)
                if not include_match:
                    continue
                for token in PROJECT_TOKEN_RE.findall(include_match.group(1)):
                    target_dir = normalize_project_path(token)
                    checked.append(str(target_dir.relative_to(ROOT)))
                    if not target_dir.exists():
                        missing.append(f"{target_dir.relative_to(ROOT)} (módulo {token} ausente)")

    return sorted(set(checked)), missing


def verify_legacy_build_references() -> list[str]:
    legacy_issues: list[str] = []

    for build_file in BUILD_FILES_FOR_LEGACY_SCAN:
        if not build_file.exists():
            continue
        text = build_file.read_text(encoding="utf-8")
        rel_build_file = build_file.relative_to(ROOT)
        for legacy_ref, replacement_ref in LEGACY_REFERENCE_MAP.items():
            has_effective_reference = False
            for raw_line in text.splitlines():
                line = raw_line.strip()
                if not line or line.startswith("#"):
                    continue
                if legacy_ref not in line:
                    continue
                if any(token in line for token in ("=", "file(", "rootProject.file(", " -c ")):
                    has_effective_reference = True
                    break
            if has_effective_reference:
                legacy_issues.append(
                    f"{rel_build_file} contém referência legada '{legacy_ref}'; use '{replacement_ref}'"
                )

    return legacy_issues


def main() -> int:
    checked, missing = verify_gradle_files()
    legacy_issues = verify_legacy_build_references()

    print("[verify_repo_file_dependencies] Arquivos/módulos verificados:")
    for item in checked:
        print(f"  - {item}")

    if missing:
        print("\n[verify_repo_file_dependencies] FALTANDO:")
        for item in missing:
            print(f"  - {item}")
        return 1

    if legacy_issues:
        print("\n[verify_repo_file_dependencies] REFERÊNCIAS LEGADAS:")
        for item in legacy_issues:
            print(f"  - {item}")
        return 1

    print("\n[verify_repo_file_dependencies] OK: todas as dependências locais de arquivo/módulo existem no repositório.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
