"""T6 Trichromatic, set misto: ATTREZZI/ARMI dello stile 3 (Cristallo Scheggiato) + ARMATURA dello stile 5
(Reliquia Rituale). Tutto disegnato A MANO: ogni sprite e ogni faccia dell'armatura indossata è una griglia
ASCII scritta carattere per carattere. Il codice converte le griglie in PNG, le dispone nel layout UV
128x128 e compone panoramica, anteprime e pagina; non disegna forme, contorni, ombre o luci.

Le griglie di partenza (spada e ascia dello stile 3, i quattro pezzi d'armatura dello stile 5) stanno in
tools/texture_hand.py; qui ci sono le versioni ritoccate a mano per le varianti e tutti i pezzi nuovi.

Uso:  python3 tools/texture_mix.py              # tutto (scrive SOLO in design/texture/proposte/t6-mix/)
      python3 tools/texture_mix.py confronto DIR  # confronti x12 con la bedrock di Spectrum
      python3 -I tools/texture_mix.py install     # installa nella mod il set approvato (vedi sotto)

INSTALLAZIONE NELLA MOD (comando `install`). È l'unica strada con cui le texture T6 entrano nella mod; non
copiarle a mano. Copia i file GIÀ GENERATI e approvati in design/texture/proposte/t6-mix/ (non li rigenera:
per cambiare il disegno prima si rilancia lo script senza argomenti, si controlla stili.html, poi `install`):
  set/t6_<pezzo>_anim.png(.mcmeta) -> src/main/resources/assets/spectral_ascension/textures/item/
                                       trichromatic_<pezzo>.png(.mcmeta)   (striscia 16x192, 12 x 5 tick)
  armatura/t6_armor.png            -> textures/armor/trichromatic_armor.png    (statica, la usa gen_data.py)
  armatura/t6_armor_<i>.png        -> textures/armor/trichromatic_armor_<i>.png  (i = 0..11)
Toglie i fotogrammi dell'armatura oltre l'ultimo (residui del vecchio set a 21) e non tocca le texture degli
oggetti speciali (workstaff, ultra greatsword, crossbow, bident). Il numero di fotogrammi (FRAMES) e i tick per
fotogramma (FRAMETIME) devono restare uguali a ARMOR_FRAMES/ARMOR_FRAME_TICKS di SAArmorItem.java;
tools/validate.py controlla che i file della mod coincidano byte per byte con quelli di design/.
"""
import base64
import io
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402

ROOT = H.ROOT
REF = H.REF
OUT = os.path.join(ROOT, "design", "texture", "proposte", "t6-mix")
S3 = next(s for s in H.STYLES if s["num"] == 3)["grids"]
S5 = next(s for s in H.STYLES if s["num"] == 5)["grids"]
SET6 = ["sword", "axe", "helmet", "chestplate", "leggings", "boots"]

# ---------------------------------------------------------------------------
# Palette unificata: un solo metallo cenere-viola per armi e armatura (a metà tra lo stile 3 e lo 5),
# pergamena dello stile 5 in g-k, il resto dalla palette di base (allthemodium, C, M, Y).
# ---------------------------------------------------------------------------
METAL = {"0": "#0b0a11", "1": "#1c1926", "2": "#2c2838", "3": "#3e394f", "4": "#555068",
         "5": "#736d8a", "6": "#9d97b4"}
PARCH = {"g": "#33210f", "h": "#7a5a35", "i": "#b8935f", "j": "#e3c690", "k": "#fff0cc"}
PAL = {k: H.hexc(v) for k, v in {**H.BASE, **METAL, **PARCH}.items()}
# palette originali (per la riga «prima»)
PAL3 = H.palette(next(s for s in H.STYLES if s["num"] == 3))
PAL5 = H.palette(next(s for s in H.STYLES if s["num"] == 5))


def G(text):
    """Una griglia scritta a mano in un blocco di testo (16 righe)."""
    rows = [r.strip() for r in text.strip().splitlines()]
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    return rows


# ===========================================================================
# VARIANTE B: richiamo incrociato leggero
#   spada: un piccolo occhio-gemma sulla guardia; ascia: un anello di catena con perla di citrino;
#   corazza: una scheggia d'ametista che spunta dal fianco destro.
# ===========================================================================
B_SWORD = G("""
..............mm
.............mpm
............mpom
...........mponm
..........mpedca
.q.......053d10.
qtq.q...054210..
qtsqtq.043210...
.qsrsrq04320....
..adedc5320.....
..0554adeda0....
..0443afoma0....
...0400aca0.....
..030...........
.uw0............
uxu.............
""")
B_AXE = G("""
.qq.............
qtsq.........00.
qtsrq.......040.
qtssre0000.040..
qtsrrd55440040..
qssrqc443324mpm.
qsrq.a332240monm
.qrq.0221406.mm.
..q...0040.3....
.......40..6....
......30...w....
.....40....v....
....30..........
...40...........
.uw0............
uxu.............
""")
B_CHEST = G("""
................
.00000....00000.
0566540..0566540
05e5430000554430
04c6445adeeda320
0444054afomfa410
.0404643acdca030
.03044063322020.
.02043330621010.
..004332220100..
...0433222160.m.
...0gk01110w0mpm
...0js430432dpnm
...0ij320321cmm.
...0hi210000....
....gh..........
""")

# ===========================================================================
# VARIANTE C: gli stessi due materiali passano in entrambe le direzioni
#   armi: l'impugnatura è fasciata da una striscia di pergamena con un segno d'inchiostro;
#   armatura: le perle che pendono dalle catene diventano piccoli citrini sfaccettati.
# ===========================================================================
C_SWORD = G("""
..............mm
.............mpm
............mpom
...........mponm
..........mpedca
.q.......053d10.
qtq.q...054210..
qtsqtq.043210...
.qsrsrq04320....
..adedc5320.....
..05543e2100....
..0443221100....
...gkg00000.....
..gsg.h.........
.uw0............
uxu.............
""")
C_AXE = G("""
.qq.............
qtsq.........00.
qtsrq.......040.
qtssre0000.040..
qtsrrd55440040..
qssrqc443324mpm.
qsrq.a332240monm
.qrq.022140..mm.
..q...0040......
.......40.......
.....gkg........
....gsgh........
....30..........
...40...........
.uw0............
uxu.............
""")
C_HELMET = G("""
................
................
....00000000....
...0566554430...
..05655adeeda06.
..05554afomfa03.
.gk44443acdca06.
.hj00c000000003.
.iS04433032210x.
.jh03322022110w.
.iY02211011100u.
.hg.00000000....
..hg............
................
................
................
""")
C_CHEST = G("""
................
.00000....00000.
0566540..0566540
05e5430000554430
04c6445adeeda320
0444054afomfa410
.0404643acdca030
.03044063322020.
.02043330621010.
..004332220100..
...0433222160.m.
...0gk01110x0mpm
...0jS430432dpnm
...0ij320321cmm.
...0hi210000....
....gh..........
""")
C_LEGS = G("""
................
................
..000000000000..
..055544433210e.
..0443332221106.
..0543000032100.
.gjkjig..042106.
.hiSihg..043100.
.i04310..aefdax.
.h03320..acdbaw.
.g04310..03210u.
..03210..03210..
..03210..02110..
..00000..00000..
................
................
""")
C_BOOTS = G("""
................
................
................
.00000....00000.
.05540....05540.
.gkjjg....04430.
.hiSih....05320.
.04320h...60606x
.04310g...04310v
043210....033210
aed430....043320
adc320....032210
aa0000....000000
................
................
................
""")

VARIANTS = [
    {"num": 1, "lett": "A", "nome": "Metallo unico",
     "desc": "Stesse griglie degli stili 3 e 5, cambia solo il metallo: un unico grigio-viola cenere, a metà tra i due.",
     "grids": {"sword": S3["sword"], "axe": S3["axe"], "helmet": S5["helmet"], "chestplate": S5["chestplate"],
               "leggings": S5["leggings"], "boots": S5["boots"]}},
    {"num": 2, "lett": "B", "nome": "Richiamo incrociato",
     "desc": "A, più un richiamo per famiglia: occhio-gemma sulla guardia della spada, anello di catena con perla di citrino sull'ascia, scheggia d'ametista che spunta dal fianco della corazza.",
     "grids": {"sword": B_SWORD, "axe": B_AXE, "helmet": S5["helmet"], "chestplate": B_CHEST,
               "leggings": S5["leggings"], "boots": S5["boots"]}},
    {"num": 3, "lett": "C", "nome": "Materiali condivisi",
     "desc": "A, più i due materiali che passano in entrambe le direzioni: le impugnature delle armi sono fasciate di pergamena scritta, le perle appese alle catene dell'armatura diventano piccoli citrini e dal fianco della corazza spunta una scheggia d'ametista.",
     "grids": {"sword": C_SWORD, "axe": C_AXE, "helmet": C_HELMET, "chestplate": C_CHEST,
               "leggings": C_LEGS, "boots": C_BOOTS}},
]

# ===========================================================================
# ATTREZZI NUOVI nello stile 3 (variante C: impugnature fasciate di pergamena)
# ===========================================================================
TOOLS_ART = '''
== shovel
................
............uuu.
...........uxxwu
..........uxxwvu
.........uxwwvvu
........udwwvvu.
........0edvvu..
.......054cuu...
......05430.....
......40........
.....30.........
...gkg..........
..gsgh..........
..40............
.uw0............
uxu.............
== hoe
..........000...
....mmmmmd05540.
...mpppooe04430.
...mponnmd03320.
...mpnm..c0420..
...mnm...40.....
....m...40......
.......30.......
......40........
.....gkg........
....gsgh........
....30..........
...40...........
..30............
.uw0............
uxu.............
== shears
............qq..
...........qtsq.
..........qtsq..
.........qtrq..m
........qtrq..mp
.......qsrq..mpn
.......edq..mpom
......0e0..mpnm.
.....4030.monm..
.5444...dnm.....
5..4...30m......
4..3...3........
.32..kjg........
....j..h........
....i..g........
.....hg.........
== bow
........0000.qq.
.....0005544dtsq
....05430000.dsq
...0430.....r.q.
..0430.....r....
..0d0.....r.....
.0kg.....r......
.0jg....r.......
.0ig...r........
.0d0..r.........
.030.r..........
.030r...........
.d4r............
md0.............
pon.............
mm..............
== bow_pulling_0
........0000.qq.
.qq..0005544dtsq
.qts05430000.dsq
..st430.....r.q.
..05s0.....r....
..0d050...r.....
.0kg.50..r......
.0jg..5ir.......
.0ig..ijr.......
.0d0...rr.......
.030..r.........
.030rr..........
.d4r............
md0.............
pon.............
mm..............
== bow_pulling_1
........0000.qq.
.qq..0005544etsq
.qts05430000.esq
..st430.....s.q.
..05s0......s...
..0d050....s....
.0kg.50....s....
.0jg..50..s.....
.0ig...5i.s.....
.0d0...ijs......
.030....ss......
.030...s........
.d4.ss..........
md0s............
pon.............
mm..............
== bow_pulling_2
........0000.qq.
qq...0005544ftsq
qts.05430000.esq
.st5430.....t.q.
..t50.......t...
..0d500.....t...
.0kg.50.....t...
.0jg..50...t....
.0ig...50..t....
.0d0....5i.t....
.030.....ijt....
.030.....tt.....
.e4..ttt........
me0tt...........
pon.............
mm..............
== fishing_rod
.............q..
............0tr.
...........40.r.
..........40..r.
.........40...r.
........40....r.
.......40.....r.
......40......r.
.....40.......r.
....4ea.......r.
...4ca........d.
..gkg........mpm
.gsgh........mon
.30...........nm
uw0...........m.
xu..............
== fishing_rod_cast
...............r
...............r
...............r
............000t
..........005440
.........044000.
.......0440.....
.......40.......
......40........
.....4fa........
....4ea.........
..gkg...........
.gsgh...........
.30.............
uw0.............
xu..............
'''

def parse_blocks(text):
    out, cur = {}, None
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("=="):
            cur = line[2:].strip()
            out[cur] = []
        elif line and cur:
            out[cur].append(line)
    return out


TOOLS = parse_blocks(TOOLS_ART)

# Il set completo nella variante scelta (C)
FINAL = {"sword": C_SWORD, "axe": C_AXE, **TOOLS,
         "helmet": C_HELMET, "chestplate": C_CHEST, "leggings": C_LEGS, "boots": C_BOOTS}
