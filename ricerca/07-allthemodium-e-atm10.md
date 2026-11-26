# 07 — Allthemodium e ATM10: materiali, tier, modifiche a Spectrum

> Ricerca per **Spectral Ascension** (addon NeoForge 1.21.1 per Spectrum 1.12.9, modpack ATM10 8.5).
> Fonti principali, tutte lette in sola lettura:
> - `jar(allthemodium)` = jar estratto `allthemodium-3.2.1_mc_1.21.1` (dati + bytecode letto con `javap -p -c -constants`)
> - `src(spectrum)` = sorgente `spectrum-1.12.9`, `jar(spectrum)` = jar estratto
> - `istanza:` = la cartella della tua istanza CurseForge di ATM10
>
> Legenda: **[V]** = verificato leggendo file/bytecode · **[D]** = dedotto (ragionamento o conoscenza generale, da confermare in gioco)

---

## 0. Mini-glossario (per chi è nuovo al modding)

- **Tag** — una lista con nome di item o blocchi, definita in JSON dentro `data/<namespace>/tags/...`. Una ricetta che chiede `#c:ingots/vibranium` accetta *qualsiasi* item in quella lista. Più mod possono aggiungere voci allo stesso tag, e i file vengono uniti.
- **Tag `c:` (convention tags)** — il namespace condiviso `c` che NeoForge e Fabric usano per i materiali comuni (`c:ingots/iron`, `c:storage_blocks/...`). Usarli nelle ricette rende il mod compatibile con qualunque mod fornisca quel materiale. Attenzione: anche Allthemodium mette in `c:` dei tag *suoi* (per esempio `c:needs_vibranium_tool`). Stanno in `c:` ma **non** sono convenzioni ufficiali NeoForge.
- **Tier (≈ ToolMaterial)** — in 1.21.1 è l'interfaccia `net.minecraft.world.item.Tier`. Fornisce durabilità (`uses`), velocità di scavo (`speed`), bonus danno (`attackDamageBonus`), incantabilità e ingrediente di riparazione. Fornisce anche `getIncorrectBlocksForDrops()`, cioè un **tag di blocchi che questo tool NON può raccogliere**.
- **Livello di mining in 1.21** — non esiste più un numero intero. Ci sono due famiglie di tag:
  - `needs_X_tool`: i blocchi che richiedono *almeno* il tier X;
  - `incorrect_for_X_tool`: i blocchi che il tier X *non* può raccogliere. È il tag che il Tier restituisce.
  Un blocco "di livello superiore" si crea aggiungendolo a `needs_Y_tool` e mettendo `#needs_Y_tool` dentro `incorrect_for_X_tool` per ogni X inferiore.
- **ArmorMaterial** — in 1.21 è un oggetto registrato in un registry (`Registries.ARMOR_MATERIAL`). Contiene difesa per slot, incantabilità, suono, riparazione, layer delle texture, toughness e knockback resistance. La **durabilità dell'armatura non sta nel material**: si imposta per item con `Item.Properties.durability(n)`.
- **Item component (data component)** — in 1.21 i dati di un ItemStack sono "componenti" tipizzati (`DataComponents.UNBREAKABLE`, `MAX_DAMAGE`, `ATTRIBUTE_MODIFIERS`…). Un item è indistruttibile se ha il componente `UNBREAKABLE` oppure se non ha `MAX_DAMAGE`, cioè non ha durabilità.
- **KubeJS** — mod che permette ai creatori di modpack di modificare ricette, tag e loot con script JavaScript (`kubejs/server_scripts/*.js`) e JSON (`kubejs/data/...`) senza scrivere un mod Java. ATM10 lo usa molto.

---

## 1. Materiali Allthemodium (id esatti e tag)

Gli id vengono da `jar(allthemodium):assets/allthemodium/lang/en_us.json` **[V]**. I tag vengono da `jar(allthemodium):data/c/tags/item/**` **[V]**.

### 1.1 Metalli base (×3: `allthemodium`, `vibranium`, `unobtainium`)

