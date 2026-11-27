"""Controllo statico dei dati della mod, da lanciare prima di ogni prova in gioco.

Controlla ciò che Minecraft e Spectrum ignorerebbero in silenzio (campi scritti male, id inesistenti,
traduzioni mancanti) e le regole della checklist ricerca/00-checklist-progressione.md.

Uso: python3 -I tools/validate.py [cartella mods]
Senza argomento i jar di Spectrum e Allthemodium sono cercati da tools/sa_paths.py (variabili SA_*, cache di Gradle).
Esce con codice 1 se trova errori.
"""
import json
import os
import re
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
JAVA = os.path.join(ROOT, "src", "main", "java")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from sa_paths import spectrum_jar, atm_jar  # noqa: E402
MOD = "spectral_ascension"

errors, warnings = [], []


def err(msg):
    errors.append(msg)


def warn(msg):
    warnings.append(msg)


# ---------- Contenuto dei jar di Spectrum e Allthemodium ----------

def jar_names(prefix, trova):
    if len(sys.argv) > 1:
        for f in os.listdir(sys.argv[1]):
            if f.startswith(prefix) and f.endswith(".jar"):
                with zipfile.ZipFile(os.path.join(sys.argv[1], f)) as z:
                    return set(z.namelist())
        sys.exit(f"jar {prefix}* non trovato in {sys.argv[1]}")
    with zipfile.ZipFile(trova()) as z:
        return set(z.namelist())


EXT = jar_names("spectrum-", spectrum_jar) | jar_names("allthemodium-", atm_jar)


def ext_item(ns, name):
    return f"assets/{ns}/models/item/{name}.json" in EXT


def ext_tag(ns, path):
    return f"data/{ns}/tags/item/{path}.json" in EXT


def ext_adv(ns, path):
    return f"data/{ns}/advancement/{path}.json" in EXT


# ---------- Contenuto della mod ----------

def load_json(path):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:  # noqa: BLE001
        err(f"JSON non valido: {os.path.relpath(path, ROOT)}: {e}")
        return None


def walk(sub):
    base = os.path.join(RES, sub)
    for r, _, fs in os.walk(base):
        for f in fs:
            yield os.path.join(r, f)


java = ""
for r, _, fs in os.walk(JAVA):
    for f in fs:
        if f.endswith(".java"):
            java += open(os.path.join(r, f), encoding="utf-8").read()
ITEMS = set(re.findall(r'ITEMS\.register\("([a-z0-9_/]+)"', java))
# Armatura: SAItems.armorPiece(SATier.X, materiale, ArmorItem.Type.Y, livello) registra «<prefisso del tier>_<pezzo>».
_tier_prefix = dict(re.findall(r'^\s*(\w+)\("(\w+)"', open(os.path.join(JAVA, "cromocon", "spectralascension", "item", "SATier.java"), encoding="utf-8").read(), re.M))
ITEMS |= {f"{_tier_prefix[t]}_{piece.lower()}"
          for t, piece in re.findall(r'armorPiece\(SATier\.(\w+),[^,]+,\s*ArmorItem\.Type\.(\w+)', java)}
if not ITEMS:
    err("nessun item trovato in SAItems")

ADVS = {os.path.relpath(p, os.path.join(RES, f"data/{MOD}/advancement"))[:-5].replace(os.sep, "/")
        for p in walk(f"data/{MOD}/advancement") if p.endswith(".json")}
RECIPES = {os.path.relpath(p, os.path.join(RES, f"data/{MOD}/recipe"))[:-5].replace(os.sep, "/")
           for p in walk(f"data/{MOD}/recipe") if p.endswith(".json")}
LANG = {}
for lang in ("en_us", "it_it"):
    LANG[lang] = load_json(os.path.join(RES, f"assets/{MOD}/lang/{lang}.json")) or {}


def item_exists(ref):
    ns, _, name = ref.partition(":")
    if ns == MOD:
        return name in ITEMS
    if ns == "minecraft":
        return True  # non verificabile senza il jar di Minecraft
    return ext_item(ns, name)


