"""T6 Trichromatic «Eternal Classics», variante A «Brace scura»: corpo quasi nero con sottotono caldo (come le ombre
della bedrock EC), contorno nero pieno, finiture in rame/arancio bruciato con luce dura di 1 px, tre gemme (ametista
magenta, citrino giallo, topazio ciano) riunite in un castone per oggetto, ognuna con un pixel di luce quasi bianco.

Tutto disegnato A MANO: ogni sprite e' una griglia di caratteri scritta riga per riga (16x16 e 32x32). Il codice
converte le griglie in PNG, compone la tavola di confronto e verifica i file; non disegna forme, ombre o luci.
Nelle griglie 32x32 una riga puo' essere scritta come «x:testo x:testo» (colonna di partenza e pixel a mano) e la coda
di punti a destra puo' essere omessa; gli stati di arco e balestra riusano il corpo e cambiano a mano solo corda e
proiettile (sovrapposizioni scritte pixel per pixel).

Uso:  python3 tools/texture_t6ec_a_brace.py     # scrive SOLO in design/texture/proposte/t6-ec/a_brace/

Lettere della palette:
  .  trasparente
  K  contorno nero-bruno          1 2 3  corpo (scuro, medio, bordo di luce)
  a b c  rame bruciato (ombra, medio, luce dura)
  n o p  ametista (ombra, magenta, luce)    u v w x  citrino (ombra, giallo, luce)
  q r s t  topazio (ombra, ciano, luce)     W  scintilla quasi bianca
  e  corda/penna grigio-bruno   l L  pietra di luna   g  vetro   y  onice (luce)
"""
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "design", "texture", "proposte", "t6-ec", "a_brace")
SET = os.path.join(OUT, "set")
CUR = os.path.join(ROOT, "src", "main", "resources", "assets", "spectral_ascension", "textures", "item")
EC = os.path.join(ROOT, "design", "per_brz", "2_spectrum_eternal_classics", "assets", "spectrum", "textures", "item")

PAL = {
    ".": None,
    "K": "#0b0708", "1": "#150e10", "2": "#231819", "3": "#3f2e2c",
    "a": "#5e220e", "b": "#b8501a", "c": "#f4953e",
    "n": "#7a1070", "o": "#e234c4", "p": "#ffc8f2",
    "u": "#3a2a05", "v": "#b08408", "w": "#ffd814", "x": "#fff6b8",
    "q": "#052a3a", "r": "#0a7a9c", "s": "#18d4f2", "t": "#d4ffff",
    "W": "#fff4f8",
    "e": "#8a8298", "l": "#c4bcec", "L": "#f6f2ff", "g": "#8cd4e4", "y": "#6a6478",
}


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


# ---------------------------------------------------------------------------------------------------------------
# Utilita' di lettura delle griglie scritte a mano
# ---------------------------------------------------------------------------------------------------------------
def G(text, n=16):
    """Griglia nxn scritta a mano, una riga per riga; la coda di punti a destra si puo' omettere."""
    rows = [r.strip() for r in text.split("\n")[1:-1]]
    out = []
    for i, r in enumerate(rows):
        if ":" in r:
            row = ["."] * n
            for tok in r.split():
                x, t = tok.split(":")
                for k, ch in enumerate(t):
                    if ch != ".":
                        assert int(x) + k < n, (i, r)
                        row[int(x) + k] = ch
            r = "".join(row)
        out.append(r.ljust(n, ".") if n != 16 else r)
    assert len(out) == n, ("righe", len(out), n)
    for i, r in enumerate(out):
        assert len(r) == n, ("riga", i, len(r), r)
    return out


def OV(base, rows):
    """Sovrappone a un corpo (a mano) un insieme di pixel: '.' lascia il corpo, '_' cancella, altro lettera."""
    n = len(base)
    g = [list(r) for r in base]
    for y, spec in rows.items():
        for tok in spec.split():
            x, t = tok.split(":")
            for k, ch in enumerate(t):
                if ch == ".":
                    continue
                g[y][int(x) + k] = "." if ch == "_" else ch
    return ["".join(r) for r in g]


# ---------------------------------------------------------------------------------------------------------------
# 16x16
# ---------------------------------------------------------------------------------------------------------------
SWORD = G("""
..............KK
............KK3K
..........KK32K.
........KK321K..
........K321K...
.......K321K....
......K321K.....
.....K321K......
....K321K.......
.KK.K321K.KK....
.KcbbopwbbbcK...
.KbbbstxbbbbK...
..KKKKKKKKKK....
.....K12K.......
....K12K........
...KbcK.........
""")

