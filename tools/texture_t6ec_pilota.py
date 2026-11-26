"""T6 Trichromatic, prova «Eternal Classics» (pilota, secondo tentativo): spada, workstaff, balestra a riposo.

Regole di questa prova (dall'utente):
- palette e posizione delle gemme = quelle del T6 attuale (fotogramma 0); si aggiungono solo toni intermedi della
  stessa famiglia per allungare le rampe;
- la sagoma cambia solo nel senso di EC (più piena, più massiccia): v1 moderata, v2 decisa;
- disegno a mano: ogni sprite è una griglia di caratteri scritta riga per riga qui sotto. Il codice converte e basta.

Formato delle righe: "x:pixel x:pixel ..." = segmenti che partono dalla colonna x; il resto è trasparente.
Una riga vuota "" è tutta trasparente.

Uso:  python3 tools/texture_t6ec_pilota.py   # scrive SOLO in design/texture/proposte/t6-ec/pilota/
"""
import colorsys
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "design", "texture", "proposte", "t6-ec", "pilota")
CUR = os.path.join(ROOT, "src", "main", "resources", "assets", "spectral_ascension", "textures", "item")
REF_D = os.path.join(ROOT, "design", "per_brz", "1_spectrum_default", "assets", "spectrum", "textures", "item")
REF_E = os.path.join(ROOT, "design", "per_brz", "2_spectrum_eternal_classics", "assets", "spectrum", "textures", "item")

# ---------------------------------------------------------------------------
# Palette: tutti i colori vengono dal fotogramma 0 del T6 attuale, tranne quelli marcati «+» (toni intermedi
# della stessa famiglia, per rampe a 5-6 gradini e per il contorno scuro delle gemme).
# ---------------------------------------------------------------------------
PAL = {
    # onice / ardesia (0 = contorno)
    "0": "#110e1d", "1": "#1d1830",  # + 1: tra contorno e 2, per incavi e scanalature
    "2": "#2d2843", "3": "#3c3657", "4": "#4f486e", "5": "#675f89", "6": "#8a83ad",
    # oro (A = ombra più scura, F = luce crema)
    "A": "#3a1406", "B": "#7c2f0c", "C": "#bc5a10", "D": "#ea9a27", "E": "#ffc75e", "F": "#fff1bc",
    # topazio ciano
    "r": "#12708f", "s": "#2fc0e0", "t": "#b4f6ff",
    # ametista magenta
    "n": "#8a1c78", "o": "#d443a8", "p": "#ffa0e8",
    # citrino giallo
    "v": "#9c7f10", "w": "#eedc30", "x": "#fffcb0",
}
ADDED = {"1"}


def grid(size, rows):
    assert len(rows) == size, (size, len(rows))
    out = []
    for y, row in enumerate(rows):
        line = ["."] * size
        for seg in row.split():
            x0, px = seg.split(":")
            x0 = int(x0)
            assert x0 + len(px) <= size, (y, seg)
            for i, c in enumerate(px):
                line[x0 + i] = c
        out.append("".join(line))
    return out


def G16(text):
    rows = [r.strip() for r in text.strip().splitlines()]
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [(i, r, len(r)) for i, r in enumerate(rows)]
    return rows


# ---------------------------------------------------------------------------
# SPADA 16x16. Lama «/» con contorno pieno; filo alto-sinistro in oro chiaro, anima d'onice a due toni, filo
# basso-destro in oro d'ombra; punta crema. Disco dell'elsa con le gemme negli stessi pixel dell'attuale:
# topazio (5,7)(4,8)(5,8)(6,8), citrino (3,9)(4,9), ametista (5,9)(6,9)(7,9).
# ---------------------------------------------------------------------------
SWORD_V1 = G16("""
.............000
............0FE0
...........0E6E0
..........0F64D0
.........0D64C0.
....00..0D53C0..
...05300D53B0...
..0DEtC042B0....
..0FtsrC2A0.....
.0ExwponA0......
..0BCCCBBA0.....
...050BC20......
..060.0B0.......
.050...0........
AEC.............
AB..............
""")

SWORD_V2 = G16("""
............0000
...........0FED0
..........0F65D0
.........0E653C0
...00...0E653C0.
..0650.0D643C0..
..05430D542B0...
.0FEDtC032B0....
0EDCtsrC2B0.....
0DCxwponB0......
0BCCCBBBA0......
.00530BCA0......
.0530.0B0.......
0DC0...0........
EFB0............
CBA.............
""")

