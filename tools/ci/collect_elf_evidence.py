#!/usr/bin/env python3
"""Collect ELF evidence for JNI shared libraries.

Runs `file` and `readelf -h` for a given `.so` path and prints JSON to stdout.
"""

from __future__ import annotations

import json
import shutil
import subprocess
import sys
from pathlib import Path


def run_command(args: list[str]) -> dict:
    try:
        proc = subprocess.run(args, capture_output=True, text=True, check=False)
    except Exception as exc:  # pragma: no cover - defensive path
        return {"exit_code": -1, "output": str(exc)}

    output = (proc.stdout or "").strip()
    stderr = (proc.stderr or "").strip()
    if stderr:
        output = "\n".join(part for part in [output, stderr] if part)
    return {"exit_code": proc.returncode, "output": output}


def main() -> int:
    if len(sys.argv) != 2:
        print(json.dumps({"error": "usage: collect_elf_evidence.py <path-to-so>"}))
        return 2

    so_path = Path(sys.argv[1]).resolve()
    if not so_path.exists() or not so_path.is_file():
        print(json.dumps({"error": f"file not found: {so_path}"}))
        return 3

    evidence: dict[str, object] = {
        "path": str(so_path),
        "tool_status": {"file": "missing", "readelf": "missing"},
        "file_output": "",
        "readelf_header": "",
    }

    file_tool = shutil.which("file")
    readelf_tool = shutil.which("readelf")

    if file_tool:
        file_result = run_command([file_tool, str(so_path)])
        evidence["tool_status"]["file"] = "ok" if file_result["exit_code"] == 0 else "error"
        evidence["file_output"] = file_result["output"]

    if readelf_tool:
        readelf_result = run_command([readelf_tool, "-h", str(so_path)])
        evidence["tool_status"]["readelf"] = "ok" if readelf_result["exit_code"] == 0 else "error"
        if readelf_result["output"]:
            header_lines = []
            for line in readelf_result["output"].splitlines():
                striped = line.strip()
                if striped.startswith(("Class:", "Data:", "Machine:", "OS/ABI:", "Type:")):
                    header_lines.append(striped)
            evidence["readelf_header"] = " | ".join(header_lines)

    print(json.dumps(evidence, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
