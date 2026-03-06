# docs/assets — Convenções e Governança de Diagramas

Este diretório centraliza a governança dos ativos visuais da documentação técnica.

## 1) Política canônica (obrigatória)

1. **Sem binário local obrigatório**: PNG/JPG/SVG não são requisito local e não devem ser comitados como dependência obrigatória.
2. **ASCII versionado obrigatório**: cada conceito visual precisa de uma versão canônica em `docs/assets/ascii/*.ascii.md`.
3. **Proveniência obrigatória**: toda imagem referenciada deve ter origem explícita (URL estável, issue, commit ou "Provided via chat prompt") e hash SHA-256 no manifesto.
4. **Fonte única de verdade**: `docs/assets/MANIFEST.md`.

## 2) Nomes canônicos

- `asset_id`: `kebab-case`, sem espaços e sem sufixos ambíguos.
- `file_name`: `docs/assets/ascii/<asset-id>.ascii.md` para novos ativos.
- IDs canônicos mais recentes adicionados:
  - `rafaelia-vazio-verbo-axis`
  - `rafaelia-toroid-recursive-container`

## 3) Registro de ativos ASCII versionados

| asset_id | file_name | provenance/origem | SHA-256 |
|---|---|---|---|
| `rafaelia-fractal-architecture` | `ascii/rafaelia-fractal-architecture.ascii.md` | `https://github.com/user-attachments/assets/3c9e94f2-fc94-4782-80e8-b884bc4c6d3e` | `ab92e471db6d86f8bc56ab88cd801ade4cf7552e05fbe481fb92002c70e41214` |
| `rafaelia-system-pipeline` | `ascii/rafaelia-system-pipeline.ascii.md` | `https://github.com/user-attachments/assets/aafce52f-c83b-480b-b1d4-d579256c2363` | `cb54a1744516acc982dbc01d2a8d45dbc642342cf888b6ecdf58c7895305cb7b` |
| `rafaelia-mathematical` | `ascii/rafaelia-mathematical.ascii.md` | `https://github.com/user-attachments/assets/d69640be-83f6-413b-b3b1-2ebbfc1e7cd4` | `06626ffa0a5f4ca9127f7c3c77d65c5dcd131f3a7eb2152006668180cb12e515` |
| `vectra-mystical-ui-concept` | `ascii/vectra-mystical-ui-concept.ascii.md` | `https://github.com/user-attachments/assets/aa58501e-263c-47fe-91b6-406b373cb3f6` | `f7bd76f8251aa7b1554b1dc3ae5c1b345c42cac02766b472496060d150a494ce` |
| `ziprafa-integrity-architecture` | `ascii/ziprafa-integrity-architecture.ascii.md` | `https://github.com/user-attachments/assets/97010343-3677-4766-b070-9fece88ba754` | `cf80bf70c0e5ebaedb7897be817a1ced7c5abe8b666378765193251b59b28e4c` |
| `additional-image-01` | `ascii/additional-image-01.ascii.md` | `https://github.com/user-attachments/assets/2123ec50-7240-490a-8975-bd675ca1fa92` | `d2775ebaae74ec6b24070dde7139513479724db1dab1e5209a4f0699782ce6c4` |
| `rafaelia-toroid-recursive-container` | `ascii/rafaelia-toroid-recursive-container.ascii.md` | `Provided via chat prompt (canonicalized in-repo; concept renamed to canonical ID)` | `9104bec83cd74b77b942ade0b177f1488260e504a8013b4ba24dffee62194a7f` |
| `rafaelia-vazio-verbo-axis` | `ascii/rafaelia-vazio-verbo-axis.ascii.md` | `Provided via chat prompt (canonicalized in-repo; concept renamed to canonical ID)` | `08c9fb6b85c824e936fa1eeef2c23fa30bb9a6046abae890bbbb5b1b3300e3f1` |

## 4) Checklist para novos assets

- Definir `asset_id` canônico.
- Criar `docs/assets/ascii/<asset-id>.ascii.md`.
- Calcular SHA-256 do arquivo ASCII.
- Registrar no `docs/assets/MANIFEST.md`.
- Atualizar `docs/IMAGES_INDEX.md` com camada de sistema + caminhos de código.
