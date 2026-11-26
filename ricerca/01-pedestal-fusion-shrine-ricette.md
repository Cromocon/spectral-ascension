# 01 — Pedestal, Fusion Shrine e formato delle ricette Spectrum

> Ricerca per **Spectral Ascension** (addon NeoForge 1.21.1 per Spectrum 1.12.9 / ATM10).
> Data: 2026-11-16.

## Fonti e convenzioni

- `jar:` = jar estratto di Spectrum 1.12.9 (`jars/spectrum-1.12.9-1.21.1-neo/`). Sono i dati che girano davvero nel modpack.
- `src:` = sorgente Spectrum ≈1.12.9 (`src/spectrum-1.12.9/src/main/java/de/dafuqs/spectrum/`). I numeri di riga si riferiscono a quella copia.
- `wiki:` = wiki clonata (`src/spectrum-wiki/`, commit `600bf36`, online su https://github.com/DaFuqs/Spectrum/wiki).
- Altri jar: `jars/SpectralDecorations-1.10.1-1.21.1-neo/`, `jars/spectrumcompat-0.1.0/`, `jars/allthemodium-3.2.1_mc_1.21.1/`.
- **[V]** = verificato leggendo codice o dati. **[D]** = dedotto, da confermare in gioco.

Dove wiki e codice non coincidono, vale il codice: la wiki in alcuni punti è rimasta alla 1.20 (per esempio `copy_nbt` e `"item"` nel result).

---

## 0. Concetti di modding in breve

| Termine | Cos'è |
|---|---|
| **Datapack** | La cartella `data/<namespace>/...` dentro un jar (o in un world). Contiene ricette, tag, advancement e loot table in JSON. Il gioco la ricarica con `/reload`. Il namespace (`spectrum`, `allthemodium`, il nostro modid) evita collisioni tra mod. |
| **Recipe / RecipeType / RecipeSerializer** | Il `RecipeType` è la "categoria" che una macchina interroga (es. `spectrum:pedestal`). Il `RecipeSerializer` è registrato con un id, che è il valore del campo `"type"` nel JSON (es. `spectrum:pedestal_shapeless`): sa trasformare il JSON in un oggetto Java e quell'oggetto in pacchetti di rete. Più serializer possono produrre ricette dello stesso RecipeType. |
| **Codec** | Lo schema dichiarativo di Mojang (libreria DataFixerUpper) che descrive come leggere e scrivere un oggetto da JSON. Leggere il codec dice con esattezza quali campi esistono, quali sono opzionali e quali valori di default hanno. |
| **Tag** | Un elenco nominato di item, blocchi, fluidi ecc. (`data/<ns>/tags/item/<nome>.json`). Nelle ricette si scrive `#ns:nome`. Il namespace `c:` è la convenzione "common" condivisa tra mod (es. `c:ingots/allthemodium`). |
| **DataComponent** | In 1.21 sostituisce l'NBT degli ItemStack. Sono dati tipizzati sull'item: `minecraft:enchantments`, `minecraft:damage`, `minecraft:custom_name`, ecc. Nel JSON compaiono in `"components": {...}`. |
| **Advancement** | Gli "obiettivi". Spectrum li usa come sistema di sblocco: una ricetta indica `required_advancement` e il giocatore non la può craftare né vedere finché non ce l'ha. Revelationary, la libreria di DaFuqs, aggiunge il trigger `revelationary:advancement_gotten` per concatenarli. |
| **Block entity** | Un blocco con stato e logica propri (inventario, tick). Pedestal e Fusion Shrine sono block entity che a ogni tick cercano una ricetta che corrisponda al loro contenuto. |

---

## 1. Pigment Pedestal

### 1.1 Classi principali [V]

| Classe | Ruolo |
|---|---|
| `src:recipe/pedestal/PedestalRecipe.java` | Classe base astratta: tier, polveri, output, xp, tempo, flag; `matches`, `canCraft`, `consumeIngredients`. |
| `src:recipe/pedestal/ShapedPedestalRecipe.java` | Ricetta shaped (pattern e key). Serializer `spectrum:pedestal`. |
| `src:recipe/pedestal/ShapelessPedestalRecipe.java` | Ricetta shapeless (`ingredients`). Serializer `spectrum:pedestal_shapeless`. |
| `src:recipe/pedestal/RawShapedPedestalRecipe.java` | Parsing di pattern e key, con match anche specchiato. Copia della `ShapedRecipePattern` vanilla, ma con `IngredientStack`. |
| `src:recipe/pedestal/PedestalRecipeTier.java` | Enum dei tier: `BASIC`, `SIMPLE`, `ADVANCED`, `COMPLEX`. |
| `src:blocks/pedestal/BuiltinPedestalVariant.java` | Varianti fisiche del blocco e tier che ciascuna abilita. |
| `src:blocks/pedestal/BuiltinGemstoneColor.java` | I 5 colori `spectrum:cyan|magenta|yellow|black|white` e la polvere associata a ciascuno. |
| `src:blocks/pedestal/PedestalBlockEntity.java` | Inventario, tick, ricerca della ricetta, verifica della struttura, upgrade. |
| `src:recipe/GatedSpectrumRecipe.java`, `GatedStackSpectrumRecipe.java`, `src:api/recipe/GatedRecipe.java` | Base comune a tutte le ricette Spectrum: `group`, `required_advancement`, `reveal_secret_advancement`, `additional_recipe_viewer_results`, gating per advancement e toast di sblocco. |
| `src:api/recipe/IngredientStack.java` | Ingrediente con quantità, filtro su component e predicati. Lo usano Pedestal, Fusion Shrine e Spirit Instiller. |

### 1.2 Tier e varianti: "Moonstone" significa `complex` [V]

Non esiste un tier chiamato `moonstone`. Il tier massimo è **`complex`**, e lo abilita solo la variante Moonstone.

| Variante blocco (`BuiltinPedestalVariant`) | Tier ricetta | Advancement che sblocca il tier (`PedestalRecipeTier`) | Slot polveri |
|---|---|---|---|
| `pedestal_basic_topaz/amethyst/citrine` | `basic` | `spectrum:place_pedestal` | C, M, Y |
| `pedestal_all_basic` (CMY) | `simple` | `spectrum:build_basic_pedestal_structure` | C, M, Y |
| `pedestal_onyx` | `advanced` | `spectrum:midgame/build_advanced_pedestal_structure` | C, M, Y, K |
| `pedestal_moonstone` | `complex` | `spectrum:lategame/build_complex_pedestal_structure` | C, M, Y, K, W |

Fonti: `src:blocks/pedestal/BuiltinPedestalVariant.java:8-14`, `src:recipe/pedestal/PedestalRecipeTier.java:20-24`, `src:registries/SpectrumAdvancements.java:90-94`.

`PedestalRecipe.canCraft` (`src:recipe/pedestal/PedestalRecipe.java:202`) consente il craft solo se valgono tutte queste condizioni:
1. il proprietario del pedestal (l'ultimo giocatore che l'ha piazzato o usato) è **online**;
2. `canPlayerCraft`, cioè il giocatore ha l'advancement del tier **e** il `required_advancement` (`:226`);
3. `pedestal.getHighestAvailableRecipeTier() >= recipe.tier`.

Il tier disponibile è `min(tier della variante, tier della struttura)` (`PedestalBlockEntity.java:821-838`). La struttura viene validata come multiblock sotto il pedestal (`getStructureTier`, `:840-870`). La struttura complex "senza engravings" vale solo `ADVANCED`. Quindi **`"tier": "complex"` richiede insieme il blocco Moonstone e la struttura complex completa**. È proprio il vincolo che vogliamo.

Caso speciale: se il `result` è un `PedestalBlockItem`, la ricetta è un **upgrade del pedestal stesso**. Il blocco viene sostituito invece di produrre un item (`PedestalRecipe.java:68`, `PedestalBlockEntity.java:397-404`). Esempio: `jar:data/spectrum/recipe/pedestal/tier3/pedestal_moonstone.json`, con tier `advanced` e un solo `moonstone_shard`.

### 1.3 Inventario e polveri di gemma [V]

`PedestalBlockEntity.java:45-52`: 16 slot in tutto.
- **0–8**: griglia 3×3.
- **9–13**: polveri. 9 = topaz (cyan), 10 = amethyst (magenta), 11 = citrine (yellow), 12 = onyx (black), 13 = moonstone (white). Mapping in `getSlotForGemstonePowder`, `:436-445`.
- **14**: Crafting Tablet. Se presente, il pedestal accetta solo la ricetta memorizzata nel tablet.
- **15**: output.

Le polveri sono un **costo separato dalla griglia**. Il campo `colors` mappa l'id del colore (registry `spectrum:gemstone_color`) a una quantità. `matches` controlla che ogni slot polvere contenga almeno quella quantità (`PedestalRecipe.java:76-88`). `consumeIngredients` le consuma divise per il modificatore dell'upgrade *Efficiency*, con arrotondamento probabilistico (`:171-180`).

Nota: `matches` controlla solo i 5 colori builtin. Un addon che registrasse un sesto `GemstoneColor` non avrebbe uno slot dove metterlo. **Per noi conta solo questo: per i tier "Onyx" e "Moonstone" usiamo `spectrum:black` e `spectrum:white`.**

### 1.4 Ciclo di crafting [V]

`PedestalBlockEntity.serverTick` (`:147-215`) e `calculateRecipe` (`:283-326`):
1. Al cambio di inventario cerca una ricetta con `RecipeManager.getRecipeFor(SpectrumRecipeTypes.PEDESTAL, ...)`. Se non ne trova e la config lo permette, cerca una ricetta vanilla di crafting.
2. Il craft parte **solo con un segnale redstone o con il pulsante della GUI** (`shouldCraft`). Commento nel codice: "only craft when there is redstone power".
3. Il tempo totale è `ceil(time / modificatore Speed)`.
4. Alla fine: consuma ingredienti e polveri, applica *Yield* (se non è `disable_yield_upgrades`) **limitato a `maxStackSize`**, accumula xp × modificatore *Experience* (rilasciata quando l'output viene ritirato) e attiva i criteri `spectrum:pedestal_crafting`.

Conseguenza: per oggetti non impilabili come gli attrezzi, *Yield* non può duplicare il risultato, perché il conteggio viene limitato a 1 (`:374-379`). Mettere `disable_yield_upgrades: true` resta comunque buona pratica.

Il match **shaped** usa `RawShapedPedestalRecipe.matches`, che prova anche la versione specchiata. Il match **shapeless** usa `matchIngredientStacksExclusively`: il numero di stack non vuoti nella griglia deve essere uguale al numero di ingredienti (`GatedStackSpectrumRecipe.java`).

`getRecipeFor` restituisce la **prima** ricetta che corrisponde. Se due ricette accettano lo stesso input, una oscura l'altra **[D]**: evitare pattern identici a quelli di Spectrum o ATM.

### 1.5 Formato JSON completo (dal codec) [V]

Codec shaped: `src:recipe/pedestal/ShapedPedestalRecipe.java:91-104`. Codec shapeless: `ShapelessPedestalRecipe.java:57-70`. Pattern e key: `RawShapedPedestalRecipe.java:200-219`.

| Campo | Tipo | Obblig. | Default | Note |
|---|---|---|---|---|
| `type` | id | sì | — | `spectrum:pedestal` (shaped) oppure `spectrum:pedestal_shapeless` |
| `group` | string | no | `""` | Raggruppa i toast di sblocco |
| `required_advancement` | id advancement | no | nessuno | Senza questo campo basta l'advancement del tier |
| `reveal_secret_advancement` | id advancement | no | nessuno | La ricetta appare come "segreta" nei viewer finché il giocatore non ha l'advancement |
| `additional_recipe_viewer_results` | lista ItemStack | no | `[]` | Output aggiuntivi mostrati solo in JEI/EMI |
| `tier` | `basic|simple|advanced|complex` | no | **`basic`** | |
| `pattern` + `key` (solo shaped) | lista di stringhe + mappa char→IngredientStack | sì | — | Righe di uguale larghezza; `' '` è riservato; il padding viene rimosso. Errore se nella key ci sono simboli inutilizzati |
| `ingredients` (solo shapeless) | lista IngredientStack | sì | — | |
| `colors` | mappa `spectrum:<colore>` → int | **sì** | — | Può essere `{}` (vedi SpectralDecorations). Gli zeri sono ammessi |
| `result` | ItemStack (`id`, `count`, `components`) | sì | — | Formato 1.21: `"id"`, non `"item"` (la wiki ha esempi ancora in formato 1.20) |
| `experience` | float | no | `0` | |
| `time` | int (tick) | no | `200` | |
| `skip_recipe_remainders` | bool | no | `false` | Se true, non restituisce i "remainder" (secchi vuoti ecc.) |
| `disable_yield_upgrades` | bool | no | `false` | Disattiva *Yield* sull'output. Nel codice il campo si chiama `noBenefitsFromYieldUpgrades`; **il nome JSON è `disable_yield_upgrades`, non `no_benefits_from_upgrades`** |
| `neoforge:conditions` | lista | no | — | Gestito da NeoForge, non da Spectrum (vedi §5) |

**IngredientStack** (`src:api/recipe/IngredientStack.java:24-38`) accetta due forme:
- stringa: `"minecraft:diamond"` oppure `"#c:gems/diamond"`, con count 1;
- oggetto: i campi di un `Ingredient` NeoForge (`"item"`, `"tag"` o un custom ingredient con `"type"`), più `count` (default 1), `components` (filtro esatto sui DataComponent), `predicates` (item sub-predicate vanilla, es. `minecraft:stored_enchantments`, `minecraft:enchantments`) e `preview_components` (component mostrati solo nell'anteprima dei viewer).

Nel pedestal **anche le celle della key** sono IngredientStack. Si può quindi chiedere, ad esempio, `{"item": "...", "count": 4}` in una singola cella: `consumeIngredients` ne consuma `count`.

Template completo shaped, con tutti i campi:

```json
{
  "type": "spectrum:pedestal",
  "group": "esempio",
  "required_advancement": "modid:unlocks/qualcosa",
  "reveal_secret_advancement": "modid:hidden/qualcosa",
  "additional_recipe_viewer_results": [],
  "tier": "complex",
  "pattern": ["ABA", " C ", " C "],
  "key": {
    "A": "minecraft:diamond",
    "B": { "tag": "c:ingots/gold", "count": 2 },
    "C": { "item": "minecraft:stick" }
  },
  "colors": {
    "spectrum:cyan": 4, "spectrum:magenta": 4, "spectrum:yellow": 4,
    "spectrum:black": 0, "spectrum:white": 2
  },
  "result": { "id": "modid:item", "count": 1, "components": { "minecraft:enchantments": { "levels": { "minecraft:efficiency": 5 } } } },
  "experience": 2.0,
  "time": 600,
  "skip_recipe_remainders": false,
  "disable_yield_upgrades": true
}
```

Nota sul formato `enchantments`. I JSON di Spectrum scrivono `"enchantments": {"spectrum:razing": 3}`, cioè la forma compatta senza `levels`. Il codec vanilla `ItemEnchantments` in 1.21.1 accetta anche questa forma (codec con alternativa) **[D, dedotto dal fatto che Spectrum la usa e funziona]**. Il prefisso `minecraft:` è facoltativo.

### 1.6 Esempi reali (jar)

**a) Dragonrending Pickaxe**: tier complex, shaped, risultato pre-incantato. File `jar:data/spectrum/recipe/pedestal/tier4/dragonrending_pickaxe.json`:

```json
{
  "type": "spectrum:pedestal",
  "time": 600,
  "tier": "complex",
  "colors": { "spectrum:cyan": 0, "spectrum:magenta": 4, "spectrum:yellow": 8, "spectrum:white": 4, "spectrum:black": 0 },
  "experience": 2,
  "pattern": ["MPM", " S ", " S "],
  "key": { "S": "minecraft:smooth_basalt", "M": "spectrum:dragonbone_chunk", "P": "#spectrum:resplendent_feathers" },
  "result": { "id": "spectrum:dragonrending_pickaxe", "components": { "enchantments": { "spectrum:razing": 3 } }, "count": 1 },
  "required_advancement": "spectrum:unlocks/equipment/dragonrending_pickaxe"
}
```
Struttura simile in `tier4/resonant_pickaxe.json` (Moonstone shard e Resonance shard, `spectrum:resonance` 1).

**b) Elytra**: tier complex, molta polvere moonstone. File `jar:data/spectrum/recipe/pedestal/tier4/vanilla/elytra.json`: `time` 1200, `colors` cyan 2 e white 8, `experience` 4.0, pattern `CGC/MPM/MPM`, `required_advancement: spectrum:hidden/collect_paltaeria_gem`.

**c) Yield Upgrade II**: shapeless, complex. File `jar:data/spectrum/recipe/pedestal/tier4/upgrade_yield2.json`:
```json
{
  "type": "spectrum:pedestal_shapeless",
  "time": 6000, "tier": "complex",
  "colors": { "spectrum:cyan": 0, "spectrum:magenta": 0, "spectrum:yellow": 0, "spectrum:black": 0, "spectrum:white": 32 },
  "experience": 4.0,
  "pattern": ["S", "U"],
  "ingredients": ["spectrum:upgrade_yield", "spectrum:pure_diamond"],
  "result": { "id": "spectrum:upgrade_yield2", "count": 1 },
  "required_advancement": "spectrum:unlocks/upgrades/upgrade_yield2"
}
```
Contiene un campo `pattern` che il codec shapeless non legge: viene ignorato in silenzio. Questo dimostra che i campi sconosciuti non causano errori **[V, il MapCodec non è strict]**. Per noi significa che un refuso nel nome di un campo opzionale **non** produce errori: il campo viene semplicemente ignorato. Attenzione.