| Forma | Id item (sostituisci `M`) | Tag `c:` specifico | Tag `c:` aggregato |
|---|---|---|---|
| Minerale | `allthemodium:allthemodium_ore`, `allthemodium:allthemodium_slate_ore`; `allthemodium:vibranium_ore`, `allthemodium:other_vibranium_ore`; `allthemodium:unobtainium_ore` | `c:ores/M` | `c:ores` |
| Raw | `allthemodium:raw_M` | `c:raw_materials/M` | `c:raw_materials` |
| Blocco raw | `allthemodium:raw_M_block` | `c:storage_blocks/raw_M` | `c:storage_blocks` |
| Lingotto | `allthemodium:M_ingot` | `c:ingots/M` | `c:ingots` |
| Pepita | `allthemodium:M_nugget` | `c:nuggets/M` | `c:nuggets` |
| Blocco | `allthemodium:M_block` | `c:storage_blocks/M` | `c:storage_blocks` |
| Piastra | `allthemodium:M_plate` | `c:plates/M` | `c:plates` |
| Ingranaggio | `allthemodium:M_gear` | `c:gears/M` | `c:gears` |
| Asta | `allthemodium:M_rod` | `c:rods/M` | — |
| Polvere | `allthemodium:M_dust` | `c:dusts/M` | `c:dusts` |
| Processing Mekanism | `M_clump`, `M_shard`, `M_crystal`, `dirty_M_dust`, `crushed_M` | `c:clumps/M`, `c:shards/M`, `c:crystals/M`, `c:dirty_dusts/M` | (voci `required:false`) |

Ricette di base in `jar(allthemodium):data/allthemodium/recipe/` **[V]**:
- `allthemodium_plate.json`: 4 lingotti in quadrato 2×2 danno 1 piastra. Usa `#c:ingots/allthemodium`.
- `allthemodium_rod.json`: 3 lingotti in colonna danno 1 asta.
- `allthemodium_gear.json`: 8 lingotti danno 1 ingranaggio.
- Per vibranium e unobtainium le ricette sono analoghe.

### 1.2 Leghe (alloy): ne esistono 3

| Lega | Lingotto | Polvere | Blocco | Tag |
|---|---|---|---|---|
| Vibranium + Allthemodium | `allthemodium:vibranium_allthemodium_alloy_ingot` | `..._alloy_dust` | `..._alloy_block` | `c:ingots/vibranium_allthemodium_alloy`, `c:dusts/...`, `c:storage_blocks/...` |
| Unobtainium + Allthemodium | `allthemodium:unobtainium_allthemodium_alloy_ingot` | idem | idem | `c:ingots/unobtainium_allthemodium_alloy` (il file elenca la voce due volte, è innocuo) |
| Unobtainium + Vibranium | `allthemodium:unobtainium_vibranium_alloy_ingot` | idem | idem | `c:ingots/unobtainium_vibranium_alloy` |

- Per le leghe **non esistono** pepite, piastre, aste o ingranaggi **[V]**. Il tag `c:ingots` aggregato include i 3 lingotti lega.
- Il tag di riparazione del tier Alloy è `TagRegistry.UNOBATM_ALLOY` **[V]**, che corrisponde a `c:ingots/unobtainium_allthemodium_alloy` **[D]**.

**Come si fanno le leghe**
- **Nel mod da solo** **[V]**:
  - `recipe/alloys/molten_*.json` mescola 90 mB + 90 mB di metallo fuso nella fonderia di Tinkers' Construct (`tconstruct:alloy`, temperatura 9000).
  - `recipe/casting/alloys/*_alloy_ingot.json` cola il fuso su un **cuore di Piglich come stampo consumato** (`cast_consumed: true`).
  - `*_alloy_block.json` e `*_alloy_ingot_from_block.json` fanno la compressione 9↔1.
  - Tutte queste ricette sono condizionate a `mod_loaded: tconstruct`.
- **In ATM10 ci sono ricette in più**, in `istanza:kubejs/server_scripts/modpack/atm_alloys.js` **[V]**:
  - **Vibranium-ATM**: Powah *Energizing* con `#c:ingots/allthemodium`, `#c:ingots/vibranium`, 2× `allthemodium:piglich_heart` e `allthecompressed:nitro_crystal_block_1x`, per **1.000.000.000 FE**. Il blocco richiede 9 G FE.
  - **Unobtainium-ATM**: *Enchanting Apparatus* di Ars Nouveau. Reagente `ars_nouveau:source_gem`; sui piedistalli 2× piglich heart, i lingotti ATM e unobtainium e le 4 essenze (aria, terra, fuoco, acqua); costo 10.000 source.
  - **Unobtainium-Vibranium**: *Dissolution Chamber* di Industrial Foregoing. 4× `industrialforegoing:pink_slime`, i lingotti vibranium e unobtainium, 2× piglich heart e 100 mB di `allthemodium:soul_lava`.

  In tutti e tre i casi la lega richiede **2 Piglich Heart per lingotto** più materiali di endgame di altre mod.

### 1.3 Item speciali

