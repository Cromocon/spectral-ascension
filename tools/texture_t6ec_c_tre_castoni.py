"""T6 Trichromatic, variante EC «c_tre_castoni» (corpo carbone, tre gemme pure in tre punti diversi).

Tutto disegnato A MANO: ogni sprite e' una griglia di caratteri scritta riga per riga. Il codice qui sotto
converte le griglie in PNG, sovrappone gli strati disegnati a mano (corda, proiettile) al corpo di arco e balestra,
verifica i file e compone la tavola di confronto. Non genera forme, gradienti, rumore o ricolorazioni.

Palette (lettere):
  0-4  carbone neutro opaco (0 contorno, 1-3 corpo, 4 luce dura)      z corda / ferro chiaro   W bianco
  q r s t  topazio ciano     (q contorno, r ombra, s tono pieno, t luce dura)
  m n o p  ametista magenta  (m contorno, n ombra, o tono pieno, p luce dura)
  u v w x  citrino giallo    (u contorno, v ombra, w tono pieno, x luce dura)
Ogni gemma e' un grappolo con rampa propria di 3 toni piu' contorno; i colori non si mescolano mai nello stesso grappolo.

Uso:  python3 tools/texture_t6ec_c_tre_castoni.py     # scrive SOLO in design/texture/proposte/t6-ec/c_tre_castoni/
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VAR = "c_tre_castoni"
OUT = os.path.join(ROOT, "design", "texture", "proposte", "t6-ec", VAR)
SET = os.path.join(OUT, "set")
CUR = os.path.join(ROOT, "src", "main", "resources", "assets", "spectral_ascension", "textures", "item")
EC = os.path.join(ROOT, "design", "per_brz", "2_spectrum_eternal_classics", "assets", "spectrum", "textures", "item")

PAL = {
    "0": "#060608", "1": "#1a1a20", "2": "#2a2a33", "3": "#3d3d49", "4": "#5a5a69",
    "z": "#9a9aae", "W": "#ffffff",
    "q": "#04283a", "r": "#0790b8", "s": "#14d6ff", "t": "#c4fbff",
    "m": "#38042f", "n": "#a2107e", "o": "#f01fc4", "p": "#ffb2f2",
    "u": "#3a2c00", "v": "#b58f00", "w": "#ffe014", "x": "#fffcb4",
}


def hx(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def G(text, n=16):
    rows = [r.strip() for r in text.strip().splitlines()]
    assert len(rows) == n and all(len(r) == n for r in rows), [(i, len(r), r) for i, r in enumerate(rows) if len(r) != n] or len(rows)
    return rows


def to_img(rows):
    n = len(rows)
    im = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch != ".":
                im.putpixel((x, y), hx(PAL[ch]))
    return im


def overlay(base, *layers):
    """Sovrappone griglie (a mano) al corpo: il carattere '.' lascia il pixel com'e'; ',' lo cancella."""
    rows = [list(r) for r in base]
    for lay in layers:
        for y, r in enumerate(lay):
            for x, ch in enumerate(r):
                if ch == ".":
                    continue
                rows[y][x] = "." if ch == "," else ch
    return ["".join(r) for r in rows]


A16 = {}

# ===========================================================================
# 16x16: griglie scritte a mano (fotogramma 0)
# ===========================================================================
A16["sword"] = G("""
..............00
............0tt0
...........0ts20
..........0s320.
.........0s320..
........0s320...
...0uu00s320....
....0uws320.....
....0uwwv0......
.....0wwwv0.....
....020.0wv0....
...020....0u0...
..020...........
.0no0...........
0op0............
.00.............
""")
A16["axe"] = G("""
......0000......
....00tsss000...
...0tss3333320..
..0ss3320wvv20..
..0s33200xwv30..
..0s330.0vvu00..
...0s0..0030....
...00..0230.....
......0230......
.....0230.......
....0230........
...0230.........
..0230..........
.0no0...........
0opn0...........
.000............
""")
A16["shovel"] = G("""
.........0000...
........0ttss0..
.......0tss3320.
.......0ss33320.
.......0s333220.
........0s3220..
........0uwv0...
.......0230.....
......0230......
.....0230.......
....0230........
...0230.........
..0230..........
.0no0...........
0opn0...........
.000............
""")
A16["hoe"] = G("""
......00000.....
....00tssss00...
...0ts33333320..
...0s00.0uwv20..
...0s0..0xwv0...
...00...0uv00...
.........020....
........0230....
.......0230.....
......0230......
.....0230.......
....0230........
...0230.........
..0no0..........
.0opn0..........
..000...........
""")
A16["shears"] = G("""
.........00..00.
........0ts00ts0
.......0ts30s320
.......0s3200320
........0s30320.
.........0uw30..
.........0wv0...
........0uwv0...
.......00uv00...
......0230.020..
.....0230...0230
....0n00....0m00
...0onp0....0opm
...0nnp0....0ppm
....000......000
................
""")
A16["helmet"] = G("""
................
....00000000....
...0334444330...
..033444433330..
..0340uwv03320..
..0330xwv03320..
..0333300033320.
..0ts00000003320
..0s02333330320.
..0330....0no0..
..0320....0op0..
..0220....0000..
..0000..........
................
................
................
""")
A16["chestplate"] = G("""
................
.00000....00000.
.0tss3000033320.
.0srs30uwwv03320
.0s3330xwwv03320
.003330vwwu03320
..03330000003320
...0433333320...
...0433333220...
...0333333220...
...033330no00...
...033330op00...
...0332200000...
...0222222220...
...0000000000...
................
""")
A16["leggings"] = G("""
................
..000000000000..
..0333444433320.
..03330uwv033320
..03330xwv033320
..03330vwu033320
..03320000033320
..03320..03320..
..0s320..03320..
..0s320..0no20..
..0s330..0op20..
..03320..03320..
..03320..03320..
..02220..02220..
..00000..00000..
................
""")
A16["boots"] = G("""
................
................
..00000.........
..0tss0.........
..0s330.........
..03320..00000..
..03320..0uwv0..
..03320..0xwv0..
..03320..0vwu0..
.033333200333320
.033333200333320
.0tsr3320333no20
.0sr33220333op20
.00000000000000.
................
................
""")
A16["fishing_rod"] = G("""
.............0t0
............0s0z
...........0s0.z
..........0r0..z
.........030...z
........030....z
.......030.....z
......030.....no
.....030.000..op
....030xwv0.....
...030wwu0......
..0300000.......
.0n0............
0op0............
.00.............
................
""")
A16["fishing_rod_cast"] = G("""
.............0t0
............0s0z
...........0s0.z
..........0r0..z
.........030...z
........030....z
.......030.....z
......030......z
.....030.000...z
....030xwv0....z
...030wwu0.....z
..0300000......z
.0n0...........z
0op0...........z
.00............z
................
""")

BOW_BODY = G("""
.....00000000000
....04433333rst0
..0443333333rs0.
.0443330000000..
0443300.........
0430............
0vw0............
0xw0............
0wv0............
0430............
0430............
0430............
0no0............
0op0............
.00.............
................
""")


def pix(spec, ch="z", n=16):
    """Strato disegnato a mano pixel per pixel: «riga,colonna» separati da spazio."""
    g = [["."] * n for _ in range(n)]
    for p in spec.split():
        r, c = p.split(",")
        g[int(r)][int(c)] = ch
    return ["".join(r) for r in g]


BOW_S = {
    "bow": pix("3,12 4,11 5,10 6,9 7,8 8,7 9,6 10,5 11,4"),
    "bow_pulling_0": pix("3,12 4,11 5,11 6,10 7,10 8,9 9,9 10,8 10,7 10,6 11,5 11,4"),
    "bow_pulling_1": pix("3,12 4,12 5,12 6,12 7,11 8,11 9,11 10,11 10,10 10,9 10,8 10,7 11,6 11,5 11,4"),
    "bow_pulling_2": pix("3,12 4,13 5,13 6,13 7,13 8,13 9,13 10,13 11,13 12,13 12,12 12,11 12,10 12,9 12,8 12,7 12,6 12,5 12,4 11,4"),
}
for k, v in BOW_S.items():
    A16[k] = overlay(BOW_BODY, v)

A16["workstaff"] = G("""
........0000....
.......0tss30...
......0trs3320..
......0rs33uw0..
......0333xwv0..
......0no33vu0..
.......0op320...
.......0000.....
.....0320.......
....0320........
...0320.........
..0320..........
.0320...........
0320............
.00.............
................
""")
A16["ultra_greatsword"] = G("""
..........000000
.........0ts3320
........0ts3320.
.......0ts3320..
......0ts3320...
.....0ts3320....
....0ts3320.....
...0ts3320......
..0uwwwwwv0.....
..0xwv0.0wv0....
...020..........
..020...........
.020............
0no0............
0op0............
.00.............
""")
A16["bident"] = G("""
.........0ts0...
........0sr0....
.......0sr0.....
......0sr0....0t
.....0sr0....0st
....0uw0....0sr0
.....0xw0..0sr0.
......0wv00sr0..
.....032wvsr0...
....03200vu0....
...0320.0000....
..0320..........
.0320...........
0no0............
0op0............
.00.............
""")

# --- balestra 16x16: corpo + strati a mano (corda z, proiettile)
XB_BODY = G("""
...000000000000.
..04443333333rs0
.044433333333ss0
04433000000000s0
0430..........s0
0430............
043003330.......
0430.0uwv0......
0430..0xwv0.....
0430...0vwu0....
0430....03330...
0430.....03330..
0no0......03330.
0op0.......03320
0pn0........0330
.00..........000
""")
XB_S = {
    "crossbow_standby": pix("4,13 5,12 6,11 7,10 10,7 11,6 12,5 13,4"),
    "crossbow_pulling_0": pix("4,13 5,12 6,12 7,11 8,11 9,10 10,9 11,8 11,7 12,6 12,5 13,4"),
    "crossbow_pulling_1": pix("4,13 5,13 6,13 7,12 8,12 9,12 10,11 11,10 12,9 12,8 12,7 12,6 12,5 13,4"),
    "crossbow_pulling_2": pix("4,13 5,13 6,13 7,13 8,13 9,13 10,13 11,13 12,13 12,12 12,11 12,10 12,9 12,8 12,7 12,6 12,5 13,4"),
}
XB_P = {
    "arrow": [("x", "4,4 5,5"), ("z", "6,6 7,7 8,8 9,9 10,10"), ("W", "11,11")],
    "firework": [("p", "5,5"), ("o", "6,6 7,7 9,9"), ("w", "8,8 11,11"), ("n", "10,10")],
    "amethyst": [("p", "5,5"), ("o", "6,6 7,7 8,8"), ("n", "9,9 10,10")],
    "citrine": [("x", "5,5"), ("w", "6,6 7,7 8,8"), ("v", "9,9 10,10")],
    "topaz": [("t", "5,5"), ("s", "6,6 7,7 8,8"), ("r", "9,9 10,10")],
    "onyx": [("z", "5,5"), ("1", "6,6 7,7 9,9"), ("0", "8,8 10,10")],
    "moonstone": [("W", "5,5 7,7 9,9"), ("z", "6,6 8,8 10,10")],
    "glass": [("t", "5,5 8,8"), ("z", "6,6 7,7 9,9 10,10")],
}


def xb_state(strings_key, proj=None):
    layers = [XB_S[strings_key]]
    for ch, spec in (XB_P[proj] if proj else []):
        layers.append(pix(spec, ch))
    return overlay(XB_BODY, *layers)


for k in XB_S:
    A16[k] = xb_state(k, "arrow" if k == "crossbow_pulling_2" else None)
for p in XB_P:
    A16["crossbow_" + p] = xb_state("crossbow_pulling_2", p)

# ===========================================================================
# 32x32 (_oversized): righe scritte a mano nel formato «colonna:testo» (spazio = nuovo segmento, ~ = riga vuota)
# ===========================================================================
def S(text, n=32):
    lines = [l.strip() for l in text.strip().splitlines()]
    while lines and lines[-1] == "~":
        lines.pop()
    assert len(lines) <= n, len(lines)
    lines += ["~"] * (n - len(lines))
    rows = []
    for i, l in enumerate(lines):
        row = ["."] * n
        if l != "~":
            for seg in l.split():
                c, t = seg.split(":")
                c = int(c)
                assert c + len(t) <= n, (i, seg)
                for k, ch in enumerate(t):
                    if ch != ".":
                        row[c + k] = ch
        rows.append("".join(row))
    return rows


A32 = {}
A32["workstaff"] = S("""
~
19:00000000
17:044444433330
16:044qqq11111320
15:044qtssq11111320
15:043qssrq111uuu20
15:043qsrrq11uxwwu0
15:0433qqq111uwwvu0
15:0433111112uwvvu0
15:04331111223uuu20
15:04331mmm11122220
15:0433mpoom1122220
15:0433moonm1122220
16:043monnm122220
17:033mmm222220
14:04320 19:00000000
13:04320
12:04320
11:04320
10:04320
9:04320
8:04320
7:04320
6:04320
5:04320
4:04320
3:04320
2:04320
1:04320
0:04320
0:4320
0:320
""")
BLADE = S("""
30:t0
26:0tt0
23:0tsr30
20:0tsr3320
17:0tssr43220
15:0tssr433220
14:0tssr433220
13:0tssr433220
12:0tssr433220
11:0tssr433220
10:0tssr433220
9:0tssr433220
8:0tssr433220
10:ssr433220
11:433220
""")
GRIP = S("""
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
9:04320
8:04320
7:04320
6:04320
5:04320
4:04320
3:04320
3:04320
2:04320
2:04320
1:04320
""")
BAR = S("""
~
~
~
~
~
~
~
~
~
~
7:uxu
7:uxwwu
8:uxwvu
9:uwwvu
10:uwwvu
11:uwvvu
12:uwvvu
13:uvvvu
14:uvvvu
16:uvu
""")
POMMEL = S("""
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
1:mmm
0:mpoom
0:moonm
0:monnm
1:mmm
""")
A32["ultra_greatsword"] = overlay(BLADE, GRIP, BAR, POMMEL)

# --- bident 32: fusto, due rebbi topazio, traversa citrino, pomo ametista
BI_SHAFT = S("""
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
14:04320
13:04320
12:04320
11:04320
10:04320
9:04320
8:04320
7:04320
6:04320
5:04320
4:04320
~
~
~
~
~
""")
BI_PRONGS = S("""
21:qtq
19:qtsrq
18:qtsrq
17:qtsrq
16:qtsrq
15:qtsrq
14:qtsrq
13:qtsrq
12:qtsrq
11:qtsrq
29:qtq
27:qtsrq
26:qtsrq
25:qtsrq
24:qtsrq
23:qtsrq
22:qtsrq
21:qtsrq
20:qtsrq
""")
BI_BAR = S("""
~
~
~
~
~
~
~
~
~
~
10:uxwvu
11:uxwwu
12:uxwvu
13:uwwvu
14:uwwvu
15:uwvvu
16:uwvvu
17:uvvvu
18:uvvvu
19:uvvvu
""")
BI_POMMEL = S("""
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
~
2:mmm
1:mpoom
1:moonm
1:monnm
2:mmm
""")
A32["bident"] = overlay(BI_SHAFT, BI_PRONGS, BI_BAR, BI_POMMEL)

# --- balestra 32: corpo 20x20 disegnato a mano, incollato in basso a destra (come nelle _oversized attuali: riquadro 12..31)
XB32_BODY = G("""
...0000000000000qqqq
..04444333333333qtsq
.044443333333333qssq
0444333333333333qsrq
0443000000000000qrrq
0443............qrq.
0443.00000..........
04430433330.........
0443.0433330........
0443..0uxwwu0.......
0443...0uwwvu0......
0443....0uvvvu0.....
0443.....0433330....
0443......0333320...
0443.......0333320..
mpom........0333220.
moom.........0332220
monm..........032222
mnnm...........02222
.mm.............0222
""", 20)
XB32_S = {
    "crossbow_standby": pix("5,16 6,15 7,14 8,13 13,8 14,7 15,6 16,5", "z", 20),
    "crossbow_pulling_0": pix("5,16 6,15 7,15 8,14 9,14 10,13 11,13 12,12 13,11 13,10 14,9 14,8 15,7 15,6 16,5", "z", 20),
    "crossbow_pulling_1": pix("5,16 6,16 7,16 8,16 9,15 10,15 11,15 12,15 13,15 14,15 15,15 15,14 15,13 15,12 15,11 15,10 15,9 15,8 15,7 15,6 16,5", "z", 20),
    "crossbow_pulling_2": pix("5,16 6,17 7,17 8,17 9,17 10,17 11,17 12,17 13,17 14,17 15,17 16,17 17,17 17,16 17,15 17,14 17,13 17,12 17,11 17,10 17,9 17,8 17,7 17,6 17,5 16,5", "z", 20),
}
D = "5,5 6,6 7,7 8,8 9,9 10,10 11,11 12,12 13,13"
R = "6,7 7,8 8,9 9,10 10,11 11,12 12,13 14,14"
XB32_P = {
    "arrow": [("x", "5,5 6,6"), ("z", "7,7 8,8 9,9 10,10 11,11 12,12 13,13 15,15"), ("W", "14,14 16,16")],
    "firework": [("p", "5,5"), ("o", "6,6 7,7 9,9 11,11 12,12"), ("w", "8,8 10,10 13,13 15,15"), ("n", "14,14"), ("x", "16,16")],
    "amethyst": [("p", "5,5"), ("o", D), ("n", R)],
    "citrine": [("x", "5,5"), ("w", D), ("v", R)],
    "topaz": [("t", "5,5"), ("s", D), ("r", R)],
    "onyx": [("z", "5,5"), ("4", D), ("1", R)],
    "moonstone": [("p", "5,5"), ("W", D), ("z", R)],
    "glass": [("W", "5,5"), ("t", D), ("z", R)],
}


def xb32_state(strings_key, proj=None):
    layers = [XB32_S[strings_key]]
    for ch, spec in (XB32_P[proj] if proj else []):
        layers.append(pix(spec, ch, 20))
    body = overlay(XB32_BODY, *layers)
    full = [["."] * 32 for _ in range(32)]
    for y, r in enumerate(body):
        for x, ch in enumerate(r):
            full[12 + y][12 + x] = ch
    return ["".join(r) for r in full]


for k in XB32_S:
    A32[k] = xb32_state(k, "arrow" if k == "crossbow_pulling_2" else None)
for p_ in XB32_P:
    A32["crossbow_" + p_] = xb32_state("crossbow_pulling_2", p_)


# ===========================================================================
# Uscite, verifica, tavola, note
# ===========================================================================
NAMES16 = ["sword", "axe", "shovel", "hoe", "shears", "helmet", "chestplate", "leggings", "boots",
           "fishing_rod", "fishing_rod_cast", "bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2",
           "workstaff", "ultra_greatsword", "bident",
           "crossbow_standby", "crossbow_pulling_0", "crossbow_pulling_1", "crossbow_pulling_2",
           "crossbow_arrow", "crossbow_firework", "crossbow_amethyst", "crossbow_citrine", "crossbow_topaz",
           "crossbow_onyx", "crossbow_moonstone", "crossbow_glass"]
NAMES32 = ["workstaff", "ultra_greatsword", "bident", "crossbow_standby", "crossbow_pulling_0",
           "crossbow_pulling_1", "crossbow_pulling_2", "crossbow_arrow", "crossbow_firework", "crossbow_amethyst",
           "crossbow_citrine", "crossbow_topaz", "crossbow_onyx", "crossbow_moonstone", "crossbow_glass"]
FILES = [(f"trichromatic_{n}.png", A16[n], 16) for n in NAMES16] + \
        [(f"trichromatic_{n}_oversized.png", A32[n], 32) for n in NAMES32]


def current_frame0(fname, size):
    im = Image.open(os.path.join(CUR, fname)).convert("RGBA")
    return im.crop((0, 0, size, size))


def write_set():
    os.makedirs(SET, exist_ok=True)
    for fname, rows, n in FILES:
        to_img(rows).save(os.path.join(SET, fname))


def verify():
    ok = True
    missing = [f for f, _, _ in FILES if not os.path.exists(os.path.join(SET, f))]
    print(f"file attesi: {len(FILES)}  presenti: {len(FILES) - len(missing)}  mancanti: {missing}")
    ok &= not missing
    existing = sorted(f for f in os.listdir(CUR) if f.startswith("trichromatic_") and f.endswith(".png"))
    print("stessi nomi dei file attuali:", existing == sorted(f for f, _, _ in FILES))
    ok &= existing == sorted(f for f, _, _ in FILES)
    for fname, rows, n in FILES:
        im = Image.open(os.path.join(SET, fname))
        errs = []
        if im.size != (n, n):
            errs.append(f"dimensioni {im.size}")
        if im.mode != "RGBA":
            errs.append(f"modo {im.mode}")
        alphas = set(im.getchannel("A").getdata())
        if not alphas <= {0, 255}:
            errs.append(f"alfa {sorted(alphas)}")
        cur = current_frame0(fname, n)
        if list(cur.getdata()) == list(im.convert("RGBA").getdata()):
            errs.append("identico all'attuale")
        if errs:
            ok = False
            print("ERRORE", fname, errs)
    print("verifica:", "OK" if ok else "FALLITA")
    return ok


def font(sz):
    for p in ("/usr/share/fonts/TTF/DejaVuSans.ttf", "/usr/share/fonts/dejavu/DejaVuSans.ttf"):
        if os.path.exists(p):
            return ImageFont.truetype(p, sz)
    return ImageFont.load_default()


def tavola():
    bg = (34, 34, 38, 255)
    f = font(13)
    sc = 8
    pad = 14
    items = []  # (label, left, right, size)
    for fname, rows, n in FILES:
        items.append((fname[len("trichromatic_"):-4], current_frame0(fname, n), to_img(rows), n))
    s16 = [i for i in items if i[3] == 16]
    s32 = [i for i in items if i[3] == 32]
    pw16 = 2 * 16 * sc + 3 * pad
    ph16 = 16 * sc + 2 * pad + 18
    pw32 = 2 * 32 * sc + 3 * pad
    ph32 = 32 * sc + 2 * pad + 18
    cols16, cols32 = 4, 2
    W = max(cols16 * pw16, cols32 * pw32)
    rows16 = (len(s16) + cols16 - 1) // cols16
    rows32 = (len(s32) + cols32 - 1) // cols32
    # confronti EC: 4 coppie (ours | EC)
    ec = [("sword  /  EC bedrock_sword", A16["sword"], "bedrock_sword.png", 16),
          ("chestplate  /  EC bedrock_chestplate", A16["chestplate"], "bedrock_chestplate.png", 16),
          ("workstaff  /  EC glass_crest_workstaff", A16["workstaff"], "glass_crest_workstaff.png", 16),
          ("crossbow_amethyst oversized  /  EC glass_crest", A32["crossbow_amethyst"], "glass_crest_crossbow_amethyst_oversized.png", 32)]
    H = rows16 * ph16 + rows32 * ph32 + 40 + 2 * (16 * sc + 2 * pad + 18) + 32 * sc + 2 * pad + 18
    sheet = Image.new("RGBA", (W, H), bg)
    d = ImageDraw.Draw(sheet)

    def put(im, x, y, size):
        big = im.resize((size * sc, size * sc), Image.NEAREST)
        sheet.paste(big, (x, y), big)

    y = 0
    for k, (label, cur, new, n) in enumerate(s16):
        x = (k % cols16) * pw16
        yy = y + (k // cols16) * ph16
        d.text((x + pad, yy + 4), label, fill=(200, 200, 210, 255), font=f)
        put(cur, x + pad, yy + 18 + pad // 2, 16)
        put(new, x + 2 * pad + 16 * sc, yy + 18 + pad // 2, 16)
    y += rows16 * ph16
    for k, (label, cur, new, n) in enumerate(s32):
        x = (k % cols32) * pw32
        yy = y + (k // cols32) * ph32
        d.text((x + pad, yy + 4), label + " (oversized)", fill=(200, 200, 210, 255), font=f)
        put(cur, x + pad, yy + 18 + pad // 2, 32)
        put(new, x + 2 * pad + 32 * sc, yy + 18 + pad // 2, 32)
    y += rows32 * ph32 + 40
    d.text((pad, y - 28), "Confronti con Eternal Classics (nostro a sinistra, EC a destra)", fill=(255, 255, 255, 255), font=font(16))
    # le prime tre coppie in riga da 16x16 (2 per riga), l'ultima 32x32
    pos = 0
    for label, ours, ecname, n in ec[:3]:
        x = (pos % 2) * (W // 2)
        yy = y + (pos // 2) * (16 * sc + 2 * pad + 18)
        d.text((x + pad, yy + 4), label, fill=(200, 200, 210, 255), font=f)
        put(to_img(ours), x + pad, yy + 18 + pad // 2, 16)
        ecim = Image.open(os.path.join(EC, ecname)).convert("RGBA")
        ecim = ecim.crop((0, 0, ecim.width, ecim.width))
        put(ecim, x + 2 * pad + 16 * sc, yy + 18 + pad // 2, 16)
        pos += 1
    yy = y + 2 * (16 * sc + 2 * pad + 18)
    label, ours, ecname, n = ec[3]
    d.text((pad, yy + 4), label, fill=(200, 200, 210, 255), font=f)
    put(to_img(ours), pad, yy + 18 + pad // 2, 32)
    ecim = Image.open(os.path.join(EC, ecname)).convert("RGBA")
    put(ecim.crop((0, 0, 32, 32)), 2 * pad + 32 * sc, yy + 18 + pad // 2, 32)
    sheet.convert("RGB").save(os.path.join(OUT, f"tavola_{VAR}.png"))


def main():
    os.makedirs(OUT, exist_ok=True)
    write_set()
    ok = verify()
    tavola()
    print("tavola:", os.path.join(OUT, f"tavola_{VAR}.png"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
