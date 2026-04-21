#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

LOG_PREFIX="[forks-sync]"
MANIFEST_FILE="${FORKS_MANIFEST_FILE:-tools/termux-arm64-orchestrator/fork-manifests/forks-sources.json}"
ALLOW_NETWORK_FORKS="${ALLOW_NETWORK_FORKS:-1}"

log() { echo "$LOG_PREFIX $*"; }

if [[ ! -f "$MANIFEST_FILE" ]]; then
  echo "$LOG_PREFIX manifest not found: $MANIFEST_FILE" >&2
  exit 1
fi

python3 - "$ROOT_DIR" "$MANIFEST_FILE" "$ALLOW_NETWORK_FORKS" <<'PY'
import io
import json
import os
import shutil
import sys
import tarfile
import tempfile
import urllib.request

root, manifest_file, allow_network = sys.argv[1:4]
allow_network = allow_network == "1"

with open(manifest_file, 'r', encoding='utf-8') as f:
    m = json.load(f)

destination = m.get('destination', '.third_party_forks')
out_root = os.path.join(root, destination)
os.makedirs(out_root, exist_ok=True)

forks = m.get('forks', [])
if not forks:
    print('[forks-sync] no forks declared; nothing to sync')
    sys.exit(0)

def safe_extract(tf, path):
    abs_path = os.path.abspath(path)
    for member in tf.getmembers():
        target = os.path.abspath(os.path.join(path, member.name))
        if not target.startswith(abs_path + os.sep):
            raise RuntimeError(f'unsafe tar path detected: {member.name}')
    tf.extractall(path)

for item in forks:
    name = item.get('name', '').strip()
    repo = item.get('repo', '').strip()
    ref = item.get('ref', '').strip() or 'main'
    rel_path = item.get('path', '').strip() or name
    required = bool(item.get('required', False))

    if not name or not repo or not rel_path:
        print('[forks-sync] invalid manifest entry missing name/repo/path', file=sys.stderr)
        sys.exit(1)

    dest = os.path.join(out_root, rel_path)
    if os.path.isdir(dest) and os.listdir(dest):
        print(f'[forks-sync] already present: {name} -> {dest}')
        continue

    if not allow_network:
        if required:
            print(f'[forks-sync] required fork missing locally and ALLOW_NETWORK_FORKS=0: {name}', file=sys.stderr)
            sys.exit(1)
        print(f'[forks-sync] skipping optional fork (network disabled): {name}')
        continue

    url = f'https://codeload.github.com/{repo}/tar.gz/{ref}'
    print(f'[forks-sync] downloading {name} from {url}')
    try:
        with urllib.request.urlopen(url, timeout=60) as resp:
            data = resp.read()
    except Exception as e:
        if required:
            print(f'[forks-sync] failed required fork download {name}: {e}', file=sys.stderr)
            sys.exit(1)
        print(f'[forks-sync] failed optional fork download {name}: {e}')
        continue

    tmp_extract = tempfile.mkdtemp(prefix='forksync-', dir=out_root)
    try:
        with tarfile.open(fileobj=io.BytesIO(data), mode='r:gz') as tf:
            safe_extract(tf, tmp_extract)

        roots = [x for x in os.listdir(tmp_extract) if os.path.isdir(os.path.join(tmp_extract, x))]
        if not roots:
            raise RuntimeError('downloaded tar has no root folder')

        extracted_root = os.path.join(tmp_extract, roots[0])
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        if os.path.isdir(dest):
            shutil.rmtree(dest)
        shutil.move(extracted_root, dest)
        print(f'[forks-sync] synced {name} -> {dest}')
    except Exception as e:
        if required:
            print(f'[forks-sync] failed required fork sync {name}: {e}', file=sys.stderr)
            sys.exit(1)
        print(f'[forks-sync] failed optional fork sync {name}: {e}')
    finally:
        shutil.rmtree(tmp_extract, ignore_errors=True)
PY
