"""T9 Alloy «Prismatic»: 10 proposte di texture (spada, ascia, elmo, corazza 16x16 + armatura indossata 128x128).

Il T9 e' fatto con le tre leghe di Allthemodium (ricerca/14-leghe-allthemodium.md, §6):
  VA vibranio-allthemodium   verde acqua #0f5c7a #178287 #1bb38a #26de88 #73ffb9 + arancio/oro #cf5a13 #ff8b04 #ffc70c
  UA unobtainium-allthemodium viola #432a94 #6c1ce1 #a82ce3 #d152e3 #ea84f5 + oro #f2a61d #ffc70c
  UV unobtainium-vibranio    viola #432a94 #6c1ce1 #bc2feb + verde acqua #1ebd95 #26de88 #73ffb9 (niente oro)
Ogni proposta parte da questi colori e dal tema prismatico di Spectrum (ciano, magenta, giallo, nero, bianco).

LE PROPOSTE
  01 Spettro       evolutiva       il T8 fa il passo successivo (vedi EVOLUTIVA qui sotto)
  02 Notte Iridata stesse sagome   corpo nero; la lega corre lungo il taglio come un arcobaleno a 6 tinte
  03 Trittico      stesse sagome   tre zone, tre leghe: in ogni zona corpo e taglio prendono i 2 colori di una lega
  04 Opale         stesse sagome   corpo d'opale latteo con scaglie di «fuoco» a mano; taglio iridescente, gemme d'opale nero
  05 Geode         stesse sagome   corpo viola scuro, taglio VA, un cristallo sfaccettato CMY incastonato al posto della gemma
  06 Scheggia      sagome nuove    lame e creste fatte di schegge di cristallo, una per lega
  07 Lamelle       sagome nuove    armatura a placche sovrapposte: ogni placca una lega, bordi chiari
  08 Forgia        sagome nuove    ferro battuto scuro e pesante, rivetti d'oro, intarsi di lega
  09 Radice        sagome nuove    forme organiche color osso con venature di lega che pulsano verso la gemma
  10 Aureola       sagome nuove    anelli di luce: un'aureola dorata e un raggio di spettro che la attraversa

METODO
  Proposte 01-05 (stesse sagome): partono dai PNG FINALI del T8 su disco (t8-argent/set e armatura), letti come griglie
  di RUOLI (t8_roles): ogni colore del T8 e' univoco per ruolo, quindi si torna alla lettera:
      0-6 corpo (argento)   a-f parte tagliente (unobtainium)   A-F puntini (vibranio)
      q r s t gemma (moonstone: contorno, scuro, medio, riflesso)   g-k stoffa   W riflesso bianco
  Ogni proposta da' a quei ruoli nuovi materiali (palette), eventualmente per ZONE disegnate a mano (griglie 16x16 di
  lettere-zona, o una zona per cubo/faccia/riga dell'armatura) e aggiunge DETTAGLI disegnati a mano pixel per pixel
  (dizionari {(x, y): lettera}). Le sagome (alpha) restano quelle del T8 (asserzione), cioe' quelle di T6/T7/T8.
  Proposte 06-10 (sagome nuove): icone disegnate a mano come griglie 16x16; l'armatura parte dalla griglia dei ruoli
  del T8 (solo per le facce laterali/superiori/inferiori, rimappate nei materiali della proposta) e ridisegna a mano
  tutte le facce che si vedono davanti e dietro (testa, busto, braccia, gambe, stivali, mantello, perizoma).

EVOLUTIVA (01 Spettro). La catena: T6 bedrock + allthemodium + gemme CMY -> T7 metallo nero + vibranio + puntini di
  allthemodium + onice -> T8 argento + unobtainium + puntini di vibranio + moonstone. Il passo T8 -> T9:
      corpo 0-6 argento          -> madreperla iridescente: contorno prugna, toni medi che girano lilla -> rosa -> menta
      taglio a-f unobtainium     -> LEGA a fasce: ogni pixel prende la rampa della sua fascia (VA, UA, UV), stessi 6 toni;
                                    nelle rampe il colore chiaro e' il secondo metallo della lega (VA: verde acqua con luci
                                    d'oro; UA: viola/magenta con luci d'oro; UV: viola con luci verde acqua)
      puntini A-F vibranio       -> unobtainium viola: ESATTAMENTE la rampa UNOB del T8 (il tier precedente «cola»)
      gemme moonstone            -> tutto lo spettro (T6 CMY -> T7 nero -> T8 bianco -> T9 tutti e 5): ogni pixel di gemma
                                    ha uno dei 5 colori, a mano nelle icone; nell'armatura riflesso t = bianco,
                                    contorno q e scuro r = nero, i pixel medi s di ogni fila = ciano, magenta, giallo
      stoffa ardesia             -> prugna scura (sta sotto la madreperla e non si confonde col viola della lega)
  Fasce: disegnate a mano per icona (BAND_*) e per cubo/riga nell'armatura (BAND_ARMOR).

Uso:  python3 -I tools/texture_t9_proposte.py   # scrive design/texture/proposte/t9-proposte/ (10 cartelle, proposte.json,
                                               # panoramica.png). Legge soltanto t8-argent/.
"""
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_mix as M  # noqa: E402
import texture_t8 as E8  # noqa: E402   solo palette del T8 (lettura)

PROP = os.path.join(H.ROOT, "design", "texture", "proposte")
T8DIR = os.path.join(PROP, "t8-argent")
OUT = os.path.join(PROP, "t9-proposte")
ICONS = ("sword", "axe", "helmet", "chestplate")
MAN_BG = (52, 52, 60, 255)          # sfondo del manichino, come T7/T8


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


# ======================================================================================================================
# Lettura dei PNG finali del T8 come griglie di ruoli
# ======================================================================================================================
T8_ROLE = {}
for _k, _v in E8.ARGENT.items():
    T8_ROLE[rgb(_v)] = _k
for _k, _v in E8.UNOB.items():
    T8_ROLE[rgb(_v)] = _k
for _k, _v in E8.VIB_DOT.items():
    T8_ROLE[rgb(_v)] = _k.upper()
for _r, _c in zip(("rim", "dark", "mid", "sheen"), "qrst"):
    T8_ROLE[rgb(E8.MOON[_r])] = _c
for _k, _v in E8.CLOTH.items():
    T8_ROLE[rgb(_v)] = _k
T8_ROLE[rgb(E8.STRING)] = "z"
T8_ROLE[(255, 255, 255)] = "W"
assert len(T8_ROLE) == 7 + 6 + 6 + 4 + 5 + 2


def t8_roles(path):
    im = Image.open(path).convert("RGBA")
    px = im.load()
    rows = []
    for y in range(im.height):
        r = ""
        for x in range(im.width):
            p = px[x, y]
            r += "." if p[3] == 0 else T8_ROLE[p[:3]]
        rows.append(r)
    return rows


def t8_icon(n):
    return t8_roles(os.path.join(T8DIR, "set", f"t8_{n}.png"))


def t8_armor():
    return t8_roles(os.path.join(T8DIR, "armatura", "t8_armor.png"))


# mappa pixel della texture 128x128 -> (cubo, faccia, x, y nella faccia)
WHERE = {}
for _cube, _box in M.CUBES.items():
    for _face, (_fx, _fy, _fw, _fh) in M.cube_faces(*_box).items():
        for _y in range(_fh):
            for _x in range(_fw):
                WHERE[(_fx + _x, _fy + _y)] = (_cube, _face, _x, _y)


# ======================================================================================================================
# Strumenti di composizione (nessuna regola di disegno qui)
# ======================================================================================================================
def render(grid, pal):
    h, w = len(grid), len(grid[0])
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(grid):
        assert len(row) == w, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = rgb(pal[ch]) + (255,)
    return im


def render_z(grid, pal, zone_of, zpal):
    """Come render, ma la palette di un pixel dipende dalla sua zona: zpal[zona] sovrascrive pal."""
    h, w = len(grid), len(grid[0])
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            z = zone_of(x, y)
            p = zpal.get(z, {}) if z else {}
            px[x, y] = rgb(p.get(ch, pal.get(ch))) + (255,)
    return im


def put(grid, pts):
    """Dettagli a mano: pts = {(x, y): lettera}. Solo su pixel gia' pieni (la sagoma non cambia)."""
    g = [list(r) for r in grid]
    for (x, y), ch in pts.items():
        assert g[y][x] != ".", ("dettaglio fuori sagoma", x, y)
        g[y][x] = ch
    return ["".join(r) for r in g]


def zgrid(text):
    rows = text.split()
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    return rows


def face_rows(grid128, cube, face):
    fx, fy, fw, fh = M.cube_faces(*M.CUBES[cube])[face]
    return [grid128[fy + y][fx:fx + fw] for y in range(fh)]


def set_faces(grid128, faces):
    """faces: {(cubo, faccia): righe}. Righe scritte a mano; '.' = trasparente (non su mantello e perizoma)."""
    g = [list(r) for r in grid128]
    for (cube, face), rows in faces.items():
        if isinstance(rows, str):
            rows = rows.split()
        fx, fy, fw, fh = M.cube_faces(*M.CUBES[cube])[face]
        assert len(rows) == fh and all(len(r) == fw for r in rows), (cube, face, fw, fh, rows)
        if cube in ("cape", "front_cloth"):
            assert "." not in "".join(rows), (cube, face)
        for y, r in enumerate(rows):
            for x, ch in enumerate(r):
                g[fy + y][fx + x] = ch
    return ["".join(r) for r in g]


