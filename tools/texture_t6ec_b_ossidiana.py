"""T6 Trichromatic, versione Eternal Classics, variante B «Ossidiana fredda».

Corpo nero-blu/violaceo lucido (rampa 0-7), luci a schegge grigio-lilla, finiture in oro chiaro sottile (b-e) e tre gemme
saturissime: ametista (m-p), citrino (u-x), topazio (q-t). Le griglie sono scritte a mano nei due moduli
t6ec_b_g16.py (16x16) e t6ec_b_g32.py (32x32); qui c'e' solo palette, composizione degli stati (corpo + corda + proiettile,
tutti a mano), scrittura dei PNG, tavola di confronto e verifica.

Uso:  python3 tools/texture_t6ec_b_ossidiana.py     # scrive SOLO in design/texture/proposte/t6-ec/b_ossidiana/
"""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import t6ec_b_g16 as G16  # noqa: E402
import t6ec_b_g32 as G32  # noqa: E402

ROOT = os.path.dirname(HERE)
OUT = os.path.join(ROOT, "design", "texture", "proposte", "t6-ec", "b_ossidiana")
SET = os.path.join(OUT, "set")
CUR = os.path.join(ROOT, "src", "main", "resources", "assets", "spectral_ascension", "textures", "item")
EC = os.path.join(ROOT, "design", "per_brz", "2_spectrum_eternal_classics", "assets", "spectrum", "textures", "item")

PAL = {
    "0": "#06050d", "1": "#0f0c20", "2": "#191535", "3": "#272250", "4": "#3a3470", "5": "#5d5a95", "6": "#9a98c8",
    "7": "#dcdaf4",                                                   # ossidiana + schegge lilla
    "b": "#6e5416", "c": "#b8923a", "d": "#ecd47e", "e": "#fff2b8",   # oro chiaro
    "m": "#300a3a", "n": "#9a14a8", "o": "#e832c8", "p": "#ffa6f0",   # ametista
    "q": "#062a3c", "r": "#0a8fb5", "s": "#19d4f2", "t": "#b8fbff",   # topazio
    "u": "#3a2a05", "v": "#b88a08", "w": "#ffd21a", "x": "#fff6a8",   # citrino
    "W": "#ffffff", "S": "#8f8cb8",                                   # riflesso, corda
}


def rows(text):
    return text.strip("\n").splitlines()


def overlay(base, *layers):
    out = [list(r) for r in rows(base)]
    for lay in layers:
        for y, r in enumerate(rows(lay)):
            for x, c in enumerate(r):
                if c != ".":
                    out[y][x] = c
    return ["".join(r) for r in out]


def to_img(grid):
    n = len(grid)
    im = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    for y, r in enumerate(grid):
        assert len(r) == n, (y, len(r), n)
        for x, c in enumerate(r):
            if c != ".":
                h = PAL[c].lstrip("#")
                im.putpixel((x, y), (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255))
    return im


# gemme in canna, scritte a mano: 2x2 + asta (16) e 3x3 (32)
GEM16 = {"amethyst": ("on", "pn", "o"), "citrine": ("wv", "xw", "w"), "topaz": ("sr", "ts", "s"),
         "onyx": ("21", "30", "4"), "moonstone": ("77", "67", "6"), "glass": ("65", "76", "7")}
GEM32 = {"amethyst": ("pon", "oon", "nnm", "o"), "citrine": ("xwv", "wwv", "vvu", "w"),
         "topaz": ("tsr", "ssr", "rrq", "s"), "onyx": ("432", "321", "210", "4"),
         "moonstone": ("W76", "765", "654", "6"), "glass": ("765", "654", "543", "7")}


def gem_layer(n, g, at, shaft_from, shaft_to, dc=1):
    lay = [["."] * n for _ in range(n)]
    for dy, t in enumerate(g[:-1]):
        for dx, ch in enumerate(t):
            lay[at[0] + dy][at[1] + dx] = ch
    for r in range(shaft_from, shaft_to):
        lay[r][r + dc] = g[-1] if r % 2 else "6"
    return "\n".join("".join(r) for r in lay)


def build():
    items = {}
    for k in ("sword", "axe", "shovel", "hoe", "shears", "helmet", "chestplate", "leggings", "boots",
              "fishing_rod", "fishing_rod_cast", "workstaff", "ultra_greatsword", "bident"):
        items[k] = overlay(G16.A[k])
    items["bow"] = overlay(G16.BOW, G16.BOW_S[0])
    for i in range(3):
        items["bow_pulling_%d" % i] = overlay(G16.BOW, G16.BOW_S[i + 1], G16.BOW_A[i])
    items["crossbow_standby"] = overlay(G16.XB, G16.XB_S[0])
    for i in range(3):
        items["crossbow_pulling_%d" % i] = overlay(G16.XB, G16.XB_S[i + 1])
    items["crossbow_arrow"] = overlay(G16.XB, G16.XB_S[3], G16.XB_P["arrow"])
    items["crossbow_firework"] = overlay(G16.XB, G16.XB_S[3], G16.XB_P["firework"])
    for k, g in GEM16.items():
        items["crossbow_" + k] = overlay(G16.XB, G16.XB_S[3], gem_layer(16, g, (2, 2), 4, 11))
    items["ultra_greatsword_oversized"] = overlay(G32.UGS)
    items["workstaff_oversized"] = overlay(G32.WS)
    items["bident_oversized"] = overlay(G32.BID)
    items["crossbow_standby_oversized"] = overlay(G32.XB, G32.XB_S[0])
    for i in range(3):
        items["crossbow_pulling_%d_oversized" % i] = overlay(G32.XB, G32.XB_S[i + 1])
    items["crossbow_arrow_oversized"] = overlay(G32.XB, G32.XB_S[3], G32.XB_ARROW)
    items["crossbow_firework_oversized"] = overlay(G32.XB, G32.XB_S[3], G32.XB_FIREWORK)
    for k, g in GEM32.items():
        items["crossbow_%s_oversized" % k] = overlay(G32.XB, G32.XB_S[3], gem_layer(32, g, (14, 14), 18, 29, 2))
    return items


