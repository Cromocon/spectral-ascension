# 13 · Menu della workstaff: come è fatto e come estenderlo per T7+

> Solo analisi: per il T6 non serve niente di nuovo nel menu (Silk Touch / Fortune / Resonance e area fino a 5×5 sono già quelli di Spectrum, ereditati estendendo `GlassCrestWorkstaffItem`). Qui si studia come aggiungere, dal T7 in su, **Foundry**, **Inventory Insertion** e le aree **7×7 / 9×9** (decisione di design: T7 + Foundry e 7×7, T8 + Inventory Insertion, Alloy 9×9).

Percorsi relativi a `src/main/java/de/dafuqs/spectrum/` del sorgente di Spectrum ≈1.12.9; `jar:` = `spectrum-1.12.9-1.21.1-neo.jar`.

## 1. Come funziona oggi

### 1.1 Apertura del menu

| Passo | Dove | Cosa fa |
| --- | --- | --- |
| Shift + tasto destro | `items/tools/WorkstaffItem.java:58-67` (`use`) | Sul server apre `createScreenHandlerFactory(stack)` |
| Fabbrica del menu | `WorkstaffItem.java:84-89` | `SimpleMenuProvider` → `new WorkstaffScreenHandler(syncId, inv, stack)`. Il metodo è **public e non final**: una sottoclasse può restituire un menu suo |
| Tipo di menu | `inventories/SpectrumMenuTypes.java:20` | `WORKSTAFF = registerSimple(... WorkstaffScreenHandler::new)` |
| Schermata client | `inventories/SpectrumMenuTypes.java:67` | `event.register(SpectrumMenuTypes.WORKSTAFF, WorkstaffScreen::new)` |
| Handler | `inventories/WorkstaffScreenHandler.java:10-34` | Costruttore con `super(SpectrumMenuTypes.WORKSTAFF, syncId)` **fisso** (riga 17); `stillValid` controlla che lo stack sia ancora *lo stesso oggetto* in una mano (righe 22-29); `onWorkstaffToggleSelectionPacket` → `WorkstaffItem.applyToggle` (righe 31-33) |

### 1.2 La schermata (menu "a croce", non radiale)

- `inventories/QuickNavigationGridScreen.java`: ogni `Grid` (righe 47-92) ha **5 caselle**: centro + su/destra/giù/sinistra. Le sotto-griglie si aprono con `selectGrid` (riga 292, `protected`) e si impilano in `gridStack` (riga 276, `public final`). `GridEntry.text/item/colored/textured` (righe 112-130) sono factory pubbliche.
- `inventories/WorkstaffScreen.java`:
  - `RANGE_GRID` (righe 18-24, `private static`): su = 1×1, destra = 5×5, giù = indietro, sinistra = 3×3. **Tutte le caselle occupate**.
  - `ENCHANTMENT_GRID` (righe 26-32, `private static`): su = Silk Touch, destra = indietro, giù = Resonance, sinistra = Fortune. **Tutte occupate**.
  - Costruttore (righe 34-72): sceglie la griglia principale in base a `instanceof GlassCrestWorkstaffItem` (riga 45). Per la glass crest: centro = chiudi, su = area, destra = azioni tasto destro, giù = proiettili, sinistra = incantesimi. **Anche la griglia principale è piena.**
  - `select(toggle)` (righe 74-79): manda al server `WorkstaffToggleSelectedPayload(toggle.ordinal())` e chiude.

### 1.3 Rete

- `networking/c2s_payloads/WorkstaffToggleSelectedPayload.java:13-36`: un solo `int` = **ordinale** di `WorkstaffItem.GUIToggle`. Il gestore (righe 26-35) accetta il pacchetto solo se `player.containerMenu instanceof WorkstaffScreenHandler` e fa `GUIToggle.values()[index]` **senza controllo dei limiti**.
- Registrato in `networking/SpectrumC2SPackets.java:27`.

### 1.4 Le opzioni (`WorkstaffItem`)

- `GUIToggle` (`WorkstaffItem.java:30-52`): `SELECT_SILK_TOUCH`, `SELECT_FORTUNE`, `SELECT_RESONANCE`, `SELECT_1x1`, `SELECT_3x3`, `SELECT_5x5`, abilita/disabilita azioni col tasto destro, abilita/disabilita proiettili. Enum normale (non `IExtensibleEnum`).
- `applyToggle` (`WorkstaffItem.java:102-139`, `public static`): le aree scrivono il componente `spectrum:aoe` (1×1 = rimosso, 3×3 = 1, 5×5 = 2); gli incantesimi passano da `enchantAndRemoveOthers`.
- `enchantAndRemoveOthers` (`WorkstaffItem.java:141-174`): se lo stack ha già l'incantesimo, messaggio e basta; per Fortune usa il livello memorizzato in `spectrum:workstaff.fortune_level` (riga 153), passando ad altro memorizza `max(memorizzato, Fortune attuale, 1)` (righe 155-157); poi toglie Silk/Resonance/Fortune e aggiunge il nuovo con `addOrUpgradeEnchantment(..., allowConflicts = advancement APPLY_CONFLICTING_ENCHANTMENTS)` (riga 166). **Verificato dal GameTest `workstaff_menu_toggle`**: Fortune VIII → Silk Touch → Resonance → Fortune VIII, Efficiency IX resta.
- Componente `spectrum:workstaff` = `WorkstaffComponent(canTill, canShoot, fortuneLevel)`, default `(true, false, 4)` (`components/WorkstaffComponent.java:8-10`).

