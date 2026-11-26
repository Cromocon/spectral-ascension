"""Prepara il test alla cieca delle icone: immagini anonime mescolate + chiave separata.

Uso: python3 -I tools/test_cieco/prepara_test.py <cartella_uscita> [--con-t7]
Di default il T7 è escluso (l'utente non ci ha ancora lavorato); --con-t7 lo include.
Crea <uscita>/immagini/lotto_N (16x16, 3 lotti) e lotto_oversized_N (32x32, 2 lotti),
e <uscita>/chiave/chiave.json (da NON dare all'agente del test).
"""
import glob, os, random, json, re, shutil, sys
from collections import Counter
from PIL import Image

PROJ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ROOT = os.path.join(PROJ, "design")
OUT = os.path.abspath(sys.argv[1])
P = ROOT + "/texture/proposte"
R = ROOT + "/riferimenti/classica"


def label(name):
    n = re.sub(r"_oversized$", "", name)
    n = re.sub(r"^(t6|t7|bedrock|glass_crest|ferocious_glass_crest|fractal_glass_crest)_", "", n)
    if n.startswith("crossbow_"):
        s = n[len("crossbow_"):]
        if s == "standby": return "balestra scarica"
        if s.startswith("pulling"): return "balestra in carica"
        if s == "arrow": return "balestra carica con freccia"
        if s == "firework": return "balestra carica con razzo"
        return "balestra carica con freccia di gemma o cristallo"
    if n.startswith("bow_pulling"): return "arco teso"
    return {"sword": "spada", "axe": "ascia", "pickaxe": "piccone", "shovel": "pala", "hoe": "zappa",
            "shears": "cesoie", "bow": "arco", "fishing_rod": "canna da pesca",
            "fishing_rod_cast": "canna da pesca lanciata", "helmet": "elmo", "chestplate": "corazza",
            "leggings": "gambali", "boots": "stivali", "ultra_greatsword": "spadone",
            "workstaff": "bastone da lavoro (workstaff)", "bident": "bident"}[n]


def ok(p):
    b = os.path.basename(p)
    return b.endswith(".png") and "_anim" not in b and "armor" not in b and "projectile" not in b


def gruppo(p):
    if "/t6-castone/" in p: return "T6"
    if "/t7/" in p or "/t7-v2" in p: return "T7"
    return "Spectrum"


PATTERNS = [P + "/t6-castone/set/t6_*.png", P + "/t6-castone/speciali/t6_*.png",
            R + "/bedrock_*.png", R + "/*glass_crest*.png"]
if "--con-t7" in sys.argv:  # il T7 si esclude finché l'utente non ci lavora (deciso 2026-11-18)
    # T7 = proposta v2 «puntini fuori» (scelta 2026-11-18); il T7 originale è in t7/
    PATTERNS += [P + "/t7-v2-puntini-fuori/set/t7_*.png", P + "/t7-v2-puntini-fuori/speciali/t7_*.png"]
files = []
for pat in PATTERNS:
    files += [f for f in glob.glob(pat) if ok(f)]
files = sorted(set(files))
small = [f for f in files if "_oversized" not in f]
big = [f for f in files if "_oversized" in f]

random.seed()
for d in ("immagini", "chiave"):
    shutil.rmtree(os.path.join(OUT, d), ignore_errors=True)
os.makedirs(os.path.join(OUT, "chiave"))


def lotti(lst, n):
    random.shuffle(lst)
    k = -(-len(lst) // n)
    return [lst[i * k:(i + 1) * k] for i in range(n)]


key = []
num = 0
for prefisso, lst, n in (("lotto", small, 3), ("lotto_oversized", big, 2)):
    for i, lot in enumerate(lotti(lst, n), 1):
        d = os.path.join(OUT, "immagini", f"{prefisso}_{i}")
        os.makedirs(d)
        for f in lot:
            num += 1
            nome = f"img_{num:03d}.png"
            im = Image.open(f).convert("RGBA")
            s = 512 // im.width
            im = im.resize((im.width * s, im.height * s), Image.NEAREST)
            bg = Image.new("RGBA", im.size, (128, 128, 128, 255))
            bg.alpha_composite(im)
            bg.convert("RGB").save(os.path.join(d, nome))
            key.append({"lotto": f"{prefisso}_{i}", "img": nome, "file": os.path.relpath(f, ROOT),
                        "gruppo": gruppo(f), "atteso": label(os.path.splitext(os.path.basename(f))[0])})
json.dump(key, open(os.path.join(OUT, "chiave", "chiave.json"), "w"), indent=1, ensure_ascii=False)
print(len(small), len(big), Counter(k["lotto"] for k in key))
for d in sorted(os.listdir(os.path.join(OUT, "immagini"))):
    fs = sorted(os.listdir(os.path.join(OUT, "immagini", d)))
    print(d, fs[0], fs[-1], len(fs))
