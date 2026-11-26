"""T6 Trichromatic «Castone»: il set completo e animato della proposta 4 approvata (t6-v2/04_castone).

Tutto disegnato A MANO: ogni sprite 16x16 e ogni faccia dell'armatura indossata è una griglia ASCII scritta
carattere per carattere. Il codice converte le griglie in PNG, le dispone nel layout UV 128x128 di Spectrum,
calcola i fotogrammi dell'animazione e compone anteprime e pagina; non disegna forme, contorni, ombre o luci.

- Palette, facce comuni dell'armatura, corazza e gambali sono una COPIA congelata delle griglie della proposta 4 di
  tools/texture_t6v2.py: quello script può cambiare, questo no. main() verifica che cesoie, canna (normale e
  lanciata) e corazza (icone, animazioni e le facce nella texture dell'armatura) coincidano pixel per pixel con prima/.
- QUARTO giro: ascia (filo di 1 px sul taglio a sinistra, stessa forma), zappa (forma del secondo giro, oro sul filo
  basso/sinistro della lama, castone sull'attacco), elmo (10 righe, fronte più alta), arco (ametista dentro il flettente,
  come appoggio della freccia). Il terzo giro sta in prima/, il secondo in prima_giro2/.
- Ultimo giro di correzioni (stesso stile: filo d'allthemodium su un solo spigolo, castone a prisma fuori asse con topazio,
  ametista e citrino): spada (guardia più stretta), ascia (testa a mezzaluna compatta, filo sul taglio, castone alla
  base della testa), pala (bastone più lungo), zappa (testa ad angolo retto), arco (variante B: gemme sulle punte dei
  flettenti, freccia lunga; t6_bow, t6_bow_pulling_0/1/2), elmo (più alto), gambali (gambe più larghe), stivali (altezza
  intermedia, gambale più largo). Approvati e invariati: cesoie, canna (normale e lanciata), corazza.
- Il «prima» (il giro precedente) è copiato in design/texture/proposte/t6-castone/prima/ (set/ e armatura/), per i
  confronti di prima_dopo.png.
- Layout UV e manichino si importano da tools/texture_mix.py (solo import, non si modifica).

ANIMAZIONE (scelta dell'utente, invariata): ciclo di 5 s = 100 tick = 20 fotogrammi da 5 tick, interpolate false.
  f0       riposo: il fotogramma 0 è identico all'icona ferma
  f1-f5    un'onda di luce corre lungo il filo d'allthemodium verso il castone (i pixel lontani si accendono prima)
  f5-f8    si accende il topazio (ciano), f9-f12 l'ametista (magenta), f13-f16 il citrino (giallo): la gemma di
           turno sale di un gradino e le altre due scendono di uno; intanto la montatura arancio resta più chiara
  f17-f18  respiro: arancio e gemme scendono di un gradino; f19 torna al riposo
  La bedrock (0-6) e i contorni delle gemme (q, m, u) non cambiano mai. Le rampe sono quelle della palette.
  L'ordine di accensione è uguale per tutti i pezzi, quindi gli stati dell'arco e le 2 canne restano sincroni.

Uso:  python3 tools/texture_t6castone.py     # scrive SOLO in design/texture/proposte/t6-castone/
"""
import base64
import io
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_hand as H  # noqa: E402
import texture_mix as M  # noqa: E402  (solo import: tabella UV e manichino)

ROOT = H.ROOT
OUT = os.path.join(ROOT, "design", "texture", "proposte", "t6-castone")
T7 = os.path.join(ROOT, "design", "texture", "proposte", "t7")
REF = H.REF


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
    ("right_boot", "west"): F("abcda 4cd22 01110"), ("right_boot", "east"): F("acdba 22dc4 01110"),
    ("right_boot", "top"): LEG_TOP, ("right_boot", "bottom"): ["00000"] * 5,
    ("left_boot", "west"): F("54332 4cd22 01110"), ("left_boot", "east"): F("23345 22dc4 01110"),
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



