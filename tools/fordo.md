# FORDO — RAFAELIA / State Geometry Lab / Safe Fórmulas

## 1. Visão geral

Este repositório contém uma camada de experimentação computacional que conecta conceitos de sementes RAFAELIA, fórmulas em ponto fixo, geometria de estados, dinâmica de atratores e execução low-level. No estado atual do checkout, a parte executável observável está concentrada em `engine/rmr/` (Q16.16, atratores, fórmulas, kernels C/ASM), e não em um diretório `tools/state_geometry_lab/`.

Conexões-alvo desta camada:

- sementes RAFAELIA (como ideia/contrato de modelagem);
- fórmulas e constantes (Q16.16);
- geometria de estados;
- sistemas dinâmicos;
- projeções toroidais;
- atratores;
- assinatura/fingerprint de estado;
- execução multi-runtime (C/ASM + ponte JNI);
- validação futura em Termux/ARM/Vectras.

Definições de escopo neste documento:

- `RAFAELIA_SEMENTES.txt`: **não encontrado neste checkout** (caminho solicitado: `rmr/RAFAELIA_SEMENTES.txt`).
- State Geometry Lab (`tools/state_geometry_lab`): **não encontrado neste checkout**.
- Safe Fórmulas: tratado aqui como **metodologia operacional de validação** para evitar conclusões sem medição, teste e reprodutibilidade.

## 2. O que foi criado

| Área | Arquivo/Diretório | Função | Status |
|---|---|---|---|
| Sementes RAFAELIA | `rmr/RAFAELIA_SEMENTES.txt` | Base de sementes/hipóteses | **não encontrado (gap)** |
| Sementes RRR | `rmr/rrr/Rafael_Rafael_semente.txt` | Complemento de sementes | **não encontrado (gap)** |
| State Geometry Lab (raiz) | `tools/state_geometry_lab/` | Bancada dedicada de métodos | **não encontrado (gap)** |
| State Geometry Lab README | `tools/state_geometry_lab/README.md` | Documentação de entrada | **não encontrado (gap)** |
| Methods | `tools/state_geometry_lab/docs/METHODS.md` | Catálogo de métodos | **não encontrado (gap)** |
| Python Lab | `tools/state_geometry_lab/py/state_geometry_lab.py` | Implementação de referência | **não encontrado (gap)** |
| C Lab | `tools/state_geometry_lab/c/state_geometry_lab.c` | Kernel C da bancada | **não encontrado (gap)** |
| ASM Lab | `tools/state_geometry_lab/asm/mod_cycle_aarch64.S` | Kernel ASM inicial | **não encontrado (gap)** |
| Modules Lab | `tools/state_geometry_lab/modules/` | Módulos auxiliares/stubs | **não encontrado (gap)** |
| Raw C Lab | `tools/state_geometry_lab/raw_c/` | Material bruto/expansão C | **não encontrado (gap)** |
| Núcleo RAFAELIA real | `engine/rmr/src/rafaelia_formulas_core.c` + `engine/rmr/include/rafaelia_formulas_core.h` | Fórmulas Q16.16 e operações determinísticas | executável / precisa validação externa |
| Atratores | `engine/rmr/src/rmr_attractor.c` | Classe/retenção de atratores no core | executável / precisa validação de modelo |
| Toro em Q16 | `engine/rmr/src/rmr_torus_flow.c` | Dinâmica determinística em Q16.16 | executável / protótipo |
| ASM interop ARM64 | `engine/rmr/interop/rmr_casm_arm64.S`, `engine/rmr/interop/rmr_stability_arm64.S` | Rotinas low-level ARM64 | protótipo |

## 3. Vetor estrutural do projeto

V(RAFAELIA/SGL) =

```text
[
  SEED,
  FORMULA,
  DOMAIN,
  STATE,
  PROJECTION,
  SIGNATURE,
  RUNTIME,
  MEASUREMENT,
  VALIDATION
]
```

