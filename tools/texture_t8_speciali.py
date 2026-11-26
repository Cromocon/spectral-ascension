"""T8 «Argent»: gli speciali (workstaff, ultra greatsword, bident, balestra in 12 stati; 16x16 e oversized 32x32, animati).

Stesse regole e stessa mappatura di texture_t8.py (CMAP: colore T7 -> colore T8), applicata ai PNG FINALI del T7 v2
(t7-v2-puntini-fuori/speciali/, quindi con i ritocchi dell'utente sulle balestre). Le sagome sono quelle del T7.
Il metodo per i casi ambigui e' la POSIZIONE, non il colore:
- corda della balestra: il menta #8ab4b6 e' anche la cifra 6 del metallo. Le celle di corda sono quelle in cui vince la
  sovrapposizione di corda di texture_t7v2_speciali.crossbow_overlays (griglia ST* piu' il ritocco STRING16/20), calcolate
  rifacendo la stessa sovrapposizione (S.lay) con lettere-segnaposto per ogni fotogramma. Corda -> lilla pallido #c6b2f2.
- munizioni (freccia, razzo, frecce gemma): le celle in cui vince una sovrapposizione di munizione restano IDENTICHE al T7
  (colori T6). Alcuni colori di munizione coincidono con colori del metallo/onice T7 (es. onice #46526e), per questo non si mappano per colore.
- tutto il resto (corpo): CMAP come nel set. Le celle di gemma del castone non coperte restano corpo (onice -> moonstone).
Asserzioni: sagome = T7; fotogramma 0 = icona; 20 fotogrammi; mcmeta uguale; nessun arancio allthemodium T8 (nel corpo);
puntini verdi dove erano arancio; corda lilla in tutte le celle di corda; munizioni invariate; corpo uguale nei 12 stati.

Uso:  python3 -I tools/texture_t8_speciali.py    # scrive t8-argent/speciali/, rigenera vetrina_t8.png e speciali_confronto_t6_t7_t8.png
"""
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_t6castone as T  # noqa: E402
import texture_t6castone_speciali as S  # noqa: E402
import texture_t7v2_speciali as X7  # noqa: E402
import texture_t8 as E  # noqa: E402

SRC = os.path.join(E.SRC, "speciali")
OUT = os.path.join(E.OUT, "speciali")
T6SP = S.OUT
STRING = E.rgb(E.STRING)
ORANGE7 = {E.X_PAL[k] for k in "ABCDEF"}
GREEN = {E.rgb(v) for v in E.VIB_DOT.values()}
FRAMES = T.FRAMES


def roles(name):
    """Per ogni fotogramma una griglia di ruoli: 'b' corpo, 's' corda, 'm' munizione (n x n). Solo le balestre hanno s/m."""
    n = 32 if name.endswith("_oversized") else 16
    if not name.startswith("crossbow"):
        return [["b" * n] * n] * FRAMES
    st = name[len("crossbow_"):].replace("_oversized", "")
    over = name.endswith("_oversized")
    size = 20 if over else 16
    base, _, _ = X7.user_body(S.GRIDS["CB20" if over else "CB16"], "crossbow20" if over else "crossbow16")
    ov = X7.crossbow_overlays(st)[1 if over else 0]
    nstr = 1 + (st in (X7.STRING20 if over else X7.STRING16))        # prime griglie = corda (ST + ritocco), il resto = munizione
    mark = []
    for i, g in enumerate(ov):
        c = "s" if i < nstr else "m"
        m = S.Top if isinstance(g, S.Top) else list
        mark.append(m("".join("." if ch == "." else c for ch in r) for r in g))
    out = []
    for fr in base:
        # il corpo e' 'b' dove non c'e' sovrapposizione; lay rispetta la regola delle gemme (GEMCH) sul corpo reale
        body = ["".join("b" if ch != "." else "." for ch in r) for r in fr]
        gem = [r for r in fr]
        o = [list(r) for r in body]
        for g in mark:
            for y, r in enumerate(g):
                for x, c in enumerate(r):
                    if c != "." and (isinstance(g, S.Top) or gem[y][x] not in S.GEMCH):
                        o[y][x] = c
        rows = ["".join(r) for r in o]
        out.append(S.big(rows) if over else rows)
    return out


