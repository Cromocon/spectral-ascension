"""T8 «Argent» (unobtainium, bianco/moonstone): il T7 v2 «puntini fuori» ricolorato, stesse sagome e stessa animazione.

Metodo: mappatura colore -> colore sui PNG FINALI del T7 v2 su disco (icone, *_anim, armatura, 20 fotogrammi),
quindi eredita tutti i ritocchi a mano dell'utente (USER_ICONS, USER_ARMOR, canne, corda). Le sagome (alpha) sono
identiche al T7 (asserzione). I colori del T7 sono univoci per ruolo, tranne la corda dell'arco / il filo della canna,
che nel T7 hanno lo stesso mint #8ab4b6 della cifra 6 del metallo: si risolvono per POSIZIONE (vedi STRING_MASK).
Speciali: texture_t8_speciali.py (stessa mappatura, corda e munizioni per posizione).

RUOLI (T7 v2 -> T8)
  corpo, metallo nero 0-6      -> argento chiaro (ARGENT). Stessa luminosita' relativa (0 contorno ... 6 riflesso).
  vibranio a-f (parte tagliente) -> unobtainium viola, base #7e35c3 = tono c (UNOB)
  puntini d'allthemodium A-F   -> puntini di vibranio verde acqua a-f, stesse posizioni, stesso tono (VIB_DOT = VIB del T7)
  gemme onice (q-x)            -> moonstone: bianco perlato con riflessi lilla/azzurrini (MOON)
  stoffa g-k (petrolio)        -> blu ardesia (CLOTH)
  corda dell'arco + freccia + filo della canna (mint) -> lilla pallido (STRING)
  W (riflesso bianco)          -> invariato

SCELTE
  Contorno (ARGENT 0): #4a4f66 grigio-blu medio-scuro, NON nero. Sullo sfondo dell'inventario (#8b8b8b) ha contrasto
  ~2.3:1 con lo sfondo e ~3:1 col corpo (cifra 2 #9ba1b5), cosi' la sagoma si legge sia su grigio sia su sfondi scuri
  (su scuro il corpo chiaro stacca da solo). Il nero puro su corpo bianco sarebbe stato duro e avrebbe tolto il «colare».
  Rampa corpo: 0 #4a4f66, 1 #6b7189, 2 #9ba1b5, 3 #b8bdce, 4 #d2d6e2, 5 #eaecf3, 6 #fbf9ff. La 2 e' la piu' usata
  (21k px nel T7): abbastanza scura da non fondersi col #8b8b8b e da lasciare spazio a 3-6 come luci.
  Unobtainium (base #7e35c3 = c): a #1d0b36 contorno, b #4b1f88 ombra, c #7e35c3 base, d #a661ea, e #cfa3ff, f #f2e6ff.
  Moonstone (rim/dark/mid/sheen): #6f6aa0, #a9a6d9, #dcd9f6, #f4fbff. Lilla piu' saturo del corpo, cosi' le gemme non si
  confondono con l'argento; pulsano come l'onice (dark -> mid -> sheen).
  Stoffa blu ardesia: g #262a47, h #363c63, i #4a527f, j #5f689b, k #7882b6. Fredda e scura: stacca dall'argento chiaro e
  dall'unobtainium (tinta piu' blu e meno satura del viola).
  Corda/filo: #c6b2f2 lilla pallido: piu' saturo e piu' violaceo del corpo, piu' chiaro dell'unobtainium.
  Arco: la corda e la freccia (pixel mint non del flettente) prendono lo stesso lilla.

Animazione: identica al T7 (20 fotogrammi da 5 tick, fotogramma 0 = icona ferma): vale per costruzione.

Uso:  python3 -I tools/texture_t8.py     # scrive in design/texture/proposte/t8-argent/ (set/, armatura/, vetrina_t8.png,
                                          # confronto_t6_t7_t8.png); legge soltanto t7-v2-puntini-fuori e t6-castone.
"""
import io
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_mix as M  # noqa: E402
import texture_t6castone as T  # noqa: E402
import texture_t7v2 as X  # noqa: E402

