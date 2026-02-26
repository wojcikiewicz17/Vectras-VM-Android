# BITΩ Postdoc Pack (skeleton)
Este diretório é o “pacote acadêmico” do BITΩ, mantendo a linguagem formal e executável.

## Convenção oficial de nomes (fonte de verdade)
A convenção oficial segue **exatamente** os nomes internos publicados no
`BITOMEGA_OVERLAY__V1.zip`.

Arquivos centrais do overlay ZIP:

- `01_FORMALISM_BITOMEGA.md`
- `02_TRANSITION_GRAPH.md`
- `03_IMPLEMENTATION_MAP.md`
- `04_EXPERIMENTS.md`
- `05_RESULTS_TABLES.md`

Documentos complementares mantidos neste diretório:

- `00_THESIS_OVERVIEW.md`
- `06_LIMITATIONS_NEXT.md`

Decisão adotada: **Estratégia A** (atualizar a checagem para os nomes atuais do ZIP).
Não existe etapa de renomeação pós-extração para aliases legados.

Aliases legados **não utilizados**:

- `01_FOUNDATIONS.md`
- `02_METHODS.md`
- `03_RESULTS.md`
- `04_IMPL_DETAILS.md`
- `05_VALIDATION.md`

## Compatibilidade com aliases antigos
Aliases históricos podem aparecer em rascunhos internos, mas **não** fazem parte da
validação automática atual.

| Alias antigo | Nome canônico (ZIP) |
| --- | --- |
| `01_FOUNDATIONS.md` | `01_FORMALISM_BITOMEGA.md` |
| `02_METHODS.md` | `02_TRANSITION_GRAPH.md` |
| `03_RESULTS.md` | `03_IMPLEMENTATION_MAP.md` |
| `04_IMPL_DETAILS.md` | `04_EXPERIMENTS.md` |
| `05_VALIDATION.md` | `05_RESULTS_TABLES.md` |

Se um fluxo externo exigir os aliases, a conversão deve acontecer fora deste diretório.

## Validação local
Executar:

```bash
docs/bitomega_postdoc/validate_pack.sh
```

O script valida o conjunto completo do pacote (`00` até `06`) usando a mesma
convenção de nomes do ZIP e emite apenas **aviso** se encontrar aliases legados
no diretório.
O script valida:

1. presença dos arquivos oficiais do ZIP;
2. presença dos arquivos complementares locais;
3. ausência dos aliases legados.

## Ordem sugerida
1. 00_THESIS_OVERVIEW.md
2. 01_FORMALISM_BITOMEGA.md
3. 02_TRANSITION_GRAPH.md
4. 03_IMPLEMENTATION_MAP.md
5. 04_EXPERIMENTS.md
6. 05_RESULTS_TABLES.md
7. 06_LIMITATIONS_NEXT.md
