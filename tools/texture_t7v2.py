"""T7 v2 «Umbral»: identico al T6 Castone (stessi pixel, stesse forme, stessa animazione) con tre differenze.

1. allthemodium arancio -> vibranio verde acqua (rampa a-f della palette T7: stessi 6 toni, stessa luminosita' relativa);
2. gemme CMY del castone -> onice nero (neri del T7 Umbral: METAL 0, 1, 3, 5; contorno, scuro, medio, riflesso);
3. l'allthemodium arancio «affiora» da sotto la colata di vibranio (due varianti, pixel fissi, non pulsano):
   striscia: sul bordo interno del vibranio (pixel a-f con un vicino 4-conn. di bedrock 1-6 o stoffa g-k) una striscia di
             1-2 px con i toni d'arancio del T6 (stessa lettera a-f del pixel sostituito: scuro->chiaro seguendo la luce).
             Dove il vibranio e' sottile (nessun altro pixel di vibranio dietro) niente striscia piena: solo un pixel ogni tanto.
             Interruzioni: ~28% dei pixel candidati saltati (hash fisso), e secondo strato (2 px) su ~1 candidato su 4.
   puntini:  niente striscia; 1-2 px di arancio sparsi sulla colata, distanza di Chebyshev >= 3 tra un punto e l'altro
             (circa ogni 3-5 px di colata), ordine casuale fisso (crc32), 30% dei punti larghi 2 px.
   I pixel arancio sono sovrapposti DOPO il calcolo dell'animazione T6: forme e fasi non cambiano.
La bedrock (0-6) resta quella del T6; la stoffa (g-k) e' verde acqua scuro (CLOTH), non piu' viola.

Varianti «fuori» (correzione: il vibranio resta TUTTO com'e'; l'allthemodium fuso sta FUORI dalla colata):
   puntini_fuori:  gocce di 1-2 px sui pixel di bedrock (1-6) 4-adiacenti al vibranio, distanza di Chebyshev >= 3
                   (circa 3-5 px), ordine casuale fisso (crc32), 30% delle gocce larghe 2 px.
   striscia_fuori: 1 px di allthemodium su quasi ogni bedrock adiacente al vibranio, ~25% saltati (hash fisso) = interruzioni.
   Tono: segue la luce della bedrock (1->A, 2->B, 3->B, 4->C, 5->D, 6->E, toni arancio del T6). Mai su 0, stoffa, gemme.
   Asserzioni: nessun pixel di vibranio cambia (tranne i pezzi ritoccati dall'utente), sagoma uguale al T6, fotogramma 0 = icona ferma.
   Giro 1 dell'utente (solo puntini_fuori): USER_ICONS / USER_ARMOR, vedi sotto; l'uscita coincide pixel per pixel con modifiche_utente/giro1/.

Uso:  python3 -I tools/texture_t7v2.py [--variante striscia|puntini|puntini_fuori|striscia_fuori|entrambe|fuori]
      scrive in design/texture/proposte/t7-v2-<variante>/{set,armatura} (underscore -> trattino);
      entrambe = vecchie due (+ confronto2), fuori = nuove due (+ confronto3, default)
"""
import argparse
import os
import sys
import zlib

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_mix as M  # noqa: E402
import texture_t6castone as T  # noqa: E402
import texture_t7 as T7  # noqa: E402

PROP = os.path.join(H.ROOT, "design", "texture", "proposte")
T6OUT, T7OUT = T.OUT, T.T7