def tag_exists(ref):
    ns, _, path = ref.lstrip("#").partition(":")
    if ns in ("c", "minecraft", "neoforge") and not ext_tag(ns, path):
        local = os.path.join(RES, f"data/{ns}/tags/item/{path}.json")
        return os.path.exists(local) or ns != "c"  # i tag c: devono esistere in Spectrum/ATM o nella mod
    return ext_tag(ns, path) or os.path.exists(os.path.join(RES, f"data/{ns}/tags/item/{path}.json"))


def adv_exists(ref):
    ns, _, path = ref.partition(":")
    return path in ADVS if ns == MOD else (ns == "minecraft" or ext_adv(ns, path))


def check_ingredient(where, ing):
    if isinstance(ing, str):
        ref = ing
    elif isinstance(ing, dict):
        ref = ing.get("item") or ("#" + ing["tag"] if "tag" in ing else None)
        if ref is None:
            err(f"{where}: ingrediente senza 'item' né 'tag': {ing}")
            return
    else:
        err(f"{where}: ingrediente non valido: {ing}")
        return
    if ref.startswith("#"):
        if not tag_exists(ref):
            err(f"{where}: tag inesistente {ref}")
    elif not item_exists(ref):
        err(f"{where}: item inesistente {ref}")


# ---------- Ricette ----------

PEDESTAL_KEYS = {"type", "group", "required_advancement", "reveal_secret_advancement",
                 "additional_recipe_viewer_results", "tier", "pattern", "key", "ingredients", "colors", "result",
                 "experience", "time", "skip_recipe_remainders", "disable_yield_upgrades", "neoforge:conditions"}
FUSION_KEYS = {"type", "group", "required_advancement", "reveal_secret_advancement", "fluid", "ingredients",
               "result", "experience", "time", "copy_components", "disable_yield_upgrades",
               "play_crafting_finished_effects", "world_conditions", "effects", "description", "neoforge:conditions"}
COLORS = {"spectrum:cyan", "spectrum:magenta", "spectrum:yellow", "spectrum:black", "spectrum:white"}

for rid in sorted(RECIPES):
    path = os.path.join(RES, f"data/{MOD}/recipe/{rid}.json")
    d = load_json(path)
    if d is None:
        continue
    where = f"ricetta {MOD}:{rid}"
    t = d.get("type")
    if t in ("spectrum:pedestal", "spectrum:pedestal_shapeless"):
        for k in set(d) - PEDESTAL_KEYS:
            err(f"{where}: campo sconosciuto '{k}' (verrebbe ignorato)")
        if d.get("tier") != "complex":
            err(f"{where}: tier '{d.get('tier')}' invece di 'complex' (Pedestal Moonstone)")
        if "/tier4/" not in "/" + rid:
            err(f"{where}: le ricette complex vanno in una cartella tier4/ (sanity di Spectrum)")
        colors = d.get("colors")
        if not isinstance(colors, dict):
            err(f"{where}: manca 'colors'")
        else:
            for c in set(colors) - COLORS:
                err(f"{where}: colore sconosciuto {c}")
        if t == "spectrum:pedestal":
            pattern, key = d.get("pattern", []), d.get("key", {})
            used = set("".join(pattern)) - {" "}
            if len({len(r) for r in pattern}) > 1:
                err(f"{where}: righe del pattern di lunghezza diversa")
            for ch in used - set(key):
                err(f"{where}: simbolo '{ch}' del pattern assente nella key")
            for ch in set(key) - used:
                err(f"{where}: simbolo '{ch}' della key non usato (Spectrum dà errore)")
            for ch, ing in key.items():
                check_ingredient(where, ing)
        else:
            for ing in d.get("ingredients", []):
                check_ingredient(where, ing)
    elif t == "spectrum:fusion_shrine":
        for k in set(d) - FUSION_KEYS:
            err(f"{where}: campo sconosciuto '{k}' (verrebbe ignorato)")
        ings = d.get("ingredients", [])
        for ing in ings:
            check_ingredient(where, ing)
        # La Fusion Shrine ha 7 slot (FusionShrineBlockEntity, codec listOf(0, 7)): il liquido non conta.
        if not 1 <= len(ings) <= 7:
            err(f"{where}: {len(ings)} ingredienti alla Fusion Shrine (da 1 a 7, liquido escluso)")
        if d.get("copy_components") and not d.get("disable_yield_upgrades"):
            err(f"{where}: upgrade con copy_components senza disable_yield_upgrades (rischio dupe, checklist D2)")
    else:
        warn(f"{where}: tipo {t} non controllato")
    res = d.get("result", {})
    if "item" in res:
        err(f"{where}: result usa 'item' (formato 1.20): in 1.21 è 'id'")
    if not item_exists(res.get("id", "")):
        err(f"{where}: risultato inesistente {res.get('id')}")
    adv = d.get("required_advancement")
    if not adv:
        err(f"{where}: manca required_advancement")
    elif not adv_exists(adv):
        err(f"{where}: required_advancement inesistente {adv}")
    elif "__wip" in adv:
        err(f"{where}: dipende da spectrum:__wip (irraggiungibile)")
    group = d.get("group")
    if group:
        for lang, keys in LANG.items():
            if f"recipeGroup.spectrum.{group}" not in keys:
                err(f"{where}: gruppo '{group}' senza traduzione recipeGroup.spectrum.{group} in {lang}")

