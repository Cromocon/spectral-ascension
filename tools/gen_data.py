"""Generatore dei dati della mod: ricette, advancement, modelli, texture provvisorie, tag, traduzioni e
pagine del guidebook, a partire dalla tabella OBJECTS qui sotto (una riga per oggetto).

È l'equivalente semplificato del "datagen" dei modder: i file generati NON vanno modificati a mano,
si cambia la riga e si rilancia. Il codice Java degli item (SAItems) resta scritto a mano.

Uso: python3 -I tools/gen_data.py [jar di spectrum]
Poi: python3 -I tools/validate.py
"""
import json
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
MOD = "spectral_ascension"
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from sa_paths import spectrum_jar  # noqa: E402
JAR = sys.argv[1] if len(sys.argv) > 1 else spectrum_jar()

# ---------- Tier ----------
# Materiale ATM (ingot) e prefisso degli id = nome del tier.
TIERS = {
    "t6": {
        "prefix": "trichromatic", "ingot": "c:ingots/allthemodium",
        "name_en": "Trichromatic",
        # Aggettivo italiano per genere/numero: m, f, mp (maschile plurale), fp.
        "name_it": {"m": "tricromatico", "f": "tricromatica", "mp": "tricromatici", "fp": "tricromatiche"},
        "entry": {"x": 0, "y": 3, "parent": "spectrum:equipment/tools_and_armor/bedrock_tools"},
        "entry_name_en": "Trichromatic Equipment", "entry_name_it": "Equipaggiamento tricromatico",
        "page0_en": "Bedrock was only the beginning. Deep in the ancient cities I found a golden metal, Allthemodium, "
                    "and in a **Fusion Shrine** filled with **Dragonrot** it drank in the three pigments as if it had been "
                    "waiting for them.\\\n\\\n"
                    "I joined the **unbreakability of Bedrock** with the **magical properties of Allthemodium**: "
                    "what was born of it knows no wear, and no enchantment needs to guard it any longer.\\\n\\\n"
                    "*Three colors, one metal.*",
        "page0_it": "La bedrock era solo l'inizio. Nelle profondità delle ancient city ho trovato un metallo dorato, "
                    "l'allthemodium, e in una **Fusion Shrine** piena di **Dragonrot** ha assorbito i tre pigmenti come se "
                    "li stesse aspettando.\\\n\\\n"
                    "Ho unito l'**indistruttibilità della bedrock** alle **proprietà magiche dell'allthemodium**: "
                    "ciò che ne è nato non conosce usura, nessun incanto deve più proteggerlo.\\\n\\\n"
                    "*Tre colori, un solo metallo.*",
    },
    # T7: tema nero (onice).
    # Ogni pezzo si fa dal pezzo T6 corrispondente (base) più vibranium, con la stessa macchina e lo stesso schema.
    "t7": {
        "prefix": "umbral", "ingot": "c:ingots/vibranium",
        "name_en": "Umbral",
        "name_it": {"m": "umbrale", "f": "umbrale", "mp": "umbrali", "fp": "umbrali"},
        # Accanto alla voce T6 (0,3), che è anche il genitore: la linea nel libro mostra la catena dei tier.
        "entry": {"x": 1, "y": 3, "parent": "spectrum:equipment/spectral_ascension_t6_equipment"},
        # Lucchetto del tier: il tier precedente (checklist C: T6 -> T7), che porta con sé il Pedestal Moonstone.
        "requires_tier": "t6",
        # Nome e prima pagina della voce. Diario in prima persona.
        "entry_name_en": "Umbral Equipment", "entry_name_it": "Equipaggiamento umbrale",
        "page0_en": "I thought the three pigments had nothing left to teach me. Then, in the Nether, I found a green metal, "
                    "Vibranium, and when I held it up to the light it *drank* it, the way Onyx does: a metal that keeps a "
                    "little of the dark inside.\\\n\\\n"
                    "*Whatever it had learned, it kept.*",
        "page0_it": "Credevo che i tre pigmenti non avessero più niente da insegnarmi. Poi nel nether ho trovato un "
                    "metallo verde, il vibranium, e quando l'ho alzato verso la luce l'ha *bevuta*, come fa l'onice: un "
                    "metallo che trattiene dentro un po' di buio.\\\n\\\n"
                    "*Quello che aveva imparato, l'ha tenuto.*",
    },
    # T8: tema bianco (moonstone). Dal 2026-11-19 tutti i pezzi alla Fusion Shrine.
    # Ogni pezzo si fa dal pezzo T7 corrispondente (base) più unobtainium e polvere di moonstone.
    "t8": {
        "prefix": "argent", "ingot": "c:ingots/unobtainium",
        "name_en": "Argent",
        "name_it": {"m": "argenteo", "f": "argentea", "mp": "argentei", "fp": "argentee"},
        # Accanto alla voce T7 (1,3), che è il genitore: la catena dei tier continua nel libro.
        "entry": {"x": 2, "y": 3, "parent": f"spectrum:equipment/{MOD}_t7_equipment"},
        # Lucchetto del tier: il T7 (checklist C: T7 -> T8) e, come da ricerca/04 §5.1, gli attrezzi glass crest.
        "requires_tier": "t7",
        "extra_unlocks": ["spectrum:unlocks/malachite/glass_crest_tools"],
        "entry_name_en": "Argent Equipment", "entry_name_it": "Equipaggiamento argenteo",
        "page0_en": "The dark kept everything I gave it, but gave nothing back. Then, in the End, I found a violet metal, "
                    "Unobtainium, and it does the opposite: hold it under the moon and it *returns* the light, pale and "
                    "whole, the way Moonstone does.\\\n\\\n"
                    "*The shadow is still in there. Now it shines.*",
        "page0_it": "Il buio teneva tutto quello che gli davo, ma non restituiva niente. Poi nell'end ho trovato un "
                    "metallo viola, l'unobtainium, che fa il contrario: tenuto sotto la luna *restituisce* la luce, "
                    "pallida e intera, come fa la moonstone.\\\n\\\n"
                    "*L'ombra è ancora lì dentro. Adesso brilla.*",
    },
    # T9 = tier Alloy «Prismatic» (SATier.ALLOY in Java; qui "t9" come nelle cartelle delle texture). Tema prismatico:
    # Ogni pezzo si fa alla Fusion Shrine (dal 2026-11-19) dal pezzo T8 (base) più 1 lingotto di
    # ciascuna delle 3 leghe di Allthemodium (ricerca/README.md «Materiali per tier», ricerca/14): niente Piglich Heart
    # come ingrediente (decisione dell'utente 2026-11-18), niente aste (le leghe hanno solo lingotto, polvere e
    # blocco). "ingot" = ingrediente di riparazione nominale di SATier.ALLOY; "alloys" = le 3 leghe della ricetta e
    # del lucchetto. RICETTE PROVVISORIE: T7/T8/T9 aspettano le liste dell'utente (PASSAGGIO.md, «Da fare» n. 2).
    "t9": {
        "prefix": "prismatic", "ingot": "c:ingots/unobtainium_allthemodium_alloy",
        "alloys": ["c:ingots/vibranium_allthemodium_alloy", "c:ingots/unobtainium_allthemodium_alloy",
                   "c:ingots/unobtainium_vibranium_alloy"],
        "name_en": "Prismatic",
        "name_it": {"m": "prismatico", "f": "prismatica", "mp": "prismatici", "fp": "prismatiche"},
        # Accanto alla voce T8 (2,3), che è il genitore: la catena dei tier continua nel libro.
        "entry": {"x": 3, "y": 3, "parent": f"spectrum:equipment/{MOD}_t8_equipment"},
        # Lucchetto del tier (ricerca/04 §5.1): il T8 e UNO QUALSIASI dei tre lingotti di lega (OR). Gate: vedi gate_path().
        "requires_tier": "t8",
        "entry_name_en": "Prismatic Equipment", "entry_name_it": "Equipaggiamento prismatico",
        "page0_en": "Three metals, and I kept them apart as if they were rivals. Melted two by two they stop fighting: "
                    "now the light that hits them splits into *every* color at once.\\\n\\\n"
                    "*Every color, all of them in one place.*",
        "page0_it": "Tre metalli, e li ho tenuti separati come se fossero rivali. Fusi a due a due smettono di "
                    "combattersi: ora la luce che li colpisce si apre in *tutti* i colori insieme.\\\n\\\n"
                    "*Tutti i colori, in un posto solo.*",
    },
}

# ---------- Oggetti ----------
# kind: tipo di attrezzo o pezzo d'armatura (decide tag, modello e pattern). base: oggetto Spectrum da consumare
# nella ricetta (alla Fusion Shrine è il PRIMO ingrediente). fusion_extras (facoltativo): altri oggetti
# di Spectrum subito dopo la base. Se hanno un advancement di sblocco (EXTRA_UNLOCKS), l'advancement dell'oggetto lo
# richiede (checklist §A/§B).
# it_full (facoltativo): nome italiano completo quando l'accordo non segue "<nome> <tier>" (eccezioni all'accordo automatico).
# enchantments: quelli con cui esce l'oggetto. DEVONO coincidere con SAItems.java (lo verifica il GameTest).
#
# T6 (riscritto su indicazione dell'utente, 2026-11-18): TUTTO alla Fusion Shrine con Dragonrot. Ogni ricetta ha la
# base (bedrock o glass crest), gli eventuali accessori, `allthemodium` lingotti di allthemodium, `bedrock_dust`
# Shattered Bedrock (spectrum:bedrock_dust) e 32 polveri di topazio, ametista e citrino (FUSION["t6"]). Massimo 7
# ingredienti, liquido escluso (FusionShrineBlockEntity ha 7 slot): lo controllano gen_fusion_recipe e validate.py.
# copy_components su tutte e 15 (le basi bedrock conservano i loro incantesimi). Tempo e xp: FUSION_TIME_XP.
#
# Tempo (tick) e xp di TUTTE le ricette Fusion Shrine, per tier e classe (decisione dell'utente 2026-11-23):
# "pesanti" = workstaff, ultra greatsword, bident, balestra e mazza (dal 2026-11-25); "leggeri" = gli altri 11 pezzi.
FUSION_HEAVY_KINDS = {"workstaff", "ultra_greatsword", "bident", "crossbow", "mace"}
FUSION_TIME_XP = {  # tier: {classe: (tick, xp)}
    "t6": {"light": (1600, 24.0), "heavy": (2000, 48.0)},
    "t7": {"light": (1600, 24.0), "heavy": (2000, 48.0)},
    "t8": {"light": (2000, 48.0), "heavy": (2400, 64.0)},
    "t9": {"light": (2000, 48.0), "heavy": (2400, 64.0)},
}
OBJECTS = [
    # Workstaff: è il piccone di ogni tier (decisione di design). Upgrade della workstaff glass crest alla Fusion
    # Shrine con copy_components (vedi FUSION): conserva incantesimi, area di scavo e opzioni del menu.
    # Fortune è la selezione di default del menu, come la workstaff di Spectrum che esce con Fortune IV.
    {"tier": "t6", "kind": "workstaff", "machine": "fusion", "base": "spectrum:glass_crest_workstaff",
     "copy_components": True, "allthemodium": 6, "bedrock_dust": 6,
     "enchantments": {"minecraft:efficiency": 5, "minecraft:fortune": 6},
     "en": "Workstaff", "it": "Workstaff",
     "guide_en": "My Glass Crest Workstaff went into the fusion and came out as the pickaxe of this metal, "
                 "without losing anything I had given it. Every vein I open gives me more than before.",
     "guide_it": "La mia workstaff glass crest è entrata nella fusione ed è uscita come il piccone di questo metallo, "
                 "senza perdere niente di quello che le avevo dato. Ogni vena che apro mi rende più di prima."},
    # Oggetti speciali: upgrade alla Fusion Shrine delle versioni glass crest, come la workstaff.
    {"tier": "t6", "kind": "ultra_greatsword", "machine": "fusion", "base": "spectrum:glass_crest_ultra_greatsword",
     "copy_components": True, "allthemodium": 8, "bedrock_dust": 8,
     "enchantments": {"minecraft:sweeping_edge": 6},
     "en": "Ultra Greatsword", "it": "Ultra spadone",
     "guide_en": "The Glass Crest Ultra Greatsword kept its enchantments and gained reach: it strikes farther, "
                 "and when I slam it into the ground it throws back everything around me harder than before.",
     "guide_it": "Lo spadone glass crest ha tenuto i suoi incantesimi e ha guadagnato portata: colpisce più lontano, "
                 "e il suo schianto a terra respinge tutto quello che ho intorno più forte di prima."},
    {"tier": "t6", "kind": "crossbow", "machine": "fusion", "base": "spectrum:glass_crest_crossbow",
     "copy_components": True, "allthemodium": 6, "bedrock_dust": 6,
     "enchantments": {"minecraft:quick_charge": 3, "minecraft:piercing": 5},
     "en": "Crossbow", "it": "Balestra",
     "guide_en": "The Glass Crest Crossbow came out of the fusion more willing in my hands: it loads faster, "
                 "and its bolts fly faster and straighter.",
     "guide_it": "La balestra glass crest è uscita dalla fusione più docile fra le mie mani: si carica più in fretta, "
                 "e i dardi volano più veloci e più dritti."},
    # Bident: fonde i due glass crest (decisione di design). Il Ferocious è il primo ingrediente (copy_components copia
    # da lì), il Fractal il secondo (fusion_extras, letto da FusionShrineRecipeMixin). Modalità scelte dal menu (SABident, BidentMenu).
    {"tier": "t6", "kind": "bident", "machine": "fusion", "base": "spectrum:ferocious_glass_crest_bident",
     "fusion_extras": ["spectrum:fractal_glass_crest_bident"],
     "copy_components": True, "allthemodium": 8, "bedrock_dust": 8,
     "enchantments": {"minecraft:impaling": 6},
     "en": "Bident", "it": "Bidente",
     "guide_en": "I fused a Ferocious and a Fractal Bident into one, and now I am the one who chooses the power "
                 "I need in the moment.",
     "guide_it": "Ho fuso un bidente Ferocious e uno Fractal in uno solo, e ora scelgo io l'abilità che mi serve nel momento."},
    {"tier": "t6", "kind": "axe", "machine": "fusion", "base": "spectrum:bedrock_axe",
     "allthemodium": 6, "bedrock_dust": 6,
     "enchantments": {"minecraft:efficiency": 7},
     "en": "Axe", "it": "Ascia",
     "guide_en": "My Bedrock Axe, reborn. Trees and foes alike go down before I have finished the swing.",
     "guide_it": "La mia ascia di bedrock, rinata. Alberi e nemici vanno giù prima che io finisca il colpo."},
    {"tier": "t6", "kind": "shovel", "machine": "fusion", "base": "spectrum:bedrock_shovel",
     "allthemodium": 4, "bedrock_dust": 4,
     "enchantments": {"minecraft:efficiency": 7},
     "en": "Shovel", "it": "Pala",
     "guide_en": "I barely push it into the ground and whole mountains of earth are already gone. "
                 "I should be careful where I point it.",
     "guide_it": "La spingo appena nel terreno e intere montagne di terra sono già sparite. "
                 "Dovrei stare attento a dove la punto."},
    {"tier": "t6", "kind": "hoe", "machine": "fusion", "base": "spectrum:bedrock_hoe",
     "allthemodium": 4, "bedrock_dust": 4,
     "enchantments": {"minecraft:fortune": 6},
     "en": "Hoe", "it": "Zappa",
     "guide_en": "Every harvest I gather with it is richer than the last. I have started planting more just to see "
                 "how far it goes.",
     "guide_it": "Ogni raccolto che faccio con lei è più ricco del precedente. Ho iniziato a piantare di più solo "
                 "per vedere fin dove arriva."},
    {"tier": "t6", "kind": "sword", "machine": "fusion", "base": "spectrum:bedrock_sword",
     "allthemodium": 6, "bedrock_dust": 6,
     "enchantments": {"minecraft:sharpness": 7},
     "en": "Sword", "it": "Spada",
     "guide_en": "No whetstone could ever give it this edge, and it will never need one: it does not dull.",
     "guide_it": "Nessuna pietra per affilare potrebbe darle questo filo, e non ne avrà mai bisogno: non si smussa."},
    {"tier": "t6", "kind": "bow", "machine": "fusion", "base": "spectrum:bedrock_bow",
     "allthemodium": 6, "bedrock_dust": 6,
     "enchantments": {"minecraft:power": 7},
     "en": "Bow", "it": "Arco",
     "guide_en": "It keeps the steady aim and the long sight of the Bedrock Bow, but now its arrows strike "
                 "with a force that surprises even me.",
     "guide_it": "Tiene la mira salda e la vista lunga dell'arco di bedrock, ma ora le frecce colpiscono con una "
                 "forza che sorprende anche me."},
    {"tier": "t6", "kind": "fishing_rod", "machine": "fusion", "base": "spectrum:bedrock_fishing_rod",
     "allthemodium": 4, "bedrock_dust": 4,
     "enchantments": {"minecraft:luck_of_the_sea": 6, "minecraft:lure": 3},
     "en": "Fishing Rod", "it": "Canna da pesca",
     "guide_en": "Like the Bedrock one it fishes even in lava, and the sea has become far more generous with me.",
     "guide_it": "Come quella di bedrock pesca anche nella lava, e il mare è diventato molto più generoso con me."},
    {"tier": "t6", "kind": "shears", "machine": "fusion", "base": "spectrum:bedrock_shears",
     "allthemodium": 2, "bedrock_dust": 2,
     "enchantments": {"minecraft:efficiency": 7},
     "en": "Shears", "it": "Cesoie",
     "guide_en": "Wool, leaves, cobwebs: they cut through everything as if it were not there.",
     "guide_it": "Lana, foglie, ragnatele: tagliano tutto come se non fossero lì."},
    # Mazza (2026-11-25, 16° oggetto del tier): upgrade della NOSTRA mazza di bedrock (BEDROCK_MACE qui sotto: Spectrum
    # non ne ha una). Oggetto pesante: tempo/xp della classe "heavy" e quantità come lo spadone e il bident (8 + 8).
    # Wind Burst +1 a tier: bedrock IV, T6 V, T7 VI, T8 VII, T9 XI (tetto ATM10 VIII + 3). TESTI: BOZZE da far
    # rivedere all'utente (design/testi_mace.md).
    {"tier": "t6", "kind": "mace", "machine": "fusion", "base": "spectral_ascension:bedrock_mace",
     "copy_components": True, "allthemodium": 8, "bedrock_dust": 8,
     "enchantments": {"minecraft:wind_burst": 5},
     "en": "Mace", "it": "Mazza",
     "guide_en": "My Bedrock Mace drank in the three pigments too. It falls heavier than before, and the wind it "
                 "raises lifts me higher.",
     "guide_it": "Anche la mia mazza di bedrock ha assorbito i tre pigmenti. Cade più pesante di prima, e il vento "
                 "che solleva mi porta più in alto."},
    # Armatura: gli accessori Azure Dike della ricetta tornano come Dike del set completo (SAArmorSet).
    {"tier": "t6", "kind": "helmet", "machine": "fusion", "base": "spectrum:bedrock_helmet",
     "fusion_extras": ["spectrum:puff_circlet"], "allthemodium": 5, "bedrock_dust": 5,
     "enchantments": {"minecraft:projectile_protection": 6},
     "en": "Helmet", "it": "Elmo",
     "guide_en": "I set a Puff Circlet into the Bedrock Helmet. Together with the rest of the armor, the trinkets "
                 "inside seem to work again.",
     "guide_it": "Ho incastonato un Puff Circlet nell'elmo di bedrock. Sembra che insieme al resto dell'armatura "
                 "riprendano a funzionare."},
    {"tier": "t6", "kind": "chestplate", "machine": "fusion", "base": "spectrum:bedrock_chestplate",
     "fusion_extras": ["spectrum:azure_dike_ring"], "allthemodium": 8, "bedrock_dust": 8,
     "enchantments": {"minecraft:protection": 6},
     "en": "Chestplate", "it": "Corazza",
     "guide_en": "An Azure Dike Ring went into this one. It shields me from every kind of harm, and the bedrock "
                 "cape still falls from its shoulders. Together with the rest of the armor, the trinkets inside "
                 "seem to work again.",
     "guide_it": "In questa è finito un Azure Dike Ring. Mi protegge da ogni tipo di danno, e dalle spalle scende "
                 "ancora il mantello di bedrock. Sembra che insieme al resto dell'armatura riprendano a funzionare."},
    {"tier": "t6", "kind": "leggings", "machine": "fusion", "base": "spectrum:bedrock_leggings",
     "fusion_extras": ["spectrum:azure_dike_belt"], "allthemodium": 7, "bedrock_dust": 7,
     "enchantments": {"minecraft:blast_protection": 6},
     "en": "Leggings", "it": "Gambali",
     "guide_en": "With an Azure Dike Belt inside, explosions have lost almost all their bite against me.\\\n"
                 "Together with the rest of the armor, the trinkets inside seem to work again.",
     "guide_it": "Con dentro un Azure Dike Belt, le esplosioni contro di me hanno perso quasi tutta la loro forza.\\\n"
                 "Sembra che insieme al resto dell'armatura riprendano a funzionare."},
    # Stivali: niente Shattered Bedrock, al suo posto lo Shieldgrasp Amulet e l'Azuresque Dike Core (1 + 2 + 1 + 3 = 7).
    {"tier": "t6", "kind": "boots", "machine": "fusion", "base": "spectrum:bedrock_boots",
     "fusion_extras": ["spectrum:shieldgrasp_amulet", "spectrum:azuresque_dike_core"], "allthemodium": 4, "bedrock_dust": 0,
     "enchantments": {"minecraft:fire_protection": 6},
     "en": "Boots", "it": "Stivali",
     "guide_en": "A Shieldgrasp Amulet and an Azuresque Dike Core went into these. Fire and lava hardly frighten "
                 "me anymore.\\\nTogether with the rest of the armor, the trinkets inside seem to work again.",
     "guide_it": "Qui dentro sono finiti uno Shieldgrasp Amulet e un Azuresque Dike Core. Fuoco e lava non mi fanno "
                 "quasi più paura.\\\nSembra che insieme al resto dell'armatura riprendano a funzionare."},
]

