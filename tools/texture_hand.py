"""T6 Trichromatic: texture disegnate A MANO, pixel per pixel.

Ogni sprite è una griglia ASCII 16x16 scritta carattere per carattere; ogni stile ha una
palette (carattere -> colore). Il codice qui sotto NON genera forme, contorni, ombre o luci:
converte soltanto le griglie in PNG e compone panoramica, confronti e pagina HTML.

L'animazione dello stile scelto usa le stesse griglie: i pixel marcati come «energia» in una
griglia di fase (scritta a mano) prendono, fotogramma per fotogramma, il colore di una
sequenza di 12 tinte scritta a mano.

Uso:
    python3 tools/texture_hand.py            # PNG + panoramica + animazione + pagina
    python3 tools/texture_hand.py confronto DIR   # anche i confronti x12 con la bedrock in DIR
"""
import base64
import io
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REF = os.path.join(ROOT, "design", "riferimenti", "classica")
OUT = os.path.join(ROOT, "design", "texture", "proposte", "t6-mano")
ITEMS = ["sword", "axe", "helmet", "chestplate", "leggings", "boots"]
NOMI = {"sword": "spada", "axe": "ascia", "helmet": "elmo", "chestplate": "corazza",
        "leggings": "gambali", "boots": "stivali"}


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# ---------------------------------------------------------------------------
# Palette di base (condivisa). Ogni stile ne sovrascrive o aggiunge una parte.
#   0-6  metallo scuro (0 = contorno)          a-f  allthemodium, la LUCE (a = contorno caldo)
#   q-t  ciano / topazio (q = contorno)        m-p  magenta / ametista (m = contorno)
#   u-x  giallo / citrino (u = contorno)       W    riflesso bianco
# ---------------------------------------------------------------------------
BASE = {
    "0": "#0c0a14", "1": "#1f1a2b", "2": "#2f2840", "3": "#413956", "4": "#564d70",
    "5": "#71698f", "6": "#9a92ba",
    "a": "#2c1105", "b": "#6a2c0b", "c": "#a9520f", "d": "#e89725", "e": "#ffc04e", "f": "#fff1b4",
    "q": "#071d2b", "r": "#0f5f7e", "s": "#20a9d2", "t": "#94f0ff",
    "m": "#290724", "n": "#6c1460", "o": "#c1329b", "p": "#ff8de4",
    "u": "#2b2304", "v": "#8a760c", "w": "#e6d62c", "x": "#fffba8",
    "W": "#ffffff",
}

STYLES = []


def parse_art(text):
    """Le griglie di uno stile, scritte a mano: «== oggetto» e sotto 16 righe da 16 caratteri."""
    grids, cur = {}, None
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("=="):
            cur = line[2:].split()[0]
            grids[cur] = []
        elif line and cur:
            grids[cur].append(line)
    return grids


# ===========================================================================
# 1 - FORGIATO ORNATO
# ===========================================================================
STYLES.append({
    "num": 1, "slug": "forgiato", "nome": "Forgiato Ornato",
    "desc": "Metallo bedrock grigio-viola; l'allthemodium è oro incandescente colato a grumi e incastona le gemme CMY, ognuna in un punto diverso.",
    "asim": "Guardia a mezzaluna sbilanciata, spallaccio d'oro solo a sinistra, ala solo a destra dell'elmo, gemma su un ginocchio e placca sull'altra coscia, uno stivale con gemma e l'altro col puntale acceso.",
    "cmy": 'tre gemme incastonate', "luce": "grumi d'oro e filo della lama",
    "pal": {},
    "art": """
        == sword
        ..............00
        .............06a
        ............05ea
        ...........052da
        ..........043230
        .........053230.
        ....a...0431ca..
        ...aea.0642ea...
        ...adbaa32ea....
        ....adedcdcaa...
        ....aqtefedcba..
        ...0qsrcdeccea..
        ..030qqabbaaba..
        .030..a....a.a..
        uwxu............
        .vu.............
        == axe
        ............00..
        ..aaa000000.040.
        .aeff565544040..
        .ade55444po40...
        .05544333on0....
        .ac433210d0.....
        .ae321004a......
        .ad210.30.......
        ..ab0.40........
        ...a.30.........
        ....40..........
        ...da...........
        ..30............
        .ae0............
        .ca.............
        ................
        == helmet
        ................
        ..............a.
        ....00000000.aea
        ...0566554430fda
        ..056555443320da
        ..055544433220ea
        ..aefedca22210ca
        .atsa0e0000d00ba
        .asra544332210a.
        ..qq0443320110..
        ..033322201010..
        ..022211111110..
        ...0000000000...
        ................
        ................
        ................
        == chestplate
        ................
        .aaaa.....00000.
        aeffda0..0456540
        adeedca000455430
        acddcbed55304320
        abcba565d4430c10
        .04056543eda0e10
        .03055433axwa20.
        .02044332awva10.
        ..00443322aa00..
        ...0433222110...
        ...0apoa01110...
        ...0554304320...
        ...0443203210...
        ...033210000....
        ...00000........
        == leggings
        ................
        ................
        ..000000aea000..
        ..05654adfca20..
        ..045433acb210..
        ..054400003210..
        ..05430..aedba..
        ..04430..adca0..
        ..aetsa..04310..
        ..adsra..04320..
        ..0bab0..05320..
        ..05420..04210..
        ..04310..03210..
        ..03210..0000...
        ..00000.........
        ................
        == boots
        ................
        ................
        .aaaaa..........
        .aefdca..00000..
        .adpoba..05540..
        .abona0..05430..
        ..05430..04420..
        ..05420..0d320..
        ..04320..04320..
        .054320..043210.
        0554430..043ceda
        0443320..032bdca
        0000000..000aaa.
        ................
        ................
        ................
""",
})