ORDER = ["sword", "axe", "shovel", "hoe", "shears", "bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2",
         "fishing_rod", "fishing_rod_cast", "helmet", "chestplate", "leggings", "boots"]
NOMI = {"sword": "spada", "axe": "ascia", "shovel": "pala", "hoe": "zappa", "shears": "cesoie", "bow": "arco",
        "bow_pulling_0": "arco, tensione 0", "bow_pulling_1": "arco, tensione 1", "bow_pulling_2": "arco, tensione 2",
        "fishing_rod": "canna da pesca", "fishing_rod_cast": "canna lanciata", "helmet": "elmo",
        "chestplate": "corazza", "leggings": "gambali", "boots": "stivali"}

# inchiostro animabile (stessi colori di s / o / w)
PAL.update({"S": PAL["s"], "O": PAL["o"], "Y": PAL["w"]})

# ===========================================================================
# ARMATURA INDOSSATA 128x128 - layout UV del BedrockArmorModel di Spectrum (LayerDefinition 128x128).
# Tabella dei cubi ricopiata, solo come dato, da tools/draw_textures.py (che non va toccato):
# nome: (u, v, larghezza, altezza, profondità)
# Ogni faccia qui sotto è una griglia scritta a mano. Mantello e perizoma sono resi 'entitySolid':
# nessun pixel trasparente.
# ===========================================================================
CUBES = {
    "head_in": (0, 20, 9, 9, 9), "head_out": (0, 0, 10, 10, 10),
    "body_in": (31, 33, 9, 13, 5), "body_out": (36, 20, 10, 10, 3),
    "right_arm": (22, 51, 5, 13, 5), "right_arm_extra": (57, 45, 6, 4, 6),
    "left_arm": (40, 0, 5, 13, 5), "left_arm_extra1": (62, 20, 5, 1, 5), "left_arm_extra2": (54, 12, 6, 2, 6),
    "left_leg": (42, 51, 5, 10, 5), "left_boot": (60, 0, 5, 3, 5),
    "right_leg": (59, 28, 5, 10, 5), "right_boot": (0, 61, 5, 3, 5),
    "cape": (0, 80, 11, 23, 1), "front_cloth": (62, 55, 7, 14, 1),
}


def cube_faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "west": (u, v + d, d, h),
            "north": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h)}


RIM5 = "22222 21112 21112 21112 22222".split()
RIM6 = "222222 2....2 2....2 2....2 2....2 222222".split()
RIM10 = ["2222222222"] + ["2........2"] * 8 + ["2222222222"]
HEAD_IN = "111111111 121112111 111111111 111211121 111111111 211111211 111111111 112111112 111111111".split()
ARM_TOP = "55554 54443 44443 44333 33332".split()
LEG_TOP = "44443 43333 43332 33322 33222".split()
PAULDRON_TOP = "566655 665544 655443 554433 544332 443322".split()

FACES = {
    # ---- testa interna (si vede solo dalle fessure) ----
    **{("head_in", f): HEAD_IN for f in ("top", "bottom", "west", "north", "east", "south")},
    # ---- elmo: occhio sulla fronte spostato a destra, pergamena a sinistra, catena a destra ----
    ("head_out", "north"): "5666555443 556adeeda3 455afomfa3 4544acdca2 4300c00032 5544544433 5444044332 4444043020 4433032121 3222022221".split(),
    ("head_out", "west"): "5555444433 5544443322 444433gkg2 443333jSj1 000000iji0 433322jYj1 433221iji1 322211hOh0 322110gig0 2111000hg0".split(),
    ("head_out", "east"): "5555444433 5444433322 4644333322 4033333221 0600000000 4033332221 4633322211 4033222111 4x22211110 2w11100000".split(),
    ("head_out", "south"): "4555554443 4554654332 4443d43322 443dad3221 4333d32221 3332332211 3322322111 3221221110 2211211100 1100100000".split(),
    ("head_out", "top"): "4555555543 5666655543 5665555443 5655554433 5555544433 4555444333 4444443332 4444333322 3433333222 3333322221".split(),
    ("head_out", "bottom"): RIM10,
    # ---- corazza: catena in diagonale, occhio in alto a destra, scheggia d'ametista al fianco sinistro ----
    ("body_out", "north"): "5665005665 5e54adeeda 4c64afomfa 44403acdca d434643322 e433303322 d333336322 pd43333033 oc33333363 m3322222x1".split(),
    ("body_out", "south"): "4555555544 4554554433 44gkg44332 44jSj43322 43iji33322 43hYh33221 33gig32221 3332322211 3322222111 2221111110".split(),
    ("body_out", "west"): "554 544 444 443 443 433 333 332 322 221".split(),
    ("body_out", "east"): "543 443 433 433 333 332 322 322 221 210".split(),
    ("body_out", "top"): "5665005665 4554004554 3443003443".split(),
    ("body_out", "bottom"): "2222222222 2111111112 2222222222".split(),
    # ---- busto interno: davanti si vede la cintura con la fibbia spostata a sinistra ----
    ("body_in", "north"): "433333334 433333334 432222234 432222234 432222234 432222234 432222234 432222234 332222233 322222223 00ade0000 11ada1111 222222222".split(),
    ("body_in", "south"): "433333334 433333334 432222234 432222234 432222234 432222234 432222234 432222234 332222233 322222223 000000000 111111111 222222222".split(),
    ("body_in", "west"): "43333 43222 43222 43222 43222 43222 43222 43222 33222 32222 00000 11111 22222".split(),
    ("body_in", "east"): "43333 43222 43222 43222 43222 43222 43222 43222 33222 32222 00000 11111 22222".split(),
    ("body_in", "top"): ["333333333"] * 5,
    ("body_in", "bottom"): ["222222222"] * 5,
    # ---- braccio destro: pergamena legata all'avambraccio, spallaccio ----
    ("right_arm", "north"): "55554 54443 44433 44333 43332 33322 gkjkg hjSjh gijig 33222 aedca acdba 22111".split(),
    ("right_arm", "south"): "55444 44433 44333 43332 33322 33222 gkjkg hjjjh gijig 32222 aedca acdba 22111".split(),
    ("right_arm", "west"): "55554 54443 44433 44333 43332 33322 gkjkg hjOjh gijig 3h222 aedca acdba 22111".split(),
    ("right_arm", "east"): "44443 44333 43332 33322 33222 32222 gkjkg hjjjh gijig 22211 acdca abcba 11111".split(),
    ("right_arm", "top"): ARM_TOP, ("right_arm", "bottom"): RIM5,
    ("right_arm_extra", "north"): "566655 555443 444332 adedca".split(),
    ("right_arm_extra", "south"): "555544 444433 333322 acdcba".split(),
    ("right_arm_extra", "west"): "556654 554433 443322 acddca".split(),
    ("right_arm_extra", "east"): "556654 554433 443322 acddca".split(),
    ("right_arm_extra", "top"): PAULDRON_TOP, ("right_arm_extra", "bottom"): RIM6,
    # ---- braccio sinistro: catena attorno al braccio, spallaccio a due strati d'oro ----
    ("left_arm", "north"): "55554 54443 46063 44333 43332 33322 33222 32222 32221 22211 aedca acdba 22111".split(),
    ("left_arm", "south"): "55444 44433 40603 43332 33322 33222 32222 32221 22211 22111 aedca acdba 22111".split(),
    ("left_arm", "west"): "44443 44333 06060 33322 33222 32222 32221 22211 22111 21111 acdca abcba 11111".split(),
    ("left_arm", "east"): "55554 54443 60606 44333 43332 33322 33222 32222 32221 22211 aedca acdba 22111".split(),
    ("left_arm", "top"): ARM_TOP, ("left_arm", "bottom"): RIM5,
    **{("left_arm_extra1", f): ["aedca"] for f in ("west", "north", "east", "south")},
    ("left_arm_extra1", "top"): "aedda e5554 d5443 d4432 acdca".split(),
    ("left_arm_extra1", "bottom"): "aaaaa a...a a...a a...a aaaaa".split(),
    **{("left_arm_extra2", f): ["566654", "443332"] for f in ("west", "north", "east", "south")},
    ("left_arm_extra2", "top"): PAULDRON_TOP, ("left_arm_extra2", "bottom"): RIM6,
    # ---- gamba destra: nastro di pergamena sulla coscia ----
    ("right_leg", "north"): "44433 gkjkg hjOjh gijig 43332 54443 43332 33222 32221 22111".split(),
    ("right_leg", "south"): "44333 gkjkg hjjjh gijig 33322 44333 33322 32221 22211 21111".split(),
    ("right_leg", "west"): "44433 gkjkg hjjjh gijig 43332 44433 33322 32221 22211 21111".split(),
    ("right_leg", "east"): "44333 gkjkg hjSjh gijig 33322 44332 33222 32211 22111 11111".split(),
    ("right_leg", "top"): LEG_TOP, ("right_leg", "bottom"): RIM5,
    # ---- gamba sinistra: ginocchiera accesa, catena con citrino sul lato esterno ----
    ("left_leg", "north"): "44433 44333 43332 33322 adeda acfda 33222 33221 32211 22111".split(),
    ("left_leg", "south"): "44333 43332 33322 33222 32222 33222 32221 32211 22111 21111".split(),
    ("left_leg", "west"): "44433 44333 43332 33322 acdca abcba 33222 32221 22211 21111".split(),
    ("left_leg", "east"): "44433 64333 03332 63322 x3222 w3221 33221 32211 22111 21111".split(),
    ("left_leg", "top"): LEG_TOP, ("left_leg", "bottom"): RIM5,
    # ---- stivali: pergamena e puntale acceso a destra, catena alla caviglia a sinistra ----
    ("right_boot", "north"): "gkjkg aed32 00000".split(),
    ("right_boot", "south"): "gkjkg 33222 00000".split(),
    ("right_boot", "west"): "gkjkg 43322 00000".split(),
    ("right_boot", "east"): "gkjkg 33222 00000".split(),
    ("right_boot", "top"): LEG_TOP, ("right_boot", "bottom"): ["00000"] * 5,
    ("left_boot", "north"): "60606 43322 00000".split(),
    ("left_boot", "south"): "06060 33222 00000".split(),
    ("left_boot", "west"): "60606 43322 00000".split(),
    ("left_boot", "east"): "06060 33222 00000".split(),
    ("left_boot", "top"): LEG_TOP, ("left_boot", "bottom"): ["00000"] * 5,
    # ---- mantello: rotolo di pergamena con tre colonne di rune CMY e un sigillo con l'occhio ----
    ("cape", "south"): """34444444443 23333333332 ghiiiiiiihg ghijjjjiihg ghSSiOiYYhg ghiSjOOiYhg ghSSiiOiYhg
        ghiijiiiihg ghiiiiiiihg ghijiiiiihg ghiiiiijihg ghiadeedahg ghiafomfahg ghiiacdcahg ghiiiiiiihg
        ghSSiiOiYhg ghiSjOiiYhg ghSSiiOOYhg ghiijiiiihg ghiiiiiiihg hhghhhghhhh ghhghhghhgg 1g11g1gg1g1""".split(),
    ("cape", "north"): """23333333332 hhhghhhghhh hhhghhhhghh hhghhhhghhh hhhghhhghhh hhhghhhhghh hhghhhhghhh
        hhhghhhghhh hhhghhhhghh hhghhhhghhh hhhghhhghhh hhhghhhhghh hhghhhhghhh hhhghhhghhh hhhghhhhghh
        hhghhhhghhh hhhghhhghhh hhhghhhhghh hhghhhhghhh hhhghhhghhh ghhghhghhgg ghhghhghhgg 1g11g1gg1g1""".split(),
    ("cape", "west"): list("3ggggggggggggggggggggg1"),
    ("cape", "east"): list("3ggggggggggggggggggggg1"),
    ("cape", "top"): ["34444444443"], ("cape", "bottom"): ["1g11g1gg1g1"],
    # ---- perizoma: talismano di pergamena tra le gambe ----
    ("front_cloth", "north"): "3444443 ghiiihg ghOOOhg ghiOihg ghOOOhg ghiiihg ghSiShg ghiSihg ghSiShg ghiiihg ghYYYhg ghiYihg hhghhhh g1hgh1g".split(),
    ("front_cloth", "south"): "2333332 hhhhhhh hhghhhh hhhhghh hhghhhh hhhhghh hhghhhh hhhhghh hhghhhh hhhhghh hhghhhh hhhhghh ghhghhg g1hgh1g".split(),
    ("front_cloth", "west"): list("3hhhhhhhhhhhhg"),
    ("front_cloth", "east"): list("3hhhhhhhhhhhhg"),
    ("front_cloth", "top"): ["3444443"], ("front_cloth", "bottom"): ["g1hgh1g"],
}


