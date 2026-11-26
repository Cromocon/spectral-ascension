# 14 — Le 3 leghe di Allthemodium (alloy): id, ricette, usi in ATM10, colori

> Ricerca per **Spectral Ascension**. Tier Alloy «Prismatic». Solo lettura: nulla modificato nell'istanza.
> Legenda: **[V]** verificato nel file (percorso indicato), **[D]** dedotto.
> Abbreviazioni:
> - `jar(atm)` = `jar allthemodium-3.2.1_mc_1.21.1.jar` estratto in `<scratchpad>/atm/` (copia del jar in `istanza:mods/`)
> - `istanza:` = la cartella della tua istanza CurseForge di ATM10
> - Aggiunge e corregge [07](07-allthemodium-e-atm10.md) §1.2 e [10](10-armature-piu-forti-atm10.md). Checklist progressione: [00](00-checklist-progressione.md).

## 0. Sintesi

| | **VA** Vibranium+Allthemodium | **UA** Unobtainium+Allthemodium | **UV** Unobtainium+Vibranium |
|---|---|---|---|
| Ricetta ATM10 (ingot) | **Powah Energizing**, 1.000.000.000 FE | **Ars Nouveau Enchanting Apparatus**, 10.000 Source | **Industrial Foregoing Dissolution Chamber**, 100 mB soul lava, 200 tick |
| Ingredienti comuni | 2 Piglich Heart + 1 lingotto per metallo | 2 Piglich Heart + 1 lingotto per metallo | 2 Piglich Heart + 1 lingotto per metallo |
| Extra | 1 Nitro Crystal Block (All the Compressed) | 4 essenze Ars (aria, terra, fuoco, acqua) + Source Gem | 4 Pink Slime + 100 mB soul lava |
| Differenze di proprietà | nessuna (stesse 3 voci, stessa texture a 16 px) | nessuna | nessuna |
| Usi esclusivi | Creative Jetpack (blocco), Infusione Patrick Star (lingotto) | ATM Star: **28 blocchi** | ATM Star: **2 blocchi «Awakened»** (versione con Unbreaking 1, creata a un Star Altar) |
| Usi comuni (tutte e 3) | tool Alloy (1 lingotto per tipo), Infused Patrick Star, ExtendedAE Infinity-like cell (kubejs), ATM Star | idem | idem |

Le tre leghe **non hanno nessuna differenza meccanica**: sono solo item/blocchi crafting. Cambia solo come si fabbricano (tre macchine diverse) e dove sono richieste nelle ricette di endgame.

## 1. Id, nomi in gioco e tag

Esistono **solo** lingotto, polvere e blocco **[V]** (`jar(atm)/assets/allthemodium/lang/en_us.json`, `data/c/tags/item/{ingots,dusts,storage_blocks}/*_alloy.json`).
**Non esistono** pepite, piastre, ingranaggi, barre, né item grezzi **[V]**: `data/allthemodium/recipe/` ha `*_plate/gear/rod/nugget` solo per i 3 metalli singoli. Le texture `*_alloy_plate.png` sono nel jar ma senza modello né registrazione (`models/item/` non ha `*_alloy_plate.json`; `ModRegistry.class` non ha il nome) **[V]**: texture orfane.

| Lega | Lingotto | Polvere | Blocco |
|---|---|---|---|
| VA | `allthemodium:vibranium_allthemodium_alloy_ingot` — «Vibranium - Allthemodium Alloy Ingot» | `allthemodium:vibranium_allthemodium_alloy_dust` | `allthemodium:vibranium_allthemodium_alloy_block` — «Block of Vibranium - Allthemodium Alloy» |
| UA | `allthemodium:unobtainium_allthemodium_alloy_ingot` — «Unobtainium - Allthemodium Alloy Ingot» | `allthemodium:unobtainium_allthemodium_alloy_dust` | `allthemodium:unobtainium_allthemodium_alloy_block` |
| UV | `allthemodium:unobtainium_vibranium_alloy_ingot` — «Unobtainium - Vibranium Alloy Ingot» | `allthemodium:unobtainium_vibranium_alloy_dust` | `allthemodium:unobtainium_vibranium_alloy_block` |