AXE = G("""
................
......KKKKK.....
.....K32221KK...
.....K322211bcK.
.....K32ow21bcK.
.....K22sW11bcK.
......K2211bbK..
......KK11KKK...
.......K12K.....
......K12K......
.....K12K.......
....K12K........
...K12K.........
..KbcK..........
..KKK...........
................
""")

SHOVEL = G("""
................
........KKKKK...
.......K322221K.
.......K32ow21K.
.......K22sW11K.
........K2211K..
.........KbcK...
.........K12K...
........K12K....
.......K12K.....
......K12K......
.....K12K.......
....K12K........
...K12K.........
..KbcK..........
..KKK...........
""")

HOE = G("""
................
....KKKKKKKKK...
...K32222221bK..
...K3ow22221bK..
...K2sW1KKKKK...
....K1K..K12K...
....KKK.K12K....
.......K12K.....
......K12K......
.....K12K.......
....K12K........
...K12K.........
..K12K..........
.KbcK...........
.KKK............
................
""")

SHEARS = G("""
................
...........KKK..
..........K32KK.
.........K32K21K
........K32K21K.
.......K32K21K..
......K32K21K...
.....K3owK1K....
....K2sWK1K.....
..KbbbK..KbbbK..
..Kc.cK..Kb.bK..
..KbbbK..KbbbK..
...KKK....KKK...
................
................
................
""")

HELMET = G("""
................
................
................
....KKKKKKKK....
...K32222211K...
..K3222222ow1K..
..K3222222sW1K..
..K22KKKKKK12K..
..K12KKKKKK21K..
..K12K....K21K..
..K12K....K21K..
..KbbK....KbbK..
..KKKK....KKKK..
................
................
................
""")

CHESTPLATE = G("""
................
..KKKK....KKKK..
.K3221K..K3221K.
.K32221KK12221K.
.K322222222221K.
.K3222222ow221K.
.K3222222sW221K.
.K322222222221K.
.K322222222221K.
.K322222222221K.
.KcbbbbbaaaaaaK.
.K312222222211K.
..K1222222221K..
..KKKKKKKKKKKK..
................
................
""")

LEGGINGS = G("""
................
................
..KKKKKKKKKKKK..
..KcbbbowbaaaK..
..K3222sW2221K..
..K3221K.K3221K.
..K3221K.K3221K.
..K3222K.K3222K.
..K3222K.K2221K.
..K3221K.K2221K.
..KcbbbK.K2221K.
..K3221K.K3221K.
..K1221K.K1221K.
..KKKKKK.KKKKKK.
................
................
""")

BOOTS = G("""
................
................
................
.KKKKKK..KKKKKK.
.K3221K..K3221K.
.KcbbbK..K3221K.
.K3221K..Kow21K.
.K3221K..KsW21K.
.K3221K..K3221K.
.K3221K..K2221K.
K32221K.K32221K.
K12221K.K12221K.
KKKKKKK.KKKKKKK.
................
................
................
""")

FISHING_ROD = G("""
................
.............KK.
...........K3K.e
..........K3K..e
.........K2K...e
........K3K....e
.......K2K.....e
......K3K......e
.....K2K.......e
....K3K........e
...KowK........e
..KbsWK........e
..K2K.........cc
.K3K..........c.
KbK.............
................
""")

FISHING_ROD_CAST = G("""
..............e.
.............Ke.
...........K3K..
..........K3K...
.........K2K....
........K3K.....
.......K2K......
......K3K.......
.....K2K........
....K3K.........
...KowK.........
..KbsWK.........
..K2K...........
.K3K............
KbK.............
................
""")

# arco: corpo comune (flettente ad angolo in alto a sinistra, rame sulle punte), cambia solo corda e freccia
BOW_BODY = G("""
................
..KKKKKKKKKKKcK.
.KK3ow2222222bK.
.K32sW..........
.K32............
.K32............
.K32............
.K32............
.K32............
.K32............
.K32............
.K32............
.KbK............
.KcK............
.KKK............
................
""")

BOW = OV(BOW_BODY, {3: "13:e", 4: "12:e", 5: "11:e", 6: "10:e", 7: "9:e", 8: "8:e", 9: "7:e", 10: "6:e", 11: "5:e", 12: "4:e"})
BOW_P0 = OV(BOW_BODY, {3: "13:e", 4: "12:e", 5: "12:e", 6: "11:e", 7: "11:e", 8: "10:e", 9: "9:e",
                       12: "4:e", 11: "5:ee", 10: "7:ee"}) 