- `allthemodium:piglich_heart` e `allthemodium:piglich_heart_block` (tag `c:storage_blocks` aggregato) **[V]**.
  - Il cuore cade dal mob `allthemodium:piglich`: `loot_table/entities/piglich.json` dà 1 cuore garantito **[V]**.
  - ATM10 classifica il Piglich come boss: `istanza:kubejs/server_scripts/Tweaks/tags.js`, tag entità `c:bosses` **[V]**.
- Smithing template (sagome di forgiatura) **[V]**:
  - `allthemodium:allthemodium_upgrade_smithing_template`: si applica a equipaggiamento di netherite, con un lingotto ATM.
  - `allthemodium:vibranium_upgrade_smithing_template`: si applica a equipaggiamento allthemodium, con un lingotto vibranium.
  - `allthemodium:unobtainium_upgrade_smithing_template`: si applica a equipaggiamento vibranium, con un lingotto unobtainium.

  Duplicazione: `recipe/smithing/*_upgrade_smithing_template.json` dà 2 template a partire da template + 7 lingotti del tier precedente (netherite, ATM, vibranium) + un blocco (deepslate, `allthemodium:ancient_stone`, end_stone) **[V]**.

  Dove si trovano: le loot table `loot_table/arch.json` (template ATM), `arch2.json` (template vibranium) e `chest/template.json` (template unobtainium) **[V]**. I nomi `arch*` fanno pensare a blocchi sospetti di archeologia nelle strutture ATM (ancient pyramid, dungeon, piglin village in `worldgen/structure/`) **[D]**.
- Equipaggiamento esistente **[V]**:
  - Per ogni metallo: `M_sword`, `M_pickaxe`, `M_axe`, `M_shovel`, `M_hoe`, `M_mace`, `M_brush`, `M_helmet`, `M_chestplate`, `M_leggings`, `M_boots`.
  - Extra: `allthemodium:allthemodium_bow`, `allthemodium:vibranium_shield`, `allthemodium:unobtainium_crossbow`.
  - Alloy: `allthemodium:alloy_sword`, `alloy_pick`, `alloy_axe`, `alloy_shovel`, `alloy_paxel`, `alloy_trident`. **Non esiste armatura Alloy.**
- Altri: `allthemodium_apple`, `allthemodium_carrot`, `teleport_pad`, i fluidi `molten_*` e `soul_lava`.
- **ATM Star** (da All The Tweaks, non da Allthemodium): `allthetweaks:atm_star`, tag `allthetweaks:atm_star`; blocco `allthetweaks:atm_star_block`, tag `c:storage_blocks/atm_star` **[V]** (`jar(allthetweaks-2.12.1):data/...`). È l'obiettivo finale del pack. Ricette in `istanza:kubejs/server_scripts/modpack/atm_star.js`.

### 1.4 Tag di item non `c:` utili

- `allthemodium:helmets`, `allthemodium:chestplates`, `allthemodium:leggings`, `allthemodium:boots` (`jar(allthemodium):data/allthemodium/tags/item/`) **[V]**. Il tag dei caschi è quello che l'helmet usa per la respirazione in acqua (`TagRegistry.ATM_HELMETS`).
- `c:tools/mining_tool` include i picconi ATM e `alloy_pick`; `c:tools/melee_weapon` include spade, asce e mazze **[V]**.

---

## 2. Tier: statistiche e livelli di mining

### 2.1 Tool tier di Allthemodium

Fonte: `jar(allthemodium):com/thevortex/allthemodium/material/ATMTier.class` **[V]**. Il costruttore è `(incorrectTag, speed, uses, attackBonus, enchant, repair)`; la mappatura è stata verificata leggendo i `putfield`.

| Tier | uses | speed | attackDamageBonus | enchant | incorrect tag | riparazione |
|---|---|---|---|---|---|---|
| ALLTHEMODIUM | 9001 | 15.0 | 12.0 | 85 | `c:incorrect_for_allthemodium_tool` | `c:ingots/allthemodium` |
| VIBRANIUM | 9001 | 16.0 | 25.0 | 100 | `c:incorrect_for_vibranium_tool` | `c:ingots/vibranium` |
| UNOBTAINIUM | 9001 | 18.0 | 35.0 | 125 | `c:incorrect_for_unobtainium_tool` | `c:ingots/unobtainium` |
| ALLOY | 9001 | 35.0 | 65.0 | 200 | `c:incorrect_for_allthemodiumalloy_tool` | lega unob-ATM |

Danno effettivo dei tool, letto da `ModRegistry` (`XItem.createAttributes(tier, dmg, speedMod)`) **[V]**. Il calcolo **[D]** usa la formula vanilla: danno = 1 + bonus del tier + parametro dell'item; velocità d'attacco = 4 + modificatore.

