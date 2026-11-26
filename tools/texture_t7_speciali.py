"""T7 «Umbral»: texture degli oggetti speciali (workstaff, ultra greatsword, balestra in 12 stati, bident),
ciascuno 16x16 (inventario) e 32x32 «oversized» (in mano), tutte animate (12 fotogrammi x 5 tick, interpolate false).

Disegno a mano: ogni sprite è una griglia ASCII scritta carattere per carattere; i caratteri sono quelli della
palette T7 di tools/texture_t7.py (metallo scuro, vibranium a-f, fasce g-k, onice q-x) più quelli delle munizioni.
La balestra ha UNA base scarica disegnata a mano (16x16 e 20x20, quest'ultima messa nella zona 12-31 del 32x32) e,
per ogni stato, una sovrapposizione scritta a mano con solo corda e munizione: la luce animata si calcola sulla base,
quindi è identica in tutti gli stati.
Animazione: per ogni base, una mappa scritta a mano pixel -> fotogramma in cui passa la cresta di luce; il pixel sale
lungo la sua rampa (texture_hand.RAMPS) di texture_hand.WAVE gradini. La luce parte dal pomolo (o dal grilletto) e
risale fino alle punte.

Uso: python3 tools/texture_t7_speciali.py   (scrive SOLO in design/texture/proposte/t7/speciali/; la pagina la compone
     tools/texture_t7.py, che importa questo modulo)
"""
import base64
import io
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_t7 as T  # noqa: E402

OUT = os.path.join(T.OUT, "speciali")
REF = H.REF


def G(text):
    rows = text.strip("\n").splitlines()
    assert len({len(r) for r in rows}) == 1 and len(rows) == len(rows[0]), (len(rows), {len(r) for r in rows})
    return rows


# ---------------------------------------------------------------------------
# Colori in più (munizioni). Le punte delle frecce gemma usano G (media), H (luce, un solo pixel), I (ombra).
# ---------------------------------------------------------------------------
AMMO_BASE = {"A": "#8a6a3f", "B": "#5a3f22", "F": "#a8a8a8", "K": "#5c5c5c", "E": "#eeeeee",
             "R": "#b3262a", "D": "#6a1414", "P": "#e8d8b0", "X": "#fff6a0", "Z": "#ffb040"}
GEMS = {
    # Giro punte: ogni punta e 2x2 (3x3 nella 32) nel colore della gemma, con 1 pixel di luce (H); le alette (G) sono
    # dello stesso colore. L = bordo argento dell'onice, T = pixel quasi trasparente del vetro (RGBA, vedi palette).
    "arrow": {"G": "#8a8f98", "H": "#d0d3d9", "I": "#55585f"},
    "topaz": {"G": "#20a9d2", "H": "#94f0ff", "I": "#0f5f7e"},
    "amethyst": {"G": "#c1329b", "H": "#ff8de4", "I": "#6c1460"},
    "citrine": {"G": "#e6d62c", "H": "#fffba8", "I": "#8a760c"},
    "onyx": {"G": "#0a0a10", "H": "#b8c4e0", "I": "#05050a", "L": "#9aa6c6"},
    "moonstone": {"G": "#d9c8f2", "H": "#fbf8ff", "I": "#b39ad6"},
    "glass": {"G": "#a6e2f5", "H": "#f2fcff", "I": "#7fb4cc", "T": (191, 230, 245, 70)},
}

# ===========================================================================
# GRIGLIE (scritte a mano)
# ===========================================================================
# --- Giro rilettura: ultra spadone con il metodo del glass crest. Stesse sagome dei riferimenti (lama larga, guardia a
# croce in diagonale, impugnatura verso il basso a sinistra); materiali nostri: bordi di metallo scuro, canale di vibranium
# lungo la lama, ali della guardia in vibranium, gemma d'onice al centro, fasce di vibranium sull'impugnatura.
_V16 = ["064cd30", "064ce30", "064cd30", "054cd30"]
_V32 = ["06544cd3320", "06544cd3320", "06544ce3320", "05544cd3320", "06544cd3320", "06554cd3320", "06544ce3320", "05544cd3320"]


def _build(n, lo, v, lama_da, lama_a, extra):
    rows = [["."] * n for _ in range(n)]
    for y in range(lama_da, lama_a + 1):
        pat = v[y % len(v)]
        L = lo - y
        for i, ch in enumerate(pat):
            if 0 <= L + i < n:
                rows[y][L + i] = ch
    for y, segs in extra.items():
        for c, t in segs:
            for i, ch in enumerate(t):
                rows[y][c + i] = ch
    return ["".join(r) for r in rows]


GS16 = _build(16, 12, _V16, 0, 10, {
    7: [(1, "ab")], 8: [(1, "adb")], 9: [(2, "3")],
    11: [(3, "0qtr0")], 12: [(2, "0cdec0")],
    13: [(1, "0j0"), (5, "0cdb")], 14: [(0, "0i0"), (7, "ab")], 15: [(0, "tq")]})

GS32 = _build(32, 26, _V32, 0, 22, {
    17: [(3, "ab")], 18: [(3, "adb")], 19: [(3, "0dc3")], 20: [(4, "03")], 21: [(4, "3")],
    23: [(5, "0cdeedc10")], 24: [(5, "0aqtrqa0")], 25: [(5, "0aqrsqb0")], 26: [(4, "03cdeedc30")],
    27: [(3, "0j0"), (9, "3cddb0")], 28: [(2, "0i0"), (12, "0ba")],
    29: [(0, "0tq0")], 30: [(0, "qr0")], 31: [(0, "0q0")]})

