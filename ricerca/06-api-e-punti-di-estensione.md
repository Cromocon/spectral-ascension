# 06 — API di Spectrum e punti di estensione

> Ricerca del 16/11/2026. **[V]** = verificato leggendo file/URL; **[D]** = dedotto.
> Sorgente ≈1.12.9: `scratchpad/src/spectrum-1.12.9/` (abbreviato **`S/`** = `…/src/spectrum-1.12.9/src/main/java/de/dafuqs/spectrum/`). Sorgente HEAD 1.13.0: `scratchpad/src/spectrum/`. Jar: `scratchpad/jars/spectrum-1.12.9-1.21.1-neo/`. Percorso completo dello scratchpad: `<scratchpad>`.

---

## 1. Punti pubblici di Spectrum

Il package `de.dafuqs.spectrum.api` contiene **109 file** [V]. Non c'è documentazione (javadoc quasi assente, nessuna pagina "API" nella wiki clonata [V: grep su `spectrum-wiki/*.md`]); la wiki documenta invece molto bene i **formati dati** (ricette, color mapping, predicati, pagine Modonomicon). Qui sotto: raggruppati per quanto ci servono.

### 1.1 Essenziali per Spectral Ascension

**Interfacce per item (`S/api/item/`)** — implementandole i nostri item si comportano "come" quelli di Spectrum:

| File | Descrizione |
|---|---|
| `api/item/Preenchanted.java` | Item che nasce con incantesimi predefiniti (`getDefaultEnchantments()`), + helper `getDefaultEnchantedStack`, `onlyHasPreEnchantments`. Usato da Bedrock gear, `WorkstaffItem`, `GreatswordItem`, `MalachiteCrossbowItem`, `MalachiteBidentItem` [V]. |
| `api/ink/InkPowered.java` | Item che consuma Ink; `getUsedColors(stack)`, tooltip standard, `canUse(player)` legato all'advancement di sblocco Ink. Usato da Glass Crest Greatsword/Crossbow, Ferocious/Fractal Bident [V]. |
| `api/item/AoEBreakingTool.java` | Rottura ad area (workstaff). |
| `api/item/SplitDamageItem.java`, `ArmorPiercingItem.java` | Danno diviso tra tipi / che ignora armatura (Glass Crest, bident). Nota nel codice: "wrap your damage source with stack tracking". |
| `api/item/ActivatableItem.java` | Item con stato attivo/non attivo (bident Fractal/Ferocious). |
| `api/item/ExpandedStatTooltip.java` | Tooltip statistiche esteso. |
| `api/render/ExtendedItemBarProvider.java` | Barre extra sull'item (es. carica/ink). |
| `api/render/SlotBackgroundEffectProvider.java` | Effetto di sfondo nello slot (look degli oggetti Glass Crest). |
| `api/item/ArmorWithHitEffect.java` | Armatura con effetto quando colpiti. |
| `api/item/DamageAwareItem.java`, `TickAwareItem.java`, `SlotReservingItem.java`, `CreativeOnlyItem.java` | Hook vari (reazione ai danni, tick in inventario, ecc.). |
| `api/item/GemstoneColor.java` | Interfaccia dei colori gemma (registro `spectrum:gemstone_color`); `PedestalRecipeTier` usa i `BuiltinGemstoneColor` [V]. |
| `api/item_group/ItemGroupIDs.java` | Id delle sotto-tab creative di Spectrum: `SUBTAB_EQUIPMENT`, `SUBTAB_RESOURCES`, ecc. [V] — da usare con `de.dafuqs.fractal.api.CreativeSubTabEvent`. |

Non esiste un'interfaccia `ExtendedEnchantable` in 1.12.9 né in HEAD [V: grep vuoto]. L'abilità di superare il livello massimo passa da ricette e advancement (vedi sotto), non da un'interfaccia sugli item.

**Ricette (`S/api/recipe/`, `S/registries/SpectrumRecipeTypes.java`)**