PROP = os.path.join(H.ROOT, "design", "texture", "proposte")
SRC = os.path.join(PROP, "t7-v2-puntini-fuori")
OUT = os.path.join(PROP, "t8-argent")
T6 = T.OUT

ARGENT = dict(zip("0123456", ("#4a4f66", "#6b7189", "#9ba1b5", "#b8bdce", "#d2d6e2", "#eaecf3", "#fbf9ff")))
UNOB = dict(zip("abcdef", ("#1d0b36", "#4b1f88", "#7e35c3", "#a661ea", "#cfa3ff", "#f2e6ff")))
MOON = {"rim": "#6f6aa0", "dark": "#a9a6d9", "mid": "#dcd9f6", "sheen": "#f4fbff"}
CLOTH = dict(zip("ghijk", ("#262a47", "#363c63", "#4a527f", "#5f689b", "#7882b6")))
STRING = "#c6b2f2"
VIB_DOT = X.VIB                      # verde acqua del T7: a..f

rgb = lambda h: tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))
X_PAL = {k: v[:3] for k, v in X.PAL.items()}

# --- mappa colore T7 -> colore T8 (per lettera della palette T7, tranne 6/z risolti per posizione) -------------------
CMAP = {}


def put(src_letter, dst_hex):
    CMAP[X_PAL[src_letter]] = rgb(dst_hex)


for k in "012345":
    put(k, ARGENT[k])
put("6", ARGENT["6"])                                  # '6' = 'z' come colore: la corda e' fatta a parte per posizione
for k in "abcdef":
    put(k, UNOB[k])                                    # vibranio -> unobtainium
    put(k.upper(), VIB_DOT[k])                         # allthemodium A-F -> vibranio a-f, stesse posizioni
for k, v in CLOTH.items():
    put(k, v)
for role, chars in X.ONYX_CHARS.items():
    for ch in chars:
        put(ch, MOON[role])
CMAP[(255, 255, 255)] = (255, 255, 255)                # W
assert len(set(CMAP)) == len(CMAP)
MINT = X_PAL["6"]
assert X_PAL["z"] == MINT

# --- posizioni della corda (pixel mint che NON sono corpo) --------------------------------------------------------------
BOWS = ("bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2")
LIMB6 = {(9, 1), (11, 1), (8, 2)}                      # le tre cifre 6 del flettente nel T6 (le altre 6 dell'arco = corda/freccia)
STRING_MASK = {n: {(x, y) for y in range(16) for x in range(16) if T.ITEMS[n][y][x] == "6"} - LIMB6 for n in BOWS}
STRING_MASK["fishing_rod"] = {(14, y) for y in range(2, 9)}    # filo della canna (T6: colonna 14, righe 2-8)


def recolor(im, mask=(), tile=None):
    """mask: pixel (x, y) della tessera 16x16 che, se mint, diventano corda. tile: altezza della tessera (strisce animate)."""
    im = im.convert("RGBA")
    out = Image.new("RGBA", im.size)
    src, dst = im.load(), out.load()
    for y in range(im.height):
        for x in range(im.width):
            p = src[x, y]
            if p[3] == 0:
                continue
            c = p[:3]
            if c == MINT and (x, y % tile if tile else y) in mask:
                dst[x, y] = rgb(STRING) + (255,)
            else:
                assert c in CMAP, ("colore T7 non mappato", c, x, y)
                dst[x, y] = CMAP[c] + (255,)
    assert out.getchannel("A").tobytes() == im.getchannel("A").tobytes()
    return out


def sil(im):
    return im.convert("RGBA").getchannel("A").tobytes()


