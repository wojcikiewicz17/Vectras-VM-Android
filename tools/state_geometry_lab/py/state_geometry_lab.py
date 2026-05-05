#!/usr/bin/env python3
import argparse
import itertools
import math
from dataclasses import dataclass
from typing import Dict, List, Sequence, Tuple

PHI = (1 + 5 ** 0.5) / 2


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


def modular_tensor(values: Sequence[int], mods: Sequence[int]) -> List[List[int]]:
    return [[v % m if m else v for m in mods] for v in values]


def phi_pi_index_field(values: Sequence[int]) -> List[Tuple[float, float]]:
    return [((PHI * v) / (i + 1), (math.pi * (i + 1)) / (v + 1)) for i, v in enumerate(values)]


def equilateral_height(side: float, root_n: int = 3) -> float:
    return side * math.sqrt(root_n) / 2


def toroidal_map(theta: float, phi: float, r: float, R: float) -> Tuple[float, float, float]:
    x = (R + r * math.cos(theta)) * math.cos(phi)
    y = (R + r * math.cos(theta)) * math.sin(phi)
    z = r * math.sin(theta)
    return x, y, z


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


def multilevel_permutations(symbols: Sequence[str], depth: int = 3) -> List[Tuple[str, ...]]:
    return list(itertools.islice(itertools.product(symbols, repeat=depth), 0, 144))


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


def demo(max_n: int, mods: List[int]) -> None:
    fib = fibonacci_variant_patterns(32)
    values = list(range(0, min(max_n, 999) + 1, 3))
    tensor = modular_tensor(values[:64], mods)
    field = phi_pi_index_field(values[:16])
    pts = attractor_field(42)
    sig = spectral_64bit_signature(values[:128])
    print("fib_keys=", list(fib.keys()))
    print("tensor_rows=", len(tensor), "tensor_cols=", len(mods))
    print("phi_pi_first=", field[0])
    print("torus=", toroidal_map(0.7, 1.2, math.sqrt(3 / 2), 4 / 3))
    print("ang_moment=", angular_moments(pts))
    print("signature64=", hex(sig))
    print("base=", base_projection(936), base_projection(999))


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--max", type=int, default=144000)
    ap.add_argument("--mods", nargs="*", type=int, default=[64, 20, 18, 13, 12, 10, 14, 5, 4, 3, 2, 1, 0])
    ap.add_argument("--demo", action="store_true")
    ns = ap.parse_args()
    if ns.demo:
        demo(ns.max, ns.mods)


if __name__ == "__main__":
    main()
