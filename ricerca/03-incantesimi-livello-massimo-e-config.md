# 03 — Incantesimi oltre il livello massimo: Enchanter, Bedrock Anvil e config di Spectrum

> Ricerca per **Spectral Ascension** (addon NeoForge 1.21.1 di Spectrum 1.12.9, modpack ATM10).
> Obiettivo: far applicare livelli oltre il "massimo vanilla" a seconda del tier dell'oggetto
> (bedrock Spectrum = +1, T6 = +2, T7 = +3, T8 = +4, Alloy = senza limiti).
>
> Legenda delle fonti:
> - `src:` = sorgente Spectrum ≈1.12.9 in `scratchpad/src/spectrum-1.12.9/src/main/java/de/dafuqs/spectrum/`
> - `jar:` = jar estratto `scratchpad/jars/spectrum-1.12.9-1.21.1-neo/`
> - `cfg:` = la cartella `config/` della tua istanza CurseForge di ATM10
> - `wiki:` = `scratchpad/src/spectrum-wiki/`
> - `mc:` = bytecode vanilla/NeoForge 1.21.1 letto con `javap` da
>   `<cartella Install di CurseForge>/libraries/net/minecraft/client/1.21.1-20240808.144430/client-1.21.1-20240808.144430-srg.jar`
>   e `.../net/neoforged/neoforge/21.1.265/neoforge-21.1.265-{client,universal}.jar` (versione NeoForge dell'istanza: 21.1.265).
>
> **[V]** = verificato leggendo il file/bytecode. **[D]** = dedotto, da verificare in gioco o nel codice di un'altra mod.
>
> Il sorgente "≈1.12.9" corrisponde al jar: ho controllato con `javap` che `BedrockAnvilScreenHandler`,
> `EnchantmentUpgradeRecipe.isInNormalRange` ed `EnchanterBlockEntity.getRequiredExperienceForEnchantment`
> nel jar chiamano `getMaxLevel()` e leggono `BedrockAnvilCanExceedMaxVanillaEnchantmentLevel` esattamente come nel sorgente [V].

---

## 0. Concetti base (per chi arriva dal web)

- **Enchantment data-driven (1.21)**: dalla 1.21 gli incantesimi non sono più classi Java ma **file JSON**
  in `data/<namespace>/enchantment/<nome>.json` (es. `jar:data/spectrum/enchantment/clovers_favor.json:11` → `"max_level": 3`).
  Il gioco li carica in un **registry dinamico**: un "dizionario" `ResourceKey → Enchantment` costruito
  all'avvio del mondo/server dai datapack (non all'avvio del gioco). Per questo nel codice si usa
  `Holder<Enchantment>` (un riferimento a una voce del registry) invece dell'oggetto diretto.
  Un datapack può sovrascrivere il JSON e cambiare `max_level`.
- **`Enchantment.getMaxLevel()`**: in vanilla restituisce semplicemente `definition().maxLevel()`, cioè il
  `max_level` del JSON (`mc: Enchantment.getMaxLevel` = `getfield definition; invokevirtual EnchantmentDefinition.maxLevel(); ireturn`) [V].
  Le mod possono però alterarne il risultato con un mixin (vedi Apothic Enchanting, §5.6).
- **`ItemEnchantments` (data component)**: in 1.21 gli oggetti non hanno più NBT libero ma *data component*
  tipizzati. Gli incantesimi di un oggetto stanno nel component `minecraft:enchantments`, quelli di un libro
  incantato in `minecraft:stored_enchantments`. Entrambi sono un `ItemEnchantments` (mappa `Holder<Enchantment> → int livello`).
  Si modifica tramite `ItemEnchantments.Mutable` (un "builder").
- **Mixin**: libreria che permette di "iniettare" codice in classi di altre mod o di Minecraft al caricamento,
  senza modificarne il sorgente. Tipi che useremo:
  - `@Inject(at = @At("HEAD"), cancellable = true)` → esegue codice all'inizio di un metodo e può interromperlo/forzare il valore di ritorno.
  - `@ModifyReturnValue` (MixinExtras) → riceve il valore che il metodo sta per restituire e lo può cambiare.
  - `@ModifyExpressionValue` / `@WrapOperation` (MixinExtras) → intercetta il risultato di una singola chiamata *dentro* un metodo
    (es. la chiamata a `enchantment.getMaxLevel()` dentro `createResult`) e la sostituisce. Molto più chirurgico.
  - `@ModifyArg` → cambia un argomento passato a una chiamata.
  - `@ModifyConstant` → cambia una costante letterale (es. `255`) dentro un metodo.
  - `@Local` (MixinExtras) → permette di leggere una variabile locale del metodo target (es. lo stack in input).
- **Access Transformer (AT)**: file `META-INF/accesstransformer.cfg` che rende `public` un campo/metodo `private` di
  Minecraft. Serve solo per classi vanilla/NeoForge; per accedere a membri privati di Spectrum si usa un mixin `@Accessor`/`@Invoker`.
- **Config NeoForge (`ModConfigSpec`)**: si definiscono chiavi con default e il framework genera un file TOML.
  Tipi: `CLIENT`, `COMMON`, `SERVER` (per-mondo, sincronizzato ai client) e `STARTUP`. Vedi §4.0.
- **Eventi NeoForge**: hook ufficiali (`@SubscribeEvent`) che il gioco "lancia" in punti precisi, es. `AnvilUpdateEvent`
  quando cambia il risultato dell'incudine vanilla. Più stabili dei mixin ma esistono solo dove NeoForge li ha previsti.

---

## 1. Enchanter

### 1.1 Struttura e classi [V]

| Ruolo | File:riga |
|---|---|
| Blocco | `src:blocks/enchanter/EnchanterBlock.java` (verifica struttura, `verifyStructure`, `scatterContents`) |
| Block entity (logica) | `src:blocks/enchanter/EnchanterBlockEntity.java:43` |
| Inventario reale: slot 0 = oggetto centrale, slot 1 = Knowledge Gem | `EnchanterBlockEntity.java:62` |
| 8 Item Bowl a offset fissi (±5/±3) | `EnchanterBlockEntity.java:50-59`, letti in `inventoryChanged()` `:770-790` |
| Inventario "virtuale" (centro + gemma + 8 bowl) usato per il matching ricette | `:71`, `:772-783` |
| Tick server | `serverTick` `:126-232` |
| Ricetta `spectrum:enchanter` | `src:recipe/enchanter/EnchanterRecipe.java` (codec `:142-151`) |
| Ricetta `spectrum:enchantment_upgrade` | `src:recipe/enchanter/EnchantmentUpgradeRecipe.java` |
| Helper | `src:helpers/SpectrumEnchantmentHelper.java` |

