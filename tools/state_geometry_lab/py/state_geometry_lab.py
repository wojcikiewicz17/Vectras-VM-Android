#!/usr/bin/env python3
import argparse
import itertools
import json
import math
from pathlib import Path
from typing import Dict, List, Optional, Sequence, Tuple

PHI = (1 + 5 ** 0.5) / 2
SQRT3_OVER_2 = math.sqrt(3.0) / 2.0
Q16_SCALE = 65536
Q16_SQRT3_OVER_2 = 0xDDB4
FNV64_OFFSET = 0xCBF29CE484222325
FNV64_PRIME = 0x100000001B3


METHODS = [
    "load_seed_digits", "fibonacci_variant_patterns", "prime_fibonacci_graph", "modular_tensor",
    "coexistence_matrices", "phi_pi_index_field", "poincare_sphere_sections", "equilateral_height",
    "poincare_ratio_field", "toroidal_map", "lateral_geometry_metrics", "attractor_field",
    "mandelbrot_escape", "julia_escape", "fractal_spectrum_72", "multilevel_permutations",
    "random_permutations_72", "rgb_cmyb_interpolate", "angular_moments", "polynomial_square_borrow",
    "spiral_matrix_cycles", "inverse_antiderivative_stack", "spectral_64bit_signature", "base_projection",
    "rafaelia_formula_catalog", "rafaelia_toroidal_map7", "rafaelia_triangular_core",
    "grassberger_procaccia_probe", "language_viscosity_metrics", "quantum_link_hamiltonian",
]
# Backwards-compatible alias used by older scripts.
METHODS_21 = METHODS


def is_prime(n: int) -> bool:
    if n < 2:
        return False
    if n % 2 == 0:
        return n == 2
    d = 3
    while d * d <= n:
        if n % d == 0:
            return False
        d += 2
    return True


def load_seed_digits(path: str) -> List[int]:
    p = Path(path)
    if not p.exists():
        return [0]
    digits: List[int] = []
    for ch in p.read_text(encoding="utf-8"):
        if ch.isdigit():
            digits.append(int(ch))
    return digits or [0]


def fibonacci_variant_patterns(n: int) -> Dict[str, List[int]]:
    seed_map = {
        "0001123": [0, 0, 0, 1, 1, 2, 3],
        "001123": [0, 0, 1, 1, 2, 3],
        "01123": [0, 1, 1, 2, 3],
        "0123": [0, 1, 2, 3],
        "123": [1, 2, 3],
    }
    out = {}
    for name, seed in seed_map.items():
        seq = seed[:]
        while len(seq) < n:
            seq.append(seq[-1] + seq[-2])
        out[name] = seq[:n]
    return out


def prime_fibonacci_graph(n: int) -> Dict[int, List[int]]:
    fib = [0, 1]
    while len(fib) < n:
        fib.append(fib[-1] + fib[-2])
    prime_idx = [i for i, v in enumerate(fib) if is_prime(v)]
    graph = {i: [] for i in prime_idx}
    for a, b in zip(prime_idx, prime_idx[1:]):
        graph[a].append(b)
    return graph


def modular_tensor(values: Sequence[int], mods: Sequence[int]) -> List[List[int]]:
    return [[v % m if m else v for m in mods] for v in values]


def coexistence_matrices(values: Sequence[int], mods: Sequence[int]) -> Dict[int, List[List[int]]]:
    mats: Dict[int, List[List[int]]] = {}
    for m in mods:
        if m <= 0:
            continue
        row = [v % m for v in values]
        mats[m] = [[(row[i] + row[j]) % m for j in range(len(row))] for i in range(len(row))]
    return mats


def phi_pi_index_field(values: Sequence[int]) -> List[Tuple[float, float]]:
    return [((PHI * v) / (i + 1), (math.pi * (i + 1)) / (v + 1)) for i, v in enumerate(values)]


