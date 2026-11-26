"""T9 Alloy «Prismatic», GIRO 4: 3 MIX tra C1 Aurora (chiara) e C2 Eclisse (scura) del giro 3.

Richiesta: «un mix tra C1 e C2: voglio rappresentare sia il bianco che il nero». Tutti e tre sono «culmine» (alpha
identico al T8 in tutti i fotogrammi), 20 fotogrammi da 5 tick, un castone con le 5 gemme di Spectrum (4 fotogrammi
ciascuna) come al giro 3. Si riusano griglie, palette, spettro e uscite del giro 3 (texture_t9_giro3.py).

  M1 CREPUSCOLO   divisione per MATERIALE: corpo e manico in ossidiana nera, lama di cristallo chiaro con l'arcobaleno
                  pastello di C1 che sale, montatura bianca. Armatura: base nera, placche (toni alti 5-6 del T8) in
                  perla, orli di cristallo pastello. (Provata anche l'inversa, corpo perla e lama fume' col fascio di
                  C2: VARIANTE_M1 = "inversa" la rigenera.)
  M2 EQUINOZIO    divisione nello SPAZIO: maschere a mano (l = lato del giorno come C1, n = lato della notte come C2,
                  * = confine). Il confine e' una linea di spettro vivo che scorre; il castone sta sul confine.
                  Armatura: elmo e petto tagliati a meta' (yin-yang: elmo chiaro a sinistra, petto chiaro a destra,
                  mantello come l'elmo), braccia, gambe e stivali alternati, con la linea di spettro sul taglio.
  M3 GIORNO E NOTTE  divisione nel TEMPO: k(f) = (1 - cos(2 pi f / 20)) / 2 va da 0 (f0, giorno = C1) a 1 (f10, notte
                  = C2) e torna. Il corpo sfuma perla -> notte; l'arcobaleno pastello si STRINGE in un fascio vivo
                  (larghezza P*(1-k) + Wb*k) mentre fuori dal fascio il cristallo si scurisce in fume'. Lo spettro c'e'
                  sempre; il fotogramma 0 e' C1 puro. La gemma cambia in dissolvenza di giorno, con il lampo di notte.

Castone dell'ascia: al giro 3 era una perlina obliqua sul manico; qui e' un castone 4x4 nella testa (dove c'era il
dorso), con montatura agli angoli; il manico torna metallo. L'alpha non cambia.

Uso:  python3 -I tools/texture_t9_giro4.py   # scrive design/texture/proposte/t9-proposte/giro4/
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_mix as M  # noqa: E402
import texture_t9_proposte as P  # noqa: E402
import texture_t9_giro3 as G3  # noqa: E402

OUT = os.path.join(P.OUT, "giro4")
G3DIR = os.path.join(P.OUT, "giro3")
ICONS = P.ICONS
FRAMES, FRAMETIME = G3.FRAMES, G3.FRAMETIME
SLOT, DARK = G3.SLOT, (24, 22, 30, 255)
rgb, mix, G16 = G3.rgb, G3.mix, G3.G16
TONE = G3.TONE
VARIANTE_M1 = os.environ.get("VARIANTE_M1", "notte")      # "notte" (scelta) o "inversa"

# ======================================================================================================================
# GRIGLIE: quelle del culmine del giro 3, con l'ascia ridisegnata (castone 4x4 nella testa)
# ======================================================================================================================
CG = dict(G3.CG)
CG["axe"] = G16("""
    ................
    .........aa.....
    ........aeWa....
    .......aceGqqG..
    ......abdeqtsq..
    ......abcdqsrq..
    ......abcbHqqH..
    ........043110..
    .......040.100..
    ......040..00...
    .....040........
    ....040.........
    ...040..........
    ..040...........
    ..adc...........
    ..ab............