# ===========================================================================
# I 6 PEZZI APPROVATI (copia congelata della proposta 4 di tools/texture_t6v2.py)
# ===========================================================================
CASTONE = {
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
            ............0ff0
            ...........0e5f0
            ..........0d54f0
            .........0c54e0.
            ....00..0c43d0..
            ...04300b42c0...
            ..05dtc032b0....
            ..0dtsrb2b0.....
            .0dxwponb0......
            ..0bccdcba0.....
            ...040bc40......
            ..040.0b0.......
            .040...0........
            adc.............
            ab..............
        """),
        "axe": G("""
            ................
            .........00.....
            ........0de0....
            .......0cddd00..
            ......0bcd3220..
            ......0bc32210..
            ......0b321110..
            ........021110..
            .......dtc.100..
            ......dwo..00...
            .....abb........
            ....040.........
            ...040..........
            ..040...........
            ..adc...........
            ..ab............
        """),
        "helmet": G("""
            ................
            ................
            .....000000.....
            ...0054544dc0...
            ..0543443dtc20..
            ..0dcb43dwsoc0..
            ..0310abcba0b0..
            ..041......140..
            ..042......240..
            ..0432....2340..
            ..0432....2340..
            ...000....000...
            ................
            ................
            ................
            ................
        """),
        # CORAZZA (nota 3): la corazza del vecchio «set scuro t6-mix» (come nella riga «set scuro t6-mix (prima)» di
        # t7/panoramica.png) COSÌ COM'È: stessa forma, occhio, spallacci e cintura arancio. Le lettere sono le stesse, quindi il
        # metallo scuro (0-6) prende il nero e grigio scuro della bedrock Castone.
        "chestplate": G("""
            ................
            ................
            00000.0cd0.00000
            06e540bcdb045e60
            05543adeeda34550
            05433afomfa3340.
            .0443acddca3440.
            .03343444434330.
            ..034556654430..
            ..034454454330..
            ..022bcddcb220..
            ..022acddca220..
            ..034451154430..
            ..033441144330..
            ..022331133220..
            ..000000000000..
        """),
        "leggings": G("""
            ................
            ................
            ..000000000000..
            ..0666554d5440..
            ..acdcdcdtc440..
            ..05543dwsob30..
            ..05542abcba30..
            ..05530..04210..
            ..dc430..04320..
            ..e4430..05430..
            ..d4320..04320..
            ..04430..03320..
            ..03320..04430..
            ..00000..00000..
            ................
            ................
        """),
        "boots": G("""
            ................
            ................
            ................
            ..00000..00000..
            ..05430..0d430..
            ..05330..0d330..
            ..04320..dtsc0..
            ..05430.dxwpob..
            ..04320.abccba..
            ..04320..04320..
            .065430..034560.
            0adc430..034cda0
            0aaa000..000aaa0
            ................
            ................
            ................
        """),
    },
    "faces": {
        # elmo (giro occhi: fessura dalla riga 5 su 8 colonne, piastra interna righe 4-5 aperte sulle colonne 1-7 = occhi del giocatore liberi; icona: fessura riga 8 su 8 colonne, volto 6): castone e filo arancio sulla fronte (righe 0-4); l'apertura trasparente (volto) parte dalla riga 5 ed è larga
        # 6 colonne nel mezzo (un pixel in più per lato e in alto rispetto al giro precedente), 8 sul mento.
        # Ogni faccia interna (9x9) ripete il disegno dell'esterna (10x10): stessa apertura, stesso castone, stessi fili.
        ("head_out", "north"): F("0455555de0 4566544cd3 45543dtc32 adcbdwsob1 3310abcba1 4........1 40......01 30......01 20......01 10......00"),
        ("head_in", "north"): F("045555de0 4543dtc32 adbdwsob1 310abcba1 4.......1 4.......1 30.....01 20.....01 10.....00"),
        # dietro: solo ombreggiatura a grappoli di bedrock più chiari e più scuri (il filo arancio in alto a destra è quello di prima)
        ("head_out", "south"): F("3445655de4 3345665cd4 2344455bc3 2223444ab2 2113344432 1122333532 1122234422 0111133322 0012323221 0001122110"),
        ("head_in", "south"): F("345655de4 335665cd4 234455bc3 223444ab2 213344432 112234422 011133322 002323221 001122110"),
        ("head_in", "west"): F("455554332 555443322 454443221 bcdccbba1 443332211 343221110 332221100 322111000 abba11000"),
        ("head_in", "east"): F("233455554 223445555 122344454 1abcbcdcb 112233344 011122343 001122233 000111223 000111aba"),
        ("head_in", "top"): F("344554433 455655443 456655433 455644332 455543432 344433222 234332221 233322211 122221110"),
        ("head_in", "bottom"): F("222222222 2.......2 2.......2 2.......2 2.......2 2.......2 2.......2 2.......2 22.....22"),
        # ritocco utente (armatura1): la rima di sotto dell'elmo esterno si apre sul mento come quella interna
        ("head_out", "bottom"): F("2222222222 2........2 2........2 2........2 2........2 2........2 2........2 2........2 2........2 22......22"),
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
        ("right_boot", "north"): F("45543 4dc32 01110"), ("right_boot", "south"): F("34554 23cd4 01110"),
        ("left_boot", "north"): F("3dtc3 dwsob 01110"), ("left_boot", "south"): F("34554 23322 01110"),
        ("front_cloth", "north"): F("ghhdhhg ghdtchg gdwsobg gabcbag ghijihd ghiijhc ghijihd ghiihhc ghijihb gghiihc ghhijhb gghiihc ghhhhgb gggggga"),
        ("cape", "north"): F("""abcdcbaghhg ghhdhhgihhg ghdtchgihhg gdwsobgijhd gabcbaiihgc ghiijihhhgd ghiiihhghgc ghijihhghhb ghhiihgghgc ghiiihgghgd ghhiihgghhc ghiihhgghgb ghiiihghhgc ghhiihghigd ghiihhghigc gghihhghihb ghhihgghhgc ghiihgghigb gghihggghga ghhhggghhgb gghhgghhgga gghggghggga ggggggggggg"""),
    },
}


PAL = CASTONE["pal"]
APPROVED = CASTONE["items"]          # sword, axe, helmet, chestplate, leggings, boots
# ===========================================================================
# PEZZI NUOVI E RIFATTI, disegnati a mano
# (correzioni dell'utente: ascia a mezzaluna, pala con bastone lungo, zappa con testa ad angolo retto, cesoie chiuse sulla
#  struttura delle T7, arco variante B, elmo, gambali e stivali ritoccati)
# ASCIA (in CASTONE["items"]): testa compatta a mezzaluna con il taglio curvo arancio, corno alto e barba in basso, castone
# attaccato alla base della testa. È l'unica ascia del set (t6_axe).
# ===========================================================================
NEW = {}

# PALA: lama larga e piatta, sagoma a losanga inclinata come la bedrock di Spectrum, filo arancio sullo spigolo di taglio, castone al collare, sotto la
# lama a sinistra. Il bastone è lungo: 7 passi in diagonale fino al pomolo.
NEW["shovel"] = G("""
    ................
    ................
    ...........0ef..
    ..........0deef.
    .........064def.
    ........05432de.
    .........0321c..
    ........04010...
    .......dtc.0....
    ......dwo.......
    .....abc........
    ....040.........
    ..bb40..........
    ..adc...........
    ...ab...........
    ................
""")

# ZAPPA: testa in cima ad angolo retto come la zappa di bedrock di Spectrum (barra orizzontale e lama piatta che scende sulla
# destra), manico diagonale. Il filo arancio corre sul bordo di taglio della lama, il castone sta sull'attacco testa-manico.
NEW["hoe"] = G("""
    ......0000......
    ......f6540.....
    ......ee54300...
    .......ddc3320..
    .........cb320..
    ..........b320..
    .........04320..
    ........dtc0320.
    .......dwo.0210.
    ......abc...00..
    .....040........
    ....040.........
    ...040..........
    ..adc...........
    ..ab............
    ................
""")

# CESOIE: struttura delle T7 (due lame chiuse una sull'altra in diagonale, perno, due anelli per le dita sfalsati). La lama
# alta ha il filo arancio sullo spigolo, il castone fa da perno, gli anelli sono di bedrock con il foro trasparente.
NEW["shears"] = G("""
    ...........0000.
    ..........054ff0
    .........054ee0.
    ........054de0e0
    .......054dd0e50
    ......054cc0d540
    .....054cc0c540.
    .....04bb0c540..
    .000.03b0b540...
    055500dtsc00....
    05.50dxwpob.....
    05550.abcc......
    .000..04550.....
    ......05.50.....
    ......05550.....
    .......000......
""")

# ARCO (variante B scelta dall'utente): gemme sulle punte dei flettenti, topazio alla punta alta, citrino alla punta bassa,
# una piccola ametista sul bordo interno del flettente basso. Corda (6) come prima. La freccia è bianca (W): punta a prisma
# di tre gemme (ciano, giallo, magenta) che attraversa l'angolo dell'arco in alto a sinistra (come nel T7 e nell'arco di
# Spectrum), asta bianca fino alla corda; la corda tirata si sposta lungo la stessa freccia nei tre stati di tensione.
# FRECCIA DINAMICA: stessa lunghezza (11 px: punta 4, asta 5, alette 2) nei tre stati; in pulling_0 sporge in avanti oltre i
# flettenti, poi arretra di un passo in diagonale per stato (punta a 0, 1, 2 passi dall'angolo) con la coda sul vertice della corda.
BOWS = {}
BOWS["bow"] = G("""
    ................
    .......ee6565tsc
    .....dd562222rqb
    ....c532....6...
    ...cpo2....6....
    ..d5nm....6.....
    ..d42....6......
    .e53....6.......
    .e52...6........
    .42...6.........
    .42..6..........
    .52.6...........
    .426............
    .xw.............
    .vu.............
    .cb.............
""")

BOWS["bow_pulling_0"] = G("""
    Wt..............
    xo.....ee6565tsc
    ..6..dd562222rqb
    ...6c532....6...
    ...c6o2.....6...
    ..d5n6.....6....
    ..d42.6....6....
    .e53...c..6.....
    .e52....d6......
    .42.....6.......
    .42....6........
    .52..66.........
    .4266...........
    .xw.............
    .vu.............
    .cb.............
""")

BOWS["bow_pulling_1"] = G("""
    ................
    .Wt....ee6565tsc
    .xo..dd562222rqb
    ...6c532....6...
    ...c6o2.....6...
    ..d5n6.....6....
    ..d42.6....6....
    .e53...6..6.....
    .e52....c.6.....
    .42......d6.....
    .42....666......
    .52..66.........
    .4266...........
    .xw.............
    .vu.............
    .cb.............
""")

BOWS["bow_pulling_2"] = G("""
    ................
    .......ee6565tsc
    ..Wt.dd562222rqb
    ..xoc532.....6..
    ...c6o2......6..
    ..d5n6......6...
    ..d42.6.....6...
    .e53...6....6...
    .e52....6..6....
    .42......c.6....
    .42.......d6....
    .52.....666.....
    .42..666........
    .xw66...........
    .vu.............
    .cb.............
""")


# CANNA: asta di bedrock in diagonale, il filo d'allthemodium solo sul tratto basso, che entra nel castone
# (il mulinello, fuori asse sotto l'asta). Lenza (6) che pende dalla punta fino al galleggiante arancio e bedrock.
NEW["fishing_rod"] = G("""
    ................
    ............56..
    ...........50.6.
    ..........50..6.
    .........d0...6.
    ........e0....6.
    .......e0.....6.
    ......d0......6.
    .....e0.......6.
    ....d0d.......e.
    ...e0dtc...e.cfc
    ..d0dwsob..cdffd
    .40.abcba...dce.
    430.............
    adc.............
    ab..............
""")
# CANNA LANCIATA: stesso manico e stesso mulinello, la punta si piega in avanti e la lenza sale dritta fuori
# dal bordo alto, senza galleggiante.
NEW["fishing_rod_cast"] = G("""
    ................
    ............56..
    ...........50...
    ..........60....
    .........d0.....
    ........e0......
    .......e0.......
    ......d0........
    .....e0.........
    ....d0d.........
    ...e0dtc........
    ..d0dwsob.......
    .40.abcba.......
    430.............
    adc.............
    ab..............
""")


ITEMS = {**APPROVED, **NEW, **BOWS}
BOW_NAMES = ["bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2"]
ORDER = ["sword", "axe", "shovel", "hoe", "shears"] + BOW_NAMES + ["fishing_rod", "fishing_rod_cast", "helmet",
                                                                  "chestplate", "leggings", "boots"]
SET_ORDER = ORDER
NOMI = dict(M.NOMI)
NOMI.update({"axe": "ascia"})


def t7key(n):
    """Nome del pezzo T7 che corrisponde a un pezzo T6 (stesso nome)."""
    return n



# ---------------------------------------------------------------------------
# BUSTO INDOSSATO (nota 4): le facce del torso, delle braccia e degli spallacci riprendono la nuova icona della corazza.
# Mappa: colonna del corpo = colonna dell'icona - 3 (il torso largo 10 copre le colonne 3-12 dell'icona), riga del corpo =
# riga dell'icona - 3; le braccia (5 colonne) sono le colonne 0-4 (destro del personaggio, a sinistra dell'icona) e 11-15
# (sinistro); l'occhio sta al centro del petto, la cintura arancio sotto di esso, gli spallacci hanno la scintilla arancio.
# ---------------------------------------------------------------------------
CHEST_FACES = {
    ("body_out", "north"): F("540bcdb045 43adeeda34 33afomfa33 43acddca34 3434444343 3455665443 3445445433 22bcddcb22 22acddca22 3445115443"),
    ("body_out", "south"): F("540bcdb045 4344665434 3344554433 4334554334 3434444343 3455665443 3445445433 22bcddcb22 22acddca22 3445115443"),
    ("body_out", "west"): F("554 544 444 443 433 433 333 bcd acb 221"),
    ("body_out", "east"): F("543 443 433 433 333 332 322 dcb bca 210"),
    # le prime 10 righe di body_in stanno dietro body_out: le ultime 3 sono le righe 13-15 dell'icona (9 colonne: via una colonna centrale)
    ("body_in", "north"): BASE[("body_in", "north")][:10] + F("334414433 223313322 000000000"),
    ("body_in", "south"): BASE[("body_in", "south")][:10] + F("334414433 223313322 000000000"),
    ("body_in", "west"): BASE[("body_in", "west")][:10] + F("22222 22221 01110"),
    ("body_in", "east"): BASE[("body_in", "east")][:10] + F("22222 22211 01110"),
    # braccia: sotto lo spallaccio solo placche di bedrock, senza fasce arancio (l'icona non ne ha)
    ("right_arm", "north"): F("55654 54543 44433 43432 43332 33322 33322 43322 33222 33222 32221 22211 21110"),
    ("left_arm", "north"): F("55654 54543 44433 43432 43332 33322 33322 43322 33222 33222 32221 22211 21110"),
    ("right_arm", "south"): F("45554 44543 34433 33432 23322 33322 33322 33222 32222 32221 22211 22111 11100"),
    ("left_arm", "south"): F("45554 44543 34433 33432 23322 33322 33322 33222 32222 32221 22211 22111 11100"),
    ("right_arm", "west"): F("55554 54443 44433 44333 43332 33322 43322 33222 32222 32221 33221 32211 21110"),
    ("right_arm", "east"): F("44443 44333 43332 33322 33222 32222 32222 33222 32221 22221 22211 22111 11110"),
    ("left_arm", "west"): F("44443 44333 43332 33322 33222 32222 32222 22221 22211 22111 21111 11111 01110"),
    ("left_arm", "east"): F("55554 54443 44433 44333 43332 33322 33322 33222 32221 22211 22111 21111 11110"),
    # spallacci: a destra (sul braccio destro del personaggio) quello dell'icona a sinistra, con la scintilla arancio;
    # a sinistra il cappuccio e quello dell'icona a destra
    ("right_arm_extra", "north"): F("06e540 055433 054333 004433"),
    ("right_arm_extra", "south"): F("045e60 334550 433332 003322"),
    ("right_arm_extra", "west"): F("455543 555432 444322 003322"),
    ("right_arm_extra", "east"): F("345554 344555 333444 223300"),
    ("right_arm_extra", "top"): F("566655 6e5544 655443 554433 544332 443322"),
    ("left_arm_extra2", "north"): F("045e60 334550"),
}

FACES = {**BASE, **CASTONE["faces"], **CHEST_FACES}

# ===========================================================================
# ANIMAZIONE: 20 fotogrammi da 5 tick (100 tick = 5 s), interpolate false
# ===========================================================================
FRAMES, FRAMETIME = 20, 15
CYAN, MAGENTA, YELLOW = ["r", "s", "t", "W"], ["n", "o", "p", "W"], ["v", "w", "x", "W"]
ORANGE = ["a", "b", "c", "d", "e", "f"]
GEM_RAMPS = (CYAN, MAGENTA, YELLOW)
# Gemme: da f5 a f16 una alla volta sale di un gradino (4 fotogrammi ciascuna) e le altre due scendono di uno,
# così il turno si legge anche a 16x16; f17-f18 tutte giù (respiro), f0-f4 e f19 a riposo.
#                  f: 0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19
GEM_LEVEL = {"c": [0, 0, 0, 0, 0, 1, 1, 1, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0],
             "m": [0, 0, 0, 0, 0, -1, -1, -1, -1, 1, 1, 1, 1, -1, -1, -1, -1, -1, -1, 0],
             "y": [0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, 1, 1, 1, 1, -1, -1, 0]}
SETTING_ON = range(5, 17)       # la montatura del castone resta più chiara mentre le gemme si accendono a turno
BREATH = (17, 18)               # respiro: tutto un gradino più scuro
assert all(len(v) == FRAMES for v in GEM_LEVEL.values())


def shift(ch, k, ramp):
    i = ramp.index(ch)
    return ramp[max(0, min(len(ramp) - 1, i + k))]


def gem_ramp(ch):
    for key, ramp in zip("cmy", GEM_RAMPS):
        if ch in ramp[:3]:          # W (riflesso) resta bianco
            return key, ramp
    return None, None


def orange_phases(grid, x0=0, y0=0, w=None, h=None):
    """Per ogni pixel arancio di una regione: (è montatura?, fotogramma in cui lo raggiunge l'onda).
    L'onda parte dai pixel più lontani dal castone (f1) e arriva ai più vicini (f4), subito prima delle gemme. Il castone sono le gemme
    che toccano l'arancio; una regione senza castone si accende tutta all'inizio dell'onda."""
    w = w or len(grid[0])
    h = h or len(grid)
    cells = [(x, y) for y in range(y0, y0 + h) for x in range(x0, x0 + w)]

    def at(x, y):
        return grid[y][x] if x0 <= x < x0 + w and y0 <= y < y0 + h else "."

    orange = [(x, y) for x, y in cells if at(x, y) in ORANGE]
    gems = [(x, y) for x, y in cells if gem_ramp(at(x, y))[0] or at(x, y) == "W"]
    seat = [(x, y) for x, y in gems
            if any(at(x + dx, y + dy) in ORANGE for dx in (-1, 0, 1) for dy in (-1, 0, 1))]
    out = {}
    if not seat:
        return {p: (False, 1) for p in orange}
    dist = {p: min(math.hypot(p[0] - g[0], p[1] - g[1]) for g in seat) for p in orange}
    dmax = max(dist.values(), default=1)
    for p, d in dist.items():
        setting = d < 1.5
        arrive = 4 if dmax <= 1.5 else 1 + round(3 * (dmax - d) / (dmax - 1.5))
        out[p] = (setting, min(4, max(1, arrive)))
    return out


def frame_grid(grid, f, phases):
    rows = [list(r) for r in grid]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            key, ramp = gem_ramp(ch)
            if key:
                row[x] = shift(ch, GEM_LEVEL[key][f], ramp)
            elif ch in ORANGE:
                setting, a = phases[(x, y)]
                if setting:      # la montatura non prende la cresta: si schiarisce di un gradino mentre le gemme girano
                    k = 1 if f in SETTING_ON else 0
                else:            # il filo: cresta di due gradini, un gradino prima e dopo
                    k = 2 if f == a else 1 if f in (a - 1, a + 1) and f >= 1 else 0
                if f in BREATH:
                    k = -1
                row[x] = shift(ch, k, ORANGE)
    return ["".join(r) for r in rows]


def icon_grids(grid):
    ph = orange_phases(grid)
    return [frame_grid(grid, f, ph) for f in range(FRAMES)]


def armor_phases(grid):
    ph = {}
    for cube, (u, v, w, h, d) in M.CUBES.items():
        for face, (fx, fy, fw, fh) in M.cube_faces(u, v, w, h, d).items():
            ph.update(orange_phases(grid, fx, fy, fw, fh))
    return ph


# ===========================================================================
# Conversione e composizione (nessun disegno qui)
# ===========================================================================
up, on_bg, SLOT = H.up, H.on_bg, H.SLOT
MAN_BG = (60, 60, 70, 255)


def render(grid):
    return H.render(grid, PAL)


def render_big(grid):
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), PAL[ch])
    return im