WS16 = G("""
....0000000..000
....0455540.03d0
.....bcdec102c40
......15653bd10.
......035qq3c0..
.....024qtsq43b0
.....013qsrq32c0
.......13qq522d0
......0312b432d0
.....3c0.23523c0
....3j0..12..2b0
...3b0........b0
..3c0...........
.qs0............
qrq.............
rq..............
""")

BI16 = G("""
........a.......
.......aea......
....aaadca..a.a.
..adccdca..aeada
..aeacba...adcba
...acba...acba..
..abbqtq.adcba..
..aaasrsacba....
.....qqqdcba....
....40acba......
...kh..aa.......
..jh............
.40.............
tq..............
qr..............
................
""")

WS32 = G("""
................................
..............0000000...........
..............0455530.....0000..
...............0cdec0....b4d30..
.................0c0.....c3e20..
.................035653..d2c10..
.................0245531bcd10...
................0356654410......
...............03556356530......
...............03565.565420.....
...............035qqq5cb420.....
.................qtssq.4530.3c0.
................0qsrrq654202bd0.
................3qsrsq654303ce0.
................04qqq4531202bd0.
...............03420.1c30...2c0.
..............340....1c0....2b0.
.............3c0.............c0.
............340.................
...........hjg..................
..........hkg...................
.........3c0....................
........340.....................
.......340......................
......3c0.......................
.....340........................
....hjg.........................
...3c0..........................
..340...........................
.qs0............................
qrq.............................
rq..............................
""")

BI32 = G("""
................................
.........................a......
........................aea.....
.......................adca.....
......................adcba.....
.....................adcba......
....................adcba.......
...............aaa.adcba........
..............adcccdcba...a.a...
..............adaadcba...aeada..
..............aeaccba....adcba..
...............accba....adccba..
..............acbba....adcba....
.............abqttsq..adcbca....
............abbqtsrq.adcba......
...........abbbqsrrqadccba......
..........abbbaqsrsqdcba........
...........aaa.qqqqqdcba........
..............50.abbba..........
.............40...aaa...........
............40..................
...........d0...................
..........40....................
.........50.....................
........kh......................
.......jh.......................
......kh........................
.....40.........................
....40..........................
...40...........................
.qtq............................
.rq.............................
""")

# ---------------------------------------------------------------------------
# BALESTRA (test 3): stessa sagoma del glass crest di Spectrum, pixel per pixel. Il calcio e lungo e va in diagonale dal
# fulcro (angolo in alto a sinistra) al pomolo d'onice in basso a destra; i flettenti sono corti ganci attorno al fulcro
# (uno verso destra in alto, uno verso il basso a sinistra); la corda e una linea chiara che li unisce.
# Base scarica = i soli pixel presenti in TUTTI gli stati (flettenti, fulcro, calcio con la guida della munizione);
# per ogni stato una sovrapposizione con corda e munizione. La testa della freccia sta nell'angolo, sopra il fulcro.
# ---------------------------------------------------------------------------
CB16 = G("""
................
..........cde...
...5qt.bbccdec..
..45rsrbab..cb..
..45rrs6........
..45rs356.......
...b231456......
..cb02314c6.....
..cb.02314d6....
..cb..02314e....
.cb....02314c...
.cb.....023156..
.dec......03156.
..bc.......0rsq.
............0rtq
..............rq
""")

CB20 = G("""
............bdec....
..54....bccdcedc....
.54.qtsrbbabb.cb....
.4.5rsrrba....ba....
..45rrs36...........
..45rsr356..........
..cb2314556.........
..cb02314c56........
.ccb023314d6........
.ccb.023314de.......
.cb...0233145c......
.cb....02341556.....
dec......0231456....
cb........0231556...
bcdb.......012rsq...
abba........01rstq..
.............0rstsq.
...............0rssq
................rstq
.................rsq
""")


def pts(n, lst, ch, dx=0, dy=0):
    rows = [["."] * n for _ in range(n)]
    for x, y in lst:
        rows[y + dy][x + dx] = ch
    return ["".join(r) for r in rows]


def _line(a, b):
    (x0, y0), (x1, y1) = a, b
    n = max(abs(x1 - x0), abs(y1 - y0))
    return [(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)) for i in range(n + 1)]


# Corda (pixel in 16 e in 20). standby: corda a riposo (diagonale tra le punte); pulling: sempre piu indietro; da pulling_2
# in poi a L, con il vertice sul calcio.
STR16 = {
    "standby": _line((11, 4), (4, 11)),
    "pulling_0": _line((12, 4), (4, 12)),
    "pulling_1": [(12, 4), (12, 5), (11, 6), (11, 7), (10, 8), (10, 9), (10, 10), (9, 10), (8, 10), (7, 11), (6, 11), (5, 12), (4, 12)],
    "pulling_2": [(11, y) for y in range(3, 12)] + [(x, 11) for x in range(3, 11)],
}
STR20 = {
    "standby": _line((13, 3), (3, 13)),
    "pulling_0": [(13, 4), (12, 5), (12, 6), (11, 7), (11, 8), (10, 9), (10, 10), (9, 10), (8, 11), (7, 11), (6, 12), (5, 12), (4, 13)],
    "pulling_1": [(14, 4), (14, 5), (13, 6), (13, 7), (12, 8), (12, 9), (12, 10), (12, 11), (12, 12), (11, 12), (10, 12), (9, 12),
                  (8, 12), (7, 13), (6, 13), (5, 14), (4, 14)],
    "pulling_2": [(14, y) for y in range(4, 15)] + [(x, 14) for x in range(4, 14)],
}
STRCH = "e"

