"""T7 «Umbral» (vibranium, tema nero/onice): stesso disegno del set approvato t6-mix, nuova palette e nuovi materiali.

Le griglie NON sono duplicate: si importano da tools/texture_mix.py (icone, armatura indossata 128x128, griglie di
fase delle animazioni). Qui cambiano solo i colori dei caratteri, cioè i materiali:
  0-6   metallo scuro di base, più freddo e più nero (tema onice); 6 = luce del metallo e anelli delle catene, in
        acciaio verde acqua chiaro
  a-f   prima allthemodium (luce arancio e ornamenti d'oro) -> VIBRANIUM verde acqua: a-c metallo (fermagli, guardie,
        fibbie, puntali, palpebre), d-f luce verde acqua luminosa (fratture, bordi accesi, bagliori)
  g-k   prima pergamena (fasce sulle impugnature) -> fasce di vibranium
  q-t, m-p, u-x   prima cristalli e gemme C, M, Y -> ONICE nero lucido: contorno acciaio freddo (si stacca dal metallo
        scuro), faccia d'ombra quasi nera, faccia media nera, faccia di luce fredda come riflesso
Eccezioni di materiale per arco e canna: corda, lenza e asta della freccia erano energia (r, w): diventano filo e asta
di vibranium, altrimenti in onice sparirebbero.

Uso:  python3 tools/texture_t7.py      # scrive SOLO in design/texture/proposte/t7/ (niente installazione)
"""
import base64
import io
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_mix as M  # noqa: E402

OUT = os.path.join(H.ROOT, "design", "texture", "proposte", "t7")

# ---------------------------------------------------------------------------
# Palette T7 (rampe con hue shifting: ombre verso il blu scuro, luci verso il menta)
# ---------------------------------------------------------------------------
METAL = {"0": "#07080d", "1": "#14161f", "2": "#20232f", "3": "#2e3242", "4": "#414658", "5": "#5a6075",
         "6": "#8ab4b6"}
VIBRANIUM = {"a": "#08202b", "b": "#12536a", "c": "#209187", "d": "#3bbfa6", "e": "#86ecc8", "f": "#dcfff0"}
VIB_WRAP = {"g": "#0a1f2a", "h": "#164e5e", "i": "#1f7f7a", "j": "#2fa592", "k": "#73d6bd"}
ONYX = {"rim": "#46526e", "dark": "#0c0d14", "mid": "#1c1e2a", "sheen": "#8492b4"}
ONYX_CHARS = {"rim": "qmu", "dark": "rnv", "mid": "sowSOY", "sheen": "tpx"}
WHITE = {"W": "#e6f6ff"}
# arco e canna: corda, lenza e asta della freccia in vibranium
BOW_ROD = {"r": "#1d5a5e", "w": "#9eead6"}
BOW_ROD_ITEMS = ("bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2", "fishing_rod", "fishing_rod_cast")


def palette(item=None):
    p = {**METAL, **VIBRANIUM, **VIB_WRAP, **WHITE}
    for role, chars in ONYX_CHARS.items():
        for ch in chars:
            p[ch] = ONYX[role]
    if item in BOW_ROD_ITEMS:
        p.update(BOW_ROD)
    return {k: H.hexc(v) for k, v in p.items()}


PAL7 = palette()
ORDER = M.ORDER
NOMI = M.NOMI
FRAMES, FRAMETIME = M.FRAMES, M.FRAMETIME
up, on_bg = H.up, H.on_bg


def icon(n):
    return H.render(M.FINAL[n], palette(n))


def icon_frames(n):
    pal = palette(n)
    g = M.FINAL[n]
    grids = M.wave_frames(g, M.PHASES[n]) if n in M.PHASES else M.ink_frames(g)
    return [H.render(fr, pal) for fr in grids]


def render_big(grid):
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), PAL7[ch])
    return im