**Equipaggiamento bedrock e malachite.** Nel jar **non** esistono ricette pedestal per bedrock o malachite. Tutti gli strumenti bedrock (`fusion_shrine/bedrock/*.json`) e i quattro oggetti malachite e glass crest (`fusion_shrine/malachite/*.json`) sono ricette **Fusion Shrine** [V, grep su `"id": "spectrum:(malachite|bedrock|glass_crest)`]. Nel pedestal ci sono solo cose come `tier1/arrows/malachite.json`, `tier4/glass_ampoules/malachite.json` e i mattoni.

---

## 2. Fusion Shrine

### 2.1 Funzionamento [V]

Classi: `src:recipe/fusion_shrine/FusionShrineRecipe.java`, `src:blocks/fusion_shrine/FusionShrineBlockEntity.java`, `FusionShrineBlock.java`.

- **Gli input stanno dentro lo shrine**: inventario interno di **7 slot** (`FusionShrineBlockEntity.java:39`), riempito con click destro o hopper. **Non** usa pedestal laterali: quelli sono dell'Enchanter (Item Bowl) e dello Spirit Instiller.
- **Fluido nel blocco**: un tank da **1 secchio** (`:49`, `FluidType.BUCKET_VOLUME`). `matches` richiede che il fluido corrisponda e che il tank sia **pieno** (`FusionShrineRecipe.java:79-90`). Se la ricetta non specifica `fluid`, il tank deve essere vuoto **[V nella wiki; codice: `FluidIngredient.empty()`]**. Fluidi usati nel jar: `spectrum:liquid_crystal` (40 ricette), `spectrum:midnight_solution` (17), `minecraft:lava` (8), `minecraft:water` (7), `spectrum:dragonrot` (1).
- **Match degli ingredienti**: `matchIngredientStacksExclusively`. Il numero di stack non vuoti deve essere uguale al numero di ingredienti (massimo 7, `listOf(0, 7)`) e ogni ingrediente va abbinato a uno stack diverso. Gli stack devono avere almeno `count` elementi.
- **Ogni 60 tick** (`FusionShrineBlockEntity.java:136-150`) ricontrolla:
  - `canPlayerCraft`: l'advancement di tipo `spectrum:build_fusion_shrine` (UNLOCK_IDENTIFIER, `FusionShrineRecipe.java:31`) **e** `required_advancement`, entrambi sul proprietario, che deve essere online;
  - `world_conditions`;
  - la struttura multiblock (`FusionShrineBlock.verifyStructure`);
  - **l'accesso al cielo**: nessun blocco opaco sopra (`verifySkyAccess`, `FusionShrineBlock.java:74`).

  Se manca la struttura o il cielo, lo shrine **sparpaglia il contenuto** a terra. Se mancano solo le condizioni, si limita ad azzerare il progresso.