def armor_grid(faces=None, cutout=False):
    """Dispone le facce disegnate a mano nel layout UV 128x128 (controlla misure e facce mancanti)."""
    grid = [["."] * 128 for _ in range(128)]
    for cube, (u, v, w, h, d) in CUBES.items():
        for face, (fx, fy, fw, fh) in cube_faces(u, v, w, h, d).items():
            g = (faces or FACES)[(cube, face)]
            assert len(g) == fh and all(len(r) == fw for r in g), (cube, face, fw, fh, [len(r) for r in g])
            if cube in ("cape", "front_cloth") and not cutout:
                assert "." not in "".join(g), (cube, face, "entitySolid: niente trasparenze")
            for y, row in enumerate(g):
                for x, ch in enumerate(row):
                    grid[fy + y][fx + x] = ch
    return ["".join(r) for r in grid]


# Posizioni del manichino (vista frontale e posteriore), come in tools/draw_textures.py
MANNEQUIN = [("right_leg", -4.4, 11.85), ("left_leg", -0.6, 11.85), ("right_boot", -4.4, 21.15),
             ("left_boot", -0.6, 21.15), ("body_in", -4.5, -0.5), ("front_cloth", -3.5, 9.65), ("body_out", -5, 0),
             ("right_arm", -8.25, -0.5), ("left_arm", 3.25, -0.5), ("head_in", -4.5, -8.5), ("head_out", -5, -9)]


def mannequin(tex, scale, cape=True, cloth=True):
    vw, vh = 20, 36
    W = 2 * vw + 2
    out = Image.new("RGBA", (W, vh), (0, 0, 0, 0))
    for view, dx in (("north", 0), ("south", vw + 2)):
        seq = MANNEQUIN if view == "north" else [p for p in MANNEQUIN if p[0] != "front_cloth"] + [("cape", -5.5, 0.5)]
        seq = [p for p in seq if (cape or p[0] != "cape") and (cloth or p[0] != "front_cloth")]
        for cube, mx, my in seq:
            # il cubo del mantello è girato di 180 gradi: da dietro si vede la sua faccia "north"
            fx, fy, fw, fh = cube_faces(*CUBES[cube])["north" if cube == "cape" else view]
            if view == "south":
                mx = -mx - fw
            ox, oy = int(round(mx + 10)) + dx, int(round(my + 9.5))
            face = tex.crop((fx, fy, fx + fw, fy + fh))
            out.alpha_composite(face, (ox, oy))
    return up(out, scale)


# ===========================================================================
# ANIMAZIONI (12 fotogrammi x 5 tick = 3 s, interpolate false)
# Attrezzi: griglie di FASE scritte a mano ('.' fermo, '0'-'9','A','B' = fotogramma in cui passa la
#   cresta di luce); il pixel sale lungo la sua rampa di WAVE gradini (rampe e onda scritte a mano).
# Armatura: l'inchiostro (S, O, Y) scorre verso il basso cambiando colore ogni 4 fotogrammi; l'iride
#   degli occhi (o) si accende una volta per ciclo secondo IRIS.
# ===========================================================================
FRAMES, FRAMETIME = 12, 15
RAMPS = H.RAMPS
WAVE = H.WAVE
INK = ["S", "O", "Y"]
IRIS = ["o", "o", "o", "o", "o", "o", "o", "p", "W", "p", "o", "o"]

PHASES = {
    "sword": H.PHASES["sword"],
    "axe": H.PHASES["axe"],
    "shovel": """
................
................
............888.
...........7777.
..........66666.
.........35555..
.........3344...
..........2.....
................
................
................
................
................
................
..1.............
.0..............""",
    "hoe": """
................
.........3......
....876543......
....8765.2......
....98...2......
....9...........
................
................
................
................
................
................
................
................
..1.............
.0..............""",
    "shears": """
................
............66..
...........55...
..........44....
.........33....7
........22....66
.......00....55.
.......0....44..
...........33...
........12......
................
................
................
................
................
................""",
    "bow": """
................
............9AA.
.............9A.
............8...
...........8....
..........7.....
.........6......
........5.......
.......5........
......4.........
.....3..........
....3...........
.1.2............
.1..............
000.............
................""",
    "bow_pulling_0": """
................
............899.
..AA.........89.
..AA........7...
....A......6....
..........5.....
.........5......
........4.......
........4.......
.......33.......
......2.........
....22..........
.1.1............
.1..............
000.............
................""",
    "bow_pulling_1": """
................
............899.
..AA.........89.
..AA........7...
....A.......7...
...........6....
...........6....
..........5.....
..........5.....
.........4......
........44......
.......3........
.1..22..........
.1.1............
000.............
................""",
    "bow_pulling_2": """
................
............899.
.AA..........89.
.AA.........7...
..A.........7...
............6...
............6...
...........5....
...........5....
...........4....
...........4....
.........33.....
.1...222........
.1.22...........
000.............
................""",
    "fishing_rod": """
................
.............23.
..............3.
..............4.
..............4.
..............5.
..............5.
..............6.
..............6.
.....1........7.
....1.........8.
..............9.
..............99
..............A.
.0..............
0...............""",
    "fishing_rod_cast": """
...............5
...............4
...............3
...............2
................
................
................
................
................
......1.........
.....1..........
................
................
................
.0..............
0...............""",
}
PHASES = {k: (v if isinstance(v, list) else [r for r in v.strip("\n").splitlines()]) for k, v in PHASES.items()}


def wave_frames(grid, phases):
    frames = []
    for f in range(FRAMES):
        rows = []
        for y in range(16):
            row = list(grid[y])
            for x in range(16):
                ph = phases[y][x]
                if ph == ".":
                    continue
                ramp = next((r for r in RAMPS if row[x] in r), None)
                assert ramp, f"energia su '{row[x]}' in ({x},{y})"
                step = WAVE[(f - int(ph, 16)) % FRAMES]
                row[x] = ramp[min(ramp.index(row[x]) + step, len(ramp) - 1)]
            rows.append("".join(row))
        frames.append(rows)
    return frames


