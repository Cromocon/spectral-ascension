"""T6 «Castone»: tavola di analisi prima/dopo dell'ultimo giro di correzioni, per punto.

Ogni pannello è ingrandito con la griglia numerata (colonne da 1 a SINISTRA, righe da 1 dal BASSO), come le coordinate
dell'utente [colonna, riga]. «Prima» = design/texture/proposte/t6-castone/prima_giro_analisi/ (copia dello stato di partenza),
«dopo» = i file attuali di t6-castone/. Scrive solo t6-castone/analisi_prima_dopo.png.

Uso:  python3 tools/texture_t6castone_analisi.py   (dopo texture_t6castone.py e texture_t6castone_speciali.py)
"""
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_mix as M  # noqa: E402
import texture_t6castone as T  # noqa: E402
import texture_t6castone_speciali as S  # noqa: E402

OUT = T.OUT
PRE = os.path.join(OUT, "prima_giro_analisi")
BG = (24, 22, 30, 255)
F9, F12, F14 = S.font(9), S.font(12), S.font(14, True)


def load(*parts):
    return Image.open(os.path.join(*parts)).convert("RGBA")


def face(tex, cube, name):
    u, v, w, h, d = M.CUBES[cube]
    fx, fy, fw, fh = M.cube_faces(u, v, w, h, d)[name]
    return tex.crop((fx, fy, fx + fw, fy + fh))


def panel(im, k, label):
    p = S.numerata(im, k, F9)
    cell = Image.new("RGBA", (p.width + 8, p.height + 20), BG)
    cell.alpha_composite(p, (0, 0))
    ImageDraw.Draw(cell).text((26, p.height), label, fill=(236, 231, 241), font=F12)
    return cell


def strip(panels, gap=14):
    w = sum(p.width for p in panels) + gap * (len(panels) - 1)
    h = max(p.height for p in panels)
    s = Image.new("RGBA", (w, h), BG)
    x = 0
    for p in panels:
        s.alpha_composite(p, (x, 0))
        x += p.width + gap
    return s


def grid_rows(panels, width):
    """Dispone i pannelli in righe che non superano la larghezza data."""
    rows, cur, w = [], [], 0
    for p in panels:
        if cur and w + p.width + 14 > width:
            rows.append(strip(cur))
            cur, w = [], 0
        cur.append(p)
        w += p.width + 14
    if cur:
        rows.append(strip(cur))
    return rows


def titled(title, note, body_rows, width):
    head = Image.new("RGBA", (width, 54), BG)
    d = ImageDraw.Draw(head)
    d.text((0, 4), title, fill=(234, 154, 39), font=F14)
    d.text((0, 26), note, fill=(200, 192, 212), font=F12)
    h = head.height + sum(r.height + 12 for r in body_rows) + 20
    sec = Image.new("RGBA", (width, h), BG)
    sec.alpha_composite(head, (0, 0))
    y = head.height
    for r in body_rows:
        sec.alpha_composite(r, (0, y))
        y += r.height + 12
    return sec


