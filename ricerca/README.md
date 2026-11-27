# Ricerca — Spectrum e il suo ecosistema, per Spectral Ascension

Base di conoscenza per sviluppare **Spectral Ascension**, addon NeoForge 1.21.1 per Spectrum 1.12.9 in ATM10. Data: 2026-11-16.

Prima regola: **ogni tier e ogni passo passa la [checklist di progressione](00-checklist-progressione.md)** (MUST CHECK).

## Indice

| File | Contenuto |
| --- | --- |
| [00-checklist-progressione.md](00-checklist-progressione.md) | **MUST CHECK.** Checklist dalla wiki Adjusting-Progression, comando `/spectrum sanity`, correzioni alla wiki |
| [01-pedestal-fusion-shrine-ricette.md](01-pedestal-fusion-shrine-ricette.md) | Pedestal e Fusion Shrine: funzionamento, formato JSON completo, `copy_components`, tag, ricette d'esempio per noi |
| [02-oggetti-bedrock-e-speciali.md](02-oggetti-bedrock-e-speciali.md) | Oggetti bedrock (indistruttibilità, incantesimi preimpostati), workstaff, ultra greatsword, crossbow, bident; asset e naming |
| [03-incantesimi-livello-massimo-e-config.md](03-incantesimi-livello-massimo-e-config.md) | Dove Enchanter e Bedrock Anvil leggono il livello massimo, come superarlo, analisi di `spectrum-startup.toml` |
| [04-progressione-e-guidebook.md](04-progressione-e-guidebook.md) | Advancement di sblocco, Revelationary, guidebook Modonomicon, tooltip «added by», voce in General, bozze JSON |
| [05-ecosistema-addon.md](05-ecosistema-addon.md) | Addon esistenti (Spectral Decorations, Spectrum Compat…), come sono costruiti, lezioni pratiche |
| [06-api-e-punti-di-estensione.md](06-api-e-punti-di-estensione.md) | API pubbliche di Spectrum, coordinate Gradle, stabilità dell'API, domande per DaFuqs |
| [07-allthemodium-e-atm10.md](07-allthemodium-e-atm10.md) | Materiali, tag, tier e statistiche Allthemodium; modifiche ATM10 (nessuna su Spectrum) |
| [08-template-addon.md](08-template-addon.md) | Template di partenza del progetto Gradle (ModDevGradle), verificato con una build |
| [09-livelli-massimi-incantesimi-atm10.md](09-livelli-massimi-incantesimi-atm10.md) | Livello massimo di ogni incantesimo in ATM10 (Apothic Enchanting, overenchanting Spectrum, fonti speciali) |
| [10-armature-piu-forti-atm10.md](10-armature-piu-forti-atm10.md) | Armature più forti del pack, tetti degli attributi (AttributeFix), formula del danno di Apothic Attributes |
| [11-gimmick-armatura-alloy.md](11-gimmick-armatura-alloy.md) | Gimmick Ink per l'immunità dell'armatura Alloy: Azure Dike, proposte A–D, raccomandazione A + D |
| [12-materiali-spectrum-mining-dimension.md](12-materiali-spectrum-mining-dimension.md) | Minerali Spectrum nella Mining Dimension di ATM: strati, biome modifier, JSON d'esempio, rischi di progressione |
| [13-menu-workstaff.md](13-menu-workstaff.md) | Menu della workstaff (griglie, pacchetto, area): come aggiungere Foundry, Inventory Insertion e 7×7 / 9×9 per T7+ |

Legenda usata nei file: **[V]** verificato su file o codice, **[D]** dedotto, da provare in gioco.

## Risposte alle domande della ricerca, in breve

### 1. Pedestal e Fusion Shrine ([01](01-pedestal-fusion-shrine-ricette.md))