def write_set(items):
    os.makedirs(SET, exist_ok=True)
    for k, g in items.items():
        to_img(g).save(os.path.join(SET, "trichromatic_%s.png" % k))


def cur_f0(name):
    im = Image.open(os.path.join(CUR, "trichromatic_%s.png" % name)).convert("RGBA")
    return im.crop((0, 0, im.width, im.width))


def verify(items):
    ok = True
    files = sorted(os.listdir(SET))
    print("file nel set:", len(files), "(attesi 45: il brief dice 46, ma i file attuali sono 45)")
    ok &= len(files) == 45
    for k in items:
        p = os.path.join(SET, "trichromatic_%s.png" % k)
        im = Image.open(p)
        exp = 32 if k.endswith("_oversized") else 16
        if im.size != (exp, exp) or im.mode != "RGBA":
            print("ERRORE formato", k, im.size, im.mode); ok = False
        al = {a for *_, a in im.getdata()}
        if not al <= {0, 255}:
            print("ERRORE alfa", k, al); ok = False
        if list(im.getdata()) == list(cur_f0(k).getdata()):
            print("ERRORE identico all'attuale", k); ok = False
    print("VERIFICA:", "OK" if ok else "FALLITA")
    return ok


def tavola(items):
    f = None
    sc = 8
    bg = (38, 38, 42)
    order = [k for k in items if not k.endswith("_oversized")] + [k for k in items if k.endswith("_oversized")]
    small = [k for k in order if not k.endswith("_oversized")]
    big = [k for k in order if k.endswith("_oversized")]
    cw16, ch16 = 16 * sc * 2 + 36, 16 * sc + 28
    cw32, ch32 = 32 * sc * 2 + 36, 32 * sc + 28
    per16, per32 = 5, 3
    r16 = (len(small) + per16 - 1) // per16
    r32 = (len(big) + per32 - 1) // per32
    ec = [("bedrock_sword", "sword", 16), ("bedrock_chestplate", "chestplate", 16),
          ("glass_crest_workstaff", "workstaff", 16), ("glass_crest_crossbow_amethyst_oversized",
                                                       "crossbow_amethyst_oversized", 32)]
    wtot = max(cw16 * per16, cw32 * per32, 40 + sum((s * sc * 2 + 40) for *_, s in ec))
    h = r16 * ch16 + r32 * ch32 + 60 + 32 * sc + 60
    im = Image.new("RGB", (wtot, h), bg)
    d = ImageDraw.Draw(im)

    def paste(img, x, y, size):
        img = img.resize((size * sc, size * sc), Image.NEAREST)
        im.paste(img, (x, y), img)

    y = 0
    for i, k in enumerate(small):
        x, yy = (i % per16) * cw16 + 8, y + (i // per16) * ch16 + 18
        d.text((x, yy - 14), k, fill=(200, 200, 200))
        paste(cur_f0(k), x, yy, 16)
        paste(to_img(items[k]), x + 16 * sc + 12, yy, 16)
    y += r16 * ch16 + 10
    for i, k in enumerate(big):
        x, yy = (i % per32) * cw32 + 8, y + (i // per32) * ch32 + 18
        d.text((x, yy - 14), k, fill=(200, 200, 200))
        paste(cur_f0(k.replace("_oversized", "") + "_oversized"), x, yy, 32)
        paste(to_img(items[k]), x + 32 * sc + 12, yy, 32)
    y += r32 * ch32 + 20
    d.text((8, y), "Confronti con Eternal Classics (EC a sinistra, nostro a destra)", fill=(255, 255, 255))
    y += 22
    x = 8
    for en, ours, s in ec:
        e = Image.open(os.path.join(EC, en + ".png")).convert("RGBA")
        e = e.crop((0, 0, e.width, e.width))
        e = e.resize((s * sc, s * sc), Image.NEAREST) if e.width == s else e.resize((s * sc, s * sc), Image.NEAREST)
        im.paste(e, (x, y), e)
        o = to_img(items[ours]).resize((s * sc, s * sc), Image.NEAREST)
        im.paste(o, (x + s * sc + 8, y), o)
        d.text((x, y + 32 * sc + 4), en, fill=(180, 180, 180))
        x += s * sc * 2 + 40
    im.save(os.path.join(OUT, "tavola_b_ossidiana.png"))


def main():
    items = build()
    assert len(items) == 45, len(items)
    write_set(items)
    ok = verify(items)
    tavola(items)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