# Razzo (firework). 16: cono rosso nell'angolo in alto a sinistra e tubo di carta con fasce rosse lungo la diagonale.
FIREWORK = G("""
ZRRD............
RRXR............
RDRR............
DRRD............
....P...........
.....P..........
......R.........
.......R........
........P.......
.........PE.....
.........EA.....
...........B....
................
................
................
................
""")

# Razzo 20: griglia 21x21 con origine in (-1,-1) rispetto alla zona 20x20, perche il cono sporge di un pixel sopra e a sinistra.
FIREWORK21 = G("""
ZRRD.................
RRXR.................
RDRRD................
DRRRR................
..DRR................
.....PA..............
......PA.............
.......RR............
........RP...........
.........PP..........
..........RR.........
...........PP........
............AA.......
.............EA......
..............AE.....
...............EB....
.....................
.....................
.....................
.....................
.....................
""")


# Frecce: punta 2x2 (3x3 senza l'angolo nella 32) nell'angolo sopra il fulcro, asta di legno A lungo la diagonale del calcio,
# alette nel colore della punta, cocca B sul vertice della corda.
HEADS = {
    "arrow": (("HG", "GI"), "GG"),
    "glass": (("HG", "GT"), "GG"),
    "topaz": (("HG", "GI"), "GG"),
    "amethyst": (("HG", "GI"), "GG"),
    "citrine": (("HG", "GI"), "GG"),
    "onyx": (("HG", "GI"), "LG"),
    "moonstone": (("HG", "GI"), "GG"),
}
HEADS_BIG = {
    "arrow": ("HGG", "GGG", "GGI"), "glass": ("HGG", "GGG", "GGT"), "topaz": ("HGG", "GGG", "GGI"),
    "amethyst": ("HGG", "GGG", "GGI"), "citrine": ("HGG", "GGG", "GGI"), "onyx": ("HGG", "GGG", "GGI"),
    "moonstone": ("HGG", "GGG", "GGI"),
}


def ammo16(kind):
    (r1, r2), fl = HEADS[kind]
    spec = {2: f"..{r1}", 3: f"..{r2}", 4: "....A", 5: ".....A", 6: "......A", 7: ".......A", 8: "........A",
            9: f".........A{fl[0]}", 10: f".........{fl[1]}A", 11: "...........B"}
    return [spec.get(i, "").ljust(16, ".") for i in range(16)]


def ammo20(kind):
    """Munizione nella zona 20x20 (poi messa nel 32x32): asta A con ombra B a destra, alette, cocca."""
    r = HEADS_BIG[kind]
    fl = HEADS[kind][1]
    g = [["."] * 20 for _ in range(20)]
    g[1][2:4] = list(r[0][:2])
    g[2][1:4] = list(r[1])
    g[3][1:4] = list(r[2])
    for k in range(4, 13):
        g[k][k] = "A"
        g[k][k + 1] = "B"
    g[12][13], g[13][12] = fl[0], fl[1]
    g[13][13], g[13][14] = "A", "B"
    g[14][14] = "B"
    return ["".join(r) for r in g]


def lay(*gs):
    out = [list(r) for r in gs[0]]
    for g in gs[1:]:
        for y, r in enumerate(g):
            for x, c in enumerate(r):
                if c != ".":
                    out[y][x] = c
    return ["".join(r) for r in out]


def big(g, off=12):
    """Una griglia nxn messa nel 32x32 con l'angolo in alto a sinistra in (off, off): la balestra 20x20 sta nella zona
    12-31 come il riferimento glass crest (il razzo 21x21 parte da 11)."""
    n = len(g)
    rows = [["."] * 32 for _ in range(32)]
    for y, r in enumerate(g):
        for x, c in enumerate(r):
            if c != ".":
                rows[off + y][off + x] = c
    return ["".join(r) for r in rows]


STATES = ["standby", "pulling_0", "pulling_1", "pulling_2", "arrow", "firework",
          "glass", "topaz", "amethyst", "citrine", "onyx", "moonstone"]


def crossbow_overlays(state):
    """(sovrapposizioni 16x16, sovrapposizioni 32x32, palette delle munizioni) per lo stato."""
    key = state if state in STR16 else "pulling_2"
    s16 = pts(16, STR16[key], STRCH)
    s32 = big(pts(20, STR20[key], STRCH))
    if state in ("standby", "pulling_0", "pulling_1", "pulling_2"):
        return [s16], [s32], {}
    if state == "arrow":
        return [s16, ammo16("arrow")], [s32, big(ammo20("arrow"))], GEMS["arrow"]
    if state == "firework":
        return [s16, FIREWORK], [s32, big(FIREWORK21, 11)], {}
    return [s16, ammo16(state)], [s32, big(ammo20(state))], GEMS[state]


