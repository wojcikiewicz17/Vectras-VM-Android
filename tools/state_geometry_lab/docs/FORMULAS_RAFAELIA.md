# Fórmulas RAFAELIA integradas

Fonte: `RAFAELIA_SEMENTES.txt` (commit `9f2eece84f27e4c35b98712ea7ab97727fa5ab2a`).

## IDs disponíveis na CLI
- `q16_sqrt3_2`: `Q16_SQRT3_2 = 0xDDB4 = 56756/65536 ≈ 0.86603`
- `lyapunov_torus`: `λ = ln(sqrt(3)/2)`
- `fibonacci_rafael_fixed_point`: `F* = (π·sin(81°))/(1 - sqrt(3)/2)`
- `equilateral_height`: `h = (sqrt(3)/2)·lado`
- `toroidal_capacity_bits`: `I_Tn = n · 32`
- `semantic_capacity_bits`: `I_sem(L,k) = k · log2(V)`
- `n_ball_volume`: `V_n(r) = π^(n/2) / Γ(n/2+1) · r^n`
- `kaplan_yorke_dimension`: `D_KY = 1 + λ+/|λ−|`
- `attractor_uniform_fraction`: `fração = 1/42`
- `correlation_integral`: `C(ε) = #{pares |x_i−x_j|<ε}/N²`

## Uso
```bash
python3 tools/state_geometry_lab/py/state_geometry_lab.py
python3 tools/state_geometry_lab/py/state_geometry_lab.py --formula list
python3 tools/state_geometry_lab/py/state_geometry_lab.py --formula n_ball_volume --params "n=7,r=1" --json
python3 tools/state_geometry_lab/py/state_geometry_lab.py --formula semantic_capacity_bits --params "k=7,V=170000"
```

Sem parâmetros, a CLI imprime helper com cabeçalho e instruções de parâmetros.
