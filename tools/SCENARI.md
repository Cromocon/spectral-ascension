# Scenari in gioco (scenario runner)

Il runner esegue nel client di sviluppo una sequenza di passi letti da un file JSON. Salva gli screenshot e scrive un report.
Serve a controllare tre cose: le animazioni delle texture, il funzionamento degli oggetti e lo sblocco della progressione di Spectrum.

- **Solo sviluppo.** Il codice sta nel source set `dev`: `src/dev/java/cromocon/spectralascension/dev/` e `src/dev/resources/`.
  - È una mod separata, `spectral_ascension_dev`, caricata solo da `runClient` e `runClientAuto`.
  - Il jar di `./gradlew build` non la contiene.
- **Mai attivo da solo.** Parte solo in due modi:
  - con la property `-Dspectral_ascension.autoScenario=...` (avvio automatico);
  - con il comando `/sa scenario run <nome>`.

## Come lanciarli

**Automatico**, per l'agente: niente finestra sul desktop. Il gioco si chiude da solo a fine scenari.
```
tools/run_auto.sh --scenario smoke --timeout 400       # uno o più scenari: smoke,presets
tools/run_auto.sh --timeout 600                        # tutti (= all)
python3 -I tools/check_scenari.py                      # riassunto del report e del log (--all: anche i passi OK)
```
Equivale a `./gradlew runClientAuto -PsaScenario=smoke`. Senza `-PsaScenario` esegue tutti gli scenari.

**A mano**: `./gradlew runClient`. Nel mondo `sa_test` usa questi comandi del client, che non richiedono di essere op:
- `/sa scenario list`: elenca gli scenari e i loro file;
- `/sa scenario run smoke`: esegue uno scenario, oppure più nomi separati da spazi o virgole, oppure `all`;
- `/sa scenario stop`: interrompe e scrive comunque il report.

### Configurazioni opzionali (solo sviluppo, scaricate da Gradle da Modrinth)

Per default il dev carica EMI + JEI, senza SpectrumJEI e senza Apothic. Due varianti, scelte con una property di Gradle:

- **JEI come in ATM10**: `tools/run_auto.sh --jei ...` oppure `./gradlew runClient -PjeiOnly`. Carica JEI + SpectrumJEI
  (`maven.modrinth:spectrumjei`) **senza EMI** (SpectrumJEI «sconsiglia» EMI e FML bloccherebbe il client con un avviso).
  - azione `jei_dump` (facoltativo `modid`, default `spectral_ascension`): conta e scrive nel log (`[SA-JEI]`) le ricette Fusion Shrine
    che SpectrumJEI mostra, visibili/totali e nostre visibili/nostre totali; serve il runtime di JEI (plugin dev `JeiDev`);
  - `open_screen` con `screen: jei` + `item`: apre JEI sulla ricetta Fusion Shrine che produce l'oggetto;
  - scenari `jei_bloccate` (giocatore vergine: ricette attese nascoste) e `jei_ricette` (da lanciare dopo `sblocchi`: conteggio e
    uno screenshot per tier): `tools/run_auto.sh --jei --scenario jei_bloccate,sblocchi,jei_ricette`.