- **Nessun concetto di tier**: la Fusion Shrine non sa nulla dei pedestal. **Per richiedere "il pedestal Moonstone" bisogna usare `required_advancement`** (vedi §6).
- **World effects** (`getWorldEffectForTick`, `:157-180`): `start` al tick 1, `finish` all'ultimo tick, `during` suddiviso in parti uguali sulla durata del craft.
- **Craft** (`:203-255`): calcola quante ricette può fare in un colpo, limitato dal `maxStackSize` dell'output (per un attrezzo vale 1). Poi decrementa gli ingredienti, considerando *Efficiency* e i remainder. Infine fa comparire l'output come entità sopra lo shrine e rilascia xp.

### 2.2 `copy_components` [V]

Il campo si chiamava `copy_nbt` in 1.20 e la wiki lo riporta ancora così. In 1.21 è **`copy_components`**. Codice: `FusionShrineRecipe.java:250-252` e `GatedSpectrumRecipe.copyComponents` (`src:recipe/GatedSpectrumRecipe.java:71-84`).

1. La sorgente è **lo stack che corrisponde al PRIMO ingrediente della lista** (`firstStack`, `:211-217`). **L'oggetto da preservare va quindi messo per primo in `ingredients`.**
2. `transmuteCopy(outputItem, count)` copia **tutti i DataComponent modificati** dell'input sull'output: incantesimi, nome, danno, component custom.
3. Gli incantesimi definiti nel `result` della ricetta sostituiscono quelli dell'input. Poi gli incantesimi dell'input vengono **ri-aggiunti o potenziati** con `SpectrumEnchantmentHelper.addOrUpgradeEnchantment`. In pratica il risultato ha l'unione dei due insiemi, e per ogni incantesimo si tiene il livello più alto, con possibilità di superare il massimo **[D sul comportamento esatto di addOrUpgrade; il collega degli incantesimi lo verifica]**.