def poincare_sphere_sections(mods: Sequence[int], sections: int = 42) -> List[Tuple[int, float, float]]:
    out: List[Tuple[int, float, float]] = []
    for i in range(sections):
        m = mods[i % len(mods)]
        theta = (2 * math.pi * i) / sections
        phi = math.acos(max(-1.0, min(1.0, 1 - 2 * ((i + 1) / (sections + 1)))))
        angular_momentum = (m * math.sin(phi) * math.cos(theta))
        out.append((m, theta, angular_momentum))
    return out


def equilateral_height(side: float, root_n: int = 3) -> float:
    return side * math.sqrt(root_n) / 2


def poincare_ratio_field(values: Sequence[float], ratio_a: float = 7 / 3, ratio_b: float = 77 / 33) -> List[Tuple[float, float, float]]:
    if math.isclose(ratio_a, ratio_b, rel_tol=1e-12, abs_tol=1e-12):
        ratio_b = ratio_b + (PHI / 1000.0)
    out = []
    for i, v in enumerate(values):
        t = i / max(1, len(values) - 1)
        warp = math.tanh(v / (i + 1 if i + 1 else 1))
        out.append((ratio_a * t + warp, ratio_b * (1 - t) + warp, (ratio_a - ratio_b) * (t - 0.5)))
    return out


def toroidal_map(theta: float, phi: float, r: float, R: float) -> Tuple[float, float, float]:
    x = (R + r * math.cos(theta)) * math.cos(phi)
    y = (R + r * math.cos(theta)) * math.sin(phi)
    z = r * math.sin(theta)
    return x, y, z


def lateral_geometry_metrics(side: float, ratio: float = 7 / 3) -> Dict[str, float]:
    side_scaled = side * ratio
    per = 4 * side_scaled
    area = side_scaled * side_scaled
    diag = side_scaled * math.sqrt(2)
    return {"side_scaled": side_scaled, "perimeter": per, "area": area, "diagonal": diag}


def attractor_field(steps: int = 42) -> List[Tuple[float, float, float]]:
    pts = []
    x = y = z = 0.1
    a, b, c = 10.0, 28.0, 8.0 / 3.0
    dt = 0.01
    for _ in range(steps):
        dx = a * (y - x)
        dy = x * (b - z) - y
        dz = x * y - c * z
        x, y, z = x + dx * dt, y + dy * dt, z + dz * dt
        pts.append((x, y, z))
    return pts


def mandelbrot_escape(c: complex, max_iter: int = 80) -> int:
    z = 0 + 0j
    for i in range(max_iter):
        z = z * z + c
        if (z.real * z.real + z.imag * z.imag) > 4.0:
            return i
    return max_iter


def julia_escape(z: complex, k: complex, max_iter: int = 80) -> int:
    for i in range(max_iter):
        z = z * z + k
        if (z.real * z.real + z.imag * z.imag) > 4.0:
            return i
    return max_iter


def fractal_spectrum_72(seed_digits: Sequence[int]) -> List[Tuple[int, int, int]]:
    specs = []
    for i in range(72):
        d = seed_digits[i % len(seed_digits)]
        c = complex((d - 4.5) / 6.0, math.sin(i / 7.0) / 2.0)
        m = mandelbrot_escape(c, max_iter=80)
        j = julia_escape(c, complex(-0.8, 0.156), max_iter=80)
        specs.append((i, m, j))
    return specs


def multilevel_permutations(symbols: Sequence[str], depth: int = 3) -> List[Tuple[str, ...]]:
    return list(itertools.islice(itertools.product(symbols, repeat=depth), 0, 144))


def random_permutations_72(seed_digits: Sequence[int], levels: int = 72) -> List[List[int]]:
    base = list(range(levels))
    out = []
    for i in range(levels):
        shift = seed_digits[i % len(seed_digits)]
        rotated = base[shift:] + base[:shift]
        if i % 2 == 0:
            rotated = list(reversed(rotated))
        out.append(rotated)
        base = rotated
    return out