# Elmo indossato: due ritocchi alle facce nord, solo qui (texture_mix.py non cambia). Nessun pixel trasparente sul
# contorno: la fessura a T e la banda degli occhi restano uguali, ma
#  - nella piastra interna l'ultimo pixel della riga 4 era vuoto (bucava il bordo destro): ora e pieno;
#  - la placca esterna ha le righe 0-4 vuote (si vede la piastra interna, piu piccola di mezzo pixel): le colonne dei
#    bordi diventano piene, cosi non resta un'intercapedine attorno alla fronte, e la riga 9 (sotto il mento) e piena.
# Giro apertura: l'apertura (T) ha la forma di quella nell'icona (barra 6 px, tratto 2 px fino al mento); la piastra interna
# ha l'apertura allargata di 1 px (offset di mezzo pixel tra le due facce); colonna 1 righe 5-6 (dal basso) vuote.
# Giro apertura/fianchi: apertura a T allargata di 1 px per lato (occhi cols 1-8, tratto del mento 4 px, fronte piena con gemma e onice, ripresa dalla vecchia piastra interna); la piastra interna di ogni faccia e la copia ridotta 10x10 -> 9x9 della placca esterna (si toglie una
# colonna e una riga in zone lisce, dichiarate in RIDUCI), cosi attraverso le aperture si vede lo stesso disegno.
# Fianchi: nelle catene delle facce ovest/est i pixel neri tra una perla e l'altra (colonna 6 ovest, colonna 1 est)
# facevano una fessura verticale: ora sono metallo scuro, la catena si legge come perle d'acciaio su piastra.
OUT_FACES = {
    ("head_out", "north"): "45adeeda53 44afomfa43 444acddca3 4322112233 3........2 3........6 343....330 643....326 pdc....22x 322....221".split(),
    ("head_out", "west"): "5555444433 5544443322 4444336322 4433332321 0000006000 4333222321 4332216211 3222111210 322110p110 2111000n00".split(),
    ("head_out", "east"): "5555444433 5444433322 4644333322 4233333221 0600000000 4233332221 4633322211 4233222111 4x22211110 2w11100000".split(),
    ("head_out", "south"): M.FACES[("head_out", "south")],
    ("head_out", "top"): M.FACES[("head_out", "top")],
    ("head_out", "bottom"): M.FACES[("head_out", "bottom")],
}
# (colonna tolta, riga tolta) per faccia
RIDUCI = {"north": (5, 3), "west": (2, 0), "east": (7, 0), "south": (7, 7), "top": (7, 7), "bottom": (4, 4)}


def riduci(g, dc, dr):
    return ["".join(c for i, c in enumerate(r) if i != dc) for j, r in enumerate(g) if j != dr]


HELMET_FIX = dict(OUT_FACES)
for _f, (_dc, _dr) in RIDUCI.items():
    HELMET_FIX[("head_in", _f)] = riduci(OUT_FACES[("head_out", _f)], _dc, _dr)


# Giro occhi: gli occhi del giocatore (riga 5 della placca esterna, righe 4-5 della piastra interna, mezzo pixel di sfasamento)
# restano scoperti. Esterna: righe 4-5 gia aperte sulle colonne 1-8. Interna: la riga 5 (copia ridotta della riga 6 esterna,
# aperta solo al centro) copriva meta occhio: ora e aperta sulle colonne 1-7 come la riga 4. Fronte, banda e onice (righe 0-3) stanno sopra.
_n = list(HELMET_FIX[("head_in", "north")])
_n[5] = "3.......0"
HELMET_FIX[("head_in", "north")] = _n

# Icona: stessa apertura dell'elmo indossato: fessura degli occhi di 8 px su 2 righe (x4-11, y7-8), tratto del volto di 4 px (x6-9)
# da y9 al bordo basso (prima 6 px e 2 px).
_h = [list(r) for r in M.FINAL["helmet"]]
for _y in (7, 8):
    _h[_y][4:12] = "0" * 8
for _y in (9, 10, 11):
    _h[_y][6:10] = "0" * 4
M.FINAL["helmet"] = ["".join(r) for r in _h]


