#!/usr/bin/env python3
"""Static sanity checks for root CMake compilation ordering patterns."""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[2]
cmake = (root / 'CMakeLists.txt').read_text(encoding='utf-8')

checks = {
    'rmr_unified_arena_selftest declared': r'rmr_add_host_executable\(rmr_unified_arena_selftest',
    'selftest target declared': r'add_custom_target\(run_selftest',
    'selftest command loop': r'foreach\(RMR_SELFTEST_CMD IN LISTS RMR_RUN_SELFTEST_COMMANDS\)',
    'CASM marker definition': r'VECTRA_HAS_CASM_MARKER',
}

for name, pattern in checks.items():
    if not re.search(pattern, cmake):
        print(f'failed: {name}')
        sys.exit(1)

print('compilation order sanity: ok')
