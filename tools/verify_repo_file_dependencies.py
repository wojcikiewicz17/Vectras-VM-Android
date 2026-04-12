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

BUILD_FILE_PATTERNS = (
    "*.sh",
    "*.mk",
    "CMakeLists.txt",
    "*.gradle",
    "*.gradle.kts",
)

LEGACY_SCAN_ROOTS = (
    ROOT,
    ROOT / "tools",
)

LEGACY_SCAN_EXCLUDED_DIRS = {
    "build",
    "outputs",
    ".git",
}

# Mensagens prescritivas para referências legadas detectadas em arquivos de build.
LEGACY_REFERENCE_MAP = {
    "termux-shared": "Use o módulo local equivalente no repositório em vez de depender de termux-shared legado.",
    "terminal-shared": "Substitua terminal-shared por módulos atuais versionados no repo (ex.: terminal-view/terminal-emulator).",
    "termux-app": "Não referencie termux-app legado; use os caminhos/módulos atuais deste repositório.",
}

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


def is_in_excluded_dir(path: Path) -> bool:
    return any(part in LEGACY_SCAN_EXCLUDED_DIRS for part in path.parts)


def discover_build_files() -> list[Path]:
    discovered: set[Path] = set()

    for scan_root in LEGACY_SCAN_ROOTS:
        if not scan_root.exists():
            continue
        for pattern in BUILD_FILE_PATTERNS:
            for candidate in scan_root.rglob(pattern):
                if not candidate.is_file() or is_in_excluded_dir(candidate):
                    continue
                discovered.add(candidate.resolve())

    return sorted(discovered)


def verify_legacy_build_references(build_files: list[Path]) -> list[str]:
    problems: list[str] = []

    for build_file in build_files:
        text = build_file.read_text(encoding="utf-8", errors="ignore")
        file_relative_path = build_file.relative_to(ROOT)

        for legacy_reference, guidance in LEGACY_REFERENCE_MAP.items():
            if legacy_reference in text:
                problems.append(
                    f"{legacy_reference} encontrado em {file_relative_path}: {guidance}"
                )

    return problems


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


def main() -> int:
    checked, missing = verify_gradle_files()
    build_files = discover_build_files()
    legacy_references = verify_legacy_build_references(build_files)

    print("[verify_repo_file_dependencies] Arquivos/módulos verificados:")
    for item in checked:
        print(f"  - {item}")

    if missing:
        print("\n[verify_repo_file_dependencies] FALTANDO:")
        for item in missing:
            print(f"  - {item}")

    if legacy_references:
        print("\n[verify_repo_file_dependencies] REFERÊNCIAS LEGADAS DETECTADAS:")
        for item in legacy_references:
            print(f"  - {item}")

    if missing or legacy_references:
        return 1

    print("\n[verify_repo_file_dependencies] OK: todas as dependências locais de arquivo/módulo existem no repositório.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
