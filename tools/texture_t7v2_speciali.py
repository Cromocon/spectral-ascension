"""T7 v2 «Umbral», variante puntini_fuori: i 4 oggetti speciali (workstaff, ultra greatsword, bident, balestra in 12 stati),
16x16 e oversized 32x32, animati (20 fotogrammi da 5 tick).

Stesse regole del set/armatura T7 v2 (texture_t7v2.py), applicate alle griglie degli speciali T6 approvati
(texture_t6castone_speciali.py, che non si modifica):
- corpo: bedrock 0-6 -> rampa METAL del T7; allthemodium a-f -> vibranio verde acqua; gemme CMY -> onice originale T7;
- gocce di allthemodium fuso (dots_out) sui pixel di corpo 1-6 4-adiacenti al vibranio; il vibranio non cambia mai;
- animazione T6 invariata (pulsano vibranio e onice), fotogramma 0 = icona ferma.
Specifico degli speciali:
- balestra: le gocce si calcolano UNA volta sulla base comune (CB16/CB20), non per stato; corda e munizione (sovrapposizioni
  del T6) stanno sopra. La corda e' la cifra 6 del T6, che la palette T7 v2 mappa come negli archi (menta #8ab4b6).
  Le munizioni (freccia, razzo, frecce gemma) restano con i colori T6 (AMMO/GEMS): sono la munizione, non il materiale.
- oversized 32x32 (e balestra 20x20): distanza minima fra gocce 5 invece di 3, cosi' la densita' visiva e' simile al 16x16.
- giro 2 dell'utente (Blockbench, solo T7): ha ritoccato a mano la balestra pulling_2, 16x16 e oversized (13 e 31 pixel, tutti
  nel corpo, nessuno sulla corda; archivio in modifiche_utente/giro2/). Il ritocco e' replicato su tutti i 12 stati come
  override del CORPO COMUNE: OVR16 / OVR20 = {(x, y): lettera della palette}, y = riga dall'alto nella griglia 16x16 / 20x20
  (per l'oversized prima dell'incollo in 12-31), ricavati per differenza dalle PNG (non a mano). Lettere: cifre 0-6 = metallo,
  q-x = onice (stessa rampa di gemma del pixel di partenza o della gemma piu' vicina, quindi pulsano come le altre gemme),
  !@#$%^ = gocce fisse di allthemodium (sostituiscono/aggiungono a quelle generate, che si calcolano sempre sulla base T6 non
  ritoccata cosi' le altre restano invariate). Le lettere non-goccia entrano nella griglia semantica da animare; le gocce si
  sovrappongono dopo, come in texture_t7v2.split_final. Le sovrapposizioni di stato (corda, munizione) restano sopra dove
  coprono: li' il ritocco non si replica (S.lay lascia comunque visibili le gemme del castone sotto la corda). Il
  fotogramma 0 di pulling_2 coincide pixel per pixel con le PNG dell'utente (asserzione). Le gocce dell'utente sono escluse
  dall'asserzione «gocce su BED adiacenti al vibranio», che vale per le generate.
- sagome identiche al T6 (asserzione su ogni fotogramma, contro i PNG T6 su disco).

Uso:  python3 -I tools/texture_t7v2_speciali.py
Scrive in design/texture/proposte/t7-v2-puntini-fuori/speciali/ (t7_<nome>.png, _anim.png, .mcmeta) e speciali_confronto.png.
"""
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_t6castone as T  # noqa: E402
import texture_t6castone_speciali as S  # noqa: E402
import texture_t7v2 as V  # noqa: E402

MODE = "puntini_fuori"
OUT = os.path.join(V.PROP, V.dirname(MODE), "speciali")
CONF = os.path.join(V.PROP, V.dirname(MODE), "speciali_confronto.png")
T6SP = S.OUT
DIST = {16: 3, 20: 5, 32: 5}     # distanza minima fra gocce per lato della griglia (16x16: come il set; 20x20 e 32x32: 5)
DROP = dict(zip("ABCDEF", "!@#$%^"))     # segnaposto delle gocce (le maiuscole A-F servono alle munizioni)
BED, VIBS = V.BED, V.VIBS
GIRO2 = os.path.join(V.PROP, V.dirname(MODE), "modifiche_utente", "giro2")
# giro 2: ritocchi dell'utente al corpo comune della balestra {(x, y): lettera}, ricavati dalle PNG di pulling_2 (vedi docstring)
OVR16 = {(11, 1): '$', (3, 2): 'n', (4, 2): 'r', (2, 3): 'n', (8, 3): '%', (2, 4): 'r', (3, 6): '$', (8, 7): 't', (7, 8): 'x',
         (8, 8): 'n', (9, 8): 'p', (8, 9): 'p', (1, 11): '%'}