L'Enchanter ha **tre modalità**, scelte in `calculateCurrentRecipe` (`:600-653`) e `serverTick` (`:131-150`):

1. **`spectrum:enchantment_upgrade`** — cercata per prima (`:610-631`). Alza di +1 il livello di un **libro incantato** al centro.
2. **`spectrum:enchanter`** — ricette "di crafting" (es. mela d'oro incantata): 1 ingrediente centrale + 8 nelle bowl,
   provate in 8 rotazioni × 2 specchiature (`:633-652`). Campi JSON: `ingredients`, `result`, `time` (default 200),
   `required_experience` (default 0), `disable_yield_and_efficiency_upgrades`, `copy_components`, `required_advancement`
   (`EnchanterRecipe.java:142-151`; esempio `jar:data/spectrum/recipe/enchanter/enchanted_golden_apple.json`).
   Non toccano i livelli degli incantesimi (gli `enchanter/vanilla_books` e `spectrum_books` creano libri di livello I).
3. **Incantamento "in codice" dell'oggetto centrale** (nessuna ricetta JSON): oggetto incantabile (o libro) al centro,
   **libri incantati** nelle bowl, Knowledge Gem nello slot 1 → copia sull'oggetto i livelli più alti trovati nei libri
   (`isValidCenterEnchantingSetup` `:241-284`, `enchantCenterItem` `:338-364`).

### 1.2 Costo in esperienza (Knowledge Gem) [V]

- L'XP non viene dal giocatore ma da un `ExperienceStorageItem` (Knowledge Gem) nello slot 1 (`:246`, `drainExperience` `:744-767`).
  Il prelievo è ridotto dall'upgrade "Experience" (`getExperienceWithMod` `:456-459`).
- **Upgrade recipe**: costo fisso per gradino = campo `experience` della voce `levels[livello_attuale-1]`
  (`EnchantmentUpgradeRecipe.getForSourceLevel` `:132-138`, consumato in `craftEnchantmentUpgradeRecipe` `:550-551`).
  Gli oggetti nelle bowl: `ingredient_count_per_bowl × 8` (`:140-142`, controllato in `matches` `:109-123`).
- **Incantamento in codice**: `getRequiredExperienceForEnchantment` (`:438-454`):
  `rarityCost(anvil_cost) × levelCost × treasure/curse × disponibilità × 16/(2+enchantability)`, con
  `levelCost = level × min(1, level / enchantment.getMaxLevel())` (**`:447`**). Quindi oltre il max il costo cresce
  linearmente col livello (il fattore si satura a 1). Se l'incantesimo è in conflitto ×4 (`:424-426`).

### 1.3 Dove legge il livello massimo e se lo può superare

**a) Upgrade recipe (`spectrum:enchantment_upgrade`)** [V]
- **Non c'è un campo `level_cap`/`max_level` nel JSON**: il tetto è implicito, `getLevelCap() = levels.size() + 1`
  (`EnchantmentUpgradeRecipe.java:144-146`). `matches` rifiuta se `bookLevel >= getLevelCap()` (`:96`).
  Il risultato è clampato a `min(cur+1, getLevelCap())` (`EnchanterBlockEntity.java:549`).
- Il "livello vanilla" entra solo in `isInNormalRange(level) = level < enchantment.getMaxLevel()` (**`EnchantmentUpgradeRecipe.java:200-205`**).
  Se il livello di partenza **non** è nel range normale serve che il "proprietario" (ultimo giocatore che ha interagito)
  abbia l'advancement di overenchanting: `canOwnerOverenchant` (`EnchanterBlockEntity.java:65`, impostato in `setOwner` `:818`,
  controllato in `checkRecipeRequirements` `:315-319` e `calculateCurrentRecipe` `:615`).
  L'advancement è `spectrum:milestones/unlock_overenchanting_with_enchanter` (`src:registries/SpectrumAdvancements.java:39`).
- **Le ricette di Spectrum superano già il max vanilla.** Tetto di ogni ricetta (= `levels.size()+1`, letto dai JSON in `jar:data/spectrum/recipe/enchantment_upgrade/`):

  | Incantesimo | max vanilla | tetto ricetta Spectrum | XP ultimo gradino |
  |---|---|---|---|
  | sharpness, protection, efficiency, power | 5/4/5/5 | **VIII** | 12800 |
  | smite, impaling, piercing, fire/blast/proj. protection | 5/5/4/4 | **VIII** | 6400 |
  | sweeping_edge | 3 | VII | 6400 |
  | unbreaking, looting, feather_falling, thorns, respiration, swift_sneak | 3/3/4/3/3/3 | VI | 3200–6400 |
  | fortune, knockback, punch, lure, luck_of_the_sea, quick_charge, breach, density, wind_burst | 3/2/2/3/3/3/4/5/3 | V | 1600–6400 |
  | bane_of_arthropods | 5 | **IV** (sotto il vanilla!) | 400 |
  | spectrum:exuberance | 5 | VIII | 12800 |
  | spectrum:clovers_favor / treasure_hunter / first_strike / sniping / inertia / razing | 3/3/2/2/3/3 | VI/V/V/V/V/V | — |

  (es. `jar:data/spectrum/recipe/enchantment_upgrade/minecraft/sharpness.json:1-40`: 7 voci, pigmento nero 1→64 per bowl, XP 200→12800.)
- **Scoperta importante [V]**: l'advancement `unlock_overenchanting_with_enchanter` ha come unico criterio
  `revelationary:advancement_gotten` con `advancement_identifier: "spectrum:__wip"`
  (`jar:data/spectrum/advancement/milestones/unlock_overenchanting_with_enchanter.json`), ma **nel jar non esiste alcun
  advancement `spectrum:__wip`** (`find .../advancement -name "__wip*"` → nessun risultato). Quindi in Spectrum 1.12.9
  l'overenchanting all'Enchanter è **di fatto bloccato** (stesso trucco per `unlock_conflicted_enchanting_with_enchanter`).
  La wiki dice esplicitamente che il modpack può modificare questo advancement con un datapack
  (`wiki:Custom-Enchantment-Upgrade-Recipes.md:10-12`, `wiki:Adjusting-Progression.md:142-144`).