def build():
    sdir, adir = os.path.join(OUT, "set"), os.path.join(OUT, "armatura")
    for d in (sdir, adir):
        os.makedirs(d, exist_ok=True)
    icons = {}
    for n in T.SET_ORDER:
        mask = STRING_MASK.get(n, set())
        a7 = Image.open(os.path.join(SRC, "set", f"t7_{n}.png"))
        i8 = recolor(a7, mask)
        s7 = Image.open(os.path.join(SRC, "set", f"t7_{n}_anim.png"))
        s8 = recolor(s7, mask, tile=16)
        assert s8.size == (16, 16 * 20), n                                   # 20 fotogrammi
        assert s8.crop((0, 0, 16, 16)).tobytes() == i8.tobytes(), n          # fotogramma 0 = icona
        assert sil(i8) == sil(a7) and sil(s8) == sil(s7)                     # sagome come il T7
        assert sil(i8) == sil(Image.open(os.path.join(T6, "set", f"t6_{n}.png")))
        i8.save(os.path.join(sdir, f"t8_{n}.png"))
        s8.save(os.path.join(sdir, f"t8_{n}_anim.png"))
        with open(os.path.join(sdir, f"t8_{n}_anim.png.mcmeta"), "w") as fh:
            fh.write(open(os.path.join(SRC, "set", f"t7_{n}_anim.png.mcmeta")).read())
        icons[n] = i8
        # puntini: i verdi del T8 stanno dove stavano gli arancio del T7
        orange = {(x, y) for y in range(16) for x in range(16)
                  if a7.getpixel((x, y))[:3] in {X_PAL[k] for k in "ABCDEF"}}
        green = {(x, y) for y in range(16) for x in range(16)
                 if i8.getpixel((x, y))[:3] in {rgb(v) for v in VIB_DOT.values()}}
        assert orange == green, n
        assert not any(i8.getpixel(p)[:3] == rgb(STRING) for p in orange)
    # armatura
    t7 = Image.open(os.path.join(SRC, "armatura", "t7_armor.png"))
    tex = recolor(t7)
    tex.save(os.path.join(adir, "t8_armor.png"))
    frames = []
    for i in range(20):
        fr = recolor(Image.open(os.path.join(SRC, "armatura", f"t7_armor_{i}.png")))
        fr.save(os.path.join(adir, f"t8_armor_{i}.png"))
        frames.append(fr)
    assert frames[0].tobytes() == tex.tobytes()
    assert sil(tex) == sil(t7)
    n_or = sum(1 for p in t7.getdata() if p[:3] in {X_PAL[k] for k in "ABCDEF"})
    n_gr = sum(1 for p in tex.getdata() if p[:3] in {rgb(v) for v in VIB_DOT.values()})
    assert n_or == n_gr
    man_bg = (52, 52, 60, 255)
    H.on_bg(M.mannequin(tex, 8), man_bg).save(os.path.join(adir, "manichino.png"))
    imgs = [H.on_bg(M.mannequin(fr, 6), man_bg).convert("RGB") for fr in frames]
    imgs[0].save(os.path.join(adir, "manichino_animato.gif"), save_all=True, append_images=imgs[1:],
                 duration=T.FRAMETIME * 50, loop=0, disposal=1)
    print("ok t8:", len(icons), "icone, 20 fotogrammi, puntini armatura", n_gr)
    return icons, tex


def _grow(img, h):
    out = Image.new("RGB", (img.width, h), (52, 52, 60))
    out.paste(img, (0, 0))
    return out