- **SEED**: dado inicial determinístico.
- **FORMULA**: relação matemática ou hipótese operacional.
- **DOMAIN**: domínio de validade (intervalo, escala, ABI, precisão, corpus).
- **STATE**: representação interna do sistema (ex.: vetores em Q16.16).
- **PROJECTION**: transformação para matriz, toro, campo, base ou atrator.
- **SIGNATURE**: fingerprint/hash comparável entre execuções.
- **RUNTIME**: Python/C/ASM/Rust/Shell/Termux/ARM.
- **MEASUREMENT**: benchmark, tempo, memória, jitter, energia.
- **VALIDATION**: aceitação/refutação por teste, erro e repetição.

## 4. Safe Fórmulas

**Safe Fórmulas** é uma metodologia para impedir que fórmulas virem apenas afirmações bonitas. Uma fórmula só passa de hipótese para fórmula segura quando atravessa critérios mínimos de validação.

Checklist mínimo:

- [ ] Domínio declarado
- [ ] Escala/unidade declarada
- [ ] Seed definida
- [ ] Entrada e saída especificadas
- [ ] Invariante conhecido
- [ ] Implementação executável
- [ ] Assinatura/hash gerado
- [ ] Resultado reprodutível
- [ ] Teste automatizado
- [ ] Benchmark ou medição
- [ ] Status explícito: hipótese, protótipo, validado, refutado

| Nível | Nome | Critério |
|---|---|---|
| 0 | Fórmula solta | Apenas expressão |
| 1 | Fórmula documentada | Tem descrição |
| 2 | Fórmula executável | Roda em código |
| 3 | Fórmula assinada | Gera hash/assinatura |
| 4 | Fórmula reprodutível | Mesmo input gera mesmo output |
| 5 | Fórmula validada | Tem teste, medição e limite de erro |

## 5. Blocos principais do RAFAELIA_SEMENTES.txt

Leitura solicitada de `rmr/RAFAELIA_SEMENTES.txt`: **arquivo não encontrado neste checkout**.

Impacto:

- não foi possível mapear IDs numerados diretamente de 1..100 a partir desse arquivo;
- abaixo, o mapeamento usa apenas evidências rastreáveis em arquivos presentes (`engine/rmr/*` e `docs/RAFAELIA_T7_MULTILINGUAL_MAPPING.md`).

| ID | Tema | Fórmula/ideia central | Status | Próximo teste |
|---|---|---|---|---|
| §62 (referência funcional) | operador √3/2 | `Spiral(n)=(√3/2)^n` em Q16.16 (`RAF_SPIRAL_Q16`) | executável no core / precisa validação formal | testar estabilidade e erro acumulado por N iterações |
| §72 (hipótese de pesquisa) | incompressibilidade semântica/dimensão crítica | hipótese de `n_crítico(L)` com escala Q16 e limite informacional | hipótese / precisa validação | definir corpus, métrica de compressão e erro semântico |
| §100 (hipótese testável) | atratores, dimensão efetiva, D_H/D_2 | atrator finito (42), espaço T^n/T^7 como hipótese operacional | protótipo/hypótese | implementar pipeline Grassberger-Procaccia e regressão log-log |

### §62 — operador √3/2

- √3/2 é constante geométrica conhecida (altura relativa do triângulo equilátero).
- No repositório atual, aparece como operador de contração/estabilidade em Q16.16 (`RAF_SPIRAL_Q16`).
- Há ligação com recorrência operacional no núcleo RAFAELIA (`raf_f_rafael_step_q16`).
- Referências textuais sugerem ponto fixo próximo de `F* ≈ 23.158` em materiais narrativos; **não tratado aqui como prova**.
- Status: **hipótese + cálculo interno executável**, ainda sem validação externa formal.

### §72 — incompressibilidade semântica / dimensão crítica

Separação por camada:

- **Matemática**: hipótese de dimensão mínima de representação (`n_crítico(L)`) para manter informação.
- **Linguística**: relação entre linguagem/corpus e perda de significado.
- **Computacional**: modelagem em embeddings/compressão com escala controlada (ex.: Q16).
- **Especulativa**: qualquer universalização sem benchmark/corpus é aberta.

Status: **hipótese de pesquisa**, requer experimento em corpus real com protocolo reproduzível.

### §100 — atratores / Hausdorff / Grassberger-Procaccia