| File | Descrizione |
|---|---|
| `registries/SpectrumRecipeTypes.java` | Tipi di ricetta: `pedestal`, `anvil_crushing`, `fusion_shrine`, `enchanter`, `enchantment_upgrade`, `potion_workshop_brewing/crafting/reacting`, `liquid_crystal_converting`, `midnight_solution_converting`, `dragonrot_converting`, `spirit_instiller`, `ink_converting`, `crystallarieum_growing`, `cinderhearth`, `titration_barrel`, `primordial_fire_burning` [V]. |
| `recipe/pedestal/PedestalRecipeTier.java` | **Enum** `BASIC/SIMPLE/ADVANCED/COMPLEX`, ognuno con advancement di sblocco e colori di polvere ammessi [V]. È un enum, **non** un registro: non si possono aggiungere tier di Pedestal [V]. |
| `recipe/enchanter/EnchantmentUpgradeRecipe.java` | Ricetta di upgrade: `enchantment` + lista `levels` (1→2, 2→3, …); `isInNormalRange(level)` confronta con `getMaxLevel()` [V]. Wiki: "You are able to specify enchantment upgrades over the usual max level (and very welcome to!)", sbloccati dall'advancement `spectrum:milestones/unlock_overenchanting_with_enchanter` [V: `spectrum-wiki/Custom-Enchantment-Upgrade-Recipes.md`]. **È la via ufficiale per i nostri "incantesimi oltre il max", ma in 1.12.9 quell'advancement dipende da `spectrum:__wip`, che non esiste: oggi è irraggiungibile (vedi [03](03-incantesimi-livello-massimo-e-config.md) e [00](00-checklist-progressione.md)).** |
| `api/recipe/IngredientStack.java` | Ingrediente con quantità (`ICustomIngredient`, tipo `spectrum:ingredient_stack`), usato da Fusion Shrine/Enchanter. |
| `api/recipe/GatedRecipe.java`, `DescriptiveGatedRecipe.java` | Ricette legate a un `required_advancement`. |
| `api/recipe/WorldEffect.java` | Effetti nel mondo al termine di una Fusion Shrine (registro `spectrum:world_effect`, non sincronizzato, solo codice). |
| `api/recipe/FluidRecipeInput.java` | Input con fluido (Fusion Shrine). |
| `registries/SpectrumResourceConditions.java` | Condizioni per i JSON: `spectrum:integration_pack_active`, `spectrum:fluid_tag_empty`, `spectrum:registered` [V]. |

**Data component types (`S/registries/SpectrumDataComponentTypes.java`)** [V] — *Data component*: dal 1.20.5 ogni `ItemStack` porta dati tipizzati (come un oggetto JSON fortemente tipizzato) al posto del vecchio NBT. Quelli di Spectrum potenzialmente utili per i nostri upgrade: `workstaff` (impostazioni del workstaff), `aoe`, `ink_storage`, `ink_color`, `activated`, `overcharged`, `socketed`, `stored_experience`, `canvas_enchantments`, `bound_item`, `damage_immune`, `hide_usage_tooltip`. Elenco completo: activated, aoe, beverage, bottomless_stack, bound_item, canvas_enchantments, paired_food_component, custom_potion_data, ender_splice, hide_usage_tooltip, inertia, infused_beverage, ink_color, ink_powered_potion_contents, ink_storage, is_preview_item, jade_wine, last_cooldown_start, memory, concealed_effect(_profile), overcharged, paired_item, timestamp, shooting_star, slot_reserver, socketed, stable, stored_block, stored_experience, stored_recipe, targeted_structure, wrapped_present, with_milk, workstaff, damage_immune, fluid_content, sleep_altering_effects, gravitable.

Per l'indistruttibilità il Bedrock gear **non** usa nulla di Spectrum: usa il componente vanilla `DataComponents.UNBREAKABLE` + `.fireResistant()` [V: `registries/SpectrumItems.java` righe 130–139]. Possiamo fare lo stesso.

Tool tier e materiali: `registries/SpectrumToolTiers.java` (enum `GEMSTONE, OBLIVION, BEDROCK, DRACONIC, MALACHITE, GLASS_CREST, KNOTTED_SWORD, …`) e `registries/SpectrumArmorMaterials.java` [V]. Sono enum/holder interni: per i nostri tier creiamo i nostri `Tier` e `ArmorMaterial`.

### 1.2 Utili / probabili

