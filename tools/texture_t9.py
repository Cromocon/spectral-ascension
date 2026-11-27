"""T9 Alloy «Prismatic», versione completa: M3 «Giorno e notte» LENTA (texture_t9_m3.py) su tutto il tier.

Stesse regole della M3 lenta (importata): 40 fotogrammi da 5 tick (ciclo 10 s), giorno C1 al fotogramma 0, notte C2 al
20; 5 gemme x 8 fotogrammi nel castone; spettro che sale con periodo 20; alpha identico al T8 in ogni fotogramma.

COME SI OTTENGONO LE GRIGLIE
  - spada, ascia, elmo, corazza: le griglie a mano del giro 4 (G4.CG), identiche alla M3 lenta (asserzione: stessi byte).
  - gli altri pezzi del set e gli speciali: i ruoli del PNG finale del T8 (P.T8_ROLE), letti pixel per pixel:
        0-6 corpo -> corpo         a-f unobtainium -> prisma a-f        A-F puntini -> corpo (si sciolgono, come nel giro 3)
        W -> W                     z corda/filo -> z (colore fisso)     q r s t moonstone -> prisma (t e, s d, r c, q a)
    poi il CASTONE a mano (CASTONI qui sotto) in UNA posizione per pezzo: q contorno, t riflesso, s medio, r scuro,
    G/H montatura chiara/scura. Il moonstone del T8 fuori dal castone diventa cristallo (prisma).
  - arco in carica: le celle che differiscono dall'arco scarico sono la freccia: W resta W, z resta corda, il resto e'
    la punta/il fusto in un colore fisso (y); il castone non si disegna sotto la freccia.
  - balestra (12 stati): ruoli b/s/m di texture_t8_speciali.roles(): s corda -> z; m munizione -> il pixel del T8
    identico (non si tocca); b corpo -> ruoli del T8 come sopra. Il castone sta dove nessuno stato ha corda o munizione.
  - corda dell'arco, filo della canna e corda della balestra: un colore fisso (STRING), uguale di giorno e di notte.
  - 32x32 (oversized): s = (dx*x + dy*y) / 2, cosi' lo spettro ha la stessa scala del 16x16.

USCITE  design/texture/proposte/t9-m3/{set,speciali,armatura}/ con i nomi del T8 (prefisso t9_):
  t9_<pezzo>.png (= fotogramma 0), t9_<pezzo>_anim.png (40 fotogrammi), .mcmeta (frametime 5), t9_<pezzo>.gif (x8, 250 ms);
  armatura: t9_armor.png, t9_armor_0..39.png (quelle della M3 lenta, rifatte e confrontate byte per byte),
  t9_armor_anim.png 128x5120 (+ .mcmeta) per Blockbench, manichino.png, manichino_animato.gif; vetrina_t9.png.

Uso:  python3 -I tools/texture_t9.py

NOTA 2026-11-26: le balestre T9 «in mano» (oversized) sono ritoccate dall'utente (gemma al calcio in (27,27), corda c1b5e4);
il generatore le riproduce identiche per il default; i ritocchi a mano dell'EC NON hanno generatore.
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_mix as M  # noqa: E402
import texture_t6castone as T  # noqa: E402
import texture_t6castone_speciali as S  # noqa: E402
import texture_t8_speciali as SP  # noqa: E402
import texture_t9_proposte as P  # noqa: E402
import texture_t9_giro3 as G3  # noqa: E402
import texture_t9_giro4 as G4  # noqa: E402
import texture_t9_m3 as M3  # noqa: E402

OUT = os.environ.get("SA_T9_OUT") or os.path.join(P.PROP, "t9-m3")   # SA_T9_OUT: cartella di prova
M3DIR = os.path.join(M3.OUT, "M3-lenta")
T8 = P.T8DIR
FRAMES, FRAMETIME = M3.FRAMES, M3.FRAMETIME
T8FRAMES = 20
GIF_MS = FRAMETIME * 50
MCMETA = '{\n  "animation": {\n    "frametime": %d,\n    "interpolate": false\n  }\n}\n' % FRAMETIME
assert MCMETA == G3.MCMETA

STRING = P.rgb("#d9ccff")      # corda / filo: lilla perla, fisso nelle due fasi
STRING_HAND = P.rgb("#c1b5e4")  # corda delle balestre «in mano» (oversized): ritoccata a mano il 2026-11-26
ARROW = P.rgb("#f3eeff")       # punta e fusto della freccia dell'arco in carica: fisso
GEM_FALLBACK = {"t": "e", "s": "d", "r": "c", "q": "a"}   # moonstone del T8 fuori dal castone -> cristallo
DOT = dict(zip("ABCDEF", "123456"))

# direzione dello spettro (verso la punta / verso l'alto)
DIR = dict(G3.DIR)
DIR.update({n: (1, -1) for n in ("shovel", "hoe", "shears", "bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2",
                                 "fishing_rod", "fishing_rod_cast", "workstaff", "ultra_greatsword", "bident", "crossbow")})
DIR.update({"leggings": (0.5, -2), "boots": (0.5, -2)})

# ======================================================================================================================
# CASTONI a mano: pezzo -> (x0, y0, righe). '.' = lascia il pixel com'e'. Sempre dentro la sagoma del T8.
# ======================================================================================================================
QUAD = ["GqqG",                  # 4x4 come l'ascia del giro 4
        "qtsq",
        "qsrq",
        "HqqH"]
CASTONI = {
    "shovel": (9, 4, QUAD),                             # attacco della lama (come la testa dell'ascia); il moonstone
                                                        # del T8 sul collo diventa cristallo
    "hoe": (9, 2, ["GqqG",                              # attacco della lama della zappa
                   "qtsq",
                   "qsrq",
                   ".qqH"]),
    "shears": (6, 8, QUAD),                             # perno delle cesoie
    "bow": (2, 3, ["..Gqq",                             # impugnatura (centro dell'arco); le punte diventano cristallo.
                   ".qstq",                             # riflesso t fuori dalla diagonale della freccia in carica
                   "qsrq.",
                   "GqH.."]),
    "fishing_rod": (4, 9, ["..G..",                     # mulinello
                           ".qtq.",
                           "qssrq",
                           "HqqqH"]),
    "leggings": (8, 3, QUAD),                           # cintura, fuori asse come l'elmo
    "boots": (9, 5, QUAD),                              # stivale destro
    "workstaff": (8, 4, QUAD),
    "ultra_greatsword": (3, 8, QUAD),                   # guardia
    "bident": (7, 5, QUAD),
    "crossbow": (11, 11, [".qq.",                       # calcio: l'unico punto senza corda/munizioni in tutti i 12 stati
                          "qtsq",
                          "qsrq",
                          ".qqH"]),
    "workstaff_oversized": (18, 10, QUAD),
    "ultra_greatsword_oversized": (6, 20, ["G.qq.G",   # guardia: un castone 6x6 al posto dei tre moonstone del T8
                                           ".qtsq.",
                                           "qtssrq",
                                           "qssrrq",
                                           ".qsrq.",
                                           "H.qq.H"]),
    "bident_oversized": (20, 8, QUAD),
    "crossbow_oversized": (27, 27, [".qqG",             # calcio in basso a destra (ritocco utente 2026-11-26; prima (14,18, QUAD)).
                                    "qtsq",             # l'angolo (27,27) resta com'e': e' munizione/corda in alcuni stati
                                    "qsrq",
                                    "HqqH"]),
}
CASTONI["fishing_rod_cast"] = CASTONI["fishing_rod"]
for _b in ("bow_pulling_0", "bow_pulling_1", "bow_pulling_2"):
    CASTONI[_b] = CASTONI["bow"]


# ======================================================================================================================
# GRIGLIE
# ======================================================================================================================
def t8_roles(path):
    return P.t8_roles(path)


def derive(rows):
    out = []
    for r in rows:
        s = ""
        for ch in r:
            if ch in DOT:
                ch = DOT[ch]
            elif ch in GEM_FALLBACK:
                ch = GEM_FALLBACK[ch]
            assert ch in ".0123456abcdefWzghijk", ch
            s += ch
        out.append(s)
    return out


def apply_castone(grid, key, skip=()):
    x0, y0, pat = CASTONI[key]
    g = [list(r) for r in grid]
    for dy, r in enumerate(pat):
        for dx, ch in enumerate(r):
            if ch == ".":
                continue
            x, y = x0 + dx, y0 + dy
            if (x, y) in skip:
                continue
            assert g[y][x] not in ".zm", (key, x, y, g[y][x], "castone fuori sagoma o su corda/munizione")
            g[y][x] = ch
    return ["".join(r) for r in g]


def castone_cells(key):
    x0, y0, pat = CASTONI[key]
    return {(x0 + dx, y0 + dy) for dy, r in enumerate(pat) for dx, ch in enumerate(r) if ch in "qrst"}


def set_grid(n):
    if n in G4.CG:
        return G4.CG[n]
    rows = t8_roles(os.path.join(T8, "set", f"t8_{n}.png"))
    if n.startswith("bow_pulling"):
        base = t8_roles(os.path.join(T8, "set", "t8_bow.png"))
        arrow = {(x, y) for y in range(16) for x in range(16)
                 if rows[y][x] != "." and (rows[y][x] != base[y][x] or base[y][x] == "z") and rows[y][x] != "z"}
        g = derive(rows)
        zc = {(x, y) for y in range(16) for x in range(16) if rows[y][x] == "z"}
        g = apply_castone(g, n, skip=arrow | zc)       # la freccia (fusto z compreso) passa davanti al castone
        g = [list(r) for r in g]
        for x, y in arrow:
            g[y][x] = "W" if rows[y][x] == "W" else "y"
        return ["".join(r) for r in g]
    return apply_castone(derive(rows), n)


def special_grid(name):
    """Griglia del pezzo speciale; per le balestre 'm' = munizione (pixel del T8 copiato)."""
    rows = t8_roles(os.path.join(T8, "speciali", f"t8_{name}.png")) if not name.startswith("crossbow") else None
    over = name.endswith("_oversized")
    if not name.startswith("crossbow"):
        return apply_castone(derive(rows), name)
    rl = SP.roles(name)[0]
    im = Image.open(os.path.join(T8, "speciali", f"t8_{name}.png")).convert("RGBA")
    px = im.load()
    g = []
    for y, r in enumerate(rl):
        s = ""
        for x, c in enumerate(r):
            if c == ".":
                assert px[x, y][3] == 0
                s += "."
            elif c == "s":
                s += "z"
            elif c == "m":
                s += "m"
            else:
                s += P.T8_ROLE[px[x, y][:3]]
        g.append(s)
    g = derive_keep_m(g)
    return apply_castone(g, "crossbow_oversized" if over else "crossbow")


def derive_keep_m(rows):
    return ["".join("m" if ch == "m" else derive([ch])[0] for ch in r) for r in rows]


# ======================================================================================================================
# RENDER
# ======================================================================================================================
def color(ch, x, y, f, s, string=STRING):
    if ch == "z":
        return string
    if ch == "y":
        return ARROW
    return M3.color_m3(ch, x, y, f, s, False)


def render(grid, f, sfun, mun=None, string=STRING):
    h, w = len(grid), len(grid[0])
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            px[x, y] = mun[x, y] if ch == "m" else color(ch, x, y, f, sfun(x, y), string) + (255,)
    return im


def sil(im):
    return im.getchannel("A").tobytes()


def diff(a, b):
    return sum(abs(p - q) for p, q in zip(a.tobytes(), b.tobytes()))


def write_piece(d, n, frames):
    w = frames[0].width
    strip = Image.new("RGBA", (w, w * FRAMES), (0, 0, 0, 0))
    for i, fr in enumerate(frames):
        strip.paste(fr, (0, w * i))
    frames[0].save(os.path.join(d, f"t9_{n}.png"))
    strip.save(os.path.join(d, f"t9_{n}_anim.png"))
    with open(os.path.join(d, f"t9_{n}_anim.png.mcmeta"), "w") as fh:
        fh.write(MCMETA)
    G3.gif(frames, os.path.join(d, f"t9_{n}.gif"), 8, G3.SLOT, ms=GIF_MS)


def check_piece(d, n, frames, grid, t8png, t8anim, key, string=STRING):
    """40 fotogrammi, f0 = icona, alpha = T8 (icona e ogni fotogramma della striscia T8), castone unico con 5 gemme x 8,
    ciclo f39 -> f0 senza salti."""
    w = frames[0].width
    st = Image.open(os.path.join(d, f"t9_{n}_anim.png")).convert("RGBA")
    assert st.size == (w, w * FRAMES), (n, st.size)
    assert st.crop((0, 0, w, w)).tobytes() == Image.open(os.path.join(d, f"t9_{n}.png")).convert("RGBA").tobytes(), n
    t8 = Image.open(t8png).convert("RGBA")
    s8 = Image.open(t8anim).convert("RGBA")
    assert s8.height == w * T8FRAMES
    for f in range(FRAMES):
        a = sil(st.crop((0, w * f, w, w * (f + 1))))
        assert a == sil(t8) == sil(s8.crop((0, w * (f % T8FRAMES), w, w * (f % T8FRAMES + 1)))), (n, f, "alpha != T8")
    gem = {(x, y) for y, r in enumerate(grid) for x, ch in enumerate(r) if ch in "qrst"}
    if key is not None:
        assert gem == castone_cells(key) - {c for c in castone_cells(key) if grid[c[1]][c[0]] not in "qrst"}, n
        assert {grid[y][x] for x, y in gem} == set("qrst"), (n, "il castone non ha q r s t")
    gp = G3.gem_pixels(grid)
    cnt = M3.stable_gems(lambda f: frames[f].getpixel(gp[0])[:3], [gm["s"] for _, gm in G3.GEMS5])
    assert sorted(cnt.values()) == [8] * 5, (n, cnt)
    jump = diff(frames[FRAMES - 1], frames[0])
    adj = max(diff(frames[i], frames[i + 1]) for i in range(FRAMES - 1))
    assert jump <= adj, (n, jump, adj)
    # corda/filo: stesso colore in tutti i fotogrammi
    zc = [(x, y) for y, r in enumerate(grid) for x, ch in enumerate(r) if ch == "z"]
    for f in range(FRAMES):
        for p in zc:
            assert frames[f].getpixel(p)[:3] == string
    return jump, adj, len(gem)


def build_set():
    d = os.path.join(OUT, "set")
    os.makedirs(d, exist_ok=True)
    rep, allf, grids = [], {}, {}
    for n in T.SET_ORDER:
        g = set_grid(n)
        dx, dy = DIR[n]
        frames = [render(g, f, lambda x, y: dx * x + dy * y) for f in range(FRAMES)]
        write_piece(d, n, frames)
        key = None if n in G4.CG else n
        jump, adj, ng = check_piece(d, n, frames, g, os.path.join(T8, "set", f"t8_{n}.png"),
                                    os.path.join(T8, "set", f"t8_{n}_anim.png"), key)
        if n in G4.CG:      # identici alla M3 lenta scelta
            for suf in (".png", "_anim.png"):
                a = Image.open(os.path.join(d, f"t9_{n}{suf}")).convert("RGBA").tobytes()
                b = Image.open(os.path.join(M3DIR, f"t9_{n}{suf}")).convert("RGBA").tobytes()
                assert a == b, (n, suf, "diverso dalla M3 lenta")
        rep.append((n, jump, adj, ng))
        allf[n], grids[n] = frames, g
    return rep, allf, grids


def build_speciali():
    d = os.path.join(OUT, "speciali")
    os.makedirs(d, exist_ok=True)
    rep, allf, grids = [], {}, {}
    for name in S.ALL:
        over = name.endswith("_oversized")
        g = special_grid(name)
        base = name.replace("_oversized", "")
        kind = "crossbow" if base.startswith("crossbow") else base
        dx, dy = DIR[kind]
        k = 0.5 if over else 1.0
        t8i = Image.open(os.path.join(T8, "speciali", f"t8_{name}.png")).convert("RGBA")
        mun = t8i.load()
        string = STRING_HAND if (over and kind == "crossbow") else STRING
        frames = [render(g, f, lambda x, y: k * (dx * x + dy * y), mun, string) for f in range(FRAMES)]
        write_piece(d, name, frames)
        key = ("crossbow_oversized" if over else "crossbow") if kind == "crossbow" else name
        jump, adj, ng = check_piece(d, name, frames, g, os.path.join(T8, "speciali", f"t8_{name}.png"),
                                    os.path.join(T8, "speciali", f"t8_{name}_anim.png"), key, string)
        # munizioni identiche al T8 (icona e ogni fotogramma della striscia T8)
        s8 = Image.open(os.path.join(T8, "speciali", f"t8_{name}_anim.png")).convert("RGBA")
        w = t8i.width
        mc = [(x, y) for y, r in enumerate(g) for x, ch in enumerate(r) if ch == "m"]
        for f in range(FRAMES):
            fr8 = s8.crop((0, w * (f % T8FRAMES), w, w * (f % T8FRAMES + 1)))
            for p in mc:
                assert frames[f].getpixel(p) == t8i.getpixel(p) == fr8.getpixel(p), (name, f, p, "munizione")
        rep.append((name, jump, adj, ng, len(mc)))
        allf[name], grids[name] = frames, g
    # balestra: corpo uguale nei 12 stati (fuori da corda e munizioni di qualunque stato), in ogni fotogramma
    for suf in ("", "_oversized"):
        cover = set()
        for st in S.STATES:
            g = grids[f"crossbow_{st}{suf}"]
            cover |= {(x, y) for y, r in enumerate(g) for x, ch in enumerate(r) if ch in "zm"}
        assert not (castone_cells("crossbow" + suf) & cover)
        ref = allf["crossbow_standby" + suf]
        for st in S.STATES:
            fr = allf[f"crossbow_{st}{suf}"]
            for f in range(FRAMES):
                a, b = fr[f].load(), ref[f].load()
                w = fr[f].width
                for y in range(w):
                    for x in range(w):
                        if (x, y) not in cover:
                            assert a[x, y] == b[x, y], ("corpo balestra", st, suf, f, x, y)
    return rep, allf, grids


def build_armor():
    d = os.path.join(OUT, "armatura")
    os.makedirs(d, exist_ok=True)
    ag = G3.armor_culmine()
    adx, ady = G3.ARMOR_DIR
    pos = {}

    def sa(x, y):
        if (x, y) not in pos:
            wx, wy = G3.armor_pos(x, y)
            pos[(x, y)] = adx * wx + ady * wy
        return pos[(x, y)]
    af = [M3.render(ag, f, sa, armor=True) for f in range(FRAMES)]
    t8a = Image.open(os.path.join(T8, "armatura", "t8_armor.png")).convert("RGBA")
    strip = Image.new("RGBA", (128, 128 * FRAMES), (0, 0, 0, 0))
    for i, fr in enumerate(af):
        ref = Image.open(os.path.join(M3DIR, f"t9_armor_{i}.png")).convert("RGBA")
        assert fr.tobytes() == ref.tobytes(), ("armatura diversa dalla M3 lenta", i)
        assert sil(fr) == sil(t8a), i
        fr.save(os.path.join(d, f"t9_armor_{i}.png"))
        strip.paste(fr, (0, 128 * i))
    af[0].save(os.path.join(d, "t9_armor.png"))
    strip.save(os.path.join(d, "t9_armor_anim.png"))
    with open(os.path.join(d, "t9_armor_anim.png.mcmeta"), "w") as fh:
        fh.write(MCMETA)
    gp = G3.gem_pixels(ag)
    cnt = M3.stable_gems(lambda f: af[f].getpixel(gp[0])[:3], [gm["s"] for _, gm in G3.GEMS5])
    assert sorted(cnt.values()) == [8] * 5
    jump, adj = diff(af[-1], af[0]), max(diff(af[i], af[i + 1]) for i in range(FRAMES - 1))
    assert jump <= adj
    man_bg = P.MAN_BG
    H.on_bg(M.mannequin(af[0], 8), man_bg).save(os.path.join(d, "manichino.png"))
    imgs = [H.on_bg(M.mannequin(fr, 6), man_bg).convert("RGB") for fr in af]
    imgs[0].save(os.path.join(d, "manichino_animato.gif"), save_all=True, append_images=imgs[1:], duration=GIF_MS,
                 loop=0, disposal=1)
    return af, (jump, adj)


# ======================================================================================================================
# VETRINA (stile vetrina_t8.png): giorno (f0) e notte (f20)
# ======================================================================================================================
def font(s):
    p = "/usr/share/fonts/TTF/DejaVuSans.ttf"
    return ImageFont.truetype(p, s) if os.path.exists(p) else ImageFont.load_default()


def vetrina(path, setf, spf, af):
    BG, SL, TX, TI = (52, 52, 60), (139, 139, 139), (225, 225, 230), (255, 214, 120)
    COLS, CW, CH, X0 = 8, 144, 170, 24
    blocks = [("Set, giorno (f0) su slot #8b8b8b", [(n, setf[n][0], T.NOMI[n]) for n in T.SET_ORDER]),
              ("Set, notte (f20) su slot", [(n, setf[n][20], T.NOMI[n]) for n in T.SET_ORDER])]
    for title, suf in (("Speciali 16×16, giorno (f0)", ""), ("Speciali 32×32 (in mano), giorno (f0)", "_oversized")):
        blocks.append((title, [(n, spf[n + suf][0], SP.NOMI_V[n]) for n in SP.VORD]))
    blocks.append(("Speciali 32×32, notte (f20)", [(n, spf[n + "_oversized"][20], SP.NOMI_V[n]) for n in SP.VORD]))
    rows_h = sum(30 + ((len(b) + COLS - 1) // COLS) * CH + 10 for _, b in blocks)
    man0 = H.on_bg(M.mannequin(af[0], 6), BG + (255,))
    man20 = H.on_bg(M.mannequin(af[20], 6), BG + (255,))
    strip_h = 30 + 64 + 2 * (16 * 3 + 30)
    img = Image.new("RGB", (1184, 56 + rows_h + strip_h + 30 + man0.height + 20), BG)
    d = ImageDraw.Draw(img)
    d.text((16, 14), "T9 Prismatic, M3 «Giorno e notte» lenta (40 fotogrammi, ciclo 10 s)", fill=TI, font=font(22))
    y = 56
    for title, items in blocks:
        d.text((16, y), title, fill=TX, font=font(14))
        y += 30
        for i, (n, im, lab) in enumerate(items):
            x, yy = X0 + (i % COLS) * CW, y + (i // COLS) * CH
            big = im.resize((128, 128), Image.NEAREST)
            d.rectangle((x, yy, x + 127, yy + 127), fill=SL)
            img.paste(big, (x, yy), big)
            d.text((x + 64 - d.textlength(lab, font=font(14)) / 2, yy + 134), lab, fill=TX, font=font(14))
        y += ((len(items) + COLS - 1) // COLS) * CH + 10
    d.text((16, y), "Set su sfondo scuro (f0) e il ciclo della spada e della balestra: f0 f4 ... f36", fill=TX, font=font(14))
    y += 24
    for i, n in enumerate(T.SET_ORDER):
        im = setf[n][0].resize((64, 64), Image.NEAREST)
        x = 24 + i * 76
        d.rectangle((x, y, x + 63, y + 63), fill=(28, 28, 34))
        img.paste(im, (x, y), im)
    y += 74
    for frs in (setf["sword"], spf["crossbow_arrow"]):
        for j, f in enumerate(range(0, FRAMES, 4)):
            im = frs[f].resize((48, 48), Image.NEAREST) if frs[f].width == 16 else frs[f].resize((48, 48), Image.NEAREST)
            x = 24 + j * 56
            d.rectangle((x, y, x + 47, y + 47), fill=SL)
            img.paste(im, (x, y), im)
            d.text((x + 14, y + 50), f"f{f}", fill=TX, font=font(11))
        y += 78
    d.text((16, y), "Armatura indossata: giorno (f0) e notte (f20)", fill=TX, font=font(14))
    y += 24
    img.paste(man0.convert("RGB"), (16, y))
    img.paste(man20.convert("RGB"), (32 + man0.width, y))
    img.save(path)


def main():
    os.makedirs(OUT, exist_ok=True)
    rs, setf, sg = build_set()
    for n, j, a, ng in rs:
        print(f"set {n:18s} castone {ng:2d} px  diff f39-f0 {j:6d}  max fra vicini {a:6d}")
    rp, spf, spg = build_speciali()
    for n, j, a, ng, nm in rp:
        print(f"speciale {n:30s} castone {ng:2d} px  munizione {nm:3d} px  diff f39-f0 {j:6d}  max {a:6d}")
    af, (j, a) = build_armor()
    print("armatura: 40 fotogrammi identici alla M3 lenta; diff f39-f0", j, "max", a)
    vetrina(os.path.join(OUT, "vetrina_t9.png"), setf, spf, af)
    print("scritto", OUT)
    return sg, spg


if __name__ == "__main__":
    main()