BOW_P1 = OV(BOW_BODY, {3: "13:e", 4: "13:e", 5: "13:e", 6: "13:e", 7: "12:e", 8: "12:e", 9: "12:e", 10: "12:e", 11: "11:e",
                       12: "4:eeeeeeee"})
BOW_P2 = OV(BOW_BODY, {3: "14:e", 4: "14:e", 5: "14:e", 6: "14:e", 7: "14:e", 8: "14:e", 9: "14:e", 10: "14:e", 11: "14:e",
                       12: "14:e", 13: "4:eeeeeeeeeee"})
# la freccia (a mano) negli stati di tensione: punta chiara in alto a sinistra, asta di rame, cocca sulla corda
BOW_P0 = OV(BOW_P0, {5: "5:W", 6: "6:b", 7: "7:b", 8: "8:b", 9: "9:e"})
BOW_P1 = OV(BOW_P1, {5: "5:W", 6: "6:b", 7: "7:b", 8: "8:b", 9: "9:b", 10: "10:b", 11: "11:e"})
BOW_P2 = OV(BOW_P2, {4: "5:W", 5: "6:b", 6: "7:b", 7: "8:b", 8: "9:b", 9: "10:b", 10: "11:b", 11: "12:b", 12: "13:e"})

WORKSTAFF = G("""
................
.........KKKKK..
.......KK32221Kb
......K32KKK21Kb
.....bK2KowK11KK
......K2KsWK11Kb
......K32KKK21K.
.......KK3221K..
.......K21K.....
......K21K......
.....K21K.......
....K21K........
...K21K.........
..K21K..........
.KbcK...........
.KKK............
""")

ULTRA = G("""
................
..............KK
............KK3K
..........KK32K.
........KK322K..
......KK3221K...
.....K3221K.....
....K3221K......
...K3221K.......
.KKKKKKKKK......
..KcbopwbbK.....
...KbstxbbbK....
....KKKKKKKK....
..K12K..........
.KbcK...........
.KKK............
""")

BIDENT = G("""
........KK..KKK.
.......K32K.K32K
......K32K.K32K.
.....K32K.K32K..
....K32K.K32K...
...K32K.K32K....
..KcbbbbbbbK....
...KopwbbK......
....KstxbK......
.....K21K.......
....K21K........
...K21K.........
..K21K..........
.K21K...........
KbcK............
KKK.............
""")

# balestra: corpo comune (flettente in alto e a sinistra con le punte di rame, calcio in diagonale con il castone),
# cambiano solo corda e proiettile
CB_BODY = G("""
................
..KKKKKKKKKKKcK.
.KK3222222222bK.
.K322KKKK.......
.K32K322K.......
.K32K321K.......
.K32.K321K......
.K32..K321K.....
.K32...Kow1K....
.K32....KsW1K...
.K32.....K321K..
.K32......K321K.
.KbK.......K221K
.KcK.......KKKKK
.KKK............
................
""")
CB_STANDBY = OV(CB_BODY, {3: "13:e", 4: "12:e", 5: "11:e", 6: "10:e", 10: "6:e", 11: "5:e", 12: "4:e"})
CB_P0 = OV(CB_BODY, {3: "13:e", 4: "12:e", 5: "12:e", 6: "11:e", 7: "11:e", 8: "10:e", 9: "9:e", 10: "7:ee",
                     11: "5:ee", 12: "4:e"})
CB_P1 = OV(CB_BODY, {3: "14:e", 4: "13:e", 5: "13:e", 6: "13:e", 7: "12:e", 8: "12:e", 9: "12:e", 10: "12:e",
                     11: "12:e", 12: "4:eeeeeeee"})
CB_P2 = OV(CB_BODY, {3: "14:e", 4: "15:e", 5: "15:e", 6: "15:e", 7: "15:e", 8: "15:e", 9: "15:e", 10: "15:e",
                     11: "15:e", 12: "15:e", 13: "15:e", 14: "4:eeeeeeeeeeee"})


def bolt(head, shaft, tail, base=CB_P2):
    """Proiettile caricato (a mano): punta in alto a sinistra, asta lungo il calcio, cocca verso la corda."""
    return OV(base, {4: "6:" + head[0], 5: "7:" + head[1], 6: "8:" + shaft, 7: "9:" + shaft, 8: "10:" + shaft,
                     9: "11:" + shaft, 10: "12:" + tail[0], 11: "13:" + tail[1]})


