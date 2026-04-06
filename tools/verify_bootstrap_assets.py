#!/usr/bin/env python3
"""Valida presença e integridade básica dos bootstraps versionados no app.
Pode ser executado diretamente (./tools/verify_bootstrap_assets.py) ou via python3.
"""

from __future__ import annotations

import argparse
import hashlib
import os
import sys
import tarfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BOOTSTRAP_DIR = ROOT / "app" / "src" / "main" / "assets" / "bootstrap"
GENERATED_BOOTSTRAP_DIR = ROOT / "app" / "build" / "generated" / "bootstrapAssets" / "bootstrap"
REQUIRED_BOOTSTRAPS = [
    "arm64-v8a.tar",
    "armeabi-v7a.tar",
    "x86.tar",
    "x86_64.tar",
]
LOADER_APK_NAME = "loader.apk"
TERMUX_MARKERS = [
    ROOT / "app" / "src" / "main" / "java" / "com" / "termux",
    ROOT / "app" / "src" / "main" / "AndroidManifest.xml",
]
STRICT_ENV_VAR = "VERIFY_BOOTSTRAP_STRICT_GENERATED_ASSETS"
CI_ENV_VARS = ("CI", "GITHUB_ACTIONS", "GITLAB_CI", "BUILDKITE")


def sha256_prefix(path: Path, limit_bytes: int = 1024 * 1024) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        remaining = limit_bytes
        while remaining > 0:
            chunk = handle.read(min(65536, remaining))
            if not chunk:
                break
            digest.update(chunk)
            remaining -= len(chunk)
    return digest.hexdigest()[:16]


def validate_tar(path: Path) -> tuple[int, str]:
    with tarfile.open(path, "r") as archive:
        members = archive.getmembers()
        if not members:
            raise RuntimeError("arquivo tar vazio")
        return len(members), members[0].name


def is_termux_enabled() -> bool:
    termux_dir = TERMUX_MARKERS[0]
    if termux_dir.exists():
        return True

    manifest_path = TERMUX_MARKERS[1]
    if manifest_path.exists():
        manifest_text = manifest_path.read_text(encoding="utf-8", errors="ignore")
        return "com.termux" in manifest_text
    return False


def env_flag_enabled(name: str) -> bool:
    value = os.environ.get(name, "").strip().lower()
    return value in {"1", "true", "yes", "on"}


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Valida a presença dos bootstraps versionados e do loader gerado por syncShellLoaderBootstrap."
        )
    )
    parser.add_argument(
        "--strict-generated-assets",
        action="store_true",
        help=(
            "Exige assets gerados (incluindo bootstrap/loader.apk) mesmo fora de CI. "
            f"Também pode ser ativado por {STRICT_ENV_VAR}=1."
        ),
    )
    return parser.parse_args(argv)


def should_require_generated_loader(strict_generated_assets: bool) -> tuple[bool, str]:
    if strict_generated_assets:
        return True, "modo estrito habilitado"

    for var_name in CI_ENV_VARS:
        if os.environ.get(var_name):
            return True, f"ambiente de CI detectado ({var_name})"

    if GENERATED_BOOTSTRAP_DIR.exists():
        return True, (
            f"diretório de gerados já existe ({GENERATED_BOOTSTRAP_DIR.relative_to(ROOT)}), "
            "indicando que syncShellLoaderBootstrap já deveria ter copiado o loader"
        )

    return False, "checkout limpo sem execução prévia de syncShellLoaderBootstrap"


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or [])
    strict_generated_assets = args.strict_generated_assets or env_flag_enabled(STRICT_ENV_VAR)
    print("[verify_bootstrap_assets] Validando bootstraps do repositório...")

    if not BOOTSTRAP_DIR.exists():
        print(f"[verify_bootstrap_assets] FALHA: diretório ausente: {BOOTSTRAP_DIR.relative_to(ROOT)}")
        return 1

    failures: list[str] = []

    for name in REQUIRED_BOOTSTRAPS:
        path = BOOTSTRAP_DIR / name
        if not path.exists():
            failures.append(f"ausente: {path.relative_to(ROOT)}")
            continue

        size = path.stat().st_size
        if size <= 0:
            failures.append(f"vazio: {path.relative_to(ROOT)}")
            continue

        try:
            member_count, first_member = validate_tar(path)
        except (tarfile.TarError, RuntimeError) as exc:
            failures.append(f"inválido: {path.relative_to(ROOT)} ({exc})")
            continue

        print(
            f"  - OK {path.relative_to(ROOT)} size={size} entries={member_count} "
            f"sha256_prefix={sha256_prefix(path)} first_entry={first_member}"
        )

    if is_termux_enabled():
        # Origem esperada do loader.apk:
        # - app/build.gradle task syncShellLoaderBootstrap (linhas ~161-167),
        #   que copia o APK de :shell-loader para app/build/generated/bootstrapAssets/bootstrap/loader.apk.
        # - fallback versionado em app/src/main/assets/bootstrap/loader.apk, quando presente no repositório.
        loader_candidates = [
            BOOTSTRAP_DIR / LOADER_APK_NAME,
            GENERATED_BOOTSTRAP_DIR / LOADER_APK_NAME,
        ]
        loader_path = next((candidate for candidate in loader_candidates if candidate.exists()), None)
        if loader_path is None:
            require_loader, reason = should_require_generated_loader(strict_generated_assets)
            message = (
                "ausente (Termux habilitado): esperado em "
                f"{(BOOTSTRAP_DIR / LOADER_APK_NAME).relative_to(ROOT)} "
                f"ou {(GENERATED_BOOTSTRAP_DIR / LOADER_APK_NAME).relative_to(ROOT)}; "
                "a cópia para gerados ocorre na task app:syncShellLoaderBootstrap"
            )
            if require_loader:
                failures.append(f"{message} (falha fatal: {reason})")
            else:
                print(f"  - AVISO {message} (não fatal: {reason})")
        else:
            loader_size = loader_path.stat().st_size
            if loader_size <= 0:
                failures.append(f"vazio (Termux habilitado): {loader_path.relative_to(ROOT)}")
            else:
                print(f"  - OK {loader_path.relative_to(ROOT)} size={loader_size} (Termux habilitado)")

    if failures:
        print("\n[verify_bootstrap_assets] FALHAS:")
        for failure in failures:
            print(f"  - {failure}")
        return 1

    print("\n[verify_bootstrap_assets] OK: bootstraps essenciais estão versionados e íntegros.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