- **[D] Effetto di Apothic Enchanting in ATM10**: `isInNormalRange` usa `getMaxLevel()`. Se Apothic alza
  `getMaxLevel()` (es. sharpness → 9, vedi §5.6), allora tutti i gradini fino a VIII risultano "nel range normale"
  e **non serve l'advancement**: nell'istanza l'Enchanter probabilmente porta già Sharpness a VIII. Da verificare in gioco.

**a-bis) Dove il codice controlla l'advancement di overenchanting e come sbloccarlo** [V]

Catena completa nel codice:
1. Costante: `SpectrumAdvancements.OVERENCHANTING = locate("milestones/unlock_overenchanting_with_enchanter")` (`src:registries/SpectrumAdvancements.java:39`); gemello `APPLY_CONFLICTING_ENCHANTMENTS` a `:35`.
2. Lettura: `EnchanterBlockEntity.setOwner(Player)` (`src:blocks/enchanter/EnchanterBlockEntity.java:815-820`) fa
   `canOwnerOverenchant = AdvancementHelper.hasAdvancement(player, OVERENCHANTING)` (`:818`). Il valore è **memorizzato nel block entity**
   (salvato in NBT come `owner_can_overenchant`, `:673` / `:711`) e si aggiorna solo quando un giocatore interagisce di nuovo con l'Enchanter.
3. Uso: `calculateCurrentRecipe` (`:615`) accetta la ricetta di upgrade solo se `canOwnerOverenchant || upgrade.isInNormalRange(livello)`;
   `checkRecipeRequirements` (`:318-319`) ricontrolla la stessa condizione ogni 60 tick durante il crafting.
4. UI: toast di sblocco in `src:progression/UnlockToastManager.java:28`; tooltip "overchanting" in EMI/REI (`EnchantmentUpgradeEmiRecipeGated.java:75`, `EnchantmentUpgradeCategory.java:52-57`).

Perché è irraggiungibile in 1.12.9: l'unico criterio è `revelationary:advancement_gotten` con `advancement_identifier: "spectrum:__wip"`
(`jar:data/spectrum/advancement/milestones/unlock_overenchanting_with_enchanter.json`) e **nel jar non esiste nessun advancement `spectrum:__wip`**
(stesso schema per `unlock_conflicted_enchanting_with_enchanter.json`, e anche altri file usano `__wip`: `unlocks/resources/spectral_shard.json`,
`unlocks/trinkets/artists_palette.json`, `unlocks/items/bottle_of_forfeiture.json`, `milestones/unlock_hostile_memorizing.json`,
`milestones/unlock_remembering_boss_memories.json`). La wiki invece lo presenta come normale lock di progressione
(`wiki:Adjusting-Progression.md:142-144`, `wiki:Custom-Enchantment-Upgrade-Recipes.md:12`) → wiki non allineata al 1.12.9.
Eccezioni: `/advancement grant <player> only spectrum:milestones/unlock_overenchanting_with_enchanter` lo concede comunque (gli advancement con criterio impossibile si possono dare da comando) [D];
e in ATM10, se Apothic alza `getMaxLevel()`, il gate di fatto non scatta fino a VIII (§1.3a) [D].

Cosa servirebbe per sbloccarlo — opzioni per il nostro addon:

| Opzione | Come | Pro | Contro |
|---|---|---|---|
| **A. Override dell'advancement** (consigliata) | nel jar dell'addon: `data/spectrum/advancement/milestones/unlock_overenchanting_with_enchanter.json` con un criterio reale, es. `revelationary:advancement_gotten` su un nostro advancement `spectral_ascension:...t6_tool`, oppure `minecraft:inventory_changed` con un nostro item. Mantenere `parent`/display coerenti con l'originale | zero codice; usa il meccanismo previsto da DaFuqs; il toast/tooltip di Spectrum funzionano; il modpack può risovrascriverlo | è un override di un file di Spectrum: conflitto se un'altra mod/datapack lo sovrascrive (vince l'ordine dei datapack); se Spectrum in futuro sostituisce `__wip` col proprio criterio, il nostro override lo maschera |
| B. Fornire noi `spectrum:__wip` | creare un advancement con quell'id | sblocca tutto ciò che dipende da `__wip` | **sconsigliato**: sblocca anche contenuti WIP non finiti (spectral shard, artist's palette, ecc.) |
| C. Mixin su `setOwner` / `isInNormalRange` | `@ModifyExpressionValue` su `AdvancementHelper.hasAdvancement(...)` dentro `setOwner`, o `@ModifyReturnValue` su `isInNormalRange` | logica arbitraria (per tier, per giocatore) | codice fragile per un problema che i dati risolvono; bypassa toast/UI di Spectrum |
| D. Non fare nulla | — | nessun rischio | i libri oltre `getMaxLevel()` restano non producibili (in ATM10 forse irrilevante per via di Apothic) |

Attenzione comune a tutte: `canOwnerOverenchant` è cache del block entity, quindi dopo lo sblocco il giocatore deve ri-interagire con l'Enchanter (rimettere un oggetto) perché `setOwner` lo ricalcoli (chiamato solo all'interazione col blocco: `src:blocks/enchanter/EnchanterBlock.java:106` e `:117`).

**b) Incantamento in codice dell'oggetto centrale** [V]
- **Non c'è alcun clamp al livello massimo.** `enchantCenterItem` (`:338-345`) chiama
  `SpectrumEnchantmentHelper.addOrUpgradeEnchantment(stack, ench, level, false, canOwnerApplyConflicting)` per ogni
  incantesimo trovato nei libri; l'helper (`SpectrumEnchantmentHelper.java:45-77`) controlla solo: supporto dell'oggetto,
  conflitti, e che il livello nuovo sia > dell'esistente (`:69`), poi `builder.set(enchantment, level)` (`:74`).
- Conseguenza: **un libro Sharpness VIII prodotto dall'upgrade si applica tale e quale a qualunque spada**. Il "limite"
  sta solo in quanto in alto riesci a portare il libro.

