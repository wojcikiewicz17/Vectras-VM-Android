# State Geometry Lab

Pacote multi-CLI com módulos em **py/c/rs/sh**, todos com 21 métodos em camadas acessíveis por parâmetro.

## CLI por linguagem
- Python: `python3 tools/state_geometry_lab/py/state_geometry_lab.py --method list`
- Shell wrapper: `tools/state_geometry_lab/sh/state_geometry_lab.sh rmr/rrr/rafaelia_semente.txt list`
- C CLI: `cc tools/state_geometry_lab/c/state_geometry_cli.c -o /tmp/sgl_c && /tmp/sgl_c list`
- Rust CLI: `cd tools/state_geometry_lab/rs && cargo run -- list`

## Fórmulas RAFAELIA (via CLI)
- Listar fórmulas: `python3 tools/state_geometry_lab/py/state_geometry_lab.py --formula list`
- Calcular fórmula: `python3 tools/state_geometry_lab/py/state_geometry_lab.py --formula <id> --params "k=v,..."`
- Helper automático (sem argumentos): `python3 tools/state_geometry_lab/py/state_geometry_lab.py`
- Referência completa: `tools/state_geometry_lab/docs/FORMULAS_RAFAELIA.md`

## Semente
- `rmr/rrr/rafaelia_semente.txt`
Toolkit expandido com Poincaré em seções da esfera, 42 atratores, 72 espectros fractais (Mandelbrot/Julia), e permutações randomicas em 72 níveis.

## Entradas especiais
- Semente de aleatoriedade total: `rmr/rrr/rafaelia_semente.txt`
- Mods coexistentes: `12 14 18 13 7 35 50`
- Seções de momento angular de Poincaré: `{7, 70, 35, 50, 14, 10, 60}`

## Execução
```bash
python3 tools/state_geometry_lab/py/state_geometry_lab.py --demo
python3 tools/state_geometry_lab/py/state_geometry_lab.py --demo --seed rmr/rrr/rafaelia_semente.txt --max 144000
```
