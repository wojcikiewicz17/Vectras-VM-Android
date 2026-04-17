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

LEGACY_BUILD_REFERENCE_RE = re.compile(r"(?<![A-Za-z0-9_])android/app/build\.gradle(?![A-Za-z0-9_])")
LINE_CONTINUATION_RE = re.compile(r"(?<!\\)(?:\\\\)*\\\s*$")

# Política explícita: manter referências legadas apenas para mensagens estáticas
# de documentação/erro já existentes (ex.: falha informativa em validação).
ALLOWED_LEGACY_LITERAL_FRAGMENTS = {
    "Arquivo legado ausente: android/app/build.gradle",
    "legacy_app_gradle=\"$ROOT_DIR/android/app/build.gradle\"",
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


def _strip_active_content_by_type(line: str, file_type: str, in_block_comment: bool) -> tuple[str, bool]:
    """Remove comentários de uma linha mantendo somente conteúdo ativo.

    Suporta blocos multiline simples (/* ... */) para gradle/cmake.
    """
    active_parts: list[str] = []
    index = 0
    length = len(line)

    while index < length:
        if in_block_comment:
            end_idx = line.find("*/", index)
            if end_idx == -1:
                return "".join(active_parts), True
            index = end_idx + 2
            in_block_comment = False
            continue

        if file_type in {"gradle", "cmake"} and line.startswith("/*", index):
            in_block_comment = True
            index += 2
            continue
        if file_type == "gradle" and line.startswith("//", index):
            break
        if file_type in {"shell", "cmake"} and line.startswith("#", index):
            break
        if file_type in {"shell", "cmake"} and line[index] == "#" and index > 0 and line[index - 1].isspace():
            break

        active_parts.append(line[index])
        index += 1

    return "".join(active_parts), in_block_comment


def _detect_file_type(path: Path) -> str:
    suffix = path.suffix.lower()
    if suffix in {".sh", ".bash"}:
        return "shell"
    if suffix in {".gradle", ".gradle.kts"}:
        return "gradle"
    if suffix in {".cmake"} or path.name == "CMakeLists.txt":
        return "cmake"
    return "plain"


def scan_legacy_android_app_build_reference(path: Path, text: str) -> list[str]:
    """Retorna ocorrências ativas de referência legada `android/app/build.gradle`.

    Regras:
    - ignora comentários por tipo de arquivo (shell/gradle/cmake);
    - considera continuação de linha (`\\`) e bloco multiline simples;
    - preserva exceções explícitas para literais de documentação/erro.
    """
    file_type = _detect_file_type(path)
    findings: list[str] = []
    in_block_comment = False
    logical_line = ""
    logical_start = 1

    for line_no, raw_line in enumerate(text.splitlines(), start=1):
        if not logical_line:
            logical_start = line_no
        logical_line += raw_line if not logical_line else f"\n{raw_line}"
        if LINE_CONTINUATION_RE.search(raw_line):
            logical_line = LINE_CONTINUATION_RE.sub("", logical_line)
            continue

        active_chunks: list[str] = []
        for chunk in logical_line.splitlines():
            active_chunk, in_block_comment = _strip_active_content_by_type(chunk, file_type, in_block_comment)
            if active_chunk.strip():
                active_chunks.append(active_chunk)
        active_text = " ".join(active_chunks)

        if active_text:
            is_allowed_literal = any(fragment in active_text for fragment in ALLOWED_LEGACY_LITERAL_FRAGMENTS)
            if not is_allowed_literal and LEGACY_BUILD_REFERENCE_RE.search(active_text):
                findings.append(f"{path}:{logical_start}")

        logical_line = ""

    if logical_line:
        active_text, _ = _strip_active_content_by_type(logical_line, file_type, in_block_comment)
        if active_text and not any(fragment in active_text for fragment in ALLOWED_LEGACY_LITERAL_FRAGMENTS):
            if LEGACY_BUILD_REFERENCE_RE.search(active_text):
                findings.append(f"{path}:{logical_start}")

    return findings


def discover_build_files() -> list[Path]:
    build_files: list[Path] = []
    seen: set[Path] = set()

    for scan_root in LEGACY_SCAN_ROOTS:
        if not scan_root.exists():
            continue
        for pattern in BUILD_FILE_PATTERNS:
            for candidate in scan_root.rglob(pattern):
                if not candidate.is_file():
                    continue
                rel = candidate.relative_to(ROOT)
                if any(part in LEGACY_SCAN_EXCLUDED_DIRS for part in rel.parts):
                    continue
                resolved = candidate.resolve()
                if resolved in seen:
                    continue
                seen.add(resolved)
                build_files.append(resolved)

    return sorted(build_files)


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


def verify_legacy_build_references(build_files: list[Path]) -> tuple[list[str], list[str]]:
    legacy_issues: list[str] = []
    forbidden_android_app_refs: list[str] = []

    for build_file in build_files:
        if not build_file.exists():
            continue
        text = build_file.read_text(encoding="utf-8")
        rel_build_file = build_file.relative_to(ROOT)

        forbidden_android_app_refs.extend(scan_legacy_android_app_build_reference(rel_build_file, text))

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

    return sorted(set(legacy_issues)), sorted(set(forbidden_android_app_refs))


def main() -> int:
    checked, missing = verify_gradle_files()
    build_files = discover_build_files()
    legacy_issues, legacy_references = verify_legacy_build_references(build_files)

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

    if legacy_issues:
        print("\n[verify_repo_file_dependencies] REFERÊNCIAS LEGADAS DETECTADAS:")
        for item in legacy_issues:
            print(f"  - {item}")

    if missing or legacy_issues or legacy_references:
        return 1

    print("\n[verify_repo_file_dependencies] OK: todas as dependências locais de arquivo/módulo existem no repositório.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