# ===========================================================================
# 2 - DRAPPO REGALE
# metallo blu-notte; velluto magenta (g-k) con fodera ciano e frange gialle
# ===========================================================================
STYLES.append({
    "num": 2, "slug": "drappo", "nome": "Drappo Regale",
    "desc": "Metallo blu-notte con rifiniture d'oro; un drappo di velluto magenta foderato di ciano, con frange gialle, cade sempre da un solo lato.",
    "asim": "Nastro dalla guardia, nappa dall'ascia, velo dalla corona a sinistra, mantello sulla spalla destra, tabarro su una gamba sola, risvolto di velluto su un solo stivale.",
    "cmy": 'velluto, fodera e frange', "luce": 'corona, fermagli, occhi del visore',
    "pal": {
        "0": "#090a16", "1": "#171a2e", "2": "#252a45", "3": "#343a5c", "4": "#495076",
        "5": "#656d98", "6": "#9098c4",
        "g": "#22051d", "h": "#561044", "i": "#8f1f6f", "j": "#c63f98", "k": "#ee80c8",
    },
    "art": """
        == sword
        ..............00
        .............0fa
        ............05ea
        ...........06420
        ..........05430.
        ...aa....05320..
        ..aeda..04320...
        ...abda04420....
        ....aedf3da.....
        .....adecea.....
        ....03heddca....
        ...030gjicba....
        ..030.gkihg.....
        .q30...gjih.....
        qtsq...gihg.....
        .rq.....wvu.....
        == axe
        ..aa0...........
        .aed0........00.
        .ae40.......040.
        ad5430.....040..
        ae55443322ax0...
        ac44332211dwv0..
        ad332210040h....
        .ae210..40.h....
        .ad0...40.gjg...
        ..aa..30.gkihg..
        .....40..gjihg..
        ....da...wvwvw..
        ...40.....w.v...
        ..30............
        .ae0............
        .ca.............
        == helmet
        ...........a....
        ...a.......fa...
        ..aea..a..aea...
        ..adeaaeaadeda..
        .gaeddcdxwdcba..
        gjh56554v43320..
        gkig5544433210..
        gjig0ed000c000..
        gisg4433033210..
        .gjrg33202210...
        .gihg0220110....
        .ghgw.01000.....
        ..wv...0........
        ................
        ................
        ................
        == chestplate
        ................
        .00000....ggg...
        0566540..ahjkg..
        0dedca000exhjig.
        0tsa45544dwgikhg
        .rq0554433agjihg
        .0404544320gijhg
        .0304443320ghjig
        .0203433220ghkig
        ..003433210gijhg
        ...04332210gijhg
        ...0adea000ghjhg
        ...05443040gikig
        ...04432030gsjhg
        ...03321000gtsig
        ...00000...wqwgw
        == leggings
        ................
        ................
        ..0000aea00000..
        ..0554dfd54320..
        ..ghhhgca33210..
        ..gjkig0003210..
        ..gjjihg.04310..
        ..gijihg.05320..
        ..gijhhg.adeca..
        .gsijhg..abdba..
        .grhijg..04320..
        .gqhihg..05310..
        ..wgwgw..04210..
        ..04320..03210..
        ..00000..00000..
        ................
        == boots
        ................
        ................
        .gggg...........
        gjkihg...00000..
        gsrrqg...05540..
        .0ae40...04430..
        .04420...adeca..
        .05320...04320..
        .04320...04310..
        054320...043210.
        0554320..0443320
        0443210..03322da
        0000000..00000aa
        ................
        ................
        ................
""",
})