# ---------- Advancement ----------

for aid in sorted(ADVS):
    d = load_json(os.path.join(RES, f"data/{MOD}/advancement/{aid}.json"))
    if d is None:
        continue
    for cname, crit in d.get("criteria", {}).items():
        ref = crit.get("conditions", {}).get("advancement_identifier")
        if ref:
            if not adv_exists(ref):
                err(f"advancement {MOD}:{aid}: criterio '{cname}' cita advancement inesistente {ref}")
            if ref == f"{MOD}:{aid}":
                err(f"advancement {MOD}:{aid}: dipende da se stesso")
            if "__wip" in ref:
                err(f"advancement {MOD}:{aid}: dipende da spectrum:__wip (irraggiungibile)")
        for pred in crit.get("conditions", {}).get("items", []):
            items = pred.get("items")
            if isinstance(items, str) and items.startswith("#") and not tag_exists(items):
                err(f"advancement {MOD}:{aid}: tag inesistente {items}")

# Override di Spectrum per l'overenchanting (gen_data.gen_overenchanting_unlock): niente spectrum:__wip, criteri
# tutti in un'unica riga di requirements (OR), con un livello minimo: per ogni incantesimo un libro (book_*,
# stored_enchantments) e un oggetto incantato non nostro (item_*, enchantments + spectral_ascension:not_ours).
_over = load_json(os.path.join(RES, "data/spectrum/advancement/milestones/unlock_overenchanting_with_enchanter.json"))
if _over is None:
    err("manca l'override spectrum:milestones/unlock_overenchanting_with_enchanter")
else:
    if "__wip" in json.dumps(_over):
        err("unlock_overenchanting_with_enchanter: dipende ancora da spectrum:__wip")
    if _over.get("requirements") != [list(_over.get("criteria", {}))]:
        err("unlock_overenchanting_with_enchanter: i criteri non sono un'unica riga in OR")
    _crit = _over.get("criteria", {})
    for _cn, _c in _crit.items():
        _items = _c.get("conditions", {}).get("items", [])
        _book = _cn.startswith("book_")
        _key = "minecraft:stored_enchantments" if _book else "minecraft:enchantments"
        _preds = [p for i in _items for p in i.get("predicates", {}).get(_key, [])]
        _ok = _c.get("trigger") == "minecraft:inventory_changed" and _preds and all("min" in p.get("levels", {}) for p in _preds)
        if not _book:
            _ok = _ok and _cn.startswith("item_") and all(f"{MOD}:not_ours" in i.get("predicates", {}) for i in _items)
        if not _ok:
            err(f"unlock_overenchanting_with_enchanter: criterio '{_cn}' non è un libro o un oggetto non nostro con livello minimo")
    _bk = {c[len("book_"):] for c in _crit if c.startswith("book_")}
    _it = {c[len("item_"):] for c in _crit if c.startswith("item_")}
    if _bk != _it or not _bk:
        err(f"unlock_overenchanting_with_enchanter: libri e oggetti non coprono gli stessi incantesimi ({sorted(_bk ^ _it)})")

