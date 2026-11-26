"""T9 Alloy «Prismatic», GIRO 5: M3 «Giorno e notte» LENTA, ciclo di 200 tick (10 s) invece di 100 (5 s).

Stessa estetica di M3 del giro 4 (texture_t9_giro4.py, importato), ma 40 fotogrammi da 5 tick:
  - giorno -> notte -> giorno: k(f) = (1 - cos(2 pi f / 40)) / 2; notte piena al fotogramma 20, f0 = C1 puro;
  - gemme: 5 gemme x 8 fotogrammi (2 s); il passaggio alla successiva negli ultimi 2 fotogrammi (k = 6, 7), cioe'
    lo stesso 1/4 del tempo di prima;
  - spettro: periodo di scorrimento 20 fotogrammi (stessa velocita' reale di oggi: P ogni 100 tick), quindi 2 giri
    completi in 40 fotogrammi: divide 40, nessun salto fra f39 e f0.
  - alpha identico al T8 in tutti i fotogrammi.

NOTA PER L'INSTALLAZIONE: l'armatura animata nella mod usa ARMOR_FRAMES per tier (oggi 20, in SAArmorItem.java e
texture_mix.FRAMES). Se il T9 usera' questa proposta va impostato a 40 per il T9 (ARMOR_FRAME_TICKS resta 5).
Qui il codice della mod NON e' toccato.

Uso:  python3 -I tools/texture_t9_m3.py   # scrive design/texture/proposte/t9-proposte/giro5/
"""
import json
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_mix as M  # noqa: E402
import texture_t9_proposte as P  # noqa: E402
import texture_t9_giro3 as G3  # noqa: E402
import texture_t9_giro4 as G4  # noqa: E402

OUT = os.path.join(P.OUT, "giro5")
FRAMES, FRAMETIME = 40, 15
SPEC_PERIOD = 20          # fotogrammi per un giro dello spettro (divide FRAMES)
GEM_FRAMES, GEM_TRANS = 8, 2
ICONS, TONE, mix, rgb = P.ICONS, G3.TONE, G3.mix, G3.rgb
assert FRAMES % SPEC_PERIOD == 0 and FRAMES == 5 * GEM_FRAMES
assert not G4.C1.get("fuse") and not G4.C2.get("fuse")


def k_of(f):
    return (1 - math.cos(2 * math.pi * f / FRAMES)) / 2


def gem(ch, f, x, y, trans):
    slot, k = divmod(f, GEM_FRAMES)
    cur = G3.GEMS5[slot][1]
    nxt = G3.GEMS5[(slot + 1) % 5][1]
    if k < GEM_FRAMES - GEM_TRANS:
        return cur[ch]
    if trans == "dissolvenza":
        return (nxt if (x + y) % 2 == 0 else cur)[ch]
    return cur["t"] if ch == "q" else G3.FLASH[ch]


def prism_m3(ch, s, f, armor):
    k = k_of(f)
    t = TONE.index(ch)
    P_, Wb = G4.M3P[armor]
    wb = P_ * (1 - k) + Wb * k
    d = (s - P_ * f / SPEC_PERIOD) % P_
    if d < wb:
        h = int(d / wb * 6) % 6
        return mix(G3.HUES_PASTEL[h][t], G3.HUES_VIVID[h][min(t + 1, 5)], k)
    hd = int(d / P_ * 6) % 6
    sm = G3.SMOKE[min(t + 1, 5)] if (d < wb + 2 or d > P_ - 2) else G3.SMOKE[t]
    return mix(G3.HUES_PASTEL[hd][t], sm, k)


def color_m3(ch, x, y, f, s, armor):
    k = k_of(f)
    if ch in "qrst":
        return gem(ch, f, x, y, "dissolvenza" if k < 0.5 else "lampo")
    if ch in TONE:
        return prism_m3(ch, s, f, armor)
    return mix(G3.color(G4.C1, ch, x, y, f, s, armor), G3.color(G4.C2, ch, x, y, f, s, armor), k)


def render(grid, f, sfun, armor=False):
    h, w = len(grid), len(grid[0])
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = color_m3(ch, x, y, f, sfun(x, y), armor) + (255,)
    return im


def sil(im):
    return im.getchannel("A").tobytes()


def diff(a, b):
    return sum(abs(p - q) for p, q in zip(a.tobytes(), b.tobytes()))


def stable_gems(getter, gem_s):
    """Ogni gemma i occupa i fotogrammi 8i..8i+7: i primi 6 mostrano il suo colore, gli ultimi 2 sono il passaggio."""
    cnt = {}
    for f in range(FRAMES):
        slot, k = divmod(f, GEM_FRAMES)
        c = getter(f)
        if k < GEM_FRAMES - GEM_TRANS:
            assert c == gem_s[slot], (f, c, gem_s[slot])
        cnt[gem_s[slot]] = cnt.get(gem_s[slot], 0) + 1
    return cnt