- Una ricetta è un **file JSON** in un *datapack* (dati caricati dal server). Il nostro jar ne contiene uno: `data/spectral_ascension/recipe/...`. Il campo `type` (`spectrum:pedestal`, `spectrum:pedestal_shapeless`, `spectrum:fusion_shrine`) dice quale macchina la usa.
- **Pedestal.** Nel JSON il tier si scrive `"tier": "complex"`: non esiste un tier `moonstone`. Il gioco controlla da solo blocco Moonstone, struttura completa e advancement `spectrum:lategame/build_complex_pedestal_structure`, quindi il nostro vincolo costa zero codice. Le polveri di gemma vanno in `colors` (`spectrum:cyan`, `magenta`, `yellow`, `black`, `white`). Il Pedestal **non** preserva incantesimi o dati dell'input.
- **Fusion Shrine.** Fino a 7 input, un fluido (1 secchio), condizioni di mondo (`world_conditions`) ed effetti (`effects`). **Non ha tier**, quindi il requisito Moonstone passa da un nostro advancement in `required_advancement`. `copy_components` copia i dati dal **primo** ingrediente e fonde gli incantesimi: è lo strumento per gli upgrade.
- Tutto l'equipaggiamento bedrock e malachite di Spectrum si fa alla Fusion Shrine.
- Possibile **dupe** con l'upgrade Yield sulla Fusion: dedotto dal codice, da provare. Si previene con `"disable_yield_upgrades": true`.

### 2. Oggetti bedrock e speciali ([02](02-oggetti-bedrock-e-speciali.md))

- **Indistruttibilità:** componente vanilla `minecraft:unbreakable` + `fireResistant()`, oltre all'esclusione dal tag `minecraft:enchantable/durability`. Nessun codice speciale.
- **Incantesimi preimpostati:** li mette la **ricetta Fusion** (`result.components.enchantments`). L'interfaccia `Preenchanted` serve solo per il creativo e il tavolo degli incantesimi, quindi un `/give` dà l'oggetto senza incantesimi.
- **Oggetti speciali:** classi `public` e non `final`. Molte abilità scattano con `instanceof`, quindi estendendole le ereditiamo. Da rifare: predicati client del modello `_oversized`, riparazione legata alla malachite, menu del workstaff (massimo 5×5).
- Malachite e glass crest **non** sono indistruttibili. Non esiste un set base malachite (piccone, armatura…).

### 3. Livello massimo degli incantesimi ([03](03-incantesimi-livello-massimo-e-config.md))

- **Bedrock Anvil:** limita a `getMaxLevel()` (`BedrockAnvilScreenHandler.createResult`). La config `bedrock_anvil_can_exceed_max_vanilla_enchantment_level` toglie il limite a **tutti** gli oggetti. Non lancia `AnvilUpdateEvent`, quindi per un limite per tier serve un **mixin** (codice che modifica un metodo di Spectrum al caricamento).
- **Enchanter:** le ricette `enchantment_upgrade` arrivano già a VIII. Oltre il vanilla serve l'advancement `spectrum:milestones/unlock_overenchanting_with_enchanter`, che dipende da `spectrum:__wip` (inesistente): **oggi è irraggiungibile**. Quando l'Enchanter incanta un oggetto non applica nessun limite di livello.
- **Tetto tecnico:** 255 (codec vanilla 1.21.1).
- **Raccomandazione del file 03:** una funzione unica `capFor(stack, incantesimo)` con bonus per tier letti da item tag, mixin mirati su Anvil ed Enchanter, config di Spectrum lasciata a `false`.

### 4. Progressione e guidebook ([04](04-progressione-e-guidebook.md), [00](00-checklist-progressione.md))