# Prima della riscrittura del T6 (2026-11-18) il T7 e il T8 copiavano incantesimi dal T6: restano com'erano (vecchio tetto del T6).
# T7 (2026-11-19): TUTTI e 15 alla Fusion Shrine con Dragonrot (pezzo T6 + vibranium + polvere di onice).
# T8 (2026-11-19): idem, con pezzo T7 + unobtainium + polvere di moonstone (T8_SPEC).
# T9 (2026-11-19): idem, con pezzo T8 + un lingotto per lega + Colorful Shooting Star + Stardust (T9_SPEC). Gli
# incantesimi del T9 (T9_ENCHANTMENTS) restano congelati ai valori che aveva quando derivava dal vecchio T8.
# kind: (lingotti di vibranium, polvere di onice, incantesimi). Tempo e xp: FUSION_TIME_XP. Quantità decise dall'utente.
T7_SPEC = {
    # Workstaff: + Foundry dal T7 (menu nostro, SATools.Workstaff / WorkstaffOption; 2026-11-19).
    "workstaff": (8, 64, {"minecraft:fortune": 7, "minecraft:efficiency": 7, "spectrum:foundry": 1}),
    "axe": (8, 64, {"minecraft:efficiency": 8}),
    "shovel": (4, 32, {"minecraft:efficiency": 8}),
    "hoe": (4, 32, {"minecraft:fortune": 7}),
    "sword": (6, 48, {"minecraft:sharpness": 8}),
    "bow": (6, 48, {"minecraft:power": 8}),
    "fishing_rod": (4, 32, {"minecraft:luck_of_the_sea": 7, "minecraft:lure": 4}),
    "shears": (2, 16, {"minecraft:efficiency": 8}),
    "mace": (8, 64, {"minecraft:wind_burst": 6}),
    "ultra_greatsword": (8, 64, {"minecraft:sweeping_edge": 7}),
    "crossbow": (6, 48, {"minecraft:piercing": 6, "minecraft:quick_charge": 4}),
    "bident": (8, 64, {"minecraft:impaling": 7}),
    "helmet": (5, 32, {"minecraft:projectile_protection": 7}),
    "chestplate": (8, 64, {"minecraft:protection": 7}),
    "leggings": (7, 64, {"minecraft:blast_protection": 7}),
    "boots": (4, 32, {"minecraft:fire_protection": 7}),
}
# kind: (lingotti di unobtainium, polvere di moonstone, incantesimi). Tempo e xp: FUSION_TIME_XP. Quantità decise dall'utente.
T8_SPEC = {
    # Workstaff: Foundry + Inventory Insertion dal T8 (2026-11-19).
    "workstaff": (8, 64, {"minecraft:fortune": 8, "minecraft:efficiency": 8, "spectrum:foundry": 1,
                          "spectrum:inventory_insertion": 1}),
    "axe": (8, 64, {"minecraft:efficiency": 9}),
    "shovel": (4, 32, {"minecraft:efficiency": 9}),
    "hoe": (4, 32, {"minecraft:fortune": 8}),
    "sword": (6, 48, {"minecraft:sharpness": 9}),
    "bow": (6, 48, {"minecraft:power": 9}),
    "fishing_rod": (4, 32, {"minecraft:luck_of_the_sea": 8, "minecraft:lure": 5}),
    "shears": (2, 16, {"minecraft:efficiency": 9}),
    "mace": (8, 64, {"minecraft:wind_burst": 7}),
    "ultra_greatsword": (8, 64, {"minecraft:sweeping_edge": 8}),
    # Balestra: Quick Charge IV (con V la carica era istantanea) + Sniping I di Spectrum (2026-11-20).
    "crossbow": (6, 48, {"minecraft:piercing": 7, "minecraft:quick_charge": 4, "spectrum:sniping": 1}),
    "bident": (8, 64, {"minecraft:impaling": 8}),
    "helmet": (5, 32, {"minecraft:projectile_protection": 8}),
    "chestplate": (8, 64, {"minecraft:protection": 8}),
    "leggings": (7, 64, {"minecraft:blast_protection": 8}),
    "boots": (4, 32, {"minecraft:fire_protection": 8}),
}
# kind: (Colorful Shooting Star, Stardust). Tempo e xp: FUSION_TIME_XP. Quantità decise dall'utente; gli incantesimi sono T9_ENCHANTMENTS.
T9_SPEC = {
    "workstaff": (2, 32),
    "ultra_greatsword": (2, 32),
    "bident": (2, 32),
    "crossbow": (1, 24),
    "axe": (1, 32),
    "shovel": (1, 16),
    "hoe": (1, 16),
    "sword": (1, 24),
    "bow": (1, 24),
    "fishing_rod": (1, 16),
    "shears": (1, 8),
    "mace": (2, 32),
    "helmet": (1, 20),
    "chestplate": (2, 32),
    "leggings": (2, 28),
    "boots": (1, 16),
}
# Incantesimi del T9 (Alloy), CONGELATI: erano «T8 di prima + 3» (tetto ATM10 + 3). Il T9 non cambia col nuovo T8.
# DEVONO coincidere con SAItems.PRISMATIC_*.
T9_ENCHANTMENTS = {
    # Workstaff: + Foundry e Inventory Insertion come il T8 (2026-11-19; gli altri livelli restano congelati).
    "workstaff": {"minecraft:fortune": 11, "minecraft:efficiency": 12, "spectrum:foundry": 1,
                  "spectrum:inventory_insertion": 1},
    "ultra_greatsword": {"minecraft:sweeping_edge": 11},
    # Balestra: Quick Charge IV + Sniping II di Spectrum (2026-11-20).
    "crossbow": {"minecraft:piercing": 11, "minecraft:quick_charge": 4, "spectrum:sniping": 2},
    "bident": {"minecraft:impaling": 10},
    "axe": {"minecraft:efficiency": 12},
    "shovel": {"minecraft:efficiency": 12},
    "hoe": {"minecraft:fortune": 11},
    "sword": {"minecraft:sharpness": 12},
    "bow": {"minecraft:power": 12},
    "fishing_rod": {"minecraft:luck_of_the_sea": 11, "minecraft:lure": 5},
    "shears": {"minecraft:efficiency": 12},
    # Mazza (2026-11-25): Wind Burst XI = tetto ATM10 VIII + 3 (DEVE coincidere con SAItems.PRISMATIC_MACE).
    "mace": {"minecraft:wind_burst": 11},
    "helmet": {"minecraft:projectile_protection": 14},
    "chestplate": {"minecraft:protection": 11},
    "leggings": {"minecraft:blast_protection": 12},
    "boots": {"minecraft:fire_protection": 12},
}

# ---------- T7 Umbral ----------
# Un pezzo per ogni pezzo T6, un tier sopra: stessa macchina, con il pezzo T6 come base (primo
# ingrediente alla Fusion Shrine) e i lingotti di vibranium. Gli accessori Dike
# dell'armatura T6 non tornano: sono già nel pezzo T6. Alla Fusion Shrine copy_components porta con sé incantesimi, area di scavo e, per il
# bident, modalità e set di incantesimi (FusionShrineRecipeMixin non interviene: il primo ingrediente è già un bident
# nostro). Incantesimi = stesso tetto del T6 (massimo ATM10, decisione di design).
# TODO Alloy (Prismatic): quando si crea il tier, SATier.ALLOY ha già freeAoE = true (area della workstaff senza Ink,
# SATools.Workstaff.hasFreeAoE) e il bonus danno +25% per tier (vedi SATier): scrivere nel testo del libro dell'Alloy che
# l'area è gratis (chiave lang item.<mod>.workstaff.aoe_free già presente). T6, T7 e T8 restano a pagamento.
# Workstaff T7+: menu nostro (2026-11-19, ricerca/13) con Foundry (T7), area 7x7 a 9 White Ink per blocco (T7/T8),
# Inventory Insertion (T8) e area 9x9 gratis (Alloy). Lingua del menu in main() (gui.<mod>.workstaff_menu.*).
T7_GUIDE = {
    "workstaff": ("Now even stone seems to give way to it out of fear, and whatever it tears from the rock comes out still warm.",
                  "Ora anche la pietra sembra cederle per paura, e quello che strappa alla roccia ne esce ancora caldo."),
    "ultra_greatsword": ("Heavier and longer than the last one: it strikes farther, and when I slam it into the ground it hits harder still.",
                         "Più pesante e più lungo del precedente: colpisce più lontano, e il suo schianto a terra è ancora più forte."),
    "crossbow": ("It loads faster than ever, and its bolts fly faster and truer. I barely have to aim anymore.",
                 "Si carica più in fretta che mai, e i dardi volano più veloci e più precisi. Quasi non devo più prendere la mira."),
    "bident": ("It kept everything it was, and now its strikes slip deeper through armor and protection.",
               "Ha tenuto tutto quello che era, e ora i suoi colpi passano più a fondo armature e protezioni."),
    "axe": ("It falls like a shadow, and the tree is down before I hear it.",
            "Cade come un'ombra, e l'albero è a terra prima che io lo senta."),
    "shovel": ("The earth opens before it as if it remembered the dark below.",
               "La terra le si apre davanti come se ricordasse il buio di sotto."),
    "hoe": ("The soil it turns gives back more than I ever gave it.",
            "Il terreno che rivolta mi restituisce più di quanto gli abbia mai dato."),
    "sword": ("Its edge is so dark it seems to cut the light itself. I try not to stare at it.",
              "Il filo è così scuro che sembra tagliare la luce stessa. Cerco di non fissarlo troppo."),
    "bow": ("Its arrows leave the string like shadows, and hit harder than any before them.",
            "Le frecce lasciano la corda come ombre, e colpiscono più forte di tutte quelle di prima."),
    "fishing_rod": ("Water and lava give up their treasures to it, a little more willingly each time.",
                    "Acqua e lava le cedono i loro tesori, ogni volta un po' più volentieri."),
    "shears": ("They close without a sound, and the cut is already made.",
               "Si chiudono senza un suono, e il taglio è già fatto."),
    "mace": ("It falls from the sky like a shadow, and the gust that follows carries me even higher.",
             "Cade dal cielo come un'ombra, e la folata che segue mi porta ancora più in alto."),
    "helmet": ("Arrows and projectiles slide off it like rain off onyx.",
               "Frecce e proiettili ci scivolano sopra come pioggia sull'onice."),
    "chestplate": ("Heavier than it looks, sturdier than before, and the cape still flows from its shoulders.",
                   "Più pesante di quanto sembri, più robusta di prima, e dalle spalle scende ancora il mantello."),
    "leggings": ("The blast of an explosion barely reaches me now.",
                 "Lo scoppio di un'esplosione ormai mi arriva appena."),
    "boots": ("Fire and lava frighten me even less than before.",
              "Fuoco e lava mi fanno ancora meno paura di prima."),
}