def armor_grid():
    return M.armor_grid(FACES)


def armor_frames(grid):
    ph = armor_phases(grid)
    return [render_big(frame_grid(grid, f, ph)) for f in range(FRAMES)]


def b64(data, fmt="png"):
    if not isinstance(data, bytes):
        buf = io.BytesIO()
        data.save(buf, format="PNG")
        data = buf.getvalue()
    return f"data:image/{fmt};base64," + base64.b64encode(data).decode()


def gif_bytes(frames, scale=1, bg=SLOT, ms=FRAMETIME * 50):
    imgs = [on_bg(up(fr, scale), bg).convert("RGB") for fr in frames]
    buf = io.BytesIO()
    imgs[0].save(buf, format="GIF", save_all=True, append_images=imgs[1:], duration=ms, loop=0, disposal=1)
    return buf.getvalue()


def save_anim(frames, base, frametime=FRAMETIME):
    strip = Image.new("RGBA", (frames[0].width, frames[0].height * len(frames)), (0, 0, 0, 0))
    for i, fr in enumerate(frames):
        strip.alpha_composite(fr, (0, fr.height * i))
    strip.save(base + ".png")
    with open(base + ".png.mcmeta", "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": %d,\n    "interpolate": false\n  }\n}\n' % frametime)


def strip_frames(path):
    """Fotogrammi di una striscia animata 16xN (per il confronto con il T7)."""
    im = Image.open(path).convert("RGBA")
    return [im.crop((0, i * im.width, im.width, (i + 1) * im.width)) for i in range(im.height // im.width)]


def mcmeta_ticks(path):
    import json
    try:
        with open(path + ".mcmeta") as fh:
            return json.load(fh)["animation"].get("frametime", 1)
    except (OSError, KeyError, ValueError):
        return 1


def ref(name):
    p = os.path.join(REF, f"bedrock_{name}.png")
    return Image.open(p).convert("RGBA") if os.path.exists(p) else None


# ---------------------------------------------------------------------------
# Verifica: pala, cesoie, canna (normale e lanciata), arco, elmo, corazza, gambali, stivali e armatura indossata coincidono pixel per pixel con prima/
# ---------------------------------------------------------------------------
def same(a, b):
    a, b = a.convert("RGBA"), b.convert("RGBA")
    if a.size != b.size:
        return False
    ba, bb = a.tobytes(), b.tobytes()
    # i pixel del tutto trasparenti contano come uguali qualunque sia il loro RGB
    return all(ba[i:i + 4] == bb[i:i + 4] or (ba[i + 3] == 0 and bb[i + 3] == 0) for i in range(0, len(ba), 4))


FILO = os.path.join(OUT, "prima_filo")      # stato prima del giro del filo (ascia, zappa, cesoie, spada, workstaff)
NOTE = os.path.join(OUT, "prima_note")     # stato prima del giro delle note (zappa, ascia, cesoie, corazza, busto indossato)
ARMOR_CHANGED = ("body_in", "body_out", "right_arm", "left_arm", "right_arm_extra", "left_arm_extra2")   # nota 4: cambia solo il busto; il resto è uguale a prima_note/
UNCHANGED = ("leggings", "boots")      # icone e animazioni identiche a prima_giro_analisi/ (l'elmo e escluso: giro occhi, apertura dell'icona allargata); cambiano solo le 3 tensioni dell'arco


def verify(icons, anims, tex, tex0):
    """Pezzi approvati: icona ferma e intera striscia animata identiche a prima/set; armatura indossata identica a
    prima/armatura in tutti i cubi."""
    rep = []
    for n in UNCHANGED:
        old = Image.open(os.path.join(PRIMA, "set", f"t6_{n}.png"))
        old_anim = strip_frames(os.path.join(PRIMA, "set", f"t6_{n}_anim.png"))
        ok = same(icons[n], old) and len(old_anim) == len(anims[n]) and all(same(a, b) for a, b in zip(anims[n], old_anim))
        rep.append((n, ok))
    # armatura indossata: identica pixel per pixel alla texture ritoccata dall'utente (armatura1) e fotogramma 0 uguale
    appr = Image.open(os.path.join(OUT, "modifiche_utente", "armatura1", "t6_armor.png")).convert("RGBA")
    ok = same(tex, appr) and same(tex0, appr)
    rep.append(("armor", ok))
    # canne ritoccate dall'utente (giro9_canne): icona ferma identica alla PNG, fotogramma 0 dell'animazione uguale all'icona
    for n in ("fishing_rod", "fishing_rod_cast"):
        u = Image.open(os.path.join(OUT, "modifiche_utente", "giro9_canne", f"t6_{n}.png")).convert("RGBA")
        rep.append((n, same(icons[n], u) and same(anims[n][0], u) and len(anims[n]) == 20))
    return rep


# ---------------------------------------------------------------------------
# Tavole PNG
# ---------------------------------------------------------------------------
FONT_B, FONT_S = ImageFont.load_default(size=22), ImageFont.load_default(size=13)


def panoramica(path, icons, tex):
    k, cell = 6, 16 * 6 + 10
    names = SET_ORDER
    per = 8
    rows = (len(names) + per - 1) // per
    man = on_bg(M.mannequin(tex, 6), MAN_BG)
    W = max(per * cell + 16, man.width + 16)
    Hh = 40 + rows * (cell + 18) + 30 + man.height + 16
    sheet = Image.new("RGBA", (W, Hh), (24, 22, 30, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), "T6 Trichromatic, Castone (icone ferme, x6)", fill=(234, 154, 39), font=FONT_B)
    y = 40
    for i, n in enumerate(names):
        x, yy = 8 + (i % per) * cell, y + (i // per) * (cell + 18)
        sheet.alpha_composite(on_bg(up(icons[n], k)), (x, yy))
        dr.text((x, yy + cell - 8), NOMI.get(n, n), fill=(165, 157, 177), font=FONT_S)
    y += rows * (cell + 18) + 4
    dr.text((10, y), "Armatura indossata, davanti e dietro", fill=(200, 196, 210), font=FONT_S)
    sheet.alpha_composite(man, (8, y + 22))
    sheet.save(path)


def confronto_t7_png(path, icons, tex, t7):
    """Ogni pezzo T6 (sinistra) accanto al T7 (destra), x5; gli oggetti speciali T6 si leggono da speciali/ (script degli speciali)."""
    k, pad = 5, 6
    cw = 16 * k + 4
    pair = 2 * cw + pad
    per = 5
    cells = [(NOMI[n], icons[n], t7["icons"].get(t7key(n))) for n in ORDER]
    cells += [(NOMI_SPECIALI.get(n, n), t7.get("t6special", {}).get(n, (None, None))[0], im) for n, im, _, _ in t7["special"]]
    rows = (len(cells) + per - 1) // per
    man6 = on_bg(M.mannequin(tex, 5), MAN_BG)
    man7 = on_bg(M.mannequin(t7["armor"], 5), MAN_BG) if t7.get("armor") is not None else None
    W = per * (pair + 18) + 16
    Hh = 44 + rows * (cw + 26) + 30 + man6.height + 16
    sheet = Image.new("RGBA", (W, Hh), (24, 22, 30, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), "T6 Castone (sinistra) e T7 Umbral (destra)", fill=(234, 154, 39), font=FONT_B)
    y = 44
    for i, (name, a, b) in enumerate(cells):
        x, yy = 8 + (i % per) * (pair + 18), y + (i // per) * (cw + 26)
        if a is not None:
            sheet.alpha_composite(on_bg(up(a, k)), (x, yy))
        else:
            dr.rectangle((x, yy, x + 16 * k - 1, yy + 16 * k - 1), outline=(70, 64, 80, 255))
            dr.text((x + 14, yy + 34), "T6 da fare", fill=(165, 157, 177), font=FONT_S)
        if b is not None:
            sheet.alpha_composite(on_bg(up(b, k)), (x + cw + pad, yy))
        dr.text((x, yy + cw + 2), name, fill=(165, 157, 177), font=FONT_S)
    y += rows * (cw + 26)
    dr.text((10, y), "Armatura indossata: T6 (sinistra) e T7 (destra), davanti e dietro", fill=(200, 196, 210), font=FONT_S)
    sheet.alpha_composite(man6, (8, y + 22))
    if man7:
        sheet.alpha_composite(man7, (8 + man6.width + 20, y + 22))
    sheet.save(path)


def anteprima(path, icons):
    """Per la revisione: ogni pezzo rifatto x12 sul fondo dello slot."""
    names = ["sword", "axe", "shovel", "hoe", "helmet", "leggings", "boots", "bow", "bow_pulling_0", "bow_pulling_1",
             "bow_pulling_2"]
    k = 12
    cell = 16 * k + 8
    sheet = Image.new("RGBA", (6 * cell + 8, 2 * (cell + 20) + 8), (24, 22, 30, 255))
    dr = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        x, y = 8 + (i % 6) * cell, 8 + (i // 6) * (cell + 20)
        sheet.alpha_composite(on_bg(up(icons[n], k)), (x, y))
        dr.text((x, y + cell - 6), NOMI.get(n, n), fill=(165, 157, 177), font=FONT_S)
    sheet.save(path)


# ---------------------------------------------------------------------------
# Dati del T7 per il confronto
# ---------------------------------------------------------------------------
T7_SPECIALI = ["workstaff", "ultra_greatsword", "bident", "crossbow_standby", "crossbow_pulling_0",
               "crossbow_pulling_1", "crossbow_pulling_2", "crossbow_arrow", "crossbow_firework", "crossbow_topaz",
               "crossbow_amethyst", "crossbow_citrine", "crossbow_onyx", "crossbow_moonstone", "crossbow_glass"]
NOMI_SPECIALI = {"workstaff": "workstaff", "ultra_greatsword": "spadone", "bident": "bidente",
                 "crossbow_standby": "balestra", "crossbow_pulling_0": "balestra, carica 0",
                 "crossbow_pulling_1": "balestra, carica 1", "crossbow_pulling_2": "balestra, carica 2",
                 "crossbow_arrow": "balestra con freccia", "crossbow_firework": "balestra con razzo",
                 "crossbow_topaz": "balestra, topazio", "crossbow_amethyst": "balestra, ametista",
                 "crossbow_citrine": "balestra, citrino", "crossbow_onyx": "balestra, onice",
                 "crossbow_moonstone": "balestra, pietra di luna", "crossbow_glass": "balestra, vetro"}


T7_KEYS = ["sword", "axe", "shovel", "hoe", "shears", "bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2",
           "fishing_rod", "fishing_rod_cast", "helmet", "chestplate", "leggings", "boots"]


def load_t6_speciali():
    """Gli speciali T6 veri (16x16 fermo e animato), scritti da texture_t6castone_speciali.py in speciali/."""
    out = {}
    for n in T7_SPECIALI:
        p = os.path.join(OUT, "speciali", f"t6_{n}.png")
        if os.path.exists(p):
            a = os.path.join(OUT, "speciali", f"t6_{n}_anim.png")
            out[n] = (Image.open(p).convert("RGBA"), strip_frames(a) if os.path.exists(a) else None)
    return out


def load_t7():
    d = {"icons": {}, "anims": {}, "ms": {}, "special": [], "armor": None, "armor_frames": []}
    for n in T7_KEYS:
        p = os.path.join(T7, "set", f"t7_{n}.png")
        if os.path.exists(p):
            d["icons"][n] = Image.open(p).convert("RGBA")
            a = os.path.join(T7, "set", f"t7_{n}_anim.png")
            if os.path.exists(a):
                d["anims"][n] = strip_frames(a)
                d["ms"][n] = mcmeta_ticks(a) * 50
    for n in T7_SPECIALI:
        p = os.path.join(T7, "speciali", f"t7_{n}.png")
        if os.path.exists(p):
            a = os.path.join(T7, "speciali", f"t7_{n}_anim.png")
            fr = strip_frames(a) if os.path.exists(a) else [Image.open(p).convert("RGBA")]
            d["special"].append((n, Image.open(p).convert("RGBA"), fr, mcmeta_ticks(a) * 50))
    d["t6special"] = load_t6_speciali()
    p = os.path.join(T7, "armatura", "t7_armor.png")
    if os.path.exists(p):
        d["armor"] = Image.open(p).convert("RGBA")
        i = 0
        while os.path.exists(os.path.join(T7, "armatura", f"t7_armor_{i}.png")):
            d["armor_frames"].append(Image.open(os.path.join(T7, "armatura", f"t7_armor_{i}.png")).convert("RGBA"))
            i += 1
    return d


# ---------------------------------------------------------------------------
# Pagina
# ---------------------------------------------------------------------------
CSS = M.CSS.replace("--accent: #e89725;", "--accent: #ea9a27;") + """
.swatches { display: flex; flex-wrap: wrap; gap: 10px; }
.sw { display: grid; gap: 4px; width: 92px; }
.sw i { display: block; height: 32px; border: 1px solid var(--line); }
.sw b { font-size: 12px; font-weight: 600; }
.sw code { font-size: 12px; color: var(--muted); }
.duo { display: flex; gap: 4px; }
.duo .slot { width: 72px; height: 72px; }
.duo .slot img { width: 64px; height: 64px; }
.duo .todo { width: 72px; height: 72px; display: grid; place-items: center; text-align: center; font-size: 11px;
  color: var(--muted); border: 1px dashed var(--line); padding: 4px; line-height: 1.2; }
.duo-lbl { display: flex; gap: 4px; font-size: 10px; letter-spacing: .08em; text-transform: uppercase; color: var(--muted); }
.duo-lbl span { width: 72px; text-align: center; }
.items.pairs6 { grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); }
.items.trio { grid-template-columns: repeat(3, minmax(0, 160px)); }
.items.quad { grid-template-columns: repeat(4, minmax(0, 160px)); }
.worn .man.small img { width: 252px; }
.check { margin: 0; padding-left: 18px; color: var(--muted); }
.check b { color: var(--fg); }
.timeline { display: grid; grid-template-columns: repeat(20, minmax(0, 1fr)); gap: 2px; max-width: 760px; }
.timeline div { font-size: 10px; text-align: center; color: var(--muted); }
.timeline i { display: block; height: 12px; margin-top: 2px; }
"""


def html(path, icons, anims, tex, aframes, t7, checks, pd):
    def img(im, alt):
        return f'<img src="{b64(im)}" alt="{alt}">'

    def gif(frames, alt, ms=FRAMETIME * 50, scale=1, bg=SLOT):
        return f'<img src="{b64(gif_bytes(frames, scale, bg, ms), "gif")}" alt="{alt}">'

    def fig(im, cap):
        return f'<figure><div class="slot">{img(im, cap)}</div><figcaption>{cap}</figcaption></figure>'

    def fig_gif(frames, cap):
        return f'<figure><div class="slot">{gif(frames, cap)}</div><figcaption>{cap}</figcaption></figure>'

    worn_gif = gif([M.mannequin(fr, 1) for fr in aframes], "manichino T6 animato", scale=8, bg=MAN_BG)
    man = img(on_bg(M.mannequin(tex, 8), MAN_BG), "manichino T6")

    # confronto con la bedrock di Spectrum
    refpairs = []
    for n in ORDER:
        r = ref(t7key(n))
        if r is None:
            continue
        refpairs.append(f'<figure><div class="duo"><div class="slot">{img(r, "bedrock di Spectrum")}</div>'
                        f'<div class="slot">{img(icons[n], "T6")}</div></div><figcaption>{NOMI[n]}</figcaption></figure>')
    ref_man = Image.open(os.path.join(REF, "bedrock_armor_indossata.png")).convert("RGBA")

    # confronto T6 / T7
    t7pairs = []
    for n in ORDER:
        k7 = t7key(n)
        if k7 in t7["anims"]:
            right = f'<div class="slot">{gif(t7["anims"][k7], "T7 " + NOMI[n], t7["ms"][k7])}</div>'
        elif k7 in t7["icons"]:
            right = f'<div class="slot">{img(t7["icons"][k7], "T7 " + NOMI[n])}</div>'
        else:
            right = '<div class="todo">T7 assente</div>'
        t7pairs.append(f'<figure><div class="duo-lbl"><span>T6</span><span>T7</span></div><div class="duo">'
                       f'<div class="slot">{gif(anims[n], "T6 " + NOMI[n])}</div>{right}</div>'
                       f'<figcaption>{NOMI[n]}</figcaption></figure>')
    for n, im, fr, ms in t7["special"]:
        t6im, t6fr = t7.get("t6special", {}).get(n, (None, None))
        left = (f'<div class="slot">{gif(t6fr, "T6 " + n)}</div>' if t6fr else
                f'<div class="slot">{img(t6im, "T6 " + n)}</div>' if t6im is not None else '<div class="todo">T6 assente</div>')
        t7pairs.append(f'<figure><div class="duo-lbl"><span>T6</span><span>T7</span></div><div class="duo">'
                       f'{left}<div class="slot">{gif(fr, "T7 " + n, ms)}</div></div>'
                       f'<figcaption>{NOMI_SPECIALI.get(n, n)}</figcaption></figure>')
    t7_worn = ""
    if t7["armor"] is not None:
        fr7 = t7["armor_frames"] or [t7["armor"]]
        t7_worn = (f'<div class="block"><h3>T7, animato</h3><div class="slot man small">'
                   f'{gif([M.mannequin(f, 1) for f in fr7], "manichino T7", scale=6, bg=MAN_BG)}</div></div>')

    # arco: i 4 stati affiancati al T7 (animati)
    def row(label, figs):
        return f'<div class="block"><h3>{label}</h3><div class="items quad">{figs}</div></div>'

    t7bow = "".join(
        f'<figure><div class="slot">{gif(t7["anims"][k], "T7 " + k, t7["ms"][k]) if k in t7["anims"] else img(t7["icons"][k], "T7 " + k)}'
        f'</div><figcaption>{c}</figcaption></figure>'
        for k, c in (("bow", "a riposo"), ("bow_pulling_0", "tensione 0"), ("bow_pulling_1", "tensione 1"),
                     ("bow_pulling_2", "tensione 2")) if k in t7["icons"])
    bow_rows = row("T7 Umbral, per confronto", t7bow)
    bow_rows += row("T6 Castone: gemme sulle punte dei flettenti", "".join(
        fig_gif(anims[n], c) for n, c in zip(BOW_NAMES, ("a riposo", "tensione 0", "tensione 1", "tensione 2"))))
    bow_real = "".join(f'<div class="slot s2">{img(icons[n], NOMI[n])}</div>' for n in BOW_NAMES)

    # correzioni: prima/dopo
    corr = [("helmet", "Elmo, apertura", "L'apertura trasparente sulla fronte è più grande di circa 1 px per lato e in alto: da riga 5 (prima da riga 6), larga 6 colonne nel mezzo (prima 4) e 8 sul mento (prima 6), stessa forma. Vedi analisi_prima_dopo.png."),
            ("helmet_in", "Elmo, facce interne", "Ogni faccia interna 9×9 ripete il disegno della corrispondente esterna 10×10 (apertura, castone, fili): attraverso l'apertura non si vede più una faccia diversa."),
            ("helmet_back", "Elmo, retro", "Aggiunta solo ombreggiatura a grappoli di bedrock più chiari e più scuri, senza nuovo arancio né gemme."),
            ("bow", "Arco, frecce di tensione", "Asta in bedrock chiaro (#8a83ad, la lettera 6 della palette) al posto del bianco; alette arancio sugli ultimi due pixel; punta a prisma invariata. Lunghezze 9, 10, 11 invariate."),
            ("special", "Oggetti speciali", "Workstaff e bident oversized rifatti con il metodo del glass crest, come le 16×16; balestra con il filo arancio solo sul bordo alto e razzo con il cono attaccato al corpo.")]
    corr_html = "".join(f'<li><b>{t}</b>: {d}</li>' for _, t, d in corr)

    # linea del tempo dell'animazione
    cols = []
    for f in range(FRAMES):
        lv = {k: GEM_LEVEL[k][f] for k in "cmy"}
        top = max(lv, key=lambda k: lv[k])
        if f in BREATH:
            col, lab = "#3a1406", "respiro"
        elif f == 0 or f == 19:
            col, lab = "#2d2843", "riposo"
        elif lv[top] > 0:
            col = {"c": "#2fc0e0", "m": "#d443a8", "y": "#eedc30"}[top]
            lab = {"c": "ciano", "m": "magenta", "y": "giallo"}[top]
        else:
            col, lab = "#ffc75e", "onda"
        cols.append(f'<div title="f{f}: {lab}">{f}<i style="background:{col}"></i></div>')

    sw = []
    pal_groups = (("Bedrock", "0123456"), ("Allthemodium", "abcdef"), ("Stoffa", "ghijk"),
                  ("Topazio", "qrst"), ("Ametista", "mnop"), ("Citrino", "uvwx"))
    for title, keys in pal_groups:
        sw.append(f'<div class="block"><h3>{title}</h3><div class="swatches">'
                  + "".join(f'<div class="sw"><i style="background:#{"%02x%02x%02x" % PAL[k][:3]}"></i><b>{k}</b>'
                            f'<code>#{"%02x%02x%02x" % PAL[k][:3]}</code></div>' for k in keys)
                  + "</div></div>")

    checks_html = "".join(f'<li><b>{NOMI.get(n, n) if n != "armor" else "armatura 128×128"}</b>: '
                          f'{"identico" if ok else "DIVERSO"}</li>' for n, ok in checks)
    nuovi = ["shovel", "hoe", "shears", "fishing_rod", "fishing_rod_cast"]

    page = f"""<title>Castone, set T6</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&family=Pixelify+Sans:wght@500;600&display=swap">
<style>{CSS}</style>
<header class="intro"><div class="wrap">
  <h1>Castone, set T6 <span class="tri"><i style="background:var(--c)"></i><i style="background:var(--m)"></i><i style="background:var(--y)"></i></span></h1>
  <p class="lead">Il set completo della proposta approvata: base di <b>bedrock</b> grigio-viola, un filo d'<b>allthemodium</b> su un solo
  spigolo e un <b>castone a prisma</b> fuori asse con topazio, ametista e citrino. Ultimo giro: elmo (apertura più grande, interno uguale all'esterno, retro ombreggiato) e frecce dell'arco (asta di bedrock, alette arancio); gli altri pezzi sono invariati. Animazione di 5 secondi. Solo design: niente è installato nella mod.</p>
</div></header>
<main class="wrap">
<section class="style" id="correzioni">
  <div class="head"><div class="num">&#9998;</div><h2>Correzioni</h2>
  <p>Per ogni pezzo cambiato, il prima (sinistra: il giro appena visto) e il dopo (destra) ingranditi ×12, poi i manichini di prima e di dopo.
  In questo giro cambiano l'elmo indossato (apertura, interno, retro) e le tre frecce dell'arco; tutti gli altri pezzi sono identici al «prima». Gli speciali hanno la loro sezione.</p></div>
  <div class="block"><div class="slot sheet"><img src="{b64(pd)}" alt="prima e dopo"></div></div>
  <ul class="check">{corr_html}</ul>
</section>
<section class="style best" id="set">
  <div class="head"><div class="num">T6</div><h2>Il set</h2>
  <p>Tutti i pezzi in slot d'inventario, ingranditi, poi a dimensione reale 1× e 2×.</p></div>
  <div class="items wide">{"".join(fig(icons[n], NOMI[n]) for n in SET_ORDER)}</div>
  <div class="lower">
    <div class="block"><h3>Dimensione reale 1×</h3><div class="real">{"".join(f'<div class="slot s1">{img(icons[n], NOMI[n])}</div>' for n in SET_ORDER)}</div></div>
    <div class="block"><h3>2×</h3><div class="real">{"".join(f'<div class="slot s2">{img(icons[n], NOMI[n])}</div>' for n in SET_ORDER)}</div></div>
  </div>
  <div class="worn">
    <div class="block"><h3>Manichino, fermo (davanti e dietro)</h3><div class="slot man">{man}</div></div>
    <div class="block gif"><h3>Manichino, animato</h3><div class="slot man">{worn_gif}</div></div>
    <div class="block"><h3>Texture 128×128, 3×</h3><div class="slot sheet">{img(tex, "texture armatura T6")}</div></div>
  </div>
</section>
<section class="style" id="nuovi">
  <div class="head"><div class="num">+</div><h2>I pezzi nuovi</h2>
  <p>Stesse regole dei pezzi approvati: il filo arancio corre su un solo spigolo (la lama della pala, il bordo di taglio
  della zappa, la lama alta delle cesoie, il tratto basso della canna) e il castone sta sempre di lato: al collare della pala,
  all'attacco del manico della zappa, sul perno delle cesoie, sotto l'asta della canna come mulinello. L'arco ha la sua
  sezione. Canna: a riposo la lenza pende col galleggiante, lanciata la punta si piega e la lenza sale.</p></div>
  <div class="items wide">{"".join(fig(icons[n], NOMI[n]) for n in nuovi)}</div>
</section>
<section class="style" id="arco">
  <div class="head"><div class="num">&#127993;</div><h2>Arco</h2>
  <p>Variante B scelta: gemme sulle punte dei flettenti (topazio in alto, citrino in basso, ametista sul bordo interno). Quattro stati
  (a riposo e tensione 0, 1, 2) affiancati all'arco T7. La freccia, con l'asta di bedrock chiaro, le alette arancio e la punta a prisma, attraversa l'arco dall'angolo
  in alto a sinistra fino alla corda. Nel set sono <code>t6_bow</code> e <code>t6_bow_pulling_0/1/2</code>.</p></div>
  {bow_rows}
  <div class="block"><h3>2×</h3><div class="real">{bow_real}</div></div>
</section>
<section class="style" id="animazioni">
  <div class="head"><div class="num">~</div><h2>Animazioni</h2>
  <p>20 fotogrammi da 5 tick (5 secondi), interpolate false. Un'onda di luce corre lungo il filo arancio verso il castone, poi le
  gemme si accendono a turno, ciano, magenta e giallo, mentre la montatura resta più chiara; alla fine arancio e gemme
  respirano un gradino più scuri e tornano al riposo. La bedrock non si muove. Il fotogramma 0 è l'icona ferma.</p></div>
  <div class="timeline">{"".join(cols)}</div>
  <div class="items wide gif">{"".join(fig_gif(anims[n], NOMI[n]) for n in SET_ORDER)}</div>
</section>
<section class="style" id="t7">
  <div class="head"><div class="num">T7</div><h2>T6 Castone e T7 Umbral</h2>
  <p>Ogni pezzo T6 accanto al T7 corrispondente, animati; anche gli oggetti speciali (workstaff, spadone, bidente, balestra in vari stati) sono quelli veri del T6.</p></div>
  <div class="items pairs6">{"".join(t7pairs)}</div>
  <div class="worn">
    <div class="block"><h3>T6, animato</h3><div class="slot man small">{gif([M.mannequin(fr, 1) for fr in aframes], "manichino T6", scale=6, bg=MAN_BG)}</div></div>
    {t7_worn}
  </div>
</section>
<section class="style" id="bedrock">
  <div class="head"><div class="num">B</div><h2>Confronto con la bedrock di Spectrum</h2>
  <p>A sinistra il pezzo bedrock di Spectrum (riferimento), a destra il T6 Castone.</p></div>
  <div class="items pairs6">{"".join(refpairs)}</div>
  <div class="worn">
    <div class="block"><h3>Bedrock di Spectrum</h3><div class="slot man small">{img(on_bg(M.mannequin(ref_man, 6), MAN_BG), "manichino bedrock")}</div></div>
    <div class="block"><h3>T6 Castone</h3><div class="slot man small">{img(on_bg(M.mannequin(tex, 6), MAN_BG), "manichino T6")}</div></div>
  </div>
</section>
<section class="style" id="palette">
  <div class="head"><div class="num">#</div><h2>Palette e verifica</h2>
  <p>La palette è quella approvata. Verifica automatica dei pezzi approvati contro prima/ (icona ferma, animazione intera, armatura indossata tranne la faccia dell'elmo):</p></div>
  <ul class="check">{checks_html}</ul>
  <div class="lower">{"".join(sw)}</div>
</section>
</main>
<footer><div class="wrap">Sprite originali per Spectral Ascension, disegnati a mano a griglia.</div></footer>
"""
    with open(path, "w") as fh:
        fh.write(page)


# ---------------------------------------------------------------------------
PRIMA = os.path.join(OUT, "prima_giro_analisi")   # copia dello stato prima di questo giro
GIRO2 = os.path.join(OUT, "prima_giro2")


def helmet_rows(im):
    a = im.convert("RGBA").getchannel("A").point(lambda v: 255 if v else 0)
    box = a.getbbox()
    return box[3] - box[1]


def prima_dopo(path, icons, tex):
    """Per ogni pezzo cambiato: il prima (copia in prima/set) e il dopo, x12; poi i manichini di prima e di dopo."""
    k, cell = 12, 16 * 12 + 8

    def old(n):
        return Image.open(os.path.join(PRIMA, "set", f"t6_{n}.png")).convert("RGBA")

    # (nome del prima, nome del dopo, etichetta): l'arco prima era la variante B (t6_bow_b...)
    pezzi = [("bow_pulling_0", "bow_pulling_0", "Arco, tensione 0"), ("bow_pulling_1", "bow_pulling_1", "Arco, tensione 1"),
             ("bow_pulling_2", "bow_pulling_2", "Arco, tensione 2")]
    man_old = on_bg(M.mannequin(Image.open(os.path.join(PRIMA, "armatura", "t6_armor.png")).convert("RGBA"), 8), MAN_BG)
    man_new = on_bg(M.mannequin(tex, 8), MAN_BG)
    pair_w = 2 * cell + 30
    per = 3
    rows = (len(pezzi) + per - 1) // per
    W = max(per * pair_w + 16, 2 * man_old.width + 60)
    Hh = 44 + rows * (cell + 40) + (cell + 50) + man_old.height + 50
    sheet = Image.new("RGBA", (W, Hh), (24, 22, 30, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), "Correzioni T6 Castone: prima (sinistra) e dopo (destra), x12. Elmo: la sola icona non cambia, cambia l'armatura indossata", fill=(234, 154, 39), font=FONT_B)
    y = 44
    for i, (no, nn, nome) in enumerate(pezzi):
        x, yy = 8 + (i % per) * pair_w, y + (i // per) * (cell + 40)
        sheet.alpha_composite(on_bg(up(old(no), k)), (x, yy))
        sheet.alpha_composite(on_bg(up(icons[nn], k)), (x + cell, yy))
        dr.text((x, yy + cell - 4), f"{nome}: prima", fill=(165, 157, 177), font=FONT_S)
        dr.text((x + cell, yy + cell - 4), f"{nome}: dopo", fill=(234, 200, 140), font=FONT_S)
    y += rows * (cell + 40)
    # elmo: bedrock, T7, T6 prima, T6 dopo, con l'altezza in righe
    t7h = Image.open(os.path.join(T7, "set", "t7_helmet.png")).convert("RGBA")
    bh = ref("helmet")
    for i, (im, lab) in enumerate(((bh, "Elmo bedrock"), (t7h, "Elmo T7"), (old("helmet"), "Elmo T6 prima"),
                                   (icons["helmet"], "Elmo T6 dopo"))):
        x = 8 + i * (cell + 30)
        sheet.alpha_composite(on_bg(up(im, k)), (x, y))
        dr.text((x, y + cell - 4), f"{lab}: {helmet_rows(im)} righe", fill=(234, 200, 140), font=FONT_S)
    y += cell + 50
    sheet.alpha_composite(man_old, (8, y))
    sheet.alpha_composite(man_new, (8 + man_old.width + 30, y))
    dr.text((8, y + man_old.height + 4), "Armatura indossata: prima", fill=(165, 157, 177), font=FONT_S)
    dr.text((8 + man_old.width + 30, y + man_old.height + 4), "Armatura indossata: dopo", fill=(234, 200, 140), font=FONT_S)
    sheet.save(path)
    return sheet


def main():
    sdir, adir = os.path.join(OUT, "set"), os.path.join(OUT, "armatura")
    for d in (sdir, adir):
        os.makedirs(d, exist_ok=True)
    icons = {n: render(g) for n, g in ITEMS.items()}
    anims = {n: [render(fg) for fg in icon_grids(g)] for n, g in ITEMS.items()}
    # via i file di pezzi eliminati (axe_v2, il vecchio arco, ...)
    keep = set()
    for n in SET_ORDER:
        keep |= {f"t6_{n}.png", f"t6_{n}_anim.png", f"t6_{n}_anim.png.mcmeta"}
    for f in os.listdir(sdir):
        if f not in keep:
            os.remove(os.path.join(sdir, f))
    for n in SET_ORDER:
        icons[n].save(os.path.join(sdir, f"t6_{n}.png"))
        save_anim(anims[n], os.path.join(sdir, f"t6_{n}_anim"))
    grid = armor_grid()
    tex = render_big(grid)
    tex.save(os.path.join(adir, "t6_armor.png"))
    aframes = armor_frames(grid)
    for i, fr in enumerate(aframes):
        fr.save(os.path.join(adir, f"t6_armor_{i}.png"))
    on_bg(M.mannequin(tex, 8), MAN_BG).save(os.path.join(adir, "manichino.png"))
    with open(os.path.join(adir, "manichino_animato.gif"), "wb") as fh:
        fh.write(gif_bytes([M.mannequin(fr, 1) for fr in aframes], 8, MAN_BG))
    checks = verify(icons, anims, tex, aframes[0])
    for n, ok in checks:
        print(f"verifica {n}: {'identico' if ok else 'DIVERSO'}")
    panoramica(os.path.join(OUT, "panoramica.png"), icons, tex)
    anteprima(os.path.join(OUT, "anteprima_nuovi.png"), icons)
    pd = prima_dopo(os.path.join(OUT, "prima_dopo.png"), icons, tex)
    t7 = load_t7()
    confronto_t7_png(os.path.join(OUT, "confronto_t6_t7.png"), icons, tex, t7)
    html(os.path.join(OUT, "stili.html"), icons, anims, tex, aframes, t7, checks, pd)
    if not all(ok for _, ok in checks):
        sys.exit("ATTENZIONE: un pezzo approvato o l'armatura indossata non coincide con prima/")


if __name__ == "__main__":
    main()