Due osservazioni dal codice, da testare in gioco:
- **Danno.** Viene copiato anche `minecraft:damage`, quindi un workstaff usurato resta usurato dopo l'upgrade. Se il nuovo item ha una durabilità massima diversa, il valore di danno resta assoluto.
- **Efficiency e duplicazione [D].** In `decrementIngredients` l'esenzione da *Efficiency* per l'oggetto trasformato vale per lo **slot 0 dello shrine** (`i == 0 && copyComponents`, `:263`), non per "il primo ingrediente". Se l'attrezzo sta in un altro slot e lo shrine ha upgrade Efficiency, potrebbe non essere consumato. Nello stesso modo *Yield* può generare **più di un attrezzo**, perché `spawnCraftingResultAndXP` non limita a `maxStackSize` (`:281-288`).

  **Raccomandazione: `"disable_yield_upgrades": true` su tutte le nostre ricette di upgrade.** Così si disattivano sia Yield sia Efficiency.

### 2.3 Formato JSON completo (dal codec) [V]

Codec: `src:recipe/fusion_shrine/FusionShrineRecipe.java:303-319`.

| Campo | Tipo | Obblig. | Default | Note |
|---|---|---|---|---|
| `type` | id | sì | — | `spectrum:fusion_shrine` |
| `group`, `required_advancement`, `reveal_secret_advancement`, `additional_recipe_viewer_results` | come per il pedestal | no | | |
| `ingredients` | lista IngredientStack (0–7) | sì | — | |
| `fluid` | FluidIngredient NeoForge: `{"fluid": "ns:id"}` oppure `{"tag": "ns:tag"}` | no | vuoto (tank vuoto) | `wiki:Fluid-Ingredients.md` |
| `result` | ItemStack | no | vuoto | Può mancare: le ricette meteo hanno solo effetti |
| `experience` | float | no | `0` | |
| `time` | int | no | `200` | |
| `disable_yield_upgrades` | bool | no | `false` | Disattiva Yield **ed** Efficiency |
| `play_crafting_finished_effects` | bool | no | **`true`** | |
| `copy_components` | bool | no | `false` | |
| `world_conditions` | oggetto **o** lista di oggetti | no | `[]` | **Lista = OR** (`anyMatch`, `:135-139`); i campi dentro un oggetto sono in **AND** (`WorldConditionsPredicate.test`, `:76-82`). La wiki dice "tutte devono essere vere", ma vale solo dentro un singolo oggetto |
| `effects` | `{start, during[], finish}` | no | start `nothing`, during `[]`, finish `single_visual_explosion_on_shrine` | Se presente, `start` e `finish` sono obbligatori |
| `description` | Text Component | no | vuoto | Mostrata nei recipe viewer |