# ===========================================================================
# 3 - CRISTALLO SCHEGGIATO
# metallo viola-nero; cristalli CMY con faccia chiara e faccia scura
# ===========================================================================
STYLES.append({
    "num": 3, "slug": "cristallo", "nome": "Cristallo Scheggiato",
    "desc": "Metallo viola-nero da cui spuntano cristalli CMY sfaccettati; dove il cristallo spacca il metallo, la frattura arde d'arancio.",
    "asim": "Punta della lama d'ametista e grappolo di topazio solo su un braccio della guardia, lama dell'ascia di topazio, cristalli in alto a destra dell'elmo con grata solo a sinistra, grappolo su una spalla, cristallo su un ginocchio, uno stivale col cristallo sul tallone e l'altro sulla punta.",
    "cmy": 'cristalli che spuntano', "luce": 'fratture alla radice dei cristalli',
    "pal": {
        "0": "#08080e", "1": "#16151f", "2": "#23212f", "3": "#322f41", "4": "#454158",
        "5": "#5e5974", "6": "#88829f",
    },
    "art": """
        == sword
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
        ...04000000.....
        ..030...........
        .uw0............
        uxu.............
        == axe
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
        ......30........
        .....40.........
        ....30..........
        ...40...........
        .uw0............
        uxu.............
        == helmet
        ..........m.....
        .........mpm..q.
        ....0000mponmqtq
        ...05665mponmqsq
        ..0565544eddcerq
        ..05554433cb20..
        ..045443322b10..
        ..000000000000..
        ..040404032210..
        .ud40403032210..
        uxw03322221110..
        uwvu0221111110..
        .uu.000000000...
        ................
        ................
        ................
        == chestplate
        ..q..m..........
        .qtq.mpm..00000.
        .qtsqonmu0456540
        .qsredmxu0455430
        05dedcdw05434320
        045c44b444330410
        .0405b4443320310
        .03054433322020.
        .02044333221010.
        ..004333221100..
        ...0433222110...
        ...0110111010q..
        ...04430432dqtq.
        ...04320321csq..
        ...032100000q...
        ...00000........
        == leggings
        ................
        ................
        ..000000000000..
        ..055544433210..
        ..04c433322110.q
        ..05c300003210qt
        ..054d0..0421qts
        ..043c0..0431tsq
        ..04b20..043esrq
        ..03320..03ddrq.
        ..04310..042cpm.
        ..03210..032monm
        .ud3210..0210mm.
        uxu0000..0000...
        uv..............
        ................
        == boots
        ................
        ................
        .......m........
        .00000mpm.00000.
        .05540onm.05540.
        .0543dnm..04430.
        .044c0....05320.
        .05320....04320.
        .04320....04310.
        054310....033210
        065430....043dtq
        044320....032csq
        000000....0000q.
        ................
        ................
        ................
""",
})

# ===========================================================================
# 4 - BRACE NELLE CREPE
# metallo carbone caldo; crepe di allthemodium fuso; cuoio sangue di bue (g-k) sulle impugnature
# ===========================================================================
STYLES.append({
    "num": 4, "slug": "brace", "nome": "Brace nelle Crepe",
    "desc": 'Metallo carbone-viola spaccato da crepe ramificate di allthemodium fuso; nei nodi delle crepe pulsa un nucleo C, M o Y. Cuoio sangue di bue su impugnature e cinghie.',
    "asim": "Ogni crepa si ramifica in modo diverso; un occhio dell'elmo acceso e l'altro spento, cinghia su una spalla sola, fascia di cuoio su un ginocchio, lacci su uno stivale e crepa sull'altro.",
    "cmy": 'nuclei nei nodi delle crepe', "luce": 'crepe di metallo fuso',
    "pal": {
        "0": "#0b080c", "1": "#1a1219", "2": "#271c26", "3": "#362834", "4": "#483746",
        "5": "#614c5d", "6": "#86707f",
        "g": "#1d0608", "h": "#45121a", "i": "#6e2229", "j": "#96383a", "k": "#bd5a52",
    },
    "art": """
        == sword
        ..............00
        .............050
        ............0540
        ..........05b420
        .........054c320
        ........054edb0.
        .......05cf320..
        ...000043se10...
        ..0554d3210000..
        ..043ce4433210..
        ...00boc32210...
        ....0j000000....
        ...0jh..........
        ..0ig...........
        .0w0............
        ..0.............
        == axe
        ................
        .000000000...00.
        0556554430..040.
        05e54433200040..
        0cfd433221140...
        0b4es3221140....
        0433dc21140.....
        03322b1040......
        .00000040.......
        ......jg........
        .....40.........
        ....jg..........
        ...40...........
        ..jg............
        .0w0............
        ..0.............
        == helmet
        ................
        ................
        ....0c000000....
        ...05e6554430...
        ..0565e5443320..
        ..0554d4c33220..
        ..044b4oe32210..
        ..0300323ex010..
        ..0332322cd210..
        ..gj22121b1110..
        ..gi1101110110..
        ..gh0000000000..
        ...ih...........
        ....g...........
        ................
        ................
        == chestplate
        ................
        .0hh00....00000.
        05ji540..0565c40
        05ij430000554e30
        04hi44554434d220
        .00hj554433s1010
        .04ih54443eb2030
        .03ji4433d221020
        .02ih4333e211010
        ...hj333c2210...
        ...0ih3b22110...
        ...0hadahhhh0...
        ...0544304320...
        ...04c3203210...
        ...033210000....
        ...00000........
        == leggings
        ................
        ................
        ..gggggggggggg..
        ..hjjiaedaiihg..
        ..045433322110..
        ..05c400003210..
        ..054e0..04210..
        ..04of0..04310..
        ..0d3c0..jihhg..
        ..0b3b0..ijihg..
        ..04320..03210..
        ..03210..03210..
        ..03210..02110..
        ..00000..00000..
        ................
        ................
        == boots
        ................
        ................
        ................
        .ggggg....00000.
        .hjjig....055c0.
        .04430....044e0.
        .0i4i0....04d30.
        .04h30....03w20.
        .0i3i0....04c20.
        054320....03b210
        0ce430....043320
        04b320....032210
        000000....000000
        ................
        ................
        ................
""",
})