def rgb_cmyb_interpolate(rgb: Tuple[float, float, float], t: float = 0.5) -> Tuple[float, float, float, float]:
    r, g, b = rgb
    c, m, y = 1 - r, 1 - g, 1 - b
    k = min(c, m, y)
    cmyb = (c, m, y, b)
    return tuple((1 - t) * v + t * k for v in cmyb)


def angular_moments(points: Sequence[Tuple[float, float, float]]) -> float:
    return sum(math.sqrt(x * x + y * y + z * z) for x, y, z in points)


def polynomial_square_borrow(x: float) -> float:
    return x * x - math.pi * x + math.sqrt(abs(x))


def spiral_matrix_cycles(size: int, cycles: int) -> List[List[int]]:
    mat = [[0] * size for _ in range(size)]
    v = 1
    for c in range(cycles):
        freeze = c % size
        for i in range(size):
            for j in range(size):
                if i == freeze or j == freeze:
                    continue
                mat[i][j] = v
                v += 1
    return mat


def inverse_antiderivative_stack(values: Sequence[float]) -> Dict[str, List[float]]:
    inv = [1 / v if v else 0 for v in values]
    rev = list(reversed(values))
    anti = []
    acc = 0.0
    for v in values:
        acc += v
        anti.append(acc)
    return {"inverse": inv, "reverse": rev, "antiderivative": anti}


def spectral_64bit_signature(values: Sequence[int]) -> int:
    mask = (1 << 64) - 1
    sig = 0x9E3779B97F4A7C15
    for v in values:
        sig ^= (v + 0x9E3779B97F4A7C15 + ((sig << 6) & mask) + (sig >> 2)) & mask
    return sig & mask



def clamp01(value: float) -> float:
    return max(0.0, min(1.0, value))


def q16_to_unit(value: int) -> float:
    return (value & 0xFFFF) / Q16_SCALE


def fnv1a64(data: bytes, seed: int = FNV64_OFFSET) -> int:
    h = seed & ((1 << 64) - 1)
    for byte in data:
        h ^= byte
        h = (h * FNV64_PRIME) & ((1 << 64) - 1)
    return h


def entropy_milli(data: bytes) -> int:
    if not data:
        return 0
    unique = len(set(data))
    transitions = sum(1 for a, b in zip(data, data[1:]) if a != b)
    transition_den = max(1, len(data) - 1)
    return int((unique * 6000) / 256 + (transitions * 2000) / transition_den)


def ewma_update(current: float, incoming: float, alpha: float = 0.25) -> float:
    return (1.0 - alpha) * current + alpha * incoming


def coherence_phi(coherence: float, entropy: float) -> float:
    return clamp01((1.0 - clamp01(entropy)) * clamp01(coherence))


