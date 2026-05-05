# State Geometry Lab

Pacote multi-CLI com módulos em **py/c/rs/sh**, todos com 21 métodos em camadas acessíveis por parâmetro.

## CLI por linguagem
- Python: `python3 tools/state_geometry_lab/py/state_geometry_lab.py --method list`
- Shell wrapper: `tools/state_geometry_lab/sh/state_geometry_lab.sh rmr/rrr/rafaelia_semente.txt list`
- C CLI: `cc tools/state_geometry_lab/c/state_geometry_cli.c -o /tmp/sgl_c && /tmp/sgl_c list`
- Rust CLI: `cd tools/state_geometry_lab/rs && cargo run -- list`

## Semente
- `rmr/rrr/rafaelia_semente.txt`