# ---------------------------------------------------------------------------
# WORKSTAFF 16x16. Barra d'oro in alto, barra d'oro a destra, anello d'onice con castone d'oro e gemme 2x2 in
# (9,5) topazio chiaro, (10,5) topazio, (9,6) citrino, (10,6) ametista, come l'attuale.
# ---------------------------------------------------------------------------
STAFF_V1 = G16("""
....0000000..000
....0FEEDD0.0FD0
.....0DC65000EC0
......066655400.
......06EDDC30..
.....065DtsC4000
.....004CwoB34E0
.......0CBBA3DC0
......0502332DB0
.....060.03000C0
....050..00..0B0
...060........00
..050...........
.CE0............
CFA.............
AB..............
""")

STAFF_V2 = G16("""
...00000000.0000
...0FEEEDD0.0ED0
...0DDCCCB000DC0
.....06666550DC0
....0665EDDC3DB0
....0654DtsC2EC0
....0543CwoB2DC0
.....032CBBA1DB0
.....02111110CB0
....050000000CA0
...060.......0B0
..050.........00
.050............
0DC0............
EFB0............
CBA.............
""")

# ---------------------------------------------------------------------------
# BALESTRA A RIPOSO 16x16. Anello di gemme all'incrocio dei flettenti negli stessi pixel dell'attuale:
# ametista (3,2)(2,3), topazio (4,2)(2,4), citrino (5,3)(3,5); sul fusto topazio (8,7), citrino (7,8), ametista (8,8).
# ---------------------------------------------------------------------------
XBOW_V1 = G16("""
...........000..
...00.00000CE0..
..0pt0CDDEFEDC0.
.0o22x320000DB0.
.0s26540...600..
..0w34560.6.....
.0C2234456......
.0D20234t50.....
.0D0.02wo450....
.0E0..6234450...
.0F0.6.0234450..
0CE06...023450..
0DFC0....023450.
.0CB0......02450
..00........0350
.............00.
""")

XBOW_V2 = G16("""
.....0000000000.
..0000EFFEEEDD0.
..0pt0DDDDCCCBC0
.0o22x3200000DB0
.0s244560..6000.
.00w3445606.....
0DC22344560.....
0EC02234t560....
0EC0022wo4560...
0FD0.062344560..
0ED0.6022344560.
0ED06..022344560
0FEC0...02234450
.0CB0....0234560
..00.......02350
............000.
""")

# ---------------------------------------------------------------------------
# WORKSTAFF OVERSIZED 32x32 (ingombro attuale: x 0-30, y 1-31). Gemme in (19,11)(20,11) topazio, (19,12) citrino,
# (20,12) ametista, come l'attuale.
# ---------------------------------------------------------------------------
STAFF_OV_V1 = grid(32, [
    "",
    "14:0000000",
    "14:0EFFED0 26:0000",
    "15:00CCB0 25:0EFD0",
    "17:050 25:065D0",
    "17:040000 25:043C0",
    "17:006665000000",
    "16:0665554430",
    "15:06413111320",
    "15:051131331420",
    "15:051EEDC21450",
    "15:041DtsB11350 28:000",
    "15:035CwoA344500 28:5E0",
    "16:04BBAA1240430D0",
    "16:0334111230000C0",
    "15:0600223320 28:0B0",
    "14:050 19:00000 28:0A0",
    "13:060 29:00",
    "12:050",
    "11:050",
    "10:050",
    "9:040",
    "8:040",
    "7:040",
    "6:040",
    "5:030",
    "4:030",
    "3:030",
    "2:030",
    "1:DE0",
    "0:CFA",
    "0:AB",
])

STAFF_OV_V2 = grid(32, [
    "",
    "13:000000000",
    "13:0EFFFEED0 25:00000",
    "13:0DDDCCCB0 24:0EFED0",
    "13:000005400 24:065DC0",
    "17:0650 24:0543C0",
    "16:005400000432B0",
    "15:06666555430000",
    "14:0665555444320",
    "14:0651154111320",
    "14:0651EEDC11320 27:0000",
    "14:0545DtsB112205ED0",
    "14:0544CwoA332104DC0",
    "14:0431BBAA112103DB0",
    "15:0311321121002CB0",
    "15:0532222110 27:0CB0",
    "14:0640000000 28:0A0",
    "13:0530 29:00",
    "12:0530",
    "11:0630",
    "10:0530",
    "9:0530",
    "8:0430",
    "7:0530",
    "6:0430",
    "5:0430",
    "4:0420",
    "3:0420",
    "2:0EC0",
    "0:0EDB0",
    "0:EFC0",
    "0:CA0",
])