Tag (ognuno contiene solo il proprio item) **[V]**: `c:ingots/<lega>`, `c:dusts/<lega>`, `c:storage_blocks/<lega>` con `<lega>` = `vibranium_allthemodium_alloy`, `unobtainium_allthemodium_alloy`, `unobtainium_vibranium_alloy`. I tag delle polveri sono aggiunti anche da KubeJS (`istanza:kubejs/server_scripts/Tweaks/tags.js` r.61-69) **[V]**. Fluidi fusi (usati solo da Tinkers, vedi §2.4): `allthemodium:molten_atmvib`, `molten_atmunob`, `molten_vibunob` **[V]**.
Compressione 9↔1: `…_alloy_block` da `aaa/aaa/aaa` di `#c:ingots/<lega>`, e block → 9 lingotti (`recipe/*_alloy_block.json`, `*_ingot_from_block.json`) **[V]**. All the Compressed ha i 9 livelli compressi di ciascun blocco lega (`allthecompressed:<lega>_alloy_block_1x…`) **[V]** (`allthecompressed-1.21.1-4.4.0.jar/data/allthecompressed/recipe/…`).
Polvere: unica ricetta trovata è Occultism, lingotto → polvere (`occultism:crushing`, `ignore_crushing_multiplier`, tag `c:ingots/<lega>` → `c:dusts/<lega>`), `occultism-1.21.1-neoforge-1.225.0.jar/data/occultism/recipe/crushing/*_alloy_dust_from_ingot.json` **[V]**. Nessuna ricetta polvere → lingotto (nessun smelting/blasting per le leghe nel jar) **[V]**: la polvere è un vicolo cieco. Cercando gli id in tutti i jar di `mods/`, i soli con ricette sulle leghe sono allthemodium, allthecompressed e occultism **[V]**.

## 2. Come si fabbricano

### 2.1 VA — Powah Energizing (KubeJS ATM10)
`istanza:kubejs/server_scripts/modpack/atm_alloys.js` **[V]**, ricetta `powah:energizing` id `kubejs:energizing/vibranium_allthemodium_alloy_ingot`:
- ingredienti: `#c:ingots/allthemodium`, `allthemodium:piglich_heart`, `allthecompressed:nitro_crystal_block_1x`, `allthemodium:piglich_heart`, `#c:ingots/vibranium` (5 slot);
- energia **1.000.000.000 FE** (1 G).
Blocco: `#c:storage_blocks/allthemodium`, 2 `piglich_heart_block`, `nitro_crystal_block_2x`, `#c:storage_blocks/vibranium`, **9.000.000.000 FE**. Esistono anche i blocchi compressi da 3x a 9x (9 G × 3^(n-2)) **[V]**.
Quindi blocco = 9 × lingotto (stesso costo, comodo da compattare).

### 2.2 UA — Ars Nouveau Enchanting Apparatus (KubeJS ATM10)
Stesso file **[V]**: reagente `ars_nouveau:source_gem`; 8 piedistalli: 2 `piglich_heart`, `ars_nouveau:air_essence`, `earth_essence`, `fire_essence`, `water_essence`, `#c:ingots/allthemodium`, `#c:ingots/unobtainium`; **10.000 Source**; `keepNbtOfReagent: false`.
Blocco: `source_gem_block` + 2 `piglich_heart_block`, 4 blocchi essenza (`kubejs:*_essence_block`, 9 essenze ciascuno), blocchi dei due metalli; **90.000 Source**.

### 2.3 UV — Industrial Foregoing Dissolution Chamber (KubeJS ATM10)
Stesso file **[V]**: 8 ingressi: 4× `industrialforegoing:pink_slime`, `#c:ingots/vibranium`, `#c:ingots/unobtainium`, 2× `piglich_heart`; fluido `100x allthemodium:soul_lava`; **processing_time 200** (tick); risultato 1 lingotto. Blocco: 4 `pink_slime_block`, 2 `piglich_heart_block`, blocchi dei due metalli, `900x soul_lava`, 200 tick.
Nota: i valori energetici della Dissolution Chamber (default di IF, 40 FE/tick circa) non sono impostati dallo script **[D]**.