def build():
    d = os.path.join(OUT, "M3-lenta")
    os.makedirs(d, exist_ok=True)
    G3.MCMETA  # frametime 15
    allf, report = {}, []
    gem_s = [gm["s"] for _, gm in G3.GEMS5]
    for n in ICONS:
        g = G4.CG[n]
        dx, dy = G3.DIR[n]
        frames = [render(g, f, lambda x, y: dx * x + dy * y) for f in range(FRAMES)]
        strip = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
        for i, fr in enumerate(frames):
            strip.paste(fr, (0, 16 * i))
        frames[0].save(os.path.join(d, f"t9_{n}.png"))
        strip.save(os.path.join(d, f"t9_{n}_anim.png"))
        with open(os.path.join(d, f"t9_{n}_anim.png.mcmeta"), "w") as fh:
            fh.write(G3.MCMETA)
        G3.gif(frames, os.path.join(d, f"t9_{n}.gif"), 8, G3.SLOT, ms=FRAMETIME * 50)
        st = Image.open(os.path.join(d, f"t9_{n}_anim.png")).convert("RGBA")
        assert st.size == (16, 16 * FRAMES)
        assert st.crop((0, 0, 16, 16)).tobytes() == Image.open(os.path.join(d, f"t9_{n}.png")).convert("RGBA").tobytes()
        t8 = Image.open(P.T8DIR + f"/set/t8_{n}.png").convert("RGBA")
        assert {sil(fr) for fr in frames} == {sil(t8)}, (n, "alpha diverso dal T8")
        gp = G3.gem_pixels(g)
        cnt = stable_gems(lambda f: frames[f].getpixel(gp[0])[:3], gem_s)
        jump = diff(frames[FRAMES - 1], frames[0])
        adj = max(diff(frames[i], frames[i + 1]) for i in range(FRAMES - 1))
        assert jump <= adj, (n, jump, adj)
        report.append((n, jump, adj, cnt))
        allf[n] = frames
    ag = G3.armor_culmine()
    t8a = Image.open(os.path.join(P.T8DIR, "armatura", "t8_armor.png")).convert("RGBA")
    adx, ady = G3.ARMOR_DIR
    pos = {}

    def sa(x, y):
        if (x, y) not in pos:
            wx, wy = G3.armor_pos(x, y)
            pos[(x, y)] = adx * wx + ady * wy
        return pos[(x, y)]
    af = [render(ag, f, sa, armor=True) for f in range(FRAMES)]
    for i, fr in enumerate(af):
        fr.save(os.path.join(d, f"t9_armor_{i}.png"))
        assert sil(fr) == sil(t8a), "alpha armatura diversa dal T8"
    af[0].save(os.path.join(d, "t9_armor.png"))
    gp = G3.gem_pixels(ag)
    cnt = stable_gems(lambda f: af[f].getpixel(gp[0])[:3], gem_s)
    jump, adj = diff(af[-1], af[0]), max(diff(af[i], af[i + 1]) for i in range(FRAMES - 1))
    assert jump <= adj, ("armatura", jump, adj)
    report.append(("armor", jump, adj, cnt))
    G3.man_gif(af, os.path.join(d, "manichino.gif"))
    return d, allf, af, report


def main():
    os.makedirs(OUT, exist_ok=True)
    d, allf, af, report = build()
    mx = [m for m in G4.MIXES if m["cod"] == "M3"][0]
    rel = os.path.relpath(d, H.ROOT)
    meta = [{
        "numero": "M3", "nome": "Giorno e notte (lenta)",
        "frase": "Come M3 del giro 4 ma l'oggetto vive il suo giorno in 10 secondi: 40 fotogrammi da 5 tick, aurora al "
                 "fotogramma 0, eclisse al 20, ritorno al giorno. Ogni gemma resta 2 secondi, lo spettro scorre alla "
                 "velocita' di prima (2 giri nel ciclo) e si stringe in un fascio di notte.",
        "nota": "40 fotogrammi, ciclo 10 s",
        "famiglia": "culmine (mix C1 + C2)", "divisione": "tempo", "spettro": mx["spettro"],
        "cambio_gemma": mx["cambio_gemma"], "palette": G4.palette(mx),
        "png": {n: f"{rel}/t9_{n}.png" for n in ICONS} | {"armatura": f"{rel}/t9_armor.png"},
        "anim": {n: f"{rel}/t9_{n}_anim.png" for n in ICONS},
        "gif": {n: f"{rel}/t9_{n}.gif" for n in ICONS} | {"manichino": f"{rel}/manichino.gif"},
        "verifiche": ["40 fotogrammi, f0 = icona, alpha = T8, ogni gemma 8 fotogrammi, ciclo f39 -> f0 senza salti"],
    }]
    with open(os.path.join(OUT, "proposte.json"), "w") as fh:
        json.dump(meta, fh, ensure_ascii=False, indent=2)
    for n, j, a, c in report:
        print(n, "diff f39-f0:", j, "max fra vicini:", a, "fotogrammi per gemma:", sorted(c.values()))
    print("scritto", OUT)


if __name__ == "__main__":
    main()
