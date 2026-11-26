# 10 - Le armature più forti di ATM10 e i tetti degli attributi

Ricerca in sola lettura sulla tua istanza CurseForge di ATM10 (514 jar, `config/`, `kubejs/`).
Legenda: **[V]** verificato (config o bytecode letto direttamente), **[D]** dedotto.
Percorsi abbreviati: `INST` = la cartella dell'istanza sopra; `jars2/<jar>` = jar estratto in `<scratchpad>`.

## 1. Concetti in breve

- **Punti armatura** (`minecraft:generic.armor`): somma dei valori dei 4 pezzi. Ogni pezzo ha il suo valore (es. netherite 3/8/6/3 = 20).
- **Toughness** (`generic.armor_toughness`): in 1.21 il valore del material è **per pezzo** e si somma sui 4 pezzi (netherite 3 per pezzo = 12 sul set). Nel vanilla riduce l'effetto dei colpi molto forti.
- **Knockback resistance**: anche questa è per pezzo e si somma (netherite 0.1 per pezzo = 0.4 sul set). Il tetto dell'attributo è 1.0 (= immune al knockback).
- **Tetto vanilla**: armor max 30, toughness max 20. In più, la formula vanilla considera al massimo 20 punti di armatura (riduzione massima 80%).
- **Formula vanilla (1.21)**: `danno_subito = danno * (1 - min(max(armor - danno/(2 + toughness/4), armor/5), 20) / 25)`.
- **ArmorMaterial**: in 1.21 è un oggetto registrato da codice con difesa per slot, toughness, knockback resistance, enchantability. La durabilità sta nell'item, non nel material.

## 2. Tetti effettivi degli attributi nel pack

Tre cose lavorano insieme. Sono tutte **[V]**.

1. **AttributeFix** (`mods/attributefix-neoforge-1.21.1-21.1.3.jar`) con i file in `INST/config/attributefix/minecraft/`:
   - `generic.armor.json`: `modify_range=true`, `min 0.0`, **`max 1000000.0`** (default del mod).
   - `generic.armor_toughness.json`: `modify_range=true`, `min 0.0`, **`max 1000000.0`**.
   - `generic.knockback_resistance.json`: `modify_range=false`, max 1.0. Resta quindi **1.0** (e nessun altro mod lo alza).
   - Conclusione: l'armatura oltre 30 e la toughness oltre 20 **contano davvero**. Il vecchio tetto 30/20 non esiste più nel pack. Non c'è un nuovo tetto pratico (1 milione).
