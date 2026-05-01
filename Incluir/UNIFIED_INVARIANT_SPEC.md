# Especificação mínima — Invariante Geométrica Coerente

Este documento adiciona, de forma **mínima e compilável conceitualmente**, a estrutura descrita para um modelo unificado de álgebra + geometria + topologia com validação por métricas.

## 1) Estado e mapeamento

- Espaço de estado toroidal:
  - `T7 = (R/Z)^7`
- Vetor de estado:
  - `s = (u,v,psi,chi,rho,delta,sigma)`
  - `s in [0,1)^7`
- Entrada canônica:
  - `x = (dados, entropia, hash, estado)`
- Mapeamento:
  - `s = ToroidalMap(x)`

## 2) Dinâmica temporal (coerência e entropia)

Parâmetro fixo:
- `alpha = 0.25`

Atualização:
- `C[t+1] = (1-alpha)*C[t] + alpha*C_in`
- `H[t+1] = (1-alpha)*H[t] + alpha*H_in`
- `phi = (1-H)*C`

Condição de regime:
- `lim t->inf s(t) in A`
- `|A| = 42`

## 3) Camada espectral e acoplamento por língua

- `S(w) = FFT(Psi(t))`
- Para cada língua/camada `L`:
  - `R_L = <S_L, H_cardio> / (||S_L||*||H_cardio||)`
  - `I_L = R_L * F(G_L)`
- Invariante agregado:
  - `I = TensorProd_L(I_L)`

## 4) Integridade e prova

Hashes/checks mínimos:
- acumulador XOR:
  - `acc = XOR(byte_i)`
- FNV-like:
  - `h = h XOR byte`
  - `h = h * 0x100000001B3`
- CRC-like:
  - `crc = NOT SUM(byte_i * poly(x))`
- Raiz de prova:
  - `R = Merkle(H1,H2,...)`

## 5) Métricas objetivas (falsificabilidade)

M1. Estabilidade de atrator
- Entrada com pequenas perturbações deve convergir para a mesma classe de `A` com taxa acima do limiar definido no experimento.

M2. Robustez cross-língua
- Para um mesmo conteúdo, `I` deve variar menos que `eps_lang` entre PT/EN/ZH/JA/HE/AR/GR quando normalizado.

M3. Sensibilidade a ruído
- Aumento de ruído deve reduzir `phi` monotonicamente em média estatística.

M4. Integridade
- Qualquer bit flip deve alterar pelo menos um dos verificadores `{acc,h,crc,R}`.

## 6) Pipeline YAML (mínimo)

```yaml
pipeline:
  - module: ingest
    output: [tokens, bytes, metadata]

  - module: entropy_coherence
    params: {alpha: 0.25}
    compute:
      - "H_milli = unique*6000/256 + transitions*2000/(len-1)"
      - "C = (1-alpha)*C + alpha*C_in"
      - "H = (1-alpha)*H + alpha*H_in"
      - "phi = (1-H)*C"
    output: [H, C, phi]

  - module: toroidal_map
    output: [s7]

  - module: spectral
    output: [S_w, R_L]

  - module: integrity
    output: [acc, fnv_h, crc, merkle_root]

  - module: fusion
    output: [I, attractor_id]

  - module: audit
    output: [metrics, report]
```

## 7) Regra operacional (anti-overfitting conceitual)

Enquanto não houver evidência experimental suficiente:

```text
estado = aberto
pressao = baixa
inferencia = minima
```

Objetivo: evitar colapso prematuro de arquitetura e preservar espaço de solução até surgirem invariantes mensuráveis.