""")

# ======================================================================================================================
# M2: maschere a mano  l = giorno (C1)   n = notte (C2)   * = confine di spettro   . = vuoto
#   spada: taglio lungo l'asse della lama (sopra-sinistra giorno, sotto-destra notte), il confine e' l'anima della lama
#   ascia: taglio lungo l'asse del manico che attraversa la testa in diagonale
#   elmo e corazza: taglio verticale a S (yin-yang), giorno a sinistra
# ======================================================================================================================
MASK = {
    "sword": G16("""
        .............ll*
        ............ll*n
        ...........ll*nn
        ..........ll*nnn
        .........ll*nnn.
        ....ll..ll*nnn..
        ...llllll*nnn...
        ..llllll*nnn....
        ..lllll*nnn.....
        .lllll*nnn......
        ..lll*nnnnn.....
        ...l*nnnnn......
        ..l*n.nnn.......
        .l*n...n........
        l*n.............
        *n..............
    """),
    "axe": G16("""
        ................
        .........ll.....
        ........llll....
        .......llll*nn..
        ......llll*nnn..
        ......lll*nnnn..
        ......ll*nnnnn..
        ........*nnnnn..
        .......l*n.nnn..
        ......l*n..nn...
        .....l*n........
        ....l*n.........
        ...l*n..........
        ..l*n...........
        ..l*n...........
        ..l*............
    """),
    "helmet": G16("""
        ................
        ................
        .....lll*nn.....
        ...llllll*nnn...
        ..lllllll*nnnn..
        ..llllll*nnnnn..
        ..llllll*nnnnn..
        ..lll......nnn..
        ..lll......nnn..
        ..llll....nnnn..
        ..llll....nnnn..
        ...lll....nnn...
        ................
        ................
        ................
        ................
    """),
    "chestplate": G16("""
        ................
        ................
        lllll.l*nn.nnnnn
        lllllll*nnnnnnnn
        lllllll*nnnnnnnn
        lllllll*nnnnnnn.
        .lllllll*nnnnnn.
        .lllllll*nnnnnn.
        ..lllll*nnnnnn..
        ..lllll*nnnnnn..
        ..llllll*nnnnn..
        ..llllll*nnnnn..
        ..lllll*nnnnnn..
        ..lllll*nnnnnn..
        ..llllll*nnnnn..
        ..llllll*nnnnn..
    """),
}

# armatura M2: pezzi tagliati a meta' (sinistra dello schermo davanti = 'l' se FLIP 0) o interi
SPLIT = {"head_out": 0, "head_in": 0, "body_out": 1, "body_in": 1, "cape": 0, "front_cloth": 1}
WHOLE = {"right_arm": "l", "right_arm_extra": "l", "left_arm": "n", "left_arm_extra1": "n", "left_arm_extra2": "n",
         "right_leg": "l", "left_leg": "n", "right_boot": "n", "left_boot": "l"}


def armor_side(x, y):
    w = P.WHERE.get((x, y))
    if not w:
        return "l"
    cube, face, fx, fy = w
    if cube in WHOLE:
        return WHOLE[cube]
    cw = M.CUBES[cube][2]
    flip = SPLIT[cube]
    if face in ("west", "east"):
        left = face == "east"      # faccia laterale: tutta del lato vicino
    else:
        u = cw - 1 - fx if face == "south" else fx          # il retro e' visto specchiato: coerente in 3D
        if u == cw // 2 and cube in ("head_out", "body_out", "cape"):
            return "*"
        left = u < cw // 2
    return "l" if left != bool(flip) else "n"


# ======================================================================================================================
# LOOK: due «aspetti» del giro 3 e i loro pezzi
# ======================================================================================================================
C1 = dict(G3.PROPOSTE[0])
C2 = dict(G3.PROPOSTE[1])
SEAM = dict(C1, hues=G3.HUES_VIVID, mode="flusso")       # linea di confine: spettro vivo che scorre

M1_NOTTE = dict(C1, body="ossidiana", cloth="notte", mount="bianco", trans="lampo")
M1_INVERSA = dict(C2, body="perla", cloth="lilla", mount="oro_scuro", trans="lampo")
PEARL_PLATE = {"5": 3, "6": 5}          # armatura M1: i toni alti del T8 (placche in rilievo) diventano perla
PEARL_CUBES = {"right_arm_extra", "left_arm_extra1", "left_arm_extra2", "right_boot", "left_boot"}  # spallacci, stivali


def k_of(f):
    return (1 - math.cos(2 * math.pi * f / FRAMES)) / 2


def gem(ch, f, x, y, trans):
    return G3.gem_color(ch, f, x, y, trans)


def color_m1(ch, x, y, f, s, armor):
    look = M1_NOTTE if VARIANTE_M1 == "notte" else M1_INVERSA
    if armor and VARIANTE_M1 == "notte" and ch in "0123456":
        if ch in PEARL_PLATE:
            return rgb(G3.BODY["perla"][PEARL_PLATE[ch]])
        w = P.WHERE.get((x, y))
        if w and (w[0] in PEARL_CUBES or (w[0] == "head_out" and (w[1] == "top" or w[3] < 2))):
            return rgb(G3.BODY["perla"][max(int(ch), 1)])
    return G3.color(look, ch, x, y, f, s, armor)


def color_m2(ch, x, y, f, s, armor, side):
    if ch in "qrstW":
        return G3.color(C1, ch, x, y, f, s, armor) if ch == "W" else gem(ch, f, x, y, "dissolvenza")
    if side == "*":
        if ch in "0123456":
            t = min(TONE.index(G3.BODY_TONE[int(ch)]) + 1, 5)
        elif ch in TONE:
            t = min(TONE.index(ch) + 1, 5)
        else:
            return G3.color(C1, ch, x, y, f, s, armor)
        return G3.prism(SEAM, TONE[t], s, f, armor)
    return G3.color(C1 if side == "l" else C2, ch, x, y, f, s, armor)


# M3: un solo periodo per giorno e notte, cosi' l'arcobaleno si stringe nel fascio senza salti
M3P = {False: (30, 12), True: (48, 20)}     # (P, Wb) icone / armatura


def prism_m3(ch, s, f, armor):
    k = k_of(f)
    t = TONE.index(ch)
    P_, Wb = M3P[armor]
    wb = P_ * (1 - k) + Wb * k
    d = (s - P_ * f / FRAMES) % P_
    if d < wb:
        h = int(d / wb * 6) % 6
        return mix(G3.HUES_PASTEL[h][t], G3.HUES_VIVID[h][min(t + 1, 5)], k)
    # fuori dal fascio: il colore che il giorno avrebbe qui, sfumato nel fume'
    hd = int(d / P_ * 6) % 6
    sm = G3.SMOKE[min(t + 1, 5)] if (d < wb + 2 or d > P_ - 2) else G3.SMOKE[t]
    return mix(G3.HUES_PASTEL[hd][t], sm, k)


def color_m3(ch, x, y, f, s, armor):
    k = k_of(f)
    if ch in "qrst":
        return gem(ch, f, x, y, "dissolvenza" if k < 0.5 else "lampo")
    if ch in TONE:
        return prism_m3(ch, s, f, armor)
    return mix(G3.color(C1, ch, x, y, f, s, armor), G3.color(C2, ch, x, y, f, s, armor), k)


MIXES = [
    dict(cod="M1", slug="crepuscolo", nome="Crepuscolo", divisione="materiale",
         frase="Il nero e il bianco divisi per materiale: corpo, manico e guardia in ossidiana, lama di cristallo chiaro "
               "con l'arcobaleno pastello di Aurora che sale, montatura e riflessi bianchi. Sull'armatura la base e' "
               "nera, le placche in rilievo sono perla e gli orli cristallo; la gemma cambia con un lampo.",
         spettro="flusso (pastello)", cambio_gemma="lampo"),
    dict(cod="M2", slug="equinozio", nome="Equinozio", divisione="spazio",
         frase="Meta' giorno e meta' notte, alla yin-yang: un lato e' Aurora (perla e cristallo pastello), l'altro e' "
               "Eclisse (nero e vetro fume' col fascio), e sul confine corre una linea di spettro vivo. La spada e' "
               "tagliata lungo la lama, l'ascia lungo il manico, elmo e corazza a S; l'armatura alterna i lati.",
         spettro="flusso (giorno) + fascio (notte) + confine vivo", cambio_gemma="dissolvenza"),
    dict(cod="M3", slug="giorno-e-notte", nome="Giorno e notte", divisione="tempo",
         frase="L'oggetto vive un giorno in 5 secondi: al fotogramma 0 e' Aurora (perla, arcobaleno pastello), poi "
               "imbrunisce fino a Eclisse al fotogramma 10 (corpo nero, l'arcobaleno si stringe in un fascio di luce "
               "dentro il vetro fume') e torna giorno. La gemma cambia in dissolvenza di giorno, col lampo di notte.",
         spettro="flusso che si stringe in fascio", cambio_gemma="dissolvenza di giorno, lampo di notte"),
]


# ======================================================================================================================
# RENDER
# ======================================================================================================================
def render(cod, grid, f, sfun, armor=False, mask=None):
    h, w = len(grid), len(grid[0])
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            s = sfun(x, y)
            if cod == "M1":
                c = color_m1(ch, x, y, f, s, armor)
            elif cod == "M2":
                side = armor_side(x, y) if armor else mask[y][x]
                assert side in "ln*", (x, y, side)
                c = color_m2(ch, x, y, f, s, armor, side)
            else:
                c = color_m3(ch, x, y, f, s, armor)
            px[x, y] = c + (255,)
    return im


def sil(im):
    return im.getchannel("A").tobytes()


def build(mx):
    cod = mx["cod"]
    d = os.path.join(OUT, f"{cod}-{mx['slug']}")
    os.makedirs(d, exist_ok=True)
    res = {"frames": {}}
    for n in ICONS:
        g = CG[n]
        mask = MASK[n] if cod == "M2" else None
        if mask:
            for y in range(16):
                for x in range(16):
                    assert (g[y][x] == ".") == (mask[y][x] == "."), (n, x, y, "maschera fuori sagoma")
        dx, dy = G3.DIR[n]
        frames = [render(cod, g, f, lambda x, y: dx * x + dy * y, mask=mask) for f in range(FRAMES)]
        strip = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
        for i, fr in enumerate(frames):
            strip.paste(fr, (0, 16 * i))
        frames[0].save(os.path.join(d, f"t9_{n}.png"))
        strip.save(os.path.join(d, f"t9_{n}_anim.png"))
        with open(os.path.join(d, f"t9_{n}_anim.png.mcmeta"), "w") as fh:
            fh.write(G3.MCMETA)
        G3.gif(frames, os.path.join(d, f"t9_{n}.gif"), 8, SLOT)
        # verifiche
        st = Image.open(os.path.join(d, f"t9_{n}_anim.png")).convert("RGBA")
        assert st.size == (16, 16 * FRAMES)
        assert st.crop((0, 0, 16, 16)).tobytes() == Image.open(os.path.join(d, f"t9_{n}.png")).convert("RGBA").tobytes()
        t8 = Image.open(P.T8DIR + f"/set/t8_{n}.png").convert("RGBA")
        assert {sil(fr) for fr in frames} == {sil(t8)}, (cod, n, "alpha diverso dal T8")
        gp = G3.gem_pixels(g)
        seen = {st.getpixel((gp[0][0], gp[0][1] + 16 * f))[:3] for f in (0, 4, 8, 12, 16)}
        assert seen == {gm["s"] for _, gm in G3.GEMS5}, (cod, n, "il castone non mostra le 5 gemme")
        res["frames"][n] = frames
    # armatura
    ag = G3.armor_culmine()
    t8a = Image.open(os.path.join(P.T8DIR, "armatura", "t8_armor.png")).convert("RGBA")
    adx, ady = G3.ARMOR_DIR
    pos = {}

    def sa(x, y):
        if (x, y) not in pos:
            wx, wy = G3.armor_pos(x, y)
            pos[(x, y)] = adx * wx + ady * wy
        return pos[(x, y)]
    aframes = [render(cod, ag, f, sa, armor=True) for f in range(FRAMES)]
    for i, fr in enumerate(aframes):
        fr.save(os.path.join(d, f"t9_armor_{i}.png"))
        assert sil(fr) == sil(t8a), (cod, "alpha armatura diversa dal T8")
    aframes[0].save(os.path.join(d, "t9_armor.png"))
    gp = G3.gem_pixels(ag)
    assert {aframes[f].getpixel(gp[0])[:3] for f in (0, 4, 8, 12, 16)} == {gm["s"] for _, gm in G3.GEMS5}
    G3.man_gif(aframes, os.path.join(d, "manichino.gif"))
    res["armor_frames"] = aframes
    res["dir"] = d
    res["checks"] = ["icone: 20 fotogrammi, f0 = icona, alpha = T8 in tutti i fotogrammi, 5 gemme nel castone",
                     "armatura: 20 fotogrammi, f0 = t9_armor.png, alpha = T8, 5 gemme"]
    return res


def load_g3(cod, slug):
    """C1/C2 dal giro 3 (sola lettura) per il confronto in panoramica."""
    d = os.path.join(G3DIR, f"{cod}-{slug}")
    fr = {}
    for n in ICONS:
        st = Image.open(os.path.join(d, f"t9_{n}_anim.png")).convert("RGBA")
        fr[n] = [st.crop((0, 16 * i, 16, 16 * i + 16)) for i in range(FRAMES)]
    af = [Image.open(os.path.join(d, f"t9_armor_{i}.png")).convert("RGBA") for i in range(FRAMES)]
    return {"frames": fr, "armor_frames": af}


def panoramica(rows, path):
    BG, TI = (44, 42, 52), (255, 214, 120)
    k, ks, kt = 6, 3, 2
    cw = 16 * k + 4 + 16 * ks + 8 + 5 * (16 * ks + 2) + 16
    man_w = 42 * 3
    W = 16 + 4 * cw + 3 * (man_w + 6) + 10
    rowh = 16 * k + 16 * kt + 44
    img = Image.new("RGB", (W, 64 + len(rows) * rowh), BG)
    dr = ImageDraw.Draw(img)
    dr.text((16, 12), "T9 Prismatic, giro 4: C1 e C2 (giro 3) per confronto, poi i 3 mix", fill=TI, font=G3.font(20, True))
    dr.text((16, 38), "per oggetto: f0 su slot | f0 su fondo scuro | le 5 gemme (f0 f4 f8 f12 f16); sotto: f0 f2 f4 ... f18. "
                      "Manichino: f0, f5, f10", fill=(200, 200, 210), font=G3.font(13))
    y = 64
    for title, r in rows:
        dr.text((16, y), title, fill=TI, font=G3.font(15, True))
        yy = y + 22
        for i, n in enumerate(ICONS):
            x = 16 + i * cw
            fr = r["frames"][n]
            img.paste(H.on_bg(H.up(fr[0], k), SLOT).convert("RGB"), (x, yy))
            x2 = x + 16 * k + 4
            img.paste(H.on_bg(H.up(fr[0], ks), DARK).convert("RGB"), (x2, yy))
            x3 = x2 + 16 * ks + 8
            for j, f in enumerate((0, 4, 8, 12, 16)):
                img.paste(H.on_bg(H.up(fr[f], ks), SLOT).convert("RGB"), (x3 + j * (16 * ks + 2), yy))
            for j, f in enumerate(range(0, FRAMES, 2)):
                img.paste(H.on_bg(H.up(fr[f], kt), SLOT).convert("RGB"), (x + j * (16 * kt + 2), yy + 16 * k + 6))
        x = 16 + 4 * cw
        for j, f in enumerate((0, 5, 10)):
            man = H.on_bg(M.mannequin(r["armor_frames"][f], 3), G3.MAN_BG).convert("RGB")
            img.paste(man, (x + j * (man_w + 6), yy - 4))
        y += rowh
    img.save(path)


def hexs(c):
    return "#%02x%02x%02x" % tuple(c)


def palette(mx):
    gems = {nm: hexs(gm["s"]) for nm, gm in G3.GEMS5}
    spettro_p = [hexs(h[3]) for h in G3.HUES_PASTEL]
    spettro_v = [hexs(h[3]) for h in G3.HUES_VIVID]
    fume = [hexs(c) for c in G3.SMOKE]
    if mx["cod"] == "M1":
        lk = M1_NOTTE if VARIANTE_M1 == "notte" else M1_INVERSA
        p = {"corpo": list(G3.BODY[lk["body"]]), "montatura": list(G3.MOUNT[lk["mount"]]),
             "stoffa": list(G3.CLOTH[lk["cloth"]]),
             "spettro": spettro_p if lk["hues"] is G3.HUES_PASTEL else spettro_v}
        if VARIANTE_M1 == "notte":
            p["placche_armatura_perla"] = [G3.BODY["perla"][v] for v in PEARL_PLATE.values()]
        else:
            p["fume"] = fume
    elif mx["cod"] == "M2":
        p = {"giorno": {"corpo": list(G3.BODY["perla"]), "montatura": list(G3.MOUNT["oro_chiaro"]),
                        "stoffa": list(G3.CLOTH["lilla"]), "spettro": spettro_p},
             "notte": {"corpo": list(G3.BODY["notte"]), "montatura": list(G3.MOUNT["oro_scuro"]),
                       "stoffa": list(G3.CLOTH["notte"]), "spettro": spettro_v, "fume": fume},
             "confine": spettro_v}
    else:
        p = {"giorno (f0)": {"corpo": list(G3.BODY["perla"]), "montatura": list(G3.MOUNT["oro_chiaro"]),
                             "stoffa": list(G3.CLOTH["lilla"]), "spettro": spettro_p},
             "notte (f10)": {"corpo": list(G3.BODY["notte"]), "montatura": list(G3.MOUNT["oro_scuro"]),
                             "stoffa": list(G3.CLOTH["notte"]), "spettro": spettro_v, "fume": fume}}
    p["gemme"] = gems
    return p


def main():
    os.makedirs(OUT, exist_ok=True)
    rows = [("C1 Aurora (giro 3, riferimento)", load_g3("C1", "aurora")),
            ("C2 Eclisse (giro 3, riferimento)", load_g3("C2", "eclisse"))]
    meta = []
    for mx in MIXES:
        r = build(mx)
        rows.append((f"{mx['cod']} {mx['nome']}  (divisione per {mx['divisione']}, spettro: {mx['spettro']}, "
                     f"cambio gemma: {mx['cambio_gemma']})", r))
        rel = os.path.relpath(r["dir"], H.ROOT)
        meta.append({
            "numero": mx["cod"], "nome": mx["nome"], "frase": mx["frase"], "famiglia": "culmine (mix C1 + C2)",
            "divisione": mx["divisione"], "spettro": mx["spettro"], "cambio_gemma": mx["cambio_gemma"],
            "palette": palette(mx),
            "png": {n: f"{rel}/t9_{n}.png" for n in ICONS} | {"armatura": f"{rel}/t9_armor.png"},
            "anim": {n: f"{rel}/t9_{n}_anim.png" for n in ICONS},
            "gif": {n: f"{rel}/t9_{n}.gif" for n in ICONS} | {"manichino": f"{rel}/manichino.gif"},
            "verifiche": r["checks"],
        })
        print("ok", mx["cod"], mx["nome"])
    with open(os.path.join(OUT, "proposte.json"), "w") as fh:
        json.dump(meta, fh, ensure_ascii=False, indent=2)
    panoramica(rows, os.path.join(OUT, "panoramica.png"))
    print("scritto", OUT)


if __name__ == "__main__":
    main()