- La progressione è fatta di **advancement nascosti** collegati con il trigger `revelationary:advancement_gotten`. Le cartelle sono `unlocks/` (lucchetti di ricette e voci), `milestones/reveal_*` (blocchi nascosti), `hidden/` e l'albero visibile.
- **Guidebook:** libreria **Modonomicon** (1.123.0), libro `spectrum:guidebook`. Un addon aggiunge voci mettendo i JSON in `data/spectrum/modonomicon/books/guidebook/entries/<categoria>/` **dentro il proprio jar**. Spectral Decorations fa così.
- **Tooltip «added by …»:** è il campo standard `description` della voce, collegato a una chiave lang (`"§oAdded by Spectral Ascension"`). Niente codice.
- **Voce in General:** sempre visibile, `read_by_default`. Nel file 04 c'è una posizione proposta e la bozza JSON.
- L'addon può essere aggiunto a un **mondo già esistente**: aprendo il guidebook Spectrum ricalcola gli sblocchi.

### 5. API e punti di estensione ([06](06-api-e-punti-di-estensione.md))

- Utili: interfacce `api.item.*` (es. `Preenchanted`), `ItemGroupIDs` + `CreativeSubTabEvent` di Fractal per mettere i nostri item nella sotto-tab Equipment di Spectrum, i recipe type pubblici, circa 40 data component, registri di colori e ink.
- Non ci sono **eventi custom**. Gli integration pack non sono estendibili. I tier del Pedestal sono un enum chiuso, quindi non si può aggiungere un tier 5.
- **L'API non è stabile:** tra ≈1.12.9 e 1.13.0 sono cambiati 7 file in `api/`, e Spectral Decorations 1.10.1 probabilmente si rompe con 1.13.0. Conviene un `versionRange` chiuso su Spectrum.

### 6. Template ([08](08-template-addon.md))

- **ModDevGradle 2.0.140** (lo stesso di Spectrum), Java 21, NeoForge 21.1.265 come l'istanza.
- **Spectrum** si prende da Modrinth maven (`maven.modrinth:spectrum:1.12.9-1.21.1-neo`), perché il maven di DaFuqs si ferma a 1.12.5. Le sue dipendenze vanno dichiarate a mano.
- **Allthemodium 3.2.1** si prende da CurseMaven (`curse.maven:allthemodium-364466:9143638`), insieme a GeckoLib.

## Allthemodium e ATM10, in breve ([07](07-allthemodium-e-atm10.md))

- **ATM10 non modifica Spectrum:** niente in KubeJS, la config di Spectrum è tutta di default.
- Bedrock e malachite di Spectrum sono al livello netherite: minano l'allthemodium ma non vibranium, unobtainium o ancient stone. I tag `c:incorrect_for_<tier>_tool` di ATM sono pronti da usare nei nostri Tier.
- Tool ATM (velocità / bonus danno / incantabilità): allthemodium 15/12/85, vibranium 16/25/100, unobtainium 18/35/125, alloy 35/65/200. Sono tutti indistruttibili. **Non esiste armatura Alloy** in ATM.
- Le leghe sono 3 (vibranium-ATM, unobtainium-ATM, unobtainium-vibranium) e hanno solo lingotto, polvere e blocco. In ATM10 si fanno con macchine di altre mod.

## Decisioni prese dopo la ricerca (2026-11-16)