# ===========================================================================
# FASI dell'animazione (scritte a mano): pixel (x, y) -> fotogramma (0-11) in cui passa la cresta di luce
# ===========================================================================
def ph(*groups):
    out = {}
    for phase, cells in groups:
        for c in cells:
            out[c] = phase
    return out


PHASES = {
    "GS16": None, "GS32": None,   # calcolate sulle griglie (vedi fasi_diagonale)
    # workstaff (giro 5, metodo del T6): la luce parte dal pomolo, sale lungo il manico fino all'onice
    # e da li' si diffonde ai tagli delle tre sezioni (lama alta, lama destra, corpo centrale)
    "WS16": ph((0, [(1, 14), (0, 15)]),
               (1, [(2, 13)]),
               (2, [(3, 12)]),
               (3, [(4, 11)]),
               (4, [(6, 9)]),
               (5, [(9, 6), (10, 6)]),
               (6, [(9, 5), (10, 5)]),
               (7, [(12, 4), (10, 8), (12, 3), (11, 3)]),
               (8, [(9, 2), (8, 2), (13, 2), (14, 5)]),
               (9, [(7, 2), (6, 2), (5, 2), (14, 1), (14, 6), (14, 7)]),
               (10, [(14, 8), (14, 9)]),
               (11, [(14, 10), (14, 11)])),
    "WS32": ph((0, [(0, 31), (1, 30)]),
               (1, [(2, 29)]),
               (2, [(4, 27)]),
               (3, [(7, 24)]),
               (4, [(10, 21)]),
               (5, [(14, 17)]),
               (6, [(18, 12), (19, 13)]),
               (7, [(18, 11), (19, 12), (20, 12)]),
               (8, [(19, 11), (20, 11), (20, 13), (22, 10), (23, 10), (22, 15), (22, 16)]),
               (9, [(16, 3), (17, 3), (18, 3), (19, 3), (18, 4), (24, 6), (25, 6), (26, 6)]),
               (10, [(25, 4), (25, 3), (25, 5), (27, 4), (29, 11), (29, 12), (29, 13)]),
               (11, [(29, 14), (29, 15), (29, 16), (29, 17)])),
    "BI16": ph((0, [(0, 13), (1, 14)]),
               (3, [(3, 6), (4, 5), (4, 6), (5, 5), (5, 7), (6, 6), (6, 7), (7, 7), (7, 9), (8, 8), (8, 9)]),
               (4, [(3, 3), (3, 4), (4, 3), (5, 3), (5, 4), (6, 3), (6, 4), (7, 3), (9, 7), (9, 8), (10, 6), (10, 7), (10, 8)]),
               (5, [(7, 2), (8, 1), (8, 2), (11, 5), (11, 6), (12, 3), (12, 4), (12, 5), (12, 6)]),
               (6, [(13, 4), (14, 3), (14, 4)])),
    "BI32": ph((0, [(1, 31), (2, 30)]),
               (3, [(11, 21)]),
               (4, [(11, 16), (12, 15), (12, 16), (13, 15), (13, 16), (14, 15), (16, 15), (16, 16), (17, 15), (17, 16)]),
               (5, [(13, 14), (14, 13), (14, 14), (15, 12), (16, 12), (16, 13), (16, 14), (17, 12), (17, 13), (17, 14), (18, 13), (18, 14), (18, 15), (18, 16), (18, 18), (19, 18), (20, 16), (20, 17), (20, 18)]),
               (6, [(15, 8), (15, 9), (15, 10), (16, 8), (16, 11), (17, 8), (17, 10), (17, 11), (18, 8), (18, 9), (18, 10), (18, 11), (19, 8), (19, 9), (19, 10), (20, 8), (20, 9), (21, 8), (21, 15), (21, 16), (21, 17), (22, 14), (22, 15), (22, 16), (22, 17), (23, 13), (23, 14), (23, 15), (24, 12), (24, 13), (24, 14), (24, 15)]),
               (7, [(20, 7), (21, 6), (21, 7), (22, 5), (22, 6), (22, 7), (23, 4), (23, 5), (23, 6), (24, 4), (24, 5), (25, 4), (25, 11), (25, 12), (25, 13), (26, 9), (26, 10), (26, 11), (26, 12), (26, 13), (27, 10), (27, 11), (28, 9), (28, 10), (28, 11)]),
               (8, [(24, 3), (25, 2), (25, 3)])),
    "CB16": None, "CB20": None,   # calcolate sulla base (vedi fasi_balestra)
}


def fasi_diagonale(grid):
    """Fasi per lo spadone: la cresta di luce parte dal pomolo (in basso a sinistra) e sale in diagonale fino alla punta;
    solo i pixel di vibranium/gemma (caratteri delle rampe) si accendono."""
    n = len(grid)
    out = {}
    for y, r in enumerate(grid):
        for x, ch in enumerate(r):
            if any(ch in ramp for ramp in H.RAMPS):
                out[(x, y)] = min(10, round((x - y + n - 1) * 10 / (2 * n - 2)))
    return out