# ===========================================================================
# 5 - RELIQUIA RITUALE
# metallo cenere; nastri di pergamena (g-k); catene; occhio-gemma
# ===========================================================================
STYLES.append({
    "num": 5, "slug": "reliquia", "nome": "Reliquia Rituale",
    "desc": "Metallo cenere con occhi-gemma dalle palpebre d'oro allthemodium, catene e strisce di pergamena scritte con inchiostro CMY.",
    "asim": "Talismano sulla lama e catena che pende da un lato della guardia, occhio sulla fronte spostato a destra, pergamena a sinistra e catena a destra dell'elmo, catena in diagonale sul petto, nastro su una coscia, catena a una caviglia.",
    "cmy": 'iride, inchiostro, perline', "luce": "palpebre d'oro e puntali",
    "pal": {
        "0": "#0d0b12", "1": "#201d29", "2": "#312d3d", "3": "#444055", "4": "#5c576f",
        "5": "#7b758f", "6": "#a6a0ba",
        "g": "#33210f", "h": "#7a5a35", "i": "#b8935f", "j": "#e3c690", "k": "#fff0cc",
    },
    "art": """
        == sword
        ..............00
        .............060
        ............0540
        ...........05420
        ..........054kjg
        .........05jsig.
        ...aa...04ijhg..
        ...aeda043210...
        ....adeeda10....
        ....afomfa......
        ....0acdcaeda...
        ...040.....aa...
        ..030.......6...
        .040........4...
        qtq.........6...
        .r..........w...
        == axe
        ................
        .aed5500.....00.
        ae5554430...040.
        adadeda3300040..
        0afomfa322140...
        acacdca22140....
        ae332211040.....
        .ad2110jkhg.....
        ..aa004ihjg.....
        .....40.hg......
        ....40...g......
        ...40...........
        ..40............
        .40.............
        uw0.............
        .u..............
        == helmet
        ................
        ................
        ....00000000....
        ...0566554430...
        ..05655adeeda06.
        ..05554afomfa03.
        .gk44443acdca06.
        .hj00c000000003.
        .is04433032210w.
        .jh03322022110v.
        .iw02211011100..
        .hg.00000000....
        ..hg............
        ................
        ................
        ................
        == chestplate
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
        ...0433222160...
        ...0gk01110w0...
        ...0js4304320...
        ...0ij3203210...
        ...0hi210000....
        ....gh..........
        == leggings
        ................
        ................
        ..000000000000..
        ..055544433210e.
        ..0443332221106.
        ..0543000032100.
        .gjkjig..042106.
        .hisihg..043100.
        .i04310..aefdaw.
        .h03320..acdba..
        .g04310..03210..
        ..03210..03210..
        ..03210..02110..
        ..00000..00000..
        ................
        ................
        == boots
        ................
        ................
        ................
        .00000....00000.
        .05540....05540.
        .gkjjg....04430.
        .hisih....05320.
        .04320h...60606.
        .04310g...04310x
        043210....033210
        aed430....043320
        adc320....032210
        aa0000....000000
        ................
        ................
        ................
""",
})