def put_faces(grid128, pts):
    """Dettagli a mano sull'armatura: pts = {(cubo, faccia): {(x, y): lettera}} (coordinate nella faccia)."""
    g = [list(r) for r in grid128]
    for (cube, face), d in pts.items():
        fx, fy, fw, fh = M.cube_faces(*M.CUBES[cube])[face]
        for (x, y), ch in d.items():
            assert 0 <= x < fw and 0 <= y < fh and g[fy + y][fx + x] != ".", (cube, face, x, y)
            g[fy + y][fx + x] = ch
    return ["".join(r) for r in g]


def gem_rule(grid128, t="W", q="K", r="K", s_run=("7", "8", "9")):
    """Gemme dell'armatura (ruoli q r s t del T8): riflesso, contorno e scuro fissi; i pixel medi s di ogni fila
    orizzontale prendono s_run da sinistra a destra (in una faccia del T8 le file di s sono lunghe 1-3)."""
    g = [list(row) for row in grid128]
    for y, row in enumerate(g):
        k = 0
        for x, ch in enumerate(row):
            if ch == "s":
                g[y][x] = s_run[k % len(s_run)]
                k += 1
            else:
                k = 0
                if ch == "t":
                    g[y][x] = t
                elif ch == "q":
                    g[y][x] = q
                elif ch == "r":
                    g[y][x] = r
    return ["".join(row) for row in g]


def armor_zone_fn(spec, default=None):
    """spec: {cubo: zona} oppure {(cubo, faccia): zona o stringa di zone per riga o lista di righe di zone}."""
    def zone_of(x, y):
        w = WHERE.get((x, y))
        if not w:
            return default
        cube, face, fx, fy = w
        z = spec.get((cube, face), spec.get(cube, default))
        if isinstance(z, list):
            return z[fy][fx]
        if isinstance(z, str) and len(z) > 1:
            return z[fy]
        return z
    return zone_of


def sil(im):
    return im.getchannel("A").tobytes()


def save_proposal(num, slug, icons, armor):
    d = os.path.join(OUT, f"{num:02d}-{slug}")
    os.makedirs(d, exist_ok=True)
    for n in ICONS:
        im = icons[n]
        assert im.size == (16, 16) and im.mode == "RGBA", (num, n)
        im.save(os.path.join(d, f"t9_{n}.png"))
    assert armor.size == (128, 128) and armor.mode == "RGBA"
    armor.save(os.path.join(d, "t9_armor.png"))
    man = H.on_bg(M.mannequin(armor, 8), MAN_BG)
    man.save(os.path.join(d, "manichino.png"))
    return d


# ======================================================================================================================
# 01 SPETTRO (evolutiva)
# ======================================================================================================================
P1_BODY = dict(zip("0123456", ("#3b3150", "#625677", "#958aa9", "#b2acd2", "#dbc9da", "#e6f3ea", "#fffbf2")))
#   0 prugna (contorno, piu' caldo del grigio-blu del T8)  2 lilla-grigio  3 pervinca  4 rosa perla  5 menta-bianco
P1_ALLOY = {
    "V": dict(zip("abcdef", ("#0c2730", "#0f5c7a", "#1bb38a", "#26de88", "#ffb21c", "#fff1a8"))),   # VA
    "U": dict(zip("abcdef", ("#1d0b38", "#432a94", "#8f2fe0", "#d152e3", "#ff9d1a", "#ffe28a"))),   # UA
    "X": dict(zip("abcdef", ("#120c36", "#432a94", "#6c1ce1", "#1ebd95", "#73ffb9", "#dcfff0"))),   # UV
}
P1_DOTS = {k.upper(): v for k, v in E8.UNOB.items()}      # unobtainium del T8, identico
P1_CLOTH = dict(zip("ghijk", ("#22162b", "#33213f", "#472f56", "#5e3f70", "#7a568e")))
SPEC5 = {"7": "#3fd8f2", "8": "#ee4fc6", "9": "#ffdf3a", "K": "#16121d", "W": "#ffffff"}   # ciano magenta giallo nero bianco
P1_PAL = {**P1_BODY, **P1_ALLOY["V"], **P1_DOTS, **P1_CLOTH, **SPEC5, "W": "#ffffff"}

# fasce della lega (V = VA, U = UA, X = UV), a mano: contano solo sui pixel a-f
BAND_SWORD = zgrid("""
VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV XXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXX
XXXXXXXUUUUUUUUU UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU VVVVVXXXUUUUUUUU XXXXXXUUUUUUUUUU
XXXXXXUUUUUUUUUU VVVVVVVUUUUUUUUU VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV""")
BAND_AXE = zgrid("""
VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV XXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXX
UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU XXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXX
XXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXX VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV""")
BAND_HELMET = zgrid("""
UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU VVVVVVVVUUUUUUUU
VVVVVVXXXXXXUUUU VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV
VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV""")
BAND_CHEST = zgrid("""
VVVVVXXXXXXUUUUU VVVVVXXXXXXUUUUU VVVVVXXXXXXUUUUU VVVVVXXXXXXUUUUU VVVVVXXXXXXUUUUU VVVVVXXXXXXUUUUU
VVVVVXXXXXXUUUUU VVVVVXXXXXXUUUUU VVVVVXXXXXXUUUUU VVVVVXXXXXXUUUUU VVVVVVVUUXXXXXXX VVVVVVVUUXXXXXXX
VVVVVVVUUXXXXXXX VVVVVVVUUXXXXXXX VVVVVVVUUXXXXXXX VVVVVVVUUXXXXXXX""")
P1_BANDS = {"sword": BAND_SWORD, "axe": BAND_AXE, "helmet": BAND_HELMET, "chestplate": BAND_CHEST}
# gemme delle icone: ogni pixel un colore dello spettro (a mano)
P1_GEMS = {
    # spada: il castone fa da prisma, la luce bianca si apre in diagonale: bianco, ciano, magenta, giallo, nero
    "sword": {(5, 7): "W", (4, 8): "W", (3, 9): "W", (5, 8): "7", (4, 9): "7", (6, 8): "8", (5, 9): "8", (6, 9): "9",
              (7, 9): "K"},
    "axe": {(8, 8): "W", (7, 9): "7", (8, 9): "8"},
    "helmet": {(10, 4): "W", (9, 5): "7", (10, 5): "8", (11, 5): "9"},
    "chestplate": {(7, 5): "8", (8, 5): "K"},
}
# fasce nell'armatura: per cubo, o per faccia con una zona per riga
BAND_ARMOR = {
    "head_out": "U", "head_in": "U",
    ("body_out", "north"): "XXXXXXXUVV", ("body_out", "south"): "XXXXXXXUVV",
    ("body_out", "west"): "XXXXXXXUVV", ("body_out", "east"): "XXXXXXXUVV", "body_out": "X", "body_in": "X",
    "right_arm_extra": "V", "left_arm_extra1": "V", "left_arm_extra2": "V",
    "right_leg": "X", "right_boot": "V", "left_leg": "U", "left_boot": "U", "cape": "U", "front_cloth": "X",
}


def evolutiva():
    """01 Spettro: dai PNG finali del T8 (icone e armatura), sagome identiche (asserzione)."""
    icons = {}
    for n in ICONS:
        g = t8_icon(n)
        assert set("".join(g)) <= set(".0123456abcdefABCDEFqrstW"), n
        gems = {p for p, ch in ((((x, y), ch)) for y, r in enumerate(g) for x, ch in enumerate(r)) if ch in "qrst"}
        assert gems == set(P1_GEMS[n]), (n, sorted(gems))
        g = put(g, P1_GEMS[n])
        band = P1_BANDS[n]
        im = render_z(g, P1_PAL, lambda x, y: band[y][x], P1_ALLOY)
        assert sil(im) == sil(Image.open(os.path.join(T8DIR, "set", f"t8_{n}.png")).convert("RGBA")), n
        icons[n] = im
    g = gem_rule(t8_armor())
    arm = render_z(g, P1_PAL, armor_zone_fn(BAND_ARMOR, "U"), P1_ALLOY)
    assert sil(arm) == sil(Image.open(os.path.join(T8DIR, "armatura", "t8_armor.png")).convert("RGBA"))
    return icons, arm