**`world_conditions`** (`src:api/predicate/location/WorldConditionsPredicate.java:45-58`):
- `moon_phase`: `full_moon`, `waning_gibbous`, `third_quarter`, `waning_crescent`, `new_moon`, `waxing_crescent`, `first_quarter`, `waxing_gibbous`;
- `time_of_day`: un nome (`day`, `night`, `noon`, `sunset`, `midnight`, `sunrise`), un intervallo MinMax `{min, max}` oppure un intero;
- `weather`: `clear_sky`, `rain`, `strict_rain`, `thunder`, `not_thunder`;
- `command`: stringa di comando, vera se il comando restituisce più di 0;
- `biomes`, `structures`: id, lista o `#tag`;
- `dimension`: es. `minecraft:the_nether`;
- `light`, `block`, `fluid`: predicati vanilla di LocationPredicate;
- `smokey`, `can_see_sky`: bool.

**`effects`**: sono stringhe. Id registrati in `src:registries/SpectrumWorldEffects.java`: `nothing`, `weather_clear`, `weather_rain`, `weather_thunder`, `weather_rain_short`, `weather_thunder_short`, `lightning_on_shrine`, `lightning_around_shrine`, `visual_explosions_on_shrine`, `single_visual_explosion_on_shrine`, `maybe_place_midnight_solution`, `place_midnight_solution`, `explosions_around_shrine`, `explosions_and_lightning_around_shrine`, `play_glass_breaking_sound`, `legendary_tool_craft`, `ridiculously_squeaky_fart`. Una stringa che inizia con `/` viene **eseguita come comando** (`WorldEffect.fromString`). Gli id senza namespace sono interpretati come `spectrum:`.

> Nel brief i campi erano chiamati `start_world_effect`, `during_world_effects` e `finish_world_effect`. **In 1.21 non esistono**: sono raggruppati in un unico oggetto `effects`.

### 2.4 Esempi reali (jar)

**a) Bedrock Pickaxe**: preincantato, copy_components, midnight solution. File `jar:data/spectrum/recipe/fusion_shrine/bedrock/pickaxe.json`:
```json
{
  "type": "spectrum:fusion_shrine",
  "group": "bedrock_tools",
  "time": 1200, "experience": 16.0,
  "fluid": { "fluid": "spectrum:midnight_solution" },
  "copy_components": true,
  "ingredients": [
    "minecraft:golden_pickaxe",
    { "item": "spectrum:bedrock_dust", "count": 3 },
    { "item": "spectrum:black_pigment", "count": 3 },
    { "item": "spectrum:onyx_powder", "count": 3 },
    { "item": "spectrum:stratine_gem", "count": 1 },
    { "item": "spectrum:stratine_fragments", "count": 3 }
  ],
  "result": { "id": "spectrum:bedrock_pickaxe", "components": { "enchantments": { "minecraft:silk_touch": 1 } } },
  "required_advancement": "spectrum:unlocks/equipment/bedrock_tools"
}
```
L'armatura segue lo stesso schema (`bedrock/chestplate.json`: golden_chestplate, 8× dust, pigment e onyx powder, stratine gem, rose bush, risultato con `protection` 5).

**b) Malachite Workstaff**: fonde tre attrezzi diamond. File `jar:data/spectrum/recipe/fusion_shrine/malachite/malachite_workstaff.json`: fluido `spectrum:liquid_crystal`, `copy_components: true`, ingredienti `diamond_pickaxe`, `diamond_axe`, `diamond_shovel`, 8× `pure_malachite`, 8× `pure_quartz`, 4× `paltaeria_fragments`, 8× `moonstone_powder`. Risultato `spectrum:malachite_workstaff` con `fortune` 4. Advancement `spectrum:unlocks/malachite/malachite_tools`. Vengono copiati solo i component del pickaxe, che è il primo ingrediente.

**c) Glass Crest Workstaff**: upgrade di un oggetto Spectrum, con predicato sul libro. È il modello più vicino ai nostri upgrade. File `jar:data/spectrum/recipe/fusion_shrine/malachite/glass_crest_workstaff.json`:
```json
{
  "type": "spectrum:fusion_shrine",
  "time": 1200, "experience": 16.0,
  "fluid": { "fluid": "spectrum:liquid_crystal" },
  "copy_components": true,
  "ingredients": [
    "spectrum:malachite_workstaff",
    "spectrum:moonstone_core",
    { "item": "spectrum:pure_diamond", "count": 8 },
    {
      "item": "minecraft:enchanted_book",
      "predicates": { "minecraft:stored_enchantments": [ { "enchantments": "spectrum:resonance", "levels": { "min": 1 } } ] },
      "preview_components": { "minecraft:stored_enchantments": { "levels": { "spectrum:resonance": 1 } } }
    },
    { "item": "spectrum:white_pigment", "count": 16 }
  ],
  "result": { "id": "spectrum:glass_crest_workstaff" },
  "required_advancement": "spectrum:unlocks/malachite/glass_crest_workstaff"
}
```
Il malachite workstaff, con il suo Fortune IV e gli altri incantesimi, viene trasformato e i suoi component sono preservati.

**d) Onyx Shard**: condizioni ed effetti. File `jar:data/spectrum/recipe/fusion_shrine/onyx_shard.json`: lava; `world_conditions: {"time_of_day": "midnight", "moon_phase": "new_moon"}`; `effects: {"start": "weather_thunder_short", "during": ["visual_explosions_on_shrine", "nothing", "visual_explosions_on_shrine"], "finish": "lightning_on_shrine"}`; `description` translatable. Anche `dreamflayer.json` (notte di luna piena, `finish: legendary_tool_craft`) è un buon modello per un'arma "leggendaria".

---

## 3. Altre macchine di crafting (panoramica)

Tutte condividono i campi gated (`group`, `required_advancement`, `reveal_secret_advancement`, `additional_recipe_viewer_results`).