# Giro rilettura: ascia e corazza (icone) riportate alle proporzioni di bedrock di Spectrum, con disegno Umbral.
# Ascia: testa compatta (8x7) con il taglio netto sul lato destro (filo di vibranium), manico diagonale fasciato.
M.FINAL["axe"] = """
................
.........00.....
........0650....
.......0654bcd..
......05443bce..
......0543 3bcd..
......0433 2bcd..
........032bcd..
.......030.bcd..
......040..bc...
.....0j0........
....0i0.........
...0h0..........
..0c0...........
..ab............
................
""".replace(" ", "").strip("\n").splitlines()
M.PHASES["axe"] = """
................
................
................
............99..
............88..
............77..
............66..
............55..
............44..
............3...
................
................
................
...0............
...1............
................
""".replace(" ", "").strip("\n").splitlines()
# Corazza: stessa sagoma della bedrock (spalle larghe 0-15, colletto, maniche 1-14, busto 2-13, fino al bordo basso);
# occhio di vibranium con iride d'onice al centro, spalline con filo di vibranium, cintura di vibranium.
M.FINAL["chestplate"] = """
................
................
00000.0cd0.00000
06e540bcdb045e60
05543adeeda34550
05433afomfa3340.
.0443acddca3440.
.0334344443 4330.
..03455665443 0..
..03445445433 0..
..022bcddcb220..
..022acddca220..
..034451154430..
..033441144330..
..022331133220..
..000000000000..
""".replace(" ", "").strip("\n").splitlines()


# Giro riconoscibilità (test alla cieca): arco in tensione, zappa e pala riportati alla sagoma di bedrock di Spectrum.
# Arco teso: stessa sagoma (flettenti curvi, corda a V, freccia che attraversa l'arco). Per non sembrare una balestra la
# corda diventa la linea più luminosa (rampa R-T-Z, solo per gli stati di tensione), l'asta della freccia è più scura e la
# punta è di vibranium (e/f/c) invece che onice.
M.RAMPS.append(["R", "T", "Z", "W"])
for _k in "RTZ":  # il confronto «set scuro» in panoramica() disegna le stesse griglie con M.PAL
    M.PAL.setdefault(_k, M.PAL["r"])
BOW_PULL_ITEMS = ("bow_pulling_0", "bow_pulling_1", "bow_pulling_2")
_pal_base = palette


def palette(item=None):  # noqa: F811
    p = _pal_base(item)
    if item in BOW_PULL_ITEMS:
        p.update({k: H.hexc(v) for k, v in {"R": "#52c9b4", "T": "#a4f0dc", "Z": "#e6fff6", "w": "#4d8a92"}.items()})
    return p


def _tense(n):
    g = [list(r.replace("r", "R")) for r in M.FINAL[n]]
    for y in range(4):  # punta della freccia (angolo in alto a sinistra)
        for x in range(5):
            g[y][x] = {"q": "f", "t": "e", "s": "c"}.get(g[y][x], g[y][x])
    return ["".join(r) for r in g]


def _tense_phases(n):
    return [r for r in M.PHASES[n]]


for _n in BOW_PULL_ITEMS:
    M.FINAL[_n] = _tense(_n)

# Zappa: testa sottile a L (barra in alto, lama piatta che scende sul lato destro) e manico diagonale, come bedrock.
# Filo di vibranium sul bordo della lama (lato destro e bordo inferiore).
M.FINAL["hoe"] = """
................
......0665......
......05443.....
.......054bcde..
.........0bcde..
..........0cde..
.........0bcde..
........030.0cd.
.......040..cee.
.....gkg........
....gsgh........
....30..........
...40...........
..30............
.uw0............
uxu.............
""".replace(" ", "").strip("\n").splitlines()
M.PHASES["hoe"] = """
................
................
................
.............9..
.............8..
.............7..
.............6..
.............54.
............43..
................
................
................
................
................
..1.............
.0..............
""".replace(" ", "").strip("\n").splitlines()

# Pala: lama a cucchiaio larga e corta in cima (punta in alto a destra), manico diagonale. Corpo in acciaio con
# contorno scuro, filo di vibranium sul bordo esterno della lama.
M.FINAL["shovel"] = """
................
................
...........dee..
..........0654d.
.........06543d.
........065443d.
.........0543d..
........0433c...
.......054.c....
......40........
.....30.........
...gkg..........
..gsgh..........
..40............
.uw0............
uxu.............
""".replace(" ", "").strip("\n").splitlines()
M.PHASES["shovel"] = """
................
................
...........BA9..
..............8.
..............7.
..............6.
.............5..
............4...
...........3....
................
................
................
................
................
..1.............
.0..............
""".replace(" ", "").strip("\n").splitlines()


