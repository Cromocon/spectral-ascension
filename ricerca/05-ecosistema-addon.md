# 05 — Ecosistema degli addon di Spectrum

> Ricerca del 16/11/2026. Legenda: **[V]** = verificato leggendo file/API; **[D]** = dedotto (ragionamento, non controllato direttamente).
> Fonti locali: i jar estratti in `scratchpad/jars/` e i repo clonati in `scratchpad/src/` (cartella temporanea di lavoro).
> Fonti web: API Modrinth (`https://api.modrinth.com/v2/search?query=…`, `/v2/project/<slug>`, `/v2/project/<slug>/dependencies`), ricerca codice GitHub (`gh search code 'de.dafuqs:Spectrum'`, `gh search code spectrum_version --filename gradle.properties`).

---

## 0. Mini-glossario (solo quello che serve per leggere questo file)

- **Addon**: una mod separata che *dipende* da un'altra mod (Spectrum) e ne usa classi, registri, tipi di ricetta e dati. Non contiene il codice di Spectrum: lo trova a runtime nella cartella `mods/`.
- **Fork**: una *copia* del codice sorgente di un'altra mod, che poi evolve per conto suo come mod indipendente (stesso punto di partenza, strade separate). Un fork non dipende dall'originale, lo *sostituisce*.
- **`neoforge.mods.toml`**: il "package.json" di una mod NeoForge (`META-INF/neoforge.mods.toml` dentro al jar). Dichiara modId, versione, file mixin, e le **dipendenze**:
  - `type="required"` → il gioco non parte senza quella mod; `"optional"` → se c'è deve rispettare la `versionRange`, se manca va bene; `"incompatible"` → il gioco rifiuta di partire se presente.
  - `ordering="AFTER"` → la nostra mod viene caricata *dopo* la dipendenza (utile per sovrascrivere le sue risorse e perché i suoi registri siano già inizializzati quando il nostro costruttore gira).
- **Mixin**: una tecnica per "iniettare" codice dentro metodi di classi altrui (Minecraft o altre mod) al caricamento. Potentissima ma fragile: se la classe bersaglio cambia, il mixin si rompe. Si dichiarano in un file `<modid>.mixins.json` referenziato dal `mods.toml`.
- **Datagen** (data generation): invece di scrivere a mano centinaia di JSON (ricette, loot table, modelli), si scrive codice Java che li genera con un task Gradle (`runData`). È opzionale.
- **Gradle / ModDevGradle (MDG)**: Gradle è il build tool (come npm+webpack per Java). ModDevGradle è il plugin ufficiale NeoForge (`net.neoforged.moddev`) che scarica Minecraft decompilato, configura le run (client/server/data) e costruisce il jar. *NeoGradle* è il plugin alternativo più vecchio; tutti gli addon visti qui usano MDG.

---

## 1. Panoramica degli addon esistenti

### 1.1 Addon NeoForge 1.21.1 (rilevanti per noi)

| Progetto | Link | Loader / MC | Stato | Cosa aggiunge |
|---|---|---|---|---|
| **Spectral Decorations** (DaFuqs & co.) | <https://modrinth.com/mod/spectral-decorations> · <https://github.com/DaFuqs/SpectralDecorations> (branch `1.21.1-neoforge`) | Fabric/Quilt (1.19.2, 1.20.1, 1.21.1) + **NeoForge 1.21.1**; LGPL-3.0 | Attivo, ultimo update 05/10/2026 [V] | Blocchi decorativi colorati (travi, anfore, lanterne, luci, tubi in metallo, piume resplendenti colorate), colorazione di Bedrock Armor e Bottomless Bundle, varianti colorate del Kindling, peluche. **È installato nella nostra istanza** (1.10.1). |
| **Spectrum Compat** (MWojt) | <https://modrinth.com/mod/spectrum-compat> · <https://github.com/MarekWojt/spectrumcompat> | Solo NeoForge 1.21.1; MIT | Attivo (push 25/10/2026) [V] | Teste di mob (Aether, Deep Aether, VanillaBackport, Copper Age Backport) per il sistema Treasure Hunter di Spectrum, ricette Spirit Instiller/Anvil Crushing/Enchanter. **Installato nella nostra istanza** (0.1.0). |
| **SpectrumJEI** (TheLMiffy1111) | <https://modrinth.com/mod/spectrumjei> · <https://github.com/TheLMiffy1111/SpectrumJEI> | Fabric + NeoForge (fino a 1.21.1); LGPL-3.0 | Attivo (28/08/2026) [V] | Integrazione JEI per i tipi di ricetta Spectrum (Spectrum di suo supporta EMI/REI). **Installato nell'istanza** (`SpectrumJEI-21.1.11.1+neoforge.jar`). Rilevante: le nostre ricette Pedestal/Fusion Shrine appariranno in JEI tramite questo addon, senza lavoro extra [D]. |
| **ProjectE: EMC for Spectrum** | <https://modrinth.com/mod/projecte-emc-for-spectrum> · <https://github.com/KURONAMI333/spectrum-emc> | NeoForge 1.21.1; ARR | Attivo (06/09/2026) [V] | Valori EMC per gemme e risorse Spectrum. Solo dati/integrazione. |