def fasi_balestra(grid, lim):
    """Fasi per la balestra: la luce parte dal calcio (in basso a destra), risale lo scorrevole fino al fulcro d'onice
    e da li' corre lungo i due flettenti fino alle punte. Si accendono solo i pixel di vibranium (b-f)."""
    n = len(grid)
    pts = [(x, y) for y, r in enumerate(grid) for x, ch in enumerate(r) if ch in "bcdef"]
    stock = [x + y for x, y in pts if min(x, y) > lim]
    lo, hi = min(stock), max(stock)
    out = {}
    for x, y in pts:
        if min(x, y) > lim:
            out[(x, y)] = round((hi - (x + y)) * 5 / (hi - lo))
        else:
            out[(x, y)] = min(11, 6 + round((max(x, y) - lim) * 5 / (n - 1 - lim)))
    return out


PHASES["CB16"] = fasi_balestra(CB16, 3)
PHASES["CB20"] = fasi_balestra(CB20, 3)
PHASES["GS16"] = fasi_diagonale(GS16)
PHASES["GS32"] = fasi_diagonale(GS32)


def wave(grid, phases):
    frames = []
    for f in range(T.FRAMES):
        rows = [list(r) for r in grid]
        for (x, y), p in phases.items():
            ch = rows[y][x]
            ramp = next((r for r in H.RAMPS if ch in r), None)
            assert ramp, f"luce su '{ch}' in ({x},{y})"
            step = H.WAVE[(f - p) % T.FRAMES]
            rows[y][x] = ramp[min(ramp.index(ch) + step, len(ramp) - 1)]
        frames.append(["".join(r) for r in rows])
    return frames


def palette(extra=None):
    p = dict(T.PAL7)
    p.update({k: H.hexc(v) for k, v in AMMO_BASE.items()})
    if extra:
        p.update({k: (v if isinstance(v, tuple) else H.hexc(v)) for k, v in extra.items()})
    return p


def render(rows, pal):
    im = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, c in enumerate(r):
            if c != ".":
                im.putpixel((x, y), pal[c])
    return im


def items():
    """nome -> (fotogrammi come griglie, palette, riferimento glass crest)."""
    out = {
        "workstaff": (wave(WS16, PHASES["WS16"]), palette(), "glass_crest_workstaff.png"),
        "workstaff_oversized": (wave(WS32, PHASES["WS32"]), palette(), "glass_crest_workstaff_oversized.png"),
        "ultra_greatsword": (wave(GS16, PHASES["GS16"]), palette(), "glass_crest_ultra_greatsword.png"),
        "ultra_greatsword_oversized": (wave(GS32, PHASES["GS32"]), palette(), "glass_crest_ultra_greatsword_oversized.png"),
        "bident": (wave(BI16, PHASES["BI16"]), palette(), "fractal_glass_crest_bident.png"),
        "bident_oversized": (wave(BI32, PHASES["BI32"]), palette(), "fractal_glass_crest_bident_oversized.png"),
    }
    base16 = wave(CB16, PHASES["CB16"])
    base20 = wave(CB20, PHASES["CB20"])
    for st in STATES:
        o16, o20, extra = crossbow_overlays(st)
        out[f"crossbow_{st}"] = ([lay(fr, *o16) for fr in base16], palette(extra), f"glass_crest_crossbow_{st}.png")
        out[f"crossbow_{st}_oversized"] = ([lay(big(fr), *o20) for fr in base20], palette(extra),
                                           f"glass_crest_crossbow_{st}_oversized.png")
    return out


ORDER = ["ultra_greatsword", "workstaff", "bident"] + [f"crossbow_{s}" for s in STATES]
NOMI = {"ultra_greatsword": "ultra greatsword", "workstaff": "workstaff", "bident": "bident",
        **{f"crossbow_{s}": "balestra, " + s.replace("_", " ") for s in STATES}}


def build():
    """Scrive PNG fermi, strisce animate e .mcmeta; ritorna {nome: [fotogrammi PIL]}."""
    os.makedirs(OUT, exist_ok=True)
    res = {}
    for name, (frames, pal, _ref) in items().items():
        ims = [render(fr, pal) for fr in frames]
        ims[0].save(os.path.join(OUT, f"t7_{name}.png"))
        T.save_anim(ims, os.path.join(OUT, f"t7_{name}_anim"))
        res[name] = ims
    griglie(os.path.join(OUT, "griglie.png"), res)
    if os.path.isdir(PRIMA) and os.path.isdir(ULTIMO) and os.path.isdir(GIRO3) and os.path.isdir(GIRO4):
        prima_dopo(os.path.join(OUT, "prima_dopo.png"), res)
    confronto_guida(os.path.join(OUT, "confronto_guida.png"), res)
    return res


def ref(fname):
    p = os.path.join(REF, fname)
    return Image.open(p).convert("RGBA") if os.path.exists(p) else None



PRIMA = os.path.join(OUT, "prima")        # giro 1
ULTIMO = os.path.join(OUT, "ultimo")      # giro 2
GIRO3 = os.path.join(OUT, "giro3")        # giro 3
GIRO4 = os.path.join(OUT, "giro4")        # giro 4: la workstaff con la testa ruotata (copia dello stato prima di questo giro)
CAMBIATI = ["workstaff", "workstaff_oversized"]   # pezzi toccati in questo giro
BG = (24, 22, 30, 255)


