<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Auditoria de Cobertura Documental (2026-04-07)

## Objetivo
Executar uma revisão metódica da documentação Markdown distribuída no repositório, priorizando coerência entre:
- código-fonte vigente;
- READMEs por diretório;
- mapas estruturais (`FILES_MAP.md`) quando existentes.

## Escopo e método
- Escopo de varredura: arquivos rastreados no Git com extensão Markdown case-insensitive (`*.md`, `*.MD`, `*.Md`, `*.mD`).
- Total de arquivos Markdown encontrados na data da auditoria: **237**.
- Método:
  1. Inventário de arquivos `.md`.
  2. Agregação por diretório de primeiro nível.
  3. Verificação de presença de `README.md` e `FILES_MAP.md` por domínio.
  4. Registro de pendências estruturais para redução de drift documental.

## Cobertura por domínio (nível 1)
| Domínio | Qtde `.md` | `README.md` | `FILES_MAP.md` | Observação |
|---|---:|---|---|---|
| `(root)` | 16 | n/a | n/a | Índices e guias globais |
| `docs/` | 87 | sim | sim | Maior concentração documental |
| `bug/` | 53 | sim | sim | Histórico/triagem extensa |
| `reports/` | 18 | sim | sim | Relatórios operacionais |
| `archive/` | 11 | sim | sim | Conteúdo histórico |
| `tools/` | 7 | sim | sim | Guias de utilitários |
| `addthis/` | 6 | sim | não | Diretório sem `FILES_MAP.md` |
| `engine/` | 4 | sim | sim | Núcleo nativo canônico |
| `.github/` | 3 | não | não | Templates de issue/documentação auxiliar |
| `app/` | 3 | sim | sim | Domínio Android principal |
| `runtime/` | 3 | sim | sim | Execução/supervisão |
| `.ci/` | 2 | não | não | Material auxiliar de automação |
| `web/` | 2 | sim | sim | Artefatos de interface web |
| `terminal-emulator/` | 2 | sim | sim | Emulador de terminal |
| `terminal-view/` | 2 | sim | sim | Camada de visualização |
| `shell-loader/` | 2 | sim | sim | Loader e assinatura |
| `fastlane/` | 2 | sim | sim | Metadados de distribuição |
| `gradle/` | 2 | sim | sim | Camada de build |
| `3dfx/` | 2 | sim | sim | Módulo legado/especializado |
| `resources/` | 2 | sim | sim | Ativos e referências |
| `_incoming/` | 2 | sim | não | Área transitória |
| `evidence/` | 1 | sim | não | Evidências pontuais |
| `__DELTA__/` | 1 | não | não | Snippets/pacote de delta |

## Pendências estruturais mapeadas
Diretórios com `.md` e sem `README.md` local:
- `.ci/`
- `.github/ISSUE_TEMPLATE/`
- `__DELTA__/`
- `archive/experimental/rafael_melo_reis_bundle/teoremas/modulos/camadas/gui/ui/ux/`
- `archive/experimental/seguranda/`
- `archive/root-history/`
- `docs/assets/ascii/`
- `docs/ci/`
- `docs/navigation/`

## Interpretação operacional
- A base documental principal está consolidada e navegável em `README.md` + `DOC_INDEX.md` + `docs/README.md`.
- O ponto crítico não é ausência total de docs, mas **heterogeneidade de profundidade** entre subpastas especializadas.
- Para manter coerência contínua, mudanças de código devem atualizar documentação canônica do domínio no mesmo ciclo (especialmente em `engine/`, `app/`, `tools/`, `runtime/` e `docs/`).
- O inventário exaustivo arquivo-a-arquivo está em `docs/active/ALL_MARKDOWN_FILES_2026-04-07.md`.

## Próximos passos recomendados
1. Tratar pendências de README local apenas onde houver consumo recorrente por manutenção.
2. Evitar duplicação: usar documentos de domínio como fonte de verdade e índices como roteadores.
3. Repetir esta auditoria sempre que houver refatoração estrutural de diretórios.

## Comandos de referência usados na auditoria
```bash
python3 - <<'PY'
import os
import subprocess
from collections import defaultdict
files=subprocess.check_output(['git','ls-files'], text=True).splitlines()
md=[p for p in files if p.lower().endswith('.md')]
print('TOTAL_MD_TRACKED',len(md))
by=defaultdict(int)
for p in md:
    top=p.split('/',1)[0] if '/' in p else '(root)'
    by[top]+=1
for top in sorted(by):
    if top == '(root)':
        print(top, by[top], 'n/a', 'n/a')
        continue
    print(top, by[top], os.path.exists(f'{top}/README.md'), os.path.exists(f'{top}/FILES_MAP.md'))
PY
```