def _t7_row(t6):
    row = {"tier": "t7", "kind": t6["kind"], "base": f"spectral_ascension:{TIERS['t6']['prefix']}_{t6['kind']}",
           "enchantments": dict(T7_SPEC[t6["kind"]][2]), "en": t6["en"], "it": t6["it"],
           "guide_en": T7_GUIDE[t6["kind"]][0], "guide_it": T7_GUIDE[t6["kind"]][1],
           "machine": "fusion", "copy_components": True, "vibranium": T7_SPEC[t6["kind"]][0],
           "onyx": T7_SPEC[t6["kind"]][1]}
    return row


OBJECTS += [_t7_row(o) for o in list(OBJECTS) if o["tier"] == "t6"]

# ---------- T8 Argent ----------
# Come il T7, un tier sopra: un pezzo per ogni pezzo T7, TUTTI alla Fusion Shrine con Dragonrot (dal 2026-11-19), con
# il pezzo T7 come base (primo ingrediente), lingotti di unobtainium e polvere di moonstone (T8_SPEC, decisioni dell'utente).
T8_GUIDE = {
    "workstaff": ("Stone parts before it like mist in moonlight, and what it digs never even touches the ground: "
                  "I find it already in my pack.",
                  "La pietra le si apre davanti come nebbia al chiaro di luna, e quello che scava non tocca nemmeno terra: "
                  "me lo ritrovo già nello zaino."),
    "ultra_greatsword": ("Its reach grows longer still, and when I slam it into the ground it hits harder than ever.",
                         "La sua portata si allunga ancora, e il suo schianto a terra è più forte che mai."),
    "crossbow": ("It loads faster still, and now its bolts leave so fast they seem to arrive before the sound.",
                 "Si carica ancora più in fretta, e ora i dardi partono così veloci che sembrano arrivare prima del suono."),
    "bident": ("It forgot nothing of what it was, and its strikes pierce armor and protection deeper than before.",
               "Non ha dimenticato niente di quello che era, e i suoi colpi passano armature e protezioni ancora più a fondo."),
    "axe": ("A pale flash, and the tree is already down.",
            "Un lampo pallido, e l'albero è già a terra."),
    "shovel": ("The earth opens before it as if the moon had asked.",
               "La terra le si apre davanti come se gliel'avesse chiesto la luna."),
    "hoe": ("Whatever grows where it passes seems to reach toward the light.",
            "Ciò che cresce dove passa sembra allungarsi verso la luce."),
    "sword": ("Its edge gleams like moonlight on still water, and cuts just as cleanly.",
              "Il filo brilla come la luna sull'acqua ferma, e taglia altrettanto pulito."),
    "bow": ("Its arrows fly like shards of moonlight, and hit harder than ever.",
            "Le frecce volano come schegge di luna, e colpiscono più forte che mai."),
    "fishing_rod": ("Water and lava give up their treasures to it even more willingly, and the bites come sooner.",
                    "Acqua e lava le cedono i loro tesori ancora più volentieri, e i pesci abboccano prima."),
    "shears": ("They close with a glint, and the cut is already made.",
               "Si chiudono con un riflesso, e il taglio è già fatto."),
    "mace": ("A pale flash from above, and the wind throws me back up toward the moon.",
             "Un lampo pallido dall'alto, e il vento mi rilancia su verso la luna."),
    "helmet": ("Arrows and projectiles glance off it like light off Moonstone.",
               "Frecce e proiettili ci rimbalzano sopra come la luce sulla moonstone."),
    "chestplate": ("Lighter than it looks and tougher than before; the cape still flows from its shoulders.",
                   "Più leggera di quanto sembri e più resistente di prima; dalle spalle scende ancora il mantello."),
    "leggings": ("An explosion right beside me now sounds like distant thunder.",
                 "Un'esplosione accanto a me ora sembra un tuono lontano."),
    "boots": ("I walk through fire as if it were moonlight.",
              "Cammino nel fuoco come se fosse chiaro di luna."),
}


def _t8_row(t7):
    row = {"tier": "t8", "kind": t7["kind"], "base": f"spectral_ascension:{TIERS['t7']['prefix']}_{t7['kind']}",
           "enchantments": dict(T8_SPEC[t7["kind"]][2]), "en": t7["en"], "it": t7["it"],
           "guide_en": T8_GUIDE[t7["kind"]][0], "guide_it": T8_GUIDE[t7["kind"]][1],
           "machine": "fusion", "copy_components": True, "unobtainium": T8_SPEC[t7["kind"]][0],
           "moonstone": T8_SPEC[t7["kind"]][1]}
    return row


OBJECTS += [_t8_row(o) for o in list(OBJECTS) if o["tier"] == "t7"]

# ---------- T9 Alloy «Prismatic» ----------
# Come il T8, un tier sopra: un pezzo per ogni pezzo T8, TUTTI alla Fusion Shrine con Dragonrot (dal 2026-11-19), con
# il pezzo T8 come base (primo ingrediente), 1 lingotto per ciascuna delle 3 leghe (TIERS["t9"]["alloys"]), Colorful
# Shooting Star e Stardust (T9_SPEC, decisioni dell'utente). Niente polveri di gemma.
# Incantesimi = tetto ATM10 + 3 (ricerca/README.md «Tetto incantesimi»): quelli del vecchio T8 + 3 livelli, congelati in
# T9_ENCHANTMENTS (il T9 non dipende dal T8 attuale) (DEVONO coincidere
# con SAItems.PRISMATIC_*). Area della workstaff gratis (SATier.ALLOY.freeAoE).
# Menu della workstaff (9x9 gratis): fatto il 2026-11-19.
T9_GUIDE = {
    "workstaff": ("This version I discovered seems to need no White Ink at all: the alloys carry the light themselves.",
                  "Questa versione che ho scoperto sembra non usare più Ink bianco: la luce la portano le leghe stesse."),
    "ultra_greatsword": ("My experiments led me to this longer, heavier version that is remarkably powerful.",
                         "I miei esperimenti mi hanno portato a questa versione più lunga e pesante che è particolarmente potente."),
    "crossbow": ("These experiments help me a great deal: this new crossbow loads in a heartbeat and its bolts fly so "
                 "close to the target that it takes real effort to miss.",
                 "Questi esperimenti mi aiutano molto, questa nuova balestra si carica in un battito e i dardi volano "
                 "molto più vicini al bersaglio, ora ci vuole impegno a mancarlo."),
    "bident": ("This improvement furthers its ability to slip past the enemy's armor and protection. Very little can stop it now.",
               "Questo miglioramento che ho trovato migliora ulteriormente le sue proprietà che gli permettono di ignorare "
               "parte delle protezioni del nemico."),
    "axe": ("This new axe fells trees in an instant; I marvel at its speed.",
            "Questa nuova ascia rimuove alberi in un istante, mi meraviglio della sua velocità."),
    "shovel": ("This improvement brought such speed that I seem to see rainbows when I use it. Maybe I should get myself checked…",
               "Questo miglioramento ha portato ad una velocità tale che mi sembra di vedere arcobaleni quando la uso, "
               "forse dovrei farmi controllare..."),
    "hoe": ("Since I started using this hoe, it seems that whatever I plant takes on every color.",
            "Da quando utilizzo questa zappa mi sembra che i risultati di ciò che pianto assumano ogni colore."),
    "sword": ("This sword is far too light to be called one, yet it seems to have a devastating effect and shows no sign "
              "of breaking despite how thin it is.",
              "Questa spada è fin troppo leggera per esserne considerata una, eppure sembra avere un effetto devastante e "
              "che non si rompa nonostante il suo spessore."),
    "bow": ("The arrows I shoot from this bow strike with a force and precision I did not think possible, and now they seem to leave a trail of shining colors behind them.",
            "Le frecce che lancio da questo arco colpiscono con una forza e precisione che non credevo possibili, e ora sembrano lasciare una fila di colori splendenti."),
    "fishing_rod": ("The new colors of the hook seem to call everything that lives, and doesn't, in water and lava.",
                    "Questi nuovi colori dell'amo sembrano chiamare tutto ciò che vive, e non, nell'acqua e lava."),
    "shears": ("They close in a shimmer, and the cut is already made.",
               "Si chiudono in un luccichio, e il taglio è già fatto."),
    "mace": ("These alloys make it light in my hand and terribly heavy when it lands. The wind it raises now throws "
             "me higher than I would ever dare to jump.",
             "Queste leghe la rendono leggera in mano e pesantissima quando colpisce. Il vento che solleva ora mi "
             "lancia più in alto di quanto oserei mai saltare."),
    "helmet": ("Arrows and projectiles scatter off it like light through a prism.",
               "Frecce e proiettili ci si sparpagliano sopra come la luce in un prisma."),
    "chestplate": ("Lighter than it looks and tougher than ever; these materials work wonders.",
                   "Più leggera di quanto sembri e più resistente di prima, questi materiali fanno miracoli."),
    "leggings": ("My leggings now keep me from even realizing the danger of an explosion.",
                 "I miei gambali ora non mi fanno neanche realizzare il pericolo di un'esplosione."),
    "boots": ("Fire and lava? You mean rain and water — this improvement makes me ignore them as if they were nothing.",
              "Fuoco e lava? Volevi dire pioggia e acqua, questo miglioramento me li fa ignorare come fossero nulla."),
}


def _t9_row(t8):
    row = {"tier": "t9", "kind": t8["kind"], "base": f"spectral_ascension:{TIERS['t8']['prefix']}_{t8['kind']}",
           "enchantments": dict(T9_ENCHANTMENTS[t8["kind"]]),
           "en": t8["en"], "it": t8["it"],
           "guide_en": T9_GUIDE[t8["kind"]][0], "guide_it": T9_GUIDE[t8["kind"]][1],
           "machine": "fusion", "copy_components": True, "stars": T9_SPEC[t8["kind"]][0],
           "stardust": T9_SPEC[t8["kind"]][1]}
    return row


OBJECTS += [_t9_row(o) for o in list(OBJECTS) if o["tier"] == "t8"]

# Advancement di sblocco degli oggetti di Spectrum usati come base o fusion_extras (id verificati nel jar e in
# SpectrumAdvancements.java). L'advancement dell'oggetto li richiede tutti, oltre al tier.
EXTRA_UNLOCKS = {
    # Sblocca la ricetta della workstaff glass crest (richiede a sua volta unlocks/malachite/glass_crest_tools).
    "spectrum:glass_crest_workstaff": "spectrum:unlocks/malachite/glass_crest_workstaff",
    "spectrum:glass_crest_ultra_greatsword": "spectrum:unlocks/malachite/glass_crest_ultra_greatsword",
    "spectrum:glass_crest_crossbow": "spectrum:unlocks/malachite/glass_crest_crossbow",
    "spectrum:ferocious_glass_crest_bident": "spectrum:unlocks/malachite/ferocious_glass_crest_bident",
    "spectrum:fractal_glass_crest_bident": "spectrum:unlocks/malachite/fractal_glass_crest_bident",
    "spectrum:puff_circlet": "spectrum:unlocks/trinkets/puff_circlet",
    "spectrum:azure_dike_ring": "spectrum:unlocks/trinkets/azure_dike_ring",
    "spectrum:shieldgrasp_amulet": "spectrum:unlocks/trinkets/shieldgrasp_amulet",
    "spectrum:azure_dike_belt": "spectrum:unlocks/trinkets/azure_dike_belt",
    "spectrum:azuresque_dike_core": "spectrum:unlocks/trinkets/azuresque_dike_core",
    # Tutte le ricette Fusion Shrine bedrock di Spectrum (attrezzi e armatura) richiedono questo advancement
    # (verificato in jar:data/spectrum/recipe/fusion_shrine/bedrock/*.json).
    **{f"spectrum:bedrock_{kind}": "spectrum:unlocks/equipment/bedrock_tools"
       for kind in ("axe", "shovel", "hoe", "sword", "bow", "fishing_rod", "shears",
                    "helmet", "chestplate", "leggings", "boots")},
    # Mazza T6: la base è la NOSTRA mazza di bedrock, che si sblocca come gli attrezzi bedrock di Spectrum (BEDROCK_MACE).
    f"{MOD}:bedrock_mace": "spectrum:unlocks/equipment/bedrock_tools",
    # T7: ogni pezzo richiede lo sblocco del pezzo T6 da cui parte (stesso advancement di ricetta e pagina T6).
    **{f"{MOD}:{TIERS['t6']['prefix']}_{o['kind']}": f"{MOD}:unlocks/equipment/t6/{TIERS['t6']['prefix']}_{o['kind']}"
       for o in OBJECTS if o["tier"] == "t6"},
    # T8: lo stesso col pezzo T7 da cui parte.
    **{f"{MOD}:{TIERS['t7']['prefix']}_{o['kind']}": f"{MOD}:unlocks/equipment/t7/{TIERS['t7']['prefix']}_{o['kind']}"
       for o in OBJECTS if o["tier"] == "t7"},
    # T9 (Alloy): lo stesso col pezzo T8 da cui parte.
    **{f"{MOD}:{TIERS['t8']['prefix']}_{o['kind']}": f"{MOD}:unlocks/equipment/t8/{TIERS['t8']['prefix']}_{o['kind']}"
       for o in OBJECTS if o["tier"] == "t8"},
}
# Genere/numero grammaticale del nome italiano di ogni tipo (per accordare l'aggettivo del tier).
# "workstaff": femminile ("la workstaff", come la staff).
IT_GENDER = {"workstaff": "f", "ultra_greatsword": "m", "crossbow": "f", "bident": "m", "axe": "f", "shovel": "f", "hoe": "f", "sword": "f", "bow": "m", "fishing_rod": "f",
             "shears": "fp", "mace": "f", "helmet": "m", "chestplate": "f", "leggings": "mp", "boots": "mp"}