# Test 2: spada e canna lanciata ridisegnate sulla sagoma di Spectrum (bedrock_sword, bedrock_fishing_rod_cast); test 3: spada ridisegnata
# pixel per pixel sulla maschera della bedrock_sword (lama, guardia diagonale con gemma d'onice, impugnatura, pomolo).
# Spada: lama diagonale larga 5 px con canale di vibranium a tratti, punta d'onice, guardia a croce corta (5 pixel in
# diagonale, spessa 2) con gemma d'onice al centro e capi in vibranium, impugnatura fasciata e pomolo d'onice.
M.FINAL["sword"] = """
.............rsq
............2cd5
...........23cd5
..........23ce5.
.........23cd5..
..00.00.23ce5...
066544023cd5....
064c4023cd5.....
044d023cd5......
05440qtq060.....
.0220tsr040.....
.gijhqrq00......
..hjkig0640.....
qtsgij06420.....
qsr.044c20......
rsq...04c0......
""".replace(" ", "").strip("\n").splitlines()
M.PHASES["sword"] = """
................
.............BB.
.............BB.
............AA..
...........99...
..........88....
.........77.....
...4....66......
...3...55.......
................
................
................
................
................
.......1........
........0.......
""".replace(" ", "").strip("\n").splitlines()
# Cesoie: le due lame (prima onice) in vibranium verde acqua: contorno scuro b, riflesso chiaro e, corpo d, vena scura c.
M.FINAL["shears"] = [r.translate(str.maketrans("qmutpxsowSOYrnv", "bbbeeeddddddccc")) for r in M.FINAL["shears"]]

# Canna lanciata: la stessa canna di fishing_rod (asta in diagonale, mulinello con fasce), ma il filo parte dalla punta e
# pende dritto fino a un galleggiante ben visibile (meta alta chiara di vibranium, meta bassa d'onice).
M.FINAL["fishing_rod_cast"] = """
................
................
...........40tr.
..........40..r.
.........40...r.
........40....r.
.......40.....r.
......40......r.
.....40.......r.
....4ea.......r.
...4ca.......0e0
..gkg........efd
.gsgh........mon
.30...........n.
uw0.............
xu..............
""".replace(" ", "").strip("\n").splitlines()
M.PHASES["fishing_rod_cast"] = """
................
................
.............23.
..............4.
..............5.
..............6.
..............7.
..............8.
..............9.
.....1........A.
....1.........B.
.............BB.
................
................
.0..............
0...............
""".replace(" ", "").strip("\n").splitlines()