# --- palette ---------------------------------------------------------------
VIB = dict(zip("abcdef", ("#08202b", "#12536a", "#209187", "#3bbfa6", "#86ecc8", "#dcfff0")))
# onice: neri del T7 Umbral (METAL 3 contorno, 0 scuro, 1 medio, 5 riflesso); prima: #46526e #0c0d14 #1c1e2a #8492b4
ONYX = {"rim": "#46526e", "dark": "#0c0d14", "mid": "#1c1e2a", "sheen": "#8492b4"}   # onice originale del T7 (texture_t7.py)
ONYX_V1 = {"rim": "#2e3242", "dark": "#07080d", "mid": "#14161f", "sheen": "#5a6075"}   # prima versione (neri del metallo)
METAL = T7.METAL    # corpo: la bedrock 0-6 del T6 diventa la rampa METAL del T7, cifra per cifra
ONYX_CHARS = {"rim": "qmu", "dark": "rnv", "mid": "sow", "sheen": "tpx"}
ORANGE = {c.upper(): "#%02x%02x%02x" % T.PAL[c][:3] for c in "abcdef"}   # allthemodium T6: A..F = a..f
# stoffa T7 v2: verde acqua scuro (petrolio) al posto del viola del T6; g..k stessa luminosita' relativa di prima
# (T6 g-k: #120c24 #21173d #312256 #43306f #5a4589). Sta sotto il vibranio #209187 anche al tono piu' chiaro.
CLOTH = {"g": "#061417", "h": "#0b2328", "i": "#12343a", "j": "#1a474e", "k": "#245b62"}


def make_pal():
    p = {k: "#%02x%02x%02x" % v[:3] for k, v in T.PAL.items()}     # parte dalla palette T6 (bedrock, W)
    p.update(METAL)
    p.update(VIB)
    for role, chars in ONYX_CHARS.items():
        for ch in chars:
            p[ch] = ONYX[role]
    p.update(ORANGE)
    p.update(CLOTH)
    p["z"] = "#8ab4b6"     # corda dell'arco base, ora menta come negli archi tesi (lettera z)
    return {k: H.hexc(v) for k, v in p.items()}


PAL = make_pal()

# --- affioramento dell'allthemodium ------------------------------------------
VIBS, INNER = set("abcdef"), set("123456ghijk")
N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))


def _h(seed, x, y):
    return zlib.crc32(f"{seed}:{x}:{y}".encode())


def _at(rows, x, y):
    return rows[y][x] if 0 <= y < len(rows) and 0 <= x < len(rows[y]) else "."


def strip(rows, seed):
    """Variante striscia: vedi docstring. Restituisce {(x, y): lettera maiuscola}."""
    vib = {(x, y) for y, r in enumerate(rows) for x, ch in enumerate(r) if ch in VIBS}
    cand = {p for p in vib if any(_at(rows, p[0] + dx, p[1] + dy) in INNER for dx, dy in N4)}
    out = {}
    for x, y in sorted(cand):
        backed = any((x + dx, y + dy) in vib and (x + dx, y + dy) not in cand for dx, dy in N4)
        if backed:
            if _h(seed, x, y) % 7 >= 2:                 # interruzioni
                out[(x, y)] = rows[y][x].upper()
        elif _h(seed, x, y) % 5 == 0 and not any((x + dx, y + dy) in out for dx, dy in N4):
            out[(x, y)] = rows[y][x].upper()            # vibranio sottile: solo qualche pixel
    for x, y in sorted(vib - set(out)):                  # secondo strato (2 px)
        if _h(seed + "b", x, y) % 4 == 0 and any((x + dx, y + dy) in out for dx, dy in N4) \
                and any((x + dx, y + dy) in vib and (x + dx, y + dy) not in cand and (x + dx, y + dy) not in out
                        for dx, dy in N4):
            out[(x, y)] = rows[y][x].upper()
    return out