def rafaelia_formula_catalog() -> List[Dict[str, str]]:
    formulas = [
        ("T7", "T^7=(R/Z)^7", "seven-dimensional toroidal state space"),
        ("state", "s=(u,v,psi,chi,rho,delta,sigma)", "normalized state vector"),
        ("map", "s=ToroidalMap(x)", "data entropy hash state projection"),
        ("input", "x=(data,entropy,hash,state)", "runtime observation bundle"),
        ("coherence_ewma", "C[t+1]=(1-alpha)C[t]+alpha*C_in", "coherence low-pass filter"),
        ("entropy_ewma", "H[t+1]=(1-alpha)H[t]+alpha*H_in", "entropy low-pass filter"),
        ("alpha", "alpha=0.25", "Q2-friendly smoothing"),
        ("phi_coherence", "phi=(1-H)*C", "coherence under entropy pressure"),
        ("attractor_limit", "lim s(t) in A", "eventual attractor basin"),
        ("attractor_count", "|A|=42", "RAFAELIA attractor classes"),
        ("spectrum", "S(w)=F[Psi(t)]", "signal spectrum"),
        ("cardio_resonance", "R=<S,H_cardio>/(||S|| ||H_cardio||)", "normalized cardio spectral match"),
        ("language_tensor", "I=⊗_L(R_L*F(G_L))", "cross-language resonance tensor"),
        ("entropy_estimate", "H≈U/256+T/N", "unique-byte and transition estimate"),
        ("phi_hash", "h=(h xor x)*phi", "golden multiplicative mixer"),
        ("crc", "CRC(x)=sum x_i*P(x)", "polynomial checksum"),
        ("merkle", "R=Merkle(H_1,H_2,...)", "audit root"),
        ("spiral_decay", "r_n=(sqrt(3)/2)^n", "geometric contraction"),
        ("golden_ratio", "phi=(1+sqrt(5))/2", "irrational mixing constant"),
        ("edge_energy", "E=sin(dtheta)cos(dphi)", "angular link energy"),
        ("iteration", "x[n+1]=f(x[n])", "discrete dynamics"),
        ("fibonacci_rafael", "F[n+1]=F[n]*sqrt(3)/2-pi*sin(279deg)", "contractive fixed point"),
        ("period_42", "x[n+42]=x[n]", "cycle window"),
        ("capacity", "C=M*N", "grid cardinality"),
        ("info_bound", "I<=log2(M*N)", "geometric information limit"),
        ("pi_max", "Pi_max=max{H|state!=VOID}", "non-void entropy ceiling"),
        ("pi_max_value", "Pi_max≈0.9", "operational coherence threshold"),
        ("row_coprime", "gcd(delta_r,R)=1", "ergodic row stride"),
        ("col_coprime", "gcd(delta_c,C)=1", "ergodic column stride"),
        ("xor_acc", "acc=xor_i byte_i", "byte parity accumulator"),
        ("fnv_xor", "h=h xor byte", "FNV xor step"),
        ("fnv_mul", "h=h*0x100000001B3", "FNV prime step"),
        ("crc_complement", "crc=~sum byte_i*poly(x)", "complemented CRC"),
        ("vfc_key", "k(t)=Q(VFC(t))", "time-varying quantized key"),
        ("stream_xor", "c_i=p_i xor k(t_i)", "stream cipher relation"),
        ("gauss", "div E=rho/epsilon_0", "field-source constraint"),
        ("angular_kernel", "sin(dtheta)cos(dphi)", "directional similarity kernel"),
        ("equilateral_height", "h=sqrt(3)/2*l", "equilateral geometry"),
        ("spiral", "Spiral(n)=(sqrt(3)/2)^n", "contractive spiral"),
        ("prime_factor", "n=prod p_i^e_i", "number-theoretic factorization"),
        ("language_graph_fft", "F(G_L)", "language graph spectrum"),
        ("metric_gap", "d_theta(u,v)!=d_gamma(u,v)", "metric-dependent meaning"),
        ("entropy_milli", "unique*6000/256+transitions*2000/(len-1)", "integer entropy proxy"),
        ("language_resonance", "R_L=<S_L,H_cardio>/(||S_L|| ||H_cardio||)", "per-language resonance"),
        ("unit_state", "s in [0,1)^7", "normalized torus coordinates"),
        ("geom_bits", "bits_geom=log2(M*N)", "grid information bits"),
        ("hamiltonian", "Hhat=sum eps_i|a_i><a_i|+sum J_ij(...)", "coupled alphabet-symbol Hamiltonian"),
        ("link_energy", "E_link=alpha*sin(dtheta)cos(dphi)", "scaled angular coupling"),
        ("geom_capacity", "C_geom=M*N", "geometric capacity"),
        ("core_phi", "I=Phi(s,S,H,C,G)", "unified RAFAELIA core functional"),
    ]
    return [{"id": f"F{i:02d}", "name": name, "formula": formula, "meaning": meaning} for i, (name, formula, meaning) in enumerate(formulas, 1)]