def _font(size, bold=False):
    from PIL import ImageFont
    for p in ("/usr/share/fonts/TTF/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/TTF/DejaVuSans.ttf",
              "/usr/share/fonts/dejavu/DejaVuSans.ttf"):
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def griglie(path, res):
    """Ogni texture ingrandita con la griglia numerata: colonne 1.. da sinistra, righe 1.. dal basso."""
    from PIL import ImageDraw
    f9 = _font(9)
    cells = []
    for n, (ims) in res.items():
        im = ims[0]
        k = 24 if im.width == 16 else 16
        side = im.width * k
        cell = Image.new("RGBA", (side + 40, side + 44), BG)
        d = ImageDraw.Draw(cell)
        cell.alpha_composite(H.on_bg(H.up(im, k)), (28, 18))
        for i in range(im.width + 1):
            d.line([(28 + i * k, 18), (28 + i * k, 18 + side)], fill=(70, 66, 82, 255))
            d.line([(28, 18 + i * k), (28 + side, 18 + i * k)], fill=(70, 66, 82, 255))
        for i in range(im.width):
            lab = str(i + 1)
            d.text((28 + i * k + (k - d.textlength(lab, font=f9)) / 2, 6), lab, fill=(134, 236, 200), font=f9)
            lab = str(im.width - i)          # le righe si contano dal basso
            d.text((4, 18 + i * k + (k - 10) / 2), lab, fill=(134, 236, 200), font=f9)
        d.text((28, 18 + side + 4), n.replace("crossbow_", "balestra ").replace("_", " "), fill=(236, 231, 241), font=_font(12, True))
        cells.append(cell)
    cols_w = 1700
    x = y = rowh = 0
    pos = []
    for c in cells:
        if x + c.width > cols_w:
            x, y, rowh = 0, y + rowh, 0
        pos.append((x, y))
        x += c.width
        rowh = max(rowh, c.height)
    sheet = Image.new("RGBA", (cols_w, y + rowh), BG)
    for c, p in zip(cells, pos):
        sheet.alpha_composite(c, p)
    sheet.save(path)


def prima_dopo(path, res):
    """Giro 1 / 2 / 3 / 4 / ora, con il glass crest accanto (16x16 a x12, 32x32 a x6). Solo la workstaff cambia in questo giro."""
    from PIL import ImageDraw
    f12, f14 = _font(12), _font(14, True)
    k16, k32 = 12, 6
    refs = {n: v[2] for n, v in items().items()}
    rows = []
    for n in ("workstaff",):
        trio = []
        for suf, k in (("", k16), ("_oversized", k32)):
            g1 = Image.open(os.path.join(PRIMA, f"t7_{n}{suf}.png")).convert("RGBA")
            g2 = Image.open(os.path.join(ULTIMO, f"t7_{n}{suf}.png")).convert("RGBA")
            g3 = Image.open(os.path.join(GIRO3, f"t7_{n}{suf}.png")).convert("RGBA")
            g4 = Image.open(os.path.join(GIRO4, f"t7_{n}{suf}.png")).convert("RGBA")
            trio.append([("giro 1", g1, k), ("giro 2", g2, k), ("giro 3", g3, k), ("giro 4", g4, k), ("ora", res[n + suf][0], k),
                         ("glass crest", ref(refs[n + suf]), k)])
        rows.append((n, trio))
    W = 12 + max(sum(im.width * k + 12 for _l, im, k in t[0]) + sum(im.width * k + 12 for _l, im, k in t[1]) + 24 for _n, t in rows)
    rh = 32 * k32 + 44
    sheet = Image.new("RGBA", (W, 50 + rh * len(rows)), BG)
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), "T7 Umbral, workstaff: giro 1 / 2 / 3 / 4 / ora / glass crest (16x16 a x12, 32x32 a x6)", fill=(134, 236, 200), font=_font(16, True))
    for i, (n, trio) in enumerate(rows):
        y0 = 40 + i * rh
        dr.text((10, y0), NOMI[n], fill=(236, 231, 241), font=f14)
        x = 10
        for group in trio:
            for lab, im, k in group:
                top = y0 + 22 + (32 * k32 - im.height * k) // 2
                sheet.alpha_composite(H.on_bg(H.up(im, k)), (x, top))
                dr.text((x, y0 + 22 + 32 * k32 + 4), lab, fill=(200, 192, 212), font=f12)
                x += im.width * k + 12
            x += 24
    sheet.save(path)


def _numerata(im, k, f9):
    """Un pannello ingrandito xk con la griglia numerata (colonne da sinistra, righe dal basso)."""
    from PIL import ImageDraw
    side = im.width * k
    cell = Image.new("RGBA", (side + 30, side + 22), BG)
    d = ImageDraw.Draw(cell)
    cell.paste((139, 139, 139, 255), (26, 18, 26 + side, 18 + side))      # fondo grigio medio: si vedono anche i contorni scuri
    cell.alpha_composite(H.up(im, k), (26, 18))
    for i in range(im.width + 1):
        d.line([(26 + i * k, 18), (26 + i * k, 18 + side)], fill=(112, 112, 112, 255))
        d.line([(26, 18 + i * k), (26 + side, 18 + i * k)], fill=(112, 112, 112, 255))
    for i in range(im.width):
        lab = str(i + 1)
        d.text((26 + i * k + (k - d.textlength(lab, font=f9)) / 2, 5), lab, fill=(134, 236, 200), font=f9)
        lab = str(im.width - i)
        d.text((2, 18 + i * k + (k - 10) / 2), lab, fill=(134, 236, 200), font=f9)
    return cell