ARMOR_KINDS = ("helmet", "chestplate", "leggings", "boots")
# Pagina di testo dopo i pezzi d'armatura: solo per i tier che la possiedono (T6 e T9).
ARMOR_SET_PAGES = {
    "t6": {
        "title_en": "The Full Set", "title_it": "Il set completo",
        "text_en": "The Dike trinkets I fused into the armor did not vanish: wearing all four pieces surrounds me with an "
                   "**Azure Dike** as strong as all of them together, and it recharges just as quickly.\\\n\\\n"
                   "But once the full set is on, the Azure Dike trinkets in my trinket slots stop working: their power "
                   "already lives in the armor.",
        "text_it": "Gli accessori Dike che ho fuso nell'armatura non sono spariti: indossando tutti e quattro i pezzi mi "
                   "avvolge un **Azure Dike** forte quanto tutti loro insieme, che si ricarica altrettanto in fretta.\\\n\\\n"
                   "Però, con il set completo addosso, gli accessori Azure Dike negli slot degli accessori smettono di "
                   "funzionare: il loro potere vive già nell'armatura.",
    },
    "t9": {
        "title_en": "The Full Set", "title_it": "Il set completo",
        "text_en": "Wearing all four pieces seems to keep the **Azure Dike** properties I used in the original creation.\\\n\\\n"
                   "However, once the full set is on, the Dike trinkets seem to stop working.",
        "text_it": "Indossare tutti e quattro i pezzi sembra che continui a mantenere le proprietà dell'**Azure Dike** che ho "
                   "usato nella creazione iniziale.\\\n\\\n"
                   "Tuttavia sembra che una volta indossati gli accessori di Dike non facciano più il loro effetto.",
    },
}


# ---------- Mazza di bedrock (2026-11-25) ----------
# Spectrum non ha una mazza di bedrock: la facciamo noi, nello stile dei suoi attrezzi bedrock (SAItems.BEDROCK_MACE:
# indistruttibile, UNCOMMON, resistente al fuoco, fuori da minecraft:enchantable/durability) con Wind Burst IV.
# Ricetta COPIATA da jar:data/spectrum/recipe/fusion_shrine/bedrock/sword.json (stesso liquido, tempo, xp e materiali;
# base minecraft:mace al posto della spada d'oro), più disable_yield_upgrades (checklist D2, obbligatorio con
# copy_components). Sblocco: quello degli attrezzi bedrock di Spectrum, e basta (decisione dell'utente 2026-11-25: la
# mazza bedrock è un attrezzo bedrock come gli altri e la ricetta li segue; niente advancement nostro, né Pedestal
# Moonstone). Lo stesso advancement per ricetta e pagina del libro (checklist §A), come per la spada bedrock di Spectrum.
# TESTI: BOZZE (design/testi_mace.md).
BEDROCK_MACE = {
    "id": "bedrock_mace",
    "unlock": "spectrum:unlocks/equipment/bedrock_tools",
    "recipe": "fusion_shrine/bedrock/bedrock_mace",
    "group": f"{MOD}_bedrock_tools",
    "fluid": "spectrum:midnight_solution", "time": 1200, "experience": 16.0,
    "ingredients": ["minecraft:mace",
                    {"item": "spectrum:bedrock_dust", "count": 2},
                    {"item": "spectrum:black_pigment", "count": 2},
                    {"item": "spectrum:onyx_powder", "count": 2},
                    {"item": "spectrum:stratine_gem", "count": 1},
                    {"item": "spectrum:stratine_fragments", "count": 2}],
    "enchantments": {"minecraft:wind_burst": 4},
    # Colore d'inchiostro come gli attrezzi bedrock di Spectrum (jar:data/spectrum/ink_color_mapping/item/spectrum.json).
    "ink": "spectrum:black",
    # La mazza vanilla (base della ricetta) non ha colore in Spectrum: grigio come il nucleo pesante e la Breeze Head
    # (spectrum:breeze_head è grigia in jar:data/spectrum/ink_color_mapping/item/spectrum.json). Toglie l'avviso
    # [SANITY: Fusion Shrine Recipe] e dà gli effetti colorati alla Fusion Shrine.
    "base_ink": ("spectrum:gray", "minecraft:mace"),
    # Texture disegnata dall'utente e approvata il 2026-11-26 (proposta in design/texture/proposte/mace_bedrock/).
    "texture": "spectral_ascension:item/bedrock_mace",
    # Voce del libro accanto agli attrezzi bedrock di Spectrum (1,1), a sinistra della colonna 0 (posto libero).
    "entry": {"x": -1, "y": 2, "parent": "spectrum:equipment/tools_and_armor/bedrock_tools"},
    "name_en": "Bedrock Mace", "name_it": "Mazza di bedrock",
    "group_en": "Bedrock Tools", "group_it": "Attrezzi di bedrock",
    "page0_en": "My mace hits hard when I drop on something from high above, but every blow wears it down a little. "
                "So I tried what had already worked for my other tools, and the bedrock took to it without "
                "complaint.\\\n\\\n*Heavier than the sky, and it will never break.*",
    "page0_it": "La mia mazza colpisce forte quando piombo su qualcosa dall'alto, ma ogni colpo la consuma un po'. "
                "Così ho provato quello che aveva già funzionato con gli altri miei attrezzi, e la bedrock l'ha "
                "accolta senza fare storie.\\\n\\\n*Più pesante del cielo, e non si romperà mai.*",
    "page1_en": "Now every blow from above ends in a gust of wind that throws me back up toward the sky.",
    "page1_it": "Ora ogni colpo dall'alto finisce in una folata di vento che mi rilancia verso il cielo.",
}


def gen_bedrock_mace(lang):
    """Mazza di bedrock: ricetta, modello, traduzioni e voce del libro (BEDROCK_MACE)."""
    m = BEDROCK_MACE
    write_json(f"data/{MOD}/recipe/{m['recipe']}.json", {
        "type": "spectrum:fusion_shrine",
        "group": m["group"],
        "time": m["time"],
        "experience": m["experience"],
        "fluid": {"fluid": m["fluid"]},
        "copy_components": True,
        "disable_yield_upgrades": True,
        "ingredients": m["ingredients"],
        "result": {"id": f"{MOD}:{m['id']}", "count": 1,
                   "components": {"minecraft:enchantments": {"levels": m["enchantments"]}}},
        "required_advancement": m["unlock"],
    })
    write_json(f"assets/{MOD}/models/item/{m['id']}.json",
               {"parent": MODEL_PARENT["mace"], "textures": {"layer0": m["texture"]}})
    key = f"book.{MOD}.guidebook.{m['id']}"
    for code, sfx in (("en_us", "en"), ("it_it", "it")):
        lang[code][f"item.{MOD}.{m['id']}"] = m["name_" + sfx]
        lang[code][f"{key}.name"] = m["name_" + sfx]
        lang[code][f"{key}.page0.text"] = m["page0_" + sfx]
        lang[code][f"{key}.page1.text"] = m["page1_" + sfx]
        lang[code][f"recipeGroup.spectrum.{m['group']}"] = m["group_" + sfx]
    write_json(f"data/spectrum/modonomicon/books/guidebook/entries/equipment/{MOD}_{m['id']}.json", {
        "name": f"{key}.name",
        "description": f"book.{MOD}.added_by",
        "condition": {"type": "modonomicon:advancement", "advancement_id": m["unlock"]},
        "icon": {"item": f"{MOD}:{m['id']}"},
        "category": "spectrum:equipment",
        "hide_while_locked": True,
        "parents": [{"entry": m["entry"]["parent"]}],
        "background_u_index": 0,
        "background_v_index": 0,
        "x": m["entry"]["x"],
        "y": m["entry"]["y"],
        "pages": [
            {"type": "modonomicon:text", "title": f"{key}.name", "text": f"{key}.page0.text"},
            {"type": "spectrum:fusion_shrine_crafting", "title": f"item.{MOD}.{m['id']}",
             "recipe_id": f"{MOD}:{m['recipe']}",
             "condition": {"type": "modonomicon:advancement", "advancement_id": m["unlock"]},
             "text": f"{key}.page1.text"},
        ],
    })


def category(obj):
    """Gruppo della ricetta (e del suo nome tradotto): attrezzi o armatura."""
    return "armor" if obj["kind"] in ARMOR_KINDS else "tools"

# Ricette Fusion Shrine (oggetti con "machine": "fusion"): upgrade di un oggetto di Spectrum che resta il PRIMO
# ingrediente, da cui copy_components copia incantesimi e componenti (checklist D2). Valori PROVVISORI, ispirati a
# jar:data/spectrum/recipe/fusion_shrine/{bedrock,malachite}/*.json (1200 tick, 16 xp, max 7 ingredienti).
# Polveri di gemma = tema del tier (T6: ciano, magenta, giallo, i tre pigmenti).
FUSION = {
    # T6 (2026-11-18, indicazioni dell'utente): Dragonrot; lingotti di allthemodium e Shattered Bedrock in quantità
    # per oggetto (campi "allthemodium" e "bedrock_dust" della riga, vedi t6_fusion_ingredients), poi 32 polveri di
    # ogni pigmento. Tempo e xp: FUSION_TIME_XP.
    "t6": {
        "fluid": "spectrum:dragonrot",
        "ingredients": [
            {"item": "spectrum:amethyst_powder", "count": 32},
            {"item": "spectrum:citrine_powder", "count": 32},
            {"item": "spectrum:topaz_powder", "count": 32},
        ],
        # Colori d'inchiostro per gli effetti della Fusion Shrine (altrimenti /spectrum sanity li segnala).
        "ink_colors": {"spectrum:orange": ["allthemodium:allthemodium_ingot", "allthemodium:allthemodium_block"]},
    },
    # T7 (2026-11-19, indicazioni dell'utente): Dragonrot; ingredienti = pezzo T6 (primo), lingotti di vibranium e polvere
    # di onice, quantità per oggetto (campi "vibranium" e "onyx" della riga, vedi t7_fusion_ingredients).
    "t7": {
        "fluid": "spectrum:dragonrot",
        "ink_colors": {"spectrum:cyan": ["allthemodium:vibranium_ingot", "allthemodium:vibranium_block"]},
    },
    # T8 (2026-11-19, indicazioni dell'utente): Dragonrot; ingredienti = pezzo T7 (primo), lingotti di unobtainium e polvere
    # di moonstone, quantità per oggetto (campi "unobtainium" e "moonstone" della riga, vedi t8_fusion_ingredients).
    "t8": {
        "fluid": "spectrum:dragonrot",
        # Unobtainium viola (come il suo colore in ATM).
        "ink_colors": {"spectrum:purple": ["allthemodium:unobtainium_ingot", "allthemodium:unobtainium_block"]},
    },
    # T9 Alloy (2026-11-19, indicazioni dell'utente): Dragonrot; ingredienti = pezzo T8 (primo), 1 lingotto per ciascuna
    # delle 3 leghe, Colorful Shooting Star e Stardust, quantità per oggetto (campi "stars" e "stardust" della riga, vedi
    # t9_fusion_ingredients): 1 + 3 + 1 + 1 = 6 slot su 7, 7 con 2 stelle (una per slot, si impilano a 1).
    "t9": {
        "fluid": "spectrum:dragonrot",
        # Colori delle leghe (ricerca/14 §6): VA verde-acqua → ciano, UA viola/magenta → magenta, UV viola → viola.
        "ink_colors": {"spectrum:cyan": ["allthemodium:vibranium_allthemodium_alloy_ingot"],
                       "spectrum:magenta": ["allthemodium:unobtainium_allthemodium_alloy_ingot"],
                       "spectrum:purple": ["allthemodium:unobtainium_vibranium_alloy_ingot"]},
    },
}
# Colore d'inchiostro dei nostri oggetti fatti alla Fusion Shrine, per tier: T6 arancio come le versioni glass crest
# (jar:data/spectrum/ink_color_mapping/item/spectrum.json), T7 nero come il suo tema (onice), T8 bianco (moonstone).
# T9 (Alloy) magenta, PROVVISORIO: il tema «prismatico» non ha un colore solo; magenta come l'unobtainium delle leghe.
FUSION_INK = {"t6": "spectrum:orange", "t7": "spectrum:black", "t8": "spectrum:white", "t9": "spectrum:magenta"}

# Tag di tipo (decidono quali incantesimi accetta e come lo vedono le altre mod): gli stessi delle
# versioni bedrock di Spectrum (jar: data/*/tags/item/...).
KIND_TAGS = {
    # Come glass_crest_workstaff: spectrum:pickaxes/axes/hoes finiscono in minecraft:* e c:tools/mining_tool,
    # spectrum:workstaffs in c:tools/knife e altri. Escluso spectrum:glass_crest_equipment (raggruppa il materiale
    # glass crest e fa scattare l'advancement lategame/create_glass_crest_tool).
    "workstaff": ["spectrum:workstaffs", "spectrum:pickaxes", "spectrum:axes", "spectrum:hoes"],
    # Come le versioni glass crest (escluso spectrum:glass_crest_equipment). spectrum:greatswords → spectrum:swords →
    # minecraft:swords; spectrum:crossbows e spectrum:bidents finiscono in c:tools/* da soli.
    "ultra_greatsword": ["spectrum:greatswords"],
    "crossbow": ["spectrum:crossbows", "minecraft:enchantable/crossbow"],
    # I bident glass crest stanno anche in spectrum:shovels. #spectrum:bidents è in minecraft:enchantable/durability:
    # da lì ci toglie il "remove" di gen_tags.
    "bident": ["spectrum:bidents", "spectrum:shovels", "minecraft:enchantable/trident"],
    "axe": ["minecraft:axes"],
    "shovel": ["minecraft:shovels"],
    "hoe": ["minecraft:hoes"],
    "sword": ["minecraft:swords", "spectrum:swords"],
    "bow": ["minecraft:enchantable/bow", "c:tools/bow", "c:tools/ranged_weapon"],
    "fishing_rod": ["minecraft:enchantable/fishing", "spectrum:fishing_rods"],
    "shears": ["minecraft:enchantable/mining", "c:tools/shear"],
    # Mazza: come minecraft:mace (Wind Burst, Density, Breach; Smite e Bane; Fire Aspect) e c:tools/mace di NeoForge.
    # Niente minecraft:enchantable/durability (indistruttibile: lo toglie gen_tags, come per gli altri).
    "mace": ["minecraft:enchantable/mace", "minecraft:enchantable/weapon", "minecraft:enchantable/fire_aspect",
             "c:tools/mace"],
    # Come bedrock_helmet ecc. (esclusi i tag spectrum:bedrock_*). Da trimmable_armor Spectrum li toglie: vedi REMOVE_TAGS.
    "helmet": ["minecraft:head_armor"],
    "chestplate": ["minecraft:chest_armor"],
    "leggings": ["minecraft:leg_armor"],
    "boots": ["minecraft:foot_armor"],
}
# Oggetti "oversized" come quelli malachite/glass crest: texture 16×16 nell'inventario e 32×32 (<id>_oversized) in
# mano, scelta dall'override sul predicato "oversized" (registrato in SAClient). Valore = texture di Spectrum di partenza.
OVERSIZED = {"workstaff": "glass_crest_workstaff", "ultra_greatsword": "glass_crest_ultra_greatsword"}
# Oggetti con modelli propri (copiati da quelli glass crest di Spectrum): kind → funzione(iid) che restituisce
# (modello base, {suffisso: modello}, {suffisso texture: texture di Spectrum di partenza}).
CROSSBOW_STATES = [  # stato → predicati, nell'ordine di jar:assets/spectrum/models/item/glass_crest_crossbow.json
    ("pulling_0", {"pulling": 1}), ("pulling_1", {"pulling": 1, "pull": 0.58}), ("pulling_2", {"pulling": 1, "pull": 1.0}),
    ("arrow", {"charged": 1}), ("firework", {"charged": 1, "projectile": 0.1}), ("glass", {"charged": 1, "projectile": 0.2}),
    ("topaz", {"charged": 1, "projectile": 0.3}), ("amethyst", {"charged": 1, "projectile": 0.4}),
    ("citrine", {"charged": 1, "projectile": 0.5}), ("onyx", {"charged": 1, "projectile": 0.6}),
    ("moonstone", {"charged": 1, "projectile": 0.7}),
]