CB_ARROW = bolt("We", "b", "ee")
CB_FIREWORK = bolt("wc", "b", "aW")
CB_AMETHYST = bolt("po", "b", "ee")
CB_CITRINE = bolt("xw", "b", "ee")
CB_TOPAZ = bolt("ts", "b", "ee")
CB_ONYX = bolt("ey", "b", "ee")
CB_MOONSTONE = bolt("Ll", "b", "ee")
CB_GLASS = bolt("Wg", "b", "ee")

S16 = {
    "sword": SWORD, "axe": AXE, "shovel": SHOVEL, "hoe": HOE, "shears": SHEARS, "helmet": HELMET,
    "chestplate": CHESTPLATE, "leggings": LEGGINGS, "boots": BOOTS, "fishing_rod": FISHING_ROD,
    "fishing_rod_cast": FISHING_ROD_CAST, "bow": BOW, "bow_pulling_0": BOW_P0, "bow_pulling_1": BOW_P1,
    "bow_pulling_2": BOW_P2, "workstaff": WORKSTAFF, "ultra_greatsword": ULTRA, "bident": BIDENT,
    "crossbow_standby": CB_STANDBY, "crossbow_pulling_0": CB_P0, "crossbow_pulling_1": CB_P1,
    "crossbow_pulling_2": CB_P2, "crossbow_arrow": CB_ARROW, "crossbow_firework": CB_FIREWORK,
    "crossbow_amethyst": CB_AMETHYST, "crossbow_citrine": CB_CITRINE, "crossbow_topaz": CB_TOPAZ,
    "crossbow_onyx": CB_ONYX, "crossbow_moonstone": CB_MOONSTONE, "crossbow_glass": CB_GLASS,
}

WORKSTAFF32 = G("""
17:KKKKKKKK 26:KcK
15:K3333222221KbK
14:K333222222111K
13:K33322222222111K
13:K322KKKKKKKK211K
13:K322KpoKKxwK211K 29:KK
13:K322KonKWwvK211K 29:cK
13:K322KKKtsKKK211K 29:bK
13:K322KKKrqKKK211K 29:KK
13:K322KKKKKKKK211K
13:K222KKKKKKKK111K
13:K22222222111111K
14:K222221111111K
15:K1111111111K
17:KKKKKKKK
14:K32K
13:KcbK
12:KbaK
11:K32K
10:K32K
9:K32K
8:K32K
7:K32K
6:K32K
5:K32K
4:KcbK
3:KbaK
2:KcbK
1:KbbK
0:KbaK
0:KKK

""", 32)

ULTRA32 = G("""
29:KK
27:KK3K
25:KK32K
23:KK322K
21:KK3221K
19:KK32211K
18:K322211K
17:K322211K
16:K322211K
15:K322211K
14:K322211K
13:K322211K
12:K322211K
11:K322211K
10:K322211K
9:K322211K
1:KKKKKKK 8:K322211K
2:Kc2221K 9:22211K
3:Kc2221K 10:211K
4:Kcbbb1K 11:1K
5:KbopwbK
6:KbstxbK
7:Kc22bbK
5:K32 8:Kc2221K
4:K32K 9:Kc2221K
3:K32K 10:KKKKKKK
2:K32K
1:KcbK
0:KbcK
0:KbbK
0:KKK

""", 32)

BIDENT32 = G("""

29:KK
28:K32K
27:K32K 20:KK
26:K32K 19:K32K
25:K32K 18:K32K
24:K32K 17:K32K
23:K32K 16:K32K
22:K32K 15:K32K
21:K32K 14:K32K
20:K32K 13:K32K
19:K32K 12:K32K
18:K32K 11:K32K
17:K32K 10:K32K
10:KcbbbbbbbbK
9:KbbopwbbbbK
8:KbbstxbbbbK
8:KbbbbbbbK
10:K32K
9:KcbK
8:KbaK
7:K32K
6:K32K
5:K32K
4:K32K
3:K32K
2:K32K
1:KcbK
0:KbbK
0:KbbK
0:KKK

""", 32)

CB20_BODY = G("""
2:KKKKKKKKKKKKKKK
0:KK 2:3222222222222 15:bcK
0:K32 3:KKKKKKKKKKKK 15:bcK
0:K32K 4:K321K 15:KKK
0:K32K 5:K321K
0:K32K 6:K321K
0:K32K 7:K321K
0:K32K 8:Kow1K
0:K32K 9:KsW1K
0:K32K 10:K321K
0:K32K 11:K321K
0:K32K 12:K221K
0:K32K 13:K321K
0:K32K 14:K321K
0:K32K 15:KcbbK
0:K32K 15:KKKKK
0:K32K
0:KbbK
0:KbcK
0:KKKK
""", 20)

