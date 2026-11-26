"""Scrive la struttura vuota 1x1x1 usata dai GameTest (data/spectral_ascension/structure/empty.nbt).

Formato NBT compresso con gzip; DataVersion 3955 = Minecraft 1.21.1.
Uso: python3 -I tools/make_empty_structure.py src/main/resources/data/spectral_ascension/structure/empty.nbt
"""
import gzip
import struct
import sys


def name(s):
    b = s.encode()
    return struct.pack(">H", len(b)) + b


def tag_int(n, v):
    return b"\x03" + name(n) + struct.pack(">i", v)


def tag_list_int(n, values):
    return b"\x09" + name(n) + b"\x03" + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values)


def tag_list_empty(n):
    return b"\x09" + name(n) + b"\x00" + struct.pack(">i", 0)


body = (tag_int("DataVersion", 3955) + tag_list_int("size", [1, 1, 1])
        + tag_list_empty("palette") + tag_list_empty("blocks") + tag_list_empty("entities"))
root = b"\x0a" + name("") + body + b"\x00"

with gzip.open(sys.argv[1], "wb") as f:
    f.write(root)
print("scritto", sys.argv[1])
