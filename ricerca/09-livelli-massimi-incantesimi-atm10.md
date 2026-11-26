# 09 - Livelli massimi degli incantesimi in ATM10 (istanza CurseForge di ATM10)

Ricerca a livello di dati sull'istanza CurseForge di ATM10 (NeoForge 1.21.1). Tutto in sola lettura. Legenda: **[V]** verificato leggendo file/bytecode, **[D]** dedotto. Abbreviazioni: `CFG` = `<istanza>/config/apotheosis/enchantments.cfg` (numero = riga del blocco dell'incantesimo; il valore e' la riga `I:"Max Level"`); `jar(X)` = `<istanza>/mods/X*.jar`.

## 1. Concetti

- **Incantesimo data-driven (1.21)**: ogni incantesimo e' un file `data/<ns>/enchantment/<nome>.json` con `max_level`, `min_cost`/`max_cost`, `supported_items`, `weight`... Nessun codice Java lo definisce piu'. Il valore `max_level` e' quello che restituisce `Enchantment.getMaxLevel()` di vanilla.
- **Override di datapack**: un file con lo stesso percorso in un altro pack sostituisce quello originale (vince il pack con priorita' piu' alta; il pack KubeJS sta in cima, l'ordine tra jar di mod diverse non e' garantito). In ATM10 ho trovato solo 3 override di incantesimi vanilla e **nessuno cambia `max_level`** (sezione 5).
- **Come Apothic Enchanting cambia i massimi [V]**: NON riscrive i JSON. Un coremod ASM (`coremods/ench/ench_info_redirector.js` nel jar ApothicEnchanting) sostituisce le chiamate a `Enchantment.getMaxLevel()` con `EnchHooks.getMaxLevel(...)`, che legge `EnchantmentInfo.getMaxLevel()`, cioe' il valore `Max Level` del file di config `config/apotheosis/enchantments.cfg` (classe `EnchantmentInfo.load`, chiavi `Max Level`, `Max Loot Level`, `Forced Level Cap`, `Max/Min Power Function`). Il default di `Max Level` e' calcolato da `ApothicEnchanting.getDefaultMaxLevel` (il livello piu' alto il cui costo minimo resta entro la potenza massima 200 del tavolo; 1 se l'originale e' 1). Il file di config presente nell'istanza ha **tutti i `Max Level` uguali al default** (verificato programmaticamente: unica differenza `miners_fervor`, vedi sezione 5), quindi i valori sotto sono quelli standard di Apothic 1.6.4, non modifiche manuali dell'ATM team.
- Hook sul "loot" [V]: `ench_info_loot_redirector.js` sostituisce `getMaxLevel` con `getMaxLootLevel` in `EnchantRandomlyFunction`, `VillagerTrades$EnchantBookForEmeralds`, `EnchantmentHelper`, `SingleEnchantment`. `Max Loot Level` nel CFG = livello originale (es. sharpness 5): loot e trade dei villager NON superano il vanilla.

## 2. Dove vale il nuovo massimo (tavolo, incudine) [V]

| Sistema | Usa il massimo Apothic (CFG `Max Level`)? | Prova |
|---|---|---|
| Incudine vanilla (`AnvilMenu`) | **Si**: il coremod la include | `coremods/ench/ench_info_redirector.js` (lista classi) + `mixin/AnvilMenuMixin` |
| Libri incantati (`EnchantedBookItem`), `/enchant` (`EnchantCommand`), tab creativa | Si | stesso coremod |
| Tavolo incantesimi Apothic | **Si**: `ApothEnchantmentHelper.lambda$getAvailableEnchantmentResults$4` cicla da `EnchantmentInfo.getMaxLevel()` scendendo, offrendo il primo livello la cui finestra [`getMinPower`, `getMaxPower`] contiene la potenza scelta | bytecode di `dev/shadowsoffire/apothic_enchanting/table/ApothEnchantmentHelper` |
| Biblioteca Apothic (`EnchLibraryTile.getEnchantmentCap`) | Si | bytecode |
| Loot / villager / EnchantRandomly | **No**, usa `Max Loot Level` = originale | coremod loot |
| Cap forzato (`Forced Level Cap`) | Mai attivo: e' `-1` in tutti i 161 blocchi del CFG | `ApothEnchEvents.clamp` usa `levelCap()` solo se != -1 |
| **Spectrum** `EnchantmentUpgradeRecipe.isInNormalRange` ("range normale" prima dell'overenchanting) | **No**: usa il `getMaxLevel()` vanilla = `max_level` del JSON (es. sharpness 5, non 9) | sorgente `src/.../recipe/enchanter/EnchantmentUpgradeRecipe.java:204`; la classe non e' nella lista del coremod |
| **Spectrum** Incudine di Bedrock | **No**: cap a `enchantment.getMaxLevel()` del JSON, perche' `bedrock_anvil_can_exceed_max_vanilla_enchantment_level = false` (`config/spectrum-startup.toml:33`) | sorgente `BedrockAnvilScreenHandler.java:197-199` |

Il tavolo di Apothic arriva davvero al massimo solo con potenza alta (Eterna/Quanta): la potenza massima di default e' 200 [V `PowerFunction$DefaultMaxPowerFunction`: 200]; se il massimo di un incantesimo sia raggiungibile in pratica con gli scaffali ATM10 non e' stato verificato [D]. Con incudine + libri il massimo CFG e' raggiungibile combinando libri dello stesso livello.

**ApothicAttributes**: nessuna classe del jar `ApothicAttributes-1.21.1-2.11.0.jar` referenzia `getMaxLevel` ne' enchantment (grep sulle classi, 0 risultati): irrilevante per i livelli [V].

## 3. Altre fonti di livelli alti

1. **Spectrum - ricette `spectrum:enchantment_upgrade`** [V]: `jar(spectrum):data/spectrum/recipe/enchantment_upgrade/{minecraft,spectrum}/*.json`, 45 file (+ nessuna altra mod ne ha: ho cercato `enchantment_upgrade` in tutti i jar, solo spectrum). Cap = `levels` + 1 (`EnchantmentUpgradeRecipe.getLevelCap`, riga 144), si applica a libri incantati (`stored_enchantments`), +1 livello per ricetta. Sopra `max_level` del JSON serve l'advancement `OVERENCHANTING` (`EnchanterBlockEntity.java:818`): il nostro addon lo sbloccherebbe. Colonna "cap overenchanting" della tabella. Per quasi tutti gli incantesimi il cap Spectrum (es. Sharpness/Efficiency/Power/Protection 8) e' **inferiore** al massimo Apothic (9): l'overenchanting Spectrum non lo supera. Lo supera solo per `thorns` (6 > 5) e `swift_sneak` (6 > 5), marcati `(>eff)`. Per gli incantesimi `spectrum:*` il cap e' sempre <= massimo Apothic.
2. **Oggetti Spectrum preimpostati** [V] (`jar(spectrum):data/spectrum/recipe/fusion_shrine/bedrock/*.json`): spada Sharpness 6, ascia/pala/cesoie Efficiency 6, arco Power 6, picconi/zappa Fortune 4, armature Protection 5 / Fire 5 / Blast 5 / Projectile 5, balestra Quick Charge 4, canna Luck of the Sea 4. Malachite: bident Impaling 6, balestra Piercing 5. Pedestal: Lure 3, Fortune 3, Looting 3, Razing 3. Tutti <= massimo Apothic.
3. **Apotheosis 8.9.0 (jar `Apotheosis-1.21.1-8.9.0.jar`)** [V il contenuto, D la raggiungibilita']:
   - `data/apotheosis/gear_sets/pinnacle/**` (boss/loot di tier "pinnacle"): **15** per Sharpness, Efficiency, Fortune, Looting, Unbreaking, Power, Piercing, Feather Falling, Respiration, Thorns; **10** Protection; Apothic: Scavenger 10, Boon of the Earth 10, Crescendo of Bolts 10, Capturing 10, Icy Thorns 15, Shield Bash 15, Reflective Defenses 15. Superano il massimo CFG. In tabella indicati con `*` e "[D] speciale".
   - `data/apotheosis/wanderer_trades/rare_gear/*.json` (mercante errante, richiede Apothic Enchanting + Spawners): Sharpness 10 (bonesplitter, eternal_vigilance), Efficiency 10 (treecapitator), Rebounding 10 (thunder_forged_legguards), Life Mending 5, Scavenger 5.
   - `data/apotheosis/gear_sets/{summit,ascent,gateway_only}/**` e le altre: tutti <= massimo CFG.
4. **Altri jar**: ricette/loot con livelli (Ars Nouveau, Ars Elemental, Ars Additions spellweave 1-3, EvilCraft vengeance pickaxe Fortune 5, Tombstone loot table libri fino a 10, Twilight Forest equip, Refined Storage upgrade, Minecolonies, Actually Additions lens...) sono tutti **<= massimo CFG**: nessun altro livello supera il massimo effettivo. Script: `scratchpad/scripts/scan_levels.py`, output `levels_scan.tsv` (154 righe sopra il massimo, tutte in Apotheosis).
5. **KubeJS dell'istanza**: `kubejs/server_scripts/**` e `startup_scripts/**` non contengono livelli superiori al massimo (solo Unbreaking 1, Mending 1, Fortune 5/Vengeance 3 nella pickaxe EvilCraft [V]).

## 4. Tabella principale

Colonne: **max originale** = `max_level` del JSON del jar della mod (per i vanilla: valore vanilla 1.21.1, confermato dal commento "originally N" del CFG); **max effettivo ATM10** = `Max Level` del CFG, applicato a tavolo/incudine/libri; **cap overenchanting** = `levels`+1 della ricetta Spectrum (`(>eff)` se supera il massimo effettivo); **massimo ATM10** = massimo di tutte le fonti non-comando; `*` = il valore deriva da una fonte speciale (Apotheosis pinnacle/wanderer, [D] per la raggiungibilita' in gioco: se la escludi, usa la colonna "max effettivo").

Ordine: prima i vanilla da arma/attrezzo/armatura, poi Spectrum, Apothic, altre mod. Tutte le 42 voci vanilla + 80 non-vanilla del CFG sono presenti (CFG e jar coincidono 1:1, verificato).

### A. Vanilla: armi, attrezzi, archi/tridenti

| id | max originale | max effettivo ATM10 | max Spectrum overenchanting (cap ricetta) | **massimo ATM10** | fonte |
|---|---|---|---|---|---|
| `minecraft:sharpness` | 5 | 9 [V] | 8 | **15*** | CFG L1927; `jar(ApothicEnchanting):data/minecraft/enchantment/sharpness.json` ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/enchanted_yeti.json` |
| `minecraft:smite` | 5 | 10 [V] | 8 | **10** | CFG L2019; vanilla MC 1.21.1 |
| `minecraft:bane_of_arthropods` | 5 | 10 [V] | 4 | **10** | CFG L55; vanilla MC 1.21.1 |
| `minecraft:sweeping_edge` | 3 | 8 [V] | 7 | **8** | CFG L2272; vanilla MC 1.21.1 |
| `minecraft:fire_aspect` | 2 | 5 [V] | 4 | **5** | CFG L653; vanilla MC 1.21.1 |
| `minecraft:knockback` | 2 | 5 [V] | 5 | **5** | CFG L975; vanilla MC 1.21.1 |
| `minecraft:looting` | 3 | 8 [V] | 6 | **15*** | CFG L1090; vanilla MC 1.21.1 ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/enchanted_yeti.json` |
| `minecraft:breach` | 4 | 8 [V] | 5 | **8** | CFG L193; vanilla MC 1.21.1 |
| `minecraft:density` | 5 | 10 [V] | 5 | **10** | CFG L469; vanilla MC 1.21.1 |
| `minecraft:wind_burst` | 3 | 8 [V] | 5 | **8** | CFG L2456; vanilla MC 1.21.1 |
| `minecraft:efficiency` | 5 | 9 [V] | 8 | **15*** | CFG L561; vanilla MC 1.21.1 ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/enchanted_yeti.json` |
| `minecraft:fortune` | 3 | 8 [V] | 5 | **15*** | CFG L745; vanilla MC 1.21.1 ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/enchanted_yeti.json` |
| `minecraft:silk_touch` | 1 | 1 [V] | — | **1** | CFG L1973; vanilla MC 1.21.1 |
| `minecraft:unbreaking` | 3 | 8 [V] | 6 | **15*** | CFG L2364; vanilla MC 1.21.1 ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/ranged/enchanted_netherite.json` |
| `minecraft:mending` | 1 | 1 [V] | — | **1** | CFG L1251; vanilla MC 1.21.1 |
| `minecraft:power` | 5 | 9 [V] | 8 | **15*** | CFG L1513; vanilla MC 1.21.1 ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/ranged/enchanted_yeti.json` |
| `minecraft:punch` | 2 | 5 [V] | 5 | **5** | CFG L1582; vanilla MC 1.21.1 |
| `minecraft:flame` | 1 | 1 [V] | — | **1** | CFG L722; vanilla MC 1.21.1 |
| `minecraft:infinity` | 1 | 1 [V] | — | **1** | CFG L929; vanilla MC 1.21.1 |
| `minecraft:piercing` | 4 | 8 [V] | 8 | **15*** | CFG L1398; vanilla MC 1.21.1 ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/ranged/enchanted_netherite.json` |
| `minecraft:quick_charge` | 3 | 5 [V] | 5 | **5** | CFG L1628; vanilla MC 1.21.1 |
| `minecraft:multishot` | 1 | 1 [V] | — | **1** | CFG L1306; vanilla MC 1.21.1 |
| `minecraft:impaling` | 5 | 10 [V] | 8 | **10** | CFG L860; vanilla MC 1.21.1 |
| `minecraft:loyalty` | 3 | 9 [V] | 4 | **9** | CFG L1113; vanilla MC 1.21.1 |
| `minecraft:riptide` | 3 | 9 [V] | 4 | **9** | CFG L1812; vanilla MC 1.21.1 |
| `minecraft:channeling` | 1 | 1 [V] | — | **1** | CFG L308; `jar(silent):data/minecraft/enchantment/channeling.json` |
| `minecraft:protection` | 4 | 8 [V] | 8 | **10*** | CFG L1559; `jar(ApothicEnchanting):data/minecraft/enchantment/protection.json` ; [D] speciale 10: `Apotheosis:gear_sets/pinnacle/spawners/enchanted_yeti.json` |

### B. Vanilla: armature e utilità

| id | max originale | max effettivo ATM10 | max Spectrum overenchanting (cap ricetta) | **massimo ATM10** | fonte |
|---|---|---|---|---|---|
| `minecraft:fire_protection` | 4 | 9 [V] | 8 | **9** | CFG L676; vanilla MC 1.21.1 |
| `minecraft:blast_protection` | 4 | 9 [V] | 8 | **9** | CFG L124; vanilla MC 1.21.1 |
| `minecraft:projectile_protection` | 4 | 11 [V] | 8 | **11** | CFG L1536; vanilla MC 1.21.1 |
| `minecraft:feather_falling` | 4 | 11 [V] | 6 | **15*** | CFG L630; vanilla MC 1.21.1 ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/spawners/enchanted_yeti.json` |
| `minecraft:thorns` | 3 | 5 [V] | 6 (>eff) | **15*** | CFG L2341; vanilla MC 1.21.1 ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/spawners/enchanted_yeti.json` |
| `minecraft:respiration` | 3 | 7 [V] | 6 | **15*** | CFG L1766; vanilla MC 1.21.1 ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/spawners/enchanted_yeti.json` |
| `minecraft:aqua_affinity` | 1 | 1 [V] | — | **1** | CFG L9; vanilla MC 1.21.1 |
| `minecraft:depth_strider` | 3 | 7 [V] | 3 | **7** | CFG L492; vanilla MC 1.21.1 |
| `minecraft:frost_walker` | 2 | 7 [V] | 4 | **7** | CFG L768; vanilla MC 1.21.1 |
| `minecraft:soul_speed` | 3 | 7 [V] | 4 | **7** | CFG L2111; vanilla MC 1.21.1 |
| `minecraft:swift_sneak` | 3 | 5 [V] | 6 (>eff) | **6** | CFG L2295; vanilla MC 1.21.1 |
| `minecraft:luck_of_the_sea` | 3 | 8 [V] | 5 | **8** | CFG L1136; vanilla MC 1.21.1 |
| `minecraft:lure` | 3 | 8 [V] | 5 | **8** | CFG L1159; vanilla MC 1.21.1 |
| `minecraft:binding_curse` | 1 | 1 [V] | — | **1** | CFG L101; vanilla MC 1.21.1 |
| `minecraft:vanishing_curse` | 1 | 1 [V] | — | **1** | CFG L2410; vanilla MC 1.21.1 |

### C. Spectrum

| id | max originale | max effettivo ATM10 | max Spectrum overenchanting (cap ricetta) | **massimo ATM10** | fonte |
|---|---|---|---|---|---|
| `spectrum:big_catch` | 3 | 7 [V] | 3 | **7** | CFG L2732; `jar(spectrum):data/spectrum/enchantment/big_catch.json` |
| `spectrum:clovers_favor` | 3 | 7 [V] | 6 | **7** | CFG L2778; `jar(spectrum):data/spectrum/enchantment/clovers_favor.json` |
| `spectrum:disarming` | 2 | 6 [V] | 4 | **6** | CFG L2801; `jar(spectrum):data/spectrum/enchantment/disarming.json` |
| `spectrum:exuberance` | 5 | 9 [V] | 8 | **9** | CFG L2824; `jar(spectrum):data/spectrum/enchantment/exuberance.json` |
| `spectrum:first_strike` | 2 | 6 [V] | 5 | **6** | CFG L2870; `jar(spectrum):data/spectrum/enchantment/first_strike.json` |
| `spectrum:foundry` | 1 | 1 [V] | — | **1** | CFG L2893; `jar(spectrum):data/spectrum/enchantment/foundry.json` |
| `spectrum:improved_critical` | 2 | 6 [V] | 4 | **6** | CFG L3077; `jar(spectrum):data/spectrum/enchantment/improved_critical.json` |
| `spectrum:indestructible` | 1 | 1 [V] | — | **1** | CFG L3100; `jar(spectrum):data/spectrum/enchantment/indestructible.json` |
| `spectrum:inertia` | 3 | 7 [V] | 5 | **7** | CFG L3123; `jar(spectrum):data/spectrum/enchantment/inertia.json` |
| `spectrum:inexorable` | 1 | 1 [V] | — | **1** | CFG L3146; `jar(spectrum):data/spectrum/enchantment/inexorable.json` |
| `spectrum:inventory_insertion` | 1 | 1 [V] | — | **1** | CFG L3169; `jar(spectrum):data/spectrum/enchantment/inventory_insertion.json` |
| `spectrum:pest_control` | 1 | 1 [V] | — | **1** | CFG L3215; `jar(spectrum):data/spectrum/enchantment/pest_control.json` |
| `spectrum:razing` | 3 | 7 [V] | 5 | **7** | CFG L3284; `jar(spectrum):data/spectrum/enchantment/razing.json` |
| `spectrum:resonance` | 1 | 1 [V] | — | **1** | CFG L3330; `jar(spectrum):data/spectrum/enchantment/resonance.json` |
| `spectrum:serendipity_reel` | 2 | 6 [V] | 3 | **6** | CFG L3399; `jar(spectrum):data/spectrum/enchantment/serendipity_reel.json` |
| `spectrum:sniping` | 2 | 6 [V] | 5 | **6** | CFG L3422; `jar(spectrum):data/spectrum/enchantment/sniping.json` |
| `spectrum:steadfast` | 1 | 1 [V] | — | **1** | CFG L3514; `jar(spectrum):data/spectrum/enchantment/steadfast.json` |
| `spectrum:tight_grip` | 2 | 7 [V] | 4 | **7** | CFG L3583; `jar(spectrum):data/spectrum/enchantment/tight_grip.json` |
| `spectrum:treasure_hunter` | 3 | 7 [V] | 5 | **7** | CFG L3629; `jar(spectrum):data/spectrum/enchantment/treasure_hunter.json` |
| `spectrum:voiding` | 1 | 1 [V] | — | **1** | CFG L3652; `jar(spectrum):data/spectrum/enchantment/voiding.json` |

### D. Apothic Enchanting

| id | max originale | max effettivo ATM10 | max Spectrum overenchanting (cap ricetta) | **massimo ATM10** | fonte |
|---|---|---|---|---|---|
| `apothic_enchanting:berserkers_fury` | 3 | 3 [V] | — | **3** | CFG L78; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/berserkers_fury.json` |
| `apothic_enchanting:boon_of_the_earth` | 5 | 5 [V] | — | **10*** | CFG L170; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/boon_of_the_earth.json` ; [D] speciale 10: `Apotheosis:gear_sets/pinnacle/apothic/enchanted_yeti.json` |
| `apothic_enchanting:chainsaw` | 1 | 1 [V] | — | **1** | CFG L285; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/chainsaw.json` |
| `apothic_enchanting:chromatic` | 1 | 1 [V] | — | **1** | CFG L354; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/chromatic.json` |
| `apothic_enchanting:crescendo_of_bolts` | 5 | 5 [V] | — | **10*** | CFG L400; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/crescendo_of_bolts.json` ; [D] speciale 10: `Apotheosis:gear_sets/pinnacle/ranged/apothic/enchanted_netherite.json` |
| `apothic_enchanting:endless_quiver` | 1 | 1 [V] | — | **1** | CFG L584; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/endless_quiver.json` |
| `apothic_enchanting:growth_serum` | 1 | 1 [V] | — | **1** | CFG L814; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/growth_serum.json` |
| `apothic_enchanting:icy_thorns` | 3 | 5 [V] | — | **15*** | CFG L837; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/icy_thorns.json` ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/ranged/apothic/enchanted_netherite.json` |
| `apothic_enchanting:infusion` | 1 | 1 [V] | — | **1** | CFG L952; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/infusion.json` |
| `apothic_enchanting:knowledge_of_the_ages` | 3 | 3 [V] | — | **3** | CFG L998; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/knowledge_of_the_ages.json` |
| `apothic_enchanting:life_mending` | 3 | 3 [V] | — | **5*** | CFG L1021; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/life_mending.json` ; [D] speciale 5: `Apotheosis:wanderer_trades/rare_gear/eternal_vigilance.json` |
| `apothic_enchanting:miners_fervor` | 5 | 1 [V] | — | **1** | CFG L1274; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/miners_fervor.json` ; DISABILITATO da KubeJS (vedi conflitti) |
| `apothic_enchanting:natures_blessing` | 3 | 7 [V] | — | **7** | CFG L1352; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/natures_blessing.json` |
| `apothic_enchanting:rebounding` | 3 | 5 [V] | — | **10*** | CFG L1697; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/rebounding.json` ; [D] speciale 10: `Apotheosis:wanderer_trades/rare_gear/thunder_forged_legguards.json` |
| `apothic_enchanting:reflective_defenses` | 5 | 7 [V] | — | **15*** | CFG L1720; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/reflective_defenses.json` ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/apothic/enchanted_yeti.json` |
| `apothic_enchanting:scavenger` | 3 | 3 [V] | — | **10*** | CFG L1835; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/scavenger.json` ; [D] speciale 10: `Apotheosis:gear_sets/pinnacle/apothic/enchanted_yeti.json` |
| `apothic_enchanting:shield_bash` | 4 | 7 [V] | — | **15*** | CFG L1950; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/shield_bash.json` ; [D] speciale 15: `Apotheosis:gear_sets/pinnacle/apothic/enchanted_yeti.json` |
| `apothic_enchanting:stable_footing` | 1 | 1 [V] | — | **1** | CFG L2226; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/stable_footing.json` |
| `apothic_enchanting:tempting` | 1 | 1 [V] | — | **1** | CFG L2318; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/tempting.json` |
| `apothic_enchanting:worker_exploitation` | 1 | 1 [V] | — | **1** | CFG L2479; `jar(ApothicEnchanting):data/apothic_enchanting/enchantment/worker_exploitation.json` |

### E. Altre mod

| id | max originale | max effettivo ATM10 | max Spectrum overenchanting (cap ricetta) | **massimo ATM10** | fonte |
|---|---|---|---|---|---|
| `ad_astra_giselle_addon:acid_rain_proof` | 1 | 1 [V] | — | **1** | CFG L2709; `jar(Ad):data/ad_astra_giselle_addon/enchantment/acid_rain_proof.json` |
| `ad_astra_giselle_addon:gravity_normalizing` | 1 | 1 [V] | — | **1** | CFG L2985; `jar(Ad):data/ad_astra_giselle_addon/enchantment/gravity_normalizing.json` |
| `ad_astra_giselle_addon:space_breathing` | 1 | 1 [V] | — | **1** | CFG L3445; `jar(Ad):data/ad_astra_giselle_addon/enchantment/space_breathing.json` |
| `ad_astra_giselle_addon:space_fire_proof` | 1 | 1 [V] | — | **1** | CFG L3468; `jar(Ad):data/ad_astra_giselle_addon/enchantment/space_fire_proof.json` |
| `apothic_spawners:capturing` | 3 | 7 [V] | — | **10*** | CFG L239; `jar(ApothicSpawners):data/apothic_spawners/enchantment/capturing.json` ; [D] speciale 10: `Apotheosis:gear_sets/pinnacle/spawners/enchanted_yeti.json` |
| `ars_additions:spellweave` | 3 | 27 [V] | — | **27** | CFG L3491; `jar(ars_additions):data/ars_additions/enchantment/spellweave.json` |
| `ars_elemental:mirror_shield` | 3 | 7 [V] | — | **7** | CFG L1283; `jar(ars_elemental):data/ars_elemental/enchantment/mirror_shield.json` |
| `ars_elemental:soulbound` | 1 | 1 [V] | — | **1** | CFG L2134; `jar(ars_elemental):data/ars_elemental/enchantment/soulbound.json` |
| `ars_nouveau:mana_boost` | 3 | 7 [V] | — | **7** | CFG L1205; `jar(ars_nouveau):data/ars_nouveau/enchantment/mana_boost.json` |
| `ars_nouveau:mana_regen` | 3 | 7 [V] | — | **7** | CFG L1228; `jar(ars_nouveau):data/ars_nouveau/enchantment/mana_regen.json` |
| `ars_nouveau:reactive` | 4 | 8 [V] | — | **8** | CFG L1674; `jar(ars_nouveau):data/ars_nouveau/enchantment/reactive.json` |
| `astralsorcery:scorching_heat` | 1 | 1 [V] | — | **1** | CFG L3376; `jar(AstralSorcery):data/astralsorcery/enchantment/scorching_heat.json` |
| `botanypots:idle_hands` | 1 | 1 [V] | — | **1** | CFG L3054; `jar(botanypots):data/botanypots/enchantment/idle_hands.json` |
| `create:capacity` | 3 | 6 [V] | — | **6** | CFG L2755; `jar(create):data/create/enchantment/capacity.json` |
| `create:potato_recovery` | 3 | 6 [V] | — | **6** | CFG L3238; `jar(create):data/create/enchantment/potato_recovery.json` |
| `deeperdarker:catalysis` | 3 | 5 [V] | — | **5** | CFG L262; `jar(deeperdarker):data/deeperdarker/enchantment/catalysis.json` |
| `deeperdarker:reverberation` | 3 | 8 [V] | — | **8** | CFG L3353; `jar(deeperdarker):data/deeperdarker/enchantment/reverberation.json` |
| `deeperdarker:sculk_smite` | 5 | 10 [V] | — | **10** | CFG L1858; `jar(deeperdarker):data/deeperdarker/enchantment/sculk_smite.json` |
| `deeperdarker:volume` | 4 | 9 [V] | — | **9** | CFG L3675; `jar(deeperdarker):data/deeperdarker/enchantment/volume.json` |
| `draconicevolution:reaper` | 5 | 9 [V] | — | **9** | CFG L3307; `jar(Draconic):data/draconicevolution/enchantment/reaper.json` |
| `dungeons_arise:discharge` | 3 | 5 [V] | — | **5** | CFG L2525; `jar(DungeonsArise):data/dungeons_arise/enchantment/discharge.json` |
| `dungeons_arise:ensnaring` | 4 | 6 [V] | — | **6** | CFG L2548; `jar(DungeonsArise):data/dungeons_arise/enchantment/ensnaring.json` |
| `dungeons_arise:lolths_curse` | 1 | 1 [V] | — | **1** | CFG L2571; `jar(DungeonsArise):data/dungeons_arise/enchantment/lolths_curse.json` |
| `dungeons_arise:purification` | 3 | 4 [V] | — | **4** | CFG L2594; `jar(DungeonsArise):data/dungeons_arise/enchantment/purification.json` |
| `dungeons_arise:voltaic_shot` | 1 | 1 [V] | — | **1** | CFG L2663; `jar(DungeonsArise):data/dungeons_arise/enchantment/voltaic_shot.json` |
| `eternal_starlight:abyssal_touch` | 2 | 6 [V] | — | **6** | CFG L2686; `jar(eternalstarlight):data/eternal_starlight/enchantment/abyssal_touch.json` |
| `eternal_starlight:fearless` | 2 | 6 [V] | — | **6** | CFG L607; `jar(eternalstarlight):data/eternal_starlight/enchantment/fearless.json` |
| `eternal_starlight:fertile` | 3 | 7 [V] | — | **7** | CFG L2847; `jar(eternalstarlight):data/eternal_starlight/enchantment/fertile.json` |
| `eternal_starlight:gathering` | 2 | 6 [V] | — | **6** | CFG L2939; `jar(eternalstarlight):data/eternal_starlight/enchantment/gathering.json` |
| `eternal_starlight:glacial_sowing` | 2 | 6 [V] | — | **6** | CFG L2962; `jar(eternalstarlight):data/eternal_starlight/enchantment/glacial_sowing.json` |
| `eternal_starlight:homing` | 3 | 7 [V] | — | **7** | CFG L3031; `jar(eternalstarlight):data/eternal_starlight/enchantment/homing.json` |
| `eternal_starlight:overheat` | 2 | 6 [V] | — | **6** | CFG L3192; `jar(eternalstarlight):data/eternal_starlight/enchantment/overheat.json` |
| `eternal_starlight:poisoning` | 4 | 8 [V] | — | **8** | CFG L1467; `jar(eternalstarlight):data/eternal_starlight/enchantment/poisoning.json` |
| `eternal_starlight:precision` | 5 | 9 [V] | — | **9** | CFG L3261; `jar(eternalstarlight):data/eternal_starlight/enchantment/precision.json` |
| `eternal_starlight:soul_snatcher` | 3 | 5 [V] | — | **5** | CFG L2088; `jar(eternalstarlight):data/eternal_starlight/enchantment/soul_snatcher.json` |
| `eternal_starlight:swift_lash` | 5 | 9 [V] | — | **9** | CFG L3537; `jar(eternalstarlight):data/eternal_starlight/enchantment/swift_lash.json` |
| `eternal_starlight:tearing` | 1 | 1 [V] | — | **1** | CFG L3560; `jar(eternalstarlight):data/eternal_starlight/enchantment/tearing.json` |
| `eternal_starlight:tracing` | 2 | 6 [V] | — | **6** | CFG L3606; `jar(eternalstarlight):data/eternal_starlight/enchantment/tracing.json` |
| `evilcraft:breaking` | 3 | 8 [V] | — | **8** | CFG L216; `jar(evilcraft):data/evilcraft/enchantment/breaking.json` |
| `evilcraft:life_stealing` | 3 | 6 [V] | — | **6** | CFG L1044; `jar(evilcraft):data/evilcraft/enchantment/life_stealing.json` |
| `evilcraft:poison_tip` | 3 | 7 [V] | — | **7** | CFG L1444; `jar(evilcraft):data/evilcraft/enchantment/poison_tip.json` |
| `evilcraft:unusing` | 1 | 1 [V] | — | **1** | CFG L2387; `jar(evilcraft):data/evilcraft/enchantment/unusing.json` |
| `evilcraft:vengeance` | 3 | 8 [V] | — | **8** | CFG L2433; `jar(evilcraft):data/evilcraft/enchantment/vengeance.json` |
| `farmersdelight:backstabbing` | 3 | 8 [V] | — | **8** | CFG L32; `jar(FarmersDelight):data/farmersdelight/enchantment/backstabbing.json` |
| `forbidden_arcanus:soul_looting` | 3 | 8 [V] | — | **8** | CFG L2042; `jar(forbidden_arcanus):data/forbidden_arcanus/enchantment/soul_looting.json` |
| `minecolonies:raider_damage_enchant` | 2 | 6 [V] | — | **6** | CFG L1651; `jar(minecolonies):data/minecolonies/enchantment/raider_damage_enchant.json` |
| `mysticalagriculture:mystical_enlightenment` | 5 | 9 [V] | — | **9** | CFG L1329; `jar(MysticalAgriculture):data/mysticalagriculture/enchantment/mystical_enlightenment.json` |
| `mysticalagriculture:soul_siphoner` | 5 | 9 [V] | — | **9** | CFG L2065; `jar(MysticalAgriculture):data/mysticalagriculture/enchantment/soul_siphoner.json` |
| `occultism:fracture_soul` | 5 | 20 [V] | — | **20** | CFG L2916; `jar(occultism):data/occultism/enchantment/fracture_soul.json` |
| `quarryplus:quarry_pickaxe` | 1 | 1 [V] | — | **1** | CFG L1605; `jar(AdditionalEnchantedMiner):data/quarryplus/enchantment/quarry_pickaxe.json` |
| `railcraft:destruction` | 3 | 7 [V] | — | **7** | CFG L515; `jar(railcraft):data/railcraft/enchantment/destruction.json` |
| `railcraft:implosion` | 5 | 10 [V] | — | **10** | CFG L883; `jar(railcraft):data/railcraft/enchantment/implosion.json` |
| `railcraft:smack` | 4 | 9 [V] | — | **9** | CFG L1996; `jar(railcraft):data/railcraft/enchantment/smack.json` |
| `railcraft:wrecking` | 5 | 9 [V] | — | **9** | CFG L2502; `jar(railcraft):data/railcraft/enchantment/wrecking.json` |
| `reliquary:severing` | 5 | 9 [V] | — | **9** | CFG L1904; `jar(reliquary):data/reliquary/enchantment/severing.json` |
| `supplementaries:stasis` | 1 | 1 [V] | — | **1** | CFG L2249; `jar(supplementaries):data/supplementaries/enchantment/stasis.json` |
| `the_bumblezone:comb_cutter` | 2 | 3 [V] | — | **3** | CFG L377; `jar(the_bumblezone):data/the_bumblezone/enchantment/comb_cutter.json` |
| `the_bumblezone:hive_lifeline` | 1 | 1 [V] | — | **1** | CFG L3008; `jar(the_bumblezone):data/the_bumblezone/enchantment/hive_lifeline.json` |
| `the_bumblezone:neurotoxins` | 2 | 13 [V] | — | **13** | CFG L1375; `jar(the_bumblezone):data/the_bumblezone/enchantment/neurotoxins.json` |
| `the_bumblezone:potent_poison` | 3 | 11 [V] | — | **11** | CFG L1490; `jar(the_bumblezone):data/the_bumblezone/enchantment/potent_poison.json` |
| `tombstone:blessing` | 10 | 13 [V] | — | **13** | CFG L147; `jar(tombstone):data/tombstone/enchantment/blessing.json` |
| `tombstone:curse_of_bones` | 10 | 13 [V] | — | **13** | CFG L423; `jar(tombstone):data/tombstone/enchantment/curse_of_bones.json` |
| `tombstone:decrepitude` | 10 | 13 [V] | — | **13** | CFG L446; `jar(tombstone):data/tombstone/enchantment/decrepitude.json` |
| `tombstone:frostbite` | 10 | 13 [V] | — | **13** | CFG L791; `jar(tombstone):data/tombstone/enchantment/frostbite.json` |
| `tombstone:incurable_wounds` | 10 | 13 [V] | — | **13** | CFG L906; `jar(tombstone):data/tombstone/enchantment/incurable_wounds.json` |
| `tombstone:magic_siphon` | 10 | 13 [V] | — | **13** | CFG L1182; `jar(tombstone):data/tombstone/enchantment/magic_siphon.json` |
| `tombstone:plague_bringer` | 10 | 13 [V] | — | **13** | CFG L1421; `jar(tombstone):data/tombstone/enchantment/plague_bringer.json` |
| `tombstone:ruthless_strike` | 10 | 13 [V] | — | **13** | CFG L2617; `jar(tombstone):data/tombstone/enchantment/ruthless_strike.json` |
| `tombstone:sanctified` | 10 | 13 [V] | — | **13** | CFG L2640; `jar(tombstone):data/tombstone/enchantment/sanctified.json` |
| `tombstone:soulbound` | 1 | 1 [V] | — | **1** | CFG L2157; `jar(tombstone):data/tombstone/enchantment/soulbound.json` |
| `tombstone:spectral_bite` | 10 | 13 [V] | — | **13** | CFG L2180; `jar(tombstone):data/tombstone/enchantment/spectral_bite.json` |
| `tombstone:spectral_conjurer` | 10 | 13 [V] | — | **13** | CFG L2203; `jar(tombstone):data/tombstone/enchantment/spectral_conjurer.json` |
| `twilightforest:chill_aura` | 3 | 8 [V] | — | **8** | CFG L331; `jar(twilightforest):data/twilightforest/enchantment/chill_aura.json` |
| `twilightforest:destruction` | 3 | 8 [V] | — | **8** | CFG L538; `jar(twilightforest):data/twilightforest/enchantment/destruction.json` |
| `twilightforest:fire_react` | 3 | 8 [V] | — | **8** | CFG L699; `jar(twilightforest):data/twilightforest/enchantment/fire_react.json` |
| `twilightforest:renewal` | 1 | 1 [V] | — | **1** | CFG L1743; `jar(twilightforest):data/twilightforest/enchantment/renewal.json` |
| `undergarden:longevity` | 3 | 7 [V] | — | **7** | CFG L1067; `jar(The_Undergarden):data/undergarden/enchantment/longevity.json` |
| `undergarden:ricochet` | 3 | 7 [V] | — | **7** | CFG L1789; `jar(The_Undergarden):data/undergarden/enchantment/ricochet.json` |
| `undergarden:self_sling` | 1 | 1 [V] | — | **1** | CFG L1881; `jar(The_Undergarden):data/undergarden/enchantment/self_sling.json` |

## 5. Conflitti, override e incertezze

**Override di incantesimi di un altro namespace** (scansione di tutti i 514 jar in `mods/`, incluso jar-in-jar, script `scratchpad/scripts/scan_ench.py`: 122 file `data/*/enchantment/*.json`) [V]:

| File sovrascritto | Jar | `max_level` nel jar | Vanilla | Effetto |
|---|---|---|---|---|
| `data/minecraft/enchantment/sharpness.json` | `jar(ApothicEnchanting)` | 5 | 5 | nessuna modifica del massimo (il cambio reale e' via CFG) |
| `data/minecraft/enchantment/protection.json` | `jar(ApothicEnchanting)` | 4 | 4 | idem |
| `data/minecraft/enchantment/channeling.json` | `jar(silent-gear)` | 1 | 1 | idem |

- Nessun percorso `data/<ns>/enchantment/<nome>.json` compare in due jar diversi: **nessun conflitto tra mod** da risolvere con l'ordine di caricamento [V].
- Datapack dell'istanza: `datapacks/sawmill.zip` contiene solo `pack.mcmeta`/`pack.png`; `moonlight-global-datapacks/` contiene solo config; nessun incantesimo [V]. `kubejs/data/` ha un solo file di incantesimo: `kubejs/data/apothic_enchanting/enchantment/miners_fervor.json`, con `"neoforge:conditions":[{"type":"neoforge:false"}]` e `max_level` 5 (come nel jar). Sovrascrive quello di Apothic con una condizione sempre falsa, quindi **`apothic_enchanting:miners_fervor` risulta disabilitato** [V il file, D che il pack KubeJS vinca sul jar]; i tag in `kubejs/server_scripts/Tweaks/tags.js` (righe ~225-231) lo toccano. Il CFG ha `Max Level` = 1 per lui: non rilevante.

**Incertezze**
1. [D] **Il CFG e' quello generato dall'istanza locale** (default Apothic 1.6.4). `defaultconfigs/` non contiene file apotheosis. Se il CFG viene rigenerato o differisce nell'ATM10 ufficiale, i valori cambiano. Incantesimi di mod aggiunte in futuro avranno il default calcolato a runtime, non presente nel file.
2. [D] **Raggiungibilita' pratica dei massimi Apothic al tavolo**: richiede potenza fino a ~200 con scaffali Apothic; via incudine/libri e' possibile combinando libri allo stesso livello.
3. [D] **Gear set `pinnacle` (livello 15) e mercante errante (10)**: contenuto verificato, ma non ho verificato come i boss "pinnacle" vengano generati in ATM10 ne' il drop rate. Per un tetto "pulito" ignora i `*` e usa il "max effettivo".
4. [V] **Spectrum usa il `max_level` del JSON, non quello Apothic**, per il range normale della ricetta di upgrade e per il cap dell'Incudine di Bedrock (`bedrock_anvil_can_exceed_max_vanilla_enchantment_level = false`). Con l'advancement Overenchanting sbloccato dall'addon, la ricetta di upgrade sale fino a `levels+1`; l'Incudine di Bedrock resta cappata al `max_level` del JSON (es. Sharpness 5) salvo cambio di `spectrum-startup.toml`. Possibile punto di design per l'addon.
5. [V] Quark (Ancient Tomes), Natamus e Goblin Traders sono nella lista del coremod ma **non sono installati** (nessun jar in `mods/`).
6. [D] Il "max originale" dei vanilla deriva dal commento "originally N" del CFG e dalla conoscenza di vanilla 1.21.1; per le mod e' il `max_level` del JSON [V].
7. Comandi (`/enchant`) esclusi come richiesto; `EnchantCommand` e' agganciato al massimo Apothic.

## 6. File letti (principali)

- `<istanza>/config/apotheosis/{enchantments,apotheosis,apothic_enchanting}.cfg`, `config/spectrum-startup.toml`
- `jar(ApothicEnchanting-1.21.1-1.6.4):coremods/ench/*.js`, `META-INF/coremods.json`, `apothic_enchanting.mixins.json`, classi (javap) `dev/shadowsoffire/apothic_enchanting/{asm/EnchHooks,EnchantmentInfo,ApothicEnchanting,ApothEnchEvents,table/ApothEnchantmentHelper,PowerFunction$DefaultMaxPowerFunction}`, `data/minecraft/enchantment/{sharpness,protection}.json`
- `jar(spectrum):data/spectrum/recipe/enchantment_upgrade/**`, `data/spectrum/recipe/fusion_shrine/**`; sorgente `src/main/java/de/dafuqs/spectrum/{recipe/enchanter/EnchantmentUpgradeRecipe,blocks/enchanter/EnchanterBlockEntity,inventories/BedrockAnvilScreenHandler,config/SpectrumConfig}.java`
- `jar(Apotheosis-1.21.1-8.9.0):data/apotheosis/{gear_sets,wanderer_trades}/**`
- `<istanza>/kubejs/data/apothic_enchanting/enchantment/miners_fervor.json`, `kubejs/server_scripts/**`
- Scansioni su tutti i jar di `mods/` (enchantment JSON, ricette, loot table, trade).