for _st in STYLES:
    _st["grids"] = parse_art(_st["art"])


# ===========================================================================
# ANIMAZIONE (stile 3, il migliore): 12 fotogrammi, 3 s, interpolate false.
# Griglie di FASE scritte a mano: '.' = pixel fermo; '0'-'9','A','B' = pixel di
# energia e fotogramma in cui passa la cresta di luce. Ogni pixel di energia sale
# lungo la sua rampa (scritta a mano qui sotto) di WAVE[(fotogramma - fase) % 12]
# gradini. La luce parte dal pomolo, accende le radici arancio e risale i cristalli.
# ===========================================================================
ANIM_STYLE = 3
RAMPS = [["r", "s", "t", "W"],          # topazio
         ["n", "o", "p", "W"],          # ametista
         ["v", "w", "x", "W"],          # citrino
         ["b", "c", "d", "e", "f"]]     # allthemodium
WAVE = [2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0]
FRAMES, FRAMETIME = 12, 15              # 12 x 15 tick = 180 tick = 9 s

PHASES = {
    "sword": [
        "................",
        "..............9.",
        ".............88.",
        "............777.",
        "...........6555.",
        "............4...",
        ".7..............",
        ".66.6...........",
        "..5555..........",
        "...4444.........",
        ".......3........",
        "................",
        "................",
        "................",
        "..1.............",
        ".0..............",
    ],
    "axe": [
        "................",
        ".88.............",
        ".776............",
        ".76543..........",
        ".76543..........",
        ".765.3.......4..",
        ".87..........56.",
        "..8.............",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "..1.............",
        ".0..............",
    ],
}


def anim_grids(grid, phases):
    """I 12 fotogrammi: la griglia disegnata, con i soli pixel di energia spostati sulla rampa."""
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
                assert ramp, f"pixel di energia ({x},{y}) su '{row[x]}', che non sta in nessuna rampa"
                step = WAVE[(f - int(ph, 16)) % FRAMES]
                row[x] = ramp[min(ramp.index(row[x]) + step, len(ramp) - 1)]
            rows.append("".join(row))
        frames.append(rows)
    return frames


# ---------------------------------------------------------------------------
# Conversione e composizione (nessuna regola di disegno qui sotto)
# ---------------------------------------------------------------------------
SLOT = (38, 34, 45, 255)


def palette(style):
    p = dict(BASE)
    p.update(style["pal"])
    return {k: hexc(v) for k, v in p.items()}


def render(grid, pal):
    assert len(grid) == 16, f"{len(grid)} righe"
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        assert len(row) == 16, f"riga {y} lunga {len(row)}: {row!r}"
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), pal[ch])
    return im


def up(im, k):
    return im.resize((im.width * k, im.height * k), Image.NEAREST)


def on_bg(im, col=SLOT):
    bg = Image.new("RGBA", im.size, col)
    bg.alpha_composite(im)
    return bg


def ref(item):
    return Image.open(os.path.join(REF, f"bedrock_{item}.png")).convert("RGBA")


def sprites(style):
    pal = palette(style)
    return {it: render(style["grids"][it], pal) for it in ITEMS if it in style["grids"]}


def style_dir(st):
    return os.path.join(OUT, f"{st['num']:02d}_{st['slug']}")


def confronto(style, d):
    """Autocritica: bedrock Spectrum sopra, lo stile sotto, x12; in basso 1x e 2x."""
    sp = sprites(style)
    k, pad = 12, 8
    W = len(ITEMS) * (16 * k + pad) + pad
    H = 2 * (16 * k + pad) + 60
    sheet = Image.new("RGBA", (W, H), (24, 22, 30, 255))
    for i, it in enumerate(ITEMS):
        x = pad + i * (16 * k + pad)
        sheet.alpha_composite(on_bg(up(ref(it), k)), (x, pad))
        sheet.alpha_composite(on_bg(up(sp[it], k)), (x, 16 * k + 2 * pad))
        yb = 2 * (16 * k + pad) + 10
        sheet.alpha_composite(on_bg(sp[it]), (x, yb + 8))
        sheet.alpha_composite(on_bg(up(sp[it], 2)), (x + 24, yb))
        sheet.alpha_composite(on_bg(ref(it)), (x + 72, yb + 8))
        sheet.alpha_composite(on_bg(up(ref(it), 2)), (x + 96, yb))
    sheet.save(os.path.join(d, f"confronto_{style['num']}.png"))