def rafaelia_toroidal_map7(payload: bytes, entropy: Optional[float] = None, state: int = 0) -> Dict[str, object]:
    h = fnv1a64(payload)
    entropy_value = clamp01((entropy_milli(payload) / 8000.0) if entropy is None else entropy)
    unique_ratio = len(set(payload)) / 256.0 if payload else 0.0
    transition_ratio = (sum(1 for a, b in zip(payload, payload[1:]) if a != b) / max(1, len(payload) - 1)) if payload else 0.0
    words = [
        h & 0xFFFF, (h >> 16) & 0xFFFF, (h >> 32) & 0xFFFF, (h >> 48) & 0xFFFF,
        int(entropy_value * Q16_SCALE), int(unique_ratio * Q16_SCALE),
        (state * Q16_SQRT3_OVER_2 + int(transition_ratio * Q16_SCALE)) & 0xFFFF,
    ]
    labels = ["u", "v", "psi", "chi", "rho", "delta", "sigma"]
    coords = {label: q16_to_unit(word) for label, word in zip(labels, words)}
    attractor = (words[0] ^ words[1]) % 42
    coherence = 1.0 - entropy_value
    return {
        "hash64": f"0x{h:016x}",
        "entropy": entropy_value,
        "coherence": coherence,
        "phi": coherence_phi(coherence, entropy_value),
        "state": coords,
        "attractor": attractor,
        "period": 42,
    }


def n_ball_volume(n: int, radius: float = 1.0) -> float:
    return (math.pi ** (n / 2.0) / math.gamma(n / 2.0 + 1.0)) * (radius ** n)


def fibonacci_rafael_fixed_point(theta_deg: float = 30.0, forcing_angle_deg: float = 279.0) -> Dict[str, float]:
    contraction = math.cos(math.radians(theta_deg))
    forcing = -math.pi * math.sin(math.radians(forcing_angle_deg))
    fixed = forcing / (1.0 - contraction)
    return {"contraction": contraction, "forcing": forcing, "fixed_point": fixed, "lyapunov": math.log(abs(contraction))}


def language_viscosity_metrics() -> Dict[str, Dict[str, float]]:
    languages = {
        "english": (26, 170000, 1.30, 5.0, 7.0),
        "portuguese": (26, 260000, 1.35, 5.3, 7.0),
        "chinese": (214, 50000, 2.20, 1.5, 8.0),
        "japanese": (96 + 2136, 200000, 2.05, 2.0, 9.0),
        "hebrew": (22, 80000, 1.55, 4.0, 3.0),
        "aramaic": (22, 30000, 1.45, 4.0, 3.0),
        "greek": (24, 120000, 1.50, 5.0, 4.0),
    }
    out: Dict[str, Dict[str, float]] = {}
    for name, (alphabet, vocab, entropy_char, avg_word, target_dim) in languages.items():
        h_max = math.log2(alphabet)
        sem_bits = 7.0 * avg_word * entropy_char
        q16_dim = sem_bits / 32.0
        volume_fit = min(range(1, 13), key=lambda n: abs(n_ball_volume(n) - (entropy_char / h_max)))
        viscosity = (math.log2(vocab) / 32.0) * (1.0 + entropy_char / h_max)
        out[name] = {
            "alphabet": alphabet,
            "vocabulary": vocab,
            "entropy_bits_per_char": entropy_char,
            "h_max_bits": h_max,
            "semantic_bits_7_words": sem_bits,
            "q16_dimension_estimate": q16_dim,
            "n_ball_dimension_fit": float(volume_fit),
            "declared_transition_dimension": target_dim,
            "semantic_viscosity": viscosity,
        }
    return out


