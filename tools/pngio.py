"""Lettura e scrittura minima di PNG (solo libreria standard), per gli script di tools/."""
import struct
import zlib


def read_png(data: bytes):
    """Ritorna (larghezza, altezza, lista di pixel RGBA riga per riga)."""
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "non è un PNG"
    i, idat, pal, trns = 8, b"", None, None
    w = h = ct = 0
    while i < len(data):
        length, = struct.unpack(">I", data[i:i + 4])
        kind, chunk = data[i + 4:i + 8], data[i + 8:i + 8 + length]
        i += 12 + length
        if kind == b"IHDR":
            w, h, depth, ct = struct.unpack(">IIBB", chunk[:10])
            assert depth == 8, "solo PNG a 8 bit"
        elif kind == b"PLTE":
            pal = [tuple(chunk[j:j + 3]) for j in range(0, len(chunk), 3)]
        elif kind == b"tRNS":
            trns = chunk
        elif kind == b"IDAT":
            idat += chunk
    bpp = {6: 4, 2: 3, 3: 1, 4: 2, 0: 1}[ct]
    raw, stride, prev, k, out = zlib.decompress(idat), w * bpp, bytearray(w * bpp), 0, []
    for _ in range(h):
        f = raw[k]
        k += 1
        line = bytearray(raw[k:k + stride])
        k += stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            up, ul = prev[x], (prev[x - bpp] if x >= bpp else 0)
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + up) & 255
            elif f == 3:
                line[x] = (line[x] + (a + up) // 2) & 255
            elif f == 4:
                p = a + up - ul
                pa, pb, pc = abs(p - a), abs(p - up), abs(p - ul)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else up if pb <= pc else ul)) & 255
        prev = line
        for x in range(w):
            if ct == 6:
                out.append(tuple(line[x * 4:x * 4 + 4]))
            elif ct == 2:
                out.append(tuple(line[x * 3:x * 3 + 3]) + (255,))
            elif ct == 4:
                out.append((line[x * 2],) * 3 + (line[x * 2 + 1],))
            elif ct == 0:
                out.append((line[x],) * 3 + (255,))
            else:
                ix = line[x]
                out.append(pal[ix] + ((trns[ix] if trns and ix < len(trns) else 255),))
    return w, h, out


def write_png(path: str, w: int, h: int, pixels) -> None:
    """Scrive un PNG RGBA a 8 bit."""
    raw = b"".join(b"\x00" + bytes(c for px in pixels[y * w:(y + 1) * w] for c in px) for y in range(h))

    def chunk(kind, body):
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)