OVR20 = {(14, 0): '$', (2, 1): 'n', (3, 1): 'r', (4, 1): 'r', (1, 2): 'n', (8, 2): '5', (10, 2): '%', (11, 2): '%', (1, 3): 'r',
         (1, 4): 'r', (2, 6): '5', (10, 7): '5', (11, 7): '5', (3, 8): '5', (10, 8): '3', (11, 8): '4', (10, 9): 't', (11, 9): 't',
         (2, 10): '$', (8, 10): '3', (9, 10): 'x', (12, 10): 'p', (2, 11): '$', (7, 11): '3', (8, 11): '4', (9, 11): 'x',
         (12, 11): 'p', (10, 12): 'p', (11, 12): 'p', (10, 13): '5', (0, 14): '%'}
OVR = {16: OVR16, 20: OVR20}
# ritocchi di corda per stato (pixel menta che l'utente ha aggiunto e che non sono nella sovrapposizione T6): {stato: {(x, y): '6'}}
# pixel di corda ST16_0 che nella PNG dell'utente (pulling_0 16x16) sono rimasti onice (probabile svista: nell'oversized sono menta);
# vince la regola «la corda sta sempre sopra», quindi la PNG differisce qui e solo qui. {(stato, lato): {(x, y)}}
USERGAP = {("pulling_0", 16): {(9, 8), (8, 9)}}
STRING16 = {"standby": {(8, 7): "6", (7, 8): "6"}, "pulling_0": {(10, 8): "6", (8, 10): "6"}}
STRING20 = {"standby": {(11, 8): "6", (10, 9): "6", (9, 10): "6", (8, 11): "6"}}
USERD = {n: {p for p, ch in o.items() if ch in DROP.values()} for n, o in OVR.items()}     # gocce fisse dell'utente


def pal(extra=None):
    p = dict(V.PAL)
    for k, c in V.ORANGE.items():
        p[DROP[k]] = H.hexc(c)
    p.update({k: H.hexc(v) for k, v in S.AMMO.items()})      # munizione: colori T6, sovrascrive A-F maiuscole
    if extra:
        p.update({k: H.hexc(v) for k, v in extra.items()})
    return p


def drops(grid, seed):
    d = V.dots_out(grid, seed, dist=DIST[len(grid)])
    for (x, y), ch in d.items():
        assert grid[y][x] in BED
    return {p: DROP[ch] for p, ch in d.items()}


def with_drops(frame, d):
    rows = [list(r) for r in frame]
    for (x, y), ch in d.items():
        assert rows[y][x] in BED, (x, y, rows[y][x])      # la bedrock non anima: il punto va sempre
        rows[y][x] = ch
    return ["".join(r) for r in rows]


def frames_with_drops(grid, seed):
    d = drops(grid, seed)
    return [with_drops(fr, d) for fr in S.animate(grid)], d


def user_body(grid, seed):
    """Corpo comune della balestra con il giro 2 dell'utente: (griglia semantica ritoccata, gocce = generate + fisse dell'utente).
    Le gocce generate si calcolano sulla base T6 non ritoccata; quelle dell'utente le sostituiscono o si aggiungono."""
    n = len(grid)
    o = OVR[n]
    d = drops(grid, seed)
    g = [list(r) for r in grid]
    for (x, y), ch in o.items():
        if ch in DROP.values():
            assert grid[y][x] in BED, (x, y)
            d[(x, y)] = ch
        else:
            assert (x, y) not in d, ("goccia generata su un pixel ritoccato", x, y)
            g[y][x] = ch
    g = ["".join(r) for r in g]
    return [with_drops(fr, d) for fr in S.animate(g)], d, g


