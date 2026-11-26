"""T6 Trichromatic, seconda tornata (t6-v2): proposte di stile su base BEDROCK + ALLTHEMODIUM + gemme CMY.

Tutto disegnato A MANO: ogni sprite 16x16 e ogni faccia dell'armatura indossata è una griglia ASCII scritta
carattere per carattere. Il codice converte le griglie in PNG con la palette della proposta, le dispone nel
layout UV 128x128 di Spectrum e compone confronti, panoramica e pagina; non disegna forme, contorni, ombre o luci.

Il layout UV e la proiezione del manichino NON sono duplicati: si importano da tools/texture_mix.py
(CUBES, cube_faces, armor_grid, mannequin), che qui non si modifica.

Uso:  python3 tools/texture_t6v2.py        # scrive SOLO in design/texture/proposte/t6-v2/
"""
import base64
import io
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_mix as M  # noqa: E402  (solo import: tabella UV e manichino)

ROOT = H.ROOT
REF = H.REF
OUT = os.path.join(ROOT, "design", "texture", "proposte", "t6-v2")
T7_PANORAMICA = os.path.join(ROOT, "design", "texture", "proposte", "t7", "panoramica.png")
ITEMS = ["sword", "axe", "helmet", "chestplate", "leggings", "boots"]
NOMI = H.NOMI


def G(text):
    """Una griglia 16x16 scritta a mano."""
    rows = [r.strip() for r in text.strip().splitlines()]
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [(i, r, len(r)) for i, r in enumerate(rows)]
    return rows


def F(text):
    """Una faccia dell'armatura scritta a mano: righe separate da spazi o a capo."""
    return text.split()


# ---------------------------------------------------------------------------
# Lettere della palette (uguali per tutte le proposte, cambiano i colori):
#   0-6  bedrock grigio-viola (0 = contorno violaceo, mai nero puro)
#   a-f  allthemodium arancio (a = contorno bruno caldo, b-c ombre che virano al rosso, e-f luci che virano al giallo)
#   g-k  stoffa del mantello e del perizoma (g = contorno)
#   q-t  topazio ciano (q = contorno)   m-p ametista magenta (m = contorno)   u-x citrino giallo (u = contorno)
#   W    riflesso bianco
# ---------------------------------------------------------------------------
GEMS = {"q": "#0b2a3a", "r": "#12708f", "s": "#2fc0e0", "t": "#b4f6ff",
        "m": "#3a0a33", "n": "#8a1c78", "o": "#d443a8", "p": "#ffa0e8",
        "u": "#3a2a05", "v": "#9c7f10", "w": "#eedc30", "x": "#fffcb0", "W": "#ffffff"}


def palette(bed, ore, cloth):
    keys = "0123456", "abcdef", "ghijk"
    pal = dict(GEMS)
    for ks, cols in zip(keys, (bed, ore, cloth)):
        assert len(ks) == len(cols)
        pal.update(dict(zip(ks, cols)))
    return {k: H.hexc(v) for k, v in pal.items()}


# ===========================================================================
# ARMATURA INDOSSATA: facce condivise (lati, fondi, parti nascoste), scritte a mano una per una.
# Ogni proposta sovrascrive le facce che caratterizzano il suo stile.
# ===========================================================================
RIM5 = F("22222 2...2 2...2 2...2 22222")
RIM6 = F("222222 2....2 2....2 2....2 2....2 222222")
RIM10 = ["2222222222"] + ["2........2"] * 8 + ["2222222222"]
HEAD_IN = F("111111111 112111211 111111111 121111112 111111111 111121111 211111111 111111121 111211111")
ARM_TOP = F("45554 45443 34443 34332 23321")
LEG_TOP = F("34443 34433 23332 23322 12221")
CLOTH_IN = F("""ghhhhhhhhhg ghhhhghhhhg ghhhhhhhhhg ghhghhhhhhg ghhhhhhhhhg ghhhhhhghhg ghhhhhhhhhg ghhhhhhhhhg
    ghhhghhhhhg ghhhhhhhhhg ghhhhhhhhhg ghhhhhhhghg ghhhhhhhhhg ghhhhhhhhhg ghhghhhhhhg ghhhhhhhhhg ghhhhhhhhhg
    ghhhhhhghhg ghhhhhhhhhg ghhhhhhhhhg ghhhhhhhhhg ghhhhhhhhhg ggggggggggg""")

BASE = {
    **{("head_in", f): HEAD_IN for f in ("top", "bottom", "west", "north", "east", "south")},
    ("head_out", "top"): F("3445554433 4556655443 4566555433 4556544332 4555443432 3454433322 3444333222 2343332221 2333222211 1222221110"),
    ("head_out", "bottom"): RIM10,
    ("head_out", "west"): F("4555544332 5554443322 4544433221 bcdcbcbba1 4433322211 4433222110 3432221110 3322211100 3221111000 abba110000"),
    ("head_out", "east"): F("2334455554 2234445555 1223344454 1abcbbcdcb 1122233344 0112223344 0111222343 0011122233 0001111223 0000111aba"),
    ("body_out", "top"): F("4566556654 4555445554 3443333443"),
    ("body_out", "bottom"): F("2222222222 2111111112 2222222222"),
    ("body_out", "west"): F("554 544 444 443 433 433 333 332 322 221"),
    ("body_out", "east"): F("543 443 433 433 333 332 322 322 221 210"),
    ("body_in", "north"): F("433333334 432222234 432222234 432222234 432222234 432222234 432222234 432222234 332222233 322222223 adcdedcba abcbcbcba 011111110"),
    ("body_in", "south"): F("433333334 432222234 432222234 432222234 432222234 432222234 432222234 432222234 332222233 322222223 abcdcdcda abbcbcbba 011111110"),
    ("body_in", "west"): F("43333 43222 43222 43222 43222 43222 43222 43222 33222 32222 bcdca abcba 01110"),
    ("body_in", "east"): F("43333 43222 43222 43222 43222 43222 43222 43222 33222 32222 acdcb abcba 01110"),
    ("body_in", "top"): ["333333333"] * 5,
    ("body_in", "bottom"): ["222222222"] * 5,
    ("right_arm", "west"): F("55554 54443 44433 44333 43332 abcba 43322 bcdcb acdca abcba 33221 32211 21110"),
    ("right_arm", "east"): F("44443 44333 43332 33322 33222 abcba 32222 bcdcb acdca abcba 22211 22111 11110"),
    ("right_arm", "top"): ARM_TOP, ("right_arm", "bottom"): RIM5,
    ("left_arm", "west"): F("44443 44333 43332 33322 33222 32222 abcda 22221 22211 22111 21111 11111 01110"),
    ("left_arm", "east"): F("55554 54443 44433 44333 43332 33322 adcba 33222 32221 22211 22111 21111 11110"),
    ("left_arm", "top"): ARM_TOP, ("left_arm", "bottom"): RIM5,
    # spallacci: grande e arancio a destra, piccolo cappuccio di bedrock a sinistra
    ("right_arm_extra", "north"): F("adeedc acddcb abccba 0aaba0"),
    ("right_arm_extra", "south"): F("acddcb abccba abbbaa 0aaaa0"),
    ("right_arm_extra", "west"): F("adedcb acdccb abcbba 0abaa0"),
    ("right_arm_extra", "east"): F("bcdeda bccdca abccba 0aaba0"),
    ("right_arm_extra", "top"): F("cdeedc deffed deffed cdeedc bcddcb abccba"),
    ("right_arm_extra", "bottom"): RIM6,
    **{("left_arm_extra1", f): ["abcda"] for f in ("west", "north", "east", "south")},
    ("left_arm_extra1", "top"): F("abcda b555c c544d c443c abcba"),
    ("left_arm_extra1", "bottom"): F("aaaaa a...a a...a a...a aaaaa"),
    **{("left_arm_extra2", f): ["455543", "333221"] for f in ("west", "north", "east", "south")},
    ("left_arm_extra2", "top"): F("455554 556654 456543 455433 344332 233221"),
    ("left_arm_extra2", "bottom"): RIM6,
    ("right_leg", "west"): F("44433 44333 43332 abcba 33322 43322 33222 32221 22211 21110"),
    ("right_leg", "east"): F("44333 43332 43322 abcba 33222 33222 32221 32211 22111 11110"),
    ("right_leg", "top"): LEG_TOP, ("right_leg", "bottom"): RIM5,
    ("left_leg", "west"): F("44433 44333 43332 33322 abdca 43322 33222 32221 22211 21110"),
    ("left_leg", "east"): F("44333 43332 43322 33222 acdba 33222 32221 32211 22111 11110"),
    ("left_leg", "top"): LEG_TOP, ("left_leg", "bottom"): RIM5,
    ("right_boot", "west"): F("abcda 43322 00000"), ("right_boot", "east"): F("acdba 33222 00000"),
    ("right_boot", "top"): LEG_TOP, ("right_boot", "bottom"): ["00000"] * 5,
    ("left_boot", "west"): F("43332 43322 00000"), ("left_boot", "east"): F("43322 33222 00000"),
    ("left_boot", "top"): LEG_TOP, ("left_boot", "bottom"): ["00000"] * 5,
    ("cape", "south"): CLOTH_IN,
    ("cape", "west"): list("bgggghgggggghggggghgggg"),
    ("cape", "east"): list("bggghgggggghgggggghgggg"),
    ("cape", "top"): ["abcdcbghhhg"], ("cape", "bottom"): ["ggggggggggg"],
    ("front_cloth", "south"): F("ghhhhhg ghhhhhg ghhghhg ghhhhhg ghhhhhg ghhhhgg ghhhhhg ghhhhhg ghghhhg ghhhhhg ghhhhhg ghhhhhg ghhhhhg ggggggg"),
    ("front_cloth", "west"): list("agggghggggghgg"),
    ("front_cloth", "east"): list("aggghggggghggg"),
    ("front_cloth", "top"): ["abcdcba"], ("front_cloth", "bottom"): ["ggggggg"],
}

