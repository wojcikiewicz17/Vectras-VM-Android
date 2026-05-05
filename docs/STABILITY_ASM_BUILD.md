# Stability ASM build (ARM32/ARM64 interop)

Fluxo nativo para compilar rotinas low-level de estabilidade diretamente em Assembly, sem camada C no hot path.

## Artefatos

- `out/stability_asm/rmr_stability_arm64_v8a.o`
- `out/stability_asm/rmr_stability_armv7a.o`
- `out/stability_asm/rmr_stability_armv5te.o` (interoperabilidade legada)
- `out/stability_asm/manifest.txt`

## Build

```bash
./tools/build_stability_asm.sh
```

## Contrato de registradores

### arm64 (armv8-a)
- entrada: `x0=state*`, `x1=events*`, `x2=count`
- processamento linear: `x3..x8`
- saída: `x0=stability_score`

### arm32 (armv7-a / armv5te)
- entrada: `r0=state*`, `r1=events*`, `r2=count`
- saída: `r0=stability_score`

## Matriz de interoperabilidade

- ARMv8-A (`arm64-v8a`): trilha Android oficial 64-bit.
- ARMv7-A (`armeabi-v7a`): trilha Android 32-bit suportada para validação interna.
- ARMv5TE: trilha legada de interoperabilidade hardware/software fora da matriz de release oficial Android atual.

## Objetivo

- manter coerência determinística de estado sob recorrência de falhas
- minimizar fricção de pipeline (rotina compacta)
- gerar `.o` específicos por ISA para integração JNI/NDK