# ======================================================================================================================
# 02 NOTTE IRIDATA: corpo nero, la lega corre lungo il taglio in 6 tinte (oro, arancio, magenta, viola, verde acqua, menta)
# ======================================================================================================================
P2_BODY = dict(zip("0123456", ("#07060c", "#15121f", "#221d31", "#322b46", "#463d5e", "#605679", "#8a80a6")))
P2_HUE = {   # a contorno, b profondo, c base, d acceso, e chiaro, f pallido
    "1": dict(zip("abcdef", ("#3a1d04", "#9a5a06", "#e8a010", "#ffc70c", "#ffe26a", "#fff7c8"))),   # oro (allthemodium)
    "2": dict(zip("abcdef", ("#3a1206", "#a93a08", "#e2650e", "#ff8b04", "#ffb45a", "#ffe2b8"))),   # arancio
    "3": dict(zip("abcdef", ("#3a0a34", "#8a1a78", "#c03ab0", "#e05ad8", "#f29aec", "#ffd8f8"))),   # magenta (UA)
    "4": dict(zip("abcdef", ("#1c0a3a", "#432a94", "#6c1ce1", "#9a52f0", "#c79cff", "#efe0ff"))),   # viola (unobtainium)
    "5": dict(zip("abcdef", ("#062a30", "#0f5c7a", "#178f9a", "#1bc0b0", "#6ee8d8", "#d0fff6"))),   # verde acqua
    "6": dict(zip("abcdef", ("#08301c", "#127a4a", "#1bb38a", "#26de88", "#73ffb9", "#e0fff0"))),   # menta (vibranio)
}
P2_DOTS = dict(zip("ABCDEF", ("#2a2340", "#5b4f8a", "#8fa8e0", "#b6e6f0", "#f6f0c0", "#ffffff")))   # stelle
P2_GEM = {"q": "#2a2a44", "r": "#9fb4e0", "s": "#e8f0ff", "t": "#ffffff"}                          # diamante
P2_CLOTH = dict(zip("ghijk", ("#0e0d16", "#181626", "#242138", "#34304d", "#4a4568")))
P2_PAL = {**P2_BODY, **P2_HUE["1"], **P2_DOTS, **P2_GEM, **P2_CLOTH, "W": "#ffffff"}
# tinta per riga (spada, ascia) o per colonna (elmo, corazza), a mano: la lega «scorre» lungo l'oggetto
P2_ROWS = {"sword": "1112233445611111", "axe": "1123456444311111"}
P2_COLS = {"helmet": "1111223344556666", "chestplate": "1112223344556666"}
# stelle in piu' sul corpo nero (a mano): piccole scintille bianche
P2_STARS = {
    "sword": {(12, 4): "E", (4, 6): "D", (8, 11): "F"},
    "axe": {(11, 5): "F", (10, 7): "D", (4, 12): "E"},
    "helmet": {(6, 3): "F", (3, 8): "D", (12, 9): "E"},
    "chestplate": {(3, 3): "F", (12, 7): "E", (4, 13): "D", (11, 12): "F"},
}
P2_ARMOR_ZONE = {"head_out": "3", "head_in": "3", "body_out": "4", ("body_out", "north"): "4444444456",
                 ("body_out", "south"): "4444444456", "body_in": "4", "right_arm_extra": "2", "left_arm_extra1": "1",
                 "left_arm_extra2": "1", "right_leg": "5", "right_boot": "6", "left_leg": "1", "left_boot": "2",
                 "cape": "4", "front_cloth": "5"}
P2_ARMOR_STARS = {
    ("body_out", "north"): {(1, 5): "E", (8, 4): "F", (2, 9): "D"},
    ("body_out", "south"): {(7, 1): "F", (2, 5): "E"},
    ("right_arm", "north"): {(1, 3): "F", (3, 8): "D"},
    ("left_arm", "north"): {(3, 2): "E", (1, 9): "F"},
    ("head_out", "north"): {(7, 0): "F"},
    ("head_out", "south"): {(2, 4): "E", (6, 7): "F"},
    ("right_leg", "north"): {(3, 5): "E"},
}


def p02_notte_iridata():
    icons = {}
    for n in ICONS:
        g = put(t8_icon(n), P2_STARS[n])
        if n in P2_ROWS:
            zf = (lambda s: lambda x, y: s[y])(P2_ROWS[n])
        else:
            zf = (lambda s: lambda x, y: s[x])(P2_COLS[n])
        icons[n] = render_z(g, P2_PAL, zf, P2_HUE)
    g = put_faces(t8_armor(), P2_ARMOR_STARS)
    arm = render_z(g, P2_PAL, armor_zone_fn(P2_ARMOR_ZONE, "4"), P2_HUE)
    return icons, arm


# ======================================================================================================================
# 03 TRITTICO: tre zone, tre leghe. In ogni zona il corpo prende il primo metallo della lega e il taglio il secondo;
#    i puntini prendono il colore che manca in quella zona, la gemma un colore dello spettro.
# ======================================================================================================================
P3_PAL = {"0": "#140d1a", "W": "#ffffff", **dict(zip("ghijk", ("#2a0f1e", "#3d1630", "#561f44", "#72305c", "#904877")))}
P3_ZONES = {
    "V": {**dict(zip("123456", ("#173238", "#24525a", "#337470", "#4c9686", "#76bba0", "#b2e2c6"))),     # verde acqua
          **dict(zip("abcdef", ("#3a1606", "#a92405", "#cf5a13", "#ff8b04", "#ffc70c", "#fff2a0"))),     # + oro/arancio
          **dict(zip("ABCDEF", ("#2a1050", "#4b1f88", "#7e35c3", "#a661ea", "#cfa3ff", "#f2e6ff"))),     # puntini viola
          "q": "#0c3a48", "r": "#1a8fb0", "s": "#3fd8f2", "t": "#d8fbff"},                               # gemma ciano
    "U": {**dict(zip("123456", ("#25183f", "#3a2768", "#53398e", "#7155ae", "#987bcc", "#c7b2ec"))),     # viola
          **dict(zip("abcdef", ("#3a1206", "#9a3a12", "#d07020", "#f2a61d", "#ffd84a", "#fff6c0"))),     # + oro
          **dict(zip("ABCDEF", ("#08301c", "#127a4a", "#1bb38a", "#26de88", "#73ffb9", "#e0fff0"))),     # puntini menta
          "q": "#4a0c3e", "r": "#a8238a", "s": "#ee4fc6", "t": "#ffd0f2"},                               # gemma magenta
    "X": {**dict(zip("123456", ("#142445", "#1f3a6a", "#2c5590", "#3f74b0", "#62a0cc", "#a4d6ea"))),     # blu (viola+verde)
          **dict(zip("abcdef", ("#160a33", "#432a94", "#6c1ce1", "#bc2feb", "#73ffb9", "#e0fff0"))),     # viola, luci menta
          **dict(zip("ABCDEF", ("#3a1d04", "#9a5a06", "#e8a010", "#ffc70c", "#ffe26a", "#fff7c8"))),     # puntini oro
          "q": "#4a3a06", "r": "#b09010", "s": "#ffdf3a", "t": "#fffbd0"},                               # gemma gialla
}
P3_ZMAP = {
    "sword": zgrid("""
VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV UUUUUUUVVVVVVVVV
UUUUUUUVVVVVVVVV UUUUUUUUVVVVVVVV UUUUUUUUVVVVVVVV UUUUUUUUUVVVVVVV UUUUUUUUUUUUUUUU XXXXXXXXXXXXXXXX
XXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXX"""),
    "axe": zgrid("""
VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV
VVVVVVVVVVVVVVVV VVVVVVVVVVVVVVVV UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU XXXXXXXXXXXXXXXX
XXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXX"""),
    "helmet": zgrid("""
UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU UUUUUUUUUUUUUUUU VVVVVVVVXXXXXXXX
VVVVVVVVXXXXXXXX VVVVVVVVXXXXXXXX VVVVVVVVXXXXXXXX VVVVVVVVXXXXXXXX VVVVVVVVXXXXXXXX VVVVVVVVXXXXXXXX
VVVVVVVVXXXXXXXX VVVVVVVVXXXXXXXX VVVVVVVVXXXXXXXX VVVVVVVVXXXXXXXX"""),
    "chestplate": zgrid(" ".join(["VVVVVUUUUUUXXXXX"] * 16)),
}
P3_ARMOR_ZONE = {
    "head_out": "U", "head_in": "U",
    ("body_out", "north"): ["VVVUUUUXXX"] * 10, ("body_out", "south"): ["XXXUUUUVVV"] * 10,
    ("body_out", "west"): "V", ("body_out", "east"): "X", ("body_out", "top"): ["VVVUUUUXXX"] * 3,
    ("body_out", "bottom"): ["VVVUUUUXXX"] * 3, "body_in": "U", ("body_in", "north"): ["VVVUUUXXX"] * 13,
    ("body_in", "south"): ["XXXUUUVVV"] * 13,
    "right_arm": "V", "right_arm_extra": "V", "left_arm": "X", "left_arm_extra1": "X", "left_arm_extra2": "X",
    "right_leg": "V", "right_boot": "U", "left_leg": "X", "left_boot": "U", "cape": "U", "front_cloth": "U",
}


def p03_trittico():
    icons = {}
    for n in ICONS:
        z = P3_ZMAP[n]
        icons[n] = render_z(t8_icon(n), P3_PAL, lambda x, y: z[y][x], P3_ZONES)
    arm = render_z(t8_armor(), P3_PAL, armor_zone_fn(P3_ARMOR_ZONE, "U"), P3_ZONES)
    return icons, arm