PROPOSTE = []

# ===========================================================================
# PROPOSTA 1 - GHIERA. Bedrock dominante, arancio solo nei raccordi (ghiere, fibbie, guardie), gemme come
# borchie a gruppi. Forme slanciate.
# ===========================================================================
PROPOSTE.append({
    "n": 1, "slug": "01_ghiera", "nome": "Ghiera",
    "desc": "Il corpo è tutto bedrock; l'allthemodium compare solo dove i pezzi si uniscono: ghiere, fibbie, "
            "la guardia a uncino della spada, un solo spallaccio. Le gemme sono borchie piccole, a gruppi di tre.",
    "asim": "Un solo spallaccio, a destra. La guardia della spada ha un uncino lungo da un lato e un moncherino dall'altro. Le borchie stanno tutte su un fianco; la ghiera dell'elmo scende su una guancia sola.",
    "tags": {"Arancio": "poco, solo nei raccordi", "Gemme": "borchie incastonate", "Motivo": "ghiere e fibbie",
             "Forme": "slanciate"},
    "pal": palette(("#150f1e", "#251d33", "#342b45", "#443a58", "#574d6e", "#6e6488", "#9188ab"),
                   ("#3a1406", "#7a2e0c", "#b8560f", "#e89725", "#ffc25a", "#fff0b8"),
                   ("#1c0d1c", "#3a1a37", "#57284f", "#743a66", "#93527f")),
    "items": {
        "sword": G("""
            .............000
            ............0650
            ...........06540
            ..........065420
            .........064220.
            ........052430..
            ..ab...065340...
            .adcb.064520....
            .acedb65430.....
            ..bcfdc420......
            ...bdqsdca......
            ...0ctrecb......
            ..030bcdba......
            .0420.aba.......
            apa0....a.......
            ana.............
        """),
        "axe": G("""
            ................
            ..........000...
            .........065550.
            ........06544560
            ....ab.065423560
            ...adcod54221460
            ....bcedc4321560
            .....abdc032250.
            ......0420.0000.
            .....0420.......
            ....0430........
            ...0420.........
            ..adc0..........
            .aqsa...........
            .ara............
            ................
        """),
        "helmet": G("""
            ................
            ................
            ......00000.....
            ....0066554400..
            ...066545443320.
            ..0665452443320.
            ..0654432233s10.
            ..adcdedcbbdca0.
            ..0310000000120.
            ..0430000000dc0.
            ...0320...0edca.
            ....00.....bca..
            ................
            ................
            ................
            ................
        """),
        "chestplate": G("""
            ................
            .00000......000.
            0566550....05540
            0555443000044430
            adcdcba665544320
            .abba0655443210.
            ...0654432st10..
            ...065433o2110..
            ...05443w22110..
            ...05432221110..
            ...adcdedcbcda..
            ...0432bc2110...
            ...0432100210...
            ...0321...010...
            ....00....00....
            ................
        """),
        "leggings": G("""
            ................
            ................
            ...0000000000...
            ...0665545440...
            ...adcdbbcdca...
            ...0543st3210...
            ...0542002210...
            ...0530..0410...
            ..adca0..0420...
            ..aedcb..0530...
            ...bcb0..0420...
            ...0430..0320...
            ...0320..0430...
            ...0000..0000...
            ................
            ................
        """),
        "boots": G("""
            ................
            ................
            ................
            ................
            ...adca...0000..
            ...bdcb...0540..
            ...0540...0430..
            ...0430...0430..
            ...0530...0520..
            ..05420...04320.
            .065430...054320
            .054320...0wv320
            .000000...000000
            ................
            ................
            ................
        """),
    },
    "faces": {
        ("head_out", "north"): F("4556554432 5665454332 4554433322 adcdedcbba 4310000132 4320440232 3320340dca 3221330edb 2211220cba 1100110ba0"),
        ("head_out", "south"): F("3445555443 3345565544 2334455543 abbcdcdcda 2233444543 1223344432 1122333432 0112233322 0011223221 00ab11bca0"),
        ("body_out", "north"): F("4565adda54 5654abba43 4554433st3 45433322o2 4443322w21 4443322211 3433222111 3332221110 3322211100 2211100000"),
        ("body_out", "south"): F("4556655544 4555544433 4454443332 4444433322 3443332222 3343322221 3333222211 2332221111 2222211110 1111100000"),
        ("right_arm", "north"): F("55654 54543 44433 43432 33322 adcba 43322 bcdcb adeda acwca abcba 32211 21110"),
        ("right_arm", "south"): F("45554 44543 34433 33432 23322 abcba 33322 bcdcb acdca abcba abbba 22211 11100"),
        ("left_arm", "north"): F("45565 34545 33444 23434 22333 22333 abdca 22332 12232 12222 11222 11121 01110"),
        ("left_arm", "south"): F("45554 44543 34443 33433 23333 22322 acdba 22322 22221 12221 11211 11111 01110"),
        ("right_leg", "north"): F("44443 54433 44332 adcba 33322 43322 33221 32221 22211 21110"),
        ("right_leg", "south"): F("44433 44333 43332 abcba 33322 33322 33221 32211 22211 21110"),
        ("left_leg", "north"): F("34444 33445 23344 22334 abdca 23344 22334 12233 12223 01122"),
        ("left_leg", "south"): F("34443 33444 23344 22334 acdba 23334 22233 12233 12223 01122"),
        ("right_boot", "north"): F("abdca 54332 10000"), ("right_boot", "south"): F("abcba 43322 00000"),
        ("left_boot", "north"): F("45443 4w332 00000"), ("left_boot", "south"): F("44433 43322 00000"),
        ("front_cloth", "north"): F("adcdcba abcocba gijjihg ghiijhg ghijihg ghiihhg ghijihd ghiihgc ghhihgd ghiihgc gghihgb ghhhhgc gghhggb gggggga"),
        ("cape", "north"): F("""abdcba12221 acsdcagihhg abobagijihg gaagijjihhg ghijjiihhgg ghiijihhhgg ghiiihhghhg ghijihhghhg
            ghhiihgghig ghiiihgghig ghhiihgghhg ghiihhgghhg ghiiihghhig ghhiihghiig ghiihhghiig gghihhghihg ghhihgghhhg
            ghiihgghihg gghihggghhg ghhhggghhgg gghhgghhggg ggbcgggcbgg abaggaabgga"""),
    },
})