**c) Config che influenzano l'Enchanter**: nessuna in `SpectrumConfig` riguarda livelli/XP dell'Enchanter [V]
(l'unica sui livelli è quella del Bedrock Anvil). Influiscono invece: advancement (datapack), JSON delle ricette (datapack),
upgrade di velocità/efficienza/esperienza (blocchi upgrade intorno, `calculateUpgrades` `:823-826`), e `getMaxLevel()` di altre mod.

---

## 2. Bedrock Anvil

### 2.1 Classi [V]

| Ruolo | File:riga |
|---|---|
| Blocco (estende `AnvilBlock`) | `src:blocks/BedrockAnvilBlock.java:20`, apre il menu a `:63` |
| Menu (server+client) | `src:inventories/BedrockAnvilScreenHandler.java:23` — **estende `ItemCombinerMenu`, non `AnvilMenu`** |
| Tipo di menu | `src:inventories/SpectrumMenuTypes.java:24` |
| Schermata client | `src:inventories/BedrockAnvilScreen.java:22` |
| Payload rename/lore | `src:networking/c2s_payloads/RenameItemInBedrockAnvilPayload.java`, `AddLoreBedrockAnvilPayload.java` |
| Mixin: incudine indistruttibile | `src:mixin/AnvilBlockMixin.java:83-88` (`@Inject HEAD` su `AnvilBlock.damage`) |

### 2.2 Logica di combinazione (`createResult`, `BedrockAnvilScreenHandler.java:106-294`) [V]

È una copia modificata di `AnvilMenu.createResult` vanilla con differenze commentate "We added/changed":
- rinomina gratuita e colorabile con pigmenti (`:223-244`), lore (`:246-254`);
- **nessun limite "Too Expensive" a 40 livelli** (righe vanilla commentate `:262-270`);
- per ogni incantesimo del secondo slot: `newLevel = (t == new) ? new+1 : max(new, t)` (`:179`) — la classica regola "due IV → V";
- **clamp** (`:197-200`):
  ```java
  boolean capToMaxLevel = (inputStack.is(Items.ENCHANTED_BOOK) && !inputStack.has(DataComponents.STORED_ENCHANTMENTS))
                          || !SpectrumConfig.CONFIG.BedrockAnvilCanExceedMaxVanillaEnchantmentLevel.get();
  if (capToMaxLevel && newEnchantmentLevel > enchantment.getMaxLevel()) {
      newEnchantmentLevel = enchantment.getMaxLevel();
  }
  ```
- costo: `anvilCost × newLevel` (dimezzato se dal libro) + repair cost cumulativo (`:203-211`, `:256-257`), con repair cost che raddoppia+1 (`getNextCost` `:73-75`).
- **Non lancia `AnvilUpdateEvent`/`AnvilRepairEvent`** di NeoForge: nel sorgente non c'è nessuna chiamata a `CommonHooks.onAnvilChange` (grep vuoto) [V],
  mentre l'`AnvilMenu` vanilla patchato da NeoForge la chiama (`mc: AnvilMenu`, invoke `CommonHooks.onAnvilChange`) [V].
  Quindi **un listener di `AnvilUpdateEvent` non vede il Bedrock Anvil**.

### 2.3 Config `bedrock_anvil_can_exceed_max_vanilla_enchantment_level` [V]

- Definita in `src:config/SpectrumConfig.java:35` e `:239-245`, default `false`. Nell'istanza: `false` (`cfg:spectrum-startup.toml:33`).
- **`false`**: il risultato è clampato a `enchantment.getMaxLevel()` (come la vanilla).
- **`true`**: il clamp **sparisce del tutto** (tranne il caso limite "libro incantato vuoto" come primo input).
  Non esiste un "quanto oltre": combinando due oggetti/libri dello stesso livello si sale di +1 ogni volta,
  **senza tetto** se non quello tecnico di 255 (§5.7) e il costo in livelli (che cresce senza limite di 40).
  Vale per **tutti** gli oggetti, non solo per i nostri, ed è globale.
- Nota: `createResult` gira **anche sul client** (lo slot di output viene ricalcolato localmente), e la config è STARTUP
  non sincronizzata (§4.0): se client e server hanno valori diversi l'anteprima può differire dal risultato reale [D].

---

## 3. Tutti i punti di Spectrum che leggono il livello massimo [V]

Ricerca su sorgente (`getMaxLevel|maxLevel|definition()`) e conferma sul jar (`grep -a getMaxLevel` sulle `.class`): **6 classi**, nessuna usa `definition().maxLevel()` direttamente, nessuna costante tipo `MAX_LEVEL` propria.

| # | Punto | Uso | Effetto gameplay |
|---|---|---|---|
| 1 | `src:inventories/BedrockAnvilScreenHandler.java:198-199` | clamp risultato incudine | **sì, è il cap del Bedrock Anvil** |
| 2 | `src:recipe/enchanter/EnchantmentUpgradeRecipe.java:204` (`isInNormalRange`) | serve l'advancement overenchant oltre questo livello | **sì, gate dell'upgrade** |
| 3 | `src:blocks/enchanter/EnchanterBlockEntity.java:447` | formula costo XP | solo costo |
| 4 | `src:compat/emi/recipes/EnchantmentUpgradeEmiRecipeGated.java:46` | colore/tooltip "overchanting" in EMI | solo UI |
| 5 | `src:compat/REI/plugins/EnchantmentUpgradeDisplay.java:36-37` + `EnchantmentUpgradeCategory.java:52-57,93` | idem in REI | solo UI |
| 6 | `src:commands/SanityCommand.java:466` | warning se manca la ricetta upgrade | solo debug |

Altri tetti non legati a `getMaxLevel`: `EnchantmentUpgradeRecipe.getLevelCap()` `:144-146` (lunghezza lista `levels`).

**Mixin di Spectrum su questi punti** (`jar:spectrum.mixins.json`, voci `AnvilBlockMixin` r.11, `EnchantmentHelperMixin` r.21,
`RandomChanceWithEnchantedBonusLootConditionMixin` r.52, `client.EnchantmentMixin` r.94) [V]:
- `src:mixin/EnchantmentHelperMixin.java:20-34` — filtra gli incantesimi offerti dal tavolo per oggetti `Preenchanted`. Non tocca livelli massimi.
- `src:mixin/client/EnchantmentMixin.java:54-68` — `@ModifyReturnValue` su `Enchantment.getFullname` per offuscare nomi non sbloccati. Non tocca livelli.
- `src:mixin/AnvilBlockMixin.java:83-88` — rende indistruttibile il Bedrock Anvil.
- **Spectrum NON ha mixin su `Enchantment.getMaxLevel`, `AnvilMenu`, né su `ItemEnchantments`** → nessun conflitto diretto con un nostro mixin su quei target.

---

## 4. `spectrum-startup.toml` dell'istanza

### 4.0 Che cos'è una config "startup" [V/D]
- Spectrum registra **una sola** config di tipo `ModConfig.Type.STARTUP` (`src:SpectrumCommon.java:89`); il file è
  `cfg:spectrum-startup.toml`. Nel sorgente c'è un `// TODO: split into server/client config` (`SpectrumConfig.java:9`).
- Semantica di `STARTUP` in NeoForge 21.1 [D, da documentazione NeoForge]: viene caricata **subito, durante la costruzione della mod**
  (prima della registrazione di blocchi/oggetti), dalla cartella `config/` globale (non per-mondo), **non è sincronizzata**
  dal server ai client e richiede il **riavvio** del gioco per applicare modifiche. Spectrum ne ha bisogno perché
  alcuni valori servono alla registrazione degli oggetti (es. statistiche dei tool, pendenti in `src:registries/SpectrumItems.java:495-496`).
- Conseguenza per il multiplayer: server e client devono avere lo stesso file a mano (il modpack lo distribuisce).
- Il nostro addon può **leggere** `SpectrumConfig.CONFIG.X.get()` in qualsiasi momento (è già caricata), ma non dovrebbe scriverla.

### 4.1 Confronto con i default [V]
Ho confrontato con uno script tutte le 117 chiavi del file con i `.define(...)` di `src:config/SpectrumConfig.java`:
**tutti i valori dell'istanza coincidono con i default**; nessuna chiave mancante o in più. ATM10 non ha toccato la config di Spectrum
(e nemmeno ha override Spectrum in `kubejs/data/`: non c'è una cartella `kubejs/data/spectrum` [V]).

### 4.2 Opzioni raggruppate per area (riga nel TOML → riga di definizione in `SpectrumConfig.java`)

**Incantesimi / incudine — rilevanti per noi**
| Chiave | TOML | Def. | Valore | Note |
|---|---|---|---|---|
| `bedrock_anvil_can_exceed_max_vanilla_enchantment_level` | 33 | 245 | false | §2.3 — l'unica leva config sui livelli |
| `name_for_unrevealed_enchantments` | 92 | 386 | "" | nome degli incantesimi Spectrum non sbloccati (offuscato) |
| `improved_critical_extra_damage_multiplier_per_level` | 94 | 391 | 0.5 | scala per livello: con livelli alti cresce linearmente |
| `first_strike_damage_per_level` | 96 | 396 | 2.0 | idem |
| `disarming_chance_per_level_mobs` / `_players` | 98/100 | 401/406 | 0.01 / 0.0025 | idem — a livelli molto alti può arrivare al 100% |
| `pest_control_experience_multiplier` | 102 | 411 | 2.0 | |

**Tool e armature (statistiche dei materiali)** — rilevanti per bilanciare i nostri tier
| Chiave | TOML | Valore |
|---|---|---|
| `bedrock_mining_speed` / `_attack_damage` / `_enchantability` | 134-136 | 15.0 / 5.0 / **3** |
| `bedrock_armor_{helmet,leggings,chestplate,boots}_protection`, `_toughness`, `_knockback_resistance` | 119-124 | 5/9/12/5, 3.0, 0.3 |
| `gemstone_*` (tool 126-129, armor 107-118) | | dur 16, ench 10; armor 3/7/5/4 + effetti |
| `oblivion_*` 130-133, `draconic_*` 137-140, `malachite_*` 141-144, `glass_crest_*` 145-148, `knotted_sword_*` 149-152, `nectar_lance_*` 153-156, `dreamflayer_*` 157-160, `nightfalls_blade_*` 161-164 | | default |

Nota: `bedrock_enchantability = 3` entra nel costo XP dell'Enchanter (`16/(2+3)` ≈ ×3.2, `EnchanterBlockEntity.java:450`) — i tool bedrock sono già cari da incantare all'Enchanter.

**Progressione / mondo**: `shooting_star_dimensions` 41, `shooting_star_spawn_chance` 45, `storm_stone_dimensions` 47, `storm_stone_spawn_chance` 49,
`enderman_holding_treasure_chance(_in_end)` 38-39, `dimension_portals` 7, `allow_dynamic_end_portal_shape` 86, `charged_creepers_drop_spectrum_mob_heads` 57,
`dragonrot/liquid_crystal/midnight_solution_dripstone_drip_chance` 178-182.

**Decay**: `*_spread_chance_on_random_tick` 69-72, `can_bottle_up_*` 74-77, `*_can_destroy_block_entities` 79-82, `log_placing_of_decay` 84, `decay_is_stopped_by_supported_claim_mods` 172.

**Ink / pastel / crafting**: `constructors_staff_ink_cost_per_block` 2, `exchanging_staff_ink_cost_per_block` 4, `vanilla_recipe_crafting_time_ticks` 53,
`repair_anything_recipe_repair_percentage` 55, `pastel_network_particles` 23.

**Trinket / pozioni**: `max_level_for_effects_in_lesser_potion_pendant` 165 (=3), `..._greater_potion_pendant` 166 (=1), `glow_vision_goggles_duration` 104, `omni_accelerator_pvp` 106.

**Client / estetica / compat**: `toast_milliseconds` 9, `dimension_reverb_*` 11-13, `wind_sim*` 14-17, `post_process_shaders` 19, `always_spawn_light_block_particles` 21,
`dimension_brightness_mod` 25, `add_item_tooltips` 29, `block_sound_volume` 88, `ore_aura_sound_volume` 90, `rei_lists_unrevealed_recipes_as_not_unlocked` 176,
`integration_packs_to_skip_loading` 27.

---

## 5. Strategie per Spectral Ascension

### 5.0 Requisito riformulato
Serve una funzione unica, ad es. `SAEnchantCaps.capFor(ItemStack target, Holder<Enchantment> ench) → int`, che restituisce:
`base(ench) + bonus(target)`, con `bonus` = 1/2/3/4 per bedrock/T6/T7/T8 e `Integer.MAX_VALUE` (cioè 255) per Alloy,
e `base(ench)` per tutto il resto. Poi la si applica nei **due** punti di applicazione su oggetto: Bedrock Anvil (§2.2) e
incantamento in codice dell'Enchanter (§1.3b). L'upgrade dei **libri** (§1.3a) non ha un oggetto-tier, quindi va gestito a parte.

**Decisione aperta 1 — cosa è "vanilla"?** In ATM10 Apothic Enchanting alza i massimi (sharpness 9, protection 8, ...; §5.6).
Se `base = getMaxLevel()` (valore effettivo con Apothic) → bedrock = Sharpness X; se `base = definition().maxLevel()` (JSON) → bedrock = Sharpness VI,
ma allora **un oggetto qualsiasi in ATM10 arriva già a IX** tramite Apothic e il nostro "bonus" sarebbe un malus. Consiglio: `base = enchantment.getMaxLevel()`
(si adatta a qualunque pack), con opzione config per usare `definition().maxLevel()`.

**Decisione aperta 2 — solo oggetti dell'addon o tutti?** Con i mixin sotto, gli oggetti non-addon restano a `base` (comportamento attuale di Spectrum).
Estendere a "tutti" significa solo cambiare `bonus()` (es. via tag), quindi la scelta si può rimandare.

### 5.1 (a) Solo config
- `bedrock_anvil_can_exceed_max_vanilla_enchantment_level = true` → nessun limite per nessuno (§2.3). **Non selettivo**, globale, e non tocca l'Enchanter.
- Pro: zero codice. Contro: non soddisfa il requisito per tier; rompe il bilanciamento di tutto il pack; è una config del modpack, non dell'addon.
- **Verdetto: no**, salvo come "modalità caos" documentata.

### 5.2 (b) Mixin sui punti trovati — target precisi

| Target | Metodo | Iniezione proposta | Scopo |
|---|---|---|---|
| `de.dafuqs.spectrum.inventories.BedrockAnvilScreenHandler` | `createResult()V` | `@WrapOperation(at=@At(value="INVOKE", target="Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))` + `@Local(ordinal=0) ItemStack inputStack` (lo stack nello slot 0, var. locale `:109`) | sostituisce il cap con `capFor(inputStack, ench)`. Colpisce entrambe le chiamate `:198` e `:199` (stessa invocazione) |
| idem | `createResult()V` | `@ModifyExpressionValue` su `ModConfigSpec$ConfigValue.get()` (oppure sulla variabile `capToMaxLevel` con `@ModifyVariable`) | forzare `capToMaxLevel = true` per gli oggetti del nostro mod anche se l'utente ha messo la config a `true` — così il nostro tetto vale comunque. Opzionale |
| `de.dafuqs.spectrum.blocks.enchanter.EnchanterBlockEntity` | `enchantCenterItem(EnchanterBlockEntity)V` (static) e `getRequiredExperienceToEnchantCenterItem(EnchanterBlockEntity)I` (static) | `@ModifyArg(at=@At(value="INVOKE", target="Lde/dafuqs/spectrum/helpers/SpectrumEnchantmentHelper;addOrUpgradeEnchantment(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;IZZ)Lnet/minecraft/util/Tuple;"), index=2)` → `min(level, capFor(stack, ench))` | clamp dei livelli applicati dall'Enchanter all'oggetto centrale |
| idem | `getRequiredExperienceToEnchantWithEnchantment(ItemStack,Holder,int,boolean)I` (static, `:408`) | `@ModifyVariable(at=@At("HEAD"), argsOnly=true, ordinal=?)` sul parametro `level` → clamp; oppure `@Inject HEAD cancellable` che restituisce `-1` se `existingLevel >= cap` | coerenza: senza questo, il calcolo XP e il check "redundant" usano il livello non clampato e l'Enchanter può ripartire in loop su un oggetto già al cap |
| idem | `isValidCenterEnchantingSetup(EnchanterBlockEntity)Z` (`:241`, check `isRedundant` `:263`) | `@WrapOperation`/`@ModifyExpressionValue` sull'`entry.getIntValue()` usato nel confronto, oppure `@ModifyReturnValue` che ricontrolla con il cap | evita che il setup risulti "valido" quando l'oggetto è già al massimo del tier |
| `de.dafuqs.spectrum.recipe.enchanter.EnchantmentUpgradeRecipe` | `isInNormalRange(I)Z` | `@ModifyReturnValue` | **solo se** vogliamo legare l'overenchanting dei libri a qualcosa di nostro; in alternativa basta il datapack sull'advancement (§5.5) |
| `de.dafuqs.spectrum.blocks.enchanter.EnchanterBlockEntity` | `getRequiredExperienceForEnchantment(int,Holder,int)I` (`:447`) | `@WrapOperation` su `getMaxLevel()` | opzionale: far crescere il costo XP anche oltre il max (oggi saturato a 1) |

Note pratiche:
- Sono tutti metodi **pubblici/statici** di Spectrum, nessun AT necessario. I metodi `private static` (`checkRecipeRequirements`, `calculateCurrentRecipe`, `getLevel`) si possono comunque targettare con mixin; i campi `protected` (`canOwnerOverenchant`, `virtualInventory`) con `@Accessor`.
- Il mixin va dichiarato in un `*.mixins.json` dell'addon con `"required": true` e il nome di classe **Mojang** (NeoForge 1.21 gira in mappature Mojang: niente remap).
- **Rischio**: mixin su codice di un'altra mod = fragile agli aggiornamenti di Spectrum (righe/firma). Mitigazione: `@WrapOperation` su INVOKE di metodi vanilla stabili (`Enchantment.getMaxLevel`) invece di ordinali/costanti; `require = 1` per fallire in modo visibile al load; test a ogni update di Spectrum.
- Pro: selettivo per oggetto, preciso, nessun effetto su altre mod. Contro: va mantenuto; l'anteprima client usa lo stesso codice → coerente se l'addon è installato su entrambi i lati (lo è, è una mod obbligatoria).

### 5.3 (c) Eventi NeoForge
- **`AnvilUpdateEvent`** (`mc: net.neoforged.neoforge.event.AnvilUpdateEvent`, campi left/right/name/output/cost/materialCost/player) [V]:
  è lanciato **solo** dall'`AnvilMenu` vanilla. Il Bedrock Anvil non lo chiama (§2.2) → **inutile per il Bedrock Anvil**.
  Utile però se vogliamo che anche l'**incudine normale** rispetti i tier (es. impedire che un'incudine normale applichi un libro oltre `base`... cosa che il vanilla già fa).
- **`GetEnchantmentLevelEvent`** (`mc: net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent`, espone `ItemEnchantments.Mutable` modificabile) [V]:
  è lanciato da `IItemExtension.getAllEnchantments` / `getEnchantmentLevel` quando il gioco **legge il livello effettivo** di un oggetto.
  Permette livelli "virtuali": l'oggetto salvato ha Sharpness V, ma l'evento aggiunge +2 se è T6. Pro: niente mixin, compatibile, reversibile.
  Contro: **non** è "applicare incantesimi oltre il limite" all'incudine/Enchanter, è un bonus passivo; il tooltip mostra il livello salvato (bisogna aggiungere un tooltip); doppio conteggio se combinato con livelli reali alti. Utile come meccanica *complementare* (es. per l'Alloy).