def dots(rows, seed):
    """Variante puntini: vedi docstring. Restituisce {(x, y): lettera maiuscola}."""
    vib = sorted(((x, y) for y, r in enumerate(rows) for x, ch in enumerate(r) if ch in VIBS),
                 key=lambda p: _h(seed, *p))
    pts = []
    for p in vib:
        if all(max(abs(p[0] - q[0]), abs(p[1] - q[1])) >= 3 for q in pts):
            pts.append(p)
    out = {p: rows[p[1]][p[0]].upper() for p in pts}
    for p in pts:                                          # 30% dei punti larghi 2 px
        if _h(seed + "w", *p) % 10 < 3:
            for dx, dy in N4:
                q = (p[0] + dx, p[1] + dy)
                if q in vib and q not in out and all(max(abs(q[0] - r[0]), abs(q[1] - r[1])) >= 2 for r in pts if r != p):
                    out[q] = rows[q[1]][q[0]].upper()
                    break
    return out


BED = set("123456")
BED2ORANGE = {"1": "A", "2": "B", "3": "B", "4": "C", "5": "D", "6": "E"}


def outer_cand(rows):
    """Pixel di bedrock (1-6) 4-adiacenti a un pixel di vibranio: {(x, y): lettera arancio}."""
    c = {}
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in BED and any(_at(rows, x + dx, y + dy) in VIBS for dx, dy in N4):
                c[(x, y)] = BED2ORANGE[ch]
    return c


def dots_out(rows, seed, dist=3, wide=3):
    """Variante C puntini fuori. dist: distanza minima di Chebyshev fra le gocce;
    wide: gocce larghe 2 px su 10 (3 = 30%). Restituisce {(x, y): lettera maiuscola}."""
    cand = outer_cand(rows)
    pts = []
    for p in sorted(cand, key=lambda p: _h(seed, *p)):
        if all(max(abs(p[0] - q[0]), abs(p[1] - q[1])) >= dist for q in pts):
            pts.append(p)
    out = {p: cand[p] for p in pts}
    for p in pts:
        if _h(seed + "w", *p) % 10 < wide:
            for dx, dy in N4:
                q = (p[0] + dx, p[1] + dy)
                if q in cand and q not in out and all(max(abs(q[0] - r[0]), abs(q[1] - r[1])) >= 2 for r in pts if r != p):
                    out[q] = cand[q]
                    break
    return out


def strip_out(rows, seed):
    """Variante D striscia fuori. Restituisce {(x, y): lettera maiuscola}."""
    return {p: ch for p, ch in outer_cand(rows).items() if _h(seed, *p) % 4 != 0}


MODES = {"striscia": strip, "puntini": dots, "puntini_fuori": dots_out, "striscia_fuori": strip_out}
OUTER = {"puntini_fuori", "striscia_fuori"}
OLD, NEW = ["striscia", "puntini"], ["puntini_fuori", "striscia_fuori"]


def dirname(mode):
    return "t7-v2-" + mode.replace("_", "-")


def overlay(rows, pts, check=True, allowed=VIBS, soft=False):
    """soft: salta i pixel che nel fotogramma non sono del tipo atteso (animazione)."""
    rows = [list(r) for r in rows]
    for (x, y), ch in pts.items():
        if rows[y][x] not in allowed:
            assert soft or not check, (x, y, rows[y][x])
            continue
        rows[y][x] = ch
    return ["".join(r) for r in rows]


ARMOR_KW = {"puntini_fuori": dict(dist=5, wide=2)}   # solo armatura: distanza 5 e gocce larghe 20% anziche' 30% (102 -> 44 px); icone invariate


def faces_v2(mode):
    f = {k: list(v) for k, v in T.FACES.items()}
    kw = ARMOR_KW.get(mode, {})
    for (cube, face), rows in T.FACES.items():
        f[(cube, face)] = overlay(rows, MODES[mode](rows, f"{cube}/{face}", **kw), allowed=BED if mode in OUTER else VIBS)
    return f