# ===========================================================================
# PROPOSTA 2 - COLATA. L'allthemodium è colato su un lato di ogni pezzo e si è rappreso in grumi e gocce;
# l'altro lato resta bedrock, attraversato da crepe dove passa l'energia delle gemme. Forme massicce.
# ===========================================================================
PROPOSTE.append({
    "n": 2, "slug": "02_colata", "nome": "Colata",
    "desc": "L'allthemodium è stato colato su un fianco di ogni pezzo e si è rappreso in grumi e gocce che "
            "pendono. L'altro fianco resta bedrock, spaccato da crepe in cui scorre l'energia ciano, magenta e gialla.",
    "asim": "La colata copre sempre lo stesso fianco: spalla, braccio, gamba e metà dell'elmo destri. Il fianco sinistro è bedrock con le crepe. Sul mantello scende da una spalla sola.",
    "tags": {"Arancio": "molto, su un solo lato", "Gemme": "energia nelle crepe", "Motivo": "colata e gocce",
             "Forme": "massicce"},
    "pal": palette(("#140e1c", "#241c31", "#332a44", "#433958", "#564c6d", "#6c6386", "#8c84a6"),
                   ("#3d1305", "#842c0a", "#c4560e", "#ef9a24", "#ffc95e", "#fff3c2"),
                   ("#0f1320", "#1d2335", "#2b3249", "#3c445e", "#535d78")),
    "items": {
        "sword": G("""
            .............a00
            ............ae50
            ...........aed40
            ..........aedr40
            .........aedcs40
            ........aedsr430
            .......aecd5430.
            ..aa..adcr5420..
            .adeaadcds5320..
            .aefedcdsr420...
            ..aeddcbr320....
            ...abcbcca0.....
            ...0420ab.c.....
            ..0430..b.......
            adca0...........
            aoa.............
        """),
        "axe": G("""
            ................
            .........aa000..
            ........aed5540.
            .......aedr54320
            ......aedcs43210
            .....adcdr543210
            ....aedcba44320.
            ....abca.04320..
            .....040..0000..
            ....040.........
            ...0420.........
            ..0430..........
            .adca...........
            .awea...........
            ..aa............
            ................
        """),
        "helmet": G("""
            ................
            ................
            .....aaa000.....
            ...aaedd55400...
            ..aeddc6554430..
            .aedcb65544320..
            .adcb554433210..
            .acb00mnon0010..
            .abc0400004210..
            .abd03200.3210..
            ..bc0210..2100..
            ..aa.c0...000...
            .....b..........
            ................
            ................
            ................
        """),
        "chestplate": G("""
            ................
            .aaa......0000..
            adeda0000054440.
            aedcda5566544320
            adcdcba655443210
            .abdcdca5r433210
            ..abdcdca4s3210.
            ..0abdcda43r210.
            ..04abcdca4s210.
            ..0544abdcb4310.
            ..05433abca3210.
            ..0432abda32110.
            ..04320ab021100.
            ..0321..c.01110.
            ...00...b..000..
            ................
        """),
        "leggings": G("""
            ................
            ................
            ..adddca000000..
            ..aedcdba54440..
            ..adcbca554320..
            ..abcba0443210..
            ..adca0..04s20..
            ..acdba..0r320..
            ..abcda..04320..
            ..0abca..0o420..
            ..04ab0..04320..
            ..043c0..03w10..
            ..0320...03210..
            ..0000...00000..
            ................
            ................
        """),
        "boots": G("""
            ................
            ................
            ................
            ..aaaa....0000..
            ..adeda...0540..
            ..acdca...0430..
            ..abdc0...0w30..
            ..0bc40...0v30..
            ..04b40...0430..
            .0543c0...04320.
            06543b0...054320
            0543220...054320
            0000000...000000
            ................
            ................
            ................
        """),
    },
    "faces": {
        ("head_out", "north"): F("aeddc55443 adcdb65443 acdcb54432 abcba44332 a0mnoonm01 abc0443221 acb0343221 abc0332211 aba0221110 aa00110000"),
        ("head_out", "south"): F("3445555dea 2344555dca 2334454bda 1233444acb 1223343abc 1122333bca 0112233aba 0011223abc 0001122aba 000011aaba"),
        ("body_out", "north"): F("adeda05654 aedcdb5543 adcdcba4r3 abcdcdbas2 4abcdca3r2 44abdcba22 344abca221 3334aba211 33322c2110 2222b11100"),
        ("body_out", "south"): F("45556adeda 4455abdcda 4445aabdcb 34444abcba 3344433aba 3334332aba 23333222ab 2223222111 2222211110 1111100000"),
        ("right_arm", "north"): F("adeda adcdb acdcb abcba 0abca 04aba 043ba 04cba 03bca 032ba 0321a 03210 02110"),
        ("right_arm", "south"): F("adeda bcdca abcca abbca acba0 aba40 ab340 abc40 acb30 ab230 a1230 01230 01120"),
        ("left_arm", "north"): F("45554 44543 34443 34s33 33r32 33332 23s22 22322 22r21 12221 11211 11111 01110"),
        ("left_arm", "south"): F("45554 44543 34443 33433 23333 22322 22322 22221 12221 12221 11211 11111 01110"),
        ("right_leg", "north"): F("adcda acdba abcba abca0 aba30 ab330 a3320 33221 32211 21110"),
        ("right_leg", "south"): F("adcda abdca abcba 0acba 03aba 033ba 0332a 32221 22211 21110"),
        ("left_leg", "north"): F("34444 33445 23s44 22r34 23344 22s34 12233 12223 12223 01122"),
        ("left_leg", "south"): F("34443 33444 23344 22334 23334 22334 22233 12233 12223 01122"),
        ("right_boot", "north"): F("abdca acb32 00000"), ("right_boot", "south"): F("acdba 23bca 00000"),
        ("left_boot", "north"): F("44443 4w432 00000"), ("left_boot", "south"): F("44433 43322 00000"),
        ("front_cloth", "north"): F("adedcba acdcbag abdbajg gacahig ghbaijg ghcaihg ghaiijg ghbijhg ghaiihg gghiihg ghhijhg gghiihg ghhhhgg ggggggg"),
        ("cape", "north"): F("""adeedcbghhg aedcdcbaihg adcdcbaijhg abcdcbhijhg gacdbaiijhg gabcahijihg ghacaiijhhg ghabahijhgg
            ghiaaiihhgg ghhcbiihhig ghiaoihhhig ghiihhgghhg ghiiihghhig ghhiihghiig ghiihhghiig gghihhghihg ghhihgghhhg
            ghiihgghihg gghihggghhg ghhhggghhgg gghhgghhggg gghggghgggg ggggggggggg"""),
    },
})