def crossbow_models(iid):
    """Balestra: ogni stato ha un modello 16×16 e uno _oversized 32×32; il modello base è lo stato standby.
    Predicati registrati in SAClient.registerCrossbow (+ oversized)."""
    parent = "spectrum:templates_item/crossbow_oversized"
    src = "glass_crest_crossbow"
    overrides = [{"predicate": {"oversized": 1.0}, "model": f"{MOD}:item/{iid}_standby_oversized"}]
    models = {"_standby_oversized": {"parent": parent, "textures": {"layer0": f"{MOD}:item/{iid}_standby_oversized"}}}
    textures = {"_standby": f"{src}_standby", "_standby_oversized": f"{src}_standby_oversized"}
    for state, pred in CROSSBOW_STATES:
        for suffix, extra in ((f"_{state}", {}), (f"_{state}_oversized", {"oversized": 1.0})):
            overrides.append({"predicate": {**extra, **pred}, "model": f"{MOD}:item/{iid}{suffix}"})
            models[suffix] = {"parent": parent, "textures": {"layer0": f"{MOD}:item/{iid}{suffix}"}}
            textures[suffix] = src + suffix
        # In mano il modello si sceglie DUE volte: ItemRenderer.getModel risolve gli override col seed normale
        # (oversized = 0), poi ItemRendererMixin di Spectrum risolve di nuovo, col seed oversized, gli override del
        # modello GIÀ scelto. Per gli stati diversi da standby il primo passo sceglie il modello 16×16 dello stato
        # (es. _arrow), che senza override propri restava quello: in prima persona, carica o in tensione, la balestra
        # mostrava la texture 16×16 con la posa e la scala 1,25 pensate per la 32×32, cioè pixel doppi e una sagoma
        # enorme. Con questo override il secondo passo arriva alla variante _oversized. (Spectrum ha lo stesso
        # difetto sulla glass crest: i suoi modelli di stato non hanno override.)
        models[f"_{state}"]["overrides"] = [{"predicate": {"oversized": 1.0},
                                             "model": f"{MOD}:item/{iid}_{state}_oversized"}]
    base = {"parent": parent, "textures": {"layer0": f"{MOD}:item/{iid}_standby"}, "overrides": overrides}
    return base, models, textures


def bident_models(iid):
    """Bident: 16×16 nell'inventario, _oversized in mano e lanciato (BidentEntityRenderer usa il seed oversized),
    _oversized_throwing mentre si carica il lancio (predicato bident_throwing 0,5, in SAClient). Texture dal Ferocious."""
    src = "ferocious_glass_crest_bident"
    base = {"parent": "spectrum:templates_item/handheld_oversized", "textures": {"layer0": f"{MOD}:item/{iid}"},
            "overrides": [
                {"predicate": {"oversized": 1.0, "bident_throwing": 0.0}, "model": f"{MOD}:item/{iid}_oversized"},
                {"predicate": {"oversized": 1.0, "bident_throwing": 0.5}, "model": f"{MOD}:item/{iid}_oversized_throwing"}]}
    models = {
        "_oversized": {"parent": "spectrum:templates_item/handheld_oversized",
                       "textures": {"layer0": f"{MOD}:item/{iid}_oversized"}},
        "_oversized_throwing": {"parent": "spectrum:templates_item/handheld_oversized_throwing",
                                "textures": {"layer0": f"{MOD}:item/{iid}_oversized"}},
    }
    return base, models, {"": src, "_oversized": f"{src}_oversized"}


SPECIAL_MODELS = {"crossbow": crossbow_models, "bident": bident_models}
# TEXTURE SEGNAPOSTO (2026-11-25, DA SOSTITUIRE quando l'utente approva le texture vere): tipo → tipo dello stesso
# tier di cui il modello usa la texture già installata. Nessun file nuovo (validate.py lo segnala come AVVISO).
PLACEHOLDER_TEXTURE = {}  # 2026-11-26: le mazze T6-T9 hanno le loro texture (approvate dall utente)
# Modello vanilla da cui ereditare la posa in mano.
MODEL_PARENT = {"bow": "minecraft:item/bow", "fishing_rod": "minecraft:item/handheld_rod",
                "mace": "minecraft:item/handheld_mace",
                "shears": "minecraft:item/generated", **{k: "minecraft:item/generated" for k in ARMOR_KINDS}}
# Varianti animate: suffisso texture → (predicato, modello padre della variante).
VARIANTS = {
    "bow": [("_pulling_0", {"pulling": 1}), ("_pulling_1", {"pulling": 1, "pull": 0.65}),
            ("_pulling_2", {"pulling": 1, "pull": 0.9})],
    "fishing_rod": [("_cast", {"cast": 1})],
}


def gate_path(tier_id):
    """Percorso (sotto advancement/) del lucchetto di un tier, col nome del tier (2026-11-23): unlocks/tiers/trichromatic,
    umbral, argent, prismatic."""
    return f"unlocks/tiers/{TIERS[tier_id]['prefix']}"


def gate(tier_id):
    return f"{MOD}:{gate_path(tier_id)}"


def write_json(rel, data):
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def item_id(obj):
    return f"{TIERS[obj['tier']]['prefix']}_{obj['kind']}"


def is_fusion(obj):
    return obj.get("machine") == "fusion"


def recipe_path(obj):
    return f"fusion_shrine/{obj['tier']}/{item_id(obj)}"


def unlock_path(obj):
    return f"unlocks/equipment/{obj['tier']}/{item_id(obj)}"


# La Fusion Shrine ha 7 slot (FusionShrineBlockEntity, ricerca/01 §2.1): massimo 7 ingredienti, liquido escluso.
FUSION_MAX_INGREDIENTS = 7


def t6_fusion_ingredients(obj):
    """Ingredienti dopo base e fusion_extras per il T6: N lingotti di allthemodium, N Shattered Bedrock (se N > 0),
    poi le polveri di FUSION["t6"]."""
    out = [{"tag": "c:ingots/allthemodium", "count": obj["allthemodium"]}]
    if obj["bedrock_dust"]:
        out.append({"item": "spectrum:bedrock_dust", "count": obj["bedrock_dust"]})
    return out + FUSION["t6"]["ingredients"]


def t7_fusion_ingredients(obj):
    """Ingredienti dopo la base per il T7: N lingotti di vibranium e M polveri di onice."""
    return [{"tag": "c:ingots/vibranium", "count": obj["vibranium"]},
            {"item": "spectrum:onyx_powder", "count": obj["onyx"]}]


def t8_fusion_ingredients(obj):
    """Ingredienti dopo la base per il T8: N lingotti di unobtainium e M polveri di moonstone."""
    return [{"tag": "c:ingots/unobtainium", "count": obj["unobtainium"]},
            {"item": "spectrum:moonstone_powder", "count": obj["moonstone"]}]


def t9_fusion_ingredients(obj):
    """Ingredienti dopo la base per il T9: 1 lingotto per lega, N Colorful Shooting Star e M Stardust."""
    # La Colorful Shooting Star si impila a 1 (jar: ShootingStarItem, IS.of(1, UNCOMMON)): uno slot della Fusion Shrine non
    # può tenerne 2, quindi una ricetta con "count": 2 non si completerebbe mai. Ogni stella ha il suo slot (count 1):
    # 4 slot fissi + stelle + Stardust = 7 al massimo, con 2 stelle.
    return ([{"tag": tag, "count": 1} for tag in TIERS["t9"]["alloys"]]
            + [{"item": "spectrum:colorful_shooting_star", "count": 1}] * obj["stars"]
            + [{"item": "spectrum:stardust", "count": obj["stardust"]}])


def gen_fusion_recipe(obj):
    """Upgrade alla Fusion Shrine: la Fusion Shrine non ha tier, quindi il requisito "Pedestal Moonstone" passa
    dall'advancement dell'oggetto (che richiede il gate del tier, gate_path)."""
    fusion = FUSION[obj["tier"]]
    time, experience = FUSION_TIME_XP[obj["tier"]]["heavy" if obj["kind"] in FUSION_HEAVY_KINDS else "light"]
    rest = {"t6": t6_fusion_ingredients, "t7": t7_fusion_ingredients, "t8": t8_fusion_ingredients,
            "t9": t9_fusion_ingredients}[obj["tier"]](obj)
    # Ingredienti extra di Spectrum subito dopo il primo (es. il Fractal per il bident, gli accessori dell'armatura).
    ingredients = [obj["base"]] + obj.get("fusion_extras", []) + rest
    assert len(ingredients) <= FUSION_MAX_INGREDIENTS, \
        f"{item_id(obj)}: {len(ingredients)} ingredienti alla Fusion Shrine (massimo {FUSION_MAX_INGREDIENTS})"
    recipe = {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "allthemodium"}],
        "type": "spectrum:fusion_shrine",
        "group": f"{MOD}_{obj['tier']}_{category(obj)}",
        "time": time,
        "experience": experience,
        "fluid": {"fluid": fusion["fluid"]},
    }
    # Copia dal primo ingrediente (l'oggetto di Spectrum o del tier precedente): incantesimi, area di scavo, opzioni
    # del menu. Sempre, anche sulle basi bedrock del T6 (decisione dell'utente 2026-11-18): per lo stesso incantesimo
    # vince il livello più alto tra pezzo e result (SpectrumEnchantmentHelper.addOrUpgradeEnchantment).
    recipe["copy_components"] = True
    # Senza, l'upgrade Yield della struttura potrebbe duplicare l'oggetto (checklist D2).
    recipe["disable_yield_upgrades"] = True
    recipe["ingredients"] = ingredients
    recipe["result"] = {"id": f"{MOD}:{item_id(obj)}", "count": 1,
                        "components": {"minecraft:enchantments": {"levels": obj["enchantments"]}}}
    recipe["required_advancement"] = f"{MOD}:{unlock_path(obj)}"
    write_json(f"data/{MOD}/recipe/{recipe_path(obj)}.json", recipe)


def gen_object(obj, lang):
    gen_fusion_recipe(obj)
    gen_rest(obj, lang)


def gotten_with_tick(name, advancement_id):
    """Coppia di criteri in OR per un lucchetto di progressione. revelationary:advancement_gotten scatta solo nel
    momento in cui l'advancement viene assegnato: chi aggiunge l'addon a una partita già avanzata non lo vedrebbe mai.
    Il criterio minecraft:tick con il predicato del giocatore controlla di continuo se l'advancement è già ottenuto
    (formato 1.21.1: player = lista di loot condition entity_properties, type_specific player/advancements).
    Ritorna ({criteri}, [nomi]) da mettere in un'unica riga di requirements (OR)."""
    criteria = {
        name: {"trigger": "revelationary:advancement_gotten",
               "conditions": {"advancement_identifier": advancement_id}},
        "tick_" + name: {"trigger": "minecraft:tick", "conditions": {"player": [{
            "condition": "minecraft:entity_properties", "entity": "this",
            "predicate": {"type_specific": {"type": "minecraft:player",
                                            "advancements": {advancement_id: True}}}}]}},
    }
    return criteria, [name, "tick_" + name]


def gen_tier_unlock(tier_id):
    """unlocks/tiers/<nome del tier> (gate_path): il lucchetto precedente e il primo lingotto del tier. Per il T6 il lucchetto è il
    Pedestal complex di Spectrum; dal T7 è il tier precedente (che lo contiene già: checklist C, T6 -> T7 -> ...).
    Il lingotto (inventory_changed) scatta al primo cambio d'inventario anche a partita avanzata, non serve il tick.
    extra_unlocks (dal T8): altri advancement di Spectrum richiesti in AND (ricerca/04 §5.1: T8 = T7 + glass crest)."""
    tier = TIERS[tier_id]
    if "requires_tier" in tier:
        prev = tier["requires_tier"]
        criteria, built = gotten_with_tick(f"unlocked_{prev}", f"{gate(prev)}")
    else:
        criteria, built = gotten_with_tick("built_complex_pedestal", "spectrum:lategame/build_complex_pedestal_structure")
    requirements = [built]
    for adv in tier.get("extra_unlocks", []):
        c, req = gotten_with_tick("unlocked_" + adv.rpartition("/")[2], adv)  # es. unlocked_glass_crest_tools
        criteria.update(c)
        requirements.append(req)
    # T9: uno qualsiasi dei tre lingotti di lega (una sola riga di requirements = OR, ricerca/04 §5.1).
    ingots = []
    for tag in tier.get("alloys", [tier["ingot"]]):
        ingot = "has_" + tag.rpartition("/")[2] + "_ingot"  # es. has_allthemodium_ingot, has_vibranium_allthemodium_alloy_ingot
        criteria[ingot] = {"trigger": "minecraft:inventory_changed",
                           "conditions": {"items": [{"items": "#" + tag}]}}
        ingots.append(ingot)
    write_json(f"data/{MOD}/advancement/{gate_path(tier_id)}.json",
               {"criteria": criteria, "requirements": requirements + [ingots]})


def object_locks(obj):
    """Lucchetti dell'advancement dell'oggetto, come (nome del criterio, advancement): il tier e lo sblocco di ogni
    oggetto di Spectrum (o del tier precedente) usato come base o fusion_extra. Li usa anche lo scenario sblocchi."""
    locks = [(f"unlocked_{obj['tier']}", gate(obj['tier']))]
    for extra in [obj["base"]] + obj.get("fusion_extras", []):
        if extra in EXTRA_UNLOCKS:
            locks.append(("unlocked_" + extra.partition(":")[2], EXTRA_UNLOCKS[extra]))
    return locks


def gen_rest(obj, lang):
    tier = TIERS[obj["tier"]]
    iid = item_id(obj)
    # Un advancement per oggetto, usato sia dalla ricetta sia dalla pagina del guidebook (checklist §A).
    # Richiede il tier e lo sblocco di ogni oggetto di Spectrum usato come base o extra (criteri tutti obbligatori).
    criteria, requirements = {}, []
    for name, adv in object_locks(obj):
        c, req = gotten_with_tick(name, adv)
        criteria.update(c)
        requirements.append(req)
    write_json(f"data/{MOD}/advancement/{unlock_path(obj)}.json", {"criteria": criteria, "requirements": requirements})
    if obj["kind"] in SPECIAL_MODELS:
        gen_special_models(obj, iid)
    else:
        gen_models(obj, iid)
    lang["en_us"][f"item.{MOD}.{iid}"] = f"{tier['name_en']} {obj['en']}"
    lang["it_it"][f"item.{MOD}.{iid}"] = f"{obj['it']} {tier['name_it'][IT_GENDER[obj['kind']]]}"
    lang["en_us"][f"book.{MOD}.guidebook.{obj['tier']}_equipment.{iid}.text"] = obj["guide_en"]
    lang["it_it"][f"book.{MOD}.guidebook.{obj['tier']}_equipment.{iid}.text"] = obj["guide_it"]


def make_texture(iid, suffix, source, tier_id):
    """Texture provvisoria ricolorata da una texture di Spectrum. Non sovrascrive mai una texture esistente."""
    tex = os.path.join(RES, f"assets/{MOD}/textures/item/{iid}{suffix}.png")
    if not os.path.exists(tex):
        subprocess.run([sys.executable, "-I", os.path.join(ROOT, "tools", "make_placeholder.py"), JAR,
                        source, tier_id, tex], check=True)


def gen_special_models(obj, iid):
    base, models, textures = SPECIAL_MODELS[obj["kind"]](iid)
    write_json(f"assets/{MOD}/models/item/{iid}.json", base)
    for suffix, model in models.items():
        write_json(f"assets/{MOD}/models/item/{iid}{suffix}.json", model)
    for suffix, source in textures.items():
        make_texture(iid, suffix, source, obj["tier"])