# ---------------------------------------------------------------------------
# BALESTRA A RIPOSO OVERSIZED 32x32 (ingombro attuale: x 12-31, y 12-31). Anello di gemme in (14,13) ametista,
# (15,13)(16,13)(13,15)(13,16) topazio, (17,14)(14,17) citrino, (13,14) ametista; sul fusto topazio 2x2 in
# (22-23,20-21), citrino 2x2 in (20-21,22-23), ametista 2x2 in (22-23,22-23), come l'attuale.
# ---------------------------------------------------------------------------
XBOW_OV_V1 = grid(32, [
    "", "", "", "", "", "", "", "", "", "", "", "",
    "14:000 18:000000000DE0",
    "13:0ots00CCDDEEFEEDC0",
    "12:0o322x233322000DCB0",
    "12:0s22133456 27:600",
    "12:0s21233450 26:6",
    "13:0w2334450 26:6",
    "13:0223344560 25:6",
    "12:0C12233444556 25:0",
    "12:0C12333445ts0",
    "12:0D12002344sr50",
    "12:0D20 18:03xwpo450",
    "12:0EB0 18:02wvon350",
    "12:0E0 19:60223445 27:00",
    "12:0F0 18:6 21:023344560",
    "12:CE0 16:66 22:00234450",
    "12:DFC6 24:023445 30:0",
    "12:CDB0 24:0223450",
    "12:0BA0 26:023440",
    "13:00 27:00200",
    "29:00",
])

XBOW_OV_V2 = grid(32, [
    "", "", "", "", "", "", "", "", "", "", "", "",
    "14:000 18:000000000ED0",
    "13:0ots00DEEEFFEEEDC0",
    "12:0o322x0CCDDDDCCCBB0",
    "12:0s2213345560000DB0",
    "12:0s21233445 23:0 27:600",
    "13:0w22334456 23:0 26:6",
    "13:0122334456 23:0 25:6",
    "12:0DC1223344556 25:0",
    "12:0EC12233 20:45ts50",
    "12:0ED00233 21:4sr560",
    "12:0FD0 17:023xwpo4560",
    "12:0EC0 17:022wvon3450",
    "12:0EC0 18:06223344560",
    "12:0DC0 18:6 21:02234456 29:0",
    "12:0DB0 17:6 22:02234456 30:0",
    "12:DEEC6 23:0233456 30:0",
    "12:CDDB0 24:023345 30:0",
    "12:0BA0 25:0234450",
    "13:00 26:023340",
    "27:00000",
])

SPRITES = {
    "trichromatic_sword": (SWORD_V1, SWORD_V2),
    "trichromatic_workstaff": (STAFF_V1, STAFF_V2),
    "trichromatic_workstaff_oversized": (STAFF_OV_V1, STAFF_OV_V2),
    "trichromatic_crossbow_standby": (XBOW_V1, XBOW_V2),
    "trichromatic_crossbow_standby_oversized": (XBOW_OV_V1, XBOW_OV_V2),
}
ANALOG = {
    "trichromatic_sword": "bedrock_sword",
    "trichromatic_workstaff": "glass_crest_workstaff",
    "trichromatic_workstaff_oversized": "glass_crest_workstaff_oversized",
    "trichromatic_crossbow_standby": "glass_crest_crossbow_standby",
    "trichromatic_crossbow_standby_oversized": "glass_crest_crossbow_standby_oversized",
}


def hexc(h):
    return int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255


def render(rows):
    n = len(rows)
    im = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == n
        for x, c in enumerate(row):
            if c != ".":
                im.putpixel((x, y), hexc(PAL[c]))
    return im


def frame0(path):
    im = Image.open(path).convert("RGBA")
    w = im.width
    return im.crop((0, 0, w, w))


def up(im, k):
    return im.resize((im.width * k, im.height * k), Image.NEAREST)


def tavola(path, news):
    k = 8
    font = ImageFont.load_default()
    pad, lab = 16, 18
    rows = []
    for name in SPRITES:
        cur = frame0(os.path.join(CUR, name + ".png"))
        v1, v2 = news[name]
        a = ANALOG[name]
        d = frame0(os.path.join(REF_D, a + ".png"))
        e = frame0(os.path.join(REF_E, a + ".png"))
        cells = [("attuale", cur), ("v1 moderata", v1), ("v2 decisa", v2), None,
                 (a + " default", d), (a + " EC", e)]
        rows.append((name, cells))
    colw = [0] * 6
    rowh = []
    for name, cells in rows:
        h = 0
        for i, c in enumerate(cells):
            if c is None:
                colw[i] = max(colw[i], 24)
                continue
            # i riferimenti Spectrum a 48 px si mostrano alla stessa scala visiva di 32
            s = c[1].width * k if c[1].width <= 32 else 32 * k
            colw[i] = max(colw[i], s)
            h = max(h, s)
        rowh.append(h)
    W = sum(colw) + pad * 7
    H = sum(h + lab * 2 + pad for h in rowh) + pad
    sheet = Image.new("RGBA", (W, H), (38, 38, 42, 255))
    dr = ImageDraw.Draw(sheet)
    y = pad
    for (name, cells), h in zip(rows, rowh):
        dr.text((pad, y), name, fill=(230, 230, 230, 255), font=font)
        y += lab
        x = pad
        for i, c in enumerate(cells):
            if c is not None:
                im = c[1]
                big = up(im, k) if im.width <= 32 else im.resize((32 * k, 32 * k), Image.NEAREST)
                slot = Image.new("RGBA", (big.width, big.height), (58, 58, 64, 255))
                sheet.alpha_composite(slot, (x, y + lab))
                sheet.alpha_composite(big, (x, y + lab))
                dr.text((x, y), c[0], fill=(200, 200, 200, 255), font=font)
            x += colw[i] + pad
        y += h + lab + pad
    sheet.save(path)