CB20_STANDBY = OV(CB20_BODY, {3: "16:e", 4: "15:e", 5: "14:e", 6: "13:e", 10: "9:e", 11: "8:e", 12: "7:e", 13: "6:e",
                              14: "5:e", 15: "4:e"})
CB20_P0 = OV(CB20_BODY, {3: "16:e", 4: "15:e", 5: "15:e", 6: "14:e", 7: "14:e", 8: "13:e", 9: "11:ee", 10: "9:ee",
                         11: "8:e", 12: "7:e", 13: "6:e", 14: "5:e", 15: "4:e"})
CB20_P1 = OV(CB20_BODY, {3: "16:e", 4: "16:e", 5: "16:e", 6: "15:e", 7: "15:e", 8: "15:e", 9: "15:e", 10: "14:e",
                         11: "14:e", 12: "14:e", 13: "13:e", 14: "9:eeee", 15: "4:eeeee"})
CB20_P2 = OV(CB20_BODY, {3: "16:e", 4: "17:e", 5: "18:e", 6: "19:e", 7: "19:e", 8: "19:e", 9: "19:e", 10: "19:e",
                         11: "19:e", 12: "19:e", 13: "19:e", 14: "19:e", 15: "19:e", 16: "19:e", 17: "19:e",
                         18: "5:eeeeeeeeeeeeeee"})


def bolt20(head, shaft, tail):
    """Proiettile caricato (a mano) sul calcio: punta di tre pixel di gemma, asta, cocca."""
    L, M, D = head
    return OV(CB20_P2, {4: "8:" + L, 5: "9:" + M, 6: "10:" + M, 7: "11:" + D, 8: "12:" + shaft, 9: "13:" + shaft,
                        10: "14:" + shaft, 11: "15:" + tail, 12: "16:" + tail, 13: "17:" + tail})


def at(g20):
    """Colloca la griglia 20x20 nell'ingombro dell'originale (riquadro 12..31) della tela 32x32."""
    return ["." * 32] * 12 + ["." * 12 + r for r in g20]


S32 = {
    "workstaff": WORKSTAFF32, "ultra_greatsword": ULTRA32, "bident": BIDENT32,
    "crossbow_standby": at(CB20_STANDBY), "crossbow_pulling_0": at(CB20_P0), "crossbow_pulling_1": at(CB20_P1),
    "crossbow_pulling_2": at(CB20_P2),
    "crossbow_arrow": at(bolt20("Wey", "b", "e")), "crossbow_firework": at(bolt20("wcb", "b", "W")),
    "crossbow_amethyst": at(bolt20("pon", "b", "e")), "crossbow_citrine": at(bolt20("xwv", "b", "e")),
    "crossbow_topaz": at(bolt20("tsr", "b", "e")), "crossbow_onyx": at(bolt20("eyy", "b", "e")),
    "crossbow_moonstone": at(bolt20("Lll", "b", "e")), "crossbow_glass": at(bolt20("Wgg", "b", "e")),
}


# ---------------------------------------------------------------------------------------------------------------
# Costruzione dei PNG, tavola di confronto e verifica
# ---------------------------------------------------------------------------------------------------------------
def render(grid):
    n = len(grid)
    im = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = hexc(PAL[ch])
    return im


def files():
    """Elenco (nome file, griglia): 16x16 e 32x32 _oversized con gli stessi nomi dei file attuali."""
    out = [("trichromatic_%s.png" % k, g) for k, g in S16.items()]
    out += [("trichromatic_%s_oversized.png" % k, g) for k, g in S32.items()]
    return out


def current_names():
    return sorted(f for f in os.listdir(CUR) if f.startswith("trichromatic_") and f.endswith(".png"))


def cur_frame0(name):
    im = Image.open(os.path.join(CUR, name)).convert("RGBA")
    return im.crop((0, 0, im.width, im.width))


def ec_frame0(name):
    im = Image.open(os.path.join(EC, name)).convert("RGBA")
    return im.crop((0, 0, im.width, im.width))


def up(im, k=8):
    return im.resize((im.width * k, im.height * k), Image.NEAREST)


