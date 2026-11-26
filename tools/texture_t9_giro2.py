"""T9 Alloy «Prismatic», GIRO 2: 4 varianti del set (armi + armatura), tutte sulle sagome del T8 (= T6/T7/T8).

Parte dai voti del giro 1 (design/texture/proposte/t9-proposte/voti_giro1.md): armi della 01 «Spettro» (evolutiva) e
armatura della 02 «Notte Iridata». Riusa gli strumenti di tools/texture_t9_proposte.py (lettura dei PNG del T8 come
griglie di ruoli, render, put, put_faces, mannequin) e le palette della 01 e della 02.

LE NOTE DELL'UTENTE E COME SONO APPLICATE
  1. corpo piu' scuro       -> niente madreperla: il corpo e' il nero della 02 (A, C), antracite (B), nero-prugna (D)
  2. taglio meno caotico    -> niente fasce: UNA lega dominante su tutto il taglio (a-f, ombre nel primo metallo, luci
                               nel secondo); le altre due leghe entrano solo come RIFLESSI: 1-3 pixel a mano (u v) del
                               metallo che manca alla lega dominante (UA -> vibranio, UV -> allthemodium, VA -> unobtainium)
  3. meno puntini viola     -> i puntini del T8 (ruoli A-F, sul taglio) diventano corpo; al loro posto pochi grappoli
                               di 1-3 pixel (P Q R, unobtainium chiaro) messi a mano SOLO su pixel di corpo
  4. gemme piu' grandi      -> castone ridisegnato a mano: ciano, magenta, giallo, nero, bianco, ognuno con 2 pixel
                               circa, in una montatura d'oro (G luce, H ombra). Il castone si allarga dentro la sagoma
                               (prende pixel di taglio e corpo vicini): l'alpha resta IDENTICO al T8 (asserzione)
  armatura                  -> base 02 (corpo nero), ma un'unica lega su tutti i bordi al posto delle 6 tinte, stelle
                               tolte, stesso castone delle armi (timbro sulle gemme del T8), riflessi e puntini a mano su
                               tutte le facce (davanti, dietro, fianchi, sopra), non solo davanti e dietro

LETTERE DELLE GRIGLIE
  0-6 corpo   a-f taglio (lega dominante)   u v riflesso (metallo mancante)   P Q R puntini (unobtainium)
  G H montatura del castone (oro)   K W 7 8 9 gemma: nero, bianco, ciano, magenta, giallo   g-k stoffa

VARIANTI
  A Notte Viola   corpo nero 02, lega UA (viola, luci d'oro), riflessi di vibranio
  B Antracite     come A ma corpo grigio antracite con riflesso viola (si stacca anche dal T7 nero-blu)
  C Notte Menta   come A ma lega UV (viola, luci verde menta), riflessi d'oro
  D Abisso        corpo nero-prugna (il viola passa al metallo), lega VA (verde acqua, luci d'oro), riflessi magenta

Uso:  python3 -I tools/texture_t9_giro2.py   # scrive design/texture/proposte/t9-proposte/giro2/ (legge t8-argent/ e 01/)
"""
import json
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_mix as M  # noqa: E402
import texture_t9_proposte as P  # noqa: E402   giro 1 (solo lettura: funzioni e palette)

OUT = os.path.join(P.OUT, "giro2")
ICONS = P.ICONS
T8_ICON = {n: os.path.join(P.T8DIR, "set", f"t8_{n}.png") for n in ICONS}
T8_ARMOR = os.path.join(P.T8DIR, "armatura", "t8_armor.png")


def G16(text):
    rows = text.split()
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    return rows