def recolor_frame(im, role, name, f):
    """im: PNG T7 16x16/32x32 di un fotogramma; role: griglia dei ruoli."""
    im = im.convert("RGBA")
    out = Image.new("RGBA", im.size)
    src, dst = im.load(), out.load()
    for y in range(im.height):
        for x in range(im.width):
            p = src[x, y]
            if p[3] == 0:
                continue
            r = role[y][x]
            assert r != ".", (name, f, x, y)
            if r == "s":
                assert p[:3] == E.MINT, ("corda non menta", name, f, x, y, p)
                dst[x, y] = STRING + (255,)
            elif r == "m":
                dst[x, y] = p                                     # munizione: invariata
            else:
                assert p[:3] in E.CMAP, ("colore T7 non mappato", p, name, f, x, y)
                dst[x, y] = E.CMAP[p[:3]] + (255,)
    return out


def build():
    os.makedirs(OUT, exist_ok=True)
    res, rol = {}, {}
    for name in S.ALL:
        rl = roles(name)
        rol[name] = rl
        a7 = Image.open(os.path.join(SRC, f"t7_{name}.png")).convert("RGBA")
        s7 = Image.open(os.path.join(SRC, f"t7_{name}_anim.png")).convert("RGBA")
        n = a7.width
        assert s7.size == (n, n * FRAMES), name
        i8 = Image.new("RGBA", a7.size)
        i8 = recolor_frame(a7, rl[0], name, "icona")
        s8 = Image.new("RGBA", s7.size)
        fr8 = []
        for f in range(FRAMES):
            fr = recolor_frame(s7.crop((0, f * n, n, (f + 1) * n)), rl[f], name, f)
            s8.paste(fr, (0, f * n))
            fr8.append(fr)
        assert fr8[0].tobytes() == i8.tobytes(), ("frame0 != icona", name)
        assert len(fr8) == FRAMES
        # sagome = T7
        assert E.sil(i8) == E.sil(a7) and E.sil(s8) == E.sil(s7), ("sagoma", name)
        i8.save(os.path.join(OUT, f"t8_{name}.png"))
        s8.save(os.path.join(OUT, f"t8_{name}_anim.png"))
        with open(os.path.join(OUT, f"t8_{name}_anim.png.mcmeta"), "w") as fh:
            fh.write(open(os.path.join(SRC, f"t7_{name}_anim.png.mcmeta")).read())
        # puntini: nessun arancio nel corpo; verdi dove erano arancio nel T7 (corpo)
        for f in range(FRAMES):
            a = s7.crop((0, f * n, n, (f + 1) * n)).load()
            b = fr8[f].load()
            r = rl[f]
            for y in range(n):
                for x in range(n):
                    if r[y][x] != "b":
                        continue
                    o7 = a[x, y][:3] in ORANGE7 and a[x, y][3]
                    assert not (b[x, y][:3] in ORANGE7 and b[x, y][3]), ("arancio nel T8", name, f, x, y)
                    assert bool(o7) == (b[x, y][:3] in GREEN and b[x, y][3] > 0), ("puntino", name, f, x, y)
                    # solo i puntini d'allthemodium sono verdi: altrimenti il verde dei puntini sarebbe ambiguo col vibranio?
        res[name] = fr8
    # corda lilla ovunque sia corda; munizione identica al T7; nessun arancio nelle icone, corpo uguale nei 12 stati
    for suf in ("", "_oversized"):
        cover = set()
        for st in S.STATES:
            for f in range(FRAMES):
                cover |= {(x, y) for y, r in enumerate(rol[f"crossbow_{st}{suf}"][f]) for x, c in enumerate(r) if c in "sm"}
        for f in range(FRAMES):
            ref = res["crossbow_standby" + suf][f]
            for st in S.STATES:
                im = res[f"crossbow_{st}{suf}"][f]
                for y in range(im.height):
                    for x in range(im.width):
                        if (x, y) not in cover:
                            assert im.getpixel((x, y)) == ref.getpixel((x, y)), ("corpo", st, suf, f, x, y)
        for st in S.STATES:
            nm = f"crossbow_{st}{suf}"
            a7 = Image.open(os.path.join(SRC, f"t7_{nm}.png")).convert("RGBA")
            i8 = Image.open(os.path.join(OUT, f"t8_{nm}.png")).convert("RGBA")
            for y in range(a7.height):
                for x in range(a7.width):
                    r = rol[nm][0][y][x]
                    if r == "m":
                        assert i8.getpixel((x, y)) == a7.getpixel((x, y))
                    elif r == "s":
                        assert i8.getpixel((x, y))[:3] == STRING
            # ogni stato ha almeno una cella di corda lilla visibile
            assert sum(c == "s" for row in rol[nm][0] for c in row) > 0, nm
    nstr = {n: sum(c == "s" for r in rol[n][0] for c in r) for n in S.ALL if n.startswith("crossbow")}
    nam = {n: sum(c == "m" for r in rol[n][0] for c in r) for n in S.ALL if n.startswith("crossbow")}
    print("ok speciali T8:", len(S.ALL), "pezzi x", FRAMES, "fotogrammi; celle corda (icona) min/max",
          min(nstr.values()), max(nstr.values()), "; celle munizione max", max(nam.values()))
    return res