Altri progetti NeoForge 1.21.1 trovati via ricerca codice GitHub che *dipendono* da Spectrum ma sono "core mod" di modpack o mod non-addon: `TeamAOF/Craftoria-Core` (core del modpack Craftoria, usa `de.dafuqs:Spectrum` + Fractal + Revelationary), `TheStaticVoid/StaTech-Companion-Mod` (usa CurseMaven `spectrum_version=556967:9021449`), `Ethanolgie/I-Just-Want-to-Ruin-the-Ceiling`, `hexqua/apprentice_codex`, `Velron-Devs/Botania-Revived`, `Anvil-Dev/SiliconeDolls`, `Bivrik/FancyNotify` (compat opzionale). [V: risultati di `gh search code`]

### 1.2 Addon Fabric/Quilt (versioni precedenti, utili come esempi ma non direttamente)

| Progetto | Link | Loader / MC | Stato | Cosa aggiunge |
|---|---|---|---|---|
| Spectrum Jetpacks | <https://modrinth.com/mod/spectrum-jetpacks> · github MayaqqDev/spectrum-jetpacks | Fabric/Quilt 1.19.2–1.20.1 | Fermo dal 09/2024 | Jetpack alimentati a Ink (usa l'API ink). |
| Spectrum Storage | <https://modrinth.com/mod/spectrum-storage> · github TheDrawingCoder-Gamer/spectrum-storage | Fabric + NeoForge, 1.20.1/1.21.1 (dichiarati) | 06/2026 | Oggetti di storage nella progressione Spectrum (scritto in Scala). Il build.gradle visto usa Spectrum 1.8.1 (Fabric) [V]. |
| Exspectriments | <https://modrinth.com/mod/exspectriments> · sylveon.pet/cyan/exspectriments | Fabric/Quilt 1.20.1/1.21.1 | 09/2025 | "Cose varie". Dipendenze dichiarate: fractal-lib, revelationary, emi, spectrum, cosmetic-armor [V]. |
| Spectral Sync | <https://modrinth.com/mod/spectral-sync> · github walksanatora/spectralsync | Fabric 1.20.1/1.21.1 | 12/2025 | Libro per condividere i progressi Spectrum/Pastel. |
| Spectral Utilities | github eehunter/Spectral-Utilities | Fabric | 01/2026 | Automazione/QoL. |
| AppSpec | github vgskye/appspec | Fabric | 01/2026 | Trasporto di Ink con AE2. |
| Sharing Hearts | github KatTheFox/sharing-hearts | Fabric | 01/2025 | Heartbound Chest condivise per team. |
| Luminosity | github MayaqqDev/Luminosity | Fabric | 2023 | "A spectrum Addon". |
| Big Globe – Spectrum compat | modrinth big-globe-spectrum-compatibility | Fabric/Quilt | 01/2026 | Compat worldgen. |
| Spectral Core | github murphy-slaw/spectral-core | Fabric | 11/2026 | Core del modpack Spectral Seas (mixin di tweak). |