# Pagina «Overchanting» del guidebook di Spectrum (gen_data.gen_overchanting_guidebook): le chiavi sono sovrascritte
# in assets/spectrum/lang/en_us.json (stesso file di Spectrum, non il namespace della mod: vincerebbe «spectrum») e i
# livelli massimi elencati non superano il tetto ATM10 di SAEnchantCap (Thorns V, non VI).
_oc = load_json(os.path.join(RES, "assets/spectrum/lang/en_us.json")) or {}
_oc_text = "\n".join(_oc.get(f"book.spectrum.guidebook.overchanting.page{i}.text", "") for i in (1, 2, 3))
if not _oc_text:
    err("manca l'override del testo della pagina Overchanting in assets/spectrum/lang/en_us.json")
else:
    _cap_src = open(os.path.join(ROOT, "src/main/java/cromocon/spectralascension/item/SAEnchantCap.java"), encoding="utf-8").read()
    _cap = {k: int(v) for k, v in re.findall(r'Map\.entry\("(\w+)", (\d+)\)', _cap_src)}
    _names = {"Thorns": "thorns", "Bane of Arthrop.": "bane_of_arthropods", "Sharpness": "sharpness", "Efficiency": "efficiency",
              "Fortune": "fortune", "Looting": "looting", "Power": "power", "Protection": "protection", "Unbreaking": "unbreaking"}
    for _m in re.finditer(r"^- (.+?): (\d+) \u2192 (\d+)$", _oc_text, re.M):
        _n, _hi = _m.group(1), int(_m.group(3))
        if _n in _names and _hi > _cap[_names[_n]]:
            err(f"pagina Overchanting: {_n} arriva a {_hi}, oltre il tetto ATM10 {_cap[_names[_n]]}")
    for _want in ("- Thorns: 3 \u2192 5", "- Exuberance: 5 \u2192 8", "- Sniping: 2 \u2192 5", "- Bane of Arthrop.: 5 \u2192 5"):
        if _want not in _oc_text:
            err(f"pagina Overchanting: manca la riga «{_want}»")

# Ingredienti con un proprio sblocco (es. accessori di Spectrum in unlocks/trinkets/): l'advancement della
# ricetta deve richiederlo, altrimenti ricetta e pagina del guidebook compaiono prima che il giocatore possa
# avere l'ingrediente (checklist §A/§B).
for rid in sorted(RECIPES):
    d = load_json(os.path.join(RES, f"data/{MOD}/recipe/{rid}.json")) or {}
    adv = d.get("required_advancement", "")
    if not adv.startswith(MOD + ":"):
        continue
    a = load_json(os.path.join(RES, f"data/{MOD}/advancement/{adv.partition(':')[2]}.json")) or {}
    required = {c.get("conditions", {}).get("advancement_identifier") for c in a.get("criteria", {}).values()}
    crit = a.get("criteria", {})
    reqs = a.get("requirements") or [[n] for n in crit]
    if sorted(n for g in reqs for n in g) != sorted(crit):
        err(f"advancement {adv}: requirements e criteria non coincidono (criterio mancante o ripetuto)")
    for g in reqs:
        # Un gruppo in OR deve essere solo la coppia gotten + tick sullo stesso advancement (vedi gen_data.gotten_with_tick).
        if len(g) > 1:
            ids = {crit[n].get("conditions", {}).get("advancement_identifier") for n in g if n in crit}
            ticks = {k for n in g if n in crit and crit[n].get("trigger") == "minecraft:tick"
                     for k in crit[n]["conditions"]["player"][0]["predicate"]["type_specific"]["advancements"]}
            if len(g) != 2 or ids - {None} != ticks:
                err(f"advancement {adv}: gruppo OR {g} non è una coppia gotten/tick sullo stesso advancement")
    ings = list(d.get("key", {}).values()) + list(d.get("ingredients", []))
    for ing in ings:
        ref = ing if isinstance(ing, str) else (ing.get("item") if isinstance(ing, dict) else None)
        if not ref or ref.startswith("#"):
            continue
        ns, _, name = ref.partition(":")
        # Accessori (unlocks/trinkets/) e attrezzi malachite/glass crest (unlocks/malachite/, es. la workstaff
        # glass crest che la Fusion Shrine trasforma in quella T6).
        for unlock in (f"unlocks/trinkets/{name}", f"unlocks/malachite/{name}"):
            if ext_adv(ns, unlock) and f"{ns}:{unlock}" not in required:
                err(f"ricetta {MOD}:{rid}: usa {ref} ma {adv} non richiede {ns}:{unlock}")

