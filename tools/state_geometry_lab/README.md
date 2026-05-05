# State Geometry Lab

Conjunto de métodos combinatórios, geométricos e algébricos (1D..56D) com foco em:
- sequências tipo Fibonacci mod N e permutações multinível;
- transformações em toro, espirais, atratores e projeções;
- métricas (lado, raio, perímetro, área, volume) e inversões;
- espaço de cor RGB/CMYB com interpolação e coerência espectral.

## Estrutura
- `py/state_geometry_lab.py`: núcleo com 14 métodos coerentes e CLI.
- `c/state_geometry_lab.c`: biblioteca C para kernels modulares.
- `asm/mod_cycle_aarch64.S`: exemplo ASM AArch64 para ciclo modular.
- `docs/METHODS.md`: mapeamento matemático resumido.

## Execução rápida
```bash
python3 tools/state_geometry_lab/py/state_geometry_lab.py --demo
python3 tools/state_geometry_lab/py/state_geometry_lab.py --max 144000 --mods 64 20 18 13 12 10 14 5 4 3 2 1
```