def hsv(c):
    return colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)


def main():
    news = {}
    for sub in ("v1", "v2"):
        os.makedirs(os.path.join(OUT, sub), exist_ok=True)
    for name, (g1, g2) in SPRITES.items():
        a, b = render(g1), render(g2)
        a.save(os.path.join(OUT, "v1", name + ".png"))
        b.save(os.path.join(OUT, "v2", name + ".png"))
        news[name] = (a, b)
    tavola(os.path.join(OUT, "tavola_pilota.png"), news)

    # ------------------------------------------------------------------ verifica
    ok = True
    cur_cols = set()
    for name in SPRITES:
        f = frame0(os.path.join(CUR, name + ".png"))
        cur_cols |= {p[:3] for p in f.getdata() if p[3]}
    used = {}
    for sub in ("v1", "v2"):
        for name in SPRITES:
            p = os.path.join(OUT, sub, name + ".png")
            im = Image.open(p)
            cur = frame0(os.path.join(CUR, name + ".png"))
            size_ok = im.size == cur.size and im.mode == "RGBA"
            alphas = {px[3] for px in im.getdata()}
            alpha_ok = alphas <= {0, 255}
            same = list(im.getdata()) == list(cur.getdata())
            line = f"{sub}/{name}: {im.size} {im.mode} alfa={sorted(alphas)} diverso_da_attuale={not same}"
            if name.endswith("_oversized"):
                bb, cb = im.getbbox(), cur.getbbox()
                bb_ok = all(abs(u - v) <= 1 for u, v in zip(bb, cb))
                line += f" ingombro={bb} attuale={cb} {'OK' if bb_ok else 'FUORI'}"
                ok &= bb_ok
            print(line)
            ok &= size_ok and alpha_ok and not same
            for px in im.getdata():
                if px[3]:
                    used[px[:3]] = used.get(px[:3], 0) + 1
    gem_cols = {hexc(PAL[k])[:3] for k in "rstnopvwx"}

    def gem_px(im):
        w = im.width
        return {(x, y) for y in range(w) for x in range(w)
                if im.getpixel((x, y))[3] and im.getpixel((x, y))[:3] in gem_cols}

    print("\nPosizione delle gemme (deve coincidere con l'attuale):")
    for name in SPRITES:
        g0 = gem_px(frame0(os.path.join(CUR, name + ".png")))
        for sub in ("v1", "v2"):
            g = gem_px(Image.open(os.path.join(OUT, sub, name + ".png")).convert("RGBA"))
            print(f"  {sub}/{name}: {'uguale' if g == g0 else 'DIVERSA'}")
            ok &= g == g0
    print("\nColori usati (tutti i 10 PNG):")
    inv = {hexc(v)[:3]: k for k, v in PAL.items()}
    for c in sorted(used, key=lambda c: (inv[c].isdigit(), inv[c])):
        h = "#%02x%02x%02x" % c
        if c in cur_cols:
            tag = "attuale"
        else:
            near = min(cur_cols, key=lambda d: sum((u - v) ** 2 for u, v in zip(c, d)))
            hc, hn = hsv(c), hsv(near)
            dh = min(abs(hc[0] - hn[0]), 1 - abs(hc[0] - hn[0])) * 360
            tag = f"AGGIUNTO, vicino a #%02x%02x%02x (scarto di tinta {dh:.0f} gradi)" % near
            if dh > 15:
                ok = False
                tag += " FUORI FAMIGLIA"
        print(f"  {inv[c]} {h} x{used[c]:4d}  {tag}")
    print("\nVERIFICA", "OK" if ok else "FALLITA")


if __name__ == "__main__":
    main()
