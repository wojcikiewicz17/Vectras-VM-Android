#!/usr/bin/env python3
"""Auditoria completa de todos os arquivos versionados no repositório.

Saídas:
- reports/full_repo_audit.tsv
- reports/FULL_REPO_AUDIT_REPORT.md
"""
from __future__ import annotations

import subprocess
from collections import Counter
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
REPORTS = ROOT / "reports"
TSV = REPORTS / "full_repo_audit.tsv"
REPORT = REPORTS / "FULL_REPO_AUDIT_REPORT.md"

MD_LINK_RE = re.compile(r"\[[^\]]+\]\(([^)]+)\)")


def git_files() -> list[Path]:
    out = subprocess.check_output(["git", "ls-files"], cwd=ROOT, text=True)
    return [ROOT / p for p in out.splitlines() if p.strip()]


def is_text(data: bytes) -> bool:
    if b"\x00" in data:
        return False
    try:
        data.decode("utf-8")
        return True
    except UnicodeDecodeError:
        return False


def markdown_broken_links(path: Path, text: str) -> int:
    broken = 0
    for link in MD_LINK_RE.findall(text):
        link = link.strip()
        if not link or "://" in link or link.startswith("#") or link.startswith("mailto:"):
            continue
        target = link.split("#", 1)[0].strip()
        if not target:
            continue
        candidate = (path.parent / target).resolve()
        if not candidate.exists():
            broken += 1
    return broken


def main() -> int:
    REPORTS.mkdir(parents=True, exist_ok=True)
    files = sorted(git_files(), key=lambda p: str(p.relative_to(ROOT)))

    ext_counter: Counter[str] = Counter()
    topdir_counter: Counter[str] = Counter()
    issue_counter: Counter[str] = Counter()

    text_files = 0
    binary_files = 0
    total_lines = 0
    directories_seen = set()

    with TSV.open("w", encoding="utf-8") as out:
        out.write(
            "path\tkind\tsize\tlines\text\tissues\tsha\n"
        )
        for p in files:
            rel = p.relative_to(ROOT)
            rels = str(rel)
            data = p.read_bytes()
            size = len(data)
            ext = p.suffix.lower() or "<noext>"
            ext_counter[ext] += 1
            top = rel.parts[0] if len(rel.parts) > 1 else "<root>"
            topdir_counter[top] += 1
            for i in range(1, len(rel.parts)):
                directories_seen.add("/".join(rel.parts[:i]))

            issues: list[str] = []
            lines = 0
            if is_text(data):
                text_files += 1
                text = data.decode("utf-8")
                if "\r\n" in text:
                    issues.append("CRLF")
                    issue_counter["CRLF"] += 1
                split = text.splitlines()
                lines = len(split)
                total_lines += lines
                for line in split:
                    if line.endswith(" ") or line.endswith("\t"):
                        issues.append("trailing-whitespace")
                        issue_counter["trailing-whitespace"] += 1
                if text and not text.endswith("\n"):
                    issues.append("missing-final-newline")
                    issue_counter["missing-final-newline"] += 1
                if p.suffix.lower() == ".md":
                    broken = markdown_broken_links(p, text)
                    if broken:
                        issues.append(f"broken-md-links:{broken}")
                        issue_counter["broken-md-links"] += broken
                kind = "text"
            else:
                binary_files += 1
                kind = "binary"

            issue_str = ",".join(issues) if issues else "-"
            out.write(f"{rels}\t{kind}\t{size}\t{lines}\t{ext}\t{issue_str}\t-\n")

    with REPORT.open("w", encoding="utf-8") as rep:
        rep.write("# Auditoria Completa de Todos os Arquivos\n\n")
        rep.write("Escopo: todos os arquivos versionados retornados por `git ls-files`.\n\n")
        rep.write(f"- Arquivos auditados: **{len(files)}**\n")
        rep.write(f"- Diretórios auditados (derivados dos paths versionados): **{len(directories_seen)}**\n")
        rep.write(f"- Arquivos de texto: **{text_files}**\n")
        rep.write(f"- Arquivos binários: **{binary_files}**\n")
        rep.write(f"- Total de linhas de texto inspecionadas: **{total_lines}**\n")
        rep.write(f"- Inventário detalhado por arquivo: `{TSV.relative_to(ROOT)}`\n\n")

        rep.write("## Top 20 extensões\n")
        for ext, count in ext_counter.most_common(20):
            rep.write(f"- `{ext}`: {count}\n")

        rep.write("\n## Top 20 diretórios raiz\n")
        for d, count in topdir_counter.most_common(20):
            rep.write(f"- `{d}`: {count}\n")

        rep.write("\n## Inconsistências detectadas (contagem de ocorrências)\n")
        if issue_counter:
            for issue, count in issue_counter.most_common():
                rep.write(f"- `{issue}`: {count}\n")
        else:
            rep.write("- Nenhuma inconsistência detectada nas regras automáticas desta auditoria.\n")

        rep.write("\n## Método\n")
        rep.write("1. Enumeração determinística com `git ls-files`.\n")
        rep.write("2. Classificação text/binary por UTF-8 + presença de byte nulo.\n")
        rep.write("3. Regras de coerência: CRLF, trailing whitespace, newline final e links Markdown quebrados.\n")
        rep.write("4. Registro linha a linha no TSV para rastreabilidade completa.\n")

    print(f"OK: {TSV.relative_to(ROOT)}")
    print(f"OK: {REPORT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