# ======================================================================================================================
# 04 OPALE: corpo d'opale latteo con scaglie di «fuoco» messe a mano; il taglio cambia tinta col tono (iridescenza:
#    viola in ombra, verde acqua nei medi, oro nelle luci: le tre leghe in una rampa); gemme d'opale nero.
# ======================================================================================================================
P4_PAL = {
    **dict(zip("0123456", ("#4a4466", "#7d7a9e", "#a9acc8", "#c6cfe0", "#dce6ee", "#edf4f4", "#ffffff"))),
    **dict(zip("abcdef", ("#2a1450", "#5b2ea8", "#2f9fb0", "#5fe0b8", "#ffd27a", "#fff6e0"))),
    **dict(zip("ABCDEF", ("#3a0f3a", "#7a1f78", "#c13cb8", "#e86ad8", "#f8a8ec", "#ffe0f8"))),
    "q": "#120e1e", "r": "#2a2350", "s": "#3d6bd0", "t": "#6dffc0",
    **dict(zip("ghijk", ("#0f2230", "#173246", "#22465e", "#2f5c78", "#417896"))),
    # fuoco dell'opale (scaglie): rosa, verde, arancio, azzurro, viola
    "P": "#ff7ac8", "G": "#3fe89a", "O": "#ffa040", "U": "#5ab4ff", "L": "#a06cff", "W": "#ffffff",
}
P4_FIRE = {
    "sword": {(13, 2): "G", (12, 4): "P", (11, 5): "U", (4, 6): "O", (8, 7): "G", (4, 11): "L", (8, 11): "P"},
    "axe": {(11, 5): "P", (10, 6): "G", (12, 7): "U", (11, 7): "L", (4, 12): "O"},
    "helmet": {(6, 3): "G", (4, 4): "P", (5, 4): "O", (7, 5): "U", (3, 8): "O", (12, 9): "G", (11, 10): "P"},
    "chestplate": {(3, 3): "P", (4, 4): "O", (2, 5): "U", (6, 7): "G", (10, 8): "O", (7, 9): "U", (4, 12): "P",
                   (11, 13): "G", (12, 6): "L", (9, 7): "P"},
}
P4_ARMOR_FIRE = {
    ("head_out", "north"): {(1, 0): "G", (2, 1): "P", (8, 0): "U"},
    ("head_out", "south"): {(2, 1): "O", (4, 5): "G", (7, 5): "P", (3, 8): "U"},
    ("head_out", "west"): {(2, 1): "P", (5, 5): "G"},
    ("head_out", "east"): {(6, 1): "U", (4, 5): "O"},
    ("head_out", "top"): {(3, 2): "P", (6, 4): "G", (2, 6): "U"},
    ("body_out", "north"): {(1, 5): "P", (2, 4): "O", (7, 5): "G", (8, 6): "U", (2, 9): "L"},
    ("body_out", "south"): {(2, 2): "G", (6, 3): "P", (5, 5): "O", (8, 6): "U"},
    ("right_arm", "north"): {(1, 1): "G", (2, 4): "P", (1, 8): "O"},
    ("right_arm", "south"): {(2, 2): "U", (1, 7): "P"},
    ("left_arm", "north"): {(3, 1): "O", (2, 5): "G", (3, 9): "U"},
    ("left_arm", "south"): {(1, 3): "P", (3, 6): "G"},
    ("right_leg", "north"): {(3, 1): "P", (2, 5): "G"},
    ("left_leg", "north"): {(1, 1): "O", (3, 8): "U"},
    ("left_leg", "south"): {(2, 2): "G", (3, 6): "P"},
    ("right_leg", "south"): {(1, 4): "U"},
    ("right_arm_extra", "north"): {(3, 1): "P"},
    ("left_arm_extra2", "north"): {(2, 0): "G"},
}


def p04_opale():
    icons = {n: render(put(t8_icon(n), P4_FIRE[n]), P4_PAL) for n in ICONS}
    arm = render(put_faces(t8_armor(), P4_ARMOR_FIRE), P4_PAL)
    return icons, arm


# ======================================================================================================================
# 05 GEODE: corpo viola scuro (unobtainium brunito), taglio VA (verde acqua con luci d'oro), puntini d'oro; al posto
#    della gemma un cristallo sfaccettato piu' grande, coi colori dello spettro (bianco, ciano, magenta, giallo, contorno).
# ======================================================================================================================
P5_PAL = {
    **dict(zip("0123456", ("#120a1c", "#24163a", "#352454", "#483470", "#5e478c", "#7a62a8", "#a08cc8"))),
    **dict(zip("abcdef", ("#0b2a2a", "#12686a", "#1bb38a", "#4fe0a0", "#ffc70c", "#fff3b0"))),
    **dict(zip("ABCDEF", ("#3a1606", "#8a3a08", "#cf5a13", "#ff8b04", "#ffc70c", "#fff0a0"))),
    **dict(zip("ghijk", ("#140b20", "#1f1231", "#2c1a45", "#3b245b", "#4d3174"))),
    # cristallo: K contorno, L luce, W riflesso, T ciano, P magenta, Y giallo
    "K": "#1a1236", "L": "#e4f6ff", "W": "#ffffff", "T": "#5ce8ff", "P": "#ff6ad8", "Y": "#ffe45c",
}
P5_CRYSTAL = {
    "sword": {(3, 7): "K", (4, 7): "L", (5, 7): "W", (6, 7): "T", (3, 8): "L", (4, 8): "W", (5, 8): "T", (6, 8): "P",
              (7, 8): "K", (2, 9): "K", (3, 9): "T", (4, 9): "T", (5, 9): "P", (6, 9): "P", (7, 9): "Y", (8, 9): "K",
              (4, 10): "K", (5, 10): "Y", (6, 10): "Y", (7, 10): "K"},
    "axe": {(10, 5): "L", (11, 5): "W", (10, 6): "T", (11, 6): "T", (12, 6): "P", (10, 7): "P", (11, 7): "Y",
            (12, 7): "K", (8, 8): "W", (7, 9): "T", (8, 9): "P"},
    "helmet": {(10, 3): "L", (11, 3): "K", (9, 4): "L", (10, 4): "W", (11, 4): "T", (8, 5): "K", (9, 5): "T",
               (10, 5): "P", (11, 5): "P", (12, 5): "K", (8, 6): "K", (9, 6): "Y", (10, 6): "K"},
    "chestplate": {(6, 4): "K", (7, 4): "W", (8, 4): "L", (9, 4): "K", (6, 5): "T", (7, 5): "T", (8, 5): "P",
                   (9, 5): "P", (6, 6): "K", (7, 6): "Y", (8, 6): "Y", (9, 6): "K"},
}
P5_ARMOR_CRYSTAL = {
    ("body_out", "north"): {(3, 1): "K", (4, 1): "W", (5, 1): "L", (6, 1): "K", (3, 2): "T", (4, 2): "T",
                            (5, 2): "P", (6, 2): "P", (3, 3): "K", (4, 3): "Y", (5, 3): "Y", (6, 3): "K"},
    ("head_out", "north"): {(5, 2): "L", (6, 2): "W", (7, 2): "T", (4, 3): "K", (8, 3): "K"},
}


def p05_geode():
    icons = {n: render(put(t8_icon(n), P5_CRYSTAL[n]), P5_PAL) for n in ICONS}
    g = put_faces(t8_armor(), P5_ARMOR_CRYSTAL)
    g = gem_rule(g, t="W", q="K", r="P", s_run=("T", "P", "Y"))
    arm = render(g, P5_PAL)
    return icons, arm


# ======================================================================================================================
# SAGOME NUOVE (06-10): icone e facce dell'armatura disegnate a mano
# ======================================================================================================================
def G16(text):
    rows = text.split()
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    return rows


def ramp(letters, hexes):
    assert len(letters) == len(hexes)
    return dict(zip(letters, hexes))


def nuova(P):
    """Sagome nuove: icone dalle griglie; armatura = ruoli del T8 nei materiali della proposta (facce laterali, sopra,
    sotto, interno della testa, spallacci) + facce davanti/dietro ridisegnate a mano."""
    pal = P["pal"]
    icons = {}
    for n in ICONS:
        g = P["icons"][n]
        miss = set("".join(g)) - set(pal) - {"."}
        assert not miss, (P["nome"], n, miss)
        icons[n] = render(g, pal)
    g = set_faces(t8_armor(), P["faces"])
    miss = set("".join(g)) - set(pal) - {"."}
    assert not miss, (P["nome"], "armatura", miss)
    return icons, render(g, pal)


