# MUST CHECK — Checklist di progressione

> **Regola del progetto:** ogni tier, oggetto, ricetta, advancement o voce di guidebook che aggiungiamo passa questa checklist **prima** di essere considerato finito. Se una casella non si può spuntare, il lavoro non è chiuso.

Fonte principale: wiki di Spectrum, [Adjusting Progression](https://github.com/DaFuqs/Spectrum/wiki/Adjusting-Progression), letta dal clone locale `Adjusting-Progression.md`. La wiki è in parte superata rispetto a Spectrum 1.12.9: le correzioni sono in fondo, verificate nel jar e nel sorgente.

## Perché esiste questa checklist

La wiki si apre con un avvertimento: la progressione di Spectrum è diversa da quella delle altre mod. Toccata senza cura, **confonde i giocatori o li blocca del tutto (softlock)**, cioè non possono più progredire.

**Concetto di modding — advancement come lucchetti.** In Spectrum quasi tutto è sbloccato da *advancement* (gli "obiettivi" di Minecraft), molti dei quali nascosti. Un advancement ha dei *criteri* (cosa deve succedere). Spectrum li usa come lucchetti:

- le ricette hanno un campo `required_advancement`: senza quell'advancement la macchina non esegue la ricetta;
- le voci del guidebook si sbloccano con advancement;
- le *revelation* (via la libreria Revelationary) nascondono blocchi e item finché non ottieni un advancement (cartella `milestones/`, prefisso `reveal_`);
- altri lucchetti stanno in `milestones/` (stelle cadenti, overenchanting, quarto slot del Potion Workshop).

Il trigger usato per collegare un lucchetto all'altro è `revelationary:advancement_gotten`, cioè "hai ottenuto l'advancement X".

## Checklist (da ripetere per ogni tier e per ogni passo)

### A. Coerenza ricetta ↔ guidebook

- [ ] La ricetta e la voce del guidebook che la spiega usano **lo stesso** advancement di sblocco. La wiki lo mostra con `spectrum:unlocks/items/crafting_tablet`: un advancement dedicato, usato sia dalla ricetta sia dalla voce. Se cambia uno solo dei due, il giocatore vede la ricetta senza la spiegazione, o viceversa.
- [ ] Per ogni oggetto esiste un advancement dedicato del tipo `<modid>:unlocks/items/<oggetto>`. Si regolano le condizioni **dentro l'advancement**, non spargendo id diversi tra ricette e voci.

### B. Ogni lucchetto è raggiungibile

- [ ] Ogni `required_advancement` delle nostre ricette punta a un advancement **esistente**.
- [ ] Ogni criterio `revelationary:advancement_gotten` punta a un advancement esistente.
- [ ] **Nessun riferimento circolare**: A richiede B e B richiede A. Il comando di debug **non** li trova, quindi vanno controllati a mano. Si disegna la catena del tier dalla radice (advancement di Spectrum) fino all'oggetto.
- [ ] Nessuna ricetta usa oggetti oggi irraggiungibili perché dietro `spectrum:__wip`: **Artist's Palette** e **Spectral Shard** ([11](11-gimmick-armatura-alloy.md)).
- [ ] Nessun minerale aggiunto in altre dimensioni (es. Mining Dimension di ATM) fa saltare un passo: il cloak di Revelationary vale ovunque, ma l'accesso a Nether, End e Deeper Down non va reso inutile ([12](12-materiali-spectrum-mining-dimension.md)).
- [ ] Nessun lucchetto dipende da `spectrum:__wip`. È l'id che Spectrum usa per "non ancora disponibile" e non esiste nel jar: chi ci dipende è **irraggiungibile**.

### C. Il gating segue la catena del tier

- [ ] Tutto il nostro contenuto richiede il late game di Spectrum: il Pedestal di ultimo tier, cioè `COMPLEX` nel codice (quello che chiamiamo "Moonstone").
- [ ] Ogni tier richiede il tier precedente: T6 → T7 → T8 → Alloy.
- [ ] Gli advancement "visibili" (fuori da `hidden/`, `progression/`, `milestones/`) hanno un parent e un criterio chiamato `gotten_previous` che richiede quel parent. È la convenzione di Spectrum, e il comando sanity segnala chi non la rispetta.
- [ ] Ogni dipendenza da progressione ATM (es. avere un lingotto di vibranium) è **suggerita al giocatore**: advancement visibile, voce del guidebook o quest. La wiki avverte che gate oscuri si scoprono solo per caso.

### D. Ricette Pedestal

- [ ] Il tier della ricetta corrisponde alla cartella: `BASIC` → `/tier1/`, `SIMPLE` → `/tier2/`, `ADVANCED` → `/tier3/`, `COMPLEX` → `/tier4/`. Per noi, quindi, `data/<modid>/recipe/pedestal/tier4/...`.
- [ ] Le polveri moonstone (`white`) si usano solo con tier `COMPLEX`, e l'onyx (`black`) non con BASIC o SIMPLE. Per noi è sempre vero, perché usiamo solo `COMPLEX`.
- [ ] Nel JSON il tier si scrive `"tier": "complex"`: non esiste un tier `moonstone`. Vedi [01](01-pedestal-fusion-shrine-ricette.md).

### D2. Ricette Fusion Shrine

- [ ] La Fusion Shrine **non ha tier**. Il requisito "Pedestal Moonstone" va espresso con un nostro advancement in `required_advancement`, che dipende da `spectrum:lategame/build_complex_pedestal_structure`.
- [ ] Le ricette di upgrade che preservano l'oggetto (`copy_components`) hanno `"disable_yield_upgrades": true`. Senza, l'upgrade Yield della struttura potrebbe duplicare l'attrezzo. Il rischio è dedotto dal codice, da provare in gioco.
- [ ] L'oggetto da preservare è il **primo** ingrediente della lista: `copy_components` copia da lì.

### D3. Validità dei JSON

- [ ] I campi scritti male vengono **ignorati senza errori nel log**. Ogni ricetta e advancement va validato con uno script, oltre che con `/spectrum sanity`.
- [ ] Le ricette stanno nel nostro namespace (`data/<modid>/recipe/...`), mai in `spectrum`. Le voci del guidebook invece stanno sotto `data/spectrum/modonomicon/...` nel nostro jar, come fa Spectral Decorations. Vedi [04](04-progressione-e-guidebook.md).

### E. Pulizia generale (controllata dal comando sanity)

- [ ] Ogni item ha una ricetta.
- [ ] Ogni chiave di traduzione esiste (nomi, gruppi di ricette).
- [ ] Ogni item è nel creative tab, altrimenti sparisce anche da JEI/EMI.
- [ ] Gli item usati in Fusion Shrine ed Enchanter hanno un colore registrato, se la ricetta genera effetti basati sul colore.
- [ ] I blocchi (se ne aggiungeremo) hanno tag mineable e loot table.

### F. Verifica in gioco

- [ ] Nella tua istanza CurseForge di ATM10 lanciare `/spectrum sanity <nostro_modid>` (serve permesso livello 2, cioè cheat attivi in singleplayer) e leggere gli avvisi `[SANITY: ...]` nel log (`logs/latest.log`).
- [ ] Lanciare anche `/spectrum sanity` (senza argomento, controlla Spectrum) per assicurarsi di non aver rotto niente di Spectrum.
- [ ] Mondo nuovo, survival: verificare che il tier si sblocchi nell'ordine previsto e che la voce del guidebook compaia insieme alla ricetta.

## Il comando di debug: `/spectrum sanity [mod_id]`

Verificato nel sorgente ≈1.12.9 (`src/main/java/de/dafuqs/spectrum/commands/SanityCommand.java`, registrato in `registries/SpectrumCommands.java:11-13`). Accetta il **mod id** come argomento, quindi controlla anche il nostro addon. Controlli rilevanti per noi:

| Controllo (tag nel log) | Cosa trova | Riga |
| --- | --- | --- |
| `Pedestal Recipe Ingredients` | Polveri onyx/moonstone usate in tier troppo bassi | 163-183 |
| `… Recipe Unlocks` | `required_advancement` che punta ad advancement inesistenti | 290-298 |
| `Has_Advancement Criteria` / `Advancement_Count Criteria` | Criteri che citano advancement inesistenti | 326-353 |
| `Advancement Gating` | Advancement visibili senza `gotten_previous` verso il parent | 356-394 |
| `Pedestal Recipes` | Ricette pedestal nella cartella `tierN` sbagliata | 396-416 |
| `Recipes` | Item del mod senza ricetta | 307-312 |
| `Block Cloaks` / `Item Cloaks` | Revelation legate ad advancement inesistenti | 314-325 |
| traduzioni | Chiavi lang mancanti | 185+ |

Limite dichiarato dalla wiki: **non rileva i riferimenti circolari**.

## Dove la wiki è superata (verificato su 1.12.9)

| La wiki dice | In 1.12.9 è | Verifica |
| --- | --- | --- |
| Le ricette hanno il campo `advancement_identifier` | Il campo è `required_advancement`, e l'esempio della wiki stessa lo usa | 1558 occorrenze nei JSON di `jar:data/spectrum/recipe/`, zero di `advancement_identifier` |
| Trigger `spectrum:has_advancement` (secondo esempio) | Si usa `revelationary:advancement_gotten` | 1419 occorrenze in `jar:data/spectrum/advancement/`, zero di `spectrum:has_advancement` |
| Il link al comando punta a `Commands#spectrum_test_progression_sanity` | Il comando è `/spectrum sanity [mod_id]` e la pagina Commands della wiki non lo documenta | `SanityCommand.java:67-77` |
| `milestones/unlock_overenchanting_with_enchanter` sblocca l'overenchanting all'Enchanter | L'unico criterio richiede `spectrum:__wip`, che non esiste: **oggi è irraggiungibile** | `jar:data/spectrum/advancement/milestones/unlock_overenchanting_with_enchanter.json` |

L'ultimo punto tocca direttamente il nostro obiettivo "incantesimi oltre il vanilla". Approfondimento in [03-incantesimi-livello-massimo-e-config.md](03-incantesimi-livello-massimo-e-config.md).

## Lucchetti in `milestones/` citati dalla wiki

- `spectrum:milestones/unlock_shooting_stars`: abilita la comparsa delle stelle cadenti.
- `spectrum:milestones/unlock_overenchanting_with_enchanter`: libri oltre il livello vanilla all'Enchanter. In 1.12.9 è bloccato, vedi sopra.
- `spectrum:milestones/unlock_fourth_potion_workshop_reagent_slot`: quarto slot reagente del Potion Workshop.

## Nota: noi siamo un addon, non un modpack

La wiki è scritta per chi fa modpack e **sovrascrive** gli advancement di Spectrum con un datapack. Noi preferiamo **non sovrascrivere** file di Spectrum: è un principio del design doc ("non toccare le altre mod"). Creiamo i nostri advancement nel nostro namespace, che *dipendono* da quelli di Spectrum tramite `revelationary:advancement_gotten`. Se un giorno servisse cambiare un lucchetto di Spectrum (es. l'overenchanting), va deciso esplicitamente e segnato nel design doc.