def vetrina(path):
    from PIL import ImageFont
    F = lambda s: ImageFont.truetype("/usr/share/fonts/TTF/DejaVuSans.ttf", s) if os.path.exists("/usr/share/fonts/TTF/DejaVuSans.ttf") else ImageFont.load_default()
    BG, SL, TX, TI = (52, 52, 60), (139, 139, 139), (225, 225, 230), (198, 178, 242)   # SL = #8b8b8b, slot dell'inventario
    COLS, CW, CH, X0 = 8, 144, 170, 24
    img = Image.new("RGB", (1184, 1000), BG)
    d = ImageDraw.Draw(img)
    d.text((16, 14), "T8 Argent (unobtainium, moonstone)", fill=TI, font=F(22))
    y = 56
    d.text((16, y), "Set (sfondo = slot dell'inventario #8b8b8b)", fill=TX, font=F(14))
    y += 30
    for i, n in enumerate(T.SET_ORDER):
        x, yy = X0 + (i % COLS) * CW, y + (i // COLS) * CH
        im = Image.open(os.path.join(OUT, "set", f"t8_{n}.png")).convert("RGBA").resize((128, 128), Image.NEAREST)
        d.rectangle((x, yy, x + 127, yy + 127), fill=SL)
        img.paste(im, (x, yy), im)
        lab = T.NOMI[n]
        d.text((x + 64 - d.textlength(lab, font=F(14)) / 2, yy + 134), lab, fill=TX, font=F(14))
    y += 2 * CH + 10
    d.text((16, y), "Set su sfondo scuro", fill=TX, font=F(14))
    y += 24
    for i, n in enumerate(T.SET_ORDER):
        im = Image.open(os.path.join(OUT, "set", f"t8_{n}.png")).convert("RGBA").resize((64, 64), Image.NEAREST)
        x = 24 + i * 76
        d.rectangle((x, y, x + 63, y + 63), fill=(28, 28, 34))
        img.paste(im, (x, y), im)
    y += 84
    sp = os.path.join(OUT, "speciali")
    if os.path.isdir(sp):                                   # speciali (texture_t8_speciali.py), stesse righe della vetrina T7
        import texture_t8_speciali as SP
        for title, suf, size in (("Speciali 16\u00d716", "", 16), ("Speciali 32\u00d732 (in mano)", "_oversized", 32)):
            d.text((16, y), title, fill=TX, font=F(14))
            y += 30
            for i, n in enumerate(SP.VORD):
                x, yy = X0 + (i % COLS) * CW, y + (i // COLS) * CH
                if yy + CH > img.height:
                    img = _grow(img, yy + CH)
                    d = ImageDraw.Draw(img)
                im = Image.open(os.path.join(sp, f"t8_{n}{suf}.png")).convert("RGBA").resize((128, 128), Image.NEAREST)
                d.rectangle((x, yy, x + 127, yy + 127), fill=SL)
                img.paste(im, (x, yy), im)
                lab = SP.NOMI_V[n]
                d.text((x + 64 - d.textlength(lab, font=F(14)) / 2, yy + 134), lab, fill=TX, font=F(14))
            y += 2 * CH + 10
    d.text((16, y), "Armatura indossata", fill=TX, font=F(14))
    y += 24
    man = M.mannequin(Image.open(os.path.join(OUT, "armatura", "t8_armor.png")).convert("RGBA"), 8)
    bg = Image.new("RGBA", man.size, BG + (255,))
    bg.alpha_composite(man)
    img = _grow(img, y + man.height + 16)
    ImageDraw.Draw(img).text((16, y - 24), "Armatura indossata", fill=TX, font=F(14))
    img.paste(bg.convert("RGB"), (16, y))
    img.save(path)


def confronto(path):
    k, pad, BG = 6, 6, (139, 139, 139, 255)
    cw = 16 * k
    cell = 3 * cw + 2 * pad
    per = 5
    names = T.SET_ORDER
    rows = (len(names) + per - 1) // per
    W = per * (cell + 20) + 16
    sheet = Image.new("RGBA", (W, 56 + rows * (cw + 28) + 10), (60, 60, 66, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), "T6 Castone | T7 Umbral | T8 Argent", fill=(255, 255, 255, 255), font=T.FONT_B)
    for i, n in enumerate(names):
        x, y = 8 + (i % per) * (cell + 20), 40 + (i // per) * (cw + 28)
        paths = (os.path.join(T6, "set", f"t6_{n}.png"), os.path.join(SRC, "set", f"t7_{n}.png"),
                 os.path.join(OUT, "set", f"t8_{n}.png"))
        for j, p in enumerate(paths):
            sheet.alpha_composite(H.on_bg(H.up(Image.open(p).convert("RGBA"), k), BG), (x + j * (cw + pad), y))
        dr.text((x, y + cw + 3), T.NOMI[n], fill=(235, 235, 240, 255), font=T.FONT_S)
    sheet.save(path)


def main():
    build()
    vetrina(os.path.join(OUT, "vetrina_t8.png"))
    confronto(os.path.join(OUT, "confronto_t6_t7_t8.png"))


if __name__ == "__main__":
    main()