# ======================================================================================================================
# ICONE disegnate a mano (copia ritoccata delle griglie dei ruoli del T8: stessi pixel pieni, nuovo disegno dentro)
# ======================================================================================================================
ICON_GRIDS = {
    # spada: punta d'oro (e f), un riflesso di 2 pixel sul filo destro sotto la punta, due puntini sulla costola in
    # basso; il castone sulla guardia passa da 9 a 13 pixel di gemma (W in alto a sinistra, poi ciano, magenta,
    # giallo verso il basso a destra, due faccette nere in ombra) con la montatura d'oro.
    "sword": G16("""
.............000
............0fe0
...........0e6f0
..........0d5vf0
.........0cd4u0.
....00..0cQ3d0..
...05300bcPc0...
..05GWG032b0....
..0GW77H2b0.....
.0G78899H0......
..0HK89KHa0.....
...040bc50......
..040.0b0.......
.0c0...0........
adc.............
ab.............."""),
    # ascia: angolo alto d'oro, riflesso di 2 pixel sul dorso alto, un grappolo di 2 puntini sul dorso scuro;
    # castone sul collo da 3 a 7 pixel di gemma.
    "axe": G16("""
................
.........00.....
........0ef0....
.......0cdvu00..
......0bcd3220..
......0bc32Q10..
......0b221P10..
........021110..
.......GW9.100..
......789..00...
.....H8K........
....040.........
...040..........
..040...........
..adc...........
..ab............"""),
    # elmo: castone da 4 a 8 pixel di gemma sopra l'occhio destro, riflesso sul paraguance sinistro, due puntini
    # sulla calotta a sinistra e uno sulla guancia destra.
    "helmet": G16("""
................
................
.....000000.....
...005R54GWG0...
..0543Q437W8H0..
..0vub43G789H0..
..0cb0abHKH0b0..
..041......130..
..042......2P0..
..0432....2340..
..0432....2340..
...000....000...
................
................
................
................"""),
    # corazza: medaglione da 2 a 10 pixel di gemma, riflesso sulla spalla destra e sulla fibbia della cintura,
    # 2 grappoli di puntini (spalla sinistra, ventre a destra) al posto dei 9 puntini del T8.
    "chestplate": G16("""
................
................
00000.0cd0.00000
06e540bcdb046v60
055R3aGW7Ga34450
054Q4aW789a3340.
.0443aK89Ka3440.
.03343444434330.
..034556654430..
..03445445QP30..
..022bcdvcb220..
..022acduca220..
..034451154430..
..033441144330..
..022331133220..
..000000000000..
"""),
}


# ======================================================================================================================
# ARMATURA: griglia dei ruoli del T8 -> puntini A-F tolti (diventano corpo), castone timbrato su ogni gemma,
#           riflessi e puntini a mano su tutte le facce
# ======================================================================================================================
DOT_TO_BODY = dict(zip("ABCDEF", "123456"))


def castone(grid):
    """Timbro del castone sulle gemme del T8 (forma t / sss / abcba): stesso disegno e stessi colori delle armi.
         riga t:    G W G
         riga sss:  7 7 W 8 8     (prende anche i due pixel di taglio ai lati)
         riga sotto H 9 9 K H     (solo se sotto c'e' taglio: sugli stivali c'e' la suola e si salta)"""
    g = [list(r) for r in grid]
    hits = 0
    for y in range(len(g) - 1):
        for x in range(2, len(g[y]) - 2):
            if g[y][x] == "t" and "".join(g[y + 1][x - 1:x + 2]) == "sss":
                # stessa faccia: il timbro non deve uscire dal rettangolo della faccia della gemma
                face = P.WHERE[(x, y)][:2]
                for dx in (-2, -1, 0, 1, 2):
                    assert P.WHERE.get((x + dx, y + 1), (None, None))[:2] == face, (x, y)
                g[y][x - 1], g[y][x], g[y][x + 1] = "G", "W", "G"
                for dx, ch in zip((-2, -1, 0, 1, 2), "77W88"):
                    g[y + 1][x + dx] = ch
                below = "".join(g[y + 2][x - 2:x + 3]) if y + 2 < len(g) else ""
                if len(below) == 5 and set(below) <= set("abcdef") and \
                        all(P.WHERE.get((x + dx, y + 2), (None, None))[:2] == face for dx in (-2, -1, 0, 1, 2)):
                    for dx, ch in zip((-2, -1, 0, 1, 2), "H99KH"):
                        g[y + 2][x + dx] = ch
                hits += 1
    return ["".join(r) for r in g], hits


# medaglione della corazza (body_out north, gemma s q del T8): disegnato a mano come quello dell'icona
CHEST_MEDAL = {(3, 1): "G", (4, 1): "W", (5, 1): "8", (6, 1): "G",
               (3, 2): "7", (4, 2): "7", (5, 2): "W", (6, 2): "8",
               (3, 3): "H", (4, 3): "9", (5, 3): "9", (6, 3): "K"}