- **Enchanter** (`spectrum:enchanter`, `src:recipe/enchanter/EnchanterRecipe.java:140-151`). È un blocco centrale con 8 Item Bowl attorno. Campi: `ingredients` (lista di `Ingredient` vanilla, senza count: il primo va al centro, gli altri 8 nelle bowl), `result`, `time` (200), `required_experience` (0, xp presa da un Knowledge Gem / Experience storage), `disable_yield_and_efficiency_upgrades`, `copy_components`. Esempio: `jar:.../enchanter/enchanted_golden_apple.json`. Serve per item "magici" e libri; i dettagli li copre il collega.
- **Enchantment Upgrade** (`spectrum:enchantment_upgrade`, `EnchantmentUpgradeRecipe.java:209-216`). Si fa nell'Enchanter e porta un incantesimo oltre il livello massimo. Campi: `enchantment` e `levels: [{ingredient, ingredient_count_per_bowl, experience}, ...]`. Esempio: `enchantment_upgrade/spectrum/treasure_hunter.json`.
- **Spirit Instiller** (`spectrum:spirit_instiller`, `SpiritInstillerRecipe.java:196-207`). Ha un centro e 2 Item Bowl. Campi: `center_ingredient`, `ingredient1`, `ingredient2` (IngredientStack), `result` (STRICT), `time` (200), `experience` (**1.0**), `disable_yield_and_efficiency_upgrades`, `copy_components`. Esempio: `spirit_instiller/ominous_sapling.json`.
- **Anvil Crushing** (`spectrum:anvil_crushing`, `AnvilCrushingRecipe.java:122-133`). Si fa lasciando cadere un'incudine su un item. Campi: `ingredient`, `result`, `crushedItemsPerPointOfDamage`, `experience` (obbligatorio), `particleEffectIdentifier`, `particleCount` (1), `soundEventIdentifier` (obbligatorio). Esempio: `anvil_crushing/stratine_fragments.json`.

Per i nostri set queste macchine sono escluse dal vincolo "solo Pedestal e Fusion Shrine". Enchanter ed Enchantment Upgrade potrebbero però servire per eventuali incantesimi custom.

---

## 4. Come un addon aggiunge ricette

### 4.1 Id registrati [V]

`src:registries/SpectrumRecipeTypes.java:26-42` e `SpectrumRecipeSerializers.java:35-102`. Tutti nel namespace `spectrum`.

| RecipeType (interrogato dal blocco) | Serializer (`"type"` nel JSON) |
|---|---|
| `spectrum:pedestal` | `spectrum:pedestal` (shaped), `spectrum:pedestal_shapeless`, `spectrum:pedestal_star_candy` (dinamica, interna) |
| `spectrum:fusion_shrine` | `spectrum:fusion_shrine`, `spectrum:shooting_star_hardening` (dinamica) |
| `spectrum:enchanter` | `spectrum:enchanter` |
| `spectrum:enchantment_upgrade` | `spectrum:enchantment_upgrade` |
| `spectrum:spirit_instiller` | `spectrum:spirit_instiller` più varianti `spirit_instiller_*` dinamiche |
| `spectrum:anvil_crushing` | `spectrum:anvil_crushing` |

### 4.2 (a) JSON nel datapack dell'addon [V, approccio consigliato]

Il percorso del file **non** influisce sul tipo di ricetta. Conta solo il campo `"type"`, e il percorso diventa l'id della ricetta. Convenzione: `src/main/resources/data/spectral_ascension/recipe/pedestal/allthemodium/pickaxe.json`, che ha id `spectral_ascension:pedestal/allthemodium/pickaxe`.

SpectralDecorations, un addon ufficiale, fa esattamente così (`jars/SpectralDecorations-.../data/spectral_decorations/recipe/pedestal/tier1/...`, con `"type": "spectrum:pedestal"`). spectrumcompat invece scrive ricette **nel namespace `spectrum`** (`jars/spectrumcompat-0.1.0/data/spectrum/recipe/enchanter/tunneling.json`): funziona, ma rischia di sovrascrivere file di Spectrum. **Da evitare.**

Le ricette sono "special" (`isSpecial() = true`, `GatedSpectrumRecipe.java`): non compaiono nel recipe book vanilla.

### 4.3 (b) Datagen [V]

**Spectrum 1.12.9 non ha datagen attivo né builder pubblici per le sue ricette.** I provider sono in `src-root:disabled/datagen/` (`SpectrumRecipeProvider extends FabricRecipeProvider`, residuo del port da Fabric, non compilato), e `build.gradle` non definisce un run `data`. Le ricette Spectrum sono JSON scritti a mano, aiutati da script PowerShell in `src-root:raw/*.ps1`.

Opzioni per noi:
1. **JSON a mano**, con una o due ricette per item: per circa 4 tier × 10 item è gestibile. Consigliato all'inizio.
2. **Datagen NeoForge proprio [D].** Un `RecipeProvider` dell'addon può costruire istanze di `ShapedPedestalRecipe` o `FusionShrineRecipe` (i costruttori sono pubblici, e `RawShapedPedestalRecipe.create(key, pattern)` conserva i `Data` necessari per riserializzare) e passarle a `RecipeOutput.accept(id, recipe, null)`. Vanilla le serializza con il codec del serializer. Rischio: il costruttore chiama `registerInToastManager` se l'ambiente è client. Più semplice e sicuro scrivere un piccolo builder che genera `JsonObject`, oppure usare uno script di template.

### 4.4 (c) Recipe viewer e guidebook

- **EMI** e **REI** sono integrati in Spectrum (`src:compat/emi/SpectrumEmiPlugin.java:129-131`: `addAll(registry, SpectrumRecipeTypes.PEDESTAL, ...)`). Iterano **tutte** le ricette del RecipeType, qualunque sia il namespace, quindi le nostre compaiono automaticamente [V]. Una ricetta risulta "locked" finché manca `required_advancement` (`GatedSpectrumEmiRecipe.isUnlocked`, `:44`). Le ricette con `reveal_secret_advancement` mostrano un testo "segreto" con un hint opzionale, preso dalla lang key `recipe.<ns>.<path con i punti>.hint` (`GatedRecipe.getSecretHintText`).
- **JEI**: Spectrum non contiene un plugin JEI; in ATM10 se ne occupa la mod separata *SpectrumJEI*, il cui jar non è disponibile qui. **[D]** Quasi certamente anche lei legge le ricette per RecipeType dal RecipeManager, quindi le nostre ricette dovrebbero comparire senza lavoro aggiuntivo. Da verificare in gioco.
- **Toast di sblocco**: ogni ricetta gated si registra nell'`UnlockToastManager`. Quando il giocatore ottiene `required_advancement` appare un toast "nuova ricetta Pedestal/Fusion". `group` raggruppa più sblocchi in un solo toast. La wiki indica la chiave `recipeGroup.spectrum.<group>` per la localizzazione [wiki, non verificato nel codice].
- **Guidebook** (Modonomicon, "Colorful World"). Le pagine ricetta referenziano la ricetta **per id**: es. `{"type": "spectrum:fusion_shrine_crafting", "recipe_id": "spectrum:fusion_shrine/malachite/malachite_workstaff", "title": ..., "text": ...}` (`jar:data/spectrum/modonomicon/books/guidebook/entries/dimension/malachite_tools.json`). Tipi di pagina: `spectrum:pedestal_crafting`, `spectrum:fusion_shrine_crafting`, `spectrum:enchanter_crafting`, ecc. (`wiki:Modonomicon-Recipe-Pages.md`; la wiki scrive `recipe`, ma i dati reali usano **`recipe_id`**). Le pagine restano nascoste finché la ricetta non è sbloccata, a meno di `"skip_recipe_unlock_check": true`.

  Un addon può aggiungere una propria entry al libro di Spectrum, o creare un proprio libro, puntando ai propri `recipe_id`. Questo è un tema per un altro file di ricerca.