# Busto indossato ripreso dall'icona della corazza (M.FINAL["chestplate"], che non cambia). Corrispondenza con la mappa UV
# nel manichino (vista frontale): colonna icona = colonna schermo - 2, riga icona = riga schermo - 7. Quindi:
#  - body_out davanti (10x10) = icona colonne 3-12, righe 3-12 (collare, occhio con iride, piastre, cintura di vibranium);
#  - body_in davanti (9x13): la riga 0 sporge sopra il busto (collare), le righe 11-12 sporgono sotto (piastre basse e bordo);
#  - braccia (5x13) = icona colonne 0-4 (destro) e 11-15 (sinistro): spallaccio con borchia di vibranium e manica con
#    polsino di vibranium; spallacci (cubi extra) = lo spallaccio dell'icona (destro) e il suo specchio (sinistro);
#  - lati del busto = colonne laterali dell'icona; dietro = stesse piastre e cintura, senza l'occhio.
def _chest_faces():
    I = [r.replace(".", "0") for r in M.FINAL["chestplate"]]
    crop = lambda r0, r1, c0, c1: [I[r][c0:c1] for r in range(r0, r1)]
    mir = lambda g: [r[::-1] for r in g]
    north = crop(3, 13, 3, 13)
    back = list(north)
    back[1:4] = [north[4], north[5], north[6]]
    back[1] = back[1][:2] + "4433" + back[1][6:]
    in_n = [I[2][4:13]] + [I[2 + r][4:13] for r in range(1, 11)] + [I[14][4:13], I[15][4:13]]
    arm_r = ["00000", "06e54", "05543", "05433", "00443", "23343", "23434", "23443", "23433", "22233",
             "2bcdb", "2acda", "22111"]
    arm_l = mir(arm_r)
    ext_r = ["06e554", "055443", "054433", "044322"]
    ext_r_side = ["066554", "055443", "054433", "044322"]
    f = {
        ("body_out", "north"): north, ("body_out", "south"): back,
        ("body_out", "west"): crop(3, 13, 2, 5), ("body_out", "east"): crop(3, 13, 11, 14),
        ("body_in", "north"): in_n,
        ("right_arm", "north"): arm_r, ("right_arm", "west"): arm_r, ("right_arm", "east"): arm_l,
        ("right_arm", "south"): arm_l,
        ("left_arm", "north"): arm_l, ("left_arm", "west"): arm_r, ("left_arm", "east"): arm_l,
        ("left_arm", "south"): arm_r,
        ("right_arm_extra", "north"): ext_r, ("right_arm_extra", "south"): mir(ext_r_side),
        ("right_arm_extra", "west"): ext_r_side, ("right_arm_extra", "east"): mir(ext_r_side),
        ("left_arm_extra2", "north"): mir(ext_r[:2]), ("left_arm_extra2", "south"): ext_r_side[:2],
        ("left_arm_extra2", "west"): ext_r_side[:2], ("left_arm_extra2", "east"): mir(ext_r_side[:2]),
    }
    return f


def armor_grid():
    return M.armor_grid({**M.FACES, **HELMET_FIX, **_chest_faces()})


def b64(data, fmt="png"):
    if not isinstance(data, bytes):
        buf = io.BytesIO()
        data.save(buf, format="PNG")
        data = buf.getvalue()
    return f"data:image/{fmt};base64," + base64.b64encode(data).decode()


def gif_bytes(frames, scale=1, bg=H.SLOT):
    imgs = [on_bg(up(fr, scale), bg).convert("RGB") for fr in frames]
    buf = io.BytesIO()
    imgs[0].save(buf, format="GIF", save_all=True, append_images=imgs[1:], duration=FRAMETIME * 50, loop=0)
    return buf.getvalue()


def save_anim(frames, base):
    strip = Image.new("RGBA", (frames[0].width, frames[0].height * FRAMES), (0, 0, 0, 0))
    for i, fr in enumerate(frames):
        strip.alpha_composite(fr, (0, fr.height * i))
    strip.save(base + ".png")
    with open(base + ".png.mcmeta", "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": %d,\n    "interpolate": false\n  }\n}\n' % FRAMETIME)


