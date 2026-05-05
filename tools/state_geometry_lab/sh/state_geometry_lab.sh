#!/usr/bin/env bash
set -euo pipefail
SEED_FILE="${1:-rmr/rrr/rafaelia_semente.txt}"
METHOD="${2:-list}"

methods=(
load_seed_digits fibonacci_variant_patterns prime_fibonacci_graph modular_tensor coexistence_matrices
phi_pi_index_field poincare_sphere_sections equilateral_height poincare_ratio_field toroidal_map
lateral_geometry_metrics attractor_field mandelbrot_escape julia_escape fractal_spectrum_72
multilevel_permutations random_permutations_72 rgb_cmyb_interpolate angular_moments polynomial_square_borrow
spectral_64bit_signature
)

if [[ "$METHOD" == "list" ]]; then
  printf '%s\n' "${methods[@]}"
  exit 0
fi

python3 tools/state_geometry_lab/py/state_geometry_lab.py --seed "$SEED_FILE" --method "$METHOD" --json
EOF && chmod +x tools/state_geometry_lab/sh/state_geometry_lab.sh