- In alternativa, l'oggetto stesso può fare override di `IItemExtension.getEnchantmentLevel(ItemStack, Holder)` / `getAllEnchantments(...)` (metodi presenti in `mc: IItemExtension`) [V] — stesso effetto dell'evento ma solo per le nostre classi item.
- `EnchantmentLevelSetEvent` riguarda il tavolo vanilla, non ci serve.

### 5.4 (d) Tag / data component "max level bonus"
- Invece di hardcodare i tier nella classe, definire:
  - un **data component** custom `spectral_ascension:enchant_level_bonus` (int, o `-1` = illimitato) messo sul default dell'item; oppure
  - **item tag** `spectral_ascension:enchant_bonus/plus_1`, `plus_2`, `plus_3`, `plus_4`, `unlimited`.
- `capFor()` (§5.0) legge il component/tag; i mixin di §5.2 chiamano `capFor()`.
- Pro: il modpack (o un datapack) può dare il bonus anche a oggetti di altre mod (risolve "solo addon o tutti" senza ricompilare);
  il component permette upgrade dinamici (es. un rituale che porta un T6 a T7). I tag sono più semplici e dichiarativi.
- Consiglio: **tag** per i bonus statici di tier + eventuale component per override per-stack.

### 5.5 (e) API / dati di Spectrum
- **Nessuna API Java** di Spectrum per il max level (nessun hook/evento in `src:api/`) [V, grep].
- Leve **data-driven** già esistenti, usabili dal nostro datapack interno all'addon:
  1. **Allungare le ricette `enchantment_upgrade`** (`levels`) per produrre libri fino al tetto Alloy desiderato (es. sharpness fino a XII). Un override di
     `data/spectrum/recipe/enchantment_upgrade/minecraft/sharpness.json` nel nostro jar (o ricette nuove nel nostro namespace — attenzione: due ricette per lo stesso incantesimo → vince la prima trovata da `getRecipeFor`, `EnchanterBlockEntity.java:610-612`).
  2. **Rendere ottenibile `spectrum:milestones/unlock_overenchanting_with_enchanter`** sovrascrivendo il JSON (oggi dipende da `spectrum:__wip` inesistente), legandolo per es. al primo tool T6 dell'addon.
  3. Il libro risultante va poi applicato: il clamp per tier dei mixin §5.2 decide quanto ne "assorbe" l'oggetto.