def crossbow_overlays(st):
    """Sovrapposizioni di S.crossbow_overlays, ma con la corda (prima griglia, ST16_x / ST20_x) come Top: la corda sta SEMPRE sopra
    gli override dell'utente del corpo (anche lettere di onice) e sotto la munizione. Piu' i ritocchi di corda per stato (STRING16/20)."""
    o16, o20, extra = S.crossbow_overlays(st)
    o16, o20 = [S.Top(o16[0])] + o16[1:], [S.Top(o20[0])] + o20[1:]
    if st in STRING16:
        o16.insert(1, S.patch(STRING16[st], 16))
    if st in STRING20:
        o20.insert(1, S.patch(STRING20[st], 20))
    return o16, o20, extra


BODY = {}     # corpo comune ritoccato (griglie semantiche), riempito da items()


def items():
    """nome -> (fotogrammi come griglie, palette); piu' le gocce usate: {nome: {(x, y): segnaposto}}."""
    out, dr = {}, {}
    for name, key in (("workstaff", "WS"), ("ultra_greatsword", "GS"), ("bident", "BI")):
        for suf, n in (("", 16), ("_oversized", 32)):
            g = S.GRIDS[f"{key}{n}"]
            out[name + suf], dr[name + suf] = frames_with_drops(g, f"{name}{suf}")
    base16, d16, G16 = user_body(S.GRIDS["CB16"], "crossbow16")
    base20, d20, G20 = user_body(S.GRIDS["CB20"], "crossbow20")
    BODY.update({16: G16, 20: G20})
    pals = {}
    for k in out:
        pals[k] = pal()
    for st in S.STATES:
        o16, o20, extra = crossbow_overlays(st)
        out[f"crossbow_{st}"] = [S.lay(fr, *o16) for fr in base16]
        out[f"crossbow_{st}_oversized"] = [S.big(S.lay(fr, *o20)) for fr in base20]
        pals[f"crossbow_{st}"] = pals[f"crossbow_{st}_oversized"] = pal(extra)
        dr[f"crossbow_{st}"], dr[f"crossbow_{st}_oversized"] = d16, {(x + 12, y + 12): c for (x, y), c in d20.items()}
    return out, pals, dr, (d16, d20)


def alpha(im):
    return im.getchannel("A").tobytes()