### 2.4 Ricette originali di Allthemodium: presenti nel jar ma morte in ATM10
Il jar ha (`jar(atm)/data/allthemodium/recipe/`):
- `alloys/molten_*.json`: `tconstruct:alloy`, 90 mB di ogni metallo fuso → 90 mB di lega fusa, temperatura 9000 **[V]**;
- `casting/alloys/*_alloy_ingot.json`: `tconstruct:casting_table` con **Piglich Heart come stampo consumato**, 90 mB di lega fusa; cooling_time 740 (VA), 1209 (UA), 1804 (UV) tick **[V]**.
Tutte con condizione `neoforge:mod_loaded: tconstruct`. In `istanza:mods/` **non c'è Tinkers' Construct** (ls di mods/: nessun `tconstruct`/`mantle`) **[V]**, quindi in ATM10 queste ricette non esistono: da qui il kubejs del §2.1-2.3. Conferma che il «1 cuore per lingotto + 90 mB» originale è stato sostituito da «2 cuori + materiali» **[D]** (il 07 lo dice già).
Blocchi alternativi **Ice and Fire Dragon Forge** (`istanza:kubejs/data/iceandfire/recipe/dragonforge/*.json`) **[V]**: secchio fuso di un metallo + secchio fuso dell'altro in 1000 tick → 1 **blocco** lega: VA (fuoco: allthemodium + vibranium), UA (ghiaccio: allthemodium + unobtainium), UV (fulmine: vibranium + unobtainium). Servono 3 tipi di drago diversi (fuoco, ghiaccio, fulmine) e le ricette non richiedono Piglich Heart: è l'unica via ai blocchi senza cuori **[D]**.

## 3. Proprietà (tutte uguali)
- Non c'è nessun livello di strumento o tag per singola lega. `ATMTier.ALLOY` (9001 usi nominali, velocità 35, bonus 65, incantabilità 200, tag `c:incorrect_for_allthemodiumalloy_tool`) è un tier unico dei tool **alloy_***, non delle leghe **[V]** (07 §3).
- Non esiste **nessuna armatura** in lega (`ArmorRegistries` ha solo i 3 set dei metalli base) e non c'è materiale Tinkers / Silent Gear per le leghe: `data/allthemodium/tinkering/materials/*` ha solo allthemodium, unobtainium, vibranium **[V]**. (Le chiavi lang `material.silentgear.*_alloy` esistono ma senza definizione.)
- **Tool in lega**: `allthemodium:alloy_sword`, `alloy_pick`, `alloy_axe`, `alloy_shovel`, `alloy_paxel`, `alloy_trident` (tag di riparazione `UNOBATM_ALLOY`) **[V]** (`ModRegistry.class`). Si fabbricano così (ATM10) **[V]**: 1 lingotto di **ciascuna delle 3 leghe** + il tool unobtainium corrispondente + 3 scaglie di drago di Forbidden Arcanus (silver, golden, aquatic) + 2 eternal stella, nell'**Hephaestus Forge** (tier 4; essenze aureal 100, blood 1000, souls 32, experience 300), oppure nella macchina **Auto Forge** di Modern Industrialization (1024 EU/t, 100 tick; fluidi: liquid aureal 1000, liquid souls 320, blood 10.000, experience 3000) (`istanza:kubejs/data/allthemodium/forbidden_arcanus/hephaestus_forge/ritual/alloy_*.json`, `kubejs/server_scripts/modpack/runic_multis/recipes/auto_forge.js` r.160-225). Il paxel non richiede leghe ma i 4 tool alloy + 4 prismi + paxel awakened supremium (r.226-241).
- Il trident non ha ricetta nel jar né nello script esaminato (non cercata in profondità) **[D]**.
- I blocchi lega richiedono un piccone unobtainium (`c:needs_unobtainium_tool`, 07 §4) **[V]**.

## 4. A cosa servono in ATM10 (tutte le ricette che usano le leghe)
Elenco completo (ricerca su tutti i jar di `mods/` + tutto `kubejs/`, `config/`, `datapacks/`) **[V]**.