---

## 5. Tag e riferimenti a item di altre mod

### 5.1 Tag Spectrum utili [V, `jar:data/spectrum/tags/item/`]

| Tag | Contenuto |
|---|---|
| `spectrum:gemstone_powders` | amethyst, citrine, topaz, onyx, moonstone powder |
| `spectrum:gemstone_shards` | amethyst_shard (vanilla), citrine, topaz, moonstone, onyx shard |
| `spectrum:workstaffs` | malachite_workstaff, glass_crest_workstaff |
| `spectrum:greatswords` | malachite / glass_crest ultra_greatsword |
| `spectrum:bidents` | malachite_bident, ferocious e fractal glass_crest_bident |
| `spectrum:crossbows` | malachite, bedrock, glass_crest crossbow |
| `spectrum:malachite_equipment`, `spectrum:glass_crest_equipment`, `spectrum:bedrock_equipment`, `spectrum:bedrock_armor`, `spectrum:gemstone_armor` | i set |
| `spectrum:pedestals` | le 6 varianti di pedestal |
| `spectrum:pure_resources` | pure_* (con voci opzionali `required: false` per AE2, Create ecc.) |
| `c:gems` (contiene `#spectrum:gemstone_shards`), `c:dusts` (polveri, bedrock_dust, stardust…) | aggiunti da Spectrum in `jar:data/c/tags/item/` |

Id singoli ricorrenti: `spectrum:topaz_powder`, `amethyst_powder`, `citrine_powder`, `onyx_powder`, `moonstone_powder`; `spectrum:moonstone_core`; `spectrum:pure_diamond`; `spectrum:*_pigment`; `spectrum:liquid_crystal` e `spectrum:midnight_solution` (fluidi).

### 5.2 Allthemodium [V, `jars/allthemodium-3.2.1_mc_1.21.1/`]

- Id item verificati nella lang `assets/allthemodium/lang/en_us.json`: `allthemodium:allthemodium_ingot`, `allthemodium:allthemodium_plate`, `allthemodium:allthemodium_rod`, `allthemodium:allthemodium_pickaxe`, `allthemodium:vibranium_pickaxe`. Attenzione all'**`allthemodium:alloy_pick`**: si chiama `pick`, non `pickaxe`.
- Tag `c:` forniti da ATM: `c:ingots/{allthemodium, vibranium, unobtainium, vibranium_allthemodium_alloy, unobtainium_allthemodium_alloy, unobtainium_vibranium_alloy}`, `c:plates/{allthemodium, vibranium, unobtainium}`, `c:rods/...`, `c:storage_blocks/...`, `c:nuggets/...`, `c:dusts/...`, `c:gears/...` (`data/c/tags/item/`).

### 5.3 Tag o id? [V e D]

- **Preferire i tag `c:`** per i materiali ATM (`"#c:ingots/allthemodium"`): sono robusti a rinomine e permettono a modpack o KubeJS di sostituire. Usare l'id diretto quando serve proprio quell'oggetto, ad esempio un attrezzo ATM da trasformare con `copy_components`.
- Se una ricetta referenzia un item di una mod assente, il codec fallisce e la ricetta viene **scartata con un errore nel log**. Per le dipendenze opzionali si usa la condizione NeoForge a livello di root della ricetta. Esempio reale da `jars/spectrumcompat-0.1.0/data/spectrum/recipe/enchanter/tunneling.json`:
  ```json
  "neoforge:conditions": [ { "type": "neoforge:mod_loaded", "modid": "betterarcheology" } ]
  ```
  Spectrum aggiunge anche la condizione `spectrum:integration_pack_active` (`src:registries/SpectrumResourceConditions.java:21`), usata da SpectralDecorations.
- Allthemodium sarà una **dipendenza obbligatoria** dell'addon (nel `neoforge.mods.toml`), quindi le condizioni `mod_loaded` per ATM sono superflue. Possono servire per eventuali ricette verso altre mod.
- Nei tag si può marcare una voce come opzionale con `{"id": "mod:item", "required": false}` (stessa tecnica di `spectrum:pure_resources`).

---

## 6. Implicazioni per Spectral Ascension

### 6.1 Regole che derivano dalla ricerca

1. **Pedestal**: tutte le ricette T6, T7, T8 e Alloy usano `"tier": "complex"`. Questo impone a livello di codice il blocco Moonstone, la struttura complex completa e l'advancement `spectrum:lategame/build_complex_pedestal_structure`.
2. **Colori per tier**, dal nostro tema: T6 → `cyan/magenta/yellow`; T7 → `black`; T8 → `white`; Alloy → tutti e cinque. Su un pedestal complex tutti e 5 gli slot sono disponibili.
   - **Decisione dell'utente (applicata il 2026-11-18): il T8 usa tutti e 5 i colori**, ciano, magenta, giallo, nero e bianco, sia sul Pedestal (8 polveri per colore) sia alla Fusion Shrine (polveri di topazio, ametista, citrino, onice e moonstone). Alla Fusion Shrine, per stare nei 7 slot (pezzo T7 + 6), l'unobtainium è passato da 2 blocchi + 8 lingotti a 3 blocchi. In `tools/gen_data.py` (`TIERS["t8"]["colors"]`, `FUSION["t8"]`); verificato dal GameTest `t8_five_colors`. Da rivedere quando arrivano le liste T7/T8 dell'utente.