def gen_models(obj, iid):
    model = {"parent": MODEL_PARENT.get(obj["kind"], "minecraft:item/handheld"),
             "textures": {"layer0": f"{MOD}:item/{iid}"}}
    if obj["kind"] in PLACEHOLDER_TEXTURE:
        model["textures"]["layer0"] = f"{MOD}:item/{TIERS[obj['tier']]['prefix']}_{PLACEHOLDER_TEXTURE[obj['kind']]}"
        write_json(f"assets/{MOD}/models/item/{iid}.json", model)
        return
    variants = VARIANTS.get(obj["kind"], [])
    if obj["kind"] in OVERSIZED:
        # Stesso template di Spectrum (posa e scala in mano); l'override vale 1 solo nel rendering in mano.
        model["parent"] = "spectrum:templates_item/handheld_oversized"
        variants = [("_oversized", {"oversized": 1.0})]
    if variants:
        model["overrides"] = [{"predicate": pred, "model": f"{MOD}:item/{iid}{suffix}"} for suffix, pred in variants]
    write_json(f"assets/{MOD}/models/item/{iid}.json", model)
    for suffix, _ in variants:
        parent = "minecraft:item/fishing_rod" if obj["kind"] == "fishing_rod" else model["parent"]
        # Il modello _oversized non ha a sua volta override (come glass_crest_workstaff_oversized.json).
        write_json(f"assets/{MOD}/models/item/{iid}{suffix}.json",
                   {"parent": parent, "textures": {"layer0": f"{MOD}:item/{iid}{suffix}"}})
    # Texture provvisorie ricolorate dalla versione bedrock (o glass crest per gli oversized).
    source = OVERSIZED.get(obj["kind"], obj["base"].partition(":")[2])
    for suffix in [""] + [s for s, _ in variants]:
        make_texture(iid, suffix, source + suffix, obj["tier"])


# Voce-indizio «Idea», SOLO per il T6 (decisione dell'utente 2026-11-23: le Idee di T7, T8 e T9 sono state tolte),
# costruita come quelle di Spectrum (general/*_hint.json: spectrum:hint, parents [],
# hide_while_locked, read_by_default). Visibile dal primo attrezzo di malachite (lategame/create_malachite_tool,
# concesso dalla Fusion Shrine); le pagine si aprono gratis allo sblocco del tier. Costo 8/24 Moonstone Shard come
# le idee Moonstone di Spectrum: arrivati alla malachite il giocatore ha già craftato il Pedestal Moonstone (che
# consuma uno shard) e costruito la struttura complex, quindi li ha di sicuro (checklist §B).
# Titoli: chiavi di Spectrum (small_clue, concise_instructions); in it_it le definiamo noi, Spectrum non ha l'italiano.
HINTS = {
    "t6": {
        "visible_after": "spectrum:lategame/create_malachite_tool",
        "x": 6, "y": 8,
        "cost": [("spectrum:moonstone_shard", 8), ("spectrum:moonstone_shard", 24)],
        "titles": ["book.spectrum.guidebook.ideas.small_clue", "book.spectrum.guidebook.ideas.concise_instructions"],
        "text_en": [
            "Bedrock is the toughest thing I have ever worked with, yet something nags at me: it feels like a beginning, "
            "not an end. Somewhere down in the ancient cities, a golden metal glimmers with more than mere strength.",
            "That golden metal will not be worked on just any bench: it needs an altar worthy of it, and I have a feeling "
            "my best equipment has not had its last word yet.\\\n\\\n*Anything less would only embarrass it.*"],
        "text_it": [
            "La bedrock è la cosa più dura con cui abbia mai lavorato, eppure qualcosa mi tormenta: sembra un inizio, "
            "non una fine. Da qualche parte, giù nelle ancient city, un metallo dorato brilla di qualcosa di più della "
            "semplice forza.",
            "Quel metallo dorato non si lascia lavorare su un banco qualsiasi: ha bisogno di un altare alla sua altezza, "
            "e ho la sensazione che il mio equipaggiamento migliore non abbia ancora detto l'ultima parola.\\\n\\\n"
            "*Qualunque cosa di meno non farebbe altro che metterla in imbarazzo.*"],
    },
}
SPECTRUM_KEYS = {  # chiavi di Spectrum usate dalla nostra Idea: EN identico a Spectrum, IT nostro (Spectrum non l'ha)
    "en_us": {"book.spectrum.guidebook.idea": "An Idea",
              "book.spectrum.guidebook.ideas.small_clue": "An observation",
              "book.spectrum.guidebook.ideas.concise_instructions": "Concise Instructions"},
    "it_it": {"book.spectrum.guidebook.idea": "Un'idea",
              "book.spectrum.guidebook.ideas.small_clue": "Un'osservazione",
              "book.spectrum.guidebook.ideas.concise_instructions": "Istruzioni concise"},
}


def gen_tier_hint(tier_id, lang):
    h = HINTS[tier_id]
    pages = []
    for i in range(2):
        criteria, built = gotten_with_tick(f"unlocked_{tier_id}", gate(tier_id))
        criteria["hint_purchased"] = {"trigger": "minecraft:impossible"}
        write_json(f"data/{MOD}/advancement/triggers/hints/{tier_id}_page{i}.json",
                   {"criteria": criteria, "requirements": [["hint_purchased"] + built]})
        item, count = h["cost"][i]
        # Spectrum 1.12.9 legge `amount` (BookHintPage.fromJson); i suoi JSON e il wiki usano `cost`: scriviamo entrambe per compatibilità con la correzione futura.
        pages.append({"type": "spectrum:hint", "title": h["titles"][i],
                      "completion_advancement": f"{MOD}:triggers/hints/{tier_id}_page{i}",
                      "cost": {"item": item, "count": count},
                      "amount": {"item": item, "count": count},
                      "text": f"book.{MOD}.guidebook.{tier_id}_hint.page{i}.text"})
        for code in ("en_us", "it_it"):
            lang[code][f"book.{MOD}.guidebook.{tier_id}_hint.page{i}.text"] = h["text_" + code[:2]][i]
    for code in ("en_us", "it_it"):
        lang[code].update(SPECTRUM_KEYS[code])
        lang[code].pop(f"book.{MOD}.guidebook.{tier_id}_hint.description", None)
    write_json(f"data/spectrum/modonomicon/books/guidebook/entries/general/{MOD}_{tier_id}_hint.json", {
        "name": "book.spectrum.guidebook.idea",
        "description": f"book.{MOD}.added_by",  # le idee di Spectrum mostrano il soggetto; qui vale la regola «added by»
        "category": "spectrum:general",
        "hide_while_locked": True,
        "parents": [],
        "background_u_index": 0,
        "background_v_index": 5,
        "x": h["x"],
        "y": h["y"],
        "icon": "spectrum:textures/gui/guidebook/icons/idea.png",
        "condition": {"type": "modonomicon:advancement", "advancement_id": h["visible_after"]},
        "read_by_default": True,
        "pages": pages,
    })


def gen_tier_entry(tier_id, objs):
    tier = TIERS[tier_id]
    pages = [{"type": "modonomicon:text",
              "title": f"book.{MOD}.guidebook.{tier_id}_equipment.name",
              "text": f"book.{MOD}.guidebook.{tier_id}_equipment.page0.text"}]
    for obj in objs:
        pages.append({"type": "spectrum:fusion_shrine_crafting",
                      "title": f"item.{MOD}.{item_id(obj)}",
                      "recipe_id": f"{MOD}:{recipe_path(obj)}",
                      # Stesso advancement della ricetta: la pagina compare solo quando si può fare (checklist §A).
                      "condition": {"type": "modonomicon:advancement", "advancement_id": f"{MOD}:{unlock_path(obj)}"},
                      "text": f"book.{MOD}.guidebook.{tier_id}_equipment.{item_id(obj)}.text"})
    if tier_id in ARMOR_SET_PAGES and any(category(o) == "armor" for o in objs):
        pages.append({"type": "modonomicon:text",
                      "title": f"book.{MOD}.guidebook.{tier_id}_equipment.armor_set.title",
                      "text": f"book.{MOD}.guidebook.{tier_id}_equipment.armor_set.text"})
    write_json(f"data/spectrum/modonomicon/books/guidebook/entries/equipment/{MOD}_{tier_id}_equipment.json", {
        "name": f"book.{MOD}.guidebook.{tier_id}_equipment.name",
        "description": f"book.{MOD}.added_by",
        # Nascosta finché è bloccata, come le voci di Spectrum (nessuno spoiler, niente tooltip).
        "condition": {"type": "modonomicon:advancement", "advancement_id": gate(tier_id)},
        "icon": {"item": f"{MOD}:{item_id(objs[0])}"},
        "category": "spectrum:equipment",
        "hide_while_locked": True,
        "parents": [{"entry": tier["entry"]["parent"]}],
        "background_u_index": 0,
        "background_v_index": 0,
        "x": tier["entry"]["x"],
        "y": tier["entry"]["y"],
        "pages": pages,
    })


def gen_tags():
    by_tag = {}
    # La mazza di bedrock per prima, con i tag di tutte le mazze.
    for tag in KIND_TAGS["mace"]:
        by_tag.setdefault(tag, []).append(f"{MOD}:{BEDROCK_MACE['id']}")
    for obj in OBJECTS:
        for tag in KIND_TAGS[obj["kind"]]:
            by_tag.setdefault(tag, []).append(f"{MOD}:{item_id(obj)}")
    for tag, items in by_tag.items():
        ns, _, path = tag.partition(":")
        write_json(f"data/{ns}/tags/item/{path}.json", {"values": items})
    # Tag da cui togliere gli oggetti, come fa Spectrum con la bedrock: indistruttibili fuori dal tag della
    # durabilità (niente Unbreaking/Mending); armatura fuori da trimmable_armor (il modello 3D non ha i trim).
    remove = {"minecraft:enchantable/durability": OBJECTS,
              "minecraft:trimmable_armor": [o for o in OBJECTS if o["kind"] in ARMOR_KINDS]}
    for tag, objs in remove.items():
        if objs:
            ns, _, path = tag.partition(":")
            extra = [f"{MOD}:{BEDROCK_MACE['id']}"] if tag == "minecraft:enchantable/durability" else []
            write_json(f"data/{ns}/tags/item/{path}.json",
                       {"values": [], "remove": extra + [f"{MOD}:{item_id(o)}" for o in objs]})


def gen_ink_colors():
    """Colori d'inchiostro (data/<ns>/ink_color_mapping/item/, letti da ColorMappingDataLoader di Spectrum) per
    ingredienti e risultati delle nostre ricette Fusion Shrine: servono agli effetti della macchina."""
    mapping = {}
    for tier_id, fusion in FUSION.items():
        if any(is_fusion(o) and o["tier"] == tier_id for o in OBJECTS):
            for color, items in fusion["ink_colors"].items():
                mapping.setdefault(color, []).extend(i for i in items if i not in mapping.get(color, []))
    # Tutti i nostri oggetti col colore del tier: possono finire in Fusion Shrine ed
    # Enchanter (es. come base del tier successivo) e /spectrum sanity li vuole colorati (checklist §E).
    mapping.setdefault(BEDROCK_MACE["ink"], []).append(f"{MOD}:{BEDROCK_MACE['id']}")
    mapping.setdefault(BEDROCK_MACE["base_ink"][0], []).append(BEDROCK_MACE["base_ink"][1])
    for obj in OBJECTS:
        mapping.setdefault(FUSION_INK[obj["tier"]], []).append(f"{MOD}:{item_id(obj)}")
    if mapping:
        write_json(f"data/{MOD}/ink_color_mapping/item/{MOD}.json", mapping)


def drop_stale_lang(lang):
    """Toglie le traduzioni generate per oggetti non più in OBJECTS (i file generati vanno invece cancellati a mano)."""
    ids = {item_id(o) for o in OBJECTS}
    for tier_id, tier in TIERS.items():
        prefixes = (f"item.{MOD}.{tier['prefix']}_", f"book.{MOD}.guidebook.{tier_id}_equipment.{tier['prefix']}_")
        for code in lang:
            for key in [k for k in lang[code] if k.startswith(prefixes)]:
                rest = key[len(next(p for p in prefixes if key.startswith(p))):]
                if f"{tier['prefix']}_{rest.removesuffix('.text')}" not in ids:
                    del lang[code][key]
        # Testi delle Idee dei tier che non ne hanno più una (HINTS: solo T6 dal 2026-11-23).
        if tier_id not in HINTS:
            for code in lang:
                for key in [k for k in lang[code] if k.startswith(f"book.{MOD}.guidebook.{tier_id}_hint.")]:
                    del lang[code][key]
        # Pagina armor_set solo per i tier che la possiedono (T6 e T9).
        if tier_id not in ARMOR_SET_PAGES:
            for code in lang:
                lang[code].pop(f"book.{MOD}.guidebook.{tier_id}_equipment.armor_set.title", None)
                lang[code].pop(f"book.{MOD}.guidebook.{tier_id}_equipment.armor_set.text", None)
    # Vecchie chiavi orfane (es. unbreakable del T6)
    for code in lang:
        lang[code].pop(f"book.{MOD}.guidebook.t6_equipment.unbreakable.title", None)
        lang[code].pop(f"book.{MOD}.guidebook.t6_equipment.unbreakable.text", None)


def gen_armor_texture(tier_id):
    """Texture del modello 3D indossato (stesso layout 128×128 di bedrock_armor.png), provvisoria e ricolorata.
    SAArmorItem.getArmorTexture la cerca in textures/armor/<prefisso>_armor.png. Non sovrascrive mai."""
    tex = os.path.join(RES, f"assets/{MOD}/textures/armor/{TIERS[tier_id]['prefix']}_armor.png")
    if not os.path.exists(tex):
        os.makedirs(os.path.dirname(tex), exist_ok=True)
        subprocess.run([sys.executable, "-I", os.path.join(ROOT, "tools", "make_placeholder.py"), JAR,
                        "assets/spectrum/textures/armor/bedrock_armor.png", tier_id, tex], check=True)


# ---------- Scenario «sblocchi» (src/dev/resources/scenarios/sblocchi.json) ----------
# Concede IN ORDINE gli advancement che servono per vedere in EMI e nel guidebook le ricette di tutti i tier di
# TIERS/OBJECTS: prima la progressione di Spectrum richiesta (SPECTRUM_BASE_UNLOCKS), poi per ogni tier il lucchetto
# del tier, gli sblocchi di Spectrum degli ingredienti di ogni oggetto (object_locks, gli stessi dell'advancement) e
# l'advancement dell'oggetto, infine le pagine dell'Idea (solo T6). Si rigenera da solo: un tier nuovo in TIERS/OBJECTS (T8,
# Alloy) finisce nello scenario senza toccarlo. Non dà oggetti e non cambia la modalità di gioco: si può usare nel
# proprio mondo con /sa scenario run sblocchi.
SCENARIO_DIR = os.path.join(ROOT, "src", "dev", "resources", "scenarios")
# Progressione di Spectrum richiesta da tutti i tier, in ordine. "until" = l'advancement e i suoi antenati.
# Le ultime voci sono gli advancement "mantello Revelationary" degli ingredienti Spectrum (altrimenti mostrati camuffati da oggetti vanilla con nomi illeggibili).
SPECTRUM_BASE_UNLOCKS = [
    ("spectrum:place_pedestal", "Pedestal: sblocco del tipo di ricetta, EMI lo chiede per mostrare le ricette Pedestal"),
    ("spectrum:lategame/build_complex_pedestal_structure", "Pedestal Moonstone: tier complex delle ricette Pedestal"),
    ("spectrum:build_fusion_shrine", "Fusion Shrine: sblocco del tipo di ricetta"),
    ("spectrum:lategame/create_malachite_tool", "primo attrezzo di malachite: rende visibile l'Idea del T6"),
    ("spectrum:milestones/unlock_ink_use", "Ink: area della workstaff, overcharge della balestra, modalità del bident"),
    ("spectrum:midgame/break_decayed_bedrock", "rivela la Bedrock Dust (camuffata da polvere da sparo da Revelationary)"),
    ("spectrum:hidden/collect_shards/amethyst", "rivela la Amethyst Powder (camuffata da colorante)"),
    ("spectrum:hidden/collect_shards/citrine", "rivela la Citrine Powder (camuffata da colorante)"),
    ("spectrum:hidden/collect_shards/topaz", "rivela la Topaz Powder (camuffata da colorante)"),
    ("spectrum:create_onyx_shard", "rivela la Onyx Powder (T7+)"),
    ("spectrum:lategame/collect_moonstone", "rivela la Moonstone Powder (T8+); esplicito per sicurezza"),
    ("spectrum:milestones/unlock_shooting_stars", "rivela la Stardust (T9: camuffata da tintura viola da Revelationary)"),
]


