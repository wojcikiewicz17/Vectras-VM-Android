# Core Low-Level Implementation Documentation (Finalização)

## Escopo
Documentação objetiva dos arquivos codificados em `core/` para fechar rastreabilidade entre implementação e especificação operacional.

## Inventário `core/`
- `core/unified_coherence.h`
- `core/unified_coherence.c`
- `core/keygen.c`
- `core/signature.c`

---

## 1) `core/unified_coherence.h`
API mínima de execução do modelo unificado:
- `UCContext`: estado 7D + coerência + entropia + hash + crc + state.
- `UCInput`: entradas de coerência, entropia, payload e state.
- `uc_init(ctx, seed)`: inicialização determinística do contexto.
- `uc_step(ctx, in)`: passo atômico de atualização.

Objetivo operacional: manter interface de baixo overhead sem heap allocation.

## 2) `core/unified_coherence.c`
Implementação efetiva do pipeline técnico:
1. **Constantes de runtime**
   - `UC_DIM=7`, `UC_ALPHA=0.25`, `UC_ATTRACTOR_CARDINALITY=42`.
2. **Normalização toroidal**
   - `frac01`: projeção para `[0,1)`.
3. **Integridade incremental**
   - `fnv1a_step` para hash.
   - `crc32c_sw_step` para CRC32C (substituível por hardware).
4. **Mapeamento de estado**
   - `toroidal_map`: converte fluxo de bytes para vetor 7D.
5. **Acoplamento angular**
   - `spectral_link = sin(dtheta) * cos(dphi)`.
6. **Atualização temporal**
   - `C/H` por suavização exponencial com `alpha=0.25`.
   - `phi = (1 - H) * C`.
7. **Roteamento de atrator**
   - cardinalidade fixa em 42 estados.

Resumo: implementação in-place e linear no tamanho da entrada.

## 3) `core/keygen.c`
Gerador de chave com estado interno `RAF_State`:
- Campos: `s`, `phi`, `d`.
- `next_attractor`: rotação/mistura 64-bit.
- `raf_step`: atualização por entrada + transição condicional de atrator.
- `generate_key`: 64 iterações com clock e rotação.

Uso atual: utilitário standalone de geração rápida de chave.

## 4) `core/signature.c`
Assinatura/verificação por hash incremental:
- `mix`: rotina de mistura avalanche 64-bit.
- `sign_data(data,key)`: assinatura do payload.
- `verify_signature`: comparação determinística.

Uso atual: referência mínima para assinatura/verificação local.

---

## Mapeamento direto com conceitos solicitados
- `T^7` / `s in [0,1)^7`: realizado em `unified_coherence` (`UC_DIM=7`, `frac01`).
- `C_{t+1}` e `H_{t+1}` com `alpha=0.25`: `uc_step`.
- `phi = (1-H)C`: `uc_step`.
- `|A|=42`: `UC_ATTRACTOR_CARDINALITY`.
- `sin(dtheta)cos(dphi)`: `spectral_link`.
- `h = h xor byte; h *= 0x100000001B3`: `fnv1a_step`.
- `CRC`: `crc32c_sw_step`.

## Gap explícito (ainda não codificado no core)
Ainda não existe implementação nativa em `core/` para:
- Merkle tree de blocos.
- FFT/correlação por língua `R_L`.
- Pipeline multilíngue completo `I = tensor_product_L(...)`.

## Próximo passo recomendado
Criar módulos dedicados de produção:
- `core/merkle.c|h`
- `core/spectral.c|h`
- `core/multilang_fusion.c|h`

Com isso, fecha-se 100% da cadeia conceitual em código executável low-level.
