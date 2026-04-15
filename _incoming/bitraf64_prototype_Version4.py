# Protótipo inicial bitraf64 - permute + mask + checksum (Python)
# Requisitos: Python 3.8+
# pip install pyblake2 blake3 (opcional para blake3)

import hashlib
import struct
from typing import List

MAGIC = b'BITRAF64'  # 8 bytes ideal (we pad below if needed)

def expand_selos(seed: int, selos: List[str], length: int) -> bytes:
    out = b''
    counter = 0
    while len(out) < length:
        h = hashlib.blake2b(digest_size=32)
        h.update(struct.pack("<Q", seed))
        h.update("".join(selos).encode('utf-8'))
        h.update(struct.pack("<I", counter))
        out += h.digest()
        counter += 1
    return out[:length]

def fractal_permute(block: bytes, seed: int) -> bytes:
    n = len(block)
    perm = list(range(n))
    a = 6364136223846793005
    c = 1442695040888963407
    x = seed or 1
    for i in range(n-1, 0, -1):
        x = (a * x + c) & ((1<<64)-1)
        j = x % (i+1)
        perm[i], perm[j] = perm[j], perm[i]
    out = bytearray(n)
    for i, p in enumerate(perm):
        out[i] = block[p]
    return bytes(out)

def fractal_unpermute(block: bytes, seed: int) -> bytes:
    n = len(block)
    a = 6364136223846793005
    c = 1442695040888963407
    x = seed or 1
    perm = list(range(n))
    for i in range(n-1, 0, -1):
        x = (a * x + c) & ((1<<64)-1)
        j = x % (i+1)
        perm[i], perm[j] = perm[j], perm[i]
    inv = [0]*n
    for i,p in enumerate(perm):
        inv[p] = i
    out = bytearray(n)
    for i in range(n):
        out[inv[i]] = block[i]
    return bytes(out)

def encode_block(block: bytes, seed: int, selos: List[str], block_index: int) -> bytes:
    permuted = fractal_permute(block, seed)
    mask = expand_selos(seed, selos, len(block))
    masked = bytes(a ^ b for a,b in zip(permuted, mask))
    checksum = hashlib.sha3_256(masked).digest()[:8]
    header = bytearray()
    header += MAGIC.ljust(8, b'\x00')            # 8 bytes
    header += struct.pack("<B", 1)               # version
    header += struct.pack("<B", 0)               # flags
    header += struct.pack("<Q", seed)            # seed (8 bytes)
    s_hash = hashlib.blake2b("".join(selos).encode('utf-8'), digest_size=8).digest()
    header += s_hash                              # selos id (8 bytes)
    header += struct.pack("<Q", block_index)      # block_index (8 bytes)
    header += struct.pack("<I", len(masked))      # payload_len (4 bytes)
    header += checksum                             # checksum (8 bytes)
    if len(header) < 64:
        header += b'\x00' * (64 - len(header))
    return bytes(header) + masked

def decode_block(blob: bytes, selos: List[str]):
    header = blob[:64]
    payload = blob[64:]
    seed = struct.unpack("<Q", header[10:18])[0]
    checksum = header[30:38]
    calc = hashlib.sha3_256(payload).digest()[:8]
    if calc != checksum:
        raise ValueError("Checksum mismatch")
    mask = expand_selos(seed, selos, len(payload))
    permuted = bytes(a ^ b for a,b in zip(payload, mask))
    original = fractal_unpermute(permuted, seed)
    return original

if __name__ == "__main__":
    data = b"Hello BITRAF64 prototype" * 100
    seed = 123456789
    selos = ["Σ","Ω","Δ","Φ","B","I","T","R","A","F"]
    blob = encode_block(data, seed, selos, 0)
    recovered = decode_block(blob, selos)
    assert recovered == data
    print("Round-trip OK, len:", len(data))