def animations():
    """Salva striscia verticale + .mcmeta (formato Minecraft) e GIF di anteprima. Ritorna i GIF."""
    st = next(s for s in STYLES if s["num"] == ANIM_STYLE)
    pal, out = palette(st), {}
    for it, phases in PHASES.items():
        frames = [render(g, pal) for g in anim_grids(st["grids"][it], phases)]
        strip = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
        for i, fr in enumerate(frames):
            strip.alpha_composite(fr, (0, 16 * i))
        base = os.path.join(style_dir(st), f"t6_{it}_anim")
        strip.save(base + ".png")
        with open(base + ".png.mcmeta", "w") as fh:
            fh.write('{\n  "animation": {\n    "frametime": %d,\n    "interpolate": false\n  }\n}\n' % FRAMETIME)
        gif = [on_bg(fr).convert("RGB") for fr in frames]
        buf = io.BytesIO()
        gif[0].save(buf, format="GIF", save_all=True, append_images=gif[1:], duration=FRAMETIME * 50, loop=0)
        with open(base + "_anteprima.gif", "wb") as fh:
            fh.write(buf.getvalue())
        row = Image.new("RGBA", (FRAMES * 18 - 2, 16), (0, 0, 0, 0))
        for i, fr in enumerate(frames):
            row.alpha_composite(on_bg(fr), (i * 18, 0))
        out[it] = (buf.getvalue(), row)
    return out