def tavola(items):
    bg = (96, 96, 104, 255)
    pad = 10
    label_h = 14
    cells = []
    for name, g in items:
        new = render(g)
        cur = cur_frame0(name)
        cells.append((name, up(cur), up(new)))
    cols = 3
    # 16x16 e 32x32 su righe separate
    small = [c for c in cells if c[1].width == 128]
    big = [c for c in cells if c[1].width == 256]
    ec_pairs = [("bedrock_sword.png", "trichromatic_sword.png"), ("bedrock_chestplate.png", "trichromatic_chestplate.png"),
                ("glass_crest_workstaff.png", "trichromatic_workstaff.png"),
                ("glass_crest_crossbow_amethyst_oversized.png", "trichromatic_crossbow_amethyst_oversized.png")]
    widths = [(2 * 128 + 3 * pad) * cols, (2 * 256 + 3 * pad) * 2]
    W = max(widths)

    def block_h(lst, cell_h, c):
        rows = (len(lst) + c - 1) // c
        return rows * (cell_h + label_h + pad)

    H = pad + block_h(small, 128, cols) + block_h(big, 256, 2) + 2 * (256 + 2 * label_h + 3 * pad)
    sh = Image.new("RGBA", (W, H), bg)
    d = ImageDraw.Draw(sh)
    y = pad

    def put(lst, w, c, y):
        for i, (name, a, b) in enumerate(lst):
            r, k = divmod(i, c)
            x = pad + k * (2 * w + 3 * pad)
            yy = y + r * (w + label_h + pad)
            d.text((x, yy), name.replace("trichromatic_", "").replace(".png", ""), fill=(200, 200, 205, 255))
            sh.paste(a, (x, yy + label_h), a)
            sh.paste(b, (x + w + pad, yy + label_h), b)
        return y + block_h(lst, w, c)

    y = put(small, 128, cols, y)
    y = put(big, 256, 2, y)
    # confronti con EC: la nostra versione sta a destra
    d.text((pad, y), "Confronto con Eternal Classics (sinistra: EC, destra: nostra)", fill=(255, 210, 160, 255))
    y += label_h + pad
    x = pad
    lookup = dict(items)
    for ecn, ours in ec_pairs:
        if ours not in lookup:
            continue
        a = up(ec_frame0(ecn))
        b = up(render(lookup[ours]))
        if x + a.width + b.width + 2 * pad > W:
            x = pad
            y += 256 + pad
        d.text((x, y), ecn.replace(".png", ""), fill=(200, 200, 205, 255))
        sh.paste(a, (x, y + label_h), a)
        sh.paste(b, (x + a.width + 4, y + label_h), b)
        x += a.width + b.width + 3 * pad
    sh.save(os.path.join(OUT, "tavola_a_brace.png"))


BB = []


def verify(items):
    ok = True
    want = current_names()
    got = sorted(n for n, _ in items)
    print("file attesi (attuali):", len(want), "- prodotti:", len(got))
    if want != got:
        ok = False
        print("  mancanti:", sorted(set(want) - set(got)), "extra:", sorted(set(got) - set(want)))
    for name, g in items:
        im = Image.open(os.path.join(SET, name))
        n = 32 if name.endswith("_oversized.png") else 16
        if im.size != (n, n):
            ok = False
            print("  dimensioni sbagliate", name, im.size)
        if im.mode != "RGBA":
            ok = False
            print("  modo sbagliato", name, im.mode)
        if any(a not in (0, 255) for a in im.getchannel("A").getdata()):
            ok = False
            print("  alfa intermedio", name)
        cur = cur_frame0(name)
        if name.endswith("_oversized.png"):
            bb, bc = im.getbbox(), cur.getbbox()
            BB.append((name, bc, bb))
            if any(abs(a - b) > 1 for a, b in zip(bb, bc)):
                ok = False
                print("  ingombro diverso", name, bc, bb)
        if list(cur.getdata()) == list(im.getdata()):
            ok = False
            print("  identico all'attuale", name)
        for row in g:
            for ch in row:
                if ch not in PAL:
                    ok = False
                    print("  lettera sconosciuta", name, ch)
    for n, bc, bb in BB:
        print("  bbox", n.replace("trichromatic_", ""), "attuale", bc, "nuovo", bb)
    print("verifica:", "OK" if ok else "FALLITA")
    return ok


def main():
    os.makedirs(SET, exist_ok=True)
    items = files()
    for name, g in items:
        render(g).save(os.path.join(SET, name))
    tavola(items)
    return 0 if verify(items) else 1


if __name__ == "__main__":
    sys.exit(main())