| Uso | Mod / macchina | VA | UA | UV | Fonte |
|---|---|---|---|---|---|
| **ATM Star** `allthetweaks:atm_star` | MI **Star Altar** (2048 EU/t, 200 tick) | — | **28 blocchi** | **2 blocchi Awakened** (Unbreaking 1, nome custom) | `kubejs/server_scripts/modpack/runic_multis/recipes/star_altar.js` |
| Awakened UV block | MI Star Altar | — | — | 1 blocco + 4 awakened supremium essence/gemstone + 4 libri Unbreaking | idem |
| **Infused Patrick Star** (serve all'ATM Star in alternativa e allo Shard) | MI Star Altar | 1 lingotto | 1 lingotto | 1 lingotto | idem |
| ATM Star Shard (1..5) | n.d. | — | — | — | nessuna lega nelle ricette trovate [D: non letto atm_star.js] |
| Creative Jetpack | `ironjetpacks:jetpack[creative]`, kubejs shaped | 2 blocchi | — | — | `kubejs/server_scripts/modpack/atm_star_creative.js` r.150-170 |
| Celle «infinite» di ExtendedAE (`kubejs:lava_cell`, `dirt_cell`, …; ~45 ricette) | crafting a banco, tutte con la stessa matrice | 1 lingotto | 1 lingotto | 1 lingotto | `kubejs/server_scripts/mods/ExtendedAE/InfinityCellRecipes.js` |
| Tool alloy (sword, pick, axe, shovel) | Hephaestus Forge / MI Auto Forge | 1 lingotto a tool | 1 a tool | 1 a tool | §3 |
| Craft di VA / UA / UV block | vedi §2 | — | — | — | — |
| Polvere | Occultism crushing | ingot → dust | idem | idem | §1 |
| Compressione (All the Compressed) | crafting 9↔1 su 9 livelli | sì | sì | sì | jar allthecompressed |

Mod **senza** ricette con le leghe: Mekanism, Powah (solo come macchina), AE2/Advanced AE/MEGA, Create, Ars (solo come macchina), Occultism (solo polvere) **[V]**. Quindi **nessun upgrade Mekanism / AE2 dipende dalle leghe**.
Quest FTB: capitolo `allthemodium.snbt` (r.1030-1202) e `chapter_2_the_star.snbt` (r.359-375, 971, 4052) le chiedono come task/obiettivi **[V]**.

### 4.1 Differenze d'uso
- **Intercambiabili** nelle celle ExtendedAE, nei tool alloy e nell'Infused Patrick Star (1 lingotto ciascuna sempre insieme).
- **Specializzate**: UA = grande volume nell'ATM Star (28 blocchi = 252 lingotti), UV = poche unità «Awakened» (2 blocchi = 18 lingotti) per lo stesso obiettivo, VA = Creative Jetpack.
- Le tre leghe viaggiano **sempre in tripletta** nei tool alloy: la progettazione di ATM lega i tool alloy ai 3 tipi insieme. È esattamente la scelta del nostro tier Alloy.

## 5. Rarità e costo relativo
- **Per lingotto** i metalli di base sono 1+1 e Piglich Heart ×2 per tutte (identico). Cambia il lavoro extra: **VA** 1 G FE + Nitro Crystal Block (il più pesante in energia), **UA** 10.000 Source + 4 essenze (pesante in Ars), **UV** soul lava + pink slime (il più semplice e veloce: 200 tick) **[D]**.
- **Più usata:** UA (28 blocchi per l'ATM Star, 252 lingotti = 504 Piglich Heart), poi tutte pari nelle celle e nei tool. VA e UV sono richieste in poche unità **[V sui numeri, D sul giudizio]**.
- La difficoltà reale è il **Piglich Heart** (boss), non la lega **[D]**.

## 6. Colori (dalle texture 16×16 del jar) **[V]**
Colori dominanti (esadecimale, % dei pixel opachi), da `jar(atm)/assets/allthemodium/textures/item/*_alloy_ingot.png`, `…_dust.png` e `block/*_alloy_block.png`:

| Lega | Lingotto | Polvere | Blocco | Colore tema |
|---|---|---|---|---|
| VA | `#0f5c7a` 19%, `#cf5a13` 12%, `#ff8b04` 11%, `#26de88` 10%, `#178287`, `#1bb38a` | `#ffc70c` 19%, `#73ffb9` 15%, `#a92405`, `#0f5c7a`, `#26de88`, `#b8ffc7` | `#1ebd95` 15%, `#f2e419`, `#f2a61d`, `#f2c01a`, `#187a85`, `#73ffb9` | verde-acqua (vibranium) + arancio-oro (allthemodium) |
| UA | `#432a94` 19%, `#cf5a13`, `#ff8b04`, `#d152e3`, `#6c1ce1`, `#a82ce3` | `#ffc70c`, `#ea84f5`, `#a92405`, `#472787`, `#d152e3`, `#f7b7f4` | `#f2c01a`, `#f2a61d`, `#d152e3`, `#942df7`, `#bc2feb`, `#fbfb5d` | viola/magenta (unobtainium) + oro |
| UV | `#0f5c7a` 19%, `#432a94`, `#6c1ce1`, `#26de88`, `#178287`, `#1bb38a` | `#a82ce3`, `#73ffb9`, `#3e1570`, `#0f5c7a`, `#26de88`, `#b8ffc7` | `#bc2feb` 15%, `#26de88`, `#1c9e91`, `#1ebd95`, `#6c1ce1`, `#ea84f5` | viola + verde-acqua, **niente oro/arancio** |
Le leghe che contengono allthemodium (VA, UA) hanno l'arancio/oro `#cf5a13`/`#ff8b04`/`#f2a61d` nelle texture; UV no. Texture a 16 px, singoli frame.

## 7. Implicazioni per il tier Alloy «Prismatic»
1. **Decisione «tutte e 3 le leghe, 1 lingotto per lega per oggetto»: coerente** con ATM (tool alloy: 1 lingotto per ciascuna delle 3 leghe, sempre insieme).
2. **«6 Piglich Heart a oggetto»**: il conto torna con ATM10 (2 cuori per ognuno dei 3 lingotti, in tutte e tre le ricette KubeJS). Ma il cuore si consuma nel craft della lega, non nella nostra ricetta: se l'oggetto chiede solo 3 lingotti, i 6 cuori sono un costo indiretto, non un ingrediente. Se vogliamo davvero 6 cuori come ingredienti si sommerebbero ai lingotti (doppio costo): da decidere. Le ricette originali del jar usano 1 cuore per lingotto (via Tinkers, assente in ATM10) **[D]**. **Deciso dall'utente (2026-11-18): solo costo indiretto, niente Piglich Heart nelle ricette Alloy.**
3. **Costo dell'oggetto Alloy in rapporto ad ATM**: ATM Star = 270 lingotti lega (252 UA + 18 UV); i nostri 15 oggetti usano 15 lingotti per lega (45 totali). Proporzionato. Un tool alloy costa 3 lingotti: il nostro oggetto (3 lingotti) è **uguale** a un tool alloy ATM per materiale.
4. **Nessuna armatura Alloy in ATM**: nessun conflitto, siamo noi a introdurla. Stesso schema dei tool (3 leghe insieme) è la scelta naturale.
5. **Niente pepite/piastre/ingranaggi/barre**: le ricette devono usare **lingotti o blocchi** (tag `c:ingots/<lega>`, `c:storage_blocks/<lega>`); le texture orfane `*_alloy_plate.png` non sono item.
6. **Il Piglich Heart** non ha tag: va indicato con l'id `allthemodium:piglich_heart` solo se lo vogliamo nella ricetta.
7. **Colori per le texture Prismatic**: usare le 3 palette del §6, ad esempio il verde-acqua `#0f5c7a/#26de88`, il viola `#432a94/#d152e3` e l'oro/arancio `#cf5a13/#ff8b04` come «ingredienti» cromatici (coerente con «Prismatic»), evitando di copiare le texture.
8. Ricetta con soli lingotti (nessun blocco): ok, i blocchi si ottengono 9→1 senza cuori (Dragon Forge) o con cuori (kubejs); i lingotti da blocco: 9 per blocco.