**Ink (`S/api/ink/`)**: `color/InkColor.java`, `InkColors.java` (costanti), `InkColorTags.java`, `InkColorMixes.java`; `InkAmount.java`; `storage/InkStorage.java` (+ `SingleInkStorage`, `TotalCappedInkStorage`, `IndividualCappedInkStorage`, `FixedSingleInkStorage`, `CreativeInkStorage`, `TotalCappedElementalMixingInkStorage`); `storage/InkStorageItem.java`, `InkStorageBlockEntity.java`; `capability/InkCapability.java`, `InkCapabilities.java`, `ItemInkCapability.java`, `BlockEntityInkCapability.java` (capability NeoForge per trasferire ink); `InkTransferHelper.java`; `InkPoweredMobEffectInstance.java`. Servono se i nostri upgrade del bident/greatsword consumano Ink.

**Interazioni (`S/api/interaction/`)**: `EntityColorProcessor(Registry).java` (colorare entità), `ItemProjectileBehavior.java`, `ItemProvider(Registry).java`, `NaturesStaffTriggered.java`, `OmniAcceleratorProjectile.java`, `ResonanceProcessor.java` (drop dell'incantesimo Resonance, registro datapack `spectrum:resonance_processor`). Se facciamo blocchi nostri: `api/block/ColorableBlock`, `PaintbrushTriggered`, `MoonstoneStrikeableBlock`, `StrippableDrop`, `PlayerOwned`, `MultiblockCrafter`, `GatedGuidebookPage`.

**Predicati per advancement/loot (`S/api/predicate/`)**: `item/InkPredicate`, `StoredExperiencePredicate`, `BottomlessStackPredicate`, …; `location/MoonPhasePredicate`, `TimeOfDayPredicate`, `WeatherPredicate`, `WorldConditionsPredicate`, `CommandPredicate`; `block/BrokenBlockPredicate`. Documentati in `spectrum-wiki/Type-Specific-Predicates.md` e `Advancement-Criteria.md`.

**Registri custom (`S/registries/SpectrumRegistries.java`, `SpectrumRegistryKeys.java`)** [V]

| Chiave | Tipo | Come si estende |
|---|---|---|
| `spectrum:world_effect` | registro statico (codice), non sincronizzato | `DeferredRegister` sul registro |
| `spectrum:gemstone_color` | statico, sincronizzato | codice |
| `spectrum:glass_arrow_variant` | statico, sincronizzato | codice |
| `spectrum:ink_color` | statico, sincronizzato | codice |
| `spectrum:resonance_processor_type`, `pastel_payload_type`, `pastel_payload` | statici | codice |
| `spectrum:resonance_processor` | **datapack** | JSON in `data/<ns>/spectrum/resonance_processor/` [D sul percorso: segue la convenzione NeoForge `data/<ns>/<namespace_registro>/<path_registro>/`, confermata da SD per kindling_variant] |
| `spectrum:kindling_variant` | **datapack** | JSON in `data/<ns>/spectrum/kindling_variant/` [V: usato da Spectral Decorations] |
| `spectrum:lizard_frill_variant`, `lizard_horn_variant` | datapack | idem |
| `spectrum:geode_ores` | datapack | idem |
| `spectrum:pastel_upgrade` | datapack | idem |

*Spiegazione:* un **registro statico** si riempie con codice all'avvio (`DeferredRegister`); un **registro datapack** (o "dinamico") si riempie da file JSON, come le ricette, e può essere cambiato da datapack senza ricompilare.

**Data loader JSON (non registri, `S/data_loaders/`)**: `ColorMappingDataLoader` (`ink_color_mapping/<tipo>`: associa item/entità a un colore ink, wiki `Color-Mappings.md`), `NaturesStaffConversionDataLoader` (`natures_staff_conversion`), `EntityFishingDataLoader` (`entity_fishing`), `CrystalApothecarySimulationsDataLoader` [V].

### 1.3 Marginali

- `api/gui/SlotWithOnClickAction`, `SpectrumTooltipComponent`; `api/fluid/SpectrumFluidTank`; `api/entity/PackEntity`, `POIMemorized`; `api/pastel_network/*` (rete logistica di Spectrum); `api/item/FermentedItem`, `LoomPatternProvider`, `PresentUnpackBehavior`, `MergeableItem`, `SplittableItem`, `InkPoweredPotionFillable`, `AzureDikeItem`, `ExperienceStorageItem`.
- **Eventi custom: nessuno.** Spectrum non definisce sottoclassi di `Event` né fa `NeoForge.EVENT_BUS.post(…)` [V: grep vuoto]. I punti di aggancio sono registri, interfacce, `register(...)` statici (es. `EntityColorProcessorRegistry.register`) e gli eventi di Fractal (`CreativeSubTabEvent`). `S/events/` contiene solo game event/position source per meccaniche interne.
- **Integration pack** (`S/compat/SpectrumIntegrationPacks.java`) [V]: è il sistema con cui Spectrum attiva il proprio codice di compat (ae2, botania, create, malum, modonomicon, …) se la mod è presente e non è nella config `IntegrationPacksToSkipLoading`. `registerIntegrationPack` e la mappa sono `protected` e la lista è fissa nel metodo `register` → **un addon non può aggiungere un proprio integration pack**; può solo leggerne lo stato con `isIntegrationPackActive(modId)` o con la condizione JSON `spectrum:integration_pack_active`.
- **Config** (`S/config/SpectrumConfig.java`, `ModConfigSpec`, 113 `define`) [V]: rilevanti `BedrockAnvilCanExceedMaxVanillaEnchantmentLevel`, le `…Enchantability` per materiale (Gemstone, Bedrock, Malachite, GlassCrest, …), `IntegrationPacksToSkipLoading`, `AddItemTooltips`. Leggibili via `SpectrumConfig.CONFIG.<campo>.get()` [D sul nome del campo statico, visto in `SpectrumIntegrationPacks`: `SpectrumConfig.CONFIG.IntegrationPacksToSkipLoading.get()`].
- **Interface injection**: Spectrum pubblica `spectrum-injected-interfaces.json` (aggiunge interfacce a `MapDecoration`, `MobEffectInstance`, `FoodProperties.Builder`) [V: jar + `build.gradle` blocco `interfaceInjectionData { publish … }`]. Non ci riguarda direttamente.

### 1.4 Le classi che ci toccherà estendere (non API, ma inevitabili)

Gli oggetti speciali che vogliamo potenziare sono classi in `S/items/tools/` [V]:
- `WorkstaffItem extends MultiToolItem implements AoEBreakingTool, Preenchanted`; `GlassCrestWorkstaffItem extends WorkstaffItem`
- `GreatswordItem extends SwordItem implements Preenchanted`; `GlassCrestGreatswordItem extends GreatswordItem implements SplitDamageItem, ExtendedItemBarProvider, SlotBackgroundEffectProvider, InkPowered`
- `MalachiteCrossbowItem extends CrossbowItem implements Preenchanted, ArrowheadCrossbow`; `GlassCrestCrossbowItem`; `BedrockCrossbowItem`
- `MalachiteBidentItem extends TridentItem implements Preenchanted, ExpandedStatTooltip, ArmorPiercingItem`; `FerociousBidentItem`, `FractalBidentItem`

Estenderle è la via più semplice per "upgrade" fedeli, ma sono classi interne (vedi §3). `ArrowheadCrossbow` viene dalla libreria **Arrowhead** di DaFuqs (jar-in-jar in Spectrum).

---

## 2. Come ottenere Spectrum per lo sviluppo

### 2.1 Concetti

- **Maven repository**: un server che ospita librerie Java con coordinate `gruppo:artefatto:versione` (come il registry npm). Gradle scarica da lì le dipendenze dichiarate nel `build.gradle`.
- **Modrinth maven** (`https://api.modrinth.com/maven`, gruppo `maven.modrinth`) e **CurseMaven** (`https://cfa2.cursemaven.com`, gruppo `curse.maven`): espongono come maven i file caricati su Modrinth/CurseForge. Comodi, ma il POM che generano è **vuoto** (nessuna dipendenza transitiva) [V: POM Modrinth di spectrum 1.12.9 senza `<dependencies>`], quindi le dipendenze di Spectrum vanno dichiarate a mano.
- **`compileOnly` vs `implementation`** (vedi file 05 §2.8). Con ModDevGradle una mod in `implementation` finisce anche nel classpath delle run di test; con `compileOnly` serve aggiungerla a `localRuntime`/`runtimeOnly` per vederla in gioco.
- **Jar-in-jar (`jarJar`)**: includere un'altra libreria *dentro* il nostro jar (`META-INF/jarjar/`); NeoForge la estrae e la carica, scegliendo la versione più alta se più mod includono la stessa. Spectrum include così **Fractal 1.8.2, Arrowhead 2.1.0, AdditionalEntityAttributes 2.0.2, jgrapht, apfloat, jheaps** [V: `META-INF/jarjar/metadata.json`]. **Revelationary, Modonomicon e Curios invece NON sono inclusi**: sono mod separate obbligatorie [V: `mods.toml`]. Noi non dovremmo includere nulla di tutto questo (sono già nel modpack).

### 2.2 Dove sta Spectrum [V]

1. **Maven di DaFuqs/"sappho company": `https://maven.is-immensely.gay/releases`** — è il repo su cui Spectrum pubblica (`publishing { repositories { maven { name = "sapphoCompany"; url = "https://maven.is-immensely.gay/releases" } } }` in `spectrum-1.12.9/build.gradle`). Coordinate `de.dafuqs:Spectrum:<versione>` (artifactId con la **S maiuscola**, = `archives_base_name`). **Ma** `maven-metadata.xml` elenca solo fino a **1.12.5-1.21.1-neo** (lastUpdated 24/08/2026): 1.12.6–1.12.9 non sono state pubblicate lì [V]. Il POM di 1.12.5 contiene le dipendenze transitive (Revelationary, AEA, Arrowhead, Fractal, Modonomicon, exclusions-lib, jspecify, EMI, Curios, in scope `runtime`) [V]. Questo è il metodo usato da Spectral Decorations e Craftoria-Core.
2. **Modrinth maven**: `maven.modrinth:spectrum:1.12.9-1.21.1-neo` → jar e POM rispondono 200 [V]. (Version id Modrinth equivalente: `ODbJTcVf`.) Usato da Spectrum Compat.
3. **CurseMaven**: `curse.maven:spectrum-556967:<fileId>` (project id 556967 [V da StaTech-Companion-Mod: `spectrum_version=556967:9021449`]; il fileId di 1.12.9 non verificato perché CurseForge blocca le richieste automatiche).
4. Non esistono `maven.cafeteria.dev` o Ladysnake per Spectrum: Ladysnake è citato nel build.gradle solo per Cardinal Components (eredità Fabric) [V].

### 2.3 Coordinate Gradle consigliate (NeoForge 1.21.1, istanza ATM10)

Versioni = quelle installate nell'istanza [V: `ls mods/` dell'istanza]: Spectrum 1.12.9, Revelationary 1.5.2, Modonomicon 1.123.0, Curios 9.5.1, Allthemodium 3.2.1 (+ SpectrumJEI 21.1.11.1, JEI 19.70.0.469).