# Cicli tra i nostri advancement
graph = {}
for aid in ADVS:
    d = load_json(os.path.join(RES, f"data/{MOD}/advancement/{aid}.json")) or {}
    graph[aid] = [c.get("conditions", {}).get("advancement_identifier", "").partition(":")[2]
                  for c in d.get("criteria", {}).values()
                  if c.get("conditions", {}).get("advancement_identifier", "").startswith(MOD + ":")]


def has_cycle(node, stack):
    if node in stack:
        return stack[stack.index(node):] + [node]
    for nxt in graph.get(node, []):
        found = has_cycle(nxt, stack + [node])
        if found:
            return found
    return None


for aid in graph:
    cyc = has_cycle(aid, [])
    if cyc:
        err("riferimento circolare tra advancement: " + " -> ".join(cyc))
        break

# ---------- Item: modello, texture, traduzioni ----------

for item in sorted(ITEMS):
    model = os.path.join(RES, f"assets/{MOD}/models/item/{item}.json")
    if not os.path.exists(model):
        err(f"item {item}: manca il modello {os.path.relpath(model, ROOT)}")
    else:
        for tex in (load_json(model) or {}).get("textures", {}).values():
            ns, _, p = tex.partition(":")
            if ns == MOD and not os.path.exists(os.path.join(RES, f"assets/{MOD}/textures/{p}.png")):
                err(f"item {item}: texture mancante {tex}")
    for lang, keys in LANG.items():
        if f"item.{MOD}.{item}" not in keys:
            err(f"item {item}: manca la traduzione in {lang}")
    if not any(f'"{MOD}:{item}"' in open(os.path.join(RES, f"data/{MOD}/recipe/{r}.json"), encoding="utf-8").read()
               for r in RECIPES):
        err(f"item {item}: nessuna ricetta lo produce")

# Texture SEGNAPOSTO (gen_data.PLACEHOLDER_TEXTURE e BEDROCK_MACE, 2026-11-25): modelli delle mazze che usano ancora
# la texture di un altro oggetto. Non è un errore (le texture vere aspettano l'approvazione dell'utente), ma va ricordato.
for item in sorted(ITEMS):
    if item.endswith("_mace"):
        model = load_json(os.path.join(RES, f"assets/{MOD}/models/item/{item}.json")) or {}
        tex = model.get("textures", {}).get("layer0", "")
        if tex != f"{MOD}:item/{item}":
            warn(f"item {item}: texture SEGNAPOSTO {tex} (da sostituire con la texture approvata)")

# ---------- Tag ----------

for path in walk("data"):
    if "/tags/" not in path.replace(os.sep, "/") or not path.endswith(".json"):
        continue
    d = load_json(path) or {}
    for ref in d.get("values", []) + d.get("remove", []):
        ref = ref if isinstance(ref, str) else ref.get("id", "")
        if not ref.startswith("#") and ref.startswith(MOD + ":") and not item_exists(ref):
            err(f"tag {os.path.relpath(path, RES)}: item inesistente {ref}")

# ---------- Guidebook (Modonomicon) ----------

