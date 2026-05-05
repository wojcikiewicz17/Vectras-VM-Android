#!/usr/bin/env python3
import argparse
import itertools
import json
import math
from pathlib import Path
from typing import Dict, List, Sequence, Tuple

PHI = (1 + 5 ** 0.5) / 2


METHODS_21 = [
    "load_seed_digits","fibonacci_variant_patterns","prime_fibonacci_graph","modular_tensor","coexistence_matrices",
    "phi_pi_index_field","poincare_sphere_sections","equilateral_height","poincare_ratio_field","toroidal_map",
    "lateral_geometry_metrics","attractor_field","mandelbrot_escape","julia_escape","fractal_spectrum_72",
    "multilevel_permutations","random_permutations_72","rgb_cmyb_interpolate","angular_moments","polynomial_square_borrow",
    "spectral_64bit_signature"
]


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
        if ns.method not in METHODS_21 and ns.method != "list":
            raise SystemExit(f"unknown method: {ns.method}")
        if ns.method == "list":
            print(json.dumps(METHODS_21) if ns.json else "\n".join(METHODS_21))
            return
        payload = {"method": ns.method, "seed": ns.seed, "status": "ok"}
        print(json.dumps(payload) if ns.json else payload)
        return
    if ns.demo:
        demo(ns.max, ns.mods, ns.seed)


if __name__ == "__main__":
    main()