| Item | param (danno, velocità) | Danno | Velocità d'attacco |
|---|---|---|---|
| allthemodium_sword | 5, −1.4 | **18** | 2.6 |
| vibranium_sword | 10, −0.4 | **36** | 3.6 |
| unobtainium_sword | 15, +0.4 | **51** | 4.4 |
| alloy_sword | 3, +2.9 | **69** | 6.9 |
| allthemodium_pickaxe | 2, −1.0 | 15 | 3.0 |
| vibranium_pickaxe | 4, −0.8 | 30 | 3.2 |
| unobtainium_pickaxe | 6, −0.6 | 42 | 3.4 |
| alloy_pick | 2, +2.0 | 68 | 6.0 |

**Indistruttibilità ATM** **[V]**:
- Tutti i tool, inclusi gli Alloy e il paxel, hanno `.component(DataComponents.UNBREAKABLE, new Unbreakable(true))` e `.fireResistant()`, con rarità EPIC. La durabilità 9001 del Tier è quindi solo nominale.
- Le armature **non hanno** `.durability(...)` (proprietà: `fireResistant`, `rarity EPIC`, `stacksTo(1)`). Senza il componente `MAX_DAMAGE` non si consumano mai **[D]**.

### 2.2 Armor material di Allthemodium

Fonte: `jar(allthemodium):.../registry/ArmorRegistries.class` **[V]**.

| Material | Elmo / Petto / Gambali / Stivali (Body) | Totale | Toughness | Knockback res. | Enchant |
|---|---|---|---|---|---|
| `allthemodium:allthemodium` | 4 / 9 / 7 / 4 (8) | 24 | 5.0 | 0.5 | 85 |
| `allthemodium:vibranium` | 6 / 11 / 9 / 6 (10) | 32 | 9.0 | 0.8 | 105 |
| `allthemodium:unobtainium` | 8 / 13 / 11 / 8 (12) | 40 | 15.0 | 1.0 | 125 |

L'associazione tra le mappe di difesa (lambda 0, 1, 2) e i material segue l'ordine nel sorgente **[D, molto probabile]**.

Gli **effetti speciali** sono in `events/ArmorEvents.class` e `Allthemodium_Helmet.class` **[V]**:
- gli stivali annullano il danno da caduta;
- il petto riduce il danno MAGIC (×0.5 ATM, ×0.25 Vibranium, ×0.1 Unobtainium);
- il petto Unobtainium protegge dal void (`FELL_OUT_OF_WORLD`);
- il petto dà immunità a fuoco e lava;
- l'elmo dà ossigeno sott'acqua e rimuove l'effetto Darkness;
- i caschi gestiscono anche i danni `FLY_INTO_WALL` e `DROWN`;
- tutti i pezzi rendono i piglin neutrali.

### 2.3 Tag di mining level

Fonte: `jar(allthemodium):data/...` **[V]**.

| File | Contenuto |
|---|---|
| `neoforge/tags/block/needs_netherite_tool.json` | allthemodium_ore, allthemodium_slate_ore, raw_allthemodium_block, allthemodium_block |
| `c/tags/block/needs_allthemodium_tool.json` | vibranium_ore, other_vibranium_ore, raw_vibranium_block, vibranium_block |
| `c/tags/block/needs_vibranium_tool.json` | unobtainium_ore, raw_unobtainium_block, unobtainium_block |
| `c/tags/block/needs_unobtainium_tool.json` | tutte le varianti di `ancient_*` stone bricks, polished, smooth e cracked, più i 3 blocchi lega |
| `c/tags/block/needs_allthemodiumalloy_tool.json` | *(vuoto)* |
| `c/tags/block/incorrect_for_allthemodium_tool.json` | `#c:needs_vibranium_tool`, `#c:needs_unobtainium_tool`, `#c:needs_allthemodiumalloy_tool` |
| `c/tags/block/incorrect_for_vibranium_tool.json` | `#c:needs_unobtainium_tool`, `#c:needs_allthemodiumalloy_tool` |
| `c/tags/block/incorrect_for_unobtainium_tool.json` | `#c:needs_allthemodiumalloy_tool` |
| `c/tags/block/incorrect_for_allthemodiumalloy_tool.json` | *(vuoto: può tutto)* |
| `minecraft/tags/block/incorrect_for_netherite_tool.json` e `incorrect_for_diamond_tool.json` | aggiungono tutti e 4 i `#c:needs_*_tool` ATM |

Attenzione all'**offset di un livello**: il tag `needs_X_tool` contiene i blocchi che *X sa scavare e i tier inferiori no*. In pratica:
- il minerale di vibranium è in `needs_allthemodium_tool`, quindi basta un piccone allthemodium;
- l'unobtainium serve un piccone vibranium;
- l'ancient stone e i blocchi lega servono un piccone unobtainium;
- il minerale di allthemodium richiede la netherite.