def panoramica(path):
    from PIL import ImageDraw, ImageFont
    k, cell, lab = 8, 16 * 8 + 12, 190
    small = 6 * 20 + 8
    W = lab + 6 * cell + small + 16
    rows = [("Spectrum", "bedrock (metro)", {it: ref(it) for it in ITEMS})]
    rows += [(str(st["num"]), st["nome"], sprites(st)) for st in STYLES]
    H = len(rows) * cell + 12
    sheet = Image.new("RGBA", (W, H), (20, 18, 26, 255))
    dr = ImageDraw.Draw(sheet)
    big, txt = ImageFont.load_default(size=30), ImageFont.load_default(size=15)
    for r, (a, b, sp) in enumerate(rows):
        y = 8 + r * cell
        dr.text((12, y + 34), a, fill=(232, 151, 37) if r else (200, 196, 210), font=big)
        dr.text((12, y + 72), b, fill=(200, 196, 210), font=txt)
        for i, it in enumerate(ITEMS):
            sheet.alpha_composite(on_bg(up(sp[it], k)), (lab + i * cell, y))
            sheet.alpha_composite(on_bg(sp[it]), (lab + 6 * cell + 4 + i * 20, y + 20))
            sheet.alpha_composite(on_bg(up(sp[it], 2)), (lab + 6 * cell + 4 + (i % 3) * 40, y + 48 + (i // 3) * 40))
    sheet.save(path)


def b64(im_or_bytes, fmt="PNG"):
    if isinstance(im_or_bytes, bytes):
        data = im_or_bytes
    else:
        buf = io.BytesIO()
        im_or_bytes.save(buf, format=fmt)
        data = buf.getvalue()
    return f"data:image/{fmt.lower()};base64," + base64.b64encode(data).decode()


CSS = """
:root {
  /* Layout: barra fissa col metro Spectrum, poi una scheda per stile; ogni oggetto in uno slot d'inventario */
  --bg: #131118; --surface: #1c1922; --slot: #26222d; --slot-hi: #3a3443; --slot-lo: #0c0a10;
  --fg: #ece7f1; --muted: #a59db1; --line: #2f2a37; --accent: #e89725;
  --c: #20a9d2; --m: #c1329b; --y: #e6d62c;
  --display: "Pixelify Sans", "Courier New", monospace;
  --text: "Inter", system-ui, -apple-system, "Segoe UI", sans-serif;
  color-scheme: dark;
}
* { box-sizing: border-box; }
body { background: var(--bg); color: var(--fg); font-family: var(--text); font-size: 15px; line-height: 1.5; margin: 0; }
body::before { content: ""; position: fixed; inset: 0 0 auto; height: env(safe-area-inset-top, 0px); z-index: 10; background: var(--surface); }
.wrap { max-width: 1120px; margin: 0 auto; padding-left: max(16px, env(safe-area-inset-left, 0px)); padding-right: max(16px, env(safe-area-inset-right, 0px)); }
header.intro { padding-block: 28px 10px; }
h1 { font-family: var(--display); font-weight: 600; font-size: clamp(28px, 5vw, 44px); line-height: 1.1; margin: 0 0 10px; text-wrap: balance; }
h1 .tri { display: inline-flex; gap: 4px; vertical-align: middle; margin-left: 10px; }
h1 .tri i { width: 12px; height: 12px; display: block; }
.lead { color: var(--muted); max-width: 68ch; margin: 0; }
.lead b { color: var(--fg); font-weight: 600; }
.ref { position: sticky; top: env(safe-area-inset-top, 0px); z-index: 5; background: var(--surface); border-bottom: 1px solid var(--line); }
.ref .wrap { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 16px; padding-block: 8px; }
.ref .lbl { font-size: 12px; letter-spacing: .06em; text-transform: uppercase; color: var(--muted); }
.ref .lbl b { color: var(--fg); display: block; letter-spacing: 0; text-transform: none; font-size: 13px; }
.ref .row { display: flex; flex-wrap: wrap; gap: 6px; }
.ref .slot { width: 56px; height: 56px; }
.ref .slot img { width: 48px; height: 48px; }
.ref nav { display: flex; flex-wrap: wrap; gap: 4px; margin-left: auto; }
.ref nav a { font-family: var(--display); color: var(--fg); text-decoration: none; min-width: 32px; height: 32px; display: grid; place-items: center; background: var(--slot); border: 1px solid var(--line); font-size: 15px; }
.ref nav a:hover, .ref nav a:focus-visible { border-color: var(--accent); color: var(--accent); outline: none; }
main { display: grid; gap: 28px; padding-block: 24px 48px; }
.style { background: var(--surface); border: 1px solid var(--line); padding: 20px; display: grid; gap: 18px; min-width: 0; scroll-margin-top: 90px; }
.style.best { border-color: var(--accent); }
.head { display: grid; grid-template-columns: auto 1fr; gap: 4px 18px; align-items: start; }
.num { font-family: var(--display); font-size: 64px; line-height: .9; color: var(--accent); grid-row: span 2; min-width: 1.2ch; text-align: center; }
.head h2 { font-family: var(--display); font-weight: 500; font-size: 26px; margin: 0; line-height: 1.15; }
.head h2 small { font-family: var(--text); font-size: 12px; letter-spacing: .08em; text-transform: uppercase; color: var(--accent); margin-left: 10px; vertical-align: middle; }
.head p { margin: 0; color: var(--muted); max-width: 70ch; min-width: 0; }
dl.tags { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 6px 16px; margin: 0; }
dl.tags div { min-width: 0; }
dl.tags div.wide { grid-column: 1 / -1; }
dl.tags dt { font-size: 11px; letter-spacing: .08em; text-transform: uppercase; color: var(--muted); }
dl.tags dd { margin: 0; font-size: 14px; max-width: 90ch; }
.items { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 10px; }
figure { margin: 0; display: grid; gap: 4px; justify-items: center; min-width: 0; }
figcaption { font-size: 12px; color: var(--muted); }
.slot { background: var(--slot); border: 2px solid; border-color: var(--slot-lo) var(--slot-hi) var(--slot-hi) var(--slot-lo); display: grid; place-items: center; }
.items .slot { width: 100%; max-width: 140px; aspect-ratio: 1; }
.items .slot img { width: 86%; height: auto; max-width: 100%; }
img { image-rendering: pixelated; image-rendering: crisp-edges; display: block; }
.lower { display: flex; flex-wrap: wrap; gap: 18px 28px; align-items: flex-start; }
.block { display: grid; gap: 6px; min-width: 0; }
.block h3 { font-size: 11px; letter-spacing: .08em; text-transform: uppercase; color: var(--muted); margin: 0; font-weight: 600; }
.real { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.real .slot { padding: 2px; }
.real .s1 { width: 22px; height: 22px; } .real .s1 img { width: 16px; height: 16px; }
.real .s2 { width: 38px; height: 38px; } .real .s2 img { width: 32px; height: 32px; }
.anim { display: flex; flex-wrap: wrap; gap: 10px; }
.anim .slot { width: 140px; height: 140px; max-width: 100%; }
.anim .slot img { width: 128px; height: 128px; }
.strip { overflow-x: auto; max-width: 100%; }
.strip img { width: 856px; height: 64px; max-width: none; }
.note { font-size: 13px; color: var(--muted); max-width: 70ch; margin: 0; }
footer { color: var(--muted); font-size: 13px; padding-block: 0 40px; }
@media (max-width: 720px) {
  .items { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .num { font-size: 48px; }
  .style { padding: 14px; }
  .ref .slot { width: 44px; height: 44px; } .ref .slot img { width: 36px; height: 36px; }
  .anim .slot { width: 120px; height: 120px; } .anim .slot img { width: 96px; height: 96px; }
}
@media (prefers-reduced-motion: reduce) { .gif { display: none; } }
"""


def html(path, anims):
    def slot_row(sp, size):
        return "".join(f'<div class="slot {size}"><img src="{b64(sp[it])}" alt="{NOMI[it]}"></div>' for it in ITEMS)

    refs = {it: ref(it) for it in ITEMS}
    nav = "".join(f'<a href="#stile-{s["num"]}">{s["num"]}</a>' for s in STYLES)
    parts = [f"""<title>Trichromatic disegnato a mano</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&family=Pixelify+Sans:wght@500;600&display=swap">
<style>{CSS}</style>
<header class="intro"><div class="wrap">
  <h1>Trichromatic, disegnato a mano <span class="tri" aria-hidden="true"><i style="background:var(--c)"></i><i style="background:var(--m)"></i><i style="background:var(--y)"></i></span></h1>
  <p class="lead">Cinque stili per il tier T6, ognuno con spada, ascia ed elmo, corazza, gambali e stivali in vista inventario.
  Corpo scuro come la bedrock di Spectrum, l'arancio allthemodium usato come <b>luce</b>, il ciano, magenta e giallo nelle gemme e nell'energia.
  Ogni sprite è una griglia 16×16 scritta pixel per pixel, senza forme generate. <b>Scegli il numero</b> che ti convince: lo rifiniamo da lì.</p>
</div></header>
<div class="ref"><div class="wrap">
  <div class="lbl">Metro di paragone<b>Spectrum classico, bedrock</b></div>
  <div class="row">{"".join(f'<div class="slot"><img src="{b64(refs[it])}" alt="bedrock {NOMI[it]}"></div>' for it in ITEMS)}</div>
  <nav aria-label="Vai allo stile">{nav}</nav>
</div></div>
<main class="wrap">"""]
    for st in STYLES:
        sp = sprites(st)
        best = st["num"] == ANIM_STYLE
        figs = "".join(f'<figure><div class="slot"><img src="{b64(sp[it])}" alt="{NOMI[it]} {st["nome"]}"></div>'
                       f'<figcaption>{NOMI[it]}</figcaption></figure>' for it in ITEMS)
        extra = ""
        if best:
            gifs = "".join(f'<figure><div class="slot"><img src="{b64(g, "GIF")}" alt="{NOMI[it]} animata"></div>'
                           f'<figcaption>{NOMI[it]}, 3 s</figcaption></figure>' for it, (g, _) in anims.items())
            strips = "".join(f'<div class="strip"><img src="{b64(r)}" alt="12 fotogrammi {NOMI[it]}"></div>'
                             for it, (_, r) in anims.items())
            extra = f"""
    <div class="block"><h3>Animazione: la luce risale dal pomolo ai cristalli</h3>
      <div class="anim gif">{gifs}</div>
      <p class="note">12 fotogrammi da 5 tick (3 s), interpolate false. Si muovono solo i pixel di energia:
      la cresta di luce parte dal pomolo di citrino, accende le fratture arancio e risale i cristalli fino alla punta.</p>
      {strips}
    </div>"""
        parts.append(f"""
<section class="style{' best' if best else ''}" id="stile-{st['num']}">
  <div class="head"><div class="num">{st['num']}</div><h2>{st['nome']}{'<small>il mio preferito</small>' if best else ''}</h2><p>{st['desc']}</p></div>
  <dl class="tags">
    <div><dt>CMY</dt><dd>{st['cmy']}</dd></div><div><dt>Luce arancio</dt><dd>{st['luce']}</dd></div>
    <div class="wide"><dt>Asimmetria</dt><dd>{st['asim']}</dd></div>
  </dl>
  <div class="items">{figs}</div>
  <div class="lower">
    <div class="block"><h3>Dimensione reale 1×</h3><div class="real">{slot_row(sp, 's1')}</div></div>
    <div class="block"><h3>2×</h3><div class="real">{slot_row(sp, 's2')}</div></div>{extra}
  </div>
</section>""")
    parts.append("""
</main>
<footer><div class="wrap">Sprite originali per Spectral Ascension, disegnati a mano studiando la tecnica di Spectrum (asimmetria, tre materiali, contorni tinti, luce a grappoli), senza ricalcarne le texture.</div></footer>
""")
    with open(path, "w") as fh:
        fh.write("".join(parts))


def main():
    args = sys.argv[1:]
    if args and args[0] == "confronto":
        os.makedirs(args[1], exist_ok=True)
        for st in STYLES:
            confronto(st, args[1])
        return
    for st in STYLES:
        os.makedirs(style_dir(st), exist_ok=True)
        for it, im in sprites(st).items():
            im.save(os.path.join(style_dir(st), f"t6_{it}.png"))
    anims = animations()
    panoramica(os.path.join(OUT, "panoramica.png"))
    html(os.path.join(OUT, "stili.html"), anims)


if __name__ == "__main__":
    main()
