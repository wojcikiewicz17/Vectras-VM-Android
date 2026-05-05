# Stability ASM build (arm32 + arm64)

Este fluxo compila rotinas low-level de estabilidade diretamente em Assembly, sem camadas C no hot path.

## Artefatos

- `out/stability_asm/rmr_stability_arm64.o`
- `out/stability_asm/rmr_stability_armv7.o`

## Build

```bash
./tools/build_stability_asm.sh
```

## Contrato de registradores

### arm64
- entrada: `x0=state*`, `x1=events*`, `x2=count`
- saída: `x0=stability_score`

### armv7
- entrada: `r0=state*`, `r1=events*`, `r2=count`
- saída: `r0=stability_score`

## Objetivo

- manter coerência determinística de estado sob recorrência de falhas
- minimizar fricção de pipeline (rotina compacta, sem abstrações de runtime)
- gerar `.o` dedicados para integração JNI/NDK