### 5.6 Compatibilità con le mod dell'istanza
Mod rilevanti trovate con `ls` in `mods/` [V]: `ApothicEnchanting-1.21.1-1.6.4.jar`, `Apotheosis-1.21.1-8.9.0.jar`, `ApothicAttributes-1.21.1-2.11.0.jar`,
`Placebo-1.21.1-9.9.4.jar`, `create-enchantment-industry-2.5.4b.jar`, `disenchanting_table-merged-1.21.1-5.0.2.jar`, `enchdesc-neoforge-1.21.1-21.1.11.jar`,
`spectrumcompat-0.1.0.jar`, `SpectrumJEI-21.1.11.1+neoforge.jar`.

- **Apothic Enchanting** — `cfg:apotheosis/enchantments.cfg` contiene per ogni incantesimo `Max Level`, `Max Loot Level`, `Forced Level Cap` (range 1–127) [V]. Valori nell'istanza (tutti default di Apothic, `Forced Level Cap = -1` ovunque):

  | Incantesimo | orig. | Max Level Apothic | riga cfg |
  |---|---|---|---|
  | sharpness | 5 | **9** | 1930 |
  | smite / bane_of_arthropods | 5 / 5 | 10 / 10 | 2022 / 58 |
  | protection | 4 | 8 | 1562 |
  | efficiency | 5 | 9 | 564 |
  | unbreaking | 3 | 8 | 2367 |
  | fortune / looting | 3 / 3 | 8 / 8 | 748 / 1093 |
  | power | 5 | 9 | 1516 |
  | feather_falling / projectile_protection | 4 / 4 | 11 / 11 | 633 / 1539 |
  | spectrum:exuberance | 5 | 9 | 2827 |
  | spectrum:clovers_favor / treasure_hunter / razing / inertia / big_catch | 3 | 7 | 2781 / 3632 / 3287 / 3126 / 2735 |
  | spectrum:improved_critical / first_strike / sniping / disarming / serendipity_reel | 2 | 6 | 3080 / 2873 / 3425 / 2804 / 3402 |
  | mending, silk_touch, infinity, spectrum:foundry/voiding/resonance/... | 1 | 1 | — |

  **[D]** Apothic applica questi valori agganciando `Enchantment.getMaxLevel()` (mixin): non ho potuto aprire il jar (vincolo "solo ls"); da verificare con
  `unzip -p mods/ApothicEnchanting-*.jar apothic_enchanting.mixins.json` o in gioco (Sharpness VI–IX ottenibile all'incudine normale?).
  Se confermato:
  - Il Bedrock Anvil di Spectrum, che clampa a `getMaxLevel()`, in ATM10 **già** permette Sharpness IX a qualunque oggetto.
  - `isInNormalRange` dell'Enchanter è vero fino a VIII → overenchanting non necessario (§1.3a).
  - Il nostro `@WrapOperation` su `getMaxLevel()` *dentro* `createResult` riceve come `original` il valore già modificato da Apothic → usare `original + bonus` si compone bene.
  - Se Apothic usa un `@Inject HEAD cancellable` su `getMaxLevel` e noi **non** tocchiamo `getMaxLevel` globalmente (solo le chiamate dentro i metodi Spectrum), non c'è conflitto di mixin.
  - **Forced Level Cap** (se un pack lo imposta) limita il livello *effettivo* "regardless of NBT" → se impostato, i nostri livelli oltre quel valore non avrebbero effetto in combattimento [D].
- **Tetto 127 di Apothic**: le sue config accettano max 127; non è un limite del gioco ma un'indicazione che oltre 127 Apothic non è testato [D].
- **Enchantment Descriptions / tooltip**: i numeri romani vanilla esistono solo fino a `enchantment.level.10`; oltre, senza una mod che li generi, il tooltip mostra la chiave grezza [D]. Apothic dovrebbe gestirlo; da verificare.
- **Create: Enchantment Industry, Disenchanting Table**: possono estrarre/copiare libri da oggetti → un oggetto T8 con Sharpness IX potrebbe "esportare" un libro IX applicabile poi altrove (via Enchanter, che non clampa!) — motivo in più per clampare all'Enchanter (§5.2) [D].
- **spectrumcompat / SpectrumJEI**: solo integrazioni/visualizzazione; la UI di JEI per gli upgrade non è stata analizzata.

### 5.7 Tetto tecnico reale del livello in 1.21.1 [V]
- `ItemEnchantments.LEVEL_CODEC = Codec.intRange(0, 255)` (`mc: ItemEnchantments.<clinit>`, `sipush 255; Codec.intRange`).
- Il costruttore di `ItemEnchantments` lancia `IllegalArgumentException` se un livello è `< 0` o `> 255` (`mc: ItemEnchantments.<init>`, offset 53-90).
- `ItemEnchantments.Mutable.set(...)` e `.upgrade(...)` fanno `Math.min(level, 255)` (`mc: ItemEnchantments$Mutable.set` offset 22-25, `.upgrade` offset 10-13).
- `Enchantment.MAX_LEVEL = 255` (costante pubblica) e nel JSON `max_level` è `ExtraCodecs.intRange(1, 255)` (`mc: Enchantment$EnchantmentDefinition`, `sipush 255` + `"max_level"`).
- Lo stream codec di rete usa `VAR_INT` (nessun limite a 127) [V].
- **Quindi "Alloy senza limiti" = massimo 255.** Superarlo richiederebbe `@ModifyConstant` su tre punti vanilla + codec: sconsigliato (rompe salvataggi/compat). In pratica conviene un tetto Alloy configurabile (es. 10–20) perché molti effetti scalano linearmente (Sharpness 255 ≈ +128 danni) e le formule di Spectrum (`first_strike_damage_per_level`, `disarming_chance_per_level_*`) diventano degeneri.

---

## 6. Raccomandazione

1. **Non usare la config di Spectrum** (`bedrock_anvil_can_exceed_max_vanilla_enchantment_level` resta `false`): è globale e senza tetto.
2. **Un'unica funzione `capFor(stack, ench)`** nell'addon, con:
   - `base = enchantment.getMaxLevel()` (rispetta Apothic/datapack; opzione config per usare `definition().maxLevel()`),
   - `bonus` letto da **item tag** (`spectral_ascension:enchant_bonus/plus_1..plus_4`, `.../unlimited`), con override opzionale via data component,
   - tetto Alloy configurabile (default proposto: `base + 10`, massimo 255).
   - Config dell'addon in un file **`COMMON`/`SERVER`** (non STARTUP) così è per-mondo e sincronizzabile.
3. **Mixin minimi e "stabili"** (§5.2):
   - `BedrockAnvilScreenHandler.createResult` → `@WrapOperation` su `Enchantment.getMaxLevel()` con `@Local` dello stack in input → `capFor`.
   - `EnchanterBlockEntity.enchantCenterItem` + `getRequiredExperienceToEnchantCenterItem` → `@ModifyArg` sul livello passato ad `addOrUpgradeEnchantment`; più il controllo coerente in `getRequiredExperienceToEnchantWithEnchantment` / `isValidCenterEnchantingSetup` per evitare loop. Questo chiude anche la "falla" per cui oggi l'Enchanter applica libri di qualsiasi livello a qualsiasi oggetto (decidere se applicarlo anche agli oggetti non-addon: consiglio **sì**, con `bonus = 0`, ma dietro un'opzione config per non cambiare il comportamento di Spectrum senza volerlo).