# ---------------------------------------------------------------------------------------------------------------------
# 06 SCHEGGIA: lame e creste fatte di schegge di cristallo, una per lega. Montature di metallo ardesia scuro.
#   l m n verde acqua (VA) | u v w viola (UA/UV) | x y z oro-arancio (allthemodium delle leghe) | 0-6 ardesia
# ---------------------------------------------------------------------------------------------------------------------
P6 = {"nome": "Scheggia", "pal": {
    **ramp("0123456", ("#120d1e", "#231a36", "#3a3054", "#554a74", "#776d96", "#a39bbf", "#d0cbe6")),
    **ramp("lmn", ("#0f5c7a", "#1bb38a", "#73ffb9")), **ramp("uvw", ("#432a94", "#a82ce3", "#ea84f5")),
    **ramp("xyz", ("#a94a08", "#ff8b04", "#ffd84a")), "W": "#ffffff",
    # ruoli del T8 per le facce non ridisegnate
    **ramp("abcdef", ("#1a0838", "#432a94", "#6c1ce1", "#a82ce3", "#ea84f5", "#fbd8fb")),
    **ramp("ABCDEF", ("#08202b", "#0f5c7a", "#178f9a", "#1bb38a", "#73ffb9", "#dcfff0")),
    **ramp("qrst", ("#2a1a50", "#6c4ad0", "#c0b0ff", "#ffffff")),
    **ramp("ghijk", ("#0d0b1a", "#16132b", "#221d40", "#302957", "#43397a"))},
    "icons": {
        "sword": G16("""
..............00 .............0n0 ............0nm0 ...........0nml0 ..........0nml0. .........0nml0..
........0wml0... .......0wvl0.... ..00..0wvu0..... ..0z00wvu0...... ...0zyvu0....... ....0yxyz0......
...020x0zy0..... ..020...0x0..... .0z0.....0...... .00............."""),
        "axe": G16("""
................ ...0000......... ..0Wnnm0........ .0nnmmml0...00.. .0nmmml0...0zy0. .0nmml0w0.0zyx0.
.0nml0wv00yx00.. ..0m0wvu0x00.... .....0vu0130.... ......00130..... ......0130...... .....0130.......
....0130........ ...0130......... ..0z30.......... ..000..........."""),
        "helmet": G16("""
................ ....0....0...0.. ...0n0..0w0.0z0. ...0m00000v0yx0. ..0nm5566vu4y30. ..0m55665544330.
..05ml0000yx320. ..04m0....0y310. ..0430....03210. ..0320....02210. ..0000....00000. ................
................ ................ ................ ................"""),
        "chestplate": G16("""
................ .0n0........0z0. 0nm40......04yz0 0ml5400000045yx0 00l4543ww3454x00 .00454wvvw45400.
..0443vuuv3440.. ..04433uu33440.. ..0344l33y3430.. ..034433333430.. ..0nml0wv0zyx0.. ..023344433220..
..022334433220.. ..012233322110.. ..000000000000.. ................"""),
    },
    "faces": {
        ("head_out", "north"): "5n55w55z54 4nm4wv4zy3 4ml3vu3yx3 3443443432 0000000000 4........2 30......02 30......01 20......01 1n......x0",
        ("head_out", "south"): "4555555543 4554w55433 4440vw0332 443wvuv322 3330uu0222 3333333222 3322l22211 322lml2211 2211l11110 1100000000",
        ("body_out", "north"): "5400000045 450wwvw054 440vvuv044 4n30vu034y nm44334yx3 ml45566543 3445445433 2234443322 0nm0vu0yx0 3445115443",
        ("body_out", "south"): "5455555545 4345665434 3344554433 4334ww4334 343wvvw343 3450uu0543 3445005433 2234444322 0nm0vu0yx0 3445115443",
        ("right_arm", "north"): "55654 5n543 nml33 4l432 43332 33322 33322 43322 0nml0 3mm22 32221 22211 21110",
        ("right_arm", "south"): "45554 44543 34433 33432 23322 33322 33322 33222 0nml0 32221 22211 22111 11100",
        ("left_arm", "north"): "55654 545z3 44yx3 443x2 43332 33322 33322 43322 0zyx0 33y22 32221 22211 21110",
        ("left_arm", "south"): "45554 44543 34433 33432 23322 33322 33322 33222 0zyx0 32221 22211 22111 11100",
        ("right_leg", "north"): "44443 44433 43332 4n332 nml22 3l322 33221 32221 22211 21110",
        ("right_leg", "south"): "44333 43332 43322 33222 33222 32222 32221 32211 22211 21110",
        ("left_leg", "north"): "34444 33445 23344 23w34 2wvu3 23u33 12233 12223 12223 01122",
        ("left_leg", "south"): "34443 33444 23344 22334 22334 23334 22233 12233 12223 01122",
        ("right_boot", "north"): "4n543 nml32 01110", ("right_boot", "south"): "34554 23322 01110",
        ("left_boot", "north"): "45z43 4zyx2 01110", ("left_boot", "south"): "34554 23322 01110",
        ("cape", "north"): """34455554433 ghhhhwhhhhg ghhhwvwhhhg ghhhvvuhhhg ghhhhuhhhhg ghihhhhhihg ghihhhhhihg ghinhhhzihg
            ghnmlhzyxhg ghimhhhyihg ghilhhhxihg ghihhhhhihg ghihhjhhihg ghihhjhhihg ghihhihhihg ghihhihhihg ghjhhihhjhg
            ghihhihhihg ghihhihhihg ghihhghhihg ghhhhghhhhg gghhgghhhgg gnggvggyggg""",
        ("front_cloth", "north"): "3444443 ghhwhhg ghwvwhg ghhuhhg ghihihg ghihihg ghnhzhg ghmhyhg ghlhxhg ghihihg ghihihg ghhhhhg gghhhgg ggggggg",
    }}

# ---------------------------------------------------------------------------------------------------------------------
# 07 LAMELLE: armatura a placche sovrapposte; ogni placca e' una lega (riga chiara in alto = bordo che prende luce),
#   le placche a destra e a sinistra della cucitura sono sfalsate di una riga. Acciaio grigio-lilla per le montature.
#   l m n verde acqua | u v w viola | x y z oro | 0-6 acciaio
# ---------------------------------------------------------------------------------------------------------------------
P7 = {"nome": "Lamelle", "pal": {
    **ramp("0123456", ("#1a1420", "#2e2a3a", "#464156", "#635d76", "#847e98", "#aaa5bc", "#d6d3e2")),
    **ramp("lmn", ("#0e6470", "#1bb38a", "#8affc6")), **ramp("uvw", ("#5a2a9e", "#a03ae0", "#f0a0ff")),
    **ramp("xyz", ("#b05a0c", "#f2a61d", "#ffe27a")), "W": "#ffffff",
    **ramp("abcdef", ("#1e0b3a", "#5a2a9e", "#7d32c0", "#a03ae0", "#c86cf0", "#f0a0ff")),
    **ramp("ABCDEF", ("#3a1d04", "#b05a0c", "#d98a14", "#f2a61d", "#ffd04a", "#ffe27a")),
    **ramp("qrst", ("#0b3a40", "#1bb38a", "#8affc6", "#ffffff")),
    **ramp("ghijk", ("#141019", "#1f1a27", "#2c2538", "#3b324b", "#4d4262"))},
    "icons": {
        "sword": G16("""
.............00. ............0nn0 ...........0nmn0 ..........0nmml0 .........0wwml0. ........0wvvl0..
.......0wvvu0... ..0...0zzvu0.... .050.0zyyu0..... ..050zyyx0...... ...05yyx0....... ...0333x0.......
..031030........ .0310.030....... 0y10...0........ yz0............."""),
        "axe": G16("""
................ ...00000000.0z0. ..0nnnnnn440130. .0nmmmmll40130.. 0wwwwwvu40130... 0vvvvuu40130....
0zzzzy40130..... .0yyx40130...... ..0000130....... ....0130........ ...0130......... ..0130..........
.0130........... 0130............ yz0............. 00.............."""),
        "helmet": G16("""
................ ................ ....000000000... ...0wwwwwww50... ..0vvv6vvvvvu0.. ..0nnnnnnnnnl0..
..0mm000000ml0.. ..0zz0....0yx0.. ..0yy0....0zx0.. ..0zz0....0yx0.. ..0yx0....0yx0.. ..0zz0....0000..
..0000.......... ................ ................ ................"""),
        "chestplate": G16("""
................ ................ .0000......0000. 0nnnn0....0zzzz0 0mmml0wwww0yyyx0 .0ll0vvvvvv0xx0.
..0nnnn5mmmml0.. ..0mmml3wwwww0.. ..0wwww5vvvvu0.. ..0vvvu3zzzzz0.. ..0zzzz5yyyyx0.. ..0yyyx3nnnnn0..
...0nnn5mmml0... ...0mml3wwwu0... ...0000000000... ................"""),
    },
    "faces": {
        ("head_out", "north"): "wwwww5wwww vvvvvvvvvu nnnnnnnnnl mmmlmmmmml 0000000000 z........y y0......0z z0......0y y0......0x x0......00",
        ("head_out", "south"): "wwwww5vvvu vvvvv3nnnl nnnnn5mmml mmmml3zzzz zzzzz5yyyx yyyyx3wwww wwwww5vvvu vvvvu3nnnl nnnnn5mmml mmmml3llll",
        ("body_out", "north"): "5400000045 nnnn5mmmml mmml3wwwww wwww5vvvvu vvvu3zzzzz zzzz5yyyyx yyyx3nnnnn nnnn5mmmml 2y2222y222 3445115443",
        ("body_out", "south"): "zzzzz5wwww yyyyx3vvvu wwwww5nnnn vvvvu3mmml nnnnn5zzzz mmmml3yyyx zzzzz5wwww yyyyx3vvvu 2y2222y222 3445115443",
        ("right_arm", "north"): "nnnnn mmmml wwwww vvvvu 43332 33322 33322 43322 zzzzz yyyyx zzzzz 22211 21110",
        ("right_arm", "south"): "nnnnn mmmml wwwww vvvvu 23322 33322 33322 33222 zzzzz yyyyx zzzzz 22111 11100",
        ("left_arm", "north"): "zzzzz yyyyx nnnnn mmmml 43332 33322 33322 43322 wwwww vvvvu wwwww 22211 21110",
        ("left_arm", "south"): "zzzzz yyyyx nnnnn mmmml 23322 33322 33322 33222 wwwww vvvvu wwwww 22111 11100",
        ("right_leg", "north"): "wwwww vvvvu nnnnn mmmml 43332 zzzzz yyyyx 32221 22211 21110",
        ("right_leg", "south"): "wwwww vvvvu nnnnn mmmml 33322 33322 33221 32211 22211 21110",
        ("left_leg", "north"): "zzzzz yyyyx wwwww vvvvu 43332 nnnnn mmmml 32221 22211 21110",
        ("left_leg", "south"): "zzzzz yyyyx wwwww vvvvu 33322 33322 33221 32211 22211 21110",
        ("right_boot", "north"): "nnnnn mmmml 01110", ("right_boot", "south"): "nnnnn mmmml 01110",
        ("left_boot", "north"): "zzzzz yyyyx 01110", ("left_boot", "south"): "zzzzz yyyyx 01110",
        ("cape", "north"): """wwwww5wwwwu vvvvv3vvvvu nnnnn5nnnnl mmmmm3mmmml zzzzz5zzzzx yyyyy3yyyyx ghhihhhihhg ghhihhhihhg
            ghhihhhihhg ghhihhhihhg ghhihjhihhg ghhihhhihhg ghhihhhihhg ghhihhhihhg ghhihhhijhg ghhihhhihhg ghhihhhihhg
            ghhihhhihhg ghhihhhihhg ghhhhhhhhhg ghhhhhhhhhg gghhgghhhgg ggggggggggg""",
        ("front_cloth", "north"): "4444443 nnnnnnl mmmmmml ghhihhg ghhihhg wwwwwwu vvvvvvu ghhihhg ghhihhg zzzzzzx yyyyyyx ghhihhg gghhhgg ggggggg",
    }}