| Tema | Decisione |
| --- | --- |
| Tetto incantesimi | Riferimento = **massimo "pulito" di ATM10** (config di Apothic, senza fonti rare). Incantesimo principale (2026-11-27): T6 = tetto − 2, T7 = tetto − 1, T8 = tetto (es. Sharpness IX), Alloy = tetto + 2 (Sharpness XI). Valori in [09](09-livelli-massimi-incantesimi-atm10.md) |
| Dove vale il tetto | Per **tutti** gli oggetti al Bedrock Anvil e all'Enchanter. Mixin sui punti in cui Spectrum legge `getMaxLevel()` (Bedrock Anvil, range normale delle ricette di upgrade, costo in XP), che oggi vedono il valore vanilla perché Apothic non li intercetta ([03](03-incantesimi-livello-massimo-e-config.md), [09](09-livelli-massimi-incantesimi-atm10.md)) |
| Mining Dimension | **Shimmerstone, azurite, stratine e paltaeria** (niente malachite) nella Mining Dimension di Allthemodium, a **circa metà** delle quantità originali. Biome modifier sul bioma `allthemodium:mining` ([12](12-materiali-spectrum-mining-dimension.md)) |
| Protezione a Ink | **Dal T7** (C, *Spectral Equilibrium*): ogni punto di danno oltre armatura e Dike costa Ink da **ciascuno** dei 5 colori elementali: T7 1000, T8 500, Alloy 250 per colore. Il T6 ha solo armatura e Dike. Se manca un colore la protezione si ferma. Solo Alloy: **D**, *Moonstone Absolution* (colpo letale → 1 cuore, White Ink, ricarica). Una Pigment Palette ha 409.600 Ink per colore; 1 blocco di pigmento = 900 Ink ([11](11-gimmick-armatura-alloy.md)) |
| Materiali per tier | T6 allthemodium, T7 vibranium, T8 unobtainium. **Alloy: tutte e 3 le leghe** (Vibranium+Allthemodium, Unobtainium+Allthemodium, Unobtainium+Vibranium), **1 lingotto per lega per oggetto**: 6 Piglich Heart a oggetto, 90 per i 15 oggetti Alloy, come **costo indiretto** (si consumano nel craft delle leghe, 2 per lingotto). Il Piglich Heart **non** è un ingrediente delle nostre ricette (decisione utente 2026-11-18, [14](14-leghe-allthemodium.md)). Tag `c:ingots/<lega>` ([07](07-allthemodium-e-atm10.md)) |
| PvP | **Nessun moltiplicatore**: il costo Ink è lo stesso contro mob e giocatori |
| Azure Dike e accessori | **40 per tutti i tier**, Alloy compreso. Gli accessori Dike entrano nelle ricette T6: Puff Circlet → elmo, Azure Dike Ring + Shieldgrasp Amulet → petto, Azure Dike Belt + Azuresque Dike Core → gambali. Con il set indossato gli accessori Dike indossati **si disattivano** (tooltip) |
| Valore del Dike | 40 = accessori Dike non potenziati: Puff Circlet 4 + Azure Dike Ring 6 + Azure Dike Belt 8 + Shieldgrasp Amulet al minimo 2 = 20, × 2 dell'Azuresque Dike Core (`src:items/trinkets/*.java`) |
| Piccone = workstaff | **Niente piccone separato**: la workstaff è il piccone di ogni tier (glass crest → T6 → T7 → T8 → Alloy, alla Fusion Shrine). Sblocchi del menu: glass crest Silk Touch / Fortune IV / Resonance, area fino a 5×5; **T6** Fortune VIII + Efficiency IX; **T7** + Foundry, area 7×7; **T8** + Inventory Insertion; **Alloy** livelli +3, area 9×9. **15 oggetti per tier, 60 in tutto**; Alloy 90 Piglich Heart |
| Incantesimi preimpostati | Gli oggetti **escono già incantati al tetto** del loro tier. Per l'Alloy, copiando gli incantesimi su un Gilded Book all'Enchanter si ottiene solo il massimo ATM10: livello XII salvato sull'oggetto + mixin su `getHighestEnchantmentsInItemBowls` (strada A, vedi nota sotto) |
| Overenchanting all'Enchanter | **Lo sblocchiamo**, sovrascrivendo `spectrum:milestones/unlock_overenchanting_with_enchanter` (eccezione voluta). Da comunicare a DaFuqs |
| Bident | **Fusione**: dal T6 un solo bident che unisce ferocious e fractal. Il set base comprende anche arco, canna da pesca e cesoie (dalla bedrock); la balestra c'è solo come upgrade glass crest. 15 oggetti per tier, 60 in tutto (piccone unito alla workstaff) |
| Statistiche armatura | Elmo / petto / gambali / stivali: T6 6/14/10/6 = 36, T7 7/16/11/7 = 41, T8 8/18/12/8 = 46, Alloy 9/19/13/9 = 50. Toughness a pezzo 5 / 6 / 7 / 7,5; knockback a pezzo 0,1 / 0,15 / 0,2 / 0,225 (Alloy 0,9 totale). Riferimenti del pack: Dragonsteel 74, Silent Gear fino a 90 ([10](10-armature-piu-forti-atm10.md)) |
| Nomi dei tier | **T6 Trichromatic** (Tricromatico), **T7 Umbral** (Umbrale), **T8 Argent** (Argenteo), **Alloy Prismatic** (Prismatico). Id: `trichromatic_<oggetto>`, `umbral_…`, `argent_…`, `prismatic_…`. Nessun nome di metallo ATM nei nomi visibili; collisioni controllate su 286 jar del pack |
| Mod id | `spectral_ascension` (stessa forma di Spectral Decorations: `spectral_decorations`) |
| Package Java | `cromocon.spectralascension` |
| Licenza | LGPL-3.0, come Spectrum |

