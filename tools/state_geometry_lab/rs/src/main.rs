use std::env;

fn methods() -> Vec<&'static str> {
    vec![
        "load_seed_digits","fibonacci_variant_patterns","prime_fibonacci_graph","modular_tensor","coexistence_matrices",
        "phi_pi_index_field","poincare_sphere_sections","equilateral_height","poincare_ratio_field","toroidal_map",
        "lateral_geometry_metrics","attractor_field","mandelbrot_escape","julia_escape","fractal_spectrum_72",
        "multilevel_permutations","random_permutations_72","rgb_cmyb_interpolate","angular_moments","polynomial_square_borrow",
        "spectral_64bit_signature"
    ]
}

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() <= 1 || args[1] == "list" {
        for m in methods() { println!("{}", m); }
        return;
    }
    let method = &args[1];
    if !methods().contains(&method.as_str()) {
        eprintln!("unknown method: {}", method);
        std::process::exit(2);
    }
    println!("{{\"method\":\"{}\",\"status\":\"ok\"}}", method);
}