# ===========================================================================
# PROPOSTA 3 - SCAGLIE. Placche di bedrock sovrapposte come scaglie, ognuna col labbro superiore in
# allthemodium; le placche sono sfalsate e di larghezze diverse. Una grossa gemma a cabochon per pezzo.
# ===========================================================================
PROPOSTE.append({
    "n": 3, "slug": "03_scaglie", "nome": "Scaglie",
    "desc": "Ogni pezzo è fatto di placche di bedrock sovrapposte, sfalsate e di misure diverse; ogni placca "
            "ha il labbro in allthemodium. Una sola gemma grande a cabochon per pezzo, che cambia colore da oggetto a oggetto.",
    "asim": "Le placche non si ripetono mai uguali: larghezze, altezze e sporgenze diverse a ogni strato. Lo spallaccio a scaglie è da un lato, il cabochon dall'altro.",
    "tags": {"Arancio": "medio, sui bordi delle placche", "Gemme": "un cabochon grande", "Motivo": "placche sovrapposte",
             "Forme": "massicce, a strati"},
    "pal": palette(("#170f1c", "#281d30", "#382a42", "#4a3a55", "#5e4d6a", "#776683", "#9b8aa6"),
                   ("#401708", "#86330d", "#c05e12", "#ec9d2c", "#ffcb6a", "#fff2c8"),
                   ("#1e0a0c", "#3b1418", "#5a2025", "#783034", "#984648")),
    "items": {
        "sword": G("""
            .............000
            ............0560
            ...........05640
            ..........065430
            .........0edcb0.
            ........065430..
            .......0543320..
            ......0edcbba...
            ..aa..065420....
            .aedba54320.....
            .acdcedcba......
            ..abqstdcb......
            ..0aqrsba.......
            .040.aaa........
            040.............
            ab..............
        """),
        "axe": G("""
            ................
            .........0000...
            ........0edddc0.
            ........05544320
            .......0edcba430
            ......aa05543320
            .....aopb054320.
            .....anmb00000..
            ......aba0......
            .....0420.......
            ....0430........
            ...0420.........
            ..0430..........
            .adca...........
            .abba...........
            ................
        """),
        "helmet": G("""
            ................
            ................
            ......000000....
            ....0066554400..
            ...066545544330.
            ..0edcb54433xw0.
            ..065441433vu0..
            ..054300000320..
            ..043200000420..
            ..0edc0...0320..
            ..0430....0210..
            ...00......00...
            ................
            ................
            ................
            ................
        """),
        "chestplate": G("""
            ................
            .00000.....0000.
            0566540000005540
            0554430665544430
            0dcbedc65544320.
            .05443065rts420.
            ..0543054qsrq20.
            ..05420544qrq10.
            ..043305443cedb0
            ..0432edcba11110
            ..edcb122222210.
            ..0543322211100.
            ..043210.002100.
            ..0321.....0110.
            ...00.......00..
            ................
        """),
        "leggings": G("""
            ................
            ................
            ...0000000000...
            ...0665545440...
            ...05544edcb0...
            ...0543211110...
            ...0542002210...
            ...0530..0410...
            ...0530.0edc0...
            ...0430..0wx20..
            ...0420..0vu20..
            ...0430..03210..
            ...0320..03210..
            ...0000..00000..
            ................
            ................
        """),
        "boots": G("""
            ................
            ................
            ................
            ...0edc0..0000..
            ...05430..0540..
            ...04321..0430..
            ..0edcb0..0430..
            ..054320..0530..
            ...04320..0430..
            ..054320..04320.
            .0654320..0654op
            .0544320..0543nm
            .0000000..000000
            ................
            ................
            ................
        """),
    },
    "faces": {
        ("head_out", "north"): F("4556554432 5665554432 edcb544xw2 5544144vu2 4300000032 4320440032 3320340021 edc0330021 5430220011 3210110000"),
        ("head_out", "south"): F("3445555443 3345565544 2334455543 2233edcba2 2223311142 1122333432 1122233322 0112233322 0011223221 0001111110"),
        ("body_out", "north"): F("4566005654 5655rts543 4554srq432 4544qq3432 4443332ced 443edcba11 edc1112221 1112222211 3322211100 2211100000"),
        ("body_out", "south"): F("4556655544 4555544433 4454443332 4444edcba2 3443311122 3343322221 3333222211 2332221111 2222211110 1111100000"),
        ("right_arm", "north"): F("55654 edcba 41112 43432 33322 33322 43322 0edcb 54111 43321 33221 32211 21110"),
        ("right_arm", "south"): F("45554 dcbba 31112 33432 23322 33322 33322 0dcbb 43111 33221 22211 22211 11100"),
        ("left_arm", "north"): F("45565 34545 33444 23434 22333 22333 2edcb 22111 12232 12222 11222 11121 01110"),
        ("left_arm", "south"): F("45554 44543 34443 33433 23333 22322 2dcbb 22111 22221 12221 11211 11111 01110"),
        ("right_leg", "north"): F("44443 54433 44332 43322 33322 43322 33221 32221 22211 21110"),
        ("right_leg", "south"): F("44433 44333 43332 33322 33322 33322 33221 32211 22211 21110"),
        ("left_leg", "north"): F("34444 33445 23344 0edcb 2wx11 2vu34 22334 12233 12223 01122"),
        ("left_leg", "south"): F("34443 33444 23344 0dcbb 23111 23334 22233 12233 12223 01122"),
        ("right_boot", "north"): F("0edcb 54311 00000"), ("right_boot", "south"): F("dcbba 43322 00000"),
        ("left_boot", "north"): F("54443 4op32 00000"), ("left_boot", "south"): F("dcbba 43322 00000"),
        ("front_cloth", "north"): F("0566540 0544430 edcbaa0 g111hhg ghijihg ghiijhg ghijihg ghiihhg ghijihg gghiihg ghhijhg gghiihg ghhhhgg ggggggg"),
        ("cape", "north"): F("""0566554ghhg 054op43gihg 054nm32gihg edcbbaa1ihg g111hhijhhg ghiijihhhgg ghiiihhghhg ghijihhghhg ghhiihgghig ghiiihgghig ghhiihgghhg ghiihhgghhg ghiiihghhig ghhiihghiig ghiihhghiig gghihhghihg ghhihgghhhg ghiihgghihg gghihggghhg ghhhggghhgg gghhgghhggg gghggghgggg ggggggggggg"""),
    },
})