Conseguenze per Spectrum **[D, dalla logica dei tag]**: bedrock e malachite usano `INCORRECT_FOR_NETHERITE_TOOL` (vedi `src(spectrum):registries/SpectrumToolTiers.java`). Quindi:
- **possono** minare il minerale di allthemodium;
- **non possono** minare vibranium, unobtainium o ancient stone.

### 2.4 Confronto con vanilla e Spectrum

Fonti Spectrum **[V]**:
- `src(spectrum):.../registries/SpectrumToolTiers.java`
- `.../registries/SpectrumArmorMaterials.java`
- `.../registries/SpectrumItems.java`
- i default in `.../config/SpectrumConfig.java`, che coincidono con `istanza:config/spectrum-startup.toml` (righe 119-144, valori **non modificati** da ATM10)

I valori della netherite sono quelli vanilla **[D]**, da conoscenza generale di MC 1.21.1.

**Tool**

| Tier | Durabilità | Speed | Bonus danno | Enchant | Incorrect tag | Spada (danno / vel.) | Indistruttibile |
|---|---|---|---|---|---|---|---|
| Netherite (vanilla) | 2031 | 9.0 | 4.0 | 15 | `incorrect_for_netherite_tool` | 8 / 1.6 | no |
| Malachite (Spectrum) | 1536 | 14.0 | 5.0 | 20 | `incorrect_for_netherite_tool` | ultra greatsword +7: 13 / 1.2 [D] | no |
| Bedrock (Spectrum) | `Integer.MAX_VALUE` | 15.0 | 5.0 | 3 | `incorrect_for_netherite_tool` | 10 / 1.6 | sì (`Unbreakable(false)` = nascosto nel tooltip) |
| Allthemodium | 9001 (nominale) | 15.0 | 12.0 | 85 | `c:incorrect_for_allthemodium_tool` | 18 / 2.6 | sì (`Unbreakable(true)`) |
| Vibranium | 9001 | 16.0 | 25.0 | 100 | `c:incorrect_for_vibranium_tool` | 36 / 3.6 | sì |
| Unobtainium | 9001 | 18.0 | 35.0 | 125 | `c:incorrect_for_unobtainium_tool` | 51 / 4.4 | sì |
| Alloy | 9001 | 35.0 | 65.0 | 200 | `c:incorrect_for_allthemodiumalloy_tool` | 69 / 6.9 | sì |

**Armature**

| Material | Elmo/Petto/Gamb./Stiv. | Totale | Toughness | KB res. | Enchant | Durabilità |
|---|---|---|---|---|---|---|
| Netherite (vanilla) [D] | 3/8/6/3 | 20 | 3.0 | 0.1 | 15 | 407/592/555/481 |
| Bedrock (Spectrum) | 5/12/9/5 | 31 | 3.0 | 0.3 | 5 | 70×13 / 70×15 / 70×16 / 70×11, ma `Unbreakable` |
| Gemstone (Spectrum) | da config | — | — | — | 15 | — |
| Allthemodium | 4/9/7/4 | 24 | 5.0 | 0.5 | 85 | nessuna (non si consuma) |
| Vibranium | 6/11/9/6 | 32 | 9.0 | 0.8 | 105 | nessuna |
| Unobtainium | 8/13/11/8 | 40 | 15.0 | 1.0 | 125 | nessuna |