# riflessi (u v) sui bordi di lega e puntini (P Q R) sul corpo, a mano, su tutte le facce che si vedono
ARMOR_DETAIL = {
    # testa: riflesso sulla fascia (davanti a sinistra, fianco sinistro, fianco destro sfalsato), puntini dietro e sopra
    ("head_out", "north"): {(1, 3): "v", (2, 3): "u", (8, 0): "R"},
    ("head_out", "west"): {(3, 3): "u", (4, 3): "v", (6, 5): "Q"},
    ("head_out", "east"): {(7, 3): "v", (3, 1): "Q", (4, 1): "P"},
    ("head_out", "south"): {(6, 5): "R", (6, 6): "Q", (5, 7): "P", (8, 3): "u"},
    ("head_out", "top"): {(3, 3): "Q", (4, 4): "P", (7, 1): "R"},
    # busto: medaglione davanti; cintura con riflesso; puntini a grappolo sul petto destro e sulla schiena
    ("body_out", "north"): {**CHEST_MEDAL, (5, 7): "v", (5, 8): "u", (7, 4): "R", (7, 5): "Q", (8, 6): "P"},
    ("body_out", "south"): {(3, 7): "u", (4, 7): "v", (2, 3): "Q", (2, 4): "P", (7, 1): "R"},
    ("body_out", "west"): {(1, 2): "Q", (1, 3): "P", (1, 7): "v"},
    ("body_out", "east"): {(1, 4): "Q", (0, 7): "u"},
    ("body_out", "top"): {(2, 1): "Q", (7, 0): "R"},
    # braccia: niente lega nel T8, solo corpo -> un grappolo per braccio, sfalsati (asimmetria)
    ("right_arm", "north"): {(1, 3): "R", (2, 4): "Q", (2, 5): "P"},
    ("right_arm", "east"): {(2, 7): "Q", (3, 8): "P"},
    ("right_arm", "south"): {(3, 2): "Q"},
    ("left_arm", "north"): {(3, 8): "R", (2, 9): "Q"},
    ("left_arm", "south"): {(1, 4): "Q", (2, 5): "P"},
    ("left_arm", "west"): {(2, 2): "Q"},
    # spallacci
    ("right_arm_extra", "top"): {(3, 2): "Q", (4, 3): "P"},
    ("right_arm_extra", "north"): {(1, 0): "u"},
    ("left_arm_extra1", "top"): {(4, 2): "v"},
    ("left_arm_extra2", "top"): {(2, 2): "R", (3, 3): "Q"},
    # gambe: riflesso sui pannelli di lega, puntini sul corpo
    ("right_leg", "north"): {(0, 0): "v", (1, 1): "u", (3, 6): "Q", (2, 7): "P"},
    ("right_leg", "west"): {(2, 3): "v"},
    ("right_leg", "south"): {(3, 1): "u", (1, 6): "Q"},
    ("left_leg", "west"): {(2, 4): "v", (1, 7): "Q"},
    ("left_leg", "east"): {(2, 4): "u"},
    ("left_leg", "south"): {(2, 3): "R", (2, 4): "Q", (3, 5): "P"},
    ("left_leg", "north"): {(3, 0): "Q"},
    # stivali
    ("right_boot", "south"): {(3, 1): "v"},
    ("right_boot", "west"): {(2, 0): "u"},
    ("left_boot", "east"): {(3, 1): "u"},
    # mantello e perizoma: un riflesso lungo la bordura di lega
    ("cape", "north"): {(10, 7): "u", (10, 8): "v"},
    ("front_cloth", "north"): {(6, 6): "v"},
}


def armor_grid():
    g = P.t8_armor()
    g = ["".join(DOT_TO_BODY.get(c, c) for c in r) for r in g]
    g, hits = castone(g)
    assert hits >= 5, hits
    g = P.put_faces(g, ARMOR_DETAIL)
    return g