# --- ritocchi dell'utente (giro 1, Blockbench), solo variante puntini_fuori ------------------------------------------
# Icone ferme approvate, 16x16, nelle lettere di PAL. Minuscole a-f = vibranio (pulsa come prima), q-x = onice (le
# lettere r/s/t, n/o/p, v/w/x pulsano, q/m/u no), 0-6 corpo, g-k stoffa, MAIUSCOLE A-F = gocce di allthemodium fisse
# (prendono il posto di quelle generate), z = corda dell'arco base, ora menta come negli archi tesi (#8ab4b6).
# build() ne ricava la griglia «semantica T6» (maiuscole -> bedrock '1') per l'animazione e i punti arancio da
# sovrapporre: il fotogramma 0 coincide con la PNG dell'utente, vibranio e onice pulsano come gli altri.
USER_DIR = os.path.join(PROP, "t7-v2-puntini-fuori", "modifiche_utente", "giro1")
# Giro 2 (canne da pesca, normale e lanciata): le PNG stanno in modifiche_utente/giro2/ (USER_DIR2), voci fishing_rod e
# fishing_rod_cast; la sagoma e quella del T6 aggiornato (giro9_canne).
USER_DIR2 = os.path.join(PROP, "t7-v2-puntini-fuori", "modifiche_utente", "giro2")
USER_ICONS = {
    "axe": [
        "................",
        ".........00.....",
        "........0de0....",
        ".......0cddd00..",
        "......0bcdCB20..",
        "......0bcC2210..",
        "......0bB21110..",
        "........021110..",
        ".......dtc.100..",
        "......dwo..00...",
        ".....abb........",
        "....040.........",
        "...040..........",
        "..040...........",
        "..adc...........",
        "..ab............",
    ],
    "bow": [
        "................",
        ".......ee6565tcs",
        ".....ddDE2222rbq",
        "....c5D2....z...",
        "...cpo2....z....",
        "..d5nm....z.....",
        "..dD2....z......",
        ".eD3....z.......",
        ".eE2...z........",
        ".42...z.........",
        ".42..z..........",
        ".52.z...........",
        ".42z............",
        ".xw.............",
        ".cb.............",
        ".vu.............",
    ],
    "chestplate": [
        "................",
        "................",
        "00000.0cd0.00000",
        "06e540bcdb046e60",
        "05E43adeeda34D50",
        "0543Cafomfa3340.",
        ".044DacddcaC440.",
        ".03343444434330.",
        "..034556654430..",
        "..034D544E4330..",
        "..022bcddcb220..",
        "..022acddca220..",
        "..034D5115D430..",
        "..033441144330..",
        "..022331133220..",
        "..000000000000..",
    ],
    "helmet": [
        "................",
        "................",
        ".....000000.....",
        "...005454Cdc0...",
        "..0543443dtcC0..",
        "..0dcb43dwsoc0..",
        "..0CB0abcba0b0..",
        "..041......1E0..",
        "..042......240..",
        "..0432....2340..",
        "..0432....2340..",
        "...000....000...",
        "................",
        "................",
        "................",
        "................",
    ],
    "hoe": [
        "......0000......",
        "......eE540.....",
        "......eeE4300...",
        ".......ddcC320..",
        ".........cb320..",
        "..........b320..",
        ".........0cE20..",
        "........dtc0320.",
        ".......dwo.0210.",
        "......abc...00..",
        ".....040........",
        "....040.........",
        "...040..........",
        "..adc...........",
        "..ab............",
        "................",
    ],
    "shears": [
        "...........0000.",
        "..........054fe0",
        ".........054ee0.",
        "........05Cde0e0",
        ".......05Ddd0eD0",
        "......054cc0d540",
        ".....05Ccc0c540.",
        ".....04bb0cE40..",
        ".000.03b0b540...",
        "055500dtsc00....",
        "05.50dxwpob.....",
        "05550.abcc......",
        ".000..05550.....",
        "......05.50.....",
        "......05550.....",
        ".......000......",
    ],
    "shovel": [
        "................",
        "................",
        "...........0ef..",
        "..........0deef.",
        ".........0D4def.",
        "........054EDde.",
        ".........0321c..",
        "........05010...",
        ".......dtc.0....",
        "......dwo.......",
        ".....0bc........",
        "....040.........",
        "..bb40..........",
        "..adc...........",
        "...ab...........",
        "................",
    ],
    "sword": [
        ".............000",
        "............0ff0",
        "...........0e5f0",
        "..........0d5Ef0",
        ".........0cD4e0.",
        "....00..0c43d0..",
        "...05300bCEc0...",
        "..05dtc032b0....",
        "..0dtsrb2b0.....",
        ".0dxwponb0......",
        "..0bccdcba0.....",
        "...040bc50......",
        "..040.0b0.......",
        ".0c0...0........",
        "adc.............",
        "ab..............",
    ],
    "fishing_rod": [
        "................",
        "............56..",
        "...........50.6.",
        "..........50..6.",
        ".........d0...6.",
        "........E0....6.",
        ".......e0.....6.",
        "......d0......6.",
        ".....e0.......6.",
        "....d0d.......e.",
        "...e0dtc...e.cEc",
        "..d0dwsob..cdEEe",
        ".40.abcba...ecd.",
        "Cb0.............",
        "adc.............",
        "ab..............",
    ],
    "fishing_rod_cast": [
        "................",
        "............56..",
        "...........50...",
        "..........50....",
        ".........d0.....",
        "........E0......",
        ".......e0.......",
        "......d0........",
        ".....e0.........",
        "....d0d.........",
        "...e0dtc........",
        "..d0dwsob.......",
        ".40.abcba.......",
        "Cb0.............",
        "adc.............",
        "ab..............",
    ],
}
# armatura indossata (128x128): pixel ritoccati dall'utente rispetto all'uscita automatica, {(x, y): lettera}.
# (elmo: due pixel del contorno 3 -> 4; i ritocchi stanno sia nel fotogramma statico sia nei 20 fotogrammi)
USER_ARMOR = {(10, 14): "4", (9, 32): "4"}
BOW_PULL = ("bow_pulling_0", "bow_pulling_1", "bow_pulling_2")
CORDA = "z"     # corda dell'arco ritoccata