### 1.5 Dove si legge l'area

- `api/item/AoEBreakingTool.java:40-42`: `getAoERange` = `spectrum:aoe` (default 0). Interfaccia pubblica, metodo `default`: si può sovrascrivere.
- `AoEBreakingTool.afterBreakingBlock` (righe 13-23), chiamato da `registries/SpectrumEventListeners.java:1039-1041` per l'oggetto in mano principale.
- `helpers/AoEHelper.java:22-41`: il raggio è **generico** (nessun limite a 2). Attenzione: sulle pareti l'area va da `-1` a `2r-1` in verticale (riga 37-38), quindi parte un blocco sotto quello colpito; vale per qualunque raggio. I blocchi si rompono con `gameMode.destroyBlock` (riga 96), cioè come se li rompesse il giocatore: drop, incantesimi e mixin dei drop si applicano a ogni blocco.
- Costo: `WorkstaffItem.canUseAoE` (righe 92-100) = `3^raggio` di inchiostro **bianco** per ogni blocco rotto a mano: 3×3 = 3, 5×5 = 9, **7×7 = 27, 9×9 = 81**. Metodo public, sovrascrivibile.
- Tooltip `mining_range` (`WorkstaffItem.java:69-77`): mostra `1+2r` per qualunque `r`, quindi 7×7 e 9×9 compaiono già giusti.

## 2. Foundry e Inventory Insertion esistono già in Spectrum

Non c'è da inventare la logica: sono **incantesimi di Spectrum**.

| Incantesimo | Chiave | Effetto | Esclusivo con | Oggetti |
| --- | --- | --- | --- | --- |
| Foundry | `SpectrumEnchantmentKeys.FOUNDRY` (`registries/SpectrumEnchantmentKeys.java:16`) | Fonde i drop: `mixin/BlockBehaviorMixin.java:83-87` (tag `spectrum:effect/smelts_more_loot`) | `minecraft:silk_touch` (`jar:data/spectrum/tags/enchantment/exclusive_set/foundry.json`) | `#minecraft:enchantable/mining_loot` + canne |
| Inventory Insertion | `SpectrumEnchantmentKeys.INVENTORY_INSERTION` (riga 21) | Drop direttamente nell'inventario: `BlockBehaviorMixin.java:89-110` (tag `spectrum:effect/inventory_insertion`) | nessuno | `#minecraft:enchantable/mining` e altri |

Il mixin agisce su `Block.getDrops` con lo strumento nei parametri di loot, quindi funziona anche sui blocchi rotti dallo scavo ad area (vedi 1.5). Ordine nel mixin: Resonance → Foundry → Inventory Insertion.

Conseguenze:
- **Inventory Insertion (T8)** non confligge con niente: basta metterla negli incantesimi del `result` della ricetta T8 (sempre attiva). Il menu serve solo se si vuole poterla spegnere.
- **Foundry (T7)** confligge con Silk Touch. Se fosse solo "incisa" nel risultato, scegliendo Silk Touch dal menu di Spectrum `addOrUpgradeEnchantment` rifiuterebbe ("would result in conflicting enchantments", `WorkstaffItem.java:171`), salvo chi ha l'advancement `APPLY_CONFLICTING_ENCHANTMENTS`. Serve quindi un interruttore Foundry nel menu, che la tolga quando si sceglie Silk Touch e la ricordi.

## 3. Come aggiungere le opzioni T7+

### 3.1 Cosa si riusa senza toccare Spectrum

- `WorkstaffItem.applyToggle` (public static) per tutte le opzioni di Spectrum.
- Componente `spectrum:aoe` (registro pubblico `SpectrumDataComponentTypes.AOE`) per 7×7 (= 3) e 9×9 (= 4): lo scavo, il costo e il tooltip lo gestiscono già.
- `QuickNavigationGridScreen` / `QuickNavigationGridScreenHandler` / `GridEntry` (public) per avere lo stesso aspetto.
- Traduzioni `item.spectrum.workstaff.gui.*` e `item.spectrum.workstaff.message.*` per le voci esistenti.
- `copy_components` porta `spectrum:aoe`, `spectrum:workstaff` e un nostro eventuale componente da un tier al successivo.

