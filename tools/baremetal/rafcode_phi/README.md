<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# tools/baremetal/rafcode_phi/

Micro-base RAFCODE❤️PHI para **C (casca)** e **ASM (núcleo)** com emissão em **hexadecimal**.

## Objetivo
- Tornar o caminho de compilação amigável para bare-metal:
  - parser/validação mínima em C;
  - emissão de palavras de instrução em ASM;
  - trilha de opcodes em hex (`raf_u32 opcode_hex`).

## Estrutura
- `include/rafcode_phi_abi.h`
  - ABI autoral C↔ASM;
  - tipos fixos sem dependência de `stdint`/libc;
  - contratos de emissão (`rafphi_emit_word_abi`, `rafphi_emit_word_asm`, `rafphi_emit_block_hex`).
- `c/rafcode_phi_front_shell.c`
  - casca determinística em C;
  - mapa mínimo de mnemônicos (`NOP`, `RET`, `BRK`, `HLT`) para hex por arquitetura (`aarch64`, `x86_64`, `riscv64`);
  - CRC32C local para integridade da saída.
- `c/rafcode_phi_vecbit.c`
  - verificação vetorial de vizinhança de bits entre palavras emitidas;
  - distância de Hamming entre vizinhos consecutivos;
  - hash chain FNV-1a 64-bit sobre toda cadeia.
- `asm/rafcode_phi_emit_word.S`
  - rotina ASM para gravação da palavra hex no buffer de saída;
  - entrada ABI crua em registradores (`rafphi_emit_word_abi`);
  - caminhos para `__aarch64__` e `__x86_64__`.

## Contrato de direção
- Núcleo técnico: **C → ASM**.
- `sh`/`rs`: somente suporte de pipeline/startup.

## Inspeção rápida
```bash
sed -n '1,220p' tools/baremetal/rafcode_phi/include/rafcode_phi_abi.h
sed -n '1,260p' tools/baremetal/rafcode_phi/c/rafcode_phi_front_shell.c
sed -n '1,260p' tools/baremetal/rafcode_phi/c/rafcode_phi_vecbit.c
sed -n '1,220p' tools/baremetal/rafcode_phi/asm/rafcode_phi_emit_word.S
```

## Erros comuns
- **Referência legado inexistente**: `asm/rafaelia_core.S` **não** faz parte deste repositório.
- **Caminho válido do backend ASM**: use `tools/baremetal/rafcode_phi/asm/rafcode_phi_emit_word.S`.
- Se houver script/local config antigo apontando para `asm/rafaelia_core.S`, atualize para o caminho acima antes de compilar.