# ===========================================================================
# PROPOSTA 4 - CASTONE. Bedrock con un filo d'allthemodium su un solo spigolo; il centro di ogni pezzo è un
# castone grande, fuori asse, che tiene insieme le tre gemme (ciano, magenta, giallo) come faccette di un prisma.
# ===========================================================================
PROPOSTE.append({
    "n": 4, "slug": "04_castone", "nome": "Castone",
    "desc": "Bedrock con un filo d'allthemodium lungo un solo spigolo. Il protagonista è un castone grande e fuori asse "
            "che stringe le tre gemme insieme, come le facce di un prisma: il topazio in cima, citrino e ametista alla base.",
    "asim": "Il castone non sta mai al centro: è sulla tempia, sul petto a destra, sul fianco, su un solo stivale. Il filo arancio corre lungo un solo spigolo.",
    "tags": {"Arancio": "medio: filo e castone", "Gemme": "tre insieme in un castone", "Motivo": "prisma incastonato",
             "Forme": "compatte"},
    "pal": palette(("#110e1d", "#1f1b31", "#2d2843", "#3c3657", "#4f486e", "#675f89", "#8a83ad"),
                   ("#3a1406", "#7c2f0c", "#bc5a10", "#ea9a27", "#ffc75e", "#fff1bc"),
                   ("#120c24", "#21173d", "#312256", "#43306f", "#5a4589")),
    "items": {
        "sword": G("""
            .............000
            ............0e50
            ...........0d540
            ..........0e5420
            .........0d5420.
            ........0d4320..
            .....d.0e420....
            ....dtc0d20.....
            ...dtsrb20......
            ..dxwponb.......
            ..abccdcba......
            ...040..........
            ..040...........
            .040............
            adc.............
            ab..............
        """),
        "axe": G("""
            ................
            ..........0ed0..
            .........0d5540.
            ........0e654430
            ......d0d6544320
            .....dtc0543210.
            ....dtsrb43210..
            ...dxwponb200...
            ...abccdcba.....
            ....040.........
            ...040..........
            ..0420..........
            .0430...........
            040.............
            ad..............
            ................
        """),
        "helmet": G("""
            ................
            ................
            .....000000.....
            ...0065554dd0...
            ..06654544dc20..
            ..06544543d320..
            ..0543443dtc10..
            ..adcb43dwsob0..
            ..0310abcba010..
            ..042000000020..
            ..0320....0320..
            ...00......00...
            ................
            ................
            ................
            ................
        """),
        "chestplate": G("""
            ................
            .0000......0dd0.
            056650....0de550
            055544300004dc40
            0454433665544c30
            .0443443d443b20.
            ..054343dtc34c0.
            ..04433dwsob3d0.
            ..04333abcba3c0.
            ..04333222112b0.
            ..033222211110a.
            ..03222211110...
            ..0432100210....
            ..0321...010....
            ...00....00.....
            ................
        """),
        "leggings": G("""
            ................
            ................
            ...0000000000...
            ...066554d540...
            ...adcdcdtc40...
            ...0543dwsob0...
            ...0542abcba0...
            ...0530..0410...
            ...dc30..0420...
            ...e430..0530...
            ...d420..0420...
            ...0430..0320...
            ...0320..0430...
            ...0000..0000...
            ................
            ................
        """),
        "boots": G("""
            ................
            ................
            ................
            ................
            ...0000...0000..
            ...0540...0d40..
            ...0430...dtc0..
            ...0530..dwsob..
            ...0430..abcba..
            ..05420..054320.
            .065430..054320.
            .adc320..054320.
            .aaa000..000000.
            ................
            ................
            ................
        """),
    },
    "faces": {
        ("head_out", "north"): F("0455555de0 4566544cd3 455544d4b2 45543dtc32 adcbdwsob1 4310abcba1 4320000021 3320000021 2211000121 1100000110"),
        ("head_out", "south"): F("3445555de4 3345565cd4 2334455bc3 2233444ab2 2223344432 1122333432 1122233322 0112233322 0011223221 0001111110"),
        ("body_out", "north"): F("dde5665544 bcd5554d43 4ab544dtc3 44335dwsob 44335abcba 3443332222 3433222111 3332221110 3322211100 2211100000"),
        ("body_out", "south"): F("4556655edd 4555544dcb 4454443cba 4444433322 3443332222 3343322221 3333222211 2332221111 2222211110 1111100000"),
        ("right_arm", "north"): F("55654 54543 44433 43432 33322 33322 43322 32d22 3dtc2 dwsob abcba 32211 21110"),
        ("right_arm", "south"): F("45554 44543 34433 33432 23322 33322 33322 abbba acdca abcba abbba 22211 11100"),
        ("left_arm", "north"): F("dd565 cd545 bc444 a3434 22333 22333 22333 22332 12232 12222 11222 11121 01110"),
        ("left_arm", "south"): F("dd554 cd543 bc443 a3433 23333 22322 22322 22322 22221 12221 11211 11111 01110"),
        ("right_leg", "north"): F("dd443 dc433 cb332 b3322 33322 43322 33221 32221 22211 21110"),
        ("right_leg", "south"): F("44dd3 44dc3 43cb2 433b2 33322 33322 33221 32211 22211 21110"),
        ("left_leg", "north"): F("34444 33445 23344 22d34 2dtc4 dwsob abcba 12233 12223 01122"),
        ("left_leg", "south"): F("34443 33444 23344 22334 22334 23334 22233 12233 12223 01122"),
        ("right_boot", "north"): F("45443 dd332 00000"), ("right_boot", "south"): F("44433 43322 00000"),
        ("left_boot", "north"): F("3dtc3 dwsob 00000"), ("left_boot", "south"): F("44433 43322 00000"),
        ("front_cloth", "north"): F("ghhdhhg ghdtchg gdwsobg gabcbag ghijihd ghiijhc ghijihd ghiihhc ghijihb gghiihc ghhijhb gghiihc ghhhhgb gggggga"),
        ("cape", "north"): F("""abcdcbaghhg ghhdhhgihhg ghdtchgihhg gdwsobgijhd gabcbaiihgc ghiijihhhgd ghiiihhghgc ghijihhghhb ghhiihgghgc ghiiihgghgd ghhiihgghhc ghiihhgghgb ghiiihghhgc ghhiihghigd ghiihhghigc gghihhghihb ghhihgghhgc ghiihgghigb gghihggghga ghhhggghhgb gghhgghhgga gghggghggga ggggggggggg"""),
    },
})

# ===========================================================================
# PROPOSTA 5 - FILIGRANA. Bedrock più chiaro, percorso da fili d'allthemodium sottili che si arricciano solo su
# un lato; le gemme sono gocce piccole alla fine dei riccioli. Forme slanciate, il set più chiaro.
# ===========================================================================
PROPOSTE.append({
    "n": 5, "slug": "05_filigrana", "nome": "Filigrana",
    "desc": "Bedrock più chiaro, percorso da un filo d'allthemodium che si arriccia solo su un lato di ogni pezzo. "
            "Le gemme sono gocce piccole appese alla fine dei riccioli. È il set più chiaro e leggero.",
    "asim": "La filigrana si arriccia su un solo lato di ogni pezzo e finisce con una goccia: anello con l'ametista da una parte della guardia, ricciolo col topazio dall'altra.",
    "tags": {"Arancio": "poco, ma in vista: fili", "Gemme": "gocce a fine ricciolo", "Motivo": "filigrana",
             "Forme": "slanciate, leggere"},
    "pal": palette(("#1a1324", "#2b2238", "#3b314c", "#4d4361", "#625877", "#7c7192", "#a096b5"),
                   ("#46190a", "#8f3a0f", "#cf6915", "#f4a531", "#ffd273", "#fff6d2"),
                   ("#26192a", "#423046", "#5d4762", "#7a6280", "#9c85a0")),
    "items": {
        "sword": G("""
            .............000
            ............0650
            ...........06540
            ..........065420
            .........06e420.
            ........065d320.
            ..edc..065cb20..
            .e...cd6d4320...
            .d.obb5d4320....
            ..d..cd4320.....
            ...dcb0d20......
            ....0420c.......
            ...0430.d.......
            ..0420..bs......
            .apa............
            ..a.............
        """),
        "axe": G("""
            ................
            ..........000...
            .........065540.
            ........06d54430
            ....ed.065ed4320
            ...e..c05d443210
            ...d.o.cd543210.
            ....dcb0d43200..
            ......0420......
            .....0430.......
            ....0420........
            ...0430.........
            ..0420..........
            .edc............
            .cpa............
            ................
        """),
        "helmet": G("""
            ................
            ................
            .....000000.....
            ...0066554400...
            ..06654d544320..
            ..0665d4ed4320..
            ..06544d5e4310..
            ..05443d43d210..
            ..0310000002d0..
            ..0420000000s...
            ..0320....0r....
            ...00...........
            ................
            ................
            ................
            ................
        """),
        "chestplate": G("""
            ................
            ..000......000..
            .06650....05540.
            0665540000554430
            0655446655ed4320
            .054436554bd210.
            ..05443e5d43210.
            ..0544d44c43210.
            ..0543c4bd32110.
            ..0443d24432110.
            ..04332d4332110.
            ..0432bdc22110..
            ..043210o21100..
            ..0321...0110...
            ...00.....00....
            ................
        """),
        "leggings": G("""
            ................
            ................
            ...0000000000...
            ...0665545440...
            ...0554edc540...
            ...0543b2d320...
            ...0542002210...
            ...0530..0410...
            ...0ed0..0420...
            ...d4c0..0530...
            ...cb40..0420...
            ...0o30..0320...
            ...0320..0430...
            ...0000..0000...
            ................
            ................
        """),
        "boots": G("""
            ................
            ................
            ................
            ................
            ...0000...0000..
            ...0540...0ed0..
            ...0430...04c0..
            ...0530...0db0..
            ...0430...0430..
            ..05420...04320.
            .065430...0543s0
            .054320...054320
            .000000...000000
            ................
            ................
            ................
        """),
    },
    "faces": {
        ("head_out", "north"): F("4556554432 566545d432 45d4ed4d32 4d543d5d22 4310000d32 4320440d32 3320340c21 3221330d21 2211220s11 1100110r10"),
        ("head_out", "south"): F("3445555443 3345565d44 23344d5d43 2233d44d32 2223d44d32 1122d33432 1122c33322 0112d33322 0011s23221 0001r11110"),
        ("body_out", "north"): F("4565005654 5654dd5543 455d54ed33 45d433d432 4d43322d21 4c43322d21 34d322d211 334dcd2110 3322211100 2211100000"),
        ("body_out", "south"): F("4556655544 4555544433 4454443332 4444433322 3443332222 3343322221 3333222211 2332221111 2222211110 1111100000"),
        ("right_arm", "north"): F("55654 54543 44433 43432 33322 33322 43322 33322 33222 32222 22211 22211 21110"),
        ("right_arm", "south"): F("45554 44543 34433 33432 23322 33322 33322 32222 32222 22221 22211 22211 11100"),
        ("left_arm", "north"): F("45565 3ed45 d34c4 d3b34 2d333 22c33 223s2 22332 12232 12222 11222 11121 01110"),
        ("left_arm", "south"): F("45554 44de3 34c4d 334bd 2333d 2232c 2223b 22322 22221 12221 11211 11111 01110"),
        ("right_leg", "north"): F("44443 54433 44332 43322 33322 43322 33221 32221 22211 21110"),
        ("right_leg", "south"): F("44433 44333 43332 33322 33322 33322 33221 32211 22211 21110"),
        ("left_leg", "north"): F("34444 33445 2ed44 d3c34 d3b34 2c334 12s33 12223 12223 01122"),
        ("left_leg", "south"): F("34443 33444 23ed4 2c3d4 23b34 22334 22233 12233 12223 01122"),
        ("right_boot", "north"): F("44443 54332 10000"), ("right_boot", "south"): F("44433 43322 00000"),
        ("left_boot", "north"): F("4d443 4s332 00000"), ("left_boot", "south"): F("44d33 43322 00000"),
        ("front_cloth", "north"): F("adcdcba ghijjhg ghdddhg gdijhdg gdisgdg ghdihdg ghidddg ghiijhg ghijdhg ghiidhg gghiihg ghhijhg gghhhgg ggggggg"),
        ("cape", "north"): F("""abcddcbaghg ghddddhiihg gdhiijdhihg dhiodijdihg dhiidijdhhg gdhhdhjdhgg ghddhhdhhgg ghiiihdhhhg
            ghhiihgdhig ghiiihgdhig ghhiihgdhhg ghiihhdhhhg ghiiidhhhig ghhidhghiig ghiidhghiig gghidhghihg ghhihdghhhg
            ghiihgdhihg gghihggdhhg ghhhgggwhgg gghhgghhggg gghggghgggg ggggggggggg"""),
    },
})