```groovy
repositories {
    maven { url = "https://maven.is-immensely.gay/releases" }                // Revelationary, Fractal, Arrowhead (DaFuqs)
    maven { url = "https://dl.cloudsmith.io/public/klikli-dev/mods/maven/" } // Modonomicon
    maven { url = "https://maven.theillusivec4.top/" }                       // Curios
    maven { url = "https://api.modrinth.com/maven"; content { includeGroup "maven.modrinth" } }
    maven { url = "https://cfa2.cursemaven.com";    content { includeGroup "curse.maven" } }
}

dependencies {
    // Spectrum 1.12.9: solo su Modrinth/CurseForge (il maven di DaFuqs si ferma a 1.12.5)
    implementation "maven.modrinth:spectrum:1.12.9-1.21.1-neo"

    // dipendenze obbligatorie di Spectrum (il POM Modrinth non le porta con sé)
    implementation "de.dafuqs:revelationary-neoforge:1.5.2+1.21.1"
    implementation("com.klikli_dev:modonomicon-1.21.1-neoforge:1.123.0") { exclude group: "mezz.jei" }
    compileOnly    "top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1:api"
    runtimeOnly    "top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1"

    // librerie che Spectrum include via jar-in-jar: servono a compile-time se usiamo le loro classi
    compileOnly "de.dafuqs:Fractal:1.8.2+1.21.1-neoforge"     // CreativeSubTabEvent
    compileOnly "de.dafuqs:arrowhead:2.1.0+1.21.1-neoforge"   // ArrowheadCrossbow (crossbow)

    // Allthemodium 3.2.1 (non su Modrinth per questa versione)
    implementation "curse.maven:allthemodium-364466:9143638"
    // Allthemodium richiede GeckoLib (required [4.7.5.1,)) -> va aggiunto per le run di test
}
```