4. **Datapack interno all'addon** (§5.5): estendere le ricette `enchantment_upgrade` per avere libri fino ai livelli T8/Alloy e rendere ottenibile `unlock_overenchanting_with_enchanter` legandolo alla progressione dell'addon.
5. **Facoltativo**: `GetEnchantmentLevelEvent` (o override di `IItemExtension.getEnchantmentLevel`) per un bonus passivo dell'Alloy, separato dal sistema di cap.
6. **Prima di implementare, verificare in gioco/nel jar**: (a) che Apothic agganci `getMaxLevel()` e quindi che in ATM10 Sharpness IX sia già raggiungibile → decidere la "Decisione aperta 1"; (b) che l'Enchanter nell'istanza porti già i libri a VIII senza advancement; (c) la resa dei tooltip oltre il livello X.

### Incertezze principali
- Meccanismo esatto di Apothic Enchanting sul max level (mixin su `getMaxLevel` vs modifica del registry) — **[D]**, influisce sulla scelta di `base`.
- Semantica precisa di `ModConfig.Type.STARTUP` (non sincronizzata, letta alla costruzione) riportata da documentazione NeoForge, non verificata nel bytecode.
- Indici `@Local`/`ordinal` dei mixin vanno confermati sul bytecode della versione di Spectrum effettivamente usata (variabili locali di `createResult`).
