# Auditoria Completa de Todos os Arquivos

Escopo: todos os arquivos versionados retornados por `git ls-files`.

- Arquivos auditados: **1653**
- Diretórios auditados (derivados dos paths versionados): **285**
- Arquivos de texto: **1517**
- Arquivos binários: **136**
- Total de linhas de texto inspecionadas: **223129**
- Inventário detalhado por arquivo: `reports/full_repo_audit.tsv`

## Top 20 extensões
- `.java`: 414
- `.xml`: 369
- `.md`: 269
- `.c`: 99
- `.png`: 93
- `.s`: 60
- `.sh`: 56
- `.h`: 47
- `.txt`: 41
- `.json`: 27
- `.kt`: 27
- `.py`: 23
- `.yml`: 22
- `<noext>`: 16
- `.rs`: 11
- `.gradle`: 10
- `.jpg`: 8
- `.tar`: 8
- `.so`: 5
- `.iso`: 4

## Top 20 diretórios raiz
- `app`: 828
- `docs`: 102
- `tools`: 100
- `bug`: 97
- `engine`: 97
- `_incoming`: 67
- `terminal-emulator`: 41
- `resources`: 39
- `addthis`: 37
- `<root>`: 35
- `.github`: 28
- `web`: 26
- `reports`: 22
- `Incluir`: 21
- `demo_cli`: 20
- `shell-loader`: 16
- `terminal-view`: 13
- `archive`: 11
- `fastlane`: 10
- `.ci`: 8

## Inconsistências detectadas (contagem de ocorrências)
- `trailing-whitespace`: 3099
- `CRLF`: 183
- `missing-final-newline`: 126

## Método
1. Enumeração determinística com `git ls-files`.
2. Classificação text/binary por UTF-8 + presença de byte nulo.
3. Regras de coerência: CRLF, trailing whitespace, newline final e links Markdown quebrados.
4. Registro linha a linha no TSV para rastreabilidade completa.