def _sovrapposizione(a, b):
    """Sagome a confronto: grigio = in entrambe, rosso = solo nel glass crest, azzurro = solo nel nuovo pezzo."""
    out = Image.new("RGBA", a.size, (0, 0, 0, 0))
    for y in range(a.height):
        for x in range(a.width):
            pa, pb = a.getpixel((x, y))[3] > 0, b.getpixel((x, y))[3] > 0
            if pa or pb:
                out.putpixel((x, y), (150, 146, 160, 255) if pa and pb else (220, 50, 50, 255) if pa else (60, 150, 255, 255))
    return out


def confronto_guida(path, res, nomi=CAMBIATI):
    """Per ogni pezzo cambiato: glass crest, nuovo pezzo e sovrapposizione delle sagome, tutto a x14 con la griglia numerata."""
    from PIL import ImageDraw
    f9, f12, f14 = _font(9), _font(12), _font(14, True)
    k = 14
    refs = {n: v[2] for n, v in items().items()}
    righe = []
    for n in nomi:
        r, new = ref(refs[n]), res[n][0]
        trio = [("glass crest", r), ("ora", new), ("sagome: grigio entrambe, rosso solo guida, azzurro solo nuovo", _sovrapposizione(r, new))]
        righe.append((n.replace("_", " ") + ": stessa sagoma e stesse divisioni del glass crest, materiali Umbral",
                      [_numerata(im, k, f9) for _l, im in trio], [l for l, _im in trio]))
    W = max(sum(c.width + 16 for c in cells) for _t, cells, _l in righe) + 20
    Ht = sum(cells[0].height + 54 for _t, cells, _l in righe) + 44
    sheet = Image.new("RGBA", (W, Ht), BG)
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), "T7 Umbral: confronto con la guida glass crest (x14, colonne da sinistra, righe dal basso)", fill=(134, 236, 200), font=_font(16, True))
    y = 40
    for titolo, cells, labs in righe:
        dr.text((10, y), titolo, fill=(236, 231, 241), font=f14)
        x = 10
        for c, l in zip(cells, labs):
            sheet.alpha_composite(c, (x, y + 20))
            dr.text((x + 26, y + 20 + c.height + 2), l, fill=(200, 192, 212), font=f12)
            x += c.width + 16
        y += cells[0].height + 54
    sheet.save(path)