### 3.2 Cosa va fatto di nostro (consigliato: menu nostro, niente mixin)

1. **Sovrascrivere `createScreenHandlerFactory`** nella nostra `SATools.Workstaff` (solo dai tier che hanno opzioni in più) per aprire un **nostro** `MenuType` + handler. Non si può estendere `WorkstaffScreenHandler`: il suo costruttore usa `SpectrumMenuTypes.WORKSTAFF` fisso, quindi il client aprirebbe la schermata di Spectrum.
2. **Schermata nostra** che estende `QuickNavigationGridScreen` e ricostruisce le griglie (quelle di Spectrum sono `private static`). La griglia principale e le sotto-griglie di Spectrum sono piene: servono livelli in più, per esempio
   - "Area" → sotto-griglia con 1×1 / 3×3 / 5×5 / "più grande" → 7×7 / 9×9 (solo se il tier lo permette);
   - "Incantesimi" → Silk / Fortune / Resonance / "Drop" → Foundry on/off, Inventory Insertion on/off.
   La disposizione è una scelta di design da fare.
3. **Pacchetto nostro** (C2S) con un nostro enum di opzioni: `SPECTRUM(GUIToggle)` per inoltrare a `applyToggle`, più `FOUNDRY_ON/OFF`, `INSERTION_ON/OFF`, `SELECT_7x7`, `SELECT_9x9`. Non si può riusare `WorkstaffToggleSelectedPayload`: accetta solo se il menu aperto è `WorkstaffScreenHandler` e usa gli ordinali di Spectrum.
4. **Controlli lato server**: l'oggetto in mano è la nostra workstaff, il tier permette l'opzione (un client modificato potrebbe chiedere 9×9 col T7), indice dell'enum nei limiti, stesso controllo di identità dello stack di `stillValid`.
5. **Foundry**: interruttore che aggiunge/toglie `spectrum:foundry` e ne ricorda lo stato in un **nostro componente** (es. `spectral_ascension:workstaff_drops {foundry, insertion}`), come Spectrum ricorda Fortune. Scegliendo Silk Touch: togliere Foundry prima di `applyToggle`; tornando a Fortune/Resonance: rimetterla se era attiva. Accendere Foundry con Silk Touch attivo: passare prima a Fortune, oppure rifiutare con un messaggio (da decidere).
6. **Limite d'area per tier**: sovrascrivere `getAoERange` per limitare il valore al massimo del tier (difesa se un componente arriva da altrove) ed eventualmente `canUseAoE` per un costo d'inchiostro meno ripido di `3^r` (81 inchiostri bianchi a blocco per il 9×9).

### 3.3 Alternativa con mixin (sconsigliata)

- Aggiungere valori a `GUIToggle`: l'enum non è estendibile; servirebbe un hack sugli enum, e gli ordinali (che viaggiano in rete) cambierebbero se Spectrum aggiungesse voci.
- `@Inject` alla fine del costruttore di `WorkstaffScreen` per sostituire la griglia quando l'oggetto è nostro: fattibile, ma servirebbe comunque un nostro pacchetto per le nuove opzioni, e il mixin dipende dalla struttura interna della schermata.

## 4. Rischi

- **API non stabile**: `GUIToggle` e i suoi ordinali, i costruttori di `WorkstaffScreenHandler`/`QuickNavigationGridScreen`, il componente `WorkstaffComponent` possono cambiare tra versioni di Spectrum. Con il menu nostro dipendiamo solo da `applyToggle`, `spectrum:aoe` e dalle classi della griglia.
- **Costo e prestazioni**: 9×9 = 80 blocchi in più per colpo, ognuno con loot, Foundry (ricerca della ricetta di fusione) e inserimento nell'inventario. Da provare su un server.
- **Area asimmetrica in verticale** (1.5): con raggio 4 si scava da 1 sotto a 7 sopra il blocco colpito. È il comportamento di Spectrum, ma si nota di più con aree grandi.
- **Foundry e Silk Touch**: con l'advancement `APPLY_CONFLICTING_ENCHANTMENTS` Spectrum permette di tenerli insieme; il nostro interruttore deve comportarsi in modo coerente in entrambi i casi.
- **Progressione**: le opzioni T7+ non devono comparire nel menu del T6 (controllo sul tier sia nella schermata sia sul server).
- **Senza client** (GameTest) si possono provare `applyToggle`, i componenti e la logica del pacchetto lato server, non la schermata.

## 5. Per il T6

Nessuna modifica al menu: `allthemodium_workstaff` estende `GlassCrestWorkstaffItem` e usa il menu di Spectrum così com'è (area fino a 5×5, Silk Touch / Fortune VIII / Resonance, azioni col tasto destro, proiettili). Sono verificati da GameTest `workstaff_menu_toggle` e `workstaff_fusion_merge` (vedi `SAGameTests`).