# ===========================================================================
# Conversione e composizione (nessun disegno qui)
# ===========================================================================
up, on_bg, SLOT = H.up, H.on_bg, H.SLOT


def render_icon(grid, pal):
    return H.render(grid, pal)


def armor_faces(p):
    f = dict(BASE)
    f.update(p["faces"])
    return f


def render_armor(p):
    grid = M.armor_grid(armor_faces(p))
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), p["pal"][ch])
    return im


def ref_icon(name):
    return Image.open(os.path.join(REF, f"bedrock_{name}.png")).convert("RGBA")


def ref_armor():
    return Image.open(os.path.join(REF, "bedrock_armor_indossata.png")).convert("RGBA")


def confronto(p, icons, tex, path):
    """Autocritica: per ogni oggetto la bedrock di Spectrum sopra e il nostro sotto, x12; poi i due manichini."""
    k, pad = 12, 8
    cw = 16 * k + pad
    man_ref = on_bg(M.mannequin(ref_armor(), 6), (60, 60, 70, 255))
    man = on_bg(M.mannequin(tex, 6), (60, 60, 70, 255))
    W = max(len(ITEMS) * cw + pad, 2 * man.width + 3 * pad)
    Hh = 2 * cw + pad + 44 + man.height + pad
    sheet = Image.new("RGBA", (W, Hh), (24, 22, 30, 255))
    for i, n in enumerate(ITEMS):
        x = pad + i * cw
        sheet.alpha_composite(on_bg(up(ref_icon(n), k)), (x, pad))
        sheet.alpha_composite(on_bg(up(icons[n], k)), (x, cw + pad))
        sheet.alpha_composite(on_bg(icons[n]), (x, 2 * cw + 12))
        sheet.alpha_composite(on_bg(up(icons[n], 2)), (x + 24, 2 * cw + 4))
    y = 2 * cw + 44
    sheet.alpha_composite(man_ref, (pad, y))
    sheet.alpha_composite(man, (2 * pad + man.width, y))
    sheet.save(path)


def build(p):
    d = os.path.join(OUT, p["slug"])
    os.makedirs(d, exist_ok=True)
    icons = {n: render_icon(p["items"][n], p["pal"]) for n in ITEMS}
    for n, im in icons.items():
        im.save(os.path.join(d, f"t6_{n}.png"))
    tex = render_armor(p)
    tex.save(os.path.join(d, "t6_armor.png"))
    worn = M.mannequin(tex, 8)
    on_bg(worn, (60, 60, 70, 255)).save(os.path.join(d, "manichino.png"))
    confronto(p, icons, tex, os.path.join(d, "confronto.png"))
    return icons, tex


CONSIGLIATA = 4  # proposta scelta dall'utente: Castone
T7_DIR = os.path.join(ROOT, "design", "texture", "proposte", "t7")  # vero T7 Umbral (texture_t7.py), non t6-mix


def t7_icons():
    return {n: Image.open(os.path.join(T7_DIR, "set", f"t7_{n}.png")).convert("RGBA") for n in ITEMS}


def t7_mannequin():
    """Texture indossata del T7 Umbral già generata da texture_t7.py: solo lettura."""
    return Image.open(os.path.join(T7_DIR, "armatura", "t7_armor.png")).convert("RGBA")


def panoramica(path, built):
    k, cell, lab = 6, 16 * 6 + 10, 190
    big, txt = ImageFont.load_default(size=26), ImageFont.load_default(size=14)
    rows = [("Spectrum", "bedrock (metro)", {n: ref_icon(n) for n in ITEMS}, ref_armor())]
    rows += [(str(p["n"]), p["nome"], built[p["n"]][0], built[p["n"]][1]) for p in PROPOSTE]
    rows.append(("T7", "Umbral (vibranium)", t7_icons(), t7_mannequin()))
    man_w = M.mannequin(ref_armor(), 3).width
    W = lab + len(ITEMS) * cell + man_w + 24
    rh = max(cell, 36 * 3 + 10)
    sheet = Image.new("RGBA", (W, len(rows) * rh + 16), (20, 18, 26, 255))
    dr = ImageDraw.Draw(sheet)
    for r, (a, b, icons, tex) in enumerate(rows):
        y = 8 + r * rh
        col = (232, 151, 37) if a not in ("Spectrum", "T7") else (165, 157, 177)
        dr.text((12, y + 20), a, fill=col, font=big)
        dr.text((12, y + 54), b, fill=(200, 196, 210), font=txt)
        for i, n in enumerate(ITEMS):
            sheet.alpha_composite(on_bg(up(icons[n], k)), (lab + i * cell, y))
        sheet.alpha_composite(on_bg(M.mannequin(tex, 3), (60, 60, 70, 255)), (lab + len(ITEMS) * cell + 8, y))
    sheet.save(path)


def b64(im):
    if isinstance(im, (bytes, bytearray)):
        data = bytes(im)
    else:
        buf = io.BytesIO()
        im.save(buf, format="PNG")
        data = buf.getvalue()
    return "data:image/png;base64," + base64.b64encode(data).decode()