- Há referência recorrente a atrator de cardinalidade 42 e dinâmica toroidal.
- Diferença crítica: dimensão do espaço de estados ≠ dimensão efetiva da órbita.
- Medidas como `D_H`/`D_2` e Kaplan-Yorke só podem ser afirmadas após medição.
- Próximo teste técnico real: Grassberger-Procaccia
  1. simular N passos;
  2. coletar vetor de estado;
  3. calcular `C(ε)`;
  4. ajustar curva log-log;
  5. estimar inclinação e incerteza.

Status: **bloco tecnicamente testável**, ainda sem implementação/medição fechada neste checkout.

## 6. State Geometry Lab

`tools/state_geometry_lab` não está presente neste checkout. Portanto, o resumo dos 14 métodos em `docs/METHODS.md` não pode ser feito sobre código real local.

Tabela de status solicitada (baseada no que foi solicitado, com estado de disponibilidade atual):

| Método | Entrada | Saída | Implementação atual | Relação com RAFAELIA | Gap |
|---|---|---|---|---|---|
| fibonacci_variant_patterns | N/A | N/A | não encontrado | alta (recorrência) | falta arquivo |
| modular_tensor | N/A | N/A | não encontrado | alta (modularidade) | falta arquivo |
| phi_pi_index_field | N/A | N/A | não encontrado | alta (φ/π) | falta arquivo |
| equilateral_height | N/A | N/A | não encontrado | alta (√3/2) | falta arquivo |
| toroidal_map | N/A | N/A | não encontrado | alta (T^n) | falta arquivo |
| attractor_field | N/A | N/A | não encontrado | alta (atratores) | falta arquivo |
| multilevel_permutations | N/A | N/A | não encontrado | média | falta arquivo |
| rgb_cmyb_interpolate | N/A | N/A | não encontrado | baixa/média | falta arquivo |
| angular_moments | N/A | N/A | não encontrado | média | falta arquivo |
| polynomial_square_borrow | N/A | N/A | não encontrado | média | falta arquivo |
| spiral_matrix_cycles | N/A | N/A | não encontrado | alta (espiral) | falta arquivo |
| inverse_antiderivative_stack | N/A | N/A | não encontrado | média | falta arquivo |
| spectral_64bit_signature | N/A | N/A | não encontrado | alta (assinatura) | falta arquivo |
| base_projection | N/A | N/A | não encontrado | alta (projeção) | falta arquivo |

Avaliação honesta no estado atual:

- Python de State Geometry Lab: **não encontrado**.
- C de State Geometry Lab: **não encontrado**.
- ASM de State Geometry Lab: **não encontrado**.
- `modules/method_001..025`: **não encontrado**.
- `raw_c`: **não encontrado**.

## 7. Código Python

Arquivo solicitado: `tools/state_geometry_lab/py/state_geometry_lab.py`.

Status no checkout: **não encontrado**.

### Python como implementação de referência

Diretriz proposta para quando o arquivo existir:

1. Python define a semântica canônica dos métodos.
2. Cada método gera saída determinística para seed fixa.
3. JSON e assinatura devem virar ouro de comparação para C/ASM.
4. Testes de snapshot devem bloquear drift semântico.

## 8. Código C

Arquivo solicitado: `tools/state_geometry_lab/c/state_geometry_lab.c`.

Status no checkout: **não encontrado**.

No núcleo existente (`engine/rmr/src/rafaelia_formulas_core.c`), há kernels mínimos úteis para:

- operações Q16.16 determinísticas;
- recorrências/combinações de fórmulas;
- base de integração NDK/JNI.

Classificação atual:

- kernel mínimo: **implementado**;
- útil para assinatura/estado comparável: **protótipo funcional**;
- base para NDK/JNI: **implementado**;
- cobertura dos 14 métodos do SGL: **não aplicável (arquivos ausentes)**.

Próximo passo recomendado:

- portar métodos canônicos (quando existirem) para C;
- comparar hash Python vs C;
- criar teste de equivalência automatizado.

## 9. ASM AArch64

Arquivo solicitado: `tools/state_geometry_lab/asm/mod_cycle_aarch64.S`.

Status no checkout: **não encontrado**.

Evidência relacionada disponível:

- `engine/rmr/interop/rmr_casm_arm64.S`
- `engine/rmr/interop/rmr_stability_arm64.S`

Classificação do estado low-level atual:

- kernel baixo nível inicial: **sim**;
- NEON dedicado para este fluxo documental: **não demonstrado aqui**;
- ARM32 equivalente para o mesmo método: **gap**;
- benchmark C vs ASM focado nesse escopo: **gap**;
- utilidade para Termux/ARM64: **alta como base**, precisa protocolo de medição.

## 10. Inovações reais

### Inovações metodológicas

| Item | Status |
|---|---|
| Safe Fórmulas (hipótese → execução → validação) | protótipo metodológico |
| seed → expansão → fronteira aberta | documentado conceitualmente |
| fórmula como objeto verificável | implementado parcialmente (Q16 + funções C) |
| separação hipótese/execução/validação | precisa validação e governança formal |

### Inovações computacionais

| Item | Status |
|---|---|
| laboratório multi-runtime | protótipo parcial (C/ASM/JNI no core RMR) |
| assinatura 64-bit como estado comparável | precisa validação explícita de contrato |
| ponte Python/C/ASM | parcial (Python do SGL não encontrado) |
| potencial Termux/ARM/Vectras | hipótese com base técnica |

### Inovações matemático-computacionais

| Item | Status |
|---|---|
| √3/2 como operador transversal de estabilidade | implementado no core Q16 / precisa prova externa |
| n_crítico(L) como dimensão semântica mínima | hipótese |
| atratores em T^7 com D_2 medível | hipótese testável |
| Poincaré-Hopf para contagem de índices (se aplicável) | hipótese/documentado em materiais externos ao checkout |
| Q16 como escala comum low-level/geometria | implementado |

## 11. O que NÃO está fechado

Não está fechado ainda:

- prova matemática formal;
- validação acadêmica externa;
- medição D_2 real;
- corpus para §72;
- benchmark industrial;
- equivalência Python/C/ASM do SGL solicitado;
- port ARM32 dedicado aos mesmos kernels SGL;
- NEON otimizado no escopo SGL;
- CI completa para trilha SGL;
- testes unitários de ponta a ponta para os 14 métodos solicitados.

## 12. Plano de validação

### Fase 1 — Índice e documentação

- localizar/publicar `rmr/RAFAELIA_SEMENTES.txt` e `rmr/rrr/Rafael_Rafael_semente.txt` no repo;
- indexar IDs reais do arquivo de sementes;
- mapear método → arquivo → runtime → status;
- separar hipótese de resultado validado.

### Fase 2 — Execução e assinatura

- executar métodos Python (quando disponíveis);
- gerar JSON determinístico;
- gerar hash/assinatura esperada;
- portar métodos críticos para C;
- comparar Python/C;
- validar ASM com suíte de equivalência.

### Fase 3 — Medição real

- benchmark em Termux;
- ARM64;
- ARM32 se possível;
- tempo;
- memória;
- jitter;
- energia (se possível);
- artifact de CI com JSON/CSV e histórico.

## 13. F DE RESOLVIDO / F DE GAP / F DE NEXT

### F DE RESOLVIDO

- Núcleo RAFAELIA em Q16.16 está presente em `engine/rmr`.
- Há implementação C executável de fórmulas e operadores fixos.
- Há interop ASM ARM64 no core.
- Há documentação parcial de mapeamento T7 e atratores em `docs/`.

### F DE GAP

- Arquivos solicitados de sementes (`rmr/...`) não encontrados.
- Diretório `tools/state_geometry_lab/` e seus subarquivos não encontrados.
- Sem índice 1..100 de sementes verificável a partir dos caminhos pedidos.
- Sem benchmark fechado e sem medição D_2 no estado atual.

### F DE NEXT

1. Criar/publicar índice de sementes a partir do arquivo oficial (`RAFAELIA_SEMENTES.txt`) dentro do repo.
2. Implementar medição do §100 (pipeline Grassberger-Procaccia com saída CSV/JSON).
3. Formalizar Safe Fórmulas + protocolo de validação de `n_crítico` (§72) com corpus, métrica e limite de erro.

## 14. Requisitos de estilo

- Documento em português do Brasil.
- Tom técnico, direto e sem marketing.
- Sem afirmar prova onde há hipótese.
- Com tabelas e estrutura navegável.
- Útil como índice técnico para continuidade por outro desenvolvedor.
