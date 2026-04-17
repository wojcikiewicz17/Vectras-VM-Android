#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


DEFAULT_SIGNATURE_REGEX = r"public\s+static\s+int\s+torusFlowChecksum\s*\(\s*int\s+seed\s*,\s*int\s+steps\s*\)"


def fail(msg: str) -> int:
    print(f"[java-contract] ERROR: {msg}", file=sys.stderr)
    return 1


def strip_java_comments(content: str) -> str:
    content = re.sub(r"/\*.*?\*/", "", content, flags=re.DOTALL)
    content = re.sub(r"//.*?$", "", content, flags=re.MULTILINE)
    return content


def validate_signature_count(java_file: Path, signature_regex: str, expected_count: int) -> int:
    if not java_file.exists():
        return fail(f"Arquivo não encontrado: {java_file}")

    content = java_file.read_text(encoding="utf-8")
    sanitized = strip_java_comments(content)
    pattern = re.compile(signature_regex)
    found = len(list(pattern.finditer(sanitized)))

    if found != expected_count:
        return fail(
            f"Assinatura regex='{signature_regex}' deve existir exatamente {expected_count} vez(es); "
            f"encontrado={found} em {java_file}"
        )
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Valida contratos críticos de assinaturas Java para CI Android.")
    parser.add_argument(
        "--java-file",
        default="app/src/main/java/com/vectras/vm/core/NativeFastPath.java",
        help="Arquivo Java alvo (relativo à raiz do repo).",
    )
    parser.add_argument(
        "--signature-regex",
        default=DEFAULT_SIGNATURE_REGEX,
        help="Regex da assinatura a validar.",
    )
    parser.add_argument(
        "--expected-count",
        type=int,
        default=1,
        help="Quantidade esperada de ocorrências da assinatura.",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    repo_root = Path(__file__).resolve().parents[2]
    java_file = repo_root / args.java_file
    rc = validate_signature_count(java_file, args.signature_regex, args.expected_count)
    if rc != 0:
        return rc

    print(
        "[java-contract] OK: "
        f"{java_file} contém {args.expected_count} ocorrência(s) da assinatura alvo."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
