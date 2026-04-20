#!/usr/bin/env python3
"""Validate CI workflow matrix/profile consistency."""
import json
from pathlib import Path
import re
import subprocess
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
for token in ['resolve_abi_profile.py', ':app:verifyDeliveredCompiledArtifacts']:
    if token not in android_ci:
        print(f'android-ci missing token: {token}')
        sys.exit(1)

resolver = ROOT / 'tools/ci/resolve_abi_profile.py'
if not resolver.exists():
    print(f'missing resolver: {resolver}')
    sys.exit(1)

profile_pattern = re.compile(r'\bofficial_[a-z0-9_]+\b|\binternal_[a-z0-9_]+\b|\bgeneric\b')
profiles_to_check: set[str] = set()
for workflow in [
    ROOT / '.github/workflows/android.yml',
    ROOT / '.github/workflows/android-ci.yml',
    ROOT / '.github/workflows/compile-matrix.yml',
]:
    content = workflow.read_text(encoding='utf-8')
    for match in profile_pattern.findall(content):
        profiles_to_check.add(match)

for profile in sorted(profiles_to_check):
    result = subprocess.run(
        [
            sys.executable,
            str(resolver),
            '--profile',
            profile,
            '--allow-generic-fallback',
            '--format',
            'json',
        ],
        check=True,
        capture_output=True,
        text=True,
    )
    payload = json.loads(result.stdout)
    if payload.get('resolved_source') != 'official':
        print(f'abi profile {profile} resolved by adaptive generic fallback -> {payload["abi_profile"]}')

print('build matrix validation: ok')