# ---------------------------------------------------------------------------------------------------------------------
# 08 FORGIA: ferro battuto scuro e caldo, rivetti d'oro, intarsi di lega (verde acqua e viola che si incrociano).
#   0-6 ferro | x y z oro dei rivetti | l m n verde acqua | u v w viola
# ---------------------------------------------------------------------------------------------------------------------
P8 = {"nome": "Forgia", "pal": {
    **ramp("0123456", ("#140e0c", "#2a211d", "#3d322c", "#54473f", "#6b5d54", "#857668", "#b5a99e")),
    **ramp("lmn", ("#0f5c7a", "#1bb38a", "#73ffb9")), **ramp("uvw", ("#432a94", "#a82ce3", "#ea84f5")),
    **ramp("xyz", ("#a85a10", "#f2a61d", "#ffe27a")), "W": "#ffffff",
    **ramp("abcdef", ("#08202b", "#0f5c7a", "#178f9a", "#1bb38a", "#73ffb9", "#dcfff0")),
    **ramp("ABCDEF", ("#3a1d04", "#a85a10", "#d98a14", "#f2a61d", "#ffd04a", "#ffe27a")),
    **ramp("qrst", ("#1e0b3a", "#432a94", "#a82ce3", "#ea84f5")),
    **ramp("ghijk", ("#1f0d0c", "#2f1512", "#42201a", "#582c22", "#70392b"))},
    "icons": {
        "sword": G16("""
.............00. ............0650 ...........06n30 ..........06n430 .........06m430. ...0....05m430..
..0y0..06v430... ..045005v420.... ...045y6v420.... ....054y420..... .....043y0...... ....02400y0.....
...0420..0...... ..0240.......... .0zy0........... .0y0............"""),
        "axe": G16("""
....0000.....0.. ...0566500..030. ..0n66554y00310. .0n66655440310.. .0n655444y310... .0m5544y44310...
.0m544000310.... .0l540.0310..... .0l4y00310...... ..0l40310....... ...00310........ ...0310.........
..0310.......... .0310........... 0zy0............ .00............."""),
        "helmet": G16("""
................ .....000000..... ...006555440.... ..0y6655544y0... ..056655443320.. .0y56554433z20..
.055mmnmmvwv20.. .04400000000y0.. .044y40..03420.. .043m40..0v320.. .033m30..0v220.. .0y2l20..0u2y0..
..0000....000... ................ ................ ................"""),
        "chestplate": G16("""
................ .00000....00000. 0y55540..04555y0 0566540000455430 05654m4y4vw44330 .054m45554v4320.
.044m44y44v4320. .043l4444u43320. .0y4l4n4n443220. ..043m4m4m4320.. ..043y4444y320.. ..043554453320..
..033444333220.. ..0y2233322y0... ...000000000.... ................"""),
    },
    "faces": {
        ("head_out", "north"): "4566655443 5y665544y3 4555544433 4nmmvvwv43 4444444332 4........3 4433343332 33y3443y22 3333m33222 2222l22211",
        ("head_out", "south"): "4555555443 5y5565554y 4555555433 4445555433 4444444332 3344443322 3333l33322 33y3m33y22 2222m22211 1111n11110",
        ("body_out", "north"): "5400000045 45y5555y54 4n556655w3 44m5665v43 444m55v443 4y44mv44y3 3444ul4433 334u44l432 1y1111y111 3445115443",
        ("body_out", "south"): "4555555544 4y555555y4 4455665443 4445555433 4444444433 3y444444y3 3344444332 3334443322 1y1111y111 3445115443",
        ("right_arm", "north"): "5y665 56554 4n443 4m433 4l332 33322 33322 43322 0y0y0 3m322 32221 22211 21110",
        ("right_arm", "south"): "56y65 45554 34433 33432 23322 33322 33322 33222 0y0y0 32221 22211 22111 11100",
        ("left_arm", "north"): "566y5 55654 44w43 44v33 43u32 33322 33322 43322 0y0y0 33v22 32221 22211 21110",
        ("left_arm", "south"): "5y665 45554 34433 33432 23322 33322 33322 33222 0y0y0 32221 22211 22111 11100",
        ("right_leg", "north"): "44y43 44433 43332 33322 4nm42 3y3y2 33221 32221 22211 21110",
        ("right_leg", "south"): "44333 43332 43322 33222 33222 32222 32221 32211 22211 21110",
        ("left_leg", "north"): "4y443 44433 43332 33322 4vw42 3y3y2 33221 32221 22211 21110",
        ("left_leg", "south"): "44333 43332 43322 33222 33222 32222 32221 32211 22211 21110",
        ("right_boot", "north"): "5y553 4mn32 01110", ("right_boot", "south"): "45554 3y322 01110",
        ("left_boot", "north"): "55y53 4vw32 01110", ("left_boot", "south"): "45554 33y22 01110",
        ("cape", "north"): """4y44y44y443 33333333332 ghhhhhhhhhg ghhhhhhhhhg ghhhhyhhhhg ghhhnzvhhhg ghhnmyvwhhg ghhhmyuhhhg
            ghhhhxhhhhg ghhihhhhihg ghhihhhhihg ghhihhjhihg ghhihhhhihg ghhihhhhihg ghjihhhhihg ghhihhhhihg ghhihhhhijg
            ghhihhhhihg ghhihhhhihg ghhihhhhihg ghhhhhhhhhg gghhgghhhgg ggggggggggg""",
        ("front_cloth", "north"): "4y44y43 ghhhhhg ghhyhhg ghnzvhg ghhxhhg ghihihg ghihihg ghihihg ghihihg ghihihg ghihihg ghhhhhg gghhhgg ggggggg",
    }}

