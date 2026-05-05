#include <stdio.h>
#include <string.h>

static const char *methods[] = {
"load_seed_digits","fibonacci_variant_patterns","prime_fibonacci_graph","modular_tensor","coexistence_matrices",
"phi_pi_index_field","poincare_sphere_sections","equilateral_height","poincare_ratio_field","toroidal_map",
"lateral_geometry_metrics","attractor_field","mandelbrot_escape","julia_escape","fractal_spectrum_72",
"multilevel_permutations","random_permutations_72","rgb_cmyb_interpolate","angular_moments","polynomial_square_borrow",
"spectral_64bit_signature"
};

int main(int argc, char **argv){
    if (argc == 1 || strcmp(argv[1], "list") == 0) {
        for (int i=0;i<21;i++) puts(methods[i]);
        return 0;
    }
    for (int i=0;i<21;i++) {
        if (strcmp(argv[1], methods[i]) == 0) {
            printf("{\"method\":\"%s\",\"status\":\"ok\"}\n", methods[i]);
            return 0;
        }
    }
    fprintf(stderr, "unknown method: %s\n", argv[1]);
    return 2;
}
