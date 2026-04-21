#!/usr/bin/env python3
"""Executa auditoria enxuta ISO 8000/27000 em 8 pontos com trilha DMAIC (Six Sigma)."""

from __future__ import annotations

import argparse
import json
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_CONTROLS = ROOT / "tools" / "ci" / "iso8000_27000_sixsigma_controls.json"
DEFAULT_OUT_DIR = ROOT / "reports" / "iso-sixsigma"


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--controls", default=str(DEFAULT_CONTROLS))
    p.add_argument("--out-dir", default=str(DEFAULT_OUT_DIR))
    p.add_argument("--strict", action="store_true", help="falha quando existir controle sem evidência")
    return p.parse_args()


def classify(found: list[str]) -> str:
    if len(found) >= 2:
        return "pass"
    if len(found) == 1:
        return "warning"
    return "fail"


def main() -> int:
    args = parse_args()
    controls_doc = json.loads(Path(args.controls).read_text(encoding="utf-8"))
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    results: list[dict] = []
    fail_count = 0

    for ctrl in controls_doc["controls"]:
        found = []
        for rel in ctrl["evidence_any"]:
            if (ROOT / rel).exists():
                found.append(rel)
        status = classify(found)
        if status == "fail":
            fail_count += 1
        dmaic = {
            "define": f"Controle {ctrl['id']} - {ctrl['title']}",
            "measure": f"evidências_encontradas={len(found)}",
            "analyze": "lacuna_detectada" if status != "pass" else "cobertura_suficiente",
            "improve": "adicionar/religar gate faltante" if status != "pass" else "manter baseline e monitorar drift",
            "control": "reexecutar auditoria em toda PR e release",
        }
        results.append(
            {
                "id": ctrl["id"],
                "title": ctrl["title"],
                "status": status,
                "expected_evidence": ctrl["evidence_any"],
                "found_evidence": found,
                "dmaic": dmaic,
            }
        )

    summary = {
        "schema": "vectras-iso-sixsigma-audit-v1",
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "controls": len(results),
        "pass": sum(1 for r in results if r["status"] == "pass"),
        "warning": sum(1 for r in results if r["status"] == "warning"),
        "fail": fail_count,
        "gate": "fail" if fail_count > 0 else "pass",
        "results": results,
    }

    json_path = out_dir / "audit.json"
    json_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")

    md_path = out_dir / "audit.md"
    lines = [
        "# Auditoria ISO 8000 / ISO 27000 com Six Sigma (8 pontos)",
        "",
        f"- generated_at: `{summary['generated_at']}`",
        f"- gate: `{summary['gate']}`",
        f"- pass/warning/fail: `{summary['pass']}/{summary['warning']}/{summary['fail']}`",
        "",
    ]
    for item in results:
        lines.append(f"## {item['id']} - {item['title']}")
        lines.append(f"- status: `{item['status']}`")
        lines.append(f"- evidence_found: `{', '.join(item['found_evidence']) or 'none'}`")
        lines.append(
            f"- DMAIC: D={item['dmaic']['define']} | M={item['dmaic']['measure']} | "
            f"A={item['dmaic']['analyze']} | I={item['dmaic']['improve']} | C={item['dmaic']['control']}"
        )
        lines.append("")
    md_path.write_text("\n".join(lines), encoding="utf-8")

    print(json.dumps({"audit_json": str(json_path.relative_to(ROOT)), "audit_md": str(md_path.relative_to(ROOT)), "gate": summary["gate"]}))

    if args.strict and summary["gate"] == "fail":
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
