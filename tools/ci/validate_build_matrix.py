#!/usr/bin/env python3
"""Validate CI workflow matrix/profile consistency."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[2]
required = [
    ROOT / '.github/workflows/pipeline-orchestrator.yml',
    ROOT / '.github/workflows/android-ci.yml',
    ROOT / '.github/workflows/host-ci.yml',
    ROOT / '.github/workflows/quality-gates.yml',
    ROOT / '.github/workflows/compile-matrix.yml',
]

missing = [str(p) for p in required if not p.exists()]
if missing:
    print('missing workflows:')
    for item in missing:
        print(f' - {item}')
    sys.exit(1)

orch = (ROOT / '.github/workflows/pipeline-orchestrator.yml').read_text(encoding='utf-8')
for token in ['host_only', 'android_only', 'full', 'quality-gates']:
    if token not in orch:
        print(f'orchestrator missing token: {token}')
        sys.exit(1)

android_ci = (ROOT / '.github/workflows/android-ci.yml').read_text(encoding='utf-8')
for token in ['official_arm64', 'internal_arm32_arm64', ':app:verifyDeliveredCompiledArtifacts']:
    if token not in android_ci:
        print(f'android-ci missing token: {token}')
        sys.exit(1)

print('build matrix validation: ok')