def ink_frames(grid):
    frames = []
    for f in range(FRAMES):
        rows = []
        for y, r in enumerate(grid):
            row = list(r)
            for x, ch in enumerate(row):
                if ch in INK:
                    row[x] = INK[(INK.index(ch) + (f - y) // 4) % 3]
                elif ch == "o":
                    row[x] = IRIS[f]
            rows.append("".join(row))
        frames.append(rows)
    return frames


# ===========================================================================
# SECONDO GIRO (scelte dell'utente): spada B+C, catena della corazza più leggibile, arco ridisegnato da capo,
# canna più robusta, armatura indossata con meno pergamena. Le versioni precedenti restano in OLD_* per il
# confronto prima/dopo.
# ===========================================================================
OLD_FINAL = dict(FINAL)
OLD_FACES = dict(FACES)

# Spada B+C: l'occhio-gemma sulla guardia (B) e l'impugnatura fasciata di pergamena (C)
BC_SWORD = G("""
..............mm
.............mpm
............mpom
...........mponm
..........mpedca
.q.......053d10.
qtq.q...054210..
qtsqtq.043210...
.qsrsrq04320....
..adedc5320.....
..0554adeda0....
..0443afoma0....
...gkg0aca0.....
..gsg.h.........
.uw0............
uxu.............
""")

# Corazza: la catena diagonale ora è fatta di maglie doppie (chiaro-medio) separate da un giunto scuro
NEW_CHEST = G("""
................
.00000....00000.
0566540..0566540
05e5430000554430
04c6445adeeda320
0444534afomfa410
.0404063acdca030
.03044353322020.
.02043330621010.
..004332225100..
...0433222100.m.
...0gk01110x0mpm
...0jS430432dpnm
...0ij320321cmm.
...0hi210000....
....gh..........
""")

# Arco ridisegnato: i due flettenti sono uno lo specchio dell'altro rispetto alla diagonale (impugnatura
# all'angolo in alto a sinistra), la corda è dritta da punta a punta. L'asimmetria sta nei dettagli:
# punta di topazio in alto, d'ametista in basso; scheggia sul bordo esterno in punti diversi; fratture diverse.
BOW2 = {
    "bow": G("""
.........t0000qq
.......00d555ets
.....005553c3dsr
...00j53330000qq
...0ki3000....r.
..0ji00......r..
..0530......r...
.0530......r....
pd530.....r.....
.0530....r......
0530....r.......
0530...r........
05c0..r.........
0ed0.r..........
mpomr...........
monm............
"""),
    "bow_pulling_0": G("""
.........t0000qq
.q.....00d555ets
..t..005553c3dsr
...60j53330000qq
...06i3000....r.
..0ji60......r..
..0530.6.....r..
.0530...6...r...
pd530...6i.r....
.0530...i6.r....
0530......r.....
0530....rr......
05c0...r........
0ed0.rr.........
mpomr...........
monm............
"""),
    "bow_pulling_1": G("""
.........t0000qq
.t.....00d555fts
..s..005553c3esr
...60j53330000qq
...06i3000....s.
..0ji60.......s.
..0530.6.....s..
.0530...6....s..
pd530....6..s...
.0530....6i.s...
0530.....i6s....
0530......ss....
05c0....ss......
0fd0..ss........
mpomss..........
monm............
"""),
    "bow_pulling_2": G("""
q........t0000qq
.t.....00d555fts
..s..005553c3fsr
...60j53330000qq
...06i3000....t.
..0ji60.......t.
..0530.6......t.
.0530...6....t..
pd530....6...t..
.0530.....6..t..
0530......6it...
0530......i6t...
05c0......ttt...
0fd0...ttt......
mpomttt.........
monm............
"""),
}

# Canna da pesca più robusta: canna a tre pixel con scheggia d'ametista e fratture, punta di topazio
ROD2 = {
    "fishing_rod": G("""
............qq..
...........qtsqr
..........0esq.r
.......m.054d0.r
......mp05430..r
.......d5430...r
......05430....r
.....05c30.....r
....05430......r
...05430.....mdm
..05430ea....mpm
.gkjg0ca.....mon
gsjg0.........nm
uw0...........m.
xu..............
................
"""),
    "fishing_rod_cast": G("""
...............r
...............r
..............qr
.........00000tq
.......m055e5es.
......mp5433200.
.......d5300....
.....05c30......
....05430.......
...05430........
..05430fa.......
.gkjg0ea........
gsjg0...........
uw0.............
xu..............
................
"""),
}

# La canna resta quella del primo giro (scelta dell'utente): ROD2 è tenuta solo per il confronto.
FINAL.update({"sword": BC_SWORD, "chestplate": NEW_CHEST, **BOW2})


def PH(text):
    rows = text.strip("\n").splitlines()
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    return rows


OLD_PHASES = dict(PHASES)
PHASES.update({
    # spada B+C: come prima, più l'iride dell'occhio sulla guardia che si accende al passaggio della luce
    "sword": H.PHASES["sword"][:11] + ["........3......."] + H.PHASES["sword"][12:],
    "bow": PH("""
.........6......
.........6...899
...........6.899
................
..............7.
.............6..
............6...
...........5....
22........5.....
.........4......
........4.......
.......3........
..1...3.........
.11..2..........
.00.2...........
.00............."""),
    "bow_pulling_0": PH("""
.........8......
.........8...899
..A........8.899
................
..............7.
.............7..
.............7..
............6...
22.........6....
...........5....
..........5.....
........44......
..1....3........
.11..23.........
.00.2...........
.00............."""),
    "bow_pulling_1": PH("""
.........8......
.A.......8...899
..A........8.899
................
..............7.
..............7.
.............7..
.............7..
22..........6...
............6...
...........6....
..........55....
..1.....44......
.11...33........
.00.22..........
.00............."""),
    "bow_pulling_2": PH("""
.........8......
.A.......8...899
..A........8.899
................
..............7.
..............7.
..............7.
.............6..
22...........6..
.............6..
............5...
............5...
..1.......444...
.11....333......
.00.222.........
.00............."""),
    "fishing_rod": PH("""
................
............77.7
...........66..7
............5..8
.......4.......8
.......3.......8
...............9
.......2.......9
...............9
..............A.
.......1......A.
......1.......BB
..............B.
.0..............
0...............
................"""),
    "fishing_rod_cast": PH("""
...............9
...............8
...............7
..............6.
...........4.56.
.......3........
.......2........
.......2........
................
................
.......1........
......1.........
................
.0..............
0...............
................"""),
})

ROD2_PHASES = {n: PHASES[n] for n in ROD2}
PHASES.update({n: OLD_PHASES[n] for n in ROD2})  # canna: animazioni del primo giro

# Arco, terzo giro: geometria del secondo (flettenti speculari rispetto alla diagonale, impugnatura nell'angolo,
# corda dritta, V nei pulling) con lo spessore del primo: contorno, un pixel chiaro, un pixel d'ombra.
BOW3 = {
    "bow": G("""
..............qq
.........t0000ts
.......00d5c5esr
.....00555222dqq
....0kj222....r.
...0ji2......r..
...0j2......r...
..052......r....
.pd52.....r.....
..052....r......
.052....r.......
.052...r........
.0c2..r.........
.0ed.r..........
mpomr...........
monm............
"""),
    "bow_pulling_0": G("""
..............qq
.q.......t0000ts
..t....00d5c5esr
...6.00555222dqq
....6kj222....r.
...0j62......r..
...0j26......r..
..052..6....r...
.pd52...6i.r....
..052...i6.r....
.052......r.....
.052....rr......
.0c2...r........
.0ed.rr.........
mpomr...........
monm............
"""),
    "bow_pulling_1": G("""
..............qq
.t.......t0000ts
..s....00d5c5fsr
...6.00555222eqq
....6kj222....s.
...0j62.......s.
...0j26......s..
..052..6.....s..
.pd52...6...s...
..052....6i.s...
.052.....i6s....
.052......ss....
.0c2....ss......
.0fe..ss........
mpomss..........
monm............
"""),
    "bow_pulling_2": G("""
q.............qq
.t.......t0000ts
..s....00d5c5fsr
...6.00555222eqq
....6kj222....t.
...0j62.......t.
...0j26.......t.
..052..6.....t..
.pd52...6....t..
..052....6...t..
.052......6it...
.052......i6t...
.0c2......ttt...
.0fe...ttt......
mpomttt.........
monm............
"""),
}
FINAL.update(BOW3)
PHASES.update({
    "bow": PH("""
................
.........6....99
.........6.6.899
.............8..
..............7.
.............6..
............6...
...........5....
.22.......5.....
.........4......
........4.......
.......3........
..1...3.........
..11.2..........
.00.2...........
.00............."""),
    "bow_pulling_0": PH("""
................
.........8....99
..A......8.8.899
.............8..
..............7.
.............7..
.............7..
............6...
.22........6....
...........5....
..........5.....
........44......
..1....3........
..11.23.........
.00.2...........
.00............."""),
    "bow_pulling_1": PH("""
................
.A.......8....99
..A......8.8.899
.............8..
..............7.
..............7.
.............7..
.............7..
.22.........6...
............6...
...........6....
..........55....
..1.....44......
..11..33........
.00.22..........
.00............."""),
    "bow_pulling_2": PH("""
................
.A.......8....99
..A......8.8.899
.............8..
..............7.
..............7.
..............7.
.............6..
.22..........6..
.............6..
............5...
............5...
..1.......444...
..11...333......
.00.222.........
.00............."""),
})

# Ritocchi: spada B+C con il contorno sinistro della guardia spostato di un pixel (riga 11: x=2 -> x=3);
# arco teso ridisegnato: corda scura e sottile (solo la cocca si schiarisce al massimo), freccia CMY
# (asta di citrino, punta di topazio, impennaggio d'ametista), flettenti che si chiudono da pulling_0 a pulling_2.
PREV_BC_SWORD = BC_SWORD
BC_SWORD = BC_SWORD[:11] + ["...043afoma0...."] + BC_SWORD[12:]
assert len(BC_SWORD[11]) == 16
PREV_BOW = {n: FINAL[n] for n in ("bow_pulling_0", "bow_pulling_1", "bow_pulling_2")}
BOW4 = {
    "bow_pulling_0": G("""
................
.q.......t00..qq
..ts...00d5c00ts
..sw.00555225esr
....wkj222..2dqq
...0jw2.......r.
...0j2w......r..
..052..wo....r..
.pd52..owp..r...
..052...pw.r....
.052......r.....
.052.....r......
..0c2...r.......
..0ed.rr........
.mpomr..........
.monm...........
"""),
    "bow_pulling_1": G("""
q...............
.ts......t......
.sw....00d00..qq
...w.005555c00ts
....wkj222225esr
...0jw2.....2dqq
...0j2w.......r.
..052..w.....r..
.pd52...wo...r..
..052...owp.r...
..052....pw.r...
..052......r....
...0c2...rr.....
...0ed.rr.......
..mpomr.........
..monm..........
"""),
    "bow_pulling_2": G("""
q...............
.ts......t......
.sw....00d0.....
...w.0055550..qq
....wkj2222c00ts
...0jw2....25esr
...0j2w.....2dqq
..052..w......r.
.pd52...w.....r.
..052....wo..r..
..052....owp.r..
...052....pwr...
....0c2....rs...
....0ed..rr.....
...mpomrr.......
...monm.........
"""),
}
FINAL.update({"sword": BC_SWORD, **BOW4})
PHASES.update({
    "bow_pulling_0": PH("""
................
.........7......
..AA.....7.7..99
..A9.........899
....9........8..
.....8........7.
......8......6..
.......75....6..
.22....575..5...
........56.5....
..........4.....
.........4......
...1....3.......
...11.23........
..00.2..........
..00............"""),
    "bow_pulling_1": PH("""
................
.AA......7......
.A9......7......
...9.......7..99
....9........899
.....8.......8..
......8.......7.
.......7.....6..
.22.....75...6..
........565.5...
.........56.5...
...........4....
....1....34.....
....11.23.......
...00.2.........
...00..........."""),
    "bow_pulling_2": PH("""
................
.AA......7......
.A9......7......
...9............
....9......7..99
.....8.......899
......8......8..
.......7......7.
.22.....7.....6.
.........65..6..
.........565.5..
..........565...
.....1.....44...
.....11..33.....
....00.22.......
....00.........."""),
})

# Ritocchi 2: spada senza il pixel isolato della striscia di pergamena (6,13); arco teso con chiusura moderata:
# tensione 0 a flettenti fermi, tensione 1 e 2 con il tratto esterno e le punte chiusi di un solo pixel.
PREV2_BC_SWORD = BC_SWORD
BC_SWORD = BC_SWORD[:13] + ["..gsg..........."] + BC_SWORD[14:]
PREV2_BOW = {n: FINAL[n] for n in ("bow_pulling_0", "bow_pulling_1", "bow_pulling_2")}
BOW5 = {
    "bow_pulling_0": G("""
..............qq
.q.......t0000ts
..ts...00d5c5esr
..sw.00555222dqq
....wkj222....r.
...0jw2......r..
...0j2w......r..
..052..wo...r...
.pd52..owp.r....
..052...pw.r....
.052......r.....
.052....rr......
.0c2...r........
.0ed.rr.........
mpomr...........
monm............
"""),
    "bow_pulling_1": G("""
q...............
.ts......t00..qq
.sw....00d5c00ts
...w.00555225esr
....wkj222..2dqq
...0jw2.......r.
...0j2w.......r.
..052..w.....r..
.pd52...wo...r..
..052...owp.r...
.052.....pw.r...
.052.......r....
..0c2....rr.....
..0ed..rr.......
.mpomrr.........
.monm...........
"""),
    "bow_pulling_2": G("""
q...............
.ts......t00..qq
.sw....00d5c00ts
...w.00555225esr
....wkj222..2dqq
...0jw2.......r.
...0j2w.......r.
..052..w......r.
.pd52...w....r..
..052....wo..r..
.052.....owp.r..
.052......pwr...
..0c2......rs...
..0ed...rrr.....
.mpomrrr........
.monm...........
"""),
}
FINAL.update({"sword": BC_SWORD, **BOW5})
PHASES.update({
    "bow_pulling_0": PH("""
................
.........7....99
..AA.....7.7.899
..A9.........8..
....9.........7.
.....8.......7..
......8......6..
.......75...6...
.22....575.5....
........56.5....
..........4.....
........44......
..1....3........
..11.23.........
.00.2...........
.00............."""),
    "bow_pulling_1": PH("""
................
.AA......7......
.A9......7.7..99
...9.........899
....9........8..
.....8........7.
......8.......7.
.......7.....6..
.22.....75...6..
........565.5...
.........56.5...
...........4....
...1.....44.....
...11..33.......
..00.22.........
..00............"""),
    "bow_pulling_2": PH("""
................
.AA......7......
.A9......7.7..99
...9.........899
....9........8..
.....8........7.
......8.......7.
.......7......6.
.22.....7....6..
.........65..6..
.........565.5..
..........565...
...1.......44...
...11...333.....
..00.222........
..00............"""),
})

# Stivali avvicinati: un pixel di spazio tra i due (come le bedrock_boots e gli stivali vanilla), paio ricentrato.
PREV_BOOTS = FINAL["boots"]
NEW_BOOTS = G("""
................
................
................
..00000.00000...
..05540.05540...
..gkjjg.04430...
..hiSih.05320...
..04320h60606x..
..04310g04310v..
.043210.033210..
.aed430.043320..
.adc320.032210..
.aa0000.000000..
................
................
................
""")
FINAL["boots"] = NEW_BOOTS

# Stivali, via di mezzo: due pixel trasparenti tra i due (lontani: 4, vicini: 1), paio ricentrato.
PREV2_BOOTS = FINAL["boots"]
MID_BOOTS = G("""
................
................
................
..00000..00000..
..05540..05540..
..gkjjg..04430..
..hiSih..05320..
..04320h.60606x.
..04310g.04310v.
.043210..033210.
.aed430..043320.
.adc320..032210.
.aa0000..000000.
................
................
................
""")
FINAL["boots"] = MID_BOOTS

# Elmo: apertura a T più ampia, come proporzioni nel bedrock helmet (disegno nostro): fessura degli occhi su due
# righe, tratto verticale largo quattro pixel che si allarga a sei e si apre in basso. Vuoto scuro opaco, aperto sul fondo.
PREV_HELMET = FINAL["helmet"]
NEW_HELMET = G("""
................
................
....00000000....
...0566554430...
..05655adeeda06.
..05554afomfa03.
.gk44443acdca06.
.hj05c000004303.
.iS05000000430x.
.jh05400004320w.
.iY04300003210u.
.hg032....210...
..hg............
................
................
................
""")
FINAL["helmet"] = NEW_HELMET
PREV_HEAD_NORTH = FACES[("head_out", "north")]
FACES[("head_out", "north")] = "5666555443 556adeeda3 455afomfa3 4544acdca2 40c0000002 5400000043 5430000403 4430000332 432....321 32......21".split()

# Elmo, ritocco: tratto verticale della T largo tre pixel e guance che scendono ai lati fino in fondo.
LAST_HELMET = FINAL["helmet"]
LAST_HEAD_NORTH = FACES[("head_out", "north")]
FINAL["helmet"] = G("""
................
................
....00000000....
...0566554430...
..05655adeeda06.
..05554afomfa03.
.gk44443acdca06.
.hj05c000004303.
.iS05000000430x.
.jh05400044320w.
.iY04300033210u.
.hg043...32110..
.hg021...2110...
..h000...0000...
................
................
""")
FACES[("head_out", "north")] = "5666555443 556adeeda3 455afomfa3 4544acdca2 40c0000002 5400000043 5430004403 4430003332 432...3321 321...2211".split()

# Elmo, apertura identica al bedrock helmet (solo la sagoma del vuoto; metallo, ombre e ornamenti sono nostri).
# Icona: vuoto scuro opaco come nel bedrock helmet (fessura di 8 px a y7, x4-11; tratto verticale di 2 px, x7-8, da y7
# fino all'ultima riga y11 compresa: elmo aperto in basso, bordo inferiore x3-6 e x9-11). Indossato: come nel bedrock indossato la faccia frontale esterna è aperta tranne le guance, e la
# piastra interna (head_in) ha la T trasparente (fessura di 8 px, come nell'icona, e tratto verticale di 1 px); l'occhio-gemma passa lì.
LAST2_HELMET = FINAL["helmet"]
LAST2_HEAD_NORTH = FACES[("head_out", "north")]
FINAL["helmet"] = G("""
................
................
....00000000....
...0566554430...
..05655adeeda06.
..05554afomfa03.
.gk44443acdca06.
.hj410000001203.
.iS45440043320x.
.jh34430033210w.
.iY2d32002c110u.
.hg0333003330...
..hg............
................
................
................
""")
FACES[("head_out", "north")] = ["." * 10] * 5 + ["4........2", "543....332", "4433..3321", "4dc3..3221", "." * 10]
FACES[("head_in", "north")] = "455adeeda 454afomfa 4443acdca 432212234 4........ 4433.3221 4332.3221 3322.2211 3221.2110".split()

# Armatura indossata: meno pergamena. Mantello e perizoma diventano cotta di maglia; resta un solo talismano
# stretto e corto sul mantello (con il sigillo-occhio sotto, sulla maglia) e un cartellino sul perizoma.
# Catena diagonale della corazza a maglie doppie; niente talismano dietro la corazza; stivale destro senza pergamena.
FACES.update({
    ("body_out", "north"): "5665005665 5e54adeeda 4c65afomfa 44403acdca d434653322 e433303322 d333336522 pd43333033 oc33333365 m3322222x1".split(),
    ("body_out", "south"): "4555555544 4554554433 4444444332 443d433322 43ded33322 433d333221 3322322221 3332322211 3322222111 2221111110".split(),
    ("cape", "south"): """34444444443 23565656532 44344434443 434gjkjg434 343gSjOg343 434giSig434 343gjYjg343
        434gOiSg434 343gijig343 434gYjOg434 343gihig343 433hghgh334 3433g3g3343 33233323332 32adeeda232
        33afomfa323 323acdca233 33323332333 32333233323 33323332333 22232223222 21222122212 10101010101""".split(),
    ("cape", "north"): """23333333332 22212221222 21222122212 22212221222 21222122212 22212221222 21222122212
        22212221222 21222122212 22212221222 21222122212 22212221222 21222122212 22212221222 21222122212
        22212221222 21222122212 22212221222 21222122212 22212221222 21222122212 11121112111 10101010101""".split(),
    ("cape", "west"): list("32222222222222222222220"),
    ("cape", "east"): list("32222222222222222222220"),
    ("cape", "top"): ["34444444443"], ("cape", "bottom"): ["10101010101"],
    ("front_cloth", "north"): "3444443 4565654 3343433 34gkg33 33jOj43 34iSi33 33jYj43 34hgh33 3334333 3433343 3334333 2232322 2223222 0101010".split(),
    ("front_cloth", "south"): "2333332 2212122 2122212 2212122 2122212 2212122 2122212 2212122 2122212 2212122 2122212 2212122 1112111 0101010".split(),
    ("front_cloth", "west"): list("32222222222220"),
    ("front_cloth", "east"): list("32222222222220"),
    ("front_cloth", "top"): ["3444443"], ("front_cloth", "bottom"): ["0101010"],
    ("right_boot", "north"): "acdca aed32 00000".split(),
    ("right_boot", "south"): "acdca 33222 00000".split(),
    ("right_boot", "west"): "acdca 43322 00000".split(),
    ("right_boot", "east"): "acdca 33222 00000".split(),
})

# ===========================================================================
# ARMATURA INDOSSATA, giro dopo il controllo in gioco (screenshot texture_t6/65 e 69):
# - niente pergamena sull'armatura indossata: perizoma e mantello diventano metallo e catene con un medaglione
#   a occhio-gemma; anche la striscia sull'elmo, la fascia sul braccio destro e il nastro sulla coscia diventano catena.
#   Le icone 16x16 restano come sono (approvate).
# - mantello: nel modello di Spectrum il cubo del mantello è girato di 180 gradi, quindi da dietro si vede la faccia
#   che qui si chiama "north" e la "south" resta verso il corpo. La schiena decorata va su "north"; il manichino
#   da dietro mostra "north" del mantello (come in gioco). Il perizoma invece mostra davanti la sua "north" (in gioco
#   si vedeva il cartellino disegnato lì): mappatura giusta.
# ===========================================================================
PREV_WORN_FACES = dict(FACES)
FACES.update({
    ("cape", "north"): """34444444443 23565656532 44344434443 43434643434 34343034343 43434643434 34343034343
        433adeeda34 343afomfa43 4343acdca34 34343634343 43434043434 33333s33333 32323232323 33233323332
        32333233323 33323332333 32333233323 22565656522 22232223222 21222122212 11121112111 10101010101""".split(),
    ("cape", "south"): PREV_WORN_FACES[("cape", "north")],
    ("front_cloth", "north"): "3444443 4565654 3443443 3adeda3 3afoma2 3acdca2 4565654 3443442 3433432 3343332 3443432 2333322 2232221 0101010".split(),
    ("head_out", "west"): "5555444433 5544443322 4444336322 4433330321 0000006000 4333220321 4332216211 3222110210 322110p110 2111000n00".split(),
    ("right_arm", "north"): "55554 54443 44433 44333 43332 33322 60606 33222 06060 33222 aedca acdba 22111".split(),
    ("right_arm", "south"): "55444 44433 44333 43332 33322 33222 06060 32222 60606 32222 aedca acdba 22111".split(),
    ("right_arm", "west"): "55554 54443 44433 44333 43332 33322 60606 33222 06060 33222 aedca acdba 22111".split(),
    ("right_arm", "east"): "44443 44333 43332 33322 33222 32222 06060 32222 60606 22211 acdca abcba 11111".split(),
    ("right_leg", "north"): "44433 60606 43332 06060 43332 54443 43332 33222 32221 22111".split(),
    ("right_leg", "south"): "44333 06060 33322 60606 33322 44333 33322 32221 22211 21111".split(),
    ("right_leg", "west"): "44433 60606 43332 06060 43332 44433 33322 32221 22211 21111".split(),
    ("right_leg", "east"): "44333 06060 33322 60606 33322 44332 33222 32211 22111 11111".split(),
})
assert not any(ch in "ghijkSOY" for g in FACES.values() for row in g for ch in row), "pergamena rimasta nell'armatura indossata"

# ===========================================================================
# ICONE = RIFERIMENTO. Le quattro icone dell'armatura perdono solo la pergamena (al suo posto metallo, catena o
# una borchia d'ametista); il resto del disegno approvato non cambia. L'armatura indossata è riallineata alle icone:
# visto di fronte, il lato sinistro dell'icona è il lato destro del personaggio (cubi right_*, faccia west della testa).
# ===========================================================================
PREV_ARMOR_ICONS = {n: FINAL[n] for n in ("helmet", "chestplate", "leggings", "boots")}
FINAL["helmet"] = G("""
................
................
....00000000....
...0566554430...
..05655adeeda06.
..05554afomfa03.
.6044443acdca06.
.30410000001203.
.6045440043320x.
.3034430033210w.
.p02d32002c110u.
.n00333003330...
.m..............
................
................
................
""")
FINAL["chestplate"] = G("""
................
.00000....00000.
0566540..0566540
05e5430000554430
04c6445adeeda320
0444534afomfa410
.0404063acdca030
.03044353322020.
.02043330621010.
..004332225100..
...0433222100.m.
...0ae01110x0mpm
...054430432dpnm
...044320321cmm.
...033210000....
...000..........
""")
FINAL["leggings"] = G("""
................
................
..000000000000..
..055544433210e.
..0443332221106.
..0543000032100.
..06360..042106.
.603630..043100.
.304310..aefdax.
.603320..acdbaw.
.p04310..03210u.
..03210..03210..
..03210..02110..
..00000..00000..
................
................
""")
FINAL["boots"] = G("""
................
................
................
..00000..00000..
..05540..05540..
..05p40..04430..
..04n30..05320..
..04320..60606x.
..04310..04310v.
.043210..033210.
.aed430..043320.
.adc320..032210.
.aa0000..000000.
................
................
................
""")
assert not any(ch in "ghijkSOY" for n in PREV_ARMOR_ICONS for row in FINAL[n] for ch in row), "pergamena rimasta nelle icone"

PREV_WORN2 = dict(FACES)
FACES.update({
    # corazza come l'icona: occhio in alto a destra, catena dalla spalla sinistra con giunti scuri e citrino in fondo,
    # scheggia d'ametista sul fianco destro (in vista frontale), fibbia d'oro a sinistra sulla cintura (body_in).
    # Le colonne 0-1 e 8-9 della corazza stanno dietro le braccia: scheggia e pendente sono nelle colonne visibili.
    ("body_out", "north"): "5665005665 5e54adeeda 4c65afomfa 44403acdca 4434653322 4433303322 43333x3m22 33333vdp22 332222co21 2221111m11".split(),
    # gambali: catena attorno alla coscia destra del personaggio con pendente d'ametista sul lato esterno;
    # ginocchiera accesa e catena col citrino sulla gamba sinistra (già così)
    ("right_leg", "west"): "44433 60606 43332 06060 p3332 n4433 33322 32221 22211 21111".split(),
    # stivali: destro con borchia d'ametista e puntale acceso, sinistro con catena e citrino sul lato esterno
    ("right_boot", "north"): "44p43 aed32 00000".split(),
    ("right_boot", "south"): "44n33 33222 00000".split(),
    ("right_boot", "west"): "44433 43322 00000".split(),
    ("right_boot", "east"): "44433 33222 00000".split(),
    ("left_boot", "east"): "0606x 33222 00000".split(),
})
# Di fronte il perizoma copre le gambe (nel modello pende davanti a entrambe): porta i dettagli dell'icona dei gambali
# sugli stessi lati (banda di catena con ametista a sinistra, ginocchiera accesa a destra) e, in fondo, quelli degli
# stivali che copre (borchia d'ametista e puntale acceso a sinistra, catena col citrino a destra). Le catene dell'elmo stanno
# sui lati del cubo, invisibili di fronte: le ripeto sul bordo delle guance (ametista a sinistra, citrino a destra).
FACES.update({
    ("front_cloth", "north"): "3444443 4565654 4433443 6364443 3634433 p433aed n332acb 3332332 3322322 3322321 3222221 4p4360x aed2332 0000000".split(),
    ("head_out", "north"): ["." * 10] * 5 + ["6........6", "343....330", "6433..3326", "pdc3..322x", "." * 10],
})

# Niente mantello. Verifica nel jar di Spectrum 1.12.9 (de/dafuqs/spectrum/render/armor/BedrockCapeRenderer$BedrockCapeLayer):
# mantello e perizoma si disegnano per ogni corazza `instanceof BedrockArmorItem` (SAArmorItem lo è) con
# RenderType.entitySolid sulla stessa texture dell'armatura, quindi la trasparenza non è supportata. Il mantello pende
# dietro al busto e alle gambe: la sua faccia visibile da dietro ("north", cubo girato di 180 gradi) è dipinta come la
# schiena della corazza (piastre, giuntura, medaglione a occhio senza catena), la cintura, il retro delle due gambe e
# degli stivali, con le stesse catene e gemme sullo stesso lato del personaggio (colonna 0 = sinistra del personaggio,
# vista da dietro). Niente più maglia a pixel alternati, né sul mantello né sul retro del perizoma.
PREV_WORN3 = dict(FACES)
FACES.update({
    ("cape", "north"): """34555555443 45566655433 45555554432 44445444332 22221222211 44445444332 4adeeda4322
        4afomfa3322 43acdca3221 33333333221 00000000000 11111111111 04443060600 04433033320 04433006060
        x2222022220 w4333043330 03332033320 03322033220 03222032220 02221022210 06060043320 00000000000""".split(),
    ("cape", "south"): ["23333333332"] + ["12222222221"] * 21 + ["11111111111"],
    ("cape", "bottom"): ["11111111111"],
    ("front_cloth", "south"): ["2333332"] + ["1222221"] * 12 + ["1111111"],
    ("front_cloth", "bottom"): ["1111111"],
})


# ===========================================================================
# MANTELLO E PERIZOMA: PROPOSTE (solo pagina e design/.../mantello/; il set installato non cambia).
# Verifica nel jar di Spectrum 1.12.9: BedrockCapeRenderer$BedrockCapeLayer.render disegna perizoma (FRONT_CLOTH) e
# mantello (CAPE_MODEL) con RenderType.entitySolid per ogni corazza BedrockArmorItem; per i giocatori li anima con la
# fisica del mantello vanilla (BedrockArmorModel.computeFrontClothRotation legge xCloak/yCloak/zCloak, yBodyRot e bob):
# il perizoma ruota tra -25 e 0 gradi, il mantello si alza all'indietro fino a 45 gradi (con un'inclinazione laterale)
# ed è girato di 180 gradi; per le altre entità restano nella posa di riposo.
# ===========================================================================
# Facce comuni alle opzioni senza mantello: retro e fronte di gambe e stivali disegnati come le icone, schiena della
# corazza con il medaglione a occhio.
NUDE = {
    ("right_leg", "north"): "44433 60606 43332 06060 p3332 n4443 43332 33222 32221 22111".split(),
    ("left_leg", "north"): "44433 44336 43330 33326 adeda acfdx 3322w 33221 32211 22111".split(),
    ("right_leg", "south"): "44333 06060 33322 60606 3332p 4433n 33322 32221 22211 21111".split(),
    ("left_leg", "south"): "44333 63332 03322 63222 x2222 w3221 32221 32211 22111 21111".split(),
    ("body_out", "south"): "4555555544 4554554433 4444444332 4adeeda322 4afomfa321 43acdca321 3322322221 3332322211 3322222111 2221111110".split(),
}
# Opzione 3: mantello vero a lamelle di metallo (piastre orizzontali con giunture, fermaglio d'oro, medaglione, orlo)
CAPE_REAL = """34444444443 aed45656543 45555555443 4adeeda5432 4afomfa4432 44acdca4332 22222222211 45555554432
    44444443332 22222222211 45555544332 44444433322 22222222211 44555443322 44444333222 22222222111 44454433221
    43444332221 22222221111 43443322211 33332222111 21122d22112 11111111111""".split()
# Opzione 5: tabarro corto davanti e dietro (servono trasparenze: rendering cutout via codice)
TABARD_BACK = """34444444443 aed45656543 45555555443 4adeeda5432 4afomfa4432 44acdca4332 45555554432 44444443332
    33333333221 2d222222d12 .122222221. ..1222221.. ....111....""".split() + ["." * 11] * 10
TABARD_FRONT = "3444443 4565654 4433443 6364443 3634433 p433aed n332acb 3332332 3322322 .33233. ..323.. ....... ....... .......".split()


def faces_with(extra):
    f = dict(FACES)
    f.update(extra)
    return f


PROPOSTE = [
    {"n": 1, "nome": "Niente mantello e niente perizoma", "codice": True, "cape": False, "cloth": False,
     "faces": faces_with(NUDE),
     "nota": "Serve codice lato client (non disegnare il layer del mantello per i nostri oggetti). Si vedono davvero corazza, "
             "gambali e stivali, davanti e dietro, come nelle icone. Camminando non si muove niente oltre al corpo."},
    {"n": 2, "nome": "Solo il perizoma", "codice": True, "cape": False, "cloth": True,
     "faces": faces_with(NUDE),
     "nota": "Serve codice lato client (togliere solo il mantello). Davanti il perizoma come ora, dietro schiena e gambe vere. "
             "Camminando il perizoma oscilla in avanti fino a 25 gradi."},
    {"n": 3, "nome": "Mantello vero a lamelle", "codice": False, "cape": True, "cloth": True,
     "faces": faces_with({("cape", "north"): CAPE_REAL}),
     "nota": "Nessun codice. Il mantello resta lungo fino ai piedi (il cubo è fisso e senza trasparenze, non si può accorciare), "
             "ma è chiaramente un mantello: lamelle di metallo, fermaglio, medaglione a occhio, orlo. Camminando si alza fino a 45 gradi, come deve fare un mantello."},
    {"n": 4, "nome": "Trucco dipinto (stato attuale)", "codice": False, "cape": True, "cloth": True,
     "faces": dict(FACES),
     "nota": "Nessun codice. Da fermo sembra che il mantello non ci sia; ma per i giocatori il mantello si muove con la fisica del mantello "
             "vanilla: camminando e correndo si stacca e si alza fino a 45 gradi, e la «schiena dipinta» si apre come una porta."},
    {"n": 5, "nome": "Tabarro corto davanti e dietro", "codice": True, "cape": True, "cloth": True,
     "faces": faces_with({**NUDE, ("cape", "north"): TABARD_BACK, ("front_cloth", "north"): TABARD_FRONT}),
     "nota": "Serve codice lato client (disegnare mantello e perizoma con un render type che ammette trasparenze, es. cutout, "
             "invece di entitySolid). Due falde di metallo che finiscono a punta sopra le ginocchia; sotto si vedono gambali e stivali veri. "
             "Camminando le falde oscillano, ma essendo corte non si staccano in modo vistoso."},
]

# Scelta dell'utente: proposta 3, mantello vero a lamelle (nessun codice). Diventa il set definitivo.
PREV_WORN4 = dict(FACES)
FACES[("cape", "north")] = CAPE_REAL

# Vista laterale semplice (dal fianco sinistro del personaggio, davanti a sinistra): facce "east" dei cubi del lato
# sinistro e dei cubi centrali, alla loro profondità; mantello e perizoma come lamine sottili, anche inclinati come
# in corsa (mantello 45 gradi all'indietro, perizoma 25 in avanti, i massimi del codice di Spectrum).
SIDE = [("left_leg", 2.5, 11.85), ("left_boot", 2.5, 21.15), ("body_in", 2.5, -0.5), ("body_out", 1.5, 0),
        ("left_arm", 2.5, -0.5), ("head_in", 4.5, -8.5), ("head_out", 5, -9)]


def side_view(tex, scale, cape=True, cloth=True, running=False):
    W, Hh = 24, 36
    out = Image.new("RGBA", (W, Hh), (0, 0, 0, 0))
    cx = 10  # z = 0
    for cube, half, my in SIDE:
        fx, fy, fw, fh = cube_faces(*CUBES[cube])["east"]
        out.alpha_composite(tex.crop((fx, fy, fx + fw, fy + fh)), (int(round(cx - half)), int(round(my + 9.5))))
    for cube, show, z, ang in (("cape", cape, 3.0, 45), ("front_cloth", cloth, -3.5, -25)):
        if not show:
            continue
        fx, fy, fw, fh = cube_faces(*CUBES[cube])["north"]
        strip = tex.crop((fx + fw // 2, fy, fx + fw // 2 + 1, fy + fh))  # sezione centrale della lamina
        lay = Image.new("RGBA", (W, Hh), (0, 0, 0, 0))
        top = 0.5 if cube == "cape" else 9.65
        lay.alpha_composite(strip, (int(round(cx + z)), int(round(top + 9.5))))
        if running:
            lay = lay.rotate(ang, resample=Image.NEAREST, center=(cx + z, top + 9.5))
        out.alpha_composite(lay)
    return up(out, scale)




# ---------------------------------------------------------------------------
# Conversione e composizione
# ---------------------------------------------------------------------------
render = H.render
up = H.up
on_bg = H.on_bg


def ref(name):
    return Image.open(os.path.join(REF, f"bedrock_{name}.png")).convert("RGBA")


def icon_frames(name):
    g = FINAL[name]
    if name in PHASES:
        return [render(fr, PAL) for fr in wave_frames(g, PHASES[name])]
    return [render(fr, PAL) for fr in ink_frames(g)]


def save_anim(frames, base):
    strip = Image.new("RGBA", (frames[0].width, frames[0].height * FRAMES), (0, 0, 0, 0))
    for i, fr in enumerate(frames):
        strip.alpha_composite(fr, (0, fr.height * i))
    strip.save(base + ".png")
    with open(base + ".png.mcmeta", "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": %d,\n    "interpolate": false\n  }\n}\n' % FRAMETIME)


def gif_bytes(frames, scale=1, bg=H.SLOT):
    imgs = [on_bg(up(fr, scale), bg).convert("RGB") for fr in frames]
    buf = io.BytesIO()
    imgs[0].save(buf, format="GIF", save_all=True, append_images=imgs[1:], duration=FRAMETIME * 50, loop=0)
    return buf.getvalue()


def confronto(d):
    """Autocritica: per ogni pezzo nuovo la bedrock sopra e il nostro sotto, x12."""
    k, pad = 12, 8
    names = ORDER
    W = len(names) * (16 * k + pad) + pad
    sheet = Image.new("RGBA", (W, 2 * (16 * k + pad) + pad + 40), (24, 22, 30, 255))
    for i, n in enumerate(names):
        x = pad + i * (16 * k + pad)
        sheet.alpha_composite(on_bg(up(ref(n), k)), (x, pad))
        im = render(FINAL[n], PAL)
        sheet.alpha_composite(on_bg(up(im, k)), (x, 16 * k + 2 * pad))
        sheet.alpha_composite(on_bg(im), (x, 2 * (16 * k + pad) + 12))
        sheet.alpha_composite(on_bg(up(im, 2)), (x + 24, 2 * (16 * k + pad) + 4))
    sheet.save(os.path.join(d, "confronto_mix.png"))


def variants_rows():
    rows = [("prima", "Stili 3 e 5 come scelti", {it: render((S3 if it in ("sword", "axe") else S5)[it],
                                                            PAL3 if it in ("sword", "axe") else PAL5) for it in SET6})]
    for v in VARIANTS:
        rows.append((v["lett"], v["nome"], {it: render(v["grids"][it], PAL) for it in SET6}))
    return rows


def panoramica(path, tex, worn):
    from PIL import ImageDraw, ImageFont
    k, cell, lab = 6, 16 * 6 + 10, 170
    big, txt = ImageFont.load_default(size=26), ImageFont.load_default(size=14)
    vrows = variants_rows()
    W = max(lab + 6 * cell, 8 * cell + 8) + 16
    H1 = len(vrows) * cell
    H2 = 2 * cell + 40
    H3 = max(worn.height, 256) + 40
    sheet = Image.new("RGBA", (W, H1 + H2 + H3 + 40), (20, 18, 26, 255))
    dr = ImageDraw.Draw(sheet)
    for r, (a, b, sp) in enumerate(vrows):
        y = 8 + r * cell
        dr.text((12, y + 22), a, fill=(232, 151, 37), font=big)
        dr.text((12, y + 56), b, fill=(200, 196, 210), font=txt)
        for i, it in enumerate(SET6):
            sheet.alpha_composite(on_bg(up(sp[it], k)), (lab + i * cell, y))
    y0 = H1 + 30
    dr.text((12, y0 - 22), "Set definitivo proposto", fill=(232, 151, 37), font=txt)
    for i, n in enumerate(ORDER):
        sheet.alpha_composite(on_bg(up(render(FINAL[n], PAL), k)), (8 + (i % 8) * cell, y0 + (i // 8) * cell))
    y0 = H1 + H2 + 30
    dr.text((12, y0 - 22), "Armatura indossata (texture 128x128 e manichino)", fill=(232, 151, 37), font=txt)
    sheet.alpha_composite(on_bg(up(tex, 2), (60, 60, 70, 255)), (8, y0))
    sheet.alpha_composite(worn, (8 + 256 + 16, y0))
    sheet.save(path)


def b64(data, fmt="png"):
    if not isinstance(data, bytes):
        buf = io.BytesIO()
        data.save(buf, format="PNG")
        data = buf.getvalue()
    return f"data:image/{fmt};base64," + base64.b64encode(data).decode()


CSS = H.CSS + """
.items.wide { grid-template-columns: repeat(auto-fill, minmax(104px, 1fr)); }
.vrow { display: grid; grid-template-columns: 170px 1fr; gap: 12px 18px; align-items: center; padding-block: 12px;
  border-top: 1px solid var(--line); }
.vrow:first-of-type { border-top: 0; }
.vrow .who { display: grid; gap: 2px; min-width: 0; }
.vrow .who b { font-family: var(--display); font-size: 30px; line-height: 1; color: var(--accent); font-weight: 500; }
.vrow .who span { font-weight: 600; }
.vrow .who small { color: var(--muted); font-size: 13px; }
.vrow .who em { font-style: normal; font-size: 11px; letter-spacing: .08em; text-transform: uppercase; color: var(--accent); }
.vrow .items .slot { max-width: 112px; }
.worn { display: flex; flex-wrap: wrap; gap: 18px; align-items: flex-start; }
.worn .slot { padding: 10px; }
.worn .sheet img { width: 384px; height: 384px; max-width: 100%; height: auto; }
.items.pairs { grid-template-columns: repeat(auto-fill, minmax(96px, 112px)); }
.worn .man img { width: 336px; height: auto; max-width: 100%; }
@media (max-width: 720px) { .vrow { grid-template-columns: 1fr; } }
"""


def html(path, anims, tex, worn_gif, tex_old):
    def img(im, alt):
        return f'<img src="{b64(im)}" alt="{alt}">'

    def fig(im, cap):
        return f'<figure><div class="slot">{img(im, cap)}</div><figcaption>{cap}</figcaption></figure>'

    refs6 = {it: ref(it) for it in SET6}
    sp = {n: render(FINAL[n], PAL) for n in ORDER}
    # Icone e armatura indossata: per ogni pezzo l'icona x12 accanto alla stessa zona del manichino (davanti, dietro)
    man1 = mannequin(tex, 1)
    zone = {"helmet": (0, 11), "chestplate": (9, 22), "leggings": (19, 31), "boots": (29, 35)}
    match_rows = []
    for n, (y0, y1) in zone.items():
        front = on_bg(up(man1.crop((0, y0, 20, y1)), 10), (60, 60, 70, 255))
        back = on_bg(up(man1.crop((22, y0, 42, y1)), 10), (60, 60, 70, 255))
        match_rows.append(f"""
    <div class="vrow"><div class="who"><span>{NOMI[n].capitalize()}</span></div>
      <div class="lower"><div class="block"><h3>Icona ×12</h3><div class="slot" style="max-width:200px">{img(on_bg(up(render(FINAL[n], PAL), 12)), NOMI[n])}</div></div>
      <div class="block"><h3>Indossata, davanti</h3><div class="slot man">{img(front, NOMI[n] + " davanti")}</div></div>
      <div class="block"><h3>Indossata, dietro</h3><div class="slot man">{img(back, NOMI[n] + " dietro")}</div></div></div></div>""")
    # Mantello e perizoma: proposte (manichino davanti/dietro e vista laterale ferma / in corsa, stessa scala)
    bgc = (60, 60, 70, 255)
    prop_rows = []
    for pr in PROPOSTE:
        t = render_big(armor_grid(pr["faces"], cutout=pr["n"] == 5))
        man = on_bg(mannequin(t, 6, pr["cape"], pr["cloth"]), bgc)
        s1 = on_bg(side_view(t, 6, pr["cape"], pr["cloth"]), bgc)
        s2 = on_bg(side_view(t, 6, pr["cape"], pr["cloth"], running=True), bgc)
        tag = ("serve codice lato client" if pr["codice"] else "nessun codice") + (" · scelta" if pr["n"] == 3 else "")
        prop_rows.append(f"""
    <div class="vrow"><div class="who"><b>{pr["n"]}</b><span>{pr["nome"]}</span><em>{tag}</em><small>{pr["nota"]}</small></div>
      <div class="lower"><div class="block"><h3>Davanti e dietro</h3><div class="slot man">{img(man, pr["nome"] + " davanti e dietro")}</div></div>
      <div class="block"><h3>Di lato, fermo</h3><div class="slot man">{img(s1, pr["nome"] + " di lato")}</div></div>
      <div class="block"><h3>Di lato, in corsa</h3><div class="slot man">{img(s2, pr["nome"] + " di lato in corsa")}</div></div></div></div>""")
    cape_html = f"""
<section class="style best" id="mantello">
  <div class="head"><div class="num">M</div><h2>Mantello e perizoma: proposte</h2>
  <p>Mantello e perizoma sono un layer separato di Spectrum (<code>BedrockCapeRenderer</code>): si disegnano sempre per le corazze
  derivate dalla bedrock, senza trasparenze (<code>RenderType.entitySolid</code>), e per i giocatori si muovono con la fisica del mantello vanilla
  (perizoma fino a 25 gradi in avanti, mantello fino a 45 gradi all'indietro). Toglierli o renderli con trasparenze richiede codice lato client.
  Scelta: proposta 3, mantello vero a lamelle, ora nel set definitivo e installata. Vista laterale semplice: fianco sinistro, davanti a sinistra; «in corsa» mostra le inclinazioni massime.</p></div>
  {"".join(prop_rows)}
</section>"""

    match_html = f"""
<section class="style best" id="confronto">
  <div class="head"><div class="num">0</div><h2>Icone e armatura indossata</h2>
  <p>Le icone sono il riferimento: l'armatura indossata riprende gli stessi elementi negli stessi punti e con la stessa asimmetria, visto il giocatore di fronte.
  Di fronte il perizoma copre gambe e stivali, quindi porta lui i loro dettagli (catena con ametista a sinistra, ginocchiera e citrino a destra);
  le catene dell'elmo si vedono sul bordo delle guance. Dietro c'è il mantello a lamelle di metallo con il medaglione a occhio (proposta 3). Il manichino mostra le facce come appaiono in gioco.</p></div>
  {"".join(match_rows)}
</section>"""

    parts = [f"""<title>Trichromatic, set misto</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&family=Pixelify+Sans:wght@500;600&display=swap">
<style>{CSS}</style>
<header class="intro"><div class="wrap">
  <h1>Trichromatic, set misto <span class="tri" aria-hidden="true"><i style="background:var(--c)"></i><i style="background:var(--m)"></i><i style="background:var(--y)"></i></span></h1>
  <p class="lead">Armi e attrezzi dallo stile 3 (<b>Cristallo Scheggiato</b>), armatura dallo stile 5 (<b>Reliquia Rituale</b>), armonizzati con la variante C.
  In cima il <b>set definitivo proposto</b>; sotto, cosa è cambiato rispetto al giro precedente e, in fondo, le tre varianti di armonizzazione.
  Ogni sprite e ogni faccia dell'armatura è una griglia disegnata a mano.</p>
</div></header>
<div class="ref"><div class="wrap">
  <div class="lbl">Metro di paragone<b>Spectrum classico, bedrock</b></div>
  <div class="row">{"".join(f'<div class="slot">{img(refs6[it], "bedrock " + NOMI[it])}</div>' for it in SET6)}</div>
  <nav aria-label="Vai alla sezione"><a href="#mantello">M</a><a href="#confronto">0</a><a href="#set">1</a><a href="#indossata">2</a><a href="#animazioni">3</a><a href="#cambi">4</a><a href="#armonizzazione">5</a></nav>
</div></div>
<main class="wrap">{cape_html}{match_html}
<section class="style best" id="set">
  <div class="head"><div class="num">1</div><h2>Set definitivo proposto</h2>
  <p>Lame e punte di cristallo CMY che spuntano dal metallo, fratture arancio alla radice, impugnature fasciate di pergamena scritta, pomoli di citrino.
  La spada unisce l'occhio-gemma sulla guardia (B) alla fascia di pergamena (C), così parla con l'armatura senza staccarsi dagli altri attrezzi.
  L'arco carica la corda da tensione 0 a 2; la canna lanciata si piega e perde l'esca.</p></div>
  <div class="items wide">{"".join(fig(sp[n], NOMI[n]) for n in ORDER)}</div>
  <div class="lower">
    <div class="block"><h3>Dimensione reale 1×</h3><div class="real">{"".join(f'<div class="slot s1">{img(sp[n], NOMI[n])}</div>' for n in ORDER)}</div></div>
    <div class="block"><h3>2×</h3><div class="real">{"".join(f'<div class="slot s2">{img(sp[n], NOMI[n])}</div>' for n in ORDER)}</div></div>
    <div class="block"><h3>Bedrock di Spectrum, stessi pezzi, 2×</h3><div class="real">{"".join(f'<div class="slot s2">{img(ref(n), "bedrock " + NOMI[n])}</div>' for n in ORDER)}</div></div>
  </div>
</section>
<section class="style best" id="indossata">
  <div class="head"><div class="num">2</div><h2>Armatura indossata</h2>
  <p>Texture 128×128 nel layout del modello <code>BedrockArmorModel</code> di Spectrum, disegnata faccia per faccia. Niente pergamena: dietro pende un mantello
  di piastre e maglia con una catena e il medaglione a occhio-gemma; davanti un perizoma di metallo con lo stesso medaglione.
  Occhio sulla fronte spostato a destra, catena a maglie doppie in diagonale sul petto, catene al braccio, alla coscia e sul lato dell'elmo.</p></div>
  <div class="worn">
    <div class="block"><h3>Texture 128×128, 3×</h3><div class="slot sheet">{img(tex, "texture armatura 128x128")}</div></div>
    <div class="block"><h3>Manichino, fermo</h3><div class="slot man">{img(mannequin(tex, 8), "manichino davanti e dietro")}</div></div>
    <div class="block gif"><h3>Manichino, animato</h3><div class="slot man"><img src="{b64(worn_gif, 'gif')}" alt="manichino animato"></div></div>
  </div>
  <p class="note">Il manichino mostra le facce davanti e dietro dei cubi, senza prospettiva: la resa vera va vista in gioco.</p>
</section>
<section class="style best" id="animazioni">
  <div class="head"><div class="num">3</div><h2>Animazioni</h2>
  <p>12 fotogrammi da 5 tick, 3 secondi, interpolate false. Negli attrezzi una cresta di luce parte dal pomolo, accende le fratture e risale i cristalli;
  nell'arco corre da una punta all'altra lungo la corda, nella canna scende lungo la lenza fino all'esca, nella spada accende anche l'occhio della guardia.
  Nelle icone dell'armatura l'inchiostro scorre verso il basso da ciano a magenta a giallo; in tutta l'armatura l'iride degli occhi si accende una volta per ciclo.</p></div>
  <div class="items wide gif">{"".join(f'<figure><div class="slot"><img src="{b64(gif_bytes(anims[n]), "gif")}" alt="{NOMI[n]} animata"></div><figcaption>{NOMI[n]}</figcaption></figure>' for n in ORDER)}</div>
  <p class="note">Per l'armatura indossata servono 12 texture intere (t6_armor_0 … t6_armor_11) scelte in base al tempo di gioco, perché la texture del modello non passa dall'atlas.
  Con il movimento ridotto le GIF sono nascoste.</p>
</section>
<section class="style" id="cambi">
  <div class="head"><div class="num">4</div><h2>Cosa è cambiato</h2>
  <p>Prima a sinistra, dopo a destra.</p></div>"""]

    def pair(title, note, before, after):
        return f"""
  <div class="vrow"><div class="who"><span>{title}</span><small>{note}</small></div>
    <div class="lower"><div class="block"><h3>Prima</h3><div class="items pairs">{before}</div></div>
    <div class="block"><h3>Dopo</h3><div class="items pairs">{after}</div></div></div></div>"""

    parts.append(pair("Spada", "B aveva l'occhio ma non la pergamena degli altri attrezzi. Proposta: B+C, occhio e fascia insieme.",
                      fig(render(B_SWORD, PAL), "B") + fig(render(C_SWORD, PAL), "C"),
                      fig(sp["sword"], "B+C")))
    bows = ["bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2"]
    tesi = ["bow_pulling_0", "bow_pulling_1", "bow_pulling_2"]
    cols = [("Fermo", PREV_BOW), ("Molto piegato", PREV2_BOW), ("Nuovo", FINAL)]
    parts.append(f"""
  <div class="vrow"><div class="who"><span>Arco teso</span><small>Corda scura e sottile, freccia di citrino con punta di topazio e impennaggio d'ametista.
  Chiusura dei flettenti a metà strada: fermi a tensione 0, un solo pixel (tratto esterno e punte) a tensione 1 e 2. Spessore e geometria speculare invariati.</small></div>
    <div class="lower">{"".join(f'<div class="block"><h3>{t}</h3><div class="items pairs">' + "".join(fig(render(src[n], PAL), NOMI[n]) for n in tesi) + '</div></div>' for t, src in cols)}</div></div>""")
    from PIL import ImageDraw

    def marked(grid, px):
        im = on_bg(up(render(grid, PAL), 8))
        d = ImageDraw.Draw(im)
        d.rectangle([px[0] * 8 - 1, px[1] * 8 - 1, px[0] * 8 + 8, px[1] * 8 + 8], outline=(255, 64, 64, 255), width=2)
        return im
    def worn_with(face, inner=None):
        f = dict(FACES); f[("head_out", "north")] = face
        if inner is not None:
            f[("head_in", "north")] = inner
        return mannequin(render_big(armor_grid(f)), 4)

    def big(im, cap):
        return (f'<figure><div class="slot" style="max-width:200px">{img(on_bg(up(im, 12)), cap)}</div>'
                f'<figcaption>{cap}</figcaption><div class="real"><div class="slot s1">{img(im, cap + " 1x")}</div>'
                f'<div class="slot s2">{img(im, cap + " 2x")}</div></div></figure>')
    hcols = [("Prima", PREV_HELMET, PREV_HEAD_NORTH), ("Penultima", LAST_HELMET, LAST_HEAD_NORTH), ("Ultima", LAST2_HELMET, LAST2_HEAD_NORTH)]
    parts.append(f"""
  <div class="vrow"><div class="who"><span>Elmo</span><small>Apertura sul modello del bedrock helmet, allargata per il nostro elmo più largo: fessura degli occhi di 8 pixel, tratto verticale di 2 che scende
  fino all'ultima riga, elmo aperto in basso tra i due tratti del bordo; il vuoto è scuro e opaco come lì. Metallo, ombre, occhio-gemma, pergamena, catena col citrino e asimmetria restano nostri.
  Sull'elmo indossato, come nel bedrock indossato, la faccia esterna è aperta tranne le guance e la piastra interna ha la T trasparente, con la fessura di 8 pixel come nell'icona; l'occhio-gemma è sulla piastra.</small></div>
    <div class="lower"><div class="block"><h3>Bedrock di Spectrum</h3><div class="items pairs">{big(ref("helmet"), "bedrock")}</div></div>
    <div class="block"><h3>Nuova</h3><div class="items pairs">{big(sp["helmet"], "nostro")}</div><div class="slot man">{img(mannequin(tex, 4), "manichino nuovo")}</div></div>
    {"".join(f'<div class="block"><h3>{t}</h3><div class="items pairs">' + fig(render(g, PAL), "icona") + f'</div><div class="slot man">{img(worn_with(face, HEAD_IN), "manichino " + t.lower())}</div></div>' for t, g, face in hcols)}</div></div>""")
    scols = [("Lontani (4 px)", PREV_BOOTS), ("Vicini (1 px)", PREV2_BOOTS), ("Nuovo (2 px)", MID_BOOTS)]
    parts.append(f"""
  <div class="vrow"><div class="who"><span>Stivali</span><small>Spazio trasparente tra i due stivali: 4 pixel all'inizio, 1 nel giro scorso, ora 2, come le bedrock di Spectrum. Paio ricentrato, disegno e asimmetria invariati.</small></div>
    <div class="lower">{"".join(f'<div class="block"><h3>{t}</h3><div class="items pairs">' + fig(render(g, PAL), "stivali") + '</div></div>' for t, g in scols)}</div></div>""")
    parts.append(f"""
  <div class="vrow"><div class="who"><span>Schiena senza mantello</span><small>Il mantello è disegnato sempre (RenderType.entitySolid, niente trasparenza),
  quindi la sua faccia visibile ora è la schiena della corazza con il medaglione a occhio, la cintura, il retro delle gambe con catena e citrino sui lati giusti
  e il retro degli stivali. Niente più maglia a pixel alternati.</small></div>
    <div class="worn"><div class="block"><h3>Prima</h3><div class="slot man">{img(mannequin(render_big(armor_grid(PREV_WORN3)), 6), "manichino prima")}</div></div>
    <div class="block"><h3>Dopo</h3><div class="slot man">{img(mannequin(tex, 6), "manichino dopo")}</div></div></div></div>""")
    parts.append(pair("Icone dell'armatura senza pergamena", "Solo le strisce di pergamena diventano catena, borchia d'ametista o metallo; il resto del disegno approvato resta uguale.",
                      "".join(fig(render(PREV_ARMOR_ICONS[n], PAL), NOMI[n]) for n in PREV_ARMOR_ICONS),
                      "".join(fig(sp[n], NOMI[n]) for n in PREV_ARMOR_ICONS)))
    parts.append(pair("Spada, un pixel", "Il contorno scuro sinistro della guardia passa dalla colonna 3 alla 4 (contando da 1 dal basso a sinistra: [3,5] → [4,5]); "
                      "il pixel lasciato diventa trasparente e il contorno resta chiuso. Stessa modifica in tutti i 12 fotogrammi.",
                      fig(marked(PREV_BC_SWORD, (2, 11)), "prima"), fig(marked(PREV2_BC_SWORD, (3, 11)), "dopo")))
    parts.append(pair("Spada, pixel tolto", "Il pixel isolato [7,3] (contando da 1 dal basso a sinistra), coda della striscia di pergamena accanto all'impugnatura, "
                      "ora è trasparente, anche in tutti i 12 fotogrammi.",
                      fig(marked(PREV2_BC_SWORD, (6, 13)), "prima"), fig(marked(BC_SWORD, (6, 13)), "dopo")))
    rods = ["fishing_rod", "fishing_rod_cast"]
    parts.append(pair("Canna da pesca", "Tornata alla versione precedente (primo giro), con le sue animazioni: la prova più robusta, a sinistra, è stata scartata.",
                      "".join(fig(render(ROD2[n], PAL), NOMI[n]) for n in rods),
                      "".join(fig(sp[n], NOMI[n]) for n in rods)))
    parts.append(pair("Corazza", "Catena diagonale a maglie doppie, più leggibile.",
                      fig(render(OLD_FINAL["chestplate"], PAL), "corazza"), fig(sp["chestplate"], "corazza")))
    tex_prev = render_big(armor_grid(PREV_WORN_FACES))
    parts.append(f"""
  <div class="vrow"><div class="who"><span>Armatura indossata, dopo il controllo in gioco</span><small>Niente pergamena: mantello e perizoma ora sono metallo e catene
  con un medaglione a occhio-gemma, e catene al posto delle strisce su elmo, braccio e coscia. Il mantello aveva le facce invertite (da dietro si vedeva il lato interno):
  ora il lato decorato è quello che si vede da dietro, come mostra il manichino. «Prima» è com'era in gioco.</small></div>
    <div class="worn"><div class="block"><h3>Prima (in gioco)</h3><div class="slot man">{img(mannequin(tex_prev, 6), "manichino prima")}</div></div>
    <div class="block"><h3>Dopo</h3><div class="slot man">{img(mannequin(tex, 6), "manichino dopo")}</div></div></div></div>
</section>
<section class="style" id="armonizzazione">
  <div class="head"><div class="num">5</div><h2>Varianti di armonizzazione</h2>
  <p>Il confronto del giro precedente, da cui è nata la scelta: spada e ascia dello stile 3 con i quattro pezzi dello stile 5.</p></div>
  <div>""")
    for lett, nome, vsp in variants_rows():
        v = next((x for x in VARIANTS if x["lett"] == lett), None)
        desc = v["desc"] if v else "Le griglie scelte, ognuna con la sua palette originale."
        tag = "<em>scelta</em>" if lett == "C" else ""
        parts.append(f"""
    <div class="vrow"><div class="who"><b>{lett}</b><span>{nome}</span>{tag}<small>{desc}</small></div>
      <div class="items">{"".join(fig(vsp[it], NOMI[it]) for it in SET6)}</div></div>""")
    parts.append("""
  </div>
</section>
</main>
<footer><div class="wrap">Sprite originali per Spectral Ascension, disegnati a mano studiando la tecnica di Spectrum, senza ricalcarne le texture.</div></footer>
""")
    with open(path, "w") as fh:
        fh.write("".join(parts))


MOD_TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "spectral_ascension", "textures")
# I 15 pezzi del set T6 che hanno una texture inventario/in mano (gli stati di arco e canna compresi).
INSTALL_ICONS = ["sword", "axe", "shovel", "hoe", "shears", "bow", "bow_pulling_0", "bow_pulling_1",
                 "bow_pulling_2", "fishing_rod", "fishing_rod_cast", "helmet", "chestplate", "leggings", "boots"]


def install_pairs():
    """(sorgente in design/, destinazione nella mod) per ogni file installato."""
    sdir, adir = os.path.join(OUT, "set"), os.path.join(OUT, "armatura")
    pairs = []
    for n in INSTALL_ICONS:
        for ext in (".png", ".png.mcmeta"):
            pairs.append((os.path.join(sdir, f"t6_{n}_anim{ext}"),
                          os.path.join(MOD_TEX, "item", f"trichromatic_{n}{ext}")))
    pairs.append((os.path.join(adir, "t6_armor.png"), os.path.join(MOD_TEX, "armor", "trichromatic_armor.png")))
    for i in range(FRAMES):
        pairs.append((os.path.join(adir, f"t6_armor_{i}.png"),
                      os.path.join(MOD_TEX, "armor", f"trichromatic_armor_{i}.png")))
    return pairs


def install():
    import re
    import shutil
    pairs = install_pairs()
    missing = [s for s, _ in pairs if not os.path.exists(s)]
    if missing:
        sys.exit("mancano i sorgenti (rilancia prima lo script senza argomenti):\n  " + "\n  ".join(missing))
    for src, dst in pairs:
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(src, dst)
    adir = os.path.join(MOD_TEX, "armor")
    removed = []
    for fn in sorted(os.listdir(adir)):
        m = re.fullmatch(r"trichromatic_armor_(\d+)\.png", fn)
        if m and int(m.group(1)) >= FRAMES:
            os.remove(os.path.join(adir, fn))
            removed.append(fn)
    print(f"installati {len(pairs)} file in {MOD_TEX}"
          + (f"; rimossi {len(removed)} fotogrammi vecchi: {', '.join(removed)}" if removed else ""))


def main():
    if sys.argv[1:2] == ["install"]:
        install()
        return
    if sys.argv[1:2] == ["confronto"]:
        os.makedirs(sys.argv[2], exist_ok=True)
        confronto(sys.argv[2])
        return
    vdir, sdir, adir = (os.path.join(OUT, x) for x in ("varianti", "set", "armatura"))
    for d in (vdir, sdir, adir):
        os.makedirs(d, exist_ok=True)
    for v in VARIANTS:
        for it in SET6:
            render(v["grids"][it], PAL).save(os.path.join(vdir, f"{v['lett']}_{it}.png"))
    anims = {}
    for n in ORDER:
        render(FINAL[n], PAL).save(os.path.join(sdir, f"t6_{n}.png"))
        frames = icon_frames(n)
        save_anim(frames, os.path.join(sdir, f"t6_{n}_anim"))
        anims[n] = frames
    grid = armor_grid()
    tex = render_big(grid)
    tex.save(os.path.join(adir, "t6_armor.png"))
    aframes = [render_big(g) for g in ink_frames(grid)]
    for i, fr in enumerate(aframes):
        fr.save(os.path.join(adir, f"t6_armor_{i}.png"))
    worn = mannequin(tex, 8)
    on_bg(worn, (38, 34, 45, 255)).save(os.path.join(adir, "manichino.png"))
    worn_gif = gif_bytes([mannequin(fr, 6) for fr in aframes])
    with open(os.path.join(adir, "manichino_animato.gif"), "wb") as fh:
        fh.write(worn_gif)
    panoramica(os.path.join(OUT, "panoramica.png"), tex, on_bg(mannequin(tex, 6)))
    mdir = os.path.join(OUT, "mantello")
    os.makedirs(mdir, exist_ok=True)
    for pr in PROPOSTE:
        t = render_big(armor_grid(pr["faces"], cutout=pr["n"] == 5))
        t.save(os.path.join(mdir, f"proposta_{pr['n']}_armor.png"))
        on_bg(mannequin(t, 8, pr["cape"], pr["cloth"]), (60, 60, 70, 255)).save(os.path.join(mdir, f"proposta_{pr['n']}_manichino.png"))
        side = Image.new("RGBA", (2 * 24 * 8 + 16, 36 * 8), (24, 22, 30, 255))
        side.alpha_composite(on_bg(side_view(t, 8, pr["cape"], pr["cloth"]), (60, 60, 70, 255)), (0, 0))
        side.alpha_composite(on_bg(side_view(t, 8, pr["cape"], pr["cloth"], running=True), (60, 60, 70, 255)), (24 * 8 + 16, 0))
        side.save(os.path.join(mdir, f"proposta_{pr['n']}_lato.png"))
    tex_old = render_big(armor_grid(OLD_FACES))
    tex_old.save(os.path.join(adir, "prima_t6_armor.png"))
    html(os.path.join(OUT, "stili.html"), anims, tex, worn_gif, tex_old)


def render_big(grid):
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), PAL[ch])
    return im


if __name__ == "__main__":
    main()

