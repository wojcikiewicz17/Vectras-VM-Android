# Vectras VM – Documentation Standards

> **Objetivo / Purpose**: Padronizar a documentação técnica, acadêmica e legal, garantindo clareza, navegabilidade e conformidade autoral.

## 1) Estrutura Obrigatória / Required Structure

Cada documento técnico deve conter, quando aplicável:

- **Título**
- **Resumo** (2–4 frases)
- **Escopo** (o que cobre e o que não cobre)
- **Público-alvo**
- **Conteúdo principal** (com seções e subtítulos claros)
- **Referências** (links internos/externos relevantes)
- **Metadados** (versão, data de atualização, responsáveis)

## 2) Normas de Escrita / Writing Standards

- **Linguagem clara** e consistente (evitar ambiguidade).
- Usar **termos técnicos padronizados** conforme [docs/GLOSSARY.md](GLOSSARY.md).
- Preferir **voz ativa** e frases objetivas.
- Evitar jargão sem explicação.

## 3) Navegação e Acessibilidade

- Incluir **sumário** quando o documento for longo.
- Garantir links funcionais e caminhos relativos corretos.
- Usar **títulos hierárquicos** (H1 → H2 → H3) sem saltos.
- Imagens devem ter **texto alternativo** quando possível.

## 4) Referências e Citações

- Qualquer referência acadêmica deve estar registrada em [docs/BIBLIOGRAPHY.md](BIBLIOGRAPHY.md).
- Informações derivadas de terceiros devem ser citadas com **URL e data** quando apropriado.
- Evite copiar conteúdo protegido sem autorização.

## 5) Licenças e Direitos Autorais

- Preservar avisos de copyright e licenças.
- Incluir a licença aplicável ao documento (geralmente GPL-2.0).
- Seguir as diretrizes de [docs/LEGAL_AND_LICENSES.md](LEGAL_AND_LICENSES.md).

## 6) Versionamento e Controle de Mudanças

- Atualizar a seção **Metadados** ao modificar o documento.
- Manter um **Change Log** se o documento for crítico (ex.: arquitetura, compliance).
- Registrar saneamentos de links internos em `CHANGELOG.md` e, quando aplicável, no documento de governança correspondente.
- Alinhar versões com a versão do projeto quando possível.
- Toda mudança em caminhos críticos (app, engine, tools, web, runtime e docs de governança) deve atualizar os metadados e os links de rastreabilidade do documento correspondente.

### 6.1) Regra operacional para metadados de rastreabilidade

- Atualização **obrigatória em toda PR que altere qualquer arquivo em `docs/`**.
- `Commit de referência`:
  - usar o **commit atual de `HEAD`** quando a revisão documental for publicada sem tag de release.
  - usar a **tag de release** (e o commit apontado por ela) quando a documentação fizer parte de corte formal de versão.
- `Última atualização`: usar data ISO (`YYYY-MM-DD`) da revisão efetiva da PR.
- `Versão do documento`: incrementar em `+0.1` para ajustes editoriais/estruturais e em `+1.0` para reestruturação completa de escopo.
- Em documentos relacionados (ex.: `docs/README.md`, `docs/navigation/INDEX.md` e guias de navegação vinculados), atualizar metadados em bloco na mesma PR para manter coerência de rastreabilidade.

## 7) Padrões para Arquivos de Navegação

- Atualizar [docs/README.md](README.md) e [docs/navigation/INDEX.md](navigation/INDEX.md) ao criar ou mover documentos.
- Evitar duplicidade de índices; centralizar a navegação no `docs/README.md`.

---

## 8) Metadados Mínimos para Novos Diagramas ASCII

Todo novo diagrama ASCII adicionado em `docs/assets/` deve ter metadados mínimos no manifesto único (`docs/assets/MANIFEST.md`) antes de ser referenciado em índices ou documentação:

- `file_name` (nome exato do arquivo versionado)
- `source_url` (URL de origem rastreável)
- `capture_date` (data de captura/importação no formato `YYYY-MM-DD`)
- `sha256` (checksum SHA-256 do arquivo)

Regras adicionais:
- Entradas marcadas como “Provided via chat prompt” **não** são consideradas concluídas sem vínculo rastreável (issue, PR ou artefato versionado com link estável).
- `docs/IMAGES_INDEX.md` deve referenciar apenas itens com metadados completos no manifesto.

---

## Template Rápido / Quick Template

```markdown
# Título

## Resumo

## Escopo

## Público-alvo

## Conteúdo

## Referências

## Metadados
- Versão:
- Última atualização:
- Responsável(is):
- Licença:
```

---

**Última atualização / Last updated**: 2026-03-06

© 2024-2026 Vectras VM Development Team — Licensed under GPL-2.0.
