"""T9 Alloy «Prismatic», GIRO 3: 6 proposte ANIMATE (spada, ascia, elmo, corazza 16x16 + armatura indossata 128x128).

Il giro 1 e il giro 2 sono stati bocciati: erano ricolorazioni ferme, scure e piene di colori sparsi. Qui il centro e'
l'ANIMAZIONE, sullo schema del T6 (texture_t6castone.py): 20 fotogrammi da 5 tick, fotogramma 0 = icona ferma.

DUE MOVIMENTI
  1. GEMMA CHE CAMBIA. Ogni oggetto ha UN castone (una posizione sola, disegnata a mano: contorno q, scuro r, medio s,
     riflesso t, montatura G/H). Nel ciclo le 5 gemme di Spectrum si danno il cambio, 4 fotogrammi ciascuna:
         f0-3 topazio (ciano)   f4-7 ametista (magenta)   f8-11 citrino (giallo)   f12-15 onice (nero)   f16-19 moonstone
     Il 4o fotogramma di ogni gemma e' il passaggio alla successiva, in due modi (per proposta):
         «dissolvenza» meta' dei pixel (a scacchiera) e' gia' la gemma dopo;   «lampo» la gemma diventa bianca e luminosa.
     Sull'armatura indossata tutti i castoni (elmo, petto, gamba, stivale, mantello, perizoma) cambiano insieme.
  2. SPETTRO IN MOVIMENTO. I pixel prismatici (lettere a-f: a contorno, b ombra, c medio, d luce, e chiaro, f pallido)
     prendono, a ogni fotogramma, la tinta della loro POSIZIONE lungo l'oggetto: s = dx*x + dy*y (direzione per pezzo,
     sempre verso la punta o verso l'alto: lo spettro SALE, «Ascension»). Il ciclo torna su se stesso in 20 fotogrammi.
         «flusso»   arcobaleno continuo (6 tinte) che scorre: ogni tinta e' una banda larga P/6
         «fascio»   cristallo fumé scuro; un fascio di luce a 6 tinte, largo Wb, lo percorre (con un alone di 1 tono)
         «giro»     l'oggetto intero cambia tinta nel tempo (6 tinte in 20 fotogrammi), con una leggera sfumatura
     Le tinte sono rampe a mano in stile «gemma di Spectrum» (riferimento: spectral_shard, topaz/citrine/moonstone del
     jar, solo per lo stile): facce chiare pastello, ombre COLORATE (non grigie), contorno della stessa tinta, W bianco.
  Il disegno (quali pixel sono corpo, prisma, gemma, montatura, quale tono hanno) e' tutto a mano nelle griglie qui
  sotto; il codice calcola solo il colore della tinta in base a posizione e tempo.

LE DUE FAMIGLIE
  Culmine (C1-C3): griglie CG_* = sagome del T8 (alpha identico, asserzione su tutti i fotogrammi). Il salto: il taglio
     d'unobtainium diventa cristallo prismatico (la lama intera, gli spallacci, la cintura), i puntini di vibranio si
     sciolgono nel metallo, il moonstone diventa il castone delle 5 gemme.
  Evolute (E1-E3): le griglie del culmine + una griglia di AGGIUNTE a mano (pixel nuovi fuori dalla sagoma del T8):
     E1 cristalli che sporgono (drusa), E2 punte, E3 ali e raggi. La sagoma e' costante in tutti i fotogrammi.
  Armatura: la griglia dei ruoli del T8 (t8_armor), il taglio a-f diventa prisma, i puntini A-F metallo, i castoni
     ingranditi con un timbro a mano attorno a ogni riflesso t del T8; le evolute aggiungono grappoli di cristallo a mano.

Uso:  python3 -I tools/texture_t9_giro3.py   # scrive design/texture/proposte/t9-proposte/giro3/ (legge t8-argent/)
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_mix as M  # noqa: E402
import texture_t9_proposte as P  # noqa: E402   giro 1 (solo lettura: lettura dei ruoli del T8, WHERE, put_faces)

OUT = os.path.join(P.OUT, "giro3")
ICONS = P.ICONS
FRAMES, FRAMETIME = 20, 15
SLOT = (139, 139, 139, 255)            # #8b8b8b
MAN_BG = P.MAN_BG
rgb = P.rgb


def G16(text):
    rows = text.split()
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [(i, r, len(r)) for i, r in enumerate(rows)]
    return rows


# ======================================================================================================================
# CULMINE: griglie a mano, stessa sagoma del T8
#   0-6 corpo   a-f prisma   W riflesso   G H montatura   q r s t gemma   '.' vuoto
# ======================================================================================================================
CG = {
    # spada: lama intera di cristallo (contorno a colorato, faccia alta chiara, un riflesso W in punta e uno a meta');
    # castone grande nella guardia, montatura chiara in alto a sinistra e scura in basso a destra; punte della guardia
    # e pomolo di cristallo
    "sword": G16("""
        .............aaa
        ............aeWa
        ...........adefa
        ..........acdefa
        .........abdeda.
        ....aa..abcWca..
        ...adG00bcdba...
        ..0GqqG03bca....
        ..0qtssqGba.....
        .0GqssrqH0......
        ..0GqrqGH00.....
        ...040H5c0......
        ..040.0b0.......
        .0c0...a........
        adc.............
        ab..............
    """),
    # ascia: la mezzaluna del filo e' cristallo (luce in alto, ombra sul bordo), il dorso resta metallo;
    # castone obliquo sull'attacco del manico
    "axe": G16("""
        ................
        .........aa.....
        ........aeWa....
        .......acefd00..
        ......abdec420..
        ......abcd5310..
        ......abc43110..
        ........0qq110..
        .......qts.100..
        ......qsr..00...
        .....GqH........
        ....040.........
        ...040..........
        ..040...........
        ..adc...........
        ..ab............
    """),
    # elmo: cresta di cristallo che sale in diagonale da sinistra, fascia della fronte di cristallo, castone a destra
    # (fuori asse), guanciale destro di cristallo, sinistro di metallo
    "helmet": G16("""
        ................
        ................
        .....000000.....
        ...0054cdWGG0...
        ..0543bcdGqqG0..
        ..043bdecqtsq0..
        ..0cbabcdqsrq0..
        ..041......HG0..
        ..042......2d0..
        ..0432....2cd0..
        ..0432....2bc0..
        ...000....000...
        ................
        ................
        ................
        ................
    """),
    # corazza: spallacci di cristallo (il sinistro con il riflesso W), castone al centro del petto (6 pixel di gemma),
    # cintura di cristallo, una scintilla d sul fianco destro
    "chestplate": G16("""
        ................
        ................
        aaaaa.0GG0.aaaaa
        adeWcaGqqH0bdeda
        acdb3GqtsqH3cdba
        05434qssrqH3340.
        .0445GqrqH45440.
        .03343444434330.
        ..034556654430..
        ..034554454330..
        ..022bcddcb220..
        ..022acddca220..
        ..03455115d430..
        ..033441144330..
        ..022331133220..
        ..000000000000..
    """),
}

# ======================================================================================================================
# EVOLUTE: aggiunte a mano sopra le griglie del culmine ('.' = invariato; ogni altra lettera sostituisce o aggiunge)
# ======================================================================================================================
ADD = {
    # E1 DRUSA: cristalli che spuntano dal metallo, a grappoli di 2-3 punte di altezza diversa
    "E1": {
        "sword": G16("""
            ..........a.....
            ..........ad....
            .....a.....e....
            ....aea.a.......
            ....adbae.......
            ...acbaa........
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
        """),
        "axe": G16("""
            ........a.......
            ........ad......
            ................
            ..............a.
            .............dea
            ..............a.
            .....a..........
            .....ac.........
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
        """),
        "helmet": G16("""
            .......a........
            ......aea..a....
            ......bdc.aea...
            ...........d....
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
        """),
        "chestplate": G16("""
            .a............a.
            aeaa.........ada
            .d.e..........c.
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
        """),
    },
    # E2 ACULEI: spine di cristallo che puntano IN ALTO (o in fuori sul dorso), 2-3 pixel collegati di lato alla
    # sagoma: base chiara d, punta di contorno a. Di altezze diverse.
    "E2": {
        "sword": G16("""
            ..........a.....
            ..........d.....
            ..........b.....
            ....a...a.......
            ....d...d.......
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ........d.......
            ........a.......
            ................
        """),
        "axe": G16("""
            .........a......
            .........d......
            ................
            ................
            ..............da
            ................
            ..............da
            ......d.........
            ......a.........
            ................
            ................
            ................
            ................
            ................
            ................
            ................
        """),
        "helmet": G16("""
            ...........a....
            ....a...a..c....
            ....d...d..d....
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
        """),
        "chestplate": G16("""
            .a..a.........a.
            .d..d......a..d.
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
        """),
    },
    # E3 ASCESA: ali (penne chiare e/d con contorno a) e raggi di luce
    "E3": {
        "sword": G16("""
            ................
            ................
            ................
            .aa.............
            aeda............
            .aeda...........
            ................
            ................
            ................
            ................
            ................
            ..........a.....
            .........dea....
            ........aea.....
            .........a......
            ................
        """),
        "axe": G16("""
            .........a......
            ................
            .............aa.
            ..............ea
            ..............da
            ..............ea
            ..............a.
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
        """),
        "helmet": G16("""
            ...............a
            a..............a
            ea............ae
            dea..........aed
            ad............da
            .a............a.
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
        """),
        "chestplate": G16("""
            a......a.......a
            ea....ada.....ae
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
        """),
    },
}


def overlay(base, add):
    return ["".join(a if a != "." else b for a, b in zip(rb, ra)) for rb, ra in zip(base, add)]


# ======================================================================================================================
# ARMATURA: ruoli del T8 -> lettere del giro 3, castoni ingranditi a mano, cristalli delle evolute a mano
# ======================================================================================================================
DOT_TO_BODY = dict(zip("ABCDEF", "123456"))   # i puntini di vibranio del T8 si sciolgono nel metallo


def castone_stamp(g, tx, ty):
    """Timbro a mano attorno al riflesso t del T8 (schema 'dtc / dsssb / abcba'):
           q t q
         q s s r q
             q            (solo sui pixel di prisma o gemma: il corpo resta)"""
    stamp = {(-1, 0): "q", (0, 0): "t", (1, 0): "q",
             (-2, 1): "q", (-1, 1): "s", (0, 1): "s", (1, 1): "r", (2, 1): "q",
             (0, 2): "q"}
    for (dx, dy), ch in stamp.items():
        x, y = tx + dx, ty + dy
        if 0 <= y < len(g) and 0 <= x < len(g[0]) and g[y][x] in "abcdefqrst" and P.WHERE.get((x, y), (None,))[0] == P.WHERE[(tx, ty)][0]:
            g[y][x] = ch


# petto (body_out north): castone grande nella stella del T8 ('bcdb / adeeda / afsqfa / acddca')
CHEST_STAMP = {(3, 0): "G", (4, 0): "q", (5, 0): "q", (6, 0): "G",
               (2, 1): "G", (3, 1): "q", (4, 1): "t", (5, 1): "s", (6, 1): "q", (7, 1): "H",
               (3, 2): "q", (4, 2): "s", (5, 2): "r", (6, 2): "q",
               (3, 3): "G", (4, 3): "q", (5, 3): "q", (6, 3): "H"}


def armor_culmine():
    g = [list(r) for r in P.t8_armor()]
    fx, fy, fw, fh = M.cube_faces(*M.CUBES["body_out"])["north"]
    ts = [(x, y) for y, r in enumerate(g) for x, ch in enumerate(r) if ch == "t"]
    for y, r in enumerate(g):
        for x, ch in enumerate(r):
            if ch in DOT_TO_BODY:
                g[y][x] = DOT_TO_BODY[ch]
    for tx, ty in ts:
        if not (fx <= tx < fx + fw and fy <= ty < fy + fh):
            castone_stamp(g, tx, ty)
    rows = ["".join(r) for r in g]
    rows = P.put_faces(rows, {("body_out", "north"): CHEST_STAMP})
    # riflessi bianchi a mano sugli orli di cristallo piu' larghi (uno per pezzo, fuori asse)
    rows = P.put_faces(rows, {("head_out", "west"): {(2, 3): "W"}, ("body_out", "north"): {(4, 7): "W"},
                              ("right_leg", "north"): {(1, 0): "W"}, ("cape", "north"): {(10, 5): "W"}})
    return rows


EVO_ARMOR = {   # grappoli di cristallo a mano (tutte le evolute)
    ("head_out", "north"): {(2, 0): "e", (3, 0): "d", (2, 1): "b", (8, 1): "e"},
    ("head_out", "top"): {(3, 4): "d", (4, 4): "e", (3, 5): "b", (4, 5): "c", (6, 2): "e", (7, 2): "d", (7, 3): "b"},
    ("right_arm_extra", "north"): {(1, 0): "d", (2, 0): "e", (3, 0): "W", (1, 1): "b", (2, 1): "c"},
    ("right_arm_extra", "top"): {(0, 0): "d", (1, 0): "e", (1, 1): "d", (2, 1): "c", (4, 3): "e", (4, 4): "c"},
    ("right_arm_extra", "south"): {(3, 0): "d", (2, 0): "c"},
    ("left_arm_extra2", "north"): {(1, 0): "d", (2, 0): "e", (4, 1): "c"},
    ("left_arm_extra2", "south"): {(1, 0): "e", (2, 0): "d", (2, 1): "b"},
    ("left_arm_extra2", "top"): {(4, 0): "e", (5, 0): "d", (4, 1): "c", (1, 3): "d", (2, 3): "b"},
    ("body_out", "north"): {(1, 5): "d", (2, 5): "e", (1, 6): "b", (8, 4): "c", (8, 5): "b"},
    ("body_out", "south"): {(7, 1): "e", (8, 1): "d", (8, 2): "b", (1, 5): "c"},
    ("right_arm", "north"): {(0, 0): "e", (1, 0): "d", (0, 1): "b"},
    ("left_arm", "north"): {(3, 0): "d", (4, 0): "e", (4, 1): "c"},
}
E3_ARMOR = {    # ali: penne a mano ai lati dell'elmo (davanti) e sul mantello (dietro)
    ("head_out", "north"): {(0, 0): "e", (0, 1): "d", (1, 1): "e", (0, 2): "c", (9, 1): "e", (9, 2): "d", (8, 2): "e"},
    ("cape", "north"): {(7, 1): "e", (8, 1): "d", (9, 1): "e", (7, 2): "c", (8, 2): "e", (9, 2): "d",
                        (8, 3): "c", (9, 3): "e", (9, 4): "d"},
    ("head_out", "south"): {(0, 1): "e", (0, 2): "d", (9, 1): "e", (9, 2): "c"},
}

# altezza nel mondo (per lo spettro che sale): y del cubo sul manichino; cubi non sul manichino a mano
CUBE_Y = {c: my for c, mx, my in M.MANNEQUIN}
CUBE_Y.update({"cape": 0.5, "right_arm_extra": -1.5, "left_arm_extra1": -1.0, "left_arm_extra2": -1.0})
CUBE_X = {c: mx for c, mx, my in M.MANNEQUIN}
CUBE_X.update({"cape": -5.5, "right_arm_extra": -8.5, "left_arm_extra1": 3.0, "left_arm_extra2": 3.0})


def armor_pos(x, y):
    w = P.WHERE.get((x, y))
    if not w:
        return 0.0, 0.0
    cube, face, fx, fy = w
    wy = CUBE_Y[cube] + (fy if face not in ("top", "bottom") else (0 if face == "top" else M.CUBES[cube][3]))
    wx = CUBE_X[cube] + fx
    return wx, wy


# ======================================================================================================================
# PALETTE (a mano)
# ======================================================================================================================
def ramp(*hexes):
    return [rgb(h) for h in hexes]


# 6 tinte, toni a (contorno) b (ombra) c (medio) d (luce) e (chiaro) f (pallido). Pastello, ombre colorate.
HUES_PASTEL = [
    ramp("#5a1e57", "#a8479a", "#e978d3", "#ff9ee8", "#ffcdf4", "#fff0fb"),   # rosa
    ramp("#5e2618", "#b85238", "#f58a66", "#ffb08c", "#ffd7c4", "#fff2ea"),   # pesca
    ramp("#574010", "#ad861d", "#e6c447", "#f6df78", "#fff2b6", "#fffcea"),   # giallo
    ramp("#1d461c", "#4a933c", "#94d06a", "#bce68e", "#e0f6c9", "#f5ffee"),   # verde
    ramp("#134650", "#2b93a0", "#68d6d2", "#98e9e4", "#cdf8f4", "#effffd"),   # ciano
    ramp("#272b70", "#5764c8", "#95a4fb", "#b6c2ff", "#dde3ff", "#f3f5ff"),   # pervinca
]
HUES_VIVID = [   # per i fondi scuri: stesse tinte, luci piu' accese
    ramp("#3a0838", "#9a1f8c", "#ff4fd8", "#ff8ee6", "#ffc6f3", "#fff0fb"),
    ramp("#3d1206", "#b23c14", "#ff7a3d", "#ffa66e", "#ffd5b8", "#fff2ea"),
    ramp("#3a2a04", "#a8820c", "#ffd83a", "#ffe873", "#fff5b0", "#fffcea"),
    ramp("#0c3010", "#2f9a30", "#6fe25a", "#a6f28a", "#d8fbc8", "#f5ffee"),
    ramp("#062f38", "#14929e", "#3fe6e0", "#86f3ee", "#c8fbf8", "#effffd"),
    ramp("#141858", "#3f4ee0", "#7d8cff", "#a9b4ff", "#d8ddff", "#f3f5ff"),
]
SMOKE = ramp("#0b0816", "#1b1534", "#2e2656", "#463c7c", "#6a5ea8", "#9d92d2")   # cristallo fume' (modo fascio)
TONE = "abcdef"

GEMS5 = [   # q contorno, r scuro, s medio, t riflesso (stile delle gemme di Spectrum)
    ("topazio", {"q": "#0a2c40", "r": "#1f7ea0", "s": "#4fd2f0", "t": "#d8fff4"}),
    ("ametista", {"q": "#43093e", "r": "#a02592", "s": "#f05ad2", "t": "#ffd2f6"}),
    ("citrino", {"q": "#4a2300", "r": "#c08406", "s": "#ffdc3c", "t": "#fffbd0"}),
    ("onice", {"q": "#000000", "r": "#0c0c22", "s": "#1f2260", "t": "#6a5cff"}),
    ("moonstone", {"q": "#3a3f58", "r": "#8a8cc0", "s": "#cfe0fb", "t": "#ffffff"}),
]
GEMS5 = [(n, {k: rgb(v) for k, v in d.items()}) for n, d in GEMS5]
FLASH = {"q": None, "r": rgb("#fff6d8"), "s": rgb("#ffffff"), "t": rgb("#ffffff")}   # q = riflesso della gemma

BODY = {
    "perla": ("#4f4470", "#7d77a3", "#a7a6c9", "#c4cbe3", "#e2dcef", "#f1f6f4", "#ffffff"),
    "notte": ("#050409", "#100d1c", "#1b172d", "#282240", "#383056", "#4c436f", "#6b6194"),
    "vetro": ("#3d3658", "#5f5a84", "#8481a8", "#a2a2c4", "#bfc2dc", "#dde0f0", "#f6f8ff"),
    "brina": ("#3f4f70", "#6c7ea0", "#9fb0cc", "#bfcde2", "#dbe6f2", "#eef6fb", "#ffffff"),
    "ossidiana": ("#040307", "#0f0b16", "#1a1424", "#271e36", "#372a4b", "#4b3a63", "#6a5688"),
    "aureo": ("#5a3a28", "#8f6a48", "#c09a6a", "#dcbf8c", "#efdcb0", "#fbf0d6", "#fffcf2"),
}
CLOTH = {
    "lilla": ("#2e2848", "#3f3760", "#544a7a", "#6b6194", "#8a80ae"),
    "notte": ("#0a0812", "#120f1e", "#1c182c", "#28223d", "#373050"),
    "brina": ("#22304a", "#2f405e", "#3f5476", "#536a8e", "#6c84a8"),
    "aureo": ("#4a2c40", "#5e3a52", "#764a66", "#8e5e7c", "#a87896"),
}
MOUNT = {
    "oro_chiaro": ("#fff3c8", "#c49a4a"), "oro_scuro": ("#e0a83a", "#6a4210"), "argento": ("#f4e7ff", "#8a7ab0"),
    "bianco": ("#ffffff", "#8fa4c8"), "bronzo": ("#c08a2a", "#4a2c08"), "oro": ("#ffd76a", "#a8701a"),
}

# direzione dello spettro per pezzo (s = dx*x + dy*y, cresce verso la punta / verso l'alto)
DIR = {"sword": (1, -1), "axe": (0.6, -1.4), "helmet": (0.7, -2), "chestplate": (0.5, -2)}
ARMOR_DIR = (0.4, -2)

PROPOSTE = [
    dict(cod="C1", slug="aurora", nome="Aurora", fam="culmine", tono="chiaro",
         frase="Il metallo del T8 diventa perla iridescente e la lama tutta cristallo pastello: un arcobaleno continuo "
               "sale dal manico alla punta; il castone passa da una gemma all'altra in dissolvenza.",
         body="perla", cloth="lilla", mount="oro_chiaro", hues=HUES_PASTEL, mode="flusso", P=24, Pa=40, trans="dissolvenza"),
    dict(cod="C2", slug="eclisse", nome="Eclisse", fam="culmine", tono="scuro",
         frase="Corpo nero e lama di cristallo fume': un fascio di luce a sei tinte la attraversa dal manico alla punta "
               "come luce dentro il vetro scuro; la gemma cambia con un lampo bianco.",
         body="notte", cloth="notte", mount="oro_scuro", hues=HUES_VIVID, mode="fascio", P=30, Wb=12, Pa=56, Wba=20,
         trans="lampo"),
    dict(cod="C3", slug="fusione", nome="Fusione", fam="culmine", tono="medio (libera)",
         frase="Tutto si fonde nella luce: anche il metallo (vetro di moonstone) prende la tinta dello spettro che "
               "scorre, piu' tenue del cristallo; l'oggetto intero e' un prisma che sale.",
         body="vetro", cloth="lilla", mount="argento", hues=HUES_PASTEL, mode="flusso", P=36, Pa=48, fuse=0.42,
         trans="lampo"),
    dict(cod="E1", slug="drusa", nome="Drusa", fam="evolute", tono="chiaro",
         frase="Sagome del T8 da cui spuntano cristalli a grappoli (guardia, dorso dell'ascia, cresta dell'elmo, "
               "spallacci); metallo di brina, arcobaleno pastello che sale; gemma in dissolvenza.",
         body="brina", cloth="brina", mount="bianco", hues=HUES_PASTEL, mode="flusso", P=24, Pa=40, trans="dissolvenza"),
    dict(cod="E2", slug="aculei", nome="Aculei", fam="evolute", tono="scuro",
         frase="Ossidiana con aculei di cristallo sottili verso l'esterno; un fascio di luce a sei tinte corre lungo "
               "lama e aculei; la gemma cambia con un lampo.",
         body="ossidiana", cloth="notte", mount="bronzo", hues=HUES_VIVID, mode="fascio", P=30, Wb=12, Pa=56, Wba=20,
         trans="lampo"),
    dict(cod="E3", slug="ascesa", nome="Ascesa", fam="evolute", tono="chiaro-caldo (libera)",
         frase="Metallo bianco-oro con piccole ali (guardia, dorso dell'ascia, elmo alato, spallacci) e un raggio sopra "
               "il castone; tutto il cristallo cambia tinta insieme, girando lo spettro in 20 fotogrammi.",
         body="aureo", cloth="aureo", mount="oro", hues=HUES_PASTEL, mode="giro", P=60, Pa=120, trans="lampo"),
]


# ======================================================================================================================
# COLORE di un pixel al fotogramma f
# ======================================================================================================================
def mix(c1, c2, a):
    return tuple(round(c1[i] * (1 - a) + c2[i] * a) for i in range(3))


def hue_at(prop, s, f, P_):
    nh = len(prop["hues"])
    if prop["mode"] == "giro":
        u = s / P_ - f / FRAMES
    else:
        u = (s - P_ * f / FRAMES) / P_
    return int(math.floor((u % 1.0) * nh)) % nh


def prism(prop, ch, s, f, armor=False):
    t = TONE.index(ch)
    hues = prop["hues"]
    if prop["mode"] != "fascio":
        return hues[hue_at(prop, s, f, prop["Pa"] if armor else prop["P"])][t]
    P_, Wb = (prop["Pa"], prop["Wba"]) if armor else (prop["P"], prop["Wb"])
    d = (s - P_ * f / FRAMES) % P_
    if d < Wb:      # dentro il fascio: tinta per posizione nel fascio, un tono piu' chiaro
        h = int(d / Wb * len(hues)) % len(hues)
        return hues[h][min(t + 1, 5)]
    if d < Wb + 2 or d > P_ - 2:     # alone
        return SMOKE[min(t + 1, 5)]
    return SMOKE[t]


def gem_color(ch, f, x, y, trans):
    slot, k = divmod(f, 4)
    cur = GEMS5[slot][1]
    nxt = GEMS5[(slot + 1) % 5][1]
    if k < 3:
        return cur[ch]
    if trans == "dissolvenza":
        return (nxt if (x + y) % 2 == 0 else cur)[ch]
    # lampo
    return cur["t"] if ch == "q" else FLASH[ch]


BODY_TONE = "abbcdef"      # tono del prisma che tinge il corpo, cifra per cifra (Fusione)


def color(prop, ch, x, y, f, s, armor=False):
    if ch in "0123456":
        c = rgb(BODY[prop["body"]][int(ch)])
        if prop.get("fuse"):
            c = mix(c, prism(prop, BODY_TONE[int(ch)], s, f, armor), prop["fuse"])
        return c
    if ch in TONE:
        return prism(prop, ch, s, f, armor)
    if ch in "ghijk":
        return rgb(CLOTH[prop["cloth"]]["ghijk".index(ch)])
    if ch in "GH":
        return rgb(MOUNT[prop["mount"]]["GH".index(ch)])
    if ch == "W":
        return (255, 255, 255)
    if ch in "qrst":
        return gem_color(ch, f, x, y, prop["trans"])
    raise KeyError(ch)


def render_frame(prop, grid, f, sfun, armor=False):
    h, w = len(grid), len(grid[0])
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = color(prop, ch, x, y, f, sfun(x, y), armor) + (255,)
    return im


def icon_grids(prop):
    if prop["fam"] == "culmine":
        return dict(CG)
    return {n: overlay(CG[n], ADD[prop["cod"]][n]) for n in ICONS}


def armor_grid(prop):
    g = armor_culmine()
    if prop["fam"] == "evolute":
        g = P.put_faces(g, EVO_ARMOR)
        if prop["cod"] == "E3":
            g = P.put_faces(g, E3_ARMOR)
    return g


# ======================================================================================================================
# USCITA
# ======================================================================================================================
def sil(im):
    return im.getchannel("A").tobytes()


def gif(frames, path, scale, bg, ms=FRAMETIME * 50):
    imgs = [H.on_bg(H.up(fr, scale), bg).convert("RGB") for fr in frames]
    imgs[0].save(path, save_all=True, append_images=imgs[1:], duration=ms, loop=0, disposal=1, optimize=False)


def man_gif(frames, path):
    imgs = [H.on_bg(M.mannequin(fr, 4), MAN_BG).convert("RGB") for fr in frames]
    imgs[0].save(path, save_all=True, append_images=imgs[1:], duration=FRAMETIME * 50, loop=0, disposal=1,
                 optimize=False)


MCMETA = '{\n  "animation": {\n    "frametime": %d,\n    "interpolate": false\n  }\n}\n' % FRAMETIME


def gem_pixels(grid):
    return [(x, y) for y, r in enumerate(grid) for x, ch in enumerate(r) if ch == "s"]


def build(prop):
    d = os.path.join(OUT, f"{prop['cod']}-{prop['slug']}")
    os.makedirs(d, exist_ok=True)
    grids = icon_grids(prop)
    res = {"icone": {}, "anim": {}, "gif": {}}
    checks = []
    for n in ICONS:
        g = grids[n]
        dx, dy = DIR[n]
        frames = [render_frame(prop, g, f, lambda x, y: dx * x + dy * y) for f in range(FRAMES)]
        strip = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
        for i, fr in enumerate(frames):
            strip.paste(fr, (0, 16 * i))
        icon = frames[0]
        icon.save(os.path.join(d, f"t9_{n}.png"))
        strip.save(os.path.join(d, f"t9_{n}_anim.png"))
        with open(os.path.join(d, f"t9_{n}_anim.png.mcmeta"), "w") as fh:
            fh.write(MCMETA)
        gif(frames, os.path.join(d, f"t9_{n}.gif"), 8, SLOT)
        # verifiche
        st = Image.open(os.path.join(d, f"t9_{n}_anim.png")).convert("RGBA")
        assert st.size == (16, 16 * FRAMES), n
        assert st.crop((0, 0, 16, 16)).tobytes() == Image.open(os.path.join(d, f"t9_{n}.png")).convert("RGBA").tobytes()
        t8 = Image.open(P.T8DIR + f"/set/t8_{n}.png").convert("RGBA")
        sils = {sil(fr) for fr in frames}
        assert len(sils) == 1, (prop["cod"], n, "sagoma non costante")
        if prop["fam"] == "culmine":
            assert sils == {sil(t8)}, (prop["cod"], n, "alpha diverso dal T8")
        gp = gem_pixels(g)
        seen = {st.getpixel((gp[0][0], gp[0][1] + 16 * f))[:3] for f in (0, 4, 8, 12, 16)}
        assert seen == {gm["s"] for _, gm in GEMS5}, (prop["cod"], n, "il castone non mostra le 5 gemme")
        res["icone"][n] = f"t9_{n}.png"
        res["anim"][n] = f"t9_{n}_anim.png"
        res["gif"][n] = f"t9_{n}.gif"
        res.setdefault("frames", {})[n] = frames
    checks.append("icone: 20 fotogrammi, f0 = icona, 5 gemme, sagoma costante" +
                  (", alpha = T8" if prop["fam"] == "culmine" else ""))
    # armatura
    ag = armor_grid(prop)
    t8a = Image.open(os.path.join(P.T8DIR, "armatura", "t8_armor.png")).convert("RGBA")
    adx, ady = ARMOR_DIR
    pos = {}

    def sa(x, y):
        if (x, y) not in pos:
            wx, wy = armor_pos(x, y)
            pos[(x, y)] = adx * wx + ady * wy
        return pos[(x, y)]
    aframes = [render_frame(prop, ag, f, sa, armor=True) for f in range(FRAMES)]
    for i, fr in enumerate(aframes):
        fr.save(os.path.join(d, f"t9_armor_{i}.png"))
        assert sil(fr) == sil(t8a), (prop["cod"], "alpha armatura diversa dal T8")
    aframes[0].save(os.path.join(d, "t9_armor.png"))
    gp = gem_pixels(ag)
    seen = {aframes[f].getpixel(gp[0])[:3] for f in (0, 4, 8, 12, 16)}
    assert seen == {gm["s"] for _, gm in GEMS5}
    man_gif(aframes, os.path.join(d, "manichino.gif"))
    checks.append("armatura: 20 fotogrammi, f0 = t9_armor.png, alpha = T8, 5 gemme")
    res["armor_frames"] = aframes
    res["dir"] = d
    res["checks"] = checks
    return res


def font(s, bold=False):
    p = "/usr/share/fonts/TTF/DejaVuSans%s.ttf" % ("-Bold" if bold else "")
    return ImageFont.truetype(p, s) if os.path.exists(p) else ImageFont.load_default()


def panoramica(results, path):
    BG, TX, TI = (44, 42, 52), (225, 225, 232), (255, 214, 120)
    k, ks = 6, 3
    cw = 16 * k + 8 + 5 * (16 * ks + 2) + 18
    man_w = 42 * 3
    W = 16 + 4 * cw + man_w * 2 + 30
    rowh = 16 * k + 70
    img = Image.new("RGB", (W, 60 + len(results) * rowh), BG)
    d = ImageDraw.Draw(img)
    d.text((16, 14), "T9 Prismatic, giro 3: fotogramma 0 + le 5 gemme (f0 f4 f8 f12 f16)", fill=TI, font=font(20, True))
    y = 56
    for prop, r in results:
        d.text((16, y), f"{prop['cod']} {prop['nome']}  ({prop['fam']}, {prop['tono']}, spettro: {prop['mode']}, "
                        f"cambio gemma: {prop['trans']})", fill=TI, font=font(15, True))
        yy = y + 24
        for i, n in enumerate(ICONS):
            x = 16 + i * cw
            fr = r["frames"][n]
            img.paste(H.on_bg(H.up(fr[0], k), SLOT).convert("RGB"), (x, yy))
            for j, f in enumerate((0, 4, 8, 12, 16)):
                img.paste(H.on_bg(H.up(fr[f], ks), SLOT).convert("RGB"), (x + 16 * k + 8 + j * (16 * ks + 2), yy))
            for j, f in enumerate((2, 7, 12, 17)):      # lo spettro in 4 momenti diversi, piu' piccoli
                img.paste(H.on_bg(H.up(fr[f], 2), SLOT).convert("RGB"), (x + 16 * k + 8 + j * 34, yy + 16 * ks + 6))
        x = 16 + 4 * cw
        for j, f in enumerate((0, 10)):
            man = H.on_bg(M.mannequin(r["armor_frames"][f], 3), MAN_BG).convert("RGB")
            img.paste(man, (x + j * (man_w + 6), yy - 6))
        y += rowh
    img.save(path)


def main():
    os.makedirs(OUT, exist_ok=True)
    results, meta = [], []
    for prop in PROPOSTE:
        r = build(prop)
        results.append((prop, r))
        rel = os.path.relpath(r["dir"], H.ROOT)
        meta.append({
            "numero": prop["cod"], "nome": prop["nome"], "frase": prop["frase"], "famiglia": prop["fam"],
            "tono": prop["tono"], "spettro": prop["mode"], "cambio_gemma": prop["trans"],
            "palette": {"corpo": list(BODY[prop["body"]]), "montatura": list(MOUNT[prop["mount"]]),
                        "stoffa": list(CLOTH[prop["cloth"]]),
                        "spettro": ["#%02x%02x%02x" % h[3] for h in prop["hues"]],
                        "gemme": {nm: "#%02x%02x%02x" % gm["s"] for nm, gm in GEMS5},
                        **({"fume": ["#%02x%02x%02x" % c for c in SMOKE]} if prop["mode"] == "fascio" else {})},
            "png": {n: f"{rel}/t9_{n}.png" for n in ICONS} | {"armatura": f"{rel}/t9_armor.png"},
            "anim": {n: f"{rel}/t9_{n}_anim.png" for n in ICONS},
            "gif": {n: f"{rel}/t9_{n}.gif" for n in ICONS} | {"manichino": f"{rel}/manichino.gif"},
            "verifiche": r["checks"],
        })
        print("ok", prop["cod"], prop["nome"])
    with open(os.path.join(OUT, "proposte.json"), "w") as fh:
        json.dump(meta, fh, ensure_ascii=False, indent=2)
    panoramica(results, os.path.join(OUT, "panoramica.png"))
    print("scritto", OUT)


if __name__ == "__main__":
    main()