CSS = H.CSS + """
.items .slot img { width: 128px; height: auto; max-width: 92%; }
.worn { display: flex; flex-wrap: wrap; gap: 10px; align-items: flex-start; }
.worn .slot { padding: 8px; max-width: 100%; }
.worn img { width: 252px; height: auto; max-width: 100%; }
.worn figcaption { max-width: 252px; }
.vs { display: grid; gap: 14px; border-top: 1px solid var(--line); padding-top: 16px; }
.vs h3 { font-size: 11px; letter-spacing: .08em; text-transform: uppercase; color: var(--muted); margin: 0; font-weight: 600; }
.vsrow { display: grid; grid-template-columns: 110px minmax(0, 1fr); gap: 10px 16px; align-items: center; }
.vsrow .who { font-family: var(--display); font-size: 18px; line-height: 1.1; }
.vsrow .who small { display: block; font-family: var(--text); font-size: 12px; color: var(--muted); }
.vsrow .set { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; min-width: 0; }
.vsrow .set .slot { width: 72px; height: 72px; } .vsrow .set .slot img { width: 64px; height: 64px; }
.vsrow .set .man img { width: 168px; height: auto; max-width: 100%; }
.vsrow .set .man { width: auto; height: auto; padding: 4px; max-width: 100%; }
@media (max-width: 720px) {
  .vsrow { grid-template-columns: 1fr; }
  .vsrow .set .slot { width: 52px; height: 52px; } .vsrow .set .slot img { width: 48px; height: 48px; }
}
"""


def html(path, built):
    def slot_row(sp, size):
        return "".join(f'<div class="slot {size}"><img src="{b64(sp[it])}" alt="{NOMI[it]}"></div>' for it in ITEMS)

    def man(tex, alt):
        return on_bg(M.mannequin(tex, 6), (60, 60, 70, 255)), alt

    refs = {it: ref_icon(it) for it in ITEMS}
    nav = "".join(f'<a href="#proposta-{p["n"]}">{p["n"]}</a>' for p in PROPOSTE)
    parts = [f"""<title>Trichromatic su bedrock</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&family=Pixelify+Sans:wght@500;600&display=swap">
<style>{CSS}</style>
<header class="intro"><div class="wrap">
  <h1>Trichromatic su bedrock <span class="tri" aria-hidden="true"><i style="background:var(--c)"></i><i style="background:var(--m)"></i><i style="background:var(--y)"></i></span></h1>
  <p class="lead">Cinque proposte per il T6 ridisegnato da capo. In tutte le parti strutturali sono <b>bedrock</b>
  grigio-viola, come il tier che viene prima; l'<b>allthemodium arancio</b> fa da metallo per rinforzi, guardie, ghiere e bordi;
  il topazio ciano, l'ametista magenta e il citrino giallo sono <b>gemme vive</b>. Cambiano quanto arancio c'è, come entrano le gemme,
  il motivo e le forme. Ogni sprite e ogni faccia dell'armatura indossata è una griglia scritta pixel per pixel.
  La scelta è la <b>4, Castone</b>, che si lega meglio al T7: il confronto è sotto la sua sezione.</p>
</div></header>
<div class="ref"><div class="wrap">
  <div class="lbl">Metro di paragone<b>Spectrum classico, bedrock</b></div>
  <div class="row">{"".join(f'<div class="slot"><img src="{b64(refs[it])}" alt="bedrock {NOMI[it]}"></div>' for it in ITEMS)}</div>
  <nav aria-label="Vai alla proposta">{nav}</nav>
</div></div>
<main class="wrap">"""]
    for p in PROPOSTE:
        icons, tex = built[p["n"]]
        best = p["n"] == CONSIGLIATA
        figs = "".join(f'<figure><div class="slot"><img src="{b64(icons[it])}" alt="{NOMI[it]} {p["nome"]}"></div>'
                       f'<figcaption>{NOMI[it]}</figcaption></figure>' for it in ITEMS)
        tags = "".join(f'<div><dt>{k}</dt><dd>{v}</dd></div>' for k, v in p["tags"].items())
        m_img, _ = man(tex, "")
        extra = ""
        if best:
            t7 = t7_icons()
            rows = [("T6", "Castone (scelta)", icons, tex), ("T7", "Umbral", t7, t7_mannequin())]
            vs = "".join(
                f'<div class="vsrow"><div class="who">{a}<small>{b}</small></div><div class="set">'
                + "".join(f'<div class="slot"><img src="{b64(ic[it])}" alt="{a} {NOMI[it]}"></div>' for it in ITEMS)
                + f'<div class="slot man"><img src="{b64(on_bg(M.mannequin(tx, 4), (60, 60, 70, 255)))}" alt="{a} armatura indossata"></div>'
                + "</div></div>" for a, b, ic, tx in rows)
            extra = f"""
  <div class="vs"><h3>Confronto con il T7: i due tier si distinguono a colpo d'occhio</h3>{vs}
    <p class="note">Il Castone è bedrock grigio-viola con un sottile filo d'allthemodium lungo un solo spigolo e un castone fuori asse
    che stringe insieme le tre gemme CMY, come le facce di un prisma. Il T7 risponde con il metallo scuro e freddo, i cristalli d'onice
    e il vibranium verde acqua al posto dell'arancio: stessa idea di pietra incastonata nel metallo, materiali e colori opposti.</p></div>"""
        parts.append(f"""
<section class="style{' best' if best else ''}" id="proposta-{p['n']}">
  <div class="head"><div class="num">{p['n']}</div><h2>{p['nome']}{'<small>scelta</small>' if best else ''}</h2><p>{p['desc']}</p></div>
  <dl class="tags">{tags}<div class="wide"><dt>Asimmetria</dt><dd>{p['asim']}</dd></div></dl>
  <div class="items">{figs}</div>
  <div class="lower">
    <div class="block"><h3>Dimensione reale 1×</h3><div class="real">{slot_row(icons, 's1')}</div></div>
    <div class="block"><h3>2×</h3><div class="real">{slot_row(icons, 's2')}</div></div>
    <div class="block"><h3>Indossata, davanti e dietro</h3><div class="worn"><figure><div class="slot"><img src="{b64(m_img)}" alt="manichino {p['nome']}"></div>
      <figcaption>Modello 128×128 di Spectrum, con mantello e perizoma</figcaption></figure></div></div>
  </div>{extra}
</section>""")
    parts.append("""
</main>
<footer><div class="wrap">Disegni originali per Spectral Ascension, scritti a mano pixel per pixel studiando la tecnica di Spectrum
(asimmetria, tre materiali, contorni tinti, luce a grappoli), senza copiare né ricolorare texture di Spectrum o Allthemodium.</div></footer>
""")
    with open(path, "w") as fh:
        fh.write("".join(parts))


# ===========================================================================
# RIFINITURA 02_colata_r1: colata ridotta di 1-2 px di spessore, gemme CMY grandi 2x2 col loro punto di luce
# (t/p/x = luce, s/o/w = corpo, r/n/v = ombra). Griglie scritte a mano, come per le proposte.
# ===========================================================================
R1_ITEMS = {
    "sword": G("""
        .............a00
        ............ae50
        ...........aed40
        ..........aets40
        .........aesr540
        ........aed54330
        .......aecpo430.
        ..aa..adcno420..
        .adeaadc55320...
        .aefedcxw5420...
        ..aeddcvw320....
        ...abcbcca0.....
        ...0420ab.c.....
        ..0430..b.......
        adca0...........
        aoa.............
    """),
    "axe": G("""
        ................
        .........aa000..
        ........aed5540.
        .......aed5ts320
        ......aed54sr210
        .....adc54po3210
        ....aed544no320.
        ....aba..04320..
        .....040..0000..
        ....040.........
        ...0420.........
        ..0430..........
        .axwa...........
        .avwa...........
        ..aa............
        ................
    """),
    "helmet": G("""
        ................
        ................
        .....aaa000.....
        ...aaed655400...
        ..aed65544ts30..
        .aedc65544sr20..
        .adc5554433210..
        .ac00nopoon010..
        .ab00400n04210..
        .abd03200.xw10..
        ..bc0210..vw00..
        ..aa.c0...000...
        .....b..........
        ................
        ................
        ................
    """),
    "chestplate": G("""
        ................
        .aaa......0000..
        adea00000054440.
        aedca55665544320
        adcdca6655443210
        .abdcca54ts43210
        ..abdca44sr3210.
        ..0abdca44po210.
        ..04abcca4no210.
        ..0544abcb4xw10.
        ..05433aba3vw10.
        ..0432aba321110.
        ..04320ab021100.
        ..0321..c.01110.
        ...00...b..000..
        ................
    """),
    "leggings": G("""
        ................
        ................
        ..addca0000000..
        ..aedcba554440..
        ..adcba5554320..
        ..abca00443210..
        ..adc40..0ts20..
        ..acd30..0sr20..
        ..abc40..04po0..
        ..0abc0..04no0..
        ..04a30..0xw20..
        ..04330..0vw20..
        ..0320...03210..
        ..0000...00000..
        ................
        ................
    """),
    "boots": G("""
        ................
        ................
        ................
        ..aaaa....0000..
        ..adea0...0ts0..
        ..acd40...0sr0..
        ..abc40...0430..
        ..0b440...0po0..
        ..04340...0no0..
        .054330...04320.
        06ts430...05xw20
        05sr220...05vw20
        0000000...000000
        ................
        ................
        ................
    """),
}