def split_final(fin):
    """Griglia finale (lettere di PAL) -> (griglia semantica T6 per l'animazione, {(x, y): goccia maiuscola})."""
    pts = {(x, y): ch for y, r in enumerate(fin) for x, ch in enumerate(r) if ch in ORANGE}
    g = ["".join("1" if ch in ORANGE else ch for ch in r) for r in fin]
    return g, pts


def user_final(mode, n):
    """Griglia finale ritoccata dall'utente per l'icona n (None se non ritoccata o altra variante).
    Archi tesi: il corpo (flettenti, impugnatura, gemme, vibranio, gocce) e' quello dell'arco base ritoccato, pixel per
    pixel dove la griglia T6 del pezzo coincide con quella dell'arco base; corda e freccia restano com'erano."""
    if mode != "puntini_fuori":
        return None
    if n in USER_ICONS:
        return USER_ICONS[n]
    if n in BOW_PULL:
        g, bg, ub = T.ITEMS[n], T.ITEMS["bow"], USER_ICONS["bow"]
        old = overlay(g, dots_out(g, n), allowed=BED)
        return ["".join(ub[y][x] if g[y][x] == bg[y][x] and ub[y][x] != CORDA else old[y][x] for x in range(16))
                for y in range(16)]
    return None


def render16(grid):
    return H.render(grid, PAL)


def render128(grid):
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), PAL[ch])
    return im