# ---------------------------------------------------------------------------------------------------------------------
# 09 RADICE: forme organiche color osso; le venature delle leghe (verde acqua a sinistra, viola a destra) salgono da un
#   seme d'oro, come radici. 0-6 osso | l m n verde acqua | u v w viola | x y z oro
# ---------------------------------------------------------------------------------------------------------------------
P9 = {"nome": "Radice", "pal": {
    **ramp("0123456", ("#2a1a14", "#4e3a2e", "#7a6450", "#a48e74", "#c8b597", "#e4d6bb", "#fbf3e0")),
    **ramp("lmn", ("#0f5c7a", "#1bb38a", "#73ffb9")), **ramp("uvw", ("#432a94", "#a82ce3", "#ea84f5")),
    **ramp("xyz", ("#cf5a13", "#ff8b04", "#ffc70c")), "W": "#ffffff",
    **ramp("abcdef", ("#08202b", "#0f5c7a", "#178f9a", "#1bb38a", "#73ffb9", "#dcfff0")),
    **ramp("ABCDEF", ("#3a1606", "#a92405", "#cf5a13", "#ff8b04", "#ffc70c", "#fff0a0")),
    **ramp("qrst", ("#1e0b3a", "#6c1ce1", "#a82ce3", "#ea84f5")),
    **ramp("ghijk", ("#121a12", "#1b261a", "#273524", "#354731", "#465d40"))},
    "icons": {
        "sword": G16("""
.............050 ............0560 ...........05n60 ..........046n50 .........04m6550 ........034m5540
.......034vm440. ...0..023v4n30.. ..0x0023v4430... .0xy023y4320.... ..0yzzy3320..... .0x2yz3200x0....
..03202y0x0..... .0320.0xx0...... 0320...00....... zy0............."""),
        "axe": G16("""
...00000.....00. ..04556600000320 .04n66554yyy320. 04n65000000320.. 4nm50....0320... 4m550...0320....
4l450000320..... 04m44zy320...... .04n45320....... ..000320........ ...0320......... ..0320..........
.0320........... 0320............ zy0............. 00.............."""),
        "helmet": G16("""
................ .....00000...... ....04555400.... ...04566554y0... ..0456n65544x0.. .04565m5544v30..
.0455m45443v320. .044m400004u320. .043l40..043220. .03340....0y220. .03y0.....02z0.. ..0y0......00...
...0............ ................ ................ ................"""),
        "chestplate": G16("""
................ .0000......0000. 045540....045540 4566540000456653 4565543zy3455543 04m5543zy3455v30
.02m222yz222v20. .045555y34555v0. .02l222zy222w20. ..04555yz55540.. ..02m22zy22v20.. ..0455y3y45540..
...02222y2220... ....00000000.... ................ ................"""),
    },
    "faces": {
        ("head_out", "north"): "4556655443 456n655443 45m5554v43 4m45544v32 0l00000u00 4........3 30......0y y0......02 20......0x x0......00",
        ("head_out", "south"): "4556655443 455665n543 4455m55433 444m455v32 3343m4v332 3334mzv322 3322yy3221 3222z32211 221y221110 1100x00000",
        ("body_out", "north"): "5400000045 4554yz4554 2m22zy22v2 45554y5554 2l22yz22w2 4555zy5554 22m2yz2v22 3455zy5543 2222yy2222 3445xx5443",
        ("body_out", "south"): "4555yz5554 3455zy5543 2222yz2222 3455zy5543 22m2yz2v22 3455zy5543 2222yz2222 3445zy5443 2222yy2222 3445xx5443",
        ("right_arm", "north"): "45554 4n543 45m43 44m33 43l32 33322 33322 43322 4mm42 3n322 32221 22211 21110",
        ("right_arm", "south"): "45554 44543 34433 33432 23322 33322 33322 33222 43342 32221 22211 22111 11100",
        ("left_arm", "north"): "45554 45w43 44v43 4v433 4u332 33322 33322 43322 4vv42 33w22 32221 22211 21110",
        ("left_arm", "south"): "45554 44543 34433 33432 23322 33322 33322 33222 43342 32221 22211 22111 11100",
        ("right_leg", "north"): "44443 4m433 43m32 33m22 33l22 43322 33m21 32n21 22211 21110",
        ("right_leg", "south"): "44333 43332 43322 33222 33222 32222 32221 32211 22211 21110",
        ("left_leg", "north"): "34444 33v45 23v44 2v334 2u334 23334 22v33 12w33 12223 01122",
        ("left_leg", "south"): "34443 33444 23344 22334 22334 23334 22233 12233 12223 01122",
        ("right_boot", "north"): "45543 4mn32 01110", ("right_boot", "south"): "34554 23322 01110",
        ("left_boot", "north"): "45543 4vw32 01110", ("left_boot", "south"): "34554 23322 01110",
        ("cape", "north"): """45566554433 ghhhhhhhhhg ghnhhhhhhwg ghhmhhhhvhg ghhhmhhvhhg gnhhhmhvhwg ghmhhmzvvhg ghhmmhzhhhg
            ghhhhhzhhhg ghhhhhyhhhg ghhhhhzhhhg ghhhhhyhhhg ghhhhyhhhhg ghhhhyhhhhg ghhhhhyhhhg ghhihhyhihg ghhhhyhhhhg
            ghhhxyxhhhg ghhxhyhxhhg ghxhhxhhxhg ghhhhhhhhhg gghhgghhhgg ggggggggggg""",
        ("front_cloth", "north"): "4556543 ghhzhhg ghmyvhg gmhyhvg ghhzhhg ghhyhhg ghhzhhg ghhyhhg ghhzhhg ghxyxhg gxhyhxg ghhhhhg gghhhgg ggggggg",
    }}

# ---------------------------------------------------------------------------------------------------------------------
# 10 AUREOLA: metallo blu notte; anelli di luce dorati (aureole) e un raggio di spettro che li attraversa:
#   ciano, menta (vibranio), bianco, magenta, viola (unobtainium). 0-6 blu notte | x y z oro | 7 ciano 8 magenta
#   9 giallo m menta v viola
# ---------------------------------------------------------------------------------------------------------------------
P10 = {"nome": "Aureola", "pal": {
    **ramp("0123456", ("#0b0f24", "#151c3c", "#212b56", "#303e72", "#45578f", "#6176ad", "#8ea0cf")),
    **ramp("xyz", ("#b8650c", "#ffb21c", "#fff0a0")), "7": "#3fd8f2", "m": "#26de88", "v": "#a82ce3",
    "8": "#ee4fc6", "9": "#ffdf3a", "W": "#ffffff",
    **ramp("abcdef", ("#3a1d04", "#b8650c", "#e8900f", "#ffb21c", "#ffd860", "#fff0a0")),
    **ramp("ABCDEF", ("#06283a", "#0f5c7a", "#1f9ec0", "#3fd8f2", "#9ff0ff", "#e8fdff")),
    **ramp("qrst", ("#2a3a70", "#8fa8e0", "#e6f0ff", "#ffffff")),
    **ramp("ghijk", ("#070a1a", "#0d1230", "#141b45", "#1d275c", "#283676"))},
    "icons": {
        "sword": G16("""
.............00. ............07W0 ...........07Wv0 ..........0mW80. .........07zv0.. ....000.0mW80...
...0yyy07Wv0.... ..0y...yz80..... ..0y..Wzy0...... ..0y.Wz.y0...... ...0yyyy0....... ..012000........
.0210........... 0120............ zy0............. 00.............."""),
        "axe": G16("""
.....000000..00. .....07zyyyx210. ...07zyyyyx210.. ..0mzyyyyx210... .0Wzyyyyx210.... .0Wzyyyx210.....
.08zyyx210...... .0vyyx210....... ..0vx210........ ...00210........ ..0210.......... .0210...........
0210............ zy0............. 00.............. ................"""),
        "helmet": G16("""
....00000000.... ...0zyyyyyyx0... ..0y00000000y0.. ...0xyyyyyyx0... ....03555430.... ...0356655430...
..035667554320.. ..03568W954320.. ..0350000004320. ..034y0..0y320.. ..0330....0320.. ..0320....0220..
...00......00... ................ ................ ................"""),
        "chestplate": G16("""
................ .0000......0000. 035540....045530 3555430000345543 3565543yy3455543 0355y470080y5430
.034y0WzzW0y4320 .03y0zyyyyz0y320 .034y0WzzW0y4320 .03459400v554320 .03445y44y444320 ..0344455444320.
..0333444433220. ...02233332200.. ....00000000.... ................"""),
    },
    "faces": {
        ("head_out", "north"): "zyyyyyyyyx 4556655443 457mW8v443 4455555432 0000000000 4........2 30......02 y0......0y 20......01 10......00",
        ("head_out", "south"): "4555555543 455yyyy543 45y5555y43 4y555555y3 4y55W555y2 3y555555y2 33y4444y22 334yyyy322 2233333221 1100000000",
        ("body_out", "north"): "5400000045 47544yy845 447y55y844 4m4yWWy494 mm4yWWy499 44vy55y744 4v44yy4474 3445445433 zyyyyyyyyx 3445115443",
        ("body_out", "south"): "4555555544 4545665454 3344554433 4334554334 3434444343 3455665443 3445445433 2234444322 zyyyyyyyyx 3445115443",
        ("right_arm", "north"): "45654 4y543 y5y43 4y432 43332 33322 33322 43322 zyyyx 37322 32221 22211 21110",
        ("right_arm", "south"): "45554 44543 34433 33432 23322 33322 33322 33222 zyyyx 32221 22211 22111 11100",
        ("left_arm", "north"): "45654 45y43 4y5y3 44y32 43332 33322 33322 43322 zyyyx 33822 32221 22211 21110",
        ("left_arm", "south"): "45554 44543 34433 33432 23322 33322 33322 33222 zyyyx 32221 22211 22111 11100",
        ("right_leg", "north"): "44443 44433 43332 33322 zyyyx 37322 33221 32221 22211 21110",
        ("right_leg", "south"): "44333 43332 43322 33222 zyyyx 32222 32221 32211 22211 21110",
        ("left_leg", "north"): "34444 33445 23344 22334 zyyyx 23v33 12233 12223 12223 01122",
        ("left_leg", "south"): "34443 33444 23344 22334 zyyyx 23334 22233 12233 12223 01122",
        ("right_boot", "north"): "45543 zyyyx 01110", ("right_boot", "south"): "34554 zyyyx 01110",
        ("left_boot", "north"): "45543 zyyyx 01110", ("left_boot", "south"): "34554 zyyyx 01110",
        ("cape", "north"): """45555555543 ghhhhhhhhhg ghhyyyyyhhg ghyhhhhhyhg ghyhhWhhyhg ghyhh7hhyhg ghhyymyyhhg ghhhh9hhhhg
            ghhhh9hhhhg ghhhh8hhhhg ghhih8hhihg ghhihvhhihg ghhihvhhihg ghhihhhhihg ghhihhhhihg ghhihhhhihg ghhihhjhihg
            ghhihhhhihg ghhihhhhihg ghhihhhhihg ghhhhhhhhhg gghhgghhhgg zyyyyyyyyyx""",
        ("front_cloth", "north"): "4555543 ghhyhhg ghyhyhg ghhyhhg ghh7hhg ghhmhhg ghh9hhg ghh8hhg ghhvhhg ghhihhg ghhihhg ghhhhhg gghhhgg zyyyyyx",
    }}

