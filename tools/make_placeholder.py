"""Genera una texture PROVVISORIA ricolorando una texture di Spectrum con la palette ibrida del tier.

Serve solo finché non disegni la texture vera: stessa regola della pagina "Spectral Ascension Armory".
Corpo (grigi + tonalità del materiale di partenza) → colore del metallo ATM; dettagli → tema Spectrum del tier.

Uso:
  python3 -I tools/make_placeholder.py <jar di spectrum> <nome texture spectrum> <t6|t7|t8|alloy> <png di uscita>
  <nome texture spectrum> è un nome in textures/item/ (es. bedrock_pickaxe) oppure, se contiene "/", il percorso
  completo nel jar (es. assets/spectrum/textures/armor/bedrock_armor.png, per il modello 3D dell'armatura).
  es. python3 -I tools/make_placeholder.py "<spectrum-1.12.9-1.21.1-neo.jar>" bedrock_pickaxe t6 \
        src/main/resources/assets/spectral_ascension/textures/item/trichromatic_pickaxe.png
"""
import colorsys
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pngio import read_png, write_png  # noqa: E402

TIERS = {
    "t6": {"body": ["#2a1504", "#6e3a08", "#b8700f", "#e89725", "#ffcf73", "#fff1d2"],
           "detail": ["#14203a", "#1f8fb0", "#29d3e6", "#e04ad6", "#ffe75a", "#fffbe0"]},
    "t7": {"body": ["#03201d", "#0b4a44", "#14716a", "#209187", "#5fd6c6", "#d8fff8"],
           "detail": ["#050409", "#17131f", "#2c2539", "#4d4360", "#8b7ca3", "#d6cbe6"]},
    "t8": {"body": ["#160630", "#3a1668", "#5d27a0", "#7e35c3", "#b98cf0", "#f1e6ff"],
           "detail": ["#3c4258", "#6f7893", "#a9b3cc", "#d3dbec", "#eef2fa", "#ffffff"]},
    "alloy": {"prism": True, "alloy_hues": [87, 339, 217]},
}


def rgb(hexstr):
    n = int(hexstr[1:], 16)
    return (n >> 16) & 255, (n >> 8) & 255, n & 255


def gradient(stops, t):
    c = [rgb(s) for s in stops]
    seg = (len(c) - 1) * min(1.0, max(0.0, t))
    i = min(len(c) - 2, int(seg))
    f = seg - i
    return tuple(round(c[i][k] + (c[i + 1][k] - c[i][k]) * f) for k in range(3))


def hsl(h, s, l):
    r, g, b = colorsys.hls_to_rgb(h % 1.0, l, s)
    return round(r * 255), round(g * 255), round(b * 255)


def main(jar, name, tier_id, out):
    body_hue = (90, 180) if "glass_crest" in name or "malachite" in name else (330, 30)
    tier = TIERS[tier_id]
    with zipfile.ZipFile(jar) as z:
        entry = name if "/" in name else f"assets/spectrum/textures/item/{name}.png"
        w, h, px = read_png(z.read(entry))
    lums = [(0.299 * r + 0.587 * g + 0.114 * b) / 255 for r, g, b, a in px if a]
    lo, hi = min(lums), max(lums)
    span = max(0.05, hi - lo)
    res = []
    for idx, (r, g, b, a) in enumerate(px):
        if a == 0:
            res.append((0, 0, 0, 0))
            continue
        x, y = idx % w, idx // w
        t = ((0.299 * r + 0.587 * g + 0.114 * b) / 255 - lo) / span
        mx, mn = max(r, g, b), min(r, g, b)
        hue = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)[0] * 360
        in_body_hue = body_hue[0] <= hue < body_hue[1] if body_hue[0] <= body_hue[1] else (hue >= body_hue[0] or hue < body_hue[1])
        detail = mx > 0 and (mx - mn) / mx > 0.28 and not in_body_hue
        if tier.get("prism"):
            if detail:
                c = hsl((x + y) / (w + h) * 1.5 + 0.55, 0.85, 0.22 + 0.62 * t)
            else:
                c = hsl(tier["alloy_hues"][int((x + y) / (w + h) * 3) % 3] / 360, 0.38, 0.12 + 0.7 * t)
        else:
            c = gradient(tier["detail" if detail else "body"], t)
        res.append(c + (a,))
    write_png(out, w, h, res)
    print(f"scritto {out} ({w}x{h}) da spectrum:{name}, tier {tier_id}")


if __name__ == "__main__":
    if len(sys.argv) != 5:
        sys.exit(__doc__)
    main(*sys.argv[1:])