3. **Fusion Shrine**: non conosce i tier. Per imporre "Moonstone" creiamo i **nostri advancement di sblocco** (es. `spectral_ascension:unlocks/allthemodium_tier`) e li mettiamo come `required_advancement`. Il pattern è lo stesso di `jar:data/spectrum/advancement/unlocks/malachite/malachite_tools.json`, cioè criteri `revelationary:advancement_gotten` concatenati, più un `minecraft:inventory_changed` sul materiale. In questo modo la stessa catena di advancement gating fa da sblocco sia al pedestal sia alla fusion. Usarli anche come `required_advancement` nelle ricette pedestal dà toast e guidebook coerenti.
4. Negli upgrade con `copy_components`, **l'oggetto Spectrum da potenziare va messo per primo** in `ingredients`, e conviene **`"disable_yield_upgrades": true`** (vedi §2.2).
5. Gli incantesimi "di serie" del tier vanno in `result.components.enchantments`. Con `copy_components` si fondono con quelli dell'input.
6. Le nostre ricette vanno nel namespace `spectral_ascension`, mai in `spectrum`.
7. I campi sbagliati vengono ignorati senza errori: serve un controllo di validità sui nostri JSON (script o test di datagen).

### 6.2 Advancement di sblocco d'esempio [D: formato copiato da Spectrum e SpectralDecorations]

`data/spectral_ascension/advancement/unlocks/allthemodium_tier.json`:
```json
{
  "criteria": {
    "complex_pedestal": {
      "trigger": "revelationary:advancement_gotten",
      "conditions": { "advancement_identifier": "spectrum:lategame/build_complex_pedestal_structure" }
    },
    "has_allthemodium": {
      "trigger": "minecraft:inventory_changed",
      "conditions": { "items": [ { "items": "#c:ingots/allthemodium" } ] }
    }
  }
}
```
Senza `display` l'advancement è invisibile, come gli `unlocks/*` di Spectrum. Il formato del predicato `inventory_changed` in 1.21.1 (`"items": "#tag"`) è **da verificare**: SpectralDecorations usa `{"tag": ...}`, che è il formato di 1.20 e potrebbe non validare in 1.21.

### 6.3 Pedestal: piccone T6 (Allthemodium, CMY)

`data/spectral_ascension/recipe/pedestal/allthemodium/pickaxe.json`:
```json
{
  "type": "spectrum:pedestal",
  "group": "spectral_ascension_allthemodium_tools",
  "tier": "complex",
  "time": 800,
  "experience": 8.0,
  "pattern": [
    "IGI",
    " R ",
    " R "
  ],
  "key": {
    "I": { "tag": "c:ingots/allthemodium", "count": 2 },
    "G": "spectrum:moonstone_core",
    "R": "#c:rods/allthemodium"
  },
  "colors": {
    "spectrum:cyan": 8,
    "spectrum:magenta": 8,
    "spectrum:yellow": 8,
    "spectrum:black": 0,
    "spectrum:white": 0
  },
  "result": {
    "id": "spectral_ascension:allthemodium_prismatic_pickaxe",
    "count": 1,
    "components": {
      "minecraft:enchantments": { "levels": { "minecraft:fortune": 4 } }
    }
  },
  "required_advancement": "spectral_ascension:unlocks/allthemodium_tier",
  "disable_yield_upgrades": true
}
```
Cose da verificare:
- l'id del nostro item (`spectral_ascension:allthemodium_prismatic_pickaxe` è un placeholder);
- i tag `c:ingots/allthemodium` e `c:rods/allthemodium` esistono nel jar ATM [V];
- le quantità sono di bilanciamento, non vincolate dal codice;
- la variante con l'attrezzo ATM come input (`allthemodium:allthemodium_pickaxe`) richiederebbe la fusion, perché il pedestal non ha `copy_components`.

### 6.4 Fusion Shrine: upgrade del Workstaff a T6

`data/spectral_ascension/recipe/fusion_shrine/allthemodium/workstaff.json`:
```json
{
  "type": "spectrum:fusion_shrine",
  "group": "spectral_ascension_allthemodium_upgrades",
  "time": 1600,
  "experience": 24.0,
  "fluid": { "fluid": "spectrum:liquid_crystal" },
  "copy_components": true,
  "disable_yield_upgrades": true,
  "ingredients": [
    "spectrum:malachite_workstaff",
    { "tag": "c:ingots/allthemodium", "count": 8 },
    { "tag": "c:plates/allthemodium", "count": 4 },
    { "item": "spectrum:topaz_powder", "count": 16 },
    { "item": "spectrum:amethyst_powder", "count": 16 },
    { "item": "spectrum:citrine_powder", "count": 16 },
    "spectrum:moonstone_core"
  ],
  "result": {
    "id": "spectral_ascension:allthemodium_workstaff",
    "components": {
      "minecraft:enchantments": { "levels": { "minecraft:fortune": 5 } }
    }
  },
  "required_advancement": "spectral_ascension:unlocks/allthemodium_tier",
  "world_conditions": { "dimension": "minecraft:overworld", "time_of_day": "day" },
  "effects": {
    "start": "single_visual_explosion_on_shrine",
    "during": ["nothing", "visual_explosions_on_shrine", "nothing"],
    "finish": "legendary_tool_craft"
  },
  "description": { "translate": "spectral_ascension.recipe.fusion_shrine.explanation.allthemodium_workstaff" }
}
```
Note:
- 7 ingredienti, cioè il massimo.
- Il primo è l'oggetto da preservare.
- Se si vuole accettare anche il Glass Crest Workstaff si può usare `"#spectrum:workstaffs"`, ma così si perde la distinzione di progressione.
- `world_conditions` ed `effects` sono opzionali e servono solo come flavor.
- La `description` può omettere `"type": "translatable"`: il codec dei Component lo deduce da `translate` **[D]**.
- `spectral_ascension:allthemodium_workstaff` è un placeholder. Per il nostro workstaff ci servirà una classe Java che estenda o imiti `WorkstaffItem` di Spectrum: è un tema per un altro file.

### 6.5 Domande aperte

- **SpectrumJEI**: confermare in gioco che le ricette di addon compaiono e che il lock per advancement viene rispettato.
- **Yield ed Efficiency negli upgrade fusion**: confermare in gioco il possibile dupe o mancato consumo (§2.2). Va comunque mitigato con `disable_yield_upgrades: true`.
- Comportamento esatto della fusione degli incantesimi in `copy_components` (`SpectrumEnchantmentHelper.addOrUpgradeEnchantment`): lo verifica il collega degli incantesimi.
- Formato 1.21.1 del predicato `inventory_changed` negli advancement e della forma compatta `enchantments` nei result.