# ======================================================================================================================
# PALETTE
# ======================================================================================================================
GEM = {"K": "#16121d", "W": "#ffffff", "7": "#3fd8f2", "8": "#ee4fc6", "9": "#ffdf3a"}   # come la 01
SETTING = {"G": "#a8701a", "H": "#4a2a08"}      # oro brunito (allthemodium): scuro, cosi la gemma gialla non si confonde
DOTS = {"P": "#5d3f8f", "Q": "#a47ee6", "R": "#e6d2ff"}       # unobtainium chiaro: si stacca dal viola saturo del taglio
BLACK = dict(zip("0123456", ("#07060c", "#14111d", "#201b2d", "#2e2840", "#403856", "#574c70", "#7a6e96")))  # 02
ANTHRACITE = dict(zip("0123456", ("#0f0e13", "#211f27", "#322f3a", "#45414f", "#5b5568", "#786e88", "#a596bc")))
PLUM = dict(zip("0123456", ("#0b0511", "#1b0f26", "#2a1839", "#3a234f", "#4e3068", "#674484", "#8a66aa")))
UA = dict(zip("abcdef", ("#1d0b38", "#432a94", "#6c1ce1", "#a82ce3", "#f2a61d", "#ffe28a")))   # viola, luci d'oro
UV = dict(zip("abcdef", ("#120c36", "#432a94", "#6c1ce1", "#1ebd95", "#73ffb9", "#dcfff0")))   # viola, luci menta
VA = dict(zip("abcdef", ("#0a2a33", "#0f5c7a", "#178287", "#1bb38a", "#ffb21c", "#fff1a8")))   # verde acqua, luci d'oro
REFL_VIB = {"u": "#1bb38a", "v": "#73ffb9"}      # vibranio (manca a UA)
REFL_ATM = {"u": "#e8a010", "v": "#ffd84a"}      # allthemodium (manca a UV)
REFL_UNO = {"u": "#a82ce3", "v": "#ea84f5"}      # unobtainium (manca a VA)
CLOTH_BLACK = P.P2_CLOTH
CLOTH_ANTH = dict(zip("ghijk", ("#131218", "#1d1b24", "#2a2733", "#3a3646", "#504a60")))
CLOTH_PLUM = dict(zip("ghijk", ("#12091a", "#1c1028", "#2a1a3a", "#3b2650", "#523669")))

VARIANTI = [
    ("A", "notte-viola", "Notte Viola",
     "Corpo nero della 02; una sola lega, UA, su tutto il taglio (viola in ombra, oro nelle luci) con due riflessi "
     "verde acqua; pochi puntini d'unobtainium sul corpo e un castone grande a cinque colori in oro brunito.",
     {**BLACK, **UA, **REFL_VIB, **CLOTH_BLACK}, ICON_GRIDS),
    ("B", "antracite", "Antracite",
     "Come A, ma il corpo e' grigio antracite con le luci che virano al viola: piu' chiaro del nero-blu del T7, "
     "molto piu' scuro dell'argento del T8.",
     {**ANTHRACITE, **UA, **REFL_VIB, **CLOTH_ANTH}, ICON_GRIDS),
    ("C", "notte-menta", "Notte Menta",
     "Come A, ma domina la lega UV: viola in ombra e verde menta nelle luci, con riflessi d'oro. VA su corpo nero "
     "sarebbe tornato al T7 (nero e verde acqua).",
     {**BLACK, **UV, **REFL_ATM, **CLOTH_BLACK}, ICON_GRIDS),
    ("D", "abisso", "Abisso",
     "Il viola passa al corpo (nero-prugna, l'unobtainium brunito) e il taglio diventa VA, verde acqua con luci "
     "d'oro, il colore piu' lontano dal T8; riflessi magenta. Lo stacco dal T7 lo fa il corpo prugna.",
     {**PLUM, **VA, **REFL_UNO, **CLOTH_PLUM}, ICON_GRIDS),
]


def palette_of(base):
    return {**base, **DOTS, **GEM, **SETTING}


def build(base, grids):
    pal = palette_of(base)
    icons = {}
    for n in ICONS:
        g = grids[n]
        miss = set("".join(g)) - set(pal) - {"."}
        assert not miss, (n, miss)
        im = P.render(g, pal)
        assert P.sil(im) == P.sil(Image.open(T8_ICON[n]).convert("RGBA")), ("sagoma diversa dal T8", n)
        icons[n] = im
    g = armor_grid()
    miss = set("".join(g)) - set(pal) - {"."}
    assert not miss, ("armatura", miss)
    arm = P.render(g, pal)
    assert P.sil(arm) == P.sil(Image.open(T8_ARMOR).convert("RGBA")), "armatura: alpha diverso dal T8"
    return icons, arm


def save(letter, slug, icons, arm):
    d = os.path.join(OUT, f"{letter}-{slug}")
    os.makedirs(d, exist_ok=True)
    for n in ICONS:
        assert icons[n].size == (16, 16) and icons[n].mode == "RGBA"
        icons[n].save(os.path.join(d, f"t9_{n}.png"))
    assert arm.size == (128, 128) and arm.mode == "RGBA"
    arm.save(os.path.join(d, "t9_armor.png"))
    H.on_bg(M.mannequin(arm, 8), P.MAN_BG).save(os.path.join(d, "manichino.png"))
    return d