Stato di verifica di ciascuna coordinata:
- `maven.modrinth:spectrum:1.12.9-1.21.1-neo` — jar 200 [V].
- `de.dafuqs:revelationary-neoforge:1.5.2+1.21.1` — presente in `maven-metadata.xml` [V].
- `com.klikli_dev:modonomicon-1.21.1-neoforge:1.123.0` — jar 200 su Cloudsmith [V]. Spectrum la dichiara con `exclude(group: "com.klikli_dev")` e `exclude(group: "mezz.jei")` [V].
- `top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1` (+ classifier `:api`) — jar `-api` 200 [V]. Lo schema compileOnly-api / runtimeOnly-completo è quello di Spectrum [V].
- `de.dafuqs:Fractal:1.8.2+1.21.1-neoforge`, `de.dafuqs:arrowhead:2.1.0+1.21.1-neoforge` — presenti in `maven-metadata.xml` [V]. Se in dev le classi JiJ di Spectrum vengano caricate in automatico, o se serva metterle anche a runtime, non l'ho verificato senza fare una build [D].
- `curse.maven:allthemodium-364466:9143638` — URL CurseMaven 200 [V]. Project id e file id ricavati dall'API pubblica di terze parti `api.cfwidget.com` (file `allthemodium-3.2.1_mc_1.21.1.jar`) [V], non dall'API ufficiale di CurseForge (403).
- Allthemodium: `geckolib` required `[4.7.5.1,)`, `lootr` optional [V: mods.toml nel jar].

