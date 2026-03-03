# Auditoria Completa de Todos os Arquivos

Escopo: todos os arquivos versionados retornados por `git ls-files`.

- Arquivos auditados: **1293**
- Diretórios auditados (derivados dos paths versionados): **250**
- Arquivos de texto: **1182**
- Arquivos binários: **111**
- Total de linhas de texto inspecionadas: **182353**
- Inventário detalhado por arquivo: `reports/full_repo_audit.tsv`

## Top 20 extensões
- `.java`: 380
- `.xml`: 359
- `.md`: 186
- `.png`: 73
- `.c`: 54
- `.txt`: 31
- `.h`: 27
- `.sh`: 25
- `.kt`: 24
- `.json`: 18
- `<noext>`: 17
- `.yml`: 13
- `.rs`: 11
- `.tar`: 8
- `.gradle`: 7
- `.s`: 7
- `.jpg`: 6
- `.so`: 5
- `.iso`: 4
- `.py`: 4

## Top 20 diretórios raiz
- `app`: 771
- `bug`: 91
- `docs`: 73
- `engine`: 66
- `terminal-emulator`: 40
- `resources`: 39
- `tools`: 34
- `<root>`: 28
- `web`: 26
- `shell-loader`: 21
- `.github`: 15
- `demo_cli`: 15
- `terminal-view`: 13
- `reports`: 12
- `archive`: 11
- `fastlane`: 10
- `3dfx`: 6
- `bench`: 6
- `_incoming`: 4
- `gradle`: 4

## Inconsistências detectadas (contagem de ocorrências)
- `trailing-whitespace`: 2707
- `CRLF`: 190
- `missing-final-newline`: 135

## Método
1. Enumeração determinística com `git ls-files`.
2. Classificação text/binary por UTF-8 + presença de byte nulo.
3. Regras de coerência: CRLF, trailing whitespace, newline final e links Markdown quebrados.
4. Registro linha a linha no TSV para rastreabilidade completa.
