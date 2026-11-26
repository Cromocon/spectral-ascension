# 12 — Materiali di Spectrum nella Mining Dimension di Allthemodium

> Ricerca per **Spectral Ascension** (addon NeoForge 1.21.1 per Spectrum 1.12.9 nel modpack ATM10).
> Obiettivo: far generare i minerali di Spectrum (gemme escluse: topaz, amethyst, citrine, onyx, moonstone) anche nella Mining Dimension di Allthemodium, senza rompere la progressione.
>
> Fonti, tutte lette in sola lettura (abbreviazioni usate sotto):
> - `jar(spectrum)` = `<scratchpad>/jars/spectrum-1.12.9-1.21.1-neo/` (dati)
> - `src(spectrum)` = `<scratchpad>/src/spectrum-1.12.9/src/main/java/de/dafuqs/spectrum/` (sorgente)
> - `jar(atm)` = `<scratchpad>/jars/allthemodium-3.2.1_mc_1.21.1/` (dati e bytecode)
> - `jar(rev)` = `<scratchpad>/jars/revelationary-neoforge-1.5.2+1.21.1/` (bytecode Revelationary)
> - `istanza:` = la cartella della tua istanza CurseForge di ATM10
> - `<scratchpad>` = cartella temporanea di lavoro (jar estratti, sorgenti clonati)
>
> Legenda: **[V]** verificato leggendo file o bytecode, **[D]** dedotto (ragionamento o conoscenza generale, da provare in gioco).
> Il checklist di progressione di riferimento e' [00-checklist-progressione.md](00-checklist-progressione.md).

---

## 0. Mini-glossario worldgen (per chi e' nuovo al modding)

- **Worldgen data-driven** — da Minecraft 1.18 il modo in cui il mondo viene popolato (minerali, alberi, laghi) e' descritto in file JSON dentro `data/<namespace>/worldgen/...`, non in codice Java. Un addon puo' quindi aggiungere minerali con soli file JSON.
- **configured_feature** — dice *cosa* piazzare e *come e' fatto*. Per un minerale e' una feature di tipo `minecraft:ore` con una `config`: `size` (quanti blocchi al massimo ha un "filone"), `discard_chance_on_air_exposure` (probabilita' di scartare un blocco del filone se tocca l'aria: serve a nascondere i minerali nelle caverne) e `targets`.
- **targets / rule test / target block** — lista di coppie `target` (una *rule test*: "quali blocchi posso sostituire?") + `state` (il blocco minerale da mettere al loro posto). Le rule test usate qui sono `minecraft:tag_match` (il blocco da sostituire e' in un tag, es. `minecraft:stone_ore_replaceables`) e `minecraft:block_match` (e' esattamente quel blocco, es. `minecraft:end_stone`). I minerali nascono quindi *dentro* la pietra: servono target diversi per pietra, deepslate, netherrack, end stone.
- **placed_feature** — dice *dove e quanto spesso* piazzare una configured_feature, tramite una lista di *placement modifier* applicati in ordine:
  - `minecraft:count` = quanti tentativi per chunk (un chunk e' un'area 16x16 colonne);
  - `minecraft:rarity_filter` con `chance: N` = solo 1 chunk su N ci prova;
  - `minecraft:in_square` = sposta ogni tentativo in una posizione x,z casuale dentro il chunk;
  - `minecraft:height_range` = sceglie la quota y con un **height provider** (`uniform` = tutte le quote ugualmente probabili; `trapezoid` = piu' probabili al centro dell'intervallo; i bordi si danno come `absolute` = y fissa, `above_bottom`/`below_top` = relativo al fondo/cima della dimensione);
  - `minecraft:biome` = a fine lista, scarta il tentativo se il bioma di quel punto non contiene la feature.
