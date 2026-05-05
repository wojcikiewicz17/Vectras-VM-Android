# Métodos implementados (RAFAELIA state geometry lab)

O laboratório em `tools/state_geometry_lab` agora reúne os métodos geométricos
originais e a suíte triangular RAFAELIA para validar fórmulas de toro,
contração, entropia, fluxo de linguagem e acoplamento quântico-simbólico.

## Métodos base

1. `load_seed_digits`
2. `fibonacci_variant_patterns`
3. `prime_fibonacci_graph`
4. `modular_tensor`
5. `coexistence_matrices` mod[n]={12,14,18,13,7,35,50}
6. `phi_pi_index_field`
7. `poincare_sphere_sections` com {7,70,35,50,14,10,60}
8. `equilateral_height`
9. `poincare_ratio_field` com distinção estável entre 7/3 e 77/33
10. `toroidal_map`
11. `lateral_geometry_metrics`
12. `attractor_field` (42)
13. `mandelbrot_escape` (80 iterações)
14. `julia_escape` (80 iterações)
15. `fractal_spectrum_72`
16. `multilevel_permutations`
17. `random_permutations_72`
18. `rgb_cmyb_interpolate`
19. `angular_moments`
20. `polynomial_square_borrow`
21. `spiral_matrix_cycles`
22. `inverse_antiderivative_stack`
23. `spectral_64bit_signature`
24. `base_projection`

## Suíte triangular RAFAELIA

25. `rafaelia_formula_catalog` — catálogo executável das 50 fórmulas fornecidas.
26. `rafaelia_toroidal_map7` — projeta bytes em `s=(u,v,ψ,χ,ρ,δ,σ) ∈ [0,1)^7` usando FNV-1a, entropia e atrator `(u xor v) mod 42`.
27. `rafaelia_triangular_core` — funde as três geometrias-mãe:
    * `Δ₁` equilátero: `sqrt(3)/2`, Lyapunov `ln(sqrt(3)/2)`, ponto fixo Fibonacci-Rafael.
    * `Δ₂` retângulo: dinâmica iterativa, estimativa Kaplan-Yorke e sonda Grassberger-Procaccia.
    * `Δ₃` isósceles: volumes de n-bola, `n_crítico` aproximado e viscosidade semântica por língua.
28. `grassberger_procaccia_probe` — mede uma dimensão de correlação aproximada sobre o atrator Lorenz sintético já existente.
29. `language_viscosity_metrics` — compara inglês, português, chinês, japonês, hebraico, aramaico e grego por alfabeto, vocabulário, entropia e dimensão toroidal Q16.
30. `quantum_link_hamiltonian` — monta matriz Hamiltoniana 7x7 para os símbolos `(u,v,ψ,χ,ρ,δ,σ)` com acoplamento `α sin(Δθ)cos(Δφ)`.

## Execução

```bash
python3 tools/state_geometry_lab/py/state_geometry_lab.py --method rafaelia_triangular_core --json
bash tools/state_geometry_lab/sh/state_geometry_lab.sh rmr/rrr/rafaelia_semente.txt rafaelia_formula_catalog
cc -std=c11 -Wall -Wextra -pedantic tools/state_geometry_lab/c/state_geometry_lab.c -c -o /tmp/sgl.o -lm
```