def build():
    os.makedirs(OUT, exist_ok=True)
    grids, pals, dr, (d16, d20) = items()
    res = {}
    for name in S.ALL:
        ims = [S.render(fr, pals[name]) for fr in grids[name]]
        ims[0].save(os.path.join(OUT, f"t7_{name}.png"))
        T.save_anim(ims, os.path.join(OUT, f"t7_{name}_anim"))
        res[name] = ims
    # --- asserzioni ---------------------------------------------------------------------------------------
    stats = {}
    for name in S.ALL:
        ims = res[name]
        assert len(ims) == T.FRAMES
        af = T.strip_frames(os.path.join(OUT, f"t7_{name}_anim.png"))
        assert len(af) == T.FRAMES and af[0].tobytes() == ims[0].tobytes(), ("anim", name)
        assert ims[0].tobytes() == Image.open(os.path.join(OUT, f"t7_{name}.png")).convert("RGBA").tobytes(), name
        # sagome identiche al T6: icona ferma e tutti i 20 fotogrammi, contro i PNG T6 su disco
        t6 = Image.open(os.path.join(T6SP, f"t6_{name}.png")).convert("RGBA")
        assert t6.size == ims[0].size and alpha(t6) == alpha(ims[0]), ("sagoma", name)
        t6f = T.strip_frames(os.path.join(T6SP, f"t6_{name}_anim.png"))
        assert len(t6f) == T.FRAMES and all(alpha(a) == alpha(b) for a, b in zip(t6f, ims)), ("sagoma anim", name)
        assert open(os.path.join(OUT, f"t7_{name}_anim.png.mcmeta")).read() == open(os.path.join(T6SP, f"t6_{name}_anim.png.mcmeta")).read()
        # oversized balestra: bbox dentro (12,12)-(32,32)
        if name.startswith("crossbow") and name.endswith("_oversized"):
            for im in ims:
                bx = im.getchannel("A").getbbox()
                assert bx and bx[0] >= 12 and bx[1] >= 12 and bx[2] <= 32 and bx[3] <= 32, (name, bx)
        stats[name] = len(dr[name])
    # --- balestra: corpo comune e gocce uguali in tutti i 12 stati ----------------------------------------------
    for suf, d, size in (("", d16, 16), ("_oversized", d20, 20)):
        off = 0 if not suf else 12
        # celle toccate da una sovrapposizione in almeno uno stato (corda, munizione, patch)
        cover = set()
        for st in S.STATES:
            o = crossbow_overlays(st)[0 if not suf else 1]
            for g in o:
                cover |= {(x + off, y + off) for y, r in enumerate(g) for x, ch in enumerate(r) if ch != "."}
        for f in range(T.FRAMES):
            ref = res["crossbow_standby" + suf][f]
            for st in S.STATES:
                im = res[f"crossbow_{st}{suf}"][f]
                for y in range(im.height):
                    for x in range(im.width):
                        if (x, y) not in cover:
                            assert im.getpixel((x, y)) == ref.getpixel((x, y)), ("corpo", st, suf, f, x, y)
        base = S.GRIDS["CB16" if not suf else "CB20"]
        dist = DIST[size]
        pts = sorted(d)
        assert all(base[y][x] in BED and any(V._at(base, x + dx, y + dy) in VIBS for dx, dy in V.N4)
                   for x, y in pts if (x, y) not in USERD[size])      # le gocce dell'utente sono libere dalla regola
        # le gocce coincidono in tutti gli stati (dove non ricoperte dalla corda o dalla munizione)
        for st in S.STATES:
            im = res[f"crossbow_{st}{suf}"][0]
            for (x, y), ch in d.items():
                if (x + off, y + off) not in cover:
                    assert im.getpixel((x + off, y + off)) == pal()[ch], (st, suf, x, y)
        stats["crossbow_coperte" + suf] = sorted({(x, y) for (x, y) in d if (x + off, y + off) in cover})
        # --- giro 2: pulling_2 identica alla PNG dell'utente; negli altri stati il ritocco vale dove non coperto ---
        user = Image.open(os.path.join(GIRO2, f"t7_crossbow_pulling_2{suf}.png")).convert("RGBA")
        assert res["crossbow_pulling_2" + suf][0].tobytes() == user.tobytes(), ("pulling_2 != PNG utente", suf)
        for st in ("standby", "pulling_0"):      # giro 2b: ritocchi a mano di standby e pulling_0 (corda allungata e sopra l'onice)
            u2 = Image.open(os.path.join(GIRO2, f"t7_crossbow_{st}{suf}.png")).convert("RGBA")
            diff = {(x, y) for y in range(u2.height) for x in range(u2.width)
                    if res[f"crossbow_{st}{suf}"][0].getpixel((x, y)) != u2.getpixel((x, y))}
            assert diff == {(x + off, y + off) for x, y in USERGAP.get((st, size), ())}, (st, "!= PNG utente", suf, sorted(diff))
        # la corda della sovrapposizione di stato e' menta nel fotogramma 0 in ogni stato, salvo dove la munizione la ricopre
        mint = tuple(V.PAL["6"])[:3]
        for st in S.STATES:
            ov = crossbow_overlays(st)[0 if not suf else 1]
            im0 = res[f"crossbow_{st}{suf}"][0]
            k = 1 + (st in (STRING16 if not suf else STRING20))      # griglie di corda: ST + ritocco di corda; il resto sta sopra
            on = lambda gs: {(x, y) for g in gs for y, r in enumerate(g) for x, ch in enumerate(r) if ch != "."}
            cells = on(ov[:k]) - on(ov[k:])
            for (x, y) in cells:
                assert im0.getpixel((x + off, y + off))[:3] == tuple(mint)[:3] and im0.getpixel((x + off, y + off))[3] == 255, ("corda non menta", st, suf, x, y)
        body, ovr, P = BODY[size], OVR[size], pal()
        for st in S.STATES:
            ov = crossbow_overlays(st)[0 if not suf else 1]
            im0 = res[f"crossbow_{st}{suf}"][0]
            ncov = 0
            for (x, y), ch in ovr.items():
                hidden = any(g[y][x] != "." and (isinstance(g, S.Top) or body[y][x] not in S.GEMCH) for g in ov)
                if hidden:
                    ncov += 1
                    continue
                want = user.getpixel((x + off, y + off))
                assert im0.getpixel((x + off, y + off)) == want, ("ritocco utente", st, suf, x, y)
            stats[f"giro2_coperti_{st}{suf}"] = ncov
    # --- gocce degli altri pezzi: distanza minima e vibranio intatto -----------------------------------------------------
    for name in ("workstaff", "ultra_greatsword", "bident"):
        for suf, n in (("", 16), ("_oversized", 32)):
            g = S.GRIDS[{"workstaff": "WS", "ultra_greatsword": "GS", "bident": "BI"}[name] + str(n)]
            d = dr[name + suf]
            assert all(g[y][x] in BED and any(V._at(g, x + dx, y + dy) in VIBS for dx, dy in V.N4) for x, y in d)
            im = res[name + suf][0]
            for y in range(n):
                for x in range(n):
                    if g[y][x] in VIBS:
                        assert im.getpixel((x, y)) == pal()[g[y][x]], (name, x, y)
    return res, stats, dr


