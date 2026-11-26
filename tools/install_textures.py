#!/usr/bin/env python3
"""Installa nella mod i set di texture APPROVATI, copiando i file già generati in design/texture/proposte/
(non rigenera niente e non tocca gli script tools/texture_*.py).

  T6 «Trichromatic» = set «Castone»: design/texture/proposte/t6-castone/{set,speciali,armatura}
  T7 «Umbral»                     : design/texture/proposte/t7-v2-puntini-fuori/{set,speciali,armatura}
  T8 «Argent»                     : design/texture/proposte/t8-argent/{set,speciali,armatura}
  T9 Alloy «Prismatic»            : design/texture/proposte/t9-m3/{set,speciali,armatura} (40 fotogrammi)

Per ogni tier:
  set/<t>_<pezzo>_anim.png(.mcmeta)      -> textures/item/<prefisso>_<pezzo>.png(.mcmeta)   15 icone animate
  speciali/<t>_<nome>_anim.png(.mcmeta)  -> textures/item/<prefisso>_<nome>.png(.mcmeta)    30 texture speciali
  armatura/<t>_armor.png                 -> textures/armor/<prefisso>_armor.png            (statica)
  armatura/<t>_armor_<i>.png             -> textures/armor/<prefisso>_armor_<i>.png        (i = 0..FRAMES-1)

I nomi di destinazione sono quelli che usano i modelli JSON scritti da tools/gen_data.py (arco e canna con i loro
stati, balestra con i 12 stati ciascuno con _oversized, bident con _oversized che serve anche alla posa di lancio)
e SAArmorItem.getArmorTexture (fotogrammi dell'armatura indossata). FRAMES deve coincidere con ARMOR_FRAMES di
SAArmorItem.java per lo stesso tier: lo controlla tools/validate.py.

Idempotente: copia solo i file diversi e toglie i fotogrammi d'armatura in eccesso di un set precedente.
Uso: python3 -I tools/install_textures.py [--check] [--tier t6|t7|t8|t9]   (--check: non copia, dice solo cosa cambierebbe;
--tier: solo quel tier, senza opzione li installa tutti)
"""
import io
import os
import re
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROPOSTE = os.path.join(ROOT, "design", "texture", "proposte")
MOD_TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "spectral_ascension", "textures")

# tier -> (cartella sorgente, prefisso dei file sorgente, prefisso degli id nella mod, fotogrammi dell'armatura)
TIERS = {
    # Dal 2026-11-26 solo il T9 e' animato: T6, T7 e T8 (frames = 1) installano il fotogramma 0 di ogni icona (senza
    # .mcmeta) e il solo _armor_0 dell'armatura. I sorgenti in design/ restano animati (il pack Eternal Classics
    # e' scritto a mano e resta animato per tutti i tier).
    "t6": ("t6-castone", "t6", "trichromatic", 1),
    "t7": ("t7-v2-puntini-fuori", "t7", "umbral", 1),
    "t8": ("t8-argent", "t8", "argent", 1),
    # T9 Alloy «Prismatic» (approvato dall'utente): set t9-m3, 40 fotogrammi da 5 tick (ciclo di 10 s).
    "t9": ("t9-m3", "t9", "prismatic", 40),
}

# Icone del set base (gli stati di arco e canna compresi).
ICONS = ["sword", "axe", "shovel", "hoe", "shears", "bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2",
         "fishing_rod", "fishing_rod_cast", "helmet", "chestplate", "leggings", "boots",
         "mace"]  # mazza: aggiunta il 2026-11-26 (texture dell utente in design/texture/proposte/mace_t6 e mace_t7_t9)
# Stati della balestra, nell'ordine di gen_data.CROSSBOW_STATES (standby = modello base).
CROSSBOW_STATES = ["standby", "pulling_0", "pulling_1", "pulling_2", "arrow", "firework", "glass", "topaz",
                   "amethyst", "citrine", "onyx", "moonstone"]
SPECIALS = [n + s for n in ["workstaff", "ultra_greatsword", "bident"] + [f"crossbow_{st}" for st in CROSSBOW_STATES]
            for s in ("", "_oversized")]


def is_static(tier):
    """Tier non animato: icone ritagliate al fotogramma 0, niente .mcmeta."""
    return TIERS[tier][3] == 1


