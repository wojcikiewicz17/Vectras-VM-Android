# tools/baremetal/rafcode_phi/

Micro-base RAFCODE❤️PHI para **C (casca)** e **ASM (núcleo)** com emissão em **hexadecimal** e layout binário fixo.

## Objetivo
- Tornar o caminho de compilação amigável para bare-metal:
  - parser/validação mínima em C;
  - emissão de palavras de instrução em ASM (ou fallback C);
  - trilha de opcodes em hex (`raf_u32 opcode_hex`);
  - saída `.hex` e `.bin` determinística.

## Estrutura
- `include/rafcode_phi_abi.h`
  - ABI autoral C↔ASM;
  - tipos fixos sem dependência de `stdint`;
  - contrato de header binário fixo (`rafphi_bin_header_t`).
- `c/rafcode_phi_front_shell.c`
  - casca determinística em C;
  - tabela de token por arquitetura: AArch64, x86_64, RISC-V64;
  - CRC32C local para integridade.
- `asm/rafcode_phi_emit_word.S`
  - backend ASM de escrita de palavra para `aarch64` e `x86_64`.
- `c/rafcode_phi_emit_word_c.c`
  - backend fallback para hosts sem ASM suportado no módulo.
- `c/rafcode_phi_cli.c`
  - CLI de emissão e serialização para `.hex` e `.bin`.
- `build_rafcode_phi.sh`
  - casca de build com detecção de host/hwcaps e seleção automática de backend.
- `test_regression_crc32c.sh`
  - regressão de CRC32C, tabela de tokens e layout fixo de arquivos.

## Mapa de opcodes por arquitetura (tokens base)
- `NOP`, `RET`, `BRK`, `HLT`

### AArch64
- `NOP = 0xD503201F`
- `RET = 0xD65F03C0`
- `BRK = 0xD4200000`
- `HLT = 0xD4400000`

### x86_64
- `NOP = 0x00000090`
- `RET = 0x000000C3`
- `BRK = 0x000000CC`
- `HLT = 0x000000F4`

### RISC-V64
- `NOP = 0x00000013`
- `RET = 0x00008067`
- `BRK = 0x00100073`
- `HLT = 0x10500073`

## Contrato de direção
- Núcleo técnico: **C → ASM**.
- `sh`/`rs`: suporte de pipeline/startup.

## Build/execução local (PC/cell/server)
```bash
bash tools/baremetal/rafcode_phi/build_rafcode_phi.sh
./tools/baremetal/rafcode_phi/build/rafcode_phi_cli --arch x86_64 NOP RET BRK HLT
```

## Emissão com layout fixo `.hex` + `.bin`
```bash
./tools/baremetal/rafcode_phi/build/rafcode_phi_cli \
  --arch aarch64 \
  --out-prefix tools/baremetal/rafcode_phi/build/demo_a64 \
  NOP RET BRK HLT
```

Layout `.bin` (little-endian):
1. `magic` (`0x52414650`)
2. `version` (`0x00010000`)
3. `arch`
4. `word_count`
5. `crc32c`
6. `flags`
7. `word[0..N-1]`

## Regressão local
```bash
bash tools/baremetal/rafcode_phi/test_regression_crc32c.sh
```

## CI
- Workflow dedicado: `.github/workflows/rafcode-phi-baremetal-ci.yml`

## Portabilidade alvo de casca
- Linux, Android/Termux, macOS, BSD, Raspberry Pi (via `cc` compatível).
- Windows: via toolchain C/ASM compatível (MSYS2/Clang/MinGW).