BOOK = "data/spectrum/modonomicon/books/guidebook/entries"
for path in walk(BOOK):
    rel = os.path.relpath(path, RES).replace(os.sep, "/")
    where = "guidebook " + rel.split("entries/")[1]
    if rel in EXT:
        err(f"{where}: sovrascrive una voce di Spectrum (nome file già usato)")
    if not os.path.basename(path).startswith(MOD):
        err(f"{where}: il nome file deve iniziare con '{MOD}' per non sovrascrivere voci di altri")
    d = load_json(path) or {}
    if d.get("description") != f"book.{MOD}.added_by":
        err(f"{where}: description deve essere book.{MOD}.added_by (tooltip «added by», richiesta di DaFuqs)")
    cond = d.get("condition", {})
    if cond.get("type") == "modonomicon:advancement" and not adv_exists(cond.get("advancement_id", "")):
        err(f"{where}: condizione su advancement inesistente {cond.get('advancement_id')}")
    for parent in d.get("parents", []):
        pe = parent.get("entry", "")
        ns, _, p = pe.partition(":")
        if f"data/{ns}/modonomicon/books/guidebook/entries/{p}.json" not in EXT \
                and not os.path.exists(os.path.join(RES, f"{BOOK}/{p}.json")):
            err(f"{where}: voce genitore inesistente {pe}")
    keys = [d.get("name"), d.get("description")]
    for page in d.get("pages", []):
        keys += [page.get("title"), page.get("text")]
        rid = page.get("recipe_id")
        if rid:
            ns, _, p = rid.partition(":")
            if ns == MOD and p not in RECIPES:
                err(f"{where}: pagina con ricetta inesistente {rid}")
    for k in filter(None, keys):
        if k.startswith(("book.", "item.")) and k.split(".")[1] == MOD:
            for lang, lk in LANG.items():
                if k not in lk:
                    err(f"{where}: chiave di traduzione mancante in {lang}: {k}")

# ---------- Texture: dimensioni (animate e statiche) e armatura indossata ----------

def png_size(path):
    with open(path, "rb") as f:
        h = f.read(24)
    return int.from_bytes(h[16:20], "big"), int.from_bytes(h[20:24], "big")

TEX_ITEM = os.path.join(RES, f"assets/{MOD}/textures/item")
for fn in sorted(os.listdir(TEX_ITEM)):
    if not fn.endswith(".png"):
        continue
    w, h = png_size(os.path.join(TEX_ITEM, fn))
    if os.path.exists(os.path.join(TEX_ITEM, fn + ".mcmeta")):
        if h % w:
            err(f"texture animata {fn}: altezza {h} non multipla della larghezza {w}")
    elif (w, h) not in ((16, 16), (32, 32)):
        err(f"texture {fn}: {w}x{h}, atteso 16x16 o 32x32 (o un .mcmeta se animata)")

for fn in sorted(os.listdir(TEX_ITEM)):
    if fn.endswith(".png.mcmeta") and fn.split("_")[0] in ("trichromatic", "umbral", "argent"):
        err(f"texture {fn}: T6-T8 sono statici (solo il T9 e' animato), niente .mcmeta")

TEX_ARMOR = os.path.join(RES, f"assets/{MOD}/textures/armor")
# Fotogrammi dell'armatura indossata per tier, letti da SAArmorItem.java (mappa ARMOR_FRAMES) e prefissi degli id
# letti da SATier.java: così codice, file installati e installatore non possono divergere.
with open(os.path.join(ROOT, "src/main/java/cromocon/spectralascension/item/SAArmorItem.java"), encoding="utf-8") as f:
    _java = f.read()
_frames_block = re.search(r"ARMOR_FRAMES\s*=\s*Map\.of\((.*?)\);", _java, re.S)
if not _frames_block:
    err("SAArmorItem.java: mappa ARMOR_FRAMES non trovata")
    ARMOR_FRAMES_BY_TIER = {}
else:
    ARMOR_FRAMES_BY_TIER = {t: int(n) for t, n in re.findall(r"SATier\.(\w+),\s*(\d+)", _frames_block.group(1))}
with open(os.path.join(ROOT, "src/main/java/cromocon/spectralascension/item/SATier.java"), encoding="utf-8") as f:
    TIER_PREFIX = dict(re.findall(r"^\s*(\w+)\(\"(\w+)\"", f.read(), re.M))
ARMOR_FRAMES = {TIER_PREFIX[t]: n for t, n in ARMOR_FRAMES_BY_TIER.items() if t in TIER_PREFIX}  # prefisso -> N
for fn in sorted(os.listdir(TEX_ARMOR)):
    m = re.fullmatch(r"(.+)_armor\.png", fn)
    if m:
        n = ARMOR_FRAMES.get(m.group(1))
        if n is None:
            err(f"armatura indossata {fn}: tier senza fotogrammi in SAArmorItem.ARMOR_FRAMES")
            continue
        for i in range(n):
            if not os.path.exists(os.path.join(TEX_ARMOR, f"{m.group(1)}_armor_{i}.png")):
                err(f"armatura indossata: manca il fotogramma {m.group(1)}_armor_{i}.png")
    m = re.fullmatch(r"(.+)_armor_(\d+)\.png", fn)
    if m and int(m.group(2)) >= ARMOR_FRAMES.get(m.group(1), 0):
        err(f"armatura indossata: fotogramma {fn} oltre ARMOR_FRAMES={ARMOR_FRAMES.get(m.group(1))} (residuo, da togliere)")