Alternativa più "ufficiale" se DaFuqs pubblicherà 1.12.9 sul suo maven: `implementation "de.dafuqs:Spectrum:1.12.9-1.21.1-neo"` (le dipendenze transitive arriverebbero dal POM). Nota: con il `+` nelle versioni (es. `1.5.2+1.21.1`) Gradle funziona, ma nelle `versionRange` del `mods.toml` il `+…` è metadato di build [D].

### 2.4 Dipendenze transitive di Spectrum (riepilogo) [V]

| Mod/lib | Come la porta Spectrum | Cosa dichiarare nel nostro `mods.toml` |
|---|---|---|
| Revelationary 1.5.2 (`revelationary`) | mod separata, required `[1.5.2+1.21.1,)` | niente (arriva da Spectrum); `required` solo se usiamo direttamente le sue classi (es. `CloakedItem` per item nascosti finché non si sblocca un advancement) |
| Modonomicon (`modonomicon`) | mod separata, required `[1.117.4,)` | niente, a meno di usarne le API Java; le voci del libro sono solo JSON |
| Curios (`curios`) | mod separata, required `[9.5.1+1.21.1,)` | niente (non facciamo trinket) |
| Fractal 1.8.2, Arrowhead 2.1.0, AdditionalEntityAttributes 2.0.2 | jar-in-jar dentro Spectrum | niente; `compileOnly` nel build.gradle se le usiamo |
| jgrapht/apfloat/jheaps | jar-in-jar | niente |
| exclusions-lib | `implementation` nel build di Spectrum ma **non** required (Spectrum ha un fallback `ExclusionsLibCompat.registerNotPresent`) e assente dall'istanza ATM10 [V] | niente |
| Malum (optional), Pastel (**incompatible**) | mods.toml di Spectrum | niente |

---

## 3. Cosa NON è API e rischi di aggiornamento

### 3.1 Confronto `api/` tra ≈1.12.9 e HEAD 1.13.0

Comando: `git -C …/src/spectrum diff --stat 890784ca HEAD -- src/main/java/de/dafuqs/spectrum/api` (890784ca = 26/10/2026, `mod_version=1.12.9-1.21.1-neo`; HEAD `9710add3` = 10/11/2026, `mod_version=1.13.0-1.21.1-neo`) [V]:

```
api/block/MultiblockCrafter.java                     |  2 ++
api/block/PaintbrushHoverInfo.java  (nuovo)          | 11 +++
api/block/PaintbrushTriggered.java                   | 11 ++---   firma di onPaintBrushTrigger cambiata (via BlockHitResult, Player @Nullable)
api/interaction/EntityColorProcessor.java            | 11 ++---   Optional<DyeColor> -> Optional<InkColor>
api/interaction/EntityColorProcessorRegistry.java    | 12 ++---   idem
api/interaction/ItemProjectileBehavior.java          | 20 ++----
api/item/PrioritizedBlockInteraction.java (rimosso)  |  5 -
7 files changed, 37 insertions(+), 35 deletions(-)
```