# ======================================================================================================================
# Panoramica e confronto
# ======================================================================================================================
BG, SL, DK, TX, TI = (52, 52, 60), (139, 139, 139), (24, 22, 30), (215, 215, 222), (255, 214, 120)


def panoramica(rows, path):
    MS = 4
    man_w, man_h = 42 * MS, 36 * MS
    W = 16 + 4 * 138 + 12 + man_w + 16
    RH = 22 + 34 + max(172, man_h) + 18 + 22
    img = Image.new("RGB", (W, 50 + RH * len(rows)), BG)
    d = ImageDraw.Draw(img)
    d.text((16, 14), "T9 Alloy «Prismatic»: giro 2, 4 varianti del set", fill=TI, font=P.font(20, True))
    y = 50
    for r in rows:
        d.text((16, y), f"{r['numero']}  {r['nome']}", fill=TI, font=P.font(16, True))
        for k, ln in enumerate(P.wrap(d, r["frase"], P.font(12), W - 32)[:2]):
            d.text((16, y + 22 + k * 15), ln, fill=TX, font=P.font(12))
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
            d.text((x + 82, yy + 145), M.NOMI[n], fill=TX, font=P.font(11))
        tex = Image.open(os.path.join(H.ROOT, r["file"]["armor"])).convert("RGBA")
        man = H.on_bg(M.mannequin(tex, MS), P.MAN_BG)
        img.paste(man.convert("RGB"), (16 + 4 * 138 + 12, yy))
        sy = yy + max(172, man_h) + 6
        for k, c in enumerate(r["palette"]):
            d.rectangle((16 + k * 22, sy, 16 + k * 22 + 18, sy + 14), fill=P.rgb(c))
        y += RH
    img.save(path)


def confronto(path):
    """Armi della 01 (giro 1) accanto a quelle della A: spada e ascia (e per completezza elmo e corazza), x8 su slot."""
    src01 = os.path.join(P.OUT, "01-spettro")
    srcA = os.path.join(OUT, "A-notte-viola")
    W = 16 + 4 * 138 + 16
    img = Image.new("RGB", (W, 40 + 2 * 170 + 10), BG)
    d = ImageDraw.Draw(img)
    d.text((16, 12), "Prima (giro 1, 01 Spettro)  ->  dopo (giro 2, A Notte Viola)", fill=TI, font=P.font(16, True))
    for row, (lab, src) in enumerate((("01 Spettro", src01), ("A Notte Viola", srcA))):
        yy = 40 + row * 170
        for i, n in enumerate(ICONS):
            im = Image.open(os.path.join(src, f"t9_{n}.png")).convert("RGBA")
            x = 16 + i * 138
            d.rectangle((x, yy, x + 127, yy + 127), fill=SL)
            big = im.resize((128, 128), Image.NEAREST)
            img.paste(big, (x, yy), big)
            sm = im.resize((32, 32), Image.NEAREST)
            d.rectangle((x, yy + 132, x + 35, yy + 167), fill=DK)
            img.paste(sm, (x + 2, yy + 134), sm)
            d.text((x + 42, yy + 145), lab if i == 0 else M.NOMI[n], fill=TX, font=P.font(11))
    img.save(path)


def main():
    os.makedirs(OUT, exist_ok=True)
    rows = []
    for letter, slug, nome, frase, base, grids in VARIANTI:
        icons, arm = build(base, grids)
        d = save(letter, slug, icons, arm)
        rel = os.path.relpath(d, H.ROOT)
        files = {n: os.path.join(rel, f"t9_{n}.png") for n in ICONS}
        files["armor"] = os.path.join(rel, "t9_armor.png")
        files["manichino"] = os.path.join(rel, "manichino.png")
        pal = [base["2"], base["4"], base["b"], base["c"], base["d"], base["e"], base["v"], DOTS["Q"],
               SETTING["G"], GEM["7"], GEM["8"], GEM["9"], GEM["K"], GEM["W"]]
        rows.append({"numero": letter, "nome": nome, "frase": frase, "tipo": "sagome del T8", "palette": pal,
                     "file": files})
        print(f"ok {letter} {nome}")
    with open(os.path.join(OUT, "proposte.json"), "w") as fh:
        json.dump(rows, fh, ensure_ascii=False, indent=2)
    panoramica(rows, os.path.join(OUT, "panoramica.png"))
    confronto(os.path.join(OUT, "confronto_armi_01_A.png"))
    print("panoramica:", os.path.join(OUT, "panoramica.png"))


if __name__ == "__main__":
    main()