- **Step di generazione** — la generazione e' divisa in fasi (`GenerationStep.Decoration`) eseguite in ordine. I minerali stanno in `underground_ores`.
- **Biome modifier (NeoForge)** — file in `data/<namespace>/neoforge/biome_modifier/*.json` che *aggiunge* (o toglie) feature a biomi *esistenti*, senza toccare il file del bioma. Il tipo `neoforge:add_features` ha `biomes` (un id di bioma, una lista, o un tag con `#`), `features` (uno o piu' placed_feature) e `step`.
- **Tag di bioma** — lista con nome di biomi (`data/<ns>/tags/worldgen/biome/...`), usata nei biome modifier per colpire molti biomi insieme (es. `#c:is_overworld`).
- **dimension / dimension_type** — `dimension` = quale generatore di terreno e quale sorgente di biomi usa una dimensione; `dimension_type` = proprieta' fisiche (altezza minima `min_y`, altezza totale `height`, cielo, luce...).
- **Revelationary / cloak** — libreria che nasconde un blocco finche' il giocatore non ottiene un advancement: vedi sezione 4.

---

## 1. Minerali di Spectrum che si generano nel mondo

Metodo: ho cercato in `jar(spectrum):data/spectrum/neoforge/biome_modifier/` (i soli punti dove Spectrum aggiunge feature a biomi vanilla) e in `data/spectrum/worldgen/biome/*.json` (le feature dei biomi della Deeper Down) **[V]**. Gli id dei blocchi vengono da `src(spectrum)/registries/SpectrumBlocks.java` righe ~1224-1260 **[V]**.

### 1.1 Tabella riassuntiva

| Materiale | Id blocco (pietra / deepslate / blackslag / altro) | Dove si genera oggi | Cloaked? | Advancement che lo rivela |
|---|---|---|---|---|
| **Shimmerstone** | `spectrum:shimmerstone_ore`, `spectrum:deepslate_shimmerstone_ore`, `spectrum:blackslag_shimmerstone_ore` | Overworld (`#c:is_overworld`), `underground_ores` | Si (`ShimmerstoneOreBlock`, estende il comportamento cloaked) | `spectrum:milestones/reveal_shimmerstone` <- `spectrum:craft_using_pedestal` (**early**) |
| **Azurite** | `spectrum:azurite_ore`, `spectrum:deepslate_azurite_ore`, `spectrum:blackslag_azurite_ore` | Overworld (`#c:is_overworld`), `underground_ores` | Si (`AzuriteOreBlock extends CloakedOreBlock`) | `spectrum:milestones/reveal_azurite` <- `spectrum:midgame/collect_neolith` |
| **Stratine** | `spectrum:stratine_ore` (solo variante netherrack) | Nether (`#c:is_nether`) | Si (`CloakedOreBlock`, cloak = `netherrack`) | `spectrum:milestones/reveal_stratine` <- `spectrum:midgame/build_advanced_pedestal_structure` |
| **Paltaeria** | `spectrum:paltaeria_ore` (solo variante end stone) | End (`#c:is_end`) | Si (`CloakedOreBlock`, cloak = `end_stone`) | `spectrum:milestones/reveal_paltaeria` <- `spectrum:lategame/build_complex_pedestal_structure` |
| **Malachite** | `spectrum:malachite_ore`, `spectrum:deepslate_malachite_ore`, `spectrum:blackslag_malachite_ore` | **Solo Deeper Down** (non c'e' biome modifier; la feature e' nei biomi `spectrum:*` della DD) | Si (`CloakedOreBlock`) | `spectrum:milestones/reveal_malachite` <- `spectrum:lategame/build_complex_pedestal_structure` |
| **Bismuth** (cristalli) | `spectrum:small_bismuth_bud`, `spectrum:large_bismuth_bud`, `spectrum:bismuth_cluster` (non sono "ore": sono germogli di cristallo) | **Solo Deeper Down** (feature `dd_bismuth`) | No (blocchi normali `SpectrumClusterBlock`) | nessuno (non cloaked) |

File letti per la tabella **[V]**:
- Shimmerstone: `jar(spectrum):data/spectrum/worldgen/configured_feature/shimmerstone_ore.json`, `placed_feature/shimmerstone_ore.json`, `neoforge/biome_modifier/shimmerstone_ore.json`, `advancement/milestones/reveal_shimmerstone.json`; in piu' esiste `placed_feature/dd_shimmerstone_ore.json` (stessa configured feature, usata nella Deeper Down).
- Azurite: stessi 3 file `azurite_ore.json` + `advancement/milestones/reveal_azurite.json` + `advancement/midgame/collect_azurite.json`.
- Stratine: `stratine_ore.json` x3 + `reveal_stratine.json`.
- Paltaeria: `paltaeria_ore.json` x3 + `reveal_paltaeria.json`.
- Malachite: `configured_feature/dd_malachite_ore.json`, `placed_feature/dd_malachite_ore.json`, `advancement/milestones/reveal_malachite.json`. La lista dei biomi DD e' in `data/spectrum/tags/worldgen/biome/in_deeper_down.json` (crystal_gardens, deep_barrens, dragonrot_swamp, black_langast, razor_edge, howling_spires).
- Bismuth: `configured_feature/dd_bismuth.json` (tipo `spectrum:random_buds`), `placed_feature/dd_bismuth.json`.
- Classi cloaked: `src(spectrum)/blocks/conditional/CloakedOreBlock.java`, `blocks/geology/AzuriteOreBlock.java`, `ShimmerstoneOreBlock.java` (quest'ultima nominata nel registry ma non letta riga per riga: [D] che sia analoga ad Azurite).

Nota sulla "blackslag": e' la pietra della Deeper Down (`spectrum:blackslag`, tag `spectrum:blackslag_ore_replaceables` in `jar(spectrum):data/spectrum/tags/block/blackslag_ore_replaceables.json` = solo `spectrum:blackslag`) **[V]**. Nella Mining Dimension non esiste, quindi quel target resta inerte.

### 1.2 Parametri di generazione attuali (dai placed/configured feature)

Altezze relative: l'Overworld ha `min_y` -64 e altezza 384 (-64..320) [D, vanilla]; il Nether 0..256 [D].

| Materiale | Configured (config) | Placed (placement) | Come leggerlo |
|---|---|---|---|
| Shimmerstone | `ore`, size 7, discard 0, target: `stone_ore_replaceables` -> pietra, `deepslate_ore_replaceables` -> deepslate, `blackslag_ore_replaceables` -> blackslag | count 9, in_square, `height_range` **uniform** da `above_bottom 48` a `absolute 128` (= y -16..128), biome | 9 filoni da 7 blocchi per chunk, tra y -16 e 128 [V]; 9 per chunk e' tanto: e' il minerale "comune" |
| Azurite | `ore`, size 24, discard 0.8, stessi 3 target | **rarity_filter 32**, count 1, in_square, `height_range` **trapezoid** `above_bottom 0`..`above_bottom 32` (= y -64..-32), biome | 1 chunk su 32 ha un filone enorme (24) sul fondo del mondo [V]: estremamente raro e concentrato |
| Stratine | `ore`, size 6, discard 0, target: `#minecraft:base_stone_nether` -> `stratine_ore` | count 12, in_square, trapezoid `above_bottom 4`..`below_top 64`, biome | 12 filoni da 6 per chunk nel Nether [V] |
| Paltaeria | **`scattered_ore`**, size 4, discard 0.3, target: `block_match minecraft:end_stone` -> `paltaeria_ore` | count 4, in_square, trapezoid `above_bottom 0`..`below_top 0` (= tutta la dimensione), biome | `scattered_ore` mette blocchi *sparsi* (non un filone compatto): ne escono singoli blocchi vicini [V per il tipo; D per l'effetto] |
| Malachite | `ore`, size 4, discard 0.75, 3 target come sopra | count 20, in_square, trapezoid `above_bottom 4`..`above_bottom 64`, biome | solo DD: 20 per chunk piccoli (4) e quasi sempre nascosti in aria [V] |
| Shimmerstone in DD | stessa configured `spectrum:shimmerstone_ore` | `dd_shimmerstone_ore`: count 24, trapezoid `below_top 256`..`below_top 8` | variante densa per la DD [V] |

**Altro: Dragon fossil (dragonbone)** — non e' un minerale ma un "materiale": `spectrum:dragon_fossil_overworld_buried` (rarity 1/512, `underground_decoration`, biomi `#spectrum:dragonbone_fossils_generating_in` = `#c:is_desert` e `#c:is_badlands`) **[V]** (`placed_feature/dragon_fossil_overworld_buried.json`, `neoforge/biome_modifier/dragon_fossil_overworld_buried.json`, `tags/worldgen/biome/dragonbone_fossils_generating_in.json`). Nella Mining Dimension non ci sono deserti/badlands (l'unico bioma e' `allthemodium:mining`) e la feature e' una struttura "fossile", non un filone: **non lo consiglio** (vedi 3.5).

**Non in worldgen**: bloodstone (esistono solo `raw_bloodstone_block`, `pure_bloodstone_block`, cluster/bud, ma nessun file in `jar(spectrum):data/spectrum/worldgen/` li cita: `grep -ril bloodstone worldgen` = nessun risultato) **[V]**. Frostbite/incandescent/storm stones/radiating ender: hanno solo advancement `reveal_*`, non feature di generazione [V per la lista dei reveal, D per il resto].

---

## 2. La Mining Dimension di ATM

### 2.1 Dimensione, tipo e biomi

- Id dimensione: **`allthemodium:mining`** (`jar(atm):data/allthemodium/dimension/mining.json`) **[V]**. Le altre due sono `allthemodium:the_other` e `allthemodium:the_beyond`.
- Generatore: **`allthemodium:flat`** (classe `com.thevortex.allthemodium.chunkgenerator.CustomFlatLevelSource`), cioe' un mondo "superflat" a strati, con `"features": true` (le feature dei biomi *vengono* eseguite), `"lakes": false`, `"structure_overrides": []` **[V]**.
- Un unico bioma: **`allthemodium:mining`** (`data/allthemodium/worldgen/biome/mining.json`: `"features": []`, `"carvers": {}`, `"spawners": {}`) **[V]**. Quindi: **nessuna caverna, nessun mob che spawna, nessuna feature vanilla** (confermato dalla quest ATM: "no Caves, no Mob Spawns", `istanza:config/ftbquests/quests/lang/en_us/chapters/allthemodium.snbt`) **[V]**.
- `dimension_type` `allthemodium:mining` (`data/allthemodium/dimension_type/mining.json`): `min_y` **-64**, `height` **384**, `has_skylight` true, `has_ceiling` false, `natural` true, `logical_height` 384 **[V]**.

### 2.2 Strati (pietra base e altezze)

Layer dal basso verso l'alto, da `dimension/mining.json` **[V]**:

| Layer | Blocco | Spessore |
|---|---|---|
| 1 | `minecraft:bedrock` | 1 |
| 2 | `minecraft:end_stone` | 64 |
| 3 | `minecraft:netherrack` | 64 |
| 4 | `minecraft:deepslate` | 64 |
| 5 | `minecraft:stone` | 119 |
| 6 | `minecraft:dirt` | 4 |
| 7 | `minecraft:grass_block` | 1 |

Partendo da `min_y` -64, i layer occupano **[D]** (calcolo, assume che il flat generator parta dal fondo come il superflat vanilla): bedrock y=-64; **end stone y -63..0**; **netherrack y 1..64**; **deepslate y 65..128**; **stone y 129..247**; dirt 248..251; erba y=252.

Conferma indiretta **[V]**: gli script ATM (2.3) usano `min:-63 max:63` per i minerali che sostituiscono netherrack/end stone, e `min:65 max:250` per quelli di stone/deepslate, coerente con questa mappa. **Da verificare in gioco** con F3 (y a cui cambia pietra) prima di fissare le altezze definitive.

Conseguenza importante: **in questa dimensione le quattro pietre (end stone, netherrack, deepslate, stone) sono strati pieni e senza cavita'**, quindi:
- tutti i target che servono a Spectrum (stone, deepslate, netherrack, end stone) esistono gia' e coincidono con i target dei nostri minerali **[D]**;
- i minerali che richiedono aria vicina (bismuth: germogli su pareti/soffitti) **non possono generarsi** (3.5).

### 2.3 Come ATM aggiunge i minerali (suoi e di altre mod)

Due meccanismi, entrambi **biome modifier** con step `underground_ores`:

1. **Minerali "nativi" ATM / vanilla** (rame, ferro, diamante, ecc.): file statici in `jar(atm):data/allthemodium/neoforge/biome_modifier/allthemodium/dim_ores/*.json` (es. `iron.json`, `coal.json`, `ore_allthemodium.json`...), che puntano al tag `#allthemodium:mining_features/mining_biomes` e a placed feature in `data/allthemodium/worldgen/placed_feature/*.json` (es. `ore_iron_middle.json`: count 10, trapezoid `absolute 64`..`absolute 156`, in_square, biome) **[V]**.
2. **Minerali di altre mod** (Powah, Mystical Agriculture, Modern Industrialization, Alltheores, Forbidden Arcanus, Draconic Evolution, Silent Gear, Xycraft, ecc.): **generati a runtime da uno script KubeJS** `istanza:kubejs/server_scripts/modpack/miningDim.js` (evento `ServerEvents.generateData('after_mods', ...)`) **[V]**. Per ogni riga dell'array `miningDimOres` (campi `min`, `max`, `size`, `count`, e un blocco sostitutivo per ognuno di `stoneReplaces`, `deepslateReplaces`, `endStoneReplaces`, `netherrackReplaces`) lo script scrive tre file:
   - `allthemodium:worldgen/placed_feature/<path>.json`: count + in_square + height_range **trapezoid** tra `absolute min` e `absolute max` + biome;
   - `allthemodium:worldgen/configured_feature/<path>.json`: `minecraft:ore`, `discard_chance_on_air_exposure: 0.0`, con target `tag_match stone_ore_replaceables` (stone), `tag_match deepslate_ore_replaceables` (deepslate), `block_match minecraft:end_stone`, `block_match minecraft:netherrack`;
   - `allthemodium:neoforge/biome_modifier/allthemodium/dim_ores/<path>.json`: `neoforge:add_features`, `biomes: "#allthemodium:mining_features/mining_biomes"`, step `underground_ores`.
   Esempi di parametri: `uraninite_ore` min 65 max 250 size 8 count 8; `inferium_ore` size 8 count 16; `aggra_inferium_ore` (nether/end) min -63 max 63 count 16; `sulfur_ore` (tutte e 4 le pietre) min -63 max 250 size 8 count 3; `antimony_ore` size 5 count 20; `lignite_coal_ore` size 17 count 25.
   Nota: i blocchi minerali messi *nella dimensione* sono per lo piu' i blocchi originali delle altre mod (es. `powah:uraninite_ore`), non copie; gli id `allthemodium:*_ore` nello script sono solo i nomi dei file generati **[V]**.
3. Nessun file di `istanza:config/` (`allthemodium*.toml` non esiste: il `ls config` non mostra file `allthemodium*`) configura la dimensione **[V]**.

### 2.4 Tag di bioma utili

- `#allthemodium:mining_features/mining_biomes` (`jar(atm):data/allthemodium/tags/worldgen/biome/mining_features/mining_biomes.json`) = `allthemodium:mining` **piu'** `#allthemodium:mining_features/other_features` **[V]**.
- `#allthemodium:mining_features/other_features` = `allthemodium:soul_sand_valley`, `desert_hills`, `desert`, `warped_forest`, `crimson_forest`, `the_other` — sono i biomi di **The Other**, non della Mining **[V]**.
- Quindi: **se ci agganciamo al tag `mining_biomes`, i nostri minerali finiscono *anche* in The Other**, dove gli strati (noise generator con netherrack/basalto...) sono diversi, e in piu' ATM ci aggiunge i suoi. Per colpire *solo* la Mining Dimension conviene usare **l'id diretto `allthemodium:mining`** **[D]**.
- `allthemodium:mining` non e' nei tag `#c:is_overworld`, `#c:is_nether`, `#c:is_end`: li ho cercati in `jar(atm):data/c`, `data/minecraft`, `data/neoforge` (`find ... -path "*biome*"` = nessun file) **[V]**. Quindi i biome modifier gia' esistenti di Spectrum **non** agiscono nella Mining Dimension: serve un nostro modificatore.

### 2.5 La dimensione e' raggiungibile presto

Quest ATM: "Place the Teleport Pad in the Overworld to go to the Mining Dimension" (`quest.2C4299D33D419AA0`), ricompensa Fortune III per l'ingresso (`istanza:config/ftbquests/quests/chapters/allthemodium.snbt`, task `dimension: "allthemodium:mining"`) **[V]**. Il giocatore ci arriva con un Teleport Pad (progressione ATM, non Spectrum) **[V per la quest, D per il momento esatto]**.

---

## 3. Come aggiungere i minerali Spectrum nella Mining Dimension

### 3.1 Il metodo consigliato

1. **Riusare le `configured_feature` di Spectrum** (`spectrum:azurite_ore`, `spectrum:shimmerstone_ore`, `spectrum:stratine_ore`, `spectrum:paltaeria_ore`, `spectrum:dd_malachite_ore`): non duplicare la definizione dei blocchi, e se Spectrum le cambia in un aggiornamento le seguiamo gratis. I loro target (`stone_ore_replaceables`, `deepslate_ore_replaceables`, `base_stone_nether`, `end_stone`) coincidono con gli strati della Mining Dimension **[D]**.
2. **Definire placed_feature nuove nel nostro namespace**, perche' quelle di Spectrum **non si possono riusare**: usano quote relative al fondo/cima del mondo (`above_bottom`, `below_top`), che in una dimensione con strati diversi finirebbero nel layer sbagliato. Esempio: l'azurite originale sta a y -64..-32; nella Mining Dimension quell'intervallo e' *end stone*, dove l'azurite non ha target, e non genererebbe nulla **[D]**. Seguiamo il metodo di ATM: quote `absolute`.
3. **Un biome modifier per feature** (o uno solo con piu' feature) in `data/spectral_ascension/neoforge/biome_modifier/`, `biomes: "allthemodium:mining"`, `step: "underground_ores"` (lo stesso step di ATM).
4. **Condizione di caricamento**: ogni biome modifier con `neoforge:conditions` -> `neoforge:mod_loaded` `allthemodium`, per non far fallire il caricamento del datapack se la mod non c'e' (NeoForge 1.21.1 supporta `neoforge:conditions` nei file dati) **[D]**.

### 3.2 Percorsi nel nostro jar

```
data/spectral_ascension/worldgen/placed_feature/mining_dim/shimmerstone_ore.json
data/spectral_ascension/worldgen/placed_feature/mining_dim/azurite_ore.json
data/spectral_ascension/worldgen/placed_feature/mining_dim/stratine_ore.json
data/spectral_ascension/worldgen/placed_feature/mining_dim/paltaeria_ore.json
data/spectral_ascension/neoforge/biome_modifier/mining_dim/shimmerstone_ore.json
data/spectral_ascension/neoforge/biome_modifier/mining_dim/azurite_ore.json
data/spectral_ascension/neoforge/biome_modifier/mining_dim/stratine_ore.json
data/spectral_ascension/neoforge/biome_modifier/mining_dim/paltaeria_ore.json
```
L'id della placed feature e' il percorso dentro `placed_feature/` senza `.json`: `spectral_ascension:mining_dim/shimmerstone_ore`.

### 3.3 Esempio completo 1 — Shimmerstone (comune, quote della pietra/deepslate)

`data/spectral_ascension/worldgen/placed_feature/mining_dim/shimmerstone_ore.json`
```json
{
  "feature": "spectrum:shimmerstone_ore",
  "placement": [
    { "type": "minecraft:count", "count": 6 },
    { "type": "minecraft:in_square" },
    {
      "type": "minecraft:height_range",
      "height": {
        "type": "minecraft:uniform",
        "min_inclusive": { "absolute": 65 },
        "max_inclusive": { "absolute": 245 }
      }
    },
    { "type": "minecraft:biome" }
  ]
}
```

`data/spectral_ascension/neoforge/biome_modifier/mining_dim/shimmerstone_ore.json`
```json
{
  "neoforge:conditions": [
    { "type": "neoforge:mod_loaded", "modid": "allthemodium" }
  ],
  "type": "neoforge:add_features",
  "biomes": "allthemodium:mining",
  "features": "spectral_ascension:mining_dim/shimmerstone_ore",
  "step": "underground_ores"
}
```
Cosa succede [D]: in ogni chunk della Mining, 6 tentativi di filone da 7 blocchi, quota scelta uniformemente tra 65 e 245. La configured feature di Spectrum sostituisce `stone_ore_replaceables` con `shimmerstone_ore` (y 129..247) e `deepslate_ore_replaceables` con `deepslate_shimmerstone_ore` (y 65..128). Il target blackslag non trova nulla e non causa errori.

### 3.4 Esempio completo 2 — Stratine (strato di netherrack)

`data/spectral_ascension/worldgen/placed_feature/mining_dim/stratine_ore.json`
```json
{
  "feature": "spectrum:stratine_ore",
  "placement": [
    { "type": "minecraft:count", "count": 8 },
    { "type": "minecraft:in_square" },
    {
      "type": "minecraft:height_range",
      "height": {
        "type": "minecraft:trapezoid",
        "min_inclusive": { "absolute": 1 },
        "max_inclusive": { "absolute": 64 }
      }
    },
    { "type": "minecraft:biome" }
  ]
}
```
`data/spectral_ascension/neoforge/biome_modifier/mining_dim/stratine_ore.json`: identico al precedente, con `"features": "spectral_ascension:mining_dim/stratine_ore"`.

Perche' funziona [D]: la configured `spectrum:stratine_ore` ha come target `#minecraft:base_stone_nether` (include netherrack); lo strato netherrack della Mining e' y 1..64. Le quote fuori dallo strato non trovano target e non producono nulla.

### 3.5 Gli altri (bozza dei parametri, stessa struttura)

| Materiale | Feature da riusare | Parametri proposti per la placed (nostra) | Note |
|---|---|---|---|
| Azurite | `spectrum:azurite_ore` | `rarity_filter` 32 (o 24), `count` 1, `in_square`, trapezoid `absolute 65`..`absolute 128` (deepslate), `biome` | l'originale e' rarissimo e sul fondo (y -64..-32). Il fondo della Mining e' end stone dove l'azurite non c'e'; la deepslate (65..128) e' il suo analogo "profondo". Tenere size 24 |
| Paltaeria | `spectrum:paltaeria_ore` | `count` 3, `in_square`, trapezoid `absolute -63`..`absolute 0` (end stone), `biome` | e' un `scattered_ore`: pochi blocchi sparsi per tentativo; nell'End vero la pietra e' poca, qui e' uno strato pieno da 64 blocchi, quindi un `count` basso evita di averne troppo [D] |
| Malachite (**opzionale**) | `spectrum:dd_malachite_ore` | `count` 4, `in_square`, trapezoid `absolute 65`..`absolute 128`, `biome` | in DD ha count 20 ma quasi tutto in aria (discard 0.75); qui discard 0.75 su blocchi pieni *scarta quasi solo quelli esposti*: densita' effettiva molto piu' alta. Vedi rischio R3 |
| Bismuth | -- | **non fattibile** | `spectrum:random_buds` mette germogli solo su pareti/soffitti dove c'e' aria (`can_place_on_floor: false`, `ceiling: true`, `wall: true`, in `configured_feature/dd_bismuth.json`). La Mining e' piena e senza caverne (`carvers: {}`), quindi non c'e' dove ancorarli [D]. Se ci tenete, serve una feature nuova che scavi una cavita' (fuori scopo) |
| Dragon fossil | -- | sconsigliato | structure-feature "fossile" per biomi desertici; la Mining non ha ne' deserti ne' spazio vuoto |

### 3.6 Alternative considerate

- **Appenderci al tag `#allthemodium:mining_features/mining_biomes`** (come fa ATM): comoda, ma colpisce anche The Other (2.4). Scartata.
- **Modificare gli script KubeJS dell'istanza** (`miningDim.js`): non possibile da un addon (e' contenuto del modpack ATM, licenza "All Rights Reserved" dichiarata nell'intestazione del file) e fragile ad ogni aggiornamento ATM **[V]**.
- **Aggiungere `allthemodium:mining` a `#c:is_overworld`**: farebbe generare nella Mining *tutte* le feature con quote relative di Spectrum e vanilla; imprevedibile, scartata [D].
- **Riusare le placed feature di Spectrum**: non va, vedi 3.1 punto 2.

---

## 4. Progressione e bilanciamento

### 4.1 Il cloaking funziona nella Mining Dimension?

Si, e **non dipende dalla dimensione** **[V]**:
- Il cloak e' una proprieta' del *blocco* (`RevelationAware.register(this)` nel costruttore di `CloakedOreBlock`, `src(spectrum)/blocks/conditional/CloakedOreBlock.java`) e vale in ogni dimensione.
- Il bytecode di Revelationary (`jar(rev):de/dafuqs/revelationary/mixin/`) contiene due mixin rilevanti: `AbstractBlockMixin` sostituisce la **loot table** del blocco con quella del blocco "cloak" per chi non ha l'advancement (`revelationary$switchLootTableForCloakedBlock`, con `isVisibleTo(state, player)` e `getLootPlayerEntity`), e `BlockUnbreakingMixin` interviene su `getDestroyProgress` se il blocco non e' visibile. Quindi finche' il giocatore non ha l'advancement: il minerale ha l'aspetto della pietra "cloak" (rendering lato client) e **non da' il drop del minerale**, ma quello della pietra **[V per l'esistenza dei mixin e dei metodi; D per i dettagli esatti: es. il valore di destroy progress]**.
- `CloakedOreBlock.getExpDrop` restituisce 0 xp a chi non vede il blocco **[V]**.
- Il blocco "cloak" e' la pietra corretta per ogni variante: `STONE` per `shimmerstone_ore`/`azurite_ore`/`malachite_ore`, `DEEPSLATE` per `deepslate_*`, `NETHERRACK` per `stratine_ore`, `END_STONE` per `paltaeria_ore` (`SpectrumBlocks.java` righe 1224-1254) **[V]**. Nella Mining Dimension i minerali sostituiscono proprio quelle pietre: **l'aspetto cloaked coincide con la roccia circostante**, quindi nessuna "macchia" visibile.
- Eccezione: i rendering di mod esterne che ignorano il cloak (minimappe, Jade/WTHIT, Mining Gadgets "ore sight", Create/AE scanner, potion di ATM "Ore Sight" citata dalle quest ATM) possono mostrare il blocco col suo nome vero **[D]**: stesso rischio che gia' esiste nell'Overworld, non e' nuovo.

### 4.2 Aggiungerli puo' rompere o anticipare la progressione?

Nessun rischio di *rottura*: gli advancement di reveal non dipendono dalla dimensione. **Rischi di anticipo** (ordine dal piu' al meno serio):

| # | Rischio | Gravita' | Spiegazione |
|---|---|---|---|
| R1 | **Stratine e paltaeria raggiungibili senza Nether/End** | Media | In Spectrum sono i materiali "da esplorazione": Nether e End. Nella Mining Dimension (Teleport Pad dall'Overworld) ci sono sotto i piedi. **Non si rompe il gating** (sono cloaked: pietra + drop di pietra finche' non hai `build_advanced_pedestal_structure` / `build_complex_pedestal_structure`), ma il giocatore, una volta rivelati, ottiene tutta la quantita' che vuole senza visitare il Nether/End. E' coerente con cio' che ATM fa gia' per quasi tutte le altre mod (le "aggra", `crimson_iron`, `azure_silver` ecc. sono nella Mining con netherrack/end stone) **[V per lo script; D per la valutazione]**. Se si vuole mantenere la fatica, ridurre `count` (3/4 dell'originale) |
| R2 | **Azurite/shimmerstone troppo abbondanti** | Bassa | Azurite e' *midgame* (reveal dopo `collect_neolith`) e rarissima in Overworld; la Mining ha molta roccia da minare con strumenti ATM molto forti (e quarry, che nelle quest ATM "mine everything"). Anche con le quantita' di 3.5 e' una dimensione "da miniera": il bilancio e' una scelta di design, non un rischio di rottura. Shimmerstone e' *early* (reveal da `craft_using_pedestal`): nessun problema |
| R3 | **Malachite aggira la Deeper Down** | Media | In Spectrum la malachite e' un materiale lategame disponibile nella DD (dimensione della progressione, accessibile dopo il Pedestal Moonstone/COMPLEX). Reveal = `build_complex_pedestal_structure`, quindi e' comunque lategame; ma toglie un motivo per esplorare la DD. Consiglio: **opzionale, off di default**, oppure `count` molto basso |
| R4 | **Cloak visivo + quest ATM "mine all"** | Bassa | I quarry/tool automatici di ATM possono raccogliere gli ore cloaked: con Revelationary il drop e' lo stesso della pietra, quindi **non si ottiene materiale Spectrum non rivelato** (il drop dipende dal *giocatore* in `getLootPlayerEntity`; per un quarry senza giocatore **[D]** potrebbe usare il drop "pieno" o "cloak": da provare) |
| R5 | **Ore di Nether/End/Deeper Down gia' nella Mining** | Informativo | L'elenco ATM (2.3) porta nella Mining i minerali di *altre mod*, non quelli di Spectrum: Spectrum **non** e' nell'array `miningDimOres` **[V]** (grep: nessun `spectrum:` in `miningDim.js`). I minerali vanilla Nether/End (netherite, quarzo) e "aggra_*" gia' ci sono, ma non influenzano i reveal di Spectrum |

Controllo checklist progressione ([00-checklist-progressione.md](00-checklist-progressione.md)) per questa feature:
- Nessun nuovo advancement, nessuna nuova ricetta: **non serve** aggiungere lucchetti.
- Nessun riferimento a `spectrum:__wip`.
- Nostra regola "tutto il contenuto richiede il late game" **non si applica**: questi sono minerali Spectrum esistenti, il cui gating e' gia' dato dal reveal.

### 4.3 Quantita' proposte rispetto all'Overworld

Densita' = filoni per chunk x dimensione del filone / spessore dello strato (indicativa) **[D]**.

| Minerale | Originale | Proposta Mining | Densita' relativa |
|---|---|---|---|
| Shimmerstone | 9 x size 7, uniform y -16..128 (144 quote) | 6 x size 7, uniform y 65..245 (180 quote) | circa 0.5x (quote piu' ampie) |
| Azurite | 1/32 chunk, size 24, y -64..-32 (32 quote) | 1/32 chunk, size 24, y 65..128 (64 quote) | circa 0.5x |
| Stratine | 12 x size 6, y 4..192 trapezoid nel Nether (maggior parte nelle quote ~100, dove il Nether ha caverne) | 8 x size 6, y 1..64 | simile o superiore (strato pieno, nessuna aria che scarta) |
| Paltaeria | 4 x scattered 4, in tutta la dimensione End (poca pietra) | 3 x scattered 4, y -63..0 | superiore (strato pieno) |
| Malachite (opz.) | 20 x size 4 in DD | 4 x size 4, y 65..128 | molto ridotta ma "esposta" a minare |

Intento: **riduzione leggera** (la Mining e' gia' "ricca" per ATM, vedi count 16-25 nello script) senza rendere i materiali trascurabili. Tutti i numeri sono modificabili cambiando due righe nella placed_feature: provate 3-4 valori in gioco con `/place feature` o creando un mondo di test **[D]**.

### 4.4 Compatibilita' con ATM10 e con il reset della dimensione

- **Compatibilita'**: usiamo solo meccanismi standard (biome modifier + placed feature); non tocchiamo `miningDim.js`, ne' l'ordine di caricamento. I nostri file sono datapack di mod, applicati dopo quelli di Spectrum; l'uso di `neoforge:mod_loaded` evita errori se `allthemodium` viene rimosso **[D]**.
- **Reset della dimensione**: non ho trovato in `istanza:config/` ne' in `kubejs/` alcuna configurazione di reset della Mining Dimension (cerca "reset" in `allthetweaks-common.toml`: nessun risultato) **[V]**. In ATM la dimensione si rigenera cancellando le sue regioni dal mondo [D, conoscenza generale]. Poiche' la generazione e' **data-driven**, i chunk generati *dopo* l'aggiunta del nostro datapack avranno i minerali Spectrum; i chunk gia' generati **non** li riceveranno retroattivamente **[D]** (comportamento standard di Minecraft). Per un mondo esistente: reset della Mining o esplorazione di chunk nuovi.
- **Altezze**: dipendono dalla mappa degli strati (2.2), che e' un calcolo [D]: confermare con F3 prima del rilascio. Se in una versione futura ATM cambia gli strati, andranno riviste.
- **Cloak e world data**: i blocchi gia' piazzati si leggono come blocchi veri, quindi niente migrazioni.
- **Multiplayer**: la rivelazione e' per giocatore (advancement per giocatore, Revelationary sincronizza): nessuna differenza tra dimensioni **[D]**.

---

## 5. Cosa fare dopo (passi pratici)

1. Creare i 4 coppie di file di 3.2 (shimmerstone, azurite, stratine, paltaeria) nel jar dell'addon; la malachite solo se confermata (R3).
2. In gioco: entrare nella Mining (Teleport Pad nell'Overworld), controllare con F3 gli strati e le quote, poi usare `/spectrum` e il comando di debug di Revelationary per rivelare un ore alla volta, e verificare: (a) prima del reveal il blocco e' indistinguibile dalla pietra e il drop e' pietra; (b) dopo il reveal drop e xp corretti.
3. Provare un quarry/Mining Gadget sul filone cloaked per chiudere R4.
4. Annotare in `04-progressione-e-guidebook.md` che il gating dei minerali e' dato solo da `reveal_*` e non dalla dimensione.