# Texture installate da tools/install_textures.py: devono coincidere byte per byte con i set approvati in design/
# (per T6-T8, statici dal 2026-11-26, con le icone ritagliate al fotogramma 0)
# (T6 = t6-castone, T7 = t7-v2-puntini-fuori, T8 = t8-argent, T9 = t9-m3). Protegge da sovrascritture a mano, da placeholder di gen_data.py e da vecchi script
import importlib.util
_spec = importlib.util.spec_from_file_location("install_textures", os.path.join(ROOT, "tools", "install_textures.py"))
_inst = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_inst)
# Tier il cui set in design/ e' APPROVATO e va confrontato byte per byte con l'installato. T7 «Umbral» e' fuori finche'
# l'utente non approva le modifiche in design/texture/proposte/t7: quando le approva, rimetterlo qui e rilanciare
# python3 -I tools/install_textures.py --tier t7. Gli altri controlli (fotogrammi, residui) valgono per tutti i tier.
APPROVED_TIERS = ("t6", "t7", "t8", "t9")
# design/ è solo locale (fuori dalla repo): in un clone pulito il confronto con i set approvati viene saltato.
HAS_DESIGN = os.path.isdir(os.path.join(ROOT, "design", "texture"))
if not HAS_DESIGN:
    warn("design/texture non presente: confronto delle texture con i set approvati saltato")
for _tier, (_folder, _sp, _prefix, _n) in _inst.TIERS.items():
    if ARMOR_FRAMES.get(_prefix) != _n:
        err(f"texture {_tier}: install_textures.py installa {_n} fotogrammi d'armatura, SAArmorItem ne usa {ARMOR_FRAMES.get(_prefix)}")
    if _tier not in APPROVED_TIERS or not HAS_DESIGN:
        continue
    for src, dst in _inst.pairs_for(_tier):
        if not os.path.exists(src) or not os.path.exists(dst):
            err(f"texture {_tier}: manca {os.path.relpath(src if not os.path.exists(src) else dst, ROOT)}")
        else:
            with open(dst, "rb") as b:
                if _inst.expected_bytes(src, dst) != b.read():
                    err(f"texture {_tier} {os.path.basename(dst)} diversa dal set approvato ({_folder}): "
                        f"rilancia python3 -I tools/install_textures.py")
# Ogni texture item usata dai modelli di un oggetto del tier deve venire dal set approvato (niente placeholder).
_installed = {os.path.basename(d) for t in _inst.TIERS for _, d in _inst.pairs_for(t)}
for fn in sorted(os.listdir(TEX_ITEM)):
    if fn.endswith(".png") and any(fn.startswith(v[2] + "_") for v in _inst.TIERS.values()) and fn not in _installed:
        err(f"texture {fn}: non fa parte di un set approvato (placeholder o residuo?)")

# ---------- Resource pack opzionale «Eternal Classics» ----------
# ECCEZIONE: il pack (src/main/resources/resourcepacks/spectral_ascension_eternal_classics) è scritto a mano
# e NON è generato da gen_data.py (come design/testi.md). Qui si controlla solo che resti coerente con la mod.
_EC = os.path.join(RES, "resourcepacks", "spectral_ascension_eternal_classics")
_EC_TEX = os.path.join(_EC, "assets", MOD, "textures", "item")
_MOD_TEX = os.path.join(RES, "assets", MOD, "textures", "item")
_ec_pngs = sorted(f for f in os.listdir(_EC_TEX) if f.endswith(".png")) if os.path.isdir(_EC_TEX) else []
_tier_pngs = sorted(f for f in os.listdir(_MOD_TEX) if f.endswith(".png") and f.split("_")[0] in ("trichromatic", "umbral", "argent", "prismatic"))
if not os.path.isfile(os.path.join(_EC, "pack.mcmeta")):
    err("Eternal Classics: manca pack.mcmeta")