def build(mode):
    out = os.path.join(PROP, dirname(mode))
    allowed = BED if mode in OUTER else VIBS
    sdir, adir = os.path.join(out, "set"), os.path.join(out, "armatura")
    for d in (sdir, adir):
        os.makedirs(d, exist_ok=True)
    fn = MODES[mode]
    icons = {}
    for n in T.SET_ORDER:
        g = T.ITEMS[n]
        fin = user_final(mode, n)
        if fin:                 # pezzo ritoccato dall'utente: la sua griglia prende il posto di quella automatica
            g, pts = split_final(fin)
        else:
            pts = fn(g, n)
        icons[n] = render16(overlay(g, pts, allowed=allowed))
        if mode in OUTER and not fin:       # nessun pixel di vibranio cambia (tranne i pezzi ritoccati)
            base = render16(g)
            for (x, y) in pts:
                assert g[y][x] in BED
            ng = overlay(g, pts, allowed=allowed)
            assert all(ng[y][x] == g[y][x] for y in range(len(g)) for x in range(len(g[0])) if g[y][x] in VIBS), n
        anims = [render16(overlay(fg, pts, allowed=allowed, soft=True)) for fg in T.icon_grids(g)]
        assert anims[0].tobytes() == icons[n].tobytes(), n
        icons[n].save(os.path.join(sdir, f"t7_{n}.png"))
        T.save_anim(anims, os.path.join(sdir, f"t7_{n}_anim"))
    # armatura: fasi calcolate sulla griglia T6, poi i pixel arancio sovrapposti
    g6 = T.armor_grid()
    g7 = M.armor_grid(faces_v2(mode))
    if mode == "puntini_fuori":     # ritocchi dell'utente sull'armatura (stessa lettera in entrambe le griglie)
        for (x, y), ch in USER_ARMOR.items():
            assert g6[y][x] != "." and g7[y][x] != ".", (x, y)
            g6[y] = g6[y][:x] + ch + g6[y][x + 1:]
            g7[y] = g7[y][:x] + ch + g7[y][x + 1:]
    diff = {(x, y): g7[y][x] for y in range(128) for x in range(128) if g7[y][x] != g6[y][x]}
    assert diff and all(ch in ORANGE for ch in diff.values())
    if mode in OUTER:
        assert all(g6[y][x] in BED for (x, y) in diff), "l'arancio e' fuori dalla bedrock"
        assert all(g7[y][x] == g6[y][x] for y in range(128) for x in range(128) if g6[y][x] in VIBS), "vibranio cambiato"
    ph = T.armor_phases(g6)
    frames = []
    for f in range(T.FRAMES):
        rows = [list(r) for r in T.frame_grid(g6, f, ph)]
        for (x, y), ch in diff.items():
            if mode not in OUTER or rows[y][x] in BED:
                rows[y][x] = ch
        frames.append(render128(["".join(r) for r in rows]))
    tex = render128(g7)
    assert frames[0].tobytes() == tex.tobytes()
    tex.save(os.path.join(adir, "t7_armor.png"))
    for i, fr in enumerate(frames):
        fr.save(os.path.join(adir, f"t7_armor_{i}.png"))
    # controllo: stessa silhouette del T6
    for n in T.SET_ORDER:
        a = Image.open(os.path.join(T6OUT, "set", f"t6_{n}.png")).convert("RGBA")
        assert a.getchannel("A").tobytes() == icons[n].getchannel("A").tobytes(), n
    assert Image.open(os.path.join(T6OUT, "armatura", "t6_armor.png")).convert("RGBA").getchannel("A").tobytes() == tex.getchannel("A").tobytes()
    if mode == "puntini_fuori" and os.path.isdir(USER_DIR):      # coincidenza esatta con le PNG dell'utente
        for f in sorted(os.listdir(USER_DIR)):
            if f.endswith("_corda_viola.png"):      # archivio della versione con corda viola, non e' un riferimento
                continue
            mine =Image.open(os.path.join(adir if f == "t7_armor.png" else sdir, f)).convert("RGBA")
            assert mine.tobytes() == Image.open(os.path.join(USER_DIR, f)).convert("RGBA").tobytes(), f
        for n in ("fishing_rod", "fishing_rod_cast"):      # giro 2: canne ritoccate
            f = f"t7_{n}.png"
            mine = Image.open(os.path.join(sdir, f)).convert("RGBA")
            assert mine.tobytes() == Image.open(os.path.join(USER_DIR2, f)).convert("RGBA").tobytes(), f
        # il corpo dell'arco e' identico nei 4 stati (tutto tranne corda e freccia)
        for p in BOW_PULL:
            g, bg, ub = T.ITEMS[p], T.ITEMS["bow"], USER_ICONS["bow"]
            f = user_final(mode, p)
            assert all(f[y][x] == ub[y][x] for y in range(16) for x in range(16) if g[y][x] == bg[y][x] and ub[y][x] != CORDA), p
    print("ok", mode, len(icons), "icone,", len(frames), "fotogrammi armatura,", len(diff), "pixel arancio armatura")