def gen_unlock_scenario():
    steps, granted = [], set()

    def log(text):
        steps.append({"action": "log", "text": text})

    def grant(adv, why, mode):
        if adv in granted:
            return
        granted.add(adv)
        log(f"concedo {adv} ({mode}): {why}")
        # expect any: se l'advancement c'è già il comando "fallisce" (niente da concedere); il controllo vero è sotto
        steps.append({"action": "command", "expect": "any", "command": f"advancement grant @s {mode} {adv}"})

    log("Sblocchi in ordine fino all'ultimo tier. Nessun oggetto dato, modalità di gioco invariata.")
    for adv, why in SPECTRUM_BASE_UNLOCKS:
        grant(adv, why, "until")
    # Mazza di bedrock (prima del T6, che la usa come base).
    log("== Mazza di bedrock")
    grant(BEDROCK_MACE["unlock"], "ricetta e pagina della mazza di bedrock (attrezzi bedrock di Spectrum)", "until")
    checks = []
    for tier_id, tier in TIERS.items():
        objs = [o for o in OBJECTS if o["tier"] == tier_id]
        if not objs:
            continue
        log(f"== {tier_id.upper()} {tier['name_en']}: {len(objs)} oggetti")
        hint = HINTS.get(tier_id)
        if hint and not hint["visible_after"].startswith(MOD + ":"):
            grant(hint["visible_after"], f"rende visibile l'Idea del {tier_id.upper()}", "until")
        if "requires_tier" in tier:
            grant(gate(tier['requires_tier']), "tier precedente", "only")
        for adv in tier.get("extra_unlocks", []):
            grant(adv, f"lucchetto del {tier_id.upper()}", "until")
        # Il lingotto del tier (inventory_changed) non serve: l'advancement si concede intero, senza dare oggetti.
        grant(gate(tier_id), f"tier {tier['name_en']} (al posto del primo lingotto {tier['ingot']})", "only")
        for obj in objs:
            for _, adv in object_locks(obj)[1:]:
                grant(adv, f"ingrediente di {item_id(obj)}", "only" if adv.startswith(MOD + ":") else "until")
            grant(f"{MOD}:{unlock_path(obj)}", f"ricetta e pagina di {item_id(obj)}", "only")
        if hint:
            for i in range(len(hint["cost"])):
                grant(f"{MOD}:triggers/hints/{tier_id}_page{i}", f"pagina {i} dell'Idea, in chiaro", "only")
        checks.append((tier_id, objs))

    steps.append({"action": "wait", "ticks": 40})  # il libro del client ci mette un po' ad aggiornare le voci
    log("Controllo: advancement, ricette (server e client) e voci del guidebook")
    for adv in sorted(granted):
        steps.append({"action": "assert_advancement", "id": adv})
    mace_entry = f"spectrum:equipment/{MOD}_{BEDROCK_MACE['id']}"
    steps += [{"action": "assert_recipe_unlocked", "recipe": f"{MOD}:{BEDROCK_MACE['recipe']}"},
              {"action": "assert_book_entry", "entry": mace_entry, "unlocked": True},
              {"action": "assert_book_entry", "entry": mace_entry, "page": 1, "unlocked": True}]
    for tier_id, objs in checks:
        entry = f"spectrum:equipment/{MOD}_{tier_id}_equipment"
        steps.append({"action": "assert_book_entry", "entry": entry, "unlocked": True})
        for i, obj in enumerate(objs):
            steps.append({"action": "assert_recipe_unlocked", "recipe": f"{MOD}:{recipe_path(obj)}"})
            steps.append({"action": "assert_book_entry", "entry": entry, "page": i + 1, "unlocked": True})
        if tier_id in HINTS:
            steps.append({"action": "assert_book_entry", "entry": f"spectrum:general/{MOD}_{tier_id}_hint", "unlocked": True})
        # Una ricetta in EMI (bloccata = «You have not discovered this recipe yet») e la voce del libro.
        if objs:
            obj = objs[0]
            steps += [{"action": "open_screen", "screen": "recipes", "recipe": f"{MOD}:{recipe_path(obj)}"},
                      {"action": "wait", "ticks": 5},
                      {"action": "screenshot", "name": f"emi_{tier_id}_{item_id(obj)}"},
                      {"action": "close_screen"}]
        steps += [{"action": "open_screen", "screen": "guidebook", "entry": entry, "page": 0},
                  {"action": "wait", "ticks": 5},
                  {"action": "screenshot", "name": f"libro_{tier_id}"},
                  {"action": "close_screen"}]
    log(f"Fatto: {len(granted)} advancement concessi in ordine")
    scenario = {"description": "Sblocchi in ordine (Spectrum, poi ogni tier e i suoi oggetti) per vedere in EMI e nel "
                               "guidebook tutte le ricette; non dà oggetti né cambia la modalità di gioco",
                "stop_on_fail": False, "steps": steps}
    os.makedirs(SCENARIO_DIR, exist_ok=True)
    with open(os.path.join(SCENARIO_DIR, "sblocchi.json"), "w", encoding="utf-8") as f:
        f.write("// GENERATO da tools/gen_data.py (gen_unlock_scenario): non modificarlo a mano, rilancia il generatore.\n")
        json.dump(scenario, f, indent=2, ensure_ascii=False)
        f.write("\n")


def roman(n):
    """Numero romano (1-3999)."""
    out = ""
    for value, sym in ((1000, "M"), (900, "CM"), (500, "D"), (400, "CD"), (100, "C"), (90, "XC"), (50, "L"),
                       (40, "XL"), (10, "X"), (9, "IX"), (5, "V"), (4, "IV"), (1, "I")):
        while n >= value:
            out += sym
            n -= value
    return out


def drop_stale_recipes():
    """Cancella le ricette JSON che il generatore non scrive più (es. le vecchie Pedestal del T6, passate alla Fusion
    Shrine): niente doppie ricette per lo stesso oggetto. Tutte le ricette della mod sono generate da qui."""
    base = os.path.join(RES, f"data/{MOD}/recipe")
    wanted = {os.path.normpath(os.path.join(base, recipe_path(o) + ".json")) for o in OBJECTS}
    wanted.add(os.path.normpath(os.path.join(base, BEDROCK_MACE["recipe"] + ".json")))
    for dirpath, _, files in os.walk(base, topdown=False):
        for name in files:
            path = os.path.normpath(os.path.join(dirpath, name))
            if name.endswith(".json") and path not in wanted:
                os.remove(path)
                print("Ricetta non più generata, cancellata:", os.path.relpath(path, ROOT))
        if dirpath != base and not os.listdir(dirpath):
            os.rmdir(dirpath)


# Massimo vanilla (max_level del JSON di Minecraft 1.21.1, colonna «max originale» di ricerca/09 A-B) dei 42 incantesimi
# della tabella di SAEnchantCap (stesso elenco: lo controlla il GameTest overenchanting_advancement).
VANILLA_MAX = {
    "sharpness": 5, "smite": 5, "bane_of_arthropods": 5, "sweeping_edge": 3, "fire_aspect": 2, "knockback": 2,
    "looting": 3, "breach": 4, "density": 5, "wind_burst": 3, "efficiency": 5, "fortune": 3, "silk_touch": 1,
    "unbreaking": 3, "mending": 1, "power": 5, "punch": 2, "flame": 1, "infinity": 1, "piercing": 4,
    "quick_charge": 3, "multishot": 1, "impaling": 5, "loyalty": 3, "riptide": 3, "channeling": 1, "protection": 4,
    "fire_protection": 4, "blast_protection": 4, "projectile_protection": 4, "feather_falling": 4, "thorns": 3,
    "respiration": 3, "aqua_affinity": 1, "depth_strider": 3, "frost_walker": 2, "soul_speed": 3, "swift_sneak": 3,
    "luck_of_the_sea": 3, "lure": 3, "binding_curse": 1, "vanishing_curse": 1,
}


def gen_overenchanting_unlock():
    """Override di spectrum:milestones/unlock_overenchanting_with_enchanter (stesso percorso: il nostro pack si carica
    dopo quello di Spectrum, ordering AFTER nel mods.toml). In Spectrum 1.12.9 l'unico criterio è
    revelationary:advancement_gotten su spectrum:__wip, che non esiste: overenchanting irraggiungibile (ricerca/03 §1.3).
    Il file di Spectrum non ha display, parent né rewards: non c'è altro da conservare. Nuova condizione (decisione
    utente): avere in inventario, con uno dei 42 incantesimi della tabella ATM10 sopra il massimo vanilla (es.
    Sharpness VI+, Protection V+),
    - un libro (qualsiasi oggetto con minecraft:stored_enchantments): criteri book_*;
    - oppure un oggetto incantato (minecraft:enchantments) che NON è nostro (predicato spectral_ascension:not_ours,
      SAPredicates): criteri item_*. Contano il bottino di Apotheosis e gli oggetti vanilla, di Spectrum (es. lo
      spadone glass crest, Sweeping Edge IV: decisione utente 2026-11-24) e delle altre mod; non i nostri pezzi T6-T9.
    Criteri in OR (una sola riga di requirements), tutti minecraft:inventory_changed: si valutano a ogni cambio
    dell'inventario e al login come prima. Il tetto delle ricette di upgrade è nel mixin EnchantmentUpgradeRecipeMixin."""
    criteria = {}
    for name, vmax in VANILLA_MAX.items():
        criteria[f"book_{name}_above_vanilla"] = {
            "trigger": "minecraft:inventory_changed",
            "conditions": {"items": [{"predicates": {"minecraft:stored_enchantments": [
                {"enchantments": f"minecraft:{name}", "levels": {"min": vmax + 1}}]}}]},
        }
    for name, vmax in VANILLA_MAX.items():
        criteria[f"item_{name}_above_vanilla"] = {
            "trigger": "minecraft:inventory_changed",
            "conditions": {"items": [{"predicates": {
                "minecraft:enchantments": [{"enchantments": f"minecraft:{name}", "levels": {"min": vmax + 1}}],
                f"{MOD}:not_ours": {}}}]},
        }
    write_json("data/spectrum/advancement/milestones/unlock_overenchanting_with_enchanter.json",
               {"criteria": criteria, "requirements": [list(criteria)]})


# Pagina «Overchanting» del guidebook di Spectrum (entries/enchanting/overchanting.json): l'elenco «vanilla → massimo»
# sta nelle chiavi di lingua page1..page3.text (non nel JSON della voce). I numeri di Spectrum non coincidono con le
# ricette spectrum:enchantment_upgrade (massimo = levels + 1) tagliate al tetto ATM10 (EnchantmentUpgradeRecipeMixin):
# qui sono corretti Exuberance (10 -> 8), Sniping (4 -> 5), Bane of Arthropods (8 -> 5: la ricetta ha 3 livelli, massimo 4,
# sotto il massimo vanilla 5, quindi non si alza) e Thorns (6 -> 5, tetto ATM10). Tutto il resto è copiato da Spectrum 1.12.9.
# Le chiavi vanno in assets/spectrum/lang/en_us.json (stesso file di Spectrum, il nostro pack si carica dopo): nel
# namespace della mod i namespace sono caricati in ordine alfabetico e «spectrum» vincerebbe. Spectrum non ha it_it.
OVERCHANTING_PAGES = {
    "book.spectrum.guidebook.overchanting.page1.text": "- Clovers' Favor: 3 \u2192 6\n- Disarming: 2 \u2192 4\n- Exuberance: 5 \u2192 8\n- First Strike: 2 \u2192 5\n- Improved Critical: 2 \u2192 4\n- Inertia: 2 \u2192 5\n- Tight Grip: 2 \u2192 4\n- Trophy Hunter: 3 \u2192 5\n- Sniping: 2 \u2192 5",
    "book.spectrum.guidebook.overchanting.page2.text": "- Bane of Arthrop.: 5 \u2192 5\n- Blast Protection: 4 \u2192 8\n- Depth Strider: 3 \u2192 3\n- Efficiency: 5 \u2192 8\n- Feather Falling: 4 \u2192 6\n- Fire Aspect: 2 \u2192 4\n- Fire Protection: 4 \u2192 8\n- Fortune: 3 \u2192 5\n- Frost Walker: 2 \u2192 4\n- Impaling: 5 \u2192 8\n- Knockback: 2 \u2192 5\n- Looting: 3 \u2192 6\n- Loyalty: 3 \u2192 4",
    "book.spectrum.guidebook.overchanting.page3.text": "- Luck of the Sea: 3 \u2192 5\n- Lure: 3 \u2192 5\n- Piercing: 4 \u2192 8\n- Power: 5 \u2192 8\n- Proj. Protection: 4 \u2192 8\n- Protection: 4 \u2192 8\n- Punch: 2 \u2192 5\n- Quick Charge: 3 \u2192 5\n- Respiration: 3 \u2192 6\n- Riptide: 3 \u2192 4\n- Sharpness: 5 \u2192 8\n- Smite: 5 \u2192 8\n- Soul Speed: 3 \u2192 4\n- Sweeping Edge: 3 \u2192 7\n- Thorns: 3 \u2192 5\n- Unbreaking: 3 \u2192 6",
}


def gen_overchanting_guidebook():
    write_json("assets/spectrum/lang/en_us.json", OVERCHANTING_PAGES)


# Minerali di Spectrum nella Mining Dimension di Allthemodium (ricerca/12 §3). Quote assolute: la dimensione e' a strati
# (end stone -63..0, netherrack 1..64, deepslate 65..128, stone 129..247). Le configured feature sono quelle di Spectrum.
MINING_DIM_BIOME = "allthemodium:mining"  # id diretto, NON il tag mining_biomes (contiene anche i biomi di The Other)
MINING_DIM_ORES = [
    # ore, count, rarity_filter, tipo di altezza, min, max
    ("shimmerstone_ore", 6, None, "minecraft:uniform", 65, 247),
    ("azurite_ore", 1, 64, "minecraft:trapezoid", 65, 128),
    ("stratine_ore", 6, None, "minecraft:trapezoid", 1, 64),
    ("paltaeria_ore", 2, None, "minecraft:trapezoid", -63, 0),
]