def grassberger_procaccia_probe(samples: int = 256) -> Dict[str, object]:
    pts = attractor_field(max(8, samples))
    eps_values = [2.0 ** e for e in range(-8, 1)]
    counts = []
    n = len(pts)
    for eps in eps_values:
        c = 0
        for i in range(n):
            xi, yi, zi = pts[i]
            for j in range(i + 1, n):
                xj, yj, zj = pts[j]
                if math.dist((xi, yi, zi), (xj, yj, zj)) < eps:
                    c += 1
        corr = (2.0 * c) / max(1, n * (n - 1))
        counts.append((eps, corr))
    usable = [(math.log(e), math.log(c)) for e, c in counts if c > 0.0]
    if len(usable) >= 2:
        xs, ys = zip(*usable)
        xbar = sum(xs) / len(xs)
        ybar = sum(ys) / len(ys)
        denom = sum((x - xbar) ** 2 for x in xs) or 1.0
        slope = sum((x - xbar) * (y - ybar) for x, y in usable) / denom
    else:
        slope = 0.0
    return {"samples": n, "correlation_dimension_d2": slope, "eps_correlation": counts}


def quantum_link_hamiltonian(size: int = 7, alpha: float = 0.25) -> Dict[str, object]:
    labels = ["u", "v", "psi", "chi", "rho", "delta", "sigma"][:size]
    diagonal = [SQRT3_OVER_2 * (i + 1) / size for i in range(size)]
    couplings: List[List[float]] = []
    for i in range(size):
        row = []
        for j in range(size):
            if i == j:
                row.append(diagonal[i])
            else:
                dtheta = 2.0 * math.pi * abs(i - j) / max(1, size)
                dphi = math.pi * (i + j + 1) / max(1, size)
                row.append(alpha * math.sin(dtheta) * math.cos(dphi))
        couplings.append(row)
    return {"basis": labels, "diagonal": diagonal, "hamiltonian": couplings}


def rafaelia_triangular_core(payload: bytes = b"RAFAELIA") -> Dict[str, object]:
    fixed = fibonacci_rafael_fixed_point()
    lambda_negative = fixed["lyapunov"]
    lambda_positive = 0.05
    kaplan_yorke = 1.0 + lambda_positive / abs(lambda_negative)
    torus = rafaelia_toroidal_map7(payload)
    volumes = {str(n): n_ball_volume(n) for n in range(1, 10)}
    return {
        "delta_equilateral": {
            "cos30": SQRT3_OVER_2,
            "q16": Q16_SQRT3_OVER_2,
            "lyapunov": lambda_negative,
            "fixed_point": fixed["fixed_point"],
            "interpretation": "SIMETRIA -> CONTRACAO -> PONTO_FIXO -> OMEGA",
        },
        "delta_rectangle": {
            "sin2_plus_cos2": 1.0,
            "lambda_positive_estimate": lambda_positive,
            "kaplan_yorke_dimension": kaplan_yorke,
            "grassberger_procaccia_probe": grassberger_procaccia_probe(96),
            "interpretation": "FLUXO -> INSTABILIDADE -> FRACTAL -> D_H",
        },
        "delta_isosceles": {
            "n_ball_unit_volumes": volumes,
            "language_metrics": language_viscosity_metrics(),
            "interpretation": "ASSIMETRIA -> CODIFICACAO -> LIMITE -> n_CRITICO",
        },
        "total": {
            "equation": "R=f(cos(theta), lambda, H)",
            "toroidal_state": torus,
            "formula_count": len(rafaelia_formula_catalog()),
        },
    }

def base_projection(n: int) -> Dict[str, str]:
    def to_base(num: int, base: int) -> str:
        chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        if num == 0:
            return "0"
        out = []
        x = num
        while x > 0:
            out.append(chars[x % base])
            x //= base
        return "".join(reversed(out))

    return {"base2": to_base(n, 2), "base20": to_base(n, 20), "base60": to_base(n, 60)}


