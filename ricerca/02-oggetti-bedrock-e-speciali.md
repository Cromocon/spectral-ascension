# 02 — Oggetti bedrock e oggetti speciali di Spectrum (malachite / glass crest)

Ricerca per **Spectral Ascension** (addon NeoForge 1.21.1 per Spectrum 1.12.9, modpack ATM10).

**Fonti lette** (prefissi usati nelle citazioni):

- `src:` = sorgente Spectrum ≈1.12.9, `scratchpad/src/spectrum-1.12.9/` (i percorsi Java sono abbreviati da `src/main/java/de/dafuqs/spectrum/…` a `…/`)
- `jar:` = jar estratto `scratchpad/jars/spectrum-1.12.9-1.21.1-neo/`
- La wiki (`scratchpad/src/spectrum-wiki/`) non ha pagine sugli oggetti (solo guide per datapack), quindi non è citata.

**Legenda**: ✅ = verificato leggendo codice/dati; 🔎 = dedotto (ragionamento sul codice vanilla/NeoForge o su nomi, non verificato riga per riga).

---

## 0. Mini-glossario di modding (solo quello che serve qui)

| Concetto | In breve |
|---|---|
| **Item** | La "classe" di un oggetto (es. `SwordItem`). Ne esiste **una sola istanza** per tipo di oggetto, registrata nel gioco. Gli oggetti nell'inventario sono `ItemStack` (istanza + quantità + dati). |
| **ItemStack** | "Pila" concreta di un item: item + count + **data components** (incantesimi, danno, nome…). |
| **DataComponent** | Dal 1.20.5 i dati di uno stack sono componenti tipizzati (`DataComponents.ENCHANTMENTS`, `UNBREAKABLE`, `FIRE_RESISTANT`, …). Un item ha componenti *di default* (dati in `Item.Properties`) e lo stack può avere una *patch* che li modifica. Le mod ne registrano di propri (Spectrum: `spectrum:aoe`, `spectrum:workstaff`, `spectrum:activated`, …). |
| **Item.Properties** | Il "builder" con cui si configura un item al momento della creazione: `stacksTo`, `rarity`, `durability`, `fireResistant()`, `attributes(...)`, `component(tipo, valore)`. |
| **Tier** (in altre versioni/Fabric "ToolMaterial") | Interfaccia vanilla `net.minecraft.world.item.Tier`: durabilità, velocità di scavo, bonus danno, enchantability, ingrediente di riparazione, tag dei blocchi che *non* può raccogliere. Gli attrezzi (`DiggerItem`, `SwordItem`) ne prendono uno. |
| **ArmorMaterial** | Record vanilla registrato in un registry (`Registries.ARMOR_MATERIAL`): difesa per pezzo, enchantability, suono, toughness, knockback resistance, layer della texture. Si usa come `Holder<ArmorMaterial>`. |
| **Attribute modifiers** | Danno e velocità d'attacco mostrati nel tooltip vengono da modificatori di attributo (`ATTACK_DAMAGE`, `ATTACK_SPEED`, `ENTITY_INTERACTION_RANGE` = portata). Il valore mostrato = base del giocatore (1 danno, 4 velocità) + modificatore. |
| **DeferredRegister / DeferredItem** | Il modo NeoForge di registrare oggetti: si crea un `DeferredRegister.Items` col proprio mod id, ci si registrano `Supplier` che creano l'item, e lo si attacca all'event bus della mod. |
| **Modello item (JSON)** | `assets/<modid>/models/item/<id>.json`: dice come disegnare l'item (parent `item/generated` = piatto, `item/handheld` = tenuto come un attrezzo) e quali texture usare (`layer0`). |
| **Item property / predicate + override** | Un numero calcolato lato client (es. `pull` dell'arco). Nel JSON del modello, `overrides` sceglie un altro modello quando il predicato supera una soglia. Si registrano in Java con `ItemProperties.register(item, nome, funzione)`. |
| **Lang file** | `assets/<modid>/lang/en_us.json`: mappa chiavi → testo (`"item.spectrum.bedrock_sword": "Bedrock Sword"`). |
| **Tag** | Liste di item/blocchi in `data/<ns>/tags/item/*.json`, usate per raggruppare (es. `minecraft:swords`) e per decidere quali incantesimi accetta un item (`minecraft:enchantable/...`). |
| **Mixin** | Iniezione di codice dentro classi vanilla. Spectrum ne usa molti; per noi conta perché alcuni comportamenti si attivano con `instanceof` su interfacce di Spectrum, quindi un nostro item li eredita semplicemente implementando l'interfaccia. |

---

## 1. Equipaggiamento bedrock

### 1.1 Registrazione (✅ `src:…/registries/SpectrumItems.java:129-143, 179-203`)

Tutti gli oggetti bedrock sono registrati con lo stesso schema:

```java
new BedrockSwordItem(SpectrumToolTiers.BEDROCK,
    IS.of(Rarity.UNCOMMON)
      .attributes(SwordItem.createAttributes(SpectrumToolTiers.BEDROCK, 4, -2.4F))
      .fireResistant()
      .durability(SpectrumToolTiers.BEDROCK.getUses())          // Integer.MAX_VALUE
      .component(DataComponents.UNBREAKABLE, new Unbreakable(false)))
```

(`IS.of(...)` è un helper di Spectrum che restituisce un semplice `new Item.Properties()`, `SpectrumItems.java:529-537`.)

| id | Classe | Superclasse vanilla | Interfacce |
|---|---|---|---|
| `bedrock_pickaxe` | `SpectrumPickaxeItem` (sottoclasse anonima che sovrascrive `getDefaultEnchantments`) | `PickaxeItem` | `Preenchanted` |
| `bedrock_axe` | `BedrockAxeItem` | `AxeItem` | `Preenchanted` |
| `bedrock_shovel` | `BedrockShovelItem` | `ShovelItem` | `Preenchanted` |
| `bedrock_sword` | `BedrockSwordItem` | `SwordItem` | `Preenchanted` |
| `bedrock_hoe` | `BedrockHoeItem` | `HoeItem` | `Preenchanted` |
| `bedrock_bow` | `BedrockBowItem` | (arco) | `Preenchanted` |
| `bedrock_crossbow` | `BedrockCrossbowItem` | `CrossbowItem` | `Preenchanted`, `ArrowheadCrossbow` |
| `bedrock_shears` | `BedrockShearsItem` | (cesoie) | `Preenchanted` |
| `bedrock_fishing_rod` | `BedrockFishingRodItem` | (canna) | `Preenchanted` |
| `bedrock_helmet/chestplate/leggings/boots` | `BedrockArmorItem` (**abstract**; ogni pezzo è una sottoclasse anonima) | `ArmorItem` | `Preenchanted` |

Classi: `src:…/items/tools/Bedrock{Sword,Axe,Shovel,Hoe,Crossbow}Item.java`, `src:…/items/tools/SpectrumPickaxeItem.java`, `src:…/items/armor/BedrockArmorItem.java`. Tutte `public`, costruttori `public (Tier, Properties)` o `(Holder<ArmorMaterial>, Type, Properties)`.

Dettagli comuni (✅):
- Ogni classe tool bedrock sovrascrive `isEnchantable(stack) → true` (es. `BedrockSwordItem.java:21-24`).
- `BedrockArmorItem` (✅ `BedrockArmorItem.java:11-42`): `isValidRepairItem → false`, `isFoil → false` (niente luccichio da incantesimo), `makesPiglinsNeutral → true`, `getArmorTexture` restituisce sempre `spectrum:textures/armor/bedrock_armor.png` (texture 128×128, modello 3D custom, vedi §4.5).
- Rarità `UNCOMMON` (nome in giallo), `fireResistant()` (non brucia in lava/fuoco come item a terra).

### 1.2 Tier strumenti (✅ `src:…/registries/SpectrumToolTiers.java:41-75` + default di config `src:…/config/SpectrumConfig.java:495-584`)

`SpectrumToolTiers` è un **enum** che implementa `Tier`. I valori numerici sono letti dalla **config** di Spectrum (`SpectrumConfig.CONFIG.*`), quindi in ATM10 potrebbero essere sovrascritti dal file di config del pack (🔎 da verificare sul server).

| Tier | Durabilità | Velocità scavo | Bonus danno | Enchantability | Blocchi non raccoglibili | Riparazione |
|---|---|---|---|---|---|---|
| `BEDROCK` | `Integer.MAX_VALUE` (hardcoded) | 15.0 | 5.0 | **3** | `INCORRECT_FOR_NETHERITE_TOOL` | `spectrum:bedrock_dust` |
| `MALACHITE` | 1536 | 14.0 | 5.0 | 20 | `INCORRECT_FOR_NETHERITE_TOOL` | `spectrum:pure_malachite` |
| `GLASS_CREST` | 6144 | 18.0 | 10.0 | 5 | `INCORRECT_FOR_NETHERITE_TOOL` | `spectrum:pure_malachite` |
| (confronto) netherite vanilla | 2031 | 9.0 | 4.0 | 15 | — | — |

### 1.3 Statistiche risultanti per pezzo (✅ parametri da `SpectrumItems.java:130-139`; 🔎 i valori "mostrati" sono calcolati con la formula vanilla 1.21.1 `danno = bonusTier + parametro`, +1 base del giocatore; `velocità = 4 + parametro`)

| Pezzo | Parametri `createAttributes` | Danno mostrato | Velocità attacco | Incantesimo preimpostato | Vanilla max → "+N" |
|---|---|---|---|---|---|
| Piccone | (1, −2.8) | 7 | 1.2 | Silk Touch I | n/a (livello unico) |
| Ascia | (5, −3.0) | 11 | 1.0 | Efficiency VI | 5 → +1 |
| Pala | (1, −3.0) | 7 | 1.0 | Efficiency VI | 5 → +1 |
| Spada | (4, −2.4) | 10 | 1.6 | Sharpness VI | 5 → +1 |
| Zappa | (2, −0.0) | 8 | 4.0 | Fortune IV | 3 → +1 |
| Arco | — | — | — | Power VI | 5 → +1 |
| Balestra | — | — | — | Quick Charge IV | 3 → +1 |
| Cesoie | — | — | — | Efficiency VI | 5 → +1 |
| Canna da pesca | — | — | — | Luck of the Sea IV | 3 → +1 |

Fonte incantesimi: ricette `jar:data/spectrum/recipe/fusion_shrine/bedrock/*.json` (campo `result.components.enchantments`) e, in parallelo, `getDefaultEnchantments()` nelle classi (✅ coincidono).

**Balestra bedrock** (✅ `BedrockCrossbowItem.java:28-41`): velocità proiettile ×1.5, `getPullTimeModifier` 3.0, divergenza ×0.8 (metodi dell'interfaccia `ArrowheadCrossbow`, libreria esterna, vedi §3.6).

### 1.4 Armatura bedrock (✅ `src:…/registries/SpectrumArmorMaterials.java:37-48`, config `SpectrumConfig.java:471-493`, item `SpectrumItems.java:180-203`)

| Pezzo | Difesa | Toughness | Knockback res. | Enchantability | `durability(...)` | Incantesimo preimpostato |
|---|---|---|---|---|---|---|
| Elmo | 5 | 3.0 | 0.3 | 5 | 70×13 | Projectile Protection V |
| Corazza | 12 | 3.0 | 0.3 | 5 | 70×15 | Protection V |
| Gambali | 9 | 3.0 | 0.3 | 5 | 70×16 | Blast Protection V |
| Stivali | 5 | 3.0 | 0.3 | 5 | 70×11 | Fire Protection V |
| **Totale** | **31** (netherite = 20) | 12 | 1.2 (=100%) | | | Protezioni: 4 → +1 |

Suono di equipaggiamento: `ARMOR_EQUIP_NETHERITE`. 🔎 Vanilla limita l'armatura effettiva a 30 punti: il 31° punto della bedrock è già "sprecato"; per i tier superiori aumentare la difesa grezza serve a poco, meglio puntare su toughness/attributi/incantesimi.

### 1.5 Come è implementata l'indistruttibilità (✅)

1. **Componente vanilla `minecraft:unbreakable`** messo come componente di default in `Item.Properties`: `.component(DataComponents.UNBREAKABLE, new Unbreakable(false))` (`false` = non mostrare la riga "Unbreakable" nel tooltip). In 1.21.1 `ItemStack.isDamageableItem()` è falso se c'è questo componente, quindi `hurtAndBreak` non fa nulla.
2. **Durabilità** comunque impostata (`Integer.MAX_VALUE` per i tool, 70×N per le armature): serve solo perché alcune logiche vanilla richiedono che l'item abbia `MAX_DAMAGE` (es. `isEnchantable` di default) — 🔎.
3. **Tag**: gli item bedrock sono **rimossi** da `minecraft:enchantable/durability` (✅ `jar:data/minecraft/tags/item/enchantable/durability.json`, campo `"remove"`), così non accettano Unbreaking/Mending. Poiché `spectrum:enchantable/indestructible` = `#minecraft:enchantable/durability` (✅ `jar:data/spectrum/tags/item/enchantable/indestructible.json`), non accettano neanche l'incantesimo Indestructible di Spectrum (inutile su di loro).
4. Non c'è nessun override di `isDamageable` nelle classi bedrock. Il mixin `ItemStackMixin.java:45-50` modifica `isDamageableItem` solo per l'**incantesimo** Indestructible (tag `SpectrumEnchantmentTags.INDESTRUCTIBLE_EFFECT`), non per la bedrock.
5. Riparazione: armatura `isValidRepairItem → false`; i tool hanno `bedrock_dust` come ingrediente nel Tier ma tanto non si danneggiano.

### 1.6 Come sono applicati gli incantesimi preimpostati (✅)

Ci sono **due meccanismi paralleli**, entrambi necessari:

**a) Nella ricetta (è questo che li mette davvero sull'oggetto craftato).** Esempio `jar:data/spectrum/recipe/fusion_shrine/bedrock/sword.json`:

```json
"type": "spectrum:fusion_shrine", "time": 1200, "experience": 16.0,
"fluid": { "fluid": "spectrum:midnight_solution" },
"copy_components": true,
"ingredients": [ "minecraft:golden_sword", {"item":"spectrum:bedrock_dust","count":2}, ... ],
"result": { "id": "spectrum:bedrock_sword",
            "components": { "enchantments": { "minecraft:sharpness": 6 } } },
"required_advancement": "spectrum:unlocks/equipment/bedrock_tools"
```

Con `copy_components: true` (✅ `src:…/recipe/GatedSpectrumRecipe.java:71-84`) l'output è un `transmuteCopy` del **primo** ingrediente (copia tutta la patch di componenti: nome, incantesimi…), poi si applicano gli incantesimi del `result` e infine si "fondono" quelli dell'input con `addOrUpgradeEnchantment` (tiene il livello più alto). È così che glass crest eredita gli incantesimi della versione malachite.

**b) Interfaccia `Preenchanted`** (✅ `src:…/api/item/Preenchanted.java:14-51`, package `api` → pensata per uso esterno):

```java
public interface Preenchanted {
    Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments();
    static ItemEnchantments buildDefaultEnchantments(HolderLookup.Provider, Preenchanted);
    static <T extends Item & Preenchanted> ItemStack getDefaultEnchantedStack(HolderLookup.Provider, T);
    default boolean onlyHasPreEnchantments(ItemStack stack);
}
```

Usi (✅):
- `SpectrumItemGroups.java:61-95`: le voci del tab creativo sono create con `Preenchanted.getDefaultEnchantedStack(...)` (lo stack nel tab ha già gli incantesimi). **NB: `/give` dà l'item senza incantesimi.**
- `mixin/ItemStackMixin.java:52-60`: il tavolo degli incantesimi considera "incantabile" uno stack che ha **solo** i suoi incantesimi preimpostati (normalmente vanilla rifiuta stack già incantati).
- `mixin/EnchantmentHelperMixin.java:20-34`: per gli item `Preenchanted`, il tavolo propone solo incantesimi compatibili con quelli già presenti.

Quindi `Preenchanted` **non** applica automaticamente gli incantesimi al craft: i livelli sono duplicati a mano tra Java e JSON. Per il nostro addon conviene fare lo stesso (o generare i JSON con un datagen che legga `getDefaultEnchantments()`).

### 1.7 Malachite: non esiste un set "base" di attrezzi/armatura

✅ Esiste il Tier `MALACHITE` ma **nessun** piccone/ascia/pala/spada/zappa né armatura malachite: in `SpectrumItems.java:145-155` i soli oggetti malachite/glass crest sono workstaff, ultra greatsword, crossbow e bident(s). `SpectrumArmorMaterials` contiene solo `GEMSTONE` e `BEDROCK`. Il set base completo (tool + armatura) più alto di Spectrum è quindi **bedrock**.

---

## 2. Oggetti speciali (malachite → glass crest)

Registrazione ✅ `SpectrumItems.java:145-155`. Commento a riga 150: *"variants by socketing a moonstone core"*. Tutti `stacksTo(1)`, `Rarity.UNCOMMON`.

**Importante: questi oggetti NON sono indistruttibili** (nessun componente `UNBREAKABLE`) e solo le balestre sono `fireResistant()`. Per Spectral Ascension andranno aggiunti entrambi.

### 2.1 Workstaff

| | Malachite | Glass crest |
|---|---|---|
| id | `malachite_workstaff` | `glass_crest_workstaff` |
| Classe | `WorkstaffItem` | `GlassCrestWorkstaffItem extends WorkstaffItem` |
| Costruttore | `(MALACHITE, 1, -3.2F, props)` | `(GLASS_CREST, 1, -2.8F, props)` |
| Danno / velocità mostrati (🔎) | 7 / 0.8 | 12 / 1.2 |
| Durabilità | 1536 | 6144 |
| Velocità scavo | 14 | 18 |
| Enchantability | 20 | 5 |
| Incantesimo preimpostato | Fortune IV | (ereditato via copy_components) |
| Abilità extra | — | Proiettile di scavo a distanza |

Gerarchia (✅):
- `MultiToolItem extends DiggerItem` (`MultiToolItem.java:13-66`): scava blocchi del tag `spectrum:multitool_mineable`; tasto destro prova in ordine le azioni di pala → ascia → zappa (`Items.IRON_SHOVEL.useOn`, ecc.) se `canTill(stack)`; `canPerformAction` dichiara le `ItemAbility` di ascia/pala/zappa (compatibilità NeoForge).
- `WorkstaffItem extends MultiToolItem implements AoEBreakingTool, Preenchanted` (`WorkstaffItem.java:26`).
- `GlassCrestWorkstaffItem extends WorkstaffItem implements SlotBackgroundEffectProvider` (`GlassCrestWorkstaffItem.java:23`).

Funzionamento (✅):
- **Menu**: Shift + tasto destro apre un menu (`WorkstaffItem.use`, righe 58-67 → `WorkstaffScreenHandler` / `inventories/WorkstaffScreen.java`). Le scelte sono l'enum `WorkstaffItem.GUIToggle` (righe 30-52): `SELECT_SILK_TOUCH`, `SELECT_FORTUNE`, `SELECT_RESONANCE`, `SELECT_1x1`, `SELECT_3x3`, `SELECT_5x5`, abilita/disabilita azioni tasto destro, abilita/disabilita proiettili (solo glass crest: `WorkstaffScreen.java:45`). Il client manda al server l'**ordinale** dell'enum (`WorkstaffToggleSelectedPayload`).
- **Area di scavo**: componente `spectrum:aoe` (Integer, raggio: 1 = 3×3, 2 = 5×5). `AoEBreakingTool.afterBreakingBlock` (`api/item/AoEBreakingTool.java:13-23`) è chiamato da `SpectrumEventListeners.java:1039` per qualunque item che implementa l'interfaccia. Costo: `3^raggio` di **inchiostro bianco** per blocco (`WorkstaffItem.java:28, 92-100`).
- **Cambio incantesimo**: `applyToggle` rimuove Silk Touch/Resonance/Fortune e mette quello scelto; il livello di Fortune viene ricordato nel componente `spectrum:workstaff` = record `WorkstaffComponent(canTill, canShoot, fortuneLevel)` con default `(true, false, 4)` (`components/WorkstaffComponent.java:8-10`). Il cambio funziona solo se lo staff ha già uno di quei tre incantesimi (altrimenti advancement "unenchanted workstaff").
- **Glass crest — proiettile** (`GlassCrestWorkstaffItem.java:25-55`): se `canShoot`, tasto destro spara un `MiningProjectileEntity` (50 inchiostro bianco, cooldown 10 tick, 2 danni di durabilità).
- Bordo colorato nello slot dell'inventario (`SlotBackgroundEffectProvider`) con colore in base all'incantesimo attivo (righe 68-93).

Ricette (✅ `jar:data/spectrum/recipe/fusion_shrine/malachite/`), tutte Fusion Shrine, fluido `spectrum:liquid_crystal`, 1200 tick, `copy_components: true`:
- `malachite_workstaff.json`: diamond pickaxe + diamond axe + diamond shovel + 8 pure_malachite + 8 pure_quartz + 4 paltaeria_fragments + 8 moonstone_powder → Fortune IV.
- `glass_crest_workstaff.json`: malachite_workstaff + moonstone_core + 8 pure_diamond + 1 enchanted_book + 16 white_pigment.

### 2.2 Ultra Greatsword

| | Malachite | Glass crest |
|---|---|---|
| id | `malachite_ultra_greatsword` | `glass_crest_ultra_greatsword` |
| Classe | `GreatswordItem extends SwordItem implements Preenchanted` | `GlassCrestGreatswordItem extends GreatswordItem implements SplitDamageItem, ExtendedItemBarProvider, SlotBackgroundEffectProvider, InkPowered` |
| Costruttore `(tier, dmg, speed, reach, props)` | `(MALACHITE, 7, -2.8F, 1.0F)` | `(GLASS_CREST, 5, -2.8F, 1.0F)` |
| Danno / velocità mostrati (🔎) | 13 / 1.2 | 16 / 1.2 |
| Portata extra | +1 blocco | +1 blocco |
| Durabilità | 1536 | 6144 |
| Preimpostato | Sweeping Edge IV (3 → +1) | ereditato |

Dettagli (✅ `GreatswordItem.java:19-30`): il costruttore costruisce a mano gli attribute modifier (danno = `tier.getAttackDamageBonus() + attackDamage`, velocità, `ENTITY_INTERACTION_RANGE` con id pubblico `SpectrumEntityAttributes.REACH_MODIFIER_ID`).

Glass crest (✅ `GlassCrestGreatswordItem.java:34-168`):
- **Danno diviso**: 25% del danno è danno magico (`MAGIC_DAMAGE_SHARE`), applicato via `SplitDamageItem` (hook in `SpectrumEventListeners.java:741`).
- **Ground slam**: tenere premuto il tasto destro 32 tick, costa 25 inchiostro bianco, forza = livello di Sweeping Edge; crea un `MoonstoneStrike`. Barra di carica disegnata con `ExtendedItemBarProvider`.

Ricette: `malachite_ultra_greatsword.json` (diamond_sword + stessi materiali malachite) e `glass_crest_ultra_greatsword.json` (malachite greatsword + moonstone_core + 8 pure_netherite_scrap + nether_star + 16 white_pigment).

### 2.3 Crossbow

| | Malachite | Glass crest |
|---|---|---|
| id | `malachite_crossbow` | `glass_crest_crossbow` |
| Classe | `MalachiteCrossbowItem extends CrossbowItem implements Preenchanted, ArrowheadCrossbow` | `GlassCrestCrossbowItem extends MalachiteCrossbowItem implements ExtendedItemBarProvider, SlotBackgroundEffectProvider, InkPowered` |
| Costruttore | `(Properties)` + `durability(1536)` + `fireResistant()` | `(Properties)` + `durability(6144)` + `fireResistant()` |
| Velocità proiettile | ×1.5 | ×1.5 × (1 + overcharge) |
| Pull time modifier | 2.0 | 2.0 |
| Divergenza | ×0.75 | ×0.75 × (1 − overcharge·0.5) |
| Preimpostato | Piercing V (4 → +1) | ereditato |

✅ `MalachiteCrossbowItem.java:17-59`: accetta frecce vanilla e `#spectrum:glass_arrows`; si ripara con pure_malachite. ⚠ La riparazione usa **hardcoded** `SpectrumToolTiers.MALACHITE` (riga 37).

Glass crest (✅ `GlassCrestCrossbowItem.java:25-170`): con la balestra già carica, Shift + tasto destro tenuto fino a 6 s = **overcharge** (100 inchiostro bianco), salvato nel componente `spectrum:overcharged` (Float 0..1). Al tiro (`SpectrumEventListeners.java:601-627`, controllo `instanceof GlassCrestCrossbowItem`) particelle e, se overcharge > 99%, danno base della freccia ×1.5.

🔎 Semantica di `getPullTimeModifier` (definita nella libreria Arrowhead): la bedrock ha 3.0 e la malachite 2.0; non ho letto il codice di Arrowhead, quindi non so dire se un valore più alto ricarica più veloce o più lento.

Ricette: `malachite_crossbow.json` (crossbow + materiali malachite → Piercing V), `glass_crest_crossbow.json` (malachite_crossbow + moonstone_core + 8 pure_redstone + 4 star_fragment + 16 white_pigment).

### 2.4 Bident (tridente a due punte) — **due** varianti glass crest

| | Malachite | Ferocious (glass crest) | Fractal (glass crest) |
|---|---|---|---|
| id | `malachite_bident` | `ferocious_glass_crest_bident` | `fractal_glass_crest_bident` |
| Classe | `MalachiteBidentItem extends TridentItem implements Preenchanted, ExpandedStatTooltip, ArmorPiercingItem` | `FerociousBidentItem extends MalachiteBidentItem implements SlotBackgroundEffectProvider, InkPowered, ActivatableItem` | `FractalBidentItem extends MalachiteBidentItem implements SlotBackgroundEffectProvider, InkPowered, ActivatableItem` |
| Costruttore `(props, speed, damage, armorPierce, protPierce[, hasActive])` | `(-2.4, 9, 0.25, 0, false)` | `(-2.2, 13, 0.33, 0.33)` | `(-2.4, 6.5, 0.25, 0.25)` |
| Danno / velocità mostrati (🔎) | 10 / 1.6 | 14 / 1.8 | 7.5 / 1.6 |
| Perforazione armatura / protezione | 25% / 0% | 33% / 33% | 25% / 25% |
| Durabilità | 1536 | 6144 | 6144 |
| Preimpostato | Impaling VI (5 → +1) | ereditato | ereditato |
| Componente di default | — | `spectrum:activated` | `spectrum:activated` |

Funzionamento (✅ `MalachiteBidentItem.java:33-232`):
- Si lancia come un tridente, ma come entità `BidentEntity` di Spectrum; riptide se ha Riptide ed è in acqua/pioggia. Accetta in più Sharpness, Smite, Bane of Arthropods e Looting (`supportsEnchantment`, riga 229).
- Danno: il costruttore crea a mano i modificatori (il parametro `damage` è usato **così com'è**, non sommato a un Tier: i bident non hanno Tier).
- Tooltip esteso con Shift (`ExpandedStatTooltip`): mostra le percentuali di perforazione.
- Le varianti glass crest si attivano/disattivano con Shift + tasto destro (componente `spectrum:activated`, `ActivatableItem`).
- **Ferocious** (`FerociousBidentItem.java:21-105`): riptide livello ≥1 anche fuori dall'acqua pagando 10 inchiostro bianco (più 10 ogni 10 tick in volo), danneggia (2) i mob entro 2 blocchi durante lo spin.
- **Fractal** (`FractalBidentItem.java:19-77`): se attivo, lancia una **copia** ("mirror image", 25 inchiostro bianco) senza perdere l'arma, velocità di lancio 5 invece di 3. Accetta anche Efficiency e Power.
- ⚠ Anche qui riparazione hardcoded `SpectrumToolTiers.MALACHITE` (riga 95).

Ricette: `malachite_bident.json` (trident + materiali malachite → Impaling VI), `ferocious_glass_crest_bident.json` (malachite_bident + moonstone_core + 8 pure_prismarine + 4 storm_stone + 4 mermaids_gem + 16 white_pigment), `fractal_glass_crest_bident.json` (come sopra ma 4 echo_shard invece di storm_stone).

> ⚠ **Conteggio**: 13 oggetti × 4 tier = 52 presuppone **un solo** bident per tier. Spectrum però ne ha **due** in glass crest (ferocious e fractal). Va deciso se tenerli entrambi (56 oggetti), unirli, o farne evolvere uno solo.

### 2.5 Chi controlla cosa con `instanceof` (✅ importante per l'estendibilità)

Comportamenti che un nostro item **eredita gratis** se estende la classe Spectrum o implementa l'interfaccia:

| Controllo | Dove | Effetto |
|---|---|---|
| `instanceof AoEBreakingTool` | `SpectrumEventListeners.java:1039` | scavo ad area |
| `instanceof WorkstaffItem` / `GlassCrestWorkstaffItem` | `WorkstaffScreen.java:39,45` | voci del menu (proiettili solo se glass crest) |
| `instanceof GreatswordItem` | `sound/GreatswordChargingSoundInstance.java:45` | suono di carica |
| `instanceof GlassCrestCrossbowItem` | `SpectrumEventListeners.java:601`, `OverchargingSoundInstance.java:45` | effetti dell'overcharge |
| `instanceof SplitDamageItem` | `SpectrumEventListeners.java:741` | danno diviso |
| `instanceof ArmorPiercingItem` | `mixin/LivingEntityMixin.java:252` | perforazione armatura |
| `instanceof Preenchanted` | `ItemStackMixin.java:57`, `EnchantmentHelperMixin.java:24` | tavolo degli incantesimi |
| `instanceof SlotBackgroundEffectProvider` / `ExtendedItemBarProvider` | `mixin/client/DrawContextMixin.java:32,76` | bordo slot, barre extra |
| `instanceof BedrockArmorItem` | `render/armor/BedrockCapeRenderer.java:65-76` | **mantello** della corazza bedrock, con la texture presa da `getArmorTexture` |

Quindi: estendere `GlassCrestWorkstaffItem` dà automaticamente menu con proiettili, AoE, ecc.

Cose **non** ereditabili (registrate per singolo item, hardcoded):
- Predicati dei modelli (`oversized`, `bident_throwing`, `pull`/`pulling`/`charged`/`projectile` delle balestre) in `registries/client/SpectrumModelPredicateProviders.java:26-76`, metodi `private static` → vanno ri-registrati dall'addon per i propri item.
- Modello 3D dell'armatura bedrock (`IClientItemExtensions` registrato solo per i 4 pezzi Spectrum, `SpectrumClientEventListeners.java:92-107`).
- Le voci del menu del workstaff: solo 1×1, 3×3, 5×5 (enum `GUIToggle` + schermata). Un 7×7 richiederebbe una propria schermata/pacchetto.
- Riparazione con `SpectrumToolTiers.MALACHITE` nei crossbow/bident.

---

## 3. Meccanismi trasversali utili all'addon

### 3.1 Interfacce in `de.dafuqs.spectrum.api.*` (✅ `src:…/api/item/`, `api/render/`, `api/ink/`)

| Interfaccia | Cosa richiede | A cosa serve |
|---|---|---|
| `Preenchanted` | `getDefaultEnchantments()` | tab creativo, tavolo incantesimi (§1.6) |
| `AoEBreakingTool` | `canUseAoE(player, stack)`; default `getAoERange` legge `spectrum:aoe` | scavo ad area |
| `ActivatableItem` | solo metodi statici `setActivated/isActivated` (componente `spectrum:activated`) | toggle on/off |
| `SplitDamageItem` | `getDamageComposition(...)` | dividere il danno tra più tipi |
| `ArmorPiercingItem extends SplitDamageItem` | `getDefenseMultiplier`, `getToughnessMultiplier`, `getProtReduction` | ignorare armatura/protezioni |
| `ExpandedStatTooltip` | `expandTooltip(...)` | righe extra del tooltip (con Shift) |
| `InkPowered` (`api/ink`) | `getUsedColors(stack)`; statici `tryDrainEnergy`, `hasAvailableInk`; default `addInkPoweredTooltip` | consumo di inchiostro dalle Ink Flask/Assortment del giocatore. Richiede l'advancement `UNLOCK_INK_USE` |
| `SlotBackgroundEffectProvider` (`api/render`) | `backgroundType`, `getBackgroundColor` | bordo animato nello slot |
| `ExtendedItemBarProvider` (`api/render`) | `barCount`, `getSignature`, `allowVanillaDurabilityBarRendering` | barre extra sotto l'icona |

**Non esiste** un'interfaccia `ExtendedEnchantable` in questa versione (✅ grep senza risultati). L'enchantability viene semplicemente dal `Tier`/`ArmorMaterial` (o da `getEnchantmentValue` vanilla); i livelli oltre il massimo vanilla si ottengono con ricette, Bedrock Anvil (config `bedrock_anvil_can_exceed_max_vanilla_enchantment_level`, default `false`, `SpectrumConfig.java:239-245`) o con l'Enchanter di Spectrum.

`ArrowheadCrossbow` **non** è di Spectrum: viene dalla libreria **Arrowhead** (`de.dafuqs:arrowhead:2.1.0+1.21.1-neoforge`, inclusa nel jar come jar-in-jar: `jar:META-INF/jarjar/arrowhead-2.1.0+1.21.1-neoforge.jar`; `build.gradle:124`). L'addon dovrà averla come dipendenza di compilazione se estende le balestre.

### 3.2 Componenti dati di Spectrum rilevanti (✅ `registries/SpectrumDataComponentTypes.java`)

| Componente | Tipo | Usato da |
|---|---|---|
| `spectrum:aoe` (riga 33) | Integer | workstaff (raggio AoE) |
| `spectrum:workstaff` (riga 66) | `WorkstaffComponent(canTill, canShoot, fortuneLevel)` | workstaff |
| `spectrum:activated` (riga 32) | Unit | bident glass crest e altri `ActivatableItem` |
| `spectrum:overcharged` (riga 53) | Float | glass crest crossbow |

Sono `public static final Supplier<DataComponentType<…>>`: l'addon può usarli (in lettura e scrittura) senza registrarne di propri.

### 3.3 Tag con effetti (✅ `jar:data/...`)

| Tag | Contenuto rilevante | Effetto |
|---|---|---|
| `spectrum:swords` → incluso in `minecraft:swords` | bedrock_sword, `#spectrum:greatswords` | ⇒ incantesimi da spada |
| `spectrum:pickaxes` / `axes` / `hoes` / `shovels` → inclusi nei corrispondenti `minecraft:*` | bedrock tools; i workstaff stanno in pickaxes+axes+hoes; i bident glass crest in `shovels` (!) | ⇒ incantesimi da attrezzo, mineable |
| `minecraft:enchantable/trident` | i 3 bident | Impaling, Riptide, Loyalty… |
| `minecraft:enchantable/crossbow` | le 3 balestre | Piercing, Quick Charge… |
| `minecraft:enchantable/durability` | aggiunge `#spectrum:bidents` e le balestre malachite/glass crest; **rimuove** i bedrock | Unbreaking/Mending |
| `spectrum:enchantable/indestructible` | = `#minecraft:enchantable/durability` | incantesimo Indestructible di Spectrum |
| `spectrum:enchantable/resonance` | = `#minecraft:enchantable/mining` (+ ender splice, exchanging staff) | Resonance (scelta nel menu del workstaff) |
| `spectrum:bedrock_equipment`, `bedrock_armor`, `malachite_equipment`, `glass_crest_equipment`, `workstaffs`, `greatswords`, `bidents`, `crossbows` | raggruppamenti | usati da advancement e da blacklist di altre mod (es. `modern_industrialization:replicator_blacklist`, `things:hardening_catalyst_blacklist`, `minecraft:freeze_immune_wearables` per l'armatura bedrock) |
| `spectrum:emissive` | (gemme, stardust…, nessun tool) | l'item viene disegnato a piena luminosità (`mixin/client/ItemRendererMixin.java:24-29`) |

⚠ `minecraft:enchantable/durability` "rimuove" esplicitamente gli id bedrock: i nostri tool indistruttibili, se messi nei tag `minecraft:swords`/`pickaxes` ecc., entrerebbero di nuovo in `enchantable/durability` (vanilla include quei tag) → serve un nostro file di tag con `"remove"` per i nostri id (🔎 come fa Spectrum).

### 3.4 Resistenza al fuoco e rarità (✅)
- `Item.Properties.fireResistant()` (componente vanilla `fire_resistant`): bedrock (tutto) e le tre balestre. **Non** workstaff, greatsword e bident.
- Rarità: tutti `Rarity.UNCOMMON`. Per i nostri tier si può usare `RARE`/`EPIC` per distinguerli.

### 3.5 Tooltip (✅)
- Testi aggiunti in `appendHoverText` con chiavi `item.spectrum.<oggetto>.tooltip…` (es. `item.spectrum.workstaff.tooltip.mining_range`, `en_us.json:5097`).
- `ExpandedStatTooltip` (righe con Shift) è chiamato da `ItemStackMixin.java:70`.
- `InkPowered.addInkPoweredTooltip` aggiunge "consuma inchiostro X".

### 3.6 Compatibilità Better Combat (✅)
`jar:data/spectrum/weapon_attributes/<id>.json` (es. `glass_crest_workstaff.json` → `{"parent":"bettercombat:pickaxe"}`, greatsword → `bettercombat:claymore`, bident → `bettercombat:trident`, bedrock_sword → `bettercombat:sword`). ATM10 include Better Combat? (🔎 da verificare): se sì, l'addon deve fornire file analoghi nel proprio namespace.

---

## 4. Asset e convenzioni di naming

### 4.1 Id e lang
- Id registry: `snake_case`, materiale come **prefisso**: `bedrock_<pezzo>`, `malachite_<oggetto>`, `glass_crest_<oggetto>`; varianti con aggettivo in testa: `ferocious_glass_crest_bident`, `fractal_glass_crest_bident`.
- Lang: `item.spectrum.<id>` (es. `"item.spectrum.glass_crest_workstaff": "Glass Crest Workstaff"`, `en_us.json:4655`). Chiavi condivise per famiglia: `item.spectrum.workstaff.*` (menu, messaggi, tooltip, `en_us.json:5072-5101`), `item.spectrum.bident.*` (`4469-4471`).
- Le classi però usano chiavi **hardcoded** `item.spectrum.workstaff.*` / `item.spectrum.bident.*` / `item.spectrum.glass_crest_ultra_greatsword.tooltip`: se le nostre classi estendono quelle di Spectrum, i tooltip ereditati useranno le traduzioni di Spectrum (bene, già tradotte).

### 4.2 Modelli e texture
- Tool/armature bedrock: modello piatto standard. `models/item/bedrock_sword.json` → parent `minecraft:item/handheld`; `bedrock_helmet.json` → parent `minecraft:item/generated`. Texture 16×16 in `textures/item/<id>.png`. Arco/balestra/canna hanno texture di stato (`bedrock_bow_pulling_0..2`, `bedrock_crossbow_standby/arrow/firework/pulling_0..2`, `bedrock_fishing_rod_cast`).
- **Oggetti "oversized"** (workstaff, greatsword, crossbow, bident malachite/glass crest): due rappresentazioni:
  - nell'inventario/GUI: texture **16×16** `<id>.png`;
  - in mano/a terra/lanciato: texture **32×32** `<id>_oversized.png`, scelta tramite override col predicato `oversized`.
  - Come funziona (✅): `registerOversizedItemPredicate` (`SpectrumModelPredicateProviders.java:194-200`) registra il predicato `oversized` che vale 1 solo se il "seed" passato è `MAGIC_OVERSIZED_SEED = -567877769` (public static). Il mixin `mixin/client/ItemRendererMixin.java:31-38` risolve gli override con quel seed in `renderStatic` (rendering in mano), e `BidentEntityRenderer.java:42` fa lo stesso per il bident lanciato. Il mixin vale per **qualsiasi** item: basta che il nostro item abbia il predicato registrato e l'override nel JSON.
- Template di display in `jar:assets/spectrum/models/templates_item/`: `handheld_oversized.json` (scala 1.75 in terza persona), `handheld_oversized_high_grip`, `handheld_oversized_xl`, `handheld_oversized_throwing` (bident in posizione di lancio), `crossbow_oversized`, `blocking_sword(_oversized)`. I nostri modelli possono usare direttamente `"parent": "spectrum:templates_item/handheld_oversized"`.
- Texture emissive: per gli item si usa il tag `spectrum:emissive` (tutto l'item a piena luce), non texture `_emissive` separate (✅ per gli item visti; 🔎 non ho controllato i blocchi).
- Armatura bedrock: `textures/armor/bedrock_armor.png` **128×128** con modello 3D custom (`render/armor/BedrockArmorModel.java`, mesh `getMeshDefinition()` public static, layer `SpectrumModelLayerLocations.BEDROCK_LAYER`) + mantello (`BedrockCapeRenderer`). Esiste anche `assets/spectrum/estrogen_armor_data/bedrock_chestplate.json` (compat con la mod Estrogen).
- Resource pack alternativo incluso: `jar:resourcepacks/spectrum_alternate/assets/spectrum/textures/item/…` (texture alternative degli stessi id).

### 4.3 Esempio completo: file del Glass Crest Workstaff (✅ `find`)

```
assets/spectrum/models/item/glass_crest_workstaff.json            parent spectrum:templates_item/handheld_oversized,
                                                                  layer0 spectrum:item/glass_crest_workstaff,
                                                                  override {oversized:1} → glass_crest_workstaff_oversized
assets/spectrum/models/item/glass_crest_workstaff_oversized.json  stesso parent, layer0 …_oversized
assets/spectrum/textures/item/glass_crest_workstaff.png           16×16
assets/spectrum/textures/item/glass_crest_workstaff_oversized.png 32×32
assets/spectrum/lang/en_us.json                                   "item.spectrum.glass_crest_workstaff"
data/spectrum/recipe/fusion_shrine/malachite/glass_crest_workstaff.json
data/spectrum/advancement/unlocks/malachite/glass_crest_workstaff.json   (sblocco ricetta)
data/spectrum/weapon_attributes/glass_crest_workstaff.json               (Better Combat)
data/spectrum/tags/item/{workstaffs,pickaxes,axes,hoes,glass_crest_equipment}.json (appartenenza)
resourcepacks/spectrum_alternate/assets/spectrum/textures/item/glass_crest_workstaff{,_oversized}.png
```

Per i bident ci sono in più `<id>_oversized_throwing.json` (override `bident_throwing: 0.5`) e per il fractal `…_oversized_projectile`; per le balestre ogni stato (standby, pulling_0..2, arrow, firework, glass, topaz, amethyst, citrine, onyx, moonstone) ha la coppia normale/`_oversized` (≈24 override in `glass_crest_crossbow.json`).

---

## 5. Implicazioni per Spectral Ascension

### 5.1 Estendere vs copiare

| Oggetto nostro | Strategia consigliata | Note |
|---|---|---|
| Piccone/ascia/pala/zappa/spada T6–Alloy | **Estendere** `SpectrumPickaxeItem`, `BedrockAxeItem`, `BedrockShovelItem`, `BedrockHoeItem`, `BedrockSwordItem` (o direttamente le classi vanilla + `Preenchanted`) | Sono classi minuscole (~25 righe). Estenderle aggiunge poco; implementare `Preenchanted` su sottoclassi vanilla è altrettanto semplice e meno legato a Spectrum. In entrambi i casi sovrascrivere `getDefaultEnchantments()`. |
| Armatura | **Estendere** `BedrockArmorItem` (abstract, costruttore public) | Si ottengono gratis: no riparazione, niente glint, piglin neutrali e **il mantello** (`BedrockCapeRenderer` lavora su `instanceof BedrockArmorItem`). Bisogna sovrascrivere `getArmorTexture` (altrimenti usa la texture bedrock) e registrare un nostro `IClientItemExtensions` che restituisca `new BedrockArmorModel(root, slot)` (classe e layer pubblici) con una texture 128×128 con la stessa disposizione UV. |
| Workstaff | **Estendere** `GlassCrestWorkstaffItem` | Si eredita menu, AoE, proiettili, cambio incantesimi. Limite: AoE massimo 5×5 dal menu. Per 7×7 o più servirebbe una nostra schermata + pacchetto che scriva `spectrum:aoe = 3` (la logica AoE accetta qualunque raggio; il costo di inchiostro cresce come 3^raggio). |
| Ultra greatsword | **Estendere** `GlassCrestGreatswordItem` | Costruttore `(Tier, int, float, float, Properties)`. `MAGIC_DAMAGE_SHARE` e il costo dello slam sono `static`/`private`: per cambiarli si sovrascrivono `getDamageComposition` e `use`. |
| Crossbow | **Estendere** `GlassCrestCrossbowItem` | Gli effetti dell'overcharge al tiro funzionano via `instanceof`. Sovrascrivere `isValidRepairItem` (hardcoded malachite) e i modificatori `getProjectileVelocityModifier`/`getDivergenceMod`. Serve Arrowhead come dipendenza di compilazione. |
| Bident | **Estendere** `FerociousBidentItem` o `FractalBidentItem` | Costruttore `(Properties, speed, damage, armorPierce, protPierce)`. Attenzione: Ferocious sovrascrive `getDefenseMultiplier`/`getProtReduction` con costanti (0.66/0.33), ignorando i parametri del costruttore → sovrascriverli di nuovo. Il bident lanciato è disegnato da `BidentEntityRenderer` col modello oversized dello stack → funziona anche per i nostri item se registriamo i predicati. |

Tier e materiali: `SpectrumToolTiers` è un **enum** (non estendibile) → creiamo un nostro enum/classe che implementa `net.minecraft.world.item.Tier`. `SpectrumArmorMaterials.register(...)` è public ma registra nel namespace **spectrum** col registrar di Spectrum → **non** usarlo: creare un nostro `DeferredRegister<ArmorMaterial>` (stessa struttura, `SpectrumArmorMaterials.java:54-77`).

### 5.2 Ricetta per l'indistruttibilità (replica fedele della bedrock)

```java
new Item.Properties()
    .stacksTo(1)
    .rarity(Rarity.RARE)
    .fireResistant()
    .durability(Integer.MAX_VALUE)   // o tier.getUses()
    .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
```
+ tag `minecraft:enchantable/durability` con `"remove": [ ...i nostri id... ]`.

🔎 Per gli speciali ereditati (workstaff/greatsword/crossbow/bident), il componente `UNBREAKABLE` dovrebbe bastare a rendere innocue tutte le chiamate `hurtAndBreak(...)` presenti nelle classi Spectrum. Il bident controlla `getDamageValue() >= getMaxDamage() - 1` prima del lancio (`MalachiteBidentItem.java:69`): con danno sempre 0 e durabilità alta non scatta. Da testare in gioco.

⚠ Upgrade da glass crest con `copy_components: true`: `transmuteCopy` copia la **patch** dello stack di partenza (es. danno accumulato sullo strumento glass crest consumato). Il nostro item ha `UNBREAKABLE` come default, quindi il valore di danno copiato non conta (🔎), ma è meglio verificarlo.

### 5.3 Ricetta per gli incantesimi "vanilla+N"

Dati verificati: bedrock **e** malachite sono già a vanilla+1 (Sharpness VI, Efficiency VI, Fortune IV, Protection V, Sweeping Edge IV, Piercing V, Impaling VI, Quick Charge IV, Power VI, Luck of the Sea IV; Silk Touch I).

Proposta coerente:
1. Ogni nostro item implementa `Preenchanted` con `getDefaultEnchantments()` = livello vanilla max + N (N = 2/3/4 per T6/T7/T8; Alloy da definire — "illimitato" non è rappresentabile come livello fisso, serve una regola di design).
2. Le ricette (Fusion Shrine o altra macchina Spectrum, con `copy_components: true` per gli upgrade) mettono gli stessi livelli in `result.components.enchantments`. Con `copy_components` il livello finale è il massimo tra quello della ricetta e quello dell'input.
3. Il tab creativo usa `Preenchanted.getDefaultEnchantedStack(lookup, item)`.
4. 🔎 Conviene un datagen (o un test) che controlli che JSON e Java coincidano, perché in Spectrum sono duplicati a mano.
5. Workstaff: `WorkstaffComponent.DEFAULT.fortuneLevel = 4`. Il livello di Fortune viene "ricordato" quando si passa a Silk Touch (`max(comp.fortuneLevel, fortune attuale)`), quindi Fortune V/VI/VII sopravvive al cambio, a patto che lo stack abbia già il componente `spectrum:workstaff` o il livello Fortune giusto. Conviene mettere nel `result` della ricetta anche il componente `spectrum:workstaff` con `fortune_level` corretto.
6. Incantesimi sopra il massimo applicati dall'utente con anvil/enchanter dipendono dalla config `bedrock_anvil_can_exceed_max_vanilla_enchantment_level` (default false) e dall'Enchanter di Spectrum → fuori dal nostro controllo, ma va documentato.

### 5.4 Client: cose da registrare noi
- `ItemProperties.register(ourItem, ResourceLocation.parse("oversized"), (s,w,e,seed) -> seed == SpectrumModelPredicateProviders.MAGIC_OVERSIZED_SEED ? 1 : 0)` per ogni oggetto speciale; `bident_throwing` per i bident; `pull/pulling/charged/projectile` per le balestre (copiare la logica di `registerMalachiteCrossbowPredicates`, righe 96-131, perché è `private`). 🔎 In NeoForge 1.21.1 `ItemProperties.register` è accessibile da codice mod (Spectrum lo chiama da `FMLClientSetupEvent`).
- `IClientItemExtensions` per il modello 3D dell'armatura.
- Better Combat: `data/<nostro_ns>/weapon_attributes/<id>.json` se presente nel pack.

### 5.5 Rischi
1. **API non stabile**: solo il package `api` (Preenchanted, AoEBreakingTool, InkPowered, ecc.) è "pubblico" per intenzione. Le classi in `items.tools` (`GlassCrestWorkstaffItem`, ecc.) sono `public` e non `final`, ma i loro costruttori/metodi possono cambiare tra versioni di Spectrum (es. aggiunta di parametri). Va fissata la dipendenza a 1.12.9 (o a un intervallo stretto) in `neoforge.mods.toml`.
2. **Valori da config**: Tier e armature di Spectrum leggono `SpectrumConfig`, letto all'inizializzazione statica. Se i nostri valori dipendono da quelli Spectrum (es. "bedrock + X"), devono leggerli in modo lazy. ATM10 potrebbe aver cambiato la config (🔎 controllare `config/spectrum*.toml` sul server, in sola lettura).
3. **Hardcode**: riparazione `SpectrumToolTiers.MALACHITE` in crossbow/bident; chiavi di traduzione fisse; costanti `private static final` (costi di inchiostro, durata overcharge, `GROUND_SLAM_CHARGE_TICKS` è un campo d'istanza `public final`). Per cambiarle si sovrascrive il metodo che le usa.
4. **Enum `GUIToggle`**: il pacchetto di rete manda l'ordinale; non si possono aggiungere modalità senza una propria schermata/pacchetto.
5. **Dipendenza Arrowhead** per le balestre (jar-in-jar di Spectrum: a runtime c'è, in compilazione va dichiarata).
6. **Mining level**: tutti i tier Spectrum usano `INCORRECT_FOR_NETHERITE_TOOL`. I minerali ATM (Allthemodium/Vibranium/Unobtainium) potrebbero richiedere tag o tier specifici di ATM → da verificare nella ricerca su ATM (🔎).
7. **Cap di armatura 30** (vanilla): l'armatura bedrock arriva già a 31; nei tier superiori la difesa grezza non conta più.
8. **Doppio bident** glass crest: impatto sul conteggio 52 (vedi §2.4).