def confronto2(path, modes=None, labels=("striscia", "puntini"), k=6, out="2", t7=False):
    modes = modes or OLD
    pad = 8
    cw = 16 * k
    BG = (92, 92, 100, 255)
    names = T.SET_ORDER
    nc = 3 + int(t7)
    trip = nc * cw + (nc - 1) * pad
    per = 3 if k < 8 else 2
    rows = (len(names) + per - 1) // per
    texs = [Image.open(os.path.join(T6OUT, "armatura", "t6_armor.png")).convert("RGBA")] + [
        Image.open(os.path.join(PROP, dirname(m), "armatura", "t7_armor.png")).convert("RGBA") for m in modes]
    if t7:
        texs.append(Image.open(os.path.join(T7OUT, "armatura", "t7_armor.png")).convert("RGBA"))
    mans = [H.on_bg(M.mannequin(t, 5), BG) for t in texs]
    W = max(per * (trip + 24) + 16, sum(m.width for m in mans) + 3 * 24 + 16)
    Hh = 56 + rows * (cw + 26) + 40 + mans[0].height + 16
    sheet = Image.new("RGBA", (W, Hh), (60, 60, 66, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), "T6 Castone | T7 v2 " + " | T7 v2 ".join(labels) + (" | T7 originale" if t7 else ""), fill=(255, 255, 255, 255), font=T.FONT_B)
    y = 40
    for i, n in enumerate(names):
        x, yy = 8 + (i % per) * (trip + 24), y + (i // per) * (cw + 26)
        ims = [Image.open(os.path.join(T6OUT, "set", f"t6_{n}.png"))] + [
            Image.open(os.path.join(PROP, dirname(m), "set", f"t7_{n}.png")) for m in modes]
        if t7:
            ims.append(Image.open(os.path.join(T7OUT, "set", f"t7_{n}.png")))
        for j, im in enumerate(ims):
            sheet.alpha_composite(H.on_bg(H.up(im.convert("RGBA"), k), BG), (x + j * (cw + pad), yy))
        dr.text((x, yy + cw + 3), T.NOMI.get(n, n), fill=(235, 235, 240, 255), font=T.FONT_S)
    y += rows * (cw + 26)
    dr.text((10, y), "Armatura indossata, davanti e dietro: T6 | " + " | ".join(labels) + (" | T7 originale" if t7 else ""), fill=(255, 255, 255, 255), font=T.FONT_S)
    for j, m in enumerate(mans):
        sheet.alpha_composite(m, (8 + j * (m.width + 24), y + 22))
    sheet.save(path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--variante", choices=[*MODES, "entrambe", "fuori"], default="fuori")
    a = ap.parse_args()
    modes = {"entrambe": OLD, "fuori": NEW}.get(a.variante, [a.variante])
    for m in modes:
        build(m)
    if a.variante == "entrambe":
        confronto2(os.path.join(PROP, "t7-v2-confronto2.png"))
    if a.variante == "fuori":
        confronto2(os.path.join(PROP, "t7-v2-confronto3.png"), NEW, ("puntini fuori", "striscia fuori"), k=8, t7=True)


if __name__ == "__main__":
    main()