def demo(max_n: int, mods: List[int], seed_path: str) -> None:
    fib = fibonacci_variant_patterns(42)
    values = list(range(0, min(max_n, 1196) + 1, 3))
    tensor = modular_tensor(values[:64], mods)
    co = coexistence_matrices(values[:16], [12, 14, 18, 13, 7, 35, 50])
    primes_graph = prime_fibonacci_graph(64)
    field = phi_pi_index_field(values[:16])
    poincare = poincare_ratio_field([float(v) for v in values[:16]])
    sections = poincare_sphere_sections([7, 70, 35, 50, 14, 10, 60], sections=42)
    pts = attractor_field(42)
    sig = spectral_64bit_signature(values[:128])
    lat = lateral_geometry_metrics(33, ratio=7 / 3)
    seed_digits = load_seed_digits(seed_path)
    frac72 = fractal_spectrum_72(seed_digits)
    perm72 = random_permutations_72(seed_digits, levels=72)
    print("fib_keys=", list(fib.keys()))
    print("tensor_rows=", len(tensor), "tensor_cols=", len(mods))
    print("coexistence_mods=", sorted(co.keys()))
    print("prime_graph_nodes=", len(primes_graph), "first=", next(iter(primes_graph.items())) if primes_graph else None)
    print("phi_pi_first=", field[0])
    print("poincare_first=", poincare[0], "ratio_7_3=", 7 / 3, "ratio_77_33=", 77 / 33, "ratio_delta=", abs((7 / 3) - (77 / 33)))
    print("poincare_sections_first=", sections[0])
    print("lateral=", lat)
    print("torus=", toroidal_map(0.7, 1.2, math.sqrt(3 / 2), 4 / 3))
    print("ang_moment=", angular_moments(pts))
    print("signature64=", hex(sig))
    print("fractal72_first=", frac72[0], "fractal72_last=", frac72[-1])
    print("perm72_first_head=", perm72[0][:10], "perm72_last_head=", perm72[-1][:10])
    print("base=", base_projection(936), base_projection(999), "n1196=", base_projection(1196))
    print("rafaelia_core=", json.dumps(rafaelia_triangular_core(b"RAFAELIA"), sort_keys=True)[:700] + "...")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--max", type=int, default=144000)
    ap.add_argument("--mods", nargs="*", type=int, default=[64, 20, 18, 13, 12, 10, 14, 5, 4, 3, 2, 1, 0])
    ap.add_argument("--seed", type=str, default="rmr/rrr/rafaelia_semente.txt")
    ap.add_argument("--demo", action="store_true")
    ap.add_argument("--method", type=str, default="")
    ap.add_argument("--json", action="store_true")
    ns = ap.parse_args()
    if ns.method:
        if ns.method not in METHODS and ns.method != "list":
            raise SystemExit(f"unknown method: {ns.method}")
        if ns.method == "list":
            print(json.dumps(METHODS) if ns.json else "\n".join(METHODS))
            return
        seed_bytes = Path(ns.seed).read_bytes() if Path(ns.seed).exists() else ns.seed.encode("utf-8")
        if ns.method == "rafaelia_formula_catalog":
            payload = rafaelia_formula_catalog()
        elif ns.method == "rafaelia_toroidal_map7":
            payload = rafaelia_toroidal_map7(seed_bytes)
        elif ns.method == "rafaelia_triangular_core":
            payload = rafaelia_triangular_core(seed_bytes)
        elif ns.method == "grassberger_procaccia_probe":
            payload = grassberger_procaccia_probe()
        elif ns.method == "language_viscosity_metrics":
            payload = language_viscosity_metrics()
        elif ns.method == "quantum_link_hamiltonian":
            payload = quantum_link_hamiltonian()
        else:
            payload = {"method": ns.method, "seed": ns.seed, "status": "ok"}
        print(json.dumps(payload, ensure_ascii=False, sort_keys=True) if ns.json else payload)
        return
    if ns.demo:
        demo(ns.max, ns.mods, ns.seed)


if __name__ == "__main__":
    main()
