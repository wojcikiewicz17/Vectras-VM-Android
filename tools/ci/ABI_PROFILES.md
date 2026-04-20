# ABI profile contract (single source of truth)

Fonte de verdade versionada para perfis ABI de CI: `tools/ci/abi_profiles_contract.json`.

## Perfis suportados (7 + genérico)

- `official_arm64` → `APP_ABI_POLICY=arm64-only`
- `official_arm32_arm64` → `APP_ABI_POLICY=arm32-arm64`
- `internal_arm64` → `APP_ABI_POLICY=arm64-only` (com validação interna)
- `internal_arm32_arm64` → `APP_ABI_POLICY=arm32-arm64`
- `internal_4abi` → `APP_ABI_POLICY=internal-4abi`
- `internal_5abi` → `APP_ABI_POLICY=internal-5abi`
- `internal_riscv64` → `APP_ABI_POLICY=internal-5abi`
- `generic` → fallback adaptativo para perfis não oficiais mapeados

## Normalização de nomes

Aliases aceitos no resolvedor (`tools/ci/resolve_abi_profile.py`):

- `internal-4abi` => `internal_4abi`
- `internal-5abi` => `internal_5abi`
- `official-arm64` => `official_arm64`

A saída final sempre usa `APP_ABI_POLICY` no formato esperado pelo Gradle:
`arm64-only`, `arm32-arm64`, `internal-4abi`, `internal-5abi`.
