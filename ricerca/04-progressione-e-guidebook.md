# 04 — Progressione di Spectrum e guidebook (Modonomicon)

> Ricerca per **Spectral Ascension** (addon NeoForge 1.21.1 per Spectrum 1.12.9, ATM10).
> Data: 2026-11-16.
>
> **Legenda fonti**
> - `jar(spectrum):…` = jar estratto `spectrum-1.12.9-1.21.1-neo`
> - `jar(SpectralDecorations):…` = jar estratto `SpectralDecorations-1.10.1-1.21.1-neo`
> - `jar(spectrumcompat):…`, `jar(modonomicon):…` (1.123.0), `jar(revelationary):…` (1.5.2), `jar(allthemodium):…` (3.2.1)
> - `src:…java:N` = sorgente Spectrum ≈1.12.9 (`src/main/java/de/dafuqs/spectrum/…`, commit `890784c`, ott. 2026: **un po' più recente del jar**)
> - `wiki:Pagina` = wiki di Spectrum, https://github.com/DaFuqs/Spectrum/wiki/Pagina
> - `modo-docs:…` = documentazione Modonomicon, branch `documentation` di https://github.com/klikli-dev/modonomicon (cartella `versioned_docs/version-1.21.1/`)
>
> ✅ **Verificato** = l'ho letto nei file/codice. 🔶 **Dedotto** = ragionamento mio, da controllare in gioco.

---

## 0. Mini-glossario (per chi arriva dal web)

| Concetto | Spiegazione breve |
|---|---|
| **Advancement** | I "progressi" di Minecraft. Sono file JSON in `data/<namespace>/advancement/…`. L'id deriva dal percorso: `data/spectrum/advancement/lategame/collect_moonstone.json` → `spectrum:lategame/collect_moonstone`. Un advancement **senza** `display` è *nascosto*: non compare nella schermata dei progressi e non dà notifiche, ma esiste e si può usare come "flag" lato server (un po' come un booleano salvato per giocatore). |
| **Criterio / trigger** | Ogni advancement ha `criteria`: ogni criterio ha un `trigger` (l'evento che lo scatta, es. `minecraft:inventory_changed` = "l'inventario è cambiato") e `conditions` (filtro, es. "contiene un lingotto X"). Di default servono **tutti** i criteri (AND); con `requirements` si possono fare OR (lista di liste). Il progresso dei singoli criteri **resta salvato**, quindi l'ordine in cui il giocatore li soddisfa non conta. Le mod possono registrare trigger propri (`spectrum:crafted_with_pedestal`, ecc.). |
| **Datapack / `data/` vs `assets/`** | Nel jar di una mod, `data/` è la parte *server* (ricette, advancement, loot table, libri Modonomicon…) e funziona come un datapack: file con lo stesso percorso di un'altra mod la **sovrascrivono**, file con percorso nuovo si **aggiungono**. `assets/` è la parte *client* (texture, modelli, **lang file**) e funziona come un resource pack. |
| **Lang file** | `assets/<modid>/lang/en_us.json` (e `it_it.json`, …): mappa chiave → testo tradotto. Le chiavi sono **globali**: una chiave definita nel lang della nostra mod è usabile ovunque, anche in file JSON nel namespace `spectrum`. |
| **Revelationary** | Libreria di DaFuqs. Rende certi blocchi/item "mascherati" (*cloaked*): finché il giocatore non ha un certo advancement, vede (e droppa) un blocco diverso, es. il minerale di malachite appare come pietra. Quando ottiene l'advancement arriva il toast "rivelazione". Aggiunge anche il trigger `revelationary:advancement_gotten` ("hai ottenuto l'advancement X"). |
| **Modonomicon** | Libreria di klikli-dev per libri-guida in gioco. Struttura: **book** (`book.json`) → **category** (una "mappa" 2D di nodi) → **entry** (un nodo cliccabile) → **page** (le pagine dentro la voce). Ogni livello può avere una **condition** (condizione di sblocco, es. "ha l'advancement X"). Spectrum usa Modonomicon per il suo guidebook ("Colorful World"). |

---

## 1. Il sistema di progressione di Spectrum

### 1.1 Principio: tutto passa dagli advancement ✅

`wiki:Adjusting-Progression` (https://github.com/DaFuqs/Spectrum/wiki/Adjusting-Progression) — riassunto:

- **Avvertimento iniziale**: è un tema "altamente avanzato"; modificare la progressione rischia di confondere o **bloccare (softlock)** i giocatori. Contattare il team in caso di dubbi.
- La progressione è "quasi interamente data-driven tramite advancement (nascosti)": i modpack la possono modificare via datapack.
- Esiste un comando di debug che trova advancement inesistenti/typo **ma non i riferimenti circolari**.
- **Sblocco ricette**: ogni ricetta Spectrum ha un advancement richiesto. Il consiglio è **non cambiare l'advancement nella ricetta**, ma modificare l'advancement "unlock" dedicato (es. `spectrum:unlocks/items/crafting_tablet`), perché lo stesso advancement è usato **sia dalla ricetta sia dalla voce del guidebook**: così restano allineati.
- Gli advancement "unlock" sono fatti di criteri `revelationary:advancement_gotten` ("sblocca quando hai A e B").
- **Rivelazioni**: gli advancement in `milestones/reveal_*` controllano quando un blocco nascosto diventa visibile (es. `spectrum:milestones/reveal_shimmerstone` scatta con `spectrum:craft_using_pedestal`). Si possono legare anche a progressi di altre mod, ma bisogna sempre dare un **indizio** al giocatore (advancement visibili o quest book).
- **Altri lock** in `milestones/`: `unlock_shooting_stars`, `unlock_overenchanting_with_enchanter`, `unlock_fourth_potion_workshop_reagent_slot`.

⚠️ **La wiki è in parte obsoleta** (verificato confrontando con il jar 1.12.9):
| Wiki dice | Realtà nel jar 1.12.9 |
|---|---|
| Le ricette hanno la proprietà `advancement_identifier` | Il campo nelle ricette è **`required_advancement`** (1558 occorrenze nelle ricette del jar, nessuna `advancement_identifier`; es. `jar(spectrum):data/spectrum/recipe/pedestal/tier3/pedestal_moonstone.json`). `advancement_identifier` è invece il nome della *condizione* dentro il trigger `revelationary:advancement_gotten`. |
| Esempio con trigger `spectrum:has_advancement` | `spectrum:has_advancement` **non compare** nel jar: tutti gli advancement usano **`revelationary:advancement_gotten`** (1419 occorrenze). |
| "Debug Command" `spectrum_test_progression_sanity` | Oggi il comando è **`/spectrum sanity [mod_id]`** (vedi §1.6). |
| `milestones/unlock_overenchanting_with_enchanter` citato come lock regolabile | Nel jar 1.12.9 il suo unico criterio è `advancement_gotten` → **`spectrum:__wip`**, advancement che **non esiste**: oggi è **irraggiungibile** (overenchanting disattivato di fatto). Non usarlo come prerequisito. |
| Ricetta d'esempio con `"cyan": 0, "item": …` | Formato 1.21: `"colors": {"spectrum:cyan": 0, …}`, `"result": {"id": …}`. |
| `wiki:Modonomicon-Recipe-Pages`: pagine con chiave `"recipe"` | Il codice legge **`"recipe_id"`** (`src:compat/modonomicon/pages/BookGatedRecipePage.java:49`), ed è ciò che usano tutti i JSON del jar. |

Pagine wiki collegate utili: `wiki:Advancement-Criteria` (elenco trigger), `wiki:Modonomicon-Pages`, `wiki:Modonomicon-Recipe-Pages`, `wiki:Integrating-Into-Modpacks-&-Servers` ("non disabilitate advancement di Spectrum", "se rimuovete una ricetta impostate l'unlock a un valore impossibile").

### 1.2 Struttura di `data/spectrum/advancement` ✅

839 file JSON (`jar(spectrum):data/spectrum/advancement/`). Cartelle:

| Cartella / prefisso id | Ruolo | Ha `display`? |
|---|---|---|
| `spectrum:<nome>` (radice), `midgame/`, `lategame/`, `endgame/` | L'albero **visibile** dei progressi (early → mid → late → endgame). Quasi tutti hanno `parent` + un criterio `gotten_previous` di tipo `revelationary:advancement_gotten` che punta al genitore (convenzione verificata dal comando sanity, vedi §1.6). | sì |
| `hidden/…` (es. `hidden/collect_shards/…`, `hidden/collect_pigment/white`, `hidden/collect_stratine_gem`) | Flag "hai raccolto X", solitamente `minecraft:inventory_changed`. | no |
| `unlocks/<gruppo>/<cosa>` (es. `unlocks/equipment/bedrock_tools`, `unlocks/blocks/fusion_shrine`, `unlocks/malachite/glass_crest_tools`) | **Gate** di ricette + voci guidebook. Composti solo da criteri `revelationary:advancement_gotten`. | no |
| `milestones/reveal_*` | Gate di **Revelationary** (quando un minerale/pianta diventa visibile). | no |
| `milestones/unlock_*` | Altri lock di meccaniche (overenchanting, 4° slot reagenti, ecc.). | no |
| `triggers/hints/…` | Supporto alle pagine "hint" acquistabili del libro. | no |
| `mod_integration/<mod>/…` | Integrazioni (malum, mythicmetals, …). | — |

Esempio reale di gate (`jar(spectrum):data/spectrum/advancement/unlocks/equipment/bedrock_tools.json`):
```json
{
  "criteria": {
    "collected_bedrock_dust": {
      "trigger": "revelationary:advancement_gotten",
      "conditions": { "advancement_identifier": "spectrum:midgame/break_decayed_bedrock" }
    },
    "collected_stratine_gem": {
      "trigger": "revelationary:advancement_gotten",
      "conditions": { "advancement_identifier": "spectrum:hidden/collect_stratine_gem" }
    }
  }
}
```

Esempio di advancement visibile (`…/lategame/build_complex_pedestal_structure.json`): `parent: spectrum:lategame/craft_moonstone_pedestal`, `frame: "challenge"`, criteri `spectrum:completed_multiblock` (`multiblock_identifier: spectrum:pedestal_complex`) **+** `gotten_previous` (`revelationary:advancement_gotten` → `spectrum:lategame/craft_moonstone_pedestal`), ricompensa 500 xp.

### 1.3 Trigger usati negli advancement (conteggio sul jar) ✅

Ottenuto con grep su tutti i `"trigger"` di `jar(spectrum):data/spectrum/advancement/`:

- **Revelationary**: `revelationary:advancement_gotten` (1419), `revelationary:advancement_count` (2). Esiste anche `revelationary:had_revelation` (classe `HadRevelationCriterion` in `jar(revelationary)`).
- **Vanilla più usati**: `minecraft:inventory_changed` (292), `minecraft:impossible` (73, advancement concessi solo da codice/comandi), `consume_item`, `effects_changed`, `location`, `placed_block`, `item_used_on_block`, …
- **Spectrum** (quelli utili a noi in grassetto): **`spectrum:crafted_with_pedestal`** (22), **`spectrum:completed_multiblock`** (16), **`spectrum:crafted_with_fusion_shrine`** (6), `spectrum:enchanter_crafting`, `spectrum:enchanter_enchanting`, `spectrum:crafted_with_spirit_instiller`, `spectrum:pedestal_recipe_calculated`, `spectrum:titration_barrel_tapping`, `spectrum:potion_workshop_brewing`, `spectrum:fishing_rod_hooked`, `spectrum:block_broken`, `spectrum:pastel_node_upgrade`, `spectrum:natures_staff_conversion`, `spectrum:trinket_change`, `spectrum:preservation_check`, `spectrum:loot_function_trigger`, `spectrum:crystallarieum_growing`, `spectrum:pastel_network_creation`, `spectrum:memory_manifesting`, `spectrum:take_off_belt_jump`, `spectrum:hummingstone_hymn`, `spectrum:fluid_dipping`, `spectrum:deeper_down_portal_opening`, `spectrum:cinderhearth_smelting`, `spectrum:azure_dike_charge_change`, `spectrum:upgrade_place`, `spectrum:slime_sizing`, `spectrum:jeopardant_kill`, `spectrum:ink_projectile_killing`, `spectrum:inertia_used`, `spectrum:divinity_tick`, `spectrum:consumed_paired_food`, `spectrum:confirmation_button_pressed`, `spectrum:collect_using_crystal_apothecary`, `spectrum:blood_orchid_plucking`.

Formato dei trigger di crafting (da `wiki:Advancement-Criteria` e confermato nei JSON del jar):
```json
"crafted_moonstone_pedestal": {
  "trigger": "spectrum:crafted_with_pedestal",
  "conditions": { "item": { "items": "spectrum:pedestal_moonstone" } }
}
```
Nota: nel jar `crafted_with_pedestal` usa la chiave **`item`** (singolare, `…/lategame/craft_moonstone_pedestal.json`), mentre la wiki mostra `items` (lista); `crafted_with_fusion_shrine` nel jar usa `items` (lista) — es. `…/lategame/create_glass_crest_tool.json`: `"items": [{"items": "#spectrum:glass_crest_equipment"}]`. 🔶 Prima di usarli, verificare il codec in `src:progression/advancement/…` o copiare esattamente un JSON del jar.

### 1.4 Legame con Revelationary ✅

- I blocchi/item nascosti sono dichiarati **nel codice** (classi `CloakedOreBlock`, `CloakedItem` con l'id dell'advancement), es. `src:registries/SpectrumBlocks.java:1238` (`MALACHITE_ORE` → `SpectrumAdvancements.REVEAL_MALACHITE`) e `src:registries/SpectrumItems.java:221-222` (raw/pure malachite).
- Revelationary supporta anche definizioni **data-driven** in `data/<ns>/revelations/*.json` (loader `RevelationDataLoader`, cartella `revelations` letta dalle stringhe della classe in `jar(revelationary)`). Nel sorgente c'è `src/main/resources/data/spectrum/revelations/colored_grass.json` (`"advancement"` + mappa `block_states` "vero → travestimento"), **assente nel jar 1.12.9** (il sorgente è più recente).
- L'advancement della rivelazione, es. `jar(spectrum):…/milestones/reveal_malachite.json`, ha un solo criterio: `advancement_gotten` → `spectrum:lategame/build_complex_pedestal_structure`. Cioè: **la malachite diventa visibile solo dopo aver costruito il Pedestal Complex (Moonstone)**.
- Il trigger `advancement_gotten` scatta quando un advancement viene concesso (mixin su `PlayerAdvancements` in `jar(revelationary):de/dafuqs/revelationary/mixin/PlayerAdvancementTrackerMixin.class`).
- **Retroattività** ✅: aprendo il guidebook, Spectrum ricontrolla tutti gli advancement non completati e concede i criteri `advancement_gotten` già soddisfatti (`src:items/GuidebookItem.java:39-66`, chiamato a riga 76, una volta per giocatore per sessione). Utile per noi: se aggiungiamo l'addon a un mondo esistente, i nostri gate si "riallineano" quando il giocatore usa il libro. 🔶 Senza aprire il libro i criteri `advancement_gotten` di un advancement *già posseduto* non scattano da soli.

### 1.5 Legame con le ricette e con i tier del Pedestal ✅

- Ogni ricetta Spectrum ha `required_advancement`. Esempio `jar(spectrum):data/spectrum/recipe/pedestal/tier3/pedestal_moonstone.json` → `"tier": "advanced"`, `"required_advancement": "spectrum:lategame/collect_moonstone"`. Fusion shrine: `…/recipe/fusion_shrine/malachite/malachite_workstaff.json` → `"required_advancement": "spectrum:unlocks/malachite/malachite_tools"`.
- Per il Pedestal servono **due** cose: `this.tier.hasUnlocked(player) && hasAdvancement(player, requiredAdvancement)` (`src:recipe/pedestal/PedestalRecipe.java:226-227`).
- I tier e il loro advancement di sblocco sono **hard-coded** (`src:recipe/pedestal/PedestalRecipeTier.java:21-24`, id in `src:registries/SpectrumAdvancements.java:90-94`):

| Tier ricetta (`"tier"`) | Advancement che lo sblocca | Colori |
|---|---|---|
| `basic` | `spectrum:place_pedestal` | C, M, Y |
| `simple` | `spectrum:build_basic_pedestal_structure` | C, M, Y |
| `advanced` | `spectrum:midgame/build_advanced_pedestal_structure` | C, M, Y, K (onyx) |
| **`complex`** (Moonstone) | **`spectrum:lategame/build_complex_pedestal_structure`** | tutti (C, M, Y, K, W) |

  Esiste anche `spectrum:lategame/build_complex_pedestal_structure_without_moonstone` (usato solo per scegliere quale struttura mostrare, `PedestalRecipeTier.java:79`).
- Conteggio tier nelle ricette pedestal del jar: basic 202, simple 38, advanced 140, **complex 57** (es. `…/recipe/pedestal/tier4/resonant_pickaxe.json`).
- **Guidebook**: le pagine-ricetta di Spectrum si nascondono da sole se il giocatore non può ancora craftare la ricetta (§3.4).

### 1.6 Strumento di verifica ✅

`/spectrum sanity [mod_id]` (permesso op 2) — `src:commands/SanityCommand.java:67-77`, registrato sotto il nodo `spectrum` in `src:registries/SpectrumCommands.java:11-13`. Accetta il mod id dell'addon e controlla:
- advancement inesistenti referenziati da ricette e da criteri `advancement_gotten`/`advancement_count`;
- gating: advancement visibili (fuori da `hidden`/`progression`/`milestones`) con `parent` ma senza criterio **`gotten_previous`** di tipo `advancement_gotten` coerente col parent (righe ~360-385);
- **cartella del tier** delle ricette pedestal: il percorso dell'id deve contenere `/tier1/` (basic), `/tier2/` (simple), `/tier3/` (advanced), **`/tier4/` (complex)** (righe 407-414) → le nostre ricette Moonstone vanno in `recipe/pedestal/tier4/…`;
- polveri onyx/moonstone usate in tier dove il giocatore non le ha ancora (righe 163-177; moonstone solo in COMPLEX);
- traduzioni mancanti, item senza ricetta, cloak Revelationary con advancement mancanti.
- **Non** rileva riferimenti circolari (lo dice anche la wiki).

→ Useremo `/spectrum sanity spectral_ascension` 🔶 (il nostro mod id è un'ipotesi).

---

## 2. Advancement chiave del late game (id verificati nel jar) ✅

Tutti i file sono in `jar(spectrum):data/spectrum/advancement/`.

| Cosa | Id esatto | Tipo | Note (criteri verificati) |
|---|---|---|---|
| Inizio late game | `spectrum:lategame/spectrum_lategame` | visibile (radice) | ← `midgame/enter_dimension` (Deeper Down) |
| Trovata Moonstone | `spectrum:lategame/collect_moonstone` | visibile | `inventory_changed` `#spectrum:moonstone_crystals`. È il `required_advancement` della ricetta del Moonstone Pedestal |
| Moonstone Pedestal craftato | `spectrum:lategame/craft_moonstone_pedestal` | visibile | `crafted_with_pedestal` (item `spectrum:pedestal_moonstone`) **oppure** click su un pedestal moonstone esistente |
| **Tier Complex (struttura Moonstone)** | **`spectrum:lategame/build_complex_pedestal_structure`** | visibile, challenge | `completed_multiblock` `spectrum:pedestal_complex`. **Gate del tier `complex`** e della rivelazione della malachite |
| Hidden "costruito qualsiasi complex" | `spectrum:hidden/build_any_complex_structure` | nascosto | usato come `turnin` della voce moonstone |
| Fusion Shrine (sblocco) | `spectrum:unlocks/blocks/fusion_shrine` | gate | ← `collect_all_basic_pigments_besides_brown` + `craft_cmy_pedestal` |
| Fusion Shrine costruito | `spectrum:build_fusion_shrine` | visibile | `completed_multiblock` `spectrum:fusion_shrine` |
| Enchanter (sblocco) | `spectrum:unlocks/blocks/enchanter` | gate | ← `midgame/build_advanced_pedestal_structure` + `midgame/enter_liquid_crystal` + `midgame/collect_stratine` |
| Enchanter costruito | `spectrum:midgame/build_enchanting_structure` | visibile | `completed_multiblock` `spectrum:enchanter` |
| Overenchanting | `spectrum:milestones/unlock_overenchanting_with_enchanter` | lock | ⚠️ dipende da `spectrum:__wip` (inesistente) → irraggiungibile in 1.12.9 |
| Bedrock tools/armor | `spectrum:unlocks/equipment/bedrock_tools` | gate | ← `midgame/break_decayed_bedrock` + `hidden/collect_stratine_gem`. Ricette: `recipe/fusion_shrine/bedrock/*.json` (Midnight Solution) |
| Set bedrock completo | `spectrum:midgame/wear_complete_set_of_bedrock_armor` | visibile | — |
| Malachite raccolta | `spectrum:lategame/collect_malachite` | visibile | parent = `build_complex_pedestal_structure` |
| Malachite rivelata | `spectrum:milestones/reveal_malachite` | lock Revelationary | ← `build_complex_pedestal_structure` |
| Malachite tools (sblocco) | `spectrum:unlocks/malachite/malachite_tools` | gate | ← `lategame/collect_malachite` + `unlocks/blocks/crystallarieum` + `lategame/collect_paltaeria` |
| Malachite tool creato | `spectrum:lategame/create_malachite_tool` | visibile | `crafted_with_fusion_shrine` |
| Moonstone Core | `spectrum:lategame/collect_moonstone_core` | visibile | ← `lategame/find_forgotten_city` |
| Glass Crest (sblocco) | `spectrum:unlocks/malachite/glass_crest_tools` | gate | ← `malachite_tools` + `collect_moonstone_core` + `hidden/collect_pigment/white` + `milestones/unlock_ink_use` |
| Glass Crest creato | `spectrum:lategame/create_glass_crest_tool` | visibile | `crafted_with_fusion_shrine` items `#spectrum:glass_crest_equipment` |
| Resonant pickaxe | `spectrum:unlocks/equipment/resonant_pickaxe` | gate | ricetta tier `complex` |
| Endgame | `spectrum:endgame/spectrum_endgame` | visibile (radice) | ← `build_complex_pedestal_structure` + `collect_enough_hints` + `collect_mysterious_locket` |

Nota: in 1.21 gli "strumenti" malachite/glass crest sono workstaff, ultra greatsword, crossbow, bident (`recipe/fusion_shrine/malachite/`), non un set piccone/ascia classico.

**Raccomandazione** 🔶: come prerequisito minimo di **tutti** i nostri tier usare `spectrum:lategame/build_complex_pedestal_structure` (= tier Moonstone garantito, ed è anche il gate usato da Spectrum per malachite). Per i tier più alti aggiungere progressivamente `spectrum:unlocks/malachite/malachite_tools` e `spectrum:unlocks/malachite/glass_crest_tools` (o `spectrum:lategame/create_glass_crest_tool`), così il nostro equipaggiamento arriva *dopo* quello di punta di Spectrum. Per le ricette Fusion Shrine il Fusion Shrine è implicito (è early/mid game), ma si può aggiungere `spectrum:build_fusion_shrine` per chiarezza.

Id oggetti ATM utili (✅ `jar(allthemodium):assets/allthemodium/lang/en_us.json`, tag in `jar(allthemodium):data/c/tags/item/ingots/`): `allthemodium:allthemodium_ingot`, `allthemodium:vibranium_ingot`, `allthemodium:unobtainium_ingot`, `allthemodium:vibranium_allthemodium_alloy_ingot`, `allthemodium:unobtainium_allthemodium_alloy_ingot`, `allthemodium:unobtainium_vibranium_alloy_ingot`; tag `#c:ingots/allthemodium`, `#c:ingots/vibranium`, `#c:ingots/unobtainium`, `#c:ingots/vibranium_allthemodium_alloy`, `#c:ingots/unobtainium_allthemodium_alloy`, `#c:ingots/unobtainium_vibranium_alloy`.

---

## 3. Il guidebook di Spectrum

### 3.1 Libreria e file ✅

- Libreria: **Modonomicon** (jar 1.123.0 per NeoForge 1.21.1). Spectrum registra pagine/condizioni proprie in `src:compat/modonomicon/ModonomiconCompat.java:66-111`.
- Book id: **`spectrum:guidebook`**. Percorso: `jar(spectrum):data/spectrum/modonomicon/books/guidebook/`
  - `book.json` — impostazioni del libro (item `spectrum:guidebook`, texture, `"auto_add_read_conditions": false`, `"allow_open_book_with_invalid_links": true`…)
  - `categories/*.json` — 13 categorie: `about, brewing, creating_life, cuisine, decoration, dimension, enchanting, equipment, general, ink, magical_blocks, mod_integration, pastel_network`
  - `entries/<categoria>/…/*.json` — 426 voci
  - (Spectrum ha anche `data/spectrum/modonomicon/multiblocks/`)
- **Regola di Modonomicon** (modo-docs `basics/structure/structure.md` + `entries.md`): i file stanno in `data/<namespace del libro>/modonomicon/books/<book_id>/…`; l'**id della voce** deriva dal percorso (es. `entries/general/intro.json` → `spectrum:general/intro`), ma la **categoria** è decisa dal campo `"category"`, non dalla cartella.

### 3.2 Formato di una categoria ✅

`jar(spectrum):…/guidebook/categories/general.json`:
```json
{
  "name": "book.spectrum.guidebook.category.general.name",
  "description": "book.spectrum.guidebook.category.general.description",
  "icon": { "item": "spectrum:pedestal_basic_amethyst" },
  "sort_number": 0,
  "entry_to_open": "spectrum:general/intro",
  "open_entry_to_open_only_once": true,
  "show_category_button": true,
  "max_scroll_x": 512, "max_scroll_y": 512,
  "display_mode": "node",
  "entry_textures": "spectrum:textures/gui/modonomicon/entry_textures.png",
  "background": "spectrum:textures/gui/modonomicon/category_background.png",
  "background_width": 1024, "background_height": 1024,
  "background_texture_zoom_multiplier": 1.0,
  "background_parallax_layers": []
}
```
Le altre categorie (es. `equipment.json`) hanno `"condition": {"type": "modonomicon:category_has_visible_entries", "category_id": "spectrum:equipment"}`: compaiono solo se contengono almeno una voce visibile.

### 3.3 Formato di una voce (entry) ✅

Campi principali (modo-docs `versioned_docs/version-1.21.1/basics/structure/entries.md` + JSON reali):
- `name` (chiave lang, in **grassetto** nel tooltip quando passi il mouse sul nodo), **`description`** (chiave lang, mostrata **sotto il nome nello stesso tooltip**; niente markdown) → è qui che va l'"Added by …" (§4).
- `icon` (id item, oggetto `{"item": …, "components": …}` o texture `ns:percorso.png` 16×16), `category`, `x`, `y` (posizione nella mappa), `parents` (frecce verso voci precedenti; puramente visive), `background_u_index`/`background_v_index` (cornice del nodo), `hide_while_locked`, `condition`, `pages`, `read_by_default`, `category_to_open`, `entry_to_open`.
- Spectrum usa anche `"turnin"` (es. `pedestal_upgrade_moonstone.json`). 🔶 Non trovato nel codice di Modonomicon/Spectrum come chiave: probabilmente un residuo dell'epoca Patchouli, ignorato. Non usarlo.

Esempio reale con pagine Fusion Shrine (`jar(spectrum):…/entries/equipment/tools_and_armor/bedrock_tools.json`, abbreviato):
```json
{
  "name": "book.spectrum.guidebook.bedrock_tools.name",
  "condition": { "type": "modonomicon:advancement", "advancement_id": "spectrum:unlocks/equipment/bedrock_tools" },
  "icon": { "item": "spectrum:bedrock_pickaxe" },
  "category": "spectrum:equipment",
  "hide_while_locked": true,
  "parents": [],
  "background_u_index": 0, "background_v_index": 0,
  "x": 1, "y": 1,
  "pages": [
    { "type": "modonomicon:text", "title": "book.spectrum.guidebook.bedrock_tools.name", "text": "book.spectrum.guidebook.bedrock_tools.page0.text" },
    { "type": "spectrum:fusion_shrine_crafting", "text": "book.spectrum.guidebook.bedrock_tools.page1.text", "recipe_id": "spectrum:fusion_shrine/bedrock/pickaxe" }
  ]
}
```
Esempio reale con pagina Pedestal + multiblocco (`…/entries/general/pedestal_upgrade_moonstone.json`, abbreviato):
```json
{
  "name": "book.spectrum.guidebook.pedestal_upgrade_moonstone.name",
  "icon": { "item": "spectrum:pedestal_moonstone" },
  "condition": { "type": "modonomicon:advancement", "advancement_id": "spectrum:lategame/collect_moonstone" },
  "category": "spectrum:general",
  "hide_while_locked": true,
  "parents": [ { "entry": "spectrum:general/moonstone_shards" } ],
  "x": 9, "y": 9,
  "pages": [
    { "type": "spectrum:pedestal_crafting", "title": "book.spectrum.guidebook.pedestal_upgrade_moonstone.name",
      "recipe_id": "spectrum:pedestal/tier3/pedestal_moonstone",
      "text": "book.spectrum.guidebook.pedestal_upgrade_moonstone.page0.text" },
    { "type": "modonomicon:multiblock", "multiblock_name": "multiblock.spectrum.pedestal_complex",
      "multiblock_id": "spectrum:pedestal_complex_without_moonstone", "show_visualize_button": true,
      "text": "book.spectrum.guidebook.pedestal_upgrade_moonstone.dimensions" }
  ]
}
```

Le voci del late game Spectrum per l'equipaggiamento stanno così: `bedrock_tools`/`bedrock_armor` in **`spectrum:equipment`** (x 0–1, y 1); `malachite_tools` (x −1, y 7) e `glass_crest_tools` (x −7, y 7) in **`spectrum:dimension`**.

### 3.4 Tipi di pagina e condizioni (conteggi sul jar) ✅

Tipi usati nelle voci di Spectrum: `modonomicon:text` (406), **`spectrum:pedestal_crafting`** (376), `modonomicon:spotlight` (306), `spectrum:enchanter_crafting` (76), **`spectrum:fusion_shrine_crafting`** (73), `spectrum:titration_barrel_fermenting`, `spectrum:potion_workshop_brewing`, `spectrum:hint`, `modonomicon:crafting_recipe`, **`spectrum:collection`**, `modonomicon:image`, `spectrum:spirit_instiller_crafting`, `spectrum:anvil_crushing`, `spectrum:snippet`, `spectrum:status_effect`, `spectrum:crystallarieum_growing`, `modonomicon:multiblock`, `spectrum:enchanter_upgrading`, `modonomicon:entity`, `spectrum:checklist`, `spectrum:web_link` (tipo *entry*), `spectrum:confirmation_button`, …

Condizioni: `modonomicon:advancement` (828), `modonomicon:and` (38), `modonomicon:mod_loaded` (36), `modonomicon:category_has_visible_entries` (10). Disponibili anche `or`, `not`, `true`, `false`, `entry_read`, `entry_unlocked` (modo-docs `unlock-conditions/`). Spectrum aggiunge `spectrum:enchantment_registered` e `spectrum:recipe_loaded_and_unlocked` (`ModonomiconCompat.java:62-63,110-111`). Le condizioni valgono su categoria, voce **e singola pagina** (campo `condition` dentro la pagina; esempio in `entries/mod_integration/ae2.json`).

**Pagine-ricetta Spectrum ("gated")** — `src:compat/modonomicon/pages/BookGatedRecipePage.java:42-59`:
- campi: `recipe_id` (obbligatorio), `title`, `text`, `anchor`, `condition`, `skip_recipe_unlock_check` (default `false`); `recipe_id2`/`title2` solo per spirit instiller, titration barrel, ink converting.
- Se `skip_recipe_unlock_check` è false, alla condizione della pagina viene aggiunta in AND una `RecipesLoadedAndUnlockedCondition`: la pagina è **visibile solo se la ricetta esiste e `canPlayerCraft(player)`** (tier pedestal + `required_advancement`). Quindi se la ricetta non è caricata (es. ATM assente) la pagina scompare da sola invece di rompersi.

`spectrum:collection` legge `title`, `text`, `items` (lista `{id, components}`), `use_markdown_title`, `show_title_separator`, `anchor`, `condition` (`src:…/pages/BookCollectionPage.java:30-38`). Spectral Decorations ci mette anche `"advancement"`, che **non viene letto** dal codice 🔶 (ignorato): per noi usare `condition`.

### 3.5 Dove stanno i testi ✅

- I JSON contengono solo **chiavi lang**; i testi sono in `assets/<modid>/lang/<lingua>.json`. Spectrum: `jar(spectrum):assets/spectrum/lang/` → `de_de, en_gb, en_us, ko_kr, pt_br, ru_ru, zh_cn` (1946 chiavi `book.spectrum.guidebook…` in `en_us`). **Non c'è `it_it`.**
- Convenzione chiavi: `book.spectrum.guidebook.<voce>.name`, `.<voce>.pageN.text`, `.pageN.title`.
- I testi delle pagine supportano markdown Modonomicon (`**grassetto**`, `*corsivo*`, a capo con `\\\n` nel JSON come in Spectral Decorations); `name`/`description` delle voci **non** parsano markdown ma accettano i codici `§` (Spectral Decorations usa `§o` = corsivo).

---

## 4. Come un addon aggiunge voci al libro di un'altra mod

### 4.1 Meccanismo: file nel namespace del libro ✅

Nessuna "book extension" né codice: l'addon mette i suoi JSON **nel namespace `spectrum`** dentro il proprio jar, nello stesso percorso del libro. Il resource-loading di Minecraft unisce le cartelle `data/spectrum/…` di tutte le mod, quindi Modonomicon le vede come parte di `spectrum:guidebook`.

Spectral Decorations (`jar(SpectralDecorations):data/spectrum/modonomicon/books/guidebook/entries/`):
```
general/spectral_decorations.json            ← voce di presentazione (categoria General)
equipment/colored_bedrock_armor.json
equipment/colored_bottomless_bundles.json
decoration/{colored_amphoras, colored_lanterns, colored_lights, colored_wood_variants,
            kindling_plushie, metal_decorations, vanilla_wood_amphoras, vanilla_wood_beams}.json
creating_life/colored_resplendent_deco.json
```
- Nessuna categoria nuova, nessun `book.json`: usa le categorie esistenti tramite `"category": "spectrum:general"` ecc. e si aggancia alle voci di Spectrum con `"parents": [{"entry": "spectrum:equipment/tools_and_armor/bedrock_armor"}]`.
- **Rischio**: un file con lo stesso percorso di uno di Spectrum lo **sovrascriverebbe**. Per questo i nomi file devono essere unici (prefisso con il nome dell'addon). Spectral Decorations dichiara nel `neoforge.mods.toml` la dipendenza `spectrum` con `ordering = "AFTER"` e `reason = "load after Spectrum to override Spectrum's resource files"` (perché sovrascrive alcuni modelli/texture in `assets/spectrum/`).
- Le ricette mostrate nelle pagine possono stare nel namespace dell'addon (`"recipe_id": "spectral_decorations:pedestal/tier1/bedrock_armor_coloring"`).
- **spectrumcompat 0.1.0**: non aggiunge nulla al guidebook (nessun file `modonomicon`); sovrascrive solo 3 ricette `data/spectrum/recipe/enchanter/*.json` e aggiunge le sue in `data/spectrumcompat/`. Non rilevante per il libro.
- Alternativa via codice: Modonomicon ha una **Runtime Extending API** (`ModonomiconAPI.get().openRuntimeContentBatch()`, modo-docs `docs/advanced/runtime-extending.md`). 🔶 La doc è sul branch principale (26.1.x): **non verificato** che esista in 1.123.0; non serve, i JSON bastano.
- Le doc 1.21.1 descrivono anche le "file-based pages" (`…/books/<id>/pages/<categoria>/<voce>/*.json`) per aggiungere pagine a una voce esistente senza copiarla. 🔶 Non verificato che 1.123.0 le supporti; utile solo se un giorno volessimo aggiungere pagine a voci di Spectrum (sconsigliato: meglio voci nostre).

### 4.2 Il tooltip "Added by …" ✅

**Non è una funzione di Spectrum né un campo speciale di Modonomicon, e non c'è codice Java**: è semplicemente il campo standard **`description`** della voce, che Modonomicon mostra sotto il nome quando passi il mouse sul nodo. Spectral Decorations lo valorizza con una chiave lang comune a tutte le sue voci:

`jar(SpectralDecorations):data/spectrum/modonomicon/books/guidebook/entries/equipment/colored_bedrock_armor.json`:
```json
{
  "name": "book.spectral_decorations.guidebook.colored_bedrock_armor.name",
  "description": "book.spectral_decorations.added_by_spectral_decorations",
  "condition": { "type": "modonomicon:advancement", "advancement_id": "spectrum:unlocks/equipment/bedrock_tools" },
  "icon": { "item": "spectrum:bedrock_chestplate", "components": { "minecraft:base_color": "blue" } },
  "category": "spectrum:equipment",
  "hide_while_locked": true,
  "parents": [ { "entry": "spectrum:equipment/tools_and_armor/bedrock_armor" } ],
  "background_u_index": 0, "background_v_index": 0,
  "x": -2, "y": 1,
  "pages": [
    { "type": "spectrum:collection", "title": "…", "items": [ … ], "text": "…page0.text" },
    { "type": "spectrum:pedestal_crafting", "title": "container.spectrum.rei.pedestal_recipe",
      "recipe_id": "spectral_decorations:pedestal/tier1/bedrock_armor_coloring",
      "text": "book.spectral_decorations.guidebook.colored_bedrock_armor.page1.text" }
  ]
}
```
`jar(SpectralDecorations):assets/spectral_decorations/lang/en_us.json:206`:
```json
"book.spectral_decorations.added_by_spectral_decorations": "§oAdded by Spectral Decorations",
```
Tutte le 11 voci "di contenuto" di Spectral Decorations usano questa stessa `description` (verificato con grep). Le voci di Spectrum invece in genere **non** hanno `description` (es. `bedrock_tools.json`), quindi il tooltip "Added by" distingue a colpo d'occhio le voci dell'addon. Grep su `src:` per `added_by`/`addon`: nessuna occorrenza rilevante → conferma che non c'è supporto dedicato nel codice.

### 4.3 La voce nella categoria General ✅

`jar(SpectralDecorations):…/entries/general/spectral_decorations.json` (completo):
```json
{
  "name": "book.spectral_decorations.guidebook.spectral_decorations.name",
  "description": "book.spectral_decorations.guidebook.spectral_decorations.description",
  "icon": "spectral_decorations:icon.png",
  "category": "spectrum:general",
  "hide_while_locked": false,
  "background_u_index": 0,
  "background_v_index": 4,
  "x": -13,
  "y": -3,
  "read_by_default": true,
  "pages": [
    { "type": "modonomicon:text",
      "title": "book.spectral_decorations.guidebook.spectral_decorations.about.title",
      "text": "book.spectral_decorations.guidebook.spectral_decorations.about.text" }
  ]
}
```
Lang (`en_us.json:208-211`): name = `"Addon: Spectral Decorations"`, description = tagline della mod, testo con crediti.
- Niente `condition` → sempre visibile; `read_by_default: true` → non appare come "non letta"; `background_v_index: 4` = la stessa cornice della voce "About" di Spectrum (`entries/general/about.json`, x −12, y −4, che apre la categoria `spectrum:about`).
- L'icona è una texture: `spectral_decorations:icon.png` → `assets/spectral_decorations/icon.png` (esiste nel jar, è il logo della mod).
- Posizioni occupate vicino all'angolo in alto a sinistra di General: `about` (−12,−4), `intro` (−12,−3), Spectral Decorations (−13,−3), `topaz_hint` (−12,−1), `amethyst_hint` (−12,0), `citrine_hint` (−12,1), `geodes` (−11,0), `gemstone_ores` (−10,−1). → Per noi **(−13,−4)** sembra libera 🔶 (da verificare in gioco con Spectral Decorations installato, come in ATM10).

---

## 5. Implicazioni per Spectral Ascension

> Tutto ciò che segue è **BOZZA / dedotto** 🔶. Ipotesi: mod id `spectral_ascension`; ricette in `data/spectral_ascension/recipe/…`.

### 5.1 Advancement

Seguire lo schema di Spectrum: **un gate nascosto per tier** (`unlocks/…`, usato da ricette *e* voci del libro) + **advancement visibili** (sotto l'albero late game di Spectrum, come fa Spectral Decorations parentando a un advancement di Spectrum) per dare l'indizio al giocatore.

Catena proposta:

| Tier | Gate nascosto (ricette + libro) | Criteri (AND) |
|---|---|---|
| T6 Allthemodium | `spectral_ascension:unlocks/allthemodium_equipment` | `advancement_gotten` → `spectrum:lategame/build_complex_pedestal_structure` + `inventory_changed` `#c:ingots/allthemodium` |
| T7 Vibranium | `spectral_ascension:unlocks/vibranium_equipment` | gate T6 + `spectrum:unlocks/malachite/malachite_tools` + `#c:ingots/vibranium` |
| T8 Unobtainium | `spectral_ascension:unlocks/unobtainium_equipment` | gate T7 + `spectrum:unlocks/malachite/glass_crest_tools` + `#c:ingots/unobtainium` |
| Alloy | `spectral_ascension:unlocks/alloy_equipment` (nome adottato nel codice, 2026-11-18) | gate T8 + uno qualsiasi dei tre alloy ingot (`requirements` con OR) |

Visibili (con `display`): es. `spectral_ascension:craft_allthemodium_equipment` (parent `spectrum:lategame/build_complex_pedestal_structure`), poi `craft_vibranium_equipment` (parent il precedente), ecc., ognuno con `gotten_previous` per passare `/spectrum sanity spectral_ascension`.

`data/spectral_ascension/advancement/unlocks/allthemodium_equipment.json` (BOZZA):
```json
{
  "criteria": {
    "built_complex_pedestal": {
      "trigger": "revelationary:advancement_gotten",
      "conditions": { "advancement_identifier": "spectrum:lategame/build_complex_pedestal_structure" }
    },
    "collected_allthemodium_ingot": {
      "trigger": "minecraft:inventory_changed",
      "conditions": { "items": [ { "items": "#c:ingots/allthemodium" } ] }
    }
  }
}
```

`data/spectral_ascension/advancement/unlocks/alloy_equipment.json` (BOZZA, esempio di OR):
```json
{
  "criteria": {
    "unlocked_unobtainium": {
      "trigger": "revelationary:advancement_gotten",
      "conditions": { "advancement_identifier": "spectral_ascension:unlocks/unobtainium_equipment" }
    },
    "alloy_va": { "trigger": "minecraft:inventory_changed", "conditions": { "items": [ { "items": "#c:ingots/vibranium_allthemodium_alloy" } ] } },
    "alloy_ua": { "trigger": "minecraft:inventory_changed", "conditions": { "items": [ { "items": "#c:ingots/unobtainium_allthemodium_alloy" } ] } },
    "alloy_uv": { "trigger": "minecraft:inventory_changed", "conditions": { "items": [ { "items": "#c:ingots/unobtainium_vibranium_alloy" } ] } }
  },
  "requirements": [
    [ "unlocked_unobtainium" ],
    [ "alloy_va", "alloy_ua", "alloy_uv" ]
  ]
}
```

`data/spectral_ascension/advancement/craft_allthemodium_equipment.json` (BOZZA, visibile):
```json
{
  "parent": "spectrum:lategame/build_complex_pedestal_structure",
  "display": {
    "icon": { "id": "spectral_ascension:allthemodium_pickaxe" },
    "title": { "translate": "advancements.spectral_ascension.craft_allthemodium_equipment.title" },
    "description": { "translate": "advancements.spectral_ascension.craft_allthemodium_equipment.description" }
  },
  "criteria": {
    "crafted": {
      "trigger": "spectrum:crafted_with_pedestal",
      "conditions": { "item": { "items": "#spectral_ascension:allthemodium_equipment" } }
    },
    "gotten_previous": {
      "trigger": "revelationary:advancement_gotten",
      "conditions": { "advancement_identifier": "spectral_ascension:unlocks/allthemodium_equipment" }
    }
  }
}
```
Note:
- 🔶 `gotten_previous` qui punta al gate e non al `parent`: il sanity check vuole un `gotten_previous` di tipo `advancement_gotten` e poi lo confronta col parent (righe ~370-385, non lette fino in fondo) → potrebbe emettere un warning; in alternativa puntare `gotten_previous` al parent e mettere il gate come terzo criterio.
- 🔶 Verificare la chiave `item` vs `items` del trigger `crafted_with_pedestal` (§1.3). Per il Fusion Shrine usare `spectrum:crafted_with_fusion_shrine` con `items` (lista), come in `create_glass_crest_tool.json`.
- Le ricette: `"tier": "complex"` + `"required_advancement": "spectral_ascension:unlocks/<tier>_equipment"` (pedestal), con file in `data/spectral_ascension/recipe/pedestal/tier4/…` (il sanity check vuole `/tier4/` nel percorso per il tier COMPLEX); per fusion shrine solo `required_advancement`.
- Non usare `revelationary` per nascondere i lingotti ATM (non sono nostri): niente cloak.
- Retroattività: i gate si riallineano quando il giocatore apre il guidebook (§1.4). I criteri `inventory_changed` invece richiedono che il lingotto *entri* nell'inventario dopo l'installazione: in un mondo esistente basta muoverlo una volta.

### 5.2 Voci del guidebook

File nel **nostro jar**, namespace `spectrum`:
```
data/spectrum/modonomicon/books/guidebook/entries/
  general/spectral_ascension.json                       ← presentazione (richiesta 2 di DaFuqs)
  equipment/spectral_ascension_allthemodium.json        ← una voce per tier
  equipment/spectral_ascension_vibranium.json
  equipment/spectral_ascension_unobtainium.json
  equipment/spectral_ascension_alloy.json
assets/spectral_ascension/lang/en_us.json  (+ it_it.json)
```
- Nomi file con prefisso `spectral_ascension_` per non sovrascrivere mai voci di Spectrum o di altri addon.
- Categoria consigliata: `spectrum:equipment` (dove stanno bedrock tools/armor). Alternativa: `spectrum:dimension`, vicino a `malachite_tools`/`glass_crest_tools`. Una categoria nostra è possibile (file in `categories/`) ma più invasiva: meglio evitarla salvo chiedere a DaFuqs.
- **Ogni** voce di contenuto ha `"description": "book.spectral_ascension.added_by_spectral_ascension"` (richiesta 1 di DaFuqs, identico a Spectral Decorations).
- `condition` della voce = stesso gate delle ricette (principio della wiki: ricetta e voce allineate) e `hide_while_locked: true`.
- Le pagine `spectrum:pedestal_crafting` / `spectrum:fusion_shrine_crafting` si nascondono da sole finché la ricetta non è craftabile.
- Coordinate 🔶 da scegliere in gioco: in `equipment` Spectrum occupa circa x 0–15, y −3…9; Spectral Decorations usa (−2,1). Proposta: colonna x −4…−3, y 0…3, con `parents` verso `spectrum:equipment/tools_and_armor/bedrock_tools` e poi a catena tra i nostri tier.

`general/spectral_ascension.json` (BOZZA):
```json
{
  "name": "book.spectral_ascension.guidebook.spectral_ascension.name",
  "description": "book.spectral_ascension.guidebook.spectral_ascension.description",
  "icon": "spectral_ascension:icon.png",
  "category": "spectrum:general",
  "hide_while_locked": false,
  "background_u_index": 0,
  "background_v_index": 4,
  "x": -13,
  "y": -4,
  "read_by_default": true,
  "pages": [
    { "type": "modonomicon:text",
      "title": "book.spectral_ascension.guidebook.spectral_ascension.about.title",
      "text": "book.spectral_ascension.guidebook.spectral_ascension.about.text" }
  ]
}
```
(richiede la texture `assets/spectral_ascension/icon.png`, 16×16 se usata come icona — 🔶 Spectral Decorations usa il logo della mod, verificare come viene scalato; in alternativa `"icon": {"item": "spectral_ascension:allthemodium_pickaxe"}`.)

`equipment/spectral_ascension_allthemodium.json` (BOZZA):
```json
{
  "name": "book.spectral_ascension.guidebook.allthemodium.name",
  "description": "book.spectral_ascension.added_by_spectral_ascension",
  "condition": {
    "type": "modonomicon:advancement",
    "advancement_id": "spectral_ascension:unlocks/allthemodium_equipment"
  },
  "icon": { "item": "spectral_ascension:allthemodium_pickaxe" },
  "category": "spectrum:equipment",
  "hide_while_locked": true,
  "parents": [ { "entry": "spectrum:equipment/tools_and_armor/bedrock_tools" } ],
  "background_u_index": 0,
  "background_v_index": 0,
  "x": -3,
  "y": 1,
  "pages": [
    { "type": "modonomicon:text",
      "title": "book.spectral_ascension.guidebook.allthemodium.name",
      "text": "book.spectral_ascension.guidebook.allthemodium.page0.text" },
    { "type": "spectrum:pedestal_crafting",
      "title": "container.spectrum.rei.pedestal_recipe",
      "recipe_id": "spectral_ascension:pedestal/tier4/allthemodium_pickaxe",
      "text": "book.spectral_ascension.guidebook.allthemodium.pickaxe.text" },
    { "type": "spectrum:fusion_shrine_crafting",
      "recipe_id": "spectral_ascension:fusion_shrine/allthemodium/chestplate",
      "text": "book.spectral_ascension.guidebook.allthemodium.chestplate.text" }
  ]
}
```

`assets/spectral_ascension/lang/en_us.json` (BOZZA, estratto):
```json
{
  "book.spectral_ascension.added_by_spectral_ascension": "§oAdded by Spectral Ascension",
  "book.spectral_ascension.guidebook.spectral_ascension.name": "Addon: Spectral Ascension",
  "book.spectral_ascension.guidebook.spectral_ascension.description": "Spectrum's magic, forged into All the Mods metals",
  "book.spectral_ascension.guidebook.spectral_ascension.about.title": "Spectral Ascension",
  "book.spectral_ascension.guidebook.spectral_ascension.about.text": "Adds four tiers of equipment …\\\n\\\nMade with permission of DaFuqs.",
  "book.spectral_ascension.guidebook.allthemodium.name": "Allthemodium Equipment",
  "book.spectral_ascension.guidebook.allthemodium.page0.text": "…",
  "advancements.spectral_ascension.craft_allthemodium_equipment.title": "…",
  "advancements.spectral_ascension.craft_allthemodium_equipment.description": "…"
}
```
Spectrum non ha `it_it.json`: se aggiungiamo `it_it` nel nostro jar, le nostre voci saranno in italiano e quelle di Spectrum resteranno in inglese (fallback). Il campo `container.spectrum.rei.pedestal_recipe` è una chiave di Spectrum riutilizzata da Spectral Decorations come titolo pagina.

### 5.3 Dipendenze e caricamento

- `neoforge.mods.toml`: dipendenze `required` su `spectrum` (≥ 1.12.9), `modonomicon`, `revelationary`, `allthemodium`; `ordering = "AFTER"` su spectrum (come Spectral Decorations) — necessario solo se sovrascriviamo file `assets/spectrum/…`, ma innocuo.
- Se volessimo che l'addon funzioni anche senza Allthemodium, le voci possono usare `modonomicon:and` + `modonomicon:mod_loaded` (`mod_id: allthemodium`) come le voci di `mod_integration` di Spectrum. Per ATM10 non serve.

### 5.4 Checklist di test (in gioco)

1. `/spectrum sanity spectral_ascension` senza warning.
2. Nuovo mondo: le voci sono invisibili; con `/advancement grant @s only spectrum:lategame/build_complex_pedestal_structure` + lingotto in inventario → appare la voce T6, le pagine-ricetta compaiono.
3. Passando il mouse sul nodo compare "*Added by Spectral Ascension*" sotto il nome.
4. Voce in General visibile da subito, non segnata come non letta, non sovrapposta a quella di Spectral Decorations.
5. Mondo esistente: aprire il guidebook riallinea i gate (§1.4).

---

## Incertezze aperte

- 🔶 Chiave del trigger `spectrum:crafted_with_pedestal`: nel jar `item` (singolare), nella wiki `items` (lista). Controllare il codec prima di scriverlo.
- 🔶 `"turnin"` e `"advancement"` (nelle pagine `collection`) presenti nei JSON ma non letti dal codice consultato → ignorati.
- 🔶 Runtime Extending API e file-based pages di Modonomicon: documentate, non verificate per la versione 1.123.0.
- 🔶 Coordinate libere nelle categorie: da verificare in gioco con tutto ATM10 caricato (altri addon potrebbero usare le stesse caselle).
- 🔶 Il sorgente Spectrum consultato è un po' più recente del jar 1.12.9 (es. `data/spectrum/revelations/` presente solo nel sorgente); le righe `src:` citate possono differire leggermente dal codice compilato nel jar.