def density(res, stats):
    print("gocce (px)  e  pixel di vibranio per pezzo:")
    for name in S.ALL:
        if name.startswith("crossbow") and not name.startswith("crossbow_standby"):
            continue
        key = {"workstaff": "WS", "ultra_greatsword": "GS", "bident": "BI", "crossbow_standby": "CB"}[name.replace("_oversized", "")]
        n = 20 if key == "CB" and name.endswith("_oversized") else (32 if name.endswith("_oversized") else 16)
        g = S.GRIDS[f"{key}{n}"]
        nv = sum(ch in VIBS for r in g for ch in r)
        print(f"  {name:32s} gocce {stats[name]:3d}  vibranio {nv:3d}  rapporto {stats[name] / nv:.2f}  su {sum(ch != '.' for r in g for ch in r)} px")


def confronto(path, res):
    BG = (92, 92, 100, 255)
    k16, k32 = 8, 4
    cw = 16 * k16
    pad, lab = 10, 150
    row_h = cw + 22
    per = 8
    n = len(S.ORDER)
    cols = (n + per - 1) // per
    blk_w = lab + 4 * cw + 3 * pad + 24
    sheet = Image.new("RGBA", (cols * blk_w + 16, 44 + per * row_h + 10), (60, 60, 66, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), "Speciali: T6 Castone | T7 v2 (puntini fuori); per pezzo 16x16 T6 | T7 v2, oversized 32x32 T6 | T7 v2",
            fill=(255, 255, 255, 255), font=T.FONT_S)
    for i, nm in enumerate(S.ORDER):
        bx, by = 8 + (i // per) * blk_w, 44 + (i % per) * row_h
        dr.text((bx, by + cw // 2 - 8), S.NOMI[nm], fill=(235, 235, 240, 255), font=T.FONT_S)
        x = bx + lab
        for suf, k, src in (("", k16, "t6"), ("", k16, "t7"), ("_oversized", k32, "t6"), ("_oversized", k32, "t7")):
            p = os.path.join(T6SP if src == "t6" else OUT, f"{src}_{nm}{suf}.png")
            im = H.on_bg(H.up(Image.open(p).convert("RGBA"), k), BG)
            sheet.alpha_composite(im, (x, by))
            x += cw + pad
    sheet.save(path)


def main():
    res, stats, dr = build()
    density(res, stats)
    confronto(CONF, res)
    cov = {k: v for k, v in stats.items() if k.startswith("crossbow_coperte")}
    for k in sorted(k for k in stats if k.startswith("giro2_coperti")):
        print(f"  {k}: {stats[k]} pixel del ritocco coperti da corda/munizione (non replicati)")
    print("ok speciali T7 v2:", len(S.ALL), "pezzi x 20 fotogrammi; gocce coperte da corda/munizione in qualche stato:", cov)


if __name__ == "__main__":
    main()