2. **Apothic Attributes** (`mods/ApothicAttributes-1.21.1-2.11.0.jar`), file `INST/config/apotheosis/apothic_attributes.cfg`, sezione `combat_rules`. **Sostituisce la formula del danno**:
   - `Armor Formula` = `a / (a + armor)`, con `a` = 10 se il danno è < 20, altrimenti `10 + (danno - 20)/2` (`A-Value Formula`).
   - `Protection Formula` = `1 - min(0.025 * protPoints, 0.85)`.
   - `Armor Toughness Formula` = `min(toughness * 0.02, 0.6)`: **la toughness serve solo a ridurre gli effetti "armor pierce / armor shred" dei nemici** (riduzione massima 60%, raggiunta a 30 di toughness). **Non riduce più il danno dei colpi grossi** come nel vanilla.
   - `Negative Armor Factor` = 0.015.
   - Esempio per un colpo da 10 danni (`a = 10`): armor 30 → 25% del danno subito; armor 74 → 11,9%; armor 90 → 10%. Ogni punto in più conta sempre ma con rendimenti decrescenti (nessun muro all'80%).
3. **KubeJS**: nessuno script in `INST/kubejs/` cambia gli attributi armor/toughness (cercati `generic.armor`, `armor_toughness`, `attribute` in `server_scripts`/`startup_scripts`). Tutta la pagina `kubejs/server_scripts/modpack/att_items.js` riguarda altro (nessun valore armor).

Conseguenza di design per l'addon: i valori sopra 30 sono utilizzabili e non vengono tagliati. Con la formula di Apothic, un set più forte è comunque più utile, ma la toughness oltre 30 totali non dà più nulla contro armor pierce.

## 3. Tabella: le armature più forti del pack

Ordine: punti armatura totali del set. Ordine dei pezzi: elmo / petto / gambali / stivali. "Toughness" e "KR" sono **totali sul set** (valore per pezzo x4), con il valore per pezzo tra parentesi. "Indistruttibile": sì se l'item non ha durabilità o è dichiarato unbreakable.

| # | Mod | Set | Elmo/Petto/Gambali/Stivali | Armor totale | Toughness totale (per pezzo) | KR totale (per pezzo) | Indistruttibile | Fonte |
|---|---|---|---|---|---|---|---|---|
| 1 | Silent Gear + ATM (KubeJS) | Armatura SG con materiale **Unobtainium** (main part) | 20/25/25/20 | **90** | **90** (22.5) | n.d. | no (durabilità SG) | `INST/kubejs/data/silentgear/silentgear_materials/unobtainium.json` [V per i dati, D per il calcolo finale, vedi sez. 6] |
| 2 | Silent Gear + ATM | SG **Vibranium** | 15/20/20/15 | 70 | 70 (17.5) | n.d. | no | `.../silentgear_materials/vibranium.json` [V/D] |
| 3 | Silent Gear + ATM | SG **Allthemodium** | 15/15/15/15 | 60 | 60 (15) | n.d. | no | `.../silentgear_materials/allthemodium.json` [V/D] |
| 4 | Ice and Fire | **Dragonsteel** (fuoco / ghiaccio / fulmine, stessi valori) | 17/22/19/16 | **74** | 64 (16) | 1.0 (0.25) | no (durabilità 2760/4560/4400/4080) | config `INST/config/iceandfire/iaf-common.json` (`dragonsteel*Armor`, `dragonsteelArmorToughness=16.0`, `...KnockbackResistance=0.25`) + classe `jars2/iceandfire-2.0-beta.17/com/iafenvoy/iceandfire/registry/IafArmorMaterials.class` [V] |
| 5 | Silent Gear + ATM | SG **Uru Metal** | 8/14/12/8 | 42 | **100** (25) | n.d. | no | `INST/kubejs/data/sgearmetalworks/silentgear_materials/uru_metal.json` [V/D] |
| 6 | Allthemodium (ATM) | **Unobtainium** (T8) | 8/13/11/8 | 40 | 60 (15) | 4.0 (1.0), tagliato a 1.0 | **sì** (nessun `durability`) | `jars2/allthemodium-3.2.1_mc_1.21.1/com/thevortex/allthemodium/registry/ArmorRegistries.class` [V]; indistruttibilità [D] da doc 07 |
| 7 | Forbidden Arcanus | **Tyr** | 8/12/10/7 | 37 | 12 (3) | 0.4 (0.1) | no | `jars2/forbidden_arcanus-1.21.1-2.6.2/com/stal111/forbidden_arcanus/common/item/FAArmorMaterials.class` [V; associazione set-valori D probabile] |
| 8 | Mystical Agriculture | **Awakened Supremium** | 6/12/10/8 | 36 | 14 (3.5) | 0 | no (config `unbreakableSupremiumArmor=false`) | `jars2/MysticalAgriculture-1.21.1-8.0.29/com/blakebr0/mysticalagriculture/init/ModArmorMaterials.class`; `INST/config/mysticalagriculture-common.toml` [V] |
| 9 | Allthemodium (ATM) | **Vibranium** (T7) | 6/11/9/6 | 32 | 36 (9) | 3.2 (0.8), tagliato a 1.0 | sì | `ArmorRegistries.class` come sopra [V] |
| 10 | L_Ender's Cataclysm | **Ignitium** | 6/11/9/6 | 32 | 16 (4.0) | 0.6 (0.15) | no | `jars2/L_Ender's Cataclysm 1.21.1-3.33/com/github/L_Ender/cataclysm/items/Armortier.class`; `INST/config/cataclysm-common.toml` (`armor_multiplier=1.0`, `toughness=4.0`) [V] |
| 11 | Mekanism Tools | **Refined Obsidian** | 6/12/8/5 | 31 | 20 (5) | 0.8 (0.2) | no | `INST/config/Mekanism/tools-materials-startup.toml` (`refined_obsidian*Armor`, `...Toughness=5.0`, `...KnockbackResistance=0.2`) [V] |
| 12 | Spectrum (riferimento) | **Bedrock** | 5/12/9/5 | 31 | 12 (3.0) | 1.2 (0.3), tagliato a 1.0 | no (da doc 02) | `INST/config/spectrum-startup.toml` righe 119-124 [V] |
| 13 | Mystical Agriculture | Supremium | 5/10/8/6 | 29 | 12 (3.0) | 0 | no | `ModArmorMaterials.class` [V] |
| 14 | L_Ender's Cataclysm | Cursium | 5/10/8/5 | 28 | 16 (4.0) | 0.2 (0.05) | no | `Armortier.class` + config cataclysm [V] |
| 15 | Allthemodium (ATM) | **Allthemodium** (T6) | 4/9/7/4 | 24 | 20 (5) | 2.0 (0.5), tagliato a 1.0 | sì | `ArmorRegistries.class` [V] |
| 16 | Ad Astra | Jet Suit | 4/9/7/4 | 24 | 20 (5) | 0.4 (0.1) | no | `jars2/adastra-1.21.1-1.16.26-neoforge/earth/terrarium/adastra/common/items/armor/materials/ModArmorMaterials.class` [V; associazione D] |
| 17 | Twilight Forest | Fiery | 4/9/7/4 | 24 | 6 (1.5) | 0 | no | `jars2/twilightforest-1.21.1-4.8.3345-universal/twilightforest/init/TFArmorMaterials.class` [V] |
| 18 | Advanced AE | Quantum Alloy (armatura a energia) | 4/9/6/4 | 23 | 40 (10) | 1.0 (0.25) | n.d. | `jars2/AdvancedAE-1.6.12-1.21.1/net/pedroksl/advanced_ae/common/materials/QuantumAlloy.class` [V] |
| 19 | Mekanism | **MekaSuit** | 3/8/6/3 | 20 (+ assorbimento, vedi sez. 5) | 12 (3) | 0.4 (0.1) | sì (a energia) | `INST/config/Mekanism/startup.toml` sezione `gear.mekasuit` [V] |
| 20 | Vanilla | **Netherite** (riferimento) | 3/8/6/3 | 20 | 12 (3) | 0.4 (0.1) | no | conoscenza vanilla [D] |
| 21 | Iron's Spells | Paladin / Netherite Battlemage / school armor | 3/8/6/3 (schoolArmorMap) | 20 | Paladin 16 (4), Battlemage 12 (3) | Paladin 1.6 (0.4) | no | `jars2/irons_spellbooks-1.21.1-3.16.3/io/redspace/ironsspellbooks/registries/ArmorMaterialRegistry.class` [V] |

Fuori classifica ma da conoscere:

- **Iron's Spells "Iron's Crown" (`gold_crown`)**: singolo elmo con material `DEV` = 20 armor, 20 toughness, 20 KR su un solo pezzo (tutto x20!). Nessuna ricetta né loot trovati nel pack (cercato in `kubejs`, `config` e nel jar): è un oggetto "dev", **non ottenibile in modo normale** [V per i valori, D per la non ottenibilità]. Non va usato come riferimento.
- **Draconic Evolution**: esiste solo il petto modulare (`ModularChestpiece`), con base `ArmorMaterials.DIAMOND` (8 armor, toughness 2) più uno scudo energetico separato (`ModuleStats.cfg`), non punti armatura [V da bytecode].
- **Extended Industrialization Nano Suit**: ~6 per pezzo = 24, toughness 3, KR 0.1, a energia [V parziale, valori dedotti dal loop bytecode: D].
- **Aether Valkyrie** 3/8/6/3 = 20 (t 2), **Eternal Starlight** (Aethersent, Unrealium, Starlit Diamond) 20 (t 0 / 0.5 / 3), **Ars Elemental/Elemancy** pesante 4/10/7/4 = 25 (t 4, KR 0.05-0.1), **Deeper Darker Warden** 4/9/7/4 = 24 (t 4), **Undergarden Froststeel** 3/7/6/2 = 18: tutte sotto la riga 15.
- **allthewizardgear** (`INST/config/allthewizardgear-startup.toml`): set da mago con gli stessi valori armor di ATM (4/9/7/4, 6/11/9/6, 8/13/11/8) ma toughness più bassa (4/5/6 per pezzo) e KR 0, più mana e spell power. Mai superiori ai set ATM veri [V].
- **Accessori Curios/gioielli**: `mods/irons_jewelry-1.21.1-2.0.2.jar` dà solo +2 armor o +2 toughness per gioiello (materiale allthemodium: +2 toughness) [V], trascurabile. Gli **affissi Apotheosis** (`data/apotheosis/affixes/armor/attribute/*.json`) possono aggiungere armor: es. `ironforged` fino a +8 flat a rarità mythic [V], altri affissi sono moltiplicatori [D, non analizzati uno a uno]. Non fanno parte del set base.

## 4. Il massimo del pack

Distinguo due categorie, perché cambia la risposta.

**A. Armature a statistiche fisse (item "normali")**
- **Per armatura totale: Ice and Fire Dragonsteel, 74** (17/22/19/16). Il pack ha alzato i default del mod (default nel codice 7/12/9/6 = 34, toughness 6, KR 0.1) a 17/22/19/16, toughness 16, KR 0.25 [V: default letti da `IafCommonConfig$ArmorsConfig.class`, valori effettivi da `iaf-common.json`].
- **Per toughness: sempre Dragonsteel, 64 sul set (16 per pezzo)**, davanti all'Unobtainium ATM (60, cioè 15 per pezzo).
- Subito dietro per armatura: Allthemodium **Unobtainium 40** (t 60, KR 1.0 da solo, indistruttibile), Forbidden Arcanus Tyr 37, Mystical Awakened Supremium 36.
- Il nostro T8 (unobtainium) sta quindi a 40 armor / 60 toughness: **non è il massimo dei set fissi**; Dragonsteel lo supera per armatura (74 contro 40) e per toughness (64 contro 60).

**B. Silent Gear (armature componibili, dati sovrascritti dal KubeJS di ATM)**
- Un'armatura SG costruita con materiale **Unobtainium** ha stat del materiale **armor 20/25/25/20 = 90** e **armor_toughness 90** (SG divide la toughness per 4 sul singolo pezzo: 22.5 a pezzo, 90 sul set). Quindi **90 armor è il massimo teorico del pack**, e **Uru Metal arriva a toughness 100** (25 a pezzo) con soli 42 armor.
- Fonte: `INST/kubejs/data/silentgear/silentgear_materials/unobtainium.json` (`armor/helmet` 20, `chestplate` 25, `leggings` 25, `boots` 20, `armor_toughness` 90), `.../sgearmetalworks/silentgear_materials/uru_metal.json` (8/14/12/8, toughness 100); `GearArmorItem.getArmorToughness` fa `stat / 4.0f` [V da bytecode]; la proprietà `armor` non ha tetto (max 2.1e9) in `GearProperties` [V].

**Riepilogo per decidere il nostro massimo**
- Se si prende come riferimento "il set fisso più forte": armor **74**, toughness **64** (Dragonsteel).
- Se si include Silent Gear col materiale Unobtainium: armor **90**, toughness **90** (set da 90), con Uru Metal toughness 100.
- Valori ATM nostri: T8 = 40/60, T7 = 32/36, T6 = 24/20 (Allthemodium, KR per pezzo 1.0/0.8/0.5).

## 5. Note su armature con bonus dinamici

- **MekaSuit** (`INST/config/Mekanism/gear.toml`, `startup.toml`): armor base 3/8/6/3 = 20, toughness 3, KR 0.1 per pezzo. Il vero valore è l'**assorbimento**: con il set completo e abbastanza energia, assorbe il 100% (`unspecifiedDamageReductionRatio = 1.0`, `magicDamageReductionRatio = 1.0`, `fallDamageReductionRatio = 1.0`) dei danni non esplicitamente gestiti che non ignorano l'armatura, pagando **250 000 J per ogni punto di danno** (`mekasuit.energy_usage.damage`). Capacità base 40 000 000 J (ATM l'ha portata da 16 M a 40 M, vedi `kubejs/server_scripts/mods/Mekanism/Rebalance/rebalance.md`), scalata quadraticamente dagli upgrade. In pratica è la protezione più forte "a energia" finché non finiscono i joule. Nel pack il MekaSuit si crafta partendo dai pezzi di Unobtainium ATM (`kubejs/server_scripts/mods/Mekanism/Rebalance/Gear.js`) [V].
- **Silent Gear**: i valori dipendono dalle parti e dalla qualità (`affectedByGrades=true`, operazione `AVERAGE` tra le parti) [V]; il valore nelle tabelle è quello del solo materiale principale.
- **Quantum Alloy (Advanced AE)**: armatura a energia con bonus di velocità/step (`INST/config/advanced_ae-common.toml`, sezione `quantum armor`), armor 23, toughness 10 per pezzo [V].
- **Draconic Evolution**: scudo energetico separato dall'armatura, non conta nei punti [V].

## 6. Incertezze

1. **Silent Gear** [D]: ho letto i dati dei materiali (`armor/<slot>`, `armor_toughness`) e il codice di `GearArmorItem`, ma non ho simulato in gioco una vera armatura SG. Il valore effettivo di un pezzo può cambiare con altre parti dell'armatura (media pesata `AVERAGE`), con i "grade" del materiale (affectedByGrades) e con i tratti. Non ho verificato come il codice mappi `armor/helmet` ecc. sul pezzo (deduzione dai nomi). La KR SG viene da `KNOCKBACK_RESISTANCE / 10` per pezzo e i materiali ATM non la definiscono (n.d.).
2. **Associazione set-valori** per classi con più `ArmorMaterial` [D, molto probabile]: ho assunto che la lambda `static$N` (mappa difesa) sia quella immediatamente accanto al material che la usa, come nel doc 07. Verificato esplicitamente solo per Forbidden Arcanus (ordine delle chiamate `register` letto nel `static {}`) e per Cataclysm (valori coerenti con i nomi).
3. **Ice and Fire**: i valori Dragonsteel sono quelli di `iaf-common.json`, che deviano dai default del codice (34/6/0.1). Non so se la modifica sia di ATM, di uno script o dell'aggiornamento del mod: l'ho trovato così. Se il file venisse rigenerato, tornerebbero i default.
4. **Copertura**: ho scansionato le classi dei jar con riferimento a `ArmorMaterial` + `ArmorItem$Type` (≈70 jar) e i `data/` con `generic.armor`. Mod che assegnano armatura solo via `ItemAttributeModifiers` custom, via Curios/Accessories o via mixin potrebbero essere sfuggiti. Non ho analizzato in dettaglio: Create, Immersive Engineering (Steel 2/6/7/2 = 17), Pneumatic Craft, Reliquary, Occultism, Minecolonies, SecurityCraft, SpectralDecorations (addon Spectrum già presente nel pack, 83 file armor-like: non letto).
5. **Mystical Agriculture**: valori dal bytecode, non ho verificato se le ricette dei pezzi Awakened Supremium sono attive in ATM10 (esistono i file `data/mysticalagriculture/recipe/gear/awakened_supremium_*.json` nel jar) né se un datapack le rimuove [D].
6. **Indistruttibile** per ATM Allthemodium/Vibranium/Unobtainium: dedotto dall'assenza di `.durability()` (doc 07 [D]); per MekaSuit dedotto dalla natura a energia [D].
7. **Formula di Apothic** [V come config]: il file è presente e coincide con i default del mod, ma non ho verificato in gioco che il mod la applichi al posto della formula vanilla (comportamento documentato nel file stesso).
8. Knockback resistance: Allthemodium, Vibranium, Unobtainium e Dragonsteel superano 1.0 sul set (somma per pezzo) e vengono tagliati a 1.0 dal tetto dell'attributo, che il pack non alza.

## 7. File letti (principali)

- `INST/config/attributefix/minecraft/generic.armor.json`, `generic.armor_toughness.json`, `generic.knockback_resistance.json`
- `INST/config/apotheosis/apothic_attributes.cfg`
- `INST/config/iceandfire/iaf-common.json`; `INST/config/Mekanism/startup.toml`, `gear.toml`, `tools-materials-startup.toml`; `INST/config/cataclysm-common.toml`; `INST/config/spectrum-startup.toml`; `INST/config/mysticalagriculture-common.toml`; `INST/config/allthewizardgear-startup.toml` (set da mago, non le armature ATM)
- `INST/kubejs/data/silentgear/silentgear_materials/{allthemodium,vibranium,unobtainium}.json`, `INST/kubejs/data/sgearmetalworks/silentgear_materials/uru_metal.json`
- `INST/kubejs/server_scripts/mods/Mekanism/Rebalance/{Gear.js,rebalance.md}`
- Classi `ArmorMaterial` dei jar elencati in tabella (`javap -p -c -constants`).
- Documenti locali: `ricerca/07-allthemodium-e-atm10.md`, `.../02-oggetti-bedrock-e-speciali.md`.