Pacchetti di texture/modpack con "Spectrum" nel nome (Better Spectrum Gemstone Armor, Spectrumcraft SMP, ecc.) non sono addon di codice; `Spectrum: Optical` **non** dipende da Spectrum (dipendenze: qkl, fabric-api) [V], è solo omonimo.

**Conclusione [V+D]:** su NeoForge 1.21.1 gli addon "veri" sono pochissimi (Spectral Decorations, Spectrum Compat, SpectrumJEI, EMC). **Nessuno aggiunge equipaggiamento di tier superiore**: il nostro sarebbe il primo addon di gear/progressione per Spectrum su NeoForge. CurseForge non è stato interrogato (le API di CurseForge richiedono una chiave; la ricerca web risponde 403 alle richieste automatiche [V]), quindi potrebbero esistere addon pubblicati solo lì.

### 1.3 Pastel: un fork, non un addon

- Pastel (<https://modrinth.com/mod/pastel>, <https://github.com/terrarium-earth/Pastel>, NeoForge 1.21.1) è un **fork** di Spectrum: l'API GitHub riporta `parent = DaFuqs/Spectrum` [V]. Il codice è stato copiato e rinominato (package `earth.terrarium…` invece di `de.dafuqs.spectrum`) [V], ed è sviluppato da un altro team (Terrarium).
- Non dipende da Spectrum: è un'alternativa. Infatti il `neoforge.mods.toml` di Spectrum 1.12.9 dichiara `modId="pastel" type="incompatible"` [V: `jars/spectrum-1.12.9-1.21.1-neo/META-INF/neoforge.mods.toml`]: i due non possono stare nella stessa istanza.
- Per noi: un addon per Spectrum **non** funziona su Pastel (nomi di classi e namespace diversi). Pastel ha i suoi addon (es. PastelJEI). Il README di Pastel dice che incoraggia gli addon e offre supporto API, ma è un altro ecosistema.

---

## 2. Analisi dettagliata: Spectral Decorations 1.10.1 (NeoForge)

Fonti: jar `scratchpad/jars/SpectralDecorations-1.10.1-1.21.1-neo/` e sorgente `scratchpad/src/SpectralDecorations/` (clone del branch `1.21.1-neoforge`, commit `74a7f26`, 05/10/2026). È l'esempio più importante perché è scritto **dall'autore stesso di Spectrum**: mostra come DaFuqs si aspetta che un addon si agganci.

### 2.1 `neoforge.mods.toml` [V]

```toml
license = "lGPL3"
[[mods]]
modId = "spectral_decorations"
version = "1.10.1-1.21.1-neo"
[[mixins]]
config = "spectral_decorations.mixins.json"

[[dependencies.spectral_decorations]]  # neoforge  required [21.1.186,)  ordering NONE
[[dependencies.spectral_decorations]]  # minecraft required [1.21.1]     ordering NONE
[[dependencies.spectral_decorations]]
modId = "spectrum"
type = "required"
versionRange = "[1.12.5-1.21.1-neo,)"
ordering = "AFTER"
reason = "load after Spectrum to override Spectrum's resource files"
side = "BOTH"
```

Note:
- Dichiara **solo Spectrum** come dipendenza, non Revelationary/Modonomicon/Curios: quelle sono già `required` per Spectrum stesso, quindi sono garantite in modo transitivo [V per il toml di Spectrum, D per il ragionamento].
- `ordering = "AFTER"` serve sia a sovrascrivere risorse (texture in `assets/spectrum/…`) sia a garantire che le classi/registri di Spectrum siano pronti.
- La `versionRange` è aperta verso l'alto (`[1.12.5…,)`): accetta anche versioni future, il che è rischioso (vedi §2.8).
- Il `mods.toml` è generato da un template (`src/main/templates/META-INF/neoforge.mods.toml`) con sostituzione di variabili Gradle (`${spectrum_version_range}`), tramite il task `generateModMetadata` del build.gradle [V]. È lo schema standard del template MDK di NeoForge.

### 2.2 Struttura dei package [V]

```
de.dafuqs.spectral_decorations
├── SpectralDecorations            (@Mod: costruttore + handler di eventi)
├── SpectralDecorationsBlocks      (DeferredRegister.Blocks)
├── SpectralDecorationsItems       (DeferredRegister.Items)
├── SpectralDecorationsRecipeSerializers
├── SpectralDecorationsColorizer, …ItemTags, …KindlingVariants, …KindlingVariantTags
├── blocks/   (CuddlyBlock, WeatheringCopperRotatedPillarBlock)
├── items/    (ColoredResplendentFeatherItem)
├── recipe/   (BedrockArmorColoringRecipe, BottomlessBundleColoringRecipe, ColoringRecipe, UncoloringRecipe)
├── client/   (SpectralDecorationsClient)
└── mixin/    (KindlingEntityMixin, client/BedrockArmorItemMixin)
```

Molto piatto e semplice: una classe "registro" per tipo di oggetto, poche classi custom.

### 2.3 Come registra blocchi e item [V]

- Usa i **`DeferredRegister`** di NeoForge (`DeferredRegister.Blocks REGISTRAR`, metodo `register(IEventBus)` chiamato nel costruttore della mod). *Spiegazione:* un `DeferredRegister` è una lista di "promesse" di oggetti da registrare; NeoForge li crea al momento giusto durante l'avvio. Si ottiene un `DeferredHolder`/`DeferredBlock` da cui leggere l'oggetto con `.get()` dopo la registrazione.
- Il costruttore della mod riceve `(ModContainer, IEventBus modBus)` e chiama `SpectralDecorationsBlocks.register(modBus)`, `…Items.register(modBus)`, `…RecipeSerializers.register(modBus)`.
- **Riusa tantissime classi di Spectrum** invece di riscriverle. Riferimenti estratti con `javap` dal bytecode (numero di occorrenze):
  - blocchi Spectrum come classi base: `blocks.amphora.AmphoraBlock`, `blocks.decoration.FlexLanternBlock`, `CushionBlock`, `CushionedCarpetBlock`, `CushionedDirectionalBlock`, `SpectrumBedBlock`, `blocks.flammable.FlammableRotatedPillarBlock`;
  - item: `items.PigmentItem`, `items.armor.BedrockArmorItem`, `items.conditional.CloakedItem`, `blocks.bottomless_bundle.BottomlessBundleItem`;
  - registri: `registries.SpectrumItems`, `SpectrumBlocks`, `SpectrumBlockEntities`, `SpectrumEntityTypes`, `SpectrumFluids`, `SpectrumRegistryKeys`;
  - API: `api.item_group.ItemGroupIDs` (37 riferimenti), `api.recipe.IngredientStack`, `api.ink.color.InkColor`, `api.interaction.EntityColorProcessorRegistry`/`EntityColorProcessor`;
  - ricette: `recipe.pedestal.PedestalRecipeTier`, `ShapelessPedestalRecipe`, `blocks.pedestal.PedestalRecipeInput`, `recipe.EmptyRecipeSerializer`, `recipe.crafting.dynamic.SingleItemCraftingRecipe`.
- Le anfore nuove usano il **block entity di Spectrum**: invece di registrarne uno proprio, nell'evento `BlockEntityTypeAddBlocksEvent` aggiunge i propri blocchi al tipo `SpectrumBlockEntities.AMPHORA`. Trucco utile se mai riusassimo blocchi-con-inventario di Spectrum.
- **Creative tab**: non crea una tab propria; aggiunge gli item alle *sotto-tab* di Spectrum ascoltando `de.dafuqs.fractal.api.CreativeSubTabEvent` (libreria **Fractal** di DaFuqs) e confrontando l'id con le costanti `ItemGroupIDs.SUBTAB_DECORATION`, `SUBTAB_COLORED_WOOD`, `SUBTAB_RESOURCES`. → Anche noi possiamo mettere il gear nella sotto-tab "Equipment" di Spectrum (`ItemGroupIDs` ha costanti per tutte le sotto-tab).
- **Eventi**: la classe principale è annotata `@EventBusSubscriber(modid = …)` con metodi statici `@SubscribeEvent` per `BlockEntityTypeAddBlocksEvent` (evento del *mod bus*), `CreativeSubTabEvent` e `ItemTooltipEvent` (evento del *game bus*). *Spiegazione:* NeoForge ha due "bus" di eventi: il **mod bus** (eventi di avvio/registrazione specifici della mod, es. registri, setup client, datagen) e il **game bus** `NeoForge.EVENT_BUS` (eventi di gioco: tick, tooltip, danni, interazioni). Qui un solo `@EventBusSubscriber` serve entrambi; in NeoForge 21.1 gli eventi che implementano `IModBusEvent` vengono smistati sul mod bus [D: non ho verificato la versione esatta di NeoForge che introduce questo instradamento automatico; nel nostro codice conviene essere espliciti].
- Registra un **processore di colorazione** per il Kindling con `EntityColorProcessorRegistry.register(SpectrumEntityTypes.KINDLING, (entity, dyeColor, player) -> …)`: è l'API "ufficiale" di Spectrum per far reagire un'entità a pigmenti/pennello.

### 2.4 Mixin [V]

`spectral_decorations.mixins.json`:
```json
{ "package": "de.dafuqs.spectral_decorations.mixin", "compatibilityLevel": "JAVA_21",
  "mixins": ["KindlingEntityMixin"], "client": ["client.BedrockArmorItemMixin"],
  "injectors": { "defaultRequire": 1 } }
```
- `KindlingEntityMixin` si inietta in `KindlingEntity.aiStep()` (`@Inject(method="aiStep()V", at=HEAD)`) — cioè **dentro una classe di Spectrum**, non di Minecraft.
- `BedrockArmorItemMixin` (solo client) modifica `BedrockArmorItem` di Spectrum per il rendering colorato.
- Solo 2 mixin: anche l'autore li usa con parsimonia. Mixin su classi di Spectrum = si rompono facilmente quando Spectrum aggiorna.
- Nessun access transformer proprio [V: assente in `META-INF/`]. *Access transformer (AT)*: file `accesstransformer.cfg` che rende `public` campi/metodi `private` di Minecraft a compile-time e runtime; Spectrum ne ha uno suo [V], l'addon no.

### 2.5 Ricette [V]

Cartelle in `data/spectral_decorations/recipe/`: `pedestal/tier1/…`, `fusion_shrine/`, `crafting/`, `stonecutting/`, `midnight_solution_converting/`, `potion_workshop_reacting/`, `mod_integration/create/…`.

Esempio Pedestal (`recipe/pedestal/tier1/vanilla_wood_beams/…warped_beam.json`):
```json
{ "type": "spectrum:pedestal", "group": "vanilla_wood_beams", "time": 40, "tier": "basic",
  "colors": {}, "experience": 0.1,
  "pattern": ["PP","LL","PP"],
  "key": { "P": {"item":"minecraft:warped_planks"}, "L": {"tag":"minecraft:warped_stems"} },
  "result": { "id": "spectral_decorations:warped_beam", "count": 4 },
  "required_advancement": "spectrum:place_pedestal" }
```
Esempio Fusion Shrine (`recipe/fusion_shrine/kindling_plushie.json`): `"type": "spectrum:fusion_shrine"`, `time`, `experience`, `fluid: {"fluid":"spectrum:liquid_crystal"}`, `ingredients` con `count` (formato `IngredientStack` di Spectrum), `result`, `required_advancement`.

Punti chiave:
- Le ricette sono **JSON scritti a mano** (nessun datagen: nel sorgente non c'è `GatherDataEvent` né `DataProvider` [V]); si scrivono nel **namespace dell'addon** (`data/spectral_decorations/recipe/…`) usando i `type` di Spectrum (`spectrum:pedestal`, `spectrum:fusion_shrine`, …).
- `required_advancement` lega la ricetta alla progressione di Spectrum (la ricetta resta nascosta/inutilizzabile finché il giocatore non ha quell'advancement).
- Ricette di compat condizionate: `"neoforge:conditions": [{"type": "spectrum:integration_pack_active", "integration_pack": "create"}]` → la ricetta esiste solo se l'integration pack Create di Spectrum è attivo. Utile anche a noi per eventuali ricette opzionali.
- Ha anche un serializer di ricetta proprio (`spectral_decorations:bottomless_bundle_coloring`) che estende classi di Spectrum (`EmptyRecipeSerializer`, `ShapelessPedestalRecipe`).

### 2.6 Altri dati che "agganciano" Spectrum [V]

- `data/spectral_decorations/ink_color_mapping/item/*.json` → mappa item → colore Ink (es. `"spectrum:brown": ["spectral_decorations:iron_tubing", …]`). È il data loader `ColorMappingDataLoader` di Spectrum (`ink_color_mapping/%s`). Serve, ad esempio, al colore delle particelle/del "Pigment" associato all'item [D sullo scopo preciso].
- `data/spectral_decorations/spectrum/kindling_variant/*.json` → nuove voci del **registro datapack** `spectrum:kindling_variant`.
- `data/spectrum/tags/item/…`, `data/minecraft/tags/…`, `data/neoforge/data_maps/…` (fuels, waxables, oxidizables), `data/supplementaries/tags/…`.
- `assets/spectrum/textures|models` → sovrascrive alcune risorse di Spectrum (da qui l'`ordering=AFTER`).

### 2.7 Guidebook (solo struttura) [V]

Le voci vanno **dentro il libro di Spectrum**, quindi nel namespace `spectrum`:
```
data/spectrum/modonomicon/books/guidebook/entries/
├── general/spectral_decorations.json
├── equipment/colored_bottomless_bundles.json, colored_bedrock_armor.json
├── decoration/… (8 voci)
└── creating_life/colored_resplendent_deco.json
```
Ogni entry è un JSON Modonomicon: `"category": "spectrum:general"`, `x`/`y` (posizione nella mappa della categoria), `icon`, `hide_while_locked`, `read_by_default`, `pages: [{ "type": "modonomicon:text", "title": "book.spectral_decorations.guidebook…", "text": "…" }]`. Le chiavi di traduzione sono nel lang dell'addon (`assets/spectral_decorations/lang/`), con prefisso `book.spectral_decorations.…`. Le categorie del libro di Spectrum sono in `data/spectrum/modonomicon/books/guidebook/categories/` (about, general, equipment, enchanting, ink, magical_blocks, mod_integration, …) [V dal jar di Spectrum]. Il dettaglio sul tooltip "added by" è coperto dal collega.

### 2.8 build.gradle e dipendenza da Spectrum [V]

- Plugin: `net.neoforged.moddev` **2.0.140**, `maven-publish`, `idea`; Java 21; NeoForge `21.1.248`; Parchment `2024.11.17` (Parchment = nomi leggibili per parametri e variabili del codice decompilato di Minecraft).
- Repository principale per le mod di DaFuqs: **`https://maven.is-immensely.gay/releases`** (commento: "Revelationary, Fractal, AEA, Arrowhead…"). Più: Modrinth maven (`https://api.modrinth.com/maven`), CurseMaven (`https://cfa2.cursemaven.com`), Modonomicon (`https://dl.cloudsmith.io/public/klikli-dev/mods/maven/`), Curios (`https://maven.theillusivec4.top/`), Ladysnake, BlameJared, Parchment, ecc.
- Dipendenze:
  ```groovy
  implementation("de.dafuqs:Spectrum:${project.spectrum_version}")          // 1.12.5-1.21.1-neo
  implementation("de.dafuqs:Fractal:${project.fractal_version}")            // 1.8.2+1.21.1-neoforge
  implementation("de.dafuqs:revelationary-neoforge:${project.revelationary_version}") // 1.5.0+1.21.1
  compileOnly("maven.modrinth:create:${project.create_version}")            // compat opzionale
  ```
  *Spiegazione:* `implementation` = la libreria serve per compilare **e** viene messa nel classpath delle run di sviluppo (il gioco di test la carica); `compileOnly` = serve solo per compilare (la mod deve gestire il caso in cui non c'è a runtime); `runtimeOnly`/`localRuntime` = solo nelle run di test. Nessuna di queste include la mod nel nostro jar: per quello esiste `jarJar` (vedi file 06).
- Nota: la versione di Spectrum usata per compilare (1.12.5) è più vecchia di quella installata (1.12.9): si compila contro la versione minima supportata. Il maven `is-immensely.gay` arriva **solo fino a 1.12.5** (vedi file 06 §2).
- Pubblica a sua volta su `maven.is-immensely.gay` come `de.dafuqs:SpectralDecorations` (ultima pubblicata: 1.8.0) [V].

### 2.9 Un rischio concreto già visibile [V+D]

In Spectrum HEAD (1.13.0) l'interfaccia `EntityColorProcessor.colorEntity` passa da `Optional<DyeColor>` a `Optional<InkColor>` [V: `git diff 890784ca HEAD`]. Spectral Decorations 1.10.1 nel suo lambda fa `checkcast net/minecraft/world/item/DyeColor` sul contenuto dell'Optional [V con `javap`]. A livello di bytecode la firma è identica (i generici vengono cancellati), quindi la mod si caricherà, ma colorando un Kindling con Spectrum 1.13.0 il cast fallirà con `ClassCastException` [D]. Lezione: anche l'API "pubblica" di Spectrum cambia tra minor version, e la `versionRange` aperta (`[x,)`) non protegge.

---

## 3. Analisi breve: Spectrum Compat 0.1.0 (MWojt)

Fonti: jar `scratchpad/jars/spectrumcompat-0.1.0/`, sorgente `scratchpad/src/spectrumcompat/` (commit `45db3df`, 25/10/2026).

- **mods.toml** [V]: modId `spectrumcompat`, licenza MIT, `credits="Based on Spectrum by DaFuqs"`, `updateJSONURL` puntato a Modrinth. Dipendenze: `spectrum` **required** `[1.11.0,)` `ordering=AFTER`; `aether`, `deep_aether`, `vanillabackport`, `copperagebackport`, `betterarcheology` **optional** `ordering=AFTER`. Ogni blocco ha un commento che spiega *perché* serve: buona pratica.
- **build.gradle** [V]: MDG `2.0.141`; repository **solo Modrinth maven** con `content { includeGroup 'maven.modrinth' }`; Spectrum come:
  ```groovy
  compileOnly  "maven.modrinth:spectrum:1.11.4-1.21.1-neo"
  localRuntime "maven.modrinth:spectrum:1.11.4-1.21.1-neo"
  ```
  Ha la run `data` di datagen configurata (template MDK) ma i JSON sembrano scritti a mano [D].
- **Codice** [V]: 6 classi principali (`SpectrumCompat`, `HeadBlock`, `WallHeadBlock`, `HeadType`, `HeadLookup`, `VariantLocked`), un loot handler, classi client per i modelli delle teste. Riferimenti a Spectrum molto pochi: `recipe.InstanceRecipeInput`, `SpawnerCreatureChangeRecipe`, `blocks.mob_head.SpectrumSkullBlock`.
- **Mixin** [V]: 8, di cui 2 su classi di Spectrum (`SpectrumSkullBlock`, `SpawnerCreatureChangeRecipe`, entrambi `remap=false`), gli altri su Minecraft (`ServerAdvancementManager`, varianti di Chicken/Cow/Pig, accessor di `BlockEntityType`).
- **Dati** [V]: loot modifier `spectrum:treasure_hunter` (teste che droppano con l'incantesimo Treasure Hunter), ricette `spectrum:spirit_instiller`, `spectrum:anvil_crushing`, `spectrum:enchanter`, `spectrum:enchantment_upgrade`; tag `c:skulls`; tutto condizionato con `neoforge:mod_loaded`.
- **Possibile bug** [D]: `recipe/enchantment_upgrade/gloves_reach.json` usa i campi `bulk_item`, `item_scaling`, `level_cap`, ma il codec di `EnchantmentUpgradeRecipe` in Spectrum 1.12.9 (e in HEAD) richiede `enchantment` + `levels: [ {ingredient, ingredient_count_per_bowl, experience}, … ]` [V: `recipe/enchanter/EnchantmentUpgradeRecipe.java`, riga ~214, e wiki `Custom-Enchantment-Upgrade-Recipes.md`]. La ricetta quindi verrebbe scartata con un errore di parsing quando Deep Aether è presente. Lezione: validare i JSON contro la versione di Spectrum effettivamente usata (lo farebbe un datagen o un test in gioco).
- Il repo contiene un `CLAUDE.md`: anche questo autore ha usato un assistente AI [V: file presente].

---

## 4. Lezioni pratiche per la struttura di Spectral Ascension

1. **Modello di riferimento: Spectral Decorations.** È scritto da DaFuqs, è su NeoForge 1.21.1, ed è piccolo. Copiamo la *struttura* (non il codice): una classe `@Mod` con costruttore `(ModContainer, IEventBus)`, classi `…Blocks`/`…Items`/`…RecipeSerializers` con `DeferredRegister`, template `mods.toml` con variabili Gradle.
2. **`mods.toml`:** `spectrum` (required, `ordering="AFTER"`) e `allthemodium` (required, `AFTER`). Non serve ridichiarare Revelationary/Modonomicon/Curios a meno che usiamo direttamente le loro classi (in quel caso conviene dichiararle comunque, per chiarezza e versione minima). Consiglio: versione massima **chiusa** sul minor di Spectrum (es. `[1.12.9-1.21.1-neo,1.13)`) per evitare crash silenziosi come in §2.9 [D: scelta di design, da discutere; il rovescio è che un aggiornamento del modpack bloccherebbe l'avvio finché non aggiorniamo].
3. **Ricette come dati, nel nostro namespace**, con i `type` di Spectrum (`spectrum:pedestal` con `tier`/`colors`, `spectrum:fusion_shrine` con `fluid`, `spectrum:enchanter`, `spectrum:enchantment_upgrade`) e `required_advancement` per agganciarci alla progressione. Per gli "incantesimi oltre il max" la via data-driven esiste già: le ricette `enchantment_upgrade` possono avere più livelli del massimo vanilla (es. Sharpness di Spectrum ha 7 step, quindi fino a VIII) e quei livelli si sbloccano con l'advancement `spectrum:milestones/unlock_overenchanting_with_enchanter` [V: wiki + `data/spectrum/recipe/enchantment_upgrade/minecraft/sharpness.json`]. **Attenzione:** in 1.12.9 quell'advancement richiede `spectrum:__wip`, che non esiste, quindi oggi non si sblocca (`jar:data/spectrum/advancement/milestones/unlock_overenchanting_with_enchanter.json`; dettagli in [03](03-incantesimi-livello-massimo-e-config.md)).
4. **Guidebook:** voci in `data/spectrum/modonomicon/books/guidebook/entries/<categoria>/<nostra_voce>.json` con `category: "spectrum:equipment"` (ecc.) e chiavi lang nel nostro namespace. Attenzione alle coordinate `x`/`y` per non sovrapporci alle voci esistenti.
5. **Creative tab:** usare `CreativeSubTabEvent` di Fractal + `ItemGroupIDs` per inserirci nella sotto-tab equipaggiamento di Spectrum.
6. **Riusare le classi di Spectrum per il gear** (es. come SD usa `BedrockArmorItem`, `AmphoraBlock`) è comodo ma ci lega alle classi interne: per gli upgrade di workstaff/greatsword/crossbow/bident estendere le classi di Spectrum è probabilmente inevitabile, ma va isolato in poche classi e testato a ogni aggiornamento.
7. **Mixin: il minimo indispensabile**, preferendo API ed eventi. Con `defaultRequire: 1` un mixin che non trova il bersaglio fa crashare all'avvio: è un bene (errore chiaro invece di bug silenzioso).
8. **Ricette di compat condizionate** (`neoforge:conditions` con `neoforge:mod_loaded` o `spectrum:integration_pack_active`) se in futuro supportiamo mod opzionali.
9. **Validare i JSON** contro la versione di Spectrum target (l'errore in Spectrum Compat mostra che un formato vecchio passa inosservato). Un datagen o almeno un controllo dei log all'avvio (`Parsing error loading recipe`) va messo nella checklist.
10. **Licenza e crediti**: SD è LGPL-3.0 come Spectrum; Spectrum Compat è MIT con `credits="Based on Spectrum by DaFuqs"`. Non copiando codice di Spectrum possiamo scegliere la licenza, ma va rispettato quanto chiederà DaFuqs.