R1_FACES = {
    ("head_out", "north"): F("aedd655443 adcb655ts3 acb5544sr2 aba5444332 a0nopoon01 ab00nn3221 ab30343221 ab3033xw11 aba022vw10 aa00110000"),
    ("head_out", "south"): F("34455555ea 23445555ca 23344545da 12334444cb 12233434bc 11223333ca 01122334ba 00112234bc 00011224ba 00001134ba"),
    ("body_out", "north"): F("adea005654 aedcb55ts3 adcba54sr3 abcdca3322 4abcca3po2 44abcbano2 344aba2221 3334abaxw1 33322c2vw0 2222b11100"),
    ("body_out", "south"): F("455565deda 44555bdcda 44455abdcb 344444bcba 33444334ba 33343324ba 233332223b 2223222111 2222211110 1111100000"),
    ("right_arm", "north"): F("adeda adcb5 acb55 abc45 0ab34 04ab3 043b3 04c33 03b33 032b3 03213 03210 02110"),
    ("right_arm", "south"): F("adeda bcdc5 abc45 abb45 acb40 ab340 ab340 ab340 ac430 a3230 a1230 01230 01120"),
    ("left_arm", "north"): F("45554 44543 34ts3 33sr2 33332 3po32 2no22 22322 22xw1 12vw1 11211 11111 01110"),
    ("left_arm", "south"): F("45554 44543 34443 33433 23333 22322 22322 22221 12221 12221 11211 11111 01110"),
    ("right_leg", "north"): F("adcda acdb3 abc33 abc30 ab330 a3330 a3320 33221 32211 21110"),
    ("right_leg", "south"): F("adcda abdca abcba 0acba 03aba 033ba 0332a 32221 22211 21110"),
    ("left_leg", "north"): F("34444 33ts5 23sr4 22334 2po44 2no34 12233 12xw3 12vw3 01122"),
    ("left_leg", "south"): F("34443 33444 23344 22334 23334 22334 22233 12233 12223 01122"),
    ("right_boot", "north"): F("abda4 ac332 00000"), ("right_boot", "south"): F("acdb4 23bc4 00000"),
    ("left_boot", "north"): F("4ts43 4xw32 00000"), ("left_boot", "south"): F("44433 43322 00000"),
    ("front_cloth", "north"): F("adedhhg acdchhg abdhhjg gachhig ghhaijg ghhaihg ghaiijg ghbijhg ghaiihg gghiihg ghhijhg gghiihg ghhhhgg ggggggg"),
    ("cape", "north"): F("""adeedhgghhg aedcdhhhihg adcdchhijhg abcdchhijhg gacdhaiijhg gabhahijihg ghahaiijhhg ghahahijhgg
        ghiaaiihhgg ghhhbiihhig ghiapohhhig ghiinhgghhg ghiiihghhig ghhiihghiig ghiihhghiig gghihhghihg ghhihgghhhg
        ghiihgghihg gghihggghhg ghhhggghhgg gghhgghhggg gghggghgggg ggggggggggg"""),
    # lati: bande e spallaccio arancio, ridotti di una colonna / un pixel
    ("right_arm", "west"): F("55554 54443 44433 44333 43332 ab4ba 43322 bcd4b ac4ca ab4ba 33221 32211 21110"),
    ("right_arm", "east"): F("44443 44333 43332 33322 33222 ab4ba 32222 bcd4b ac4ca ab4ba 22211 22111 11110"),
    ("right_leg", "west"): F("44433 44333 43332 ab4ba 33322 43322 33222 32221 22211 21110"),
    ("right_leg", "east"): F("44333 43332 43322 ab4ba 33222 33222 32221 32211 22111 11110"),
    ("right_arm_extra", "north"): F("adeed5 acdd54 abcb54 0aaba0"),
    ("right_arm_extra", "south"): F("acdd54 abc544 abb544 0aaaa0"),
    ("right_arm_extra", "west"): F("aded54 acdc54 abcb54 0abaa0"),
    ("right_arm_extra", "east"): F("bcde54 bccd54 abc544 0aaba0"),
    ("right_arm_extra", "top"): F("cdee55 deff54 deff54 cdee54 bcdd54 abcc54"),
}


def r1_proposta():
    base = next(p for p in PROPOSTE if p["slug"] == "02_colata")
    faces = dict(base["faces"])
    for k, v in R1_FACES.items():
        old = faces[k] if k in faces else BASE.get(k, v)
        assert len(old) == len(v) and all(len(a) == len(b) for a, b in zip(old, v)), (k, old, v)
        faces[k] = v
    p = dict(base)
    p.update({"slug": "02_colata_r1", "nome": "Colata r1", "items": R1_ITEMS, "faces": faces})
    return p


def ore(grid_rows):
    return sum(ch in "abcdef" for r in grid_rows for ch in r)


def prima_dopo(old, new, path):
    k, pad = 12, 8
    cw = 16 * k + pad
    sheet_w = len(ITEMS) * cw + pad
    man_o = on_bg(M.mannequin(render_armor(old), 6), (60, 60, 70, 255))
    man_n = on_bg(M.mannequin(render_armor(new), 6), (60, 60, 70, 255))
    Hh = 2 * cw + pad + man_o.height + pad
    sheet = Image.new("RGBA", (max(sheet_w, 2 * man_o.width + 3 * pad), Hh), (24, 22, 30, 255))
    for i, n in enumerate(ITEMS):
        x = pad + i * cw
        sheet.alpha_composite(on_bg(up(render_icon(old["items"][n], old["pal"]), k)), (x, pad))
        sheet.alpha_composite(on_bg(up(render_icon(new["items"][n], new["pal"]), k)), (x, cw + pad))
    y = 2 * cw + pad
    sheet.alpha_composite(man_o, (pad, y))
    sheet.alpha_composite(man_n, (2 * pad + man_o.width, y))
    sheet.save(path)


def build_r1():
    old = next(p for p in PROPOSTE if p["slug"] == "02_colata")
    new = r1_proposta()
    build(new)
    prima_dopo(old, new, os.path.join(OUT, new["slug"], "prima_dopo.png"))
    for n in ITEMS:
        print(n, "arancio", ore(old["items"][n]), "->", ore(new["items"][n]))
    ta = lambda p: sum(1 for px in render_armor(p).getdata() if px[3])
    print("armor pixel colorati identici nel conteggio:", ta(old), ta(new))


def main():
    only = [int(a) for a in sys.argv[1:] if a.isdigit()]
    os.makedirs(OUT, exist_ok=True)
    built = {}
    for p in PROPOSTE:
        if only and p["n"] not in only:
            continue
        built[p["n"]] = build(p)
    if not only or 20 in only:
        build_r1()
    if only:
        return
    panoramica(os.path.join(OUT, "panoramica.png"), built)
    html(os.path.join(OUT, "stili.html"), built)


if __name__ == "__main__":
    main()