In due settimane, su un minor bump, l'API ha avuto **cambi di firma incompatibili e una rimozione**. Il cambio di `EntityColorProcessor` rompe in pratica Spectral Decorations 1.10.1 (cast a `DyeColor`, vedi file 05 §2.9). Nel frattempo l'intero repo ha avuto 599 file modificati (+9023/−4121); `registries/`, `recipe/` e `items/tools/` 23 file, tra cui `MalachiteBidentItem`, `FerociousBidentItem`, `FractalBidentItem` [V] — proprio le classi che vorremmo estendere.

### 3.2 Cosa considerare "non API"

- **Tutto fuori da `de.dafuqs.spectrum.api`**: `items.*`, `blocks.*`, `registries.*` (i campi `SpectrumItems.X` sono stabili come *id*, ma i tipi e i costruttori possono cambiare), `recipe.*` (inclusi `PedestalRecipeTier`, i serializer, `PedestalRecipeInput`), `helpers.*`, `mixin.*`, `injectors.*`, `compat.*`, `config.*`. Spectral Decorations ne usa molti, ma lo scrive DaFuqs e lo aggiorna insieme a Spectrum; noi no.
- **Anche `api` non ha garanzie** (§3.1): niente versioning semantico dichiarato, niente javadoc, `package-info.java` solo con annotazioni di nullabilità [D].
- **I formati JSON** (ricette, color mapping, voci Modonomicon) sono la superficie più stabile e documentata (wiki). Meno codice Java legato a classi interne = meno rotture.
- **Mixin su classi di Spectrum** = rischio massimo (crash all'avvio se il metodo bersaglio cambia).

### 3.3 Mitigazioni pratiche [D]

1. Spostare il più possibile nei **dati** (ricette, upgrade di incantesimi, tag, voci del libro).
2. Isolare il codice che tocca classi interne in un package `compat/spectrum/` piccolo, con un test manuale per ogni aggiornamento di Spectrum.
3. `versionRange` di Spectrum chiusa sul minor (`[1.12.9-1.21.1-neo,1.13)`), da allargare dopo aver testato 1.13.
4. Compilare contro la versione **esatta** dell'istanza (1.12.9), non contro HEAD.

---

## 4. Domande da porre a DaFuqs (lista breve)

1. **Maven**: puoi pubblicare 1.12.9 (e le prossime) su `maven.is-immensely.gay`? Oppure consigli di usare il Modrinth maven per gli addon?
2. **Stabilità**: quali classi consideri "API pubblica" stabile? Solo `de.dafuqs.spectrum.api`? È previsto un deprecation path (es. per i cambi di `EntityColorProcessor`/`PaintbrushTriggered` in 1.13)?
3. **Upgrade del gear**: per creare versioni potenziate di Workstaff, Greatsword, Crossbow e Bident è ok estendere `WorkstaffItem`, `GlassCrestGreatswordItem`, `GlassCrestCrossbowItem`, `MalachiteBidentItem` & co.? Quali sono più stabili? Esiste un modo più pulito (interfacce/componenti)?
4. **Tier del Pedestal**: `PedestalRecipeTier` è un enum chiuso. Va bene restare su `COMPLEX` per tutte le nostre ricette, o c'è un modo previsto per gating aggiuntivi (solo `required_advancement`)?
5. **Overenchanting**: per incantesimi oltre il massimo, è corretto usare solo ricette `spectrum:enchantment_upgrade` con più `levels`, sbloccate da `spectrum:milestones/unlock_overenchanting_with_enchanter`? C'è un limite massimo consigliato?
6. **Guidebook**: è ok aggiungere voci nelle categorie esistenti (`spectrum:equipment`, `spectrum:enchanting`) come fa Spectral Decorations, o preferisci una categoria dedicata agli addon? Come evitiamo conflitti di coordinate `x`/`y` con voci future?
7. **Creative tab**: va bene inserire i nostri item nella sotto-tab `SUBTAB_EQUIPMENT` tramite `CreativeSubTabEvent` di Fractal?
8. **Ink**: per item che consumano Ink, basta implementare `InkPowered` + un `InkStorageItem`, o ci sono passaggi di registrazione (capability) necessari?