## Decisioni ancora aperte

Nessuna. Tutte le decisioni emerse dalla ricerca sono prese; i valori numerici vanno bilanciati con i test in gioco.

### Nota tecnica: il +3 dell'Alloy e il Gilded Book

L'Enchanter copia gli incantesimi su un Gilded Book in `EnchanterBlockEntity.enchantCenterItem` (`src:blocks/enchanter/EnchanterBlockEntity.java:338-346`). Il metodo raccoglie i livelli più alti dalle ciotole con `getHighestEnchantmentsInItemBowls` (riga 366), lo stesso metodo che calcola il costo in XP (riga 376). Due strade:

- **A — livello salvato sull'oggetto (XII) + mixin** su `getHighestEnchantmentsInItemBowls`: prima di raccogliere i livelli, gli oggetti Alloy vengono ridotti al massimo ATM10. Semplice, ma vale solo per l'Enchanter di Spectrum. Un estrattore di incantesimi di un'altra mod copierebbe XII.
- **B — livello salvato al massimo ATM10 (IX) + bonus dinamico +3** con l'evento NeoForge `GetEnchantmentLevelEvent`, solo mentre l'oggetto Alloy è usato. Qualsiasi copia prende IX da sola, ma serve un ritocco al tooltip.

**Scelta: A** (2026-11-16). Limite accettato: un estrattore di incantesimi di un'altra mod potrebbe copiare XII; va controllato in gioco quali mod di ATM10 lo permettono.

Domande pronte per DaFuqs: [06, §4](06-api-e-punti-di-estensione.md).

## Fonti usate

- **Jar dell'istanza CurseForge di ATM10** (sola lettura, estratti in scratchpad): `spectrum-1.12.9-1.21.1-neo.jar`, `SpectralDecorations-1.10.1-1.21.1-neo.jar`, `spectrumcompat-0.1.0.jar`, `allthemodium-3.2.1_mc_1.21.1.jar`, `modonomicon-1.21.1-neoforge-1.123.0.jar`, `revelationary-neoforge-1.5.2+1.21.1.jar`. Inoltre `config/spectrum-startup.toml` e `kubejs/`.
- **Sorgente Spectrum** [DaFuqs/Spectrum](https://github.com/DaFuqs/Spectrum), branch `1.21.1-neoforge`:
  - commit `890784ca` (26/10/2026) come riferimento "≈1.12.9": è l'ultimo prima del bump a 1.13.0;
  - HEAD `9710add3` (1.13.0) per i confronti.
  - I numeri di riga citati possono differire leggermente dal jar rilasciato.
- **Wiki** [DaFuqs/Spectrum/wiki](https://github.com/DaFuqs/Spectrum/wiki), clonata localmente, in particolare [Adjusting-Progression](https://github.com/DaFuqs/Spectrum/wiki/Adjusting-Progression).
- **Web:** API Modrinth, maven `maven.is-immensely.gay`, Cloudsmith (Modonomicon), CurseMaven. I singoli URL sono citati in ciascun file.
- **Non usati:** server `atlas` (non serviva) e CurseForge web (risponde 403 alle richieste automatiche).