def pairs_for(tier):
    """(sorgente, destinazione) di tutti i file di un tier."""
    folder, src_prefix, prefix, frames = TIERS[tier]
    base = os.path.join(PROPOSTE, folder)
    pairs = []
    for sub, names in (("set", ICONS), ("speciali", SPECIALS)):
        for n in names:
            for ext in ((".png",) if frames == 1 else (".png", ".png.mcmeta")):
                pairs.append((os.path.join(base, sub, f"{src_prefix}_{n}_anim{ext}"),
                              os.path.join(MOD_TEX, "item", f"{prefix}_{n}{ext}")))
    pairs.append((os.path.join(base, "armatura", f"{src_prefix}_armor.png"),
                  os.path.join(MOD_TEX, "armor", f"{prefix}_armor.png")))
    for i in range(frames):
        pairs.append((os.path.join(base, "armatura", f"{src_prefix}_armor_{i}.png"),
                      os.path.join(MOD_TEX, "armor", f"{prefix}_armor_{i}.png")))
    return pairs


def all_pairs(tiers=None):
    return [p for tier in (tiers or TIERS) for p in pairs_for(tier)]


def expected_bytes(src, dst):
    """Contenuto che dst deve avere: la sorgente, ritagliata al fotogramma 0 se e' un'icona di un tier statico."""
    with open(src, "rb") as f:
        data = f.read()
    if os.sep + "item" + os.sep in dst and dst.endswith(".png") and any(
            os.path.basename(dst).startswith(v[2] + "_") and v[3] == 1 for v in TIERS.values()):
        from PIL import Image
        im = Image.open(io.BytesIO(data))
        im.load()
        out = io.BytesIO()
        im.crop((0, 0, im.width, im.width)).save(out, "PNG", optimize=True)
        return out.getvalue()
    return data


def same(a, b):
    if not os.path.exists(b):
        return False
    with open(b, "rb") as fb:
        return expected_bytes(a, b) == fb.read()


def stale_frames(tiers=None):
    """Fotogrammi d'armatura installati oltre il numero del set attuale (residui di un set con più fotogrammi)."""
    adir = os.path.join(MOD_TEX, "armor")
    idir = os.path.join(MOD_TEX, "item")
    out = []
    for _, _, prefix, frames in (TIERS[t] for t in (tiers or TIERS)):
        if frames == 1:  # tier statico: i .mcmeta delle icone sono residui
            out += [os.path.join(idir, fn) for fn in sorted(os.listdir(idir))
                    if fn.startswith(prefix + "_") and fn.endswith(".png.mcmeta")]
        for fn in sorted(os.listdir(adir)) if os.path.isdir(adir) else []:
            m = re.fullmatch(re.escape(prefix) + r"_armor_(\d+)\.png", fn)
            if m and int(m.group(1)) >= frames:
                out.append(os.path.join(adir, fn))
    return out


def main():
    args = sys.argv[1:]
    check = "--check" in args
    tiers = None  # senza --tier: tutti i tier (comportamento originale)
    if "--tier" in args:
        i = args.index("--tier")
        tiers = [args[i + 1]] if i + 1 < len(args) else []
        if not tiers or tiers[0] not in TIERS:
            sys.exit(f"--tier richiede uno tra: {', '.join(TIERS)}")
    pairs = all_pairs(tiers)
    missing = [s for s, _ in pairs if not os.path.exists(s)]
    if missing:
        sys.exit("mancano i sorgenti approvati:\n  " + "\n  ".join(missing))
    changed = [(s, d) for s, d in pairs if not same(s, d)]
    stale = stale_frames(tiers)
    if check:
        for s, d in changed:
            print("da copiare:", os.path.relpath(d, ROOT))
        for f in stale:
            print("da togliere:", os.path.relpath(f, ROOT))
        print(f"{len(pairs)} file attesi, {len(changed)} da copiare, {len(stale)} da togliere")
        sys.exit(1 if changed or stale else 0)
    for s, d in changed:
        os.makedirs(os.path.dirname(d), exist_ok=True)
        with open(d, "wb") as f:
            f.write(expected_bytes(s, d))
    for f in stale:
        os.remove(f)
    print(f"texture: {len(pairs)} file gestiti ({', '.join(f'{t} {len(pairs_for(t))}' for t in (tiers or TIERS))}), "
          f"{len(changed)} copiati, {len(stale)} fotogrammi vecchi tolti")


if __name__ == "__main__":
    main()