def section_html(anims, img):
    """Sezione «Oggetti speciali» per stili.html (img: funzione immagine -> <img>)."""
    up, on_bg = H.up, H.on_bg
    refs = {n: v[2] for n, v in items().items()}

    def slot(im, alt, size):
        return f'<div class="slot" style="width:{size + 12}px;height:{size + 12}px;max-width:100%">{img(on_bg(up(im, size // im.width)), alt)}</div>'

    rows = []
    for n in ["ultra_greatsword", "workstaff", "bident"]:
        s16, s32 = anims[n][0], anims[n + "_oversized"][0]
        r16, r32 = ref(refs[n]), ref(refs[n + "_oversized"])
        gif16 = T.b64(T.gif_bytes(anims[n], 8), "gif")
        gif32 = T.b64(T.gif_bytes(anims[n + "_oversized"], 4), "gif")
        rows.append(f"""
    <div class="vrow"><div class="who"><span>{NOMI[n].capitalize()}</span><small>{SHAPES[n]}</small></div>
      <div class="lower">
        <div class="block"><h3>Glass crest (Spectrum)</h3><div class="real">{slot(r16, "riferimento 16", 128)}{slot(r32, "riferimento 32", 128)}</div></div>
        <div class="block"><h3>T7, 16×16 e oversized 32×32</h3><div class="real">{slot(s16, NOMI[n] + " 16", 128)}{slot(s32, NOMI[n] + " 32", 128)}</div></div>
        <div class="block"><h3>1× e 2×</h3><div class="real"><div class="slot s1">{img(s16, "1x")}</div><div class="slot s2">{img(s16, "2x")}</div>
          <div class="slot" style="width:38px;height:38px">{img(s32, "32 1x")}</div><div class="slot" style="width:70px;height:70px">{img(up(s32, 2), "32 2x")}</div></div></div>
        <div class="block gif"><h3>Animazione</h3><div class="real"><div class="slot" style="width:140px;height:140px"><img src="{gif16}" alt="{NOMI[n]} animato" style="width:128px"></div>
          <div class="slot" style="width:140px;height:140px"><img src="{gif32}" alt="{NOMI[n]} oversized animato" style="width:128px"></div></div></div>
      </div></div>""")
    pd_path = os.path.join(OUT, "prima_dopo.png")
    cg_path = os.path.join(OUT, "confronto_guida.png")
    pd_block = ""
    if os.path.exists(cg_path):
        pd_block += (f'<div class="vrow"><div class="who"><span>Confronto con la guida (giro 5)</span><small>Workstaff 16×16 e oversized 32×32 rifatte con il metodo del T6: stessi pixel occupati del glass crest e stesse divisioni (lama alta a sinistra, lama a destra, corpo centrale col mozzo, manico lungo). Dentro, i materiali Umbral: metallo scuro con ombre a grappoli, filo di vibranium sui tagli, onice con contorno freddo al posto della gemma, fasce e pomolo d\'onice. Accanto la sovrapposizione delle sagome.</small></div>'
                     f'<div class="lower"><div class="block"><h3>Glass crest / ora / sagome (×14, colonne da sinistra, righe dal basso)</h3><img src="{T.b64(Image.open(cg_path).convert("RGBA"))}" alt="confronto con la guida" style="max-width:100%;image-rendering:pixelated"></div></div></div>')
    if os.path.exists(pd_path):
        pd_block += (f'<div class="vrow"><div class="who"><span>Workstaff: giro 1 / giro 2 / giro 3 / ora</span><small>Bident, spadone e balestra non cambiano. A destra il glass crest.</small></div>'
                     f'<div class="lower"><div class="block"><h3>Giro 1 / 2 / 3 / 4 / ora / glass crest</h3><img src="{T.b64(Image.open(pd_path).convert("RGBA"))}" alt="prima e dopo" style="max-width:100%;image-rendering:pixelated"></div></div></div>')
    cb16 = "".join(f'<figure>{slot(anims["crossbow_" + s][0], s, 96)}<figcaption>{s.replace("_", " ")}</figcaption></figure>' for s in STATES)
    cb32 = "".join(f'<figure>{slot(anims["crossbow_" + s + "_oversized"][0], s, 96)}<figcaption>{s.replace("_", " ")}</figcaption></figure>' for s in STATES)
    cbref = "".join(f'<figure>{slot(ref(f"glass_crest_crossbow_{s}.png"), s, 96)}<figcaption>{s.replace("_", " ")}</figcaption></figure>' for s in STATES)
    cb1 = "".join(f'<div class="slot s1">{img(anims["crossbow_" + s][0], s)}</div>' for s in STATES)
    cbgif = "".join(f'<figure><div class="slot" style="width:108px;height:108px"><img src="{T.b64(T.gif_bytes(anims["crossbow_" + s], 6), "gif")}" alt="{s} animata" style="width:96px"></div><figcaption>{s.replace("_", " ")}</figcaption></figure>' for s in ("standby", "pulling_2", "topaz", "firework"))
    return f"""
<section class="style best" id="speciali">
  <div class="head"><div class="num">★</div><h2>Oggetti speciali</h2>
  <p>Workstaff, ultra greatsword, balestra e bident nello stile Umbral: 16×16 per l'inventario e 32×32 «oversized» in mano,
  accanto al riferimento glass crest di Spectrum. Tutte animate (12 fotogrammi da 5 tick): la luce verde acqua parte dal pomolo o dal grilletto e risale fino alle punte.</p></div>
  {pd_block}
  {"".join(rows)}
  <div class="vrow"><div class="who"><span>Balestra, 12 stati</span><small>{SHAPES["crossbow"]}</small></div>
    <div class="lower" style="display:grid;gap:14px">
      <div class="block"><h3>Glass crest (Spectrum), 16×16</h3><div class="items wide">{cbref}</div></div>
      <div class="block"><h3>T7, 16×16</h3><div class="items wide">{cb16}</div></div>
      <div class="block"><h3>T7, oversized 32×32</h3><div class="items wide">{cb32}</div></div>
      <div class="block"><h3>1×</h3><div class="real">{cb1}</div></div>
      <div class="block gif"><h3>Animazione (uguale in tutti gli stati)</h3><div class="items wide">{cbgif}</div></div>
    </div></div>
</section>"""


SHAPES = {
    "ultra_greatsword": "Lama larga di metallo scuro con un canale centrale di luce verde acqua a tratti; guardia a fulcro: un anello di vibranium "
                        "con l'occhio-gemma d'onice al centro, un rebbio lungo e ricurvo da un lato e uno corto dall'altro. Impugnatura fasciata, pomolo d'onice.",
    "workstaff": "Sagoma e divisioni del glass crest, pixel per pixel: lama alta a sinistra, lama a destra (calotta e lama lunga), corpo centrale col mozzo; metallo scuro con ombre a grappoli, "
                 "filo di vibranium sui tagli, onice con contorno freddo al posto della gemma. Manico lungo con fasce e fratture verdi, pomolo d'onice.",
    "bident": "Fonde i due bident di Spectrum: asta diagonale con due rebbi paralleli ben separati. Il rebbio in alto a sinistra è uncinato (Ferocious, con un uncino laterale), "
              "quello in basso a destra è a scalini e finisce biforcuto (Fractal); l'onice sta nel punto da cui partono i due rebbi. Rebbi in vibranium con le punte accese.",
    "crossbow": "Arco simmetrico rispetto alla diagonale (i due flettenti uno specchio dell'altro), fulcro d'onice, calcio di metallo con anello di "
                "vibranium; asimmetrie nei dettagli: scheggia d'onice in punti diversi sui due flettenti, fratture diverse, grilletto da un solo lato. "
                "La base scarica è la stessa in tutti gli stati: cambiano solo corda, tensione e munizione. Frecce gemma con la punta del colore della "
                "gemma e un pixel di luce; freccia normale; razzo rosso e carta con scintille.",
}


if __name__ == "__main__":
    r = build()
    print(len(r), "texture in", OUT)