NOMI_V = {"ultra_greatsword": "spadone", "workstaff": "workstaff", "bident": "bident", "crossbow_standby": "scarica",
          "crossbow_pulling_0": "carica 0", "crossbow_pulling_1": "carica 1", "crossbow_pulling_2": "carica 2",
          "crossbow_arrow": "freccia", "crossbow_firework": "razzo", "crossbow_amethyst": "ametista",
          "crossbow_citrine": "citrino", "crossbow_topaz": "topazio", "crossbow_onyx": "onice",
          "crossbow_moonstone": "moonstone", "crossbow_glass": "vetro"}
# ordine come nella vetrina T7: spadone, workstaff, bident, scarica, carica 0-2, freccia / razzo, ametista, citrino, topazio, onice, moonstone, vetro
VORD = ["ultra_greatsword", "workstaff", "bident", "crossbow_standby", "crossbow_pulling_0", "crossbow_pulling_1",
        "crossbow_pulling_2", "crossbow_arrow", "crossbow_firework", "crossbow_amethyst", "crossbow_citrine",
        "crossbow_topaz", "crossbow_onyx", "crossbow_moonstone", "crossbow_glass"]
assert sorted(VORD) == sorted(S.ORDER)


def confronto(path):
    BG = (139, 139, 139, 255)
    k16, k32, pad = 6, 3, 4
    names = VORD
    cw16, cw32 = 16 * k16, 32 * k32
    rowh = cw16 + 26
    lab = 120
    W = lab + 2 * (3 * cw16 + 2 * pad + 40)
    sheet = Image.new("RGBA", (W, 50 + len(names) * rowh + 10), (60, 60, 66, 255))
    d = ImageDraw.Draw(sheet)
    d.text((10, 8), "Speciali T6 | T7 | T8  (sx 16x16, dx oversized 32x32)",
           fill=(255, 255, 255, 255), font=T.FONT_B)
    for i, n in enumerate(names):
        y = 40 + i * rowh
        d.text((10, y + cw16 // 2 - 8), NOMI_V[n], fill=(235, 235, 240, 255), font=T.FONT_S)
        for blk, suf, k in ((0, "", k16), (1, "_oversized", k32)):
            x = lab + blk * (3 * cw16 + 2 * pad + 40)
            for p in (os.path.join(T6SP, f"t6_{n}{suf}.png"), os.path.join(SRC, f"t7_{n}{suf}.png"),
                      os.path.join(OUT, f"t8_{n}{suf}.png")):
                im = H.on_bg(H.up(Image.open(p).convert("RGBA"), k), BG)
                sheet.alpha_composite(im, (x, y + (0 if k == k16 else (cw16 - cw32) // 2)))
                x += cw16 + pad
    sheet.save(path)


def main():
    build()
    E.vetrina(os.path.join(E.OUT, "vetrina_t8.png"))
    confronto(os.path.join(E.OUT, "speciali_confronto_t6_t7_t8.png"))


if __name__ == "__main__":
    main()
