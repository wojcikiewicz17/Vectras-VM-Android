#include <stdio.h>
#include <string.h>

static const char *methods[] = {
"load_seed_digits","fibonacci_variant_patterns","prime_fibonacci_graph","modular_tensor","coexistence_matrices",
"phi_pi_index_field","poincare_sphere_sections","equilateral_height","poincare_ratio_field","toroidal_map",
"lateral_geometry_metrics","attractor_field","mandelbrot_escape","julia_escape","fractal_spectrum_72",
"multilevel_permutations","random_permutations_72","rgb_cmyb_interpolate","angular_moments","polynomial_square_borrow",
"spiral_matrix_cycles","inverse_antiderivative_stack","spectral_64bit_signature","base_projection",
"rafaelia_formula_catalog","rafaelia_toroidal_map7","rafaelia_triangular_core","grassberger_procaccia_probe",
"language_viscosity_metrics","quantum_link_hamiltonian"
};

int main(int argc, char **argv){
    const int count = (int)(sizeof(methods) / sizeof(methods[0]));
    if (argc == 1 || strcmp(argv[1], "list") == 0) {
        for (int i=0;i<count;i++) puts(methods[i]);
        return 0;
    }
    for (int i=0;i<count;i++) {
        if (strcmp(argv[1], methods[i]) == 0) {
            printf("{\"method\":\"%s\",\"status\":\"ok\",\"formula_suite\":\"rafaelia_triangular\"}\n", methods[i]);
            return 0;
        }
    }
    fprintf(stderr, "unknown method: %s\n", argv[1]);
    return 2;
}