def main():
    W = 1900
    old_a, new_a = load(PRE, "armatura", "t6_armor.png"), load(OUT, "armatura", "t6_armor.png")
    secs = []

    # 1. apertura dell'elmo
    pn = []
    for lab, tex in (("prima", old_a), ("dopo", new_a)):
        pn.append(panel(face(tex, "head_out", "north"), 24, f"placca esterna davanti, {lab} (10x10)"))
    for lab, tex in (("prima", old_a), ("dopo", new_a)):
        pn.append(panel(face(tex, "head_in", "north"), 24, f"piastra interna davanti, {lab} (9x9)"))
    secs.append(titled("1. Apertura dell'elmo (davanti)",
                       "Apertura trasparente (volto): da riga 6 a riga 5 in alto, 6 colonne nel mezzo (prima 4), 8 sul mento (prima 6): circa 1 px in più per lato, stessa forma.",
                       grid_rows(pn, W), W))

    # 2. facce interne = esterne
    faces = ["north", "south", "west", "east", "top", "bottom"]
    pn = []
    for f in faces:
        pn.append(panel(face(old_a, "head_out", f), 16, f"{f}: esterna prima"))
        pn.append(panel(face(new_a, "head_out", f), 16, f"{f}: esterna dopo"))
        pn.append(panel(face(old_a, "head_in", f), 16, f"{f}: interna prima"))
        pn.append(panel(face(new_a, "head_in", f), 16, f"{f}: interna dopo"))
    secs.append(titled("2. Facce interne uguali alle esterne (tutte e 6 le facce dell'elmo)",
                       "Ogni piastra interna 9x9 ripete il disegno della placca esterna 10x10 (apertura, castone, fili). La faccia di sotto è un telaio come l'esterna (bordo bedrock, interno trasparente).",
                       grid_rows(pn, W), W))

    # 3. retro
    pn = [panel(face(old_a, "head_out", "south"), 24, "retro esterno, prima"), panel(face(new_a, "head_out", "south"), 24, "retro esterno, dopo"),
          panel(face(old_a, "head_in", "south"), 24, "retro interno, prima"), panel(face(new_a, "head_in", "south"), 24, "retro interno, dopo")]
    secs.append(titled("3. Retro dell'elmo", "Solo grappoli di bedrock più chiari e più scuri; il filo arancio in alto a destra è quello di prima; nessuna gemma nuova.",
                       grid_rows(pn, W), W))

    # 4. workstaff e bident oversized
    pn = []
    for n, ref in (("workstaff", "glass_crest_workstaff_oversized.png"), ("bident", "fractal_glass_crest_bident_oversized.png")):
        pn.append(panel(load(PRE, "speciali", f"t6_{n}_oversized.png"), 12, f"{n} 32x32, prima"))
        pn.append(panel(S.ref(ref), 12, f"{n}: glass crest 32x32"))
        pn.append(panel(load(OUT, "speciali", f"t6_{n}_oversized.png"), 12, f"{n} 32x32, dopo"))
        pn.append(panel(load(OUT, "speciali", f"t6_{n}.png"), 24, f"{n} 16x16 approvato (invariato)"))
    secs.append(titled("4. Workstaff e bident oversized (32x32)",
                       "Rifatti con il metodo del glass crest: stessi pixel occupati (sagome uguali al 100%) e stesse divisioni, materiali nostri; accanto, il 16x16 approvato.",
                       grid_rows(pn, W), W))

    # 5. balestra, 12 stati
    pn16, pn32 = [], []
    for st in S.STATES:
        pn16.append(panel(load(PRE, "speciali", f"t6_crossbow_{st}.png"), 12, f"{st}: prima"))
        pn16.append(panel(load(OUT, "speciali", f"t6_crossbow_{st}.png"), 12, f"{st}: dopo"))
        pn32.append(panel(load(PRE, "speciali", f"t6_crossbow_{st}_oversized.png"), 8, f"{st}: prima"))
        pn32.append(panel(load(OUT, "speciali", f"t6_crossbow_{st}_oversized.png"), 8, f"{st}: dopo"))
    secs.append(titled("5a. Balestra 16x16, 12 stati", "Filo arancio solo sul bordo alto (1 px); flettente basso di bedrock; razzo con il cono attaccato al corpo, sotto il castone.",
                       grid_rows(pn16, W), W))
    secs.append(titled("5b. Balestra oversized 32x32, 12 stati", "Stesse correzioni sul 32x32 (balestra nella zona 12-31).", grid_rows(pn32, W), W))

    # 6. freccia dell'arco
    pn = []
    for n, lab in (("bow_pulling_0", "tensione 0 (9 px)"), ("bow_pulling_1", "tensione 1 (10 px)"), ("bow_pulling_2", "tensione 2 (11 px)")):
        pn.append(panel(load(PRE, "set", f"t6_{n}.png"), 16, f"{lab}: prima"))
        pn.append(panel(load(OUT, "set", f"t6_{n}.png"), 16, f"{lab}: dopo"))
    secs.append(titled("6. Freccia dell'arco (stati di tensione)", "Asta in bedrock chiaro (#8a83ad) al posto del bianco, alette arancio sugli ultimi 2 pixel, punta a prisma con le tre gemme invariata.",
                       grid_rows(pn, W), W))

    H_ = sum(s.height for s in secs) + 60
    sheet = Image.new("RGBA", (W + 40, H_), BG)
    ImageDraw.Draw(sheet).text((20, 14), "T6 Castone: analisi prima/dopo (colonne da sinistra, righe dal basso)", fill=(234, 154, 39), font=S.font(20, True))
    y = 56
    for s in secs:
        sheet.alpha_composite(s, (20, y))
        y += s.height
    sheet.convert("RGB").save(os.path.join(OUT, "analisi_prima_dopo.png"))
    print(sheet.size)


if __name__ == "__main__":
    main()
