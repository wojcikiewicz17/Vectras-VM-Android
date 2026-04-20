#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path

VALID_STATUSES = {"pendente", "em_avaliacao", "integrado", "descartado"}


@dataclass(frozen=True)
class MapEntry:
    source: str
    status: str
    target: str
    promotion_plan: str


def fail(msg: str) -> int:
    print(f"[incoming-ingestion] ERROR: {msg}", file=sys.stderr)
    return 1


def parse_map(map_file: Path) -> tuple[dict[str, MapEntry], list[str]]:
    entries: dict[str, MapEntry] = {}
    errors: list[str] = []
    row_pattern = re.compile(r"^\|\s*`(_incoming/[^`]+\.(?:c|S))`\s*\|\s*`([^`]+)`\s*\|\s*`([^`]*)`\s*\|\s*`([^`]*)`\s*\|\s*$")

    for lineno, raw_line in enumerate(map_file.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw_line.strip()
        if not line.startswith("|"):
            continue
        if line.startswith("| Arquivo") or line.startswith("|---"):
            continue

        match = row_pattern.match(line)
        if not match:
            continue

        source, status, target, promotion_plan = match.groups()
        status = status.strip()
        target = target.strip()
        promotion_plan = promotion_plan.strip()

        if source in entries:
            errors.append(f"Entrada duplicada para {source} em {map_file}:{lineno}")
            continue

        if status not in VALID_STATUSES:
            errors.append(
                f"Status inválido para {source} em {map_file}:{lineno}. "
                f"Status válidos: {', '.join(sorted(VALID_STATUSES))}."
            )
            continue

        if not promotion_plan:
            errors.append(f"Plano de promoção vazio para {source} em {map_file}:{lineno}")
            continue

        entries[source] = MapEntry(
            source=source,
            status=status,
            target=target,
            promotion_plan=promotion_plan,
        )

    return entries, errors


def list_incoming_sources(repo_root: Path, incoming_dir: Path) -> list[str]:
    paths = []
    for path in sorted(incoming_dir.glob("*")):
        if path.suffix in {".c", ".S"}:
            paths.append(path.relative_to(repo_root).as_posix())
    return paths


def check_integrated_targets(repo_root: Path, entries: dict[str, MapEntry]) -> list[str]:
    errors: list[str] = []
    for source, entry in entries.items():
        if entry.status != "integrado":
            continue

        if not entry.target or entry.target == "-":
            errors.append(f"{source} está marcado como integrado, mas target_path está vazio.")
            continue

        target_path = repo_root / entry.target
        if not target_path.exists():
            errors.append(
                f"{source} está marcado como integrado, mas target_path não existe: {entry.target}"
            )
    return errors


def run_check(repo_root: Path, map_path: str, incoming_dir: str) -> int:
    map_file = repo_root / map_path
    incoming_path = repo_root / incoming_dir

    if not map_file.exists():
        return fail(f"Arquivo de mapa não encontrado: {map_file}")

    if not incoming_path.is_dir():
        return fail(f"Diretório de incoming não encontrado: {incoming_path}")

    entries, parse_errors = parse_map(map_file)
    if parse_errors:
        for err in parse_errors:
            print(f"[incoming-ingestion] ERROR: {err}", file=sys.stderr)
        return 1

    incoming_sources = list_incoming_sources(repo_root, incoming_path)
    missing = [src for src in incoming_sources if src not in entries]
    if missing:
        for src in missing:
            print(
                "[incoming-ingestion] ERROR: "
                f"Arquivo sem linha de status válida no mapa: {src} ({map_path})",
                file=sys.stderr,
            )
        return 1

    integrated_errors = check_integrated_targets(repo_root, entries)
    if integrated_errors:
        for err in integrated_errors:
            print(f"[incoming-ingestion] ERROR: {err}", file=sys.stderr)
        return 1

    print(
        "[incoming-ingestion] OK: "
        f"{len(incoming_sources)} arquivo(s) _incoming/*{{.c,.S}} mapeado(s) em {map_path}."
    )
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Valida o contrato de ingestão de _incoming para CI."
    )
    parser.add_argument(
        "--map-file",
        default="docs/INCOMING_INGESTION_MAP.md",
        help="Arquivo de mapa de ingestão relativo à raiz do repo.",
    )
    parser.add_argument(
        "--incoming-dir",
        default="_incoming",
        help="Diretório de incoming relativo à raiz do repo.",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    repo_root = Path(__file__).resolve().parents[2]
    return run_check(repo_root, args.map_file, args.incoming_dir)


if __name__ == "__main__":
    raise SystemExit(main())
