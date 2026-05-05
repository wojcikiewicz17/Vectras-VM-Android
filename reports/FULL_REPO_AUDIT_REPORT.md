# Auditoria Completa de Todos os Arquivos

Escopo: todos os arquivos versionados retornados por `git ls-files`.

- Arquivos auditados: **1857**
- Diretórios auditados (derivados dos paths versionados): **300**
- Arquivos de texto: **1687**
- Arquivos binários: **170**
- Total de linhas de texto inspecionadas: **276244**
- Inventário detalhado por arquivo: `reports/full_repo_audit.tsv`

## Top 20 extensões
- `.java`: 420
- `.xml`: 371
- `.md`: 309
- `.c`: 126
- `.png`: 98
- `.s`: 78
- `.sh`: 78
- `.h`: 62
- `.txt`: 43
- `.py`: 38
- `.json`: 32
- `.yml`: 28
- `.zip`: 27
- `.kt`: 27
- `<noext>`: 19
- `.rs`: 11
- `.gradle`: 10
- `.jpg`: 8
- `.tar`: 8
- `.docx`: 7

## Top 20 diretórios raiz
- `app`: 840
- `tools`: 116
- `docs`: 115
- `_incoming`: 100
- `engine`: 99
- `bug`: 97
- `Incluir`: 76
- `<root>`: 49
- `terminal-emulator`: 41
- `resources`: 39
- `addthis`: 37
- `.github`: 35
- `Rafaelia`: 33
- `web`: 26
- `reports`: 25
- `demo_cli`: 20
- `shell-loader`: 16
- `archive`: 13
- `terminal-view`: 13
- `android`: 12

## Inconsistências detectadas (contagem de ocorrências)
- `trailing-whitespace`: 3476
- `CRLF`: 183
- `missing-final-newline`: 141
- `broken-md-links`: 19

## Método
1. Enumeração determinística com `git ls-files`.
2. Classificação text/binary por UTF-8 + presença de byte nulo.
3. Regras de coerência: CRLF, trailing whitespace, newline final e links Markdown quebrados.
4. Registro linha a linha no TSV para rastreabilidade completa.