def panoramica(path, tex, tex6):
    from PIL import ImageDraw, ImageFont
    k, cell = 6, 16 * 6 + 10
    big, txt = ImageFont.load_default(size=24), ImageFont.load_default(size=14)
    man7 = on_bg(M.mannequin(tex, 6), (60, 60, 70, 255))
    man6 = on_bg(M.mannequin(tex6, 6), (60, 60, 70, 255))
    W = 8 * cell + 16
    H1 = 2 * cell + 40
    Hh = 30 + 2 * (H1 + 10) + man7.height + 40
    sheet = Image.new("RGBA", (W + man7.width + 30, Hh), (14, 15, 20, 255))
    dr = ImageDraw.Draw(sheet)
    y = 8
    for title, pal_fn, src in (("T7 Umbral", palette, None), ("set scuro t6-mix (prima)", None, None)):
        dr.text((12, y), title, fill=(59, 191, 166) if pal_fn else (200, 196, 210), font=big)
        y += 32
        for i, n in enumerate(ORDER):
            im = icon(n) if pal_fn else H.render(M.FINAL[n], M.PAL)
            sheet.alpha_composite(on_bg(up(im, k)), (8 + (i % 8) * cell, y + (i // 8) * cell))
        y += H1
    dr.text((12, y), "Armatura indossata: T7 (sinistra) e set scuro (destra)", fill=(200, 196, 210), font=txt)
    sheet.alpha_composite(man7, (8, y + 22))
    sheet.alpha_composite(man6, (8 + man7.width + 20, y + 22))
    sheet.save(path)


CSS = M.CSS.replace("--accent: #e89725;", "--accent: #3bbfa6;") + """
.swatches { display: flex; flex-wrap: wrap; gap: 10px; }
.sw { display: grid; gap: 4px; width: 104px; }
.sw i { display: block; height: 36px; border: 1px solid var(--line); }
.sw b { font-size: 12px; font-weight: 600; }
.sw code { font-size: 12px; color: var(--muted); }
.duo { display: flex; gap: 4px; }
.duo .slot { width: 64px; height: 64px; }
.duo .slot img { width: 56px; height: 56px; }
.items.pairs6 { grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); }
"""


def html(path, anims, tex, tex6, worn_gif, special=""):
    def img(im, alt):
        return f'<img src="{b64(im)}" alt="{alt}">'

    def fig(im, cap):
        return f'<figure><div class="slot">{img(im, cap)}</div><figcaption>{cap}</figcaption></figure>'

    sp7 = {n: icon(n) for n in ORDER}
    t6c = os.path.join(H.ROOT, "design", "texture", "proposte", "t6-castone")  # T6 Castone: PNG già pronti, da disco
    sp6 = {n: open(os.path.join(t6c, "set", f"t6_{n}.png"), "rb").read() for n in ORDER}
    man6 = open(os.path.join(t6c, "armatura", "manichino.png"), "rb").read()
    sw = []
    for title, ramp in (("Metallo scuro", METAL), ("Vibranium", VIBRANIUM), ("Fasce di vibranium", VIB_WRAP),
                        ("Onice", ONYX), ("Corda e asta (arco, canna)", BOW_ROD)):
        sw.append(f'<div class="block"><h3>{title}</h3><div class="swatches">'
                  + "".join(f'<div class="sw"><i style="background:{v}"></i><b>{k}</b><code>{v}</code></div>' for k, v in ramp.items())
                  + "</div></div>")
    parts = [f"""<title>Umbral, set T7</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&family=Pixelify+Sans:wght@500;600&display=swap">
<style>{CSS}</style>
<header class="intro"><div class="wrap">
  <h1>Umbral, set T7</h1>
  <p class="lead">Il set approvato (metallo scuro, occhi, catene, cristalli, mantello a lamelle) diventa il T7 <b>Umbral</b>:
  stesse forme, con il <b>vibranium</b> verde acqua come metallo degli ornamenti e come luce, e i cristalli in <b>onice</b> nero lucido.
  Solo design: niente è installato nella mod.</p>
</div></header>
<main class="wrap">{special}
<section class="style best" id="t7">
  <div class="head"><div class="num">T7</div><h2>Umbral</h2>
  <p>I 15 pezzi e l'armatura indossata (manichino davanti e dietro, mantello a lamelle). Animazioni: la stessa cresta di luce del set scuro, ora verde acqua.</p></div>
  <div class="items wide">{"".join(fig(sp7[n], NOMI[n]) for n in ORDER)}</div>
  <div class="lower">
    <div class="block"><h3>Dimensione reale 1×</h3><div class="real">{"".join(f'<div class="slot s1">{img(sp7[n], NOMI[n])}</div>' for n in ORDER)}</div></div>
    <div class="block"><h3>2×</h3><div class="real">{"".join(f'<div class="slot s2">{img(sp7[n], NOMI[n])}</div>' for n in ORDER)}</div></div>
  </div>
  <div class="worn">
    <div class="block"><h3>Manichino, fermo</h3><div class="slot man">{img(M.mannequin(tex, 8), "manichino T7")}</div></div>
    <div class="block gif"><h3>Manichino, animato</h3><div class="slot man"><img src="{b64(worn_gif, 'gif')}" alt="manichino T7 animato"></div></div>
    <div class="block"><h3>Texture 128×128, 3×</h3><div class="slot sheet">{img(tex, "texture armatura T7")}</div></div>
  </div>
</section>
<section class="style" id="cambio">
  <div class="head"><div class="num">↔</div><h2>T6 Castone e T7 a confronto</h2>
  <p>A sinistra il T6 Castone: bedrock grigio-viola con filo d'allthemodium e castone CMY. A destra lo stesso pezzo in T7 Umbral:
  metallo scuro, vibranium verde acqua e onice.</p></div>
  <div class="items pairs6">{"".join(f'<figure><div class="duo"><div class="slot">{img(sp6[n], "T6 Castone")}</div><div class="slot">{img(sp7[n], "T7")}</div></div><figcaption>{NOMI[n]}</figcaption></figure>' for n in ORDER)}</div>
  <div class="worn">
    <div class="block"><h3>T6 Castone</h3><div class="slot man">{img(man6, "manichino T6 Castone")}</div></div>
    <div class="block"><h3>T7 Umbral</h3><div class="slot man">{img(M.mannequin(tex, 6), "manichino T7")}</div></div>
  </div>
</section>
<section class="style" id="animazioni">
  <div class="head"><div class="num">~</div><h2>Animazioni</h2>
  <p>12 fotogrammi da 5 tick, interpolate false, stesse griglie di fase: negli attrezzi la luce verde acqua parte dal pomolo e risale i cristalli d'onice
  (che si accendono di un riflesso freddo), nell'arco corre lungo la corda di vibranium; nell'armatura l'iride d'onice degli occhi si accende una volta per ciclo.</p></div>
  <div class="items wide gif">{"".join(f'<figure><div class="slot"><img src="{b64(gif_bytes(anims[n]), "gif")}" alt="{NOMI[n]} animata"></div><figcaption>{NOMI[n]}</figcaption></figure>' for n in ORDER)}</div>
</section>
<section class="style" id="palette">
  <div class="head"><div class="num">#</div><h2>Palette</h2>
  <p>Rampe con hue shifting: le ombre del vibranium vanno verso il blu scuro, le luci verso il menta. L'onice ha un contorno in acciaio freddo
  perché sul metallo scuro un contorno nero sparirebbe, e una faccia di luce fredda come riflesso.</p></div>
  <div class="lower">{"".join(sw)}</div>
</section>
</main>
<footer><div class="wrap">Sprite originali per Spectral Ascension, disegnati a mano; il T7 riusa le griglie del set approvato con una nuova palette.</div></footer>
"""]
    with open(path, "w") as fh:
        fh.write("".join(parts))


def main():
    sdir, adir = os.path.join(OUT, "set"), os.path.join(OUT, "armatura")
    for d in (sdir, adir):
        os.makedirs(d, exist_ok=True)
    anims = {}
    for n in ORDER:
        icon(n).save(os.path.join(sdir, f"t7_{n}.png"))
        frames = icon_frames(n)
        save_anim(frames, os.path.join(sdir, f"t7_{n}_anim"))
        anims[n] = frames
    grid = armor_grid()
    tex = render_big(grid)
    tex.save(os.path.join(adir, "t7_armor.png"))
    aframes = [render_big(g) for g in M.ink_frames(grid)]
    for i, fr in enumerate(aframes):
        fr.save(os.path.join(adir, f"t7_armor_{i}.png"))
    on_bg(M.mannequin(tex, 8), (38, 34, 45, 255)).save(os.path.join(adir, "manichino.png"))
    worn_gif = gif_bytes([M.mannequin(fr, 6) for fr in aframes])
    with open(os.path.join(adir, "manichino_animato.gif"), "wb") as fh:
        fh.write(worn_gif)
    tex6 = M.render_big(grid)
    panoramica(os.path.join(OUT, "panoramica.png"), tex, tex6)
    import texture_t7_speciali as SP  # oggetti speciali: PNG in t7/speciali/ e sezione in cima alla pagina
    special = SP.section_html(SP.build(), lambda im, alt: f'<img src="{b64(im)}" alt="{alt}">')
    html(os.path.join(OUT, "stili.html"), anims, tex, tex6, worn_gif, special)


if __name__ == "__main__":
    main()