- **Dove stanno i GameTest**: `src/gametest/java` (source set `gametest`, stessa mod `spectral_ascension`), NON nel jar di rilascio. Package invariato (`cromocon.spectralascension.gametest`).
- **Apothic Enchanting vero**: `./gradlew runGameTestServer -Papothic` (vale anche per `runClient`/`runClientAuto`, non provato sul client).
  Carica Apothic Enchanting 1.6.4 + Placebo 9.9.4 + Apothic Attributes 2.11.0 (scaricati da Gradle da Modrinth, gli stessi dell'istanza ATM10) e copia
  `src/gametest-config/apotheosis/*.cfg` (config dell'istanza, tutta ai valori di default) in `run/gametest/config/apotheosis` e
  `run/client/config/apotheosis` a ogni avvio (task `copyApothicConfig`). Con Apothic si registrano anche i GameTest di
  `SAApothicTests` (batch `sa_apothic`: tomo di estrazione vero, incudine, Enchanter, overenchanting, massimi a runtime contro
  `SAEnchantCap`); senza Apothic non esistono e la run resta di 72 test. Alcuni test leggono `ModList` e cambiano le attese dove
  Apothic cambia davvero i dati (Sharpness non è più in conflitto con Impaling e Smite).

## Dove sono i risultati (in `run/client/`)

| Cosa | Dove |
|---|---|
| Screenshot | `screenshots/scenari/<scenario>/<NN>_<nome>.png` (NN = numero del passo). Si svuota a ogni nuova esecuzione dello scenario |
| Report | `scenari-report/<data_ora>.json` e `scenari-report/latest.json`: esito di ogni passo, dettagli e percorsi assoluti degli screenshot |
| Log | `logs/latest.log`: una riga per passo, `[SA-SCENARIO] <scenario> #<n> <azione> OK\|FAIL <dettagli>`, più `BEGIN`, `END` e `REPORT` |

Per ogni screenshot il runner misura la luminosità media e il numero di colori.
Un'immagine di un solo colore (per esempio nera) fa fallire il passo.

## Formato

Gli scenari si cercano in due cartelle, in quest'ordine:
1. `run/client/scenarios/*.json`: scenari locali, non versionati. Vincono in caso di nome uguale.
2. `src/dev/resources/scenarios/*.json`: scenari del progetto. Si leggono direttamente dai sorgenti, quindi una modifica vale subito, senza ricompilare.

Nel JSON sono ammessi i commenti `//`.

```json
{
  "description": "cosa verifica",
  "stop_on_fail": false,
  "steps": [
    {"action": "command", "command": "give @s spectral_ascension:trichromatic_sword"},
    {"action": "screenshot", "name": "spada"}
  ]
}
```

Campi validi in ogni passo:
- `stop_on_fail` sovrascrive quello dello scenario. Con `true`, un passo fallito ferma lo scenario e i passi restanti risultano "saltati".
- `timeout` è in tick. Il default è 30 secondi, oppure la durata prevista del passo più 10 secondi.

Le coordinate sono `[x, y, z]` assolute. Il mondo `sa_test` è superflat: il terreno è a y=-61 e il giocatore sta a y=-60.

### Azioni

| Azione | Parametri | Note |
|---|---|---|
| `command` | `command` (stringa) o `commands` (lista); `as`: `player` (default) o `server`; `expect`: `success` (default), `fail` o `any` | Il comando gira sul server integrato con permessi op (livello 4). Copre give, setblock, fill, gamemode, tp e advancement. Le risposte del comando finiscono nei dettagli |
| `client_command` | `command` | Comando del client, per esempio `sa scenario list`. L'esito va in chat e nel log |
| `wait` | `ticks` | |
| `screenshot` | `name`, `frames` (3), `toasts` (false), `chat` (false) | Scatto a fine frame con `Screenshot.grab`. Prima toglie i toast, come "Advancement Made!", e svuota la chat, che altrimenti copre il testo sopra la hotbar (`chat: true` per tenerla) |
| `screenshot_series` | `name`, `count` (6), `every` (2 tick), `chat` (false) | Per le animazioni. Esempio: la spada ha `frametime` 3, quindi `every: 3` dà un fotogramma per scatto |
| `clear_chat` | | Svuota la chat del client |
| `hold` | `slot` (0-8), `item` (facoltativo, con componenti: `id[...]`), `hand`: `main` o `off` | Mette l'oggetto nello slot con `item replace` e lo seleziona |
| `look` | `yaw`/`pitch`, oppure `at: [x,y,z]` (centro del blocco; `block_center: false` per le coordinate esatte) | |
| `fly` | `value` (true) | Volo creativo, da usare **dopo** il `tp` in aria per le inquadrature dall'alto |
| `hide_hud` | `value` (true/false), `hand` (true) | Nasconde hotbar, mirino e chat, ma lascia la mano. Con `hand: false` nasconde anche la mano, come F1 |
| `fov` | `value` (gradi, 30-110), `effects` (0: scala degli effetti FOV di volo e scatto) | Campo visivo per le inquadrature «da foto prodotto»: FOV stretto da lontano, fuori dalla portata del giocatore (niente contorno del blocco mirato). Non tocca `options.txt`: a fine scenario torna il valore di prima (il client di sviluppo ha FOV 110) |
| `first_person`, `third_person` | `view`: `back` (default) o `front` | `front` mostra il giocatore di fronte, utile per l'armatura indossata |
| `open_screen` | `screen`: `inventory`, `guidebook`, `recipes` o `none`; per il guidebook: `book` (`spectrum:guidebook`), `entry` (es. `spectrum:general/fusion_shrine`), `category`, `page` (numero della pagina nel JSON della voce); per `recipes`: `recipe` (id) oppure `item` | Apre la voce anche se è bloccata. `recipes` apre EMI (per riflessione: EMI è solo `runtimeOnly`) sulla ricetta o sulle ricette dell'oggetto |
| `book_focus` | `entry`, `book`, `hover` (true) | Apre la mappa della categoria della voce, la porta al centro e ci mette sopra il mouse: lo screenshot mostra il tooltip della voce (nome e descrizione). Con `hover: false` centra soltanto |
| `buy_hint` | `index` (0) | Preme il pulsante «Uncover Insight» di una pagina `spectrum:hint` della voce aperta (`index`: 0 = pagina di sinistra, 1 = destra), come il clic del giocatore. Serve la voce aperta con `open_screen`; in creativo il pagamento è gratis e il server non conta come acquisto, quindi per verificare il costo usa `gamemode survival` (con `expect: success`). Prima del clic il passo aspetta (`survival_timeout`, 100 tick) che il client sia survival e fallisce se resta creativo; nei dettagli riporta gli oggetti del costo nell'inventario client prima/dopo (il server li toglie dopo, controllare con `assert_item` + `exact`) |
| `close_screen` | | |
| `gui_click` | `offset` [dx, dy] dal centro della finestra, oppure `pos` [x, y] (coordinate GUI, scalate); `screen` (parte del nome di classe della schermata attesa, es. `BidentScreen`); `click` (true; false = solo mouse sopra, per fotografare un tooltip); `button` (0 = sinistro); `within` (40 tick di attesa della schermata) | Sposta davvero il mouse e chiama `mouseClicked`/`mouseReleased` della schermata, come il gioco. Menu del bident: spento `[-4, -64]`, Ferocious `[-64, -4]`, Fractal `[56, -4]`, centro (chiudi) `[0, 0]` |
| `gui_key` | `key`: `left`, `right`, `up`, `down`, `drop`, `inventory` | Chiama `keyPressed` della schermata aperta col tasto del gioco (tasti di movimento / Drop / Inventario dei menu "quick navigation") |
| `assert_screen` | `screen` (parte del nome di classe, oppure `none`); `within` (20 tick) | Quale schermata è aperta. Le schermate aperte dal server (menu) arrivano con un pacchetto: il passo aspetta |
| `use_item` | `hand`; `hold_ticks` (tiene premuto N tick); `keep: true` (tiene premuto fino a `release_use`) | Clic destro simulato (`MultiPlayerGameMode.useItem`) |
| `release_use` | | Rilascia il tasto destro |
| `use_on_block` | `pos`, `face` (`up`, `north`, ...), `hand` | Clic destro su un blocco |
| `sneak` | `value` (true/false) | Shift tenuto premuto. Esempio: Shift + `use_item` apre il menu del bident |
| `move` | `direction`: `forward`, `back`, `left`, `right`, combinabili con `+` (es. `forward+left`), oppure `none`; `sprint` (false); `ticks` (20); `keep` (false) | Tiene premuti i tasti di movimento come il giocatore. Senza `keep` li rilascia dopo `ticks`; con `keep: true` restano premuti fino al `move` successivo, così si può fotografare il giocatore mentre cammina o corre (es. mantello e perizoma dell'armatura). `none` rilascia tutto. Ogni `move` rilascia prima i tasti del precedente e, senza `sprint`, interrompe lo scatto in corso; il passo fallisce se il giocatore non si è spostato |
| `build` | `preset` + `pos`, oppure `multiblock` + `center`, oppure `blocks` (lista `{pos, block}`, con `origin` facoltativa), oppure `file` | Vedi "Strutture" qui sotto. `clear` e `validate` valgono true per default |
| `reset_book` | `book` (`spectrum:guidebook`) | Ricalcola da zero gli sblocchi del libro: Modonomicon **non** ri-blocca le voci quando un advancement viene revocato |
| `assert_advancement` | `id`, `done` (true) | Controllo sul server; nei dettagli c'è anche lo stato visto dal client |
| `assert_item` | `item` o `predicate` (sintassi di `/clear`, es. `spectral_ascension:trichromatic_bident[spectral_ascension:bident_mode=fractal]`), `count` (1), `exact` (false: la quantità deve essere esattamente `count`, non almeno), `slot` (`any`, `mainhand`, `offhand`, `head`, `chest`, `legs`, `feet`), `enchantments` (`{"minecraft:sharpness": 5}` = livello minimo; vale anche per i libri), `present` (true); `pos` + `radius` (3): cerca gli oggetti **a terra** attorno a `pos` invece che nell'inventario; `within` (tick): ripete il controllo ogni 10 tick finché passa, per aspettare la fine di un craft; `dump` (false): scrive i componenti degli oggetti trovati | Con un predicato `[componente=valore]` il confronto è **esatto** (es. tutti e soli gli incantesimi). Se non corrisponde nulla, i dettagli mostrano i componenti degli oggetti con lo stesso id |
| `assert_block` | `pos`, `block` (id o `#tag`), `properties` (`{"facing": "north"}`) | |
| `assert_multiblock` | `preset` + `pos`, oppure `multiblock` + `center`; `valid` (true) | È lo stesso `Multiblock.validate` che usano Pedestal e Fusion Shrine. Se fallisce, elenca i blocchi sbagliati |
| `assert_recipe_unlocked` | `recipe` (id), `unlocked` (true) | Vedi sotto |
| `assert_book_entry` | `entry`, `book`, `unlocked` (true), `page` (facoltativo: indice nella lista `pages` del JSON) | Stato di sblocco della voce del guidebook (Modonomicon, sul server), oppure di una sua pagina. I dettagli dicono anche se la condizione è vera adesso. Le pagine ricetta di Spectrum (`pedestal_crafting`, `fusion_shrine_crafting`) sono sbloccate solo se la ricetta lo è |
| `assert_revealed` | `item` o `block`, `revealed` (true) | Revelationary, lato client: l'oggetto o il blocco è ancora "mascherato"? |
| `assert_log` | `regex`, `present` (true), `within` (40 tick di attesa) | Cerca nelle righe di log registrate dall'inizio dello scenario, tenute in memoria. Le righe `[SA-SCENARIO]` sono escluse |
| `track_projectile` | `ticks` (8), `max_angle` (gradi, facoltativo), `from` (1) | Spara con l'oggetto in mano (clic destro, come `use_item`: per esempio una balestra carica) e segue per `ticks` tick la freccia appena comparsa, sul client e sul server integrato (stessa entità). Per ogni tick scrive nei dettagli posizione e velocità dei due lati e l'angolo fra le velocità. Con `max_angle` fallisce se l'angolo supera il limite dal tick `from` in poi |
| `mine` | `pos`, `face` (`north`); `look` (true); `from` + `to` + `block` (`minecraft:air`), `count` | Clic sinistro vero: con `look` gira il giocatore verso il centro della faccia e aspetta 3 tick (il server rifà il raycast per l'area della workstaff), poi `MultiPlayerGameMode.startDestroyBlock`. Con `from`/`to` conta, **nello stesso tick e sul ClientLevel**, i blocchi `block` della zona; `count` = numero esatto atteso. Scavo istantaneo solo se `getDestroyProgress` ≥ 1 (in sopravvivenza: Efficiency, Haste) |
| `count_blocks` | `from`, `to`, `block` (id o `#tag`, default `minecraft:air`), `side`: `client` (default, ClientLevel, subito) o `server`; `count` (esatto) oppure `min`/`max` | Conta i blocchi di una zona |
| `area_prediction` | `value` (true) | Solo sviluppo: accende o spegne la previsione sul client dello scavo ad area della workstaff (`WorkstaffAreaPrediction.enabled`), per il confronto. A fine scenario torna accesa. Scenario: `workstaff_area` |
| `particles` | `min`, `max` | Particelle vive sul client (`ParticleEngine.countParticles`, il numero «P:» della schermata F3). Dopo un `wait` di 1 tick conta quelle appena create. Scenario: `workstaff_area_9x9` |
| `log` | `text` | Annota il report |

### Ricette bloccate: come decide Spectrum

`assert_recipe_unlocked` chiama `GatedRecipe.canPlayerCraft(player)` di Spectrum due volte:
- sul server, che decide se il craft viene accettato;
- sul client, che decide se la ricetta viene mostrata.

`canPlayerCraft` richiede due advancement, controllati con `AdvancementHelper` di Revelationary:
- l'advancement di sblocco del **tipo** di ricetta. Per le ricette Pedestal è invece lo sblocco del **tier** (`PedestalRecipeTier.hasUnlocked`);
- il `required_advancement` della ricetta.

I dettagli del passo dicono quale dei due manca. Per le ricette non di Spectrum il passo guarda il libro delle ricette vanilla.

Sblocchi del tipo di ricetta (verificati in gioco):
- Pedestal: `spectrum:place_pedestal`. `canPlayerCraft` però usa il tier al suo posto; EMI invece chiede **anche** `place_pedestal`, che non è un antenato di `lategame/build_complex_pedestal_structure`: con `advancement grant ... until` sul solo ramo lategame EMI mostra «You have not discovered this recipe yet».
- Fusion Shrine: `spectrum:build_fusion_shrine` (`FusionShrineRecipe.UNLOCK_IDENTIFIER`). `collect_all_basic_pigments_besides_brown` è invece lo sblocco del **blocco** (`FusionShrineBlock.UNLOCK_IDENTIFIER`).

### Strutture (preset)

I preset sono in `src/dev/resources/scenarios/presets/` (oppure in `run/client/scenarios/presets/`). `pos` è la posizione del blocco "macchina".

- **`pedestal_moonstone`**: Pedestal Moonstone con la struttura completa.
  - Multiblock `spectrum:pedestal_complex`, 13×13×7.
  - Il centro è il blocco sotto il Pedestal.
- **`fusion_shrine`**: Fusion Shrine di basalto con la sua struttura.
  - Multiblock `spectrum:fusion_shrine`, 9×9×5.
  - Il centro è il blocco sotto la shrine. Serve il cielo libero sopra.

Per costruirli il runner fa quattro cose:
1. prende il multiblock Modonomicon dai dati di Spectrum;
2. svuota l'ingombro;
3. piazza i blocchi "display" del JSON del multiblock, dal basso in alto;
4. controlla il risultato con `Multiblock.validate(level, centro, Rotation.NONE)`, lo stesso controllo che fanno `PedestalBlockEntity` e `FusionShrineBlock`.

Lo scenario `presets` verifica anche il riconoscimento da parte di Spectrum: si ottengono gli advancement `completed_multiblock`, cioè `lategame/build_complex_pedestal_structure` e `build_fusion_shrine`.
Per farli scattare servono due accorgimenti:
- il Pedestal va ripiazzato dal giocatore, perché serve un proprietario;
- il genitore dell'advancement va revocato e riconcesso, perché il criterio `revelationary:advancement_gotten` scatta solo quando il genitore viene ottenuto di nuovo.

## Craft reali alle macchine

Esempi completi in `craft_t6.json`.
- **Pedestal**: si piazza il Pedestal a mano (proprietario), si riempie con `item replace block X Y Z container.N with ...` (slot 0-8 griglia, 9-13 polveri ciano/magenta/giallo/nero/bianco), poi un blocco di redstone accanto avvia il craft. Il risultato esce a terra sopra il Pedestal: `assert_item` con `pos` e `within`.
- **Fusion Shrine**: clic destro a mano vuota (proprietario), clic destro con il secchio del fluido, poi un clic destro per ogni stack (`hold` con quantità, es. `"item": "allthemodium:allthemodium_ingot 8"`). Il risultato esce due blocchi sopra la shrine.

## Scenari inclusi

- `smoke`: spada in mano in prima persona, serie di 6 screenshot dell'animazione, terza persona, advancement concesso e verificato, preset Pedestal Moonstone, guidebook.
- `presets`: i due preset costruiti, validati e riconosciuti da Spectrum.
- `progressione_t6`: catena di sblocco del T6 da un giocatore vergine (advancement passo per passo, ricette, voci e pagine del guidebook, voce T6 nascosta da bloccata, voce Idea con acquisto delle pagine, EMI).
- `bident_menu`: menu della modalità del bident (Shift + destro) per T6, T7, T8 e T9 (T9: screenshot `menu_t9` e scelta di Fractal): screenshot del menu con il tooltip, scelta con il clic, `bident_mode` controllato, senza Shift niente menu.
- `workstaff_menu`: menu della workstaff T7+ (`SAWorkstaffScreen`, Shift + destro) per T7, T8 e T9: screenshot `ws_t7_*`, `ws_t8_drop`, `ws_t9_*` di ogni griglia (principale, Area, Area più grande, Incantesimi, Drop, con i tooltip), scelta di 7x7 (T7) e 9x9 (T9) con `spectrum:aoe` controllato, Foundry spenta/riaccesa (anche con Silk Touch: torna Fortune), Inventory Insertion spenta (T8); alla fine il T6 col menu di Spectrum (`ws_t6_area_spectrum`). Non ancora eseguito (2026-11-19).
- `workstaff_indietro`: «Indietro» al centro delle sotto-pagine del menu della workstaff (nella principale il centro chiude), col mouse, col tasto destro e coi tasti (`gui_key`: movimento, Drop, Inventario): Area → Area più grande → indietro → indietro, Incantesimi → Drop → indietro → indietro, l'oggetto non cambia. Screenshot `indietro_*`.
- `workstaff_area_9x9`: colpo ad area 9x9 della Prismatic workstaff in sopravvivenza: particelle sul client subito dopo (al massimo 8 eventi di rottura per l'area + il blocco mirato, ~576 invece di ~5184). Screenshot `area_9x9_particelle`.
- `craft_t6`: craft reali della spada (base bedrock), della workstaff e del bident alla Fusion Shrine con Dragonrot (tutto il T6 è lì dal 2026-11-18), con incantesimi e componenti attesi e cambio di modalità del bident.
- `texture_t6`: texture del set T6 «Trichromatic» (set Castone, installato da `tools/install_textures.py`): i 15 pezzi e gli oggetti speciali in mano in prima persona (texture `_oversized`), balestra carica con freccia, freccia di topazio e razzo, bident mentre carica il lancio, serie animate, arco nei tre stati di tensione, canna lanciata, armatura indossata davanti e dietro (animata, 20 fotogrammi), inventario di sopravvivenza con tutte le icone.
- `texture_t7`: lo stesso per il T7 «Umbral» (armatura a 12 fotogrammi).
- `texture_t8`: lo stesso per il T8 «Argent» (set `t8-argent`, armatura a 20 fotogrammi), a x 80.
- `texture_t9`: lo stesso per il T9 Alloy «Prismatic» (set `t9-m3`, icone e armatura a 40 fotogrammi da 5 tick, ciclo di 10 s), a x 120.
- `armatura_t6`: armatura T6 indossata in terza persona davanti e dietro, serie dell'animazione, due armor stand di lato, poi il giocatore che cammina e corre (azione `move` con `keep`): mantello e perizoma si muovono con la fisica del mantello vanilla.
- `elmo_t6_vicino`: solo la testa dell'armatura T6, per controllare lo spiraglio fra i due gusci dell'elmo (testa 10 esterna e testa 9 interna, mezzo pixel). Due armor stand (uno a terra, uno sollevato di 1,5 blocchi) con l'armatura T6; camera in prima persona, mano nascosta, a 0,5-1 blocco: davanti, di lato, da sotto verso il mento (anche in verticale) e di tre quarti dal basso (10 screenshot). Il runner non ha FOV né zoom: ci si avvicina con la posizione.
- `elmo_t6_zombie`: come `elmo_t6_vicino`, ma con due zombie (NoAI, Silent, Invulnerable, PersistenceRequired) con l'armatura T6 completa, perché hanno la testa con la pelle e l'elmo si vede sul viso. Stesse inquadrature (davanti, di lato, da sotto, tre quarti dal basso) più due viste intere a 2,5 blocchi (12 screenshot).
- `armatura_t7`: lo stesso per l'armatura T7 (12 fotogrammi, ciclo di 3 s; per il T6 Castone 20 fotogrammi, ciclo di 5 s).
- `armatura_t8`: lo stesso per l'armatura T8 (20 fotogrammi, ciclo di 5 s), 40 blocchi più in là del T7.
- `armatura_t9`: lo stesso per l'armatura T9 Alloy (40 fotogrammi, ciclo di 10 s: la serie fa 20 scatti ogni 10 tick), 40 blocchi più in là del T8.
- `progressione_t7`: catena di sblocco del T7: il vibranium preso prima del T6 non apre niente; il T7 arriva dopo il T6 e il primo lingotto di vibranium; ogni pezzo T7 dopo il pezzo T6 corrispondente; voce T7 nascosta da bloccata (l'Idea esiste solo per il T6).
- `progressione_t8`: catena di sblocco del T8: l'unobtainium preso prima del T7 non apre niente; nemmeno il T7 senza gli attrezzi glass crest (`spectrum:unlocks/malachite/glass_crest_tools`, ricerca/04); il T8 arriva con tutti e tre; ogni pezzo T8 dopo il pezzo T7 corrispondente; voce T8 nascosta da bloccata.
- `progressione_t9` (Alloy): una lega presa prima del T8 non apre niente (poi si revoca il gate T9 per azzerare il criterio del lingotto); il T8 senza leghe nemmeno; UNA sola lega (unobtainium-allthemodium) apre il T9 (OR delle tre, ricerca/04 §5.1; il gate si chiama `unlocks/tiers/prismatic`); ogni pezzo T9 dopo il pezzo T8 corrispondente; voce T9 nascosta da bloccata; EMI della workstaff, della spada e della corazza (bloccata) T9.
- `modelli_t9`: stati dei modelli T9 in prima persona (come la parte 3 di `casi_client` per il T8): balestra a riposo, in tensione (serie), carica con freccia, razzo e le 6 frecce di vetro; arco teso (serie); canna lanciata; bident in carica. A x 160.
- `sblocchi`: tutti gli sblocchi in ordine fino all'ultimo tier, senza oggetti né cambi di modalità (vedi sopra).
- `balestra`: traiettoria della freccia client contro server e modelli in prima persona delle balestre e degli oversized.
- `scia_frecce` (2026-11-24): scia colorata delle frecce T9 (`PrismaticTrail`, particelle in `client/PrismaticTrailParticles`). Tiri veri in terza persona a x 600, z 600: balestra T9 (anche con 3 frecce cariche, come il Multishot) e arco T9 a piena tensione; subito dopo il tiro la visuale gira di 75° (`look`) perché la scia, vista da dietro, resterebbe coperta dal giocatore. Confronto con arco e balestra T8 (solo le particelle `crit` vanilla, beige), freccia marcata evocata con `/summon` (marcatore nell'NBT: `"neoforge:attachments":{"spectral_ascension:prismatic_trail":1b}`), freccia piantata a terra dopo 40 tick (niente scintille), tiri di notte. 10 screenshot. La parte lato server (quali frecce hanno il marcatore) è nel GameTest `prismatic_trail_marker`.
- `casi_client` (2026-11-18): casi scritti apposta per il client, indipendenti dagli altri scenari:
  - cesoie T6/T7/T8 in tre dispenser accesi da blocchi di redstone veri: le pecore vengono tosate e le cesoie restano dentro;
  - stati dei modelli T8 in prima persona da guardare negli screenshot: balestra a riposo, in carica (serie), carica con freccia, razzo e le 6 frecce di vetro; arco teso (serie); canna lanciata; bident in carica.
  La logica lato server degli stessi casi è nei GameTest `SAFunctionTests`, `SAEdgeCaseTests` e `SACraftTests`.
- `vetrina` (2026-11-24): uno screenshot «vetrina» per tier (T6-T9) per il README. Stanza di blocchi di Spectrum a x 2991-3009, z 2995-3010 (isolata, si svuota e si ricostruisce a ogni esecuzione): calcite levigata (pareti), colonne `polished_calcite_pillar`, zoccolo e pavimento di basalto levigato, piedistallo di calcite cesellata; per l'Argent la stanza è in negativo (basalto/calcite invertiti) perché le icone sono chiare. 15 glow item frame invisibili sulla parete di fondo (riga di 7 in alto, gruppi 2x2 di armatura a sinistra e di armi a destra), armor stand con l'armatura e lo spadone alzato. Cielo aperto a mezzogiorno, HUD e mano nascosti, FOV 40 a ~10 blocchi. Per 1920x1080: `tools/run_auto.sh --scenario vetrina --size 1920x1080` (il client è a schermo intero nel compositor virtuale). Le immagini scelte sono in `docs/vetrina/`.
- `vetrina_prismatic_fasi`: stessa stanza e inquadratura del T9 di `vetrina`, ma 24 screenshot (`fase_00`-`fase_23`) ogni ~10 tick, per scegliere le fasi scura e chiara del ciclo animato di 200 tick. `tools/run_auto.sh --scenario vetrina_prismatic_fasi --size 1920x1080`.
- `strumenti`: auto-verifica delle altre azioni:
  - ricetta Pedestal bloccata da `required_advancement`;
  - voce del guidebook;
  - Revelationary;
  - Shift + destro sul bident (Ferocious → Fractal);
  - arco teso in terza persona;
  - `assert_log`;
  - comando `/sa scenario`.

## Scenario `sblocchi`: tutte le ricette visibili

`sblocchi.json` concede **in ordine** gli advancement che servono per vedere in EMI e nel guidebook le ricette di tutti i tier.
- **Generato**: lo scrive `tools/gen_data.py` (funzione `gen_unlock_scenario`) a ogni esecuzione. Non va modificato a mano.
- **Non dà oggetti e non cambia la modalità di gioco.** Si può usare nel proprio mondo: `/sa scenario run sblocchi`.
  - Concede soltanto, non revoca niente: gli advancement che ci sono già restano. Un comando che non ha niente da concedere risulta comunque OK.
  - Ogni concessione scrive nel log una riga `log` con il motivo, seguita dal comando: `[SA-SCENARIO] sblocchi #n command OK /advancement grant ...`.
- **Ordine dei passi**:
  1. progressione di Spectrum richiesta, `SPECTRUM_BASE_UNLOCKS` in `gen_data.py`, con `until` (l'advancement e i suoi antenati):
     - `place_pedestal`;
     - `lategame/build_complex_pedestal_structure` (Pedestal Moonstone);
     - `build_fusion_shrine`;
     - `lategame/create_malachite_tool` (Idea del T6);
     - `milestones/unlock_ink_use`, che serve all'area della workstaff, all'overcharge e al bident;
  2. per ogni tier di `TIERS`, nell'ordine:
     - il tier, `unlocks/tiers/<nome del tier>` (`trichromatic`, `umbral`, `argent`, `prismatic`). Si concede intero, al posto del primo lingotto;
     - per ogni oggetto, gli sblocchi di Spectrum dei suoi ingredienti (glass crest, bedrock tools, accessori Dike), poi l'advancement dell'oggetto. Sono gli stessi lucchetti dell'advancement generato (`object_locks`);
     - le pagine dell'Idea (solo T6);
  3. controlli: ogni advancement concesso, ogni ricetta (`assert_recipe_unlocked`), la voce del tier e ogni sua pagina ricetta, la voce Idea (solo T6). Infine gli screenshot di EMI (una ricetta Pedestal e una Fusion Shrine per tier) e della prima pagina della voce del tier.
- **Aggiungere un tier (T8, Alloy)**: basta aggiungerlo a `TIERS` e le sue righe a `OBJECTS` in `gen_data.py`, con i lucchetti degli ingredienti in `EXTRA_UNLOCKS` come per il T7, poi rilanciare `python3 -I tools/gen_data.py`.
  - Lo scenario si allunga da solo.
  - Solo se il nuovo tier richiede un passo di Spectrum che nessun oggetto usa come ingrediente, quel passo va aggiunto a `SPECTRUM_BASE_UNLOCKS`.
- Per provarlo da un giocatore vergine, prima di `sblocchi` serve uno scenario locale che esegua `advancement revoke @s everything` e `reset_book`.

## Scenario `balestra`

Confronta la freccia vista dal client con quella del server (`track_projectile`) per le balestre vanilla, glass crest, T6, T7, T8 e T9 (Alloy).
Poi fotografa in prima persona le balestre (a riposo, in tensione e cariche) e gli altri oggetti oversized, accanto alle versioni glass crest.

## Note

- Il mondo `sa_test` è persistente: ogni scenario deve prepararsi da solo lo stato che gli serve (`clear`, `advancement revoke`, `tp`, ...).
  `python3 -I tools/reset_test_world.py --force` ricrea il mondo.
- Lo stato del client viene riportato a quello normale all'inizio e alla fine di ogni scenario: HUD visibile, prima persona, nessuna schermata, tasti rilasciati.
- Il runner gira sul client e manda il lavoro lato server al server integrato. Serve quindi un mondo in singleplayer, non un server dedicato.