NUOVE = [
    (6, "scheggia", "Scheggia", "sagome nuove",
     "Lame e creste fatte di schegge di cristallo, una per lega: verde acqua in punta, viola al centro, oro "
     "sull'elsa; montature di metallo ardesia.", lambda: nuova(P6),
     ["#3a3054", "#776d96", "#1bb38a", "#73ffb9", "#a82ce3", "#ea84f5", "#ff8b04", "#ffd84a"]),
    (7, "lamelle", "Lamelle", "sagome nuove",
     "Armatura a placche sovrapposte: ogni placca e' una lega con il bordo che prende luce, e le placche ai due "
     "lati della cucitura sono sfalsate.", lambda: nuova(P7),
     ["#635d76", "#aaa5bc", "#1bb38a", "#8affc6", "#a03ae0", "#f0a0ff", "#f2a61d", "#ffe27a"]),
    (8, "forgia", "Forgia", "sagome nuove",
     "Ferro battuto pesante e scuro, rivetti d'oro e intarsi di lega verde acqua e viola che si incrociano.",
     lambda: nuova(P8),
     ["#3d322c", "#6b5d54", "#b5a99e", "#f2a61d", "#ffe27a", "#1bb38a", "#a82ce3"]),
    (9, "radice", "Radice", "sagome nuove",
     "Forme organiche color osso, attraversate da venature di lega che salgono da un seme d'oro come radici: "
     "verde acqua da una parte, viola dall'altra.", lambda: nuova(P9),
     ["#a48e74", "#e4d6bb", "#1bb38a", "#73ffb9", "#a82ce3", "#ff8b04", "#ffc70c"]),
    (10, "aureola", "Aureola", "sagome nuove",
     "Metallo blu notte con anelli di luce dorati e un raggio di spettro (ciano, menta, bianco, magenta, viola) "
     "che li attraversa.", lambda: nuova(P10),
     ["#212b56", "#45578f", "#ffb21c", "#fff0a0", "#3fd8f2", "#26de88", "#ee4fc6", "#a82ce3"]),
]

PROPOSTE = [
    (1, "spettro", "Spettro", "evolutiva",
     "Il passo dopo il T8: corpo di madreperla, taglio in lega a tre fasce (VA, UA, UV), l'unobtainium del T8 che "
     "cola in puntini viola e gemme con tutti e cinque i colori di Spectrum.", evolutiva,
     ["#958aa9", "#dbc9da", "#1bb38a", "#ffb21c", "#8f2fe0", "#7e35c3", "#3fd8f2", "#ee4fc6", "#ffdf3a"]),
    (2, "notte-iridata", "Notte Iridata", "stesse sagome",
     "Corpo nero come la notte; la lega scorre lungo il taglio in sei tinte, dall'oro al menta, e qualche stella "
     "bianca brilla sul metallo.", p02_notte_iridata,
     ["#221d31", "#ffc70c", "#ff8b04", "#e05ad8", "#6c1ce1", "#1bc0b0", "#26de88", "#e8f0ff"]),
    (3, "trittico", "Trittico", "stesse sagome",
     "Tre zone, tre leghe: in ogni zona il corpo prende il primo metallo della lega e il taglio il secondo, i puntini "
     "il colore che manca e la gemma un colore dello spettro.", p03_trittico,
     ["#337470", "#ff8b04", "#53398e", "#f2a61d", "#2c5590", "#6c1ce1", "#73ffb9", "#3fd8f2", "#ee4fc6", "#ffdf3a"]),
    (4, "opale", "Opale", "stesse sagome",
     "Corpo d'opale latteo con scaglie di fuoco colorato; il taglio e' iridescente (viola in ombra, verde acqua, "
     "oro nelle luci) e le gemme sono opali neri.", p04_opale,
     ["#c6cfe0", "#edf4f4", "#5b2ea8", "#2f9fb0", "#ffd27a", "#ff7ac8", "#3fe89a", "#3d6bd0"]),
    (5, "geode", "Geode", "stesse sagome",
     "Metallo viola scuro con il taglio verde acqua e oro; al posto della gemma un grande cristallo sfaccettato "
     "di Spectrum, bianco, ciano, magenta e giallo.", p05_geode,
     ["#352454", "#5e478c", "#1bb38a", "#ffc70c", "#5ce8ff", "#ff6ad8", "#ffe45c"]),
] + NUOVE


# ======================================================================================================================
# Panoramica
# ======================================================================================================================
def font(s, bold=False):
    p = "/usr/share/fonts/TTF/DejaVuSans%s.ttf" % ("-Bold" if bold else "")
    return ImageFont.truetype(p, s) if os.path.exists(p) else ImageFont.load_default(size=s)


def wrap(d, text, f, width):
    out, line = [], ""
    for w in text.split():
        t = (line + " " + w).strip()
        if d.textlength(t, font=f) > width and line:
            out.append(line)
            line = w
        else:
            line = t
    return out + [line]


def panoramica(rows, path):
    """Una riga per proposta: titolo e frase, le 4 icone x8 su slot #8b8b8b (sotto x2 su scuro e su slot), il manichino."""
    BG, SL, DK, TX, TI = (52, 52, 60), (139, 139, 139), (24, 22, 30), (215, 215, 222), (255, 214, 120)
    MS = 4
    man_w, man_h = 42 * MS, 36 * MS
    W = 16 + 4 * 138 + 12 + man_w + 16
    RH = 22 + 34 + max(172, man_h) + 18
    img = Image.new("RGB", (W, 50 + RH * len(rows)), BG)
    d = ImageDraw.Draw(img)
    d.text((16, 14), "T9 Alloy «Prismatic»: 10 proposte", fill=TI, font=font(20, True))
    y = 50
    for r in rows:
        d.text((16, y), f"{r['numero']:02d}  {r['nome']}  ·  {r['tipo']}", fill=TI, font=font(16, True))
        for k, ln in enumerate(wrap(d, r["frase"], font(12), W - 32)[:2]):
            d.text((16, y + 22 + k * 15), ln, fill=TX, font=font(12))
        yy = y + 22 + 34
        for i, n in enumerate(ICONS):
            im = Image.open(os.path.join(H.ROOT, r["file"][n])).convert("RGBA")
            x = 16 + i * 138
            d.rectangle((x, yy, x + 127, yy + 127), fill=SL)
            big = im.resize((128, 128), Image.NEAREST)
            img.paste(big, (x, yy), big)
            sm = im.resize((32, 32), Image.NEAREST)
            d.rectangle((x, yy + 134, x + 35, yy + 169), fill=DK)
            img.paste(sm, (x + 2, yy + 136), sm)
            d.rectangle((x + 40, yy + 134, x + 75, yy + 169), fill=SL)
            img.paste(sm, (x + 42, yy + 136), sm)
            d.text((x + 82, yy + 145), M.NOMI[n], fill=TX, font=font(11))
        tex = Image.open(os.path.join(H.ROOT, r["file"]["armor"])).convert("RGBA")
        man = H.on_bg(M.mannequin(tex, MS), MAN_BG)
        img.paste(man.convert("RGB"), (16 + 4 * 138 + 12, yy))
        y += RH
    img.save(path)


def main():
    os.makedirs(OUT, exist_ok=True)
    rows = []
    for num, slug, nome, tipo, frase, fn, palette in PROPOSTE:
        icons, arm = fn()
        if tipo in ("evolutiva", "stesse sagome"):      # sagome (alpha) identiche al T8, quindi a T6/T7
            for n in ICONS:
                assert sil(icons[n]) == sil(Image.open(os.path.join(T8DIR, "set", f"t8_{n}.png")).convert("RGBA")), n
            assert sil(arm) == sil(Image.open(os.path.join(T8DIR, "armatura", "t8_armor.png")).convert("RGBA"))
        d = save_proposal(num, slug, icons, arm)
        rel = os.path.relpath(d, H.ROOT)
        files = {n: os.path.join(rel, f"t9_{n}.png") for n in ICONS}
        files["armor"] = os.path.join(rel, "t9_armor.png")
        files["manichino"] = os.path.join(rel, "manichino.png")
        rows.append({"numero": num, "nome": nome, "frase": frase, "tipo": tipo, "palette": palette, "file": files})
        print(f"ok {num:02d} {nome}")
    with open(os.path.join(OUT, "proposte.json"), "w") as fh:
        json.dump(rows, fh, ensure_ascii=False, indent=2)
    panoramica(rows, os.path.join(OUT, "panoramica.png"))
    print("panoramica:", os.path.join(OUT, "panoramica.png"))


if __name__ == "__main__":
    main()