Spectrum **non ha un'armatura di malachite**: solo tool, cioè workstaff, ultra greatsword, crossbow e bident. Notare due anomalie:
- la bedrock di Spectrum ha **più armatura** (31) dell'allthemodium (24) e quasi quanta il vibranium (32), ma molta meno toughness;
- la bedrock ha incantabilità molto bassa (3 per i tool, 5 per l'armatura).

---

## 3. Come si ottengono i materiali in ATM10 (per il bilanciamento)

Fonti: `jar(allthemodium):data/allthemodium/neoforge/biome_modifier/**`, `worldgen/placed_feature/allthemodium_ore.json`, `dimension/*.json` **[V]**.

- **Dimensioni del mod**: `allthemodium:mining` (Mining), `allthemodium:the_other` (The Other), `allthemodium:the_beyond` (The Beyond) (`data/allthemodium/dimension/`) **[V]**.
- **Allthemodium**:
  - nell'Overworld nel bioma `minecraft:deep_dark` (`allthemodium.json`; il placed feature va da count 10 a y ≤ 10 attaccato a superfici);
  - nella Mining dimension (`dim_ores/ore_allthemodium.json`);
  - in The Other (`the_other/allthemodium.json`) **[V]**.
  - Si scava con la netherite.
- **Vibranium**:
  - nel Nether (`#minecraft:is_nether`) come `vibranium_ore`;
  - in The Other come `other_vibranium_ore` dentro l'ancient stone **[V]**.
  - Serve un piccone allthemodium.
- **Unobtainium**: nell'End, bioma `minecraft:end_highlands` **[V]**. Serve un piccone vibranium.
- **Laser drill**: ATM10 aggiunge ricette Industrial Foregoing che limitano raw ATM, vibranium e unobtainium ai tag bioma `atm10:*_spawnable`:
  - deep_dark per ATM;
  - crimson/warped forest per vibranium;
  - end_highlands per unobtainium.

  File: `istanza:kubejs/data/atm10/tags/worldgen/biome/*.json` e `istanza:kubejs/data/industrialforegoing/recipe/laser_drill_ore/raw_materials/raw_*.json` **[V]**.
- **Catena dell'equipaggiamento**: netherite + template + lingotto ATM, poi template vibranium + lingotto vibranium, poi template unobtainium + lingotto unobtainium. Sono ricette `smithing_transform` in `recipe/smithing/` **[V]**. Per esempio `allthemodium_pickaxe_smithing.json` usa come base `minecraft:netherite_pickaxe`.
- **Tool Alloy in ATM10** **[V]**:
  - Rituale *Hephaestus Forge* di Forbidden Arcanus (`istanza:kubejs/data/allthemodium/forbidden_arcanus/hephaestus_forge/ritual/alloy_*.json`, `forge_tier: 4`). Ingrediente principale: il tool unobtainium corrispondente. In più: i 3 lingotti lega, scaglie di drago, 2× eternal stella, ed essenze (aureal 100, blood 1000, souls 32, experience 300).
  - Alternativa: la multiblock auto_forge (`istanza:kubejs/server_scripts/modpack/runic_multis/recipes/auto_forge.js`).
  - Il paxel Alloy combina i 4 tool Alloy, 4 prismi e `mysticalagradditions:awakened_supremium_paxel`.
- **Silent Gear**: piastre "silent" ATM, vibranium e unobtainium via smithing (`istanza:kubejs/server_scripts/mods/Allthemodium/Recipes.js`) **[V]**. È irrilevante per noi.

**Costo in sintesi** **[D]**:
- T6 (ATM) è early-endgame e richiede il deep dark o la Mining dimension;
- T7 (Vibranium) richiede il Nether o The Other;
- T8 (Unobtainium) richiede l'End;
- gli Alloy sono decisamente più costosi: Piglich (boss), 1 G FE oppure Ars o Industrial Foregoing, più Forbidden Arcanus tier 4.

---

## 4. Modifiche di ATM10 a Spectrum (ricerca nell'istanza)

**Risultato: ATM10 non tocca Spectrum** in ricette, tag, loot o dimensioni.

| Dove ho cercato | Esito |
|---|---|
| `istanza:kubejs/**`: `grep -ril spectrum` su server_scripts, startup_scripts, client_scripts, data e config | Nessuna occorrenza. L'unico risultato è `kubejs/assets/productivebees/lang/pt_br.json`, cioè una traduzione e non una ricetta. **[V]** |
| `istanza:config/spectrum-startup.toml` | Valori di bedrock e malachite identici ai default del sorgente (bedrock: speed 15, damage 5, enchant 3, armatura 5/12/9/5, toughness 3, KB 0.3; malachite: 1536 / 14 / 5 / 20). **[V]** |
| `istanza:config/apotheosis/enchantments.cfg` (righe ~2732-3652) | Elenca gli incantesimi `spectrum:*` (per esempio `spectrum:indestructible`, Max Level 1) con valori default. È una config generata da Apotheosis, non una modifica deliberata. **[V]** |
| `istanza:config/attributefix/spectrum/`, `config/cristellib/spectrum/*.json5` | Config auto-generate: attributefix lascia i range di default, Cristel Lib tiene tutte le strutture Spectrum abilitate. **[V]** |
| `istanza:mods/allthetweaks-1.21.1-2.12.1.jar` | Contiene solo star, blocchi di nether star ed ender pearl, tag `c:storage_blocks/*`, un tag di Silent Gear e uno di Mekanism. Nessun riferimento a Spectrum. **[V]** |
| `istanza:mods/atmcompat-1.21.1-1.0.0.jar` | Compatibilità ATM con Ars Nouveau (source jar) ed EnderIO. Niente su Spectrum. **[V]** |
| `istanza:mods/spectrumcompat-0.1.0.jar` | Mod di terze parti (MWojt, MIT, https://modrinth.com/project/spectrum-compat). Aggiunge **teste di mob** per mob di altre mod (Aether, ecc.) ai drop di Spectrum. Non tocca tool, tier o ricette. **[V]** |
| `istanza:datapacks/` | C'è solo `sawmill.zip`. **[V]** |
| Spectrum → ATM | `jar(spectrum):data/spectrum/crystal_apothecary_simulation/geore/{allthemodium,vibranium,unobtainium}.json` esistono, ma sono condizionati alla mod `geore` (non installata) **[V]/[D]**. |

Conseguenza **[D]**: in ATM10 i tool bedrock e malachite di Spectrum restano a livello netherite. Non scavano vibranium, unobtainium né ancient stone. Questo lascia spazio naturale ai nostri tier T6-T8.

---

## 5. Implicazioni per Spectral Ascension

### 5.1 Ingredienti consigliati per tier (preferire i tag `c:`)

| Tier SA | Ingredienti principali (Pedestal / Fusion Shrine) | Extra "di prestigio" |
|---|---|---|
| T6 Allthemodium | `#c:ingots/allthemodium`, `#c:plates/allthemodium`, `#c:rods/allthemodium`, `#c:gears/allthemodium`, `#c:storage_blocks/allthemodium` | `allthemodium:allthemodium_upgrade_smithing_template` come catalizzatore (si duplica) |
| T7 Vibranium | `#c:ingots/vibranium`, `#c:plates/vibranium`, `#c:rods/vibranium`, `#c:gears/vibranium` | `allthemodium:vibranium_upgrade_smithing_template` |
| T8 Unobtainium | `#c:ingots/unobtainium`, `#c:plates/unobtainium`, `#c:rods/unobtainium`, `#c:gears/unobtainium` | `allthemodium:unobtainium_upgrade_smithing_template` |
| Alloy | `#c:ingots/vibranium_allthemodium_alloy`, `#c:ingots/unobtainium_allthemodium_alloy`, `#c:ingots/unobtainium_vibranium_alloy` (o i rispettivi `#c:storage_blocks/...`) | `allthemodium:piglich_heart` (non ha tag: usare l'id); eventualmente `#allthetweaks:atm_star` solo per un pezzo finale |

Note:
- Per le leghe non esistono piastre, aste o ingranaggi: le ricette Alloy devono usare lingotti o blocchi.
- I template e il piglich heart non hanno tag `c:`, quindi vanno referenziati per id. Per sicurezza conviene condizionare la ricetta con `neoforge:conditions: [{type: "neoforge:mod_loaded", modid: "allthemodium"}]`.
- Il tool del tier precedente come ingrediente centrale (stile smithing upgrade) è coerente con la progressione ATM.

### 5.2 Mining level: come far minare i blocchi ATM ai nostri tool

Usare direttamente i tag di ATM come `incorrectBlocksForDrops` del nostro `Tier`. Bastano `TagKey`, senza dipendenza a compile-time:

```java
TagKey<Block> INCORRECT_T6 = BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", "incorrect_for_allthemodium_tool"));
TagKey<Block> INCORRECT_T7 = BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", "incorrect_for_vibranium_tool"));
TagKey<Block> INCORRECT_T8 = BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", "incorrect_for_unobtainium_tool"));
TagKey<Block> INCORRECT_ALLOY = BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", "incorrect_for_allthemodiumalloy_tool"));
```

- Se ATM non è installato, il tag è vuoto e quindi il tool può tutto. Per un addon pensato per ATM10 va bene **[D]**.
- Un'alternativa più robusta è definire **nostri** tag, per esempio `spectral_ascension:incorrect_for_t6_tool`, che includono `#c:incorrect_for_allthemodium_tool` con voce `required: false` più eventuali blocchi nostri. Serve se un domani vogliamo blocchi che solo i nostri tool scavano.
- Verifica pratica attesa **[D]**:
  - T6 mina il vibranium ma non l'unobtainium;
  - T7 mina l'unobtainium ma non l'ancient stone;
  - T8 mina l'ancient stone e i blocchi lega;
  - Alloy mina tutto.

  Va testato in gioco con `/give` e un blocco di ogni tipo.
- Per il paxel o i tool multi-tipo, i blocchi "mineable" restano `minecraft:mineable/pickaxe` e simili. Anche ATM usa `mineable/pickaxe` come "tool tag" (`ToolTiers.class`).

### 5.3 Statistiche suggerite (punto di partenza)

L'idea è stare **≥ ATM nello stesso tier** (sono "versioni Spectrum" dello stesso metallo, più care da fare) ma **sotto il tier ATM successivo**. Tutto indistruttibile con `DataComponents.UNBREAKABLE`. Il tier può avere `uses` alto e nominale, come fanno sia ATM sia Spectrum. Valori **[D]** da bilanciare in gioco:

| Tier SA | speed | attackBonus | enchant | Armatura E/P/G/S | Toughness | KB |
|---|---|---|---|---|---|---|
| T6 | 15-16 | 13-14 | 85-90 | 5/10/8/5 (28) | 6 | 0.5 |
| T7 | 17 | 27-28 | 100-105 | 7/12/10/7 (36) | 10 | 0.8 |
| T8 | 19-20 | 37-40 | 125-130 | 9/14/12/9 (44) | 16 | 1.0 |
| Alloy | 35-36 | 65-68 | 200 | (ATM non ha armatura Alloy: 10/15/13/10 se la si vuole) | 18-20 | 1.0 |

Attenzione:
- Gli attributi di armatura e toughness in vanilla hanno un tetto: armatura 30 totale, toughness 20 totale. AttributeFix in ATM10 può alzarli **[D, da verificare in `config/attributefix`]**.
- Oltre circa 30 punti di armatura totali il guadagno è nullo. La differenza reale tra i tier la fanno toughness, abilità e set bonus.
- I nostri tool dovrebbero mantenere le abilità Spectrum (per esempio le modalità della workstaff o la resonance) come valore aggiunto, invece di gonfiare solo i numeri.

### 5.4 Dipendenza Gradle da Allthemodium

- **Modrinth**: il progetto `allthemodium` (id `YDA6eZ1t`) ha per NeoForge 1.21.1 **solo la versione 3.0.1** (`allthemodium-3.0.1_mc_1.21.1.jar`, version id `YHejKr3B`). Fonte: https://api.modrinth.com/v2/project/allthemodium/version?loaders=["neoforge"]&game_versions=["1.21.1"] **[V, consultato oggi]**. La 3.2.1 dell'istanza **non è su Modrinth**.
- **CurseForge**: l'istanza (`istanza:minecraftinstance.json`) riporta project id **364466** e file id **9143638** per `allthemodium-3.2.1_mc_1.21.1.jar` **[V]**. Con CurseMaven:

```groovy
repositories {
    maven { url = "https://cursemaven.com"; content { includeGroup "curse.maven" } }
}
dependencies {
    // solo a compile time / per run di test: non serve a runtime nel jar
    compileOnly "curse.maven:allthemodium-364466:9143638"
    runtimeOnly "curse.maven:allthemodium-364466:9143638"     // per avviarlo in dev
    runtimeOnly "curse.maven:geckolib-388172:8893490"          // dipendenza required di ATM (geckolib-neoforge-1.21.1-4.9.3)
}
```

  Allthemodium 3.2.1 richiede NeoForge ≥ 21.1.195 e **GeckoLib ≥ 4.7.5.1** (required). Lootr è opzionale (`jar(allthemodium):META-INF/neoforge.mods.toml`) **[V]**. Per le stesse ragioni: Spectrum è CurseForge 556967, file 9021449.
- **Serve davvero la dipendenza Java?** **[D]** Probabilmente no, se usiamo solo tag e id nelle ricette JSON e i `TagKey` costruiti per nome. Basta dichiarare in `neoforge.mods.toml` `allthemodium` come dipendenza `optional` (o `required` se l'addon è solo per ATM10) e mettere `mod_loaded` nelle ricette. La dipendenza Gradle serve solo per avviare il client di sviluppo con ATM caricato e testare.

---

## 6. Incertezze e cose da verificare

1. L'associazione esatta tra mappe di difesa e material ATM (lambda 0/1/2 assegnate ad ATM, Vib, Unob) è dedotta dall'ordine nel bytecode. È coerente con la progressione ma non è verificata in gioco.
2. Il danno effettivo dei tool è calcolato con la formula vanilla di `createAttributes`. Va controllato nel tooltip in gioco.
3. Dove si trovano esattamente i template ATM e vibranium: le loot table `arch` e `arch2` sono verificate, le strutture che le usano sono dedotte (gli NBT non sono stati letti).
4. Valori della netherite vanilla e comportamento di `neoforge:needs_netherite_tool` rispetto a `incorrect_for_diamond_tool`: da conoscenza generale, non letti dal jar di Minecraft.
5. Il cap degli attributi (armatura e toughness) con AttributeFix in ATM10 non è stato verificato.
6. Su Modrinth manca Allthemodium 3.2.1: va usato CurseMaven (file id 9143638 preso dall'istanza).