def gen_mining_dim_ores():
    for ore, count, rarity, htype, lo, hi in MINING_DIM_ORES:
        placement = []
        if rarity:
            placement.append({"type": "minecraft:rarity_filter", "chance": rarity})
        placement += [
            {"type": "minecraft:count", "count": count},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range",
             "height": {"type": htype, "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}},
            {"type": "minecraft:biome"},
        ]
        write_json(f"data/{MOD}/worldgen/placed_feature/mining_dim/{ore}.json",
                   {"feature": f"spectrum:{ore}", "placement": placement})
        write_json(f"data/{MOD}/neoforge/biome_modifier/mining_dim/{ore}.json", {
            "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "allthemodium"}],
            "type": "neoforge:add_features",
            "biomes": MINING_DIM_BIOME,
            "features": f"{MOD}:mining_dim/{ore}",
            "step": "underground_ores",
        })


ABOUT_MORE = {
    "title_en": "Descent", "title_it": "Discesa",
    "text_en": "Gold in the ancient cities, green in the Nether, violet in the End. I carried each one home and watched my Spectrum gear take them in as if it had been waiting all along.<br><br>The last one was different. It did not shine. It *listened*.<br><br>*Some doors only open from the far side of the bedrock.*",
    "text_it": "Oro nelle ancient city, verde nel Nether, viola nell'End. Li ho portati a casa uno a uno e ho visto il mio equipaggiamento di Spectrum accoglierli come se non aspettasse altro.<br><br>L'ultimo era diverso. Non brillava. *Ascoltava*.<br><br>*Alcune porte si aprono solo dal lato più lontano del bedrock.*",
    "tip_en": "*Pro tip: the built-in resource pack \"Eternal Classics\" gives the equipment a different style. Switch it on in the resource packs menu.*",
    "tip_it": "*Consiglio: il resource pack integrato «Eternal Classics» dà all'equipaggiamento un altro stile. Accendilo dal menu dei resource pack.*",
}


def gen_about_entry(lang):
    """Voce General «Addon: Spectral Ascension»: pagina 1 (about, testo nel lang) + pagina 2 (Descent) + pagina 3 (consiglio)."""
    base = f"book.{MOD}.guidebook.{MOD}"
    for code, sfx in (("en_us", "en"), ("it_it", "it")):
        lang[code][f"{base}.more.title"] = ABOUT_MORE["title_" + sfx]
        # `<br>` = a capo del lang di Spectrum: backslash + newline
        lang[code][f"{base}.more.text"] = ABOUT_MORE["text_" + sfx].replace("<br>", "\\\n")
        lang[code][f"{base}.more.tip.text"] = ABOUT_MORE["tip_" + sfx]
    write_json(f"data/spectrum/modonomicon/books/guidebook/entries/general/{MOD}.json", {
        "name": f"{base}.name",
        "description": f"book.{MOD}.added_by",
        "icon": {"item": f"{MOD}:trichromatic_workstaff"},
        "category": "spectrum:general",
        "hide_while_locked": False,
        "background_u_index": 0,
        "background_v_index": 4,
        "x": -13,
        "y": -4,
        "read_by_default": True,
        "pages": [
            {"type": "modonomicon:text", "title": f"{base}.about.title", "text": f"{base}.about.text"},
            {"type": "modonomicon:text", "title": f"{base}.more.title", "text": f"{base}.more.text"},
            {"type": "modonomicon:text", "text": f"{base}.more.tip.text"},
        ],
    })


def main():
    lang = {}
    for code in ("en_us", "it_it"):
        with open(os.path.join(RES, f"assets/{MOD}/lang/{code}.json"), encoding="utf-8") as f:
            lang[code] = json.load(f)
    drop_stale_lang(lang)
    gen_about_entry(lang)
    gen_bedrock_mace(lang)
    for obj in OBJECTS:
        gen_object(obj, lang)
    for tier_id in TIERS:
        objs = [o for o in OBJECTS if o["tier"] == tier_id]
        if objs:
            gen_tier_unlock(tier_id)
            gen_tier_entry(tier_id, objs)
            for code, suffix in (("en_us", "en"), ("it_it", "it")):
                page0 = f"book.{MOD}.guidebook.{tier_id}_equipment.page0.text"
                lang[code][f"book.{MOD}.guidebook.{tier_id}_equipment.name"] = TIERS[tier_id]["entry_name_" + suffix]
                lang[code][page0] = TIERS[tier_id]["page0_" + suffix]
                lang[code].pop(f"book.{MOD}.guidebook.{tier_id}_equipment.locked", None)  # tooltip rimosso
            if tier_id in HINTS:
                gen_tier_hint(tier_id, lang)
            lang["en_us"][f"recipeGroup.spectrum.{MOD}_{tier_id}_tools"] = f"{TIERS[tier_id]['name_en']} Tools"
            lang["it_it"][f"recipeGroup.spectrum.{MOD}_{tier_id}_tools"] = f"Attrezzi {TIERS[tier_id]['name_it']['mp']}"
            if any(category(o) == "armor" for o in objs):
                gen_armor_texture(tier_id)
                lang["en_us"][f"recipeGroup.spectrum.{MOD}_{tier_id}_armor"] = f"{TIERS[tier_id]['name_en']} Armor"
                lang["it_it"][f"recipeGroup.spectrum.{MOD}_{tier_id}_armor"] = f"Armatura {TIERS[tier_id]['name_it']['f']}"
                if tier_id in ARMOR_SET_PAGES:
                    for code, suffix in (("en_us", "en"), ("it_it", "it")):
                        lang[code][f"book.{MOD}.guidebook.{tier_id}_equipment.armor_set.title"] = ARMOR_SET_PAGES[tier_id]["title_" + suffix]
                        lang[code][f"book.{MOD}.guidebook.{tier_id}_equipment.armor_set.text"] = ARMOR_SET_PAGES[tier_id]["text_" + suffix]
    # Tooltip dell'armatura (SAArmorItem.appendHoverText): uguale per tutti i tier, %s = Dike del set.
    # Workstaff (SATools.Workstaff): costo dell'area in White Ink nel tooltip e messaggio quando l'inchiostro manca.
    lang["en_us"][f"item.{MOD}.workstaff.aoe_cost"] = "Area mining costs %s White Ink per block"
    lang["it_it"][f"item.{MOD}.workstaff.aoe_cost"] = "Lo scavo ad area costa %s Ink bianco per blocco"
    lang["en_us"][f"item.{MOD}.workstaff.aoe_free"] = "Area mining is free (no White Ink)"
    lang["it_it"][f"item.{MOD}.workstaff.aoe_free"] = "Lo scavo ad area è gratuito (niente Ink bianco)"
    lang["en_us"][f"item.{MOD}.workstaff.aoe_no_ink"] = "Not enough White Ink for area mining (%s needed)"
    lang["it_it"][f"item.{MOD}.workstaff.aoe_no_ink"] = "Ink bianco insufficiente per lo scavo ad area (ne servono %s)"
    lang["en_us"][f"item.{MOD}.workstaff.aoe_ink_locked"] = "Area mining needs Ink, which you have not unlocked yet (Spectrum: craft the Pigment Pedestal)"
    lang["it_it"][f"item.{MOD}.workstaff.aoe_ink_locked"] = "Lo scavo ad area usa l'Ink, che non hai ancora sbloccato (Spectrum: crea il Pigment Pedestal)"
    # Workstaff T7+ (SATools.Workstaff, WorkstaffOption, SAWorkstaffScreen): stato degli interruttori nel tooltip,
    # messaggi sopra la hotbar delle voci nuove del menu, nomi e tooltip delle voci. Le voci di Spectrum usano le sue
    # chiavi item.spectrum.workstaff.gui.* / message.*. Foundry e Inventory Insertion restano in inglese (come Resonance).
    for code, text in (("en_us", {
            "foundry_paused": "Foundry paused while Silk Touch is selected",
            "foundry_off": "Foundry turned off (Sneak + Use to turn it on)",
            "insertion_off": "Inventory Insertion turned off (Sneak + Use to turn it on)",
            "message.7x7": "Selected 7x7 mining mode",
            "message.9x9": "Selected 9x9 mining mode",
            "message.foundry_on": "Foundry turned on",
            "message.foundry_on_fortune": "Foundry turned on: switched from Silk Touch to Fortune",
            "message.foundry_off": "Foundry turned off",
            "message.silk_touch_foundry_paused": "Switched to Silk Touch: Foundry paused until you switch back",
            "message.insertion_on": "Inventory Insertion turned on",
            "message.insertion_off": "Inventory Insertion turned off"}),
                       ("it_it", {
            "foundry_paused": "Foundry in pausa finché è scelto Tocco di velluto",
            "foundry_off": "Foundry spenta (Accovacciati + Usa per accenderla)",
            "insertion_off": "Inventory Insertion spenta (Accovacciati + Usa per accenderla)",
            "message.7x7": "Modalità di scavo 7x7 selezionata",
            "message.9x9": "Modalità di scavo 9x9 selezionata",
            "message.foundry_on": "Foundry accesa",
            "message.foundry_on_fortune": "Foundry accesa: passata da Tocco di velluto a Fortuna",
            "message.foundry_off": "Foundry spenta",
            "message.silk_touch_foundry_paused": "Passata a Tocco di velluto: Foundry in pausa finché non torni indietro",
            "message.insertion_on": "Inventory Insertion accesa",
            "message.insertion_off": "Inventory Insertion spenta"})):
        for key, value in text.items():
            lang[code][f"item.{MOD}.workstaff.{key}"] = value
    for code, text in (("en_us", {
            "larger_area": "Larger Area",
            "larger_area.tooltip_7x7": "7x7 area",
            "larger_area.tooltip_9x9": "7x7 and 9x9 areas",
            "7x7": "Select 7x7 mining mode",
            "9x9": "Select 9x9 mining mode",
            "drops": "Drops",
            "drops.tooltip_t7": "Foundry",
            "drops.tooltip_t8": "Foundry and Inventory Insertion",
            "enable_foundry": "Enable Foundry",
            "disable_foundry": "Disable Foundry",
            "foundry.tooltip": "Smelts the drops, like a furnace",
            "foundry.tooltip_paused": "Paused while Silk Touch is selected",
            "foundry.tooltip_to_fortune": "Silk Touch is selected: turning Foundry on switches to Fortune",
            "silk_touch.tooltip_pauses_foundry": "Pauses Foundry until you switch back to Fortune or Resonance",
            "enable_insertion": "Enable Inventory Insertion",
            "disable_insertion": "Disable Inventory Insertion",
            "insertion.tooltip": "Drops go straight into your inventory"}),
                       ("it_it", {
            "larger_area": "Area più grande",
            "larger_area.tooltip_7x7": "Area 7x7",
            "larger_area.tooltip_9x9": "Aree 7x7 e 9x9",
            "7x7": "Seleziona la modalità di scavo 7x7",
            "9x9": "Seleziona la modalità di scavo 9x9",
            "drops": "Drop",
            "drops.tooltip_t7": "Foundry",
            "drops.tooltip_t8": "Foundry e Inventory Insertion",
            "enable_foundry": "Accendi Foundry",
            "disable_foundry": "Spegni Foundry",
            "foundry.tooltip": "Fonde i drop, come una fornace",
            "foundry.tooltip_paused": "In pausa finché è scelto Tocco di velluto",
            "foundry.tooltip_to_fortune": "È scelto Tocco di velluto: accendere Foundry passa a Fortuna",
            "silk_touch.tooltip_pauses_foundry": "Mette in pausa Foundry finché non torni a Fortuna o Resonance",
            "enable_insertion": "Accendi Inventory Insertion",
            "disable_insertion": "Spegni Inventory Insertion",
            "insertion.tooltip": "I drop finiscono dritti nell'inventario"})):
        for key, value in text.items():
            lang[code][f"gui.{MOD}.workstaff_menu.{key}"] = value
    lang["en_us"][f"item.{MOD}.armor.tooltip.set_dike"] = "Full set: grants %s units of Azure Dike"
    lang["it_it"][f"item.{MOD}.armor.tooltip.set_dike"] = "Set completo: dà %s unità di Azure Dike"
    # Stivali con lo Shieldgrasp Amulet potenziato a Ink (SAArmorSet, componente dike_bonus): %s = Dike in più del set.
    lang["en_us"][f"item.{MOD}.armor.tooltip.dike_bonus"] = "Infused Shieldgrasp Amulet: %s Azure Dike"
    lang["it_it"][f"item.{MOD}.armor.tooltip.dike_bonus"] = "Shieldgrasp Amulet infuso: %s Azure Dike"
    for code in ("en_us", "it_it"):  # tooltip rimosso (protezione a Ink dell'armatura eliminata)
        lang[code].pop(f"item.{MOD}.armor.tooltip.ink_protection", None)
    lang["en_us"][f"item.{MOD}.armor.tooltip.trinkets_disabled"] = \
        "While the full set is worn, Azure Dike trinkets in trinket slots are disabled"
    lang["it_it"][f"item.{MOD}.armor.tooltip.trinkets_disabled"] = \
        "Con il set completo indossato, gli accessori Azure Dike negli slot degli accessori sono disattivati"
    # Bident (SABident): modalità nel tooltip e nel messaggio sopra la hotbar; "stored" = set dell'altra modalità,
    # "inactive" = incantesimi del set attivo che non si sono potuti applicare (BidentEnchantments). Le righe descrittive delle modalità
    # sono quelle dei bident glass crest di Spectrum (item.spectrum.*_glass_crest_bident.tooltip*).
    for code, text in (("en_us", {"mode": "Mode: %s", "mode.off": "Off", "mode.ferocious": "Ferocious",
                                  "mode.fractal": "Fractal", "switch_hint": "Sneak + Use to open the mode menu",
                                  "stored": "Stored for %s: %s", "inactive": "Inactive in %s: %s"}),
                       ("it_it", {"mode": "Modalità: %s", "mode.off": "Spenta", "mode.ferocious": "Ferocious",
                                  "mode.fractal": "Fractal", "switch_hint": "Accovacciati + Usa per aprire il menu delle modalità",
                                  "stored": "Memorizzati per %s: %s", "inactive": "Non attivi in %s: %s"})):
        for key, value in text.items():
            lang[code][f"item.{MOD}.bident.{key}"] = value
    # Menu del bident (BidentScreen, griglia della workstaff di Spectrum): titolo, tooltip di una riga per voce e
    # "%s (attiva)" sulla modalità attiva. I nomi delle voci sono item.<mod>.bident.mode.*.
    for code, text in (("en_us", {"": "Bident Mode", "active": "%s (active)",
                                  "off": "A plain bident: no abilities, no Ink",
                                  "ferocious": "Riptide even out of water, hurting whatever you touch (White Ink)",
                                  "fractal": "Throws a Mirror Image and stays in your hand (White Ink)"}),
                       ("it_it", {"": "Modalità del bidente", "active": "%s (attiva)",
                                  "off": "Un bidente normale: nessuna abilità, niente Ink",
                                  "ferocious": "Riptide anche fuori dall'acqua, ferendo chi tocchi (Ink bianco)",
                                  "fractal": "Lancia un'immagine speculare e resta in mano (Ink bianco)"})):
        for key, value in text.items():
            lang[code][f"gui.{MOD}.bident_menu" + (f".{key}" if key else "")] = value
    # Livelli degli incantesimi oltre il X: vanilla ha le chiavi solo fino a enchantment.level.10, Spectrum non le
    # aggiunge (in ATM10 le definisce Apothic Enchanting, fino a 127). Senza, il tooltip mostra la chiave grezza.
    for n in range(11, 31):
        for code in ("en_us", "it_it"):
            lang[code][f"enchantment.level.{n}"] = roman(n)
    drop_stale_recipes()
    gen_tags()
    gen_ink_colors()
    gen_unlock_scenario()
    gen_overenchanting_unlock()
    gen_overchanting_guidebook()
    gen_mining_dim_ores()
    for code in ("en_us", "it_it"):
        write_json(f"assets/{MOD}/lang/{code}.json", lang[code])
    print(f"Generati {len(OBJECTS) + 1} oggetti: {BEDROCK_MACE['id']}, " + ", ".join(item_id(o) for o in OBJECTS))


if __name__ == "__main__":
    main()
