# Auditoria Forense de Arquivos Não-Markdown

Este relatório cobre todos os arquivos do repositório, exceto `.md`, com inventário determinístico e hash SHA-256.

- Total de arquivos não-MD auditados: **1518**
- Inventário detalhado: `reports/non_md_inventory.tsv`

## Distribuição por tipo lógico
- source: 1146
- other: 143
- image: 108
- config-data: 82
- artifact-binary: 20
- no-extension: 19

## Distribuição text/binary
- text: 1328
- binary: 190

## Top extensões
- `.java`: 420
- `.xml`: 371
- `.c`: 126
- `.png`: 98
- `.s`: 78
- `.sh`: 78
- `.h`: 62
- `.txt`: 43
- `.py`: 38
- `.json`: 32
- `.zip`: 27
- `.kt`: 27
- `<noext>`: 19
- `.rs`: 11
- `.gradle`: 10
- `.jpg`: 8
- `.tar`: 8
- `.docx`: 7
- `.mk`: 6
- `.so`: 5

## Top diretórios (arquivos não-MD)
- `app`: 837
- `tools`: 105
- `_incoming`: 97
- `engine`: 95
- `Incluir`: 56
- `bug`: 44
- `terminal-emulator`: 39
- `resources`: 37
- `Rafaelia`: 32
- `addthis`: 31
- `web`: 24
- `<root>`: 22
- `demo_cli`: 18
- `shell-loader`: 14
- `android`: 11
- `terminal-view`: 11
- `fastlane`: 8
- `.ci`: 6
- `bench`: 6
- `3dfx`: 4

## Arquivos >= 5 MiB
- `app/src/main/assets/roms/QEMU_EFI.img` — 67108864 bytes
- `app/src/main/assets/roms/QEMU_VARS.img` — 67108864 bytes

## Candidatos sensíveis para governança
- `tools/termux-arm64-orchestrator/resolve-release-keystore.sh`

## Achados e recomendações
1. Validar política de versionamento para arquivos de chave/assinatura detectados.
2. Revisar periodicamente arquivos binários grandes em assets para controlar footprint do repositório.
3. Executar este auditor em CI para trilha contínua de integridade de inventário.