if len(_ec_pngs) != 184:  # 180 + le 4 mazze (2026-11-26)
    err(f"Eternal Classics: {len(_ec_pngs)} texture invece di 184")
# Texture della mod che non hanno ancora la versione EC (il pack ricade sulla texture base): solo avviso.
EC_DA_FARE = set()  # vuoto: le mazze EC sono state aggiunte il 2026-11-26
for _n in sorted(set(_ec_pngs) ^ set(_tier_pngs)):
    if _n in EC_DA_FARE and _n not in _ec_pngs:
        warn(f"Eternal Classics: {_n} senza versione EC (da disegnare; il pack usa la texture base)")
    else:
        err(f"Eternal Classics: {_n} non coincide 1:1 con le texture T6-T9 della mod")
for _n in _ec_pngs:
    if not os.path.isfile(os.path.join(_EC_TEX, _n + ".mcmeta")):
        err(f"Eternal Classics: manca {_n}.mcmeta")
    # Stessa larghezza, non stessa altezza: la base T6-T8 e' statica (quadrata), l'EC e' sempre una striscia animata.
    if os.path.isfile(os.path.join(_MOD_TEX, _n)) and png_size(os.path.join(_EC_TEX, _n))[0] != png_size(os.path.join(_MOD_TEX, _n))[0]:
        err(f"Eternal Classics: {_n} ha larghezza diversa da quella della mod")

# ---------- Minerali nella Mining Dimension ----------

for _name in sorted(os.listdir(os.path.join(RES, f"data/{MOD}/worldgen/placed_feature/mining_dim"))
                    if os.path.isdir(os.path.join(RES, f"data/{MOD}/worldgen/placed_feature/mining_dim")) else []):
    _ore = _name[:-5]
    _pf = load_json(os.path.join(RES, f"data/{MOD}/worldgen/placed_feature/mining_dim/{_name}")) or {}
    _ns, _, _path = _pf.get("feature", "").partition(":")
    if f"data/{_ns}/worldgen/configured_feature/{_path}.json" not in EXT:
        err(f"mining_dim/{_ore}: configured feature {_pf.get('feature')} non esiste nei jar di Spectrum/ATM")
    _bm_path = os.path.join(RES, f"data/{MOD}/neoforge/biome_modifier/mining_dim/{_name}")
    _bm = load_json(_bm_path) if os.path.exists(_bm_path) else None
    if _bm is None:
        err(f"mining_dim/{_ore}: manca il biome modifier")
        continue
    if _bm.get("type") != "neoforge:add_features":
        err(f"mining_dim/{_ore}: type del biome modifier != neoforge:add_features")
    if _bm.get("biomes") != "allthemodium:mining" or "data/allthemodium/worldgen/biome/mining.json" not in EXT:
        err(f"mining_dim/{_ore}: biomes deve essere l'id diretto allthemodium:mining (esistente in ATM), non un tag")
    if _bm.get("step") != "underground_ores":
        err(f"mining_dim/{_ore}: step != underground_ores")
    if _bm.get("features") != f"{MOD}:mining_dim/{_ore}":
        err(f"mining_dim/{_ore}: features non punta alla placed feature omonima")
    if {"type": "neoforge:mod_loaded", "modid": "allthemodium"} not in _bm.get("neoforge:conditions", []):
        err(f"mining_dim/{_ore}: manca la condizione mod_loaded allthemodium")

# ---------- Lingue allineate ----------

for k in set(LANG["en_us"]) ^ set(LANG["it_it"]):
    err(f"traduzione presente solo in {'en_us' if k in LANG['en_us'] else 'it_it'}: {k}")

print(f"Item: {len(ITEMS)} · Ricette: {len(RECIPES)} · Advancement: {len(ADVS)}")
for w in warnings:
    print("AVVISO ", w)
for e in errors:
    print("ERRORE ", e)
print("OK: nessun errore" if not errors else f"{len(errors)} errori")
sys.exit(1 if errors else 0)
